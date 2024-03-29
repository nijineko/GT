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
import com.gametable.data.Player;
import com.gametable.data.ChatEngineIF.MessageType;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Network message to broadcast information updates about a player
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
	 * Singleton instance
	 */
	private static NetSendPlayerInfo g_messageType = null;
	
	/**
	 * Create a network data packet
	 * @param player Player object containing information
	 * @return data packet
	 */
	public static byte[] makePacket(final Player player)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
      dos.writeInt(player.getID());
      dos.writeUTF(player.getCharacterName());
      dos.writeUTF(player.getPlayerName());

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
		int playerID = dis.readInt();
		String playerName = dis.readUTF();
		String characterName = dis.readUTF();
		
		GameTableCore core = GametableApp.getCore();
		Player player = core.getPlayer(playerID);
		
		if (player != null)
		{
			String message = player.getCharacterName() + "(" + player.getPlayerName() + ") is now " + characterName + " (" + playerName + ")";
			core.sendMessageLocal(MessageType.ALERT, message);
			Log.log(Log.PLAY, message);
			
			if (player == core.getPlayer())
				core.setPlayerInformation(playerName, characterName, event);
			else
			{
				player.setCharacterName(characterName);
				player.setPlayerName(playerName);
			}
		}
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
