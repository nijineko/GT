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

package com.gametable.data.net;

import java.io.DataInputStream;
import java.io.IOException;

import javax.naming.InvalidNameException;

import com.gametable.GametableApp;
import com.gametable.data.*;
import com.gametable.net.*;
import com.gametable.util.Log;

/**
 * Broadcast an action on a group
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
	 * @param player Source player sending the message
	 * @return Data packet
	 */
	public static byte[] makePacket(Action action, String goupName, String newGroupName, MapElementID mapElementID, Player player)
	{
		try
		{
			NetworkModuleIF module = GametableApp.getCore().getNetworkModule();
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
      
      dos.writeInt(player.getID());

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
   * @param player player sending the message 
   * @return
   */
  public static byte[] makeRenamePacket(final String groupName, final String newGroupName, final Player player) 
  {
  	return makePacket(Action.RENAME, groupName, newGroupName, null, player);
  }
	
	/*
	 * @see com.gametable.data.net.NetworkMessageIF#processData(com.gametable.data.net.Connection, java.io.DataInputStream)
	 */
	@Override
	public void processData(NetworkConnectionIF sourceConnection, DataInputStream dis, NetworkEvent event) throws IOException
	{
		final int actionOrd = dis.readInt();
    Action action = Action.fromOrdinal(actionOrd);
    
    final String group = dis.readUTF();
    MapElementID mapElementID = null;
    
    String newGroupName = null;
    
    if (action == Action.RENAME)
    {
    	newGroupName = dis.readUTF(); 
    }
    else
    {            
    	long mapElement = dis.readLong();
    	mapElementID = MapElementID.fromNumeric(mapElement);
    }
    
    final int playerID = dis.readInt();
    GameTableCore core = GametableApp.getCore();
    
    if (core.getPlayerID() == playerID)
    	return;
    
    if (mapElementID != null)
    {
    	handleActionNetworkMessage(action, group, mapElementID, event);
    }
    else
    {
    	handleRenameNetworkMessage(group, newGroupName, event);
    }
	}

	/**
	 * Handle a received network communication packet
	 * 
	 * @param action Action to process
	 * @param groupName Name of affected group
	 * @param elementID Element unique ID
	 * @param netEvent Source Network Event
	 */
	private void handleActionNetworkMessage(Action action, final String groupName, final MapElementID elementID, NetworkEvent netEvent)
	{
		GameTableCore core = GametableApp.getCore();		
		GameTableMap map = core.getMap(GameTableCore.MapType.PUBLIC);
		GroupManager manager = core.getGroupManager(GameTableCore.MapType.PUBLIC);
		
		final MapElement element = map.getMapElement(elementID);

		Group group = manager.getGroup(groupName, action == Action.NEW || action == Action.ADD_ELEMENT);
		if (group == null)
			return;

		switch (action)
		{
		case ADD_ELEMENT:
			group.addElement(element, netEvent);
			break;

		case REMOVE_ELEMENT:
			group.removeElement(element, netEvent);
			break;

		case DELETE:
			group.deleteGroup(netEvent);
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
	 * @param netEvent Source Network Event
	 */
	private void handleRenameNetworkMessage(final String groupName, final String newGroupName, NetworkEvent netEvent)
	{
		GameTableCore core = GametableApp.getCore();		
		GroupManager manager = core.getGroupManager(GameTableCore.MapType.PUBLIC);
		
		Group g = manager.getGroup(groupName);

		if (g != null)
		{
			try
			{
				g.setName(newGroupName, netEvent);
			}
			catch (InvalidNameException e)
			{
				Log.log(Log.PLAY, e.getMessage());
			}
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
	
	private static int g_id = 0;
	private static String g_name = null;	
}