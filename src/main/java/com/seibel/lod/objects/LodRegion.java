package com.seibel.lod.objects;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.objects.LevelPos.LevelPos;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LodUtil;

/**
 * STANDARD TO FOLLOW
 * every coordinate called posX or posZ is a relative coordinate and not and absolute coordinate
 * if an array contain coordinate the order is the following
 * 0 for x, 1 for z in 2D
 * 0 for x, 1 for y, 2 for z in 3D
 */

public class LodRegion
{
	//x coord,
	private byte minDetailLevel;
	private static final byte POSSIBLE_LOD = 10;
	private int numberOfPoints;
	private DistanceGenerationMode generationMode;
	//For each of the following field the first slot is for the level of detail
	//Important: byte have a [-128, 127] range. When converting from or to int a 128 should be added or removed
	//If there is a bug with color then it's probably caused by this.
	//in the future other fields like transparency and light level could be added
	private byte[][][][] colors;

	private short[][][] height;

	private short[][][] depth;

	private boolean[][][] dataExistence;


	public final int regionPosX;
	public final int regionPosZ;

	public LodRegion(LevelContainer levelContainer, RegionPos regionPos, DistanceGenerationMode generationMode)
	{
		this.generationMode = generationMode;
		this.regionPosX = regionPos.x;
		this.regionPosZ = regionPos.z;
		this.minDetailLevel = levelContainer.detailLevel;

		//Array of matrices of arrays
		colors = new byte[POSSIBLE_LOD][][][];

		//Arrays of matrices
		height = new short[POSSIBLE_LOD][][];
		depth = new short[POSSIBLE_LOD][][];
		dataExistence = new boolean[POSSIBLE_LOD][][];

		colors[minDetailLevel] = levelContainer.colors;
		height[minDetailLevel] = levelContainer.height;
		depth[minDetailLevel] = levelContainer.depth;
		dataExistence[minDetailLevel] = levelContainer.dataExistence;

		//Initialize all the different matrices
		for (byte lod = (byte) (minDetailLevel + 1); lod <= LodUtil.REGION_DETAIL_LEVEL; lod++)
		{
			int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
			colors[lod] = new byte[size][size][3];
			height[lod] = new short[size][size];
			depth[lod] = new short[size][size];
			dataExistence[lod] = new boolean[size][size];
		}
		updateArea(LevelPosUtil.createLevelPos(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ));
	}

