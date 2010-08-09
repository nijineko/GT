/*
 * GameTableMap.java
 * 
 * @created 2005-09-05
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.util.UtilityFunctions;
import com.maziade.tools.XMLUtils;

/**
 * Holds data pertaining to a map
 * 
 * @author sephalon
 * 
 * @audited by themaze75
 * 
 */
public class GameTableMap implements XMLSerializeIF, MapElementRepositoryIF
{
	/**
	 * Every 'square' is divided into this number of units
	 */
	private final static int    BASE_SQUARE_SIZE       = 64;

	/**
	 * Returns the number of map units found within a square
	 * @return Number of map units - NOT DIRECTLY RELATED TO PIXELS
	 */
	public final static int getBaseSquareSize()
	{
		return GameTableMap.BASE_SQUARE_SIZE;
	}

	/**
	 * Lines drawn on the map
	 */
	private final List<LineSegment>	m_lines;

	/**
	 * Unmodifiable version of m_lines
	 */
	private final List<LineSegment>	m_linesUnmodifiable;

	private List<GameTableMapListenerIF> m_listeners = new CopyOnWriteArrayList<GameTableMapListenerIF>();

	/**
	 * List of elements or all types to display on the map
	 */
	private final List<MapElement>					m_mapElements;

	// @revise Build undo buffers using Java's #{@link javax.swing.undo.UndoableEdit}

	/**
	 * Unmodifiable list of elements
	 */
	private final List<MapElement>					m_mapElementsUnmodifiable;

	/**
	 * Whether this is the public of private version of the map
	 */
	private final boolean						m_publicMap;
	
	/**
	 * Group manager object
	 */
	private final GroupManager 			m_groupManager;

	/**
	 * Constructor
	 * 
	 * @param publicMap Sets whether this map is private or public
	 */
	public GameTableMap(boolean publicMap)
	{
		m_publicMap = publicMap;

		m_lines = new ArrayList<LineSegment>();
		m_linesUnmodifiable = Collections.unmodifiableList(m_lines);

		m_mapElements = new ArrayList<MapElement>();
		m_mapElementsUnmodifiable = Collections.unmodifiableList(m_mapElements);
		
		m_groupManager = new GroupManager();
		
		m_elementListener = new MapElementAdapterOmni();
	}

	/**
	 * Adds a line segment to the map
	 * 
	 * @param ls line segment to add
	 */
	public void addLineSegment(LineSegment ls)
	{
		m_lines.add(ls);		
		// @revise trigger listener (add line)
	}

	/**
   * Adds a GameTableMapListener to this map
   * @param listener Listener to call when something changes within the map
   */
  public void addListener(GameTableMapListenerIF listener)
  {
  	m_listeners.remove(listener);
  	m_listeners.add(listener);
  }

	/**
	 * Adds an element instance to the map
	 * 
	 * @param mapElement Element to add to the map
	 */
	public void addMapElementInstance(MapElement mapElement)
	{
		m_mapElements.add(mapElement);
		
		mapElement.addListener(m_elementListener);
		
		for (GameTableMapListenerIF listener : m_listeners)
			listener.onMapElementInstanceAdded(this, mapElement);
	}
	
	/**
	 * Clear this map of all data
	 */
	public void clearMap()
	{
		getGroupManager().deleteAllGroups();
		clearLineSegments();
		clearMapElementInstances();		
	}

	/**
	 * Remove all lines from the map
	 */
	public void clearLineSegments()
	{
		m_lines.clear();
		// @revise trigger listeners (clear lines)
	}
	
  /**
	 * Remove all elements from the map
	 */
	public void clearMapElementInstances()
	{
		for (MapElement element : m_mapElements)
			element.removeListener(m_elementListener);
		
		m_mapElements.clear();
		
		for (GameTableMapListenerIF listener : m_listeners)
			listener.onMapElementInstancesCleared(this);
	}

