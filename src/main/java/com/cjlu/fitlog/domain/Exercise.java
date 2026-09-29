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
            throw new IllegalArgumentException("动作名称不能为空");
        }
        this.name = name.trim();
        this.muscleGroup = Objects.requireNonNull(muscleGroup, "肌群不能为空");
        this.type = Objects.requireNonNull(type, "动作类型不能为空");
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
