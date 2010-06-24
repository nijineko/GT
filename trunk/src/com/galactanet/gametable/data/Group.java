/*
 * Group.java
 *
 * @created 2010-06-22
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

package com.galactanet.gametable.data;

import java.util.*;
import java.util.Map.Entry;

import javax.naming.InvalidNameException;

import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.data.net.PacketSourceState;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;

/**
 * MapElement group integration
 *
 * @author vinuuen
 * 
 * @audited by themaze75
 */
public class Group
{
	public enum Action
	{
		ADD, DELETE, NEW, REMOVE, RENAME;

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
	 * All groups mapped by group name
	 */
	private static Map<String, Group>	g_groups							= new HashMap<String, Group>();


	/**
	 * Removes all groups from the list
	 */
	public static void deleteAllGroups()
	{
		for (Group g : g_groups.values())
		{
			g.removeAllElements();
			send(Action.DELETE, g, null);
		}

		g_groups.clear();
	}

	/**
	 * Removes all empty groups from the list
	 */
	public static void deleteEmpryGroups()
	{
		Iterator<Entry<String, Group>> iter = g_groups.entrySet().iterator();
		while (iter.hasNext())
		{
			Group g = iter.next().getValue();
			
			if (g.getElementCount() == 0)
			{
				iter.remove();
				send(Action.DELETE, g, null);
			}			
		}
	}

	/**
	 * Returns a list of existing group names
	 * @param names If non-null, will be populated with the group names, otherwise a new list instance will be created
	 * @return list of existing group names
	 */
	public static List<String> getGroupNames(List<String> names)
	{
		if (names == null)
			names = new ArrayList<String>();
		
		for (Group group : g_groups.values())
		{
			names.add(group.getName());
		}
		
		return names;
	}
	
