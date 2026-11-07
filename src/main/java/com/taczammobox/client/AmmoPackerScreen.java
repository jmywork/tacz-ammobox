package com.taczammobox.client;

import com.taczammobox.block.AmmoPackerBlockEntity;
import com.taczammobox.menu.AmmoPackerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import com.taczammobox.client.ui.TechButton;
import com.taczammobox.client.ui.TechTheme;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;

public class AmmoPackerScreen extends AbstractContainerScreen<AmmoPackerMenu> {
    private static final String[] FACE_KEYS = {"up", "down", "left", "right", "front", "back"};

    private Button modeButton;
    private final Button[] sideButtons = new Button[6];
    private final Direction[] shownDirections = new Direction[6];
    private Button redstoneButton;

    public AmmoPackerScreen(AmmoPackerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 280;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.modeButton = this.addRenderableWidget(new TechButton(this.leftPos + 8, this.topPos + 40, 40, 16,
                Component.empty(), button -> click(0)));

        Direction front = this.menu.getFacing();
        this.shownDirections[0] = Direction.UP;
        this.shownDirections[1] = Direction.DOWN;
        this.shownDirections[2] = front.getClockWise();
        this.shownDirections[3] = front.getCounterClockWise();
        this.shownDirections[4] = front;
        this.shownDirections[5] = front.getOpposite();

        for (int i = 0; i < sideButtons.length; i++) {
            int id = 1 + this.shownDirections[i].ordinal();
            this.sideButtons[i] = this.addRenderableWidget(new TechButton(this.leftPos + 184,
                    this.topPos + 18 + i * 20, 88, 18, Component.empty(), button -> click(id)));
        }
        this.redstoneButton = this.addRenderableWidget(new TechButton(this.leftPos + 184,
                this.topPos + 140, 88, 18, Component.empty(), button -> click(7)));

        updateLabels();
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }

    private void updateLabels() {
        if (this.modeButton != null) {
            this.modeButton.setMessage(Component.translatable(
                    this.menu.getMode() == 0 ? "gui.taczammobox.pack" : "gui.taczammobox.unpack"));
        }
        for (int i = 0; i < this.sideButtons.length; i++) {
            if (this.sideButtons[i] == null) {
                continue;
            }
            AmmoPackerBlockEntity.SideMode mode = this.menu.getSide(this.shownDirections[i]);
            MutableComponent label = Component.translatable("gui.taczammobox.side." + FACE_KEYS[i]);
            label.append(" ").append(Component.translatable("gui.taczammobox.side." + mode.name().toLowerCase()));
            this.sideButtons[i].setMessage(label);
            ((TechButton) this.sideButtons[i]).setTextColor(sideColor(mode));
        }
        if (this.redstoneButton != null) {
            AmmoPackerBlockEntity.RedstoneMode mode = this.menu.getRedstoneMode();
            this.redstoneButton.setMessage(Component.translatable(
                    "gui.taczammobox.redstone." + mode.name().toLowerCase()));
            ((TechButton) this.redstoneButton).setTextColor(redstoneColor(mode));
        }
    }

    private static int sideColor(AmmoPackerBlockEntity.SideMode mode) {
        if (mode == AmmoPackerBlockEntity.SideMode.INPUT) {
            return TechTheme.ACCENT;
        }
        if (mode == AmmoPackerBlockEntity.SideMode.OUTPUT) {
            return TechTheme.ACCENT2;
        }
        return TechTheme.MUTED;
    }

    private static int redstoneColor(AmmoPackerBlockEntity.RedstoneMode mode) {
        if (mode == AmmoPackerBlockEntity.RedstoneMode.HIGH) {
            return TechTheme.WARN;
        }
        if (mode == AmmoPackerBlockEntity.RedstoneMode.LOW) {
            return TechTheme.ACCENT;
        }
        return TechTheme.MUTED;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, TechTheme.BG);
        graphics.fill(x, y, x + this.imageWidth, y + 1, TechTheme.BORDER);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, TechTheme.BORDER);
        graphics.fill(x, y, x + 1, y + this.imageHeight, TechTheme.BORDER);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, TechTheme.BORDER);

        drawSlot(graphics, x + 55, y + 34);
        drawSlot(graphics, x + 115, y + 34);
        drawSlot(graphics, x + 147, y + 34);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, y + 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 7 + col * 18, y + 141);
        }

        graphics.fill(x + 176, y, x + this.imageWidth, y + this.imageHeight, TechTheme.PANEL);
        graphics.fill(x + 176, y, x + 177, y + this.imageHeight, TechTheme.BORDER);

        graphics.fill(x + 80, y + 35, x + 104, y + 52, TechTheme.SLOT);
        int progress = this.menu.getProgress();
        int width = progress * 24 / AmmoPackerBlockEntity.ticksPerOperation();
        int barColor = this.menu.getMode() == 0 ? TechTheme.ACCENT2 : TechTheme.ACCENT;
        graphics.fill(x + 80, y + 35, x + 80 + width, y + 52, barColor);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, TechTheme.BORDER);
        graphics.fill(x, y, x + 16, y + 16, TechTheme.SLOT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TechTheme.TEXT, false);
        int modeColor = this.menu.getMode() == 0 ? TechTheme.ACCENT2 : TechTheme.ACCENT;
        graphics.drawString(this.font, Component.translatable(
                        this.menu.getMode() == 0 ? "gui.taczammobox.pack" : "gui.taczammobox.unpack"),
                8, 22, modeColor, false);
        graphics.drawString(this.font, Component.translatable("gui.taczammobox.sides"), 180, 6, TechTheme.TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.taczammobox.filter"), 140, 22, TechTheme.TEXT, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TechTheme.TEXT, false);
    }
}
