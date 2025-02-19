package scales.github.utils;

import lombok.AllArgsConstructor;
import scales.github.Main;

import java.awt.*;
import java.util.ArrayList;

// optimized code :pray:
public class ParticleUtil {
    @AllArgsConstructor
    private static class Particle {
        public double x;
        public double xM;
        public double y;
        public double w;
        public double h;
        public Color color;
        public long time;
    }

    private static final ArrayList<Particle> particles = new ArrayList<>();

    public static void spawn(double x, double y, double width, double height, int amount, Color color) {
        for (int i = 0; i < amount; i++) {
            particles.add(new Particle((x + Math.random()) * Main.levelScale, 0.5-Math.random(), (y + Math.random()) * Main.levelScale, width * Main.levelScale, height * Main.levelScale, color, System.currentTimeMillis()));
        }
    }

    private final static int particleDuration = 300;

    public static void renderParticles() {
        for (int i = 0; i < particles.size(); i++) {
            Particle particle = particles.get(i);
            int time = (int) (System.currentTimeMillis()-particle.time);

            if (time > particleDuration) {
                particles.remove(particle);
                continue;
            }

            double percentDone = (double) time / particleDuration;

            double easing = AnimationUtil.easeInOutBack(percentDone);

            Main.graphics.setColor(new Color(particle.color.getRed(), particle.color.getGreen(), particle.color.getBlue(), (int) (255*(1-easing))));

            int x = (int) (particle.x+(easing*particle.xM*100));
            int y = (int) (particle.y+(easing*100));

            Main.graphics.fillRoundRect(x, y, (int) (particle.w), (int) (particle.h), (int) (particle.w), (int) (particle.h));
        }
    }
}
