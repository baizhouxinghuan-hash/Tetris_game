package com.he.ai;

import com.he.model.Tetromino;
import com.he.config.GameConstants;

public class TetrisAI {
    // Pierre Dellacherie 经典算法权重
    private static final double W_LANDING_HEIGHT = -4.500158;
    private static final double W_ROWS_CLEARED = 3.418126;
    private static final double W_ROW_TRANSITIONS = -3.217888;
    private static final double W_COL_TRANSITIONS = -9.348695;
    private static final double W_HOLES = -7.899265;
    private static final double W_WELLS = -3.385597;

    public static class Move {
        public int rotations; // 需要旋转的次数
        public int targetCol; // 目标列
        public Move(int r, int c) { this.rotations = r; this.targetCol = c; }
    }

    public Move getBestMove(int[][] grid, Tetromino currentPiece) {
        double bestScore = -Double.MAX_VALUE;
        Move bestMove = new Move(0, currentPiece.getCol());

        // 克隆一个用于测试的方块
        Tetromino testPiece = new Tetromino(currentPiece.getShape(), 1);

        // 尝试所有 4 种旋转角度
        for (int rot = 0; rot < 4; rot++) {
            // 尝试所有可能的水平列位置（包括左右越界的一部分，isValid会拦截）
            for (int col = -3; col < GameConstants.COLS + 3; col++) {
                testPiece.setCol(col);
                testPiece.setRow(0);

                // 如果在这个位置一开始就重叠，说明放不下，跳过
                if (!isValid(grid, testPiece)) continue;

                // 模拟方块瞬间掉落到底部
                int ghostRow = getGhostRow(grid, testPiece);
                testPiece.setRow(ghostRow);

                // 在虚拟网格上锁定这个方块
                int[][] clonedGrid = cloneGrid(grid);
                placePiece(clonedGrid, testPiece);

                // 消除满行，并获取消除的行数
                int linesCleared = clearLines(clonedGrid);

                // 核心：使用评估函数对当前局面打分
                double score = evaluate(clonedGrid, linesCleared, ghostRow);

                // 寻找最高分的操作组合
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = new Move(rot, col);
                }
            }
            // 旋转一次，准备下一轮外层循环测试
            testPiece.rotate();
        }
        return bestMove;
    }

    // ================== 六大维度评分函数 ==================
    private double evaluate(int[][] grid, int linesCleared, int dropHeight) {
        int rTrans = 0, cTrans = 0, holes = 0, wells = 0;
        int rows = GameConstants.ROWS, cols = GameConstants.COLS;

        // 1. 行变换数 (Row Transitions)
        for (int r = 0; r < rows; r++) {
            boolean lastCell = true; // 墙壁视为实体
            for (int c = 0; c < cols; c++) {
                boolean currentCell = (grid[r][c] != 0);
                if (lastCell != currentCell) rTrans++;
                lastCell = currentCell;
            }
            if (!lastCell) rTrans++; // 右侧墙壁
        }

        // 2. 列变换数 (Column Transitions)
        for (int c = 0; c < cols; c++) {
            boolean lastCell = true; // 底部视为实体
            for (int r = rows - 1; r >= 0; r--) {
                boolean currentCell = (grid[r][c] != 0);
                if (lastCell != currentCell) cTrans++;
                lastCell = currentCell;
            }
            if (!lastCell) cTrans++; // 顶部视为空洞
        }

        // 3. 空洞数 (Holes) 与 4. 井深 (Wells)
        for (int c = 0; c < cols; c++) {
            boolean blockFound = false;
            int currentWellDepth = 0;
            for (int r = 0; r < rows; r++) {
                if (grid[r][c] != 0) {
                    blockFound = true;
                } else {
                    if (blockFound) holes++;

                    // 检查井 (两侧都是阻挡物的空列)
                    boolean leftSolid = (c == 0) || (grid[r][c-1] != 0);
                    boolean rightSolid = (c == cols - 1) || (grid[r][c+1] != 0);
                    if (leftSolid && rightSolid) {
                        currentWellDepth++;
                        wells += currentWellDepth; // 深度递增叠加
                    }
                }
            }
        }

        // 5. 降落高度 (Landing Height)
        double lh = rows - dropHeight;

        // 综合计算总分
        return (lh * W_LANDING_HEIGHT) +
                (linesCleared * W_ROWS_CLEARED) +
                (rTrans * W_ROW_TRANSITIONS) +
                (cTrans * W_COL_TRANSITIONS) +
                (holes * W_HOLES) +
                (wells * W_WELLS);
    }

    // ================== 辅助模拟方法 ==================
    private boolean isValid(int[][] grid, Tetromino t) {
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

    private int getGhostRow(int[][] grid, Tetromino t) {
        int originalRow = t.getRow();
        int ghostRow = originalRow;
        while (true) {
            t.setRow(ghostRow + 1);
            if (!isValid(grid, t)) break;
            ghostRow++;
        }
        t.setRow(originalRow);
        return ghostRow;
    }

    private void placePiece(int[][] grid, Tetromino t) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] != 0) {
                    grid[t.getRow() + r][t.getCol() + c] = 1;
                }
            }
        }
    }

    private int clearLines(int[][] grid) {
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
                cleared++;
                r++;
            }
        }
        return cleared;
    }

    private int[][] cloneGrid(int[][] grid) {
        int[][] clone = new int[grid.length][grid[0].length];
        for (int i = 0; i < grid.length; i++) System.arraycopy(grid[i], 0, clone[i], 0, grid[0].length);
        return clone;
    }
}