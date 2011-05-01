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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.gametable.GametableApp;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Messages allowing to request files from the host
 */
public class NetRequestFile implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetRequestFile getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetRequestFile();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetRequestFile g_messageType = null;

	/**
	 * Request a file from a given network connection
	 * @param conn Connection through which to request the file
	 * @param fileSource Unique name identifying the file source (see registerFileSource)  
	 * @param fileName Name identifying the file within the specified file source
	 * @param destination Destination file for the received data
	 * @param listener Interface to receive notification of file delivery.
	 */
	public static void requestFile(NetworkConnectionIF conn, String fileSource, String fileName, File destination, FileRequestListenerIF listener)
	{
		// Exit if file already in queue,
		if (g_requestedFiles.containsKey(fileName))
			return;

		// Add to the request queue
		FileRequestInfo req = new FileRequestInfo();

		req.fileName = new FileName(fileSource, fileName);
		req.destinationFile = destination;
		req.listener = listener;
		
		g_requestedFiles.put(req.fileName, req);

		// Validate network connection
		if (conn == null)
			return;

		// Send an file request to the host
		conn.sendPacket(makePacket(req.fileName));
	}

	/**
	 * Build a data packet to request a file from the network
	 * 
	 * @param fileName Identifies a file request
	 * @return Data packet
	 */
	protected static byte[] makePacket(FileName fileName)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			dos.writeUTF(fileName.fileSource);
			dos.writeUTF(fileName.fileName);
			
			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}

	/*
	 * @see com.gametable.data.net.NetworkMessageIF#processData(com.gametable.data.net.Connection,
	 * java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		// We received a file request
		FileName fileName = new FileName(dis.readUTF(), dis.readUTF());

		File file = getFileFromFileName(fileName);

		// We will build a data packet and send a file packet back to the requester
		byte[] packet = null;
		
		if (file != null)
				packet = NetSendFile.makeFilePacket(fileName, file);
		
		if (packet != null)
		{
			sourceConnection.sendPacket(packet);
		}
		else
		{
			// If we somehow cannot build the packet, we will ask 'SendFile' to send the file back to the requester if we eventually receive the file
			NetSendFile.addUnfulfilledRequest(fileName.fileName, sourceConnection);
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
	 * Register an interface as a file source
	 * @param fqnPart FQN prefix uniquely identifying the file source.
	 * @param source
	 */
	public static void registerFileSource(String fqnPart, FileSourceIF source)
	{
		g_fqnSources.put(fqnPart, source);
	}
	
	/**
	 * Uses a FileName to retrieve a file from the registering module
	 * @param fileName File identifier
	 * @return File or null
	 */
	protected File getFileFromFileName(FileName fileName)
	{
		FileSourceIF source = g_fqnSources.get(fileName.fileSource);
		
		if (source == null)
		{
			Log.log(Log.SYS, "No file source registerd in NetRequestFile for " + fileName.fileSource);
			return null;
		}
		
		File file = source.getFile(fileName.fileSource, fileName.fileName);
		
		return file;
	}
	
	/**
	 * Map of registered FQN sources
	 */
	private static Map<String, FileSourceIF> g_fqnSources = new HashMap<String, FileSourceIF>();
	
	/**
	 * Interface to retrieve files from file sources
	 */
	public interface FileSourceIF
	{
		/**
		 * Retrieves a file from the file source
		 * @param fileSource String identifying the file source
		 * @param fileName String identifying the file within the source
		 * @return Valid file or null
		 */
		public File getFile(String fileSource, String fileName);
	}

	/**
	 * Message ID 
	 */
	private static int					g_id							= 0;
	
	/**
	 * Message name 
	 */
	private static String				g_name						= null;

	/**
	 * Files queued for request.  A security measure to make sure we're not receiving files we have not requested.
	 * Key = Unique, fully qualified name
	 * File = Requested write destination
	 */
	private static Map<FileName, FileRequestInfo> g_requestedFiles	= new HashMap<FileName, FileRequestInfo>();
	
	/**
	 * Get a file request info based on a file name
	 * @param fileName
	 * @return
	 */
	protected static FileRequestInfo getRequestInfo(FileName fileName)
	{
		return g_requestedFiles.get(fileName);
	}
	
	/**
	 * Structure holding file request information
	 */
	protected static class FileRequestInfo 
	{
		/**
		 * Request's file name
		 */
		protected FileName fileName;
		
		/**
		 * Destination file
		 */
		protected File destinationFile;
		
		/**
		 * Listener to notify upon reception
		 */
		protected FileRequestListenerIF listener;
	}
	
	/**
	 * Interface to receive delivery notifications
	 */
	public interface FileRequestListenerIF
	{
		/**
		 * Called when the requested file has been received
		 * @param fileName Name of the file within its data source
		 * @param file Pointer to the received file (normally at the requested destination)
		 */
		public void onFileRequestReceived(String fileName, File file);
	}
	
	/**
	 * Holds file name information
	 */
	protected static class FileName
	{
		/**
		 * 
		 * @param source
		 * @param name
		 */
		public FileName(String source, String name)
		{
			fileName = name;
			fileSource = source;
			internal = fileName + ">" + fileSource;
		}
		
		/**
		 * Name within source
		 */
		public final String fileName;
		
		/**
		 * Source name
		 */
		public final String fileSource;
		
		/**
		 * Internal representation (for hashing)
		 */
		private final String internal;
		
		/*
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof FileName)
			{				
				FileName fn = (FileName)obj;
				return (fn.internal.equals(internal));
			}
			
			return super.equals(obj);
		}
		
		/*
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{			
			return internal.hashCode();
		}
	}
}
