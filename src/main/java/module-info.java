module com.example.gpa_calculator2207083 {
    requires javafx.controls;
    requires javafx.fxml;

    requires javafx.base;
    requires javafx.graphics;
    requires java.sql;

    opens com.example.gpa_calculator2207083 to javafx.fxml;
    exports com.example.gpa_calculator2207083;
}