package com.he.network;

import java.io.Serializable;

// 这是一个纯数据类，用于在网络中传输游戏快照
public class SyncData implements Serializable {
    private static final long serialVersionUID = 1L; // 保证版本兼容

    public int[][] grid;           // 固定的网格背景
    public int score;              // 当前分数
    public int linesCleared;       // 消除行数

    // 当前正在下落的方块信息（为了让对手也能看到你正在操作的方块）
    public int[][] currentShape;
    public int pieceRow;
    public int pieceCol;
    public int pieceColor;

    public boolean isGameOver;     // 游戏是否结束

    // 构造函数：注意这里把第一个参数名正式改为了 gridOld
    public SyncData(int[][] gridOld, int score, int linesCleared,
                    int[][] shape, int row, int col, int color, boolean isGameOver) {

        // 按照原数组的尺寸，开辟一块全新的内存空间
        this.grid = new int[gridOld.length][gridOld[0].length];

        // 把老数组的数据一行行拷贝进新数组（注意这里循环条件也换成了 gridOld）
        for (int i = 0; i < gridOld.length; i++) {
            System.arraycopy(gridOld[i], 0, this.grid[i], 0, gridOld[0].length);
        }

        this.score = score;
        this.linesCleared = linesCleared;
        this.currentShape = shape;
        this.pieceRow = row;
        this.pieceCol = col;
        this.pieceColor = color;
        this.isGameOver = isGameOver;
    }
}