package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.WorkoutSession;

import java.util.List;

/**
 * 持久化抽象。作业 1 用 JSON 文件实现，作业 2 新增 DerbyWorkoutRepository
 * 实现同一接口，WorkoutService 不需要任何改动（DIP/OCP）。
 */
public interface WorkoutRepository {
    List<Exercise> findAllExercises();
    void saveExercise(Exercise exercise);

    List<WorkoutSession> findAllSessions();
    void saveSession(WorkoutSession session);

    List<BodyweightEntry> findAllBodyweights();
    void saveBodyweight(BodyweightEntry entry);
}
