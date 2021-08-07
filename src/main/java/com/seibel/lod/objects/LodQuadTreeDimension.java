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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.handlers.LodQuadTreeDimensionFileHandler;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

public class LodQuadTreeDimension
{

    /**TODO a dimension should support two different type of quadTree.
     * The ones that are near from the player should always be saved and can be fully generated (even at block level)
     * The ones that are far from the player should always be non-savable and at a high level
     * If this is not done then you could see how heavy a fully generated 64 region dimension can get.
     * IDEA : use a mask like the "isRegionDirty" to achieve this*/

	public final DimensionType dimension;
	
	private volatile int width;
	private volatile int halfWidth;
	public long seed;
	
	public static final Set<DistanceGenerationMode> FULL_COMPLEXITY_MASK = new HashSet<DistanceGenerationMode>();
	static
	{
		// I moved the setup here because eclipse was complaining
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.BIOME_ONLY);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.SURFACE);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.FEATURES);
		FULL_COMPLEXITY_MASK.add(DistanceGenerationMode.SERVER);
	}
	
	
	public volatile LodQuadTree regions[][];
	public volatile boolean isRegionDirty[][];
	
	private volatile int centerX;
	private volatile int centerZ;
	
	private LodQuadTreeDimensionFileHandler fileHandler;
	
	
	
	
	public LodQuadTreeDimension(DimensionType newDimension, LodQuadTreeWorld lodWorld, int newMaxWidth)
    {
        dimension = newDimension;
        width = newMaxWidth;
        if(newDimension != null && lodWorld != null) {
            try {
                Minecraft mc = Minecraft.getInstance();

                File saveDir;
                if (mc.hasSingleplayerServer()) {
                    // local world

                    ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(newDimension);
                    seed = serverWorld.getSeed();
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

                fileHandler = new LodQuadTreeDimensionFileHandler(saveDir, this);

            }
            catch (IOException e)
            {
                // the file handler wasn't able to be created
                // we won't be able to read or write any files
            }
        }


        regions = new LodQuadTree[width][width];
        isRegionDirty = new boolean[width][width];

        // populate isRegionDirty
        for(int i = 0; i < width; i++)
            for(int j = 0; j < width; j++)
                isRegionDirty[i][j] = false;

        centerX = 0;
        centerZ = 0;

        halfWidth = (int)Math.floor(width / 2);
    }


    /**
     * Move the center of this LodDimension and move all owned
     * regions over by the given x and z offset.
     */
    public synchronized void move(int xOffset, int zOffset)
    {
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
            centerX += xOffset;
            centerZ += zOffset;

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
        centerX += xOffset;
        centerZ += zOffset;
    }






    /**
     * Gets the region at the given X and Z
     * <br>
     * Returns null if the region doesn't exist
     * or is outside the loaded area.
     */
    public LodQuadTree getRegion(int regionX, int regionZ)
    {
        int xIndex = (regionX - centerX) + halfWidth;
        int zIndex = (regionZ - centerZ) + halfWidth;

        if (!regionIsInRange(regionX, regionZ))
            // out of range
            return null;

        if (regions[xIndex][zIndex] == null)
        {
            regions[xIndex][zIndex] = getRegionFromFile(regionX, regionZ);
            if (regions[xIndex][zIndex] == null)
            {
                regions[xIndex][zIndex] = new LodQuadTree(regionX, regionZ);
            }
        }

        return regions[xIndex][zIndex];
    }

    /**
     * Overwrite the LodRegion at the location of newRegion with newRegion.
     * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
     */
    public void addOrOverwriteRegion(LodQuadTree newRegion) throws ArrayIndexOutOfBoundsException
    {
        int xIndex = (newRegion.getLodNodeData().posX - centerX) + halfWidth;
        int zIndex = (centerZ - newRegion.getLodNodeData().posZ) + halfWidth;

        if (!regionIsInRange(newRegion.getLodNodeData().posX, newRegion.getLodNodeData().posZ))
            // out of range
            throw new ArrayIndexOutOfBoundsException();

        regions[xIndex][zIndex] = newRegion;
    }


    /**
     *this method create all the regions that are null
     */
    public void initializeNullRegions()
    {
        int n = regions.length;
        int xIndex;
        int zIndex;
        LodQuadTree region;
        for(int xRegion=0; xRegion<n; xRegion++)
        {
            for(int zRegion=0; zRegion<n; zRegion++)
            {
                xIndex = (xRegion + centerX) - halfWidth;
                zIndex = (zRegion + centerZ) - halfWidth;
                region = getRegion(xIndex,zIndex);
                
                if (region == null)
                {
                    // if no region exists, create it
                    region = new LodQuadTree(xIndex, zIndex);
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
    public Boolean addNode(LodQuadTreeNode lodNode)
    {
		RegionPos regionPos = LodUtil.convertChunkPosToRegionPos(new ChunkPos(lodNode.centerX, lodNode.centerZ));

        // don't continue if the region can't be saved
        if (!regionIsInRange(regionPos.x, regionPos.z))
        {
            return false;
        }

        LodQuadTree region = getRegion(regionPos.x, regionPos.z);

        if (region == null)
        {
            // if no region exists, create it
            region = new LodQuadTree(regionPos.x, regionPos.z);
            addOrOverwriteRegion(region);
        }
        boolean nodeAdded = region.setNodeAtLowerLevel(lodNode, true);

        // only save valid LODs to disk
        if (!lodNode.dontSave && fileHandler != null)
        {
            // mark the region as dirty so it will be saved to disk
            int xIndex = (regionPos.x - centerX) + halfWidth;
            int zIndex = (regionPos.z - centerZ) + halfWidth;
            isRegionDirty[xIndex][zIndex] = true;
            fileHandler.saveDirtyRegionsToFileAsync();
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
    public LodQuadTreeNode getLodFromCoordinates(ChunkPos chunkPos)
    {
        return getLodFromCoordinates(chunkPos.x, chunkPos.z, LodQuadTreeNode.CHUNK_LEVEL);
    }

    /**
     * Get the LodNodeData at the given X and Z coordinates
     * in this dimension.
     * <br>
     * Returns null if the LodChunk doesn't exist or
     * is outside the loaded area.
     */
    public LodQuadTreeNode getLodFromCoordinates(int chunkPosX, int chunkPosZ)
    {
        return getLodFromCoordinates(chunkPosX, chunkPosZ, LodQuadTreeNode.CHUNK_LEVEL);
    }
	
    /**
     * Get the LodNodeData at the given X and Z coordinates
     * in this dimension.
     * <br>
     * Returns null if the LodChunk doesn't exist or
     * is outside the loaded area.
     */
    public LodQuadTreeNode getLodFromCoordinates(int posX, int posZ, int detailLevel)
    {
    	if (detailLevel > LodQuadTreeNode.REGION_LEVEL)
    		throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodQuadTreeNode.REGION_LEVEL + "\" is the max.");

        int regionPosX = Math.floorDiv(posX, (int) Math.pow(2,LodQuadTreeNode.REGION_LEVEL - detailLevel));
        int regionPosZ = Math.floorDiv(posZ, (int) Math.pow(2,LodQuadTreeNode.REGION_LEVEL - detailLevel));
    	LodQuadTree region = getRegion(regionPosX, regionPosZ);

        if(region == null)
        {
            //System.out.println("THIS CASE");
            return null;
        }
        return region.getNodeAtChunkPos(posX, posZ, detailLevel);
        
        /*
        RegionPos pos = LodUtil.convertChunkPosToRegionPos(new ChunkPos(chunkX, chunkZ));
		
        LodQuadTree region = getRegion(pos.x, pos.z);
		
        return region.getNode(chunkX, chunkZ);
         */
    }

    /**
     * return true if and only if the node at that position exist
     */
    public boolean hasThisPositionBeenGenerated(int posX, int posZ, int level)
    {
    	if (level > LodQuadTreeNode.REGION_LEVEL)
    		throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + level + "\" when \"" + LodQuadTreeNode.REGION_LEVEL + "\" is the max.");
    	
        return getLodFromCoordinates(posX,posZ,level).detailLevel == level;
    }

    /**
     * method to get all the nodes that have to be rendered based on the position of the player
     * @return list of nodes
     */
    public List<LodQuadTreeNode> getNodeToRender(int x, int z, int level, Set<DistanceGenerationMode> complexityMask, int maxDistance, int minDistance)
    {
        int n = regions.length;
        List<LodQuadTreeNode> listOfData = new ArrayList<>();
        for(int i=0; i<n; i++)
        {
            for(int j=0; j<n; j++)
            {
                listOfData.addAll(regions[i][j].getNodeToRender(x,z,level,complexityMask,maxDistance,minDistance));
            }
        }
        return listOfData;
    }

    /**
     * method to get all the quadtree level that have to be generated based on the position of the player
     * @return list of quadTrees
     */
    public List<LodQuadTreeNode> getNodesToGenerate(int x, int z, byte level, DistanceGenerationMode complexity, int maxDistance, int minDistance)
    {
        int n = regions.length;
        int xIndex;
        int zIndex;
        LodQuadTree region;
        List<Map.Entry<LodQuadTreeNode,Integer>> listOfQuadTree = new ArrayList<>();
        for(int xRegion=0; xRegion<n; xRegion++){
            for(int zRegion=0; zRegion<n; zRegion++){
                xIndex = (xRegion + centerX) - halfWidth;
                zIndex = (zRegion + centerZ) - halfWidth;
                region = getRegion(xIndex,zIndex);
                if (region == null){
                    region = new LodQuadTree(xIndex, zIndex);
                    addOrOverwriteRegion(region);
                }
                listOfQuadTree.addAll(region.getNodesToGenerate(x,z,level,complexity,maxDistance,minDistance));
            }
        }
        Collections.sort(listOfQuadTree,Map.Entry.comparingByValue());
        return listOfQuadTree.stream().map(entry -> entry.getKey()).collect(Collectors.toList());
    }

    /**
     * getNodes
     * @return list of quadTrees
     */
    public List<LodQuadTreeNode> getNodes(Set<DistanceGenerationMode> complexityMask, boolean getOnlyDirty, boolean getOnlyLeaf)
    {
        int n = regions.length;
        List<LodQuadTreeNode> listOfNodes = new ArrayList<>();
        int xIndex;
        int zIndex;
        LodQuadTree region;
        for(int xRegion=0; xRegion<n; xRegion++){
            for(int zRegion=0; zRegion<n; zRegion++){
                xIndex = (xRegion + centerX) - halfWidth;
                zIndex = (zRegion + centerZ) - halfWidth;
                region = getRegion(xIndex,zIndex);
                if (region != null){
                    listOfNodes.addAll(region.getNodeList(complexityMask, getOnlyDirty, getOnlyLeaf));
                }
            }
        }
        return listOfNodes;
    }

    /**
     * Get the region at the given X and Z coordinates from the
     * RegionFileHandler.
     */
    public LodQuadTree getRegionFromFile(int regionX, int regionZ)
    {
        if (fileHandler != null)
            return fileHandler.loadRegionFromFile(regionX, regionZ);
        else
            return null;
    }


    /**
     * Returns whether the region at the given X and Z coordinates
     * is within the loaded range.
     */
    public boolean regionIsInRange(int regionX, int regionZ)
    {
        int xIndex = (regionX - centerX) + halfWidth;
        int zIndex = (regionZ - centerZ) + halfWidth;

        return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
    }
	
	
	
	
	
	
	
    public int getCenterX()
    {
        return centerX;
    }

    public int getCenterZ()
    {
        return centerZ;
    }


    /**
     * TODO THIS METHOD HAVE TO BE CHANGES. IS NOT THE SAME AS NUMER OF CHUNK
     * Is it good now? - James
     * 
     * Returns how many non-null LodChunks
     * are stored in this LodDimension.
     */
    public int getNumberOfLods()
    {
        int numbLods = 0;
        for (LodQuadTree[] regions : regions)
        {
            if(regions == null)
                continue;

            for (LodQuadTree region : regions)
            {
                if(region == null)
                    continue;
                
                for(LodQuadTreeNode node : region.getNodeList(FULL_COMPLEXITY_MASK,false,true))
				{
                	if (node != null && !node.voidNode)
                		numbLods++;
				}
            }
        }
		
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

        regions = new LodQuadTree[width][width];
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
        s += "(" + centerX + "," + centerZ + ")";

        return s;
    }
}
