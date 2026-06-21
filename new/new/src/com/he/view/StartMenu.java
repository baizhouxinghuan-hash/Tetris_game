package com.he.view;

import com.he.config.GameConstants;
import com.he.controller.GameController;
import com.he.model.GameModel;
import com.he.network.NetworkService;
import com.he.network.SyncData;
import com.he.service.AudioManager;
import com.he.service.DatabaseManager; // 引入我们刚才写的数据库引擎
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;

public class StartMenu extends JFrame {
    private static String currentUser = null; // static：关闭窗口后仍保持登录

    public StartMenu() {
        setTitle("俄罗斯方块 - 登录与大厅");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        MenuPanel menuPanel = new MenuPanel();
        add(menuPanel);
        pack();
        setLocationRelativeTo(null);

        // 启动菜单BGM
        AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
    }

    private class MenuPanel extends JPanel {
        private final Color BT = new Color(20, 20, 50);
        private final Color BB = new Color(40, 10, 60);

        // 【修改1】：加宽窗口，左边放按钮，右边放排行榜
        private static final int W = 800;
        private static final int H = 550;

        // 重新排布五个按钮的高度
        private int loginBtnY = 160;
        private int singleBtnY = 230;
        private int hostBtnY = 300;
        private int joinBtnY = 370;
        private int exitBtnY = 440;

        private int btnW = 240, btnH = 50;
        private int btnX = 60; // 【修改2】：按钮整体靠左排布

