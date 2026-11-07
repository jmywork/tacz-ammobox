package com.taczammobox.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One tab of the creative filter rail. Drawn like the vanilla creative inventory tabs
 * (same palette and bevel), which is also what TACZ's pack filters look like.
 */
public class CreativeFilterButton extends AbstractWidget {

    private static final int BORDER_DARK = 0xFF000000;
    private static final int BORDER_SOFT = 0xFF555555;
    private static final int BODY_TOP = 0xFFD8D8D8;
    private static final int BODY_BOTTOM = 0xFFC6C6C6;
    private static final int BODY_TOP_IDLE = 0xFFAFAFAF;
    private static final int BODY_BOTTOM_IDLE = 0xFF8B8B8B;
    private static final int HOVER_TINT = 0x22FFFFFF;

    private final ItemStack icon;
    private final Component label;
    private final Runnable onPress;
    private boolean selected;

    public CreativeFilterButton(int x, int y, ItemStack icon, Component label, Runnable onPress) {
        super(x, y, 32, 26, label);
        this.icon = icon;
        this.label = label;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;

        graphics.fillGradient(x + 1, y + 1, right - 1, bottom - 1,
                selected ? BODY_TOP : BODY_TOP_IDLE,
                selected ? BODY_BOTTOM : BODY_BOTTOM_IDLE);

        graphics.fill(x, y, right, y + 1, BORDER_DARK);
        graphics.fill(x, y, x + 1, bottom, BORDER_DARK);
        graphics.fill(right - 1, y + 1, right, bottom, BORDER_SOFT);
        if (!selected) {
            // The active tab melts into the panel below, like the vanilla ones.
            graphics.fill(x + 1, bottom - 1, right - 1, bottom, BORDER_SOFT);
        }
        graphics.fill(x + 1, y + 1, right - 1, y + 2, 0xFFFFFFFF);

        if (isHovered) {
            graphics.fill(x + 1, y + 1, right - 1, bottom - 1, HOVER_TINT);
        }

        graphics.renderItem(icon, x + 8, y + 5);

        if (isHovered) {
            graphics.renderTooltip(Minecraft.getInstance().font, label, mouseX, mouseY);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
