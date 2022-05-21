package com.rose.tetris.presenter;

public enum GameStatus {
    START("START"),
    PLAYING("PLAYING"),
    OVER("GAME OVER"),
    PAUSED("GAME PAUSED");

    private final String value;

    GameStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
