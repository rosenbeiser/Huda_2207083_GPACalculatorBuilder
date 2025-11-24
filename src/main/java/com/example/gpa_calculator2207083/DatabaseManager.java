package com.example.gpa_calculator2207083;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:gpa_calculator.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            // Enable foreign keys support
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            createTables();
            System.out.println("Database initialized successfully!");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database initialization failed!");
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Reconnecting to database...");
            connection = DriverManager.getConnection(DB_URL);
            // Enable foreign keys support
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }

    private void createTables() {
        String createSemesterTable = """
            CREATE TABLE IF NOT EXISTS semester (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                semester_name TEXT NOT NULL,
                total_credits REAL NOT NULL,
                gpa REAL NOT NULL,
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createCoursesTable = """
            CREATE TABLE IF NOT EXISTS courses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                semester_id INTEGER,
                course_code TEXT NOT NULL,
                course_name TEXT NOT NULL,
                credit REAL NOT NULL,
                grade TEXT NOT NULL,
                teacher1 TEXT,
                teacher2 TEXT,
                FOREIGN KEY (semester_id) REFERENCES semester(id) ON DELETE CASCADE
            )
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createSemesterTable);
            stmt.execute(createCoursesTable);
            System.out.println("Database tables created successfully!");
        } catch (SQLException e) {
            System.err.println("Failed to create tables!");
            e.printStackTrace();
        }
    }

    public int insertSemester(String semesterName, double totalCredits, double gpa) throws SQLException {
        String sql = "INSERT INTO semester (semester_name, total_credits, gpa) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, semesterName);
            pstmt.setDouble(2, totalCredits);
            pstmt.setDouble(3, gpa);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("Semester inserted with ID: " + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert semester!");
            throw e;
        }
        return -1;
    }

    public void insertCourse(int semesterId, Course course) throws SQLException {
        String sql = "INSERT INTO courses (semester_id, course_code, course_name, credit, grade, teacher1, teacher2) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, semesterId);
            pstmt.setString(2, course.getCourseCode());
            pstmt.setString(3, course.getCourseName());
            pstmt.setDouble(4, course.getCredit());
            pstmt.setString(5, course.getGrade());
            pstmt.setString(6, course.getTeacher1());
            pstmt.setString(7, course.getTeacher2());
            pstmt.executeUpdate();
            System.out.println("Course inserted: " + course.getCourseCode());
        } catch (SQLException e) {
            System.err.println("Failed to insert course!");
            throw e;
        }
    }

    public List<SemesterRecord> getAllSemesters() throws SQLException {
        List<SemesterRecord> semesters = new ArrayList<>();
        String sql = "SELECT * FROM semester ORDER BY created_date DESC";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SemesterRecord record = new SemesterRecord(
                        rs.getInt("id"),
                        rs.getString("semester_name"),
                        rs.getDouble("total_credits"),
                        rs.getDouble("gpa"),
                        rs.getTimestamp("created_date")
                );
                semesters.add(record);
            }
        }
        return semesters;
    }

    public List<Course> getCoursesBySemester(int semesterId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE semester_id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, semesterId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getDouble("credit"),
                        rs.getString("grade"),
                        rs.getString("teacher1"),
                        rs.getString("teacher2")
                );
                courses.add(course);
            }
        }
        return courses;
    }

    public void updateSemester(int id, String semesterName, double totalCredits, double gpa) throws SQLException {
        String sql = "UPDATE semester SET semester_name = ?, total_credits = ?, gpa = ? WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, semesterName);
            pstmt.setDouble(2, totalCredits);
            pstmt.setDouble(3, gpa);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        }
    }

    public void deleteSemester(int id) throws SQLException {
        String sql = "DELETE FROM semester WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public double getCumulativeGPA() throws SQLException {
        String sql = "SELECT SUM(gpa * total_credits) as weighted_sum, SUM(total_credits) as total FROM semester";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                double totalCredits = rs.getDouble("total");
                if (totalCredits > 0) {
                    return rs.getDouble("weighted_sum") / totalCredits;
                }
            }
        }
        return 0.0;
    }

    public double getTotalCredits() throws SQLException {
        String sql = "SELECT SUM(total_credits) as total FROM semester";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class SemesterRecord {
        private int id;
        private String semesterName;
        private double totalCredits;
        private double gpa;
        private Timestamp createdDate;

        public SemesterRecord(int id, String semesterName, double totalCredits, double gpa, Timestamp createdDate) {
            this.id = id;
            this.semesterName = semesterName;
            this.totalCredits = totalCredits;
            this.gpa = gpa;
            this.createdDate = createdDate;
        }

        public int getId() { return id; }
        public String getSemesterName() { return semesterName; }
        public double getTotalCredits() { return totalCredits; }
        public double getGpa() { return gpa; }
        public Timestamp getCreatedDate() { return createdDate; }

        @Override
        public String toString() {
            return semesterName + " (GPA: " + String.format("%.2f", gpa) + ")";
        }
    }

    public static class Course {
        private String courseCode;
        private String courseName;
        private double credit;
        private String grade;
        private String teacher1;
        private String teacher2;

        public Course(String courseCode, String courseName, double credit,
                      String grade, String teacher1, String teacher2) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.credit = credit;
            this.grade = grade;
            this.teacher1 = teacher1;
            this.teacher2 = teacher2;
        }

        public String getCourseCode() { return courseCode; }
        public String getCourseName() { return courseName; }
        public double getCredit() { return credit; }
        public String getGrade() { return grade; }
        public String getTeacher1() { return teacher1; }
        public String getTeacher2() { return teacher2; }

        public void setCourseCode(String value) { courseCode = value; }
        public void setCourseName(String value) { courseName = value; }
        public void setCredit(double value) { credit = value; }
        public void setGrade(String value) { grade = value; }
        public void setTeacher1(String value) { teacher1 = value; }
        public void setTeacher2(String value) { teacher2 = value; }
    }
}