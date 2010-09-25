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

import java.io.*;
import java.util.*;

import com.galactanet.gametable.data.MapElementTypeIF;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * Message allowing to send and receive images.  Used by {@link NetRequestImage}, should not be used directly.
 * 
 * .todo #Network Enhance for all file types + specialized file types
 */
public class NetSendImage implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetSendImage getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetSendImage();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetSendImage g_messageType = null;
	
protected static byte[] makeImagePacket(String filename)
	{
		// load the entire image file
		final byte[] imageFileData = UtilityFunctions.loadFileToArray(filename);

		if (imageFileData == null)
		{
			return null;
		}

		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());

			// write the file type
			dos.writeUTF("image/image");

			// write the filename
			dos.writeUTF(UtilityFunctions.getUniversalPath(filename));

			// now write the data length
			dos.writeInt(imageFileData.length);

			// and finally, the data itself
			dos.write(imageFileData);

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
		// get the file type
		final String mimeType = dis.readUTF();

		if (mimeType.equals("image/image"))
		{
			// this is an image file
			processImageData(dis);
		}
		else
		{
			Log.log(Log.SYS, "Unsupported file type " + mimeType);
		}
	}

	/**
	 * Process data stream containing image data
	 * @param dis
	 */
	private void processImageData(final DataInputStream dis)
	{
		try
		{
			// the file name
			final String filename = UtilityFunctions.getLocalPath(dis.readUTF());

			// read the length of the image file data
			final int len = dis.readInt();

			// the file itself
			final byte[] imageFile = new byte[len];
			dis.read(imageFile);

			// validate file location
			final File here = new File("").getAbsoluteFile();
			File target = new File(filename).getAbsoluteFile();
			if (!UtilityFunctions.isAncestorFile(here, target))
			{
				GametableFrame.getGametableFrame().getChatPanel().logAlertMessage("Malicious pog path? \"" + filename + "\"");
				final String temp = filename.toLowerCase();
				
				if (temp.contains("underlay"))
				{
					target = new File("underlays" + File.separator + target.getName());
				}
				else if (temp.contains("pog"))
				{
					target = new File("pogs" + File.separator + target.getName());
				}
				else
				{
					GametableFrame.getGametableFrame().getChatPanel().logAlertMessage("Illegal pog path: \"" + filename + "\", aborting transfer.");
					return;
				}
			}

			final File parentDir = target.getParentFile();
			if (!parentDir.exists())
			{
				parentDir.mkdirs();
			}

			// now save out the image file
			final OutputStream os = new BufferedOutputStream(new FileOutputStream(target));
			os.write(imageFile);
			os.flush();
			os.close();

			final MapElementTypeIF pogType = GametableFrame.getGametableFrame().getPogLibrary().getElementType(filename);
			pogType.load();

			// Tell the frame to refresh and the image list (so it gets discovered)
			GametableFrame.getGametableFrame().refreshMapElementList();

			// Ok, now send the file out to any previously unfulfilled requests.
			final File providedFile = new File(filename).getCanonicalFile();
			final Iterator<String> iterator = g_unfulfilledRequests.keySet().iterator();
			byte[] packet = null;
			
			while (iterator.hasNext())
			{
				final String requestedFilename = iterator.next();
				final Set<NetworkConnectionIF> connections = g_unfulfilledRequests.get(requestedFilename);

				if (connections.isEmpty())
				{
					iterator.remove();
					continue;
				}

				final File requestedFile = new File(requestedFilename).getCanonicalFile();
				if (requestedFile.equals(providedFile))
				{
					if (packet == null)
					{
						packet = makeImagePacket(filename);
						if (packet == null)
						{
							// Still can't make packet
							// todo echo failure message to peoples?
							break;
						}
					}

					// send to everyone asking for this file
					for (NetworkConnectionIF connection : connections)
					{
						connection.sendPacket(packet);
					}
				}
			}
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
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
	
  protected static void addUnfulfilledRequest(final String filename, final NetworkConnectionIF connection)
  {
      Set<NetworkConnectionIF> set = g_unfulfilledRequests.get(filename);
      if (set == null)
      {
      	  set = new HashSet<NetworkConnectionIF>();
          g_unfulfilledRequests.put(filename, set);
      }

      set.add(connection);
  }


	private static int		g_id		= 0;
	private static String	g_name	= null;
	
  /**
   * A Map of sets of pending incoming requests that could not be fulfilled.
   */
  private static Map<String, Set<NetworkConnectionIF>>     g_unfulfilledRequests     = new HashMap<String, Set<NetworkConnectionIF>>();
}
