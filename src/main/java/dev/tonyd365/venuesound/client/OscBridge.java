package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.MixerLayout;
import dev.tonyd365.venuesound.block.OscTarget;
import dev.tonyd365.venuesound.net.ParamPackets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import dev.tonyd365.venuesound.compat.ClientNet;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Remote control of the mixing console over OSC, like a real digital console: a tablet app
 * (TouchOSC, Open Stage Control...) or any OSC software moves the faders and knobs of the console
 * you last used. Listens on this computer only (UDP, default 127.0.0.1:9000, set in
 * config/venuesound-osc.properties), so the server needs no extra port. Changes go to the server
 * as normal game packets; the console's values are sent back to the controller so its faders
 * follow what happens in the game.
 */
public final class OscBridge {

    /** How a control's OSC value (0-1) maps to its setting. */
    private enum Conv { FADER, KNOB, TOGGLE, INVERTED_TOGGLE }

    private record Binding(List<String> params, Conv conv, boolean feedback) {
    }

    private static final ConcurrentLinkedQueue<Osc.Message> INBOX = new ConcurrentLinkedQueue<>();
    private static final Map<String, Float> SENT = new HashMap<>();
    private static volatile SocketAddress remote;
    /** A new controller connected: send it every value, not only the changes. */
    private static volatile boolean resendAll;
    private static DatagramSocket socket;
    private static int replyPort;
    private static int port = 9000;
    private static boolean warnedNoConsole;
    private static BlockPos announced;
    private static int tick;

    private OscBridge() {
    }

