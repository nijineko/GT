/*
 * MapElementID.java
 *
 * @created 2010-06-18
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

package com.gametable.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates a unique map element instance ID to enforce type safety
 *
 * @author Eric Maziade
 */
public class MapElementID implements Comparable<MapElementID> 
{
	/**
	 * Get Id from numeric value (used only for network communications) 
	 * @param id numeric value
	 * @return MapElementInstanceID
	 */
	public static MapElementID fromNumeric(long id)
	{
		MapElementID mid = g_idMap.get(id);
		if (mid == null)
		{
			mid = new MapElementID(id);
		}
		
		return mid;
	}
	
	/**
	 * Get Id from numeric value (used only for network communications) 
	 * @param id numeric value
	 * @return MapElementInstanceID
	 */
	public static MapElementID get(long id)
	{
		MapElementID mid = g_idMap.get(id);				
		return mid;
	}
	
	/**
	 * Acquire the next available MapElementInstanceID 
	 * @return ID
	 */
	public static MapElementID acquire()
	{
		return new MapElementID(g_nextAvailableID);
	}
	
	/**
	 * Clear all generated MapElementID
	 * TODO Clear IDs on new projects
	 */
	public synchronized static void clear()
	{
		g_nextAvailableID = 1;
		
		for (MapElementID id : g_idMap.values())
			id.invalidate();
		
		g_idMap.clear();
	}
	
	/**
	 * Private constructor - use static methods
	 */
	private MapElementID(long id)
	{
		if (g_idMap.get(id) != null)
			throw new IllegalArgumentException("Invalid MapElementInstanceID - " + id + " already in use");
		
		synchronized (this)
		{
			// set next available id
			if (id >= g_nextAvailableID)
				g_nextAvailableID = id + 1;
			
			m_id = id;
			g_idMap.put(id, this);	
		}		
	}
	
	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MapElementID o)
	{
		if (!m_valid)
			throw new IllegalStateException("MapElementInstance " + m_id + " has been invalidated");
		
		if (o.m_id == m_id)
			return 0;
		
		if (m_id < o.m_id)
			return -1;
		
		return 1;
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{		
		if (obj instanceof MapElementID)
			return equals((MapElementID)obj);

		return super.equals(obj);
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */	
	public boolean equals(MapElementID id)
	{
		if (!m_valid)
			throw new IllegalStateException("MapElementInstance " + m_id + " has been invalidated");
		
		return id.m_id == m_id;		
	}
	
	/**
	 * Gets the ordinal value of this ID
	 * @return numeric representation of this item
	 */
	public long numeric()
	{
		if (!m_valid)
			throw new IllegalStateException("MapElementInstance " + m_id + " has been invalidated");
		
		return m_id;
	}
	
	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		if (m_valid)
			return String.valueOf(m_id);
		
		return String.valueOf(m_id) + "<- !INVALID";
	}
	
	/**
	 * Renders this ID invalid
	 */
	private void invalidate()
	{
		m_valid = false;		
	}
	
	/**
	 * Finds a new internal ID to use for this map element ID
	 */
	public synchronized void reassignInternalID()
	{
		g_idMap.remove(m_id);
		m_id = g_nextAvailableID++;
		g_idMap.put(m_id, this);
		
	}
	
	/**
	 * Internal representation of ID
	 */
	private long m_id;
	
	/**
	 * Validity flag of pog ID
	 */
	private boolean m_valid = true;

	/**
	 * Mapping of all used IDs
	 */
	private static Map<Long, MapElementID> g_idMap = new HashMap<Long, MapElementID>();
	
	/**
	 * Last used ID
	 */
	private static long g_nextAvailableID = 1;
}
