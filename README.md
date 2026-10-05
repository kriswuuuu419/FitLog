# FitLog — Personal Fitness Workout Journal

A workout & nutrition journal built for COMP603 / ENSE600 (Group 22).

- **Assignment 1:** Java 17 + Maven **CUI** application with JSON file storage.
- **Assignment 2:** **Swing GUI** backed by an embedded Apache Derby database, reusing the same domain and service layers.

## Build & run

Prerequisites: JDK 17+ and Maven 3.6+.

Compile and run the command-line app (Assignment 1):

```bash
mvn clean compile exec:java
```

Launch the Swing GUI (Assignment 2):

```bash
mvn clean compile exec:java -Dexec.args="--gui"
```

Run the tests:

```bash
mvn test
```

The CUI stores data in `data/fitlog.json` and the GUI in an embedded Derby database under `data/fitlog-derby` (both created automatically on first run). There is no login.

## Features

- Manage an exercise library (Chest / Back / Legs / Shoulders / Arms / Core; Compound / Isolation).
- Record a workout: pick an exercise and log multiple sets (weight x reps); sets entered on the same day are grouped into one training session.
- Automatic PR detection: the Epley formula estimates 1RM and flags a new personal record as you log.
- View workout history, per-exercise PRs and weekly training volume by muscle group.
- Log bodyweight and get daily protein / fat / carb and calorie targets.
- Referential integrity: an exercise still referenced by saved sets cannot be deleted, and the app explains why.

## Architecture

```
src/main/java/com/cjlu/fitlog/
├── FitLogApplication.java        Entry point; wires dependencies and selects CUI/GUI
├── domain/                       Data classes and enums
├── service/                      Business rules and strategy interfaces (PrCalculator / NutritionCalculator)
├── repository/                   Persistence interface with JSON and embedded Derby implementations
├── cui/                          Command-line menus (Assignment 1)
├── gui/                          Swing interface (Assignment 2)
└── exception/                    FitLogException
```

Assignment 2 adds `DerbyWorkoutRepository`, which implements the same `WorkoutRepository` interface, so the service and domain code are unchanged.
