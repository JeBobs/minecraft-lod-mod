package com.seibel.lod.builders.worldGeneration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.objects.LevelPosUtil;
import com.seibel.lod.objects.PosToGenerateContainer;

import com.seibel.lod.builders.GenerationRequest;
import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * A singleton that handles all long distance LOD world generation.
 *
 * @author James Seibel
 * @version 8-24-2021
 */
public class LodWorldGenerator
{
	public Minecraft mc = Minecraft.getInstance();

	/**
	 * This holds the thread used to generate new LODs off the main thread.
	 */
	private ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " world generator"));

	/**
	 * we only want to queue up one generator thread at a time
	 */
	private boolean generatorThreadRunning = false;

	/**
	 * how many chunks to generate outside of the player's view distance at one
	 * time. (or more specifically how many requests to make at one time). I
	 * multiply by 8 to make sure there is always a buffer of chunk requests, to
	 * make sure the CPU is always busy and we can generate LODs as quickly as
	 * possible.
	 */
	public int maxChunkGenRequests;

	/**
	 * This keeps track of how many chunk generation requests are on going. This is
	 * to limit how many chunks are queued at once. To prevent chunks from being
	 * generated for a long time in an area the player is no longer in.
	 */
	public AtomicInteger numberOfChunksWaitingToGenerate = new AtomicInteger(0);

	public Set<ChunkPos> positionWaitingToBeGenerated = new HashSet<>();

	/**
	 * Singleton copy of this object
	 */
	public static final LodWorldGenerator INSTANCE = new LodWorldGenerator();


	private LodWorldGenerator()
	{

	}

	/**
	 * Queues up LodNodeGenWorkers for the given lodDimension.
	 *
	 * @param renderer needed so the LodNodeGenWorkers can flag that the
	 *                 buffers need to be rebuilt.
	 */
	public void queueGenerationRequests(LodDimension lodDim, LodRenderer renderer, LodBuilder lodBuilder)
	{
		if (LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get() != DistanceGenerationMode.NONE
				    && !generatorThreadRunning
				    && mc.hasSingleplayerServer())
		{
			// the thread is now running, don't queue up another thread
			generatorThreadRunning = true;

			// just in case the config changed
			maxChunkGenRequests = LodConfig.CLIENT.threading.numberOfWorldGenerationThreads.get() * 8;

			Thread generatorThread = new Thread(() ->
			{
				try
				{
					// round the player's block position down to the nearest chunk BlockPos
					int playerPosX = (int) mc.player.position().x;
					int playerPosZ = (int) mc.player.position().z;


					ArrayList<GenerationRequest> chunksToGen = new ArrayList<>(maxChunkGenRequests);
					// if we don't have a full number of chunks to generate in chunksToGen
					// we can top it off from this reserve


					//=======================================//
					// create the generation Request objects //
					//=======================================//
					List<GenerationRequest> generationRequestList = new ArrayList<>(maxChunkGenRequests);

					ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);

					byte farDetail = (byte) 8;
					PosToGenerateContainer posToGenerate = lodDim.getDataToGenerate(
							farDetail,
							maxChunkGenRequests,
							0.25,
							playerPosX,
							playerPosZ);
					//System.out.println(posToGenerate);
					//here we prepare two sorted set
					//the first contains the near pos to render
					//the second contain the far pos to render
					byte detailLevel;
					int posX;
					int posZ;
					int[] levelPos;
					for (int index = 0; index < posToGenerate.getNumberOfPos(); index++)
					{
						levelPos = posToGenerate.getNthPos(index);
						if(levelPos[0] == 0)
							continue;
						detailLevel = (byte) (levelPos[0] -1);
						posX = levelPos[1];
						posZ = levelPos[2];

						ChunkPos chunkPos = new ChunkPos(LevelPosUtil.getChunkPos(detailLevel,posX), LevelPosUtil.getChunkPos(detailLevel,posZ));
						if (numberOfChunksWaitingToGenerate.get() < maxChunkGenRequests)
						{
							// prevent generating the same chunk multiple times
							if (positionWaitingToBeGenerated.contains(chunkPos))
							{
								continue;
							}
						}

						// don't add null chunkPos (which shouldn't happen anyway)
						// or add more to the generation queue
						if (chunkPos == null || numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
							continue;

						positionWaitingToBeGenerated.add(chunkPos);
						numberOfChunksWaitingToGenerate.addAndGet(1);
						LodNodeGenWorker genWorker = new LodNodeGenWorker(chunkPos,  DetailDistanceUtil.getDistanceGenerationMode(detailLevel), renderer, lodBuilder, lodDim, serverWorld);
						WorldWorkerManager.addWorker(genWorker);
					}

				} catch (Exception e)
				{
					// this shouldn't ever happen, but just in case
					e.printStackTrace();
				} finally
				{
					generatorThreadRunning = false;
				}
			});

			mainGenThread.execute(generatorThread);
		} // if distanceGenerationMode != DistanceGenerationMode.NONE && !generatorThreadRunning
	}

}
