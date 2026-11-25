package org.game.tetris;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/** Game screen: the board in the center, controls at the bottom, scores at the top. */
public class GameActivity extends AppCompatActivity implements BoardView.Actions {

    private static final String KEY_HIGH_SCORE = "high_score";

    /** Direction of the swipe-up / top-half rotation, set from the start screen. */
    private boolean rotateLeftOnUp = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            game.tick();
            // a tick may have locked the piece; flag the buffer so the static
            // part is rebuilt if the stack actually changed
            board.markStackChanged();
            sync();
            if (!game.isGameOver()) {
                scheduleTick();
            }
        }
    };

    private TetrisGame game;
    private BoardView board;
    private NextPieceView nextView;
    private TextView scoreText;
    private TextView bestText;
    private View gameOverOverlay;
    private TextView gameOverText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        int highScore = prefs.getInt(KEY_HIGH_SCORE, 0);
        rotateLeftOnUp = prefs.getBoolean(MainActivity.KEY_ROTATE_LEFT_ON_UP, true);
        game = new TetrisGame();
        game.setHighScore(highScore);

        board = (BoardView) findViewById(R.id.board);
        board.setGame(game);
        board.setActions(this);
        nextView = (NextPieceView) findViewById(R.id.nextPiece);
        scoreText = (TextView) findViewById(R.id.scoreText);
        bestText = (TextView) findViewById(R.id.bestText);
        gameOverOverlay = (View) findViewById(R.id.gameOverOverlay);
        gameOverText = (TextView) findViewById(R.id.gameOverText);

        bindClick(R.id.backButton, new OnClickListener() {
            @Override
            public void onClick(View v) { finish(); }
        });
        bindClick(R.id.rotateLeftButton, new OnClickListener() {
            @Override
            public void onClick(View v) { rotateLeft(); }
        });
        bindClick(R.id.rotateRightButton, new OnClickListener() {
            @Override
            public void onClick(View v) { rotateRight(); }
        });
        bindClick(R.id.moveLeftButton, new OnClickListener() {
            @Override
            public void onClick(View v) { moveLeft(); }
        });
        bindClick(R.id.moveRightButton, new OnClickListener() {
            @Override
            public void onClick(View v) { moveRight(); }
        });
        bindClick(R.id.playAgainButton, new OnClickListener() {
            @Override
            public void onClick(View v) {
                game.startGame();
                board.markStackChanged();
                sync();
                if (!game.isGameOver()) scheduleTick();
            }
        });

        sync();
    }

    private void bindClick(int id, OnClickListener listener) {
        findViewById(id).setOnClickListener(listener);
    }

    private void rotateRight() {
        if (game.isGameOver()) return;
        game.rotateRight();
        sync();
    }

    /** Swipe up / tap the upper half: rotates in the chosen direction. */
    @Override
    public void rotateUp() {
        if (rotateLeftOnUp) {
            rotateLeft();
        } else {
            rotateRight();
        }
    }

    private void rotateLeft() {
        if (game.isGameOver()) return;
        game.rotateLeft();
        sync();
    }

    @Override
    public void moveLeft() {
        if (game.isGameOver()) return;
        game.moveLeft();
        sync();
    }

    @Override
    public void moveRight() {
        if (game.isGameOver()) return;
        game.moveRight();
        sync();
    }

    @Override
    public void drop() {
        if (game.isGameOver()) return;
        game.drop();
        board.markStackChanged();
        sync();
    }

    private void sync() {
        board.invalidate();
        nextView.setNextPiece(game.nextPiece());
        scoreText.setText(getString(R.string.score, game.getScore()));
        bestText.setText(getString(R.string.best, game.getHighScore()));

        if (game.isGameOver()) {
            gameOverText.setText(getString(R.string.game_over, game.getScore()));
            gameOverOverlay.setVisibility(View.VISIBLE);
        } else {
            gameOverOverlay.setVisibility(View.GONE);
        }
    }

    /** Gravity speed: 800ms at level 1, 70ms faster per level, 100ms minimum. */
    private long delay() {
        return Math.max(100, 800 - (game.getLevel() - 1) * 70);
    }

    private void scheduleTick() {
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, delay());
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onStop() {
        super.onStop();
                getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_HIGH_SCORE, game.getHighScore()).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!game.isGameOver()) {
            scheduleTick();
        }
    }
}
