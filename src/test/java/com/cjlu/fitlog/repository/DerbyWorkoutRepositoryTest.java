package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

public class DerbyWorkoutRepositoryTest {

    private Path tmpDir;
    private DerbyWorkoutRepository repo;

    @Before
    public void setUp() throws Exception {
        tmpDir = Files.createTempDirectory("derby-test");
        repo = new DerbyWorkoutRepository(tmpDir.resolve("db").toString());
    }

    @After
    public void tearDown() {
        repo.shutdown();
    }

    @Test
    public void exerciseRoundTrip() {
        repo.saveExercise(new Exercise("Bench Press", MuscleGroup.CHEST, WorkoutType.COMPOUND));
        List<Exercise> all = repo.findAllExercises();
        assertEquals(1, all.size());
        assertEquals("Bench Press", all.get(0).getName());
        assertEquals(MuscleGroup.CHEST, all.get(0).getMuscleGroup());
    }

    @Test
    public void sessionRoundTripKeepsSets() {
        repo.saveExercise(new Exercise("Squat", MuscleGroup.LEGS, WorkoutType.COMPOUND));
        Exercise squat = repo.findAllExercises().get(0);

        WorkoutSession s = new WorkoutSession(LocalDate.now(), "heavy");
        s.addSet(new SetRecord(squat, 100, 5));
        s.addSet(new SetRecord(squat, 105, 3));
        repo.saveSession(s);

        List<WorkoutSession> all = repo.findAllSessions();
        assertEquals(1, all.size());
        assertEquals(2, all.get(0).getSets().size());
        assertEquals(105.0, all.get(0).getSets().get(1).getWeightKg(), 0.001);
    }

    @Test
    public void bodyweightUpsert() {
        LocalDate d = LocalDate.now();
        repo.saveBodyweight(new BodyweightEntry(d, 80.0));
        repo.saveBodyweight(new BodyweightEntry(d, 81.5));
        List<BodyweightEntry> all = repo.findAllBodyweights();
        assertEquals(1, all.size());
        assertEquals(81.5, all.get(0).getKg(), 0.001);
    }

    @Test
    public void dataSurvivesRepoReopen() {
        repo.saveExercise(new Exercise("Deadlift", MuscleGroup.BACK, WorkoutType.COMPOUND));
        repo.shutdown();

        DerbyWorkoutRepository reopn = new DerbyWorkoutRepository(tmpDir.resolve("db").toString());
        assertEquals(1, reopn.findAllExercises().size());
        reopn.shutdown();
    }

    @Test
    public void emptyDatabaseReturnsEmptyLists() {
        assertTrue(repo.findAllExercises().isEmpty());
        assertTrue(repo.findAllSessions().isEmpty());
        assertTrue(repo.findAllBodyweights().isEmpty());
    }
}
