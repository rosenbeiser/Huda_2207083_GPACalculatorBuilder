module com.example.gpa_calculator2207083 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires javafx.base;
    requires javafx.graphics;

    opens com.example.gpa_calculator2207083 to javafx.fxml;
    exports com.example.gpa_calculator2207083;
}