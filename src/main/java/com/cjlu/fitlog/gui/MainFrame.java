package com.cjlu.fitlog.gui;

import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.MuscleGroup;
import com.cjlu.fitlog.domain.WorkoutSession;
import com.cjlu.fitlog.service.WorkoutService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Modern main window with two tabs: Workouts and Exercise Library.
 */
public class MainFrame extends JFrame {

    private final WorkoutService service;

    // Workout tab components
    private final JLabel summaryLabel = new JLabel("");
    private final JTable sessionTable = new JTable();
    private final DefaultTableModel sessionModel = new DefaultTableModel(
            new Object[]{"Date", "Sets", "Tonnage (kg)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // Exercise library tab components
    private final JTable exerciseTable = new JTable();
    private final DefaultTableModel exerciseModel = new DefaultTableModel(
            new Object[]{"Exercise", "Muscle Group", "Type"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // Palette - green health theme
    private static final Color BG        = new Color(240, 253, 244);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color PRIMARY   = new Color(22, 163, 74);
    private static final Color DARK_GREEN = new Color(20, 83, 45);
    private static final Color TEXT_DARK = new Color(20, 40, 28);
    private static final Color TEXT_MED  = new Color(100, 116, 108);
    private static final Color BORDER    = new Color(220, 235, 225);
    private static final Color SELECT_BG = new Color(220, 252, 231);
    private static final Font FONT       = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);

    public MainFrame(WorkoutService service) {
        super("FitLog");
        this.service = service;
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            // Override Nimbus selection colors to green
            UIManager.put("Table.selectionBackground", new Color(220, 252, 231));
            UIManager.put("Table.selectionForeground", new Color(20, 40, 28));
        }
        catch (Exception ignored) {}
        buildLayout();
    }

    private void buildLayout() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        JLabel title = new JLabel("FitLog");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Workout & Nutrition Journal");
        subtitle.setFont(FONT);
        subtitle.setForeground(new Color(187, 247, 208));
        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(title); titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Tabbed content ──
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FONT_BOLD);
        tabs.setBackground(BG);

        // Tab 1: Workouts
        JPanel workoutTab = new JPanel(new BorderLayout(12, 12));
        workoutTab.setBackground(BG);
        workoutTab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        summaryLabel.setFont(FONT_BOLD);
        summaryLabel.setForeground(TEXT_DARK);
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 0));
        workoutTab.add(summaryLabel, BorderLayout.NORTH);

        styleTable(sessionTable, sessionModel);
        JScrollPane sessionScroll = new JScrollPane(sessionTable);
        sessionScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        sessionScroll.getViewport().setBackground(CARD_BG);
        attachPopup(sessionTable, this::deleteSelectedSession);
        // Click empty viewport area to clear selection
        sessionScroll.getViewport().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                sessionTable.clearSelection();
            }
        });
        workoutTab.add(sessionScroll, BorderLayout.CENTER);

        JPanel workoutBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        workoutBtns.setBackground(BG);
        workoutBtns.add(makeButton("Record Workout", true));
        workoutBtns.add(makeButton("Log Bodyweight", false));
        workoutBtns.add(makeButton("Weight History", false));
        workoutBtns.add(makeButton("Delete Session", false));
        workoutBtns.add(makeButton("PR Board", false));
        workoutBtns.add(makeButton("Weekly Summary", false));
        workoutTab.add(workoutBtns, BorderLayout.SOUTH);

        // Tab 2: Exercise Library
        JPanel libTab = new JPanel(new BorderLayout(12, 12));
        libTab.setBackground(BG);
        libTab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel libTitle = new JLabel("Exercise Library");
        libTitle.setFont(FONT_BOLD);
        libTitle.setForeground(TEXT_DARK);
        libTitle.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 0));
        libTab.add(libTitle, BorderLayout.NORTH);

        styleTable(exerciseTable, exerciseModel);
        JScrollPane libScroll = new JScrollPane(exerciseTable);
        libScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        libScroll.getViewport().setBackground(CARD_BG);
        attachPopup(exerciseTable, this::deleteSelectedExercise);
        libScroll.getViewport().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                exerciseTable.clearSelection();
            }
        });
        libTab.add(libScroll, BorderLayout.CENTER);

        JPanel libBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        libBtns.setBackground(BG);
        libBtns.add(makeButton("Add Exercise", true));
        libBtns.add(makeButton("Delete Exercise", false));
        libTab.add(libBtns, BorderLayout.SOUTH);

        tabs.addTab("Workouts", workoutTab);
        tabs.addTab("Exercise Library", libTab);
        add(tabs, BorderLayout.CENTER);

        // ── Wire buttons ──
        ((JButton) workoutBtns.getComponent(0)).addActionListener(e ->
                new AddSessionDialog(this, service, this::refreshAll).setVisible(true));
        ((JButton) workoutBtns.getComponent(1)).addActionListener(e ->
                new BodyweightDialog(this, service, this::refreshAll).setVisible(true));
        ((JButton) workoutBtns.getComponent(2)).addActionListener(e ->
                new BodyweightHistoryDialog(this, service).setVisible(true));
        ((JButton) workoutBtns.getComponent(3)).addActionListener(e -> deleteSelectedSession());
        ((JButton) workoutBtns.getComponent(4)).addActionListener(e ->
                new PrBoardDialog(this, service).setVisible(true));
        ((JButton) workoutBtns.getComponent(5)).addActionListener(e ->
                new WeeklyDialog(this, service).setVisible(true));
        ((JButton) libBtns.getComponent(0)).addActionListener(e ->
                new AddExerciseDialog(this, service, this::refreshExercises).setVisible(true));
        ((JButton) libBtns.getComponent(1)).addActionListener(e -> deleteSelectedExercise());
    }

    private void styleTable(JTable table, DefaultTableModel model) {
        table.setModel(model);
        table.setRowHeight(36);
        table.setFont(FONT);
        table.setBackground(CARD_BG);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(220, 252, 231));  // light green
        table.setSelectionForeground(TEXT_DARK);
        table.setFocusable(false);

        // Center all cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JTableHeader th = table.getTableHeader();
        th.setFont(FONT_BOLD);
        th.setBackground(new Color(240, 253, 244));
        th.setForeground(TEXT_MED);
        th.setPreferredSize(new Dimension(0, 40));
    }

    private void attachPopup(JTable table, Runnable onDelete) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit");
        JMenuItem deleteItem = new JMenuItem("Delete");
        editItem.setFont(FONT);
        deleteItem.setFont(FONT);
        deleteItem.addActionListener(e -> onDelete.run());
        popup.add(editItem);
        popup.add(deleteItem);
        table.setComponentPopupMenu(popup);

        // Click empty space below rows to clear selection
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (table.rowAtPoint(e.getPoint()) < 0) {
                    table.clearSelection();
                }
            }
        });
    }

    private JButton makeButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setPreferredSize(new Dimension(150, 40));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        if (primary) {
            b.setBackground(PRIMARY);
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(CARD_BG);
            b.setForeground(TEXT_DARK);
            b.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        }
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    public void refreshAll() {
        refreshWorkouts();
        refreshExercises();
    }

    private void deleteSelectedSession() {
        int row = sessionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a session row first.", "Hint", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String dateStr = (String) sessionTable.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete session on " + dateStr + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { service.deleteSession(date); return null; }
            @Override protected void done() { refreshWorkouts(); }
        }.execute();
    }

    private void deleteSelectedExercise() {
        int row = exerciseTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an exercise row first.", "Hint", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = (String) exerciseTable.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete exercise '" + name + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { service.deleteExercise(name); return null; }
            @Override protected void done() { refreshExercises(); }
        }.execute();
    }

    private void refreshWorkouts() {
        new SwingWorker<List<WorkoutSession>, Void>() {
            @Override protected List<WorkoutSession> doInBackground() {
                return service.listSessions();
            }
            @Override protected void done() {
                try {
                    List<WorkoutSession> sessions = get();
                    sessionModel.setRowCount(0);
                    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
                    double totalTonnage = 0;
                    for (int i = sessions.size() - 1; i >= 0; i--) {
                        WorkoutSession s = sessions.get(i);
                        double t = s.totalTonnage();
                        totalTonnage += t;
                        sessionModel.addRow(new Object[]{s.getDate().format(fmt), s.getSets().size(), String.format("%,.0f", t)});
                    }
                    summaryLabel.setText(String.format("  %d sessions  ·  %,.0f kg total volume", sessions.size(), totalTonnage));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void refreshExercises() {
        new SwingWorker<List<Exercise>, Void>() {
            @Override protected List<Exercise> doInBackground() {
                return service.listExercises();
            }
            @Override protected void done() {
                try {
                    List<Exercise> exs = get();
                    exerciseModel.setRowCount(0);
                    for (Exercise ex : exs) {
                        exerciseModel.addRow(new Object[]{
                            ex.getName(),
                            ex.getMuscleGroup(),
                            ex.getType()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