	/*
	 * @see com.galactanet.gametable.data.XMLSerializeIF#deserialize(org.w3c.dom.Element, com.galactanet.gametable.data.XMLSerializeConverter)
	 */
  @Override  
  public void deserialize(Element parent, XMLSerializeConverter converter)
  {
  	Element elements = XMLUtils.getFirstChildElementByTagName(parent, "elements");
  	
  	clearMap();
  	
  	for (Element xmEl : XMLUtils.getChildElementsByTagName(elements, "element"))
  	{
  		MapElement el = new MapElement(xmEl, converter);
  		addMapElementInstance(el);    	
  	}
  	
  	elements = XMLUtils.getFirstChildElementByTagName(parent, "lines");
  	for (Element xmLine : XMLUtils.getChildElementsByTagName(elements, "line"))
  	{
  		LineSegment ls = new LineSegment(xmLine);
  		addLineSegment(ls);
  	}
  	
  	Element groupsEl = XMLUtils.getFirstChildElementByTagName(parent, "groups");
  	m_groupManager.deserializeGroups(groupsEl, converter, this);
  }

	/**
   * Calculate the bounds used by the specified map
   * @param map map to calculate
   * @return coordinates of the space used by the map
   */
  public MapRectangle getBounds()
  {
  	MapRectangle bounds = null;
  	
      // lines
      for (LineSegment ls : getLines())
      {
      	
          MapRectangle r = ls.getBounds();
          
          if (bounds == null)
              bounds = r;
          else
              bounds.add(r);
      }
  
      // Map elements
      for (MapElement mapElement : getMapElements())
      {
      	MapRectangle r = mapElement.getBounds();
          
          if (bounds == null)
              bounds = r;
          else
              bounds.add(r);
      }
      
      if (bounds == null)
          bounds = new MapRectangle(MapCoordinates.ORIGIN, 1, 1);
      
      return bounds;
  }

	/**
	 * Get unmodifiable list of lines contained within GameTableMap
	 * 
	 * @return list of LineSegment (never null)
	 */
	public List<LineSegment> getLines()
	{
		return m_linesUnmodifiable;
	}
	
	/**
	 * Get the maps' group manager
	 * @return Group Manager instance
	 */
	public GroupManager getGroupManager()
	{
		return m_groupManager;
	}

	/**
	 * Get map element instance by ID
	 * 
	 * @param id ID of the map element we are looking for
	 * @return Matching map element or null
	 */
	@Override
	public MapElement getMapElement(final MapElementID id)
	{
		for (MapElement mapElement : m_mapElements)
		{
			if (mapElement.getId().equals(id))
				return mapElement;
		}

		return null;
	}


	/**
	 * Get topmost element matching given position on the map
	 * 
	 * @param modelPosition Coordinates to test for
	 * @return Matching element or none
	 * 
	 * @revise Add support for disabled and hidden layers
	 */
	public MapElement getMapElementAt(MapCoordinates modelPosition)
	{
		if (modelPosition == null)
		{
			return null;
		}

		MapElement pogHit = null;
		MapElement envHit = null;
		MapElement overlayHit = null;
		MapElement underlayHit = null;

		for (MapElement mapElement : m_mapElements)
		{
			if (mapElement.contains(modelPosition))
			{
				switch (mapElement.getLayer())
				{
				case UNDERLAY:
					underlayHit = mapElement;
					break;

				case OVERLAY:
					overlayHit = mapElement;
					break;

				case ENVIRONMENT:
					envHit = mapElement;
					break;

				case POG:
					pogHit = mapElement;
					break;
				}
			}
		}

		// Pogs is top layer
		if (pogHit != null)
			return pogHit;

		// Environment is 2nd layer
		if (envHit != null)
			return envHit;

		// Overlay is 3rd layer
		if (overlayHit != null)
			return overlayHit;

		// Underlay is fourth layer
		return underlayHit;
	}

	/**
	 * Get map element instance by name
	 * 
	 * @param name name of the instance we are looking for
	 * @return instance or null
	 */
	public MapElement getMapElementByName(final String name)
	{
		return getMapElementInstancesByName(name, null);
	}

