package com.github.mythicalmontie.mobmarket.block;

import com.github.mythicalmontie.mobmarket.blockentity.MobMarketBlockEntity;
import com.github.mythicalmontie.mobmarket.util.TeamProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MobMarketBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
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

    @Nullable @Override
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

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MobMarketBlockEntity marketBE && marketBE.getOwnerUUID() == null) {
                marketBE.setOwner(player);
            }
        }
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MobMarketBlockEntity(pos, state); }
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof MobMarketBlockEntity marketBE) {
                if (lvl.isClientSide()) {
                    MobMarketBlockEntity.clientTick(lvl, pos, st, marketBE);
                } else {
                    MobMarketBlockEntity.serverTick(lvl, pos, st, marketBE);
                }
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MobMarketBlockEntity marketBE) {
            if (marketBE.getOwnerUUID() == null) {
                marketBE.setOwner(player);
                if (!level.isClientSide) {
                    player.sendSystemMessage(Component.translatable("message.mobmarket.claimed_ownership"));
                }
            }

            if (!marketBE.isPublic() && player instanceof ServerPlayer serverPlayer) {
                boolean canAccess = TeamProtectionHelper.canAccess(serverPlayer, marketBE.getOwnerUUID(), !marketBE.isPublic());
                if (!canAccess) {
                    if (!level.isClientSide) {
                        player.sendSystemMessage(Component.translatable("message.mobmarket.no_access", marketBE.getOwnerName()));
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }

            if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
                if (level.isClientSide) {
                    com.github.mythicalmontie.mobmarket.client.BoundarySelectionHandler.toggleSelectionMode(pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, marketBE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MobMarketBlockEntity marketBE) {
                ItemStackHandler inventory = marketBE.getInventory();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    var stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

}
