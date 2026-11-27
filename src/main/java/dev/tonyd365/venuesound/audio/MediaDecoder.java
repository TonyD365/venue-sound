package dev.tonyd365.venuesound.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Decodes .mp3 and .wav files into 48 kHz mono 16-bit PCM, the format Simple Voice Chat uses. */
public final class MediaDecoder {

    public static final int SAMPLE_RATE = 48000;
    /** Longest track accepted, to keep memory bounded (about 22 MB of PCM). */
    private static final int MAX_SECONDS = 4 * 60;

    private MediaDecoder() {
    }

    public static boolean isSupported(String fileName) {
        String n = fileName.toLowerCase(Locale.ROOT);
        return n.endsWith(".mp3") || n.endsWith(".wav");
    }

    public static short[] decode(Path file) throws Exception {
        String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (n.endsWith(".mp3")) {
            return decodeMp3(file);
        }
        if (n.endsWith(".wav")) {
            return decodeWav(file);
        }
        throw new IOException("Unsupported file type: " + file.getFileName());
    }

    private static short[] decodeMp3(Path file) throws Exception {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            ShortBuffer mono = new ShortBuffer();
            int rate = 0;
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer out = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                rate = decoder.getOutputFrequency();
                int channels = decoder.getOutputChannels();
                short[] buf = out.getBuffer();
                int len = out.getBufferLength();
                for (int i = 0; i + channels - 1 < len; i += channels) {
                    int sum = 0;
                    for (int c = 0; c < channels; c++) {
                        sum += buf[i + c];
                    }
                    mono.add((short) (sum / channels));
                }
                bitstream.closeFrame();
                if (rate > 0 && mono.size() > (long) rate * MAX_SECONDS) {
                    break;
                }
            }
            bitstream.close();
            if (rate == 0) {
                throw new IOException("No audio frames in " + file.getFileName());
            }
            return resample(mono.toArray(), rate);
        }
    }

    private static short[] decodeWav(Path file) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat src = source.getFormat();
            int channels = src.getChannels();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16, channels, channels * 2, src.getSampleRate(), false);
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(target, source)) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int r;
                long limit = (long) src.getSampleRate() * MAX_SECONDS * channels * 2;
                long total = 0;
                while (total < limit && (r = pcm.read(chunk)) > 0) {
                    bytes.write(chunk, 0, r);
                    total += r;
                }
                byte[] b = bytes.toByteArray();
                int frames = b.length / (channels * 2);
                short[] mono = new short[frames];
                for (int f = 0; f < frames; f++) {
                    int sum = 0;
                    for (int c = 0; c < channels; c++) {
                        int i = (f * channels + c) * 2;
                        sum += (short) ((b[i] & 0xFF) | (b[i + 1] << 8));
                    }
                    mono[f] = (short) (sum / channels);
                }
                return resample(mono, Math.round(src.getSampleRate()));
            }
        }
    }

    /** Linear-interpolation resampling to {@link #SAMPLE_RATE}. */
    static short[] resample(short[] in, int rate) {
        if (rate == SAMPLE_RATE || in.length == 0) {
            return in;
        }
        int outLen = (int) ((long) in.length * SAMPLE_RATE / rate);
        short[] out = new short[outLen];
        double step = (double) rate / SAMPLE_RATE;
        for (int i = 0; i < outLen; i++) {
            double pos = i * step;
            int i0 = (int) pos;
            int i1 = Math.min(i0 + 1, in.length - 1);
            double frac = pos - i0;
            out[i] = (short) Math.round(in[i0] * (1 - frac) + in[i1] * frac);
        }
        return out;
    }

    /** Growable short array. */
    private static final class ShortBuffer {
        private short[] data = new short[1 << 16];
        private int size;

        void add(short s) {
            if (size == data.length) {
                short[] bigger = new short[data.length * 2];
                System.arraycopy(data, 0, bigger, 0, size);
                data = bigger;
            }
            data[size++] = s;
        }

        int size() {
            return size;
        }

        short[] toArray() {
            short[] out = new short[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }
    }
}
