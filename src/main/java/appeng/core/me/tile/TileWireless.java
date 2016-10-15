/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.core.me.tile;


import java.util.EnumSet;

import io.netty.buffer.ByteBuf;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import appeng.core.api.AEApi;
import appeng.core.api.implementations.IPowerChannelState;
import appeng.core.api.implementations.tiles.IWirelessAccessPoint;
import appeng.core.api.util.AECableType;
import appeng.core.api.util.AEPartLocation;
import appeng.core.api.util.DimensionalCoord;
import appeng.core.lib.AEConfig;
import appeng.core.lib.tile.TileEvent;
import appeng.core.lib.tile.events.TileEventType;
import appeng.core.lib.tile.inventory.AppEngInternalInventory;
import appeng.core.lib.tile.inventory.InvOperation;
import appeng.core.lib.util.Platform;
import appeng.core.me.api.networking.GridFlags;
import appeng.core.me.api.networking.IGrid;
import appeng.core.me.api.networking.events.MENetworkChannelsChanged;
import appeng.core.me.api.networking.events.MENetworkEventSubscribe;
import appeng.core.me.api.networking.events.MENetworkPowerStatusChange;
import appeng.core.me.grid.GridAccessException;


public class TileWireless extends AENetworkInvTile implements IWirelessAccessPoint, IPowerChannelState
{

	public static final int POWERED_FLAG = 1;
	public static final int CHANNEL_FLAG = 2;

	private final int[] sides = { 0 };
	private final AppEngInternalInventory inv = new AppEngInternalInventory( this, 1 );

	private int clientFlags = 0;

	public TileWireless()
	{
		this.getProxy().setFlags( GridFlags.REQUIRE_CHANNEL );
		this.getProxy().setValidSides( EnumSet.noneOf( EnumFacing.class ) );
	}

	@Override
	public void setOrientation( final EnumFacing inForward, final EnumFacing inUp )
	{
		super.setOrientation( inForward, inUp );
		this.getProxy().setValidSides( EnumSet.of( this.getForward().getOpposite() ) );
	}

	@MENetworkEventSubscribe
	public void chanRender( final MENetworkChannelsChanged c )
	{
		this.markForUpdate();
	}

	@MENetworkEventSubscribe
	public void powerRender( final MENetworkPowerStatusChange c )
	{
		this.markForUpdate();
	}

	@TileEvent( TileEventType.NETWORK_READ )
	public boolean readFromStream_TileWireless( final ByteBuf data )
	{
		final int old = this.getClientFlags();
		this.setClientFlags( data.readByte() );

		return old != this.getClientFlags();
	}

	@TileEvent( TileEventType.NETWORK_WRITE )
	public void writeToStream_TileWireless( final ByteBuf data )
	{
		this.setClientFlags( 0 );

		try
		{
			if( this.getProxy().getEnergy().isNetworkPowered() )
			{
				this.setClientFlags( this.getClientFlags() | POWERED_FLAG );
			}

			if( this.getProxy().getNode().meetsChannelRequirements() )
			{
				this.setClientFlags( this.getClientFlags() | CHANNEL_FLAG );
			}
		}
		catch( final GridAccessException e )
		{
			// meh
		}

		data.writeByte( (byte) this.getClientFlags() );
	}

	@Override
	public AECableType getCableConnectionType( final AEPartLocation dir )
	{
		return AECableType.SMART;
	}

	@Override
	public DimensionalCoord getLocation()
	{
		return new DimensionalCoord( this );
	}

	@Override
	public IInventory getInternalInventory()
	{
		return this.inv;
	}

	@Override
	public boolean isItemValidForSlot( final int i, final ItemStack itemstack )
	{
		return AEApi.instance().definitions().materials().wirelessBooster().isSameAs( itemstack );
	}

	@Override
	public void onChangeInventory( final IInventory inv, final int slot, final InvOperation mc, final ItemStack removed, final ItemStack added )
	{
		// :P
	}

	@Override
	public int[] getAccessibleSlotsBySide( final EnumFacing side )
	{
		return this.sides;
	}

	@Override
	public void onReady()
	{
		this.updatePower();
		super.onReady();
	}

	private void updatePower()
	{
		this.getProxy().setIdlePowerUsage( AEConfig.instance.wireless_getPowerDrain( this.getBoosters() ) );
	}

	private int getBoosters()
	{
		final ItemStack boosters = this.inv.getStackInSlot( 0 );
		return boosters == null ? 0 : boosters.stackSize;
	}

	@Override
	public void markDirty()
	{
		this.updatePower();
	}

	@Override
	public double getRange()
	{
		return AEConfig.instance.wireless_getMaxRange( this.getBoosters() );
	}

	@Override
	public boolean isActive()
	{
		if( Platform.isClient() )
		{
			return this.isPowered() && ( CHANNEL_FLAG == ( this.getClientFlags() & CHANNEL_FLAG ) );
		}

		return this.getProxy().isActive();
	}

	@Override
	public IGrid getGrid()
	{
		try
		{
			return this.getProxy().getGrid();
		}
		catch( final GridAccessException e )
		{
			return null;
		}
	}

	@Override
	public boolean isPowered()
	{
		return POWERED_FLAG == ( this.getClientFlags() & POWERED_FLAG );
	}

	public int getClientFlags()
	{
		return this.clientFlags;
	}

	private void setClientFlags( final int clientFlags )
	{
		this.clientFlags = clientFlags;
	}
}
