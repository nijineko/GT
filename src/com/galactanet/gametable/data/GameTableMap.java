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

import com.galactanet.gametable.net.NetworkEvent;
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
public class GameTableMap implements MapElementRepositoryIF
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
	 * @param netEvent network event or null
	 */
	public void addLineSegment(LineSegment ls, NetworkEvent netEvent)
	{
		m_lines.add(ls);
		
		for (GameTableMapListenerIF listener : m_listeners)
			listener.onLineSegmentAdded(this, ls, false, netEvent);
	}
	
	/**
	 * Adds a line segment to the map
	 * 
	 * @param ls line segment to add
	 */
	public void addLineSegment(LineSegment ls)
	{
		addLineSegment(ls, null);
	}
	
	/**
	 * Adds multiple line segments to the map
	 * 
	 * @param ls line segment to add
	 * @param netEvent network event or null
	 */
	public void addLineSegments(List<LineSegment> lines, NetworkEvent netEvent)
	{
		m_lines.addAll(lines);
		
		// Batch listener first
		for (GameTableMapListenerIF listener : m_listeners)
			listener.onLineSegmentsAdded(this, lines, netEvent);
		
		// 'Macro' listeners second
		for (LineSegment line : lines)
		{
			for (GameTableMapListenerIF listener : m_listeners)
				listener.onLineSegmentAdded(this, line, false, netEvent);
		}
	}
	
	/**
	 * Adds multiple line segments to the map
	 * 
	 * @param ls line segment to add
	 */
	public void addLineSegments(List<LineSegment> lines)
	{
		addLineSegments(lines, null);
	}
	
	/**
	 * Erase part of the map
	 * 
	 * @param rect Rectangular region of the map to erase
	 * @param colorSpecific If true, will erase line segments of matching color
	 * @param color Color of the line segments to erase (if colorSpecific is true)
	 */
	public void eraseLineSegments(final MapRectangle rect, boolean colorSpecific, final int color)
	{
		eraseLineSegments(rect, colorSpecific, color, null);
	}
	
	/**
	 * Erase part of the map
	 * 
	 * @param rect Rectangular region of the map to erase
	 * @param colorSpecific If true, will erase line segments of matching color
	 * @param color Color of the line segments to erase (if colorSpecific is true)
	 * @param netEvent Network event.  Null if the change is not caused by responding to a network message.
	 */
	public void eraseLineSegments(final MapRectangle rect, boolean colorSpecific, final int color, NetworkEvent netEvent)
	{
		final MapCoordinates modelStart = rect.topLeft;
		final MapCoordinates modelEnd = new MapCoordinates(modelStart.x + rect.width, modelStart.y + rect.height);

		boolean modified = false;
		final ArrayList<LineSegment> survivingLines = new ArrayList<LineSegment>();

		for (LineSegment ls : getLines())
		{
			if (!colorSpecific || (ls.getColor().getRGB() == color))
			{
				// we are the color being erased, or we're in erase all mode
				final List<LineSegment> result = ls.crop(modelStart, modelEnd);

				if (result != null)
				{
					// No change if result contains only original line
					if (result.size() ==1 && result.contains(ls))
					{
						survivingLines.add(ls);
						continue;
					}
						
					// this line segment is still alive
					for(LineSegment line : result)
						survivingLines.add(line);
					
					modified = true;
				}
				else
					modified = true;
			}
			else
			{
				// we are not affected by this erasing because we aren't the color being erased.
				survivingLines.add(ls);
			}
		}
		
		// No modifications are to be applied, let's leave without causing reactions.
		if (!modified)
			return;
		
		// If all lines were removed, go through the more efficient 'clear all lines' way.
		if (survivingLines.size() == 0)
		{
			clearLineSegments(netEvent);
			return;			
		}

		// Replace all lines with surviving lines
		m_lines.clear();
		m_lines.addAll(survivingLines);

		for (GameTableMapListenerIF listener : m_listeners)
			listener.onEraseLineSegments(this, rect, colorSpecific, color, netEvent);
	}

	/**
   * Adds a GameTableMapListener to this map
   * @param listener Listener to call when something changes within the map
   */
  public void addListener(GameTableMapListenerIF listener)
  {
  	if (!m_listeners.contains(listener))
  		m_listeners.add(listener);
  }
  
  /**
	 * Adds an element instance to the map
	 * 
	 * @param mapElement Element to add to the map
	 */
	public void addMapElement(MapElement mapElement)
	{
		addMapElement(mapElement, null);
	}

	/**
	 * Adds an element instance to the map
	 * 
	 * @param mapElement Element to add to the map
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void addMapElement(MapElement mapElement, NetworkEvent netEvent)
	{
		m_mapElements.add(mapElement);
		
		mapElement.addListener(m_elementListener);
		
		for (GameTableMapListenerIF listener : m_listeners)
			listener.onMapElementInstanceAdded(this, mapElement, netEvent);
	}
	
	/**
	 * Clear this map of all data
	 * @param netEvent Source network event or null
	 */
	public void clearMap(NetworkEvent netEvent)
	{
		getGroupManager().deleteAllGroups(netEvent);
		clearLineSegments(netEvent);
		clearMapElementInstances(netEvent);		
	}
	
	/**
	 * Remove all lines from the map
	 */
	public void clearLineSegments()
	{
		clearLineSegments(null);
	}

	/**
	 * Remove all lines from the map
	 * @param netEvent Network event that triggered the operation.  Null if non-network related.
	 */
	public void clearLineSegments(NetworkEvent netEvent)
	{
		m_lines.clear();

		for (GameTableMapListenerIF listener : m_listeners)
			listener.onClearLineSegments(this, netEvent);
	}
	
  /**
	 * Remove all elements from the map
	 */
	public void clearMapElementInstances()
	{
		clearMapElementInstances(null);
	}
	
  /**
	 * Remove all elements from the map
	 * @param netEvent Source network event or null
	 */
	public void clearMapElementInstances(NetworkEvent netEvent)
	{
		ArrayList<MapElement> mapElements = new ArrayList<MapElement>(m_mapElements);
		
		m_mapElements.clear();
		
		for (GameTableMapListenerIF listener : m_listeners)
			listener.onMapElementInstancesCleared(this);

		for (MapElement element : mapElements)
		{
			for (GameTableMapListenerIF listener : m_listeners)
				listener.onMapElementInstanceRemoved(this, element, true);

			element.removeListener(m_elementListener);
		}
	}

	/**
	 * Restore information from your component from within supplied parent element
	 * @param parent Parent element, as restored from calling thread
	 * @param converter Converter interface to convert saved element IDs to loaded element IDs
	 * @param netEvent Source network event or null
	 */
  public void deserialize(Element parent, XMLSerializeConverter converter, NetworkEvent netEvent)
  {
  	Element elements = XMLUtils.getFirstChildElementByTagName(parent, "elements");
  	
  	clearMap(netEvent);
  	
  	for (Element xmEl : XMLUtils.getChildElementsByTagName(elements, "element"))
  	{
  		MapElement el = new MapElement(xmEl, converter);
  		addMapElement(el);    	
  	}
  	
  	elements = XMLUtils.getFirstChildElementByTagName(parent, "lines");
  	for (Element xmLine : XMLUtils.getChildElementsByTagName(elements, "line"))
  	{
  		LineSegment ls = new LineSegment(xmLine);
  		addLineSegment(ls);
  	}
  	
  	Element groupsEl = XMLUtils.getFirstChildElementByTagName(parent, "groups");
  	m_groupManager.deserializeGroups(groupsEl, converter, this, netEvent);
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
		if (id == null)
			return null;
		
		for (MapElement mapElement : m_mapElements)
		{
			if (mapElement.getID().equals(id))
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
			listener.onMapElementInstanceRemoved(this, mapElement, false);
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
  
	/**
	 * Store information from your component from inside parent element 
	 * @param parent Parent element, as populated by calling thread.  You can add custom XML data as children.
	 */
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
