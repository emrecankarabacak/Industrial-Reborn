package com.maciej916.indreb.common.block.impl.cable;

import com.maciej916.indreb.common.block.VoxelBlock;
import com.maciej916.indreb.common.enums.EnumLang;
import com.maciej916.indreb.common.registries.ModCapabilities;
import com.maciej916.indreb.common.tier.CableTier;
import com.maciej916.indreb.common.util.BlockStateHelper;
import com.maciej916.indreb.common.util.CapabilityUtil;
import com.maciej916.indreb.common.util.Constants;
import com.maciej916.indreb.common.util.TextComponentUtil;
import com.maciej916.indreb.common.util.wrench.WrenchHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.List;

public class BlockCable extends VoxelBlock implements SimpleWaterloggedBlock {

    private final CableTier cableTier;

    public BlockCable(float apothem, CableTier tier) {
        super(tier.getProperties(), apothem);
        this.cableTier = tier;
        WrenchHelper.registerAction(this).add(WrenchHelper.energyNetworkInfo()).add(WrenchHelper.dropAction());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        pTooltip.add(TextComponentUtil.build(
                new TranslatableComponent(EnumLang.POWER_TIER.getTranslationKey()).withStyle(ChatFormatting.GRAY),
                new TranslatableComponent(cableTier.getEnergyTier().getLang().getTranslationKey()).withStyle(cableTier.getEnergyTier().getColor())
        ));

        pTooltip.add(TextComponentUtil.build(
                new TranslatableComponent(EnumLang.TRANSFER.getTranslationKey()).withStyle(ChatFormatting.GRAY),
                new TranslatableComponent(EnumLang.POWER_TICK.getTranslationKey(), TextComponentUtil.getFormattedEnergyUnit(cableTier.getEnergyTier().getBasicTransfer())).withStyle(cableTier.getEnergyTier().getColor())
        ));

       if (!cableTier.isInsulated()) {
            pTooltip.add(TextComponentUtil.build(
                    new TranslatableComponent(EnumLang.CABLE_UNISOLATED.getTranslationKey()).withStyle(ChatFormatting.RED)
            ));
       }
    }

    public CableTier getCableTier() {
        return cableTier;
    }

    @Override
    protected boolean canConnect(LevelAccessor world, BlockPos pos, Direction direction) {
        BlockEntity be = world.getBlockEntity(pos);
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof BlockCable bc) {
            return bc.getCableTier().getEnergyTier() == cableTier.getEnergyTier();
        }

        return CapabilityUtil.getCapabilityHelper(be, ModCapabilities.ENERGY, direction).getIfPresentElse(e -> e.canExtractEnergy(direction.getOpposite()) || e.canReceiveEnergy(direction.getOpposite()), false);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (pLevel.isClientSide()) return;
        CapabilityUtil.getCapabilityHelper(pLevel, ModCapabilities.ENERGY_CORE).ifPresent(e -> e.getNetworks().onPlaced(pPos, pState, cableTier.getEnergyTier().getBasicTransfer()));
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pLevel.isClientSide()) return;
        CapabilityUtil.getCapabilityHelper(pLevel, ModCapabilities.ENERGY_CORE).ifPresent(e -> e.getNetworks().onRemove(pPos));
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }


    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (pLevel.isClientSide()) return;
        CapabilityUtil.getCapabilityHelper(pLevel, ModCapabilities.ENERGY_CORE).ifPresent(e -> e.getNetworks().neighborChanged(pPos, pFromPos));
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
    }


    @Override
    @Deprecated
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateHelper.waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    @Deprecated
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        if (state.getValue(BlockStateHelper.waterlogged)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        for (Direction direction : Constants.DIRECTIONS) {
            boolean valid = canConnect(level, pos.relative(direction), direction);
            state = state.setValue(FACING_TO_PROPERTY_MAP.get(direction), valid);
        }

        return state;
    }
}
