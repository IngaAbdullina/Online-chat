package server;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private ServerController controller;
    private ArrayList<ClientHandler> connectedClients;
    private ObservableList<String> clients;
    private AuthService authService;

    public Server(ServerController controller) {
        this.controller = controller;
        this.connectedClients = new ArrayList<>();
        connect();
    }

    /**
     * Подключение нового клиента
     */
    private void connect() {
        Thread thread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(controller.settings.getPort())) {
                this.authService = new DBService();
                this.authService.start();
                while (true) {
                    Socket socket = serverSocket.accept();
                    new ClientHandler(this, socket, this.controller);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.authService.stop();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void addClient(ClientHandler obj) throws IOException {
        connectedClients.add(obj);
        Platform.runLater(() -> showUsersOnline());
        obj.sendService("/authok|" + obj.getNickname());

        String list = listWhoisOnline();
        for (ClientHandler user : connectedClients) user.sendService("/online|" + list);
        broadcastMsg("The user " + obj.getNickname() + " has joined the chat.");
    }

    private synchronized void showUsersOnline() {
        ArrayList<String> list = new ArrayList<String>();
        for (ClientHandler user : connectedClients) list.add(user.getNickname());
        clients = FXCollections.observableArrayList(list);
        controller.listView.setItems(clients);
        controller.listView.refresh();
    }

    /**
     * Проверка подключения клиента
     * @param login логин клиента
     * @return true - клиент подключен
     */
    public boolean isUserConnected(String login) {
        for (ClientHandler client : connectedClients) if (client.getClientLogin().equals(login)) return true;
        return false;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized String listWhoisOnline() {
        String result = "";
        for (ClientHandler client : connectedClients) result += client.getNickname() + "|";
        return result;
    }

    public synchronized void changeNameClient(ClientHandler obj, String newName, String oldName) throws IOException {
        connectedClients.remove(obj);
        connectedClients.add(obj);
        Platform.runLater(() -> showUsersOnline());
        for (ClientHandler user : connectedClients) user.sendService("/online|" + listWhoisOnline());
        broadcastMsg("The user " + oldName + " changed the name to " + newName);
    }

    /**
     * Отображение отключения клиента
     * @param obj
     * @throws IOException
     */
    public synchronized void removeClient(ClientHandler obj) throws IOException {
        broadcastMsg("The user " + obj.getNickname() + " left the chat.");
        Platform.runLater(() -> showUsersOnline());

        connectedClients.remove(obj);
        obj.close();

        for (ClientHandler client : connectedClients) client.sendService("/online|" + listWhoisOnline());
    }

    public synchronized void broadcastMsg(String message) throws IOException {
        Platform.runLater(() -> controller.message(message));
        for (ClientHandler client : connectedClients) {
            client.sendService("/echo|" + message);
        }
    }

    public void privatMsg(String sender, String recipient, String message) {
        Platform.runLater(() -> controller.message("<b>" + message + "</b>"));
        for (ClientHandler client : connectedClients) {
            if (client.getNickname().equals(recipient)) {
                Platform.runLater(() -> {
                    try {
                        client.sendService("/echo|" + "<b>" + message + "</b>");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public synchronized void message(String message) {
        Platform.runLater(() -> controller.message(message));
    }
}
