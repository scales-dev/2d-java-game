package scales.github.utils;

public class AnimationUtil {
    // https://easings.net/#easeInOutBack
    public static double easeInOutBack(double x) {
        return Math.sqrt(1 - Math.pow(x - 1, 2));
    }
}
