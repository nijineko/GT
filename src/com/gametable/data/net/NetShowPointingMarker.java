/*
 * Net.java
 *
 * @created 2010-09-05
 *
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.gametable.data.net;

import java.io.DataInputStream;
import java.io.IOException;

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.MapCoordinates;
import com.gametable.data.Player;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Network message handling showing and hiding the player map pointers
 * 
 * @auditedby themaze75
 */
public class NetShowPointingMarker implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetShowPointingMarker getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetShowPointingMarker();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetShowPointingMarker g_messageType = null;
	
/**
	 * Create a network data packet requesting that the pointer be shown or hidden
	 * @param player Player showing or hiding his pointer
	 * @param point The coordinates at which the pointer is to be shown
	 * @param showPointer True to show the pointer, false to hide it
	 * @return data packet
	 */
	public static byte[] makePacket(Player player, MapCoordinates point, boolean showPointer)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
      dos.writeInt(player.getID());
      dos.writeInt(point.x);
      dos.writeInt(point.y);
      dos.writeBoolean(showPointer);

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}
	
	/*
	 * @see com.gametable.data.net.NetworkMessageIF#processData(com.gametable.data.net.Connection, java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		final int playerID = dis.readInt();

    MapCoordinates point = new MapCoordinates(dis.readInt(), dis.readInt());
    
    final boolean showPointer = dis.readBoolean();

    final GameTableCore core = GametableApp.getCore();
    
    // Do not show current player's pointer
		if (playerID != core.getPlayerID())
		{
			final Player player = core.getPlayer(playerID);
			
			if (player != null)
				player.setPointing(showPointer, point, event);
		}
	}
		
	/*
	 * @see com.gametable.data.net.NetworkMessageIF#getID()
	 */
	@Override
	public int getID()
	{
		return g_id;
	}
	
	/*
	 * @see com.gametable.data.net.NetworkMessageIF#getName()
	 */
	@Override
	public String getName()
	{
		if (g_name == null)
			g_name = this.getClass().getSimpleName();
		
		return g_name;
	}

	/*
	 * @see com.gametable.data.net.NetworkMessageIF#setID(int)
	 */
	@Override
	public void setID(int id)
	{
		g_id = id;		
	}
	
	private static int g_id = 0;
	private static String g_name = null;	
}
