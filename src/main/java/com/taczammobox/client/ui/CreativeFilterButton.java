package com.taczammobox.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A small square rail button for the creative inventory, styled like TACZ's gun pack
 * filters: an item icon on a dark plate, with a teal edge while it is the active filter.
 */
public class CreativeFilterButton extends AbstractWidget {

    private static final int PLATE = 0xFF131A22;
    private static final int PLATE_HOVER = 0xFF1D2733;
    private static final int PLATE_SELECTED = 0xFF223040;
    private static final int BORDER = 0xFF2C3A4A;
    private static final int ACCENT = 0xFF38E1D0;

    private final ItemStack icon;
    private final Component label;
    private final Runnable onPress;
    private boolean selected;

    public CreativeFilterButton(int x, int y, ItemStack icon, Component label, Runnable onPress) {
        super(x, y, 20, 20, label);
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
        int plate = selected ? PLATE_SELECTED : (isHovered ? PLATE_HOVER : PLATE);
        graphics.fill(x, y, x + width, y + height, plate);
        border(graphics, x, y, width, height, selected ? ACCENT : BORDER);
        graphics.renderItem(icon, x + 2, y + 2);
        if (selected) {
            graphics.fill(x, y, x + 2, y + height, ACCENT);
        }
        if (isHovered) {
            graphics.renderTooltip(Minecraft.getInstance().font, label, mouseX, mouseY);
        }
    }

    private static void border(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
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
