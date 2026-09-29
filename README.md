# FitLog — 个人健身训练日志

COMP603 / ENSE600 作业 1。Java 17 + Maven，CUI 命令行应用。

## 编译与运行

前置：JDK 17+、Maven 3.6+。

```bash
mvn clean compile exec:java
```

跑测试：

```bash
mvn test
```

数据保存在 `data/fitlog.json`（首次运行自动创建）。无登录账号。

## 功能

- 管理动作库（胸/背/腿/肩/臂/核心，复合/孤立）
- 记录一次训练：选动作 → 录若干组（重量×次数）
- 自动 PR 检测：Epley 公式估算 1RM，破纪录当场提示
- 查看历史训练、动作 PR、周容量统计
- 记录体重，自动计算每日蛋白质/脂肪/碳水目标

## 代码结构

```
src/main/java/com/cjlu/fitlog/
├── FitLogApplication.java      启动入口，组装依赖
├── domain/                     纯数据类 + 枚举
├── service/                    业务规则 + 策略接口（PrCalculator / NutritionCalculator）
├── repository/                 持久化抽象 + JSON 实现
├── cui/                        命令行菜单
└── exception/                  FitLogException
```

作业 2 扩展点：新增 `DerbyWorkoutRepository` 实现 `WorkoutRepository` 接口，业务代码不动。
