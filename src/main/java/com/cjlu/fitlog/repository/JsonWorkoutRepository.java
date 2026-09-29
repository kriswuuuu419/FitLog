package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.WorkoutSession;
import com.cjlu.fitlog.exception.FitLogException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class JsonWorkoutRepository implements WorkoutRepository {

    /** 顶层快照：一次序列化所有聚合根。 */
    public static class Snapshot {
        public List<Exercise> exercises = new ArrayList<>();
        public List<WorkoutSession> sessions = new ArrayList<>();
        public List<BodyweightEntry> bodyweights = new ArrayList<>();
    }

    private final Path file;
    private final ObjectMapper mapper;
    private Snapshot snapshot;

    public JsonWorkoutRepository(Path file) {
        this.file = file;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            snapshot = new Snapshot();
            return;
        }
        try {
            snapshot = mapper.readValue(file.toFile(), Snapshot.class);
            if (snapshot.exercises == null) snapshot.exercises = new ArrayList<>();
            if (snapshot.sessions == null) snapshot.sessions = new ArrayList<>();
            if (snapshot.bodyweights == null) snapshot.bodyweights = new ArrayList<>();
        } catch (IOException e) {
            throw new FitLogException("数据文件读取失败: " + file, e);
        }
    }

    /** 原子写：先写临时文件，再原子替换，避免半截文件损坏原数据。 */
    private void persist() {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tmp = Files.createTempFile(parent, "fitlog", ".tmp");
            mapper.writeValue(tmp.toFile(), snapshot);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("数据保存失败", e);
        }
    }

    @Override public List<Exercise> findAllExercises() {
        return new ArrayList<>(snapshot.exercises);
    }

    @Override public void saveExercise(Exercise e) {
        snapshot.exercises.removeIf(x -> x.equals(e));
        snapshot.exercises.add(e);
        persist();
    }

    @Override public List<WorkoutSession> findAllSessions() {
        return new ArrayList<>(snapshot.sessions);
    }

    @Override public void saveSession(WorkoutSession s) {
        snapshot.sessions.removeIf(x -> x.getDate().equals(s.getDate()) && x.getSets().size() == s.getSets().size());
        snapshot.sessions.add(s);
        persist();
    }

    @Override public List<BodyweightEntry> findAllBodyweights() {
        return new ArrayList<>(snapshot.bodyweights);
    }

    @Override public void saveBodyweight(BodyweightEntry b) {
        snapshot.bodyweights.removeIf(x -> x.getDate().equals(b.getDate()));
        snapshot.bodyweights.add(b);
        persist();
    }
}
