package com.example.gpa_calculator2207083;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.Optional;

public class gpainputcontroller {

    @FXML private TextField txtTotalCredit;
    @FXML private Label warning;
    @FXML private TextField txtCourseCode;
    @FXML private TextField txtCourseName;
    @FXML private TextField txtCourseCredit;
    @FXML private TextField txtGrade;
    @FXML private TextField txtTeacher1;
    @FXML private TextField txtTeacher2;
    @FXML private Button addCourse1;
    @FXML private TableView<Course> table;
    @FXML private TableColumn<Course, String> courseCode;
    @FXML private TableColumn<Course, String> courseName;
    @FXML private TableColumn<Course, Double> courseCredit;
    @FXML private TableColumn<Course, String> grade;
    @FXML private TableColumn<Course, String> teacher1;
    @FXML private TableColumn<Course, String> teacher2;
    @FXML private Label buttonWarning;
    @FXML private Button getResult;
    @FXML private Button deleteSelected;
    @FXML private Button clearAll;
    @FXML private Button saveToDatabase;
    @FXML private Button loadFromDatabase;
    @FXML private Button exportJSON;
    @FXML private Button importJSON;
    @FXML private Button viewStatistics;
    @FXML private ProgressIndicator progressIndicator;

    private ObservableList<Course> courseList = FXCollections.observableArrayList();
    private DatabaseService databaseService;

    // Observable properties for real-time updates
    private DoubleProperty totalCreditsProperty = new SimpleDoubleProperty(0.0);
    private IntegerProperty courseCountProperty = new SimpleIntegerProperty(0);

