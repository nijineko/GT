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

package com.gametable.plugins.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.ChatEngineIF.MessageType;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Network message to keep the connection alive
 */
public class NetSendDictionary implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendDictionary getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendDictionary();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendDictionary g_messageType = null;
	
	/**
	 * Private constructor 
	 */
	private NetSendDictionary()
	{
	}
	
	/**
	 * Create a dictionary packet
	 * @return
	 */
	protected static byte[] makePacket()
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			Collection<NetworkMessageTypeIF> types = ((NetworkModule)module).getRegisteredMessageTypes();
			
			dos.writeInt(types.size());
			
			for (NetworkMessageTypeIF type : types)
			{
				dos.writeInt(type.getID());
				dos.writeUTF(type.getName());
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
		GameTableCore core = GametableApp.getCore(); 
		NetworkModule module = (NetworkModule)core.getNetworkModule();
		Collection<NetworkMessageTypeIF> registeredTypes = module.getRegisteredMessageTypes();
		Map<Integer, NetworkMessageTypeIF> mapping = new HashMap<Integer, NetworkMessageTypeIF>();
		
		int size = dis.readInt();
		for (int i = 0; i < size; i++)
		{
			boolean mapped = false;
			int id = dis.readInt();
			String name = dis.readUTF();
			
			for (NetworkMessageTypeIF type : registeredTypes)
			{
				if (type.getName().equals(name))
				{
					mapping.put(id, type);
					mapped = true;
					break;
				}
			}
			
			if (!mapped)
			{
				core.sendMessageLocal(MessageType.ALERT, "Host uses missing " + name);
				Log.log(Log.NET, "Host uses missing " + name);
				// .todo Kick out for missing modules?
			}
		}
		
		module.setMapping(mapping, sourceConnection);
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
