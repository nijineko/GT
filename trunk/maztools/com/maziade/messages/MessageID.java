/*
 * MessageID.java
 * 
 * @created 2006
 * 
 * Copyright (C) 1999-2011 Eric Maziade
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
package com.maziade.messages;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eric Maziade
 * Determines the message that is to be executed.
 *
 */
public class MessageID implements Comparable<MessageID>
{
	/**
	 * Get Id from numeric value (used only for network communications) 
	 * @param id numeric value
	 * @return MapElementInstanceID
	 */
	public static MessageID get(int id)
	{
		MessageID mid = g_idMap.get(id);				
		return mid;
	}
	
	/**
	 * Acquire the next available MapElementInstanceID
	 * @param name Message ID name (for debugging purposes) 
	 * @return ID
	 */
	public static MessageID acquire(String name)
	{
		return new MessageID(g_nextAvailableID, name);
	}
	
	/**
	 * Clear all generated MessageID
	 */
	public synchronized static void clear()
	{
		g_nextAvailableID = 1;
		g_idMap.clear();
	}
	
	/**
	 * Private constructor - use static methods
	 */
	private MessageID(int id, String name)
	{
		if (g_idMap.get(id) != null)
			throw new IllegalArgumentException("Invalid MessageID - " + id + " already in use");
		
		synchronized (this)
		{
			// set next available id
			if (id >= g_nextAvailableID)
				g_nextAvailableID = id + 1;
			
			m_name = (name == null ? "" : name + " ")  + id;
			m_id = id;
			g_idMap.put(id, this);	
		}		
	}
	
	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MessageID o)
	{
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
		if (obj instanceof MessageID)
			return equals((MessageID)obj);

		return super.equals(obj);
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */	
	public boolean equals(MessageID id)
	{
		return id.m_id == m_id;		
	}
	
	/**
	 * Gets the ordinal value of this ID
	 * @return numeric representation of this item
	 */
	public int numeric()
	{
		return m_id;
	}
	
	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return m_name;
	}

	/**
	 * Internal name of message ID for debugging purposes
	 */
	private final String m_name;
	
	/**
	 * Internal representation of ID
	 */
	private final int m_id;

	/**
	 * Mapping of all used IDs
	 */
	private static Map<Integer, MessageID> g_idMap = new HashMap<Integer, MessageID>();
	
	/**
	 * Last used ID
	 */
	private static int g_nextAvailableID = 1;	
}
