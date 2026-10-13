package com.github.mythicalmontie.mobmarket.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MobMarketBlock extends Block {
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {builder.add(HorizontalDirectionalBlock.FACING, FACE);}
    @Override
    public RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}

    // Set block highlight shapes by floor and wall
    private static final VoxelShape FLOOR_NORTH = Shapes.or(
            Block.box(7.0, 0.0, 3.0, 9.0, 15.0, 5.0),
            Block.box(1.0, 7.0, 5.0, 15.0, 14.0, 6.5)
    );

    private static final VoxelShape FLOOR_SOUTH = Shapes.or(
            Block.box(7.0, 0.0, 11.0, 9.0, 15.0, 13.0),
            Block.box(1.0, 7.0, 9.5, 15.0, 14.0, 11.0)
    );

    private static final VoxelShape FLOOR_EAST = Shapes.or(
            Block.box(11.0, 0.0, 7.0, 13.0, 15.0, 9.0),
            Block.box(9.5, 7.0, 1.0, 11.0, 14.0, 15.0)
    );

    private static final VoxelShape FLOOR_WEST = Shapes.or(
            Block.box(3.0, 0.0, 7.0, 5.0, 15.0, 9.0),
            Block.box(5.0, 7.0, 1.0, 6.5, 14.0, 15.0)
    );

    //Wall start here
    private static final VoxelShape WALL_NORTH = Shapes.or(
            Block.box(1.0, 7.0, 1.0, 15.0, 14.0, 2.5),
            Block.box(2.0, 12.0, 0.0, 14.0, 13.0, 1.0),
            Block.box(2.0, 8.0, 0.0, 14.0, 9.0, 1.0)
    );

    private static final VoxelShape WALL_SOUTH = Shapes.or(
            Block.box(1.0, 7.0, 13.5, 15.0, 14.0, 15.0),
            Block.box(2.0, 12.0, 15.0, 14.0, 13.0, 16.0),
            Block.box(2.0, 8.0, 15.0, 14.0, 9.0, 16.0)
    );

    private static final VoxelShape WALL_EAST = Shapes.or(
            Block.box(13.5, 7.0, 1.0, 15.0, 14.0, 15.0),
            Block.box(15.0, 12.0, 2.0, 16.0, 13.0, 14.0),
            Block.box(15.0, 8.0, 2.0, 16.0, 9.0, 14.0)
    );

    private static final VoxelShape WALL_WEST = Shapes.or(
            Block.box(1.0, 7.0, 1.0, 2.5, 14.0, 15.0),
            Block.box(0.0, 12.0, 2.0, 1.0, 13.0, 14.0),
            Block.box(0.0, 8.0, 2.0, 1.0, 9.0, 14.0)
    );

    public MobMarketBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AttachFace face = state.getValue(FACE);
        Direction direction = state.getValue(HorizontalDirectionalBlock.FACING);

        if (face == AttachFace.FLOOR) {
            return switch (direction) {
                case SOUTH -> FLOOR_SOUTH;
                case EAST -> FLOOR_EAST;
                case WEST -> FLOOR_WEST;
                default -> FLOOR_NORTH;
            };
        }
        return switch (direction) {
            case SOUTH -> WALL_SOUTH;
            case EAST -> WALL_EAST;
            case WEST -> WALL_WEST;
            default -> WALL_NORTH;
        };
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {return 1.0F;}

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {return true;}

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        AttachFace face = state.getValue(FACE);
        Direction direction = state.getValue(HorizontalDirectionalBlock.FACING);

        if (face == AttachFace.FLOOR) {
            return canSupportCenter(level, pos.below(), Direction.UP);
        } else if (face == AttachFace.WALL || face == AttachFace.CEILING) {
            BlockPos wallPos = pos.relative(direction);
            return level.getBlockState(wallPos).isFaceSturdy(level, wallPos, direction.getOpposite());
        }
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockState blockstate;

        if (clickedFace == Direction.DOWN) {
            return null;
        }

        if (clickedFace == Direction.UP) {
            blockstate = this.defaultBlockState()
                    .setValue(FACE, AttachFace.FLOOR)
                    .setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection());
        } else {
            blockstate = this.defaultBlockState()
                    .setValue(FACE, AttachFace.WALL)
                    .setValue(HorizontalDirectionalBlock.FACING, clickedFace.getOpposite());
        }

        if (blockstate.canSurvive(context.getLevel(), context.getClickedPos())) {
            return blockstate;
        }

        return null;
    }

}
