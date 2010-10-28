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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.data.MapElementID;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.util.Log;

/**
 * Network messages requesting that map element data be set
 * @auditedby themaze75
 */
public class NetSetMapElementData implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSetMapElementData getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSetMapElementData();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSetMapElementData g_messageType = null;
	

	/**
	 * Build a map element message rename packet
	 * 
	 * @param mapElement Map element to change
	 * @param mapElementName Name of the map element (null for no change)
	 * @return data packet
	 */
	public static byte[] makeRenamePacket(MapElement mapElement, String mapElementName)
	{
		return makePacket(mapElement, mapElementName, null, null);
	}

	/**
	 * Build a map element message data packet
	 * 
	 * @param mapElement Map element to change
	 * @param mapElementName Name of the map element (null for no change)
	 * @param setAttributes List of attributes to set (null to set no elements)
	 * @param removeAttributes List of attributes to remove (null to remove no elements)
	 * @return Data packet
	 */
	public static byte[] makePacket(MapElement mapElement, String mapElementName, Map<String, String> setAttributes, List<String> removeAttributes)
	{
		try
		{
			NetworkModuleIF module = GameTableCore.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			// Map element ID
			dos.writeLong(mapElement.getID().numeric());

			// Map element name (first boolean decides if we set name)
			if (mapElementName != null)
			{
				dos.writeBoolean(true);
				dos.writeUTF(mapElementName);
			}
			else
			{
				dos.writeBoolean(false);
			}

			// Attributes to remove
			if (removeAttributes == null)
			{
				dos.writeInt(0);
			}
			else
			{
				dos.writeInt(removeAttributes.size());
				for (String key : removeAttributes)
				{
					dos.writeUTF(key);
				}
			}

			// Attributes to add
			if (setAttributes == null)
			{
				dos.writeInt(0);
			}
			else
			{
				dos.writeInt(setAttributes.size());
				for (Entry<String, String> entry : setAttributes.entrySet())
				{
					dos.writeUTF(entry.getKey());
					dos.writeUTF(entry.getValue());
				}
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
		// Map element ID
		long id = dis.readLong();
		MapElementID mapElementID = MapElementID.fromNumeric(id);
		
		// Name
		String name = null;
		if (dis.readBoolean())
			name = dis.readUTF();

		// Delete list
		final List<String> removeAttributes = new ArrayList<String>();
		final int numToDelete = dis.readInt();
		for (int i = 0; i < numToDelete; i++)
		{
			removeAttributes.add(dis.readUTF());
		}

		// Set list
		final Map<String, String> setAttributes = new HashMap<String, String>();
		final int numToAdd = dis.readInt();
		for (int i = 0; i < numToAdd; i++)
		{
			final String key = dis.readUTF();
			final String value = dis.readUTF();
			
			setAttributes.put(key, value);
		}
		
		// Tell the model
		MapElement mapElement = GameTableCore.getCore().getMapElement(mapElementID);
		if (mapElement != null)
		{
			if (name != null)
				mapElement.setName(name, event);
			
			if (setAttributes.size() > 0)
				mapElement.setAttributes(setAttributes, event);
			
			if (removeAttributes.size() > 0)
				mapElement.removeAttributes(removeAttributes, event);
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
