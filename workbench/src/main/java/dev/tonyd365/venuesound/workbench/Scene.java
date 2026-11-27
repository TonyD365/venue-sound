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
            other.outputs.remove(d);
            for (int i = 0; i < Device.INPUTS; i++) {
                if (other.inputs[i] == d) {
                    other.inputs[i] = null;
                }
            }
        }
    }

    /** Connects a cable FROM {@code a} TO {@code b}. Returns null on success, or why it failed. */
    public String connect(Device a, Device b) {
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
        if (a.outputs.contains(b)) {
            return "Already connected.";
        }
        if (!a.canAddOutput()) {
            return "No free output on " + a.type.label + ".";
        }
        if (b.kind() == NodeKind.MIXER) {
            int input = b.assignInput(a);
            if (input < 0) {
                return "All 8 console inputs are in use.";
            }
        }
        a.outputs.add(b);
        return null;
    }

    public void unplug(Device a) {
        for (Device out : a.outputs) {
            for (int i = 0; i < Device.INPUTS; i++) {
                if (out.inputs[i] == a) {
                    out.inputs[i] = null;
                }
            }
        }
        a.outputs.clear();
        if (a.kind() == NodeKind.MIXER) {
            for (Device d : devices) {
                d.outputs.remove(a);
            }
            for (int i = 0; i < Device.INPUTS; i++) {
                a.inputs[i] = null;
            }
        }
    }

    // ------------------------------------------------------------------ routing (same as SoundRouter)

    /** Gain from {@code source} to every speaker it reaches. */
    public Map<Device, Float> routes(Device source) {
        Map<Device, Float> out = new HashMap<>();
        walk(source, 1F, out, new HashSet<>(), 0, false);
        return out;
    }

    private void walk(Device from, float gain, Map<Device, Float> out, Set<Device> visited, int depth, boolean mixed) {
        if (depth > MAX_HOPS || !visited.add(from)) {
            return;
        }
        for (Device next : from.outputs) {
            switch (next.kind()) {
                case OUTLET -> walk(next, gain, out, visited, depth + 1, mixed);
                case MIXER -> {
                    if (!mixed) {
                        float g = next.gainFor(from);
                        if (g > 0F) {
                            walk(next, gain * g, out, visited, depth + 1, true);
                        }
                    }
                }
                case AMPLIFIER -> {
                    if (mixed && next.ampOn) {
                        walk(next, gain * next.ampLevel / 100F, out, visited, depth + 1, true);
                    }
                }
                case SPEAKER -> {
                    if (mixed && gain > 0F) {
                        out.merge(next, gain, Float::sum);
                    }
                }
                default -> {
                }
            }
        }
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
        Device paLeft = add(Device.Type.PA_SPEAKER, 3, 2, Device.Facing.SOUTH);
        Device paRight = add(Device.Type.PA_SPEAKER, 22, 2, Device.Facing.SOUTH);
        Device amp = add(Device.Type.AMPLIFIER, 12, 1, Device.Facing.SOUTH);
        Device outlet = add(Device.Type.OUTLET, 15, 1, Device.Facing.SOUTH);
        Device stand = add(Device.Type.STAND_MIC, 13, 5, Device.Facing.SOUTH);
        Device console = add(Device.Type.MIXER, 14, 15, Device.Facing.SOUTH);
        Device receiver = add(Device.Type.RECEIVER, 16, 15, Device.Facing.SOUTH);
        Device deck = add(Device.Type.MEDIA_DECK, 11, 15, Device.Facing.SOUTH);
        connect(receiver, console);
        connect(stand, outlet);
        connect(outlet, console);
        connect(deck, console);
        connect(console, amp);
        connect(amp, paLeft);
        connect(amp, paRight);
        listenerX = 13.5;
        listenerZ = 11.5;
    }

    private Device add(Device.Type type, int x, int z, Device.Facing facing) {
        Device d = new Device(type, x, z, facing);
        devices.add(d);
        return d;
    }
}
