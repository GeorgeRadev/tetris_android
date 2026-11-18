package org.game.tetris;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** White background with a grid of evenly spaced light gray lines. */
public class GridBackgroundView extends View {

    private final Paint fillPaint;
    private final Paint linePaint;
    private final float spacing;

    public GridBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        spacing = 32f * density;
        fillPaint = new Paint();
        fillPaint.setColor(GameColors.white);
        linePaint = new Paint();
        linePaint.setColor(GameColors.lightGray);
        linePaint.setStrokeWidth(density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), fillPaint);
        for (float x = 0f; x <= getWidth(); x += spacing) {
            canvas.drawLine(x, 0f, x, getHeight(), linePaint);
        }
        for (float y = 0f; y <= getHeight(); y += spacing) {
            canvas.drawLine(0f, y, getWidth(), y, linePaint);
        }
    }
}
