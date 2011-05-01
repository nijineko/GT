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

package com.gametable.data.net;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gametable.GametableApp;
import com.gametable.data.net.NetRequestFile.FileName;
import com.gametable.data.net.NetRequestFile.FileRequestInfo;
import com.gametable.net.*;
import com.gametable.util.Log;
import com.gametable.util.UtilityFunctions;

/**
 * Message allowing to send and receive files.  Used by {@link NetRequestFile}, should not be used directly.
 * 
 */
public class NetSendFile implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendFile getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendFile();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendFile g_messageType = null;

	/**
	 * Make a data packet sending a file
	 * @param fileName File identifier
	 * @param File file to send
	 * @return
	 */
	protected static byte[] makeFilePacket(FileName fileName, File file)
	{
		// load the entire file
		final byte[] fileData = UtilityFunctions.loadFileToArray(file);

		if (fileData == null)
		{
			return null;
		}

		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			dos.writeUTF(fileName.fileSource);
			dos.writeUTF(fileName.fileName);

			// now write the data length
			dos.writeLong(file.length());

			// and finally, the data itself
			writeFileToStream(file, dos);

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}
	
	/**
	 * Write file to output stream
	 * @param file File to read
	 * @param out Output stream
	 * @throws IOException
	 */
	private static void writeFileToStream(File file, DataPacketStream out) throws IOException
	{
		final DataInputStream infile = new DataInputStream(new FileInputStream(file));
		final byte[] buffer = new byte[1024];
		
		while (true)
		{
			final int bytesRead = infile.read(buffer);
			if (bytesRead > 0)
			{
				out.write(buffer, 0, bytesRead);
			}
			else
			{
				break;
			}
		}		
	}

	/*
	 * @see com.gametable.data.net.NetworkMessageIF#processData(com.gametable.data.net.Connection,
	 * java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		try
		{
			final FileName fileName = new FileName(dis.readUTF(), dis.readUTF());			

			// read the length of the file data
			final long len = dis.readLong();
			
			// Make sure we're receiving a file we have requested.
			FileRequestInfo req = NetRequestFile.getRequestInfo(fileName);
			if (req == null)
			{
				// This file was not in our request buffer - moving on.
				return;
			}

			File parentDir = req.destinationFile.getParentFile();
			if (!parentDir.exists())
				parentDir.mkdirs();
						
			FileOutputStream out = new FileOutputStream(req.destinationFile);
			long written = 0;
			byte buf[] = new byte[1024];
			
			while (written != len)
			{
				int size = (int)Math.min(buf.length, len - written);				
				dis.readFully(buf, 0, size);
				out.write(buf, 0, size);
				
				written += size;
			}
			
			out.flush();
			out.close();
			
			// Now that we have received the file, we can fulfill pending requests
			fullfilRequest(fileName, req.destinationFile);
			
			// Notify requester
			if (req.listener != null)
				req.listener.onFileRequestReceived(fileName.fileName, req.destinationFile);
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
		}
	}
	
	/**
	 * Fulfill any pending requests for an FQN with the specified file
	 * @param fileName File identifier
	 * @param dataFile Destination data file
	 */
	private void fullfilRequest(FileName fileName, File dataFile)
	{
		FileRequest req = g_unfulfilledRequests.get(fileName);
		if (req == null)
			return;

		// Remove from list right away to prevent potential concurrent issues
		g_unfulfilledRequests.remove(fileName);
		
		byte packet[] = makeFilePacket(fileName, dataFile);
		
		for (NetworkConnectionIF conn : req.connections)
		{
			conn.sendPacket(packet);
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
	
	/**
	 * Add file request to the 'unfulfilled' list to be tried later.
	 * @param filename
	 * @param connection
	 */
  protected static void addUnfulfilledRequest(final String fqn, final NetworkConnectionIF connection)
  {
  	FileRequest req = g_unfulfilledRequests.get(fqn);
  	if (req == null)
  	{
  		req = new FileRequest();
  		g_unfulfilledRequests.put(fqn, req);
  	}

  	if (!req.connections.contains(connection))
  		req.connections.add(connection);
  }

	private static int		g_id		= 0;
	private static String	g_name	= null;
	
  /**
   * A Map of sets of pending incoming requests that could not be fulfilled.
   */
  private static Map<String, FileRequest>     g_unfulfilledRequests     = new HashMap<String, FileRequest>();
  
  /**
   * Holds file request information
   */
  private static class FileRequest
  {
  	/**
  	 * List of connections waiting to receive the file
  	 */
  	private List<NetworkConnectionIF> connections = new ArrayList<NetworkConnectionIF>();
  }
}
