/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator.obfuscation;

import java.util.zip.Deflater;

import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.OrebfuscatorConfig;
import com.lishid.orebfuscator.internal.IPacket51;
import com.lishid.orebfuscator.internal.IPacket56;
import com.lishid.orebfuscator.internal.InternalAccessor;

public class Calculations {

	public static final ThreadLocal<Deflater> localDeflater = new ThreadLocal<Deflater>() {
		@Override
		protected Deflater initialValue() {
			// Not used from orebfuscator thread, best speed instead
			return new Deflater(Deflater.BEST_SPEED);
		}
	};

	public static void Obfuscate(Object packet, Player player) {
		IPacket51 packet51 = InternalAccessor.Instance.newPacket51();
		packet51.setPacket(packet);
		Calculations.Obfuscate(packet51, player);
	}

	public static void Obfuscate(IPacket56 packet, Player player) {
		if (packet.getFieldData(packet.getOutputBuffer()) != null) {
			return;
		}

		ChunkInfo[] infos = getInfo(packet, player);

		for (int chunkNum = 0; chunkNum < infos.length; chunkNum++) {
			// Create an info objects
			ChunkInfo info = infos[chunkNum];
			ComputeChunkInfoAndObfuscate(info, (byte[]) packet.getFieldData(packet.getBuildBuffer()));
		}
	}

	public static void Obfuscate(IPacket51 packet, Player player) {
		ChunkInfo info = getInfo(packet, player);

		if (info.chunkMask == 0 && info.extraMask == 0) {
			return;
		}

		ComputeChunkInfoAndObfuscate(info, packet.getBuffer());

		Deflater deflater = localDeflater.get();
		packet.compress(deflater);
	}

	public static ChunkInfo[] getInfo(IPacket56 packet, Player player) {
		ChunkInfo[] infos = new ChunkInfo[packet.getPacketChunkNumber()];

		int dataStartIndex = 0;

		int[] x = packet.getX();
		int[] z = packet.getZ();

		byte[][] inflatedBuffers = (byte[][]) packet.getFieldData(packet.getInflatedBuffers());

		int[] chunkMask = packet.getChunkMask();
		int[] extraMask = packet.getExtraMask();

		byte[] buildBuffer = (byte[]) packet.getFieldData(packet.getBuildBuffer());

		// Check for spigot and fix accordingly
		if (buildBuffer.length == 0) {
			int finalBufferSize = 0;
			for (int i = 0; i < inflatedBuffers.length; i++) {
				finalBufferSize += inflatedBuffers[i].length;
			}

			buildBuffer = new byte[finalBufferSize];
			int bufferLocation = 0;
			for (int i = 0; i < inflatedBuffers.length; i++) {
				System.arraycopy(inflatedBuffers[i], 0, buildBuffer, bufferLocation, inflatedBuffers[i].length);
				bufferLocation += inflatedBuffers[i].length;
			}

			packet.setFieldData(packet.getBuildBuffer(), buildBuffer);
		}

		for (int chunkNum = 0; chunkNum < packet.getPacketChunkNumber(); chunkNum++) {
			// Create an info objects
			ChunkInfo info = new ChunkInfo();
			infos[chunkNum] = info;
			info.world = player.getWorld();
			info.chunkX = x[chunkNum];
			info.chunkZ = z[chunkNum];
			info.chunkMask = chunkMask[chunkNum];
			info.extraMask = extraMask[chunkNum];
			info.data = buildBuffer;
			info.startIndex = dataStartIndex;

			dataStartIndex += inflatedBuffers[chunkNum].length;
		}

		return infos;
	}

	public static ChunkInfo getInfo(IPacket51 packet, Player player) {
		// Create an info objects
		ChunkInfo info = new ChunkInfo();
		info.world = player.getWorld();
		info.chunkX = packet.getX();
		info.chunkZ = packet.getZ();
		info.chunkMask = packet.getChunkMask();
		info.extraMask = packet.getExtraMask();
		info.data = packet.getBuffer();
		info.startIndex = 0;
		return info;
	}

