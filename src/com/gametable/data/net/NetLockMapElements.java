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
import java.util.ArrayList;
import java.util.List;

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.MapElement;
import com.gametable.data.MapElementID;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Handles network messages requesting to lock map elements
 */
public class NetLockMapElements implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetLockMapElements getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetLockMapElements();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetLockMapElements g_messageType = null;
	
/**
	 * Create a network message requesting to lock all map elements
	 * @param lock True to lock, false to unlock
	 * @return data packet
	 */
	public static byte[] makeLockAllPacket(final boolean lock)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
      dos.writeInt(-1);
      dos.writeBoolean(lock);

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}
	
	/**
	 * Create a network message packet requesting to lock a single map element
	 * @param mapElement Map element to lock or unlock
	 * @param lock True to lock, false to unlock
	 * @return data packet
	 */
	public static byte[] makePacket(MapElement mapElement, final boolean lock)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			dos.writeInt(1);
      dos.writeBoolean(lock);
      dos.writeLong(mapElement.getID().numeric());

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}
	
	/**
	 * Create a network message packet requesting to lock multiple map elements
	 * @param mapElements List of map elements to lock or unlock
	 * @param lock True to lock, false to unlock
	 * @return data packet
	 */
	public static byte[] makePacket(List<MapElement> mapElements, final boolean lock)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			dos.writeInt(mapElements.size());
			dos.writeBoolean(lock);
			
			for (MapElement mapElement : mapElements)
				dos.writeLong(mapElement.getID().numeric());

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
		long qty = dis.readInt();		
    boolean locked = dis.readBoolean();
    
    GameTableCore core = GametableApp.getCore();
    
    if (qty < 1)
    {
    	core.lockAllMapElements(GameTableCore.MapType.PUBLIC, locked, event);
    	return;
    }
    
    List<MapElement> mapElements = new ArrayList<MapElement>();
    
    for (int i = 0; i < qty; i++)
    {
    	MapElementID id = MapElementID.fromNumeric(dis.readLong());
    	MapElement element = core.getMapElement(id);
    	mapElements.add(element);
    }
    
    core.lockMapElements(GameTableCore.MapType.PUBLIC, mapElements, locked, event);
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
