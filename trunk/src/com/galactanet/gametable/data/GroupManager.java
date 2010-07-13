/*
 * GroupManager.java
 * 
 * @created 2010-07-12
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
import java.util.Map.Entry;

import javax.naming.InvalidNameException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.data.Group.Action;
import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.data.net.PacketSourceState;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;
import com.maziade.tools.XMLUtils;

/**
 * todo: comment
 * 
 * @author Eric Maziade
 */
public class GroupManager
{
	/**
	 * Global list of groups
	 */
	protected Map<MapElementID, Group>	m_elements	= new HashMap<MapElementID, Group>();

	/**
	 * All groups mapped by group name
	 */
	private Map<String, Group>					m_groups		= new HashMap<String, Group>();
	
	/**
	 * Protected constructor Should be used only by GameTableMap
	 */
	protected GroupManager()
	{
	}

	/**
	 * Removes all groups from the list
	 */
	public void deleteAllGroups()
	{
		for (Group g : m_groups.values())
		{
			g.removeAllElements();
			send(Action.DELETE, g, null);
		}

		m_groups.clear();
	}

	/**
	 * Removes all empty groups from the list
	 */
	public void deleteEmptyGroups()
	{
		Iterator<Entry<String, Group>> iter = m_groups.entrySet().iterator();
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
	 * Restore information from your component from within supplied parent element
	 * 
	 * @param parent Parent element, as restored from calling thread
	 * @param converter Converts saved IDs to map IDs
	 * @param repository Interface to get map element instances from
	 */
	public void deserializeGroups(Element parent, XMLSerializeConverter converter, MapElementRepositoryIF repository)
	{
		m_groups.clear();
		m_elements.clear();

		int idx = 0;
		List<MapElement> elements = new ArrayList<MapElement>();

		for (Element groupEl : XMLUtils.getChildElementsByTagName(parent, "group"))
		{
			String name = XMLUtils.getFirstChildElementContent(groupEl, "name");
			if (name == null)
				name = "group#" + (++idx);

			Group group = getGroup(name, true);

			Element elementsEl = XMLUtils.getFirstChildElementByTagName(groupEl, "elements");
			if (elementsEl != null)
			{
				elements.clear();
				for (Element idEl : XMLUtils.getChildElementsByTagName(elementsEl, "id"))
				{
					try
					{
						long lid = Long.valueOf(XMLUtils.getNodeValue(idEl));
						MapElementID id = converter.getMapElementID(lid);
						MapElement element = repository.getMapElement(id);

						if (element != null)
							elements.add(element);
					}
					catch (NumberFormatException e)
					{
						Log.log(Log.SYS, "Invalid numeric format loading element ID in " + parent.getOwnerDocument().getDocumentURI());
					}
				}

				group.addElements(elements);
			}
		}
	}

	/**
	 * Get the group linked to a given element
	 * 
	 * @param element Element to look for
	 * @return Group instance or null
	 */
	public Group getGroup(MapElement element)
	{
		return m_elements.get(element.getId());
	}

	/**
	 * Get an existing group
	 * 
	 * @param groupName Name for new group
	 * @return Group instance or null
	 */
	public Group getGroup(final String groupName)
	{
		return getGroup(groupName, false);
	}

	/**
	 * Get an existing group / create a new group
	 * 
	 * @param groupName Name for new group
	 * @param autoCreate If true, non-found group is automatically created
	 * @return Group instance or null
	 */
	public Group getGroup(final String groupName, boolean autoCreate)
	{
		Group g = m_groups.get(groupName);
		if (g != null)
			return g;

		if (!autoCreate)
			return null;

		g = new Group(this, groupName);
		addGroup(g);
		return g;
	}

	/**
	 * @return The number of existing groups
	 */
	public int getGroupCount()
	{
		return m_groups.size();
	}

	/**
	 * Returns a list of existing group names
	 * 
	 * @param names If non-null, will be populated with the group names, otherwise a new list instance will be created
	 * @return list of existing group names
	 */
	public List<String> getGroupNames(List<String> names)
	{
		if (names == null)
			names = new ArrayList<String>();

		for (Group group : m_groups.values())
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
	public void packetReceived(Action action, final String groupName, final MapElementID elementID)
	{
		GameTableMap map = GametableFrame.getGametableFrame().getGametableCanvas().getPublicMap();
		final MapElement element = map.getMapElement(elementID);

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
	public void renamePacketReceived(final String groupName, final String newGroupName)
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
	 * Store information from your component from inside parent element
	 * 
	 * @param parent Parent element, as populated by calling thread. You can add custom XML data as children.
	 */
	public void serializeGroups(Element parent)
	{
		Document doc = parent.getOwnerDocument();

		for (Group group : m_groups.values())
		{
			Element groupEl = doc.createElement("group");
			groupEl.appendChild(XMLUtils.createElementValue(doc, "name", group.getName()));

			// elements
			Element elementsEl = doc.createElement("elements");
			groupEl.appendChild(elementsEl);

			for (MapElement element : group.getElements())
			{
				elementsEl.appendChild(XMLUtils.createElementValue(doc, "id", String.valueOf(element.getId().numeric())));
			}

			parent.appendChild(groupEl);
		}
	}

	/**
	 * Send a network packet
	 * 
	 * @param action Network action to perform
	 * @param groupName Name of the affected group
	 * @param elementID Unique element ID, if the action is related to an element.
	 */
	protected void send(Action action, final Group group, final MapElementID elementID)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();
		GametableCanvas canvas = frame.getGametableCanvas();

		// Make sure we are not processing the packet
		// Ignore if editing the protected map (publish action will handle networking when needed)
		if (canvas.isPublicMap() && !PacketSourceState.isNetPacketProcessing())
		{
			final int player = frame.getMyPlayerId();
			frame.send(PacketManager.makeGroupPacket(action, group == null ? "" : group.getName(), elementID, player));
		}
	}

	/**
	 * Send a network packet
	 * 
	 * @param group group
	 * @param newName new name
	 */
	protected void sendRename(final Group group, String oldName, String newName)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();
		GametableCanvas canvas = frame.getGametableCanvas();

		// Make sure we are not processing the packet
		// Ignore if editing the protected map (publish action will handle networking when needed)
		if (canvas.isPublicMap() && !PacketSourceState.isNetPacketProcessing())
		{
			final int player = frame.getMyPlayerId();
			frame.send(PacketManager.makeRenameGroupPacket(oldName, newName, player));
		}
	}
	
	/**
	 * Add a group to the list of groups.  Should be used only by Group Object
	 * @param Group group instance
	 */
	protected void addGroup(Group group)
	{
		m_groups.put(group.getName(), group);
	}
	
	/**
	 * Remove a group from the list of groups.  Should be used only by Group Object
	 * @param groupName
	 */
	protected void removeGroup(String groupName)
	{
		m_groups.remove(groupName);
	}
	
	/**
	 * Register an element for fast mapping of group / element. Should be used only by Group object
	 * @param mapElementID Map Element ID
	 * @param group Group
	 */
	protected void registerElement(MapElementID mapElementID, Group group)
	{
		m_elements.put(mapElementID, group);
	}
	
	/**
	 * Unregisters an element for fast mapping of group / element. Should be used only by Group object
	 * @param mapElementID Map Element ID
	 */
	protected void unregisterElement(MapElementID mapElementID)
	{
		m_elements.remove(mapElementID);
	}
	
	

	// TODO grab onto listeners to auto-remove
}
