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
                "蛋白质 %.0fg (%dkcal)  脂肪 %.0fg (%dkcal)  碳水 %.0fg (%dkcal)  合计 %dkcal",
                proteinGrams, Math.round(proteinGrams * 4),
                fatGrams, Math.round(fatGrams * 9),
                carbGrams, Math.round(carbGrams * 4),
                Math.round(totalCalories)
        );
    }
}
