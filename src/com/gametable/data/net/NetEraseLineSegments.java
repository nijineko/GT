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
import com.gametable.data.MapRectangle;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Broadcast a request to erase all line segments within a given rectangle
 * 
 * @auditedby themaze75
 */
public class NetEraseLineSegments implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetEraseLineSegments getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetEraseLineSegments();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetEraseLineSegments g_messageType = null;
	

	/**	
	 * Create a packet to request erasing a given rectangle
   * @param rect Rectangular region of the map to erase
   * @param colorSpecific If true, will erase line segments of matching color
   * @param color Color of the line segments to erase (if colorSpecific is true)
	 * @return
	 */
	public static byte[] makePacket(final MapRectangle rect, final boolean colorSpecific, final int color)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
      dos.writeInt(rect.topLeft.x);
      dos.writeInt(rect.topLeft.y);
      dos.writeInt(rect.width);
      dos.writeInt(rect.height);
      dos.writeBoolean(colorSpecific);
      dos.writeInt(color);

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
    final MapRectangle r = new MapRectangle(
    		new MapCoordinates(dis.readInt(), dis.readInt()),
    		dis.readInt(), dis.readInt());

    final boolean bColorSpecific = dis.readBoolean();
    final int color = dis.readInt();

		// erase the lines
		GametableApp.getCore().getMap(GameTableCore.MapType.PUBLIC).removeLineSegments(r, bColorSpecific, color, event);
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
