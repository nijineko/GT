/*
 * MapElementAdapter.java
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

import java.util.Map;

import com.gametable.data.MapElementTypeIF.Layer;
import com.gametable.net.NetworkEvent;

/**
 * Empty implementation of MapElementListenerIF
 *
 * @author Eric Maziade
 */
public class MapElementAdapter implements MapElementListenerIF
{
	/*
	 * @see com.gametable.data.MapElementListenerIF#onAttributeChanged(com.gametable.data.MapElement, java.lang.String, java.lang.String, java.lang.String, boolean, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue, boolean batch, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onAttributesChanged(com.gametable.data.MapElement, java.util.Map, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onAttributesChanged(MapElement element, Map<String, String> attributes, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onLayerChanged(com.gametable.data.MapElement, com.gametable.data.MapElementTypeIF.Layer, com.gametable.data.MapElementTypeIF.Layer, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onLayerChanged(MapElement element, Layer newLayer, Layer oldLayer, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onNameChanged(com.gametable.data.MapElement, java.lang.String, java.lang.String, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onNameChanged(MapElement element, String newName, String oldName, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onPositionChanged(com.gametable.data.MapElement, com.gametable.data.MapCoordinates, com.gametable.data.MapCoordinates, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onPositionChanged(MapElement element, MapCoordinates newPosition, MapCoordinates oldPosition, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onFlipChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onFlipChanged(MapElement element, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onAngleChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onAngleChanged(MapElement element, NetworkEvent netEvent) {}	
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onFaceSizeChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onFaceSizeChanged(MapElement element, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.MapElementListenerIF#onElementTypeChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onElementTypeChanged(MapElement element, NetworkEvent netEvent) {}
}
