/*
 * GameTableMapAdapter.java
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
 * Adapter class to facilitate nameless implementations of the listener
 *
 * @author Eric Maziade
 * 
 * @audited by themaze75
 */
public class GameTableMapAdapter implements GameTableMapListenerIF
{

	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onMapElementInstanceAdded(com.gametable.data.GameTableMap, com.gametable.data.MapElementInstance)
	 */
	@Override
	public void onMapElementAdded(GameTableMap map, MapElement mapElement, NetworkEvent netEvent) {}

	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onMapElementInstanceRemoved(com.gametable.data.GameTableMap, com.gametable.data.MapElement, boolean)
	 */
	@Override
	public void onMapElementRemoved(GameTableMap map, MapElement mapElement, boolean batch, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onMapElementInstancesRemoved(com.gametable.data.GameTableMap, java.util.List, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onMapElementsRemoved(GameTableMap map, List<MapElement> mapElements, NetworkEvent netEvent) {}

	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onMapElementInstancesCleared(com.gametable.data.GameTableMap)
	 */
	@Override
	public void onMapElementsCleared(GameTableMap map, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onLineSegmentAdded(com.gametable.data.GameTableMap, com.gametable.data.LineSegment, boolean)
	 */
	@Override
	public void onLineSegmentAdded(GameTableMap map, LineSegment lineSegment, boolean batch, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onLineSegmentsAdded(com.gametable.data.GameTableMap, java.util.List)
	 */
	@Override
	public void onLineSegmentsAdded(GameTableMap map, List<LineSegment> lineSegments, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onLineSegmentsCropped(com.gametable.data.GameTableMap, com.gametable.data.MapRectangle, boolean, int, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onEraseLineSegments(GameTableMap map, MapRectangle rect, boolean colorSpecific, int color, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableMapListenerIF#onClearLineSegments(com.gametable.data.GameTableMap, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onClearLineSegments(GameTableMap map, NetworkEvent netEvent) {}	
}
