package com.he.config;

import java.awt.Color;

public class GameConstants {
    // 网格与排版参数
    public static final int ROWS = 20;
    public static final int COLS = 15;
    public static final int CELL_SIZE = 40;
    public static final int SIDEBAR_WIDTH = 300;
    public static final int PADDING = 25;

    // 胜利条件
    public static final int WIN_LINES = 20;

    // 文件与资源路径
    public static final String HIGH_SCORE_FILE = "highscore.txt";
    public static final String BGM_FILE = "src/com/music/bgm.wav";
    public static final String WIN_FILE = "src/com/music/win.wav";
    public static final String LOSE_FILE = "src/com/music/lose.wav";
    public static final String MOVE_SOUND = "src/com/music/move.wav";
    public static final String ROTATE_SOUND = "src/com/music/rotate.wav";
    public static final String DROP_SOUND = "src/com/music/drop.wav";

    // 联网服务器配置
    public static final String SERVER_HOST = "example.com";  // 纯域名，不要 https://
    public static final int SERVER_PORT = 443;  // Cloudflare Tunnel 走 HTTPS/WSS

    // 颜色矩阵
    public static final Color[] COLORS = {
            new Color(30, 30, 40),     // 背景色
            new Color(0, 255, 255),    // I
            new Color(255, 255, 0),    // O
            new Color(200, 0, 255),    // T
            new Color(0, 255, 100),    // S
            new Color(255, 50, 50),    // Z
            new Color(50, 100, 255),   // J
            new Color(255, 150, 0)     // L
    };
}