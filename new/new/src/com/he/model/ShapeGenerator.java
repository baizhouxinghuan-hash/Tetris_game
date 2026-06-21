package com.he.model;

import java.util.ArrayList;
import java.util.List;

public class ShapeGenerator {
    public static List<int[][]> generateAllConnectedShapes() {
        List<int[][]> shapes = new ArrayList<>();
        boolean[][][] used = new boolean[5][5][5];
        for (int blockCount = 1; blockCount <= 4; blockCount++) {
            boolean[][] grid = new boolean[4][4];
            generateShapesRecursive(shapes, grid, 0, 0, blockCount, 0, used);
        }
        // 特制定制复合异形十字体
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
}