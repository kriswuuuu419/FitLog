package com.cjlu.fitlog.service;

/**
 * Common template base for PR estimators (template-method pattern).
 * estimateE1RM performs shared argument validation first, then delegates the
 * concrete formula to the subclass formula(). Adding another algorithm such as
 * Brzycki only requires a new subclass implementing formula(); validation is reused.
 */
public abstract class AbstractPrCalculator implements PrCalculator {

    @Override
    public final double estimateE1RM(double weightKg, int reps) {
        if (weightKg <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
        if (reps <= 0) {
            throw new IllegalArgumentException("reps must be positive");
        }
        return formula(weightKg, reps);
    }

    protected abstract double formula(double weightKg, int reps);
}