    @FXML
    public void initialize() {
        databaseService = new DatabaseService();

        courseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        courseCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));
        grade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        teacher1.setCellValueFactory(new PropertyValueFactory<>("teacher1"));
        teacher2.setCellValueFactory(new PropertyValueFactory<>("teacher2"));
        table.setItems(courseList);

        getResult.setDisable(true);
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }

        // Observable listeners
        courseList.addListener((javafx.collections.ListChangeListener.Change<? extends Course> c) -> {
            updateCalculateButtonState();
            updateObservableProperties();
        });

        txtTotalCredit.textProperty().addListener((obs, oldVal, newVal) -> {
            updateCalculateButtonState();
        });

        // Bind properties to display real-time info
        totalCreditsProperty.addListener((obs, oldVal, newVal) -> {
            System.out.println("Total Credits Updated: " + newVal);
        });

        courseCountProperty.addListener((obs, oldVal, newVal) -> {
            System.out.println("Course Count Updated: " + newVal);
        });
    }

    private void updateObservableProperties() {
        totalCreditsProperty.set(getCurrentTotalCredits());
        courseCountProperty.set(courseList.size());
    }

    private void updateCalculateButtonState() {
        if (txtTotalCredit.getText().trim().isEmpty() || courseList.isEmpty()) {
            getResult.setDisable(true);
            return;
        }
        try {
            double maxCredits = Double.parseDouble(txtTotalCredit.getText().trim());
            double currentTotal = getCurrentTotalCredits();
            getResult.setDisable(currentTotal != maxCredits);
        } catch (NumberFormatException e) {
            getResult.setDisable(true);
        }
    }

    @FXML
    private void addCourse() {
        warning.setText("");
        buttonWarning.setText("");
        if (txtCourseCode.getText().trim().isEmpty() ||
                txtCourseName.getText().trim().isEmpty() ||
                txtCourseCredit.getText().trim().isEmpty() ||
                txtGrade.getText().trim().isEmpty()) {
            warning.setText("Please fill in all required fields!");
            return;
        }
        try {
            String code = txtCourseCode.getText().trim();
            String name = txtCourseName.getText().trim();
            double credit = Double.parseDouble(txtCourseCredit.getText().trim());
            String gradeValue = txtGrade.getText().trim().toUpperCase();
            String t1 = txtTeacher1.getText().trim();
            String t2 = txtTeacher2.getText().trim();

            if (credit <= 0) {
                warning.setText("Credit must be greater than 0!");
                return;
            }
            if (!isValidGrade(gradeValue)) {
                warning.setText("Invalid grade! Use A+, A, A-, B+, B, B-, C+, C, C-, D+, D, F");
                return;
            }

            double currentTotal = getCurrentTotalCredits();
            if (!txtTotalCredit.getText().trim().isEmpty()) {
                double maxCredits = Double.parseDouble(txtTotalCredit.getText().trim());
                if (currentTotal + credit > maxCredits) {
                    warning.setText("Total credits exceed semester limit!");
                    return;
                }
            }

            Course course = new Course(code, name, credit, gradeValue, t1, t2);
            courseList.add(course);
            clearInputFields();

        } catch (NumberFormatException e) {
            warning.setText("Invalid number format for credit!");
        }
    }

    @FXML
    private void getResult() {
        buttonWarning.setText("");
        if (courseList.isEmpty()) {
            buttonWarning.setText("No courses added yet!");
            return;
        }

        double totalGradePoints = 0.0;
        double totalCredits = 0.0;
        for (Course course : courseList) {
            double gradePoint = getGradePoint(course.getGrade());
            totalGradePoints += gradePoint * course.getCredit();
            totalCredits += course.getCredit();
        }
        double gpa = totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resultview.fxml"));
            Parent root = loader.load();
            resultcontroller resultController = loader.getController();
            resultController.setResultData(gpa, totalCredits, courseList);

            Stage stage = new Stage();
            stage.setTitle("GPA Result - Academic Achievement");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open result window", e.getMessage());
        }
    }

    @FXML
    private void saveToDatabase() {
        if (courseList.isEmpty()) {
            buttonWarning.setText("No courses to save!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Fall 2024");
        dialog.setTitle("Save Semester");
        dialog.setHeaderText("Enter Semester Name");
        dialog.setContentText("Semester:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(semesterName -> {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            double totalCredits = getCurrentTotalCredits();
            double gpa = calculateGPA();

            databaseService.saveSemesterAsync(
                    semesterName, totalCredits, gpa, courseList,
                    success -> {
                        if (progressIndicator != null) progressIndicator.setVisible(false);
                        if (success) {
                            showAlert("Success", "Semester Saved",
                                    "Semester '" + semesterName + "' saved successfully!");
                        } else {
                            showAlert("Error", "Save Failed", "Failed to save semester to database.");
                        }
                    },
                    error -> {
                        if (progressIndicator != null) progressIndicator.setVisible(false);
                        showAlert("Error", "Database Error", error);
                    }
            );
        });
    }

    @FXML
    private void loadFromDatabase() {
        if (progressIndicator != null) progressIndicator.setVisible(true);

        databaseService.loadSemestersAsync(
                semesters -> {
                    if (progressIndicator != null) progressIndicator.setVisible(false);

                    if (semesters.isEmpty()) {
                        showAlert("Info", "No Data", "No saved semesters found in database.");
                        return;
                    }

                    // Create selection dialog
                    ChoiceDialog<DatabaseManager.SemesterRecord> dialog = new ChoiceDialog<>(semesters.get(0), semesters);
                    dialog.setTitle("Load Semester");
                    dialog.setHeaderText("Select a semester to load");
                    dialog.setContentText("Choose semester:");

                    Optional<DatabaseManager.SemesterRecord> result = dialog.showAndWait();
                    result.ifPresent(selected -> {
                        if (progressIndicator != null) progressIndicator.setVisible(true);

                        databaseService.loadCoursesAsync(
                                selected.getId(),
                                courses -> {
                                    if (progressIndicator != null) progressIndicator.setVisible(false);
                                    courseList.clear();
                                    courseList.addAll(courses);
                                    txtTotalCredit.setText(String.valueOf(selected.getTotalCredits()));
                                    showAlert("Success", "Semester Loaded",
                                            "Loaded semester: " + selected.getSemesterName());
                                },
                                error -> {
                                    if (progressIndicator != null) progressIndicator.setVisible(false);
                                    showAlert("Error", "Load Error", error);
                                }
                        );
                    });
                },
                error -> {
                    if (progressIndicator != null) progressIndicator.setVisible(false);
                    showAlert("Error", "Database Error", error);
                }
        );
    }

    @FXML
    private void exportJSON() {
        if (courseList.isEmpty()) {
            buttonWarning.setText("No data to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to JSON");
        fileChooser.setInitialFileName("semester_data.json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showSaveDialog(exportJSON.getScene().getWindow());
        if (file != null) {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            String semesterName = "Current Semester";
            double gpa = calculateGPA();
            double totalCredits = getCurrentTotalCredits();

            databaseService.exportToJSONAsync(
                    semesterName, gpa, totalCredits, courseList, file.getAbsolutePath(),
                    success -> {
                        if (progressIndicator != null) progressIndicator.setVisible(false);
                        showAlert("Success", "Export Complete",
                                "Data exported to: " + file.getName());
                    },
                    error -> {
                        if (progressIndicator != null) progressIndicator.setVisible(false);
                        showAlert("Error", "Export Failed", error);
                    }
            );
        }
    }

    @FXML
    private void importJSON() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import from JSON");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showOpenDialog(importJSON.getScene().getWindow());
        if (file != null) {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            databaseService.importFromJSONAsync(
                    file.getAbsolutePath(),
                    data -> {
                        if (progressIndicator != null) progressIndicator.setVisible(false);
                        courseList.clear();
                        courseList.addAll(data.getCourses());
                        txtTotalCredit.setText(String.valueOf(data.getTotalCredits()));
                        showAlert("Success", "Import Complete",
                                "Loaded semester: " + data.getSemesterName());
                    },
                    error -> {
                        if (progressIndicator != null) progressIndicator.setVisible(false);
                        showAlert("Error", "Import Failed", error);
                    }
            );
        }
    }

    @FXML
    private void viewStatistics() {
        if (progressIndicator != null) progressIndicator.setVisible(true);

        databaseService.calculateStatisticsAsync(
                stats -> {
                    if (progressIndicator != null) progressIndicator.setVisible(false);

                    String message = String.format(
                            "Total Semesters: %d\n" +
                                    "Total Credits Earned: %.1f\n" +
                                    "Cumulative GPA: %.2f",
                            stats.getTotalSemesters(),
                            stats.getTotalCredits(),
                            stats.getCumulativeGPA()
                    );

                    showAlert("Academic Statistics", "Your Overall Performance", message);
                },
                error -> {
                    if (progressIndicator != null) progressIndicator.setVisible(false);
                    showAlert("Error", "Statistics Error", error);
                }
        );
    }

    @FXML
    private void deleteSelected() {
        buttonWarning.setText("");
        Course selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            buttonWarning.setText("Please select a course to delete!");
            return;
        }
        courseList.remove(selected);
    }

    @FXML
    private void clearAll() {
        courseList.clear();
        clearInputFields();
        txtTotalCredit.clear();
        warning.setText("");
        buttonWarning.setText("");
    }

    private void clearInputFields() {
        txtCourseCode.clear();
        txtCourseName.clear();
        txtCourseCredit.clear();
        txtGrade.clear();
        txtTeacher1.clear();
        txtTeacher2.clear();
    }

    private double getCurrentTotalCredits() {
        return courseList.stream().mapToDouble(Course::getCredit).sum();
    }

    private double calculateGPA() {
        double totalGradePoints = 0.0;
        double totalCredits = 0.0;
        for (Course course : courseList) {
            totalGradePoints += getGradePoint(course.getGrade()) * course.getCredit();
            totalCredits += course.getCredit();
        }
        return totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
    }

    private boolean isValidGrade(String grade) {
        return grade.matches("A\\+|A|A-|B\\+|B|B-|C\\+|C|C-|D\\+|D|F");
    }

    private double getGradePoint(String grade) {
        return switch (grade) {
            case "A+", "A" -> 4.0;
            case "A-" -> 3.7;
            case "B+" -> 3.3;
            case "B" -> 3.0;
            case "B-" -> 2.7;
            case "C+" -> 2.3;
            case "C" -> 2.0;
            case "C-" -> 1.7;
            case "D+" -> 1.3;
            case "D" -> 1.0;
            case "F" -> 0.0;
            default -> 0.0;
        };
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

    // Observable Course class with properties
    public static class Course {
        private final StringProperty courseCode;
        private final StringProperty courseName;
        private final DoubleProperty credit;
        private final StringProperty grade;
        private final StringProperty teacher1;
        private final StringProperty teacher2;

        public Course(String courseCode, String courseName, double credit,
                      String grade, String teacher1, String teacher2) {
            this.courseCode = new SimpleStringProperty(courseCode);
            this.courseName = new SimpleStringProperty(courseName);
            this.credit = new SimpleDoubleProperty(credit);
            this.grade = new SimpleStringProperty(grade);
            this.teacher1 = new SimpleStringProperty(teacher1);
            this.teacher2 = new SimpleStringProperty(teacher2);
        }

        // Property getters
        public StringProperty courseCodeProperty() { return courseCode; }
        public StringProperty courseNameProperty() { return courseName; }
        public DoubleProperty creditProperty() { return credit; }
        public StringProperty gradeProperty() { return grade; }
        public StringProperty teacher1Property() { return teacher1; }
        public StringProperty teacher2Property() { return teacher2; }

        // Regular getters
        public String getCourseCode() { return courseCode.get(); }
        public String getCourseName() { return courseName.get(); }
        public double getCredit() { return credit.get(); }
        public String getGrade() { return grade.get(); }
        public String getTeacher1() { return teacher1.get(); }
        public String getTeacher2() { return teacher2.get(); }

        // Setters
        public void setCourseCode(String value) { courseCode.set(value); }
        public void setCourseName(String value) { courseName.set(value); }
        public void setCredit(double value) { credit.set(value); }
        public void setGrade(String value) { grade.set(value); }
        public void setTeacher1(String value) { teacher1.set(value); }
        public void setTeacher2(String value) { teacher2.set(value); }
    }
}