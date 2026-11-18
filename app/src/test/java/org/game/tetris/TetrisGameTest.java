package org.game.tetris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/**
 * Unit tests for the pure Tetris game logic.
 *
 * Geometry used throughout (spawn = col 3, row 0, rotation 0), where each
 * piece's 4x4 box is offset by (pieceCol, pieceRow):
 *   I rot0 = row1 cols0-3   I rot1 = col2 rows0-3
 *   O rot0 = rows0-1 cols1-2 (invariant)
 */
public class TetrisGameTest {

    /** Fixed piece sequence: each nextInt(7) returns the given index 0..6, cycling. */
    private static final class SequenceRandom extends Random {
        private final int[] sequence;
        private int index;

        SequenceRandom(int... sequence) {
            super(0);
            this.sequence = sequence;
        }

        @Override
        public int nextInt(int bound) {
            return sequence[index++ % sequence.length];
        }
    }

    /** New game whose piece queue is the given piece types (1..7), cycling. */
    private static TetrisGame game(int... pieces) {
        int[] indexes = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) indexes[i] = pieces[i] - 1;
        return new TetrisGame(new SequenceRandom(indexes));
    }

    /** Always-O game. */
    private static TetrisGame oGame() {
        return game(TetrisGame.O);
    }

    /** Fills the given rows with locked cells, except columns 4 and 5. */
    private static void fillBottom(TetrisGame g, int rowsFromBottom) {
        for (int r = TetrisGame.ROWS - rowsFromBottom; r < TetrisGame.ROWS; r++) {
            for (int c = 0; c < TetrisGame.COLS; c++) {
                if (c == 4 || c == 5) continue;
                g.setCell(r, c, 1);
            }
        }
    }

    @Test
    public void newGameStartsInSpawnPosition() {
        TetrisGame g = game(TetrisGame.I, TetrisGame.T);
        assertFalse(g.isGameOver());
        assertEquals(3, g.currentPieceCol());
        assertEquals(0, g.currentPieceRow());
        assertEquals(0, g.currentRotation());
        assertEquals(TetrisGame.I, g.currentPieceType());
        assertEquals(TetrisGame.T, g.nextPiece());
        assertEquals(0, g.getScore());
        assertEquals(1, g.getLevel());
        assertEquals(0, g.cell(0, 0));
    }

    @Test
    public void pieceFallsOnTick() {
        TetrisGame g = oGame();
        for (int i = 0; i < 5; i++) {
            g.tick();
            assertEquals(i + 1, g.currentPieceRow());
        }
    }

    @Test
    public void pieceLocksAtTheBottom() {
        TetrisGame g = oGame();
        // O is 2 rows tall; its lowest cell lands on row 19 when pieceRow == 18
        for (int i = 0; i < TetrisGame.ROWS - 2; i++) {
            g.tick();
        }
        assertEquals(TetrisGame.ROWS - 2, g.currentPieceRow());
        g.tick(); // locks
        assertEquals(0, g.currentPieceRow()); // new O spawned at the top
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 1, 4));
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 1, 5));
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 2, 4));
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 2, 5));
    }

    @Test
    public void moveLeftAndRightRespectTheWalls() {
        TetrisGame g = oGame(); // O occupies cols col+1, col+2
        // left wall: cols 0,1 reached at col -1 (4 moves from spawn col 3)
        for (int i = 0; i < 4; i++) g.moveLeft();
        assertEquals(-1, g.currentPieceCol());
        g.moveLeft();
        assertEquals(-1, g.currentPieceCol());
        // right wall: cols 8,9 reached at col 7 (8 moves from col -1)
        for (int i = 0; i < 8; i++) g.moveRight();
        assertEquals(7, g.currentPieceCol());
        g.moveRight();
        assertEquals(7, g.currentPieceCol());
    }

    @Test
    public void moveIntoTheStackIsBlocked() {
        TetrisGame g = oGame(); // at spawn occupies cols 4,5
        g.setCell(1, 2, 1);
        g.moveLeft(); // col 2: occupies cols 3,4
        assertEquals(2, g.currentPieceCol());
        g.moveLeft(); // would occupy cols 2,3 -> overlaps the locked (1,2)
        assertEquals(2, g.currentPieceCol());
        g.moveRight();
        g.moveRight(); // col 4: occupies cols 5,6
        assertEquals(4, g.currentPieceCol());
    }

    @Test
    public void rotateRightAndLeft() {
        TetrisGame g = game(TetrisGame.I);
        g.rotateRight();
        assertEquals(1, g.currentRotation());
        g.rotateLeft();
        assertEquals(0, g.currentRotation());
        g.rotateLeft();
        assertEquals(3, g.currentRotation());
        g.rotateRight();
        assertEquals(0, g.currentRotation());
    }

    @Test
    public void rotationBlockedByTheStack() {
        TetrisGame g = game(TetrisGame.I);
        g.setCell(2, 5, 1);
        // I rot0 -> rot1 is a vertical bar at col col+2 = 5, which passes (2,5)
        g.rotateRight();
        assertEquals(0, g.currentRotation());
    }

    @Test
    public void rotationBlockedAtTheWall() {
        TetrisGame g = game(TetrisGame.I);
        g.rotateRight(); // vertical bar at col col+2
        assertEquals(1, g.currentRotation());
        // push the vertical bar to the right wall: max col is 7 (bar at col 9)
        for (int i = 0; i < 4; i++) g.moveRight();
        assertEquals(7, g.currentPieceCol());
        g.moveRight(); // bar would be at col 10
        assertEquals(7, g.currentPieceCol());
        g.rotateRight(); // horizontal would span cols 7..10: out of bounds
        assertEquals(1, g.currentRotation());
        g.rotateLeft(); // horizontal at cols 7..10: also blocked
        assertEquals(1, g.currentRotation());
        // with room to spare, the next rotation is allowed
        g.moveLeft(); // col 6
        g.moveLeft(); // col 5
        int before = g.currentRotation();
        g.rotateRight();
        assertEquals((before + 1) % 4, g.currentRotation());
    }

    @Test
    public void dropLocksImmediately() {
        TetrisGame g = oGame();
        g.drop();
        assertEquals(0, g.currentPieceRow()); // new piece spawned
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 1, 4));
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 2, 4));
    }

    @Test
    public void singleLineClearScores100() {
        TetrisGame g = oGame();
        fillBottom(g, 1);
        g.drop(); // O lands on the bottom row, completing it
        assertEquals(100, g.getScore());
        assertEquals(1, g.getLevel());
        // the row was cleared; the O cells of the row above shifted to the bottom
        assertEquals(0, g.cell(TetrisGame.ROWS - 1, 0));
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 1, 4));
        assertEquals(TetrisGame.O, g.cell(TetrisGame.ROWS - 1, 5));
        assertEquals(100, g.getHighScore());
    }

    @Test
    public void doubleLineClearScores300() {
        TetrisGame g = oGame();
        fillBottom(g, 2);
        g.drop(); // O completes both bottom rows
        assertEquals(300, g.getScore());
        // both full rows are cleared (the O cells were part of them), so the
        // bottom two rows are empty
        assertEquals(0, g.cell(TetrisGame.ROWS - 1, 4));
        assertEquals(0, g.cell(TetrisGame.ROWS - 1, 0));
        assertEquals(0, g.cell(TetrisGame.ROWS - 2, 4));
        assertEquals(0, g.cell(TetrisGame.ROWS - 2, 0));
    }

    @Test
    public void levelUpsEvery1000Points() {
        TetrisGame g = oGame();
        for (int i = 0; i < 4; i++) {
            fillBottom(g, 2);
            g.drop(); // +300 each at level 1
        }
        assertEquals(1200, g.getScore());
        assertEquals(2, g.getLevel());
    }

    @Test
    public void startGameResets() {
        TetrisGame g = oGame();
        g.drop();
        g.moveLeft();
        g.startGame();
        assertFalse(g.isGameOver());
        assertEquals(0, g.getScore());
        assertEquals(1, g.getLevel());
        assertEquals(3, g.currentPieceCol());
        assertEquals(0, g.currentPieceRow());
        assertEquals(TetrisGame.O, g.currentPieceType());
        for (int r = 0; r < TetrisGame.ROWS; r++) {
            for (int c = 0; c < TetrisGame.COLS; c++) {
                assertEquals(0, g.cell(r, c));
            }
        }
    }

    @Test
    public void highScoreIsKept() {
        TetrisGame g = oGame();
        fillBottom(g, 1);
        g.drop();
        assertEquals(100, g.getHighScore());
        g.setHighScore(999);
        assertEquals(999, g.getHighScore());
        g.setHighScore(-5);
        assertEquals(0, g.getHighScore());
    }

    @Test
    public void gameEndsWhenStackReachesTheTop() {
        TetrisGame g = oGame();
        // 10 O pieces fill cols 4,5 from row 19 up to row 0; the next spawn is blocked
        for (int i = 0; i < 10; i++) g.drop();
        assertTrue(g.isGameOver());
    }

    @Test
    public void noMovesAfterGameOver() {
        TetrisGame g = oGame();
        for (int i = 0; i < 10; i++) g.drop();
        assertTrue(g.isGameOver());
        int row = g.currentPieceRow();
        int col = g.currentPieceCol();
        int rot = g.currentRotation();
        g.tick();
        g.moveLeft();
        g.moveRight();
        g.rotateLeft();
        g.rotateRight();
        g.drop();
        assertEquals(row, g.currentPieceRow());
        assertEquals(col, g.currentPieceCol());
        assertEquals(rot, g.currentRotation());
    }

    @Test
    public void pieceTakesTheNextPieceOnLock() {
        TetrisGame g = game(TetrisGame.T, TetrisGame.S, TetrisGame.I);
        assertEquals(TetrisGame.T, g.currentPieceType());
        assertEquals(TetrisGame.S, g.nextPiece());
        g.drop();
        assertEquals(TetrisGame.S, g.currentPieceType());
        assertEquals(TetrisGame.I, g.nextPiece());
    }
}
