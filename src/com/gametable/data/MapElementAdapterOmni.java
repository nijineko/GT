/*
 * MapElementAdapterOmni.java
 *
 * @created 2010-08-05
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
import java.util.Map;

import com.gametable.data.MapElementTypeIF.Layer;
import com.gametable.net.NetworkEvent;

/**
 * A specialized listener adpater that only dispatches to registered listeners.
 * Faciliates registering to multiple objects at only one place
 *
 * @author Eric Maziade
 */
public class MapElementAdapterOmni implements MapElementListenerIF
{
	/*
	 * @see com.gametable.data.MapElementListenerIF#onAttributeChanged(com.gametable.data.MapElement, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue, boolean batch, NetworkEvent netEvent)
	{
		for (MapElementListenerIF listener : m_listeners)
			listener.onAttributeChanged(element, attributeName, newValue, oldValue, batch, netEvent);
		
	}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onAttributesChanged(com.gametable.data.MapElement, java.util.Map, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onAttributesChanged(MapElement element, Map<String, String> attributes, NetworkEvent netEvent)
	{
		for (MapElementListenerIF listener : m_listeners)
			listener.onAttributesChanged(element, attributes, netEvent);		
	}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onLayerChanged(com.gametable.data.MapElement, com.gametable.data.MapElementTypeIF.Layer, com.gametable.data.MapElementTypeIF.Layer, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onLayerChanged(MapElement element, Layer newLayer, Layer oldLayer, NetworkEvent netEvent)
	{
		for (MapElementListenerIF listener : m_listeners)
			listener.onLayerChanged(element, newLayer, oldLayer, netEvent);
	}
	/*
	 * @see com.gametable.data.MapElementListenerIF#onNameChanged(com.gametable.data.MapElement, java.lang.String, java.lang.String)
	 */
	@Override
	public void onNameChanged(MapElement element, String newName, String oldName, NetworkEvent netEvent)
	{
		for (MapElementListenerIF listener : m_listeners)
			listener.onNameChanged(element, newName, oldName, netEvent);
	}
	
	/**
   * Adds a MapElementListenerIF to this element
   * @param listener Listener to call when something changes within the map
   */
  public void addListener(MapElementListenerIF listener)
  {
  	m_listeners.remove(listener);
  	m_listeners.add(listener);
  }
  
  /**
   * Removes a listener from this element
   * @param listener Listener to remove
   * @return True if listener was found and removed
   */
  public boolean removeListener(MapElementListenerIF listener)
  {
  	return m_listeners.remove(listener);
  }

  /**
   * List of map element listeners
   */
  private List<MapElementListenerIF> m_listeners = new ArrayList<MapElementListenerIF>();
  
  /*
   * @see com.gametable.data.MapElementListenerIF#onPositionChanged(com.gametable.data.MapElement, com.gametable.data.MapCoordinates, com.gametable.data.MapCoordinates, com.gametable.net.NetworkEvent)
   */
  @Override
  public void onPositionChanged(MapElement element, MapCoordinates newPosition, MapCoordinates oldPosition, NetworkEvent netEvent)
  {
  	for (MapElementListenerIF listener : m_listeners)
			listener.onPositionChanged(element, newPosition, oldPosition, netEvent);
  }  
  
  /*
   * @see com.gametable.data.MapElementListenerIF#onFlipChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
   */
  @Override
  public void onFlipChanged(MapElement element, NetworkEvent netEvent)
  {
  	for (MapElementListenerIF listener : m_listeners)
			listener.onFlipChanged(element, netEvent);  	
  }
  
  /*
   * @see com.gametable.data.MapElementListenerIF#onAngleChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
   */
  @Override
  public void onAngleChanged(MapElement element, NetworkEvent netEvent)
  {
  	for (MapElementListenerIF listener : m_listeners)
			listener.onAngleChanged(element, netEvent);  	
  }
  
  /*
   * @see com.gametable.data.MapElementListenerIF#onFaceSizeChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
   */
  @Override
  public void onFaceSizeChanged(MapElement element, NetworkEvent netEvent)
  {
  	for (MapElementListenerIF listener : m_listeners)
			listener.onFaceSizeChanged(element, netEvent);
  }
  
  /*
   * @see com.gametable.data.MapElementListenerIF#onElementTypeChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
   */
  @Override
  public void onElementTypeChanged(MapElement element, NetworkEvent netEvent)
  {
  	for (MapElementListenerIF listener : m_listeners)
			listener.onElementTypeChanged(element, netEvent);
  }
}
