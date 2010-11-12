/*
 * MsgAddMapElement.java
 *
 * @created 2010-08-30
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

package com.galactanet.gametable.data.net;

import java.io.DataInputStream;
import java.io.IOException;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.GameTableMap;
import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.data.MapElementTypeIF;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.util.Log;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class NetAddMapElement implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetAddMapElement getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetAddMapElement();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetAddMapElement g_messageType = null;
	
	/**
	 * Create a data packet to add an element to the public layer
	 * @param mapElement Map element to add
	 * @return data packet
	 */
	public static byte[] makePacket(MapElement mapElement)
  {
		return makePacket(mapElement, true);
  }
	
	/**
	 * Create a data packet to add an element to a specified layer
	 * @param mapElement Map element to add
	 * @param publicLayer True if element is to be added to public layer
	 * @return data packet
	 */
	public static byte[] makePacket(MapElement mapElement, boolean publicLayer)
  {
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			dos.writeBoolean(publicLayer); // layer
			mapElement.writeToPacket(dos);

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
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
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#processData(java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		boolean addToPublicLayer = dis.readBoolean(); 

		final MapElement element = new MapElement(dis);
		
		if (element.isCorrupted())
		{
			// for one reason or another, this element is corrupt and should be ignored
			return;
		}

		// If map element is not loaded, we'll need to request it
		MapElementTypeIF type = element.getMapElementType();
		if (!type.isLoaded())
		{
			type.loadDataFromNetwork(sourceConnection);
		}

		// Have the model react
		GameTableCore core = GametableApp.getCore();
		GameTableMap map = core.getMap(addToPublicLayer ? GameTableCore.MapType.PUBLIC : GameTableCore.MapType.PRIVATE);
		
		map.addMapElement(element, event);
	}
	
	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#setID(int)
	 */
	@Override
	public void setID(int id)
	{
		g_id = id;		
	}
	
	private static int g_id = 0;
	private static String g_name = null;	
}
