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

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.data.GameTableCore.NetworkResponderCore;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.util.Log;

/**
 * Network message to send information about a specific player
 * 
 * @auditedby themaze75
 */
public class NetSendPlayerInfo implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendPlayerInfo getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendPlayerInfo();
		
		return g_messageType;
	}
	
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendPlayerInfo getMessageType(NetworkResponderCore responder)
	{
		NetSendPlayerInfo info = getMessageType();
		info.m_responder = responder;
		
		return info;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendPlayerInfo g_messageType = null;
	
	/**
	 * Create a network data packet
	 * @param player
	 * @param password
	 * @return
	 */
	public static byte[] makePacket(final Player player, final String password)
	{
		try
		{
			NetworkModuleIF module = GameTableCore.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			dos.writeInt(VERSION);
      dos.writeUTF(password);
      dos.writeUTF(player.getCharacterName());
      dos.writeUTF(player.getPlayerName());
      dos.writeBoolean(player.isHostPlayer());

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
    final int version = dis.readInt();
    if (version != VERSION)
    {
    	// Send a message notifying the player that he has been rejected due to a version mismatch and close its connection.
    	
    	sourceConnection.sendPacket(NetLoginRejected.makePacket(NetLoginRejected.RejectReason.VERSION_MISMATCH));
    	sourceConnection.close();
      return;
    }

    // We're player information from the player itself
    
    final String password = dis.readUTF();
    final String characterName = dis.readUTF();
    final String playerName = dis.readUTF();
    final Player newPlayer = new Player(playerName, characterName, -1, false);
    newPlayer.setIsHostPlayer(dis.readBoolean());

    if (m_responder != null)
    	m_responder.onPlayerJoined(sourceConnection, newPlayer, password);
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
	
	private static int VERSION = 1;
	
	/**
	 * Responder interface to communicate with 'hidden' features of the core
	 */
	private NetworkResponderCore m_responder = null; 
}
