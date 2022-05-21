package com.rose.tetris.presenter;

public class Point {
    public final int x, y;
    public boolean isFallingPoint;
    public PointType type;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        this.type = PointType.EMPTY;
        this.isFallingPoint = false;
    }

    public Point(int x, int y, PointType type, boolean isFallingPoint) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.isFallingPoint = isFallingPoint;
    }

    public boolean isStablePoint() {
        return !isFallingPoint && type == PointType.BOX;
    }
}
