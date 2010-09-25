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
import java.util.HashSet;
import java.util.Set;

import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * Messages allowing to request images from the host
 * .todo #Network Enhance for all file types + specialized file types
 *
 */
public class NetRequestImage implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetRequestImage getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetRequestImage();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetRequestImage g_messageType = null;
	
/**
	 * Request an image file for a map element. The image will be queued for request.
	 * 
	 * @param conn Connection where to send the request.
	 * @param mapElement Map element for which to request an image (usually the host)
	 */
	public static void requestMapElementImageFile(final NetworkConnectionIF conn, final MapElement mapElement)
	{
		final String desiredFile = mapElement.getMapElementType().getFullyQualifiedName();

		// Exit if image already in queue,
		if (g_requestedFiles.contains(desiredFile))
			return;

		// Add to the request queue
		g_requestedFiles.add(desiredFile);

		// Validate network connection
		if (conn == null)
			return;

		// Send an image request to the host
		conn.sendPacket(makePacket(desiredFile));
	}

	/**
	 * Build a data packet to request a file from the network
	 * 
	 * @param filename Requested file name
	 * @return Data packet
	 */
	protected static byte[] makePacket(String filename)
	{
		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			dos.writeUTF(UtilityFunctions.getUniversalPath(filename));

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
		// We received a file request
		final String filename = UtilityFunctions.getLocalPath(dis.readUTF());

		// We will build a data packet and send an image packet back to the requester
		final byte[] packet = NetSendImage.makeImagePacket(filename);
		if (packet != null)
			sourceConnection.sendPacket(packet);
		else
		{
			// If we somehow cannot build the packet, we will add it to the 'unfulfilled' list of our sister network message,
			// so it can be tried again later.
			NetSendImage.addUnfulfilledRequest(filename, sourceConnection);
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

	private static int					g_id							= 0;
	private static String				g_name						= null;

	/**
	 * Set of files queued for request from the network todo: Add some kind of timed retry feature.
	 */
	private static Set<String>	g_requestedFiles	= new HashSet<String>();
}
