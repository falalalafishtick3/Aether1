package dev.aether.module;

import dev.aether.module.impl.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<ClientModule> MODULES = new ArrayList<>();

    public static void init() {
        MODULES.add(new Aimbot());
        MODULES.add(new CrystalMacro());
        MODULES.add(new AnchorMacro());
        MODULES.add(new StunSlam());
        MODULES.add(new Freecam());
        MODULES.add(new StorageFinder());
        MODULES.add(new SpawnerFinder());
        MODULES.add(new Visuals());
        MODULES.add(new TuffChunkFinder());
        MODULES.add(new BaseFinder());
        MODULES.add(new ClickGuiModule());
    }
    public static List<ClientModule> in(Category c) { return MODULES.stream().filter(m -> m.category == c).toList(); }

    public static void tick(MinecraftClient mc) {
        // keybinds (only while no screen is open)
        for (ClientModule m : MODULES) {
            if (m.key < 0) continue;
            boolean down = mc.currentScreen == null && InputUtil.isKeyPressed(mc.getWindow(), m.key);
            if (down && !m.keyWasDown) m.toggle();
            m.keyWasDown = down;
        }
        for (ClientModule m : MODULES) if (m.enabled) m.onTick(mc);
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        for (ClientModule m : MODULES) if (m.enabled) m.onRender(ctx, mc);
    }
}
