/*
 * Copyright 2025 QuireBind Contributors
 *
 * This file is part of QuireBind.
 *
 * QuireBind is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QuireBind is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with QuireBind.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.maiitsoh.quirebind.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/** JavaFX Application entry point for the QuireBind desktop UI. */
public final class QuireBindApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxml = getClass().getResource(
            "/com/maiitsoh/quirebind/desktop/fxml/main-window.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        BorderPane root = loader.load();

        Scene scene = new Scene(root, 1024, 720);
        URL css = getClass().getResource(
            "/com/maiitsoh/quirebind/desktop/css/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        for (int size : new int[]{512, 256, 128, 64, 32, 16}) {
            String path = "/com/maiitsoh/quirebind/desktop/icons/quire-bind-" + size + ".png";
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in != null) {
                    primaryStage.getIcons().add(new Image(in));
                }
            }
        }

        primaryStage.setTitle("QuireBind");
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(480);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
