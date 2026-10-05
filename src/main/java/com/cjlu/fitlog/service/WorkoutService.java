package com.cjlu.fitlog.service;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.MuscleGroup;
import com.cjlu.fitlog.domain.SetRecord;
import com.cjlu.fitlog.domain.WorkoutSession;
import com.cjlu.fitlog.domain.WorkoutType;
import com.cjlu.fitlog.exception.FitLogException;
import com.cjlu.fitlog.repository.WorkoutRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkoutService {

    private final WorkoutRepository repo;
    private final PrCalculator prCalculator;
    private final NutritionCalculator nutritionCalculator;

    public WorkoutService(WorkoutRepository repo,
                          PrCalculator prCalculator,
                          NutritionCalculator nutritionCalculator) {
        this.repo = repo;
        this.prCalculator = prCalculator;
        this.nutritionCalculator = nutritionCalculator;
    }

    // ---------- Exercise catalog ----------

    public Exercise addExercise(String name, MuscleGroup group, WorkoutType type) {
        if (findExerciseByName(name).isPresent()) {
            throw new FitLogException("Exercise already exists: " + name);
        }
        Exercise e = new Exercise(name, group, type);
        repo.saveExercise(e);
        return e;
    }

    public List<Exercise> listExercises() {
        List<Exercise> all = new ArrayList<>(repo.findAllExercises());
        all.sort(Comparator.comparing(e -> e.getMuscleGroup().name()));
        return all;
    }

    public Optional<Exercise> findExerciseByName(String name) {
        return repo.findExerciseByName(name);
    }

    // ---------- Workout sessions ----------

    public WorkoutSession startSession(LocalDate date, String notes) {
        return new WorkoutSession(date, notes);
    }

    public SetRecord recordSet(WorkoutSession session, Exercise exercise, double weightKg, int reps) {
        SetRecord set = new SetRecord(exercise, weightKg, reps);
        session.addSet(set);
        return set;
    }

    public void finishSession(WorkoutSession session) {
        if (session.getSets().isEmpty()) {
            throw new FitLogException("Cannot save an empty workout: record at least one set first");
        }
        repo.saveSession(session);
    }

    public List<WorkoutSession> listSessions() {
        List<WorkoutSession> all = new ArrayList<>(repo.findAllSessions());
        all.sort(Comparator.comparing(WorkoutSession::getDate).reversed());
        return all;
    }

    // ---------- PR tracking ----------

    /**
     * Best historical e1RM for an exercise, scanning every saved set.
     */
    public double bestE1RM(Exercise exercise) {
        double best = 0;
        for (WorkoutSession s : repo.findAllSessions()) {
            for (SetRecord set : s.getSets()) {
                if (set.getExercise().equals(exercise)) {
                    double e = prCalculator.estimateE1RM(set.getWeightKg(), set.getReps());
                    if (e > best) best = e;
                }
            }
        }
        return best;
    }

    /**
     * Checks whether the set just entered sets a new PR for the exercise.
     * The incoming set is already part of the session, so bestE1RM would include it;
     * here we compare against the historical best recorded before this session.
     */
    public Optional<String> checkPR(Exercise exercise, double weightKg, int reps) {
        double incoming = prCalculator.estimateE1RM(weightKg, reps);
        // Historical best, excluding sets from the current in-progress session.
        double prevBest = 0;
        for (WorkoutSession s : repo.findAllSessions()) {
            for (SetRecord set : s.getSets()) {
                if (set.getExercise().equals(exercise)) {
                    double e = prCalculator.estimateE1RM(set.getWeightKg(), set.getReps());
                    if (e > prevBest) prevBest = e;
                }
            }
        }
        if (incoming > prevBest + 1e-9) {
            return Optional.of(String.format(
                    "New PR! %s estimated 1RM improved from %.1f kg to %.1f kg (this set: %.1fkg x %d)",
                    exercise.getName(), prevBest, incoming, weightKg, reps));
        }
        return Optional.empty();
    }

    // ---------- Weekly summary ----------

    public Map<MuscleGroup, Double> weeklyTonnageByGroup(LocalDate anyDayInWeek) {
        LocalDate monday = anyDayInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        Map<MuscleGroup, Double> byGroup = new LinkedHashMap<>();
        double total = 0;
        for (WorkoutSession s : repo.findSessionsBetween(monday, sunday)) {
            for (SetRecord set : s.getSets()) {
                double t = set.tonnage();
                byGroup.merge(set.getExercise().getMuscleGroup(), t, Double::sum);
                total += t;
            }
        }
        byGroup.put(null, total); // grand total under the null key
        return byGroup;
    }

    // ---------- Bodyweight & nutrition ----------

    public void recordBodyweight(LocalDate date, double kg) {
        repo.saveBodyweight(new BodyweightEntry(date, kg));
    }

    public Optional<BodyweightEntry> latestBodyweight() {
        return repo.findLatestBodyweight();
    }

    public List<BodyweightEntry> findAllBodyweights() {
        return repo.findAllBodyweights();
    }

    public void deleteBodyweight(LocalDate date) {
        repo.deleteBodyweight(date);
    }

    public NutritionResult nutritionGoal(double bodyweightKg, boolean trainingDay) {
        return nutritionCalculator.calculate(bodyweightKg, trainingDay);
    }

    public void deleteExercise(String name) {
        repo.deleteExercise(name);
    }

    public void deleteSession(LocalDate date) {
        repo.deleteSession(date);
    }
}
