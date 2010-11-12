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

package com.galactanet.gametable.data.net;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.data.GameTableCore.NetworkResponderCore;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.util.Log;

/**
 * Network message handling the distribution of the player list 
 * 
 * @auditedby themaze75
 */
public class NetSendPlayersList implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendPlayersList getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendPlayersList();
		
		return g_messageType;
	}
	
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendPlayersList getMessageType(NetworkResponderCore responder)
	{
		NetSendPlayersList info = getMessageType();
		info.m_responder = responder;
		
		return info;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendPlayersList g_messageType = null;
	
/**
	 * Create the data packet for sending information about all players, telling the recipient which one contains his own
	 * information
	 * 
	 * @param recipient Player to whom we're sending the packet
	 * @return data packet
	 */
	public static byte[] makePacket(Player recipient)
	{
		try
		{
			// create a packet with all the players in it
			final GameTableCore core = GametableApp.getCore();
			
			NetworkModuleIF module = core.getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			final List<Player> players = core.getPlayers();
			dos.writeInt(players.size());

			for (Player player : players)
			{
				dos.writeUTF(player.getCharacterName());
				dos.writeUTF(player.getPlayerName());
				dos.writeInt(player.getID());
				dos.writeBoolean(player.isHostPlayer());
			}

			// finally, tell the recipient which player he is
			dos.writeInt(recipient.getID());

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
		final int numPlayers = dis.readInt();
		final Player[] players = new Player[numPlayers];
		
		for (int i = 0; i < numPlayers; i++)
		{
			final String charName = dis.readUTF();
			final String playerName = dis.readUTF();
			final int playerID = dis.readInt();
			players[i] = new Player(playerName, charName, playerID, dis.readBoolean());
		}

		// get which ID we are
		int ourPlayerID = dis.readInt();

		// this is only ever received by players
		if (m_responder != null)
			m_responder.setPlayersInformation(players, ourPlayerID);
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
	
	/**
	 * Responder interface to communicate with 'hidden' features of the core
	 */
	private NetworkResponderCore m_responder = null;
}