	/**
	 * Handle a received network communication packet
	 * 
	 * @param action Action to process
	 * @param groupName Name of affected group
	 * @param elementID Element unique ID
	 */
	public static void packetReceived(Action action, final String groupName, final MapElementID elementID)
	{
		GameTableMap map = GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap();
		final MapElement element = map.getMapElementInstance(elementID);
		
		Group group = getGroup(groupName, action == Action.NEW);
		if (group == null)
			return;

		switch (action)
		{
		case ADD:
			group.addElement(element, false); // Do not send network packet, as we are reacting to a received packet
			break;

		case REMOVE:
			group.removeElement(element, false); // Do not send network packet, as we are reacting to a received packet
			break;

		case DELETE:
			group.deleteGroup(false); // Do not send network packet, as we are reacting to a received packet
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
	public static void reanemPacketReceived(final String groupName, final String newGroupName)
	{
		Group g = getGroup(groupName);
		
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

	/**
	 * Get an existing group / create a new group
	 * 
	 * @param groupName Name for new group
	 * @param autoCreate If true, non-found group is automatically created
	 * @return Group instance or null
	 */
	public static Group getGroup(final String groupName, boolean autoCreate)
	{
		Group g = g_groups.get(groupName);
		if (g != null)
			return g;

		if (!autoCreate)
			return null;

		g = new Group(groupName);
		g_groups.put(groupName, g);
		return g;
	}
	
	/**
	 * Get an existing group 
	 * 
	 * @param groupName Name for new group
	 * @return Group instance or null
	 */
	public static Group getGroup(final String groupName)
	{
		return getGroup(groupName, false);
	}
	
	/**
	 * @return The number of existing groups
	 */
	public static int getGroupCount()
	{
		return g_groups.size();
	}

	/**
	 * Send a network packet
	 * 
	 * @param action Network action to perform
	 * @param groupName Name of the affected group
	 * @param elementID Unique element ID, if the action is related to an element.
	 */
	private static void send(Action action, final Group group, final MapElementID elementID)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();
		GametableCanvas canvas = frame.getGametableCanvas();

		// Make sure we are not processing the packet
		// Ignore if editing the private map (publish action will handle networking when needed)
		if (canvas.isPublicMap() && !PacketSourceState.isNetPacketProcessing())
		{
			final int player = frame.getMyPlayerId();
			frame.send(PacketManager.makeGroupPacket(action, group == null ? "" : group.getName(), elementID, player));
		}
	}
	
	/**
	 * Send a network packet
	 * @param group group
	 * @param newName new name
	 */
	private static void sendRename(final Group group, String oldName, String newName)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();
		GametableCanvas canvas = frame.getGametableCanvas();

		// Make sure we are not processing the packet
		// Ignore if editing the private map (publish action will handle networking when needed)
		if (canvas.isPublicMap() && !PacketSourceState.isNetPacketProcessing())
		{
			final int player = frame.getMyPlayerId();
			frame.send(PacketManager.makeRenameGroupPacket(oldName, newName, player));
		}
	}

	/**
	 * Private constructor - use static methods
	 */
	private Group()
	{
		// nothing to do
	}
	
	/**
	 * Group name
	 */
	private String		m_name	= null;
	
	/**
	 * List of elements contained within the group
	 */
	private List<MapElement>	m_elements	= new ArrayList<MapElement>();
	
	/**
	 * Unmodifiable element list
	 */
	private List<MapElement>	m_elementsUnmodifiable	= null;

	/**
	 * Constructor
	 * @param groupName Name of the group
	 */
	private Group(final String groupName)
	{
		m_name = groupName;
	}

	/**
	 * Removes this group from the group list
	 */
	public void deleteGroup()
	{
		deleteGroup(true);
	}
	
	/**
	 * Removes this group from the group list
	 * @param network true to send command over network
	 */
	public void deleteGroup(boolean network)
	{
		removeAllElements();
		g_groups.remove(getName());
		
		if (network)
			send(Action.DELETE, this, null);
	}
	
	
	/**
	 * Add an element to this group
	 * @param element Map Element Instance to add
	 */
	public void addElement(final MapElement element)
	{
		addElement(element, true);
	}

	/**
	 * Add an element to this group
	 * @param element Map Element Instance to add
	 * @param network If true, send network message 
	 */
	protected void addElement(final MapElement element, boolean network)
	{
		if (element == null)
			return;
		
		// Remove element from any other group
		Group group = Group.getGroup(element);
		
		if (group != null && group != this)
			group.removeElement(element);
		
		if (group != this)
		{		
			m_elements.add(element);
			g_elements.put(element.getId(), this);
			
			send(Action.ADD, group, element.getId());
		}
	}
	
	/**
	 * Add elements to this group
	 * @param elements List of elements to add
	 */
	public void addElements(List<MapElement> elements)
	{
		addElements(elements, true);
	}
	
	/**
	 * Add elements to this group
	 * @param elements List of elements to add
	 * @param network If true, send network message
	 */
	protected void addElements(List<MapElement> elements, boolean network)
	{
		for (MapElement element : elements)
			addElement(element, network);
	}

	/**
	 * Get this group's name
	 * @return Group name
	 */
	public String getName()
	{
		return m_name;
	}

	/**
	 * Get the number of elements in this group
	 * @return number of elements
	 */
	public int getElementCount()
	{
		return m_elements.size();
	}

	/**
	 * Return unmodifiable, synchronized list of elements in this group
	 * @return List of elements (never null)
	 */
	public List<MapElement> getElements()
	{
		if (m_elementsUnmodifiable == null)
			m_elementsUnmodifiable = Collections.unmodifiableList(m_elements);
		
		return m_elementsUnmodifiable;	// Synchronized unmodifiable list (prevents modification by plugins)
	}

	/**
	 * Remove all element from this group
	 */
	public void removeAllElements()
	{
		for (MapElement element : m_elements)
		{
			g_elements.remove(element.getId());
		}			
		
		m_elements.clear();
	}
	
	/**
	 * Remove map element from this list
	 * @param element element to remove
	 */
	public void removeElement(final MapElement element)
	{
		removeElement(element, true);
	}

	/**
	 * Remove map element from this list
	 * @param element element to remove
	 * @param network true to send operation over network
	 */
	protected void removeElement(final MapElement element, boolean network)
	{
		m_elements.remove(element);
		g_elements.remove(element.getId());
		
		if (network)
			send(Action.REMOVE, this, element.getId());
	}
	
	/**
	 * Change this group's name
	 * @param groupName New name for the group to rename - the new group name must be unique
	 * @throws InvalidNameException if newGroupName is already in use
	 */
	public void setName(final String groupName) throws InvalidNameException
	{
		setName(groupName, true);
	}

	/**
	 * Change this group's name
	 * @param groupName New name for the group to rename - the new group name must be unique
	 * @parma network Send message over network
	 * @throws InvalidNameException if newGroupName is already in use
	 */
	public void setName(final String groupName, boolean network) throws InvalidNameException
	{
			if (groupName == null)
				return;

			if (groupName.equals(""))
				throw new InvalidNameException("Cannot set empty name");

			if (m_name != null && groupName.equals(m_name))
				return; // nothing to do

			if (getGroup(groupName, false) != null)
				throw new InvalidNameException(groupName + " already in use");

			String oldName = m_name;
			
			if (m_name != null)
				g_groups.remove(m_name);
			
			m_name = groupName;
			
			g_groups.put(m_name, this);
			
			sendRename(this, oldName, m_name);
	}

	@Override
	public String toString()
	{
		return getName();
	}
	
	/**
	 * Get the group linked to a given element
	 * @param element Element to look for
	 * @return Group instance or null
	 */
	public static Group getGroup(MapElement element)
	{
		return g_elements.get(element.getId());
	}
	
	private static Map<MapElementID, Group> g_elements = new HashMap<MapElementID, Group>();
	
	// TODO save group information
	// TODO grab onto listeners to auto-remove
}

