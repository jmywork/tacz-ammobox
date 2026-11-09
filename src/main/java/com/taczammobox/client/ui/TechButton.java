package com.taczammobox.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Button styled for the dark-tech machine panels. */
public class TechButton extends Button {
    private int textColor = TechTheme.TEXT;

    public TechButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    public TechButton setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = !active ? TechTheme.PANEL : isHoveredOrFocused() ? TechTheme.PANEL2 : TechTheme.SLOT;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        int border = isHoveredOrFocused() ? TechTheme.ACCENT : TechTheme.BORDER;
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
        int color = active ? textColor : TechTheme.MUTED;
        graphics.drawCenteredString(getFont(), getMessage(), getX() + width / 2,
                getY() + (height - 8) / 2, color);
    }

    private net.minecraft.client.gui.Font getFont() {
        return net.minecraft.client.Minecraft.getInstance().font;
    }
}
