package com.taczammobox.client;

import com.taczammobox.menu.AmmoAssemblerMenu;
import com.taczammobox.util.AssemblerRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Recipe list on the left, input grid on the right, selected recipe requirements at the bottom. */
public class AmmoAssemblerScreen extends AbstractContainerScreen<AmmoAssemblerMenu> {
    private static final int LIST_X = 10;
    private static final int LIST_Y = 20;
    private static final int LIST_W = 96;
    private static final int ROW_H = 12;
    private static final int VISIBLE_ROWS = 9;
    private static final int PANEL_W = 210;
    private static final int PANEL_H = 222;

    private int scroll;

    public AmmoAssemblerScreen(AmmoAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = 140;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF2B2F36);
        graphics.fill(x + 2, y + 2, x + PANEL_W - 2, y + PANEL_H - 2, 0xFF3A4049);

        // list background
        graphics.fill(x + LIST_X - 2, y + LIST_Y - 2, x + LIST_X + LIST_W + 2, y + LIST_Y + VISIBLE_ROWS * ROW_H + 2,
                0xFF23272D);

        // input slot frames
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                frame(graphics, x + AmmoAssemblerMenu.GRID_X - 1 + col * 18,
                        y + AmmoAssemblerMenu.GRID_Y - 1 + row * 18);
            }
        }
        frame(graphics, x + 175, y + 35);

        // progress bar
        int progress = menu.getProgress();
        int max = menu.getMaxProgress();
        int width = max <= 0 ? 0 : Math.min(40, progress * 40 / max);
        graphics.fill(x + 160, y + 64, x + 200, y + 72, 0xFF23272D);
        if (width > 0) {
            graphics.fill(x + 160, y + 64, x + 160 + width, y + 72, 0xFFE29B2A);
        }

        List<AssemblerRecipes.Entry> entries = menu.recipes();
        int selected = menu.getSelectedIndex();
        var font = Minecraft.getInstance().font;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= entries.size()) {
                break;
            }
            AssemblerRecipes.Entry entry = entries.get(index);
            int rowY = y + LIST_Y + row * ROW_H;
            if (index == selected) {
                graphics.fill(x + LIST_X - 1, rowY - 1, x + LIST_X + LIST_W + 1, rowY + ROW_H - 1, 0xFF5A6470);
            }
            graphics.renderItem(entry.output(), x + LIST_X, rowY - 2);
            String label = entry.output().getHoverName().getString() + " x" + entry.output().getCount();
            if (font.width(label) > LIST_W - 20) {
                label = font.plainSubstrByWidth(label, LIST_W - 20) + "…";
            }
            graphics.drawString(font, label, x + LIST_X + 18, rowY, 0xFFE6E6E6, false);
        }

        // requirements of the selected recipe
        AssemblerRecipes.Entry entry = menu.selectedEntry();
        if (entry != null) {
            int reqY = y + 124;
            graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.requires"),
                    x + 10, reqY - 12, 0xFFBFC6CF, false);
            int reqX = x + 10;
            for (AssemblerRecipes.IngredientStack requirement : entry.inputs()) {
                ItemStack[] items = requirement.ingredient().getItems();
                if (items.length == 0) {
                    continue;
                }
                graphics.renderItem(items[0], reqX, reqY);
                if (requirement.count() > 1) {
                    graphics.drawString(font, String.valueOf(requirement.count()), reqX + 9, reqY + 9,
                            0xFFFFFFFF, true);
                }
                reqX += 20;
            }
        }
    }

    private static void frame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF171A1E);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF4A515B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int localX = (int) mouseX - leftPos;
            int localY = (int) mouseY - topPos;
            if (localX >= LIST_X && localX <= LIST_X + LIST_W
                    && localY >= LIST_Y && localY < LIST_Y + VISIBLE_ROWS * ROW_H) {
                int row = (localY - LIST_Y) / ROW_H;
                int index = scroll + row;
                if (index < menu.recipes().size()) {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, menu.recipes().size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta)));
        return true;
    }
}
