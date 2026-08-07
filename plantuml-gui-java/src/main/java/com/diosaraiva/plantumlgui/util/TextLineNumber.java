package com.diosaraiva.plantumlgui.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

// Row header that paints line numbers next to a JTextComponent; install via JScrollPane.setRowHeaderView.
@SuppressWarnings("serial")
public final class TextLineNumber extends JComponent implements CaretListener, DocumentListener {

    private static final int MARGIN = 6;
    private static final Color TEXT_COLOR = new Color(120, 120, 120);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);

    private final JTextComponent editor;
    private int lastDigits;

    public TextLineNumber(JTextComponent editor) {
        this.editor = editor;
        setFont(editor.getFont());
        setForeground(TEXT_COLOR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        editor.getDocument().addDocumentListener(this);
        editor.addCaretListener(this);
        editor.addPropertyChangeListener("font", e -> {
            setFont(editor.getFont());
            documentChanged();
        });

        updatePreferredWidth();
    }

    // Width only changes when the digit count does, so recompute lazily.
    private void updatePreferredWidth() {
        int digits = Math.max(String.valueOf(root().getElementCount()).length(), 2);
        if (digits == lastDigits) {
            return;
        }
        lastDigits = digits;
        int width = getFontMetrics(getFont()).charWidth('0') * digits + MARGIN * 2;
        Dimension size = new Dimension(width, Integer.MAX_VALUE - 1_000_000);
        setPreferredSize(size);
        setSize(size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        FontMetrics fm = editor.getFontMetrics(editor.getFont());
        g.setFont(editor.getFont());
        g.setColor(getForeground());

        Rectangle clip = g.getClipBounds();
        int rightEdge = getSize().width - getInsets().right - MARGIN;

        Element root = root();
        int startLine = root.getElementIndex(editor.viewToModel2D(new Point(0, clip.y)));
        int endLine = root.getElementIndex(editor.viewToModel2D(new Point(0, clip.y + clip.height)));

        for (int line = startLine; line <= endLine; line++) {
            try {
                Rectangle r = editor.modelToView2D(root.getElement(line).getStartOffset()).getBounds();
                String number = String.valueOf(line + 1);
                g.drawString(number, rightEdge - fm.stringWidth(number), r.y + fm.getAscent());
            } catch (BadLocationException ignored) {
                // Line vanished between layout and paint; nothing to draw.
            }
        }
    }

    private Element root() {
        return editor.getDocument().getDefaultRootElement();
    }

    private void documentChanged() {
        SwingUtilities.invokeLater(() -> {
            updatePreferredWidth();
            revalidate();
            repaint();
        });
    }

    @Override public void caretUpdate(CaretEvent e)     { repaint(); }

    @Override public void insertUpdate(DocumentEvent e) { documentChanged(); }

    @Override public void removeUpdate(DocumentEvent e) { documentChanged(); }

    @Override public void changedUpdate(DocumentEvent e) { documentChanged(); }
}