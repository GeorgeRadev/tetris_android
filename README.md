# Tetris (Android)

A minimal, dependency-light Tetris for Android, written in Java. The game logic is a pure, Android-free class, so it is easy to test; the UI is a small set of custom views that render the board.

## Features

- Classic 10 × 20 board with all 7 tetrominoes
- On-screen control buttons (rotate left/right, move left/right)
- Touch gestures on the board (see below)
- Configurable rotation direction for the swipe-up gesture (chosen on the start screen)
- Next-piece preview, score, level and best score (persisted between sessions)
- Score: 100 / 300 / 500 / 800 for clearing 1 / 2 / 3 / 4 lines (no level multiplier)
- Level increases every 10,000 points; gravity speeds up with each level (800 ms at level 1, −70 ms per level, 100 ms minimum)

## Controls

### Gestures (touch the board)

| Gesture | Action |
| --- | --- |
| Drag left | Move left |
| Drag right | Move right |
| Drag up | Rotate (direction chosen on the start screen) |
| Drag down | Hard drop |

A drag can be extended to repeat the same action (e.g. drag far left to move several cells).

### Taps (touch the board)

| Zone | Action |
| --- | --- |
| Upper half (above 50%) | Rotate (direction chosen on the start screen) |
| Middle band (50%–90%) | Move left (left half) / move right (right half) |
| Bottom strip (below 90%) | Hard drop |

### Buttons

The start screen has two radio-style buttons that select which way "swipe up" / "tap the upper half" rotates. The choice is saved and applies to the next game.

The game screen has four icon buttons: rotate left, rotate right, move left, move right.

## Building

Requirements: JDK 11+, Android SDK 37 (compileSdk/targetSdk), minSdk 29 (Android 10).

```sh
./gradlew assembleDebug   # build the debug APK
./gradlew test            # run the pure-logic unit tests
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Project layout

```
app/src/main/java/org/game/tetris/
├── TetrisGame.java          # pure game state & logic (no Android imports)
├── MainActivity.java        # start screen: rotation-direction selector + Start
├── GameActivity.java        # game screen: game loop, scores, buttons
├── BoardView.java           # board rendering (bitmap-cached) + gestures
├── NextPieceView.java       # next-piece preview
├── GridBackgroundView.java  # decorative grid background
└── GameColors.java          # shared colors
```

## Tests

Unit tests in `app/src/test/java/org/game/tetris/TetrisGameTest.java` exercise the pure game logic (movement, rotation, line clearing, locking, game over).

## License

This project is provided as-is for personal use; no license is granted.
