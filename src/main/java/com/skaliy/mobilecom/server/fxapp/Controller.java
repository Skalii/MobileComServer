package com.skaliy.mobilecom.server.fxapp;

import com.jfoenix.controls.JFXButton;
import com.skaliy.mobilecom.server.netty.Server;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Controller {

    @FXML
    private JFXButton buttonStart;

    @FXML
    private Label labelStatus;

    public void initialize() {

        final Server[] server = {null};
        final Thread[] thread = {null};

        buttonStart.setOnAction(event -> {

            if (server[0] == null) {
                server[0] = new Server(
                        7777,
                        "ec2-79-125-13-42.eu-west-1.compute.amazonaws.com:5432/d81947dprpqjnd?sslmode=require",
                        "nesmvtsalawsxv",
                        "a1c0a6be9d1e3a3265e71b393cce3253858e7108282575d89e6ebe3bf2e25276");

                thread[0] = new Thread(server[0]);
                thread[0].start();

                if (server[0].getDb().isConnected()) {
                    labelStatus.setText("Подключение установлено!");
                    buttonStart.setText("Отключить");
                } else {
                    labelStatus.setText("Упс! Возникла проблема.");
                }

            } else {
                server[0].getDb().closeConnection();
                server[0] = null;
                thread[0].stop();
                thread[0] = null;
                buttonStart.setText("Запустить");
                labelStatus.setText("Отключено");
            }

        });

    }
}