package com.agricraft.agricraft.common.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base block entity for water-holding irrigation components (tanks and channels).
 * <p>
 * Water flows between adjacent connected components like communicating vessels: each tick a component
 * pushes water towards horizontal neighbours whose water surface is lower than its own, until the
 * surfaces even out. Tanks additionally transfer water downwards to tanks below them (gravity).
 */
public abstract class IrrigationComponentBlockEntity extends BlockEntity {

	/** Fraction of the capacity that needs to change before the content is re-synced to clients. */
	private static final double SYNC_FRACTION = 0.05;

	private int content;
	private int lastSyncedContent = -1;

	public IrrigationComponentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** @return the water capacity of this component, in mB */
	public abstract int getCapacity();

	/** @return the height (in-block fraction, 0..1) of the bottom of the water body */
	public abstract double getMinFluidHeight();

	/** @return the height (in-block fraction, 0..1) of the top of the water body */
	public abstract double getMaxFluidHeight();

	/** @return whether this component can exchange water with the given neighbour through the given side */
	public abstract boolean canConnectTo(Direction side, IrrigationComponentBlockEntity other);

	public int getContent() {
		return this.content;
	}

	public void setContent(int content) {
		this.content = Math.max(0, Math.min(this.getCapacity(), content));
		this.setChanged();
		this.checkAndSync();
	}

	public boolean isFull() {
		return this.content >= this.getCapacity();
	}

	public boolean isEmpty() {
		return this.content <= 0;
	}

	/**
	 * @return the fill fraction of this component, between 0 and 1
	 */
	public double getFillFraction() {
		return this.getCapacity() <= 0 ? 0 : (double) this.content / this.getCapacity();
	}

	/**
	 * @return the absolute Y coordinate of this component's water surface
	 */
	public double getFluidSurfaceY() {
		return this.getBlockPos().getY() + this.getMinFluidHeight()
				+ this.getFillFraction() * (this.getMaxFluidHeight() - this.getMinFluidHeight());
	}

	/**
	 * @return the "surface area" of the water body: the volume held per unit of height
	 */
	public double getSurfaceFactor() {
		return this.getCapacity() / (this.getMaxFluidHeight() - this.getMinFluidHeight());
	}

	/**
	 * Attempts to add water to this component.
	 *
	 * @param max     the maximum amount of water to add, in mB
	 * @param execute false to only simulate
	 * @return the amount of water actually added
	 */
	public int pushWater(int max, boolean execute) {
		int amount = Math.min(max, this.getCapacity() - this.content);
		if (amount > 0 && execute) {
			this.setContent(this.content + amount);
		}
		return Math.max(0, amount);
	}

	/**
	 * Attempts to remove water from this component.
	 *
	 * @param max     the maximum amount of water to remove, in mB
	 * @param execute false to only simulate
	 * @return the amount of water actually removed
	 */
	public int drainWater(int max, boolean execute) {
		int amount = Math.min(max, this.content);
		if (amount > 0 && execute) {
			this.setContent(this.content - amount);
		}
		return Math.max(0, amount);
	}

	protected void serverTick() {
		if (this.content > 0) {
			this.balanceWithNeighbours();
		}
	}

	/**
	 * Evens out the water surface with connected horizontal neighbours (communicating vessels).
	 */
	protected void balanceWithNeighbours() {
		if (this.level == null) {
			return;
		}
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (this.content <= 0) {
				return;
			}
			BlockEntity be = this.level.getBlockEntity(this.getBlockPos().relative(dir));
			if (!(be instanceof IrrigationComponentBlockEntity other)) {
				continue;
			}
			if (!this.canConnectTo(dir, other) || !other.canConnectTo(dir.getOpposite(), this)) {
				continue;
			}
			double ownSurface = this.getFluidSurfaceY();
			double otherSurface = other.getFluidSurfaceY();
			if (ownSurface <= otherSurface) {
				continue;
			}
			double ownMin = this.getBlockPos().getY() + this.getMinFluidHeight();
			double ownFactor = this.getSurfaceFactor();
			double otherFactor = other.getSurfaceFactor();
			// volume of our water above the neighbour's surface (or above our own bottom)
			double transferable = (ownSurface - Math.max(otherSurface, ownMin)) * ownFactor;
			int delta = (int) Math.ceil(transferable * otherFactor / (ownFactor + otherFactor));
			int amount = Math.min(delta, Math.min(this.content, other.getCapacity() - other.getContent()));
			if (amount > 0) {
				other.pushWater(amount, true);
				this.drainWater(amount, true);
			}
		}
	}

	/** Syncs the content to clients when it changed by more than {@link #SYNC_FRACTION} of the capacity. */
	protected void checkAndSync() {
		if (this.level == null || this.level.isClientSide) {
			return;
		}
		int delta = Math.abs(this.content - this.lastSyncedContent);
		if (this.lastSyncedContent < 0 || delta >= this.getCapacity() * SYNC_FRACTION
				|| (this.content == 0 && this.lastSyncedContent != 0)
				|| (this.isFull() && this.lastSyncedContent != this.getCapacity())) {
			this.lastSyncedContent = this.content;
			this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.content = tag.getInt("content");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("content", this.content);
	}

	@NotNull
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

}
