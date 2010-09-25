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
import java.io.StringReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.data.GameTableMap;
import com.galactanet.gametable.data.MapElementID;
import com.galactanet.gametable.data.XMLSerializeConverter;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;
import com.maziade.tools.XMLUtils;

/**
 * Network message to share the public map
 */
public class NetLoadMap implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetLoadMap getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetLoadMap();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetLoadMap g_messageType = null;
	
/**
	 * MapElementID converter when loading a map.
	 * 
	 * Reassigns element IDs that already in use
	 * 
	 * @author Eric Maziade
	 */
	private static class LoadMapSerializeConverter extends XMLSerializeConverter
	{
		/*
		 * @see com.galactanet.gametable.data.XMLSerializeConverter#getMapElementID(long)
		 */
		@Override
		public MapElementID getMapElementID(long savedElementID)
		{
			MapElementID id = super.getMapElementID(savedElementID);
			if (id != null)
				return id;

			id = MapElementID.get(savedElementID);

			// If ID was in use, let us reassign internal ID
			if (id != null)
			{
				id.reassignInternalID();
			}

			id = MapElementID.fromNumeric(savedElementID);
			this.storeMapElementID(savedElementID, id);

			return id;
		}
	}

	/**
	 * Message's private ID
	 */
	private static int		g_id		= 0;
	
	/**
	 * Messagae's unique string representation
	 */
	private static String	g_name	= null;

	/**
	 * Create a network data packet requesting that other players load the specified file as public map
	 * 
	 * @param map Map to share
	 * @return data packet
	 */
	public static byte[] makePacket(GameTableMap map)
	{
		Document doc = null;
		try
		{
			doc = XMLUtils.createDocument();
			Element rootEl = doc.createElement("map");
			doc.appendChild(rootEl);
			map.serialize(rootEl);
		}
		catch (IOException e)
		{
			Log.log(Log.SYS, "Could not create XML document " + e.getMessage());
		}

		if (doc == null)
			return null;

		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			dos.writeUTF(XMLUtils.xmlToString(doc, "UTF-8"));

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
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
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#processData(com.galactanet.gametable.data.net.Connection,
	 * java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		String xml = dis.readUTF();
		Document mapDocument = XMLUtils.parseXMLDocument(new StringReader(xml), null);

		GameTableMap map = GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap();
		map.clearMap();

		LoadMapSerializeConverter converter = new LoadMapSerializeConverter();
		map.deserialize(mapDocument.getDocumentElement(), converter);

		GametableFrame.getGametableFrame().repaint();
	}
	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#setID(int)
	 */
	@Override
	public void setID(int id)
	{
		g_id = id;
	}
}
