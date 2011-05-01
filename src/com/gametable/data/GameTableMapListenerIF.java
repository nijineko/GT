/*
 * GameTableMapListenerIF.java
 *
 * @created 2010-06-20
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

import java.util.List;

import com.gametable.net.NetworkEvent;

/**
 * Listener interface to receive GameTableMap notifications
 *
 * @author Eric Maziade
 * 
 * @audited by themaze75
 */
public interface GameTableMapListenerIF
{
	/**
	 * Called when a map element instance has been added to the map
	 * @param map The triggering map
	 * @param mapElement The map element that has been added
	 * @param netEvent Network event information that triggered the event or null
	 */
	public void onMapElementAdded(GameTableMap map, MapElement mapElement, NetworkEvent netEvent);
	
	/**
	 * Called when all map element instances have been removed in one call (onMapElementInstanceRemoved will also be called for individual items)
	 * @param map The triggering map
	 * @param netEvent Network event information that triggered the event or null
	 */
	public void onMapElementsCleared(GameTableMap map, NetworkEvent netEvent);
	
	/**
	 * Called when a map element instance has been removed from the map
	 * @param map The triggering map
	 * @param mapElement The map element that has been removed
	 * @param batch Set to true if this instance is removed because of a call to a method removing multiple mapElemtns (onMapElementInstancesCleared or onMapElementInstancesRemoved)  
	 * Allows to optimize by doing batch process through another listener when possible (onMapElementInstancesCleared will be called first)
	 * @param netEvent Network event information that triggered the event or null
	 */
	public void onMapElementRemoved(GameTableMap map, MapElement mapElement, boolean batch, NetworkEvent netEvent);
	
	/**
	 * Called when a batch of map element instances have been removed from the map. (onMapElementInstanceRemoved will also be called for individual items)
	 * @param map The triggering map
	 * @param mapElements The list of map elements that have been removed
	 * @param netEvent Network event information that triggered the event or null
	 */
	public void onMapElementsRemoved(GameTableMap map, List<MapElement> mapElements, NetworkEvent netEvent);
	
	/**
	 * Called when a line segment has been added to the map
	 * @param map The triggering map
	 * @param lineSegment The segment that has been added
	 * @param batch If true, this method has been triggered as part of an 'onLineSegmentsAdded' call
	 * Allows to optimize by doing batch process through another listener when possible (onLineSegmentsAdded will be called first) 
	 * @param netEvent If non-null, contains information about the network event that triggered the change.
	 */
	public void onLineSegmentAdded(GameTableMap map, LineSegment lineSegment, boolean batch, NetworkEvent netEvent);
	
	/**
	 * Called when multiple line segments have been added to the map in a single call (single segment method will also be called)
	 * @param map The triggering map
	 * @param lineSegments The list of segment that have been added
	 * @param netEvent If non-null, contains information about the network event that triggered the change.
	 */
	public void onLineSegmentsAdded(GameTableMap map, List<LineSegment> lineSegments, NetworkEvent netEvent);
	
	/**
	 * An erase operation has been applied on the map.  All lines in the map might have been impacted.
	 * @param map The triggering map
	 * @param rect Rectangular region of the map to erase
	 * @param colorSpecific If true, will erase line segments of matching color
	 * @param color Color of the line segments to erase (if colorSpecific is true)
	 * @param netEvent If non-null, contains information about the network event that triggered the change.
	 */
	public void onEraseLineSegments(GameTableMap map, MapRectangle rect, boolean colorSpecific, int color, NetworkEvent netEvent);
	
	/**
	 * All lines were cleared from a given map
	 * @param map The triggering map
	 * @param netEvent If non-null, contains information about the network event that triggered the change.
	 */
	public void onClearLineSegments(GameTableMap map, NetworkEvent netEvent);
}
