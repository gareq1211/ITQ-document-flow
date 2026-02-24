package com.itqgroup.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://127.0.0.1:5432/document_flow";
        String user = "myuser";
        String password = "mypassword";

        try {
            // Явно загружаем драйвер
            Class.forName("org.postgresql.Driver");
            System.out.println("Драйвер загружен");

            System.out.println("Попытка подключения к: " + url);
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Подключение успешно!");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            if (rs.next()) {
                System.out.println("Запрос выполнен: " + rs.getInt(1));
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (ClassNotFoundException e) {
            System.out.println("Драйвер не найден: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}