package dev.aether.module.impl;

import dev.aether.module.*;
import dev.aether.setting.*;

public class ClickGuiModule extends ClientModule {
    public static ClickGuiModule INSTANCE;
    private final ColorSetting accent = add(new ColorSetting("Accent", 0.46f, 0.9f, 0.75f));
    private final NumberSetting dim = add(new NumberSetting("Background Dim", 35, 0, 80, 1));

    public ClickGuiModule() {
        super("ClickGUI", "GUI appearance", Category.CLIENT);
        INSTANCE = this;
    }
    public static int accent() { return 0xFF000000 | INSTANCE.accent.rgb(); }
    public static int dimARGB() { return ((int) (INSTANCE.dim.get() / 100.0 * 255)) << 24; }
}
