package com.skaliy.mobilecom.server.fxapp;

import com.jfoenix.controls.JFXButton;
import com.skaliy.mobilecom.server.modules.FileConnectionDB;
import com.skaliy.mobilecom.server.netty.Server;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Controller {

    @FXML
    private JFXButton buttonStart;

    @FXML
    private Label labelStatus;

    public void initialize() {

        final Server[] server = {null};

        buttonStart.setOnAction(event -> {

            FileConnectionDB file = new FileConnectionDB("server.txt");
            BufferedReader dataConnection = null;

            try {
                dataConnection = file.read();
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(
                        JOptionPane.getRootFrame(),
                        "Файл с параметрами подключения не существует!\n" +
                                "Создайте файл \"server.txt\" со значениями host, user, password.");
                return;
            }

            if (server[0] == null) {
                try {
                    server[0] = new Server(
                            7777,
                            dataConnection.readLine(),
                            dataConnection.readLine(),
                            dataConnection.readLine());
                } catch (IOException e) {
                    server[0] = null;
                    return;
                }

                server[0].run();

                if (server[0].getDb().isConnected()) {
                    labelStatus.setText("Подключение установлено!");
                    buttonStart.setText("Отключить");
                    System.out.println("[SERVER] - start");
                } else {
                    labelStatus.setText("Упс! Возникла проблема.");
                }

            } else {
                server[0].getDb().closeConnection();
                server[0] = null;
                buttonStart.setText("Запустить");
                labelStatus.setText("Соединение закрыто!");
                System.out.println("[SERVER] - shutdown");
            }

        });

    }

}