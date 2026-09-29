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
import java.util.HashMap;
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
            throw new FitLogException("动作已存在: " + name);
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
        if (name == null) return Optional.empty();
        String n = name.trim();
        return repo.findAllExercises().stream()
                .filter(e -> e.getName().equalsIgnoreCase(n))
                .findFirst();
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
            throw new FitLogException("这次训练还没有记录任何一组，未保存");
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
     * 计算某个动作的历史最佳 e1RM（扫描所有历史 set）。
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
     * 检查刚录入的这一组是否刷新了该动作的 PR。
     * 注意：调用时这一组已经在 session 里了，所以 bestE1RM 会把它算进去；
     * 我们和"录入前"的历史最佳比较。
     */
    public Optional<String> checkPR(Exercise exercise, double weightKg, int reps) {
        double incoming = prCalculator.estimateE1RM(weightKg, reps);
        // 历史最佳 = 排除本次 session 内同动作同重量同次数的那一组
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
                    "🏆 新 PR! %s 估算 1RM 从 %.1f kg 提升到 %.1f kg (本次: %.1fkg x %d)",
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
        for (WorkoutSession s : repo.findAllSessions()) {
            if (s.getDate().isBefore(monday) || s.getDate().isAfter(sunday)) continue;
            for (SetRecord set : s.getSets()) {
                double t = set.tonnage();
                byGroup.merge(set.getExercise().getMuscleGroup(), t, Double::sum);
                total += t;
            }
        }
        byGroup.put(null, total); // 合计
        return byGroup;
    }

    // ---------- Bodyweight & nutrition ----------

    public void recordBodyweight(LocalDate date, double kg) {
        repo.saveBodyweight(new BodyweightEntry(date, kg));
    }

    public Optional<BodyweightEntry> latestBodyweight() {
        return repo.findAllBodyweights().stream()
                .max(Comparator.comparing(BodyweightEntry::getDate));
    }

    public NutritionResult nutritionGoal(double bodyweightKg, boolean trainingDay) {
        return nutritionCalculator.calculate(bodyweightKg, trainingDay);
    }
}
