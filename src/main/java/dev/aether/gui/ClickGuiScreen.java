package dev.aether.gui;

import dev.aether.config.ConfigManager;
import dev.aether.module.*;
import dev.aether.module.impl.ClickGuiModule;
import dev.aether.setting.*;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;               // VERSION-SENSITIVE (1.21.9+ input events)
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int W = 112, HH = 18, MH = 16, SH = 14, BAR = 8;

    private static class Panel {
        final Category cat; int x, y; boolean open = true;
        Panel(Category c, int x, int y) { cat = c; this.x = x; this.y = y; }
    }
    private interface Action { void run(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, Action click, Action drag) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private static List<Panel> panels;
    private final List<Hit> hits = new ArrayList<>();
    private Hit activeDrag; private int dragBtn;
    private Panel draggingPanel; private double dragX, dragY;
    private double scroll;
    private ClientModule binding;

    public ClickGuiScreen() {
        super(Text.literal("Aether"));
        if (panels == null) {
            panels = new ArrayList<>();
            int i = 0;
            for (Category c : Category.values()) panels.add(new Panel(c, 12 + i++ * (W + 10), 12));
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { ConfigManager.save(); super.close(); }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        hits.clear();
        g.fill(0, 0, width, height, ClickGuiModule.dimARGB());
        for (Panel p : panels) drawPanel(g, p, mx, my);
    }

    private void drawPanel(DrawContext g, Panel p, int mx, int my) {
        int ac = ClickGuiModule.accent();
        int x = p.x, y = p.y + (int) scroll;

        g.fill(x, y, x + W, y + HH, ac);
        g.drawText(textRenderer, p.cat.label, x + (W - textRenderer.getWidth(p.cat.label)) / 2, y + 5, 0xFFFFFFFF, true);
        hits.add(new Hit(x, y, W, HH, (a, b, btn) -> {
            if (btn == 0) { draggingPanel = p; dragX = a - p.x; dragY = b - (p.y + scroll); }
            else if (btn == 1) p.open = !p.open;
        }, null));
        if (!p.open) return;

        int cy = y + HH + 2;
        for (ClientModule m : ModuleManager.in(p.cat)) {
            boolean hover = mx >= x && mx < x + W && my >= cy && my < cy + MH;
            g.fill(x, cy, x + W, cy + MH, hover ? 0xE0202028 : 0xD8101014);
            if (m.enabled) g.fill(x, cy, x + 2, cy + MH, ac);
            g.drawText(textRenderer, m.enabled ? "-" : "+", x + 7, cy + 4, m.enabled ? ac : 0xFF8A8A8A, false);
            g.drawText(textRenderer, m.name, x + 20, cy + 4, m.enabled ? 0xFFFFFFFF : 0xFF9A9A9A, false);
            if (!m.settings.isEmpty())
                g.drawText(textRenderer, m.expanded ? "v" : ">", x + W - 10, cy + 4, 0xFFAAAAAA, false);
            String kl = binding == m ? "..." : (m.key >= 0 ? "[" + keyName(m.key) + "]" : null);
            if (kl != null) g.drawText(textRenderer, kl, x + W - 14 - textRenderer.getWidth(kl), cy + 4, 0xFF777788, false);
            hits.add(new Hit(x, cy, W, MH, (a, b, btn) -> {
                if (btn == 0) m.toggle(); else if (btn == 1) m.expanded = !m.expanded;
                else if (btn == 2) binding = binding == m ? null : m;
            }, null));
            cy += MH;

            if (m.expanded)
                for (Setting<?> s : m.settings) if (s.isVisible()) cy = drawSetting(g, s, x, cy, ac);
        }
    }

    private int drawSetting(DrawContext g, Setting<?> s, int x, int y, int ac) {
        g.fill(x, y, x + W, y + SH, 0xE0181820);
        int ty = y + 3;

        if (s instanceof BoolSetting b) {
            g.drawText(textRenderer, b.name, x + 8, ty, 0xFFCCCCCC, false);
            g.fill(x + W - 16, y + 3, x + W - 7, y + 12, 0xFF444450);
            if (b.get()) g.fill(x + W - 15, y + 4, x + W - 8, y + 11, ac);
            hits.add(new Hit(x, y, W, SH, (a, c, btn) -> { if (btn == 0) b.set(!b.get()); }, null));
            return y + SH;
        }
        if (s instanceof NumberSetting n) {
            double f = (n.get() - n.min) / (n.max - n.min);
            g.fill(x, y, x + (int) (W * f), y + SH, (ac & 0xFFFFFF) | 0x70000000);
            g.drawText(textRenderer, n.name, x + 8, ty, 0xFFCCCCCC, false);
            String v = n.step >= 1 ? String.valueOf(n.i()) : String.format("%.2f", n.get());
            g.drawText(textRenderer, v, x + W - 6 - textRenderer.getWidth(v), ty, 0xFFFFFFFF, false);
            Action a = (mx, my, btn) -> n.set(n.min + MathHelper.clamp((mx - x) / W, 0, 1) * (n.max - n.min));
            hits.add(new Hit(x, y, W, SH, (mx, my, btn) -> { if (btn == 0) a.run(mx, my, btn); }, a));
            return y + SH;
        }
        if (s instanceof ModeSetting md) {
            g.drawText(textRenderer, md.name, x + 8, ty, 0xFFCCCCCC, false);
            g.drawText(textRenderer, md.get(), x + W - 6 - textRenderer.getWidth(md.get()), ty, ac, false);
            hits.add(new Hit(x, y, W, SH, (a, c, btn) -> md.cycle(btn == 1 ? -1 : 1), null));
            return y + SH;
        }
        if (s instanceof ColorSetting c) {
            g.drawText(textRenderer, c.name, x + 8, ty, 0xFFCCCCCC, false);
            g.fill(x + W - 26, y + 3, x + W - 6, y + 11, 0xFF000000 | c.rgb());
            int by = y + SH;
            g.fill(x, by, x + W, by + BAR * 3, 0xE0181820);
            for (int i = 0; i < 3; i++) drawBar(g, c, i, x + 6, by + i * BAR + 1, W - 12);
            return by + BAR * 3;
        }
        return y;
    }

    private void drawBar(DrawContext g, ColorSetting c, int type, int bx, int by, int bw) {
        final int seg = 28;
        for (int k = 0; k < seg; k++) {
            float t = k / (float) seg;
            int col = switch (type) {
                case 0 -> MathHelper.hsvToRgb(t, c.s, c.b);
                case 1 -> MathHelper.hsvToRgb(c.h, t, c.b);
                default -> MathHelper.hsvToRgb(c.h, c.s, t);
            };
            g.fill(bx + k * bw / seg, by, bx + (k + 1) * bw / seg, by + BAR - 2, 0xFF000000 | col);
        }
        float val = type == 0 ? c.h : type == 1 ? c.s : c.b;
        int mxp = bx + (int) (val * bw);
        g.fill(mxp - 1, by - 1, mxp + 1, by + BAR - 1, 0xFFFFFFFF);

        Action a = (mx, my, btn) -> {
            float v = MathHelper.clamp((float) (mx - bx) / bw, 0f, 1f);
            if (type == 0) c.h = Math.min(v, 0.999f); else if (type == 1) c.s = v; else c.b = v;
        };
        hits.add(new Hit(bx, by, bw, BAR, (mx, my, btn) -> { if (btn == 0) a.run(mx, my, btn); }, a));
    }

    // ---- VERSION-SENSITIVE: 1.21.9+ passes a Click record instead of (x, y, button) ----
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y(); int btn = click.button();
        if (binding != null && btn != 2) binding = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                h.click().run(mx, my, btn);
                if (h.drag() != null) { activeDrag = h; dragBtn = btn; }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingPanel != null) {
            draggingPanel.x = (int) (click.x() - dragX);
            draggingPanel.y = (int) (click.y() - dragY - scroll);
            return true;
        }
        if (activeDrag != null && dragBtn == 0) { activeDrag.drag().run(click.x(), click.y(), dragBtn); return true; }
        return super.mouseDragged(click, dx, dy);
    }
    @Override public boolean mouseReleased(Click click) {
        draggingPanel = null; activeDrag = null;
        return super.mouseReleased(click);
    }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.min(0, scroll + v * 20);
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (binding != null) {
            int k = input.getKeycode();
            if (k == GLFW.GLFW_KEY_ESCAPE) binding = null;
            else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) { binding.key = -1; binding = null; }
            else { binding.key = k; binding = null; }
            return true;
        }
        return super.keyPressed(input);
    }

    private static String keyName(int k) {
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(k, 0);
        if (n != null) return n.toUpperCase();
        return switch (k) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT"; case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL"; case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT"; case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_UP -> "UP"; case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT"; case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE"; case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "#" + k;
        };
    }
}
