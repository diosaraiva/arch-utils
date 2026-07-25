package com.diosaraiva.plantumlgui.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.diosaraiva.plantumlgui.service.PlantUmlRenderer.CompileResult;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;

public class PlantUmlOutputPanel extends JPanel {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int PREVIEW_TAB = 0;
    private static final int CONSOLE_TAB = 1;

    private static final String PNG_CARD = "png";
    private static final String PUML_CARD = "puml";
    private static final String MSG_CARD = "msg";

    private static final double ZOOM_STEP = 0.1;
    private static final double ZOOM_MIN = 0.1;
    private static final double ZOOM_MAX = 5.0;

    private final JTabbedPane tabs = new JTabbedPane();

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final ImagePanel imagePanel = new ImagePanel();
    private final JScrollPane imageScroll = new JScrollPane(imagePanel);
    private final JTextArea pumlArea = new JTextArea();
    private final JLabel msgLabel = new JLabel();
    private final JLabel zoomLabel = new JLabel("100%");

    private final JButton zoomInBtn = SwingUtils.createToolButton("+", I18n.get("preview.zoom.in"));
    private final JButton zoomOutBtn = SwingUtils.createToolButton("\u2212", I18n.get("preview.zoom.out"));
    private final JButton fitBtn = SwingUtils.createToolButton("Fit", I18n.get("preview.zoom.fit"));
    private final JButton resetBtn = SwingUtils.createToolButton("1:1", I18n.get("preview.zoom.reset"));

    private final JTextArea consoleArea = new JTextArea();
    private final JButton refreshButton;
    private final JButton cleanButton;

    public PlantUmlOutputPanel(Runnable onConsoleRefresh) {
        super(new BorderLayout());
        refreshButton = SwingUtils.createToolButton(
                I18n.get("console.refresh"), I18n.get("console.refresh.tooltip"));
        cleanButton = SwingUtils.createToolButton(
                I18n.get("console.clean"), I18n.get("console.clean.tooltip"));
        refreshButton.addActionListener(e -> onConsoleRefresh.run());
        cleanButton.addActionListener(e -> clearConsole());

        tabs.addTab(I18n.get("tab.preview"), buildPreviewTab());
        tabs.addTab(I18n.get("tab.console"), buildConsoleTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildPreviewTab() {
        var zoomBar = SwingUtils.createToolBar();
        zoomLabel.setFont(zoomLabel.getFont().deriveFont(Font.PLAIN, 11f));
        zoomBar.add(zoomOutBtn);
        zoomBar.add(zoomLabel);
        zoomBar.add(zoomInBtn);
        zoomBar.add(fitBtn);
        zoomBar.add(resetBtn);

        zoomInBtn.addActionListener(e -> zoom(ZOOM_STEP));
        zoomOutBtn.addActionListener(e -> zoom(-ZOOM_STEP));
        resetBtn.addActionListener(e -> setZoom(1.0));
        fitBtn.addActionListener(e -> fitToWindow());

        imageScroll.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                e.consume();
                double delta = e.getWheelRotation() < 0 ? ZOOM_STEP : -ZOOM_STEP;
                zoom(delta);
            }
        });
        imageScroll.getVerticalScrollBar().setUnitIncrement(16);
        imageScroll.getHorizontalScrollBar().setUnitIncrement(16);
        cardPanel.add(imageScroll, PNG_CARD);

        pumlArea.setEditable(false);
        pumlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        cardPanel.add(new JScrollPane(pumlArea), PUML_CARD);

        msgLabel.setHorizontalAlignment(JLabel.CENTER);
        cardPanel.add(msgLabel, MSG_CARD);

