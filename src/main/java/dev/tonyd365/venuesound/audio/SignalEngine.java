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

        /** Identity used to keep EQ and effects memory per console. */
        Object id(N node);
    }

    public record Edge<N>(N target, int fromPort, int toPort) {
    }

    /** Receives the frame that reaches each speaker. */
    public interface Sink<N> {
        void accept(N speaker, float[] frame);
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
        echoes.clear();
    }

    /** Routes one 20 ms frame from a source device through the cables. */
    public void route(Graph<N> g, N source, Object stream, short[] pcm, Sink<N> sink) {
        Set<Object> visited = new HashSet<>();
        visited.add(g.id(source));
        beforeConsole(g, source, stream, pcm, sink, visited, 0);
    }

    /** Routes a frame straight into a console strip (the console's own USB player). */
    public void routeIntoConsole(Graph<N> g, N console, int slot, Object stream, short[] pcm, Sink<N> sink) {
        Set<Object> visited = new HashSet<>();
        visited.add(g.id(console));
        console(g, console, slot, stream, pcm, sink, visited);
    }

    private void beforeConsole(Graph<N> g, N from, Object stream, short[] pcm, Sink<N> sink, Set<Object> visited, int depth) {
        if (depth > MAX_HOPS) {
            return;
        }
        for (Edge<N> e : g.outputs(from)) {
            N next = e.target();
            NodeKind kind = g.kind(next);
            if (kind == NodeKind.OUTLET && visited.add(g.id(next))) {
                beforeConsole(g, next, stream, pcm, sink, visited, depth + 1);
            } else if (kind == NodeKind.PROCESSOR && visited.add(g.id(next))) {
                float k = g.ampLevel(next);
                if (k > 0F) {
                    short[] out = new short[pcm.length];
                    for (int i = 0; i < pcm.length; i++) {
                        out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, pcm[i] * k));
                    }
                    beforeConsole(g, next, stream, out, sink, visited, depth + 1);
                }
            } else if (kind == NodeKind.MIXER && visited.add(g.id(next))) {
                int slot = g.inputSlot(next, from);
                if (slot >= 0) {
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
        for (Edge<N> e : g.outputs(c)) {
            int b = e.fromPort();
            if (b < 0 || b >= 4) {
                continue;
            }
            Set<Object> v = new HashSet<>(visited);
            deliver(g, e, bus[b], sink, v, 0);
        }
    }

    /** After the console: follow outlets to amplifiers and speakers. */
    private void deliver(Graph<N> g, Edge<N> e, float[] frame, Sink<N> sink, Set<Object> visited, int depth) {
        N next = e.target();
        if (depth > MAX_HOPS || !visited.add(g.id(next))) {
            return;
        }
        switch (g.kind(next)) {
            case OUTLET -> {
                for (Edge<N> out : g.outputs(next)) {
                    deliver(g, out, frame, sink, visited, depth + 1);
                }
            }
            case PROCESSOR -> {
                float level = g.ampLevel(next);
                if (level > 0F) {
                    float[] f = scale(frame, level);
                    for (Edge<N> out : g.outputs(next)) {
                        deliver(g, out, f, sink, new HashSet<>(visited), depth + 1);
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
                        deliver(g, out, amped, sink, new HashSet<>(visited), depth + 1);
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
