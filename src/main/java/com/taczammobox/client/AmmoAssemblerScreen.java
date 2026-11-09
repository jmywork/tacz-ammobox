package com.taczammobox.client;

import com.taczammobox.block.AmmoAssemblerBlockEntity;
import com.taczammobox.menu.AmmoAssemblerMenu;
import com.taczammobox.util.AssemblerRecipes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import com.taczammobox.client.ui.TechButton;
import com.taczammobox.client.ui.TechTheme;
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
    private static final int PANEL_H = 180;

    private static final int LIST_Y = 28;
    private static final int LIST_H = 124;

    private static final int AMMO_X = 8;
    private static final int AMMO_W = 48;
    private static final int RECIPE_X = 60;
    private static final int RECIPE_W = 124;
    private static final int OUT_X = 190;
    private static final int OUT_W = 44;

    private static final int AMMO_ROW_H = 18;
    private static final int AMMO_VISIBLE = LIST_H / AMMO_ROW_H;
    private static final int RECIPE_ROW_H = 56;
    private static final int VISIBLE_RECIPES = 2;
    private static final int LINE_H = 18;
    private static final int MAX_LINES = 3;

    private static final int SCROLL_W = 4;
    private static final int AMMO_BAR_X = AMMO_X + AMMO_W - 6;
    private static final int RECIPE_BAR_X = RECIPE_X + RECIPE_W - 6;

    private int ammoScroll;
    private int recipeScroll;
    private boolean draggingAmmoBar;
    private boolean draggingRecipeBar;

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
            sideButtons[i] = addRenderableWidget(new TechButton(leftPos + 244, topPos + 26 + i * 19, 92, 17,
                    Component.empty(), button -> click(id)));
        }
        redstoneButton = addRenderableWidget(new TechButton(leftPos + 244, topPos + 135, 92, 17,
                Component.empty(), button -> click(AmmoAssemblerMenu.BUTTON_REDSTONE)));
        lockButton = addRenderableWidget(new TechButton(leftPos + 244, topPos + 156, 92, 17,
                Component.empty(), button -> click(AmmoAssemblerMenu.BUTTON_LOCK)));
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
            sideButtons[i].setMessage(label);
            ((TechButton) sideButtons[i]).setTextColor(sideColor(mode));
        }
        if (redstoneButton != null) {
            AmmoAssemblerBlockEntity.RedstoneMode mode = menu.getRedstoneMode();
            redstoneButton.setMessage(Component.translatable(
                    "gui.taczammobox.redstone." + mode.name().toLowerCase()));
            ((TechButton) redstoneButton).setTextColor(redstoneColor(mode));
        }
        if (lockButton != null) {
            lockButton.active = menu.isLocked() || menu.selectedEntry() != null;
            lockButton.setMessage(Component.translatable(menu.isLocked()
                    ? "gui.taczammobox.ammo_assembler.lock.on"
                    : "gui.taczammobox.ammo_assembler.lock.off")
                    );
            ((TechButton) lockButton).setTextColor(menu.isLocked() ? TechTheme.ACCENT2 : TechTheme.MUTED);
        }
    }

    private static int sideColor(AmmoAssemblerBlockEntity.SideMode mode) {
        if (mode == AmmoAssemblerBlockEntity.SideMode.INPUT) {
            return TechTheme.ACCENT;
        }
        if (mode == AmmoAssemblerBlockEntity.SideMode.OUTPUT) {
            return TechTheme.ACCENT2;
        }
        return TechTheme.MUTED;
    }

    private static int redstoneColor(AmmoAssemblerBlockEntity.RedstoneMode mode) {
        if (mode == AmmoAssemblerBlockEntity.RedstoneMode.HIGH) {
            return TechTheme.WARN;
        }
        if (mode == AmmoAssemblerBlockEntity.RedstoneMode.LOW) {
            return TechTheme.ACCENT;
        }
        return TechTheme.MUTED;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, TechTheme.BG);
        graphics.fill(x, y, x + PANEL_W, y + 1, TechTheme.BORDER);
        graphics.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, TechTheme.BORDER);
        graphics.fill(x, y, x + 1, y + PANEL_H, TechTheme.BORDER);
        graphics.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, TechTheme.BORDER);

        // right configuration strip
        graphics.fill(x + 240, y + 1, x + PANEL_W - 1, y + PANEL_H - 1, TechTheme.PANEL);
        graphics.fill(x + 239, y + 1, x + 240, y + PANEL_H - 1, TechTheme.BORDER);

        // three panes
        pane(graphics, x + AMMO_X - 2, y + LIST_Y - 2, AMMO_W + 4, LIST_H + 4);
        pane(graphics, x + RECIPE_X - 2, y + LIST_Y - 2, RECIPE_W + 4, LIST_H + 4);
        pane(graphics, x + OUT_X - 2, y + LIST_Y - 2, OUT_W + 4, LIST_H + 4);

        divider(graphics, x + AMMO_X + AMMO_W + 1, y + LIST_Y - 2, y + LIST_Y + LIST_H + 2);
        divider(graphics, x + RECIPE_X + RECIPE_W + 1, y + LIST_Y - 2, y + LIST_Y + LIST_H + 2);

        renderAmmo(graphics, x, y);
        renderRecipes(graphics, x, y);
        renderOutput(graphics, x, y);
        renderScrollbars(graphics, x, y);
    }

    private void renderAmmo(GuiGraphics graphics, int x, int y) {
        List<AssemblerRecipes.Group> groups = menu.groups();
        int selected = menu.getSelectedAmmo();
        int visible = AMMO_VISIBLE;
        ammoScroll = Math.max(0, Math.min(ammoScroll, Math.max(0, groups.size() - visible)));
        for (int row = 0; row < visible; row++) {
            int index = ammoScroll + row;
            if (index >= groups.size()) {
                break;
            }
            int rowY = y + LIST_Y + row * AMMO_ROW_H;
            if (index == selected) {
                graphics.fill(x + AMMO_X - 1, rowY - 1, x + AMMO_X + AMMO_W + 1, rowY + AMMO_ROW_H - 1,
                        0xFF2E4258);
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
                        rowY + RECIPE_ROW_H - 1, 0xFF2E4258);
            }
            AssemblerRecipes.Entry entry = recipes.get(index);
            int baseX = x + RECIPE_X + 3;
            int iconX = baseX;
            int iconY = rowY + 2;
            int line = 0;
            int paneRight = x + RECIPE_X + RECIPE_W - 9;
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
                        enough ? TechTheme.TEXT : TechTheme.WARN, false);
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
                y + LIST_Y + 40, TechTheme.TEXT, false);

        int progress = menu.getProgress();
        int max = menu.getMaxProgress();
        int width = max <= 0 ? 0 : Math.min(OUT_W - 6, progress * (OUT_W - 6) / max);
        int barY = y + LIST_Y + 60;
        graphics.fill(x + OUT_X + 3, barY, x + OUT_X + OUT_W - 3, barY + 6, 0xFF8B8B8B);
        if (width > 0) {
            graphics.fill(x + OUT_X + 3, barY, x + OUT_X + 3 + width, barY + 6, 0xFF6FBF4A);
        }
    }

    /** Thin draggable bar on the right edge of a list pane; hidden when everything fits. */
    private void bar(GuiGraphics graphics, int x, int y, int total, int visible, int scroll, boolean active) {
        if (total <= visible) {
            return;
        }
        int trackX = x;
        int trackY = y;
        graphics.fill(trackX, trackY, trackX + SCROLL_W, trackY + LIST_H, 0xFF0B1016);
        int thumbH = thumbHeight(LIST_H, total, visible);
        int thumbY = trackY + (LIST_H - thumbH) * scroll / Math.max(1, total - visible);
        graphics.fill(trackX, thumbY, trackX + SCROLL_W, thumbY + thumbH,
                active ? TechTheme.ACCENT : 0xFF3C536B);
    }

    private void renderScrollbars(GuiGraphics graphics, int x, int y) {
        int groups = menu.groups().size();
        bar(graphics, x + AMMO_BAR_X, y + LIST_Y, groups, AMMO_VISIBLE, ammoScroll, draggingAmmoBar);
        AssemblerRecipes.Group group = menu.selectedGroup();
        int recipes = group == null ? 0 : group.recipes().size();
        bar(graphics, x + RECIPE_BAR_X, y + LIST_Y, recipes, VISIBLE_RECIPES, recipeScroll, draggingRecipeBar);
    }

    private static int thumbHeight(int track, int total, int visible) {
        return Math.max(12, track * visible / Math.max(1, total));
    }

    private static int scrollFromMouse(double mouseY, double trackTop, int total, int visible) {
        int max = Math.max(0, total - visible);
        if (max == 0) {
            return 0;
        }
        int thumb = thumbHeight(LIST_H, total, visible);
        double travel = Math.max(1, LIST_H - thumb);
        double ratio = (mouseY - trackTop - thumb / 2.0) / travel;
        return (int) Math.round(Math.max(0, Math.min(1, ratio)) * max);
    }

    private static void pane(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, TechTheme.PANEL2);
        graphics.fill(x, y, x + w, y + 1, TechTheme.BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, TechTheme.BORDER);
    }

    private static void divider(GuiGraphics graphics, int x, int top, int bottom) {
        graphics.fill(x, top, x + 2, bottom, TechTheme.BORDER);
    }

    private int countAvailable(Ingredient ingredient) {
        return menu.countAvailable(ingredient);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 5, TechTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.ammo"),
                AMMO_X, 18, TechTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.recipes"),
                RECIPE_X, 18, TechTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.ammo_assembler.output"),
                OUT_X, 18, TechTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.taczammobox.sides"), 244, 10, TechTheme.TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int localX = (int) mouseX - leftPos;
            int localY = (int) mouseY - topPos;
            if (localY >= LIST_Y && localY < LIST_Y + LIST_H) {
                if (localX >= AMMO_BAR_X && localX < AMMO_BAR_X + SCROLL_W
                        && menu.groups().size() > AMMO_VISIBLE) {
                    draggingAmmoBar = true;
                    ammoScroll = scrollFromMouse(mouseY, topPos + LIST_Y, menu.groups().size(), AMMO_VISIBLE);
                    return true;
                }
                AssemblerRecipes.Group group = menu.selectedGroup();
                int recipeCount = group == null ? 0 : group.recipes().size();
                if (localX >= RECIPE_BAR_X && localX < RECIPE_BAR_X + SCROLL_W && recipeCount > VISIBLE_RECIPES) {
                    draggingRecipeBar = true;
                    recipeScroll = scrollFromMouse(mouseY, topPos + LIST_Y, recipeCount, VISIBLE_RECIPES);
                    return true;
                }
            }
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingAmmoBar) {
            ammoScroll = scrollFromMouse(mouseY, topPos + LIST_Y, menu.groups().size(), AMMO_VISIBLE);
            return true;
        }
        if (draggingRecipeBar) {
            AssemblerRecipes.Group group = menu.selectedGroup();
            int recipeCount = group == null ? 0 : group.recipes().size();
            recipeScroll = scrollFromMouse(mouseY, topPos + LIST_Y, recipeCount, VISIBLE_RECIPES);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingAmmoBar = false;
        draggingRecipeBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (localY >= LIST_Y && localY < LIST_Y + LIST_H) {
            if (localX >= AMMO_X && localX < AMMO_X + AMMO_W) {
                int visible = AMMO_VISIBLE;
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
