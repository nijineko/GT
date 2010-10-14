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

import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;

/**
 * Network message handling sending text message to the mechanics window
 * 
 * @auditedby themaze75
 */
public class NetSendMechanicsText implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendMechanicsText getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendMechanicsText();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendMechanicsText g_messageType = null;
	
/**
	 * Utility method to create a broadcasting data packet (same as calling makePacket with empty string or null as targetPlayerName) 
	 * @param mechanicsText Text to send to the mechanics window
	 * @return data packet
	 */
	public static byte[] makeBroadcastPacket(String mechanicsText)
	{
		return makePacket("", mechanicsText);
	}
	
	/**
	 * Makes a data packet to send mechanics text targeting a specific player
	 * @param targetPlayerName Specific player or null/empty string to broadcast 
	 * @param mechanicsText Text to send to the mechanics window
	 * @return data packet
	 */
	public static byte[] makePacket(String targetPlayerName, String mechanicsText)
	{
		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			if (targetPlayerName == null)
				targetPlayerName = "";
			
			dos.writeUTF(targetPlayerName);
      dos.writeUTF(mechanicsText);

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
		final String toName = dis.readUTF();
    final String text = dis.readUTF();

    GametableFrame frame = GametableFrame.getGametableFrame();
    
    // Broadcast
    if (toName.equals(""))
    {
    	frame.sendMechanicsMessageLocal(text);
  		return;
    }
    
    // If the current player's name has come up, display text
    if (frame.getMyPlayer().hasName(toName))
		{
			frame.sendMechanicsMessageLocal(text);
		}

    // If hosting, send the message directly to the appropriate player

    if (frame.getNetworkStatus() == NetworkStatus.HOSTING)
    {
    	for (Player player : frame.getPlayers())
			{
				if (player.hasName(toName))
					frame.send(makePacket(toName, text), player.getConnection());
			}
		}
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