package server;

import java.sql.*;
import java.util.*;

public class DBService implements AuthService {
    private Connection connection;
    private PreparedStatement preparedStatement;

    @Override
    public void start() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:/Users/inga/Documents/IdeaProjects/chat/database.sqlite");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized String getUser(String login, int password) {
        try {
            preparedStatement = connection.prepareStatement("SELECT user FROM accounts WHERE login = ? AND password = ?;");
            preparedStatement.setString(1, login);
            preparedStatement.setInt(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) return null;
            return resultSet.getString("user");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getUser(String user) {
        try {
            preparedStatement = connection.prepareStatement("SELECT user FROM accounts WHERE user = ?;");
            preparedStatement.setString(1, user);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) return null;
            return resultSet.getString("user");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getLogin(String login) {
        try {
            preparedStatement = connection.prepareStatement("SELECT login FROM accounts WHERE login = ?;");
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) return null;
            return resultSet.getString("login");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getPassword(String login) {
        try {
            preparedStatement = connection.prepareStatement("SELECT password FROM accounts WHERE login = ?;");
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) return -1;
//            return String.valueOf(resultSet.getString("password").hashCode());
            return resultSet.getInt("password");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public synchronized boolean registration(String login, int password, String name) {
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO accounts (login,password,user) VALUES (?,?,?);");
            preparedStatement.setString(1, login);
            preparedStatement.setInt(2, password);
            preparedStatement.setString(3, name);
            preparedStatement.execute();

            if (!getLogin(login).equals(login))
                return false;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean changeUserName(String oldName, String newName) {
        try {
            preparedStatement = connection.prepareStatement("UPDATE accounts SET user = ? WHERE user = ?;");
            preparedStatement.setString(1, newName);
            preparedStatement.setString(2, oldName);
            preparedStatement.execute();

            if (!getUser(newName).equals(newName)) return false;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean saveToHistory(String datetime, String sender, String receiver, String type, String message) {
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO history (datetime,sender,receiver,type,message) VALUES (?,?,?,?,?);");
            preparedStatement.setString(1, datetime);
            preparedStatement.setString(2, sender);
            preparedStatement.setString(3, receiver);
            preparedStatement.setString(4, type);
            preparedStatement.setString(5, message);
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public synchronized ArrayList<String> getHistory(String sender, int numDaysFromNow) {
        Calendar calendar = Calendar.getInstance();
        ArrayList<String> list = new ArrayList<String>();
        try {
            Timestamp old = new java.sql.Timestamp(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(numDaysFromNow)).getTime());
            Timestamp now = new java.sql.Timestamp(calendar.getTime().getTime());
            String querySql = "SELECT message FROM history WHERE type in ('broadcast','private','service') AND datetime > '" + old + "' AND datetime < '" + now + "' AND sender in ('" + sender + "','[SERVER]') ORDER BY datetime ASC;";
            System.out.println(querySql);
            preparedStatement = connection.prepareStatement(querySql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String messageR = resultSet.getString("message");
                list.add(messageR);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        list.clear();
        return list;
    }

    public void controlClient(String user, String controlWord, String datetime) {
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO control (user,controlWord,datetime) VALUES (?,?,?);");
            preparedStatement.setString(1, user);
            preparedStatement.setString(2, controlWord);
            preparedStatement.setString(3, datetime);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
