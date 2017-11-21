package com.skaliy.mobilecom.server.fxapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainServer extends Application {

    private static SystemTray systemTray;
    private static TrayIcon trayIcon;
    private MenuItem menuOpen, menuExit;
    private PopupMenu popupMenu;
    private static Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/server.fxml"));
        primaryStage.setTitle("Управление сервером \"Салон мобильной связи\"");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Platform.setImplicitExit(false);
                try {
                    systemTray = SystemTray.getSystemTray();

                    trayIcon = new TrayIcon(ImageIO.read(new File("src/main/resources/images/icons/icon_tray_server.png")));
                    trayIcon.addActionListener(e -> {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                primaryStage.show();
                                primaryStage.setIconified(false);
                                primaryStage.toFront();
                            }
                        });
                    });

                    menuOpen = new MenuItem("Открыть");
                    menuOpen.addActionListener(e -> {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                primaryStage.show();
                                primaryStage.setIconified(false);
                                primaryStage.toFront();
                            }
                        });
                    });

                    menuExit = new MenuItem("Выход");
                    menuExit.addActionListener(e -> System.exit(0));

                    popupMenu = new PopupMenu();
                    popupMenu.add(menuOpen);
                    popupMenu.add(menuExit);

                    trayIcon.setPopupMenu(popupMenu);
                    systemTray.add(trayIcon);
                } catch (IOException | AWTException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    public static void main(String[] args) {
        launch(args);
    }

    static Stage getStage() {
        return stage;
    }

    static SystemTray getSystemTray() {
        return systemTray;
    }

    static TrayIcon getTrayIcon() {
        return trayIcon;
    }

}
