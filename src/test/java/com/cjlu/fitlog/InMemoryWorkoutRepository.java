package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.WorkoutSession;

import java.util.ArrayList;
import java.util.List;

/** 测试用内存实现，不写文件。 */
public class InMemoryWorkoutRepository implements WorkoutRepository {
    public final List<Exercise> exercises = new ArrayList<>();
    public final List<WorkoutSession> sessions = new ArrayList<>();
    public final List<BodyweightEntry> bodyweights = new ArrayList<>();

    @Override public List<Exercise> findAllExercises() { return new ArrayList<>(exercises); }
    @Override public void saveExercise(Exercise e) { exercises.add(e); }
    @Override public List<WorkoutSession> findAllSessions() { return new ArrayList<>(sessions); }
    @Override public void saveSession(WorkoutSession s) { sessions.add(s); }
    @Override public List<BodyweightEntry> findAllBodyweights() { return new ArrayList<>(bodyweights); }
    @Override public void saveBodyweight(BodyweightEntry b) { bodyweights.add(b); }
}
