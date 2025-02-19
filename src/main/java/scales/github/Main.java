package scales.github;

import scales.github.entities.Entity;
import scales.github.listeners.KeyboardListener;
import scales.github.utils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Main {
    public static final JFrame frame = new JFrame("haiii ;3");
    public static Graphics2D graphics;
    private static BufferedImage level;
    private static BufferedImage backgroundImage;
    private static BufferStrategy bufferStrategy;

    public static ArrayList<Entity> entities = new ArrayList<>();
    public static ArrayList<Control> keybinds = new ArrayList<>();


    public static Entity player = new Entity(true);

    public static int gameFrameRate = 20;

    public static int baseWidth = 10000;
    public static int baseHeight = baseWidth/2;
    public static int levelScale = baseWidth/100;

    // god strike me down
    public static ArrayList<Block> levelMap = new ArrayList<>();


    private static LevelInfo levelInfo;


    public static void main(String[] args) throws IOException {
        level = ImageIO.read(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("level.png")));
        backgroundImage = ImageIO.read(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("background.png")));

        levelInfo = createLevelFromImage(level);

        createWindow();
        mainLoop();
    }

    static Boolean[][] blackListedCoordinates = new Boolean[0][0];

    private static LevelInfo createLevelFromImage(BufferedImage levelImage) {
        Vec2d spawnPos = new Vec2d(0,0);

        blackListedCoordinates = new Boolean[levelImage.getWidth()][levelImage.getHeight()];
        for (int x = 0; x < levelImage.getWidth(); x++) {
            for (int y = 0; y < levelImage.getHeight(); y++) {
                if (validPixel(x,y, null, levelImage)) {
                    Block.BlockTypes blockType = getBlockType(x,y, levelImage);
                    if (blockType == Block.BlockTypes.SPAWN) {
                        spawnPos = new Vec2d(x+0.5,y+0.5);
                        continue;
                    }
                    if (blockType == Block.BlockTypes.BACKGROUND) continue;

                    int width = 1;
                    int height = 1;

                    while (x+width < levelImage.getWidth() && validPixel(x+width,y, blockType, levelImage)) {
                        blackListedCoordinates[x+width][y] = true;
                        width++;
                    }

                    while (y+height < levelImage.getHeight() && validPixel(x,y+height, blockType, levelImage)) {
                        boolean fullLayer = true;

                        // check if every coordinate is valid for the width
                        for (int w = 0; w < width; w++) {
                            if (validPixel(x+w,y+height, blockType, levelImage)) {
                                blackListedCoordinates[x+w][y + height] = true;
                                continue;
                            }
                            fullLayer = false;
                        }

                        if (fullLayer) height++;
                        else break;
                    }

                    levelMap.add(new Block(x, y, width, height, blockType));
                }
            }
        }

        return new LevelInfo(spawnPos, levelImage.getHeight(), levelImage.getWidth());
    }

    private static boolean validPixel(int x, int y, Block.BlockTypes blockType, BufferedImage image) {
        return image.getRGB(x,y) >>> 24 != 0 && (blockType == null || getBlockType(x,y, image) == blockType) && blackListedCoordinates[x][y] == null;
    }

    private static Block.BlockTypes getBlockType(int x, int y, BufferedImage image) {
        // image.getRGB(x,y) is inverted because of signed numbers or smth magic
        return switch (16777216+image.getRGB(x,y)) {
            case 0xFF0000 -> Block.BlockTypes.SPAWN;
            case 0x00FF00 -> Block.BlockTypes.WIN;

            case 0xC800C8 -> Block.BlockTypes.LEFT_WALL;
            case 0x00C8C8 -> Block.BlockTypes.RIGHT_WALL;
            case 0xC8C800 -> Block.BlockTypes.FLOOR;
            case 0x640000 -> Block.BlockTypes.CEILING;

            // BLACK
            case 0 -> Block.BlockTypes.DEFAULT;

            default -> Block.BlockTypes.BACKGROUND;
        };
    }

    private static void createWindow() {
        //frame.setUndecorated(true);
        frame.setSize(1000,500);
        frame.setLocation(
                C.screenSize.width/2 - frame.getWidth()/2,
                C.screenSize.height/2 - frame.getHeight()/2
        );
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.setVisible(true);

        frame.addKeyListener(new KeyboardListener());

        graphics = (Graphics2D) frame.getGraphics();

        frame.createBufferStrategy(2);
        bufferStrategy = frame.getBufferStrategy();
    }

    private static void mainLoop() {
        player.spawn(levelInfo);

        while (true) {
            //long timeStart = System.nanoTime();

            startDrawing();

            graphics.drawImage(backgroundImage, 0,0,frame.getWidth(),frame.getHeight(), null);
            graphics.scale((double) frame.getWidth() / baseWidth, (double) frame.getHeight() / baseHeight);

            Vec2d interpolatedPos = player.getInterpolatedPos(false);

            graphics.translate(-interpolatedPos.x + baseWidth/2d, -interpolatedPos.y + baseHeight/2d);

            graphics.drawImage(level, 0,0,level.getWidth()*levelScale,level.getHeight()*levelScale, null);

            entities.forEach(Entity::tick);
            entities.forEach(Entity::render);

            ParticleUtil.renderParticles();

            //graphics.setFont(new Font("Comic Sans MS", Font.PLAIN, 60));
            //double fps = 1d/((System.nanoTime() - timeStart) * 1.0E-9);
            //int fpsRounded = (((int) fps));
            //graphics.drawString(String.valueOf(fpsRounded), 40,40);

            finishDrawing();
        }
    }

    private static void startDrawing() {
        if (bufferStrategy.getDrawGraphics() != null) {
            graphics = (Graphics2D) bufferStrategy.getDrawGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    private static void finishDrawing() {
        bufferStrategy.show();
        graphics.dispose();
    }
}