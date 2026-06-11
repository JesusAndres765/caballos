package Model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final String URL =
            "jdbc:mysql://localhost:3306/TaQueArdeElEstablo" + "?useSSL=false" +
                    "&serverTimezone=" + java.util.TimeZone.getDefault().getID() +
                    "&allowPublicKeyRetrieval=true";
    private static final String USER = "JesusAnd";
    private static final String PASSWORD = "JAAT";

    private static Connection connection = null;

    private ConexionDB() {}

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Error al conectar a la BD: " + e.getMessage());
        }
        return connection;
    }
}