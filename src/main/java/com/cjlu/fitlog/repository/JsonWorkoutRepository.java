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
import java.time.LocalDate;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JsonWorkoutRepository implements WorkoutRepository {

    /** Top-level snapshot: all aggregate roots are serialized together. */
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
            throw new FitLogException("Failed to read data file: " + file, e);
        }
    }

    /** Atomic write: write a temp file first, then atomically replace it so a partial write cannot corrupt the data. */
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
            throw new UncheckedIOException("Failed to save data", e);
        }
    }

    @Override public List<Exercise> findAllExercises() {
        return new ArrayList<>(snapshot.exercises);
    }

    @Override public Optional<Exercise> findExerciseByName(String name) {
        if (name == null) return Optional.empty();
        String n = name.trim();
        return snapshot.exercises.stream()
                .filter(x -> x.getName().equalsIgnoreCase(n))
                .findFirst();
    }

    @Override public void saveExercise(Exercise e) {
        snapshot.exercises.removeIf(x -> x.equals(e));
        snapshot.exercises.add(e);
        persist();
    }

    @Override public List<WorkoutSession> findAllSessions() {
        return new ArrayList<>(snapshot.sessions);
    }

    @Override public List<WorkoutSession> findSessionsBetween(LocalDate from, LocalDate to) {
        List<WorkoutSession> out = new ArrayList<>();
        for (WorkoutSession s : snapshot.sessions) {
            if (!s.getDate().isBefore(from) && !s.getDate().isAfter(to)) {
                out.add(s);
            }
        }
        return out;
    }

    @Override public void saveSession(WorkoutSession s) {
        // Upsert by date: append the new sets to that day's session when one already exists
        // (the GUI hands over a fresh one-set session object on every dialog save), otherwise
        // add a new session.
        WorkoutSession existing = snapshot.sessions.stream()
                .filter(x -> x.getDate().equals(s.getDate()))
                .findFirst().orElse(null);
        if (existing != null) {
            s.getSets().forEach(existing::addSet);
        } else {
            snapshot.sessions.add(s);
        }
        persist();
    }

    @Override public List<BodyweightEntry> findAllBodyweights() {
        return new ArrayList<>(snapshot.bodyweights);
    }

    @Override public Optional<BodyweightEntry> findLatestBodyweight() {
        return snapshot.bodyweights.stream()
                .max(Comparator.comparing(BodyweightEntry::getDate));
    }

    @Override public void saveBodyweight(BodyweightEntry b) {
        snapshot.bodyweights.removeIf(x -> x.getDate().equals(b.getDate()));
        snapshot.bodyweights.add(b);
        persist();
    }

    @Override public void deleteBodyweight(LocalDate date) {
        snapshot.bodyweights.removeIf(x -> x.getDate().equals(date));
        persist();
    }

    @Override public void deleteExercise(String name) {
        snapshot.exercises.removeIf(x -> x.getName().equalsIgnoreCase(name));
        persist();
    }

    @Override public void deleteSession(java.time.LocalDate date) {
        snapshot.sessions.removeIf(x -> x.getDate().equals(date));
        persist();
    }
}
