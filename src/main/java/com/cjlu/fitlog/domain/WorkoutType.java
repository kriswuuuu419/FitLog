package com.cjlu.fitlog.domain;

public enum WorkoutType {
    COMPOUND("Compound"),
    ISOLATION("Isolation");

    private final String label;

    WorkoutType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
