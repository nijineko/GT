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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.galactanet.gametable.data.MapElementTypeIF;
import com.galactanet.gametable.data.MapElementTypeLibrary;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.GametableCanvas.BackgroundColor;
import com.galactanet.gametable.util.Log;

/**
 * Network message to communicate a change in map background
 * 
 * @author Eric Maziade
 * 
 * TODO #AUDIT
 */
public class NetSetBackground implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSetBackground getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSetBackground();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSetBackground g_messageType = null;
	
/**
	 * Create a data packet to set the background to a predefined color
	 * 
	 * @param color Desired color
	 * @return data packet
	 */
	public static byte[] makePacket(BackgroundColor color)
	{
		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			dos.writeInt(Type.COLOR.ordinal());
			dos.writeInt(color.ordinal());

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}

	/**
	 * Create a data packet to set the background tile
	 * 
	 * @param elementType
	 * @return data packet
	 */
	public static byte[] makePacket(MapElementTypeIF elementType)
	{
		try
		{
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataOutputStream dos = new DataOutputStream(baos);

			dos.writeInt(g_id); // type
			dos.writeInt(Type.ELEMENT_TYPE.ordinal());
			dos.writeUTF(elementType.getFullyQualifiedName());

			return baos.toByteArray();
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
		int iType = dis.readInt();

		if (iType == Type.ELEMENT_TYPE.ordinal())
		{
			String mapElementTypeFQN = dis.readUTF();
			MapElementTypeIF type = MapElementTypeLibrary.getMasterLibrary().getElementType(mapElementTypeFQN);
			if (type != null)
			{
				GametableFrame.getGametableFrame().changeBackground(type, event);
			}
			else
			{
				Log.log(Log.SYS, "Map element type not found: " + mapElementTypeFQN);
			}
		}
		else
		{
			int colorID = dis.readInt();
			BackgroundColor color = BackgroundColor.fromOrdinal(colorID);

			GametableFrame.getGametableFrame().changeBackground(color, event);
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

	private static int		g_id		= 0;
	private static String	g_name	= null;

	private static enum Type
	{
		COLOR, ELEMENT_TYPE;
	}
}
