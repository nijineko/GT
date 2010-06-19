/*
 * PogGroup.java
 * 
 * @created 2009-12-28
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
package com.galactanet.gametable.data;

import java.util.*;

import javax.naming.InvalidNameException;

import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.data.net.PacketSourceState;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;

/**
 * Manages the Grouping of Pogs for all occasions Moving, Deleting when drawing and so forth
 * 
 * @audited by themaze75
 * 
 * @author vinuuen
 */
public class PogGroups
{
	public enum Action
	{
		ADD, DELETE, NEW, REMOVE;

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
	 * Internal class holding pog group group information
	 *
	 * @author Eric Maziade
	 */
	private static class Group
	{
		/**
		 * Group name
		 */
		private String		m_name	= null;
		
		/**
		 * List of pogs contained within the group
		 */
		private List<MapElementInstance>	m_pogs	= new ArrayList<MapElementInstance>();
		
		/**
		 * Unmodifiable m_pogs
		 */
		private List<MapElementInstance>	m_pogsUnmodifiable	= null;

		/**
		 * Constructor
		 * @param groupName Name of the group
		 */
		private Group(final String groupName)
		{
			m_name = groupName;
		}

		/**
		 * Add a pog to this group
		 * @param pog Pog to add
		 */
		public void addPog(final MapElementInstance pog)
		{
			if (pog == null)
				return;
			
			m_pogs.add(pog);
			pog.setGroup(m_name);
		}

		/**
		 * Remove all pogs from this group
		 */
		public void removeAllPogs()
		{
			for (MapElementInstance pog : m_pogs)
				pog.setGroup(null);	// clear the pog's group name
			
			m_pogs.clear();
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
		 * Return unmodifiable? list of pogs for this group
		 * @return List of pogs (never null)
		 */
		public List<MapElementInstance> getPogs()
		{
			if (m_pogsUnmodifiable == null)
				m_pogsUnmodifiable = Collections.unmodifiableList(m_pogs);
			
			return m_pogsUnmodifiable;	// Synchronized unmodifiable list (prevents modification by plugins)
		}

		/**
		 * Remove pog from this list
		 * @param pog Pog to remove
		 */
		public void removePog(final MapElementInstance pog)
		{
			m_pogs.remove(pog);
			pog.setGroup(null); // Clear the pog's group name
		}

		/**
		 * Change this group's name
		 * @param groupName
		 */
		public void setName(final String groupName)
		{
			m_name = groupName;
		}

		/**
		 * Get the number of pogs in this group
		 * @return number of pogs
		 */
		public int getPogCount()
		{
			return m_pogs.size();
		}

		@Override
		public String toString()
		{
			return getName();
		}
	}

	/**
	 * All groups mapped by group name
	 */
	private static Map<String, Group>	m_groups							= new HashMap<String, Group>();

	/**
	 * Unmodifiable synchronized collection of groups
	 */
	private static Collection<Group>	m_groupsUnmodifiable	= null;

	/* ********************************************************************************************** */

	/**
	 * Adds a list of pogs to a group. Group is automatically created if not found.
	 * 
	 * @param pogs List of pogs to add
	 * @param groupName name of group to add to
	 */
	public static void addPogsToGroup(final String groupName, List<MapElementInstance> pogs)
	{
		// TODO merge this in one, more efficient method (a.k.a. one server call)
		for (MapElementInstance pog : pogs)
		{
			addPogToGroup(groupName, pog);
		}
	}

	/**
	 * Adds a pog to a group. Group is automatically created if not found.
	 * 
	 * @param groupName Name of group to add to
	 * @param pog Pog instance to add to group
	 */
	public static void addPogToGroup(final String groupName, final MapElementInstance pog)
	{
		if (pog == null)
			return;

		Group g = getGroup(groupName);

		// remove pog from any previous group
		if (pog.isGrouped())
			removePogFromGroup(pog);

		g.addPog(pog);

		send(Action.ADD, groupName, pog.getId());
	}

	/**
	 * Removes all groups from the list
	 */
	public static void deleteAllGroups()
	{
		for (Group g : getGroups())
		{
			g.removeAllPogs();
			send(Action.DELETE, g.toString());
		}

		m_groups.clear();
	}

	/**
	 * Removes all empty groups from the list
	 */
	public static void deleteEmpryGroups()
	{
		for (Group g : getGroups().toArray(new Group[0])) // Group list converted to array to avoid potential concurrent
																											// modification issues
		{
			if (g.getPogCount() == 0)
			{
				m_groups.remove(g);
				send(Action.DELETE, g.toString());
			}
		}
	}

	/**
	 * Removes a group from the group list
	 * 
	 * @param groupName Name of the group to remove
	 */
	public static void deleteGroup(final String groupName)
	{
		Group g = getGroup(groupName, false);
		g.removeAllPogs();

		m_groups.remove(groupName);
		send(Action.DELETE, groupName);
	}

