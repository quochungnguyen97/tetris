package com.rose.tetris.views;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.rose.tetris.presenter.GameStatus;
import com.rose.tetris.presenter.GameView;
import com.rose.tetris.presenter.Point;

class GameViewImpl implements GameView {
    private final GameFrame mGameFrame;
    private final TextView mGameScoreText;
    private final TextView mGameStatusText;
    private final Button mGameCtlBtn;

    GameViewImpl(GameFrame gameFrame, TextView gameScoreText, TextView gameStatusText, Button gameCtlBtn) {
        mGameFrame = gameFrame;
        mGameScoreText = gameScoreText;
        mGameStatusText = gameStatusText;
        mGameCtlBtn = gameCtlBtn;
    }

    @Override
    public void init(int gameSize) {
        mGameFrame.init(gameSize);
    }

    @Override
    public void draw(Point[][] points) {
        mGameFrame.setPoints(points);
        mGameFrame.invalidate();
    }

    @Override
    public void setScore(int score) {
        mGameScoreText.setText("Score: " + score);
    }

    @Override
    public void setStatus(GameStatus status) {
        mGameStatusText.setText(status.getValue());
        mGameStatusText.setVisibility(status == GameStatus.PLAYING ? View.INVISIBLE : View.VISIBLE);
        mGameCtlBtn.setText(status == GameStatus.PLAYING ? "Pause" : "Start");
    }
}
