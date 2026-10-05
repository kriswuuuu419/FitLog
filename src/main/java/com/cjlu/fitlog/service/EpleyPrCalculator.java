package com.cjlu.fitlog.service;

/**
 * Epley estimated 1RM: e1RM = w * (1 + r/30).
 * Common argument validation is handled by the AbstractPrCalculator base class;
 * this class only supplies the concrete formula. To add another algorithm such as
 * Brzycki, create a new subclass and implement formula() without touching service code.
 */
public class EpleyPrCalculator extends AbstractPrCalculator {

    @Override
    protected double formula(double weightKg, int reps) {
        return weightKg * (1.0 + reps / 30.0);
    }
}
