package com.skaliy.netty.server.fxapp;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import com.skaliy.netty.server.modules.FileConnectionDB;
import com.skaliy.ns.Server;
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

    @FXML
    private JFXTextArea textAreaLogs;

    public void initialize() {

        final Server[] server = {null};
        final Thread[] thread = {null};

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

                server[0].setTextAreaLogs(textAreaLogs);

                thread[0] = new Thread(server[0]);
                thread[0].start();


                if (server[0].getDb().isConnected()) {
                    labelStatus.setText("Подключение установлено!");
                    buttonStart.setText("Отключить");
                    textAreaLogs.appendText("[SERVER] - start\n");
                } else {
                    textAreaLogs.appendText("Упс! Возникла проблема.\n");
                }

            } else {
                server[0].getDb().closeConnection();
                server[0] = null;
                thread[0].stop();
                thread[0] = null;
                buttonStart.setText("Запустить");
                labelStatus.setText("Соединение закрыто!");
                textAreaLogs.appendText("[SERVER] - shutdown\n");
            }

        });

        Main.stage.setOnCloseRequest(event -> {
            if (server[0] != null) {
                server[0].getDb().closeConnection();
                server[0] = null;
            }
            if (thread[0] != null) {
                thread[0].stop();
                thread[0] = null;
            }
            if (server[0] == null && thread[0] == null) {
                textAreaLogs.appendText("[SERVER] - shutdown\n");
            } else textAreaLogs.appendText("[SERVER] - did not shutdown\n");
        });

    }

}