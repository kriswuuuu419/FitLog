package com.cjlu.fitlog.gui;

import com.cjlu.fitlog.domain.*;
import com.cjlu.fitlog.service.WorkoutService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Map;

/**
 * All modal dialogs used by the Swing GUI. Each mutating action runs the DB call
 * on a background thread and then calls the parent refresh callback.
 */
class AddExerciseDialog extends JDialog {
    AddExerciseDialog(Frame parent, WorkoutService service, Runnable onDone) {
        super(parent, "Add exercise", true);
        setLayout(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField name = new JTextField(12);
        JComboBox<MuscleGroup> group = new JComboBox<>(MuscleGroup.values());
        JComboBox<WorkoutType> type = new JComboBox<>(WorkoutType.values());
        form.add(new JLabel("Name:")); form.add(name);
        form.add(new JLabel("Muscle group:")); form.add(group);
        form.add(new JLabel("Type:")); form.add(type);
        add(form, BorderLayout.CENTER);

        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    service.addExercise(name.getText().trim(),
                        (MuscleGroup) group.getSelectedItem(),
                        (WorkoutType) type.getSelectedItem());
                    return null;
                }
                @Override protected void done() {
                    dispose(); onDone.run();
                }
            }.execute();
        });
        add(save, BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(parent);
    }
}

class BodyweightDialog extends JDialog {
    BodyweightDialog(Frame parent, WorkoutService service, Runnable onDone) {
        super(parent, "Log bodyweight", true);
        setLayout(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        JTextField kg = new JTextField(8);
        JTextField date = new JTextField(LocalDate.now().toString());
        form.add(new JLabel("kg:")); form.add(kg);
        form.add(new JLabel("date (YYYY-MM-DD):")); form.add(date);
        add(form, BorderLayout.CENTER);

        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            try {
                double k = Double.parseDouble(kg.getText().trim());
                LocalDate d = LocalDate.parse(date.getText().trim());
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() { service.recordBodyweight(d, k); return null; }
                    @Override protected void done() { dispose(); onDone.run(); }
                }.execute();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        });
        add(save, BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(parent);
    }
}

class AddSessionDialog extends JDialog {
    AddSessionDialog(Frame parent, WorkoutService service, Runnable onDone) {
        super(parent, "Record workout", true);
        setLayout(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        JTextField date = new JTextField(LocalDate.now().toString());
        DefaultComboBoxModel<Exercise> model = new DefaultComboBoxModel<>();
        for (Exercise ex : service.listExercises()) model.addElement(ex);
        JComboBox<Exercise> exercise = new JComboBox<>(model);
        JTextField weight = new JTextField();
        JTextField reps = new JTextField();
        form.add(new JLabel("date:")); form.add(date);
        form.add(new JLabel("exercise:")); form.add(exercise);
        form.add(new JLabel("weight (kg):")); form.add(weight);
        form.add(new JLabel("reps:")); form.add(reps);
        add(form, BorderLayout.CENTER);

        JButton save = new JButton("Save session");
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
                    @Override protected void done() { dispose(); onDone.run(); }
                }.execute();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        });
        add(save, BorderLayout.SOUTH);
        setSize(420, 220); setLocationRelativeTo(parent);
    }
}

class PrBoardDialog extends JDialog {
    PrBoardDialog(Frame parent, WorkoutService service) {
        super(parent, "PR board (e1RM)", true);
        setLayout(new BorderLayout());
        StringBuilder sb = new StringBuilder("<html><h3>Estimated 1RM (Epley)</h3><ul>");
        java.util.List<Exercise> exs = service.listExercises();
        if (exs.isEmpty()) sb.append("<li>No exercises yet</li>");
        for (Exercise ex : exs) {
            double best = service.bestE1RM(ex);
            if (best > 0) sb.append("<li>").append(ex.getName()).append(" - ").append(String.format("%.1f", best)).append(" kg</li>");
        }
        sb.append("</ul></html>");
        add(new JScrollPane(new JLabel(sb.toString())), BorderLayout.CENTER);
        setSize(360, 320); setLocationRelativeTo(parent);
    }
}

class WeeklyDialog extends JDialog {
    WeeklyDialog(Frame parent, WorkoutService service) {
        super(parent, "Weekly tonnage", true);
        setLayout(new BorderLayout());
        Map<MuscleGroup, Double> weekly = service.weeklyTonnageByGroup(LocalDate.now());
        StringBuilder sb = new StringBuilder("<html><h3>This week by muscle group</h3><ul>");
        double total = 0;
        for (Map.Entry<MuscleGroup, Double> en : weekly.entrySet()) {
            if (en.getKey() == null) continue;
            sb.append("<li>").append(en.getKey()).append(" - ").append(String.format("%,.0f", en.getValue())).append(" kg</li>");
            total += en.getValue();
        }
        sb.append("</ul><p><b>Total: ").append(String.format("%,.0f", total)).append(" kg</b></p></html>");
        add(new JScrollPane(new JLabel(sb.toString())), BorderLayout.CENTER);
        setSize(360, 300); setLocationRelativeTo(parent);
    }
}
