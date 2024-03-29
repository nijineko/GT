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

package com.gametable.ui.net;

import java.io.DataInputStream;
import java.io.IOException;

import com.gametable.GametableApp;
import com.gametable.data.MapCoordinates;
import com.gametable.net.*;
import com.gametable.ui.GametableFrame;
import com.gametable.ui.GametableCanvas.ZoomLevel;
import com.gametable.util.Log;

/**
 * Network message allowing to move the map for players
 * 
 * @auditedby themaze75
 */
public class NetRecenterMap implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetRecenterMap getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetRecenterMap();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetRecenterMap g_messageType = null;
	

	/**
	 * Create a network data packet requesting that the map be centered and zoomed at the specified location 
	 * @param mapCenter New map center 
	 * @param zoom Zoom level
	 * @return data packet
	 */
	public static byte[] makePacket(MapCoordinates mapCenter, final ZoomLevel zoom)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			dos.writeInt(mapCenter.x);
      dos.writeInt(mapCenter.y);
      dos.writeUTF(zoom.name());

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
		GametableFrame frame = GametableApp.getUserInterface();
		if (frame == null)
			return;
		
		MapCoordinates pos = new MapCoordinates(dis.readInt(), dis.readInt());
		String zoomName = dis.readUTF();
		ZoomLevel zoom = ZoomLevel.LEVEL1;
		
		try
		{
			if (zoomName != null)
				zoom = ZoomLevel.valueOf(zoomName);
		}
		catch (IllegalArgumentException e)
		{
			// Ignore - keep default zoom level value
		}

    // tell the model
    frame.centerView(pos, zoom, event);
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
