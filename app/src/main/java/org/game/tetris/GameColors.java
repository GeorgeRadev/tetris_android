package org.game.tetris;

import android.graphics.Color;

/** Single color source: black, gray and white only. */
public final class GameColors {
    public static final int white = Color.rgb(0xff, 0xff, 0xff);
    public static final int black = Color.rgb(0x00, 0x00, 0x00);
    public static final int lightGray = Color.rgb(0xe3, 0xea, 0xf0);
    public static final int darkGray = Color.rgb(0x50, 0x50, 0x50);
    public static final int textBlack = Color.rgb(0x30, 0x30, 0x30);

    private GameColors() {}
}
