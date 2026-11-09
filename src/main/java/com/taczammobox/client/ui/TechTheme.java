package com.taczammobox.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared "dark tech" palette and drawing helpers for the three machine screens. */
public final class TechTheme {
    public static final int BG = 0xFF0E1218;
    public static final int PANEL = 0xFF181F29;
    public static final int PANEL2 = 0xFF212B38;
    public static final int BORDER = 0xFF34465E;
    public static final int ACCENT = 0xFF38E1D0;
    public static final int ACCENT2 = 0xFFFFB020;
    public static final int TEXT = 0xFFDBE7F6;
    public static final int MUTED = 0xFF7688A0;
    public static final int SLOT = 0xFF121820;
    public static final int SLOT_BORDER = 0xFF2A3A4E;
    public static final int OK = 0xFF7BE3B0;
    public static final int WARN = 0xFFFF6B5B;

    private TechTheme() {
    }

    public static void window(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, BG);
        graphics.fill(x, y, x + w, y + 1, BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER);
        graphics.fill(x, y, x + 1, y + h, BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.fill(x, y, x + w, y + 1, BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER);
    }

    public static void subPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL2);
        graphics.fill(x, y, x + w, y + 1, BORDER);
    }

    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    public static void bar(GuiGraphics graphics, int x, int y, int w, int h, float progress) {
        graphics.fill(x, y, x + w, y + h, SLOT);
        graphics.fill(x, y, x + w, y + 1, SLOT_BORDER);
        int filled = Math.max(0, Math.min(w, (int) (w * progress)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + h, ACCENT);
        }
    }

    public static void header(GuiGraphics graphics, Font font, int x, int y, int w, Component title) {
        graphics.drawString(font, title, x, y, TEXT, false);
        graphics.fill(x, y + 11, x + w, y + 12, ACCENT);
    }

    public static void divider(GuiGraphics graphics, int x, int top, int bottom) {
        graphics.fill(x, top, x + 1, bottom, BORDER);
    }
}
