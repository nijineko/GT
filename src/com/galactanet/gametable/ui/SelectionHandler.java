/*
 * MapSelectionHandler.java
 *
 * @created 2010-06-21
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

package com.galactanet.gametable.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.galactanet.gametable.data.MapElementInstance;

/**
 * Handles selection operations for the canvas
 *
 * @author Eric Maziade
 */
public class SelectionHandler
{
	/**
	 * Constructor.  Should only be used by GametableCanvas 
	 */
	protected SelectionHandler()
	{
		m_selectedElements = new ArrayList<MapElementInstance>();
		m_selectedElementsUnmodifiable = Collections.unmodifiableList(m_selectedElements);	
	}
	
  /**
   * Verifies if specified element is selected
   * @param mapElement element to verify
   * @return true if selected
   */
  public boolean isSelected(MapElementInstance mapElement)
  {
  	return m_selectedElements.contains(mapElement);    	
  }

  /**
	 * Adds a instance to the selected list
	 * 
	 * @param mapElement Instance to add to selection
	 */
	public void selectMapElementInstance(MapElementInstance mapElement)
	{
		m_selectedElements.add(mapElement);
	}

	/**
	 * Add multiple instances to the selection
	 * 
	 * @param mapElements List of instance to add to the selection
	 */
	public void selectMapElementInstances(final List<MapElementInstance> mapElements)
	{
		m_selectedElements.addAll(mapElements);
	}

	/**
	 * Remove all instance from selection
	 */
	public void unselectAllMapElementInstances()
	{
		m_selectedElements.clear();
	}

	/**
	 * Remove an instance from the selection
	 * 
	 * @param mapElement Instance to remove
	 */
	public void unselectMapElementInstance(final MapElementInstance mapElement)
	{
		m_selectedElements.remove(mapElement);
	}
	

	/**
	 * Gets selected map element instances list
	 * 
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElementInstance> getSelectedMapElementInstances()
	{
		return m_selectedElementsUnmodifiable;
	}
	
	/**
	 * Lists the currently selected elements
	 */
	private final List<MapElementInstance>					m_selectedElements;
	
	/**
	 * Unmodifiable version of selected elements
	 */
	private final List<MapElementInstance>	m_selectedElementsUnmodifiable;
}
