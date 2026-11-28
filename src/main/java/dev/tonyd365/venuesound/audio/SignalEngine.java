package dev.tonyd365.venuesound.audio;

import dev.tonyd365.venuesound.block.MixerLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Follows audio through the cables, the same way in the mod and in the Workbench:
 * <pre>
 * source -> (outlets) -> console input strip -> MAIN L / MAIN R / AUX 1 / AUX 2 -> (outlets)
 *        -> amplifier (INPUT A feeds SPK 1-2, INPUT B feeds SPK 3-4) -> (outlets) -> speakers
 * </pre>
 * A console strip applies gain, 3-band EQ, fader, pan, aux sends and the effects send.
 * Pure Java: the graph is reached through {@link Graph}.
 *
 * @param <N> the node type (a router snapshot in game, a Device in the Workbench)
 */
public final class SignalEngine<N> {

    /** How the engine sees a device graph. */
    public interface Graph<N> {
        NodeKind kind(N node);

        List<Edge<N>> outputs(N node);

        /** Console setting (see MixerLayout). */
        float param(N console, String key);

        /** Which console input {@code upstream} is plugged into, or -1. */
        int inputSlot(N console, N upstream);

        /** Amplifier gain, 0 when switched off. */
        float ampLevel(N amp);

        /** Whether a speaker is switched on. */
        boolean speakerOn(N speaker);

        /** Stage box: input socket N reaches console input (slot + N) through its snake. */
        default boolean multichannel(N node) {
            return false;
        }

        /** Identity used to keep EQ and effects memory per console. */
        Object id(N node);
    }

    public record Edge<N>(N target, int fromPort, int toPort) {
    }

    /** Receives the frame that reaches each speaker. */
    public interface Sink<N> {
        void accept(N speaker, float[] frame);
    }

    /** Receives a console's levels for every frame it mixes (for its meters and PEAK LEDs). */
    public interface Meter<N> {
        /**
         * @param strip  the channel strip the frame came in on
         * @param input  that strip's peak after gain and EQ, 0-1 (1 = full scale)
         * @param mainL  the peak this frame adds to MAIN L, 0-1
         * @param mainR  the peak this frame adds to MAIN R, 0-1
         */
        void console(N console, int strip, float input, float mainL, float mainR);
    }

    private volatile Meter<N> meter;

    public void setMeter(Meter<N> meter) {
        this.meter = meter;
    }

    private static float peak(float[] frame) {
        float p = 0;
        if (frame != null) {
            for (float f : frame) {
                p = Math.max(p, Math.abs(f));
            }
        }
        return p / 32768F;
    }

    private static final int MAX_HOPS = 32;

    private record EqKey(Object console, int slot, Object stream) {
    }

    private record EchoKey(Object console, Object stream) {
    }

    private final Map<EqKey, ConsoleDsp.Eq> eqs = new ConcurrentHashMap<>();
    private final Map<EchoKey, ConsoleDsp.Echo> echoes = new ConcurrentHashMap<>();

    public void clear() {
        eqs.clear();
        procEqs.clear();
        procEchoes.clear();
        envelopes.clear();
        echoes.clear();
    }

    /** Routes one 20 ms frame from a source device through the cables. */
    public void route(Graph<N> g, N source, Object stream, short[] pcm, Sink<N> sink) {
        Set<Object> visited = new HashSet<>();
        visited.add(g.id(source));
        beforeConsole(g, source, stream, pcm, sink, visited, 0, 0);
    }

    /** Routes a frame straight into a console strip (the console's own USB player). */
    public void routeIntoConsole(Graph<N> g, N console, int slot, Object stream, short[] pcm, Sink<N> sink) {
        Set<Object> visited = new HashSet<>();
        visited.add(g.id(console));
        console(g, console, slot, stream, pcm, sink, visited);
    }

