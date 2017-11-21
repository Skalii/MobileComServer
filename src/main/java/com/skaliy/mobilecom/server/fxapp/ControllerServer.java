package com.skaliy.mobilecom.server.fxapp;

import com.skaliy.mobilecom.server.connection.FileConnection;
import com.skaliy.mobilecom.server.server.Server;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class ControllerServer {

    @FXML
    private MenuItem menuHideTray, menuExit, menuAbout;

    @FXML
    private TextArea textAreaLogs;

    @FXML
    private Label labelStatus;

    @FXML
    private Button buttonStart;

    public void initialize() {

        menuExit.setOnAction(event -> {
            MainServer.getSystemTray().remove(MainServer.getTrayIcon());
            System.exit(0);
        });
        menuHideTray.setOnAction(event -> {
            MainServer.getStage().hide();
        });
        menuAbout.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("О программе");
            alert.setHeaderText("Курсовая работа");
            alert.getButtonTypes().setAll(ButtonType.OK);

            Label label = new Label("Студента гр.341(б)\nСкалий Дмитрия");
            label.setWrapText(true);
            label.setAlignment(Pos.CENTER);
            label.setPrefHeight(50);
            label.setPrefWidth(220);
            label.setLayoutY(15);
            label.setLayoutX(15);

            AnchorPane pane = new AnchorPane(label);
            pane.setPrefHeight(100);
            pane.setPrefWidth(250);

            alert.getDialogPane().setContent(pane);
            alert.showAndWait();
        });

        final Server[] server = {null};
        final Thread[] thread = {null};

        buttonStart.setOnAction(event -> {

            FileConnection file = new FileConnection("db.txt");
            BufferedReader dataConnection;

            try {
                dataConnection = file.read();
            } catch (FileNotFoundException e) {
                textAreaLogs.appendText("Файл с параметрами подключения к БД не существует!\n" +
                        "Создайте файл \"server.txt\" со значениями host, user, password.\n");
                return;
            }

            if (server[0] == null) {
                try {
                    server[0] = new Server(
                            7777,
                            dataConnection.readLine(),
                            dataConnection.readLine(),
                            dataConnection.readLine());
                } catch (IOException | SQLException | ClassNotFoundException e) {
                    textAreaLogs.appendText("Упс! Что-то пошло не так.\n"
                            + "Проверьте параметы подключения к БД в файле \"server.txt\"!\n");
                    server[0] = null;
                    return;
                }

                server[0].setTextAreaLogs(textAreaLogs);

                thread[0] = new Thread(server[0]);
                thread[0].start();

                while (!server[0].getDb().isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                labelStatus.setText("Подключение установлено!");
                buttonStart.setText("Отключить");
                textAreaLogs.appendText("[SERVER] - start\n");

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

        MainServer.getStage().setOnCloseRequest(event -> {

            MainServer.getSystemTray().remove(MainServer.getTrayIcon());
            System.exit(0);

            if (server[0] != null) {
                server[0].getDb().closeConnection();
                server[0] = null;
            }
            if (thread[0] != null) {
                thread[0].stop();
                thread[0] = null;
            }
        });
    }

}