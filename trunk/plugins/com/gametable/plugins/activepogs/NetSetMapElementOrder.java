/*
 * Net.java
 * 
 * @created 2010-09-05
 * 
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.gametable.plugins.activepogs;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.gametable.GametableApp;
import com.gametable.data.MapElementID;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Network packet requesting a change in map element ordering
 */
public class NetSetMapElementOrder implements NetworkMessageTypeIF
{
	/**
	 * Private constructor
	 */
	private NetSetMapElementOrder() 
	{		
	}
	
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSetMapElementOrder getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSetMapElementOrder();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSetMapElementOrder g_messageType = null;
	
	/**
	 * Create a network data packet requesting that the map elements order is changed
	 * 
	 * @param orderChanges
	 * @return data packet
	 */
	public static byte[] makePacket(final Map<MapElementID, Long> orderChanges)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			dos.writeInt(orderChanges.size());

			for (Entry<MapElementID, Long> entry : orderChanges.entrySet())
			{
				dos.writeLong(entry.getKey().numeric());
				dos.writeLong(entry.getValue());
			}

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}

	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#processData(com.galactanet.gametable.data.net.Connection,
	 * java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		final int changeCount = dis.readInt();

		final Map<MapElementID, Long> changes = new HashMap<MapElementID, Long>();

		for (int i = 0; i < changeCount; ++i)
		{
			long id = dis.readLong();
			MapElementID mapElementID = MapElementID.fromNumeric(id);

			changes.put(mapElementID, dis.readLong());
		}

		ActivePogsModule.getModule().pogReorderPacketReceived(changes);
	}

	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#getID()
	 */
	@Override
	public int getID()
	{
		return g_id;
	}

	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#getName()
	 */
	@Override
	public String getName()
	{
		if (g_name == null)
			g_name = this.getClass().getSimpleName();

		return g_name;
	}

	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#setID(int)
	 */
	@Override
	public void setID(int id)
	{
		g_id = id;
	}

	private static int		g_id		= 0;
	private static String	g_name	= null;
}