    private void beforeConsole(Graph<N> g, N from, Object stream, short[] pcm, Sink<N> sink, Set<Object> visited, int depth, int chan) {
        if (depth > MAX_HOPS) {
            return;
        }
        for (Edge<N> e : g.outputs(from)) {
            N next = e.target();
            NodeKind kind = g.kind(next);
            if (kind == NodeKind.OUTLET && visited.add(g.id(next))) {
                beforeConsole(g, next, stream, pcm, sink, visited, depth + 1, g.multichannel(next) ? e.toPort() : chan);
            } else if (kind == NodeKind.PROCESSOR && visited.add(g.id(next))) {
                float[] f = new float[pcm.length];
                for (int i = 0; i < pcm.length; i++) {
                    f[i] = pcm[i];
                }
                f = processor(g, next, stream, f);
                if (f != null) {
                    beforeConsole(g, next, stream, ConsoleDsp.toPcm(f), sink, visited, depth + 1, chan);
                }
            } else if (kind == NodeKind.MIXER && visited.add(g.id(next))) {
                int slot = g.inputSlot(next, from);
                if (slot >= 0 && chan > 0) {
                    slot += chan;
                    if (slot >= (int) g.param(next, "layout.usb")) {
                        slot++; // the snake skips the console's own USB strip
                    }
                }
                if (slot >= 0 && slot < (int) g.param(next, "layout.strips")) {
                    console(g, next, slot, stream, pcm, sink, visited);
                }
            }
        }
    }

    private void console(Graph<N> g, N c, int s, Object stream, short[] pcm, Sink<N> sink, Set<Object> visited) {
        if (g.param(c, "mute." + s) > 0.5F) {
            return;
        }
        ConsoleDsp.Eq eq = eqs.computeIfAbsent(new EqKey(g.id(c), s, stream), k -> new ConsoleDsp.Eq());
        float[] strip = eq.process(pcm, g.param(c, "low." + s), g.param(c, "mid." + s), g.param(c, "high." + s), g.param(c, "gain." + s));
        float fader = g.param(c, "fader." + s);
        float pan = g.param(c, "pan." + s);
        float[][] bus = new float[4][];
        bus[MixerLayout.BUS_MAIN_L] = scale(strip, fader * ConsoleDsp.panLeft(pan) * g.param(c, "main.l"));
        bus[MixerLayout.BUS_MAIN_R] = scale(strip, fader * ConsoleDsp.panRight(pan) * g.param(c, "main.r"));
        bus[MixerLayout.BUS_AUX1] = scale(strip, g.param(c, "aux1." + s) * g.param(c, "aux1.m"));
        bus[MixerLayout.BUS_AUX2] = scale(strip, g.param(c, "aux2." + s) * g.param(c, "aux2.m"));
        float send = g.param(c, "efx." + s);
        float ret = g.param(c, "efx.m");
        EchoKey ek = new EchoKey(g.id(c), stream);
        if ((send > 0F && ret > 0F) || echoes.containsKey(ek)) {
            float[] wet = echoes.computeIfAbsent(ek, k -> new ConsoleDsp.Echo())
                    .process(scale(strip, send * fader), g.param(c, "efx.time"));
            addInto(bus[MixerLayout.BUS_MAIN_L], wet, ret * g.param(c, "main.l"));
            addInto(bus[MixerLayout.BUS_MAIN_R], wet, ret * g.param(c, "main.r"));
        }
        Meter<N> m = meter;
        if (m != null) {
            m.console(c, s, peak(strip), peak(bus[MixerLayout.BUS_MAIN_L]), peak(bus[MixerLayout.BUS_MAIN_R]));
        }
        for (Edge<N> e : g.outputs(c)) {
            int b = e.fromPort();
            if (b < 0 || b >= 4) {
                continue;
            }
            Set<Object> v = new HashSet<>(visited);
            deliver(g, stream, e, bus[b], sink, v, 0);
        }
    }

