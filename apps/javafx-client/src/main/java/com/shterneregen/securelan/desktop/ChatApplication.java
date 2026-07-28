package com.shterneregen.securelan.desktop;

import com.shterneregen.securelan.desktop.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class ChatApplication extends Application {
    private MainView view;

    @Override
    public void start(Stage stage) {
        view = new MainView();

        Scene scene = new Scene(view.createContent(), 1360, 860);
        stage.setTitle("SecureLanSuite Chat");
        stage.getIcons().add(
                new javafx.scene.image.Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream("/icons/app-icon.png")
                        )
                )
        );
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> shutdownView());
    }

    @Override
    public void stop() {
        shutdownView();
    }

    private void shutdownView() {
        if (view != null) {
            view.shutdown();
            view = null;
        }
    }
}