package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.*;
import com.cjlu.fitlog.exception.FitLogException;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Embedded Apache Derby implementation of WorkoutRepository.
 * The database lives under data/fitlog-derby/ and is created on first boot.
 */
public class DerbyWorkoutRepository implements WorkoutRepository {

    private final String jdbcUrl;
    private Connection conn;

    public DerbyWorkoutRepository(String dbPath) {
        this.jdbcUrl = "jdbc:derby:" + dbPath + ";create=true";
        open();
        initSchema();
    }

    private void open() {
        try {
            DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
            conn = DriverManager.getConnection(jdbcUrl);
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new FitLogException("Cannot open embedded Derby database: " + e.getMessage(), e);
        }
    }

    private void initSchema() {
        execIfNotExists("CREATE TABLE exercises (" +
                "  name VARCHAR(64) PRIMARY KEY," +
                "  muscle_group VARCHAR(16) NOT NULL," +
                "  type VARCHAR(16) NOT NULL" +
                ")");
        execIfNotExists("CREATE TABLE sessions (" +
                "  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  date DATE NOT NULL," +
                "  notes VARCHAR(256)" +
                ")");
        execIfNotExists("CREATE TABLE sets (" +
                "  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  session_id INTEGER NOT NULL REFERENCES sessions(id)," +
                "  exercise_name VARCHAR(64) NOT NULL REFERENCES exercises(name)," +
                "  weight_kg DOUBLE NOT NULL," +
                "  reps INTEGER NOT NULL" +
                ")");
        execIfNotExists("CREATE TABLE bodyweights (" +
                "  date DATE PRIMARY KEY," +
                "  kg DOUBLE NOT NULL" +
                ")");
    }

