package com.itqgroup.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class TestConnection2 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://127.0.0.1:5432/document_flow";
        Properties props = new Properties();
        props.setProperty("user", "myuser");
        props.setProperty("password", "mypassword");
        props.setProperty("connectTimeout", "10");
        props.setProperty("loginTimeout", "10");
        props.setProperty("socketTimeout", "10");

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Драйвер загружен");
            System.out.println("Подключение к: " + url);

            Connection conn = DriverManager.getConnection(url, props);
            System.out.println("Подключение успешно!");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            if (rs.next()) {
                System.out.println("Запрос выполнен: " + rs.getInt(1));
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}