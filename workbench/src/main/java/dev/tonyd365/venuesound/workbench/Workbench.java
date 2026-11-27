package dev.tonyd365.venuesound.workbench;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.InputStream;

/**
 * VenueSound Workbench: build and test a venue sound system without starting Minecraft.
 * Run it with {@code java -jar venuesound-workbench.jar}.
 */
public final class Workbench {

    private Workbench() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--snapshot".equals(args[0])) {
            snapshot(java.nio.file.Path.of(args[1]));
            return;
        }
        System.setProperty("apple.awt.application.name", "VenueSound Workbench");
        SwingUtilities.invokeLater(Workbench::open);
    }

    /** Renders the demo scene and the console close-up to a PNG without opening a window (for docs and checks). */
    private static void snapshot(java.nio.file.Path out) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Scene scene = new Scene();
        scene.loadDemo();
        Models models = new Models();
        AudioEngine audio = new AudioEngine(scene);
        Inspector inspector = new Inspector(scene, audio, () -> { });
        SceneView view = new SceneView(scene, models, d -> { }, s -> { });
        Device console = scene.devices.stream().filter(d -> d.type == Device.Type.MIXER).findFirst().orElseThrow();
        console.params.put("fader.0", 1.4F);
        console.params.put("fader.2", 0.6F);
        console.params.put("mute.3", 1F);
        console.params.put("high.1", 6F);
        console.params.put("pan.0", -0.6F);
        console.params.put("aux1.2", 0.7F);
        console.usbTrack = "opening-theme.mp3";
        console.usbPlaying = true;
        inspector.show(console);
        java.awt.Dimension sv = view.getPreferredSize();
        int w = sv.width + 720, h = Math.max(sv.height, 560);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        view.setSize(sv);
        view.paint(g);
        g.translate(sv.width, 0);
        inspector.setSize(720, h);
        inspector.paint(g);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", out.toFile());
        System.out.println("wrote " + out);
    }

    private static void open() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // default look and feel
        }
        Scene scene = new Scene();
        scene.loadDemo();
        Models models = new Models();
        AudioEngine audio = new AudioEngine(scene);

        JLabel status = new JLabel(" ");
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        status.setForeground(new Color(0xE8E8E8));
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(0x15161A));
        statusBar.add(status);

        Inspector[] inspector = new Inspector[1];
        SceneView sceneView = new SceneView(scene, models, d -> inspector[0].show(d), status::setText);
        inspector[0] = new Inspector(scene, audio, sceneView::repaint);

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        JToggleButton select = new JToggleButton("Select", true);
        JToggleButton cable = new JToggleButton("Cable");
        select.addActionListener(e -> {
            select.setSelected(true);
            cable.setSelected(false);
            sceneView.setMode(SceneView.Mode.SELECT);
        });
        cable.addActionListener(e -> {
            cable.setSelected(true);
            select.setSelected(false);
            sceneView.setMode(SceneView.Mode.CABLE);
        });
        JComboBox<Device.Type> place = new JComboBox<>(Device.Type.values());
        JButton placeButton = new JButton("Place");
        placeButton.addActionListener(e -> {
            select.setSelected(false);
            cable.setSelected(false);
            sceneView.setPlaceType((Device.Type) place.getSelectedItem());
        });
        JButton rotate = new JButton("Rotate (R)");
        rotate.addActionListener(e -> sceneView.rotate());
        JButton delete = new JButton("Delete");
        delete.addActionListener(e -> {
            sceneView.deleteSelected();
            sceneView.repaint();
        });
        JToggleButton talk = new JToggleButton("🎤 Talk");
        talk.setToolTipText("Use your computer's microphone as the wireless mic you hold (headphones recommended)");
        talk.addActionListener(e -> {
            if (!audio.setTalking(talk.isSelected())) {
                talk.setSelected(false);
                status.setText(audio.error());
            } else {
                status.setText(talk.isSelected() ? "Talking: wireless mic on channel " + scene.micChannel
                        + ", and any stand mic within 3 blocks of you. Use headphones to avoid feedback." : "Mic off.");
            }
            sceneView.repaint();
        });
        JSpinner channel = new JSpinner(new SpinnerNumberModel(1, 1, 8, 1));
        channel.addChangeListener(e -> scene.micChannel = (Integer) channel.getValue());
        JButton media = new JButton("Media folder…");
        media.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(scene.mediaFolder.toFile());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(bar) == JFileChooser.APPROVE_OPTION) {
                scene.mediaFolder = chooser.getSelectedFile().toPath();
                status.setText("Media folder: " + scene.mediaFolder + " (" + audio.tracks().size() + " tracks)");
            }
        });
        JButton demo = new JButton("Reset demo");
        demo.addActionListener(e -> {
            scene.loadDemo();
            inspector[0].show(null);
            sceneView.repaint();
        });

        bar.add(select);
        bar.add(cable);
        bar.addSeparator();
        bar.add(place);
        bar.add(placeButton);
        bar.add(rotate);
        bar.add(delete);
        bar.addSeparator();
        bar.add(talk);
        bar.add(new JLabel(" Mic channel "));
        bar.add(channel);
        bar.addSeparator();
        bar.add(media);
        bar.add(demo);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(sceneView), inspector[0]);
        split.setResizeWeight(0.6);

        JFrame frame = new JFrame("VenueSound Workbench");
        try (InputStream icon = Workbench.class.getResourceAsStream("/assets/venuesound/icon.png")) {
            if (icon != null) {
                frame.setIconImage(ImageIO.read(icon));
            }
        } catch (Exception ignored) {
            // no icon
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(bar, BorderLayout.NORTH);
        frame.getContentPane().add(split, BorderLayout.CENTER);
        frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
        JComponent root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('r'), "rotate");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "delete");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete");
        root.getActionMap().put("rotate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sceneView.rotate();
            }
        });
        root.getActionMap().put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sceneView.deleteSelected();
            }
        });
        frame.setSize(1600, 900);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        sceneView.setMode(SceneView.Mode.SELECT);
        audio.start();
        new Timer(100, e -> {
            inspector[0].repaint();
            if (!audio.error().isEmpty() && !status.getText().equals(audio.error()) && !scene.talking) {
                status.setText(audio.error());
            }
        }).start();
    }
}
