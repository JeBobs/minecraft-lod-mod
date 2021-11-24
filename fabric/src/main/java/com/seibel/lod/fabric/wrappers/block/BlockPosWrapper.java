package com.seibel.lod.fabric.wrappers.block;

import java.util.Objects;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;

import net.minecraft.core.BlockPos;

public class BlockPosWrapper extends AbstractBlockPosWrapper
{
	private final BlockPos.MutableBlockPos blockPos;
	
	
	public BlockPosWrapper()
	{
		this.blockPos = new BlockPos.MutableBlockPos(0, 0, 0);
	}
	
	public BlockPosWrapper(int x, int y, int z)
	{
		this.blockPos = new BlockPos.MutableBlockPos(x, y, z);
	}
	
	public void set(int x, int y, int z)
	{
		blockPos.set(x, y, z);
	}
	
	public int getX()
	{
		return blockPos.getX();
	}
	
	public int getY()
	{
		return blockPos.getY();
	}
	
	public int getZ()
	{
		return blockPos.getZ();
	}
	
	public int get(LodDirection.Axis axis)
	{
		return axis.choose(getX(), getY(), getZ());
	}
	
	public BlockPos.MutableBlockPos getBlockPos()
	{
		return blockPos;
	}
	
	@Override
	public boolean equals(Object o)
	{
		return blockPos.equals(o);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(blockPos);
	}
	
	public BlockPosWrapper offset(int x, int y, int z)
	{
		blockPos.set(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
		return this;
	}
	
}
