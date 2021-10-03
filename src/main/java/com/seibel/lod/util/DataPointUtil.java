package com.seibel.lod.util;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.proxy.ClientProxy;
import net.minecraft.client.renderer.texture.NativeImage;

import java.util.Arrays;

public class DataPointUtil
{
	/*
	|a  |a  |a  |a  |r  |r  |r  |r   |

	|r  |r  |r  |r  |g  |g  |g  |g  |

	|g  |g  |g  |g  |b  |b  |b  |b  |

	|b  |b  |b  |b  |h  |h  |h  |h  |

	|h  |h  |h  |h  |h  |h  |d  |d  |

	|d  |d  |d  |d  |d  |d  |d  |d  |

	|bl |bl |bl |bl |sl |sl |sl |sl |

	|l  |l  |f  |g  |g  |g  |v  |e  |
	
	
	 */
	
	// Reminder: bytes have range of [-128, 127].
	// When converting to or from a int a 128 should be added or removed.
	// If there is a bug with color then it's probably caused by this.
	
	//To be used in the future for negative value
	//public final static int MIN_DEPTH = -64;
	//public final static int MIN_HEIGHT = -64;
	public final static int EMPTY_DATA = 0;
	public static int worldHeight = 256;
	
	public final static int ALPHA_DOWNSIZE_SHIFT = 4;
	
	//public final static int BLUE_COLOR_SHIFT = 0;
	//public final static int GREEN_COLOR_SHIFT = 8;
	//public final static int RED_COLOR_SHIFT = 16;
	//public final static int ALPHA_COLOR_SHIFT = 24;
	
	public final static int BLUE_SHIFT = 36;
	public final static int GREEN_SHIFT = BLUE_SHIFT + 8;
	public final static int RED_SHIFT = BLUE_SHIFT + 16;
	public final static int ALPHA_SHIFT = BLUE_SHIFT + 24;
	
	public final static int COLOR_SHIFT = 36;
	
	public final static int HEIGHT_SHIFT = 26;
	public final static int DEPTH_SHIFT = 16;
	public final static int BLOCK_LIGHT_SHIFT = 12;
	public final static int SKY_LIGHT_SHIFT = 8;
	//public final static int LIGHTS_SHIFT = SKY_LIGHT_SHIFT;
	public final static int VERTICAL_INDEX_SHIFT = 6;
	//public final static int FLAG_SHIFT = 5;
	public final static int GEN_TYPE_SHIFT = 2;
	public final static int VOID_SHIFT = 1;
	public final static int EXISTENCE_SHIFT = 0;
	
	public final static long ALPHA_MASK = 0b1111;
	public final static long RED_MASK = 0b1111_1111;
	public final static long GREEN_MASK = 0b1111_1111;
	public final static long BLUE_MASK = 0b1111_1111;
	public final static long COLOR_MASK = 0b11111111_11111111_11111111;
	public final static long HEIGHT_MASK = 0b11_1111_1111;
	public final static long DEPTH_MASK = 0b11_1111_1111;
	//public final static long LIGHTS_MASK = 0b1111_1111;
	public final static long BLOCK_LIGHT_MASK = 0b1111;
	public final static long SKY_LIGHT_MASK = 0b1111;
	public final static long VERTICAL_INDEX_MASK = 0b11;
	//public final static long FLAG_MASK = 0b1;
	public final static long GEN_TYPE_MASK = 0b111;
	public final static long VOID_MASK = 1;
	public final static long EXISTENCE_MASK = 1;
	
	
	public static long createVoidDataPoint(int generationMode)
	{
		long dataPoint = 0;
		dataPoint += (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		dataPoint += VOID_MASK << VOID_SHIFT;
		dataPoint += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}
	
	public static long createDataPoint(int height, int depth, int color, int lightSky, int lightBlock, int generationMode, int level)
	{
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, lightSky, lightBlock, generationMode, level);
	}
	
