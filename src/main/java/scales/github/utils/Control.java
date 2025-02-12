package scales.github.utils;

import scales.github.Main;
import scales.github.listeners.KeyboardListener;

public class Control {
    public int key;
    public int bufferTicks;


    public int ticksSinceRelease;

    public Control(int key, int bufferTicks) {
        this.key = key;
        this.bufferTicks = bufferTicks;
        this.ticksSinceRelease = this.bufferTicks;

        Main.keybinds.add(this);
    }

    public boolean pressed() {
        return ticksSinceRelease < bufferTicks;
    }

    public static void tickControls() {
        for (Control keybind : Main.keybinds) {
            keybind.ticksSinceRelease++;
            if (KeyboardListener.pressedKeys.containsKey(keybind.key)) keybind.ticksSinceRelease = 0;
        }
    }
}
