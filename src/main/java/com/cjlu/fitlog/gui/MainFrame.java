package com.cjlu.fitlog.gui;

import com.cjlu.fitlog.domain.WorkoutSession;
import com.cjlu.fitlog.service.WorkoutService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main window: summary bar + session history table + action buttons.
 * All long-running calls go through SwingWorker so the EDT never blocks.
 */
public class MainFrame extends JFrame {

    private final WorkoutService service;
    private final JLabel summaryLabel = new JLabel("Loading...");
    private final JTable sessionTable = new JTable();
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"Date", "Sets", "Tonnage (kg)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public MainFrame(WorkoutService service) {
        super("FitLog - Project 2");
        this.service = service;
        buildLayout();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        top.add(summaryLabel, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        sessionTable.setModel(tableModel);
        sessionTable.setRowHeight(24);
        add(new JScrollPane(sessionTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton btnAddSession = new JButton("Record workout");
        JButton btnAddExercise = new JButton("Add exercise");
        JButton btnAddWeight = new JButton("Log bodyweight");
        JButton btnPrBoard = new JButton("PR board");
        JButton btnWeekly = new JButton("Weekly summary");
        bottom.add(btnAddSession);
        bottom.add(btnAddExercise);
        bottom.add(btnAddWeight);
        bottom.add(btnPrBoard);
        bottom.add(btnWeekly);
        add(bottom, BorderLayout.SOUTH);

        btnAddExercise.addActionListener(e -> new AddExerciseDialog(this, service, this::refreshAll).setVisible(true));
        btnAddSession.addActionListener(e -> new AddSessionDialog(this, service, this::refreshAll).setVisible(true));
        btnAddWeight.addActionListener(e -> new BodyweightDialog(this, service, this::refreshAll).setVisible(true));
        btnPrBoard.addActionListener(e -> new PrBoardDialog(this, service).setVisible(true));
        btnWeekly.addActionListener(e -> new WeeklyDialog(this, service).setVisible(true));
    }

    public void refreshAll() {
        new SwingWorker<List<WorkoutSession>, Void>() {
            @Override protected List<WorkoutSession> doInBackground() {
                return service.listSessions();
            }
            @Override protected void done() {
                try {
                    List<WorkoutSession> sessions = get();
                    tableModel.setRowCount(0);
                    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
                    double totalTonnage = 0;
                    for (int i = sessions.size() - 1; i >= 0; i--) {
                        WorkoutSession s = sessions.get(i);
                        double t = s.totalTonnage();
                        totalTonnage += t;
                        tableModel.addRow(new Object[]{s.getDate().format(fmt), s.getSets().size(), String.format("%,.0f", t)});
                    }
                    summaryLabel.setText(String.format("  Sessions: %d   Total tonnage: %,.0f kg", sessions.size(), totalTonnage));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
