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

package com.galactanet.gametable.data.net;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.LineSegment;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.util.Log;

/**
 * Networking message to send line segments over the network
 * 
 * @auditedby themaze75
 */
public class NetAddLineSegments implements NetworkMessageTypeIF
{
	/**
	 * Get singleton instance of message type
	 * @return
	 */
	public static NetAddLineSegments getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetAddLineSegments();
		
		return g_messageType;
	}
	
	/**
	 * Private constructor
	 */
	private NetAddLineSegments()
	{
	}
	
	/**
	 * Make a data packet to send a new batch of line segments 
	 * @param lines Line segments
	 * @return data packet
	 */
	public static byte[] makePacket(List<LineSegment> lines)
	{
		try
		{
			NetworkModuleIF module = GameTableCore.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
		  dos.writeInt(lines.size());
		  
		  for (LineSegment line : lines)
		  {
		      line.writeToPacket(dos);
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
	 * Make a data packet to send a new of line segments 
	 * @param line Line segment
	 * @return data packet
	 */
	public static byte[] makePacket(LineSegment line)
	{
		try
		{
			NetworkModuleIF module = GameTableCore.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
      dos.writeInt(1);
      
      line.writeToPacket(dos);

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}
	
	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#processData(com.galactanet.gametable.data.net.Connection, java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
    final int numLines = dis.readInt();
    
    List<LineSegment> lines = new ArrayList<LineSegment>(numLines);
    
    for (int i = 0; i < numLines; i++)
    {
    	lines.add(new LineSegment(dis));
    }

    // tell the model
    final GameTableCore core = GameTableCore.getCore();
    core.getMap(GameTableCore.MapType.PUBLIC).addLineSegments(lines, event);
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
	
	private static int g_id = 0;
	private static String g_name = null;
	private static NetAddLineSegments g_messageType = null;
}
