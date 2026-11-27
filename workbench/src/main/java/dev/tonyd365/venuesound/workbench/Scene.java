package dev.tonyd365.venuesound.workbench;

import dev.tonyd365.venuesound.audio.NodeKind;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The devices on the Workbench floor, the cables between them and the listener ("you").
 * The signal-chain rules are the same as the mod's SoundRouter.
 */
public final class Scene {

    public static final int WIDTH = 26;
    public static final int DEPTH = 20;
    public static final double WIRELESS_RANGE = 128.0;
    public static final double STAND_MIC_RANGE = 3.0;
    public static final int MAX_CABLE = 32;
    private static final int MAX_HOPS = 32;

    public final List<Device> devices = new CopyOnWriteArrayList<>();
    /** Where you stand: you talk into the mics from here and hear the speakers here. */
    public volatile double listenerX = 13.0;
    public volatile double listenerZ = 11.0;
    /** Channel of the wireless mic you hold. */
    public volatile int micChannel = 1;
    public volatile boolean talking;
    public volatile Path mediaFolder = Path.of("venuesound_media").toAbsolutePath();

    public Device deviceAt(int x, int y, int z) {
        for (Device d : devices) {
            for (int[] c : d.cells()) {
                if (c[0] == x && c[1] == y && c[2] == z) {
                    return d;
                }
            }
        }
        return null;
    }

    public boolean isFree(Device candidate) {
        for (int[] c : candidate.cells()) {
            if (c[0] < 0 || c[2] < 0 || c[0] >= WIDTH || c[2] >= DEPTH || deviceAt(c[0], c[1], c[2]) != null) {
                return false;
            }
        }
        return true;
    }

    public void remove(Device d) {
        devices.remove(d);
        for (Device other : devices) {
            other.links.removeIf(l -> l.target() == d || l.via().contains(d));
            for (int i = 0; i < Device.INPUTS; i++) {
                if (other.inputs[i] == d) {
                    other.inputs[i] = null;
                }
            }
        }
    }

    /** Connects a cable from socket {@code fromPort} of {@code a} into socket {@code toPort} of {@code b}. Null on success, else why not. */
    public String connect(Device a, int fromPort, Device b, int toPort) {
        return connect(a, fromPort, b, toPort, List.of());
    }

    public String connect(Device a, int fromPort, Device b, int toPort, List<Device> via) {
        if (a.kind() == null || b.kind() == null) {
            return "Cable clips hold cables; plug into a device's socket.";
        }
        if (a == b) {
            return "That's the same device.";
        }
        if (a.kind().maxOutputs() == 0) {
            return "Speakers are the end of the chain. Start from a source.";
        }
        if (!a.kind().canFeed(b.kind())) {
            return a.type.label + " can't feed " + b.type.label + ".";
        }
        if (Math.hypot(a.x - b.x, a.z - b.z) > MAX_CABLE) {
            return "Too far: a cable reaches " + MAX_CABLE + " blocks.";
        }
        for (Device.Link l : a.links) {
            if (l.target() == b && l.toPort() == toPort) {
                return "Already connected.";
            }
        }
        if (a.portInUse(fromPort) || !a.canAddOutput()) {
            return "That socket already has a cable.";
        }
        if (b.kind() == NodeKind.MIXER) {
            if (toPort < 0 || toPort >= Device.INPUTS || toPort == dev.tonyd365.venuesound.block.MixerLayout.USB_STRIP) {
                return "That console input doesn't take a cable.";
            }
            if (b.inputs[toPort] != null && b.inputs[toPort] != a) {
                return "IN " + (toPort + 1) + " already has a cable.";
            }
            b.inputs[toPort] = a;
        }
        a.links.add(new Device.Link(b, fromPort, toPort, List.copyOf(via)));
        return null;
    }

    /** First free OUT socket and matching IN socket, for the demo. */
    public String connect(Device a, Device b) {
        int from = 0;
        while (a.portInUse(from)) {
            from++;
        }
        int to = 0;
        if (b.kind() == NodeKind.MIXER) {
            while (to < Device.INPUTS && b.inputs[to] != null) {
                to++;
            }
        }
        return connect(a, from, b, to);
    }

    public void unplug(Device a) {
        for (Device out : a.outputs()) {
            for (int i = 0; i < Device.INPUTS; i++) {
                if (out.inputs[i] == a) {
                    out.inputs[i] = null;
                }
            }
        }
        a.links.clear();
        if (a.kind() == NodeKind.MIXER) {
            for (Device d : devices) {
                d.links.removeIf(l -> l.target() == a);
            }
            for (int i = 0; i < Device.INPUTS; i++) {
                a.inputs[i] = null;
            }
        }
    }

    // ------------------------------------------------------------------ routing: the mod's own SignalEngine

    public final dev.tonyd365.venuesound.audio.SignalEngine<Device> engine = new dev.tonyd365.venuesound.audio.SignalEngine<>();