    /** After the console: follow outlets to amplifiers and speakers. */
    private void deliver(Graph<N> g, Object stream, Edge<N> e, float[] frame, Sink<N> sink, Set<Object> visited, int depth) {
        N next = e.target();
        if (depth > MAX_HOPS || !visited.add(g.id(next))) {
            return;
        }
        switch (g.kind(next)) {
            case OUTLET -> {
                for (Edge<N> out : g.outputs(next)) {
                    deliver(g, stream, out, frame, sink, visited, depth + 1);
                }
            }
            case PROCESSOR -> {
                float[] f = processor(g, next, stream, frame);
                if (f != null) {
                    for (Edge<N> out : g.outputs(next)) {
                        deliver(g, stream, out, f, sink, new HashSet<>(visited), depth + 1);
                    }
                }
            }
            case AMPLIFIER -> {
                float level = g.ampLevel(next);
                if (level <= 0F) {
                    return;
                }
                int side = e.toPort() == 1 ? 1 : 0; // INPUT A or B
                float[] amped = scale(frame, level);
                for (Edge<N> out : g.outputs(next)) {
                    if ((out.fromPort() < 2 ? 0 : 1) == side) {
                        deliver(g, stream, out, amped, sink, new HashSet<>(visited), depth + 1);
                    }
                }
            }
            case SPEAKER -> {
                if (g.speakerOn(next)) {
                    sink.accept(next, frame);
                }
            }
            default -> {
            }
        }
    }

    private record ProcKey(Object node, Object stream) {
    }

    private final Map<ProcKey, ConsoleDsp.Eq> procEqs = new ConcurrentHashMap<>();
    private final Map<ProcKey, ConsoleDsp.Echo> procEchoes = new ConcurrentHashMap<>();
    private final Map<ProcKey, float[]> envelopes = new ConcurrentHashMap<>();

    /** Graphic EQ (7 bands folded onto the 3-band filter), compressor, or plain level; null when off. */
    private float[] processor(Graph<N> g, N p, Object stream, float[] in) {
        float k = g.ampLevel(p);
        if (k <= 0F) {
            return null;
        }
        ProcKey key = new ProcKey(g.id(p), stream);
        if (g.param(p, "proc.eq") > 0.5F) {
            float low = (g.param(p, "eq.0") + g.param(p, "eq.1")) / 2;
            float mid = (g.param(p, "eq.2") + g.param(p, "eq.3") + g.param(p, "eq.4")) / 3;
            float high = (g.param(p, "eq.5") + g.param(p, "eq.6")) / 2;
            return procEqs.computeIfAbsent(key, x -> new ConsoleDsp.Eq()).process(ConsoleDsp.toPcm(in), low, mid, high, k);
        }
        if (g.param(p, "proc.comp") > 0.5F) {
            float thr = g.param(p, "comp.thr") * 32767F * 0.5F;
            float ratio = Math.max(1F, g.param(p, "comp.ratio"));
            float[] env = envelopes.computeIfAbsent(key, x -> new float[1]);
            float[] out = new float[in.length];
            for (int i = 0; i < in.length; i++) {
                float a = Math.abs(in[i]);
                env[0] = a > env[0] ? a : env[0] * 0.9995F;
                float gain = env[0] > thr ? (thr + (env[0] - thr) / ratio) / env[0] : 1F;
                out[i] = in[i] * gain * k;
            }
            return out;
        }
        if (g.param(p, "proc.fx") > 0.5F) {
            float mix = g.param(p, "fx.mix");
            float[] wet = procEchoes.computeIfAbsent(key, x -> new ConsoleDsp.Echo()).process(in, g.param(p, "fx.time"));
            float[] out = new float[in.length];
            for (int i = 0; i < in.length; i++) {
                out[i] = (in[i] + wet[i] * mix) * k;
            }
            return out;
        }
        if (g.param(p, "proc.xo") > 0.5F) {
            int mode = Math.round(g.param(p, "xo.mode"));
            if (mode != 0) {
                // LOW: sub output (mid and highs cut); HIGH: tops output (lows cut)
                return procEqs.computeIfAbsent(key, x -> new ConsoleDsp.Eq())
                        .process(ConsoleDsp.toPcm(in), mode == 1 ? 6 : -12, -12, mode == 1 ? -12 : 0, k);
            }
        }
        return scale(in, k);
    }

    private static float[] scale(float[] in, float k) {
        float[] out = new float[in.length];
        if (k != 0F) {
            for (int i = 0; i < in.length; i++) {
                out[i] = in[i] * k;
            }
        }
        return out;
    }

    private static void addInto(float[] dst, float[] src, float k) {
        for (int i = 0; i < dst.length && i < src.length; i++) {
            dst[i] += src[i] * k;
        }
    }
}
