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

package com.lishid.orebfuscator.internal.v1_6_R3;

import java.util.zip.Deflater;

import net.minecraft.server.v1_6_R3.Packet56MapChunkBulk;

import com.lishid.orebfuscator.utils.ReflectionHelper;

public class Packet56 {
	Packet56MapChunkBulk packet;

	byte[][] inflatedBuffers;

	public void setPacket(Object packet) {
		if (packet instanceof Packet56MapChunkBulk) {
			this.packet = (Packet56MapChunkBulk) packet;
			inflatedBuffers = (byte[][]) ReflectionHelper.getPrivateField(packet, "field_73584_f");
		}
	}

	public int getPacketChunkNumber() {
		return packet.d();
	}

	public int[] getX() {
		return (int[]) ReflectionHelper.getPrivateField(packet, "field_73589_c");
	}

	public int[] getZ() {
		return (int[]) ReflectionHelper.getPrivateField(packet, "field_73586_d");
	}

	public int[] getChunkMask() {
		return packet.a;
	}

	public int[] getExtraMask() {
		return packet.b;
	}

	public byte[][] getInflatedBuffers() {
		return inflatedBuffers;
	}

	public void compress() {
		int bufferSize = 0;
		for (int i = 0; i < inflatedBuffers.length; i++) {
			bufferSize += inflatedBuffers[i].length;
		}

		byte[] buildBuffer = new byte[bufferSize];
		bufferSize = 0;
		for (int i = 0; i < inflatedBuffers.length; i++) {
			System.arraycopy(inflatedBuffers[i], 0, buildBuffer, bufferSize, inflatedBuffers[i].length);
			bufferSize += inflatedBuffers[i].length;
		}

		Deflater deflater = new Deflater(Deflater.NO_COMPRESSION);
		deflater.setInput(buildBuffer);
		deflater.finish();

		byte[] outputBuffer = new byte[bufferSize + 100];
		ReflectionHelper.setPrivateField(packet, "field_73587_e", outputBuffer);
		ReflectionHelper.setPrivateField(packet, "field_73585_g", deflater.deflate(outputBuffer));
	}

}
