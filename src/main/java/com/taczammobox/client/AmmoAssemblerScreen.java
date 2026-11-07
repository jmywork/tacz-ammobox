package com.taczammobox.client;

import com.taczammobox.block.AmmoAssemblerBlockEntity;
import com.taczammobox.menu.AmmoAssemblerMenu;
import com.taczammobox.util.AssemblerRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * Compact three-column selector: ammo, its recipes, and the selected output. No player
 * inventory is shown; the machine is driven entirely by automation.
 */
public class AmmoAssemblerScreen extends AbstractContainerScreen<AmmoAssemblerMenu> {
    private static final String[] FACE_KEYS = {"up", "down", "left", "right", "front", "back"};

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 196;

    private static final int LIST_Y = 20;
    private static final int LIST_H = 128;

    private static final int AMMO_X = 8;
    private static final int AMMO_W = 48;
    private static final int RECIPE_X = 60;
    private static final int RECIPE_W = 124;
    private static final int OUT_X = 190;
    private static final int OUT_W = 44;

    private static final int AMMO_ROW_H = 18;
    private static final int RECIPE_ROW_H = 56;
    private static final int VISIBLE_RECIPES = 2;
    private static final int LINE_H = 18;
    private static final int MAX_LINES = 3;

    private int ammoScroll;
    private int recipeScroll;

    private final Button[] sideButtons = new Button[6];
    private final Direction[] shownDirections = new Direction[6];
    private Button redstoneButton;
    private Button lockButton;