    private void execIfNotExists(String sql) {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            // X0Y32 = table already exists; ignore on restart
            if (!"X0Y32".equals(e.getSQLState())) {
                throw new FitLogException("Cannot initialise Derby schema: " + e.getMessage(), e);
            }
        }
    }

    // ---------- Exercise reads / writes ----------

    @Override
    public List<Exercise> findAllExercises() {
        List<Exercise> out = new ArrayList<>();
        String sql = "SELECT name, muscle_group, type FROM exercises";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new Exercise(
                    rs.getString("name"),
                    MuscleGroup.valueOf(rs.getString("muscle_group")),
                    WorkoutType.valueOf(rs.getString("type"))));
            }
        } catch (SQLException e) {
            throw new FitLogException("read exercises failed", e);
        }
        return out;
    }

    @Override
    public Optional<Exercise> findExerciseByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String sql = "SELECT name, muscle_group, type FROM exercises WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Exercise(
                        rs.getString("name"),
                        MuscleGroup.valueOf(rs.getString("muscle_group")),
                        WorkoutType.valueOf(rs.getString("type"))));
                }
            }
        } catch (SQLException e) {
            throw new FitLogException("find exercise by name failed", e);
        }
        return Optional.empty();
    }

    /**
     * Atomic upsert keyed on exercise name: UPDATE first, INSERT only when no row matched,
     * all inside one transaction.
     *
     * We deliberately do not delete-then-insert: the {@code sets} table has a foreign key to
     * exercises(name), so removing an exercise row that is already referenced by a workout
     * would violate that constraint. Updating in place keeps the primary-key row intact.
     * Embedded Derby 10.14 also only accepts a base table/table function (not a parameterised
     * VALUES row) as the source of MERGE, so a single-statement MERGE upsert is unavailable;
     * the transaction makes UPDATE+INSERT atomic instead.
     */
    @Override
    public void saveExercise(Exercise e) {
        try {
            conn.setAutoCommit(false);
            int updated;
            try (PreparedStatement up = conn.prepareStatement(
                    "UPDATE exercises SET muscle_group=?, type=? WHERE name=?")) {
                up.setString(1, e.getMuscleGroup().name());
                up.setString(2, e.getType().name());
                up.setString(3, e.getName());
                updated = up.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO exercises (name, muscle_group, type) VALUES (?,?,?)")) {
                    ins.setString(1, e.getName());
                    ins.setString(2, e.getMuscleGroup().name());
                    ins.setString(3, e.getType().name());
                    ins.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new FitLogException("save exercise failed, rolled back", ex);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public void deleteExercise(String name) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM exercises WHERE name=?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            // SQL state 235xx is an integrity-constraint violation; 23503 is the foreign-key
            // restriction fired when saved sets still reference this exercise.
            if (e.getSQLState() != null && e.getSQLState().startsWith("235")) {
                throw new FitLogException("Cannot delete exercise \"" + name
                        + "\": it is still referenced by saved workout sets. "
                        + "Delete those sessions first.", e);
            }
            throw new FitLogException("delete exercise failed", e);
        }
    }

    // ---------- Session reads / writes ----------

    @Override
    public List<WorkoutSession> findAllSessions() {
        return loadSessions(null, null);
    }

    @Override
    public List<WorkoutSession> findSessionsBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new FitLogException("查询区间的起止日期不能为空", null);
        }
        return loadSessions(from, to);
    }

    /**
     * Assemble each session together with its sets in one place.
     * When from/to are null all sessions are returned; otherwise only sessions in the
     * inclusive date range are returned (used by the weekly volume summary).
     */
    private List<WorkoutSession> loadSessions(LocalDate from, LocalDate to) {
        Map<String, Exercise> exByName = new HashMap<>();
        for (Exercise e : findAllExercises()) exByName.put(e.getName(), e);

        List<WorkoutSession> out = new ArrayList<>();
        String sql = (from == null)
                ? "SELECT id, date, notes FROM sessions ORDER BY date"
                : "SELECT id, date, notes FROM sessions WHERE date BETWEEN ? AND ? ORDER BY date";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            if (from != null) {
                st.setDate(1, java.sql.Date.valueOf(from));
                st.setDate(2, java.sql.Date.valueOf(to));
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    long sid = rs.getLong("id");
                    WorkoutSession s = new WorkoutSession(rs.getDate("date").toLocalDate(), rs.getString("notes"));
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT exercise_name, weight_kg, reps FROM sets WHERE session_id=?")) {
                        ps.setLong(1, sid);
                        try (ResultSet srs = ps.executeQuery()) {
                            while (srs.next()) {
                                String en = srs.getString("exercise_name");
                                Exercise ex = exByName.get(en);
                                if (ex == null) continue;
                                s.addSet(new SetRecord(ex, srs.getDouble("weight_kg"), srs.getInt("reps")));
                            }
                        }
                    }
                    out.add(s);
                }
            }
        } catch (SQLException e) {
            throw new FitLogException("read sessions failed", e);
        }
        return out;
    }

    @Override
    public void saveSession(WorkoutSession s) {
        try {
            conn.setAutoCommit(false);
            // Upsert the parent row by date: reuse the row for that date when it already
            // exists so sets saved one at a time from the GUI merge into one session;
            // otherwise insert a new session row.
            long sid;
            try (PreparedStatement q = conn.prepareStatement(
                    "SELECT id FROM sessions WHERE date=?")) {
                q.setDate(1, java.sql.Date.valueOf(s.getDate()));
                try (ResultSet rs = q.executeQuery()) {
                    if (rs.next()) {
                        sid = rs.getLong(1);
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO sessions (date, notes) VALUES (?, ?)",
                                Statement.RETURN_GENERATED_KEYS)) {
                            ps.setDate(1, java.sql.Date.valueOf(s.getDate()));
                            ps.setString(2, s.getNotes());
                            ps.executeUpdate();
                            try (ResultSet keys = ps.getGeneratedKeys()) {
                                sid = keys.next() ? keys.getLong(1) : 0;
                            }
                        }
                    }
                }
            }
            // Append this save's sets; any existing sets for that date are preserved.
            try (PreparedStatement sp = conn.prepareStatement(
                    "INSERT INTO sets (session_id, exercise_name, weight_kg, reps) VALUES (?,?,?,?)")) {
                for (SetRecord set : s.getSets()) {
                    sp.setLong(1, sid);
                    sp.setString(2, set.getExercise().getName());
                    sp.setDouble(3, set.getWeightKg());
                    sp.setInt(4, set.getReps());
                    sp.addBatch();
                }
                sp.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new FitLogException("save session failed, rolled back", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public void deleteSession(LocalDate date) {
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM sets WHERE session_id IN (SELECT id FROM sessions WHERE date=?)")) {
                ps.setDate(1, java.sql.Date.valueOf(date));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE date=?")) {
                ps.setDate(1, java.sql.Date.valueOf(date));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new FitLogException("delete session failed", e);
        }
    }

    // ---------- Bodyweight reads / writes ----------

    @Override
    public List<BodyweightEntry> findAllBodyweights() {
        List<BodyweightEntry> out = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT date, kg FROM bodyweights ORDER BY date")) {
            while (rs.next()) out.add(new BodyweightEntry(rs.getDate("date").toLocalDate(), rs.getDouble("kg")));
        } catch (SQLException e) {
            throw new FitLogException("read bodyweights failed", e);
        }
        return out;
    }

    @Override
    public Optional<BodyweightEntry> findLatestBodyweight() {
        String sql = "SELECT date, kg FROM bodyweights ORDER BY date DESC FETCH FIRST 1 ROW ONLY";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(new BodyweightEntry(rs.getDate("date").toLocalDate(), rs.getDouble("kg")));
            }
        } catch (SQLException e) {
            throw new FitLogException("read latest bodyweight failed", e);
        }
        return Optional.empty();
    }

    /**
     * Atomic upsert keyed on bodyweight date: UPDATE first, INSERT only when no row matched,
     * all inside one transaction (one weight entry per day, later entry overwrites earlier).
     */
    @Override
    public void saveBodyweight(BodyweightEntry b) {
        try {
            conn.setAutoCommit(false);
            int updated;
            try (PreparedStatement up = conn.prepareStatement(
                    "UPDATE bodyweights SET kg=? WHERE date=?")) {
                up.setDouble(1, b.getKg());
                up.setDate(2, java.sql.Date.valueOf(b.getDate()));
                updated = up.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO bodyweights (date, kg) VALUES (?,?)")) {
                    ins.setDate(1, java.sql.Date.valueOf(b.getDate()));
                    ins.setDouble(2, b.getKg());
                    ins.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new FitLogException("save bodyweight failed, rolled back", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public void deleteBodyweight(LocalDate date) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bodyweights WHERE date=?")) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new FitLogException("delete bodyweight failed", e);
        }
    }

    public void shutdown() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException expected) {
            // Derby shutdown always throws SQL state 08006 / XJ015 — that's normal
        }
    }
}
