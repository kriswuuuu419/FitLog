package com.cjlu.fitlog.service;

public interface NutritionCalculator {
    NutritionResult calculate(double bodyweightKg, boolean trainingDay);
}
