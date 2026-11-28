package dev.tonyd365.venuesound.client;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A small OSC 1.0 codec (Open Sound Control, the protocol digital mixing consoles and remote apps
 * such as TouchOSC speak): messages with float, int, true/false arguments, and bundles.
 */
public final class Osc {

    /** One OSC message: an address such as "/venuesound/ch/1/fader" and its numeric arguments. */
    public record Message(String address, List<Float> args) {
        public float first(float fallback) {
            return args.isEmpty() ? fallback : args.get(0);
        }
    }

    private Osc() {
    }

    /** Decodes a packet (a message or a bundle of them); ignores what it can't read. */
    public static void decode(byte[] data, int length, Consumer<Message> out) {
        try {
            decode(ByteBuffer.wrap(data, 0, length).slice(), out, 0);
        } catch (RuntimeException e) {
            // malformed packet: drop it
        }
    }

    private static void decode(ByteBuffer b, Consumer<Message> out, int depth) {
        if (depth > 8 || !b.hasRemaining()) {
            return;
        }
        String address = string(b);
        if (address.equals("#bundle")) {
            b.getLong(); // time tag: applied at once
            while (b.remaining() >= 4) {
                int size = b.getInt();
                if (size <= 0 || size > b.remaining()) {
                    return;
                }
                ByteBuffer element = b.slice();
                element.limit(size);
                decode(element, out, depth + 1);
                b.position(b.position() + size);
            }
            return;
        }
        if (!address.startsWith("/")) {
            return;
        }
        List<Float> args = new ArrayList<>();
        if (b.hasRemaining()) {
            String tags = string(b);
            for (int i = 1; i < tags.length(); i++) {
                switch (tags.charAt(i)) {
                    case 'f' -> args.add(b.getFloat());
                    case 'i' -> args.add((float) b.getInt());
                    case 'd' -> args.add((float) b.getDouble());
                    case 'h' -> args.add((float) b.getLong());
                    case 'T' -> args.add(1F);
                    case 'F' -> args.add(0F);
                    case 's' -> string(b);
                    default -> {
                        out.accept(new Message(address, args));
                        return;
                    }
                }
            }
        }
        out.accept(new Message(address, args));
    }

    private static String string(ByteBuffer b) {
        int start = b.position();
        int end = start;
        while (end < b.limit() && b.get(end) != 0) {
            end++;
        }
        String s = new String(b.array(), b.arrayOffset() + start, end - start, StandardCharsets.UTF_8);
        int padded = (end - start + 4) & ~3;
        b.position(Math.min(b.limit(), start + padded));
        return s;
    }

    /** Encodes a message with one float argument. */
    public static byte[] encode(String address, float value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, address);
        writeString(out, ",f");
        int bits = Float.floatToIntBits(value);
        out.write(bits >>> 24);
        out.write(bits >>> 16);
        out.write(bits >>> 8);
        out.write(bits);
        return out.toByteArray();
    }

    private static void writeString(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
        int pad = 4 - (bytes.length % 4);
        for (int i = 0; i < pad; i++) {
            out.write(0);
        }
    }
}
