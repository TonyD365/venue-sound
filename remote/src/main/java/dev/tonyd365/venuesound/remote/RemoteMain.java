package dev.tonyd365.venuesound.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.tonyd365.venuesound.client.Osc;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VenueSound Remote: turns an iPad (or any browser) into the console's control surface, without
 * changing the game. It serves a mixing page over HTTP on this computer, translates every move into
 * an OSC message to VenueSound's OSC port (UDP 9000), and keeps the page in sync from the OSC
 * feedback the game sends back.
 *
 * <pre>java -jar venuesound-remote.jar [--game 127.0.0.1:9000] [--port 9001] [--bind 0.0.0.0] [--key CODE]</pre>
 */
public final class RemoteMain {

    private static final String[] STRIP_CONTROLS = {"fader", "gain", "hi", "mid", "lo", "aux1", "aux2", "efx", "pan", "mute", "pfl"};
    private static final String[] MASTERS = {"main/l", "main/r", "aux/1/master", "aux/2/master", "efx/return", "efx/time", "phones", "phantom"};

    /** Last value of every OSC address the game reported. */
    private final Map<String, Float> values = new ConcurrentHashMap<>();
    private volatile long lastFeedback;
    private final DatagramSocket osc;
    private final InetSocketAddress game;
    private final String key;

