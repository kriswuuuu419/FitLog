package com.cjlu.fitlog;

import com.cjlu.fitlog.service.DefaultNutritionCalculator;
import com.cjlu.fitlog.service.NutritionResult;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class NutritionCalculatorTest {

    private final DefaultNutritionCalculator calc = new DefaultNutritionCalculator();

    @Test
    public void eightyKgTrainingDay() {
        NutritionResult r = calc.calculate(80, true);
        assertEquals(160.0, r.proteinGrams(), 0.01);
        assertEquals(64.0, r.fatGrams(), 0.01);
        assertEquals(320.0, r.carbGrams(), 0.01);
        assertEquals(2496.0, r.totalCalories(), 1.0);
    }

    @Test
    public void eightyKgRestDayHasLessCarb() {
        NutritionResult r = calc.calculate(80, false);
        assertEquals(160.0, r.proteinGrams(), 0.01);
        assertEquals(160.0, r.carbGrams(), 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeBodyweightRejected() {
        calc.calculate(-5, true);
    }
}
