/*
 * ConversionIF.java
 *
 * @created 2010-07-11
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
 * Conversion interface
 * 
 * Element IDs cannot necessarily be restored as saved.  This maintains an equivalence table to use
 */
public class XMLSerializeConverter
{
	/**
	 * Get the new MapElementID matching with the stored ID 
	 * @param savedElementID 
	 * @return
	 */
	public MapElementID getMapElementID(long savedElementID)
	{
		return m_map.get(savedElementID);
	}
	
	/**
	 * Store a new mapElementID matching with a stored ID  
	 * @param savedElementID
	 * @param mapElementID
	 */
	public void storeMapElementID(long savedElementID, MapElementID mapElementID)
	{
		m_map.put(savedElementID, mapElementID);
	}
	
	/**
	 * Conversion map
	 */
	private Map<Long, MapElementID> m_map = new HashMap<Long, MapElementID>();
}