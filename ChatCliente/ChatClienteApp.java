package ChatCliente;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ChatClienteApp extends Application {

    @Override
    public void start(Stage stage) {

        Label texto = new Label(
                "Chat de Segurança da Informação"
        );

        StackPane root = new StackPane(texto);

        Scene scene = new Scene(root, 600, 400);

        stage.setTitle("Chat");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}