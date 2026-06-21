package com.he.view;

import com.he.config.GameConstants;
import com.he.model.GameModel;
import com.he.model.Tetromino;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel {
    private final GameModel localModel;
    private final GameModel remoteModel; // 新增：对手模型
    private Image imgPredict, imgLevel, imgProgress, imgScore, imgCombo, imgHighScore;
    private static final int ICON_SIZE = 28;

    private int easyBtnX, easyBtnY, hardBtnX, hardBtnY;
    private int btnW = 130, btnH = 45;
    private int backBtnX, backBtnY, backBtnW = 100, backBtnH = 50;

    private final Color BT = new Color(20, 20, 50);
    private final Color BB = new Color(40, 10, 60);

    public GamePanel(GameModel localModel, GameModel remoteModel) {
        this.localModel = localModel;
        this.remoteModel = remoteModel;

        imgPredict   = loadImage("src/com/image/1.png");
        imgLevel     = loadImage("src/com/image/2.png");
        imgProgress  = loadImage("src/com/image/3.png");
        imgScore     = loadImage("src/com/image/4.png");
        imgCombo     = loadImage("src/com/image/5.png");
        imgHighScore = loadImage("src/com/image/6.png");

        // 动态计算画布宽度：如果是双人模式，就在右侧额外增加一个网格的宽度
        int gw = GameConstants.COLS * GameConstants.CELL_SIZE;
        int totalWidth = GameConstants.PADDING * 3 + gw + GameConstants.SIDEBAR_WIDTH;
        if (this.remoteModel != null) {
            totalWidth += GameConstants.PADDING + gw; // 加上右侧对手网格的宽度
        }

        setPreferredSize(new Dimension(
                totalWidth,
                GameConstants.PADDING * 2 + GameConstants.ROWS * GameConstants.CELL_SIZE
        ));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 结算界面重玩逻辑 (仅限本地网格范围)
                GameModel.GameState st = localModel.getState();
                if (st == GameModel.GameState.GAME_OVER || st == GameModel.GameState.WIN) {
                    int cx = GameConstants.PADDING + (GameConstants.COLS * GameConstants.CELL_SIZE) / 2;
                    int cy = GameConstants.PADDING + (GameConstants.ROWS * GameConstants.CELL_SIZE) / 2;
                    int bx = cx - 110, by = cy + 70, bw = 220, bh = 60;
                    if (e.getX() >= bx && e.getX() <= bx + bw && e.getY() >= by && e.getY() <= by + bh) {
                        localModel.reset();
                    }
                }

                if (e.getX() >= easyBtnX && e.getX() <= easyBtnX + btnW &&
                        e.getY() >= easyBtnY && e.getY() <= easyBtnY + btnH) {
                    localModel.setDifficulty(GameModel.Difficulty.EASY);
                }
                if (e.getX() >= hardBtnX && e.getX() <= hardBtnX + btnW &&
                        e.getY() >= hardBtnY && e.getY() <= hardBtnY + btnH) {
                    localModel.setDifficulty(GameModel.Difficulty.HARD);
                }

                if (e.getX() >= backBtnX && e.getX() <= backBtnX + backBtnW &&
                        e.getY() >= backBtnY && e.getY() <= backBtnY + backBtnH) {
                    localModel.setState(GameModel.GameState.EXIT);
                }
            }
        });
    }

    private Image loadImage(String path) {
        try {
            java.io.File f = new java.io.File(path);
            if (f.exists()) return Toolkit.getDefaultToolkit().getImage(path);
        } catch (Exception e) {
            System.err.println("加载组件图标失败: " + path);
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 全局背景渐变
        g2.setPaint(new GradientPaint(0, 0, BT, 0, getHeight(), BB));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int gw = GameConstants.COLS * GameConstants.CELL_SIZE;

        // 1. 绘制左侧：本地玩家网格
        drawGridArea(g2, localModel, GameConstants.PADDING, GameConstants.PADDING);

        // 2. 绘制中部：侧边栏状态区
        drawSidebar(g2, GameConstants.PADDING * 2 + gw);

        // 3. 绘制右侧：对手网格 (仅在双人模式下存在)
        if (remoteModel != null) {
            int remoteX = GameConstants.PADDING * 3 + gw + GameConstants.SIDEBAR_WIDTH;
            drawGridArea(g2, remoteModel, remoteX, GameConstants.PADDING);

            // 给对手网格加个标题说明
            g2.setColor(new Color(255, 100, 100));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 20));
            g2.drawString("对手画面", remoteX, GameConstants.PADDING - 5);
        }
    }

    // 独立抽取的网格绘制通用方法
    private void drawGridArea(Graphics2D g2, GameModel m, int offsetX, int offsetY) {
        if (m == null || m.getGrid() == null) return;
        int gw = GameConstants.COLS * GameConstants.CELL_SIZE;
        int gh = GameConstants.ROWS * GameConstants.CELL_SIZE;

        // 背景
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(offsetX, offsetY, gw, gh);

        // 边线
        g2.setColor(new Color(255, 255, 255, 20));
        for (int r = 0; r <= GameConstants.ROWS; r++)
            g2.drawLine(offsetX, offsetY + r * GameConstants.CELL_SIZE, offsetX + gw, offsetY + r * GameConstants.CELL_SIZE);
        for (int c = 0; c <= GameConstants.COLS; c++)
            g2.drawLine(offsetX + c * GameConstants.CELL_SIZE, offsetY, offsetX + c * GameConstants.CELL_SIZE, offsetY + gh);

        // 已经固定的方块
        int[][] grid = m.getGrid();
        for (int r = 0; r < GameConstants.ROWS; r++) {
            for (int c = 0; c < GameConstants.COLS; c++) {
                if (grid[r][c] > 0) {
                    drawCell(g2, offsetX, offsetY, r, c, GameConstants.CELL_SIZE, GameConstants.COLORS[grid[r][c] % GameConstants.COLORS.length]);
                }
            }
        }

        // 当前正在下落的活动方块
        Tetromino cur = m.getCurrentTetromino();
        if (cur != null && (m.getState() == GameModel.GameState.PLAYING || m.getState() == GameModel.GameState.PAUSED)) {
            int[][] sh = cur.getShape();
            int t = cur.getColorIndex();
            Color baseColor = GameConstants.COLORS[t % GameConstants.COLORS.length];

            // 预测虚影 (只给本地玩家画，不看对手的虚影)
            if (m == localModel) {
                int ghostRow = m.getGhostRow();
                Color ghostColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 60);
                for (int r = 0; r < sh.length; r++) {
                    for (int c = 0; c < sh[0].length; c++) {
                        if (sh[r][c] != 0) {
                            drawCell(g2, offsetX, offsetY, ghostRow + r, cur.getCol() + c, GameConstants.CELL_SIZE, ghostColor);
                        }
                    }
                }
            }

            // 活动方块本体
            for (int r = 0; r < sh.length; r++) {
                for (int c = 0; c < sh[0].length; c++) {
                    if (sh[r][c] != 0) {
                        drawCell(g2, offsetX, offsetY, cur.getRow() + r, cur.getCol() + c, GameConstants.CELL_SIZE, baseColor);
                    }
                }
            }
        }

        // 游戏状态遮罩
        if (m.getState() == GameModel.GameState.PAUSED) {
            drawOverlay(g2, offsetX, offsetY, gw, gh, "游戏暂停", new Color(0, 0, 0, 180), m.getScore(), false);
        } else if (m.getState() == GameModel.GameState.GAME_OVER) {
            drawOverlay(g2, offsetX, offsetY, gw, gh, "挑战失败", new Color(200, 50, 50, 200), m.getScore(), m == localModel);
        } else if (m.getState() == GameModel.GameState.WIN) {
            drawOverlay(g2, offsetX, offsetY, gw, gh, "挑战成功", new Color(50, 200, 50, 200), m.getScore(), m == localModel);
        }
    }

    private void drawSidebar(Graphics2D g2, int x) {
        int y = GameConstants.PADDING + 30;

        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("微软雅黑", Font.BOLD, 36));
        g2.drawString("俄罗斯方块", x, y);
        y += 20;
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawLine(x, y, x + GameConstants.SIDEBAR_WIDTH, y);

        y += 35;
        drawIcon(g2, imgPredict, x, y);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2.drawString("预测下一个", x + ICON_SIZE + 4, y);

        y += 15;
        int boxSize = 100;
        int px = x + (GameConstants.SIDEBAR_WIDTH - boxSize) / 2;
        int py = y;
        g2.setColor(new Color(255, 255, 255, 15));
        g2.fillRoundRect(px, py, boxSize, boxSize, 12, 12);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.drawRoundRect(px, py, boxSize, boxSize, 12, 12);

        Tetromino next = localModel.getNextTetromino();
        if (next != null) {
            int[][] sh = next.getShape();
            int cIndex = next.getColorIndex();
            Color c = GameConstants.COLORS[cIndex % GameConstants.COLORS.length];

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

        drawIcon(g2, imgLevel, x, y);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2.drawString("当前等级 :  Lv " + localModel.getLevel(), x + ICON_SIZE + 4, y);

        y += 40;
        drawIcon(g2, imgProgress, x, y);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2.drawString("挑战进度 :  " + localModel.getLinesCleared() + " / " + GameConstants.WIN_LINES, x + ICON_SIZE + 4, y);

        y += 15;
        int bw = GameConstants.SIDEBAR_WIDTH, bh = 18;
        float p = Math.min(1f, (float) localModel.getLinesCleared() / GameConstants.WIN_LINES);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(x, y, bw, bh, 10, 10);
        g2.setColor(p >= 1f ? new Color(50, 255, 50) : new Color(100, 200, 255));
        g2.fillRoundRect(x, y, (int) (bw * p), bh, 10, 10);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(x, y, bw, bh, 10, 10);

        y += 55;

        drawIcon(g2, imgScore, x, y);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2.drawString("当前分数", x + ICON_SIZE + 4, y);
        if (localModel.getComboCount() > 0) {
            g2.setColor(new Color(255, 80, 80));
            g2.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 22));
            g2.drawString(localModel.getComboCount() + " COMBO!", x + 160, y);
        }
        y += 35;
        g2.setColor(new Color(255, 215, 0));
        g2.setFont(new Font("微软雅黑", Font.BOLD, 42));
        g2.drawString(String.valueOf(localModel.getScore()), x, y);

        y += 55;

        drawIcon(g2, imgCombo, x, y);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2.drawString("连击", x + ICON_SIZE + 4, y);
        y += 35;
        g2.setColor(new Color(255, 150, 0));
        g2.setFont(new Font("微软雅黑", Font.BOLD, 36));
        g2.drawString(String.valueOf(localModel.getComboCount()), x, y);

        y += 55;

        drawIcon(g2, imgHighScore, x, y);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2.drawString("历史最高分", x + ICON_SIZE + 4, y);
        y += 35;
        g2.setColor(new Color(255, 150, 0));
        g2.setFont(new Font("微软雅黑", Font.BOLD, 36));
        g2.drawString(String.valueOf(localModel.getHighScore()), x, y);

        y += 60;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2.drawString("难度模式", x, y);
        y += 25;

        easyBtnX = x; easyBtnY = y;
        boolean isEasy = localModel.getDifficulty() == GameModel.Difficulty.EASY;
        g2.setColor(isEasy ? new Color(0, 200, 100, 180) : new Color(255, 255, 255, 40));
        g2.fillRoundRect(easyBtnX, easyBtnY, btnW, btnH, 8, 8);
        g2.setColor(isEasy ? new Color(0, 255, 150) : new Color(255, 255, 255, 100));
        g2.drawRoundRect(easyBtnX, easyBtnY, btnW, btnH, 8, 8);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 18));
        g2.setColor(isEasy ? Color.WHITE : new Color(255, 255, 255, 150));
        String easyText = "简单";
        g2.drawString(easyText, easyBtnX + (btnW - g2.getFontMetrics().stringWidth(easyText)) / 2, easyBtnY + 28);

        hardBtnX = x + btnW + 20; hardBtnY = y;
        boolean isHard = localModel.getDifficulty() == GameModel.Difficulty.HARD;
        g2.setColor(isHard ? new Color(200, 50, 50, 180) : new Color(255, 255, 255, 40));
        g2.fillRoundRect(hardBtnX, hardBtnY, btnW, btnH, 8, 8);
        g2.setColor(isHard ? new Color(255, 80, 80) : new Color(255, 255, 255, 100));
        g2.drawRoundRect(hardBtnX, hardBtnY, btnW, btnH, 8, 8);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 18));
        g2.setColor(isHard ? Color.WHITE : new Color(255, 255, 255, 150));
        String hardText = "困难";
        g2.drawString(hardText, hardBtnX + (btnW - g2.getFontMetrics().stringWidth(hardText)) / 2, hardBtnY + 28);

        y += 65;

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
        backBtnX = x + 150;
        backBtnY = y - 240;
        g2.setColor(new Color(150, 100, 200, 180));
        g2.fillRoundRect(backBtnX, backBtnY, backBtnW, backBtnH, 10, 10);
        g2.setColor(new Color(200, 150, 255));
        g2.drawRoundRect(backBtnX, backBtnY, backBtnW, backBtnH, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 16));
        String backText = "返回菜单";
        g2.drawString(backText, backBtnX + (backBtnW - g2.getFontMetrics().stringWidth(backText)) / 2, backBtnY + 32);
    }

    private void drawIcon(Graphics2D g2, Image img, int x, int y) {
        if (img != null) g2.drawImage(img, x, y - ICON_SIZE + 6, ICON_SIZE, ICON_SIZE, null);
    }

    private void drawCell(Graphics2D g2, int offsetX, int offsetY, int row, int col, int size, Color color) {
        drawMiniCell(g2, offsetX + col * size, offsetY + row * size, size, color);
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

    // 整合的遮罩绘制逻辑
    private void drawOverlay(Graphics2D g2, int offsetX, int offsetY, int gw, int gh, String title, Color bg, int score, boolean showBtn) {
        g2.setColor(bg);
        g2.fillRect(offsetX, offsetY, gw, gh);
        int cx = offsetX + gw / 2;
        int cy = offsetY + gh / 2;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("微软雅黑", Font.BOLD, 46));
        g2.drawString(title, cx - g2.getFontMetrics().stringWidth(title) / 2, cy - 30);

        g2.setFont(new Font("微软雅黑", Font.PLAIN, 26));
        String s = "最终得分: " + score;
        g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, cy + 20);

        if (showBtn) {
            int bx = cx - 110, by = cy + 70, bw = 220, bh = 60;
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(bx, by, bw, bh, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(bx, by, bw, bh, 12, 12);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 24));
            String btn = "再来一次";
            g2.drawString(btn, cx - g2.getFontMetrics().stringWidth(btn) / 2, by + 40);
        }
    }
}