    public static void register() {
        Properties cfg = config();
        if (!Boolean.parseBoolean(cfg.getProperty("enabled", "true"))) {
            return;
        }
        port = Integer.parseInt(cfg.getProperty("port", "9000").trim());
        replyPort = Integer.parseInt(cfg.getProperty("reply_port", "0").trim());
        String bind = cfg.getProperty("bind", "127.0.0.1").trim();
        try {
            socket = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(bind), port));
        } catch (IOException e) {
            VenueSound.LOGGER.warn("OSC: could not listen on {}:{} ({}); OSC remote control is off", bind, port, e.getMessage());
            return;
        }
        Thread t = new Thread(OscBridge::listen, "VenueSound-OSC");
        t.setDaemon(true);
        t.start();
        ClientTickEvents.END_CLIENT_TICK.register(OscBridge::tick);
        VenueSound.LOGGER.info("OSC: listening on {}:{}", bind, port);
    }

    private static Properties config() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("venuesound-osc.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                VenueSound.LOGGER.warn("OSC: could not read {}", file);
            }
        } else {
            p.setProperty("enabled", "true");
            p.setProperty("port", "9000");
            p.setProperty("bind", "127.0.0.1");
            p.setProperty("reply_port", "0");
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "VenueSound OSC remote control. bind=127.0.0.1 accepts apps on this computer only; "
                        + "use 0.0.0.0 to accept a tablet on your network. reply_port=0 answers to the port the app sends from.");
            } catch (IOException e) {
                // fine: defaults apply
            }
        }
        return p;
    }

    private static void listen() {
        byte[] buf = new byte[4096];
        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                SocketAddress from = replyPort > 0 ? new InetSocketAddress(packet.getAddress(), replyPort) : packet.getSocketAddress();
                if (!from.equals(remote)) {
                    resendAll = true;
                }
                remote = from;
                Osc.decode(packet.getData(), packet.getLength(), INBOX::add);
            } catch (IOException e) {
                return;
            }
        }
    }

    // ------------------------------------------------------------------ addresses

    /** Every OSC address the console answers to (VenueSound's own and an X32-style subset). */
    private static Map<String, Binding> bindings(MixerLayout layout) {
        Map<String, Binding> b = new LinkedHashMap<>();
        String[][] strip = {{"fader", "fader."}, {"gain", "gain."}, {"hi", "high."}, {"mid", "mid."}, {"lo", "low."},
                {"aux1", "aux1."}, {"aux2", "aux2."}, {"efx", "efx."}, {"pan", "pan."}, {"mute", "mute."}, {"pfl", "pfl."}};
        for (int i = 0; i < layout.strips; i++) {
            int n = i + 1;
            for (String[] s : strip) {
                Conv conv = s[0].equals("fader") ? Conv.FADER : s[0].equals("mute") || s[0].equals("pfl") ? Conv.TOGGLE : Conv.KNOB;
                b.put("/venuesound/ch/" + n + "/" + s[0], new Binding(List.of(s[1] + i), conv, true));
            }
            String nn = String.format("%02d", n);
            b.put("/ch/" + nn + "/mix/fader", new Binding(List.of("fader." + i), Conv.FADER, true));
            b.put("/ch/" + nn + "/mix/on", new Binding(List.of("mute." + i), Conv.INVERTED_TOGGLE, true));
            b.put("/ch/" + nn + "/mix/pan", new Binding(List.of("pan." + i), Conv.KNOB, true));
        }
        b.put("/venuesound/main/l", new Binding(List.of("main.l"), Conv.FADER, true));
        b.put("/venuesound/main/r", new Binding(List.of("main.r"), Conv.FADER, true));
        b.put("/venuesound/main", new Binding(List.of("main.l", "main.r"), Conv.FADER, false));
        b.put("/main/st/mix/fader", new Binding(List.of("main.l", "main.r"), Conv.FADER, true));
        b.put("/venuesound/aux/1/master", new Binding(List.of("aux1.m"), Conv.FADER, true));
        b.put("/venuesound/aux/2/master", new Binding(List.of("aux2.m"), Conv.FADER, true));
        b.put("/venuesound/efx/return", new Binding(List.of("efx.m"), Conv.FADER, true));
        b.put("/venuesound/efx/time", new Binding(List.of("efx.time"), Conv.KNOB, true));
        b.put("/venuesound/phones", new Binding(List.of("phones"), Conv.KNOB, true));
        b.put("/venuesound/phantom", new Binding(List.of("phantom"), Conv.TOGGLE, true));
        return b;
    }

    /** OSC value 0-1 to the setting's value. */
    private static float toValue(MixerLayout.Control c, Conv conv, float osc) {
        float v = Math.max(0F, Math.min(1F, osc));
        return switch (conv) {
            case FADER -> v * 2F;
            case KNOB -> (float) (c.min() + v * (c.max() - c.min()));
            case TOGGLE -> v >= 0.5F ? 1F : 0F;
            case INVERTED_TOGGLE -> v >= 0.5F ? 0F : 1F;
        };
    }

    /** The setting's value as OSC 0-1. */
    private static float toOsc(MixerLayout.Control c, Conv conv, float value) {
        return switch (conv) {
            case FADER -> value / 2F;
            case KNOB -> (float) ((value - c.min()) / (c.max() - c.min()));
            case TOGGLE -> value >= 0.5F ? 1F : 0F;
            case INVERTED_TOGGLE -> value >= 0.5F ? 0F : 1F;
        };
    }

    // ------------------------------------------------------------------ client thread

    private static void tick(Minecraft mc) {
        tick++;
        BlockPos pos = OscTarget.console();
        MixerBlockEntity m = mc.level != null && pos != null && mc.level.getBlockEntity(pos) instanceof MixerBlockEntity me ? me : null;
        if (m != null && !pos.equals(announced) && mc.player != null) {
            announced = pos;
            SENT.clear();
            mc.player.displayClientMessage(Component.translatable("message.venuesound.osc_target", port).withStyle(ChatFormatting.AQUA), true);
        }
        Map<String, Float> pending = new LinkedHashMap<>();
        Osc.Message msg;
        Map<String, Binding> table = m == null ? Map.of() : bindings(m.layout());
        while ((msg = INBOX.poll()) != null) {
            if (m == null) {
                if (!warnedNoConsole && mc.player != null) {
                    warnedNoConsole = true;
                    mc.player.displayClientMessage(Component.translatable("message.venuesound.osc_no_console").withStyle(ChatFormatting.YELLOW), false);
                }
                continue;
            }
            Binding bind = table.get(msg.address());
            if (bind == null || msg.args().isEmpty()) {
                continue;
            }
            for (String param : bind.params()) {
                MixerLayout.Control c = m.layout().byParam(param);
                if (c != null) {
                    pending.put(param, toValue(c, bind.conv(), msg.first(0F)));
                }
            }
        }
        for (Map.Entry<String, Float> e : pending.entrySet()) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            buf.writeUtf(e.getKey());
            buf.writeFloat(e.getValue());
            ClientNet.send(ParamPackets.SET, buf);
        }
        if (m != null && remote != null && tick % 4 == 0) {
            if (resendAll) {
                resendAll = false;
                SENT.clear();
            }
            feedback(m, table, pos);
        }
    }

    /** Sends the controls that changed since last time back to the controller. */
    private static void feedback(MixerBlockEntity m, Map<String, Binding> table, BlockPos pos) {
        List<byte[]> out = new ArrayList<>();
        for (Map.Entry<String, Binding> e : table.entrySet()) {
            Binding bind = e.getValue();
            if (!bind.feedback()) {
                continue;
            }
            String param = bind.params().get(0);
            MixerLayout.Control c = m.layout().byParam(param);
            if (c == null) {
                continue;
            }
            float v = toOsc(c, bind.conv(), m.get(param));
            Float last = SENT.put(e.getKey(), v);
            if (last == null || Math.abs(last - v) > 0.001F) {
                out.add(Osc.encode(e.getKey(), v));
            }
        }
        // the MAIN meters, for controllers that show them (0-1, about -42 to 0 dB on an LED scale)
        for (int side = 0; side < 2; side++) {
            String address = side == 0 ? "/venuesound/meter/l" : "/venuesound/meter/r";
            float v = ConsoleMeters.main(pos, side);
            Float last = SENT.put(address, v);
            if (last == null || Math.abs(last - v) > 0.005F) {
                out.add(Osc.encode(address, v));
            }
        }
        SocketAddress to = remote;
        for (byte[] data : out) {
            try {
                socket.send(new DatagramPacket(data, data.length, to));
            } catch (IOException e) {
                return;
            }
        }
    }
}
