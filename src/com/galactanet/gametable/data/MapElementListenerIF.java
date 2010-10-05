/*
 * MapElementListenerIF.java
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

package com.galactanet.gametable.data;

import java.util.Map;

import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.net.NetworkEvent;

/**
 * Interface allowing to react to changes within an element
 *
 * @author Eric Maziade
 */
public interface MapElementListenerIF
{
	/**
	 * Layer has changed
	 * @param element Element that has changed
	 * @param newLayer new value
	 * @param oldLayer old value
	 */
	public void onLayerChanged(MapElement element, Layer newLayer, Layer oldLayer);
	
	/**
	 * Name has changed
	 * @param element Element that has changed
	 * @param newName new value
	 * @param oldName old value
	 * @param netEvent Triggering network event or null
	 */
	public void onNameChanged(MapElement element, String newName, String oldName, NetworkEvent netEvent);
	
	/**
	 * Attribute value changed
	 * @param element Element that has changed
	 * @param attributeName Name of the attribute that changed
	 * @param newValue New value (if null, the attribute has been removed)
	 * @param oldValue Old value (if null, the attribute has been added)
	 * @param batch True if this trigger is called as part of batch processing
	 * @param netEvent Triggering network event or null
	 */
	public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue, boolean batch, NetworkEvent netEvent);
	
	/**
	 * Multiple attributes have changed (onAttributeChanged will also be called with the 'batch' parameter set to true
	 * @param element Element that has changed
	 * @param attributes List of attribute name + value pairs (value is null if the attribute has been removed)
	 * @param netEvent Triggering network event or null
	 */
	public void onAttributesChanged(MapElement element, Map<String, String> attributes, NetworkEvent netEvent);
	
	/**
	 * Name has changed
	 * @param element Element that has changed
	 * @param newPosition new position
	 * @param oldPosition old position
	 * @param netEvent Triggering network event or null
	 */
	public void onPositionChanged(MapElement element, MapCoordinates newPosition, MapCoordinates oldPosition, NetworkEvent netEvent);
	
	/**
	 * Flip state has changed
	 * @param element Element that has changed
	 * @param netEvent Triggering network event or null
	 */
	public void onFlipChanged(MapElement element, NetworkEvent netEvent);
	
	/**
	 * Angle state has changed
	 * @param element Element that has changed
	 * @param netEvent Triggering network event or null
	 */
	public void onAngleChanged(MapElement element, NetworkEvent netEvent);
}
