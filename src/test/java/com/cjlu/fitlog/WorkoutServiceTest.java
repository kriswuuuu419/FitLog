package com.cjlu.fitlog;

import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.MuscleGroup;
import com.cjlu.fitlog.domain.WorkoutSession;
import com.cjlu.fitlog.domain.WorkoutType;
import com.cjlu.fitlog.repository.InMemoryWorkoutRepository;
import com.cjlu.fitlog.service.DefaultNutritionCalculator;
import com.cjlu.fitlog.service.EpleyPrCalculator;
import com.cjlu.fitlog.service.WorkoutService;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkoutServiceTest {

    private InMemoryWorkoutRepository repo;
    private WorkoutService service;
    private Exercise bench;

    @Before
    public void setUp() {
        repo = new InMemoryWorkoutRepository();
        service = new WorkoutService(repo, new EpleyPrCalculator(), new DefaultNutritionCalculator());
        bench = service.addExercise("杠铃卧推", MuscleGroup.CHEST, WorkoutType.COMPOUND);
    }

    @Test
    public void newPrIsDetected() {
        // 先存一组历史：100kg x 5 -> e1RM = 116.67
        WorkoutSession old = service.startSession(LocalDate.of(2026, 9, 1), "");
        service.recordSet(old, bench, 100, 5);
        service.finishSession(old);

        // 新一组：105kg x 5 -> e1RM = 122.5，应该触发 PR
        Optional<String> msg = service.checkPR(bench, 105, 5);
        assertTrue(msg.isPresent());
        assertTrue(msg.get().contains("PR"));
    }

    @Test
    public void equalPerformanceDoesNotTriggerPr() {
        WorkoutSession old = service.startSession(LocalDate.of(2026, 9, 1), "");
        service.recordSet(old, bench, 100, 5);
        service.finishSession(old);

        Optional<String> msg = service.checkPR(bench, 100, 5);
        assertTrue(!msg.isPresent());
    }

    @Test
    public void weeklyTonnageSumsCorrectly() {
        WorkoutSession s = service.startSession(LocalDate.of(2026, 9, 28), ""); // 本周一
        service.recordSet(s, bench, 100, 5); // 500 kg
        service.recordSet(s, bench, 100, 5); // 500 kg
        service.finishSession(s);

        Map<MuscleGroup, Double> m = service.weeklyTonnageByGroup(LocalDate.of(2026, 9, 30));
        assertEquals(1000.0, m.get(MuscleGroup.CHEST), 0.01);
        assertEquals(1000.0, m.get(null), 0.01);
    }

    @Test
    public void duplicateExerciseNameRejected() {
        try {
            service.addExercise("杠铃卧推", MuscleGroup.CHEST, WorkoutType.COMPOUND);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("已存在"));
            return;
        }
        throw new AssertionError("应该抛出重复动作异常");
    }
}
