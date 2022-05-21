package com.rose.tetris.models;

import com.rose.tetris.presenter.GameModel;

public class GameModelFactory {
    private GameModelFactory() {
    }

    public static GameModel newGameModel(GameType gameType) {
        switch (gameType) {
            case TETRIS:
                return new TetrisGameModel();
            default:
                return null;
        }
    }
}
