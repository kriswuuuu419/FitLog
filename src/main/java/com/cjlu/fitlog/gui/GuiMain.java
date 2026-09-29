package com.cjlu.fitlog.gui;

import com.cjlu.fitlog.service.WorkoutService;

import javax.swing.*;
import java.awt.*;

/**
 * Project 2 Swing entry point.
 * Wires WorkoutService to MainFrame. All DB I/O happens off the EDT via SwingWorker.
 */
public class GuiMain {

    private final WorkoutService service;

    public GuiMain(WorkoutService service) {
        this.service = service;
    }

    public void start() {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(service);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(960, 640);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.refreshAll();
        });
    }
}
