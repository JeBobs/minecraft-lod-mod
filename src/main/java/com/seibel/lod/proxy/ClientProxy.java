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
package com.seibel.lod.proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seibel.lod.builders.LodNodeBufferBuilder;
import com.seibel.lod.builders.LodNodeBuilder;
import com.seibel.lod.builders.worldGeneration.LodNodeGenWorker;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogDrawOverride;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.enums.ShadingMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodQuadTreeDimension;
import com.seibel.lod.objects.LodQuadTreeWorld;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.render.LodNodeRenderer;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of this program.
 * 
 * @author James_Seibel
 * @version 8-11-2021
 */
public class ClientProxy
{
	public static final Logger LOGGER = LogManager.getLogger("LOD");
	
	private static LodQuadTreeWorld lodWorld = new LodQuadTreeWorld();
	private static LodNodeBuilder lodNodeBuilder = new LodNodeBuilder();
	private static LodNodeBufferBuilder lodBufferBuilder = new LodNodeBufferBuilder(lodNodeBuilder);
	private static LodNodeRenderer renderer = new LodNodeRenderer(lodBufferBuilder);
	
	private boolean configOverrideReminderPrinted = false;
	
	Minecraft mc = Minecraft.getInstance();
	
	
	public ClientProxy()
	{
		
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	/**
	 * Do any setup that is required to draw LODs
	 * and then tell the LodRenderer to draw.
	 */
	public void renderLods(float partialTicks)
	{
		if (mc == null || mc.player == null || !lodWorld.getIsWorldLoaded())
			return;
		
		// update each regions' width to match the new render distance
		int newWidth = Math.max(4, 
				// TODO is this logic good?
				(mc.options.renderDistance * LodUtil.CHUNK_WIDTH * 2 * LodConfig.CLIENT.lodChunkRadiusMultiplier.get()) / LodUtil.REGION_WIDTH_IN_CHUNKS
				);
		if (lodNodeBuilder.regionWidth != newWidth)
		{
			lodWorld.resizeDimensionRegionWidth(newWidth);
			lodNodeBuilder.regionWidth = newWidth;
			
			// skip this frame, hopefully the lodWorld
			// should have everything set up by then
			return;
		}
		
		LodQuadTreeDimension lodDim = lodWorld.getLodDimension(mc.player.level.dimensionType());
		if (lodDim == null)
			return;
		
		
		// offset the regions
		double playerX = mc.player.getX();
		double playerZ = mc.player.getZ();
		
		int xOffset = ((int)playerX / (LodUtil.CHUNK_WIDTH * LodUtil.REGION_WIDTH_IN_CHUNKS)) - lodDim.getCenterX();
		int zOffset = ((int)playerZ / (LodUtil.CHUNK_WIDTH * LodUtil.REGION_WIDTH_IN_CHUNKS)) - lodDim.getCenterZ();
		
		if (xOffset != 0 || zOffset != 0)
		{
			lodDim.move(new RegionPos(xOffset, zOffset));
		}
		
		
		
		// comment out when creating a release
		applyConfigOverrides();
		
		
		
		// Note to self:
		// if "unspecified" shows up in the pie chart, it is
		// possibly because the amount of time between sections
		// is too small for the profiler to measure
		IProfiler profiler = mc.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("LOD");
		
		renderer.drawLODs(lodDim, partialTicks, mc.getProfiler());
		
		profiler.pop(); // end LOD
		profiler.push("terrain"); // restart terrain
	}	
	
	
	
	private void applyConfigOverrides()
	{
		// remind the developer(s). that config override is active
		if (!configOverrideReminderPrinted)
		{
			mc.player.sendMessage(new StringTextComponent("Debug settings enabled!"), mc.player.getUUID());
			configOverrideReminderPrinted = true;
		}
		
//		LodConfig.CLIENT.drawLODs.set(true);
//		LodConfig.CLIENT.debugMode.set(false);
		
		LodConfig.CLIENT.maxDrawDetail.set(LodDetail.FULL);
		LodConfig.CLIENT.maxGenerationDetail.set(LodDetail.FULL);
		
		LodConfig.CLIENT.lodChunkRadiusMultiplier.set(12);
		LodConfig.CLIENT.fogDistance.set(FogDistance.FAR);
		LodConfig.CLIENT.fogDrawOverride.set(FogDrawOverride.ALWAYS_DRAW_FOG_FANCY);
		LodConfig.CLIENT.shadingMode.set(ShadingMode.DARKEN_SIDES);
//		LodConfig.CLIENT.brightnessMultiplier.set(1.0);
//		LodConfig.CLIENT.saturationMultiplier.set(1.0);
		
		LodConfig.CLIENT.distanceGenerationMode.set(DistanceGenerationMode.FEATURES);
		LodConfig.CLIENT.allowUnstableFeatureGeneration.set(false);
		
//		LOGGER.info(lodBufferBuilder.numberOfChunksWaitingToGenerate.get());
	}
	
	
	//==============//
	// forge events //
	//==============//
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event)
	{
		//lodChunkBuilder.generateLodChunkAsync(event.getChunk(), lodWorld, event.getWorld());
		lodNodeBuilder.generateLodNodeAsync(event.getChunk(), lodWorld, event.getWorld());
	}
	
	
	@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event)
	{
		// the player just loaded a new world/dimension
		lodWorld.selectWorld(LodUtil.getWorldID(event.getWorld()));
		// make sure the correct LODs are being rendered
		// (if this isn't done the previous world's LODs may be drawn)
		renderer.regenerateLODsNextFrame();
	}
	
	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event)
	{
		// the player just unloaded a world/dimension
		
		if(mc.getConnection().getLevel() == null)
		{
			// if this isn't done unfinished tasks may be left in the queue
			// preventing new LodChunks form being generated
			LodNodeGenWorker.restartExecuterService();
			
			lodBufferBuilder.numberOfChunksWaitingToGenerate.set(0);
			// the player has disconnected from a server
			lodWorld.deselectWorld();
		}
	}
	
	
	@SubscribeEvent
	public void blockChangeEvent(BlockEvent event)
	{
		if (event.getClass() == BlockEvent.BreakEvent.class ||
			event.getClass() == BlockEvent.EntityPlaceEvent.class ||
			event.getClass() == BlockEvent.EntityMultiPlaceEvent.class ||
			event.getClass() == BlockEvent.FluidPlaceBlockEvent.class ||
			event.getClass() == BlockEvent.PortalSpawnEvent.class)
		{
			// recreate the LOD where the blocks were changed
			//lodChunkBuilder.generateLodChunkAsync(event.getWorld().getChunk(event.getPos()), lodWorld, event.getWorld());
			lodNodeBuilder.generateLodNodeAsync(event.getWorld().getChunk(event.getPos()), lodWorld, event.getWorld());
		}
	}
	
	
	
	
	//================//
	// public getters //
	//================//
	
	public static LodQuadTreeWorld getLodWorld()
	{
		return lodWorld;
	}
	
	public static LodNodeBuilder getLodBuilder()
	{
		return lodNodeBuilder;
	}
	
	public static LodNodeRenderer getRenderer()
	{
		return renderer;
	}
}