	public static void ComputeChunkInfoAndObfuscate(ChunkInfo info, byte[] original) {
		// Compute chunk number
		int chunkSectionNumber = 0;
		for (int i = 0; i < 16; i++) {
			if ((info.chunkMask & 1 << i) > 0) {
				chunkSectionNumber++;
			}
		}

		if (info.startIndex + 4096 * chunkSectionNumber > info.data.length) {
			return;
		}

		// Obfuscate
		if (!OrebfuscatorConfig.isWorldDisabled(info.world.getName()) && OrebfuscatorConfig.Enabled) {
			Obfuscate(info);
		}
	}

	@SuppressWarnings("deprecation")
	public static void Obfuscate(ChunkInfo info) {
		boolean isNether = info.world.getEnvironment() == Environment.NETHER;

		int initialRadius = OrebfuscatorConfig.InitialRadius;

		// Track of pseudo-randomly assigned randomBlock
		int randomIncrement = 0;

		int engineMode = OrebfuscatorConfig.EngineMode;

		int randomBlocksLength = OrebfuscatorConfig.getRandomBlocks(isNether).length;

		// Loop over 16x16x16 chunks in the 16x256x16 column
		int currentTypeIndex = 0;
		int currentExtendedIndex = 0;
		for (int i = 0; i < 16; i++) {
			if ((info.chunkMask & 1 << i) != 0) {
				currentExtendedIndex += 4096 + 2048 + 2048 + 2048;
			}
		}

		int startX = info.chunkX << 4;
		int startZ = info.chunkZ << 4;

		for (int i = 0; i < 16; i++) {
			// If the bitmask indicates this chunk is sent...
			if ((info.chunkMask & 1 << i) != 0) {

				boolean usesExtra = ((info.extraMask & 1 << i) != 0);
				int block1extra = 0;

				OrebfuscatorConfig.shuffleRandomBlocks();
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						for (int x = 0; x < 16; x++) {

							int blockY = (i << 4) + y;
							int typeID = info.world.getBlockTypeIdAt(startX + x, blockY, startZ + z);

							// Obfuscate block if needed
							if (OrebfuscatorConfig.isObfuscated(typeID, isNether) && !areAjacentBlocksTransparent(info, startX + x, blockY, startZ + z, initialRadius)) {
								int newBlockID = 0;
								if (engineMode == 1) {
									// Engine mode 1, use stone
									newBlockID = (isNether ? 87 : 1);
								} else if (engineMode == 2) {
									// Ending mode 2, get random block
									randomIncrement = CalculationsUtil.increment(randomIncrement, randomBlocksLength);
									newBlockID = OrebfuscatorConfig.getRandomBlockID(randomIncrement, isNether);
								}
								byte type = (byte) (newBlockID % 256);
								info.data[info.startIndex + currentTypeIndex] = type;
								if (usesExtra) {
									byte extra = (byte) (newBlockID / 256);
									if (currentTypeIndex % 2 == 0) {
										block1extra = extra;
									} else {
										info.data[info.startIndex + currentExtendedIndex] = (byte) (block1extra * 16 + extra);
									}
								}
							}

							if (usesExtra) {
								if (currentTypeIndex % 2 == 1) {
									currentExtendedIndex++;
								}
							}
							currentTypeIndex++;
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static boolean areAjacentBlocksTransparent(ChunkInfo info, int x, int y, int z, int countdown) {
		if (y >= info.world.getMaxHeight() || y < 0) {
			return true;
		}

		int id = 1;
		if (CalculationsUtil.isChunkLoaded(info.world, x >> 4, z >> 4)) {
			id = info.world.getBlockTypeIdAt(x, y, z);
		}

		if (OrebfuscatorConfig.isBlockTransparent(id)) {
			return true;
		}

		if (countdown == 0) {
			return false;
		}

		if (areAjacentBlocksTransparent(info, x, y + 1, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(info, x, y - 1, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(info, x + 1, y, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(info, x - 1, y, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(info, x, y, z + 1, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(info, x, y, z - 1, countdown - 1)) {
			return true;
		}

		return false;
	}

}