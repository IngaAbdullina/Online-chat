package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Класс обработки потока пользователя
 */
public class ClientHandler {

    private Server server;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String clientLogin;
    private int counterActivity;

    // Динамическая инициализация
    {
        this.nickname = null;
        this.clientLogin = null;
        this.counterActivity = 0;
    }

    /**
     * Конструктор подключения нового пользователя
     * @param server     сервер
     * @param socket     сокет
     * @param controller экземпляр контроллера
     * @throws IOException
     */
    public ClientHandler(Server server, Socket socket, ServerController controller) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(this.socket.getInputStream());
        this.out = new DataOutputStream(this.socket.getOutputStream());
        clientThread();
    }

    /**
     * Метод клиентского потока
     */
    private void clientThread() {
        Thread thread = new Thread(() -> {
            try {
                // Подключение и регистрация клиента
                while (true) {
                    String str = in.readUTF();
                    System.out.println(str);

                    if (str.startsWith("/auth|")) {
                        System.out.println(str);
                        String[] arg = str.split("\\|", 3);
                        String login = arg[1];
                        int password = arg[2].hashCode();

                        String loginInDb = this.server.getAuthService().getLogin(login);
                        int passwordInDb = this.server.getAuthService().getPassword(login);

                        if (loginInDb != null && loginInDb.equals(login)) {
                            if (passwordInDb != -1 && passwordInDb == password) {
                                if (!this.server.isUserConnected(login)) {
                                    this.nickname = this.server.getAuthService().getUser(login, password);
                                    this.clientLogin = this.server.getAuthService().getLogin(login);
                                    this.server.addClient(this);
                                    break;
                                } else sendAlert("The login is already completed.");
                            } else sendAlert("The password is incorrect.");
                        } else sendAlert("Invalid login.");

                    }

                    if (str.startsWith("/registration|")) {
                        String[] arg = str.split("\\|");
                        String newLogin = arg[1];
                        int newPassword = arg[2].hashCode();
                        String newUser = arg[3];

                        String dbLogin = this.server.getAuthService().getLogin(newLogin);
                        System.out.println("dbLogin - " + dbLogin);
                        if (dbLogin == null) { // Если пользователя нет в БД
                            if (this.server.getAuthService().registration(newLogin, newPassword, newUser)) {
                                server.message("New user " + newUser + " is added to the database");
                                sendAlert("Registration is complete!");
                            } else sendAlert("Error creating user in the database.");
                        } else sendAlert("Login is already taken.");
                    }
                }

                // Запуск счетчика активности
                counterActivity();

                // Основной цикл
                while (true) {
                    String str = in.readUTF();

                    // Отправка сообщения всем подключенным пользоввателям
                    if (str.startsWith("/msgbc|")) {
                        counterActivity = 0; // обнуляем счетчик активности
                        String message = str.split("\\|")[1];
                        this.server.broadcastMsg(message);
                        setHistory(timestamp(), this.nickname, "", "broadcast", message);
                    }

                    // Отправка личного сообщения
                    if (str.startsWith("/msgpr|")) {
                        counterActivity = 0; // обнуляем счетчик активности
                        String datetime = str.split("\\|")[1];
                        String recipient = str.split("\\|")[2];
                        String message = str.split("\\|")[3];
                        this.server.privatMsg(this.nickname, recipient, message);
                        setHistory(datetime, this.nickname, recipient, "private", message);
                    }

                    // Запрос истории переписки
                    if (str.startsWith("/gethistory|")) {
                        System.out.println("запрос на историю " + str);
                        String days = str.split("\\|")[1];  // days
                        sendService("/echo|<hr>");
                        for (String line : this.server.getAuthService().getHistory(this.nickname, Integer.parseInt(days))) {
                            sendService("/echo|" + line);
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        sendService("/echo|<hr>");
                    }

                    // Отключение клиента
                    if (str.startsWith("/disconnect")) {
                        this.server.removeClient(this);
                        break;
                    }

                    // Запрос клиентов, подключенных в текущий момент
                    if (str.startsWith("/whoisonline")) {
                        server.message(this.getNickname() + " > /whoisonline");
                        sendService("/online|" + this.server.listWhoisOnline()); // убрали this.
                    }

                    // Смена имени пользователя
                    if (str.startsWith("/changenick|")) {
                        String oldName = this.nickname;
                        this.nickname = str.split("\\|")[1];
                        if (this.server.getAuthService().changeUserName(oldName, this.nickname)) {
                            this.server.changeNameClient(this, this.nickname, oldName);
                            sendAlert("Name changed.");
                            sendService("/newnick|" + this.nickname); // для интерфейса
                        } else
                            sendAlert("The name is not changed because the user name is already used in the database.");
                    }

                    // Внести клиента в список для наблюдения
                    if (str.startsWith("/controlClient")) {
                        String cntrlWord = str.split("\\|")[1];
                        System.out.println("Control client " + this.nickname);
                        this.server.getAuthService().controlClient(this.nickname, cntrlWord, timestamp());
                    }
                }
            } catch (java.net.SocketException e) {
                // закрытие сокета
            } catch (EOFException e) {
                System.out.println("Disconnect user " + this.nickname);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * Счетчик активности клиента
     */
    private void counterActivity() {
        Thread activity = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 60000 мс = 1 минута
                    ++counterActivity;

                    // при бездействии более 10 мин отключаем пользователя
                    if (counterActivity > 10) {
                        sendService("/echo|<b>Disconnected due to inactivity in the chat.</b>");
                        Thread.sleep(1000);
                        this.server.removeClient(this);
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        activity.start();
    }

    String getNickname() {
        return this.nickname;
    }

    String getClientLogin() {
        return this.clientLogin;
    }

    /**
     * Получение текущих даты и времени
     * @return String формат: ГГГГ-ММ-ДД ЧЧ:ММ:СС:ССС
     */
    private String timestamp() {
        Calendar calendar = Calendar.getInstance();
        Timestamp timeStamp = new Timestamp(calendar.getTime().getTime());
        return timeStamp.toString();
    }

    /**
     * отправка предупреждения
     * @param message текст сообщения
     */
    private synchronized void sendAlert(String message) throws IOException {
        this.out.writeUTF("/alert|" + message);
        System.out.println("/alert|" + message);
    }

    /**
     * Отправка служебного сообщения для обработки
     * @param message сообщение
     */
    synchronized void sendService(String message) throws IOException {
        this.out.writeUTF(message);
        System.out.println(message);
    }

    synchronized void close() throws IOException {
        socket.close();
    }

    /**
     * Сохранение стории переписки
     * @param datetime время
     * @param sender   отправитель
     * @param receiver получатель
     * @param type     тип сообщения
     * @param message  сообщение
     */
    private synchronized void setHistory(String datetime, String sender, String receiver, String type, String message) {
        this.server.getAuthService().saveToHistory(datetime, sender, receiver, type, message);
    }
}
