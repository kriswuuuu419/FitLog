package com.cjlu.fitlog.cui;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.MuscleGroup;
import com.cjlu.fitlog.domain.SetRecord;
import com.cjlu.fitlog.domain.WorkoutSession;
import com.cjlu.fitlog.domain.WorkoutType;
import com.cjlu.fitlog.exception.FitLogException;
import com.cjlu.fitlog.service.NutritionResult;
import com.cjlu.fitlog.service.WorkoutService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class CuiMain {

    private final WorkoutService service;
    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;

    public CuiMain(WorkoutService service) {
        this.service = service;
    }

    public void run() {
        System.out.println("=== FitLog Workout & Nutrition Journal ===");
        while (running) {
            printMenu();
            String choice = readLine("> ");
            try {
                switch (choice) {
                    case "0": running = false; System.out.println("Goodbye!"); break;
                    case "1": handleAddExercise(); break;
                    case "2": handleListExercises(); break;
                    case "3": handleRecordWorkout(); break;
                    case "4": handleViewHistory(); break;
                    case "5": handleShowPR(); break;
                    case "6": handleWeeklySummary(); break;
                    case "7": handleBodyweightAndNutrition(); break;
                    default: System.out.println("Invalid option, please try again");
                }
            } catch (FitLogException ex) {
                System.out.println("Error: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                System.out.println("Error: invalid input - " + ex.getMessage());
            } catch (DateTimeParseException ex) {
                System.out.println("Error: invalid date; use YYYY-MM-DD (e.g. 2026-10-03), or press Enter for today");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("---- Main menu ----");
        System.out.println("1. Add exercise");
        System.out.println("2. List exercises");
        System.out.println("3. Record a workout");
        System.out.println("4. View workout history");
        System.out.println("5. Show exercise PR");
        System.out.println("6. Weekly volume summary");
        System.out.println("7. Log bodyweight / today's nutrition target");
        System.out.println("0. Exit");
    }

    // ---------- 1. Add exercise ----------
    private void handleAddExercise() {
        try {
            String name = readLine("Exercise name: ");
            System.out.println("Muscle group: 1=Chest 2=Back 3=Legs 4=Shoulders 5=Arms 6=Core");
            MuscleGroup group = readEnum("Choose muscle group (1-6): ", MuscleGroup.class);
            System.out.println("Type: 1=Compound 2=Isolation");
            WorkoutType type = readEnum("Choose type (1-2): ", WorkoutType.class);
            Exercise e = service.addExercise(name, group, type);
            System.out.println("Added: " + e);
        } catch (FitLogException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void handleListExercises() {
        List<Exercise> all = service.listExercises();
        if (all.isEmpty()) {
            System.out.println("Exercise library is empty; add a few exercises first");
            return;
        }
        System.out.println("---- Exercise library (" + all.size() + ") ----");
        for (int i = 0; i < all.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, all.get(i));
        }
    }

    // ---------- 3. Record a workout ----------
    private void handleRecordWorkout() {
        try {
            String dateStr = readLine("Workout date (YYYY-MM-DD, Enter=today): ");
            LocalDate date = dateStr.isBlank()
                    ? LocalDate.now()
                    : LocalDate.parse(dateStr);
            WorkoutSession session = service.startSession(date, "");
            while (true) {
                handleListExercises();
                String name = readLine("Exercise name (:back to finish this workout): ");
                if (":back".equalsIgnoreCase(name)) break;
                Exercise ex = service.findExerciseByName(name)
                        .orElseThrow(() -> new FitLogException("Exercise not found: " + name));
                double weight = readDouble("Weight kg: ");
                int reps = readInt("Reps: ");
                SetRecord set = service.recordSet(session, ex, weight, reps);
                System.out.println("Recorded: " + set);
                service.checkPR(ex, weight, reps).ifPresent(System.out::println);
            }
            service.finishSession(session);
            System.out.println("Workout saved: " + session.getSets().size() + " sets, total volume "
                    + Math.round(session.totalTonnage()) + " kg");
        } catch (FitLogException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (DateTimeParseException ex) {
            System.out.println("Error: invalid date; use YYYY-MM-DD (e.g. 2026-10-03), or press Enter for today");
        }
    }

    // ---------- 4. History ----------
    private void handleViewHistory() {
        List<WorkoutSession> all = service.listSessions();
        if (all.isEmpty()) {
            System.out.println("No workout records yet");
            return;
        }
        System.out.println("---- Workout history ----");
        for (WorkoutSession s : all) {
            System.out.printf("%s  %d sets  volume %.0f kg%n",
                    s.getDate(), s.getSets().size(), s.totalTonnage());
        }
    }

    // ---------- 5. PR ----------
    private void handleShowPR() {
        String name = readLine("PR for which exercise: ");
        Exercise ex = service.findExerciseByName(name)
                .orElseThrow(() -> new FitLogException("Exercise not found: " + name));
        double best = service.bestE1RM(ex);
        if (best <= 0) {
            System.out.println("No workout records yet for " + ex.getName());
        } else {
            System.out.printf("%s estimated best 1RM = %.1f kg%n", ex.getName(), best);
        }
    }

    // ---------- 6. Weekly volume ----------
    private void handleWeeklySummary() {
        String week = readLine("Which week (any day in it YYYY-MM-DD, Enter=this week): ");
        LocalDate anchor = week.isBlank() ? LocalDate.now() : LocalDate.parse(week);
        Map<MuscleGroup, Double> m = service.weeklyTonnageByGroup(anchor);
        System.out.println("---- Weekly volume ----");
        double total = m.remove(null);
        for (Map.Entry<MuscleGroup, Double> e : m.entrySet()) {
            System.out.printf("%s: %.0f kg%n", e.getKey().getLabel(), e.getValue());
        }
        System.out.printf("Total: %.0f kg%n", total);
    }

    // ---------- 7. Bodyweight + nutrition ----------
    private void handleBodyweightAndNutrition() {
        double kg = readDouble("Today's bodyweight kg: ");
        service.recordBodyweight(LocalDate.now(), kg);
        System.out.println("Logged: " + kg + " kg");

        String day = readLine("Is today a training day? (y/n): ");
        boolean training = day.trim().equalsIgnoreCase("y") || day.trim().equalsIgnoreCase("yes");
        NutritionResult r = service.nutritionGoal(kg, training);
        System.out.println("Today's nutrition target: " + r);
    }

    // ---------- Input helpers ----------

    private String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        String line = scanner.nextLine();
        if (line == null) { running = false; return ""; }
        line = line.trim();
        if (":quit".equalsIgnoreCase(line)) {
            running = false;
            System.out.println("Goodbye!");
            return "";
        }
        return line;
    }

    private double readDouble(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number");
            }
        }
    }

    private int readInt(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter an integer");
            }
        }
    }

    private <E extends Enum<E>> E readEnum(String prompt, Class<E> cls) {
        E[] values = cls.getEnumConstants();
        while (true) {
            String s = readLine(prompt);
            try {
                int i = Integer.parseInt(s);
                if (i >= 1 && i <= values.length) return values[i - 1];
            } catch (NumberFormatException ignored) {}
            System.out.println("Please enter 1-" + values.length);
        }
    }
}
