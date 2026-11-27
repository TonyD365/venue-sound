package dev.tonyd365.venuesound.workbench;

import dev.tonyd365.venuesound.audio.NodeKind;

import java.util.ArrayList;
import java.util.List;

/** One device in the Workbench scene, with the same settings as its block entity in the mod. */
public final class Device {

    /** Facing: the direction the device's front points to. */
    public enum Facing {
        NORTH(0), EAST(90), SOUTH(180), WEST(270);

        /** Clockwise model rotation (as in the blockstate files). */
        public final int rotation;

        Facing(int rotation) {
            this.rotation = rotation;
        }

        public Facing clockwise() {
            return values()[(ordinal() + 1) % 4];
        }

        public Facing counterClockwise() {
            return values()[(ordinal() + 3) % 4];
        }

        public int dx() {
            return this == EAST ? 1 : this == WEST ? -1 : 0;
        }

        public int dz() {
            return this == SOUTH ? 1 : this == NORTH ? -1 : 0;
        }
    }

    public enum Type {
        RECEIVER("Wireless Receiver", NodeKind.RECEIVER, "receiver", "receiver"),
        STAND_MIC("Stand Microphone", NodeKind.STAND_MIC, "stand_mic", "stand_mic"),
        MEDIA_DECK("Media Deck", NodeKind.MEDIA_DECK, "media_deck", "media_deck"),
        OUTLET("Wall Outlet", NodeKind.OUTLET, "outlet", "outlet"),
        MIXER("Mixing Console", NodeKind.MIXER, "mixer_left", "mixer"),
        AMPLIFIER("Power Amplifier", NodeKind.AMPLIFIER, "amplifier", "amplifier"),
        PA_SPEAKER("PA Speaker", NodeKind.SPEAKER, "pa_speaker_lower", "pa_speaker"),
        LINE_ARRAY("Line Array Module", NodeKind.SPEAKER, "speaker", "speaker");

        public final String label;
        public final NodeKind kind;
        public final String model;
        /** Key into PortLayout, same as the block id. */
        public final String key;

        Type(String label, NodeKind kind, String model, String key) {
            this.label = label;
            this.kind = kind;
            this.model = model;
            this.key = key;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final int INPUTS = 8;
    public static final float MAX_GAIN = 2.0F;
    public static final int[] RANGES = {16, 32, 48, 64, 96, 128};

    public final Type type;
    public int x;
    public int z;
    public Facing facing;
    /** A cable from one of our sockets into a socket of {@code target}. */
    public record Link(Device target, int fromPort, int toPort) {
    }

    public final List<Link> links = new ArrayList<>();

    // receiver
    public int channel = 1;
    // mixer
    public final float[] gains = new float[INPUTS];
    public final float[] trims = new float[INPUTS];
    public final boolean[] mutes = new boolean[INPUTS];
    public final Device[] inputs = new Device[INPUTS];
    public float master = 1.0F;
    // amplifier
    public boolean ampOn = true;
    public int ampLevel = 100;
    // speakers
    public int range = 64;
    public boolean speakerOn = true;
    // media deck
    public String track = "";
    public boolean loop;
    public boolean playing;

    public Device(Type type, int x, int z, Facing facing) {
        this.type = type;
        this.x = x;
        this.z = z;
        this.facing = facing;
        for (int i = 0; i < INPUTS; i++) {
            gains[i] = 1.0F;
            trims[i] = 1.0F;
        }
    }

    public NodeKind kind() {
        return type.kind;
    }

    /** Every block cell this device occupies: (x, y, z). */
    public List<int[]> cells() {
        List<int[]> cells = new ArrayList<>();
        cells.add(new int[] {x, 0, z});
        if (type == Type.MIXER) {
            Facing right = facing.counterClockwise();
            cells.add(new int[] {x + right.dx(), 0, z + right.dz()});
        } else if (type == Type.PA_SPEAKER) {
            cells.add(new int[] {x, 1, z});
        }
        return cells;
    }

    /** Height of the sound source above the block, for speakers. */
    public double soundHeight() {
        return type == Type.PA_SPEAKER ? 1.0 : 0.5;
    }

    // ------------------------------------------------------------------ same rules as the mod

    public boolean canAddOutput() {
        return links.size() < kind().maxOutputs();
    }

    public List<Device> outputs() {
        List<Device> out = new ArrayList<>();
        for (Link l : links) {
            out.add(l.target());
        }
        return out;
    }

    public boolean portInUse(int fromPort) {
        for (Link l : links) {
            if (l.fromPort() == fromPort) {
                return true;
            }
        }
        return false;
    }

    public float gainFor(Device upstream) {
        for (int i = 0; i < INPUTS; i++) {
            if (inputs[i] == upstream) {
                return mutes[i] ? 0F : gains[i] * trims[i] * master;
            }
        }
        return 0F;
    }

    public int assignInput(Device upstream) {
        for (int i = 0; i < INPUTS; i++) {
            if (inputs[i] == upstream) {
                return i + 1;
            }
        }
        for (int i = 0; i < INPUTS; i++) {
            if (inputs[i] == null) {
                inputs[i] = upstream;
                return i + 1;
            }
        }
        return -1;
    }

    public void stepTrim(int input, int direction) {
        trims[input] = Math.max(0F, Math.min(MAX_GAIN, Math.round((trims[input] + direction * 0.1F) * 10F) / 10F));
    }

    public void stepAmpLevel(boolean down) {
        int[] levels = dev.tonyd365.venuesound.block.PanelLayout.Amp.LEVELS;
        int index = 3;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] == ampLevel) {
                index = i;
            }
        }
        ampLevel = levels[Math.max(0, Math.min(levels.length - 1, index + (down ? -1 : 1)))];
    }

    public void stepRange(boolean down) {
        int index = 0;
        for (int i = 0; i < RANGES.length; i++) {
            if (RANGES[i] == range) {
                index = i;
            }
        }
        range = RANGES[Math.max(0, Math.min(RANGES.length - 1, index + (down ? -1 : 1)))];
    }
}
