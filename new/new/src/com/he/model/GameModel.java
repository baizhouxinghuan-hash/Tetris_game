package com.he.model;

import com.he.config.GameConstants;
import com.he.service.AudioManager;
import com.he.service.DatabaseManager; // <--- 新增：引入数据库引擎，删除了原来的 ScoreManager
import java.util.List;
import java.util.Random;

public class GameModel {
    public enum GameState { PLAYING, PAUSED, GAME_OVER, WIN, EXIT }
    public enum Difficulty { EASY, HARD }

    private String playerName; // <--- 新增：记录当前游玩的账号名

    private int[][] grid;
    private Tetromino currentTetromino;
    private Tetromino nextTetromino;

    private GameState state;
    private Difficulty difficulty;
    private int score;
    private int highScore;
    private int linesCleared;
    private int comboCount;
    private boolean softDrop;

    private final List<int[][]> allShapes;
    private final Random random = new Random();

    // <--- 修改：构造函数强制要求传入玩家名称
    public GameModel(String playerName) {
        this.playerName = playerName;
        grid = new int[GameConstants.ROWS][GameConstants.COLS];
        state = GameState.PLAYING;
        difficulty = Difficulty.EASY;
        allShapes = ShapeGenerator.generateAllConnectedShapes();
        // <--- 修改：从 SQLite 数据库实时读取该玩家的历史最高分
        highScore = DatabaseManager.getHighScore(playerName);
        spawnTetromino();
    }

    // 核心业务逻辑：下落更新
    public void update() {
        if (state != GameState.PLAYING) return;
        if (currentTetromino == null) {
            spawnTetromino();
            return;
        }

        if (!moveDown()) {
            lockTetromino();
            int lines = clearFullRows();
            handleScoring(lines);
            spawnTetromino();

            if (isGameOver()) {
                state = GameState.GAME_OVER;
                AudioManager.getInstance().stopBGM();
                AudioManager.getInstance().playBGM(GameConstants.LOSE_FILE, false);
            }
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
            addScore(1);
        }
        return true;
    }

    public void moveLeft() {
        if (state != GameState.PLAYING || currentTetromino == null) return;
        currentTetromino.moveLeft();
        if (!isValidPosition(currentTetromino)) {
            currentTetromino.moveRight();
        } else {
            AudioManager.getInstance().playSound(GameConstants.MOVE_SOUND);
        }
    }

    public void moveRight() {
        if (state != GameState.PLAYING || currentTetromino == null) return;
        currentTetromino.moveRight();
        if (!isValidPosition(currentTetromino)) {
            currentTetromino.moveLeft();
        } else {
            AudioManager.getInstance().playSound(GameConstants.MOVE_SOUND);
        }
    }

    public void rotate() {
        if (state != GameState.PLAYING || currentTetromino == null) return;
        AudioManager.getInstance().playSound(GameConstants.ROTATE_SOUND);

        if (difficulty == Difficulty.EASY) {
            currentTetromino.rotate();
            if (!isValidPosition(currentTetromino)) currentTetromino.rotateBack();
        } else {
            // 困难模式：随机变换形状
            int oldRow = currentTetromino.getRow();
            int oldCol = currentTetromino.getCol();
            int oldColor = currentTetromino.getColorIndex();
            int[][] newShape = allShapes.get(random.nextInt(allShapes.size()));

            currentTetromino.setShape(newShape);
            currentTetromino.setRow(oldRow);
            currentTetromino.setCol(oldCol);
            currentTetromino.setColorIndex(oldColor);

            // 简单消解碰撞
            if (!isValidPosition(currentTetromino)) {
                currentTetromino.setCol(oldCol - 1);
                if (!isValidPosition(currentTetromino)) {
                    currentTetromino.setCol(oldCol + 1);
                    if (!isValidPosition(currentTetromino)) {
                        currentTetromino.setCol(oldCol);
                    }
                }
            }
        }
    }

    public void hardDrop() {
        if (state != GameState.PLAYING || currentTetromino == null) return;
        AudioManager.getInstance().playSound(GameConstants.DROP_SOUND);
        currentTetromino.setRow(getGhostRow());

        lockTetromino();
        int lines = clearFullRows();
        handleScoring(lines);
        spawnTetromino();

        if (isGameOver()) {
            state = GameState.GAME_OVER;
            AudioManager.getInstance().stopBGM();
            AudioManager.getInstance().playBGM(GameConstants.LOSE_FILE, false);
        }
    }

    private void handleScoring(int lines) {
        if (lines > 0) {
            comboCount++;
            linesCleared += lines;
            int baseScore = switch (lines) {
                case 1 -> 100;
                case 2 -> 300;
                case 3 -> 500;
                case 4 -> 800;
                default -> lines * 100;
            };
            int comboBonus = (comboCount - 1) * lines * 50;
            addScore(baseScore + comboBonus);

            if (linesCleared >= GameConstants.WIN_LINES) {
                state = GameState.WIN;
                AudioManager.getInstance().stopBGM();
                AudioManager.getInstance().playBGM(GameConstants.WIN_FILE, false);
            }
        } else {
            comboCount = 0;
        }
    }

    private void addScore(int points) {
        score += points;
        if (score > highScore) {
            highScore = score;
            // <--- 修改：分数破纪录时，实时存入 SQLite 数据库
            DatabaseManager.updateHighScore(playerName, highScore);
        }
    }

