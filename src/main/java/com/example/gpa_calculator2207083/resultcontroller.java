package com.example.gpa_calculator2207083;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class resultcontroller {

    @FXML
    private Label gpaLabel;

    @FXML
    private Label totalCreditsLabel;

    @FXML
    private TableView<gpainputcontroller.Course> resultTable;

    @FXML
    private TableColumn<gpainputcontroller.Course, String> resultCourseCode;

    @FXML
    private TableColumn<gpainputcontroller.Course, String> resultCourseName;

    @FXML
    private TableColumn<gpainputcontroller.Course, Double> resultCourseCredit;

    @FXML
    private TableColumn<gpainputcontroller.Course, String> resultGrade;

    @FXML
    private TableColumn<gpainputcontroller.Course, String> resultTeacher1;

    @FXML
    private TableColumn<gpainputcontroller.Course, String> resultTeacher2;

    @FXML
    public void initialize() {
        resultCourseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        resultCourseName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        resultCourseCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));
        resultGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        resultTeacher1.setCellValueFactory(new PropertyValueFactory<>("teacher1"));
        resultTeacher2.setCellValueFactory(new PropertyValueFactory<>("teacher2"));
        resultTable.setSelectionModel(null);
    }

    public void setResultData(double gpa, double totalCredits, ObservableList<gpainputcontroller.Course> courseList) {
        gpaLabel.setText(String.format("%.2f", gpa));
        totalCreditsLabel.setText(String.format("%.1f", totalCredits));
        resultTable.setItems(courseList);
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) gpaLabel.getScene().getWindow();
        stage.close();
    }
}