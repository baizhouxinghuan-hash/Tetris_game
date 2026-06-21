package com.he.model;

public class Tetromino {
    private int[][] shape;
    private int row;
    private int col;
    private int colorIndex;

    public Tetromino(int[][] shape, int colorIndex) {
        setShape(shape);
        this.row = 0;
        this.col = 0;
        this.colorIndex = colorIndex;
    }

    public int[][] getShape() { return shape; }
    public void setShape(int[][] newShape) {
        this.shape = new int[newShape.length][newShape[0].length];
        for (int r = 0; r < newShape.length; r++) {
            System.arraycopy(newShape[r], 0, this.shape[r], 0, newShape[0].length);
        }
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
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = shape[r][c];
            }
        }
        shape = rotated;
    }

    public void rotateBack() {
        int rows = shape.length, cols = shape[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[cols - 1 - c][r] = shape[r][c];
            }
        }
        shape = rotated;
    }
}