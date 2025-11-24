package com.example.gpa_calculator2207083;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DatabaseService {
    private final ExecutorService executor;
    private final DatabaseManager dbManager;

    public DatabaseService() {
        this.executor = Executors.newSingleThreadExecutor();
        this.dbManager = DatabaseManager.getInstance();
    }

    public void saveSemesterAsync(String semesterName, double totalCredits, double gpa,
                                  ObservableList<gpainputcontroller.Course> courses,
                                  Consumer<Boolean> onSuccess,
                                  Consumer<String> onError) {
        executor.submit(() -> {
            try {
                int semesterId = dbManager.insertSemester(semesterName, totalCredits, gpa);

                for (gpainputcontroller.Course course : courses) {
                    // Convert JavaFX Course to DatabaseManager.Course
                    DatabaseManager.Course dbCourse = new DatabaseManager.Course(
                            course.getCourseCode(),
                            course.getCourseName(),
                            course.getCredit(),
                            course.getGrade(),
                            course.getTeacher1(),
                            course.getTeacher2()
                    );
                    dbManager.insertCourse(semesterId, dbCourse);
                }

                Platform.runLater(() -> onSuccess.accept(true));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    public void loadSemestersAsync(Consumer<List<DatabaseManager.SemesterRecord>> onSuccess,
                                   Consumer<String> onError) {
        executor.submit(() -> {
            try {
                List<DatabaseManager.SemesterRecord> semesters = dbManager.getAllSemesters();
                Platform.runLater(() -> onSuccess.accept(semesters));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    public void loadCoursesAsync(int semesterId,
                                 Consumer<List<gpainputcontroller.Course>> onSuccess,
                                 Consumer<String> onError) {
        executor.submit(() -> {
            try {
                List<DatabaseManager.Course> dbCourses = dbManager.getCoursesBySemester(semesterId);

                // Convert DatabaseManager.Course to JavaFX Course
                List<gpainputcontroller.Course> courses = dbCourses.stream()
                        .map(c -> new gpainputcontroller.Course(
                                c.getCourseCode(),
                                c.getCourseName(),
                                c.getCredit(),
                                c.getGrade(),
                                c.getTeacher1(),
                                c.getTeacher2()
                        ))
                        .toList();

                Platform.runLater(() -> onSuccess.accept(courses));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    public void exportToJSONAsync(String semesterName, double gpa, double totalCredits,
                                  ObservableList<gpainputcontroller.Course> courses,
                                  String filePath,
                                  Consumer<Boolean> onSuccess,
                                  Consumer<String> onError) {
        executor.submit(() -> {
            try {
                String json = buildJSON(semesterName, gpa, totalCredits, courses);
                Files.writeString(Paths.get(filePath), json);
                Platform.runLater(() -> onSuccess.accept(true));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    public void importFromJSONAsync(String filePath,
                                    Consumer<SemesterData> onSuccess,
                                    Consumer<String> onError) {
        executor.submit(() -> {
            try {
                String json = Files.readString(Paths.get(filePath));
                SemesterData data = parseJSON(json);
                Platform.runLater(() -> onSuccess.accept(data));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    private String buildJSON(String semesterName, double gpa, double totalCredits,
                             List<gpainputcontroller.Course> courses) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"semesterName\": \"").append(escapeJSON(semesterName)).append("\",\n");
        json.append("  \"gpa\": ").append(gpa).append(",\n");
        json.append("  \"totalCredits\": ").append(totalCredits).append(",\n");
        json.append("  \"courses\": [\n");

        for (int i = 0; i < courses.size(); i++) {
            gpainputcontroller.Course c = courses.get(i);
            json.append("    {\n");
            json.append("      \"courseCode\": \"").append(escapeJSON(c.getCourseCode())).append("\",\n");
            json.append("      \"courseName\": \"").append(escapeJSON(c.getCourseName())).append("\",\n");
            json.append("      \"credit\": ").append(c.getCredit()).append(",\n");
            json.append("      \"grade\": \"").append(escapeJSON(c.getGrade())).append("\",\n");
            json.append("      \"teacher1\": \"").append(escapeJSON(c.getTeacher1())).append("\",\n");
            json.append("      \"teacher2\": \"").append(escapeJSON(c.getTeacher2())).append("\"\n");
            json.append("    }");
            if (i < courses.size() - 1) json.append(",");
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    private SemesterData parseJSON(String json) {
        String semesterName = extractStringValue(json, "semesterName");
        double gpa = extractDoubleValue(json, "gpa");
        double totalCredits = extractDoubleValue(json, "totalCredits");

        List<gpainputcontroller.Course> courses = new ArrayList<>();
        int coursesStart = json.indexOf("\"courses\": [");
        if (coursesStart != -1) {
            int arrayStart = json.indexOf("[", coursesStart);
            int arrayEnd = json.lastIndexOf("]");
            String coursesArray = json.substring(arrayStart + 1, arrayEnd);

            int braceCount = 0;
            int objectStart = -1;

            for (int i = 0; i < coursesArray.length(); i++) {
                char c = coursesArray.charAt(i);
                if (c == '{') {
                    if (braceCount == 0) objectStart = i;
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && objectStart != -1) {
                        String courseJson = coursesArray.substring(objectStart, i + 1);
                        courses.add(parseCourse(courseJson));
                    }
                }
            }
        }

        return new SemesterData(semesterName, gpa, totalCredits, courses);
    }

    private gpainputcontroller.Course parseCourse(String json) {
        String courseCode = extractStringValue(json, "courseCode");
        String courseName = extractStringValue(json, "courseName");
        double credit = extractDoubleValue(json, "credit");
        String grade = extractStringValue(json, "grade");
        String teacher1 = extractStringValue(json, "teacher1");
        String teacher2 = extractStringValue(json, "teacher2");

        return new gpainputcontroller.Course(courseCode, courseName, credit, grade, teacher1, teacher2);
    }

    private String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\": \"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private double extractDoubleValue(String json, String key) {
        String pattern = "\"" + key + "\": ";
        int start = json.indexOf(pattern);
        if (start == -1) return 0.0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        return Double.parseDouble(json.substring(start, end));
    }

    private String escapeJSON(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void calculateStatisticsAsync(Consumer<Statistics> onSuccess,
                                         Consumer<String> onError) {
        executor.submit(() -> {
            try {
                List<DatabaseManager.SemesterRecord> semesters = dbManager.getAllSemesters();
                double cumulativeGPA = dbManager.getCumulativeGPA();
                double totalCredits = dbManager.getTotalCredits();

                Statistics stats = new Statistics(
                        semesters.size(),
                        totalCredits,
                        cumulativeGPA
                );

                Platform.runLater(() -> onSuccess.accept(stats));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    public static class SemesterData {
        private String semesterName;
        private double gpa;
        private double totalCredits;
        private List<gpainputcontroller.Course> courses;

        public SemesterData(String semesterName, double gpa, double totalCredits,
                            List<gpainputcontroller.Course> courses) {
            this.semesterName = semesterName;
            this.gpa = gpa;
            this.totalCredits = totalCredits;
            this.courses = courses;
        }

        public String getSemesterName() { return semesterName; }
        public double getGpa() { return gpa; }
        public double getTotalCredits() { return totalCredits; }
        public List<gpainputcontroller.Course> getCourses() { return courses; }
    }

    public static class Statistics {
        private int totalSemesters;
        private double totalCredits;
        private double cumulativeGPA;

        public Statistics(int totalSemesters, double totalCredits, double cumulativeGPA) {
            this.totalSemesters = totalSemesters;
            this.totalCredits = totalCredits;
            this.cumulativeGPA = cumulativeGPA;
        }

        public int getTotalSemesters() { return totalSemesters; }
        public double getTotalCredits() { return totalCredits; }
        public double getCumulativeGPA() { return cumulativeGPA; }
    }
}