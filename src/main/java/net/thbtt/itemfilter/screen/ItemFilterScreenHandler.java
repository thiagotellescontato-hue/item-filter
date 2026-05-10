package net.thbtt.itemfilter.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ItemFilterScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public ItemFilterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(10), new ArrayPropertyDelegate(1));
    }

    public ItemFilterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ITEM_FILTER_SCREEN_HANDLER, syncId);

        checkSize(inventory, 10);
        checkDataCount(propertyDelegate, 1);

        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        // Slots 0-4: filtro
        for (int i = 0; i < 5; i++) {
            this.addSlot(new FilterSlot(inventory, i, 44 + i * 18, 20));
        }

        // Slots 5-9: hopper/buffer
        for (int i = 0; i < 5; i++) {
            this.addSlot(new Slot(inventory, 5 + i, 44 + i * 18, 52));
        }

        // Inventário do player
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }

        // Hotbar
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    public boolean isFilterEnabled() {
        return this.propertyDelegate.get(0) == 1;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == 0) {
            this.propertyDelegate.set(0, this.isFilterEnabled() ? 0 : 1);
            return true;
        }

        return super.onButtonClick(player, id);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Slots do bloco: 0-9
            if (invSlot < 10) {
                if (!this.insertItem(originalStack, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Inventário do player -> slots hopper 5-9
                if (!this.insertItem(originalStack, 5, 10, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    private static class FilterSlot extends Slot {
        public FilterSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}