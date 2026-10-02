package com.cjlu.fitlog;

import com.cjlu.fitlog.cui.CuiMain;
import com.cjlu.fitlog.gui.GuiMain;
import com.cjlu.fitlog.repository.DerbyWorkoutRepository;
import com.cjlu.fitlog.repository.JsonWorkoutRepository;
import com.cjlu.fitlog.repository.WorkoutRepository;
import com.cjlu.fitlog.service.DefaultNutritionCalculator;
import com.cjlu.fitlog.service.EpleyPrCalculator;
import com.cjlu.fitlog.service.WorkoutService;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FitLogApplication {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--gui")) {
            bootGui();
        } else {
            bootCui();
        }
    }

    public static void bootCui() {
        Path dataFile = Paths.get("data", "fitlog.json");
        WorkoutRepository repo = new JsonWorkoutRepository(dataFile);
        WorkoutService service = new WorkoutService(repo, new EpleyPrCalculator(), new DefaultNutritionCalculator());
        new CuiMain(service).run();
    }

    public static void bootGui() {
        DerbyWorkoutRepository repo = new DerbyWorkoutRepository("data/fitlog-derby");
        Runtime.getRuntime().addShutdownHook(new Thread(repo::shutdown));
        WorkoutService service = new WorkoutService(repo, new EpleyPrCalculator(), new DefaultNutritionCalculator());
        new GuiMain(service).start();
    }
}
