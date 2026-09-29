package com.cjlu.fitlog.domain;

public enum WorkoutType {
    COMPOUND("复合动作"),
    ISOLATION("孤立动作");

    private final String label;

    WorkoutType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
