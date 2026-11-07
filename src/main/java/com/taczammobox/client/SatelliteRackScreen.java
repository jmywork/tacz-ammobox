package com.taczammobox.client;

import com.taczammobox.client.ui.TechTheme;
import com.taczammobox.menu.SatelliteRackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Dark-tech screen for the ten gun sockets of the satellite rack. */
public class SatelliteRackScreen extends AbstractContainerScreen<SatelliteRackMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 170;

    public SatelliteRackScreen(SatelliteRackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        TechTheme.window(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        TechTheme.panel(graphics, this.leftPos + 6, this.topPos + 16, this.imageWidth - 12, 48);
        TechTheme.panel(graphics, this.leftPos + 6, this.topPos + 82, this.imageWidth - 12, 58);
        for (Slot slot : this.menu.slots) {
            TechTheme.slot(graphics, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, TechTheme.TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.taczammobox.satellite_rack.hint"),
                this.leftPos + 8, this.topPos + 68, TechTheme.MUTED, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.leftPos + 8,
                this.topPos + this.inventoryLabelY, TechTheme.MUTED, false);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
