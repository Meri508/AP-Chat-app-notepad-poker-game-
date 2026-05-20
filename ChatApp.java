import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatApp extends Application {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private final VBox chatBox = new VBox(10);
    private final ScrollPane scrollPane = new ScrollPane(chatBox);
    private final TextField nameField = new TextField();
    private final TextArea messageArea = new TextArea();
    private final Label imageStatus = new Label("No image");
    private final Label connectionStatus = new Label("connecting...");

    private ObjectOutputStream output;
    private File selectedImage;

    @Override
    public void start(Stage stage) {
        askForUsername();

        BorderPane root = new BorderPane();
        root.setTop(createHeader());
        root.setCenter(createChatArea());
        root.setBottom(createComposer(stage));

        Scene scene = new Scene(root, 760, 620);
        scene.getStylesheets().add(createStylesheet());

        stage.setTitle("JavaFX Multi-Client Chat");
        stage.setMinWidth(620);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();

        connectToServer();
    }

    private void askForUsername() {
        String defaultName = "User" + (int) (Math.random() * 1000);
        TextInputDialog dialog = new TextInputDialog(defaultName);
        dialog.setTitle("Chat Name");
        dialog.setHeaderText("Enter your chat name");
        dialog.setContentText("Name:");

        String username = dialog.showAndWait()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .orElse(defaultName);
        nameField.setText(username);
    }

    private HBox createHeader() {
        Label title = new Label("Chat Room");
        title.getStyleClass().add("header-title");

        connectionStatus.getStyleClass().add("header-status");

        VBox titleBox = new VBox(1, title, connectionStatus);

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

    private void connectToServer() {
        Thread thread = new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                Platform.runLater(() -> connectionStatus.setText("online"));

                while (true) {
                    ChatMessage message = (ChatMessage) input.readObject();
                    Platform.runLater(() -> addMessage(message));
                }
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    connectionStatus.setText("offline");
                    showAlert("Cannot connect to server. Start ChatServer first.");
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
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

        if (output == null) {
            showAlert("You are not connected. Start ChatServer first.");
            return;
        }

        try {
            byte[] imageData = selectedImage == null ? null : Files.readAllBytes(selectedImage.toPath());
            String imageName = selectedImage == null ? null : selectedImage.getName();
            ChatMessage message = new ChatMessage(0, sender, text, imageName, imageData, LocalDateTime.now());

            output.writeObject(message);
            output.flush();
            messageArea.clear();
            clearImage();
        } catch (Exception exception) {
            showAlert("Message could not be sent: " + exception.getMessage());
        }
    }

    private void addMessage(ChatMessage message) {
        boolean mine = message.sender().equals(nameField.getText().trim());

        Label senderLabel = new Label(message.sender());
        senderLabel.getStyleClass().add("sender-label");

        VBox bubble = new VBox(5, senderLabel);
        bubble.getStyleClass().add(mine ? "message-bubble-mine" : "message-bubble-other");
        bubble.setMaxWidth(430);

        if (message.text() != null && !message.text().isBlank()) {
            Label messageText = new Label(message.text());
            messageText.setWrapText(true);
            messageText.getStyleClass().add("message-text");
            bubble.getChildren().add(messageText);
        }

        if (message.hasImage()) {
            ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(message.imageData())));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(300);
            imageView.setFitHeight(220);
            bubble.getChildren().add(imageView);
        }

        Label timeLabel = new Label(message.createdAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("time-label");
        timeLabel.setMaxWidth(Double.MAX_VALUE);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        bubble.getChildren().add(timeLabel);

        HBox row = new HBox(bubble);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        chatBox.getChildren().add(row);
        chatBox.layout();
        scrollPane.setVvalue(1.0);
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
                    -fx-padding: 7px 12px;
                }
                .chat-scroll,
                .chat-scroll > .viewport,
                .chat-box {
                    -fx-background-color: #ECE5DD;
                }
                .composer {
                    -fx-background-color: #F7F7F7;
                }
                .message-input {
                    -fx-background-color: white;
                    -fx-background-radius: 18px;
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
                .message-bubble-mine {
                    -fx-background-color: #DCF8C6;
                    -fx-background-radius: 16px;
                    -fx-padding: 9px 12px 8px 12px;
                }
                .message-bubble-other {
                    -fx-background-color: white;
                    -fx-background-radius: 16px;
                    -fx-padding: 9px 12px 8px 12px;
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

record ChatMessage(
        int id,
        String sender,
        String text,
        String imageName,
        byte[] imageData,
        LocalDateTime createdAt
) implements Serializable {
    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }
}

class Database {
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/chat_app?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public Database() {
        createDatabaseIfNeeded();
        createTableIfNeeded();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, USER, PASSWORD);
    }

    private void createDatabaseIfNeeded() {
        String sql = "CREATE DATABASE IF NOT EXISTS chat_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        try (Connection connection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            throw new RuntimeException("Database could not be created: " + exception.getMessage(), exception);
        }
    }

    private void createTableIfNeeded() {
        String sql = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    sender VARCHAR(80) NOT NULL,
                    message_text TEXT NULL,
                    image_name VARCHAR(255) NULL,
                    image_data LONGBLOB NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            throw new RuntimeException("Database table could not be created: " + exception.getMessage(), exception);
        }
    }

    public ChatMessage save(ChatMessage message) throws SQLException {
        String sql = """
                INSERT INTO messages (sender, message_text, image_name, image_data)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, message.sender());
            statement.setString(2, message.text() == null || message.text().isBlank() ? null : message.text());
            statement.setString(3, message.imageName());
            statement.setBytes(4, message.imageData());
            statement.executeUpdate();

            int id = 0;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }

            return new ChatMessage(id, message.sender(), message.text(), message.imageName(),
                    message.imageData(), LocalDateTime.now());
        }
    }

    public List<ChatMessage> loadAll() throws SQLException {
        String sql = """
                SELECT id, sender, message_text, image_name, image_data, created_at
                FROM messages
                ORDER BY id ASC
                """;

        List<ChatMessage> messages = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                messages.add(new ChatMessage(
                        resultSet.getInt("id"),
                        resultSet.getString("sender"),
                        resultSet.getString("message_text"),
                        resultSet.getString("image_name"),
                        resultSet.getBytes("image_data"),
                        createdAt.toLocalDateTime()
                ));
            }
        }
        return messages;
    }
}

class ChatServer {
    private static final int PORT = 5000;

    private final Database database = new Database();
    private final Set<ObjectOutputStream> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        new ChatServer().start();
    }

    private void start() {
        System.out.println("Chat server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                Thread clientThread = new Thread(() -> handleClient(socket));
                clientThread.setDaemon(true);
                clientThread.start();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        ObjectOutputStream output = null;

        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            clients.add(output);

            sendHistory(output);

            while (true) {
                ChatMessage received = (ChatMessage) input.readObject();
                ChatMessage saved = database.save(received);
                broadcast(saved);
            }
        } catch (EOFException ignored) {
            // The client closed the app.
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (output != null) {
                clients.remove(output);
            }
            try {
                socket.close();
            } catch (Exception ignored) {
                // The socket is already closing.
            }
        }
    }

    private void sendHistory(ObjectOutputStream output) throws SQLException {
        List<ChatMessage> history = database.loadAll();
        for (ChatMessage message : history) {
            send(output, message);
        }
    }

    private void broadcast(ChatMessage message) {
        for (ObjectOutputStream output : clients) {
            send(output, message);
        }
    }

    private void send(ObjectOutputStream output, ChatMessage message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (Exception exception) {
            clients.remove(output);
        }
    }
}