    public AmmoAssemblerScreen(AmmoAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        Direction front = menu.getFacing();
        shownDirections[0] = Direction.UP;
        shownDirections[1] = Direction.DOWN;
        shownDirections[2] = front.getClockWise();
        shownDirections[3] = front.getCounterClockWise();
        shownDirections[4] = front;
        shownDirections[5] = front.getOpposite();

        for (int i = 0; i < sideButtons.length; i++) {
            int id = 1 + shownDirections[i].ordinal();
            sideButtons[i] = addRenderableWidget(Button.builder(Component.empty(), button -> click(id))
                    .bounds(leftPos + 244, topPos + 22 + i * 19, 92, 17).build());
        }
        redstoneButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> click(AmmoAssemblerMenu.BUTTON_REDSTONE))
                .bounds(leftPos + 244, topPos + 140, 92, 17).build());
        lockButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> click(AmmoAssemblerMenu.BUTTON_LOCK))
                .bounds(leftPos + 244, topPos + 161, 92, 17).build());
        updateLabels();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }

    private void updateLabels() {
        for (int i = 0; i < sideButtons.length; i++) {
            if (sideButtons[i] == null) {
                continue;
            }
            AmmoAssemblerBlockEntity.SideMode mode = menu.getSide(shownDirections[i]);
            MutableComponent label = Component.translatable("gui.taczammobox.side." + FACE_KEYS[i]);
            label.append(" ").append(Component.translatable("gui.taczammobox.side." + mode.name().toLowerCase()));
            sideButtons[i].setMessage(label.withStyle(color(mode)));
        }
        if (redstoneButton != null) {
            AmmoAssemblerBlockEntity.RedstoneMode mode = menu.getRedstoneMode();
            redstoneButton.setMessage(Component.translatable(
                    "gui.taczammobox.redstone." + mode.name().toLowerCase()).withStyle(redstoneColor(mode)));
        }
        if (lockButton != null) {
            lockButton.active = menu.isLocked() || menu.selectedEntry() != null;
            lockButton.setMessage(Component.translatable(menu.isLocked()
                    ? "gui.taczammobox.ammo_assembler.lock.on"
                    : "gui.taczammobox.ammo_assembler.lock.off")
                    .withStyle(menu.isLocked() ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
    }

    private static ChatFormatting color(AmmoAssemblerBlockEntity.SideMode mode) {
        if (mode == AmmoAssemblerBlockEntity.SideMode.INPUT) {
            return ChatFormatting.GREEN;
        }
        if (mode == AmmoAssemblerBlockEntity.SideMode.OUTPUT) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.DARK_GRAY;
    }

    private static ChatFormatting redstoneColor(AmmoAssemblerBlockEntity.RedstoneMode mode) {
        if (mode == AmmoAssemblerBlockEntity.RedstoneMode.HIGH) {
            return ChatFormatting.RED;
        }
        if (mode == AmmoAssemblerBlockEntity.RedstoneMode.LOW) {
            return ChatFormatting.AQUA;
        }
        return ChatFormatting.DARK_GRAY;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFFC6C6C6);
        graphics.fill(x, y, x + PANEL_W, y + 1, 0xFF373737);
        graphics.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, 0xFF373737);
        graphics.fill(x, y, x + 1, y + PANEL_H, 0xFF373737);
        graphics.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, 0xFF373737);

        // right configuration strip
        graphics.fill(x + 240, y + 1, x + PANEL_W - 1, y + PANEL_H - 1, 0xFFB8B8B8);
        graphics.fill(x + 239, y + 1, x + 240, y + PANEL_H - 1, 0xFF373737);

        // three panes
        pane(graphics, x + AMMO_X - 2, y + LIST_Y - 2, AMMO_W + 4, LIST_H + 4);
        pane(graphics, x + RECIPE_X - 2, y + LIST_Y - 2, RECIPE_W + 4, LIST_H + 4);
        pane(graphics, x + OUT_X - 2, y + LIST_Y - 2, OUT_W + 4, LIST_H + 4);

        divider(graphics, x + AMMO_X + AMMO_W + 1, y + LIST_Y - 2, y + LIST_Y + LIST_H + 2);
        divider(graphics, x + RECIPE_X + RECIPE_W + 1, y + LIST_Y - 2, y + LIST_Y + LIST_H + 2);

        renderAmmo(graphics, x, y);
        renderRecipes(graphics, x, y);
        renderOutput(graphics, x, y);
    }

    private void renderAmmo(GuiGraphics graphics, int x, int y) {
        List<AssemblerRecipes.Group> groups = menu.groups();
        int selected = menu.getSelectedAmmo();
        int visible = LIST_H / AMMO_ROW_H;
        for (int row = 0; row < visible; row++) {
            int index = ammoScroll + row;
            if (index >= groups.size()) {
                break;
            }
            int rowY = y + LIST_Y + row * AMMO_ROW_H;
            if (index == selected) {
                graphics.fill(x + AMMO_X - 1, rowY - 1, x + AMMO_X + AMMO_W + 1, rowY + AMMO_ROW_H - 1,
                        0xFF9A9A9A);
            }
            graphics.renderItem(groups.get(index).display(), x + AMMO_X + 3, rowY);
        }
    }

    private void renderRecipes(GuiGraphics graphics, int x, int y) {
        AssemblerRecipes.Group group = menu.selectedGroup();
        if (group == null) {
            return;
        }
        var font = minecraft.font;
        List<AssemblerRecipes.Entry> recipes = group.recipes();
        int selected = menu.getSelectedRecipe();
        for (int row = 0; row < VISIBLE_RECIPES; row++) {
            int index = recipeScroll + row;
            if (index >= recipes.size()) {
                break;
            }
            int rowY = y + LIST_Y + row * RECIPE_ROW_H;
            if (index == selected) {
                graphics.fill(x + RECIPE_X - 1, rowY - 1, x + RECIPE_X + RECIPE_W + 1,
                        rowY + RECIPE_ROW_H - 1, 0xFF9A9A9A);
            }
            AssemblerRecipes.Entry entry = recipes.get(index);
            int baseX = x + RECIPE_X + 3;
            int iconX = baseX;
            int iconY = rowY + 2;
            int line = 0;
            int paneRight = x + RECIPE_X + RECIPE_W - 2;
            for (AssemblerRecipes.IngredientStack requirement : entry.inputs()) {
                ItemStack[] items = requirement.ingredient().getItems();
                if (items.length == 0) {
                    continue;
                }
                String text = String.valueOf(requirement.count());
                int cell = 17 + font.width(text) + 4;
                if (iconX + cell > paneRight) {
                    if (line + 1 >= MAX_LINES) {
                        break;
                    }
                    line++;
                    iconX = baseX;
                    iconY += LINE_H;
                }
                graphics.renderItem(items[0].copyWithCount(1), iconX, iconY);
                boolean enough = countAvailable(requirement.ingredient()) >= requirement.count();
                graphics.drawString(font, text, iconX + 17, iconY + 4,
                        enough ? 0xFF303030 : 0xFFB00000, false);
                iconX += cell;
            }
        }
    }

    private void renderOutput(GuiGraphics graphics, int x, int y) {
        AssemblerRecipes.Entry entry = menu.selectedEntry();
        if (entry == null) {
            return;
        }
        var font = minecraft.font;
        graphics.renderItem(entry.output().copyWithCount(1), x + OUT_X + 14, y + LIST_Y + 16);
        String count = "x" + entry.output().getCount();
        graphics.drawString(font, count, x + OUT_X + (OUT_W - font.width(count)) / 2,
                y + LIST_Y + 40, 0x404040, false);

        int progress = menu.getProgress();
        int max = menu.getMaxProgress();
        int width = max <= 0 ? 0 : Math.min(OUT_W - 6, progress * (OUT_W - 6) / max);
        int barY = y + LIST_Y + 60;
        graphics.fill(x + OUT_X + 3, barY, x + OUT_X + OUT_W - 3, barY + 6, 0xFF8B8B8B);
        if (width > 0) {
            graphics.fill(x + OUT_X + 3, barY, x + OUT_X + 3 + width, barY + 6, 0xFF6FBF4A);
        }
    }

    private static void pane(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFFB8B8B8);
        graphics.fill(x, y, x + w, y + 1, 0xFF8B8B8B);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF8B8B8B);
    }

    private static void divider(GuiGraphics graphics, int x, int top, int bottom) {
        graphics.fill(x, top, x + 2, bottom, 0xFF373737);
    }

    private int countAvailable(Ingredient ingredient) {
        return menu.countAvailable(ingredient);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.ammo"),
                AMMO_X, 14, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.recipes"),
                RECIPE_X, 14, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.output"),
                OUT_X, 14, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.sides"), 244, 8, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int localX = (int) mouseX - leftPos;
            int localY = (int) mouseY - topPos;
            if (localY >= LIST_Y && localY < LIST_Y + LIST_H && !menu.isLocked()) {
                if (localX >= AMMO_X && localX < AMMO_X + AMMO_W) {
                    int index = ammoScroll + (localY - LIST_Y) / AMMO_ROW_H;
                    if (index < menu.groups().size()) {
                        click(AmmoAssemblerMenu.AMMO_BUTTON_OFFSET + index);
                        return true;
                    }
                }
                if (localX >= RECIPE_X && localX < RECIPE_X + RECIPE_W) {
                    AssemblerRecipes.Group group = menu.selectedGroup();
                    if (group != null) {
                        int index = recipeScroll + (localY - LIST_Y) / RECIPE_ROW_H;
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localY >= LIST_Y && localY < LIST_Y + LIST_H) {
            if (localX >= AMMO_X && localX < AMMO_X + AMMO_W) {
                int visible = LIST_H / AMMO_ROW_H;
                int max = Math.max(0, menu.groups().size() - visible);
                ammoScroll = Math.max(0, Math.min(max, ammoScroll - (int) Math.signum(delta)));
                return true;
            }
            if (localX >= RECIPE_X && localX < RECIPE_X + RECIPE_W) {
                AssemblerRecipes.Group group = menu.selectedGroup();
                int size = group == null ? 0 : group.recipes().size();
                int max = Math.max(0, size - VISIBLE_RECIPES);
                recipeScroll = Math.max(0, Math.min(max, recipeScroll - (int) Math.signum(delta)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
