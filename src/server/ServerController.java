package server;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;

public class ServerController implements Initializable {
    @FXML
    public TextArea inputArea;
    @FXML
    public TextField port;
    @FXML
    public WebView browser;
    @FXML
    public ListView<String> listView;

    static Settings settings;
    private static StringBuffer html;
    public static ArrayList<String> list;

    private Server server;

    // Статический блок инициализации
    static {
        settings = new Settings();
        html = new StringBuffer();
        list = new ArrayList<>();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settings.loadFromXml();
        port.setText(String.valueOf(settings.getPort()));
        server = new Server(this);
    }

    public void message(String message) {
        list.add(message);
        showInPage();
    }

    /**
     * Метод отображения сообщений в окне сервера
     */
    private void showInPage() {
       browser.getEngine().setUserStyleSheetLocation(getClass().getResource("/server/style.css").toString());
        html.delete(0, html.length())
                .append("<!DOCTYPE html><html lang=\"ru\"><head><meta charset=\"UTF-8\"></head><body>");
        for (String line : list) html.append("<p>").append(line).append("</p>");

        html.append("</body>");
        browser.getEngine().loadContent(html.toString());
        browser.getEngine().reload();
    }

    /**
     * Метод сохраняет номер порта для подключения
     * @param actionEvent
     */
    public void onSaveSettings(ActionEvent actionEvent) {
        settings.setPort(Integer.parseInt(port.getText()));
        settings.saveToXml();
    }

    /**
     * @return текущие дата и время для отображения при отправке сообщения
     * формат: ГГГГ-ММ-ДД ЧЧ:ММ:СС:ССС
     */
    private String getTimestamp() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        int mm = cal.get(Calendar.MINUTE);
        int ss = cal.get(Calendar.SECOND);
        return String.format("%d-%02d-%02d %02d:%02d:%02d", year, month, day, hh, mm, ss);
    }

    /**
     * формат: ГГГГ-ММ-ДД ЧЧ:ММ:СС
     * @return время и дата для записи в историю чата
     */
    private String timestamp() {
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp timeStamp = new java.sql.Timestamp(calendar.getTime().getTime());
        return timeStamp.toString();
    }

    private void sendMessage() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<font color=\"grey\">[" + getTimestamp() + "]</font>&nbsp;")
                .append("<font color=\"red\">[SERVER]</font>&nbsp;::&nbsp;")
                .append(inputArea.getText());
        server.broadcastMsg(stringBuilder.toString());
        server.getAuthService().saveToHistory(timestamp(), "[SERVER]", "", "broadcast", stringBuilder.toString());
        inputArea.clear();
        inputArea.requestFocus();
    }

    public void onSendMessageToAll(ActionEvent actionEvent) throws IOException {
        sendMessage();
    }

    public void onEnterSendMessage(KeyEvent keyEvent) throws IOException {
        if (keyEvent.getCode().equals(KeyCode.ENTER))
            sendMessage();
    }
}
