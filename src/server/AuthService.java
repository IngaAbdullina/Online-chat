package server;

import java.util.ArrayList;

/**
 * Служба аутентификации
 */
public interface AuthService {
    /**
     * Подключение к БД
     */
    void start();

    /**
     * Отключение
     */
    void stop();

    /**
     * Получаем ник клиента по его логину и паролю
     * @param login    логин
     * @param password пароль
     * @return ник клиента
     */
    String getUser(String login, int password);

    /**
     * Проверка наличия пользователя в БД
     * @param user имя пользователя
     * @return имя пользователя String либо null, если пользователя нет в БД
     */
    String getUser(String user);

    /**
     * Получаем пароль по логину
     * @param login логин
     * @return пароль
     */
    int getPassword(String login);

    /**
     * Получаем логин из БД по логину для проверки наличия зарегистрированного пользователя
     * @param login логин
     * @return логин из БД либо null, если такого пользователя нет
     */
    String getLogin(String login);

    /**
     * Регистрация пользователя
     * @param login    логин нового пользователя
     * @param password пароль нового пользователя, преобразованный в хэш-код
     * @param name     имя нового пользователя
     * @return true - успешно
     */
    boolean registration(String login, int password, String name);

    /**
     * Смена имени пользователя
     * @param oldName старое имя
     * @param newName новое имя
     * @return true - имя успешно изменено
     */
    boolean changeUserName(String oldName, String newName);

    /**
     * Сохранение истории переписки в БД
     * @param datetime время
     * @param sender   отправитель
     * @param receiver получатель
     * @param type     тип сообщения (broadcast, private)
     * @param message  сообщение
     * @return true - история сохранена успешно
     */
    boolean saveToHistory(String datetime, String sender, String receiver, String type, String message);

    /**
     * Получаем сохраненную историю перепеиски из БД
     * @param sender история пользователя
     * @param day    за количество дней
     * @return ArrayList<String>
     */
    ArrayList<String> getHistory(String sender, int day);

    void controlClient(String user, String controlWord, String datetime);
}
