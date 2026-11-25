package org.game.tetris;

import java.util.Random;

/**
 * Pure Tetris game state and logic, no Android dependencies.
 *
 * Board: 10 columns x 20 rows. One of the 7 classic tetrominoes is active
 * at a time; it can move left/right, rotate left/right, and falls down
 * when {@link #tick()} is called. When it can no longer fall (or is
 * hard-dropped) it locks into the grid, full rows are cleared and the
 * score increases (100/300/500/800 x level for 1/2/3/4 lines).
 * The game is over when a newly spawned piece does not fit.
 */
public class TetrisGame {

    public static final int COLS = 10;
    public static final int ROWS = 20;

    // Piece types, also used as the "color" of locked cells (1..7).
    public static final int I = 1, O = 2, T = 3, S = 4, Z = 5, J = 6, L = 7;

    /** SHAPES[type - 1][rotation] is a flat 4x4 row-major matrix: 1 = cell. */
    private static final int[][][] SHAPES = {
        // I
        {
            {0, 0, 0, 0,
             1, 1, 1, 1,
             0, 0, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             0, 1, 0, 0,
             0, 1, 0, 0,
             0, 1, 0, 0},
            {0, 0, 0, 0,
             1, 1, 1, 1,
             0, 0, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             0, 1, 0, 0,
             0, 1, 0, 0,
             0, 1, 0, 0},
        },
        // O
        {
            {0, 0, 0, 0,
             0, 1, 1, 0,
             0, 1, 1, 0,
             0, 0, 0, 0},
            {0, 0, 0, 0,
             0, 1, 1, 0,
             0, 1, 1, 0,
             0, 0, 0, 0},
            {0, 0, 0, 0,
             0, 1, 1, 0,
             0, 1, 1, 0,
             0, 0, 0, 0},
            {0, 0, 0, 0,
             0, 1, 1, 0,
             0, 1, 1, 0,
             0, 0, 0, 0},
        },
        // T
        {
            {0, 0, 0, 0,
             1, 1, 1, 0,
             0, 1, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             1, 1, 0, 0,
             0, 1, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             1, 1, 1, 0,
             0, 0, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             0, 1, 1, 0,
             0, 1, 0, 0,
             0, 0, 0, 0},
        },
        // S
        {
            {0, 0, 0, 0,
             0, 1, 1, 0,
             1, 1, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             0, 1, 1, 0,
             0, 0, 1, 0,
             0, 0, 0, 0},
            {0, 0, 0, 0,
             0, 1, 1, 0,
             1, 1, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             0, 1, 1, 0,
             0, 0, 1, 0,
             0, 0, 0, 0},
        },
        // Z
        {
            {1, 1, 0, 0,
             0, 1, 1, 0,
             0, 0, 0, 0,
             0, 0, 0, 0},
            {0, 0, 1, 0,
             0, 1, 1, 0,
             0, 1, 0, 0,
             0, 0, 0, 0},
            {1, 1, 0, 0,
             0, 1, 1, 0,
             0, 0, 0, 0,
             0, 0, 0, 0},
            {0, 0, 1, 0,
             0, 1, 1, 0,
             0, 1, 0, 0,
             0, 0, 0, 0},
        },
        // J
        {
            {1, 0, 0, 0,
             1, 1, 1, 0,
             0, 0, 0, 0,
             0, 0, 0, 0},
            {0, 1, 1, 0,
             0, 1, 0, 0,
             0, 1, 0, 0,
             0, 0, 0, 0},
            {0, 0, 0, 0,
             1, 1, 1, 0,
             0, 0, 1, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             0, 1, 0, 0,
             1, 1, 0, 0,
             0, 0, 0, 0},
        },
        // L
        {
            {0, 0, 1, 0,
             1, 1, 1, 0,
             0, 0, 0, 0,
             0, 0, 0, 0},
            {0, 1, 0, 0,
             0, 1, 0, 0,
             0, 1, 1, 0,
             0, 0, 0, 0},
            {0, 0, 0, 0,
             1, 1, 1, 0,
             1, 0, 0, 0,
             0, 0, 0, 0},
            {1, 1, 0, 0,
             0, 1, 0, 0,
             0, 1, 0, 0,
             0, 0, 0, 0},
        },
    };

    /** Locked grid: 0 = empty, 1..7 = locked cell of that piece type. */
    private final int[][] grid = new int[ROWS][COLS];

    /** Spawn position: (col, row) of the piece matrix origin. */
    private static final int SPAWN_COL = 3;
    private static final int SPAWN_ROW = 0;

    private int piece = I;
    private int rotation = 0;
    private int nextPiece = I;
    private int pieceCol;
    private int pieceRow;

    private boolean gameOver = false;
    private long score = 0;
    private int level = 1;
    private int highScore;

    private final Random random;

    public TetrisGame() {
        this(new Random());
    }

    /** Deterministic constructor for unit tests. */
    public TetrisGame(Random random) {
        this.random = random == null ? new Random() : random;
        this.rotation = 0;
        this.pieceCol = SPAWN_COL;
        this.pieceRow = SPAWN_ROW;
        this.piece = rollPiece();
        this.nextPiece = rollPiece();
    }

    /** Draws the next random piece type (1..7). */
    private int rollPiece() {
        return random.nextInt(7) + 1;
    }

