/*
 * MapElementTypeLibraryAdapterOmni.java
 *
 * @created 2010-10-13
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

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized listener adpater that only dispatches to registered listeners.
 * Faciliates registering to multiple objects at only one place
 *
 * @author Eric Maziade
 */
public class MapElementTypeLibraryAdapterOmni implements MapElementTypeLibraryListenerIF
{

	/*
	 * @see com.gametable.data.MapElementTypeLibraryListenerIF#onLibraryAdded(com.gametable.data.MapElementTypeLibrary, com.gametable.data.MapElementTypeLibrary)
	 */
	@Override
	public void onLibraryAdded(MapElementTypeLibrary parentLibrary, MapElementTypeLibrary newLibrary)
	{
		for (MapElementTypeLibraryListenerIF listener : m_listeners)
			listener.onLibraryAdded(parentLibrary, newLibrary);
	}

	/*
	 * @see com.gametable.data.MapElementTypeLibraryListenerIF#onMapElementTypeAdded(com.gametable.data.MapElementTypeLibrary, com.gametable.data.MapElementTypeIF)
	 */
	@Override
	public void onMapElementTypeAdded(MapElementTypeLibrary parentLibrary, MapElementTypeIF newType)
	{
		for (MapElementTypeLibraryListenerIF listener : m_listeners)
			listener.onMapElementTypeAdded(parentLibrary, newType);
	}

	/*
	 * @see com.gametable.data.MapElementTypeLibraryListenerIF#onMapElementTypeRemoved(com.gametable.data.MapElementTypeLibrary, com.gametable.data.MapElementTypeIF)
	 */
	@Override
	public void onMapElementTypeRemoved(MapElementTypeLibrary parentLibrary, MapElementTypeIF removedType)
	{
		for (MapElementTypeLibraryListenerIF listener : m_listeners)
			listener.onMapElementTypeRemoved(parentLibrary, removedType);
	}

	/*
	 * @see com.gametable.data.MapElementTypeLibraryListenerIF#onMapElementTypeUpdated(com.gametable.data.MapElementTypeLibrary, com.gametable.data.MapElementTypeIF)
	 */
	@Override
	public void onMapElementTypeUpdated(MapElementTypeLibrary parentLibrary, MapElementTypeIF type)
	{
		for (MapElementTypeLibraryListenerIF listener : m_listeners)
			listener.onMapElementTypeUpdated(parentLibrary, type);
	}

	/**
   * Adds a MapElementTypeLibraryListenerIF to this element
   * @param listener Listener to call when something changes within the library
   */
  public void addListener(MapElementTypeLibraryListenerIF listener)
  {
  	if (!m_listeners.contains(listener))
  		m_listeners.add(listener);
  }
  
  /**
   * Removes a listener from this element
   * @param listener Listener to remove
   * @return True if listener was found and removed
   */
  public boolean removeListener(MapElementTypeLibraryListenerIF listener)
  {
  	return m_listeners.remove(listener);
  }

  /**
   * List of map element listeners
   */
  private List<MapElementTypeLibraryListenerIF> m_listeners = new ArrayList<MapElementTypeLibraryListenerIF>();
}
