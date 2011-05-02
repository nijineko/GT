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

package com.gametable.plugins.cards;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import com.gametable.GametableApp;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Send the list of available decks
 *
 */
public class NetSendDeckList implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendDeckList getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendDeckList();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendDeckList g_messageType = null;
	
	public static byte[] makePacket(final List<Deck> decks)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			dos.writeInt(decks.size()); // number of decks
			
      for (Deck deck : decks)
      {
      	dos.writeUTF(deck.m_name); // the name of this deck
      }

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
    final int numDecks = dis.readInt();
    final String[] deckNames = new String[numDecks];

    for (int i = 0; i < deckNames.length; i++)
    {
        deckNames[i] = dis.readUTF();
    }

    // tell the model
    CardModule.getModule().deckListPacketReceived(deckNames);
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