	/**
	 * Returns the number of current pog groups 
	 * @return number of groups
	 */
	public static int getGroupCount()
	{
		return m_groups.size();
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
		
		for (Group group : getGroups())
		{
			names.add(group.getName());
		}
		
		return names;
	}
	
	/**
	 * Get all pogs contained within a group
	 * 
	 * @param groupName Name of the group to look for
	 * @return List of pogs (never null)
	 */
	public static List<MapElementInstance> getGroupPogs(final String groupName)
	{
		Group g = getGroup(groupName, false);
		if (g == null)
			return Collections.emptyList();

		return g.getPogs();
	}
	
	/**
	 * Handle a received network communication packet
	 * 
	 * @param action Action to process
	 * @param groupName Name of affected group
	 * @param pogID Pog unique ID
	 */
	public static void packetReceived(Action action, final String groupName, final MapElementInstanceID pogID)
	{
		GameTableMap map = GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap();
		final MapElementInstance pog = map.getPogByID(pogID);

		switch (action)
		{
		case ADD:
			addPogToGroup(groupName, pog);
			break;

		case REMOVE:
			removePogFromGroup(pog, false); // Do not send network packet, as we are reacting to a received packet
			break;

		case DELETE:
			deleteGroup(groupName);
			break;

		case NEW:
			// do nothing
			break;
		}
	}

	/**
	 * Remove pog from it group
	 * 
	 * @param pog Pog to remove
	 */
	public static void removePogFromGroup(final MapElementInstance pog)
	{
		removePogFromGroup(pog, true);
	}

	/**
	 * Create a new group (or returns existing one)
	 * 
	 * @param groupName Name for new group
	 * @return Group instance
	 */
	private static Group getGroup(final String groupName)
	{
		return getGroup(groupName, true);
	}

	/**
	 * Create a new group (or returns existing one)
	 * 
	 * @param groupName Name for new group
	 * @param autoCreate If true, non-found group is automatically created
	 * @return Group instance or null
	 */
	private static Group getGroup(final String groupName, boolean autoCreate)
	{
		Group g = m_groups.get(groupName);
		if (g != null)
			return g;

		if (!autoCreate)
			return null;

		g = new Group(groupName);
		m_groups.put(groupName, g);
		return g;
	}

	/**
	 * Returns a collection of all groups
	 * 
	 * @return a collection of all groups
	 */
	private static Collection<Group> getGroups()
	{
		if (m_groupsUnmodifiable == null)
			m_groupsUnmodifiable = Collections.unmodifiableCollection(m_groups.values());

		return m_groupsUnmodifiable;
	}

	/**
	 * Remove pog from it group
	 * 
	 * @param pog Pog to remove
	 * @param send true to send action to server
	 */
	private static void removePogFromGroup(final MapElementInstance pog, final boolean send)
	{
		if (pog == null)
			return;

		if (!pog.isGrouped())
			return;

		Group g = m_groups.get(pog.getGroup());

		g.removePog(pog);
		if (send)
			send(Action.REMOVE, "", pog.getId());
	}

	/**
	 * Rename a given group
	 * 
	 * @param groupName Name of the group to rename
	 * @param newGroupName New name for the group to rename - the new group name must be unique
	 * @throws InvalidNameException if newGroupName is already in use
	 */
	protected static void renameGroup(final String groupName, final String newGroupName) throws InvalidNameException
	{
		if (groupName == null || newGroupName == null)
			return;

		if (newGroupName.equals(""))
			return;

		if (groupName.equals(newGroupName))
			return; // nothign to do

		if (m_groups.get(newGroupName) != null)
			throw new InvalidNameException(newGroupName + " already in use");

		Group g = m_groups.get(groupName);
		g.setName(newGroupName);

		m_groups.remove(groupName);
		m_groups.put(newGroupName, g);
	}

	/**
	 * Send a network packet
	 * 
	 * @param action Network action to perform
	 * @param groupName Name of the affected group
	 */
	private static void send(Action action, final String groupName)
	{
		send(action, groupName, null);
	}

	/**
	 * Send a network packet
	 * 
	 * @param action Network action to perform
	 * @param groupName Name of the affected group
	 * @param pogID Unique PogID, if the action is related to a pog.
	 */
	private static void send(Action action, final String groupName, final MapElementInstanceID pogID)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();
		GametableCanvas canvas = frame.getGametableCanvas();

		// Make sure we are not processing the packet
		// Ignore if editing the private map (publish action will handle networking when needed)
		if (canvas.isPublicMap() && !PacketSourceState.isNetPacketProcessing())
		{
			final int player = frame.getMyPlayerId();
			frame.send(PacketManager.makeGroupPacket(action, groupName, pogID, player));
		}
	}

	/**
	 * Private constructor - use static methods
	 */
	private PogGroups()
	{
		// nothing to do
	}
}
