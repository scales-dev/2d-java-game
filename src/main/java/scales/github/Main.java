package scales.github;

import scales.github.entities.Entity;
import scales.github.listeners.KeyboardListener;
import scales.github.utils.Rectangle;
import scales.github.utils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

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
    public static ArrayList<Rectangle> levelMap = new ArrayList<>();


    private static LevelInfo levelInfo;

    private static BufferedImage decoratedLevelImage;
    private static final int pixelSize = 16;


    public static void main(String[] args) {
        level = getImageResource("level.png");
        backgroundImage = getImageResource("background.png");

        levelInfo = createLevelFromImage(level);

        createWindow();
        mainLoop();
    }

    public static InputStream getResourceFile(String name) {
        return Main.class.getClassLoader().getResourceAsStream(name);
    }

    private static final HashMap<String, BufferedImage> imageCache = new HashMap<>();
    public static BufferedImage getImageResource(String name) {
        if (imageCache.containsKey(name)) return imageCache.get(name);

        try {
            BufferedImage image = ImageIO.read(getResourceFile(name));
            imageCache.put(name, image);
            return image;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    static Boolean[][] blackListedCoordinates = new Boolean[0][0];

    private static LevelInfo createLevelFromImage(BufferedImage levelImage) {
        Vec2d spawnPos = new Vec2d(0,0);

        decoratedLevelImage = new BufferedImage(level.getWidth()*pixelSize, level.getHeight()*pixelSize, BufferedImage.TYPE_INT_ARGB);
        Graphics g = decoratedLevelImage.createGraphics();

        blackListedCoordinates = new Boolean[levelImage.getWidth()][levelImage.getHeight()];
        for (int x = 0; x < levelImage.getWidth(); x++) {
            for (int y = 0; y < levelImage.getHeight(); y++) {
                // todo: when level editor, replace this with getting a Block,
                //  this would also save me having to do getBlockName(blockType), instead (Block).blockName!
                Block.BlockTypes blockType = getBlockType(x,y, levelImage);
                BufferedImage blockTexture = getImageResource(getBlockName(blockType));

                g.drawImage(blockTexture, x*pixelSize, y*pixelSize, pixelSize, pixelSize, null);

                if (validPixel(x,y, null, levelImage)) {
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

                    levelMap.add(new Rectangle(x, y, width, height, new Block(getBlockName(blockType), blockType)));
                }
            }
        }

        return new LevelInfo(spawnPos, levelImage.getHeight(), levelImage.getWidth());
    }

    // temporary, when level editor store all strings in a file maybe idk
    private static String getBlockName(Block.BlockTypes blockType) {
        return switch (blockType) {
            case WIN -> "goal.png";

            case LEFT_WALL -> "left.png";
            case RIGHT_WALL -> "right.png";
            case FLOOR -> "floor.png";
            case CEILING -> "ceiling.png";

            case DEFAULT -> "default.png";
            case DIE -> "die.png";

            default -> "";
        };
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

            case 0x960096 -> Block.BlockTypes.DIE;

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

    // lwk just here because intellij SCREAMS at me in yellow that there's a warning otherwise.
    public static boolean running = true;

    private static void mainLoop() {
        player.spawn(levelInfo);

        while (running) {
            //long timeStart = System.nanoTime();

            startDrawing();

            graphics.drawImage(backgroundImage, 0,0,frame.getWidth(),frame.getHeight(), null);
            graphics.scale((double) frame.getWidth() / baseWidth, (double) frame.getHeight() / baseHeight);

            Vec2d interpolatedPos = player.getInterpolatedPos(false);

            graphics.translate(-interpolatedPos.x + baseWidth/2d, -interpolatedPos.y + baseHeight/2d);

            graphics.drawImage(decoratedLevelImage, 0,0,(decoratedLevelImage.getWidth()/pixelSize)*levelScale,(decoratedLevelImage.getHeight()/pixelSize)*levelScale, null);

            entities.forEach(Entity::tick);
            entities.forEach(Entity::render);

            ParticleUtil.renderParticles();

            //graphics.setFont(new Font("Comic Sans MS", Font.PLAIN, 60));
            // probably calculating fps wrong?
            // 1 / nanoseconds per frame should equal frames per nanosecond? then nano to seconds = / 1000(micro)/1000(milli)/1000(seconds) = * 1E-9
            //double fps = 1d/((System.nanoTime() - timeStart) * 1E-9);
            //graphics.drawString(String.valueOf((int) fps)), 40,40);

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