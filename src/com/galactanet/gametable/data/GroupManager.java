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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
			
			for (GroupManagerListenerIF listener : m_listeners)
				listener.onRemoveGroup(g);
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
				
				for (GroupManagerListenerIF listener : m_listeners)
					listener.onRemoveGroup(g);
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
		return m_elements.get(element.getID());
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

			for (MapElement element : group.getMapElements())
			{
				elementsEl.appendChild(XMLUtils.createElementValue(doc, "id", String.valueOf(element.getID().numeric())));
			}

			parent.appendChild(groupEl);
		}
	}
	
	/**
	 * Register a group manager listener
	 * @param listener Listener to add
	 */
	public void addListener(GroupManagerListenerIF listener)
	{
		if (!m_listeners.contains(listener))
			m_listeners.add(listener);
	}
	
	/**
	 * Remove a listener from the manager
	 * @param listener listener to remove
	 * @return true if removed, false if not found
	 */
	public boolean removeListener(GroupManagerListenerIF listener)
	{
		return m_listeners.remove(listener);
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
	protected void removeGroup(Group group)
	{
		removeGroup(group, false);
	}
	
	/**
	 * Remove a group from the list of groups.
	 * @param groupName
	 * @param silent true not to trigger listeners
	 */
	private void removeGroup(Group group, boolean silent)
	{
		m_groups.remove(group.getName());
		
		if (!silent)
		{
			for (GroupManagerListenerIF listener : m_listeners)
				listener.onRemoveGroup(group);
		}
	}
	
	/**
	 * Register an element for fast mapping of group / element. Should be used only by Group object
	 * @param mapElementID Map Element ID
	 * @param group Group
	 */
	protected void registerElement(MapElementID mapElementID, Group group)
	{
		m_elements.put(mapElementID, group);

		for (GroupManagerListenerIF listener : m_listeners)
			listener.onAddMapElementToGroup(group, mapElementID);
	}
	
	/**
	 * Unregisters an element for fast mapping of group / element. Should be used only by Group object
	 * @param mapElementID Map Element ID
	 * @param group Group we're removing from
	 */
	protected void unregisterElement(MapElementID mapElementID, Group group)
	{
		m_elements.remove(mapElementID);
		
		for (GroupManagerListenerIF listener : m_listeners)
			listener.onRemoveMapElementFromGroup(group, mapElementID);		
	}
	
	/**
	 * Changes the name of a group.  Should be used only by Group object
	 * @param group Group to rename
	 * @param oldName Previous group name
	 */
	protected void renameGroup(Group group, String oldName)
	{
		if (oldName != null)
			m_groups.remove(oldName);
		
		m_groups.put(group.getName(), group);

		for (GroupManagerListenerIF listener : m_listeners)
			listener.onGroupRename(group, oldName);
	}

	/**
	 * Registered GroupManagerListeners
	 */
	private List<GroupManagerListenerIF> m_listeners = new ArrayList<GroupManagerListenerIF>();
	
	// TODO grab onto listeners to auto-remove
}
