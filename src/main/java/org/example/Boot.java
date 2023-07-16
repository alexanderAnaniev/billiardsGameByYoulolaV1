package org.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;

import java.util.ArrayList;
import java.util.List;

public class Boot extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 10f;
    private static final float WORLD_HEIGHT = 10f;
    private static final float BALL_RADIUS = 0.5f;
    private static final float MAX_PULL_DISTANCE = 8f;
    private static final float MAX_FORCE = 80f;
    private static final float HOLE_RADIUS = 0.7f;
    private static final float START_DELAY = 1f;
    private SpriteBatch batch;
    private Body table;
    private Body[] borders;
    private int index = 0;
    private OrthographicCamera camera;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private Body[] balls;

    private boolean isMousePressed = false;
    private Vector2 cueBallPosition;
    private Vector2 direction;
    private List<Vector2> trajectoryPoints;
    private ShapeRenderer shapeRenderer;
    private TextureRegion cueBallTexture;

    private boolean isMenuVisible = true;
    private TextureRegion startButtonTexture;
    private Vector2 startButtonPosition;

    private boolean gameOver = false;
    private boolean gameRestart = false;
    private TextureRegion gameOverTexture;
    private Vector2 gameOverPosition;

    private boolean gameStarted = false;
    private float elapsedTime = 0f;
    private boolean isRestarting = false;

    private TextureRegion youWinTexture;
    private Vector2 youWinPosition;
    private boolean youWin = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
        world = new World(new Vector2(0, 0), true);
        debugRenderer = new Box2DDebugRenderer();
        balls = new Body[9];
        trajectoryPoints = new ArrayList<>();
        shapeRenderer = new ShapeRenderer();

        createBall(1f, 5f);
        createBall(5f, 5f);
        createBall(6f, 5.5f);
        createBall(5f, 4f);
        createBall(4f, 4.5f);
        createBall(6f, 3.5f);
        createBall(6f, 4.4f);

        createHole(0.5f, 0.5f);
        createHole(WORLD_WIDTH - 0.5f, 0.5f);
        createHole(0.5f, WORLD_HEIGHT - 0.5f);
        createHole(WORLD_WIDTH - 0.5f, WORLD_HEIGHT - 0.5f);

        createTable();
        createBorders();

        startButtonTexture = new TextureRegion(new Texture(Gdx.files.internal("assets/start_button.png")));
        float buttonWidth = 4f;
        float buttonHeight = 6f;
        float buttonX = (WORLD_WIDTH - buttonWidth) / 2f;
        float buttonY = (WORLD_HEIGHT - buttonHeight) / 2f;
        startButtonPosition = new Vector2(buttonX, buttonY);

        gameOverTexture = new TextureRegion(new Texture(Gdx.files.internal("assets/game_over2.png")));
        float gameOverWidth = 8f;
        float gameOverHeight = 4f;
        float gameOverX = (WORLD_WIDTH - gameOverWidth) / 2f;
        float gameOverY = (WORLD_HEIGHT - gameOverHeight) / 2f;
        gameOverPosition = new Vector2(gameOverX, gameOverY);

        youWinTexture = new TextureRegion(new Texture(Gdx.files.internal("assets/you_win.png")));
        float youWinWidth = 6f;
        float youWinHeight = 3f;
        float youWinX = (WORLD_WIDTH - youWinWidth) / 2f;
        float youWinY = (WORLD_HEIGHT - youWinHeight) / 2f;
        youWinPosition = new Vector2(youWinX, youWinY);

    }

    private void createHole(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x, y);

        CircleShape shape = new CircleShape();
        shape.setRadius(HOLE_RADIUS);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.isSensor = true;

        Body hole = world.createBody(bodyDef);
        hole.createFixture(fixtureDef);
        hole.setUserData("hole");

        shape.dispose();
    }

    private void createBall(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        CircleShape shape = new CircleShape();
        shape.setRadius(BALL_RADIUS);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.restitution = 0.5f;
        fixtureDef.friction = 0.1f;
        fixtureDef.density = 1f;

        Body ball = world.createBody(bodyDef);
        if (index == 0) {
            cueBallTexture = new TextureRegion(new Texture(Gdx.files.internal("assets/cue_ball.png")));
            ball.setUserData(cueBallTexture);
        } else {
            ball.setUserData(new TextureRegion(new Texture(Gdx.files.internal("assets/ball.png"))));
        }

        ball.createFixture(fixtureDef);

        balls[index] = ball;
        index++;
        shape.dispose();
    }

    private void createTable() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        float tableX = WORLD_WIDTH / 2f;
        float tableY = WORLD_HEIGHT / 2f;
        float tableWidth = 0f;
        float tableHeight = 0f;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(tableWidth / 2f, tableHeight / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;

        table = world.createBody(bodyDef);
        table.createFixture(fixtureDef);

        shape.dispose();
    }

    private void createBorders() {

        float borderWidth = 0f;

        borders = new Body[10];

        createBorder(WORLD_WIDTH / 2f, WORLD_HEIGHT - borderWidth / 2f, WORLD_WIDTH, borderWidth);

        createBorder(WORLD_WIDTH / 2f, borderWidth / 2f, WORLD_WIDTH, borderWidth);

        createBorder(borderWidth / 2f, WORLD_HEIGHT / 2f, borderWidth, WORLD_HEIGHT);

        createBorder(WORLD_WIDTH - borderWidth / 2f, WORLD_HEIGHT / 2f, borderWidth, WORLD_HEIGHT);
    }

    private void createBorder(float x, float y, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x, y);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;

        Body border = world.createBody(bodyDef);
        border.createFixture(fixtureDef);

        borders[index] = border;

        shape.dispose();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (isMenuVisible) {
            renderMenu();
        } else {
            if (!gameStarted) {
                elapsedTime += Gdx.graphics.getDeltaTime();
                if (elapsedTime >= START_DELAY) {
                    gameStarted = true;
                }
            }
            handleInput();
            update();
            renderGame();
        }
    }

    private void renderMenu() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float screenButtonX = startButtonPosition.x;
        float screenButtonY = startButtonPosition.y;
        float buttonWidth = 4f;
        float buttonHeight = 6f;
        float screenButtonWidth = buttonWidth * camera.viewportWidth / WORLD_WIDTH;
        float screenButtonHeight = buttonHeight * camera.viewportHeight / WORLD_HEIGHT;
        batch.draw(startButtonTexture, screenButtonX, screenButtonY, screenButtonWidth, screenButtonHeight);
        batch.end();

        if (Gdx.input.justTouched()) {
            Vector3 touch = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touch);

            if (startButtonPosition.x <= touch.x && touch.x <= startButtonPosition.x + startButtonTexture.getRegionWidth() &&
                    startButtonPosition.y <= touch.y && touch.y <= startButtonPosition.y + startButtonTexture.getRegionHeight()) {
                isMenuVisible = false;
            }
        }
    }

    private void renderGame() {
        debugRenderer.render(world, camera.combined);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Body ball : balls) {
            if (ball != null) {
                TextureRegion ballTexture = (TextureRegion) ball.getUserData();
                Vector2 position = ball.getPosition();
                float angle = ball.getAngle();
                batch.draw(ballTexture, position.x - BALL_RADIUS, position.y - BALL_RADIUS, BALL_RADIUS, BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2, 1f, 1f, (float) Math.toDegrees(angle));
            }
        }
        batch.end();

        if (isMousePressed) {
            drawTrajectory();
        }

        if (gameOver) {
            batch.begin();
            float screenGameOverX = gameOverPosition.x;
            float screenGameOverY = gameOverPosition.y;
            float gameOverWidth = 8f;
            float gameOverHeight = 4f;
            float screenGameOverWidth = gameOverWidth * camera.viewportWidth / WORLD_WIDTH;
            float screenGameOverHeight = gameOverHeight * camera.viewportHeight / WORLD_HEIGHT;
            batch.draw(gameOverTexture, screenGameOverX, screenGameOverY, screenGameOverWidth, screenGameOverHeight);
            batch.end();
        }
        if (youWin) {
            batch.begin();
            float screenYouWinX = youWinPosition.x;
            float screenYouWinY = youWinPosition.y;
            float youWinWidth = 6f;
            float youWinHeight = 3f;
            float screenYouWinWidth = youWinWidth * camera.viewportWidth / WORLD_WIDTH;
            float screenYouWinHeight = youWinHeight * camera.viewportHeight / WORLD_HEIGHT;
            batch.draw(youWinTexture, screenYouWinX, screenYouWinY, screenYouWinWidth, screenYouWinHeight);
            batch.end();
        }
    }

    private void handleInput() {
        if (youWin || gameOver) {
            if (Gdx.input.justTouched()) {
                gameRestart = true;
            }
            return;
        }

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (gameStarted && isAllBallsStopped()) {
                isMousePressed = true;
                Vector3 clickPosition = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(clickPosition);

                cueBallPosition = balls[0].getPosition();
                direction = new Vector2(clickPosition.x, clickPosition.y).sub(cueBallPosition);

                float maxDirectionLength = MAX_PULL_DISTANCE;
                float distance = direction.len();
                if (distance > maxDirectionLength) {
                    direction.setLength(maxDirectionLength);
                }

                trajectoryPoints.clear();
                trajectoryPoints.add(cueBallPosition.cpy());
                Vector2 trajectoryEnd = cueBallPosition.cpy().add(direction);
                trajectoryPoints.add(trajectoryEnd);
            }
        } else {
            if (isMousePressed) {
                isMousePressed = false;
                float cueForce = calculateForce(direction.len());
                balls[0].applyForceToCenter(direction.scl(cueForce * 2f), true);
            }
        }
    }

    private boolean isAllBallsStopped() {
        for (Body ball : balls) {
            if (ball != null && ball.getLinearVelocity().len2() > 0.01f) {
                return false;
            }
        }
        return true;
    }
    private boolean ballsInHoles() {
        for (int i = 1; i < balls.length; i++) {
            Body ball = balls[i];
            if (ball != null) {
                boolean inHole = false;
                Array<Body> bodies = new Array<>();
                world.getBodies(bodies);
                for (Body body : bodies) {
                    if (body.getUserData() instanceof String && body.getUserData().equals("hole")) {
                        Vector2 holePosition = body.getPosition();
                        float distance = ball.getPosition().dst(holePosition);
                        if (distance < HOLE_RADIUS - BALL_RADIUS) {
                            inHole = true;
                            break;
                        }
                    }
                }
                if (!inHole) {
                    return false;
                }
            }
        }
        return true;
    }

    private void update() {
        float timeStep = 1f / 60f;
        int velocityIterations = 6;
        int positionIterations = 2;

        world.step(timeStep, velocityIterations, positionIterations);

        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);

        for (int i = 0; i < balls.length; i++) {
            Body ball = balls[i];
            if (ball != null && ball.getUserData() instanceof TextureRegion) {
                Vector2 ballPosition = ball.getPosition();
                boolean isBallInHole = false;
                ball.setAngularVelocity(0.0f);

                for (Body body : bodies) {
                    if (body.getUserData() instanceof String && body.getUserData().equals("hole")) {
                        Vector2 holePosition = body.getPosition();
                        float distance = ballPosition.dst(holePosition);

                        if (distance < HOLE_RADIUS - BALL_RADIUS) {
                            isBallInHole = true;
                            break;
                        }
                    }
                }

                if (isBallInHole) {
                    if (i == 0) {
                        gameOver = true;
                    } else {
                        world.destroyBody(ball);
                        balls[i] = null;
                    }
                }
            }
        }
        for (Body ball : balls) {
            if (ball != null) {
                ball.setLinearVelocity(ball.getLinearVelocity().scl(0.995f));
            }
        }
        if(ballsInHoles()){
            youWin=true;
        }

        if (gameRestart) {
            restartGame();
        }
    }

    private void drawTrajectory() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.5f);

        for (int i = 0; i < trajectoryPoints.size() - 1; i++) {
            Vector2 p1 = trajectoryPoints.get(i);
            Vector2 p2 = trajectoryPoints.get(i + 1);
            shapeRenderer.rectLine(p1.x, p1.y, p2.x, p2.y, 0.05f);
        }

        shapeRenderer.end();
    }

    private float calculateForce(float distance) {
        float forcePercentage = distance / MAX_PULL_DISTANCE;
        return MAX_FORCE * forcePercentage;
    }

    private void restartGame() {
        isMenuVisible = true;
        gameOver = false;
        gameRestart = false;
        youWin = false;

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                world.dispose();
                world = new World(new Vector2(0, 0), true);
                gameOver = false;
                index = 0;
                balls = new Body[8];
                trajectoryPoints.clear();

                createBall(1f, 5f);
                createBall(5f, 5f);
                createBall(6f, 5.5f);
                createBall(5f, 4f);
                createBall(4f, 4.5f);
                createBall(6f, 3.5f);
                createBall(6f, 4.4f);

                createHole(0.5f, 0.5f);
                createHole(WORLD_WIDTH - 0.5f, 0.5f);
                createHole(0.5f, WORLD_HEIGHT - 0.5f);
                createHole(WORLD_WIDTH - 0.5f, WORLD_HEIGHT - 0.5f);

                createTable();
                createBorders();
                isRestarting = false;

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        gameStarted = false;
                        elapsedTime = 0f;
                        youWin = false;
                    }
                });
            }
        }, START_DELAY);
    }


    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
        camera.update();

    }

    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        debugRenderer.dispose();
        shapeRenderer.dispose();
        for (Body border : borders) {
            world.destroyBody(border);
        }
        for (Body ball : balls) {
            if (ball != null) {
                world.destroyBody(ball);
            }
        }
    }
}




