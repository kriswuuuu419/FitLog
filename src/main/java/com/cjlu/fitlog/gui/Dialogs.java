package com.cjlu.fitlog.gui;

import com.cjlu.fitlog.domain.*;
import com.cjlu.fitlog.service.WorkoutService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * All dialogs - styled to match the green theme.
 */
class AddExerciseDialog extends JDialog {
    private static final Color PRIMARY = new Color(22, 163, 74);
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    AddExerciseDialog(Frame parent, WorkoutService service, Runnable onDone) {
        super(parent, "Add Exercise", true);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 253, 244));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.setOpaque(false);
        JTextField name = new JTextField(12);
        JComboBox<MuscleGroup> group = new JComboBox<>(MuscleGroup.values());
        JComboBox<WorkoutType> type = new JComboBox<>(WorkoutType.values());
        styleLabel(form, "Name:"); form.add(name);
        styleLabel(form, "Muscle group:"); form.add(group);
        styleLabel(form, "Type:"); form.add(type);
        add(form, BorderLayout.CENTER);

        JButton save = makeButton("Save", true);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setOpaque(false);
        btnPanel.add(save);
        add(btnPanel, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    service.addExercise(name.getText().trim(),
                        (MuscleGroup) group.getSelectedItem(),
                        (WorkoutType) type.getSelectedItem());
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        dispose();
                        onDone.run();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(null, cause.getMessage(),
                                "Invalid input", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        pack(); setLocationRelativeTo(parent);
    }

    private void styleLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT);
        p.add(l);
    }

    private JButton makeButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setPreferredSize(new Dimension(100, 36));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        if (primary) { b.setBackground(PRIMARY); b.setForeground(Color.WHITE); }
        else { b.setBackground(Color.WHITE); b.setForeground(Color.BLACK); }
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}

class BodyweightDialog extends JDialog {
    BodyweightDialog(Frame parent, WorkoutService service, Runnable onDone) {
        super(parent, "Log Bodyweight", true);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 253, 244));

        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.setOpaque(false);
        JTextField kg = new JTextField(8);
        JTextField date = new JTextField(LocalDate.now().toString());
        form.add(new JLabel("kg:")); form.add(kg);
        form.add(new JLabel("date (YYYY-MM-DD):")); form.add(date);
        add(form, BorderLayout.CENTER);

        JButton save = new JButton("Save");
        save.setFont(new Font("Segoe UI", Font.BOLD, 14));
        save.setBackground(new Color(22, 163, 74));
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);
        save.setPreferredSize(new Dimension(100, 36));
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bp.setOpaque(false);
        bp.add(save);
        add(bp, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            try {
                double k = Double.parseDouble(kg.getText().trim());
                LocalDate d = LocalDate.parse(date.getText().trim());
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() { service.recordBodyweight(d, k); return null; }
                    @Override protected void done() {
                    try {
                        get();
                        dispose();
                        onDone.run();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(null, cause.getMessage(),
                                "Invalid input", JOptionPane.ERROR_MESSAGE);
                    }
                }
                }.execute();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        });
        pack(); setLocationRelativeTo(parent);
    }
}

class AddSessionDialog extends JDialog {
    AddSessionDialog(Frame parent, WorkoutService service, Runnable onDone) {
        super(parent, "Record Workout", true);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 253, 244));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        form.setOpaque(false);
        JTextField date = new JTextField(LocalDate.now().toString());
        DefaultComboBoxModel<Exercise> model = new DefaultComboBoxModel<>();
        JComboBox<Exercise> exercise = new JComboBox<>(model);
        new SwingWorker<List<Exercise>, Void>() {
            @Override protected List<Exercise> doInBackground() { return service.listExercises(); }
            @Override protected void done() {
                try {
                    for (Exercise ex : get()) model.addElement(ex);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Cannot load exercises: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
        JTextField weight = new JTextField();
        JTextField reps = new JTextField();
        form.add(new JLabel("date:")); form.add(date);
        form.add(new JLabel("exercise:")); form.add(exercise);
        form.add(new JLabel("weight (kg):")); form.add(weight);
        form.add(new JLabel("reps:")); form.add(reps);
        add(form, BorderLayout.CENTER);

        JButton save = new JButton("Save session");
        save.setFont(new Font("Segoe UI", Font.BOLD, 14));
        save.setBackground(new Color(22, 163, 74));
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);
        save.setPreferredSize(new Dimension(120, 36));
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bp.setOpaque(false);
        bp.add(save);
        add(bp, BorderLayout.SOUTH);

        save.addActionListener(e -> {
            try {
                LocalDate d = LocalDate.parse(date.getText().trim());
                Exercise ex = (Exercise) exercise.getSelectedItem();
                double w = Double.parseDouble(weight.getText().trim());
                int r = Integer.parseInt(reps.getText().trim());
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() {
                        WorkoutSession s = service.startSession(d, "");
                        service.recordSet(s, ex, w, r);
                        service.finishSession(s);
                        return null;
                    }
                    @Override protected void done() {
                    try {
                        get();
                        dispose();
                        onDone.run();
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(null, cause.getMessage(),
                                "Invalid input", JOptionPane.ERROR_MESSAGE);
                    }
                }
                }.execute();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        });
        setSize(420, 240); setLocationRelativeTo(parent);
    }
}

