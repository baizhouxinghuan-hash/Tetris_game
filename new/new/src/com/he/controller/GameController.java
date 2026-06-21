package com.he.controller;

import com.he.model.GameModel;
import com.he.view.GameView;
import com.he.view.StartMenu;
import com.he.service.AudioManager;
import com.he.config.GameConstants;
import com.he.network.NetworkService; // 新增：导入网络服务
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GameController {
    private final GameModel model;
    private final GameView view;
    private final NetworkService netService; // 新增：持有网络服务的引用
    private long lastDropTime;

    // 构造函数：增加 netService 参数
    public GameController(GameModel model, GameView view, NetworkService netService) {
        this.model = model;
        this.view = view;
        this.netService = netService;
        initController();
    }

    private void initController() {
        // 绑定键盘事件到视图上
        view.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    model.togglePause();
                } else if (model.getState() == GameModel.GameState.PLAYING) {
                    // 保留现代的增强型 switch 语法 (Java 14+)
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT -> model.moveLeft();
                        case KeyEvent.VK_RIGHT -> model.moveRight();
                        case KeyEvent.VK_DOWN -> {
                            model.setSoftDrop(true);
                            model.moveDown();
                        }
                        case KeyEvent.VK_UP -> model.rotate();
                        case KeyEvent.VK_SPACE -> model.hardDrop();
                    }
                }
                view.repaint();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    model.setSoftDrop(false);
                }
            }
        });
    }

    // 启动游戏主循环线程
    public void startGameLoop() {
        // 启动背景音乐
        AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);

        new Thread(() -> {
            lastDropTime = System.currentTimeMillis();
            while (model.getState() != GameModel.GameState.EXIT) {
                if (model.getState() == GameModel.GameState.PLAYING) {
                    long currentTime = System.currentTimeMillis();
                    long interval = model.getCurrentDropInterval();

                    if (currentTime - lastDropTime >= interval) {
                        model.update();
                        lastDropTime = currentTime;
                    }
                }

                // 【新增核心网络逻辑】：只要网络服务存在，就高频发送自己的状态快照给对手
                if (netService != null) {
                    netService.sendData(model.generateSyncData());
                }

                view.repaint(); // 驱动界面刷新

                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 游戏退出时，安全关闭网络连接和底层资源
            if (netService != null) {
                netService.close();
            }
            AudioManager.getInstance().stopBGM();
            view.dispose();
            new StartMenu().setVisible(true);

        }).start();
    }
}