package com.example.gpa_calculator2207083;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    public void startButton(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("input-gpa.fxml"));
        String css = this.getClass().getResource("design2.css").toExternalForm();
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setWidth(700);
        stage.setHeight(700);
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
