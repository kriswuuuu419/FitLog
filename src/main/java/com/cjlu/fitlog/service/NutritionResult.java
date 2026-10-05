package com.cjlu.fitlog.service;

public record NutritionResult(
        double proteinGrams,
        double fatGrams,
        double carbGrams,
        double totalCalories
) {
    @Override
    public String toString() {
        return String.format(
                "Protein %.0fg (%dkcal)  Fat %.0fg (%dkcal)  Carbs %.0fg (%dkcal)  Total %dkcal",
                proteinGrams, Math.round(proteinGrams * 4),
                fatGrams, Math.round(fatGrams * 9),
                carbGrams, Math.round(carbGrams * 4),
                Math.round(totalCalories)
        );
    }
}
