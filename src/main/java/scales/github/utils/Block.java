package scales.github.utils;

import lombok.AllArgsConstructor;

// like Vec2d but smaller numbers!
@AllArgsConstructor
public class Block {
    public String blockName;
    public BlockTypes blockType;

    public enum BlockTypes {
        SPAWN,
        DEFAULT,
        DIE,

        // directional walls
        LEFT_WALL,
        RIGHT_WALL,
        FLOOR,
        CEILING,

        WIN,
        BACKGROUND
    }
}
