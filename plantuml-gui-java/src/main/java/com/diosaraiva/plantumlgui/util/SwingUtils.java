package com.diosaraiva.plantumlgui.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;

// Swing boilerplate shared by every panel: widget factories, clipboard, dialogs, look and feel.
public final class SwingUtils {

    private static final Logger LOG = Logger.getLogger(SwingUtils.class.getName());

    private SwingUtils() { }

    public static int menuShortcut() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    // Adapts the three DocumentListener callbacks to a single Runnable.
    public static DocumentListener onDocumentChange(Runnable onChange) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        };
    }

    public static JMenuItem menuItem(String text, int mnemonic,
                                     KeyStroke accelerator, ActionListener action) {
        var item = new JMenuItem(text);
        if (mnemonic != 0) { item.setMnemonic(mnemonic); }
        if (accelerator != null) { item.setAccelerator(accelerator); }
        if (action != null) { item.addActionListener(action); }
        return item;
    }

    public static JPanel createToolBar() {
        return new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
    }

    public static JButton createToolButton(String text, String tooltip) {
        var button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    public static void copyText(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    public static void copyImage(Image image) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(image), null);
    }

    public static void browse(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            LOG.warning(() -> "Failed to open URL " + url + ": " + ex.getMessage());
        }
    }

    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static void useUiFont(JComponent component) {
        component.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        component.setFont(UIManager.getFont("Label.font"));
    }

    // Applies a look and feel by class name; blank falls back to the cross-platform one.
    public static void applyLookAndFeel(String className)
            throws ReflectiveOperationException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(className == null || className.isBlank()
                ? UIManager.getCrossPlatformLookAndFeelClassName() : className);
        refreshAllWindows();
    }

    public static void applyFontFamily(String family) {
        if (family == null || family.isBlank()) { return; }
        for (Object key : new ArrayList<>(UIManager.getDefaults().keySet())) {
            if (UIManager.get(key) instanceof Font font) {
                UIManager.put(key, new FontUIResource(family, font.getStyle(), font.getSize()));
            }
        }
        refreshAllWindows();
    }

    public static void refreshAllWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.validate();
        }
    }

    private record ImageTransferable(Image image) implements Transferable {

        @Override public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}