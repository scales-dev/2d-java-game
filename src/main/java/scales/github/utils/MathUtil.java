package scales.github.utils;

import scales.github.Main;

public class MathUtil {
    /// gets ratio of how close you are from the next game tick, for rendering smoothly
    public static double getPartialTicks(long lastTick) {
        long millisSinceLastTick = System.currentTimeMillis() - lastTick;
        long millisBetweenFrame = 1000/ Main.gameFrameRate;
        return (double) millisSinceLastTick / millisBetweenFrame;
    }

    public static double interpolate(double last, double current, double partialTicks) {
        return last + ((current-last) * partialTicks);
    }

    public static double roundTo(double num, int precision) {
        double multi = Math.pow(10, precision);
        return Math.round(num * multi) / multi;
    }
}
