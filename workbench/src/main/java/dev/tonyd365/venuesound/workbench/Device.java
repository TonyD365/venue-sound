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
        MIXER("Mixing Console", NodeKind.MIXER, "mixer_c0_r0", "mixer"),
        AMPLIFIER("Power Amplifier", NodeKind.AMPLIFIER, "amplifier", "amplifier"),
        PA_SPEAKER("PA Speaker", NodeKind.SPEAKER, "pa_speaker_lower", "pa_speaker"),
        LINE_ARRAY("Line Array Module", NodeKind.SPEAKER, "speaker", "speaker"),
        CONCERT_SPEAKER("Concert Speaker Stack 4x6", NodeKind.SPEAKER, "concert_c0_l0_d0", "concert_speaker"),
        GRAPHIC_EQ("Graphic Equalizer", NodeKind.PROCESSOR, "graphic_eq", "graphic_eq"),
        COMPRESSOR("Compressor / Limiter", NodeKind.PROCESSOR, "compressor", "compressor"),
        DI_BOX("DI Box", NodeKind.PROCESSOR, "di_box", "di_box"),
        FLOOR_WEDGE("Floor Wedge Monitor", NodeKind.SPEAKER, "floor_wedge", "floor_wedge"),
        SUBWOOFER("Subwoofer", NodeKind.SPEAKER, "subwoofer", "subwoofer"),
        GOOSENECK_MIC("Gooseneck Podium Mic", NodeKind.STAND_MIC, "gooseneck_mic", "gooseneck_mic"),
        CHOIR_MIC("Hanging Choir Mic", NodeKind.STAND_MIC, "choir_mic", "choir_mic"),
        STAGE_BOX("Stage Box (8 ch snake)", NodeKind.OUTLET, "stage_box", "stage_box"),
        FX_PROCESSOR("Effects Processor", NodeKind.PROCESSOR, "fx_processor", "fx_processor"),
        CROSSOVER("Crossover / Speaker Processor", NodeKind.PROCESSOR, "crossover", "crossover"),
        FLOWN_ARRAY("Flown Line Array", NodeKind.SPEAKER, "flown_array", "flown_array"),
        IEM_TRANSMITTER("In-Ear Monitor Transmitter", NodeKind.SPEAKER, "iem_transmitter", "iem_transmitter"),
        STAGE_BOX_16("Stage Box (16 ch snake)", NodeKind.OUTLET, "stage_box_16", "stage_box_16"),
        POWER_SEQUENCER("Power Sequencer", null, "power_sequencer", "power_sequencer"),
        RACK_CABINET("Rack Cabinet", null, "rack_cabinet", "rack_cabinet"),
        TRUSS_POINT("Truss Hanging Point", null, "truss_point", "truss_point"),
        CABLE_CLIP("Cable Clip", null, "cable_clip", "cable_clip");

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

    public static final int INPUTS = dev.tonyd365.venuesound.block.MixerLayout.STRIPS;
    public static final float MAX_GAIN = 2.0F;
    public static final int[] RANGES = {16, 32, 48, 64, 96, 128};

    public final Type type;
    public int x;
    public int z;
    public Facing facing;
    /** A cable from one of our sockets into a socket of {@code target}. */
    public record Link(Device target, int fromPort, int toPort, List<Device> via) {
    }

    public final List<Link> links = new ArrayList<>();

    // receiver
    public int channel = 1;
    // console
    public final java.util.Map<String, Float> params = new java.util.concurrent.ConcurrentHashMap<>(dev.tonyd365.venuesound.block.MixerLayout.defaults());
    public final Device[] inputs = new Device[INPUTS];
    public volatile String usbTrack = "";
    public volatile boolean usbPlaying;
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
    }

    public float get(String param) {
        return params.getOrDefault(param, 0F);
    }

    public NodeKind kind() {
        return type.kind;
    }

    /** Every block cell this device occupies: (x, y, z). */
    public List<int[]> cells() {
        List<int[]> cells = new ArrayList<>();
        cells.add(new int[] {x, 0, z});
        if (type == Type.MIXER) {
            // 3 wide (to the operator's right) by 2 deep (away from the operator)
            Facing right = facing.counterClockwise();
            Facing back = facing.clockwise().clockwise();
            cells.clear();
            for (int r = 0; r < 2; r++) {
                for (int c = 0; c < 3; c++) {
                    cells.add(new int[] {x + right.dx() * c + back.dx() * r, 0, z + right.dz() * c + back.dz() * r, c, r});
                }
            }
        } else if (type == Type.PA_SPEAKER) {
            cells.add(new int[] {x, 1, z});
        } else if (type == Type.CONCERT_SPEAKER) {
            Facing right = facing.counterClockwise();
            Facing back = facing.clockwise().clockwise();
            cells.clear();
            for (int d = 0; d < 2; d++) {
                for (int l = 0; l < 6; l++) {
                    for (int c = 0; c < 4; c++) {
                        cells.add(new int[] {x + right.dx() * c + back.dx() * d, l, z + right.dz() * c + back.dz() * d, c, l, d});
                    }
                }
            }
        }
        return cells;
    }

    /** Height of the sound source above the block, for speakers. */
    public double soundHeight() {
        return type == Type.CONCERT_SPEAKER ? 3.0 : type == Type.PA_SPEAKER ? 1.0 : 0.5;
    }

    // ------------------------------------------------------------------ same rules as the mod

    public boolean canAddOutput() {
        return kind() != null && links.size() < kind().maxOutputs();
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
