package net.thbtt.itemfilter.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class ItemFilterScreen extends HandledScreen<ItemFilterScreenHandler> {
    private ButtonWidget filterToggleButton;

    public ItemFilterScreen(ItemFilterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = 74;
    }

    @Override
    protected void init() {
        super.init();

        this.titleX = 8;
        this.titleY = 6;

        this.filterToggleButton = ButtonWidget.builder(getFilterButtonText(), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(this.x + 132, this.y + 2, 36, 14).build();

        this.addDrawableChild(this.filterToggleButton);
    }

    private Text getFilterButtonText() {
        return this.handler.isFilterEnabled()
                ? Text.translatable("screen.itemfilter.filter_on")
                : Text.translatable("screen.itemfilter.filter_off");
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFC6C6C6);
        context.fill(x, y, x + this.backgroundWidth, y + 1, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + this.backgroundHeight, 0xFFFFFFFF);
        context.fill(x, y + this.backgroundHeight - 1, x + this.backgroundWidth, y + this.backgroundHeight, 0xFF555555);
        context.fill(x + this.backgroundWidth - 1, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFF555555);

        drawSlotBackgrounds(context, x, y);
    }

    private void drawSlotBackgrounds(DrawContext context, int x, int y) {
        for (int i = 0; i < 5; i++) {
            drawSlotBackground(context, x + 44 + i * 18, y + 20);
        }

        for (int i = 0; i < 5; i++) {
            drawSlotBackground(context, x + 44 + i * 18, y + 52);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotBackground(context, x + 8 + column * 18, y + 84 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlotBackground(context, x + 8 + column * 18, y + 142);
        }
    }

    private void drawSlotBackground(DrawContext context, int x, int y) {
        context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF555555);
        context.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        context.fill(x + 1, y + 1, x + 17, y + 17, 0xFFE0E0E0);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);

        context.drawText(this.textRenderer, Text.translatable("screen.itemfilter.mode"), 100, 6, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("screen.itemfilter.filter"), 8, 25, 0x404040, false);
        context.drawText(this.textRenderer, Text.translatable("screen.itemfilter.items"), 8, 57, 0x404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, 8, this.playerInventoryTitleY, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.filterToggleButton != null) {
            this.filterToggleButton.setMessage(getFilterButtonText());
        }

        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}