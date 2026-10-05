package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.WorkoutSession;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** In-memory implementation used by unit tests; writes nothing to disk. */
public class InMemoryWorkoutRepository implements WorkoutRepository {
    public final List<Exercise> exercises = new ArrayList<>();
    public final List<WorkoutSession> sessions = new ArrayList<>();
    public final List<BodyweightEntry> bodyweights = new ArrayList<>();

    @Override public List<Exercise> findAllExercises() { return new ArrayList<>(exercises); }

    @Override public Optional<Exercise> findExerciseByName(String name) {
        if (name == null) return Optional.empty();
        String n = name.trim();
        return exercises.stream().filter(e -> e.getName().equalsIgnoreCase(n)).findFirst();
    }

    @Override public void saveExercise(Exercise e) { exercises.add(e); }

    @Override public List<WorkoutSession> findAllSessions() { return new ArrayList<>(sessions); }

    @Override public List<WorkoutSession> findSessionsBetween(LocalDate from, LocalDate to) {
        List<WorkoutSession> out = new ArrayList<>();
        for (WorkoutSession s : sessions) {
            if (!s.getDate().isBefore(from) && !s.getDate().isAfter(to)) out.add(s);
        }
        return out;
    }

    @Override public void saveSession(WorkoutSession s) { sessions.add(s); }

    @Override public List<BodyweightEntry> findAllBodyweights() { return new ArrayList<>(bodyweights); }

    @Override public Optional<BodyweightEntry> findLatestBodyweight() {
        return bodyweights.stream().max(Comparator.comparing(BodyweightEntry::getDate));
    }

    @Override public void saveBodyweight(BodyweightEntry b) { bodyweights.add(b); }

    @Override public void deleteExercise(String name) { exercises.removeIf(e -> e.getName().equalsIgnoreCase(name)); }

    @Override public void deleteSession(LocalDate date) { sessions.removeIf(s -> s.getDate().equals(date)); }

    @Override public void deleteBodyweight(LocalDate date) { bodyweights.removeIf(b -> b.getDate().equals(date)); }
}
