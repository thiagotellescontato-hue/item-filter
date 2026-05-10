package net.thbtt.itemfilter.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.thbtt.itemfilter.block.ItemFilterBlock;
import net.thbtt.itemfilter.screen.ItemFilterScreenHandler;

public class ItemFilterBlockEntity extends BlockEntity implements SidedInventory, NamedScreenHandlerFactory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(10, ItemStack.EMPTY);

    private static final int[] HOPPER_SLOTS = new int[]{5, 6, 7, 8, 9};

    private boolean filterEnabled = true;
    private int transferCooldown = 0;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return filterEnabled ? 1 : 0;
            }

            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                filterEnabled = value != 0;
                markDirty();
            }
        }

        @Override
        public int size() {
            return 1;
        }
    };

    public ItemFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_FILTER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.itemfilter.item_filter");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ItemFilterScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state, ItemFilterBlockEntity blockEntity) {
        if (world.isClient()) {
            return;
        }

        if (blockEntity.transferCooldown > 0) {
            blockEntity.transferCooldown--;
            return;
        }

        boolean moved = false;

        // Primeiro tenta mandar itens que já estão dentro do filtro.
        if (blockEntity.tryMoveItemsOut(state)) {
            moved = true;
        }

        // Depois tenta puxar do inventário acima.
        if (blockEntity.tryPullItemsFromAbove()) {
            moved = true;

            // Se puxou um item, tenta mandar para a saída no mesmo tick.
            if (blockEntity.tryMoveItemsOut(state)) {
                moved = true;
            }
        }

        if (moved) {
            blockEntity.transferCooldown = 8;
            blockEntity.markDirty();
        }
    }

    private boolean tryMoveItemsOut(BlockState state) {
        if (world == null) {
            return false;
        }

        Direction outputDirection = state.get(ItemFilterBlock.FACING);
        BlockEntity targetBlockEntity = world.getBlockEntity(pos.offset(outputDirection));

        if (!(targetBlockEntity instanceof Inventory targetInventory)) {
            return false;
        }

        for (int slot = 5; slot <= 9; slot++) {
            ItemStack stack = inventory.get(slot);

            if (stack.isEmpty()) {
                continue;
            }

            ItemStack originalStack = stack.copy();
            ItemStack remainingStack = insertIntoInventory(targetInventory, stack, outputDirection.getOpposite());

            inventory.set(slot, remainingStack);

            if (remainingStack.getCount() != originalStack.getCount()) {
                targetInventory.markDirty();
                markDirty();
                return true;
            }
        }

        return false;
    }

    private boolean tryPullItemsFromAbove() {
        if (world == null) {
            return false;
        }

        BlockEntity sourceBlockEntity = world.getBlockEntity(pos.up());

        if (!(sourceBlockEntity instanceof Inventory sourceInventory)) {
            return false;
        }

        Direction extractSide = Direction.DOWN;

        if (sourceInventory instanceof SidedInventory sidedInventory) {
            for (int slot : sidedInventory.getAvailableSlots(extractSide)) {
                if (tryPullFromSlot(sourceInventory, slot, extractSide)) {
                    return true;
                }
            }
        } else {
            for (int slot = 0; slot < sourceInventory.size(); slot++) {
                if (tryPullFromSlot(sourceInventory, slot, extractSide)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean tryPullFromSlot(Inventory sourceInventory, int slot, Direction side) {
        ItemStack sourceStack = sourceInventory.getStack(slot);

        if (sourceStack.isEmpty()) {
            return false;
        }

        if (sourceInventory instanceof SidedInventory sidedInventory) {
            if (!sidedInventory.canExtract(slot, sourceStack, side)) {
                return false;
            }
        }

        if (!canAcceptItem(sourceStack)) {
            return false;
        }

        ItemStack stackToMove = sourceStack.copy();
        stackToMove.setCount(1);

        ItemStack remainingStack = insertIntoInventory(this, stackToMove, Direction.UP);

        if (!remainingStack.isEmpty()) {
            return false;
        }

        sourceStack.decrement(1);

        if (sourceStack.isEmpty()) {
            sourceInventory.setStack(slot, ItemStack.EMPTY);
        }

        sourceInventory.markDirty();
        markDirty();

        return true;
    }

    private static ItemStack insertIntoInventory(Inventory targetInventory, ItemStack stack, Direction side) {
        ItemStack remainingStack = stack.copy();

        if (targetInventory instanceof SidedInventory sidedInventory) {
            for (int slot : sidedInventory.getAvailableSlots(side)) {
                remainingStack = insertIntoSlot(targetInventory, remainingStack, slot, side);

                if (remainingStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        } else {
            for (int slot = 0; slot < targetInventory.size(); slot++) {
                remainingStack = insertIntoSlot(targetInventory, remainingStack, slot, side);

                if (remainingStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return remainingStack;
    }

    private static ItemStack insertIntoSlot(Inventory targetInventory, ItemStack stack, int slot, Direction side) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!targetInventory.isValid(slot, stack)) {
            return stack;
        }

        if (targetInventory instanceof SidedInventory sidedInventory) {
            if (!sidedInventory.canInsert(slot, stack, side)) {
                return stack;
            }
        }

        ItemStack targetStack = targetInventory.getStack(slot);

        if (targetStack.isEmpty()) {
            int moveCount = Math.min(stack.getCount(), stack.getMaxCount());

            ItemStack movedStack = stack.copy();
            movedStack.setCount(moveCount);

            targetInventory.setStack(slot, movedStack);
            stack.decrement(moveCount);

            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }

        if (!ItemStack.areItemsAndComponentsEqual(targetStack, stack)) {
            return stack;
        }

        int maxCount = targetStack.getMaxCount();
        int space = maxCount - targetStack.getCount();

        if (space <= 0) {
            return stack;
        }

        int moveCount = Math.min(space, stack.getCount());

        targetStack.increment(moveCount);
        stack.decrement(moveCount);

        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    private boolean canAcceptItem(ItemStack stack) {
        if (!filterEnabled) {
            return true;
        }

        return isItemAllowedByFilter(stack);
    }

    private boolean isItemAllowedByFilter(ItemStack stack) {
        for (int slot = 0; slot <= 4; slot++) {
            ItemStack filterStack = inventory.get(slot);

            if (!filterStack.isEmpty() && stack.isOf(filterStack.getItem())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP || side == Direction.DOWN) {
            return HOPPER_SLOTS;
        }

        return new int[0];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return slot >= 5 && slot <= 9 && dir == Direction.UP && canAcceptItem(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot >= 5 && slot <= 9 && dir == Direction.DOWN;
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);

        if (!result.isEmpty()) {
            markDirty();
        }

        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(inventory, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);

        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }

        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        inventory.clear();
        markDirty();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot >= 0 && slot <= 4) {
            return true;
        }

        return slot >= 5 && slot <= 9 && canAcceptItem(stack);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putBoolean("FilterEnabled", filterEnabled);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        Inventories.readNbt(nbt, inventory, registryLookup);

        if (nbt.contains("FilterEnabled")) {
            filterEnabled = nbt.getBoolean("FilterEnabled");
        }
    }
}