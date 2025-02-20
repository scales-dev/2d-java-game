package scales.github.utils;

import lombok.AllArgsConstructor;

// like Vec2d but smaller numbers!
@AllArgsConstructor
public class Rectangle {
    public int x;
    public int y;
    public int width;
    public int height;

    public Block block;
}
