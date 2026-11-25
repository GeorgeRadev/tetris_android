package org.game.tetris;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * The central Tetris board. White background, a black outer border and gray
 * vertical guide lines. Locked cells are drawn as light-gray squares with a
 * black outline; the active piece is drawn as white squares with a black
 * outline so it stands out.
 *
 * Image buffering: the static content (background, guide lines and the locked
 * stack) is rendered once into a bitmap that is kept in memory and only
 * rebuilt when the stack changes or the view is resized. Every frame the
 * bitmap is blitted with a single paint and only the active piece is drawn
 * on top, so the per-frame work is a bitmap blit plus at most 4 rectangles.
 *
 * Gestures: dragging left/right moves the piece, dragging up rotates it
 * left, dragging down drops it. A plain tap keeps the older behavior:
 * bottom strip drops, left half moves left, right half moves right.
 */
public class BoardView extends View {

    /** Callbacks so the view never mutates the game itself. */
    public interface Actions {
        void moveLeft();
        void moveRight();
        void rotateLeft();
        void drop();
    }

    private TetrisGame game;
    private Actions actions;
    private final float density;

    private final Paint blitPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint borderPaint;
    private final Paint guidePaint;
    private final Paint lockedFillPaint;
    private final Paint lockedStrokePaint;
    private final Paint activeFillPaint;
    private final Paint activeStrokePaint;
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();

    /** The buffered static board: valid only when not null. */
    private Bitmap buffer;
    private boolean bufferDirty = true;

    // gesture state
    private float downX;
    private float downY;
    private boolean gestureDone;

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(GameColors.black);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f * density);

        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setColor(GameColors.lightGray);
        guidePaint.setStrokeWidth(1f * density);

        lockedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lockedFillPaint.setColor(GameColors.darkGray);

        lockedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lockedStrokePaint.setColor(GameColors.black);
        lockedStrokePaint.setStyle(Paint.Style.STROKE);
        lockedStrokePaint.setStrokeWidth(1f * density);

        activeFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeFillPaint.setColor(GameColors.lightGray);

        activeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeStrokePaint.setColor(GameColors.black);
        activeStrokePaint.setStyle(Paint.Style.STROKE);
        activeStrokePaint.setStrokeWidth(1.5f * density);
    }

    public void setGame(TetrisGame game) {
        this.game = game;
        bufferDirty = true;
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    /** Flags the buffered stack as out of date (call after any game change). */
    public void markStackChanged() {
        bufferDirty = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availW = MeasureSpec.getSize(widthMeasureSpec);
        int availH = MeasureSpec.getSize(heightMeasureSpec);
        // keep the 10:20 (1:2) board ratio, fitting both the width and height
        int w = Math.min(availW, (availH / TetrisGame.ROWS) * TetrisGame.COLS);
        int h = (w / TetrisGame.COLS) * TetrisGame.ROWS;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bufferDirty = true;
    }

    private float cellSize() {
        return getWidth() / (float) TetrisGame.COLS;
    }

    /** Rebuilds the bitmap: background, vertical guides and the locked stack. */
    private void rebuildBuffer() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        Bitmap old = buffer;
        buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(buffer);

        // white background
        c.drawColor(GameColors.white);

        // gray vertical guide lines between columns
        float cell = cellSize();
        for (int col = 1; col < TetrisGame.COLS; col++) {
            float x = col * cell;
            c.drawLine(x, 0f, x, h, guidePaint);
        }

        // locked cells
        if (game != null) {
            float inset = 1f * density;
            for (int r = 0; r < TetrisGame.ROWS; r++) {
                for (int col = 0; col < TetrisGame.COLS; col++) {
                    if (game.cell(r, col) == 0) continue;
                    dstRect.set(
                            col * cell + inset,
                            r * cell + inset,
                            (col + 1) * cell - inset,
                            (r + 1) * cell - inset
                    );
                    c.drawRect(dstRect, lockedFillPaint);
                    c.drawRect(dstRect, lockedStrokePaint);
                }
            }
        }

        if (old != null) old.recycle();
        bufferDirty = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (bufferDirty || buffer == null) {
            rebuildBuffer();
        }

        // blit the static board in one draw
        srcRect.set(0, 0, buffer.getWidth(), buffer.getHeight());
        dstRect.set(0f, 0f, w, h);
        canvas.drawBitmap(buffer, srcRect, dstRect, blitPaint);

        // the active piece on top (the only per-frame vector drawing)
        if (game != null && !game.isGameOver()) {
            drawActivePiece(canvas);
        }

        // black outer border on top of everything
        canvas.drawRect(0f, 0f, w, h, borderPaint);
    }

    private void drawActivePiece(Canvas canvas) {
        float cell = cellSize();
        int col = game.currentPieceCol();
        int row = game.currentPieceRow();
        int[] shape = TetrisGame.shape(game.currentPieceType(), game.currentRotation());
        float inset = 1f * density;
        for (int dr = 0; dr < 4; dr++) {
            for (int dc = 0; dc < 4; dc++) {
                if (shape[dr * 4 + dc] != 1) continue;
                int r = row + dr;
                int c = col + dc;
                if (r < 0 || r >= TetrisGame.ROWS || c < 0 || c >= TetrisGame.COLS) continue;
                dstRect.set(
                        c * cell + inset,
                        r * cell + inset,
                        (c + 1) * cell - inset - 1,
                        (r + 1) * cell - inset - 1
                );
                canvas.drawRect(dstRect, activeFillPaint);
                canvas.drawRect(dstRect, activeStrokePaint);
            }
        }
    }

    /** How far a drag must travel before it counts as a gesture. */
    private static final int DRAG_PX = 40;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (game == null || actions == null) return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                gestureDone = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                handleDrag(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                if (gestureDone) {
                    return true;
                }
                if (game.isGameOver()) return true;
                float x = event.getX();
                float y = event.getY();
                if (y > getHeight() * 0.8f) {
                    actions.drop();
                    bufferDirty = true; // the stack changed
                } else if (x < getWidth() / 2f) {
                    actions.moveLeft();
                } else {
                    actions.moveRight();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /**
     * Turns a drag into a gesture once it passes the threshold. The origin
     * resets after each gesture, so a long continuous drag can move or
     * rotate the piece repeatedly.
     */
    private void handleDrag(float x, float y) {
        if (gestureDone || game.isGameOver()) return;
        float dx = x - downX;
        float dy = y - downY;
        float threshold = DRAG_PX * density;
        if (Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy)) {
            // horizontal drag: move the piece in the drag direction
            if (dx < 0) {
                actions.moveLeft();
            } else {
                actions.moveRight();
            }
        } else if (Math.abs(dy) >= threshold) {
            // vertical drag: up rotates left, down drops
            if (dy < 0) {
                actions.rotateLeft();
            } else {
                actions.drop();
                bufferDirty = true; // the stack changed
            }
        } else {
            return;
        }
        downX = x;
        downY = y;
        gestureDone = true;
    }
}