    private RemoteMain(InetSocketAddress game, String key) throws IOException {
        this.game = game;
        this.key = key;
        this.osc = new DatagramSocket();
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> a = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            a.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        }
        String[] g = a.getOrDefault("game", "127.0.0.1:9000").split(":");
        int port = Integer.parseInt(a.getOrDefault("port", "9001"));
        String bind = a.getOrDefault("bind", "0.0.0.0");
        String key = a.getOrDefault("key", newKey());
        RemoteMain r = new RemoteMain(new InetSocketAddress(g[0], Integer.parseInt(g.length > 1 ? g[1] : "9000")), key);
        r.start(bind, port);
        String url = "http://" + ("0.0.0.0".equals(bind) ? lanAddress() : bind) + ":" + port + "/?key=" + key;
        System.out.println("VenueSound Remote");
        System.out.println("  Open on your iPad (same Wi-Fi): " + url);
        System.out.println("  Talking to the game's OSC port: " + r.game.getHostString() + ":" + r.game.getPort());
        System.out.println("  In the game, right-click a control on the console you want to run.");
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> window(url, r));
        }
    }

    private void start(String bind, int port) throws IOException {
        Thread listen = new Thread(this::listen, "osc-feedback");
        listen.setDaemon(true);
        listen.start();
        Thread hello = new Thread(() -> {
            // a message now and then, so the game knows where to send its feedback
            while (true) {
                send("/venuesound/hello", 1F);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "osc-hello");
        hello.setDaemon(true);
        hello.start();
        HttpServer http = HttpServer.create(new InetSocketAddress(InetAddress.getByName(bind), port), 16);
        http.createContext("/", this::handle);
        http.start();
    }

    private void listen() {
        byte[] buf = new byte[4096];
        while (true) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                osc.receive(p);
                lastFeedback = System.currentTimeMillis();
                Osc.decode(p.getData(), p.getLength(), m -> {
                    if (!m.args().isEmpty()) {
                        values.put(m.address(), m.args().get(0));
                    }
                });
            } catch (IOException e) {
                return;
            }
        }
    }

    private void send(String address, float value) {
        byte[] data = Osc.encode(address, value);
        try {
            osc.send(new DatagramPacket(data, data.length, game));
        } catch (IOException e) {
            // the game isn't running yet: try again next time
        }
    }

    // ------------------------------------------------------------------ state for the page

    /** Strips the game reported, numbered from 1. */
    private int strips() {
        int n = 0;
        for (String k : values.keySet()) {
            if (k.startsWith("/venuesound/ch/")) {
                String[] parts = k.split("/");
                try {
                    n = Math.max(n, Integer.parseInt(parts[3]));
                } catch (RuntimeException e) {
                    // not a strip address
                }
            }
        }
        return n;
    }

    /** The console's strip names: VS-12 has 8 mono strips, VS-24 16; the first stereo strip is the USB player. */
    private static String stripName(int i, int strips) {
        int mono = strips > 10 ? 16 : 8;
        if (i < mono) {
            return String.valueOf(i + 1);
        }
        int ch = mono + 2 * (i - mono) + 1;
        return ch + "/" + (ch + 1) + (i == mono ? " USB" : "");
    }

    private String json() {
        int n = strips();
        if (n == 0) {
            return "{\"console\":null,\"game\":" + (System.currentTimeMillis() - lastFeedback < 5000) + "}";
        }
        StringBuilder b = new StringBuilder(4096);
        b.append("{\"console\":\"").append(n > 10 ? "VS-24" : "VS-12").append("\",\"pos\":\"via OSC ")
                .append(game.getHostString()).append(':').append(game.getPort()).append("\",\"strips\":[");
        int mono = n > 10 ? 16 : 8;
        for (int i = 0; i < n; i++) {
            b.append(i > 0 ? "," : "").append("{\"name\":\"").append(stripName(i, n)).append("\",\"usb\":").append(i == mono).append(",\"v\":{");
            boolean first = true;
            for (String c : STRIP_CONTROLS) {
                Float v = values.get("/venuesound/ch/" + (i + 1) + "/" + c);
                if (v != null) {
                    b.append(first ? "" : ",").append('"').append(c).append("\":").append(fmt(v));
                    first = false;
                }
            }
            b.append("}}");
        }
        b.append("],\"master\":{");
        for (int k = 0; k < MASTERS.length; k++) {
            b.append(k > 0 ? "," : "").append('"').append(MASTERS[k]).append("\":")
                    .append(fmt(values.getOrDefault("/venuesound/" + MASTERS[k], 0F)));
        }
        b.append("},\"meters\":[").append(fmt(values.getOrDefault("/venuesound/meter/l", 0F))).append(',')
                .append(fmt(values.getOrDefault("/venuesound/meter/r", 0F))).append("]}");
        return b.toString();
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    // ------------------------------------------------------------------ HTTP

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            Map<String, String> q = query(ex.getRequestURI().getRawQuery());
            if (!key.equals(q.get("key"))) {
                reply(ex, 403, "text/plain", "Wrong or missing access code: use the address VenueSound Remote shows.".getBytes(StandardCharsets.UTF_8));
                return;
            }
            switch (ex.getRequestURI().getPath()) {
                case "/", "/index.html" -> {
                    try (InputStream in = RemoteMain.class.getResourceAsStream("/remote.html")) {
                        reply(ex, 200, "text/html; charset=utf-8", in == null ? new byte[0] : in.readAllBytes());
                    }
                }
                case "/api/state" -> reply(ex, 200, "application/json", json().getBytes(StandardCharsets.UTF_8));
                case "/api/set" -> {
                    Map<String, String> body = "POST".equals(ex.getRequestMethod())
                            ? query(new String(ex.getRequestBody().readNBytes(256), StandardCharsets.UTF_8)) : Map.of();
                    String address = body.getOrDefault("address", "");
                    float value;
                    try {
                        value = Float.parseFloat(body.getOrDefault("value", ""));
                    } catch (NumberFormatException e) {
                        reply(ex, 400, "text/plain", new byte[0]);
                        return;
                    }
                    if (address.length() > 64 || !address.startsWith("/venuesound/") || !Float.isFinite(value)) {
                        reply(ex, 400, "text/plain", new byte[0]);
                        return;
                    }
                    send(address, value);
                    values.put(address, value);
                    reply(ex, 204, "text/plain", null);
                }
                default -> reply(ex, 404, "text/plain", new byte[0]);
            }
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> m = new TreeMap<>();
        if (raw == null) {
            return m;
        }
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                m.put(URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return m;
    }

    private static void reply(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        if (body == null) {
            ex.sendResponseHeaders(status, -1);
            return;
        }
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String newKey() {
        String chars = "abcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            b.append(chars.charAt(r.nextInt(chars.length())));
        }
        return b.toString();
    }

    private static String lanAddress() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (IOException e) {
            // fall through
        }
        return "127.0.0.1";
    }

    /** A small window with the address, for people who start the jar by double-clicking it. */
    private static void window(String url, RemoteMain r) {
        JFrame f = new JFrame("VenueSound Remote");
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        p.setBackground(new Color(0x16, 0x17, 0x1a));
        JLabel title = new JLabel("Open this on your iPad (same Wi-Fi):");
        title.setForeground(new Color(0xc8, 0xcc, 0xd2));
        JLabel link = new JLabel(url);
        link.setFont(link.getFont().deriveFont(Font.BOLD, 20f));
        link.setForeground(new Color(0xe8, 0xb8, 0x4a));
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(0x9a, 0xa0, 0xa6));
        JButton copy = new JButton("Copy address");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null));
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(status, BorderLayout.CENTER);
        south.add(copy, BorderLayout.EAST);
        p.add(title, BorderLayout.NORTH);
        p.add(link, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        f.setContentPane(p);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
        new javax.swing.Timer(1000, e -> status.setText(r.strips() > 0 ? "Connected to the game: " + (r.strips() > 10 ? "VS-24" : "VS-12")
                : System.currentTimeMillis() - r.lastFeedback < 5000 ? "Game found: right-click a control on a console"
                : "Waiting for the game (VenueSound OSC on " + r.game.getHostString() + ":" + r.game.getPort() + ")")).start();
    }
}
