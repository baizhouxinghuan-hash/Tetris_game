package com.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class UI extends JFrame {
    private Game game;
    private GamePanel gamePanel;

    private static final Color[] COLORS = {
            new Color(30, 30, 40), new Color(0, 255, 255), new Color(255, 255, 0),
            new Color(200, 0, 255), new Color(0, 255, 100), new Color(255, 50, 50),
            new Color(50, 100, 255), new Color(255, 150, 0)
    };

    // ================= 【排版优化核心参数】 =================
    private static final int CS = 40;       // 方块大小
    private static final int SW = 300;      // 侧边栏拓宽至 300px，给文字充足空间
    private static final int PADDING = 25;  // 全局四周内边距，告别局促的边框
    // =======================================================

    // ================= 【图片资源】 =================
    private Image imgPredict, imgLevel, imgProgress, imgScore, imgCombo, imgHighScore;
    private static final int ICON_SIZE = 28;
    // ==============================================

    public UI(Game game) {
        this.game = game;
        this.gamePanel = new GamePanel();

        // 加载图片
        imgPredict   = loadImage("com/image/1.png");
        imgLevel     = loadImage("com/image/2.png");
        imgProgress  = loadImage("com/image/3.png");
        imgScore     = loadImage("com/image/4.png");
        imgCombo     = loadImage("com/image/5.png");
        imgHighScore = loadImage("com/image/6.png");

        setTitle("俄罗斯方块");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(gamePanel);
        pack();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    game.togglePause();
                } else if (game.getState() == Game.GameState.PLAYING) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            game.moveLeft();
                            break;
                        case KeyEvent.VK_RIGHT:
                            game.moveRight();
                            break;
                        case KeyEvent.VK_DOWN:
                            game.setSoftDrop(true);
                            game.moveDown();
                            break;
                        case KeyEvent.VK_UP:
                            game.rotate();
                            break;
                        case KeyEvent.VK_SPACE:
                            game.hardDrop();
                            break;
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    game.setSoftDrop(false);
                }
            }
        });
        setLocationRelativeTo(null);
    }

    private Image loadImage(String path) {
        try {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                return Toolkit.getDefaultToolkit().getImage(path);
            }
        } catch (Exception e) {
            System.err.println("加载图片失败: " + path);
        }
        return null;
    }

    private void drawIcon(Graphics2D g2, Image img, int x, int y) {
        if (img != null) {
            g2.drawImage(img, x, y - ICON_SIZE + 6, ICON_SIZE, ICON_SIZE, null);
        }
    }

    @Override
    public void repaint() {
        if (gamePanel != null) gamePanel.repaint();
    }

    private class GamePanel extends JPanel {
        private final Color BT = new Color(20, 20, 50);
        private final Color BB = new Color(40, 10, 60);

        private int easyBtnX, easyBtnY, hardBtnX, hardBtnY;
        private int btnW = 130, btnH = 45;
        private int backBtnX, backBtnY, backBtnW = 220, backBtnH = 45;

        public GamePanel() {
            setPreferredSize(new java.awt.Dimension(
                    PADDING * 3 + game.getCols() * CS + SW,
                    PADDING * 2 + game.getRows() * CS
            ));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Game.GameState st = game.getState();
                    if (st == Game.GameState.GAME_OVER || st == Game.GameState.WIN) {
                        int cx = PADDING + (game.getCols() * CS) / 2;
                        int cy = PADDING + (game.getRows() * CS) / 2;
                        int bx = cx - 110, by = cy + 70, bw = 220, bh = 60;
                        if (e.getX() >= bx && e.getX() <= bx + bw && e.getY() >= by && e.getY() <= by + bh) {
                            game.reset();
                        }
                    }

                    if (e.getX() >= easyBtnX && e.getX() <= easyBtnX + btnW &&
                            e.getY() >= easyBtnY && e.getY() <= easyBtnY + btnH) {
                        game.setDifficulty(Game.Difficulty.EASY);
                    }
                    if (e.getX() >= hardBtnX && e.getX() <= hardBtnX + btnW &&
                            e.getY() >= hardBtnY && e.getY() <= hardBtnY + btnH) {
                        game.setDifficulty(Game.Difficulty.HARD);
                    }

                    // 返回主菜单按钮
                    if (e.getX() >= backBtnX && e.getX() <= backBtnX + backBtnW &&
                            e.getY() >= backBtnY && e.getY() <= backBtnY + backBtnH) {
                        game.returnToMenu();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(new GradientPaint(0, 0, BT, 0, getHeight(), BB));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int gw = game.getCols() * CS;
            int gh = game.getRows() * CS;

            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(PADDING, PADDING, gw, gh);
            g2.setColor(new Color(255, 255, 255, 20));

            for (int r = 0; r <= game.getRows(); r++) g2.drawLine(PADDING, PADDING + r * CS, PADDING + gw, PADDING + r * CS);
            for (int c = 0; c <= game.getCols(); c++) g2.drawLine(PADDING + c * CS, PADDING, PADDING + c * CS, PADDING + gh);

            int[][] grid = game.getGrid();
            for (int r = 0; r < game.getRows(); r++)
                for (int c = 0; c < game.getCols(); c++)
                    if (grid[r][c] > 0) drawCell(g2, r, c, CS, COLORS[grid[r][c] % COLORS.length]);

            Game.Tetromino cur = game.getCurrentTetromino();
            if (cur != null && (game.getState() == Game.GameState.PLAYING || game.getState() == Game.GameState.PAUSED)) {
                int[][] sh = cur.getShape();
                int t = cur.getColorIndex();
                Color baseColor = COLORS[t % COLORS.length];

                int ghostRow = game.getGhostRow();
                Color ghostColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 60);
                for (int r = 0; r < sh.length; r++) {
                    for (int c = 0; c < sh[0].length; c++) {
                        if (sh[r][c] != 0) {
                            drawCell(g2, ghostRow + r, cur.getCol() + c, CS, ghostColor);
                        }
                    }
                }

                for (int r = 0; r < sh.length; r++) {
                    for (int c = 0; c < sh[0].length; c++) {
                        if (sh[r][c] != 0) {
                            drawCell(g2, cur.getRow() + r, cur.getCol() + c, CS, baseColor);
                        }
                    }
                }
            }

            drawSidebar(g2);

            if (game.getState() == Game.GameState.PAUSED) {
                drawPauseOverlay(g2, "游戏暂停", new Color(0, 0, 0, 180));
            } else if (game.getState() == Game.GameState.GAME_OVER) {
                drawEndOverlay(g2, "挑战失败", new Color(200, 50, 50, 200));
            } else if (game.getState() == Game.GameState.WIN) {
                drawEndOverlay(g2, "挑战成功", new Color(50, 200, 50, 200));
            }
        }

        private void drawSidebar(Graphics2D g2) {
            int x = PADDING * 2 + game.getCols() * CS;
            int y = PADDING + 30;

            // 1. 标题区
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 36));
            g2.drawString("俄罗斯方块", x, y);
            y += 20;
            g2.setColor(new Color(255, 255, 255, 80));
            g2.drawLine(x, y, x + SW, y);

            // 2. 预测下一个区域（图片1.png）
            y += 35;
            drawIcon(g2, imgPredict, x, y);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2.drawString("预测下一个", x + ICON_SIZE + 4, y);

            y += 15;
            int boxSize = 100;
            int px = x + (SW - boxSize) / 2;
            int py = y;
            g2.setColor(new Color(255, 255, 255, 15));
            g2.fillRoundRect(px, py, boxSize, boxSize, 12, 12);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.drawRoundRect(px, py, boxSize, boxSize, 12, 12);

            Game.Tetromino next = game.getNextTetromino();
            if (next != null) {
                int[][] sh = next.getShape();
                int cIndex = next.getColorIndex();
                Color c = COLORS[cIndex % COLORS.length];

                int miniCellSize = 22;
                int pieceW = sh[0].length * miniCellSize;
                int pieceH = sh.length * miniCellSize;
                int startX = px + (boxSize - pieceW) / 2;
                int startY = py + (boxSize - pieceH) / 2;

                for(int r = 0; r < sh.length; r++){
                    for(int col = 0; col < sh[0].length; col++){
                        if(sh[r][col] != 0){
                            drawMiniCell(g2, startX + col * miniCellSize, startY + r * miniCellSize, miniCellSize, c);
                        }
                    }
                }
            }
            y += boxSize + 35;

            // 3. 当前等级（图片2.png）
            drawIcon(g2, imgLevel, x, y);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2.drawString("当前等级 :  Lv " + game.getLevel(), x + ICON_SIZE + 4, y);

            y += 40;
            // 4. 挑战进度（图片3.png）
            drawIcon(g2, imgProgress, x, y);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2.drawString("挑战进度 :  " + game.getLinesCleared() + " / " + game.getWinLines(), x + ICON_SIZE + 4, y);

            y += 15;
            int bw = SW, bh = 18;
            float p = Math.min(1f, (float) game.getLinesCleared() / game.getWinLines());
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(x, y, bw, bh, 10, 10);
            g2.setColor(p >= 1f ? new Color(50, 255, 50) : new Color(100, 200, 255));
            g2.fillRoundRect(x, y, (int) (bw * p), bh, 10, 10);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawRoundRect(x, y, bw, bh, 10, 10);

            y += 55;

            // 5. 当前分数（图片4.png）
            drawIcon(g2, imgScore, x, y);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2.drawString("当前分数", x + ICON_SIZE + 4, y);
            if (game.getComboCount() > 0) {
                g2.setColor(new Color(255, 80, 80));
                g2.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 22));
                g2.drawString(game.getComboCount() + " COMBO!", x + 160, y);
            }
            y += 35;
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 42));
            g2.drawString(String.valueOf(game.getScore()), x, y);

            y += 55;

            // 6. 连击（图片5.png）
            drawIcon(g2, imgCombo, x, y);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2.drawString("连击", x + ICON_SIZE + 4, y);
            y += 35;
            g2.setColor(new Color(255, 150, 0));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 36));
            g2.drawString(String.valueOf(game.getComboCount()), x, y);

            y += 55;

            // 7. 历史最高分（图片6.png）
            drawIcon(g2, imgHighScore, x, y);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2.drawString("历史最高分", x + ICON_SIZE + 4, y);
            y += 35;
            g2.setColor(new Color(255, 150, 0));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 36));
            g2.drawString(String.valueOf(game.getHighScore()), x, y);

            y += 60;

            // 8. 难度模式按钮区
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2.drawString("难度模式", x, y);
            y += 25;

            easyBtnX = x; easyBtnY = y;
            boolean isEasy = game.getDifficulty() == Game.Difficulty.EASY;
            g2.setColor(isEasy ? new Color(0, 200, 100, 180) : new Color(255, 255, 255, 40));
            g2.fillRoundRect(easyBtnX, easyBtnY, btnW, btnH, 8, 8);
            g2.setColor(isEasy ? new Color(0, 255, 150) : new Color(255, 255, 255, 100));
            g2.drawRoundRect(easyBtnX, easyBtnY, btnW, btnH, 8, 8);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g2.setColor(isEasy ? Color.WHITE : new Color(255, 255, 255, 150));
            String easyText = "简单";
            g2.drawString(easyText, easyBtnX + (btnW - g2.getFontMetrics().stringWidth(easyText)) / 2, easyBtnY + 28);

            hardBtnX = x + btnW + 20; hardBtnY = y;
            boolean isHard = game.getDifficulty() == Game.Difficulty.HARD;
            g2.setColor(isHard ? new Color(200, 50, 50, 180) : new Color(255, 255, 255, 40));
            g2.fillRoundRect(hardBtnX, hardBtnY, btnW, btnH, 8, 8);
            g2.setColor(isHard ? new Color(255, 80, 80) : new Color(255, 255, 255, 100));
            g2.drawRoundRect(hardBtnX, hardBtnY, btnW, btnH, 8, 8);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g2.setColor(isHard ? Color.WHITE : new Color(255, 255, 255, 150));
            String hardText = "困难";
            g2.drawString(hardText, hardBtnX + (btnW - g2.getFontMetrics().stringWidth(hardText)) / 2, hardBtnY + 28);

            y += 65;

            // 9. 操作说明区
            g2.setColor(new Color(255, 255, 255, 130));
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 16));
            g2.drawString("方向键 : 左右移动", x, y+30);
            g2.drawString("上键 : 旋转变换", x + 160, y+30);
            y += 26;
            g2.drawString("下键 : 加速下落", x, y+30);
            g2.drawString("空格 : 瞬间坠落", x + 160, y+30);
            y += 26;
            g2.setColor(new Color(255, 150, 50));
            g2.drawString("P : 暂停 / 继续", x, y+30);

            y += 40;
            // 10. 返回主菜单按钮
            backBtnW = 100;
            backBtnH = 50;
            backBtnX = x + 150;
            backBtnY = y - 240;
            g2.setColor(new Color(150, 100, 200, 180));
            g2.fillRoundRect(backBtnX, backBtnY, backBtnW, backBtnH, 10, 10);
            g2.setColor(new Color(200, 150, 255));
            g2.drawRoundRect(backBtnX, backBtnY, backBtnW, backBtnH, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 20));
            String backText = "返回主菜单";
            g2.drawString(backText, backBtnX + (backBtnW - g2.getFontMetrics().stringWidth(backText)) / 2, backBtnY + 30);
        }

        private void drawPauseOverlay(Graphics2D g2, String title, Color bg) {
            int gw = game.getCols() * CS;
            int gh = game.getRows() * CS;
            g2.setColor(bg);
            g2.fillRect(PADDING, PADDING, gw, gh);
            int cx = PADDING + gw / 2;
            int cy = PADDING + gh / 2;

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 46));
            g2.drawString(title, cx - g2.getFontMetrics().stringWidth(title) / 2, cy - 20);

            g2.setFont(new Font("微软雅黑", Font.PLAIN, 22));
            String sub = "按 P 键恢复游戏";
            g2.drawString(sub, cx - g2.getFontMetrics().stringWidth(sub) / 2, cy + 30);
        }

        private void drawEndOverlay(Graphics2D g2, String title, Color bg) {
            int gw = game.getCols() * CS;
            int gh = game.getRows() * CS;
            g2.setColor(bg);
            g2.fillRect(PADDING, PADDING, gw, gh);
            int cx = PADDING + gw / 2;
            int cy = PADDING + gh / 2;

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 46));
            g2.drawString(title, cx - g2.getFontMetrics().stringWidth(title) / 2, cy - 50);

            g2.setFont(new Font("微软雅黑", Font.PLAIN, 26));
            String s = "最终得分: " + game.getScore();
            g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, cy + 20);

            int bx = cx - 110, by = cy + 70, bw = 220, bh = 60;
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(bx, by, bw, bh, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(bx, by, bw, bh, 12, 12);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 24));
            String btn = "再来一次";
            g2.drawString(btn, cx - g2.getFontMetrics().stringWidth(btn) / 2, by + 40);
        }

        private void drawCell(Graphics2D g2, int row, int col, int size, Color color) {
            drawMiniCell(g2, PADDING + col * size, PADDING + row * size, size, color);
        }

        private void drawMiniCell(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            g2.fillRect(x, y, size, size);
            g2.setColor(color.brighter());
            g2.drawLine(x, y, x + size - 1, y);
            g2.drawLine(x, y, x, y + size - 1);
            g2.setColor(color.darker());
            g2.drawLine(x + size - 1, y, x + size - 1, y + size - 1);
            g2.drawLine(x, y + size - 1, x + size - 1, y + size - 1);
        }
    }
}