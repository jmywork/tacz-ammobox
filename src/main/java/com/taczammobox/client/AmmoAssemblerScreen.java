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

/**
 * Three panes: ammo types, the recipes of the selected ammo, and the output of the selected
 * recipe (count plus ingredients), mirroring how JEI presents gun-smith-table recipes.
 */
public class AmmoAssemblerScreen extends AbstractContainerScreen<AmmoAssemblerMenu> {
    private static final int PANEL_W = 210;
    private static final int PANEL_H = 246;

    private static final int AMMO_X = 8;
    private static final int AMMO_Y = 18;
    private static final int AMMO_W = 48;
    private static final int RECIPE_X = 60;
    private static final int RECIPE_W = 92;
    private static final int LIST_H = 78;
    private static final int ROW_H = 18;
    private static final int VISIBLE_ROWS = 4;
    private static final int OUT_X = 154;
    private static final int OUT_W = 52;

    private int ammoScroll;
    private int recipeScroll;

    public AmmoAssemblerScreen(AmmoAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = 158;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        var font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF262A30);
        graphics.fill(x + 2, y + 2, x + PANEL_W - 2, y + PANEL_H - 2, 0xFF343B44);

        // pane backgrounds and titles
        pane(graphics, x + AMMO_X - 2, y + AMMO_Y - 2, AMMO_W + 4, LIST_H + 4);
        pane(graphics, x + RECIPE_X - 2, y + AMMO_Y - 2, RECIPE_W + 4, LIST_H + 4);
        pane(graphics, x + OUT_X - 2, y + AMMO_Y - 2, OUT_W + 4, LIST_H + 4);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.ammo"),
                x + AMMO_X, y + 6, 0xFFAEB7C2, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.recipes"),
                x + RECIPE_X, y + 6, 0xFFAEB7C2, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.output"),
                x + OUT_X, y + 6, 0xFFAEB7C2, false);

        // column 1: ammo types
        List<AssemblerRecipes.Group> groups = menu.groups();
        int selectedAmmo = menu.getSelectedAmmo();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = ammoScroll + row;
            if (index >= groups.size()) {
                break;
            }
            int rowY = y + AMMO_Y + row * ROW_H;
            if (index == selectedAmmo) {
                graphics.fill(x + AMMO_X - 1, rowY - 1, x + AMMO_X + AMMO_W + 1, rowY + ROW_H - 1, 0xFF55606D);
            }
            graphics.renderItem(groups.get(index).display(), x + AMMO_X + 2, rowY);
        }

        // column 2: recipes of the selected ammo
        AssemblerRecipes.Group group = menu.selectedGroup();
        int selectedRecipe = menu.getSelectedRecipe();
        if (group != null) {
            List<AssemblerRecipes.Entry> recipes = group.recipes();
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int index = recipeScroll + row;
                if (index >= recipes.size()) {
                    break;
                }
                int rowY = y + AMMO_Y + row * ROW_H;
                if (index == selectedRecipe) {
                    graphics.fill(x + RECIPE_X - 1, rowY - 1, x + RECIPE_X + RECIPE_W + 1, rowY + ROW_H - 1,
                            0xFF55606D);
                }
                AssemblerRecipes.Entry entry = recipes.get(index);
                int iconX = x + RECIPE_X + 2;
                for (AssemblerRecipes.IngredientStack requirement : entry.inputs()) {
                    ItemStack[] items = requirement.ingredient().getItems();
                    if (items.length == 0) {
                        continue;
                    }
                    graphics.renderItem(items[0], iconX, rowY);
                    if (requirement.count() > 1) {
                        graphics.drawString(font, String.valueOf(requirement.count()), iconX + 9, rowY + 9,
                                0xFFFFFF55, true);
                    }
                    iconX += 17;
                    if (iconX > x + RECIPE_X + RECIPE_W - 16) {
                        break;
                    }
                }
            }
        }

        // column 3: output of the selected recipe
        AssemblerRecipes.Entry entry = menu.selectedEntry();
        if (entry != null) {
            graphics.renderItem(entry.output(), x + OUT_X + 17, y + AMMO_Y + 10);
            String count = "x" + entry.output().getCount();
            graphics.drawString(font, count, x + OUT_X + (OUT_W - font.width(count)) / 2,
                    y + AMMO_Y + 32, 0xFFFFFFFF, false);
        }

        // input grid, output slot, progress
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                frame(graphics, x + AmmoAssemblerMenu.GRID_X - 1 + col * 18,
                        y + AmmoAssemblerMenu.GRID_Y - 1 + row * 18);
            }
        }
        frame(graphics, x + 77, y + 121);
        int progress = menu.getProgress();
        int max = menu.getMaxProgress();
        int width = max <= 0 ? 0 : Math.min(32, progress * 32 / max);
        graphics.fill(x + 44, y + 130, x + 76, y + 138, 0xFF23272D);
        if (width > 0) {
            graphics.fill(x + 44, y + 130, x + 44 + width, y + 138, 0xFFE29B2A);
        }
    }

    private static void pane(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF23272D);
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
            if (localY >= AMMO_Y && localY < AMMO_Y + VISIBLE_ROWS * ROW_H) {
                if (localX >= AMMO_X && localX < AMMO_X + AMMO_W) {
                    int index = ammoScroll + (localY - AMMO_Y) / ROW_H;
                    if (index < menu.groups().size()) {
                        click(index);
                        return true;
                    }
                }
                if (localX >= RECIPE_X && localX < RECIPE_X + RECIPE_W) {
                    AssemblerRecipes.Group group = menu.selectedGroup();
                    if (group != null) {
                        int index = recipeScroll + (localY - AMMO_Y) / ROW_H;
                        if (index < group.recipes().size()) {
                            click(AmmoAssemblerMenu.RECIPE_BUTTON_OFFSET + index);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localY >= AMMO_Y && localY < AMMO_Y + VISIBLE_ROWS * ROW_H) {
            if (localX >= AMMO_X && localX < AMMO_X + AMMO_W) {
                int max = Math.max(0, menu.groups().size() - VISIBLE_ROWS);
                ammoScroll = Math.max(0, Math.min(max, ammoScroll - (int) Math.signum(delta)));
                return true;
            }
            if (localX >= RECIPE_X && localX < RECIPE_X + RECIPE_W) {
                AssemblerRecipes.Group group = menu.selectedGroup();
                int size = group == null ? 0 : group.recipes().size();
                int max = Math.max(0, size - VISIBLE_ROWS);
                recipeScroll = Math.max(0, Math.min(max, recipeScroll - (int) Math.signum(delta)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
