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
 * Network message handling sending text message to the mechanics window
 * @auditedby themaze75
 */
public class NetSendChatText implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendChatText getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendChatText();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendChatText g_messageType = null;
	
/**
	 * Utility method to create a broadcasting data packet  
	 * @param msgType Message type
	 * @param text Text to send to the chat window
	 * @return data packet
	 */
	public static byte[] makeBroadcastPacket(MessageType msgType, String text)
	{
		return makePacket(msgType, null, text);
	}
	
	/**
	 * Makes a data packet to send mechanics text targeting a specific player
	 * @param msgType Message type
	 * @param targetPlayer Specifies the target player or null/empty string to broadcast 
	 * @param text Text to send to the chat window
	 * @return data packet
	 */
	public static byte[] makePacket(MessageType msgType, Player targetPlayer, String text)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			if (text == null)
				text = "";
			
			dos.writeInt(msgType.ordinal());

			if (targetPlayer == null)
				dos.writeInt(-1);
			else
				dos.writeInt(targetPlayer.getID());
			
      dos.writeUTF(text);

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
    
    GameTableCore core = GametableApp.getCore();

    // Message type
		MessageType msgType = MessageType.fromOrdinal(dis.readInt());
		if (msgType == null)
			msgType = MessageType.CHAT;
		
		// Player
		int playerID = dis.readInt();
		Player toPlayer = null;
		if (playerID > 0)
			toPlayer = core.getPlayer(playerID);
		
    final String text = dis.readUTF();
    
    // Broadcast
    if (toPlayer == null)
    {
    	core.sendMessageLocal(msgType, text);
  		return;
    }
    
    // If the current player's name has come up, display text
    if (toPlayer.equals(core.getPlayer()))
		{
    	core.sendMessage(msgType, toPlayer, text);
			return;
		}

    // If hosting, dispatch the message directly to the appropriate player - helps prevent useless broadcasting
    if (!event.isBroadcast())
    {
	    if (core.getNetworkStatus() == NetworkStatus.HOSTING)
	    {
	    	for (Player player : core.getPlayers())
				{
					if (player.equals(toPlayer))
						core.send(makePacket(msgType, toPlayer, text), player.getConnection());
				}
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
