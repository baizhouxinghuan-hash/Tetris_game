package com.game;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Game {
    public enum GameState { PLAYING, PAUSED, GAME_OVER, WIN, EXIT }
    public enum Difficulty { EASY, HARD }

    private int[][] grid;
    private Tetromino currentTetromino;
    private Tetromino nextTetromino;

    private GameState state;
    private Difficulty difficulty;
    private int score;
    private int linesCleared;
    private static final int WIN_LINES = 20;
    private static final int ROWS = 20;
    private static final int COLS = 15;

    private long baseDropInterval = 550;
    private long lastDropTime;
    private boolean softDrop = false;
    private int comboCount = 0;

    // ================= 【新增：最高分与文件系统】 =================
    private int highScore = 0;
    private static final String HIGH_SCORE_FILE = "highscore.txt";
    // =========================================================

    // ================= 【新增：背景音乐】 =================
    private Clip bgmClip;
    private static final String BGM_FILE =
             "/com/music/bgm.wav";

    private static final String WIN_FILE =
            "/com/music/win.wav";

    private static final String LOSE_FILE =
             "/com/music/lose.wav";

    private static final String MOVE_SOUND =
             "/com/music/move.wav";

    private static final String ROTATE_SOUND =
            "/com/music/rotate.wav";

    private static final String DROP_SOUND =
             "/com/music/drop.wav";
    // =====================================================

    private UI ui;
    private static List<int[][]> allShapes;
    private static final Random RANDOM = new Random();

    public Game() {
         System.out.println("工作目录 = "
            + System.getProperty("user.dir"));
        grid = new int[ROWS][COLS];
        state = GameState.PLAYING;
        difficulty = Difficulty.EASY;
        score = 0;
        linesCleared = 0;
        ui = new UI(this);

        // 游戏初始化时加载本地最高分
        loadHighScore();

        // 初始化背景音乐（循环播放背景.mp3）
        playWav(BGM_FILE, true);

        if (allShapes == null) {
            allShapes = generateAllConnectedShapes();
        }
    }
    private void playSound(String filePath) {
         try {

            AudioInputStream ais =
                AudioSystem.getAudioInputStream(
                     Game.class.getResource(filePath)
                );

            Clip clip = AudioSystem.getClip();

            clip.open(ais);

             clip.start();

             clip.addLineListener(event -> {
                 if (event.getType() ==
                        javax.sound.sampled.LineEvent.Type.STOP) {
                     clip.close();
                 }
             });

        } catch (Exception e) {
             e.printStackTrace();
        }
    }
    // ================= 【新增：文件读写方法】 =================
    /**
     * 读取本地最高分存档
     */
    private void loadHighScore() {
        try {
            File file = new File(HIGH_SCORE_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    highScore = Integer.parseInt(line.trim());
                }
                reader.close();
            }
        } catch (Exception e) {
            System.err.println("读取最高分失败: " + e.getMessage());
        }
    }

    /**
     * 将最高分写入本地文件
     */
    private void saveHighScore() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORE_FILE));
            writer.write(String.valueOf(highScore));
            writer.close();
        } catch (Exception e) {
            System.err.println("保存最高分失败: " + e.getMessage());
        }
    }
    // =========================================================

    // ================= 【新增：背景音乐播放】 =================
    /**
     * 使用 Windows Media Player COM 对象通过 PowerShell 播放 MP3
     */
    private void playWav(String filePath, boolean loop) {
        try {

            System.out.println("播放文件: " + new File(filePath).getAbsolutePath());
            System.out.println("文件存在: " + new File(filePath).exists());

            stopBGM();

            AudioInputStream ais =
                AudioSystem.getAudioInputStream(
                         Game.class.getResource(filePath)
                );

            bgmClip = AudioSystem.getClip();

            bgmClip.open(ais);

            if (loop) {
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            }

            bgmClip.start();

            System.out.println("音乐开始播放");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopBGM() {

         if (bgmClip != null) {

            bgmClip.stop();

            bgmClip.close();

            bgmClip = null;
        }
    }

    private void resumeBGM() {
        playWav(BGM_FILE, true);
    }
    // =========================================================

    public int getHighScore() { return highScore; }
    public int getComboCount() { return comboCount; }

    public int getLevel() {
        return (linesCleared / 5) + 1;
    }

    public long getCurrentDropInterval() {
        long interval = baseDropInterval - (getLevel() - 1) * 30L;
        return Math.max(interval, 80);
    }

    public int[][] getGrid() { return grid; }
    public Tetromino getCurrentTetromino() { return currentTetromino; }
    public Tetromino getNextTetromino() { return nextTetromino; }
    public GameState getState() { return state; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { this.difficulty = d; }
    public int getScore() { return score; }
    public int getLinesCleared() { return linesCleared; }
    public int getWinLines() { return WIN_LINES; }
    public int getRows() { return ROWS; }
    public int getCols() { return COLS; }

    public void setSoftDrop(boolean softDrop) { this.softDrop = softDrop; }

    public int getGhostRow() {
        if (currentTetromino == null) return 0;
        int originalRow = currentTetromino.getRow();
        int ghostRow = originalRow;

        while (true) {
            currentTetromino.setRow(ghostRow + 1);
            if (!isValidPosition(currentTetromino)) {
                break;
            }
            ghostRow++;
        }

        currentTetromino.setRow(originalRow);
        return ghostRow;
    }

    public void hardDrop() {

        if (state != GameState.PLAYING || currentTetromino == null) return;
        playSound(DROP_SOUND);

        int ghostRow = getGhostRow();
        currentTetromino.setRow(ghostRow);

        lockTetromino();
        int lines = clearFullRows();
        if (lines > 0) {
            addScore(lines);
            comboCount++;
            linesCleared += lines;
            if (linesCleared >= WIN_LINES) {
                state = GameState.WIN;
                stopBGM();
                playWav(WIN_FILE, false);
                return;
            }
        } else {
            comboCount = 0;
        }
        spawnTetromino();
        if (isGameOver()) {
            state = GameState.GAME_OVER;
            stopBGM();
            playWav(LOSE_FILE, false);
        }

        lastDropTime = System.currentTimeMillis();
    }

    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            stopBGM();
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            lastDropTime = System.currentTimeMillis();
            resumeBGM();
        }
        ui.repaint();
    }

    public void reset() {
        grid = new int[ROWS][COLS];
        state = GameState.PLAYING;
        score = 0;
        linesCleared = 0;
        comboCount = 0;
        currentTetromino = null;
        nextTetromino = null;
        softDrop = false;
        lastDropTime = System.currentTimeMillis();
        spawnTetromino();
        resumeBGM();
    }

    public void start() {
        spawnTetromino();
        ui.setVisible(true);

        // 游戏循环必须在独立线程中运行，否则会阻塞 Swing EDT 导致界面卡死
        new Thread(() -> {
            lastDropTime = System.currentTimeMillis();
            while (state != GameState.EXIT) {
                if (state == GameState.PLAYING) {
                    long currentTime = System.currentTimeMillis();
                    long currentInterval = softDrop ? 40 : getCurrentDropInterval();

                    if (currentTime - lastDropTime >= currentInterval) {
                        update();
                        lastDropTime = currentTime;
                    }
                }
                ui.repaint();
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            // 退出循环后，关闭UI并返回主菜单
            ui.dispose();
            StartMenu menu = new StartMenu();
            menu.setVisible(true);
        }).start();
    }

    public void returnToMenu() {
        stopBGM();
        state = GameState.EXIT;
    }

    public void update() {
        if (state != GameState.PLAYING) return;
        if (currentTetromino == null) {
            spawnTetromino();
            return;
        }
        if (!moveDown()) {
            lockTetromino();
            int lines = clearFullRows();
            if (lines > 0) {
                addScore(lines);
                comboCount++;
                linesCleared += lines;
                if (linesCleared >= WIN_LINES) {
                    state = GameState.WIN;
                    stopBGM();
                    playWav(WIN_FILE, false);
                    return;
                }
            } else {
                comboCount = 0;
            }
            spawnTetromino();
            if (isGameOver()) {
                state = GameState.GAME_OVER;
                stopBGM();
                playWav(LOSE_FILE, false);
            }
        }
    }

    private void addScore(int lines) {
        int baseScore = 0;
        switch (lines) {
            case 1: baseScore = 100; break;
            case 2: baseScore = 300; break;
            case 3: baseScore = 500; break;
            case 4: baseScore = 800; break;
            default: baseScore = lines * 100; break;
        }

        int comboBonus = comboCount * lines * 50;
        score += (baseScore + comboBonus);

        // 【修改】检测并持久化保存最高分
        if (score > highScore) {
            highScore = score;
            saveHighScore(); // 破纪录时立即写入硬盘
        }
    }

    private void lockTetromino() {
        int[][] shape = currentTetromino.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] != 0) {
                    int gridRow = currentTetromino.getRow() + r;
                    int gridCol = currentTetromino.getCol() + c;
                    if (gridRow >= 0 && gridRow < ROWS && gridCol >= 0 && gridCol < COLS) {
                        grid[gridRow][gridCol] = currentTetromino.getColorIndex();
                    }
                }
            }
        }
    }

    private int clearFullRows() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                for (int row = r; row > 0; row--) {
                    System.arraycopy(grid[row - 1], 0, grid[row], 0, COLS);
                }
                for (int c = 0; c < COLS; c++) {
                    grid[0][c] = 0;
                }
                cleared++;
                r++;
            }
        }
        return cleared;
    }

    public void moveLeft() {
        if (state != GameState.PLAYING || currentTetromino == null)
          return;

        currentTetromino.moveLeft();

        if (!isValidPosition(currentTetromino)) {
            currentTetromino.moveRight();
        } else {
            playSound(MOVE_SOUND);
        }
    }

    public void moveRight() {
        if (state != GameState.PLAYING || currentTetromino == null)
             return;

        currentTetromino.moveRight();

         if (!isValidPosition(currentTetromino)) {
            currentTetromino.moveLeft();
        } else {
            playSound(MOVE_SOUND);
         }
    }

    public boolean moveDown() {
        if (state != GameState.PLAYING || currentTetromino == null) return false;
        currentTetromino.moveDown();
        if (!isValidPosition(currentTetromino)) {
            currentTetromino.moveUp();
            return false;
        }
        if (softDrop) {
            score += 1;
            // 软降得分也可能触发破纪录
            if (score > highScore) {
                highScore = score;
                saveHighScore();
            }
        }
        return true;
    }

    public void rotate() {
        if (state != GameState.PLAYING || currentTetromino == null) return;
        playSound(ROTATE_SOUND);
        if (difficulty == Difficulty.EASY) {
            currentTetromino.rotate();
            if (!isValidPosition(currentTetromino)) currentTetromino.rotateBack();
        } else {
            int oldRow = currentTetromino.getRow();
            int oldCol = currentTetromino.getCol();
            int oldColorIndex = currentTetromino.getColorIndex();
            int[][] newShape = allShapes.get(RANDOM.nextInt(allShapes.size()));
            currentTetromino.setShape(newShape);
            currentTetromino.setRow(oldRow);
            currentTetromino.setCol(oldCol);
            currentTetromino.setColorIndex(oldColorIndex);

            if (!isValidPosition(currentTetromino)) {
                currentTetromino.setCol(oldCol - 1);
                if (!isValidPosition(currentTetromino)) {
                    currentTetromino.setCol(oldCol + 1);
                    if (!isValidPosition(currentTetromino)) {
                        currentTetromino.setCol(oldCol);
                        currentTetromino.setRow(oldRow - 1);
                        if (!isValidPosition(currentTetromino)) {
                            currentTetromino.setRow(oldRow);
                            currentTetromino.setCol(oldCol);
                        }
                    }
                }
            }
        }
    }

    private boolean isValidPosition(Tetromino tetromino) {
        int[][] shape = tetromino.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] != 0) {
                    int gridRow = tetromino.getRow() + r;
                    int gridCol = tetromino.getCol() + c;
                    if (gridRow < 0 || gridRow >= ROWS || gridCol < 0 || gridCol >= COLS) return false;
                    if (grid[gridRow][gridCol] != 0) return false;
                }
            }
        }
        return true;
    }

    public void spawnTetromino() {
        if (nextTetromino == null) {
            int[][] shape = allShapes.get(RANDOM.nextInt(allShapes.size()));
            int colorIndex = RANDOM.nextInt(7) + 1;
            nextTetromino = new Tetromino(shape, colorIndex);
        }
        currentTetromino = nextTetromino;
        currentTetromino.setRow(0);
        currentTetromino.setCol(COLS / 2 - currentTetromino.getShape()[0].length / 2);

        int[][] nextShape = allShapes.get(RANDOM.nextInt(allShapes.size()));
        int nextColorIndex = RANDOM.nextInt(7) + 1;
        nextTetromino = new Tetromino(nextShape, nextColorIndex);

        softDrop = false;
    }

    public boolean isGameOver() {
        return !isValidPosition(currentTetromino);
    }

    private static List<int[][]> generateAllConnectedShapes() {
        List<int[][]> shapes = new ArrayList<>();
        boolean[][][] used = new boolean[5][5][5];
        for (int blockCount = 1; blockCount <= 4; blockCount++) {
            boolean[][] grid = new boolean[4][4];
            generateShapesRecursive(shapes, grid, 0, 0, blockCount, 0, used);
        }
            // 十字
        shapes.add(new int[][]{
            {1,1,1,1},
            {0,0,0,1},
            {0,0,0,1},
            {0,0,0,1}
        });
        shapes.add(new int[][]{
            {1,1,1},
            {0,1,0},
            {0,1,0},
        });
        return shapes;
    }

    private static void generateShapesRecursive(List<int[][]> shapes, boolean[][] grid,
                                                int startR, int startC, int targetCount, int placed, boolean[][][] used) {
        if (placed == targetCount) {
            if (!isConnected(grid, targetCount)) return;
            int[][] normalized = normalizeShape(grid);
            if (normalized == null) return;
            if (isDuplicate(normalized, shapes)) return;
            shapes.add(normalized);
            return;
        }
        for (int r = startR; r < 4; r++) {
            for (int c = (r == startR ? startC : 0); c < 4; c++) {
                if (!grid[r][c]) {
                    if (placed == 0 || hasAdjacent(grid, r, c)) {
                        grid[r][c] = true;
                        generateShapesRecursive(shapes, grid, r, c + 1, targetCount, placed + 1, used);
                        grid[r][c] = false;
                    }
                }
            }
        }
    }

    private static boolean hasAdjacent(boolean[][] grid, int r, int c) {
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < 4 && nc >= 0 && nc < 4 && grid[nr][nc]) return true;
        }
        return false;
    }

    private static boolean isConnected(boolean[][] grid, int blockCount) {
        int startR = -1, startC = -1;
        outer:
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (grid[r][c]) {
                    startR = r; startC = c; break outer;
                }
            }
        }
        if (startR == -1) return false;

        boolean[][] visited = new boolean[4][4];
        int[][] queue = new int[16][2];
        int head = 0, tail = 0;
        queue[tail][0] = startR; queue[tail][1] = startC; tail++;
        visited[startR][startC] = true;
        int count = 0;
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        while (head < tail) {
            int r = queue[head][0], c = queue[head][1]; head++; count++;
            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 0 && nr < 4 && nc >= 0 && nc < 4 && grid[nr][nc] && !visited[nr][nc]) {
                    visited[nr][nc] = true; queue[tail][0] = nr; queue[tail][1] = nc; tail++;
                }
            }
        }
        return count == blockCount;
    }

    private static int[][] normalizeShape(boolean[][] grid) {
        int minR = 4, maxR = -1, minC = 4, maxC = -1;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (grid[r][c]) {
                    minR = Math.min(minR, r); maxR = Math.max(maxR, r);
                    minC = Math.min(minC, c); maxC = Math.max(maxC, c);
                }
            }
        }
        int rows = maxR - minR + 1, cols = maxC - minC + 1;
        if (rows <= 0 || cols <= 0) return null;
        int[][] result = new int[rows][cols];
        for (int r = minR; r <= maxR; r++) {
            for (int c = minC; c <= maxC; c++) {
                result[r - minR][c - minC] = grid[r][c] ? 1 : 0;
            }
        }
        return result;
    }

    private static boolean isDuplicate(int[][] shape, List<int[][]> shapes) {
        for (int[][] existing : shapes) {
            if (shapesEqual(shape, existing)) return true;
            int[][] rotated = existing;
            for (int i = 0; i < 3; i++) {
                rotated = rotateMatrix(rotated);
                if (shapesEqual(shape, rotated)) return true;
            }
        }
        return false;
    }

    private static boolean shapesEqual(int[][] a, int[][] b) {
        if (a.length != b.length || a[0].length != b[0].length) return false;
        for (int r = 0; r < a.length; r++) {
            for (int c = 0; c < a[0].length; c++) {
                if (a[r][c] != b[r][c]) return false;
            }
        }
        return true;
    }

    private static int[][] rotateMatrix(int[][] matrix) {
        int rows = matrix.length, cols = matrix[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) rotated[c][rows - 1 - r] = matrix[r][c];
        }
        return rotated;
    }

    public class Tetromino {
        private int[][] shape;
        private int row;
        private int col;
        private int colorIndex;

        public Tetromino(int[][] shape, int colorIndex) {
            this.shape = new int[shape.length][shape[0].length];
            for (int r = 0; r < shape.length; r++) System.arraycopy(shape[r], 0, this.shape[r], 0, shape[0].length);
            this.row = 0; this.col = 0; this.colorIndex = colorIndex;
        }
        public int[][] getShape() { return shape; }
        public void setShape(int[][] newShape) {
            this.shape = new int[newShape.length][newShape[0].length];
            for (int r = 0; r < newShape.length; r++) System.arraycopy(newShape[r], 0, this.shape[r], 0, newShape[0].length);
        }
        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }
        public int getColorIndex() { return colorIndex; }
        public void setColorIndex(int colorIndex) { this.colorIndex = colorIndex; }
        public void moveLeft() { col--; }
        public void moveRight() { col++; }
        public void moveDown() { row++; }
        public void moveUp() { row--; }

        public void rotate() {
            int rows = shape.length, cols = shape[0].length;
            int[][] rotated = new int[cols][rows];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) rotated[c][rows - 1 - r] = shape[r][c];
            }
            shape = rotated;
        }

        public void rotateBack() {
            int rows = shape.length, cols = shape[0].length;
            int[][] rotated = new int[cols][rows];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) rotated[cols - 1 - c][r] = shape[r][c];
            }
            shape = rotated;
        }
    }
}