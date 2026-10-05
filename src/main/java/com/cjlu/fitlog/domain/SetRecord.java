package com.cjlu.fitlog.domain;

public class SetRecord {
    private Exercise exercise;
    private double weightKg;
    private int reps;

    public SetRecord() {
        // Jackson
    }

    public SetRecord(Exercise exercise, double weightKg, int reps) {
        if (exercise == null) throw new IllegalArgumentException("动作不能为空");
        if (weightKg <= 0) throw new IllegalArgumentException("重量必须为正数");
        if (reps < 1) throw new IllegalArgumentException("次数至少为 1");
        this.exercise = exercise;
        this.weightKg = weightKg;
        this.reps = reps;
    }

    public Exercise getExercise() { return exercise; }
    public double getWeightKg() { return weightKg; }
    public int getReps() { return reps; }

    public double tonnage() {
        return weightKg * reps;
    }

    @Override
    public String toString() {
        return exercise.getName() + "  " + weightKg + "kg x " + reps + " reps";
    }
}
