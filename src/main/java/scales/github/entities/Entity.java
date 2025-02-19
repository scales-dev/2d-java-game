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

    private final int ballSize = 2;
    public Vec2d size;
    public Vec2d lastSize;
    
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

    private long lastTick;

    public void spawn(LevelInfo levelInfo) {
        this.levelInfo = levelInfo;

        this.pos = levelInfo.spawnPos.get();
        this.lastPos = this.pos.get();
        
        this.size = new Vec2d(this.ballSize, this.ballSize);
        this.lastSize = this.size;
        
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
        // slowly resets deformations
        this.reshape();

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
        if (this.velocity.y != 0) {
            ParticleUtil.spawn(this.pos.x-this.size.x/2, this.pos.y, 0.5, 0.5, 10, new Color(255,255,255));
            this.velocity.y /= this.verticalBounceMultiplier + 0.1;
        }

        // don't use the ultra super wooper cool bounce if its not cool and super and ultra
        if (this.velocity.y < this.jumpVelocity) {
            if (!this.onGround) ParticleUtil.spawn(this.pos.x-this.size.x/2, this.pos.y - 1, 1, 1, 5, new Color(255,255,255));

            this.velocity.y = this.jumpVelocity;
        }

        ParticleUtil.spawn(this.pos.x-this.size.x/2, this.pos.y - 1, 0.5, 0.5, 5, playerColor);
        this.size.y = ballSize * (this.velocity.y);
    }

    private void bounce() {
        if (this.horizontalCollision) {
            this.velocity.x *= -this.horizontalBounceMultiplier;
            if (this.velocity.x < this.minimumHorizontalBounceVelocity && this.velocity.x > -this.minimumHorizontalBounceVelocity) this.velocity.x = 0;
            else {
                // BOUNCE GO!
                ParticleUtil.spawn(this.pos.x - this.velocity.x, this.pos.y-this.size.y/2, 0.5, 0.5, 10, playerColor);
                this.size.x = ballSize * (Math.abs(this.velocity.x));
            }
        }
        if (this.verticleCollision) {
            this.velocity.y *= -this.verticalBounceMultiplier;
            if (this.velocity.y < this.minimumVerticalBounceVelocity && this.onGround) this.velocity.y = 0;

            // bounce gogogo
            else if (this.onGround) {
                // deform player
                this.size.y = ballSize / Math.max(this.velocity.y*2, 1.1);

                ParticleUtil.spawn(this.pos.x - this.size.x / 2, this.pos.y, 0.5, 0.5, 10, playerColor);
            }
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
            if (!(this.lastPos.y - this.lastSize.y/2 >= rectangle.y + rectangle.height || this.lastPos.y + this.lastSize.y/2 <= rectangle.y)) {
                // was right of wall, now your on its left
                if (this.lastPos.x - (this.lastSize.x/2) >= rectangle.x + rectangle.width && this.pos.x - (this.size.x/2) < rectangle.x + rectangle.width) {
                    collide(rectangle, rectangle.x + rectangle.width + this.size.x/2, true, false, false);
                }

                // was left of wall, now right of wall
                if (this.lastPos.x + (this.lastSize.x/2) <= rectangle.x && this.pos.x + (this.size.x/2) > rectangle.x) {
                    collide(rectangle, rectangle.x - this.size.x/2, true, false, true);
                }
            }
        }

        // seperate loop to stop bumping into a ceiling when against a wall!
        for (Block rectangle : Main.levelMap) {
            if (this.pos.x + (this.size.x/2) > rectangle.x && this.pos.x - (this.size.x/2) < rectangle.x + rectangle.width) {
                // ceilings
                if (this.lastPos.y - (this.lastSize.y/2) >= rectangle.y + rectangle.height && this.pos.y - (this.size.y/2) <= rectangle.y + rectangle.height) {
                    // offset by 0.01 so you don't horizontally collide, can be removed probably, really specific issue, but def happens!
                    collide(rectangle, rectangle.y + rectangle.height + this.size.y/2 + 0.01, false, false, false);
                }

                // floors
                if (this.lastPos.y + (this.lastSize.y/2) <= rectangle.y && this.pos.y + (this.size.y/2) >= rectangle.y) {
                    collide(rectangle, rectangle.y - this.size.y/2, false, true, false);
                }
            }
        }
    }

    private void collide(Block rectangle, double pos, boolean horizontalCollision, boolean ground, boolean left) {
        // if not collided with left and the block only has left collisions, return
        if (!left && rectangle.blockType == Block.BlockTypes.LEFT_WALL) return;
        if (left && rectangle.blockType == Block.BlockTypes.RIGHT_WALL) return;

        // if not collided with ceiling and the block only has ceiling collisions, return
        if (ground && rectangle.blockType == Block.BlockTypes.CEILING) return;
        if (!ground && rectangle.blockType == Block.BlockTypes.FLOOR) return;

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

    public Vec2d getInterpolatedPos(boolean centre) {
        double partialTicks = MathUtil.getPartialTicks(this.lastTick);

        double interpolatedX = (MathUtil.interpolate(this.lastPos.x, this.pos.x, partialTicks) - (centre ? this.size.x/2 : 0)) * Main.levelScale;
        double interpolatedY = (MathUtil.interpolate(this.lastPos.y, this.pos.y, partialTicks) - (centre ? this.size.y/2 : 0)) * Main.levelScale;

        return new Vec2d(interpolatedX, interpolatedY);
    }

    public void render() {
        Vec2d interpolatedPos = this.getInterpolatedPos(true);

        renderKawaiiLittleBody(interpolatedPos.x, interpolatedPos.y);
        renderEyes(interpolatedPos.x, interpolatedPos.y);
    }

    private final Color playerColor = new Color(138, 140, 255);
    private void renderKawaiiLittleBody(double characterX, double characterY) {
        Main.graphics.setColor(playerColor);

        int width = (int) (this.size.x*Main.levelScale);
        int height = (int) (this.size.y*Main.levelScale);

        Main.graphics.fillRoundRect((int) characterX, (int) characterY, width, height, width, height);
    }

    private void renderEyes(double characterX, double characterY) {
        double smallestSize = Math.min(this.size.y, this.size.x);

        int eyeW = (int) getEyeSize(this.size.x);
        int eyeH = (int) getEyeSize(this.size.y);

        // doesn't do eyeW/2 because casting to int rounds it and I don't want to render 1 pixel off!!!
        int pupilW = (int) (getEyeSize(this.size.x)/2);
        int pupilH = (int) (getEyeSize(this.size.y)/2);

        Vec2d leftEyePos = getEyePos(characterX, characterY, this.pos.x / 2);
        Vec2d rightEyePos = getEyePos(characterX, characterY, this.pos.x / 2 + 10);

        Main.graphics.setColor(new Color(255, 255, 255));
        Main.graphics.fillRoundRect((int) (leftEyePos.x), (int) (leftEyePos.y), eyeW, eyeH, eyeW, eyeH);
        Main.graphics.fillRoundRect((int) (rightEyePos.x), (int) (rightEyePos.y), eyeW, eyeH, eyeW, eyeH);

        double pupilInsetX = (eyeW/2d - pupilW/2d);
        double pupilInsetY = (eyeH/2d - pupilH/2d);
        Main.graphics.setColor(new Color(0, 0, 0));
        Main.graphics.fillRoundRect((int) (leftEyePos.x + pupilInsetX), (int) (pupilInsetY + leftEyePos.y), pupilW, pupilH, pupilW, pupilH);
        Main.graphics.fillRoundRect((int) (rightEyePos.x + pupilInsetX), (int) (pupilInsetY + rightEyePos.y), pupilW, pupilH, pupilW, pupilH);
    }


    private Vec2d getEyePos(double x, double y, double rotMulti) {
        double eyeSizeW = getEyeSize(this.size.x);
        double eyeSizeH = getEyeSize(this.size.y);

        double posX = Math.cos(rotMulti) * (((this.size.x/2) * Main.levelScale) - eyeSizeW) - eyeSizeW/2d;
        double posY = Math.sin(rotMulti) * (((this.size.y/2) * Main.levelScale) - eyeSizeH) + eyeSizeH + eyeSizeH/2d;

        return new Vec2d(posX+x + ((this.size.x/2) * Main.levelScale),posY+y);
    }

    private double getEyeSize(double w) {
        return (int) ((w * Main.levelScale) / 4d);
    }

    private void reshape() {
        this.lastSize = this.size.get();

        double heightIncrease = MathUtil.roundTo(1 - (this.size.y / this.ballSize), 2);
        double widthIncrease = MathUtil.roundTo(1 - (this.size.x / this.ballSize), 2);

        this.size.y += heightIncrease;
        this.size.x += widthIncrease;
    }
}
