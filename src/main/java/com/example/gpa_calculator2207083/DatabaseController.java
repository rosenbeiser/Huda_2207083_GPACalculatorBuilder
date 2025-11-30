package com.example.gpa_calculator2207083;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class DatabaseController {

    @FXML private TableView<SemesterDisplay> semesterTable;
    @FXML private TableColumn<SemesterDisplay, String> colSemesterName;
    @FXML private TableColumn<SemesterDisplay, String> colGPA;
    @FXML private TableColumn<SemesterDisplay, String> colCredits;

    @FXML private TableView<CourseDisplay> courseTable;
    @FXML private TableColumn<CourseDisplay, String> colCourseCode;
    @FXML private TableColumn<CourseDisplay, String> colCourseName;
    @FXML private TableColumn<CourseDisplay, String> colCredit;
    @FXML private TableColumn<CourseDisplay, String> colGrade;
    @FXML private TableColumn<CourseDisplay, String> colTeacher1;
    @FXML private TableColumn<CourseDisplay, String> colTeacher2;

    @FXML private Label lblSelectedSemester;
    @FXML private Label lblTotalSemesters;
    @FXML private Label lblTotalCredits;
    @FXML private Label lblCumulativeGPA;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button btnLoad;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;
    @FXML private Button btnClose;

    private ObservableList<SemesterDisplay> semesterList = FXCollections.observableArrayList();
    private ObservableList<CourseDisplay> courseList = FXCollections.observableArrayList();
    private DatabaseService databaseService;
    private DatabaseManager dbManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");

    @FXML
    public void initialize() {
        databaseService = new DatabaseService();
        dbManager = DatabaseManager.getInstance();

        // Setup semester table columns
        colSemesterName.setCellValueFactory(new PropertyValueFactory<>("semesterName"));
        colGPA.setCellValueFactory(new PropertyValueFactory<>("gpa"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));

        // Setup course table columns
        colCourseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        colCourseName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        colTeacher1.setCellValueFactory(new PropertyValueFactory<>("teacher1"));
        colTeacher2.setCellValueFactory(new PropertyValueFactory<>("teacher2"));

        semesterTable.setItems(semesterList);
        courseTable.setItems(courseList);

        // Add selection listener
        semesterTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadCoursesForSemester(newSelection.getId());
                        lblSelectedSemester.setText("Courses for: " + newSelection.getSemesterName());
                    }
                }
        );

        progressIndicator.setVisible(false);
        loadAllData();
    }

    private void loadAllData() {
        progressIndicator.setVisible(true);

        databaseService.loadSemestersAsync(
                semesters -> {
                    semesterList.clear();
                    for (DatabaseManager.SemesterRecord record : semesters) {
                        semesterList.add(new SemesterDisplay(
                                record.getId(),
                                record.getSemesterName(),
                                String.format("%.2f", record.getGpa()),
                                String.format("%.1f", record.getTotalCredits())
                        ));
                    }
                    loadStatistics();
                },
                error -> {
                    progressIndicator.setVisible(false);
                    showAlert("Error", "Load Failed", error);
                }
        );
    }

    private void loadStatistics() {
        databaseService.calculateStatisticsAsync(
                stats -> {
                    progressIndicator.setVisible(false);
                    lblTotalSemesters.setText(String.valueOf(stats.getTotalSemesters()));
                    lblTotalCredits.setText(String.format("%.1f", stats.getTotalCredits()));
                    lblCumulativeGPA.setText(String.format("%.2f", stats.getCumulativeGPA()));
                },
                error -> {
                    progressIndicator.setVisible(false);
                    showAlert("Error", "Statistics Error", error);
                }
        );
    }

    private void loadCoursesForSemester(int semesterId) {
        progressIndicator.setVisible(true);

        databaseService.loadCoursesAsync(
                semesterId,
                courses -> {
                    progressIndicator.setVisible(false);
                    courseList.clear();
                    for (gpainputcontroller.Course course : courses) {
                        courseList.add(new CourseDisplay(
                                course.getCourseCode(),
                                course.getCourseName(),
                                String.format("%.1f", course.getCredit()),
                                course.getGrade(),
                                course.getTeacher1(),
                                course.getTeacher2()
                        ));
                    }
                },
                error -> {
                    progressIndicator.setVisible(false);
                    showAlert("Error", "Load Error", error);
                }
        );
    }

    @FXML
    private void loadSemester() {
        SemesterDisplay selected = semesterTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "No Selection", "Please select a semester to load.");
            return;
        }

        // Load the semester directly without confirmation
        Stage stage = (Stage) btnLoad.getScene().getWindow();
        stage.close();
        // Note: You'll need to implement a callback mechanism to actually load into main controller
    }

    @FXML
    private void deleteSemester() {
        SemesterDisplay selected = semesterTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "No Selection", "Please select a semester to delete.");
            return;
        }

        // Delete directly without confirmation
        progressIndicator.setVisible(true);

        new Thread(() -> {
            try {
                dbManager.deleteSemester(selected.getId());
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showAlert("Success", "Deleted", "Semester deleted successfully!");
                    loadAllData();
                    courseList.clear();
                    lblSelectedSemester.setText("Select a semester to view courses");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showAlert("Error", "Delete Failed", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void refreshData() {
        courseList.clear();
        lblSelectedSemester.setText("Select a semester to view courses");
        loadAllData();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void cleanup() {
        if (databaseService != null) {
            databaseService.shutdown();
        }
    }

    // Display classes for TableView
    public static class SemesterDisplay {
        private int id;
        private String semesterName;
        private String gpa;
        private String credits;

        public SemesterDisplay(int id, String semesterName, String gpa, String credits) {
            this.id = id;
            this.semesterName = semesterName;
            this.gpa = gpa;
            this.credits = credits;
        }

        public int getId() { return id; }
        public String getSemesterName() { return semesterName; }
        public String getGpa() { return gpa; }
        public String getCredits() { return credits; }
    }

    public static class CourseDisplay {
        private String courseCode;
        private String courseName;
        private String credit;
        private String grade;
        private String teacher1;
        private String teacher2;

        public CourseDisplay(String courseCode, String courseName, String credit,
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
        public String getCredit() { return credit; }
        public String getGrade() { return grade; }
        public String getTeacher1() { return teacher1; }
        public String getTeacher2() { return teacher2; }
    }
}