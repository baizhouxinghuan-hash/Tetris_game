package com.he.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // 数据库文件会自动生成在项目根目录下，名字叫 tetris_data.db
    private static final String DB_URL = "jdbc:sqlite:tetris_data.db";

    static {
        try {
            // 确保加载 SQLite 驱动
            Class.forName("org.sqlite.JDBC");
            initDatabase();
        } catch (Exception e) {
            System.err.println("数据库驱动加载失败，请检查是否添加了 sqlite-jdbc.jar 到 Build Path!");
        }
    }

    // 初始化数据库与表结构
    private static void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY," +
                "password TEXT NOT NULL," +
                "highscore INTEGER DEFAULT 0)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 注册新用户
    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password, highscore) VALUES(?, ?, 0)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true; // 注册成功
        } catch (SQLException e) {
            return false; // 通常是因为用户名已存在 (主键冲突)
        }
    }

    // 验证登录
    public static boolean loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // 如果有查询结果，说明账密正确
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 获取某个玩家的历史最高分
    public static int getHighScore(String username) {
        if (username == null || username.isEmpty()) return 0;
        String sql = "SELECT highscore FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("highscore");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 更新最高分 (只有打破记录才更新)
    public static void updateHighScore(String username, int newScore) {
        if (username == null || username.isEmpty()) return;
        int currentHighScore = getHighScore(username);
        if (newScore > currentHighScore) {
            String sql = "UPDATE users SET highscore = ? WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, newScore);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 获取排行榜前10名数据
    public static List<String> getTop10Scores() {
        List<String> topList = new ArrayList<>();
        String sql = "SELECT username, highscore FROM users ORDER BY highscore DESC LIMIT 10";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int rank = 1;
            while (rs.next()) {
                String name = rs.getString("username");
                int score = rs.getInt("highscore");
                topList.add(rank + ". " + name + " - " + score + "分");
                rank++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topList;
    }
}