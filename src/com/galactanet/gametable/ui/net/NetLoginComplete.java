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

package com.galactanet.gametable.ui.net;

import java.io.DataInputStream;
import java.io.IOException;

import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;
import com.plugins.network.PacketSourceState;

/**
 * Sends a network message telling that we have finished handling a new connection
 */
public class NetLoginComplete implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetLoginComplete getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetLoginComplete();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetLoginComplete g_messageType = null;
	
	/**
	 * Create a network data packet containing message information
	 * @return
	 */
	public static byte[] makePacket()
	{
		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			// there's actually no additional data. Just the info that the login is complete

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
    // there's no data in a login_complete packet.

		// TODO #Networking Not liking a message calling directly in a network module implementation.  Will most likely be cleared when we investigate PacketStourceState
		
		// this packet is never redistributed.
		// all we do in response to this allow pog text
		// highlights. The pogs don't know the difference between
		// inital data and actual player changes.
		PacketSourceState.endHostDump();
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
}
