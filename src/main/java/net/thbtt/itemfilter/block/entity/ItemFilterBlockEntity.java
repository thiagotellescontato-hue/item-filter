package net.thbtt.itemfilter.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.thbtt.itemfilter.block.ItemFilterBlock;
import net.thbtt.itemfilter.screen.ItemFilterScreenHandler;
import net.minecraft.block.ChestBlock;

import java.util.List;

public class ItemFilterBlockEntity extends BlockEntity implements SidedInventory, NamedScreenHandlerFactory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(10, ItemStack.EMPTY);

    private static final int[] HOPPER_SLOTS = new int[]{5, 6, 7, 8, 9};

    private boolean filterEnabled = true;
    private int transferCooldown = 0;

    // -1 means "choose an initial slot based on block position"
    private int pullSlotCursor = -1;

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

        // First, try to output items already inside the Item Filter.
        if (blockEntity.tryMoveItemsOut(state)) {
            moved = true;
        }

        // Then, try to pull from the inventory above.
        if (blockEntity.tryPullItemsFromAbove()) {
            moved = true;

            // If it pulled an item, try to output it in the same tick.
            if (blockEntity.tryMoveItemsOut(state)) {
                moved = true;
            }
        }

        // Finally, try to pull dropped item entities above the filter.
        if (blockEntity.tryPullDroppedItemsAbove()) {
            moved = true;

            // If it pulled a dropped item, try to output it in the same tick.
            if (blockEntity.tryMoveItemsOut(state)) {
                moved = true;
            }
        }

        if (moved) {
            blockEntity.transferCooldown = 8;
            blockEntity.markDirty();
        }
    }

    private Inventory getInventoryAt(BlockPos targetPos) {
        if (world == null) {
            return null;
        }

        BlockState targetState = world.getBlockState(targetPos);

        if (targetState.getBlock() instanceof ChestBlock chestBlock) {
            Inventory chestInventory = ChestBlock.getInventory(chestBlock, targetState, world, targetPos, true);

            if (chestInventory != null) {
                return chestInventory;
            }
        }

        if (targetState.getBlock() instanceof InventoryProvider inventoryProvider) {
            Inventory inventory = inventoryProvider.getInventory(targetState, world, targetPos);

            if (inventory != null) {
                return inventory;
            }
        }

        BlockEntity blockEntity = world.getBlockEntity(targetPos);

        if (blockEntity instanceof Inventory inventory) {
            return inventory;
        }

        return null;
    }

    private boolean tryMoveItemsOut(BlockState state) {
        if (world == null) {
            return false;
        }

        Direction outputDirection = state.get(ItemFilterBlock.FACING);
        Inventory targetInventory = getInventoryAt(pos.offset(outputDirection));

        if (targetInventory == null) {
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

        Inventory sourceInventory = getInventoryAt(pos.up());

        if (sourceInventory == null) {
            return false;
        }

        Direction extractSide = Direction.DOWN;

        if (sourceInventory instanceof SidedInventory sidedInventory) {
            int[] availableSlots = sidedInventory.getAvailableSlots(extractSide);
            return tryPullFromSlotList(sourceInventory, availableSlots, extractSide);
        }

        return tryPullFromRegularInventory(sourceInventory, extractSide);
    }

    private boolean tryPullFromRegularInventory(Inventory sourceInventory, Direction side) {
        int size = sourceInventory.size();

        if (size <= 0) {
            return false;
        }

        int startSlot = getStartSlot(size);

        for (int i = 0; i < size; i++) {
            int slot = (startSlot + i) % size;

            if (tryPullFromSlot(sourceInventory, slot, side)) {
                pullSlotCursor = (slot + 1) % size;
                markDirty();
                return true;
            }
        }

        return false;
    }

    private boolean tryPullFromSlotList(Inventory sourceInventory, int[] slots, Direction side) {
        if (slots.length == 0) {
            return false;
        }

        int startIndex = getStartSlot(slots.length);

        for (int i = 0; i < slots.length; i++) {
            int slotIndex = (startIndex + i) % slots.length;
            int slot = slots[slotIndex];

            if (tryPullFromSlot(sourceInventory, slot, side)) {
                pullSlotCursor = (slotIndex + 1) % slots.length;
                markDirty();
                return true;
            }
        }

        return false;
    }

    private int getStartSlot(int size) {
        if (size <= 0) {
            return 0;
        }

        if (pullSlotCursor >= 0) {
            return Math.floorMod(pullSlotCursor, size);
        }

        // Different Item Filters below the same double chest will start searching
        // from different slots instead of all starting from slot 0.
        int positionOffset = pos.getX() * 31 + pos.getY() * 17 + pos.getZ();
        return Math.floorMod(positionOffset, size);
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

    private boolean tryPullDroppedItemsAbove() {
        if (world == null) {
            return false;
        }

        Box box = new Box(
                pos.getX(), pos.getY() + 1.0, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0
        );

        List<ItemEntity> itemEntities = world.getEntitiesByClass(
                ItemEntity.class,
                box,
                itemEntity -> itemEntity.isAlive() && !itemEntity.getStack().isEmpty()
        );

        for (ItemEntity itemEntity : itemEntities) {
            if (tryPullFromItemEntity(itemEntity)) {
                return true;
            }
        }

        return false;
    }

    private boolean tryPullFromItemEntity(ItemEntity itemEntity) {
        ItemStack entityStack = itemEntity.getStack();

        if (entityStack.isEmpty()) {
            return false;
        }

        if (!canAcceptItem(entityStack)) {
            return false;
        }

        ItemStack stackToMove = entityStack.copy();
        stackToMove.setCount(1);

        ItemStack remainingStack = insertIntoInventory(this, stackToMove, Direction.UP);

        if (!remainingStack.isEmpty()) {
            return false;
        }

        entityStack.decrement(1);

        if (entityStack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setStack(entityStack);
        }

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
        nbt.putInt("PullSlotCursor", pullSlotCursor);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        Inventories.readNbt(nbt, inventory, registryLookup);

        if (nbt.contains("FilterEnabled")) {
            filterEnabled = nbt.getBoolean("FilterEnabled");
        }

        pullSlotCursor = nbt.contains("PullSlotCursor") ? nbt.getInt("PullSlotCursor") : -1;
    }
}