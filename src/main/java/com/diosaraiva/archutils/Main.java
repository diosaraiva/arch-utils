package com.diosaraiva.archutils;

import com.diosaraiva.archutils.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Entry point for the Arch Utils application.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
