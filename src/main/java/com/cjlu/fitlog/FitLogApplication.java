package com.cjlu.fitlog;

import com.cjlu.fitlog.cui.CuiMain;
import com.cjlu.fitlog.repository.JsonWorkoutRepository;
import com.cjlu.fitlog.repository.WorkoutRepository;
import com.cjlu.fitlog.service.DefaultNutritionCalculator;
import com.cjlu.fitlog.service.EpleyPrCalculator;
import com.cjlu.fitlog.service.WorkoutService;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FitLogApplication {
    public static void main(String[] args) {
        Path dataFile = Paths.get("data", "fitlog.json");
        WorkoutRepository repo = new JsonWorkoutRepository(dataFile);
        WorkoutService service = new WorkoutService(repo, new EpleyPrCalculator(), new DefaultNutritionCalculator());
        new CuiMain(service).run();
    }
}
