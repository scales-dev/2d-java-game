package scales.github.utils;

import lombok.AllArgsConstructor;

// like Vec2d but smaller numbers!
@AllArgsConstructor
public class Block {
    public int x;
    public int y;
    public int width;
    public int height;

    public BlockTypes blockType;

    public enum BlockTypes {
        SPAWN,
        DEFAULT,

        // directional walls
        LEFT_WALL,
        RIGHT_WALL,
        FLOOR,
        CEILING,

        WIN
    }
}
