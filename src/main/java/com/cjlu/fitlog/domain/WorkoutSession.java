package com.cjlu.fitlog.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorkoutSession {
    private LocalDate date;
    private List<SetRecord> sets = new ArrayList<>();
    private String notes;

    public WorkoutSession() {
        // Jackson
    }

    public WorkoutSession(LocalDate date, String notes) {
        this.date = date;
        this.notes = notes == null ? "" : notes;
    }

    public void addSet(SetRecord set) {
        this.sets.add(set);
    }

    public LocalDate getDate() { return date; }
    public List<SetRecord> getSets() { return sets; }
    public String getNotes() { return notes; }

    public double totalTonnage() {
        return sets.stream().mapToDouble(SetRecord::tonnage).sum();
    }

    public Set<MuscleGroup> muscleGroups() {
        Set<MuscleGroup> groups = new HashSet<>();
        for (SetRecord s : sets) {
            groups.add(s.getExercise().getMuscleGroup());
        }
        return groups;
    }
}
