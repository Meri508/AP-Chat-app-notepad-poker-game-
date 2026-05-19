import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatApp extends Application {
    private final VBox chatBox = new VBox(10);
    private final ScrollPane scrollPane = new ScrollPane(chatBox);
    private final TextField nameField = new TextField("User");
    private final TextArea messageArea = new TextArea();
    private final Label imageStatus = new Label("No image");

    private File selectedImage;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(createHeader());
        root.setCenter(createChatArea());
        root.setBottom(createComposer(stage));

        Scene scene = new Scene(root, 760, 620);
        scene.getStylesheets().add(createStylesheet());

        stage.setTitle("JavaFX Chat App");
        stage.setMinWidth(620);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    private HBox createHeader() {
        Label title = new Label("Chat Room");
        title.getStyleClass().add("header-title");

        Label status = new Label("online");
        status.getStyleClass().add("header-status");

        VBox titleBox = new VBox(1, title, status);

        nameField.setPromptText("Your name");
        nameField.getStyleClass().add("name-field");
        nameField.setMaxWidth(180);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, titleBox, spacer, nameField);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.getStyleClass().add("header");
        return header;
    }

    private ScrollPane createChatArea() {
        chatBox.setPadding(new Insets(16));
        chatBox.getStyleClass().add("chat-box");
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("chat-scroll");
        return scrollPane;
    }

    private HBox createComposer(Stage stage) {
        messageArea.setPromptText("Type a message");
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(2);
        messageArea.getStyleClass().add("message-input");

        Button imageButton = new Button("+");
        imageButton.setTooltip(new javafx.scene.control.Tooltip("Choose image"));
        imageButton.getStyleClass().add("round-button");
        imageButton.setOnAction(event -> chooseImage(stage));

        Button clearButton = new Button("x");
        clearButton.setTooltip(new javafx.scene.control.Tooltip("Clear selected image"));
        clearButton.getStyleClass().add("round-button");
        clearButton.setOnAction(event -> clearImage());

        Button sendButton = new Button("Send");
        sendButton.setTooltip(new javafx.scene.control.Tooltip("Send message"));
        sendButton.getStyleClass().add("send-button");
        sendButton.setOnAction(event -> sendMessage());

        imageStatus.getStyleClass().add("image-status");

        HBox composer = new HBox(8, imageButton, clearButton, messageArea, imageStatus, sendButton);
        composer.setAlignment(Pos.CENTER);
        composer.setPadding(new Insets(10, 12, 10, 12));
        composer.getStyleClass().add("composer");
        HBox.setHgrow(messageArea, Priority.ALWAYS);
        return composer;
    }

    private void chooseImage(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif")
        );

        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedImage = file;
            imageStatus.setText(file.getName());
        }
    }

    private void clearImage() {
        selectedImage = null;
        imageStatus.setText("No image");
    }

    private void sendMessage() {
        String sender = nameField.getText().trim();
        String text = messageArea.getText().trim();

        if (sender.isEmpty()) {
            showAlert("Enter your name.");
            return;
        }

        if (text.isEmpty() && selectedImage == null) {
            showAlert("Type a message or choose an image.");
            return;
        }

        chatBox.getChildren().add(createMessageBubble(sender, text, selectedImage));
        messageArea.clear();
        clearImage();

        chatBox.layout();
        scrollPane.setVvalue(1.0);
    }

    private HBox createMessageBubble(String sender, String text, File imageFile) {
        Label senderLabel = new Label(sender);
        senderLabel.getStyleClass().add("sender-label");

        VBox bubble = new VBox(5, senderLabel);
        bubble.getStyleClass().add("message-bubble");
        bubble.setMaxWidth(430);

        if (!text.isEmpty()) {
            Label messageText = new Label(text);
            messageText.setWrapText(true);
            messageText.getStyleClass().add("message-text");
            bubble.getChildren().add(messageText);
        }

        if (imageFile != null) {
            ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(300);
            imageView.setFitHeight(220);
            imageView.getStyleClass().add("chat-image");
            bubble.getChildren().add(imageView);
        }

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-label");
        timeLabel.setMaxWidth(Double.MAX_VALUE);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        bubble.getChildren().add(timeLabel);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Chat App");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String createStylesheet() {
        String css = """
                .root {
                    -fx-font-family: "Segoe UI";
                    -fx-font-size: 14px;
                }

                .header {
                    -fx-background-color: #075E54;
                }

                .header-title {
                    -fx-text-fill: white;
                    -fx-font-size: 21px;
                    -fx-font-weight: bold;
                }

                .header-status {
                    -fx-text-fill: #CDEBE5;
                    -fx-font-size: 12px;
                }

                .name-field {
                    -fx-background-color: white;
                    -fx-background-radius: 16px;
                    -fx-border-radius: 16px;
                    -fx-padding: 7px 12px;
                }

                .chat-scroll,
                .chat-scroll > .viewport {
                    -fx-background-color: #ECE5DD;
                }

                .chat-box {
                    -fx-background-color: #ECE5DD;
                }

                .composer {
                    -fx-background-color: #F7F7F7;
                }

                .message-input {
                    -fx-background-color: white;
                    -fx-background-radius: 18px;
                    -fx-border-color: transparent;
                    -fx-padding: 7px 10px;
                }

                .round-button {
                    -fx-background-color: white;
                    -fx-background-radius: 21px;
                    -fx-min-width: 42px;
                    -fx-min-height: 42px;
                    -fx-font-size: 17px;
                    -fx-font-weight: bold;
                    -fx-cursor: hand;
                }

                .send-button {
                    -fx-background-color: #25D366;
                    -fx-background-radius: 21px;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-min-width: 68px;
                    -fx-min-height: 42px;
                    -fx-cursor: hand;
                }

                .image-status {
                    -fx-text-fill: #5F5F5F;
                    -fx-font-size: 12px;
                    -fx-max-width: 120px;
                }

                .message-bubble {
                    -fx-background-color: #DCF8C6;
                    -fx-background-radius: 16px;
                    -fx-padding: 9px 12px 8px 12px;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 1);
                }

                .sender-label {
                    -fx-text-fill: #165A4E;
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                }

                .message-text {
                    -fx-text-fill: #1F1F1F;
                    -fx-font-size: 14px;
                }

                .time-label {
                    -fx-text-fill: #606060;
                    -fx-font-size: 11px;
                }
                """;

        return "data:text/css," + css
                .replace("\n", "%0A")
                .replace(" ", "%20")
                .replace("#", "%23")
                .replace("\"", "%22");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
