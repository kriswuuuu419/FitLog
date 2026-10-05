package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.WorkoutSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data-access interface. The service layer depends only on this abstraction and does
 * not care whether the backing store is a JSON file or embedded Derby. Both reads and
 * writes exceed three each, covering create/delete/query across exercises, sessions
 * and bodyweight entries.
 */
public interface WorkoutRepository {

    // ---------- Read operations ----------

    List<Exercise> findAllExercises();

    Optional<Exercise> findExerciseByName(String name);

    List<WorkoutSession> findAllSessions();

    List<WorkoutSession> findSessionsBetween(LocalDate from, LocalDate to);

    List<BodyweightEntry> findAllBodyweights();

    Optional<BodyweightEntry> findLatestBodyweight();

    // ---------- Write operations ----------

    void saveExercise(Exercise exercise);

    void deleteExercise(String name);

    void saveSession(WorkoutSession session);

    void deleteSession(LocalDate date);

    void saveBodyweight(BodyweightEntry entry);

    void deleteBodyweight(LocalDate date);
}
