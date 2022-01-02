package client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Calendar;

import static javafx.scene.control.SelectionMode.*;

public class ClientController implements Initializable {
    @FXML
    public TextArea inputArea;
    @FXML
    public WebView browser;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    public ListView<String> listView;
    @FXML
    public TextField loginInput;
    @FXML
    public PasswordField passwordInput;
    @FXML
    public HBox loginPanel;
    @FXML
    public HBox chatPanel;
    @FXML
    public HBox registerPanel;
    @FXML
    public HBox settingsPanel;
    @FXML
    public Button buttonConnect;
    @FXML
    public Button buttonSendMessage;
    @FXML
    public Button buttonBack;
    @FXML
    public Button buttonSaveSettings;
    @FXML
    public TextField newLoginInput;
    @FXML
    public PasswordField newPassInput;
    @FXML
    public TextField newUserInput;
    @FXML
    public MenuBar menuBar;
    @FXML
    public TextField settingsIpAddress;
    @FXML
    public TextField settingsPort;

    public static Settings settings = new Settings();
    public static Control control = new Control();
    private static StringBuffer html = new StringBuffer();
    private static ArrayList<String> list = new ArrayList<String>();

    private Thread thread;
    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;
    private static String str = null;
    private boolean isAuthorized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        settings.loadFromXml();
        control.loadFromXml();

        listView.getSelectionModel().setSelectionMode(MULTIPLE);
        colorPicker.setValue(Color.BLACK);

