package com.uniqueapps.musemix;

import atlantafx.base.theme.*;
import com.pixelduke.window.MacThemeWindowManager;
import com.pixelduke.window.ThemeWindowManager;
import com.pixelduke.window.ThemeWindowManagerFactory;
import com.pixelduke.window.Win11ThemeWindowManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public class MusemixApplication extends Application {

    private HomeController homeController;

    @Override
    public void start(Stage stage) throws IOException {
        ThemeWindowManager themeWindowManager = ThemeWindowManagerFactory.create();
        Font.loadFont(Objects.requireNonNull(MusemixApplication.class.getResource("Inter-SemiBold.ttf")).toExternalForm().replace("%20", " "), 14);
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(MusemixApplication.class.getResource("home.fxml")));
        Scene scene = new Scene(loader.load(), 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(MusemixApplication.class.getResource("style.css")).toExternalForm());
        homeController = loader.getController();
        stage.setTitle("Musemix");
        stage.initStyle(StageStyle.UNIFIED);
        stage.setScene(scene);
        stage.getIcons().add(new Image(Objects.requireNonNull(MusemixApplication.class.getResourceAsStream("logo.png"))));

        stage.setOnShown(event -> {
            themeWindowManager.setDarkModeForWindowFrame(stage, true);
            if (themeWindowManager instanceof Win11ThemeWindowManager win11ThemeWindowManager) {
                win11ThemeWindowManager.setWindowBackdrop(stage, Win11ThemeWindowManager.Backdrop.MICA);
                scene.getRoot().setStyle("-fx-background-color: transparent;");
                scene.setFill(Color.TRANSPARENT);
            } else if (themeWindowManager instanceof MacThemeWindowManager macThemeWindowManager) {
                macThemeWindowManager.setWindowFrameAppearance(stage, MacThemeWindowManager.Backdrop.NSAppearanceNameVibrantDark);
                scene.getRoot().setStyle("-fx-background-color: transparent;");
                scene.setFill(Color.TRANSPARENT);
            }
        });

        stage.show();
    }

    @Override
    public void stop() {
        if (homeController != null) {
            homeController.shutdown();
        }
    }

    public static void main(String[] args) {
        //System.setProperty("java.home", ".");
        launch();
    }
}