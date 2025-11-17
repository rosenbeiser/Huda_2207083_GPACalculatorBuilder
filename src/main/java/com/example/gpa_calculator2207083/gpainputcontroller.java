package com.example.gpa_calculator2207083;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class gpainputcontroller {

    @FXML
    private TextField txtTotalCredit;
    @FXML
    private Label warning;
    @FXML
    private TextField txtCourseCode;
    @FXML
    private TextField txtCourseName;
    @FXML
    private TextField txtCourseCredit;
    @FXML
    private TextField txtGrade;
    @FXML
    private TextField txtTeacher1;
    @FXML
    private TextField txtTeacher2;
    @FXML
    private Button addCourse1;
    @FXML
    private TableView<Course> table;
    @FXML
    private TableColumn<Course, String> courseCode;
    @FXML
    private TableColumn<Course, String> courseName;
    @FXML
    private TableColumn<Course, Double> courseCredit;
    @FXML
    private TableColumn<Course, String> grade;
    @FXML
    private TableColumn<Course, String> teacher1;
    @FXML
    private TableColumn<Course, String> teacher2;
    @FXML
    private Label buttonWarning;
    @FXML
    private Button getResult;
    @FXML
    private Button deleteSelected;
    @FXML
    private Button clearAll;

    private ObservableList<Course> courseList = FXCollections.observableArrayList();
    private double totalCredits = 0.0;

    @FXML
    public void initialize() {
        courseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        courseCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));
        grade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        teacher1.setCellValueFactory(new PropertyValueFactory<>("teacher1"));
        teacher2.setCellValueFactory(new PropertyValueFactory<>("teacher2"));
        table.setItems(courseList);
        getResult.setDisable(true);
        courseList.addListener((javafx.collections.ListChangeListener.Change<? extends Course> c) -> {
            updateCalculateButtonState();
        });

        txtTotalCredit.textProperty().addListener((obs, oldVal, newVal) -> {
            updateCalculateButtonState();
        });
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open result window");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
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
        double total = 0.0;
        for (Course course : courseList) {
            total += course.getCredit();
        }
        return total;
    }

    private boolean isValidGrade(String grade) {
        return grade.matches("A\\+|A|A-|B\\+|B|B-|C\\+|C|C-|D\\+|D|F");
    }

    private double getGradePoint(String grade) {
        switch (grade) {
            case "A+": return 4.0;
            case "A": return 4.0;
            case "A-": return 3.7;
            case "B+": return 3.3;
            case "B": return 3.0;
            case "B-": return 2.7;
            case "C+": return 2.3;
            case "C": return 2.0;
            case "C-": return 1.7;
            case "D+": return 1.3;
            case "D": return 1.0;
            case "F": return 0.0;
            default: return 0.0;
        }
    }

    public static class Course {
        private String courseCode;
        private String courseName;
        private double credit;
        private String grade;
        private String teacher1;
        private String teacher2;

        public Course(String courseCode, String courseName, double credit, String grade, String teacher1, String teacher2) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.credit = credit;
            this.grade = grade;
            this.teacher1 = teacher1;
            this.teacher2 = teacher2;
        }

        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public double getCredit() { return credit; }
        public void setCredit(double credit) { this.credit = credit; }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }

        public String getTeacher1() { return teacher1; }
        public void setTeacher1(String teacher1) { this.teacher1 = teacher1; }

        public String getTeacher2() { return teacher2; }
        public void setTeacher2(String teacher2) { this.teacher2 = teacher2; }
    }
}