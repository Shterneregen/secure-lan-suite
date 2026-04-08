package com.shterneregen.securelan.desktop;

import com.shterneregen.securelan.desktop.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatApplication extends Application {
    @Override
    public void start(Stage stage) {
        MainView view = new MainView();
        Scene scene = new Scene(view.createContent(), 980, 720);
        stage.setTitle("SecureLanSuite Chat MVP");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> view.shutdown());
    }
}
