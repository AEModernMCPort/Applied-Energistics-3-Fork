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

package appeng.misc.tile;


import java.util.EnumSet;

import io.netty.buffer.ByteBuf;

import net.minecraft.util.EnumFacing;

import appeng.core.api.implementations.IPowerChannelState;
import appeng.core.api.implementations.tiles.ICrystalGrowthAccelerator;
import appeng.core.api.util.AECableType;
import appeng.core.api.util.AEPartLocation;
import appeng.core.lib.tile.TileEvent;
import appeng.core.lib.tile.events.TileEventType;
import appeng.core.lib.util.Platform;
import appeng.core.me.api.networking.events.MENetworkEventSubscribe;
import appeng.core.me.api.networking.events.MENetworkPowerStatusChange;
import appeng.core.me.grid.GridAccessException;
import appeng.core.me.tile.AENetworkTile;


public class TileQuartzGrowthAccelerator extends AENetworkTile implements IPowerChannelState, ICrystalGrowthAccelerator
{

	private boolean hasPower = false;

	public TileQuartzGrowthAccelerator()
	{
		this.getProxy().setValidSides( EnumSet.noneOf( EnumFacing.class ) );
		this.getProxy().setFlags();
		this.getProxy().setIdlePowerUsage( 8 );
	}

	@MENetworkEventSubscribe
	public void onPower( final MENetworkPowerStatusChange ch )
	{
		this.markForUpdate();
	}

	@Override
	public AECableType getCableConnectionType( final AEPartLocation dir )
	{
		return AECableType.COVERED;
	}

	@TileEvent( TileEventType.NETWORK_READ )
	public boolean readFromStream_TileQuartzGrowthAccelerator( final ByteBuf data )
	{
		final boolean hadPower = this.isPowered();
		this.setPowered( data.readBoolean() );
		return this.isPowered() != hadPower;
	}

	@TileEvent( TileEventType.NETWORK_WRITE )
	public void writeToStream_TileQuartzGrowthAccelerator( final ByteBuf data )
	{
		try
		{
			data.writeBoolean( this.getProxy().getEnergy().isNetworkPowered() );
		}
		catch( final GridAccessException e )
		{
			data.writeBoolean( false );
		}
	}

	@Override
	public void setOrientation( final EnumFacing inForward, final EnumFacing inUp )
	{
		super.setOrientation( inForward, inUp );
		this.getProxy().setValidSides( EnumSet.of( this.getUp(), this.getUp().getOpposite() ) );
	}

	@Override
	public boolean isPowered()
	{
		if( Platform.isServer() )
		{
			try
			{
				return this.getProxy().getEnergy().isNetworkPowered();
			}
			catch( final GridAccessException e )
			{
				return false;
			}
		}

		return this.hasPower;
	}

	@Override
	public boolean isActive()
	{
		return this.isPowered();
	}

	private void setPowered( final boolean hasPower )
	{
		this.hasPower = hasPower;
	}
}
