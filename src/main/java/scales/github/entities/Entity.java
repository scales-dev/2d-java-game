package scales.github.entities;

import scales.github.Main;
import scales.github.utils.*;

import java.awt.*;
import java.awt.event.KeyEvent;

public class Entity {
    public Entity(boolean player) {
        controllable = player;
    }

    private final boolean controllable;

    private LevelInfo levelInfo;

    public Vec2d pos;
    public Vec2d lastPos;
    public Vec2d velocity;

    public boolean onGround = false;
    public boolean horizontalCollision = false;
    public boolean verticleCollision = false;
    public double gravity = 0.098;

    public int airTicks = 0;
    public int groundTicks = 0;
    private final int coyoteTime = 4;

    public final double airFriction = 0.1;
    public final double baseFriction = 0.5;
    public double friction = 0.5;

    public final double jumpVelocity = 1.2;
    public final double horizontalBounceMultiplier = 0.9;
    public final double minimumHorizontalBounceVelocity = 0.5;

    public final double verticalBounceMultiplier = 0.6;
    public final double minimumVerticalBounceVelocity = 0.5;

    private final int ballSize = 2;
    public double height = ballSize;
    public double width = ballSize;

    private final int ballRadius = ballSize/2;

    private long lastTick;

    public void spawn(LevelInfo levelInfo) {
        this.levelInfo = levelInfo;

        this.pos = levelInfo.spawnPos.get();
        this.lastPos = this.pos.get();
        this.velocity = new Vec2d(0,0);
        this.lastTick = System.currentTimeMillis();

        if (!Main.entities.contains(this)) Main.entities.add(this);
    }


    public Control left = new Control(KeyEvent.VK_A, 3);
    public Control right = new Control(KeyEvent.VK_D, 3);
    public Control up = new Control(KeyEvent.VK_SPACE, 3);

    public void tick() {
        long millisSinceLastTick = System.currentTimeMillis() - this.lastTick;
        long millisBetweenFrame = 1000/Main.gameFrameRate;

        while (millisSinceLastTick > millisBetweenFrame) {
            this.lastTick+=millisBetweenFrame;
            millisSinceLastTick = System.currentTimeMillis() - this.lastTick;

            if (controllable) Control.tickControls();

            this.tickMovement();
        }
    }

    public void tickMovement() {
        this.lastPos = this.pos.get();

        this.friction = baseFriction;

        if (!this.onGround) {
            this.airTicks++;
            this.groundTicks = 0;

            this.friction = this.airFriction;
            this.velocity.y -= this.gravity;
        }
        else {
            this.airTicks = 0;
            this.groundTicks++;
        }

        if (this.up.pressed() && this.airTicks < coyoteTime) this.jump();

        this.velocity.x = this.applyFriction(this.friction, this.velocity.x, (this.left.pressed() ? -1 : 0) + (this.right.pressed() ? 1 : 0));

        this.pos.x += this.velocity.x;
        this.pos.y -= this.velocity.y;

        // if you collide with a wall, set position to the side of the wall you collided with
        // same with floor collisions
        this.fixCollisions();

        this.bounce();
    }

    private void jump() {
        this.airTicks+=coyoteTime;
        this.velocity.x *= 1.2;

        // sooper awesome bounce wow wth!
        if (this.velocity.y != 0) this.velocity.y /= this.verticalBounceMultiplier + 0.1;

        // don't use the ultra super wooper cool bounce if its not cool and super and ultra
        if (this.velocity.y < this.jumpVelocity) this.velocity.y = this.jumpVelocity;
    }

    private void bounce() {
        if (this.horizontalCollision) {
            this.velocity.x *= -this.horizontalBounceMultiplier;
            if (this.velocity.x < this.minimumHorizontalBounceVelocity && this.velocity.x > -this.minimumHorizontalBounceVelocity) this.velocity.x = 0;
        }
        if (this.verticleCollision) {
            this.velocity.y *= -this.verticalBounceMultiplier;
            if (this.velocity.y < this.minimumVerticalBounceVelocity && this.onGround) this.velocity.y = 0;
        }
    }