    /**
     * The flat 4x4 row-major cell matrix of [type] in [rotation]:
     * 1 where the piece has a cell, 0 otherwise. Exposed for rendering.
     */
    public static int[] shape(int type, int rotation) {
        return SHAPES[(type - 1) % 7][((rotation % 4) + 4) % 4];
    }

    /** Cell (0 or piece type) of the locked grid; row 0 is the top. */
    public int cell(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return 0;
        return grid[row][col];
    }

    public int currentPieceCol() { return pieceCol; }
    public int currentPieceRow() { return pieceRow; }
    public int currentRotation() { return rotation; }
    public int currentPieceType() { return piece; }
    public int nextPiece() { return nextPiece; }

    public long getScore() { return score; }
    public int getLevel() { return level; }
    public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) {
        this.highScore = Math.max(0, highScore);
    }
    public boolean isGameOver() { return gameOver; }

    /** True when (row, col) is inside the board and unoccupied by locked cells. */
    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS && grid[row][col] == 0;
    }

    /** True when the whole row is occupied by locked cells. */
    public boolean isFull(int row) {
        for (int c = 0; c < COLS; c++) {
            if (grid[row][c] == 0) return false;
        }
        return true;
    }

    /** Sets a locked cell directly; test support only. */
    public void setCell(int row, int col, int value) {
        if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
            grid[row][col] = value;
        }
    }

    /**
     * One gravity step: the piece moves down one row, or locks and a new
     * piece spawns. No-op when the game is over.
     */
    public void tick() {
        if (gameOver) return;
        if (canMove(pieceCol, pieceRow + 1, rotation)) {
            pieceRow++;
        } else {
            lockPiece();
        }
    }

    /** Move the current piece one column left; no-op when it cannot. */
    public void moveLeft() {
        if (!gameOver && canMove(pieceCol - 1, pieceRow, rotation)) {
            pieceCol--;
        }
    }

    /** Move the current piece one column right; no-op when it cannot. */
    public void moveRight() {
        if (!gameOver && canMove(pieceCol + 1, pieceRow, rotation)) {
            pieceCol++;
        }
    }

    /** Rotate the current piece 90 degrees counter-clockwise. */
    public void rotateLeft() {
        if (!gameOver && canMove(pieceCol, pieceRow, (rotation + 3) % 4)) {
            rotation = (rotation + 3) % 4;
        }
    }

    /** Rotate the current piece 90 degrees clockwise. */
    public void rotateRight() {
        if (!gameOver && canMove(pieceCol, pieceRow, (rotation + 1) % 4)) {
            rotation = (rotation + 1) % 4;
        }
    }

    /** Hard drop: the piece falls to the bottom of its column and locks. */
    public void drop() {
        if (gameOver) return;
        while (canMove(pieceCol, pieceRow + 1, rotation)) {
            pieceRow++;
        }
        lockPiece();
    }

    /** Clears the board, resets the score/level and spawns a new piece. */
    public void startGame() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) grid[r][c] = 0;
        }
        score = 0;
        level = 1;
        gameOver = false;
        rotation = 0;
        pieceCol = SPAWN_COL;
        pieceRow = SPAWN_ROW;
        piece = rollPiece();
        nextPiece = rollPiece();
    }

    private void lockPiece() {
        int[] shape = shape(piece, rotation);
        for (int dr = 0; dr < 4; dr++) {
            for (int dc = 0; dc < 4; dc++) {
                if (shape[dr * 4 + dc] != 1) continue;
                int r = pieceRow + dr;
                int c = pieceCol + dc;
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    grid[r][c] = piece;
                }
            }
        }

        int lines = clearLines();
        long gained;
        switch (lines) {
            case 1: gained = 100L; break;
            case 2: gained = 300L; break;
            case 3: gained = 500L; break;
            case 4: gained = 800L; break;
            default: gained = 0L;
        }
        score += gained * level;
        level = (int) (score / 1000) + 1;

        piece = nextPiece;
        nextPiece = rollPiece();
        pieceCol = SPAWN_COL;
        pieceRow = SPAWN_ROW;
        rotation = 0;

        if (!canMove(pieceCol, pieceRow, rotation)) {
            gameOver = true;
        }
        if (score > highScore) {
            highScore = (int) score;
        }
    }

    /**
     * Removes all fully locked rows, shifting the rows above them down.
     * Returns the number of lines cleared.
     */
    private int clearLines() {
        int lines = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (isFull(r)) {
                lines++;
                for (int rr = r; rr > 0; rr--) {
                    System.arraycopy(grid[rr - 1], 0, grid[rr], 0, COLS);
                }
                for (int c = 0; c < COLS; c++) grid[0][c] = 0;
                r++; // re-check this row index: another row just moved into it
            }
        }
        return lines;
    }

    private boolean canMove(int col, int row, int rot) {
        int[] shape = shape(piece, rot);
        for (int dr = 0; dr < 4; dr++) {
            for (int dc = 0; dc < 4; dc++) {
                if (shape[dr * 4 + dc] != 1) continue;
                int r = row + dr;
                int c = col + dc;
                if (r < 0 || r >= ROWS || c < 0 || c >= COLS || grid[r][c] != 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
