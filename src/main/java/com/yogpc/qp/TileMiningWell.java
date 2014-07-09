/*
 * Copyright (C) 2012,2013 yogpstop
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the
 * GNU Lesser General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.yogpc.qp;

import buildcraft.BuildCraftFactory;

import com.google.common.io.ByteArrayDataInput;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileMiningWell extends TileBasic {

	private boolean working;

	@Override
	protected void C_recievePacket(byte pattern, ByteArrayDataInput data, EntityPlayer ep) {
		super.C_recievePacket(pattern, data, ep);
		switch (pattern) {
		case PacketHandler.StC_NOW:
			this.working = data.readBoolean();
			G_renew_powerConfigure();
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			break;
		}
	}

	@Override
	protected void G_renew_powerConfigure() {
		byte pmp = 0;
		if (this.worldObj != null) {
			TileEntity te = this.worldObj.getTileEntity(this.xCoord + this.pump.offsetX, this.yCoord + this.pump.offsetY, this.zCoord + this.pump.offsetZ);
			if (te instanceof TilePump) pmp = ((TilePump) te).unbreaking;
			else this.pump = ForgeDirection.UNKNOWN;
		}
		if (this.working) PowerManager.configureW(this, this.efficiency, this.unbreaking, pmp);
		else PowerManager.configure0(this);
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (this.worldObj.isRemote) return;
		int depth = this.yCoord - 1;
		while (!S_checkTarget(depth)) {
			if (this.working) this.worldObj.setBlock(this.xCoord, depth, this.zCoord, BuildCraftFactory.plainPipeBlock);
			depth--;
		}
		if (this.working) S_breakBlock(this.xCoord, depth, this.zCoord);
		S_pollItems();
	}

	private boolean S_checkTarget(int depth) {
		if (depth < 1) {
			G_destroy();
			return true;
		}
		Block b = this.worldObj.getChunkProvider().loadChunk(this.xCoord >> 4, this.zCoord >> 4).getBlock(this.xCoord & 0xF, depth, this.zCoord & 0xF);
		float h = b == null ? -1 : b.getBlockHardness(this.worldObj, this.xCoord, depth, this.zCoord);
		if (b == null || h < 0 || b == BuildCraftFactory.plainPipeBlock || b.isAir(this.worldObj, this.xCoord, depth, this.zCoord)) return false;
		if (this.pump == ForgeDirection.UNKNOWN && b.getMaterial().isLiquid()) return false;
		if (!this.working) {
			this.working = true;
			G_renew_powerConfigure();
			PacketHandler.sendNowPacket(this, (byte) 1);
		}
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttc) {
		super.readFromNBT(nbttc);
		this.working = nbttc.getBoolean("working");
		G_renew_powerConfigure();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttc) {
		super.writeToNBT(nbttc);
		nbttc.setBoolean("working", this.working);
	}

	@Override
	public void G_reinit() {
		this.working = true;
		G_renew_powerConfigure();
		PacketHandler.sendNowPacket(this, (byte) 1);
	}

	@Override
	protected void G_destroy() {
		if (this.worldObj.isRemote) return;
		this.working = false;
		G_renew_powerConfigure();
		PacketHandler.sendNowPacket(this, (byte) 0);
		for (int depth = this.yCoord - 1; depth > 0; depth--) {
			if (this.worldObj.getBlock(this.xCoord, depth, this.zCoord) != BuildCraftFactory.plainPipeBlock) {
				break;
			}
			this.worldObj.setBlockToAir(this.xCoord, depth, this.zCoord);
		}
	}

	@Override
	public boolean isActive() {
		return this.working;
	}
}