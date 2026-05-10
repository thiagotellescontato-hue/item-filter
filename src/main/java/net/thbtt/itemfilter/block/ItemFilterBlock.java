package net.thbtt.itemfilter.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.thbtt.itemfilter.block.entity.ItemFilterBlockEntity;
import net.thbtt.itemfilter.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class ItemFilterBlock extends BlockWithEntity {
    public static final MapCodec<ItemFilterBlock> CODEC = createCodec(ItemFilterBlock::new);
    public static final DirectionProperty FACING = Properties.HOPPER_FACING;

    private static final VoxelShape TOP_SHAPE = Block.createCuboidShape(
            0.0, 10.0, 0.0,
            16.0, 16.0, 16.0
    );

    private static final VoxelShape MIDDLE_SHAPE = Block.createCuboidShape(
            4.0, 4.0, 4.0,
            12.0, 10.0, 12.0
    );

    private static final VoxelShape BODY_SHAPE = VoxelShapes.union(
            TOP_SHAPE,
            MIDDLE_SHAPE
    );

    private static final VoxelShape DOWN_SHAPE = VoxelShapes.union(
            BODY_SHAPE,
            Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 4.0, 10.0)
    );

    private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(
            BODY_SHAPE,
            Block.createCuboidShape(6.0, 4.0, 0.0, 10.0, 8.0, 4.0)
    );

    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.union(
            BODY_SHAPE,
            Block.createCuboidShape(6.0, 4.0, 12.0, 10.0, 8.0, 16.0)
    );

    private static final VoxelShape EAST_SHAPE = VoxelShapes.union(
            BODY_SHAPE,
            Block.createCuboidShape(12.0, 4.0, 6.0, 16.0, 8.0, 10.0)
    );

    private static final VoxelShape WEST_SHAPE = VoxelShapes.union(
            BODY_SHAPE,
            Block.createCuboidShape(0.0, 4.0, 6.0, 4.0, 8.0, 10.0)
    );

    public ItemFilterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.DOWN));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction direction = ctx.getSide().getOpposite();

        if (direction == Direction.UP) {
            direction = Direction.DOWN;
        }

        return this.getDefaultState().with(FACING, direction);
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemFilterBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    private VoxelShape getShapeForState(BlockState state) {
        Direction facing = state.get(FACING);

        return switch (facing) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case DOWN -> DOWN_SHAPE;
            default -> DOWN_SHAPE;
        };
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeForState(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShapeForState(state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);

            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            net.minecraft.block.entity.BlockEntityType<T> type
    ) {
        if (world.isClient()) {
            return null;
        }

        return validateTicker(type, ModBlockEntities.ITEM_FILTER_BLOCK_ENTITY, ItemFilterBlockEntity::tick);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof ItemFilterBlockEntity itemFilterBlockEntity) {
                ItemScatterer.spawn(world, pos, itemFilterBlockEntity);
                world.updateComparators(pos, this);
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }
}