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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.InvalidNameException;

/**
 * MapElement group integration
 *
 * @author vinuuen
 * 
 * @audited by themaze75
 */
public class Group
{
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
	protected Group(final GroupManager manager, final String groupName)
	{
		m_manager = manager;
		m_name = groupName;
	}
	
	/**
	 * Removes this group from the group list
	 */
	public void deleteGroup()
	{
		removeAllElements();
		m_manager.removeGroup(this);
	}

	/**
	 * Add an element to this group
	 * @param element Map Element Instance to add
	 */
	public void addElement(final MapElement element)
	{
		if (element == null)
			return;
		
		// Remove element from any other group
		Group group = m_manager.getGroup(element);
		
		if (group != null && group != this)
			group.removeElement(element);
		
		if (group != this)
		{		
			m_elements.add(element);
			m_manager.registerElement(element.getID(), this);
		}
	}
	
	/**
	 * Add elements to this group
	 * @param elements List of elements to add
	 */
	public void addElements(List<MapElement> elements)
	{
		for (MapElement element : elements)
			addElement(element);
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
	public List<MapElement> getMapElements()
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
			m_manager.unregisterElement(element.getID(), this);
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
		m_manager.unregisterElement(element.getID(), this);
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
	 * @param network Send message over network
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

			if (m_manager.getGroup(groupName, false) != null)
				throw new InvalidNameException(groupName + " already in use");

			String oldName = m_name;
			m_name = groupName;
			
			m_manager.renameGroup(this, oldName);
	}

	@Override
	public String toString()
	{
		return getName();
	}


	/**
	 * Group's manager instance
	 */
	private final GroupManager m_manager;	
}

