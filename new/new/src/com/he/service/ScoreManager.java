package com.he.service;

import com.he.config.GameConstants;
import java.io.*;

public class ScoreManager {

    public static int loadHighScore() {
        File file = new File(GameConstants.HIGH_SCORE_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return Integer.parseInt(line.trim());
                }
            } catch (Exception e) {
                System.err.println("读取最高分失败: " + e.getMessage());
            }
        }
        return 0;
    }

    public static void saveHighScore(int score) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GameConstants.HIGH_SCORE_FILE))) {
            writer.write(String.valueOf(score));
        } catch (Exception e) {
            System.err.println("保存最高分失败: " + e.getMessage());
        }
    }
}