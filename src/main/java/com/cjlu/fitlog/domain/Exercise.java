package com.cjlu.fitlog.domain;

import java.util.Objects;

public class Exercise {
    private String name;
    private MuscleGroup muscleGroup;
    private WorkoutType type;

    public Exercise() {
        // Jackson deserialization
    }

    public Exercise(String name, MuscleGroup muscleGroup, WorkoutType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("exercise name must not be empty");
        }
        this.name = name.trim();
        this.muscleGroup = Objects.requireNonNull(muscleGroup, "muscleGroup must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public String getName() { return name; }
    public MuscleGroup getMuscleGroup() { return muscleGroup; }
    public WorkoutType getType() { return type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exercise)) return false;
        Exercise e = (Exercise) o;
        return name.equalsIgnoreCase(e.name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name + " [" + muscleGroup.getLabel() + "/" + type.getLabel() + "]";
    }
}
