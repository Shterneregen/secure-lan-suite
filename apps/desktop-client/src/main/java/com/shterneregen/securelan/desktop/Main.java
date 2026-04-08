package com.shterneregen.securelan.desktop;

import com.shterneregen.securelan.desktop.ui.ChatWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatWindow().setVisible(true));
    }
}
