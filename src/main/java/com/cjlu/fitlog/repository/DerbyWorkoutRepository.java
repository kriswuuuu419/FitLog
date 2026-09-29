package com.cjlu.fitlog.repository;

import com.cjlu.fitlog.domain.*;
import com.cjlu.fitlog.exception.FitLogException;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            conn = DriverManager.getConnection(jdbcUrl);
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new FitLogException("Cannot open embedded Derby database", e);
        }
    }

    private void initSchema() {
        try (Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS exercises (" +
                "  name VARCHAR(64) PRIMARY KEY," +
                "  muscle_group VARCHAR(16) NOT NULL," +
                "  type VARCHAR(16) NOT NULL" +
                ")");
            st.execute(
                "CREATE TABLE IF NOT EXISTS sessions (" +
                "  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  date DATE NOT NULL," +
                "  notes VARCHAR(256)" +
                ")");
            st.execute(
                "CREATE TABLE IF NOT EXISTS sets (" +
                "  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  session_id INTEGER NOT NULL REFERENCES sessions(id)," +
                "  exercise_name VARCHAR(64) NOT NULL REFERENCES exercises(name)," +
                "  weight_kg DOUBLE NOT NULL," +
                "  reps INTEGER NOT NULL" +
                ")");
            st.execute(
                "CREATE TABLE IF NOT EXISTS bodyweights (" +
                "  date DATE PRIMARY KEY," +
                "  kg DOUBLE NOT NULL" +
                ")");
        } catch (SQLException e) {
            throw new FitLogException("Cannot initialise Derby schema", e);
        }
    }

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
    public void saveExercise(Exercise e) {
        String sql = "MERGE INTO exercises AS t " +
                     "USING (VALUES ?) AS s(name) " +
                     "ON t.name = s.name " +
                     "WHEN MATCHED THEN UPDATE SET muscle_group=?, type=? " +
                     "WHEN NOT MATCHED THEN INSERT (name, muscle_group, type) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getMuscleGroup().name());
            ps.setString(3, e.getType().name());
            ps.setString(4, e.getName());
            ps.setString(5, e.getMuscleGroup().name());
            ps.setString(6, e.getType().name());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new FitLogException("save exercise failed", ex);
        }
    }

    @Override
    public List<WorkoutSession> findAllSessions() {
        Map<String, Exercise> exByName = new HashMap<>();
        for (Exercise e : findAllExercises()) exByName.put(e.getName(), e);

        List<WorkoutSession> out = new ArrayList<>();
        String sessionSql = "SELECT id, date, notes FROM sessions ORDER BY date";
        String setSql = "SELECT exercise_name, weight_kg, reps FROM sets WHERE session_id=?";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sessionSql)) {
            while (rs.next()) {
                long sid = rs.getLong("id");
                WorkoutSession s = new WorkoutSession(rs.getDate("date").toLocalDate(), rs.getString("notes"));
                try (PreparedStatement ps = conn.prepareStatement(setSql)) {
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
        } catch (SQLException e) {
            throw new FitLogException("read sessions failed", e);
        }
        return out;
    }

    @Override
    public void saveSession(WorkoutSession s) {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO sessions (date, notes) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setDate(1, java.sql.Date.valueOf(s.getDate()));
                ps.setString(2, s.getNotes());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                long sid = 0;
                if (keys.next()) sid = keys.getLong(1);

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
    public void saveBodyweight(BodyweightEntry b) {
        String sql = "MERGE INTO bodyweights AS t USING (VALUES ?) AS s(d) " +
                     "ON t.date = s.d WHEN MATCHED THEN UPDATE SET kg=? " +
                     "WHEN NOT MATCHED THEN INSERT (date, kg) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(b.getDate()));
            ps.setDouble(2, b.getKg());
            ps.setDate(3, java.sql.Date.valueOf(b.getDate()));
            ps.setDouble(4, b.getKg());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new FitLogException("save bodyweight failed", e);
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