        public MenuPanel() {
            setPreferredSize(new Dimension(W, H));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int mx = e.getX();
                    int my = e.getY();

                    // 判断点击是否在按钮的 X 轴范围内
                    if (mx >= btnX && mx <= btnX + btnW) {
                        if (my >= loginBtnY && my <= loginBtnY + btnH) {
                            if (currentUser != null) {
                                // 已登录：弹出退出确认
                                int choice = JOptionPane.showConfirmDialog(StartMenu.this,
                                        "当前登录: " + currentUser + "\n是否退出登录？",
                                        "退出登录", JOptionPane.YES_NO_OPTION);
                                if (choice == JOptionPane.YES_OPTION) {
                                    currentUser = null;
                                    repaint();
                                    JOptionPane.showMessageDialog(StartMenu.this, "已退出登录");
                                }
                            } else {
                                showLoginRegisterDialog(); // 未登录：弹出登录注册框
                            }
                        } else if (my >= singleBtnY && my <= singleBtnY + btnH) {
                            if (checkLogin()) startSingleGame(); // 检查登录后启动
                        } else if (my >= hostBtnY && my <= hostBtnY + btnH) {
                            if (checkLogin()) startNetworkGame(true);
                        } else if (my >= joinBtnY && my <= joinBtnY + btnH) {
                            if (checkLogin()) startNetworkGame(false);
                        } else if (my >= exitBtnY && my <= exitBtnY + btnH) {
                            AudioManager.getInstance().stopBGM();
                            System.exit(0);
                        }
                    }
                }
            });
        }

        // --- 检查是否登录拦截器 ---
        private boolean checkLogin() {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(StartMenu.this, "请先登录或注册账号才能开始游戏！", "尚未登录", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            return true;
        }

        // --- 弹出登录/注册框逻辑 ---
        private void showLoginRegisterDialog() {
            JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
            JTextField userField = new JTextField(10);
            JPasswordField passField = new JPasswordField(10);
            panel.add(new JLabel("账号名称:"));
            panel.add(userField);
            panel.add(new JLabel("账号密码:"));
            panel.add(passField);

            Object[] options = {"登录", "注册新账号", "取消"};
            int result = JOptionPane.showOptionDialog(StartMenu.this, panel, "玩家通行证",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();

            if (result == 0) { // 用户点击了"登录"
                if (DatabaseManager.loginUser(u, p)) {
                    currentUser = u;
                    JOptionPane.showMessageDialog(StartMenu.this, "登录成功！欢迎回来, " + u);
                    repaint(); // 登录成功后刷新界面，更新按钮上的名字
                } else {
                    JOptionPane.showMessageDialog(StartMenu.this, "账号或密码错误，请重试！", "登录失败", JOptionPane.ERROR_MESSAGE);
                }
            } else if (result == 1) { // 用户点击了"注册"
                if (u.isEmpty() || p.isEmpty()) {
                    JOptionPane.showMessageDialog(StartMenu.this, "账号和密码不能为空！", "注册错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (DatabaseManager.registerUser(u, p)) {
                    JOptionPane.showMessageDialog(StartMenu.this, "注册成功！请点击【登录】按钮进入游戏。");
                } else {
                    JOptionPane.showMessageDialog(StartMenu.this, "注册失败，该用户名可能已被别人占用！", "注册错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // --- 启动单机模式 ---
        private void startSingleGame() {
            AudioManager.getInstance().stopBGM();
            dispose();

            // 传入当前登录的账号
            GameModel model = new GameModel(currentUser);
            GameView view = new GameView(model, null);
            GameController controller = new GameController(model, view, null);

            view.setVisible(true);
            controller.startGameLoop();
        }

        // --- 启动双人联机模式 ---
        private void startNetworkGame(boolean isHost) {
            // 先选择模式：局域网 / 互联网
            Object[] modeOptions = {"局域网", GameConstants.SERVER_HOST};
            int modeChoice = JOptionPane.showOptionDialog(StartMenu.this,
                    "请选择联机模式：",
                    (isHost ? "创建房间" : "加入房间") + " - 选择网络模式",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, modeOptions, modeOptions[0]);

            if (modeChoice == JOptionPane.CLOSED_OPTION) return;

            boolean useInternet = (modeChoice == 1); // 0=局域网, 1=互联网

            AudioManager.getInstance().stopBGM();

            new Thread(() -> {
                try {
                    NetworkService netService = new NetworkService();

                    if (useInternet) {
                        // ========== 互联网模式 ==========
                        String roomName;
                        if (isHost) {
                            roomName = JOptionPane.showInputDialog(StartMenu.this,
                                    "请输入房间名（对方将用此名称加入）:", "创建互联网房间",
                                    JOptionPane.PLAIN_MESSAGE);
                            if (roomName == null || roomName.trim().isEmpty()) {
                                AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
                                return;
                            }
                            roomName = roomName.trim();
                        } else {
                            roomName = JOptionPane.showInputDialog(StartMenu.this,
                                    "请输入要加入的房间名:", "加入互联网房间",
                                    JOptionPane.PLAIN_MESSAGE);
                            if (roomName == null || roomName.trim().isEmpty()) {
                                AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
                                return;
                            }
                            roomName = roomName.trim();
                        }

                        // 连接服务器
                        netService.connectToServer(GameConstants.SERVER_HOST, GameConstants.SERVER_PORT);

                        String result;
                        if (isHost) {
                            // 房主：创建房间，阻塞等待对手
                            JOptionPane.showMessageDialog(StartMenu.this,
                                    "房间【" + roomName + "】已创建，等待对手加入...\n\n请让对方在加入房间时输入: " + roomName);
                            result = netService.createRoom(roomName);
                        } else {
                            result = netService.joinRoom(roomName);
                        }

                        if (result == null || result.startsWith("ERROR") || result.startsWith("ROOM_")) {
                            JOptionPane.showMessageDialog(StartMenu.this,
                                    "连接失败: " + (result != null ? result : "未知错误"),
                                    "错误", JOptionPane.ERROR_MESSAGE);
                            netService.close();
                            AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
                            return;
                        }

                    } else {
                        // ========== 局域网模式（保留原逻辑）==========
                        if (isHost) {
                            JOptionPane.showMessageDialog(StartMenu.this,
                                    "点击确定后，将在局域网内创建房间并等待对手连接...");
                            netService.hostGame(8888);
                        } else {
                            String ip = JOptionPane.showInputDialog(StartMenu.this,
                                    "请输入房主局域网IP (本机测试请输入 127.0.0.1):", "127.0.0.1");
                            if (ip == null || ip.trim().isEmpty()) {
                                AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
                                return;
                            }
                            netService.joinGame(ip.trim(), 8888);
                        }
                    }

                    dispose();

                    // 本地模型传入真实账号，远程模型只显示"对手"
                    GameModel localModel = new GameModel(currentUser);
                    GameModel remoteModel = new GameModel("对手");

                    GameView view = new GameView(localModel, remoteModel);
                    GameController controller = new GameController(localModel, view, netService);

                    netService.startListening((SyncData opponentData) -> {
                        remoteModel.overwriteWith(opponentData);
                        view.repaint();
                    });

                    view.setVisible(true);
                    controller.startGameLoop();

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StartMenu.this,
                            "网络连接失败: " + ex.getMessage());
                    AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
                }
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 背景渐变
            g2.setPaint(new GradientPaint(0, 0, BT, 0, getHeight(), BB));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // 标题 (靠左上)
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("微软雅黑", Font.BOLD, 46));
            g2.drawString("俄罗斯方块 - 联机大厅", 60, 80);

            // ================= 绘制左侧五个选项按钮 =================
            // 动态改变第一个按钮的状态
            if (currentUser == null) {
                drawButton(g2, "登录 / 注册", btnX, loginBtnY, new Color(100, 100, 100, 200), Color.WHITE);
            } else {
                drawButton(g2, "已登录: " + currentUser, btnX, loginBtnY, new Color(0, 150, 200, 200), Color.CYAN);
            }

            drawButton(g2, "单人挑战", btnX, singleBtnY, new Color(0, 180, 80, 200), new Color(0, 255, 120));
            drawButton(g2, "创建房间 (房主)", btnX, hostBtnY, new Color(200, 100, 0, 200), new Color(255, 150, 0));
            drawButton(g2, "加入房间 (挑战)", btnX, joinBtnY, new Color(0, 100, 200, 200), new Color(0, 150, 255));
            drawButton(g2, "退出游戏", btnX, exitBtnY, new Color(180, 50, 50, 200), new Color(255, 80, 80));

            // ================= 绘制右侧金碧辉煌的排行榜 =================
            int boardX = 350;
            int boardY = 160;
            int boardW = 400;
            int boardH = 330;

            // 绘制榜单背景框
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(boardX, boardY, boardW, boardH, 20, 20);
            g2.setColor(new Color(255, 215, 0, 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(boardX, boardY, boardW, boardH, 20, 20);

            // 绘制榜单标题
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 24));
            g2.drawString("巅峰排行榜 Top 10", boardX + 60, boardY + 40);
            g2.drawLine(boardX + 20, boardY + 55, boardX + boardW - 20, boardY + 55);

            // 从 SQLite 获取数据并展示
            List<String> topScores = DatabaseManager.getTop10Scores();
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 18));
            int textY = boardY + 85;

            if (topScores.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("暂无数据，快来注册账号创造历史吧！", boardX + 45, boardY + 150);
            } else {
                for (String record : topScores) {
                    // 冠亚季军用专属颜色高亮显示
                    if (record.startsWith("1.")) g2.setColor(new Color(255, 215, 0)); // 金色
                    else if (record.startsWith("2.")) g2.setColor(new Color(192, 192, 192)); // 银色
                    else if (record.startsWith("3.")) g2.setColor(new Color(205, 127, 50)); // 铜色
                    else g2.setColor(Color.WHITE);

                    g2.drawString(record, boardX + 40, textY);
                    textY += 24; // 每行文字的行距
                }
            }
        }

        private void drawButton(Graphics2D g2, String text, int x, int y, Color fill, Color border) {
            g2.setColor(fill);
            g2.fillRoundRect(x, y, btnW, btnH, 15, 15);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, btnW, btnH, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("微软雅黑", Font.BOLD, 22));
            int textX = x + (btnW - g2.getFontMetrics().stringWidth(text)) / 2;
            g2.drawString(text, textX, y + 34);
        }
    }
}