    // separate loop for ceilings and floors, o(2N) is crazy!
    // tho lwk, this code should run borderline instantly and can support thousands of pixels!
    // todo: maybe split into chunks or smth, currently do not need to!
    protected void fixCollisions() {
        this.onGround = false;
        this.horizontalCollision = false;
        this.verticleCollision = false;

        for (Block rectangle : Main.levelMap) {
            // checks if NOT (player top is above rectangle bottom OR player bottom is below the rectangle top)
            // done in inverse because this was what came to mind first and is easier to read
            if (!(this.lastPos.y - this.height/2 >= rectangle.y + rectangle.height || this.lastPos.y + this.height/2 <= rectangle.y)) {
                // was right of wall, now your on its left
                if (this.lastPos.x - (this.width/2) >= rectangle.x + rectangle.width && this.pos.x - (this.width/2) < rectangle.x + rectangle.width) {
                    collide(rectangle, rectangle.x + rectangle.width + this.width/2, true, false);
                }

                // was left of wall, now right of wall
                if (this.lastPos.x + (this.width/2) <= rectangle.x && this.pos.x + (this.width/2) > rectangle.x) {
                    collide(rectangle, rectangle.x - this.width/2, true, false);
                }
            }
        }

        // seperate loop to stop bumping into a ceiling when against a wall!
        for (Block rectangle : Main.levelMap) {
            if (this.pos.x + (this.width/2) > rectangle.x && this.pos.x - (this.width/2) < rectangle.x + rectangle.width) {
                // ceilings
                if (this.lastPos.y - (this.height/2) >= rectangle.y + rectangle.height && this.pos.y - (this.height/2) <= rectangle.y + rectangle.height) {
                    // offset by 0.01 so you don't horizontally collide, can be removed probably, really specific issue, but def happens!
                    collide(rectangle, rectangle.y + rectangle.height + this.height/2 + 0.01, false, false);
                }

                // floors
                if (this.lastPos.y + (this.height/2) <= rectangle.y && this.pos.y + (this.height/2) >= rectangle.y) {
                    collide(rectangle, rectangle.y - this.height/2, false, true);
                }
            }
        }
    }

    private void collide(Block rectangle, double pos, boolean horizontalCollision, boolean ground) {
        if (rectangle.blockType == Block.BlockTypes.WIN) {
            win();
            return;
        }

        if (horizontalCollision) {
            this.pos.x = pos;
            this.horizontalCollision = true;
        }
        else {
            this.pos.y = pos;
            this.verticleCollision = true;

            if (ground) this.onGround = true;
        }
    }

    private void win() {
        this.respawn();
    }

    private void die() {
        this.respawn();
    }


    private void respawn() {
        this.spawn(this.levelInfo);
    }

    private double applyFriction(double friction, double start, double end) {
        return start - (friction * (start-end));
    }

    public Vec2d getInterpolatedPos() {
        double partialTicks = MathUtil.getPartialTicks(this.lastTick);

        double interpolatedX = (MathUtil.interpolate(this.lastPos.x, this.pos.x, partialTicks) - ballRadius) * Main.levelScale;
        double interpolatedY = (MathUtil.interpolate(this.lastPos.y, this.pos.y, partialTicks) - ballRadius) * Main.levelScale;

        return new Vec2d(interpolatedX, interpolatedY);
    }

    public void render() {
        Vec2d interpolatedPos = this.getInterpolatedPos();

        renderKawaiiLittleBody(interpolatedPos.x, interpolatedPos.y);
        renderEyes(interpolatedPos.x, interpolatedPos.y);
    }

    private void renderKawaiiLittleBody(double characterX, double characterY) {
        Main.graphics.setColor(new Color(138, 140, 255));
        Main.graphics.fillRoundRect((int) characterX, (int) characterY, ballSize*Main.levelScale, ballSize*Main.levelScale, ballSize*Main.levelScale, ballSize*Main.levelScale);
    }

    private void renderEyes(double characterX, double characterY) {
        int eyeSize = (int) ((ballSize * Main.levelScale) / 4d);
        int pupilSize = (int) ((ballSize * Main.levelScale) / 8d);
        Vec2d leftEyePos = getEyePos(eyeSize, characterX, characterY, this.pos.x / 2);
        Vec2d rightEyePos = getEyePos(eyeSize, characterX, characterY, this.pos.x / 2 + 10);

        Main.graphics.setColor(new Color(255, 255, 255));
        Main.graphics.fillRoundRect((int) (leftEyePos.x), (int) (leftEyePos.y), eyeSize, eyeSize, eyeSize, eyeSize);
        Main.graphics.fillRoundRect((int) (rightEyePos.x), (int) (rightEyePos.y), eyeSize, eyeSize, eyeSize, eyeSize);

        double pupilInset = (eyeSize/2d - pupilSize/2d);
        Main.graphics.setColor(new Color(0, 0, 0));
        Main.graphics.fillRoundRect((int) (leftEyePos.x + pupilInset), (int) (pupilInset + leftEyePos.y), pupilSize, pupilSize, pupilSize, pupilSize);
        Main.graphics.fillRoundRect((int) (rightEyePos.x + pupilInset), (int) (pupilInset + rightEyePos.y), pupilSize, pupilSize, pupilSize, pupilSize);
    }


    private Vec2d getEyePos(double eyeSize, double x, double y, double rotMulti) {
        double eyeOffset = eyeSize/2d;
        double posX = Math.cos(rotMulti) * ((ballRadius * Main.levelScale) - eyeSize) - eyeOffset;
        double posY = Math.sin(rotMulti) * ((ballRadius * Main.levelScale) - eyeSize) + eyeSize + eyeOffset;

        return new Vec2d(posX+x + (ballRadius * Main.levelScale),posY+y);
    }
}