    public final dev.tonyd365.venuesound.audio.SignalEngine.Graph<Device> graph = new dev.tonyd365.venuesound.audio.SignalEngine.Graph<>() {
        @Override
        public NodeKind kind(Device d) {
            return d.kind() == null ? NodeKind.OUTLET : d.kind();
        }

        @Override
        public List<dev.tonyd365.venuesound.audio.SignalEngine.Edge<Device>> outputs(Device d) {
            List<dev.tonyd365.venuesound.audio.SignalEngine.Edge<Device>> edges = new ArrayList<>();
            for (Device.Link l : d.links) {
                if (devices.contains(l.target())) {
                    edges.add(new dev.tonyd365.venuesound.audio.SignalEngine.Edge<>(l.target(), l.fromPort(), l.toPort()));
                }
            }
            return edges;
        }

        @Override
        public float param(Device console, String key) {
            return console.get(key);
        }

        @Override
        public boolean multichannel(Device d) {
            return d.type == Device.Type.STAGE_BOX;
        }

        @Override
        public int inputSlot(Device console, Device upstream) {
            for (int i = 0; i < Device.INPUTS; i++) {
                if (console.inputs[i] == upstream) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public float ampLevel(Device amp) {
            return amp.ampOn ? amp.ampLevel / 100F : 0F;
        }

        @Override
        public boolean speakerOn(Device speaker) {
            return speaker.speakerOn;
        }

        @Override
        public Object id(Device d) {
            return d;
        }
    };

    /** Overall gain from a source to every speaker it reaches (measured with a test tone, without touching effects memory). */
    public Map<Device, Float> routes(Device source) {
        Map<Device, Float> out = new HashMap<>();
        short[] probe = new short[960];
        java.util.Arrays.fill(probe, (short) 10000);
        new dev.tonyd365.venuesound.audio.SignalEngine<Device>().route(graph, source, "probe", probe, (sp, frame) -> {
            float peak = 0;
            for (float f : frame) {
                peak = Math.max(peak, Math.abs(f));
            }
            out.merge(sp, peak / 10000F, Float::sum);
        });
        out.values().removeIf(v -> v < 0.001F);
        return out;
    }

    /** The sources your voice reaches right now: receivers on your mic channel, stand mics next to you. */
    public List<Device> voiceSources() {
        List<Device> list = new ArrayList<>();
        for (Device d : devices) {
            double dist = Math.hypot(d.x + 0.5 - listenerX, d.z + 0.5 - listenerZ);
            if (d.type == Device.Type.RECEIVER && d.channel == micChannel && dist <= WIRELESS_RANGE) {
                list.add(d);
            } else if (d.type == Device.Type.STAND_MIC && dist <= STAND_MIC_RANGE) {
                list.add(d);
            }
        }
        return list;
    }

    /** What is plugged into each console input, for its labels. */
    public static String sourceLabel(Device d) {
        if (d == null) {
            return "--";
        }
        return switch (d.type) {
            case RECEIVER -> "RX " + d.channel;
            case STAND_MIC -> "MIC";
            case MEDIA_DECK -> "DECK";
            default -> "LINE";
        };
    }

    // ------------------------------------------------------------------ demo

    /** A small theatre: two PA cabinets on stage, an amplifier, a console at the back and three sources. */
    public void loadDemo() {
        devices.clear();
        Device paLeft = add(Device.Type.CONCERT_SPEAKER, 0, 2, Device.Facing.SOUTH);
        Device paRight = add(Device.Type.CONCERT_SPEAKER, 22, 2, Device.Facing.SOUTH);
        paLeft.range = 128;
        paRight.range = 128;
        Device amp = add(Device.Type.AMPLIFIER, 12, 1, Device.Facing.SOUTH);
        Device outlet = add(Device.Type.OUTLET, 15, 1, Device.Facing.SOUTH);
        Device stand = add(Device.Type.STAND_MIC, 13, 5, Device.Facing.SOUTH);
        Device console = add(Device.Type.MIXER, 14, 16, Device.Facing.SOUTH);
        Device receiver = add(Device.Type.RECEIVER, 17, 17, Device.Facing.SOUTH);
        Device deck = add(Device.Type.MEDIA_DECK, 10, 17, Device.Facing.SOUTH);
        connect(receiver, 0, console, 0);
        connect(stand, 0, outlet, 0);
        connect(outlet, 1, console, 1);
        connect(deck, 0, console, 2);
        connect(console, dev.tonyd365.venuesound.block.MixerLayout.BUS_MAIN_L, amp, 0);
        connect(console, dev.tonyd365.venuesound.block.MixerLayout.BUS_MAIN_R, amp, 1);
        Device clipA = add(Device.Type.CABLE_CLIP, 11, 9, Device.Facing.NORTH);
        Device clipB = add(Device.Type.CABLE_CLIP, 11, 4, Device.Facing.NORTH);
        connect(amp, 0, paLeft, 0);
        connect(amp, 2, paRight, 0);
        // run the deck's cable along the floor through two clips
        for (Device.Link l : new ArrayList<>(deck.links)) {
            deck.links.remove(l);
            deck.links.add(new Device.Link(l.target(), l.fromPort(), l.toPort(), List.of(clipA)));
        }
        devices.remove(clipB);
        listenerX = 13.5;
        listenerZ = 11.5;
    }

    private Device add(Device.Type type, int x, int z, Device.Facing facing) {
        Device d = new Device(type, x, z, facing);
        devices.add(d);
        return d;
    }
}
