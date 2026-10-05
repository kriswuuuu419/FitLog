package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.WorkoutSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 数据访问接口，业务层只依赖它，不关心底层是 JSON 文件还是嵌入式 Derby。
 * 读操作与写操作各自都超过三个，覆盖动作库 / 训练记录 / 体重三类数据的增删查。
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
