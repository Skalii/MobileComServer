package com.skaliy.mobilecom.server.fxapp;

import com.jfoenix.controls.JFXButton;
import com.skaliy.dbc.dbms.PostgreSQL;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Controller {

    @FXML
    private JFXButton buttonStart;

    @FXML
    private Label labelStatus;

    private PostgreSQL db;

    public void initialize() {

        buttonStart.setOnAction(event -> {

            db = new PostgreSQL(
                    "ec2-79-125-13-42.eu-west-1.compute.amazonaws.com:5432/d81947dprpqjnd?sslmode=require",
                    "nesmvtsalawsxv",
                    "a1c0a6be9d1e3a3265e71b393cce3253858e7108282575d89e6ebe3bf2e25276");

            labelStatus.setText(
                    db.isConnect()
                            ? "Подключение установлено!"
                            : "Упс! Возникла проблема.");

        });

    }
}
