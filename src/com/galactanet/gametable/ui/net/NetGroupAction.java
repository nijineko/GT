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

import javax.naming.InvalidNameException;

import com.galactanet.gametable.data.*;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;

/**
 * Broadcast an action on group
 *
 * @author Eric Maziade
 */
public class NetGroupAction implements NetworkMessageTypeIF
{
	/**
	 * Singleton factory method
	 * @return
	 */
	public static NetGroupAction getMessageType()
	{
		if (g_messageType == null)
			g_messageType = new NetGroupAction();
		
		return g_messageType;
	}
	
	/**
	 * Singleton instance
	 */
	private static NetGroupAction g_messageType = null;
	
	/**
	 * Describes an action that can be performed on a group
	 */
	public static enum Action
	{
		ADD_ELEMENT, DELETE, NEW, REMOVE_ELEMENT, RENAME;

		/**
		 * Get ActionType from ordinal value
		 * 
		 * @param ord
		 * @return
		 */
		public static Action fromOrdinal(int ord)
		{
			for (Action t : Action.values())
			{
				if (t.ordinal() == ord)
					return t;
			}

			return null;
		}
	}
	
	/**
	 * Build a message describing an action to take on groups
	 * @param action Action to perform
	 * @param goupName Group name to perform action upon
	 * @param newGroupName New group name (if rename action)
	 * @param mapElementID Map Element ID (if add or remove actions) 
	 * @param playerID Source player sending the message
	 * @return Data packet
	 */
	public static byte[] makePacket(Action action, String goupName, String newGroupName, MapElementID mapElementID, int playerID)
	{
		try
		{
			NetworkModuleIF module = GametableFrame.getGametableFrame().getNetworkModule();
			DataPacketStream dos = module.createDataPacketStream(getMessageType());
			
			dos.writeInt(action.ordinal());
      if(goupName == null) 
      	dos.writeUTF("");
      else 
      	dos.writeUTF(goupName);
     
      if (action == Action.RENAME)
      {
	      if (newGroupName == null)
	      	dos.writeUTF("");
	      else
	      	dos.writeUTF(newGroupName);
      }
      else
      {      
	      if (mapElementID == null)
	      	dos.writeLong(0);
	     	else
	     		dos.writeLong(mapElementID.numeric());
      }
      
      dos.writeInt(playerID);

			return dos.toByteArray();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			return null;
		}
	}
	
  
  /**
   * Convenience method to craete a rename packet
   * @param groupName Old group name
   * @param newGroupName New group name
   * @param playerID player ID 
   * @return
   */
  public static byte[] makeRenamePacket(final String groupName, final String newGroupName, final int playerID) 
  {
  	return makePacket(Action.RENAME, groupName, newGroupName, null, playerID);
  }
	
	/*
	 * @see com.galactanet.gametable.data.net.NetworkMessageIF#processData(com.galactanet.gametable.data.net.Connection, java.io.DataInputStream)
	 */
	@Override
	public synchronized void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		try
		{
			g_processing = true;
			
			final int actionOrd = dis.readInt();
	    Action action = Action.fromOrdinal(actionOrd);
	    
	    final String group = dis.readUTF();
	    MapElementID pog = null;
	    
	    String newGroupName = null;
	    
	    if (action == Action.RENAME)
	    {
	    	newGroupName = dis.readUTF(); 
	    }
	    else
	    {            
	    	long pogID = dis.readLong();
	    	pog = MapElementID.fromNumeric(pogID);
	    }
	    
	    final int playerID = dis.readInt();
	    GametableFrame frame = GametableFrame.getGametableFrame();
	    
	    if (frame.getMyPlayerId() == playerID)
	    	return;
	    
	    // TODO #Networking this sends me through a loop...
	    
	    if (pog != null)
	    {
	    	handleActionNetworkMessage(action, group, pog);
	    }
	    else
	    {
	    	handleRenameNetworkMessage(group, newGroupName);
	    }
		}
		finally
		{
			g_processing = false;
		}
	}
	
	/**
	 * Checks whether we are currently processing data
	 * @return
	 */
	public static boolean isProcessing()
	{
		return g_processing;
	}

	/**
	 * Handle a received network communication packet
	 * 
	 * @param action Action to process
	 * @param groupName Name of affected group
	 * @param elementID Element unique ID
	 */
	private void handleActionNetworkMessage(Action action, final String groupName, final MapElementID elementID)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();		
		GameTableMap map = frame.getGametableCanvas().getPublicMap();
		GroupManager manager = frame.getPublicGroupManager();
		
		final MapElement element = map.getMapElement(elementID);

		Group group = manager.getGroup(groupName, action == Action.NEW);
		if (group == null)
			return;

		switch (action)
		{
		case ADD_ELEMENT:
			group.addElement(element);
			break;

		case REMOVE_ELEMENT:
			group.removeElement(element);
			break;

		case DELETE:
			group.deleteGroup();
			break;

		case RENAME:
			// handled through another method return

		case NEW:
			// do nothing
			break;
		}
	}

	/**
	 * Handle a received network communication packet
	 * 
	 * @param action Action to process
	 * @param groupName Name of affected group
	 * @param elementID Element unique ID
	 */
	private void handleRenameNetworkMessage(final String groupName, final String newGroupName)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();		
		GroupManager manager = frame.getPublicGroupManager();
		
		Group g = manager.getGroup(groupName);

		if (g != null)
		{
			try
			{
				g.setName(newGroupName, false);
			}
			catch (InvalidNameException e)
			{
				Log.log(Log.PLAY, e.getMessage());
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
	
	private static boolean g_processing = false;
	private static int g_id = 0;
	private static String g_name = null;	
}