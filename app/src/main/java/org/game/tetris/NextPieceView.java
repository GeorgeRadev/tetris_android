package org.game.tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Small preview of the next piece: a 4x4 area where the piece is drawn
 * centered, as white squares with a black outline.
 */
public class NextPieceView extends View {

    private int nextPiece = TetrisGame.I;

    private final Paint activeFillPaint;
    private final Paint activeStrokePaint;
    private final RectF rect = new RectF();

    public NextPieceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        activeFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeFillPaint.setColor(GameColors.white);
        activeStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeStrokePaint.setColor(GameColors.black);
        activeStrokePaint.setStyle(Paint.Style.STROKE);
        activeStrokePaint.setStrokeWidth(1.5f * density);
    }

    public void setNextPiece(int pieceType) {
        this.nextPiece = pieceType;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float side = Math.min(getWidth(), getHeight());
        float cell = side / 4f;
        float ox = (getWidth() - side) / 2f;
        float oy = (getHeight() - side) / 2f;

        // center the piece's bounding box
        int[] shape = TetrisGame.shape(nextPiece, 0);
        int minR = 4, maxR = -1, minC = 4, maxC = -1;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r * 4 + c] != 1) continue;
                minR = Math.min(minR, r);
                maxR = Math.max(maxR, r);
                minC = Math.min(minC, c);
                maxC = Math.max(maxC, c);
            }
        }
        float pad = 2f * getResources().getDisplayMetrics().density;
        for (int r = minR; r <= maxR; r++) {
            for (int c = minC; c <= maxC; c++) {
                if (shape[r * 4 + c] != 1) continue;
                rect.set(
                        ox + c * cell + pad,
                        oy + r * cell + pad,
                        ox + (c + 1) * cell - pad,
                        oy + (r + 1) * cell - pad
                );
                canvas.drawRect(rect, activeFillPaint);
                canvas.drawRect(rect, activeStrokePaint);
            }
        }
    }
}
