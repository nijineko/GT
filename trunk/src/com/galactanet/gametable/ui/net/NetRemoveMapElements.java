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

package com.galactanet.gametable.ui.net;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import com.galactanet.gametable.data.GameTableMap;
import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.data.MapElementID;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;

/**
 * Network messages handling the removal of map elements
 */
public class NetRemoveMapElements implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetRemoveMapElements getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetRemoveMapElements();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetRemoveMapElements g_messageType = null;
	
/**
	 * Create a network data packet requesting that specific map elements be removed from the game
	 * 
	 * @param mapElements List of map elements to remove
	 * @return data packet
	 */
	public static byte[] makePacket(List<MapElement> mapElements)
	{
		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			// Number of map elements to remove
			dos.writeInt(mapElements.size());

			// Map element IDs
			for (MapElement mapElement : mapElements)
			{
				dos.writeLong(mapElement.getID().numeric());
			}

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}

	/**
	 * Create a network data packet requesting that specific map elements be removed from the game
	 * 
	 * @param mapElementIDs Array of map elements to remove
	 * @return data packet
	 */
	public static byte[] makePacket(final MapElementID mapElementIDs[])
	{
		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			// Number of map elements to remove
			dos.writeInt(mapElementIDs.length);

			// Map element IDs
			for (MapElementID id : mapElementIDs)
			{
				dos.writeLong(id.numeric());
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
		final GametableFrame frame = GametableFrame.getGametableFrame();
		GameTableMap map = frame.getGametableCanvas().getActiveMap();
		
		// Array of map element IDs to remove
		final int mapElementCount  = dis.readInt();

		// Load them in
		for (int i = 0; i < mapElementCount; i++)
		{
			long id = dis.readLong();
			MapElementID mapElementID = MapElementID.fromNumeric(id);

			// TODO !#Grouping @revise automatic removal from group should be centralized in DATA
			MapElement mapElement = map.getMapElement(mapElementID);
			if (mapElement != null)
			{
				map.removeMapElementInstance(mapElement);
			}
		}
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
