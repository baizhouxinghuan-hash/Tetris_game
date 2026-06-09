package com.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class StartMenu extends JFrame {
    private Clip bgmClip;
    private static final String BGM_FILE =
         "/com/music/bgm.wav";

    public StartMenu() {
        setTitle("俄罗斯方块 - 主菜单");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        MenuPanel menuPanel = new MenuPanel();
        add(menuPanel);
        pack();
        setLocationRelativeTo(null);

        // 开始播放背景音乐
        playBGM();
    }

    private void playBGM() {
        try {

             AudioInputStream ais =
                     AudioSystem.getAudioInputStream(
                             StartMenu.class.getResource(BGM_FILE));

             bgmClip = AudioSystem.getClip();

          bgmClip.open(ais);

             bgmClip.loop(Clip.LOOP_CONTINUOUSLY);

             bgmClip.start();

        } catch (Exception e) {
          e.printStackTrace();
         }
    }

    public void stopBGM() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    private class MenuPanel extends JPanel {
        private final Color BT = new Color(20, 20, 50);
        private final Color BB = new Color(40, 10, 60);
        private static final int W = 500;
        private static final int H = 400;

        private int startBtnX, startBtnY, startBtnW = 220, startBtnH = 60;
        private int exitBtnX, exitBtnY, exitBtnW = 220, exitBtnH = 60;

        public MenuPanel() {
            setPreferredSize(new java.awt.Dimension(W, H));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int mx = e.getX();
                    int my = e.getY();

                    // 开始游戏按钮
                    if (mx >= startBtnX && mx <= startBtnX + startBtnW &&
                        my >= startBtnY && my <= startBtnY + startBtnH) {
                        startGame();
                    }

                    // 退出游戏按钮
                    if (mx >= exitBtnX && mx <= exitBtnX + exitBtnW &&
                        my >= exitBtnY && my <= exitBtnY + exitBtnH) {
                        stopBGM();
                        System.exit(0);
                    }
                }
            });
        }

        private void startGame() {
            stopBGM();
            dispose(); // 关闭主菜单窗口

            // 启动游戏
            Game game = new Game();
            game.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 背景渐变
            g2.setPaint(new GradientPaint(0, 0, BT, 0, getHeight(), BB));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // 标题
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 52));
            String title = "俄罗斯方块";
            int titleX = (getWidth() - g2.getFontMetrics().stringWidth(title)) / 2;
            g2.drawString(title, titleX, 120);

            // 副标题
            g2.setColor(new Color(200, 200, 255, 150));
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 18));
            String sub = "经典益智游戏";
            int subX = (getWidth() - g2.getFontMetrics().stringWidth(sub)) / 2;
            g2.drawString(sub, subX, 155);

            // 开始游戏按钮
            startBtnX = (getWidth() - startBtnW) / 2;
            startBtnY = 200;
            g2.setColor(new Color(0, 180, 80, 200));
            g2.fillRoundRect(startBtnX, startBtnY, startBtnW, startBtnH, 15, 15);
            g2.setColor(new Color(0, 255, 120));
            g2.drawRoundRect(startBtnX, startBtnY, startBtnW, startBtnH, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 26));
            String startText = "开始游戏";
            int startTextX = startBtnX + (startBtnW - g2.getFontMetrics().stringWidth(startText)) / 2;
            g2.drawString(startText, startTextX, startBtnY + 40);

            // 退出游戏按钮
            exitBtnX = (getWidth() - exitBtnW) / 2;
            exitBtnY = 290;
            g2.setColor(new Color(180, 50, 50, 200));
            g2.fillRoundRect(exitBtnX, exitBtnY, exitBtnW, exitBtnH, 15, 15);
            g2.setColor(new Color(255, 80, 80));
            g2.drawRoundRect(exitBtnX, exitBtnY, exitBtnW, exitBtnH, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 26));
            String exitText = "退出游戏";
            int exitTextX = exitBtnX + (exitBtnW - g2.getFontMetrics().stringWidth(exitText)) / 2;
            g2.drawString(exitText, exitTextX, exitBtnY + 40);
        }
    }
}