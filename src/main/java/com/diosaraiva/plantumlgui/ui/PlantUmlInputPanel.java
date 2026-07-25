package com.diosaraiva.plantumlgui.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import com.diosaraiva.plantumlgui.service.SampleLoader;
import com.diosaraiva.plantumlgui.util.I18n;
import com.diosaraiva.plantumlgui.util.SwingUtils;
import com.diosaraiva.plantumlgui.util.TextLineNumber;

public class PlantUmlInputPanel extends JPanel {

    private final JList<DiagramSample> sampleList = new JList<>(DiagramSample.values());
    private final JTextArea codeTextArea;
    private final JLabel countLabel = new JLabel();
    private final JCheckBox autoPreviewCheck = new JCheckBox(I18n.get("input.autoPreview"), true);
    private final JButton previewButton = new JButton(I18n.get("input.preview"));
    private final UndoManager undoManager = new UndoManager();
    private final List<Runnable> undoStateListeners = new ArrayList<>();

    private final JTabbedPane inputTabs = new JTabbedPane();
    private JScrollPane editorScroll;
    private JPanel controlsBar;

    public PlantUmlInputPanel() {
        codeTextArea = new JTextArea(10, 20);
        codeTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        initComponents();
    }

    private void initComponents() {
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(false);
        editorScroll = new JScrollPane(codeTextArea);
        editorScroll.setRowHeaderView(new TextLineNumber(codeTextArea));

        sampleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sampleList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { loadSample(); }
        });

        inputTabs.addTab(I18n.get("input.tab.code"), editorScroll);
        inputTabs.addTab(I18n.get("input.tab.samples"), new JScrollPane(sampleList));

        controlsBar = createBottomBar();

        initUndo();
        initCountLabel();

        sampleList.setSelectedValue(DiagramSample.SEQUENCE, true);
        inputTabs.setSelectedIndex(0);
    }

    public JComponent getEditorComponent() { return inputTabs; }

    public JComponent getControlsComponent() { return controlsBar; }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));

        autoPreviewCheck.setFont(autoPreviewCheck.getFont().deriveFont(Font.PLAIN, 11f));
        autoPreviewCheck.addActionListener(e -> updatePreviewButtonState());
        bar.add(autoPreviewCheck, BorderLayout.WEST);

        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 10f));
        countLabel.setHorizontalAlignment(JLabel.RIGHT);
        bar.add(countLabel, BorderLayout.CENTER);

        previewButton.setFont(previewButton.getFont().deriveFont(Font.PLAIN, 11f));
        bar.add(previewButton, BorderLayout.EAST);

        updatePreviewButtonState();
        return bar;
    }

    private void updatePreviewButtonState() {
        previewButton.setEnabled(!autoPreviewCheck.isSelected());
    }

    private void initCountLabel() {
        codeTextArea.getDocument().addDocumentListener(SwingUtils.onDocumentChange(this::updateCounts));
        updateCounts();
    }

    private void updateCounts() {
        int chars = codeTextArea.getDocument().getLength();
        int lines = codeTextArea.getLineCount();
        countLabel.setText(I18n.get("input.counts", chars, lines));
    }

    public void applyLanguage() {
        inputTabs.setTitleAt(0, I18n.get("input.tab.code"));
        inputTabs.setTitleAt(1, I18n.get("input.tab.samples"));
        autoPreviewCheck.setText(I18n.get("input.autoPreview"));
        previewButton.setText(I18n.get("input.preview"));
        updateCounts();
        repaint();
    }

    private void initUndo() {
        codeTextArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            fireUndoStateChanged();
        });

        int mod = SwingUtils.menuShortcut();
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod), "archutils-undo", this::undo);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod), "archutils-redo", this::redo);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod | InputEvent.SHIFT_DOWN_MASK),
                "archutils-redo", this::redo);
    }

    private void bindKey(KeyStroke stroke, String actionKey, Runnable action) {
        codeTextArea.getInputMap().put(stroke, actionKey);
        codeTextArea.getActionMap().put(actionKey, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void loadSample() {
        DiagramSample sample = sampleList.getSelectedValue();
        if (sample == null) { return; }
        try {
            codeTextArea.setText(SampleLoader.load(sample.getFileName()));
            codeTextArea.setCaretPosition(0);
        } catch (Exception ex) {
            codeTextArea.setText("Error loading sample: " + ex.getMessage());
        }

        inputTabs.setSelectedIndex(0);
        undoManager.discardAllEdits();
        fireUndoStateChanged();
    }

    public String getCode() { return codeTextArea.getText().trim(); }

    public void setCode(String code) {
        codeTextArea.setText(code);
        codeTextArea.setCaretPosition(0);
        undoManager.discardAllEdits();
        fireUndoStateChanged();
    }

    public void addCodeDocumentListener(DocumentListener listener) {
        codeTextArea.getDocument().addDocumentListener(listener);
    }

    public boolean isAutoPreviewEnabled() {
        return autoPreviewCheck.isSelected();
    }

    public void addPreviewButtonListener(ActionListener listener) {
        previewButton.addActionListener(listener);
    }

    public void addAutoPreviewListener(ActionListener listener) {
        autoPreviewCheck.addActionListener(listener);
    }

    public boolean canUndo() { return undoManager.canUndo(); }

    public boolean canRedo() { return undoManager.canRedo(); }

    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
        fireUndoStateChanged();
    }

    public void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
        fireUndoStateChanged();
    }

    public void copyToClipboard() {
        String selected = codeTextArea.getSelectedText();
        SwingUtils.copyText((selected != null && !selected.isEmpty())
                ? selected : codeTextArea.getText());
    }

    public void paste() {
        codeTextArea.paste();
    }

    public void selectAll() {
        inputTabs.setSelectedIndex(0);
        codeTextArea.requestFocusInWindow();
        codeTextArea.selectAll();
    }

    public void addUndoStateListener(Runnable listener) {
        undoStateListeners.add(listener);
    }

    private void fireUndoStateChanged() {
        for (Runnable r : undoStateListeners) {
            r.run();
        }
    }

    public enum DiagramSample {
        ACTIVITY("Activity", "activity.puml"),
        ARCHIMATE_APPLICATION("Archimate Application", "archimate_application.puml"),
        ARCHIMATE_BUSINESS("Archimate Business", "archimate_business.puml"),
        ARCHIMATE_IMPLEMENTATION("Archimate Implementation", "archimate_implementation.puml"),
        ARCHIMATE_LAYERED("Archimate Layered", "archimate_layered.puml"),
        ARCHIMATE_MOTIVATION("Archimate Motivation", "archimate_motivation.puml"),
        ARCHIMATE_PHYSICAL("Archimate Physical", "archimate_physical.puml"),
        ARCHIMATE_STRATEGY("Archimate Strategy", "archimate_strategy.puml"),
        ARCHIMATE_TECHNOLOGY("Archimate Technology", "archimate_technology.puml"),
        C4_COMPONENT("C4 Component", "c4_component.puml"),
        C4_CONTAINER("C4 Container", "c4_container.puml"),
        C4_CONTEXT("C4 Context", "c4_context.puml"),
        C4_DEPLOYMENT("C4 Deployment", "c4_deployment.puml"),
        CLASS("Class", "class.puml"),
        COMPONENT("Component", "component.puml"),
        DEPLOYMENT("Deployment", "deployment.puml"),
        DITAA("Ditaa", "ditaa.puml"),
        FILES("Files", "files.puml"),
        GANTT("Gantt", "gantt.puml"),
        JSON("JSON", "json.puml"),
        MINDMAP("Mind Map", "mindmap.puml"),
        OBJECT("Object", "object.puml"),
        SEQUENCE("Sequence", "sequence.puml"),
        STATE("State", "state.puml"),
        TIMING("Timing", "timing.puml"),
        USE_CASE("Use Case", "usecase.puml"),
        WBS("Work Breakdown Structure", "wbs.puml"),
        YAML("YAML", "yaml.puml"),
        CUSTOM_ARCHIMATE("Custom Archimate", "custom_archimate.puml"),
        CUSTOM_MODULAR("Custom Modular", "custom_modular.puml"),
        UTILS_SKINPARAMS("List Available [skinparams]", "util_skinparams.puml"),
        UTILS_SPRITES("List Available [sprites]", "util_sprites.puml"),
        UTILS_COLORS("List Available [colors]", "util_colors.puml"),
        UTILS_OPENICONIC("List Available [icons]", "util_openiconic.puml"),
        UTILS_EMOJI("List Available [emoji]", "util_emoji.puml");

        private final String displayName;
        private final String fileName;

        DiagramSample(String displayName, String fileName) {
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getFileName() { return fileName; }

        @Override public String toString() { return displayName; }
    }
}
