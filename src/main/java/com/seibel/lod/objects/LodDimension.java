/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.seibel.lod.objects;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodDimensionFileHandler;
import com.seibel.lod.util.LodUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

import java.io.File;
import java.io.IOException;

/**
 * This object holds all loaded LOD regions
 * for a given dimension.
 * 
 * @author Leonardo Amato
 * @author James Seibel
 * @version 8-8-2021
 */
public class LodDimension
{

	public final DimensionType dimension;

	/** measured in regions */
	private volatile int width;
	/** measured in regions */
	private volatile int halfWidth;


	public volatile LodRegion regions[][];
	public volatile boolean isRegionDirty[][];

	private volatile RegionPos center;

	private LodDimensionFileHandler fileHandler;


	/**
	 * Creates the dimension centered at (0,0)
	 *
	 * @param newWidth in regions
	 */
	public LodDimension(DimensionType newDimension, LodWorld lodWorld, int newWidth)
	{
		dimension = newDimension;
		width = newWidth;
		halfWidth = (int)Math.floor(width / 2);
		
		if(newDimension != null && lodWorld != null)
		{
			try
			{
				Minecraft mc = Minecraft.getInstance();
				
				File saveDir;
				if (mc.hasSingleplayerServer())
				{
					// local world
					
					ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(newDimension);
					
					// provider needs a separate variable to prevent
					// the compiler from complaining
					ServerChunkProvider provider = serverWorld.getChunkSource();
					saveDir = new File(provider.dataStorage.dataFolder.getCanonicalFile().getPath() + File.separatorChar + "lod");
				}
				else
				{
					// connected to server
					
					saveDir = new File(mc.gameDirectory.getCanonicalFile().getPath() +
							File.separatorChar + "lod server data" + File.separatorChar + LodUtil.getDimensionIDFromWorld(mc.level));
				}
				
				fileHandler = new LodDimensionFileHandler(saveDir, this);
			}
			catch (IOException e)
			{
				// the file handler wasn't able to be created
				// we won't be able to read or write any files
			}
		}
		
		
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		
		// populate isRegionDirty
		for(int i = 0; i < width; i++)
			for(int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
		
		center = new RegionPos(0,0);
	}
	
	
	/**
	 * Move the center of this LodDimension and move all owned
	 * regions over by the given x and z offset. <br><br>
	 * 
	 * Synchronized to prevent multiple moves happening on top of each other.
	 */
	public synchronized void move(RegionPos regionOffset)
	{
		int xOffset = regionOffset.x;
		int zOffset = regionOffset.z;
		
		// if the x or z offset is equal to or greater than
		// the total size, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
		{
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					regions[x][z] = null;
				}
			}
			
			// update the new center
			center.x += xOffset;
			center.z += zOffset;
			
			return;
		}
		
		
		// X
		if(xOffset > 0)
		{
			// move everything over to the left (as the center moves to the right)
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					if(x + xOffset < width)
						regions[x][z] = regions[x + xOffset][z];
					else
						regions[x][z] = null;
				}
			}
		}
		else
		{
			// move everything over to the right (as the center moves to the left)
			for(int x = width - 1; x >= 0; x--)
			{
				for(int z = 0; z < width; z++)
				{
					if(x + xOffset >= 0)
						regions[x][z] = regions[x + xOffset][z];
					else
						regions[x][z] = null;
				}
			}
		}
		
		
		
		// Z
		if(zOffset > 0)
		{
			// move everything up (as the center moves down)
			for(int x = 0; x < width; x++)
			{
				for(int z = 0; z < width; z++)
				{
					if(z + zOffset < width)
						regions[x][z] = regions[x][z + zOffset];
					else
						regions[x][z] = null;
				}
			}
		}
		else
		{
			// move everything down (as the center moves up)
			for(int x = 0; x < width; x++)
			{
				for(int z = width - 1; z >= 0; z--)
				{
					if(z + zOffset >= 0)
						regions[x][z] = regions[x][z + zOffset];
					else
						regions[x][z] = null;
				}
			}
		}
		
		
		
		// update the new center
		center.x += xOffset;
		center.z += zOffset;
	}
	
	
	
	
	
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(RegionPos regionPos)
	{
		int xIndex = (regionPos.x - center.x) + halfWidth;
		int zIndex = (regionPos.z - center.z) + halfWidth;
		
		if (!regionIsInRange(regionPos.x, regionPos.z))
			// out of range
			return null;
		
		if (regions[xIndex][zIndex] == null)
		{

			regions[xIndex][zIndex] = getRegionFromFile(regionPos);
			if (regions[xIndex][zIndex] == null)
			{
				/**TODO the value is currently 0 but should be determinated by the distance of the player)*/
				regions[xIndex][zIndex] = new LodRegion((byte) 0,regionPos);
			}
		}

		return regions[xIndex][zIndex];
	}

	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 *
	 * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
	 */
	public void addOrOverwriteRegion(LodRegion newRegion) throws ArrayIndexOutOfBoundsException
	{
		int xIndex = (newRegion.regionPosX - center.x) + halfWidth;
		int zIndex = (center.z - newRegion.regionPosZ) + halfWidth;

		if (!regionIsInRange(newRegion.regionPosX, newRegion.regionPosZ))
			// out of range
			throw new ArrayIndexOutOfBoundsException();

		regions[xIndex][zIndex] = newRegion;
	}


	/**
	 *this method creates all null regions
	 */
	public void initializeNullRegions()
	{
		int regionX;
		int regionZ;
		RegionPos regionPos;
		LodRegion region;

		for(int x = 0; x < regions.length; x++)
		{
			for(int z = 0; z < regions.length; z++)
			{
				regionX = (x + center.x) - halfWidth;
				regionZ = (z + center.z) - halfWidth;
				regionPos = new RegionPos(regionX,regionZ);
				region = getRegion(regionPos);

				if (region == null)
				{
					// if no region exists, create it
					region = new LodRegion((byte) 0,regionPos);
					addOrOverwriteRegion(region);
				}
			}
		}
	}


	/**
	 * Add the given LOD to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinates it will be overwritten.
	 */
	public Boolean addNode(LevelPos levelPos, LodDataPoint lodDataPoint, DistanceGenerationMode generationMode, boolean update, boolean dontSave)
	{
		// don't continue if the region can't be saved
		RegionPos regionPos = levelPos.getRegionPos();
		if (!regionIsInRange(regionPos.x, regionPos.z))
		{
			return false;
		}

		LodRegion region = getRegion(regionPos);

		if (region == null)
		{
			// if no region exists, create it
			region = new LodRegion((byte) 0,regionPos);
			addOrOverwriteRegion(region);
		}
		boolean nodeAdded = region.setData(levelPos,lodDataPoint,(byte) generationMode.complexity,true);

		// only save valid LODs to disk
		if (!dontSave && fileHandler != null)
		{
			try
			{
				// mark the region as dirty so it will be saved to disk
				int xIndex = (regionPos.x - center.x) + halfWidth;
				int zIndex = (regionPos.z - center.z) + halfWidth;
				isRegionDirty[xIndex][zIndex] = true;
			}
			catch(ArrayIndexOutOfBoundsException e)
			{
				// This method was probably called when the dimension was changing size.
				// Hopefully this shouldn't be an issue.
			}
		}
		return nodeAdded;
	}


	/**
	 * Get the LodNodeData at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public LodDataPoint getLodFromCoordinates(ChunkPos chunkPos)
	{
		return getLodFromCoordinates(chunkPos, LodUtil.CHUNK_DETAIL_LEVEL);
	}

	/**
	 * Get the LodNodeData at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public LodDataPoint getLodFromCoordinates(ChunkPos chunkPos, int detailLevel)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

    	LodRegion region = getRegion(LodUtil.convertGenericPosToRegionPos(chunkPos.x, chunkPos.z, LodUtil.CHUNK_DETAIL_LEVEL));

		if(region == null)
		{
			return null;
		}

		return region.getData(chunkPos);
	}

	/**
	 * Get the LodNodeData at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public LodDataPoint getLodFromCoordinates(LevelPos levelPos)
	{
		if (levelPos.detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + levelPos.detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

		LodRegion region = getRegion(levelPos.getRegionPos());

		if(region == null)
		{
			return null;
		}

		return region.getData(levelPos);
	}

	/**
	 * return true if and only if the node at that position exist
	 */
	public boolean hasThisPositionBeenGenerated(ChunkPos chunkPos)
	{
		LodRegion region = getRegion(LodUtil.convertGenericPosToRegionPos(chunkPos.x, chunkPos.z, LodUtil.CHUNK_DETAIL_LEVEL));

		if(region == null)
		{
			return false;
		}

		return region.doesNodeExist(new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z));
	}

	/**
	 * return true if and only if the node at that position exist
	 */

	public boolean hasThisPositionBeenGenerated(LevelPos levelPos)
	{
		LodRegion region = getRegion(levelPos.getRegionPos());

		if(region == null)
		{
			return false;
		}

		return region.doesNodeExist(levelPos);
	}

	/**
	 * Get the region at the given X and Z coordinates from the
	 * RegionFileHandler.
	 */
	public LodRegion getRegionFromFile(RegionPos regionPos)
	{
		if (fileHandler != null)
			return fileHandler.loadRegionFromFile(regionPos);
		else
			return null;
	}

	/**
	 * Save all dirty regions in this LodDimension to file.
	 */
	public void saveDirtyRegionsToFileAsync()
	{
		fileHandler.saveDirtyRegionsToFileAsync();
	}


	/**
	 * Returns whether the region at the given X and Z coordinates
	 * is within the loaded range.
	 */
	public boolean regionIsInRange(int regionX, int regionZ)
	{
		int xIndex = (regionX - center.x) + halfWidth;
		int zIndex = (regionZ - center.z) + halfWidth;

		return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
	}







	public int getCenterX()
	{
		return center.x;
	}

	public int getCenterZ()
	{
		return center.z;
	}


	/**
	 * TODO Double check that this method works as expected
	 *
	 * Returns how many non-null LodChunks
	 * are stored in this LodDimension.
	 */
	public int getNumberOfLods()
	{
		/**TODO **/
		int numbLods = 0;
		return numbLods;
	}


	public int getWidth()
	{
		if (regions != null)
		{
			// we want to get the length directly from the
			// source to make sure it is in sync with region
			// and isRegionDirty
			return regions.length;
		}
		else
		{
			return width;
		}
	}

	public void setRegionWidth(int newWidth)
	{
		width = newWidth;
		halfWidth = (int)Math.floor(width / 2);

		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];

		// populate isRegionDirty
		for(int i = 0; i < width; i++)
			for(int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
	}


	@Override
	public String toString()
	{
		String s = "";

		s += "dim: " + dimension.toString() + "\t";
		s += "(" + center.x + "," + center.z + ")";

		return s;
	}
}
