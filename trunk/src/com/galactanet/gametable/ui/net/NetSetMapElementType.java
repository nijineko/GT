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

import com.galactanet.gametable.data.*;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.util.Log;

/**
 * Network message for requesting changes in map element type
 */
public class NetSetMapElementType implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSetMapElementType getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSetMapElementType();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSetMapElementType g_messageType = null;
	
/**
	 * Create a network data packet requesting that a map element changes type with the type used by another given map
	 * element
	 * 
	 * @param mapElement Map Element to change
	 * @param mapElementType ID of a map element using the type we want to use
	 * @return data packet
	 */
	public static byte[] makePacket(final MapElement mapElement, final MapElementTypeIF mapElementType)
	{
		try
		{
			NetworkModuleIF module = GameTableCore.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			dos.writeLong(mapElement.getID().numeric());
			dos.writeUTF(mapElementType.getFullyQualifiedName());

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
		GameTableCore core = GameTableCore.getCore();
		MapElementTypeLibrary typeLib = core.getMapElementTypeLibrary();
		
		MapElementID mapElementID = MapElementID.fromNumeric(dis.readLong());
		MapElement mapElement = core.getMapElement(mapElementID);

		String typeFQN = dis.readUTF();		
		MapElementTypeIF type = typeLib.getMapElementType(typeFQN);
		
		if (mapElement != null && type != null)
		{
			mapElement.setMapElementType(type, event);
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