	public static long createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky, int lightBlock, int generationMode, int level)
	{
		long dataPoint = 0;
		dataPoint += (long) (alpha >>> ALPHA_DOWNSIZE_SHIFT) << ALPHA_SHIFT;
		dataPoint += (red & RED_MASK) << RED_SHIFT;
		dataPoint += (green & GREEN_MASK) << GREEN_SHIFT;
		dataPoint += (blue & BLUE_MASK) << BLUE_SHIFT;
		dataPoint += (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		dataPoint += (depth & DEPTH_MASK) << DEPTH_SHIFT;
		dataPoint += (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT;
		dataPoint += (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT;
		dataPoint += (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		dataPoint += (level & VERTICAL_INDEX_MASK) << VERTICAL_INDEX_SHIFT;
		dataPoint += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}
	
	public static short getHeight(long dataPoint)
	{
		return (short) ((dataPoint >>> HEIGHT_SHIFT) & HEIGHT_MASK);
	}
	
	public static short getDepth(long dataPoint)
	{
		
		return (short) ((dataPoint >>> DEPTH_SHIFT) & DEPTH_MASK);
	}
	
	public static short getAlpha(long dataPoint)
	{
		return (short) (((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK) << ALPHA_DOWNSIZE_SHIFT);
	}
	
	public static short getRed(long dataPoint)
	{
		return (short) ((dataPoint >>> RED_SHIFT) & RED_MASK);
	}
	
	public static short getGreen(long dataPoint)
	{
		return (short) ((dataPoint >>> GREEN_SHIFT) & GREEN_MASK);
	}
	
	public static short getBlue(long dataPoint)
	{
		return (short) ((dataPoint >>> BLUE_SHIFT) & BLUE_MASK);
	}
	
	public static int getLightSky(long dataPoint)
	{
		return (int) ((dataPoint >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public static int getLightBlock(long dataPoint)
	{
		return (int) ((dataPoint >>> BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
	}
	
	public static byte getGenerationMode(long dataPoint)
	{
		return (byte) ((dataPoint >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
	}
	
	
	public static boolean isVoid(long dataPoint)
	{
		return (((dataPoint >>> VOID_SHIFT) & VOID_MASK) == 1);
	}
	
	public static boolean doesItExist(long dataPoint)
	{
		return (((dataPoint >>> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1);
	}
	
	public static int getColor(long dataPoint)
	{
		return (int) (((dataPoint >>> COLOR_SHIFT) & COLOR_MASK) | (((dataPoint >>> (ALPHA_SHIFT - ALPHA_DOWNSIZE_SHIFT)) | 0b1111) << 24));
	}
	
	public static int getLightColor(long dataPoint, NativeImage lightMap)
	{
		int lightBlock = getLightBlock(dataPoint);
		int lightSky = getLightSky(dataPoint);
		int color = lightMap.getPixelRGBA(lightBlock, lightSky);
		int red = ColorUtil.getBlue(color);
		int green = ColorUtil.getGreen(color);
		int blue = ColorUtil.getRed(color);
		
		return ColorUtil.multiplyRGBcolors(getColor(dataPoint), ColorUtil.rgbToInt(red, green, blue));
	}

	public static byte getVerticalLevel(long dataPoint)
	{
		return (byte) ((dataPoint >> VERTICAL_INDEX_SHIFT) & VERTICAL_INDEX_MASK);
	}
	
	public static String toString(long dataPoint)
	{
		StringBuilder s = new StringBuilder();
		s.append(getHeight(dataPoint));
		s.append(" ");
		s.append(getDepth(dataPoint));
		s.append(" ");
		s.append(getAlpha(dataPoint));
		s.append(" ");
		s.append(getRed(dataPoint));
		s.append(" ");
		s.append(getBlue(dataPoint));
		s.append(" ");
		s.append(getGreen(dataPoint));
		s.append(" ");
		s.append(getLightBlock(dataPoint));
		s.append(" ");
		s.append(getLightSky(dataPoint));
		s.append(" ");
		s.append(getGenerationMode(dataPoint));
		s.append(" ");
		s.append(isVoid(dataPoint));
		s.append(" ");
		s.append(doesItExist(dataPoint));
		s.append('\n');
		return s.toString();
	}
	
	public static long mergeSingleData(long[] dataToMerge)
	{
		int numberOfChildren = 0;
		
		int tempAlpha = 0;
		int tempRed = 0;
		int tempGreen = 0;
		int tempBlue = 0;
		int tempHeight = Integer.MIN_VALUE;
		int tempDepth = Integer.MAX_VALUE;
		int tempLightBlock = 0;
		int tempLightSky = 0;
		int tempLevel = 3;
		byte tempGenMode = DistanceGenerationMode.SERVER.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		for (long data : dataToMerge)
		{
			if (DataPointUtil.doesItExist(data))
			{
				allEmpty = false;
				if (!(DataPointUtil.isVoid(data)))
				{
					numberOfChildren++;
					allVoid = false;
					tempAlpha += DataPointUtil.getAlpha(data);
					tempRed += DataPointUtil.getRed(data);
					tempGreen += DataPointUtil.getGreen(data);
					tempBlue += DataPointUtil.getBlue(data);
					tempHeight = Math.max(tempHeight, DataPointUtil.getHeight(data));
					tempDepth = Math.min(tempDepth, DataPointUtil.getDepth(data));
					tempLightBlock += DataPointUtil.getLightBlock(data);
					tempLightSky += DataPointUtil.getLightSky(data);
					if (tempLevel > DataPointUtil.getVerticalLevel(data)) tempLevel = DataPointUtil.getVerticalLevel(data);
				}
				tempGenMode = (byte) Math.min(tempGenMode, DataPointUtil.getGenerationMode(data));
			} else
			{
				tempGenMode = (byte) Math.min(tempGenMode, DistanceGenerationMode.NONE.complexity);
			}
		}
		
		if (allEmpty)
		{
			//no child has been initialized
			return DataPointUtil.EMPTY_DATA;
		} else if (allVoid)
		{
			//all the children are void
			return DataPointUtil.createVoidDataPoint(tempGenMode);
		} else
		{
			//we have at least 1 child
			tempAlpha = tempAlpha / numberOfChildren;
			tempRed = tempRed / numberOfChildren;
			tempGreen = tempGreen / numberOfChildren;
			tempBlue = tempBlue / numberOfChildren;
			tempLightBlock = tempLightBlock / numberOfChildren;
			tempLightSky = tempLightSky / numberOfChildren;
			return DataPointUtil.createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, tempHeight, tempDepth, tempLightSky, tempLightBlock, tempGenMode, tempLevel);
		}
	}
	
	public static long[] mergeMultiData(long[] dataToMerge, int inputVerticalData, int maxVerticalData)
	{
		int size = dataToMerge.length / inputVerticalData;
		int levelOffset = worldHeight / 16 + 1;

		// We initialize the arrays that are going to be used
		short[] projection = ThreadMapUtil.getProjectionArray(levelOffset * 4);
		short[] heightAndDepth = ThreadMapUtil.getHeightAndDepth((worldHeight + 1) * 3);
		long[] singleDataToMerge = ThreadMapUtil.getSingleAddDataToMerge(size);
		long[] dataPoint = ThreadMapUtil.getVerticalDataArray(worldHeight + 1);
		
		
		int genMode = DistanceGenerationMode.SERVER.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		long singleData;
		
		
		short depth;
		short height;
		
		//We collect the indexes of the data, ordered by the depth
		for (int index = 0; index < size; index++)
		{
			for (int dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
			{
				singleData = dataToMerge[index * inputVerticalData + dataIndex];
				if (doesItExist(singleData))
				{
					genMode = Math.min(genMode, getGenerationMode(singleData));
					allEmpty = false;
					if (!isVoid(singleData))
					{
						allVoid = false;
						depth = getDepth(singleData);
						height = getHeight(singleData);
						if (getHeight(singleData) > 255)
						{
							ClientProxy.LOGGER.info("height p1: " + getHeight(singleData));
						}
						for (int y = depth; y <= height; y++)
							projection[y / 16 + levelOffset * getVerticalLevel(singleData)] |= 1 << (y & 0xf);
					}
				}
			}
		}
		//We check if there is any data that's not empty or void
		if (allEmpty)
		{
			return dataPoint;
		}
		if (allVoid)
		{
			dataPoint[0] = createVoidDataPoint(genMode);
			return dataPoint;
		}
		//We extract the merged data
		int count = 0;
		int i = 0;
		int ii = 0;
		short level = 0;
		while (i < projection.length)
		{
			while (i < projection.length && projection[i] == 0 && i < levelOffset * (level + 1)) i++;
			if (i == projection.length)
				break; //we reached end of WORLD_HEIGHT and it's nothing more here
			if (i == levelOffset * (level + 1))
			{
				level++;
				continue;
			}
			while (ii < 15 && ((projection[i] >>> ii) & 1) == 0) ii++;
			if (ii >= 15 && ((projection[i] >>> ii) & 1) == 0) //there is nothing more in this chunk
			{
				ii = 0;
				i++;
				continue;
			}
			depth = (short) ((i - level * levelOffset) * 16 + ii);
			
			while (ii < 15 && ((projection[i] >>> ii) & 1) == 1) ii++;
			if (ii >= 15 && ((projection[i] >>> ii) & 1) == 1) //if end is not in this chunk
			{
				ii = 0;
				i++;
				while (i < projection.length && ~(projection[i]) == 0 && i < levelOffset * (level + 1)) i++; //check for big solid blocks
				if (i == projection.length || i == levelOffset * (level + 1)) //solid to WORLD_HEIGHT
				{
					heightAndDepth[count * 3] = depth;
					heightAndDepth[count * 3 + 1] = (short) (worldHeight - 1);
					heightAndDepth[count * 3 + 2] = level;
					count++;
					break;
				}
				while ((((projection[i] >>> ii) & 1) == 1)) ii++;
			}
			height = (short) (i * 16 + ii - 1);
			heightAndDepth[count * 3] = depth;
			heightAndDepth[count * 3 + 1] = height;
			heightAndDepth[count * 3 + 2] = level;
			count++;
		}
		
		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > maxVerticalData)
		{
			ii = worldHeight;
			for (i = 0; i < count - 1; i++)
			{
				if (heightAndDepth[(i + 1) * 3] - heightAndDepth[i * 3 + 1] < ii && heightAndDepth[(i + 1) * 3 + 2] == heightAndDepth[i * 3 + 2])
				{
					ii = heightAndDepth[(i + 1) * 3] - heightAndDepth[i * 3 + 1];
					j = i;
				}
			}
			if (ii == worldHeight) //all gaps are between different levels
			{
				for (i = 0; i < count - 1; i++)
				{
					if (heightAndDepth[(i + 1) * 3] - heightAndDepth[i * 3 + 1] < ii)
					{
						ii = heightAndDepth[(i + 1) * 3] - heightAndDepth[i * 3 + 1];
						j = i;
					}
				}
			}
			heightAndDepth[j * 3 + 1] = heightAndDepth[(j + 1) * 3 + 1]; //by ignoring level we set lower one as new one
			for (i = j + 1; i < count - 1; i++)
			{
				heightAndDepth[i * 3] = heightAndDepth[(i + 1) * 3];
				heightAndDepth[i * 3 + 1] = heightAndDepth[(i + 1) * 3 + 1];
				heightAndDepth[i * 3 + 2] = heightAndDepth[(i + 1) * 3 + 2];
			}
			count--;
		}
		//As standard the vertical lods are ordered from top to bottom
		for (j = 0; j < count; j++)
		{
			depth = heightAndDepth[j * 3];
			height = heightAndDepth[j * 3 + 1];
			level = heightAndDepth[j * 3 + 2];
			if ((depth == 0 && height == 0) || j >= heightAndDepth.length / 3)
				break;
			Arrays.fill(singleDataToMerge, 0);
			for (int index = 0; index < size; index++)
			{
				for (int dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
				{
					singleData = dataToMerge[index * inputVerticalData + dataIndex];
					if (getHeight(singleData) > 255)
					{
						ClientProxy.LOGGER.info("height: " + getHeight(singleData));
					}
					if (doesItExist(singleData) && !isVoid(singleData) && getVerticalLevel(singleData) == level
							&& ((depth <= getDepth(singleData) && getDepth(singleData) <= height)
							|| (depth <= getHeight(singleData) && getHeight(singleData) <= height))
							&& getHeight(singleData) > getHeight(singleDataToMerge[index]))
						singleDataToMerge[index] = singleData;
				}
			}
			long data = mergeSingleData(singleDataToMerge);
			
			dataPoint[count - j - 1] = createDataPoint(height, depth, getColor(data), getLightSky(data), getLightBlock(data), getGenerationMode(data), getVerticalLevel(data));
		}
		return dataPoint;
	}
	
	public static long[] compress(long[] data, byte detailLevel)
	{
		return null;
	}
}