package com.rose.tetris.models;

import android.os.Handler;
import android.util.Log;

import com.rose.tetris.presenter.GameModel;
import com.rose.tetris.presenter.GameTurn;
import com.rose.tetris.presenter.Point;
import com.rose.tetris.presenter.PointType;
import com.rose.tetris.presenter.PresenterCompletableObserver;
import com.rose.tetris.presenter.PresenterObserver;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

class TetrisGameModel implements GameModel {
    private static final String TAG = "TetrisGameModel";

    private static final int GAME_SIZE = 15;
    private static final int PLAYING_AREA_WIDTH = 10;
    private static final int PLAYING_AREA_HEIGHT = GAME_SIZE;
    private static final int UPCOMING_AREA_SIZE = 4;

    private Point[][] mPoints;
    private Point[][] mPlayingPoints;
    private Point[][] mUpcomingPoints;
    private int mScore;
    private final AtomicBoolean mIsGamePaused = new AtomicBoolean();
    private final AtomicBoolean mIsTurning = new AtomicBoolean();
    private final LinkedList<Point> mFallingPoints = new LinkedList<>();

    private PresenterCompletableObserver mGameOverObserver;
    private PresenterObserver<Integer> mScoreUpdatedObserver;

    private final Handler mHandler = new Handler();

    private enum BrickType {
        L(0), T(1), CHAIR(2), STICK(3), SQUARE(4);
        final int value;

        BrickType(int value) {
            this.value = value;
        }

        static BrickType fromValue(int value) {
            switch (value) {
                case 1:
                    return T;
                case 2:
                    return CHAIR;
                case 3:
                    return STICK;
                case 4:
                    return SQUARE;
                case 0:
                default:
                    return L;
            }
        }

        static BrickType random() {
            Random random = new Random();
            return fromValue(random.nextInt(5));
        }
    }

    @Override
    public void init() {
        mPoints = new Point[GAME_SIZE][GAME_SIZE];
        for (int i = 0; i < GAME_SIZE; i++) {
            for (int j = 0; j < GAME_SIZE; j++) {
                mPoints[i][j] = new Point(j, i);
            }
        }
        mPlayingPoints = new Point[PLAYING_AREA_HEIGHT][PLAYING_AREA_WIDTH];
        for (int i = 0; i < PLAYING_AREA_HEIGHT; i++) {
            System.arraycopy(mPoints[i], 0, mPlayingPoints[i], 0, PLAYING_AREA_WIDTH);
        }
        mUpcomingPoints = new Point[UPCOMING_AREA_SIZE][UPCOMING_AREA_SIZE];
        for (int i = 0; i < UPCOMING_AREA_SIZE; i++) {
            for (int j = 0; j < UPCOMING_AREA_SIZE; j++) {
                mUpcomingPoints[i][j] = mPoints[1 + i][PLAYING_AREA_WIDTH + 1 + j];
            }
        }
        for (int i = 0; i < PLAYING_AREA_HEIGHT; i++) {
            mPoints[i][PLAYING_AREA_WIDTH].type = PointType.VERTICAL_LINE;
        }
        newGame();
    }

    @Override
    public int getGameSize() {
        return GAME_SIZE;
    }

    @Override
    public void newGame() {
        mScore = 0;
        for (int i = 0; i < PLAYING_AREA_HEIGHT; i++) {
            for (int j = 0; j < PLAYING_AREA_WIDTH; j++) {
                mPlayingPoints[i][j].type = PointType.EMPTY;
            }
        }
        mFallingPoints.clear();
        generateUpcomingBrick();
    }

    private void generateUpcomingBrick() {
        BrickType upcomingBrick = BrickType.random();
        for (int i = 0; i < UPCOMING_AREA_SIZE; i++) {
            for (int j = 0; j < UPCOMING_AREA_SIZE; j++) {
                mUpcomingPoints[i][j].type = PointType.EMPTY;
            }
        }
        switch (upcomingBrick) {
            case L:
                mUpcomingPoints[1][1].type = PointType.BOX;
                mUpcomingPoints[2][1].type = PointType.BOX;
                mUpcomingPoints[3][1].type = PointType.BOX;
                mUpcomingPoints[3][2].type = PointType.BOX;
                break;
            case T:
                mUpcomingPoints[1][1].type = PointType.BOX;
                mUpcomingPoints[2][1].type = PointType.BOX;
                mUpcomingPoints[3][1].type = PointType.BOX;
                mUpcomingPoints[2][2].type = PointType.BOX;
                break;
            case CHAIR:
                mUpcomingPoints[1][1].type = PointType.BOX;
                mUpcomingPoints[2][1].type = PointType.BOX;
                mUpcomingPoints[2][2].type = PointType.BOX;
                mUpcomingPoints[3][2].type = PointType.BOX;
                break;
            case STICK:
                mUpcomingPoints[0][1].type = PointType.BOX;
                mUpcomingPoints[1][1].type = PointType.BOX;
                mUpcomingPoints[2][1].type = PointType.BOX;
                mUpcomingPoints[3][1].type = PointType.BOX;
                break;
            case SQUARE:
                mUpcomingPoints[1][1].type = PointType.BOX;
                mUpcomingPoints[1][2].type = PointType.BOX;
                mUpcomingPoints[2][1].type = PointType.BOX;
                mUpcomingPoints[2][2].type = PointType.BOX;
                break;
        }

    }