        var panel = new JPanel(new BorderLayout());
        panel.add(cardPanel, BorderLayout.CENTER);
        panel.add(zoomBar, BorderLayout.SOUTH);
        showMessage(I18n.get("preview.none"));
        return panel;
    }

    private JPanel buildConsoleTab() {
        consoleArea.setEditable(false);
        consoleArea.setLineWrap(true);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        var toolBar = SwingUtils.createToolBar();
        toolBar.add(refreshButton);
        toolBar.add(cleanButton);

        var panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);
        panel.add(toolBar, BorderLayout.SOUTH);
        return panel;
    }

    public void showRendering() {
        showMessage(I18n.get("plantuml.preview.rendering"));
        setRefreshEnabled(false);
    }

    public void showMessage(String text) {
        msgLabel.setText(text);
        cards.show(cardPanel, MSG_CARD);
    }

    public void showCompileResult(CompileResult result) {
        var image = result.previewImage();
        try {
            if (image != null && image.isFile()) {
                showDiagram(image);
            } else {
                showMessage(I18n.get("plantuml.preview.noimage"));
            }
        } catch (IOException ex) {
            showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
        }
        var header = result.isSuccess()
                ? I18n.get("console.compile.ok", result.exitCode())
                : I18n.get("console.compile.fail", result.exitCode());
        var body = result.output().isBlank() ? I18n.get("console.no.output") : result.output();
        appendConsole(header, body);
        setRefreshEnabled(true);
    }

    public void showRenderError(Throwable ex) {
        showMessage(I18n.get("plantuml.preview.error", ex.getMessage()));
        appendConsole(I18n.get("console.compile.error"), String.valueOf(ex.getMessage()));
        setRefreshEnabled(true);
    }

    public void showDiagram(File file) throws IOException {
        showDiagram(file, null);
    }

    public void showDiagram(File output, File preview) throws IOException {
        String name = output.getName().toLowerCase();
        if (name.endsWith(".svg")) {
            if (preview != null && preview.isFile()) {
                showPng(preview);
            } else {
                showMessage("SVG created: " + output.getAbsolutePath());
            }
        } else if (name.endsWith(".png")) {
            showPng(output);
        } else if (name.endsWith(".puml")) {
            showPuml(output);
        } else {
            showMessage("Unsupported format: " + name);
        }
    }

    public BufferedImage getCurrentImage() {
        return imagePanel.getImage();
    }

    private void showPng(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            showMessage("Could not load image: " + file.getName());
            return;
        }
        imagePanel.setImage(img);
        cards.show(cardPanel, PNG_CARD);
        SwingUtilities.invokeLater(() -> {
            fitToWindow();
            imageScroll.getVerticalScrollBar().setValue(0);
            imageScroll.getHorizontalScrollBar().setValue(0);
        });
    }

    private void showPuml(File file) throws IOException {
        String puml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        pumlArea.setText(puml);
        pumlArea.setCaretPosition(0);
        cards.show(cardPanel, PUML_CARD);
    }

    private void zoom(double delta) {
        setZoom(imagePanel.getScale() + delta);
    }

    private void setZoom(double scale) {
        scale = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, scale));
        imagePanel.setScale(scale);
        zoomLabel.setText(Math.round(scale * 100) + "%");
        imagePanel.revalidate();
        imageScroll.repaint();
    }

    private void fitToWindow() {
        BufferedImage img = imagePanel.getImage();
        if (img == null) return;
        int vpW = imageScroll.getViewport().getWidth();
        int vpH = imageScroll.getViewport().getHeight();
        if (vpW <= 0 || vpH <= 0) return;
        double scaleX = (double) vpW / img.getWidth();
        double scaleY = (double) vpH / img.getHeight();
        setZoom(Math.min(scaleX, scaleY));
    }

    public void selectConsole() {
        tabs.setSelectedIndex(CONSOLE_TAB);
    }

    public void appendConsole(String header, String body) {
        SwingUtilities.invokeLater(() -> {
            var sb = new StringBuilder("[").append(LocalTime.now().format(TS)).append("] ")
                    .append(header).append(System.lineSeparator());
            if (body != null && !body.isBlank()) {
                sb.append(body.strip()).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            consoleArea.append(sb.toString());
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    private void setRefreshEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> refreshButton.setEnabled(enabled));
    }

    private void clearConsole() {
        SwingUtilities.invokeLater(() -> consoleArea.setText(""));
    }

    public void applyLanguage() {
        tabs.setTitleAt(PREVIEW_TAB, I18n.get("tab.preview"));
        tabs.setTitleAt(CONSOLE_TAB, I18n.get("tab.console"));
        zoomInBtn.setToolTipText(I18n.get("preview.zoom.in"));
        zoomOutBtn.setToolTipText(I18n.get("preview.zoom.out"));
        fitBtn.setToolTipText(I18n.get("preview.zoom.fit"));
        resetBtn.setToolTipText(I18n.get("preview.zoom.reset"));
        refreshButton.setText(I18n.get("console.refresh"));
        refreshButton.setToolTipText(I18n.get("console.refresh.tooltip"));
        cleanButton.setText(I18n.get("console.clean"));
        cleanButton.setToolTipText(I18n.get("console.clean.tooltip"));
        repaint();
    }

    private static class ImagePanel extends JPanel {

        private BufferedImage image;
        private double scale = 1.0;

        ImagePanel() {
            setBackground(Color.WHITE);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            this.scale = 1.0;
            revalidate();
            repaint();
        }

        BufferedImage getImage() { return image; }

        double getScale() { return scale; }

        void setScale(double scale) {
            this.scale = scale;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (image == null) {
                return new Dimension(100, 100);
            }
            int w = (int) Math.ceil(image.getWidth() * scale);
            int h = (int) Math.ceil(image.getHeight() * scale);
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            int w = (int) Math.ceil(image.getWidth() * scale);
            int h = (int) Math.ceil(image.getHeight() * scale);

            int x = Math.max(0, (getWidth() - w) / 2);
            int y = Math.max(0, (getHeight() - h) / 2);

            g2.drawImage(image, x, y, w, h, null);
            g2.dispose();
        }
    }
}
