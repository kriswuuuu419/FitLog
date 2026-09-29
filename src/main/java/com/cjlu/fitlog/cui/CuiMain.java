package com.cjlu.fitlog.cui;

import com.cjlu.fitlog.domain.BodyweightEntry;
import com.cjlu.fitlog.domain.Exercise;
import com.cjlu.fitlog.domain.MuscleGroup;
import com.cjlu.fitlog.domain.SetRecord;
import com.cjlu.fitlog.domain.WorkoutSession;
import com.cjlu.fitlog.domain.WorkoutType;
import com.cjlu.fitlog.exception.FitLogException;
import com.cjlu.fitlog.service.NutritionResult;
import com.cjlu.fitlog.service.WorkoutService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class CuiMain {

    private final WorkoutService service;
    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;

    public CuiMain(WorkoutService service) {
        this.service = service;
    }

    public void run() {
        System.out.println("=== FitLog 健身训练日志 ===");
        while (running) {
            printMenu();
            String choice = readLine("> ");
            switch (choice) {
                case "0": running = false; System.out.println("再见!"); break;
                case "1": handleAddExercise(); break;
                case "2": handleListExercises(); break;
                case "3": handleRecordWorkout(); break;
                case "4": handleViewHistory(); break;
                case "5": handleShowPR(); break;
                case "6": handleWeeklySummary(); break;
                case "7": handleBodyweightAndNutrition(); break;
                default: System.out.println("无效选项，请重新输入");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("---- 主菜单 ----");
        System.out.println("1. 新增动作");
        System.out.println("2. 查看动作库");
        System.out.println("3. 记录一次训练");
        System.out.println("4. 查看历史训练");
        System.out.println("5. 查看动作 PR");
        System.out.println("6. 本周容量统计");
        System.out.println("7. 记录体重 / 今日营养目标");
        System.out.println("0. 退出");
    }

    // ---------- 1. 新增动作 ----------
    private void handleAddExercise() {
        try {
            String name = readLine("动作名称: ");
            System.out.println("肌群: 1=胸 2=背 3=腿 4=肩 5=臂 6=核心");
            MuscleGroup group = readEnum("选肌群 (1-6): ", MuscleGroup.class);
            System.out.println("类型: 1=复合 2=孤立");
            WorkoutType type = readEnum("选类型 (1-2): ", WorkoutType.class);
            Exercise e = service.addExercise(name, group, type);
            System.out.println("已添加: " + e);
        } catch (FitLogException ex) {
            System.out.println("✗ " + ex.getMessage());
        }
    }

    private void handleListExercises() {
        List<Exercise> all = service.listExercises();
        if (all.isEmpty()) {
            System.out.println("动作库为空，先添加几个动作");
            return;
        }
        System.out.println("---- 动作库 (" + all.size() + ") ----");
        for (int i = 0; i < all.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, all.get(i));
        }
    }

    // ---------- 3. 记录训练 ----------
    private void handleRecordWorkout() {
        try {
            String dateStr = readLine("训练日期 (YYYY-MM-DD，回车=今天): ");
            LocalDate date = dateStr.isBlank()
                    ? LocalDate.now()
                    : LocalDate.parse(dateStr);
            WorkoutSession session = service.startSession(date, "");
            while (true) {
                handleListExercises();
                String name = readLine("输入动作名称 (:back 完成本次训练): ");
                if (":back".equalsIgnoreCase(name)) break;
                Exercise ex = service.findExerciseByName(name)
                        .orElseThrow(() -> new FitLogException("动作不存在: " + name));
                double weight = readDouble("重量 kg: ");
                int reps = readInt("次数: ");
                SetRecord set = service.recordSet(session, ex, weight, reps);
                System.out.println("已记录: " + set);
                service.checkPR(ex, weight, reps).ifPresent(System.out::println);
            }
            service.finishSession(session);
            System.out.println("本次训练已保存，共 " + session.getSets().size() + " 组，总容量 "
                    + Math.round(session.totalTonnage()) + " kg");
        } catch (FitLogException ex) {
            System.out.println("✗ " + ex.getMessage());
        }
    }

    // ---------- 4. 历史 ----------
    private void handleViewHistory() {
        List<WorkoutSession> all = service.listSessions();
        if (all.isEmpty()) {
            System.out.println("还没有训练记录");
            return;
        }
        System.out.println("---- 历史训练 ----");
        for (WorkoutSession s : all) {
            System.out.printf("%s  %d 组  容量 %.0f kg%n",
                    s.getDate(), s.getSets().size(), s.totalTonnage());
        }
    }

    // ---------- 5. PR ----------
    private void handleShowPR() {
        String name = readLine("要查哪个动作的 PR: ");
        Exercise ex = service.findExerciseByName(name)
                .orElseThrow(() -> new FitLogException("动作不存在: " + name));
        double best = service.bestE1RM(ex);
        if (best <= 0) {
            System.out.println("还没有 " + ex.getName() + " 的训练记录");
        } else {
            System.out.printf("%s 历史最佳估算 1RM = %.1f kg%n", ex.getName(), best);
        }
    }

    // ---------- 6. 周容量 ----------
    private void handleWeeklySummary() {
        String week = readLine("查看哪一周 (该周任意一天 YYYY-MM-DD，回车=本周): ");
        LocalDate anchor = week.isBlank() ? LocalDate.now() : LocalDate.parse(week);
        Map<MuscleGroup, Double> m = service.weeklyTonnageByGroup(anchor);
        System.out.println("---- 周容量 ----");
        double total = m.remove(null);
        for (Map.Entry<MuscleGroup, Double> e : m.entrySet()) {
            System.out.printf("%s: %.0f kg%n", e.getKey().getLabel(), e.getValue());
        }
        System.out.printf("合计: %.0f kg%n", total);
    }

    // ---------- 7. 体重 + 营养 ----------
    private void handleBodyweightAndNutrition() {
        double kg = readDouble("录入今日体重 kg: ");
        service.recordBodyweight(LocalDate.now(), kg);
        System.out.println("已记录: " + kg + " kg");

        String day = readLine("今天是训练日吗？(y/n): ");
        boolean training = day.trim().equalsIgnoreCase("y") || day.trim().equalsIgnoreCase("yes");
        NutritionResult r = service.nutritionGoal(kg, training);
        System.out.println("今日营养目标: " + r);
    }

    // ---------- 输入工具 ----------

    private String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        String line = scanner.nextLine();
        if (line == null) { running = false; return ""; }
        line = line.trim();
        if (":quit".equalsIgnoreCase(line)) {
            running = false;
            System.out.println("再见!");
            return "";
        }
        return line;
    }

    private double readDouble(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("请输入数字");
            }
        }
    }

    private int readInt(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("请输入整数");
            }
        }
    }

    private <E extends Enum<E>> E readEnum(String prompt, Class<E> cls) {
        E[] values = cls.getEnumConstants();
        while (true) {
            String s = readLine(prompt);
            try {
                int i = Integer.parseInt(s);
                if (i >= 1 && i <= values.length) return values[i - 1];
            } catch (NumberFormatException ignored) {}
            System.out.println("请输入 1-" + values.length);
        }
    }
}