	public LodRegion(byte minDetailLevel, RegionPos regionPos, DistanceGenerationMode generationMode)
	{
		this.generationMode = generationMode;
		this.minDetailLevel = minDetailLevel;
		this.regionPosX = regionPos.x;
		this.regionPosZ = regionPos.z;

		//Array of matrices of arrays
		colors = new byte[POSSIBLE_LOD][][][];

		//Arrays of matrices
		height = new short[POSSIBLE_LOD][][];
		depth = new short[POSSIBLE_LOD][][];
		dataExistence = new boolean[POSSIBLE_LOD][][];


		//Initialize all the different matrices
		for (byte lod = minDetailLevel; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++)
		{
			int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - lod);
			colors[lod] = new byte[size][size][3];
			height[lod] = new short[size][size];
			depth[lod] = new short[size][size];
			dataExistence[lod] = new boolean[size][size];

		}
	}

	/**
	 * This method can be used to insert data into the LodRegion
	 *
	 * @param levelPos
	 * @param dataPoint
	 * @return
	 */
	public boolean addData(int[] levelPos, short[] dataPoint, boolean serverQuality)
	{
		//System.out.println(levelPos[0] + " " + levelPos[1] + " " + levelPos[2]);
		levelPos = LevelPosUtil.getRegionModule(levelPos);
		byte detailLevel = LevelPosUtil.getDetailLevel(levelPos);
		int posX = LevelPosUtil.getPosX(levelPos);
		int posZ = LevelPosUtil.getPosZ(levelPos);
		if (!doesDataExist(levelPos) || serverQuality)
		{

			//update the number of node present
			if (this.dataExistence[detailLevel][posX][posZ]) numberOfPoints++;

			//add the node data
			this.height[detailLevel][posX][posZ] = DataPoint.getHeight(dataPoint);
			this.depth[detailLevel][posX][posZ] = DataPoint.getDepth(dataPoint);
			this.colors[detailLevel][posX][posZ][0] = (byte) (DataPoint.getRed(dataPoint) - 128);
			this.colors[detailLevel][posX][posZ][1] = (byte) (DataPoint.getGreen(dataPoint) - 128);
			this.colors[detailLevel][posX][posZ][2] = (byte) (DataPoint.getBlue(dataPoint) - 128);
			this.dataExistence[detailLevel][posX][posZ] = true;
			return true;
		} else
		{
			return false;
		}
	}

	/**
	 * This method will return the data in the position relative to the level of detail
	 *
	 * @param levelPos
	 * @return the data at the relative pos and level
	 */
	public short[] getData(int[] levelPos)
	{
		levelPos = LevelPosUtil.getRegionModule(levelPos);
		byte detailLevel = LevelPosUtil.getDetailLevel(levelPos);
		int posX = LevelPosUtil.getPosX(levelPos);
		int posZ = LevelPosUtil.getPosZ(levelPos);
		return new short[]{height[detailLevel][posX][posZ],
				depth[detailLevel][posX][posZ],
				(short) (colors[detailLevel][posX][posZ][0] + 128),
				(short) (colors[detailLevel][posX][posZ][1] + 128),
				(short) (colors[detailLevel][posX][posZ][2] + 128)
		};
	}

	/**
	 * This method will return all the levelPos that are renderable according to the requisite given in input
	 *
	 * @return
	 */
	public void getDataToGenerate(ConcurrentMap<LevelPos, MutableBoolean> dataToGenerate, int playerPosX, int playerPosZ)
	{
		LevelPos levelPos = new LevelPos(LodUtil.REGION_DETAIL_LEVEL, 0, 0);
		getDataToGenerate(dataToGenerate, levelPos, playerPosX, playerPosZ);

	}

	private void getDataToGenerate(ConcurrentMap<LevelPos, MutableBoolean> dataToGenerate, LevelPos levelPos, int playerPosX, int playerPosZ)
	{
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - levelPos.detailLevel);

		//here i calculate the the LevelPos is in range
		//This is important to avoid any kind of hole in the generation
		int minDistance = levelPos.minDistance(playerPosX, playerPosZ, regionPosX, regionPosZ);


		int posX = levelPos.posX;
		int posZ = levelPos.posZ;
		byte detailLevel = levelPos.detailLevel;
		byte childDetailLevel = (byte) (detailLevel - 1);
		int childPosX = posX * 2;
		int childPosZ = posZ * 2;

		int childSize = 1 << (LodUtil.REGION_DETAIL_LEVEL - childDetailLevel);
		//we have reached the target detail level

		if (DetailDistanceUtil.getDistanceGenerationInverse(minDistance) > detailLevel)
		{
			return;
		} else if (DetailDistanceUtil.getDistanceGenerationInverse(minDistance) == detailLevel)
		{
			if (!doesDataExist(levelPos))
			{
				levelPos.changeParameters(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size);
				if (dataToGenerate.containsKey(levelPos))
				{
					dataToGenerate.get(levelPos).setTrue();
				} else
				{
					dataToGenerate.put(levelPos.clone(), new MutableBoolean(true));
				}
			}
		} else
		{
			//we want max a request per chunk. So for lod smaller than chunk we explore only the top rigth child
			if (detailLevel > LodUtil.CHUNK_DETAIL_LEVEL)
			{
				int num = 0;
				//We take all the children that are not generated to at least the generation level taken in input
				for (int x = 0; x <= 1; x++)
				{
					for (int z = 0; z <= 1; z++)
					{
						levelPos.changeParameters((byte) (detailLevel - 1), childPosX + x, childPosZ + z);

						if (!doesDataExist(levelPos))
						{
							num++;
							levelPos.changeParameters((byte) (detailLevel - 1), childPosX + x + regionPosX * childSize, childPosZ + z + regionPosZ * childSize);
							if (dataToGenerate.containsKey(levelPos))
							{
								dataToGenerate.get(levelPos).setTrue();
							} else
							{
								dataToGenerate.put(levelPos.clone(), new MutableBoolean(true));
							}
						}
					}
				}

				//only if all the children are correctly generated we go deeper
				if (num == 0)
				{
					for (int x = 0; x <= 1; x++)
					{
						for (int z = 0; z <= 1; z++)
						{
							levelPos.changeParameters((byte) (detailLevel - 1), childPosX + x, childPosZ + z);
							getDataToGenerate(dataToGenerate, levelPos, playerPosX, playerPosZ);
						}
					}
				}
			} else
			//now we keep exploring the top right child
			{
				if (DetailDistanceUtil.getLodGenDetail(childDetailLevel).detailLevel <= (childDetailLevel))
				{
					levelPos.changeParameters(detailLevel, posX, posZ);
					levelPos.convert(childDetailLevel);
					if (!doesDataExist(levelPos))
					{
						levelPos.changeParameters(levelPos.detailLevel, levelPos.posX + regionPosX * childSize, levelPos.posZ + regionPosZ * childSize);
						if (dataToGenerate.containsKey(levelPos))
						{
							dataToGenerate.get(levelPos).setTrue();
						} else
						{
							dataToGenerate.put(levelPos.clone(), new MutableBoolean(true));
						}
					} else
					{
						getDataToGenerate(dataToGenerate, levelPos, playerPosX, playerPosZ);
					}
				}
			}
		}
	}


	/**
	 * @return
	 */
	public PosToRenderContainer getDataToRender(int playerPosX, int playerPosZ)
	{
		int[] levelPos = LevelPosUtil.createLevelPos(LodUtil.REGION_DETAIL_LEVEL, 0, 0);
		PosToRenderContainer posToRender = new PosToRenderContainer(minDetailLevel);
		getDataToRender(posToRender, levelPos, playerPosX, playerPosZ);
		return posToRender;
	}

	/**
	 * @return
	 */
	private void getDataToRender(PosToRenderContainer posToRender, int[] levelPos, int playerPosX, int playerPosZ)
	{

		byte detailLevel = LevelPosUtil.getDetailLevel(levelPos);
		int posX = LevelPosUtil.getPosX(levelPos);
		int posZ = LevelPosUtil.getPosZ(levelPos);
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		int[] tempLevelPos;

		//here i calculate the the LevelPos is in range
		//This is important to avoid any kind of hole in the rendering
		int maxDistance = LevelPosUtil.maxDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);

		byte supposedLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDistanceRenderingInverse(maxDistance));
		if (supposedLevel > detailLevel)
			return;
		else if (supposedLevel == detailLevel)
		{
			posToRender.addPosToRender(LevelPosUtil.createLevelPos(detailLevel,
					posX + regionPosX * size,
					posZ + regionPosZ * size));
		} else //case where (detailLevel > supposedLevel)
		{
			int childPosX = posX * 2;
			int childPosZ = posZ * 2;
			int childrenCount = 0;
			for (int x = 0; x <= 1; x++)
			{
				for (int z = 0; z <= 1; z++)
				{
					tempLevelPos = LevelPosUtil.createLevelPos((byte) (detailLevel - 1), childPosX + x, childPosZ + z);
					if (doesDataExist(tempLevelPos)) childrenCount++;
				}
			}

			//If all the four children exist we go deeper
			if (childrenCount == 4)
			{
				for (int x = 0; x <= 1; x++)
				{
					for (int z = 0; z <= 1; z++)
					{
						tempLevelPos = LevelPosUtil.createLevelPos((byte) (detailLevel - 1), childPosX + x, childPosZ + z);
						getDataToRender(posToRender, tempLevelPos, playerPosX, playerPosZ);
					}
				}
			} else
			{
				posToRender.addPosToRender(LevelPosUtil.createLevelPos(detailLevel,
						posX + regionPosX * size,
						posZ + regionPosZ * size));
			}
		}
	}


	/**
	 * @param levelPos
	 */
	public void updateArea(int[] levelPos)
	{
		int width;
		int startX;
		int startZ;
		byte detailLevel = LevelPosUtil.getDetailLevel(levelPos);
		int posX = LevelPosUtil.getPosX(levelPos);
		int posZ = LevelPosUtil.getPosZ(levelPos);
		int[] tempoLevelPos;
		for (byte bottom = (byte) (minDetailLevel + 1); bottom <= detailLevel; bottom++)
		{
			tempoLevelPos = LevelPosUtil.convert(levelPos, bottom);
			startX = LevelPosUtil.getPosX(tempoLevelPos);
			startZ = LevelPosUtil.getPosZ(tempoLevelPos);
			width = 1 << (detailLevel - bottom);
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					tempoLevelPos = LevelPosUtil.createLevelPos(bottom, startX + x, startZ + z);
					update(tempoLevelPos);
				}
			}
		}
		for (byte tempLod = (byte) (detailLevel + 1); tempLod <= LodUtil.REGION_DETAIL_LEVEL; tempLod++)
		{
			tempoLevelPos = LevelPosUtil.convert(levelPos, tempLod);
			update(tempoLevelPos);
		}
	}

	/**
	 * @param levelPos
	 */
	private void update(int[] levelPos)
	{

		levelPos = LevelPosUtil.getRegionModule(levelPos);
		int numberOfChildren = 0;
		int numberOfVoidChildren = 0;

		int tempRed = 0;
		int tempGreen = 0;
		int tempBlue = 0;
		int tempHeight = 0;
		int tempDepth = 0;
		int newPosX;
		int newPosZ;
		byte newDetailLevel;
		int detailLevel = LevelPosUtil.getDetailLevel(levelPos);
		int posX = LevelPosUtil.getPosX(levelPos);
		int posZ = LevelPosUtil.getPosZ(levelPos);
		int[] tempLevelPos;
		for (int x = 0; x <= 1; x++)
		{
			for (int z = 0; z <= 1; z++)
			{
				newPosX = 2 * posX + x;
				newPosZ = 2 * posZ + z;
				newDetailLevel = (byte) (detailLevel - 1);
				tempLevelPos = LevelPosUtil.createLevelPos(newDetailLevel, newPosX, newPosZ);
				if (doesDataExist(tempLevelPos))
				{
					if (height[newDetailLevel][newPosX][newPosZ] != LodBuilder.DEFAULT_HEIGHT
							    && depth[newDetailLevel][newPosX][newPosZ] != LodBuilder.DEFAULT_DEPTH)
					{
						numberOfChildren++;

						tempRed += colors[newDetailLevel][newPosX][newPosZ][0];
						tempGreen += colors[newDetailLevel][newPosX][newPosZ][1];
						tempBlue += colors[newDetailLevel][newPosX][newPosZ][2];
						tempHeight += height[newDetailLevel][newPosX][newPosZ];
						tempDepth += depth[newDetailLevel][newPosX][newPosZ];
					} else
					{
						// void children have the default height (most likely -1)
						// and represent a LOD with no blocks in it
						numberOfVoidChildren++;
					}
				}
			}
		}

		if (numberOfChildren > 0)
		{
			colors[detailLevel][posX][posZ][0] = (byte) (tempRed / numberOfChildren);
			colors[detailLevel][posX][posZ][1] = (byte) (tempGreen / numberOfChildren);
			colors[detailLevel][posX][posZ][2] = (byte) (tempBlue / numberOfChildren);
			height[detailLevel][posX][posZ] = (short) (tempHeight / numberOfChildren);
			depth[detailLevel][posX][posZ] = (short) (tempDepth / numberOfChildren);
			dataExistence[detailLevel][posX][posZ] = true;
		} else if (numberOfVoidChildren > 0)
		{
			colors[detailLevel][posX][posZ][0] = (byte) 0;
			colors[detailLevel][posX][posZ][1] = (byte) 0;
			colors[detailLevel][posX][posZ][2] = (byte) 0;

			height[detailLevel][posX][posZ] = LodBuilder.DEFAULT_HEIGHT;
			depth[detailLevel][posX][posZ] = LodBuilder.DEFAULT_DEPTH;

			dataExistence[detailLevel][posX][posZ] = true;
		}
	}

	/**
	 * @param levelPos
	 * @return
	 */
	public boolean doesDataExist(int[] levelPos)
	{
		try
		{
			levelPos = LevelPosUtil.getRegionModule(levelPos);
			return dataExistence[LevelPosUtil.getDetailLevel(levelPos)][LevelPosUtil.getPosX(levelPos)][LevelPosUtil.getPosZ(levelPos)];
		} catch (NullPointerException e)
		{
			return false;
		}
	}

	/**
	 * @param levelPos
	 * @return
	 */
	public boolean doesDataExist(LevelPos levelPos)
	{
		try
		{
			levelPos = levelPos.getRegionModuleLevelPos();
			return dataExistence[levelPos.detailLevel][levelPos.posX][levelPos.posZ];
		} catch (NullPointerException e)
		{
			return false;
		}
	}

	/**
	 * @return
	 */
	public DistanceGenerationMode getGenerationMode()
	{
		return generationMode;
	}

	public byte getMinDetailLevel()
	{
		return minDetailLevel;
	}

	/**
	 * This will be used to save a level
	 *
	 * @param detailLevel
	 * @return
	 */
	public LevelContainer getLevel(byte detailLevel)
	{
		if (detailLevel < minDetailLevel)
		{
			throw new IllegalArgumentException("getLevel asked for a level that does not exist: minimum " + minDetailLevel + " level requested " + detailLevel);
		}
		return new LevelContainer(detailLevel, colors[detailLevel], height[detailLevel], depth[detailLevel], dataExistence[detailLevel]);
	}

	/**
	 * @param levelContainer
	 */
	public void addLevel(LevelContainer levelContainer)
	{
		if (levelContainer.detailLevel < minDetailLevel - 1)
		{
			throw new IllegalArgumentException("addLevel requires a level that is at least the minimum level of the region -1 ");
		}
		if (levelContainer.detailLevel == minDetailLevel - 1) minDetailLevel = levelContainer.detailLevel;
		colors[levelContainer.detailLevel] = levelContainer.colors;
		height[levelContainer.detailLevel] = levelContainer.height;
		depth[levelContainer.detailLevel] = levelContainer.depth;
		dataExistence[levelContainer.detailLevel] = levelContainer.dataExistence;

	}

	/**
	 * @param detailLevel
	 */
	public void cutTree(byte detailLevel)
	{
		if (minDetailLevel < detailLevel)
		{
			for (byte tempLod = 0; tempLod < detailLevel; tempLod++)
			{
				colors[tempLod] = new byte[0][0][0];
				height[tempLod] = new short[0][0];
				depth[tempLod] = new short[0][0];
				dataExistence[tempLod] = new boolean[0][0];
			}
			minDetailLevel = detailLevel;
		}
	}

	/**
	 * @param detailLevel
	 */
	public void expand(byte detailLevel)
	{
		if (detailLevel < minDetailLevel)
		{
			for (byte tempLod = detailLevel; tempLod < minDetailLevel; tempLod++)
			{
				int size = (short) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod);
				colors[tempLod] = new byte[size][size][3];
				height[tempLod] = new short[size][size];
				depth[tempLod] = new short[size][size];
				dataExistence[tempLod] = new boolean[size][size];
			}
			minDetailLevel = detailLevel;
		}
	}

	/**
	 * return RegionPos of this lod region
	 */
	public RegionPos getRegionPos()
	{
		return new RegionPos(regionPosX, regionPosZ);
	}

	/**
	 * return needed memory in byte
	 */
	public int getMinMemoryNeeded()
	{
		int count = 0;
		for (byte tempLod = LodUtil.REGION_DETAIL_LEVEL; tempLod > minDetailLevel; tempLod--)
		{
			//i'm doing a upper limit of the minimum
			//Color should be just 3 byte but i'm gonna calculate as 12 byte
			//Height and depth should be just 4 byte but i'm gonna calculate as 8 byte
			count += Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod) * (8 + 3 + 2 + 2 + 1);
			//count += Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - tempLod) * (24 + 8 + 8 + 8 + 8);
		}
		return count;
	}

	@Override
	public String toString()
	{
		return getLevel(LodUtil.REGION_DETAIL_LEVEL).toString();
	}
}