	/**
	 * Get list of map element instances
	 * 
	 * @return unmodifiable list of instances
	 */
	public List<MapElement> getMapElements()
	{
		return m_mapElementsUnmodifiable;
	}

	/**
	 * Find map element instances matching a given name
	 * 
	 * @param name Name of the element instance we are looking for
	 * @return List of matching elements (never null)
	 */
	public List<MapElement> getMapElementsByName(String name)
	{
		List<MapElement> retVal = new ArrayList<MapElement>();
		getMapElementInstancesByName(name, retVal);

		return retVal;
	}

	/**
	 * @return True if this is a public map.  False if it is a private map.
	 */
	public boolean isPublicMap()
	{
		return m_publicMap;
	}

	/**
	 * Remove line segment from map
	 * 
	 * @param ls line segment to remove
	 */
	public void removeLineSegment(final LineSegment ls)
	{
		m_lines.remove(ls);
		// @revise trigger listener (remove line segment)
	}
	
  
  /**
   * Removes a listener from this map
   * @param listener Listener to remove
   * @return True if listener was found and removed
   */
  public boolean removeListener(GameTableMapListenerIF listener)
  {
  	return m_listeners.remove(listener);
  }
  
  /**
	 * Remove a given map element instance from the map
	 * 
	 * @param mapElement Map element instance to remove
	 */
	public void removeMapElementInstance(final MapElement mapElement)
	{
		m_mapElements.remove(mapElement);
		
		mapElement.removeListener(m_elementListener);
		
		for (GameTableMapListenerIF listener : m_listeners)
			listener.onMapElementInstanceRemoved(this, mapElement);
	}
  
  /**
	 * Remove multiple element instances from the map
	 * 
	 * @param instances list of instances to remove
	 */
	public void removeMapElementInstances(List<MapElement> instances)
	{
		for (MapElement instance : instances)			
			removeMapElementInstance(instance);
	}
  
  /*
   * @see com.galactanet.gametable.data.XMLSerializer#serialize(org.w3c.dom.Element)
   */
  @Override
  public void serialize(Element parent)
  {
  	Document doc = parent.getOwnerDocument();
  	Element elements = doc.createElement("elements");  	
  	for (MapElement el : m_mapElements)
  	{
  		Element xmEl = doc.createElement("element");
  		el.serialize(xmEl);
  		elements.appendChild(xmEl);
  	}
  	parent.appendChild(elements);
  	
  	Element lines = doc.createElement("lines");  	
  	for (LineSegment line : m_lines)
  	{
  		Element xmLine = doc.createElement("line");
  		line.serialize(xmLine);
  		lines.appendChild(xmLine);
  	}
  	parent.appendChild(lines);
  	
  	Element groupsEl = doc.createElement("groups");
  	parent.appendChild(groupsEl);
  	m_groupManager.serializeGroups(groupsEl);
  }
  
  /**
	 * Find instances matching a given name
	 * 
	 * @param name Name of the pog we are looking for
	 * @param mapElements if non-null, will be populated with all matching pogs
	 * @return If mapElements is null, will return first matching insatnce
	 */
	private MapElement getMapElementInstancesByName(String name, List<MapElement> mapElements)
	{
		if (name == null || name.equals(""))
			return null;

		final String normalizedName = UtilityFunctions.normalizeName(name);

		for (MapElement instance : m_mapElements)
		{
			if (instance.getNormalizedName().equals(normalizedName))
			{
				if (mapElements != null)
					mapElements.add(instance);
				else
					return instance;
			}
		}

		return null;
	}
	
	/**
   * Adds a MapElementListenerIF to this element
   * @param listener Listener to call when something changes within the map
   */
  public void addMapElementListener(MapElementListenerIF listener)
  {
  	m_elementListener.addListener(listener);
  }
  
  /**
   * Removes a listener from this element
   * @param listener Listener to remove
   * @return True if listener was found and removed
   */
  public boolean removeMapElementListener(MapElementListenerIF listener)
  {
  	return m_elementListener.removeListener(listener);
  }
	
	private final MapElementAdapterOmni m_elementListener;
}
