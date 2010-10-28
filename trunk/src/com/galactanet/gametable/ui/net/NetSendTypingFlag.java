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

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame.NetworkFrameResponder;
import com.galactanet.gametable.util.Log;

/**
 * Network message handling keeping tabs on who is currently typing
 * 
 * @auditedby themaze75
 */
public class NetSendTypingFlag implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendTypingFlag getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendTypingFlag();
		
		return g_messageType;
	}
	
	/**
	 * Singleton factory method
	 * @param responder Instance of NetworkResponder, called by GametableFrame only 
	 * @return
	 */
	public static NetSendTypingFlag getMessageType(NetworkFrameResponder responder)
	{
		getMessageType();		
		g_messageType.m_responder = responder;
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendTypingFlag g_messageType = null;
	
/**
	 * Make a network data packet informing that a player is currently typing in the chat window
	 * 
	 * @param player player
	 * @param typing True if currently typing, false otherwise
	 * @return data packet
	 */
	public static byte[] makePacket(Player player, final boolean typing)
	{
		try
		{
			NetworkModuleIF module = GameTableCore.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			dos.writeInt(player.getID());
			dos.writeBoolean(typing);

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
		int playerID = dis.readInt();
		boolean typing = dis.readBoolean();

		if (m_responder != null)
			m_responder.updateTypingStatus(playerID, typing);
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
	private NetworkFrameResponder m_responder = null;
}