    @Override
    public void startGame(PresenterObserver<Point[][]> onGameDrawnListener) {
        mIsGamePaused.set(false);
        final long sleepTime = 1000 / FPS;
        new Thread(() -> {
            long count = 0;
            while (!mIsGamePaused.get()) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (count % SPEED == 0) {
                    if (mIsTurning.get()) {
                        continue;
                    }
                    next();
                    mHandler.post(() -> onGameDrawnListener.observe(mPoints));
                }
                count++;
            }
        }).start();
    }

    private synchronized void next() {
        updateFallingPoints();

        if (isNextMerged()) {
            if (isOutSide()) {
                if (mGameOverObserver != null) {
                    mHandler.post(mGameOverObserver::onNext);
                }
                mIsGamePaused.set(true);
                return;
            }
            int y = mFallingPoints.stream().mapToInt(p -> p.y).max().orElse(-1);
            while (y >= 0) {
                boolean isScored = true;
                for (int i = 0; i < PLAYING_AREA_WIDTH; i++) {
                    Point point = getPlayingPoint(i, y);
                    if (point.type == PointType.EMPTY) {
                        isScored = false;
                        break;
                    }
                }
                if (isScored) {
                    mScore++;
                    if (mScoreUpdatedObserver != null) {
                        mHandler.post(() -> mScoreUpdatedObserver.observe(mScore));
                    }
                    LinkedList<Point> tmPoints = new LinkedList<>();
                    for (int i = 0; i <= y; i++) {
                        for (int j = 0; j < PLAYING_AREA_WIDTH; j++) {
                            Point point = getPlayingPoint(j, i);
                            if (point.type == PointType.BOX) {
                                point.type = PointType.EMPTY;
                                if (i != y) {
                                    tmPoints.add(new Point(point.x, point.y + 1, PointType.BOX, false));
                                }

                            }
                        }
                    }
                    tmPoints.forEach(this::updatePlayingPoint);
                } else {
                    y--;
                }
            }
            mFallingPoints.forEach(p -> p.isFallingPoint = false);
            mFallingPoints.clear();
        } else {
            LinkedList<Point> tmPoints = new LinkedList<>();
            for (Point fallingPoint : mFallingPoints) {
                fallingPoint.type = PointType.EMPTY;
                fallingPoint.isFallingPoint = false;
                tmPoints.add(new Point(fallingPoint.x, fallingPoint.y + 1, PointType.BOX, true));
            }
            mFallingPoints.clear();
            mFallingPoints.addAll(tmPoints);
            mFallingPoints.forEach(this::updatePlayingPoint);
        }

    }

    private boolean isNextMerged() {
        for (Point fallingPoint : mFallingPoints) {
            if (fallingPoint.y + 1 >= 0 && (fallingPoint.y == PLAYING_AREA_HEIGHT - 1 ||
                    getPlayingPoint(fallingPoint.x, fallingPoint.y + 1).isStablePoint())) {
                return true;
            }
        }
        return false;
    }

    private boolean isOutSide() {
        for (Point fallingPoint : mFallingPoints) {
            if (fallingPoint.y < 0) {
                return true;
            }
        }
        return false;
    }

    private void updatePlayingPoint(Point point) {
        if (point.x >= 0 && point.x < PLAYING_AREA_WIDTH &&
                point.y >= 0 && point.y < PLAYING_AREA_HEIGHT) {
            mPoints[point.y][point.x] = point;
            mPlayingPoints[point.y][point.x] = point;
        }
    }

    private Point getPlayingPoint(int x, int y) {
        if (x >= 0 && y >= 0 && x < PLAYING_AREA_WIDTH && y < PLAYING_AREA_HEIGHT) {
            return mPlayingPoints[y][x];
        }
        return null;
    }

    private void updateFallingPoints() {
        if (mFallingPoints.isEmpty()) {
            for (int i = 0; i < UPCOMING_AREA_SIZE; i++) {
                for (int j = 0; j < UPCOMING_AREA_SIZE; j++) {
                    if (mUpcomingPoints[i][j].type == PointType.BOX) {
                        mFallingPoints.add(new Point(j + 3, i - 4, PointType.BOX, true));
                    }
                }
            }
            generateUpcomingBrick();
        }
    }

    @Override
    public void pauseGame() {
        mIsGamePaused.set(true);
    }

    @Override
    public void turn(GameTurn turn) {
        if (mIsGamePaused.get() || mIsTurning.get()) {
            return;
        }
        mIsTurning.set(true);
        LinkedList<Point> tmPoints;
        boolean canTurn;
        switch (turn) {
            case LEFT:
                updateFallingPoints();
                canTurn = true;
                for (Point fallingPoint : mFallingPoints) {
                    if (fallingPoint.y >= 0 && (fallingPoint.x == 0 ||
                            getPlayingPoint(fallingPoint.x - 1, fallingPoint.y).isStablePoint())) {
                        canTurn = false;
                        break;
                    }
                }
                if (canTurn) {
                    tmPoints = new LinkedList<>();
                    for (Point fallingPoint : mFallingPoints) {
                        tmPoints.add(new Point(fallingPoint.x - 1, fallingPoint.y, PointType.BOX, true));
                        fallingPoint.type = PointType.EMPTY;
                        fallingPoint.isFallingPoint = false;
                    }
                    mFallingPoints.clear();
                    mFallingPoints.addAll(tmPoints);
                    mFallingPoints.forEach(this::updatePlayingPoint);
                }
                break;
            case RIGHT:
                updateFallingPoints();
                canTurn = true;
                for (Point fallingPoint : mFallingPoints) {
                    if (fallingPoint.y >= 0 && (fallingPoint.x == PLAYING_AREA_WIDTH - 1 ||
                            getPlayingPoint(fallingPoint.x + 1, fallingPoint.y).isStablePoint())) {
                        canTurn = false;
                        break;
                    }
                }
                if (canTurn) {
                    tmPoints = new LinkedList<>();
                    for (Point fallingPoint : mFallingPoints) {
                        tmPoints.add(new Point(fallingPoint.x + 1, fallingPoint.y, PointType.BOX, true));
                        fallingPoint.type = PointType.EMPTY;
                        fallingPoint.isFallingPoint = false;
                    }
                    mFallingPoints.clear();
                    mFallingPoints.addAll(tmPoints);
                    mFallingPoints.forEach(this::updatePlayingPoint);
                }
                break;
            case DOWN:
                next();
                break;
            case FIRE:
                rotateFallingPoints();
            case UP:
            default:
                break;
        }
        mIsTurning.set(false);
    }

    private void rotateFallingPoints() {
        updateFallingPoints();
        int left = mFallingPoints.stream().mapToInt(p -> p.x).min().orElse(-1);
        int right = mFallingPoints.stream().mapToInt(p -> p.x).max().orElse(-1);
        int top = mFallingPoints.stream().mapToInt(p -> p.y).min().orElse(-1);
        int bottom = mFallingPoints.stream().mapToInt(p -> p.y).max().orElse(-1);
        int size = Math.max(right - left, bottom - top) + 1;
        if (rotatePoints(left, top, size)) {
            return;
        }
        if (rotatePoints(right - size + 1, top, size)) {
            return;
        }
        if (rotatePoints(left, bottom - size + 1, size)) {
            return;
        }
        rotatePoints(right - size + 1, bottom - size + 1, size);
    }

    public boolean rotatePoints(int x, int y, int size) {
        Log.i(TAG, "rotatePoints: x = " + x + ", y = " + y + ", size = " + size);
        if (x + size - 1 < 0 || x + size - 1 >= PLAYING_AREA_WIDTH) {
            return false;
        }
        boolean canRotate = true;
        Point[][] points = new Point[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Point point = getPlayingPoint(j + x, i + y);
                if (point == null) {
                    final int tmX = j + x;
                    final int tmY = i + y;
                    point = mFallingPoints.stream()
                            .filter(p -> p.x == tmX && p.y == tmY)
                            .findFirst()
                            .orElse(new Point(j + x, i + y));
                }
                if (point.isStablePoint() && getPlayingPoint(x + size - 1 - i, y + j).isFallingPoint) {
                    canRotate = false;
                    break;
                }
                if (point.isFallingPoint) {
                    Log.i(TAG, "rotatePoints: Falling point: " + point.x + ", " + point.y + "; type: " + point.type);
                }
                points[i][j] = new Point(x + size - 1 - i, y + j, point.type, point.isFallingPoint);
            }
            if (!canRotate) {
                break;
            }
        }
        for (Point point: mFallingPoints) {
            Log.i(TAG, "rotatePoints: Falling point 2: " + point.x + ", " + point.y + ", type = " + point.type);
        }
        if (!canRotate) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Point point = getPlayingPoint(i + y, j + x);
                if (point == null) {
                    continue;
                }
                point.type = PointType.EMPTY;
            }
        }
        mFallingPoints.clear();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                updatePlayingPoint(points[i][j]);
                if (points[i][j].isFallingPoint) {
                    mFallingPoints.add(points[i][j]);
                }
            }
        }
        Log.i(TAG, "rotatePoints: falling points size = " + mFallingPoints.size());
        return true;
    }

    @Override
    public void setGameOverListener(PresenterCompletableObserver onGameOverListener) {
        mGameOverObserver = onGameOverListener;
    }

    @Override
    public void setScoreUpdatedListener(PresenterObserver<Integer> onScoreUpdatedListener) {
        mScoreUpdatedObserver = onScoreUpdatedListener;
    }
}
