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

package com.galactanet.gametable.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.galactanet.gametable.data.MapElement;

/**
 * Utility to help handle selection operations 
 *
 * @author Eric Maziade
 */
public class SelectionHandler
{
	/**
	 * Constructor.  
	 */
	public SelectionHandler()
	{
		m_selectedElements = new ArrayList<MapElement>();
		m_selectedElementsUnmodifiable = Collections.unmodifiableList(m_selectedElements);	
	}
	
  /**
   * Verifies if specified element is selected
   * @param mapElement element to verify
   * @return true if selected
   */
  public boolean isSelected(MapElement mapElement)
  {
  	return m_selectedElements.contains(mapElement);    	
  }

  /**
	 * Adds a instance to the selected list
	 * 
	 * @param mapElement Instance to add to selection
	 * @param select true to select, false to unselect
	 */
	public void selectMapElement(MapElement mapElement, boolean select)
	{
		if (select)
		{
			// Make sure we're not adding doubles
			if (!m_selectedElements.contains(mapElement))
				m_selectedElements.add(mapElement);
		}
		else
			m_selectedElements.remove(mapElement);
	}

	/**
	 * Add multiple instances to the selection
	 * 
	 * @param mapElements List of instance to add to the selection
	 * @param select true to select, false to unselect
	 */
	public void selectMapElements(final List<MapElement> mapElements, boolean select)
	{
		if (select)
		{
			// Make sure we're not adding doubles - remove first
			m_selectedElements.removeAll(mapElements);
			
			m_selectedElements.addAll(mapElements);
		}
		else
			m_selectedElements.removeAll(mapElements);
	}

	/**
	 * Remove all instance from selection
	 */
	public void unselectAllMapElements()
	{
		m_selectedElements.clear();
	}

	/**
	 * Gets selected map element instances list
	 * 
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElement> getSelectedMapElements()
	{
		return m_selectedElementsUnmodifiable;
	}
	
	/**
	 * Lists the currently selected elements
	 */
	private final List<MapElement>	m_selectedElements;
	
	/**
	 * Unmodifiable version of selected elements
	 */
	private final List<MapElement>	m_selectedElementsUnmodifiable;
}