        clientThread();
        controlListener();
    }

    /**
     * Метод обработки служебных сообщений от сервера
     */
    private void clientThread() {
        thread = new Thread(() -> {
            try {
                socket = new Socket(settings.getServer(), settings.getPort());
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                // авторизация пользователя
                while (true) {
                    str = in.readUTF();

                    if (str.startsWith("/authok|")) {
                        settings.setUser(str.split("\\|")[1]);  // получаем ник клиента и записываем его в настройки
                        settings.saveToXml();
                        settings.loadFromXml();
                        Platform.runLater(() -> setAuthorized(true));
                        Platform.runLater(() -> MainClient.mainStage.setTitle("Chatter: " + settings.getUser()));
                        break;
                    }

                    if (str.startsWith("/alert|")) {
                        String[] string = str.split("\\|", 2);
                        if(string[1].equals("Registration is complete!")) {
                            str = str.replace("/alert|", "");
                            Platform.runLater(() -> {
                                showAlert(str);
                                showLoginPanel();
                            });
                        }
                        str = str.replace("/alert|", "");
                        Platform.runLater(() -> showAlert(str));
                    }
                }

                // основная часть
                while (true) {
                    str = in.readUTF();

                    if (str.startsWith("/echo|")) {
                        str = str.replace("/echo|", "");
                        System.out.println("[CLIENT] получил от сервера: " + str);
                        Platform.runLater(() -> message(str));
                    }

                    if (str.startsWith("/online|")) {
                        str = str.replace("/online|", "");
                        ArrayList<String> list = new ArrayList<String>();
                        list.addAll(Arrays.stream(str.split("\\|")).collect(Collectors.toList()));
                        ObservableList<String> items = FXCollections.observableArrayList(list);
                        Platform.runLater(() -> listView.setItems(items));
                    }

                    if (str.startsWith("/newnick|")) {
                        str = str.replace("/newnick|", "");
                        settings.setUser(str);
                        settings.saveToXml();
                        Platform.runLater(() -> MainClient.mainStage.setTitle("Chatter [" + settings.getUser() + "]"));
                    }

                    if (str.startsWith("/alert|")) {
                        str = str.replace("/alert|", "");
                        Platform.runLater(() -> showAlert(str));
                    }
                }
            } catch (EOFException e) {
                // клиент покидает чат
            } catch (IOException e) {
                Platform.runLater(() -> message("[CHAT]* No connection to the server."));
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void message(String message) {
        list.add(message);
        showInPage();
    }

    /**
     * Отображение сообщения в окне чата
     */
    private void showInPage() {
        browser.getEngine().setUserStyleSheetLocation(getClass().getResource("/client/style.css").toString());
        html.delete(0, html.length())
                .append("<!DOCTYPE html>").append("<html lang=\"ru\">")
                .append("<head>").append("<meta charset=\"UTF-8\">").append("</head>")
                .append("<body>");
        for (String line : list) html.append("<p>").append(line).append("</p>");
        html.append("</body>");
        browser.getEngine().loadContent(html.toString());
        browser.getEngine().reload();
    }

    public void onButtonSendMessageClick(ActionEvent actionEvent) {
        sendMessage();
    }

    public void onEnterSendMessage(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) sendMessage();
    }

    /**
     * Метод отправки сообщений (личных и общих для всех клиентов)
     */
    private void sendMessage() {
        // если выделены клиенты для отправки сообщения
        ObservableList<String> selectedItems = listView.getSelectionModel().getSelectedItems();
        for (String item : selectedItems) {
            if (item != null) {
                try {
                    StringBuilder privatMsg = new StringBuilder();
                    privatMsg.append("<font color=\"grey\">[" + timestamp() + "]</font>&nbsp;")
                            .append("<font color=\"blue\">[" + settings.getUser() + "]</font>&nbsp;->&nbsp;[" + item + "]::&nbsp;")
                            .append(inputArea.getText());
                    out.writeUTF("/msgpr|" + timestamp() + "|" + item + "|" + privatMsg.toString());  // отправляем на сервер
                    message(privatMsg.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // если не выделено ни одного клиента
        if (listView.getSelectionModel().getSelectedIndex() == -1) {
            try {
                Calendar calendar = Calendar.getInstance();
                java.sql.Timestamp timeStamp = new java.sql.Timestamp(calendar.getTime().getTime());

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<font color=\"grey\">[" + timestamp() + "]</font>&nbsp;")
                        .append("<font color=\"blue\">[" + settings.getUser() + "]</font>&nbsp;::&nbsp;")
                        .append(inputArea.getText());
                out.writeUTF("/msgbc|" + stringBuilder.toString());
            } catch (IOException e) {
                message("[CHAT] 'sendMessage' : " + e);
            }
        }
        inputArea.clear();
        inputArea.requestFocus();

        listView.getSelectionModel().select(-1);
    }

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!this.isAuthorized) {
            showLoginPanel();
        } else {
            showChatPanel();
        }
    }

    /**
     * Метод выводит предупреждения в отдельном окне
     * @param content - текст предупреждения
     */
    private void showAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Авторизация пользователя через проверку логина и пароля в БД
     */
    private void authorisation() {
        String login = loginInput.getText();
        String pass = passwordInput.getText();
        try {
            out.writeUTF("/auth|" + login + "|" + pass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        loginInput.clear();
        passwordInput.clear();
    }

    public void onConnect(ActionEvent actionEvent) {
        authorisation();
    }

    static void disconnect() {
        try {
            out.writeUTF("/disconnect");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public void onSaveHistory(ActionEvent actionEvent) {
        FileChooser saver = new FileChooser();
        saver.setTitle("Save file as...");
        saver.setInitialFileName("history.html");
        File file = saver.showSaveDialog(MainClient.mainStage);
        if (file != null) {
            try {
                FileWriter out = new FileWriter(file);
                out.write(html.toString());
                out.close();
            } catch (IOException e) {
                message("[CHAT] 'FileWriter' " + e.toString());
            }
        }
    }

    public void onShowUsersOnline(ActionEvent actionEvent) {
        whoIsOnline();
    }

    private void whoIsOnline() {
        try {
            out.writeUTF("/whoisonline");
            System.out.println("/whoisonline");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClearChatWindow(ActionEvent actionEvent) {
        list.clear();
        showInPage();
    }

    public void onReconnectToServer(ActionEvent actionEvent) {
        try {
            out.writeUTF("/disconnect");
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        thread.interrupt();
        setAuthorized(false);
        settings.loadFromXml();
        clientThread();
    }

    public void onButtonChangeName(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change name");
        dialog.setHeaderText("You are to change the display name in the chat");
        dialog.setContentText("New name: ");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent((n) -> {
            changeName(n);
        });
    }

    private void changeName(String newName) {
        try {
            out.writeUTF("/changenick|" + newName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onRequestGetHistory(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Get history");
        dialog.setHeaderText("How many last days to download the history from the server");
        dialog.setContentText("Days:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent((n) -> {
            getHistory(n);
        });
    }

    private void getHistory(String days) {
        try {
            System.out.println("/gethistory|" + days);
            out.writeUTF("/gethistory|" + days);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onButtonSettings(ActionEvent actionEvent) {
        showSettingsPanel();
    }

    public void onRegistration(ActionEvent actionEvent) {
        String login = newLoginInput.getText();
        String pass = newPassInput.getText();
        String name = newUserInput.getText();
        try {
            String request = "/registration|" + login + "|" + pass + "|" + name;
            out.writeUTF(request);
            System.out.println(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void controlListener() {
        inputArea.textProperty().addListener((observableValue, oldValue, newValue) -> {
            String controlWord = control.getControlWord();
            if(newValue.toLowerCase().contains(controlWord)) {
                try {
                    out.writeUTF("/controlClient|" + controlWord);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onSaveSettings(ActionEvent actionEvent) {
        settings.setServer(settingsIpAddress.getText());
        settings.setPort(Integer.parseInt(settingsPort.getText()));
        settings.saveToXml();
        showChatPanel();
    }

    private void showSettingsPanel() {
        settingsIpAddress.setText(settings.getServer());
        settingsPort.setText(String.valueOf(settings.getPort()));

        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
        settingsPanel.setVisible(true);
        settingsPanel.setManaged(true);
        buttonSaveSettings.requestFocus();
    }

    private void showChatPanel() {
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        menuBar.setVisible(true);
        menuBar.setManaged(true);
        chatPanel.setVisible(true);
        chatPanel.setManaged(true);
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
        settingsPanel.setVisible(false);
        settingsPanel.setManaged(false);
        buttonSendMessage.requestFocus();
    }

    private void showLoginPanel() {
        loginPanel.setVisible(true);
        loginPanel.setManaged(true);
        menuBar.setVisible(false);
        menuBar.setManaged(false);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
        settingsPanel.setVisible(false);
        settingsPanel.setManaged(false);
        buttonConnect.requestFocus();
    }

    private void showRegistrationPanel() {
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        menuBar.setVisible(false);
        menuBar.setManaged(false);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        registerPanel.setVisible(true);
        registerPanel.setManaged(true);
        settingsPanel.setVisible(false);
        settingsPanel.setManaged(false);
        buttonBack.requestFocus();
    }

    private String timestamp() { // формат: ГГГГ-ММ-ДД ЧЧ:ММ:СС:ССС
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp timeStamp = new java.sql.Timestamp(calendar.getTime().getTime());
        return timeStamp.toString();
    }

    public void onSelectAll(ActionEvent actionEvent) {
        listView.getSelectionModel().selectAll();
    }

    public void onDeselectAll(ActionEvent actionEvent) {
        listView.getSelectionModel().select(-1);
    }

    /**
     * Изменение шрифта вводимого сообщения
     * @param teg - опредедление изменения
     * @param a - поле ввода сообщения
     * @param color - цвет, определяемый для шрифта
     * @return
     */
    private String teggingText(String teg, TextArea a, String color) {
        StringBuilder str = new StringBuilder();
        str.append(a.getText().substring(0, a.getSelection().getStart()));
        if (teg == "font") {
            str.append("<" + teg + " color=" + color + ">");
        } else {
            str.append("<" + teg + ">");
        }
        str.append(a.getText().substring(a.getSelection().getStart(), a.getSelection().getEnd()))
                .append("</" + teg + ">")
                .append(a.getText().substring(a.getSelection().getEnd(), a.getLength()));
        return str.toString();
    }

    public void onMakeTextBold(ActionEvent actionEvent) {
        inputArea.setText(teggingText("b", inputArea, null));
    }

    public void onMakeTextItalic(ActionEvent actionEvent) {
        inputArea.setText(teggingText("i", inputArea, null));
    }

    public void onMakeTextUnderline(ActionEvent actionEvent) {
        inputArea.setText(teggingText("u", inputArea, null));
    }

    public void onMakeTextColor(ActionEvent actionEvent) {
        String color = "#" + colorPicker.getValue().toString().substring(2, 8);
        inputArea.setText(teggingText("font", inputArea, color));
    }

    public void onShowRegisterPanel(ActionEvent actionEvent) {
        showRegistrationPanel();
    }

    public void onShowLoginPanel(ActionEvent actionEvent) {
        showLoginPanel();
    }

    public void onButtonExit(ActionEvent actionEvent) {
        disconnect();
    }
}