    public void spawnTetromino() {
        if (nextTetromino == null) {
            nextTetromino = createRandomTetromino();
        }
        currentTetromino = nextTetromino;
        currentTetromino.setRow(0);
        currentTetromino.setCol(GameConstants.COLS / 2 - currentTetromino.getShape()[0].length / 2);

        nextTetromino = createRandomTetromino();
        softDrop = false;
    }

    private Tetromino createRandomTetromino() {
        int[][] shape = allShapes.get(random.nextInt(allShapes.size()));
        int colorIndex = random.nextInt(7) + 1;
        return new Tetromino(shape, colorIndex);
    }

    private void lockTetromino() {
        int[][] shape = currentTetromino.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] != 0) {
                    int gRow = currentTetromino.getRow() + r;
                    int gCol = currentTetromino.getCol() + c;
                    if (gRow >= 0 && gRow < GameConstants.ROWS && gCol >= 0 && gCol < GameConstants.COLS) {
                        grid[gRow][gCol] = currentTetromino.getColorIndex();
                    }
                }
            }
        }
    }

    private int clearFullRows() {
        int cleared = 0;
        for (int r = GameConstants.ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < GameConstants.COLS; c++) {
                if (grid[r][c] == 0) { full = false; break; }
            }
            if (full) {
                for (int row = r; row > 0; row--) {
                    System.arraycopy(grid[row - 1], 0, grid[row], 0, GameConstants.COLS);
                }
                java.util.Arrays.fill(grid[0], 0);
                cleared++;
                r++; // 重新检查当前行
            }
        }
        return cleared;
    }

    public boolean isValidPosition(Tetromino t) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] != 0) {
                    int gRow = t.getRow() + r;
                    int gCol = t.getCol() + c;
                    if (gRow < 0 || gRow >= GameConstants.ROWS || gCol < 0 || gCol >= GameConstants.COLS) return false;
                    if (grid[gRow][gCol] != 0) return false;
                }
            }
        }
        return true;
    }

    public int getGhostRow() {
        if (currentTetromino == null) return 0;
        int originalRow = currentTetromino.getRow();
        int ghostRow = originalRow;
        while (true) {
            currentTetromino.setRow(ghostRow + 1);
            if (!isValidPosition(currentTetromino)) break;
            ghostRow++;
        }
        currentTetromino.setRow(originalRow);
        return ghostRow;
    }

    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            AudioManager.getInstance().stopBGM();
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
        }
    }

    public void reset() {
        grid = new int[GameConstants.ROWS][GameConstants.COLS];
        state = GameState.PLAYING;
        score = 0;
        linesCleared = 0;
        comboCount = 0;
        currentTetromino = null;
        nextTetromino = null;
        spawnTetromino();
        AudioManager.getInstance().playBGM(GameConstants.BGM_FILE, true);
    }

    public boolean isGameOver() { return !isValidPosition(currentTetromino); }

    // Getters 和 Setters
    public String getPlayerName() { return playerName; } // <--- 新增
    public int[][] getGrid() { return grid; }
    public Tetromino getCurrentTetromino() { return currentTetromino; }
    public Tetromino getNextTetromino() { return nextTetromino; }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { this.difficulty = d; }
    public int getScore() { return score; }
    public int getHighScore() { return highScore; }
    public int getLinesCleared() { return linesCleared; }
    public int getComboCount() { return comboCount; }
    public void setSoftDrop(boolean sd) { this.softDrop = sd; }
    public int getLevel() { return (linesCleared / 5) + 1; }
    public long getCurrentDropInterval() {
        long interval = 550 - (getLevel() - 1) * 30L;
        return Math.max(interval, 80);
    }

    // --- 新增：网络同步专用方法 ---

    // 1. 生成当前状态的快照（用于发送给对手）
    public com.he.network.SyncData generateSyncData() {
        int[][] shape = null;
        int row = 0, col = 0, color = 0;
        if (currentTetromino != null) {
            shape = currentTetromino.getShape();
            row = currentTetromino.getRow();
            col = currentTetromino.getCol();
            color = currentTetromino.getColorIndex();
        }
        return new com.he.network.SyncData(
                this.grid, this.score, this.linesCleared,
                shape, row, col, color,
                this.state == GameState.GAME_OVER
        );
    }

    // 2. 用收到的网络数据强制覆盖当前模型（用于刷新对手的画面）
    public void overwriteWith(com.he.network.SyncData data) {
        if (data == null) return;
        this.grid = data.grid;
        this.score = data.score;
        this.linesCleared = data.linesCleared;

        if (data.isGameOver) {
            this.state = GameState.GAME_OVER;
        }

        if (data.currentShape != null) {
            if (this.currentTetromino == null) {
                this.currentTetromino = new Tetromino(data.currentShape, data.pieceColor);
            } else {
                this.currentTetromino.setShape(data.currentShape);
                this.currentTetromino.setColorIndex(data.pieceColor);
            }
            this.currentTetromino.setRow(data.pieceRow);
            this.currentTetromino.setCol(data.pieceCol);
        } else {
            this.currentTetromino = null;
        }
    }
} // <--- 类的大括号在这里闭合！在这个基础上改