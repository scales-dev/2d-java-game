package scales.github.utils;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Vec2d {
    public double x;
    public double y;

    public double distanceTo(Vec2d vec2d) {
        return Math.abs(vec2d.x-this.x) + Math.abs(vec2d.y-this.y);
    }

    public Vec2d get() {
        return new Vec2d(this.x, this.y);
    }
    public String getString() {
        return String.format("%s, %s", MathUtil.roundTo(this.x, 2), MathUtil.roundTo(this.y, 2));
    }

    public boolean equals(Vec2d vec2d) {
        return vec2d.x == this.x && vec2d.y == this.y;
    }

    public Vec2d offset(double x, double y) {
        return new Vec2d(this.x+x, this.y+y);
    }

}
