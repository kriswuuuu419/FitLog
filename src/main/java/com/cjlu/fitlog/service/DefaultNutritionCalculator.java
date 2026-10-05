package com.cjlu.fitlog.service;

/**
 * Common macronutrient coefficients for fitness users:
 *   protein 2.0 g/kg (midpoint of the 1.6-2.2 muscle-gain range)
 *   fat 0.8 g/kg
 *   carbs 4.0 g/kg on training days and 2.0 g/kg on rest days
 * Calorie conversion: 4 kcal/g for protein and carbs, 9 kcal/g for fat.
 */
public class DefaultNutritionCalculator implements NutritionCalculator {
    private static final double PROTEIN_PER_KG = 2.0;
    private static final double FAT_PER_KG = 0.8;
    private static final double CARB_PER_KG_TRAINING = 4.0;
    private static final double CARB_PER_KG_REST = 2.0;

    @Override
    public NutritionResult calculate(double bodyweightKg, boolean trainingDay) {
        if (bodyweightKg <= 0 || bodyweightKg > 500) {
            throw new IllegalArgumentException("invalid bodyweight: " + bodyweightKg);
        }
        double protein = bodyweightKg * PROTEIN_PER_KG;
        double fat = bodyweightKg * FAT_PER_KG;
        double carb = bodyweightKg * (trainingDay ? CARB_PER_KG_TRAINING : CARB_PER_KG_REST);
        double calories = protein * 4 + fat * 9 + carb * 4;
        return new NutritionResult(protein, fat, carb, calories);
    }
}
