package com.cjlu.fitlog.service;

/**
 * 健身人群通用宏量营养素系数：
 *   蛋白 2.0 g/kg（增肌推荐区间 1.6-2.2 的中值）
 *   脂肪 0.8 g/kg
 *   碳水 训练日 4.0 g/kg，休息日 2.0 g/kg
 * 热量换算：蛋白/碳水 4 kcal/g，脂肪 9 kcal/g
 */
public class DefaultNutritionCalculator implements NutritionCalculator {
    private static final double PROTEIN_PER_KG = 2.0;
    private static final double FAT_PER_KG = 0.8;
    private static final double CARB_PER_KG_TRAINING = 4.0;
    private static final double CARB_PER_KG_REST = 2.0;

    @Override
    public NutritionResult calculate(double bodyweightKg, boolean trainingDay) {
        if (bodyweightKg <= 0 || bodyweightKg > 500) {
            throw new IllegalArgumentException("体重数值不合理: " + bodyweightKg);
        }
        double protein = bodyweightKg * PROTEIN_PER_KG;
        double fat = bodyweightKg * FAT_PER_KG;
        double carb = bodyweightKg * (trainingDay ? CARB_PER_KG_TRAINING : CARB_PER_KG_REST);
        double calories = protein * 4 + fat * 9 + carb * 4;
        return new NutritionResult(protein, fat, carb, calories);
    }
}
