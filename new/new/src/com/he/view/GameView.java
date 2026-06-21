package com.he.view;

import com.he.model.GameModel;
import javax.swing.JFrame;

public class GameView extends JFrame {
    private final GamePanel gamePanel;

    // 接收两个模型（单机时 remoteModel 传 null 即可）
    public GameView(GameModel localModel, GameModel remoteModel) {
        this.gamePanel = new GamePanel(localModel, remoteModel);

        // 动态修改标题
        setTitle(remoteModel == null ? "俄罗斯方块 - 单机模式" : "俄罗斯方块 - 双人联机对战");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(gamePanel);
        pack(); // 会根据 GamePanel 的自适应尺寸自动拉伸窗口
        setLocationRelativeTo(null);
    }
}