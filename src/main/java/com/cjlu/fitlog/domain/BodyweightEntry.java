package com.cjlu.fitlog.domain;

import java.time.LocalDate;

public class BodyweightEntry {
    private LocalDate date;
    private double kg;

    public BodyweightEntry() {}

    public BodyweightEntry(LocalDate date, double kg) {
        if (kg <= 0 || kg > 500) throw new IllegalArgumentException("bodyweight out of valid range (1-500 kg)");
        this.date = date;
        this.kg = kg;
    }

    public LocalDate getDate() { return date; }
    public double getKg() { return kg; }
}