class PrBoardDialog extends JDialog {
    PrBoardDialog(Frame parent, WorkoutService service) {
        super(parent, "PR Board - Estimated 1RM (Epley)", true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        DefaultTableModel m = new DefaultTableModel(new Object[]{"Exercise", "Estimated 1RM (kg)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        t.setRowHeight(32);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setGridColor(new Color(220, 235, 225));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(220, 252, 231));

        new SwingWorker<Object[][], Void>() {
            @Override protected Object[][] doInBackground() {
                List<Exercise> exs = service.listExercises();
                if (exs.isEmpty()) return new Object[][]{{"No exercises yet", ""}};
                Object[][] rows = new Object[exs.size()][2];
                for (int i = 0; i < exs.size(); i++) {
                    rows[i] = new Object[]{exs.get(i).getName(),
                            String.format("%.1f", service.bestE1RM(exs.get(i)))};
                }
                return rows;
            }
            @Override protected void done() {
                try {
                    for (Object[] row : get()) m.addRow(row);
                } catch (Exception ex) {
                    m.addRow(new Object[]{"Load failed", ex.getMessage()});
                }
            }
        }.execute();
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(sp, BorderLayout.CENTER);

        // The PR Board is a read-only view. Deleting an exercise belongs to the
        // Exercise Library tab, so this dialog only offers Close - removing a risky
        // red delete action from a statistics screen.
        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setBackground(new Color(22, 163, 74));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(110, 34));
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bp.add(closeBtn);
        add(bp, BorderLayout.SOUTH);

        closeBtn.addActionListener(e -> dispose());

        setSize(420, 380); setLocationRelativeTo(parent);
    }
}

class WeeklyDialog extends JDialog {
    WeeklyDialog(Frame parent, WorkoutService service) {
        super(parent, "Weekly Volume by Muscle Group", true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        DefaultTableModel m = new DefaultTableModel(new Object[]{"Muscle Group", "Volume (kg)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        t.setRowHeight(32);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setGridColor(new Color(220, 235, 225));
        t.setShowVerticalLines(false);

        new SwingWorker<Object[][], Void>() {
            @Override protected Object[][] doInBackground() {
                Map<MuscleGroup, Double> weekly = service.weeklyTonnageByGroup(LocalDate.now());
                java.util.List<Object[]> rows = new java.util.ArrayList<>();
                double total = 0;
                for (Map.Entry<MuscleGroup, Double> en : weekly.entrySet()) {
                    if (en.getKey() == null) { total = en.getValue(); continue; }
                    rows.add(new Object[]{en.getKey().toString(), String.format("%,.0f", en.getValue())});
                }
                rows.add(new Object[]{"TOTAL", String.format("%,.0f", total)});
                return rows.toArray(new Object[0][]);
            }
            @Override protected void done() {
                try {
                    for (Object[] r : get()) m.addRow(r);
                } catch (Exception ex) {
                    m.addRow(new Object[]{"Load failed", ex.getMessage()});
                }
            }
        }.execute();

        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(sp, BorderLayout.CENTER);
        setSize(400, 350); setLocationRelativeTo(parent);
    }
}

class BodyweightHistoryDialog extends JDialog {
    BodyweightHistoryDialog(Frame parent, WorkoutService service) {
        super(parent, "Bodyweight History", true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        DefaultTableModel m = new DefaultTableModel(new Object[]{"Date", "Weight (kg)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        t.setRowHeight(32);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setGridColor(new Color(220, 235, 225));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(220, 252, 231));

        new SwingWorker<Object[][], Void>() {
            @Override protected Object[][] doInBackground() {
                List<BodyweightEntry> entries = service.findAllBodyweights();
                entries.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                if (entries.isEmpty()) return new Object[][]{{"No records yet", ""}};
                Object[][] rows = new Object[entries.size()][2];
                for (int i = 0; i < entries.size(); i++) {
                    rows[i] = new Object[]{entries.get(i).getDate().toString(),
                            String.format("%.1f", entries.get(i).getKg())};
                }
                return rows;
            }
            @Override protected void done() {
                try {
                    for (Object[] r : get()) m.addRow(r);
                } catch (Exception ex) {
                    m.addRow(new Object[]{"Load failed", ex.getMessage()});
                }
            }
        }.execute();
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(sp, BorderLayout.CENTER);

        JButton delBtn = new JButton("Delete Selected");
        delBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        delBtn.setBackground(new Color(220, 38, 38));
        delBtn.setForeground(Color.WHITE);
        delBtn.setFocusPainted(false);
        delBtn.setPreferredSize(new Dimension(130, 34));
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bp.add(delBtn);
        add(bp, BorderLayout.SOUTH);

        delBtn.addActionListener(e -> {
            int row = t.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row first"); return; }
            LocalDate d = LocalDate.parse(t.getValueAt(row, 0).toString());
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete bodyweight record on " + d + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    service.deleteBodyweight(d);
                    m.removeRow(row);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                        "Cannot delete this record: " + ex.getMessage(),
                        "Delete failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        setSize(400, 380); setLocationRelativeTo(parent);
    }
}
