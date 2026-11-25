package org.game.tetris;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Start screen: the "Tetris" title, the rotation-direction selector and a
 * Start button.
 */
public class MainActivity extends AppCompatActivity {

    public static final String PREFS = "tetris_prefs";
    public static final String KEY_ROTATE_LEFT_ON_UP = "rotate_left_on_up";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton selectLeft = (ImageButton) findViewById(R.id.rotateLeftSelect);
        ImageButton selectRight = (ImageButton) findViewById(R.id.rotateRightSelect);
        boolean rotateLeft = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_ROTATE_LEFT_ON_UP, true);
        updateSelection(selectLeft, selectRight, rotateLeft);
        selectLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDirection(true);
            }
        });
        selectRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDirection(false);
            }
        });

        TextView start = (TextView) findViewById(R.id.startButton);
        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });
    }

    /** Radio behavior: exactly one button selected at all times. */
    private void selectDirection(boolean rotateLeft) {
        ImageButton selectLeft = (ImageButton) findViewById(R.id.rotateLeftSelect);
        ImageButton selectRight = (ImageButton) findViewById(R.id.rotateRightSelect);
        updateSelection(selectLeft, selectRight, rotateLeft);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_ROTATE_LEFT_ON_UP, rotateLeft).apply();
    }

    private void updateSelection(ImageButton selectLeft, ImageButton selectRight, boolean rotateLeft) {
        int selected = R.drawable.bg_rotate_selected;
        int unselected = R.drawable.bg_rotate_unselected;
        selectLeft.setBackgroundResource(rotateLeft ? selected : unselected);
        selectRight.setBackgroundResource(!rotateLeft ? selected : unselected);
    }
}
