/*
 * GameTableFrameAdapter.java
 *
 * @created 2010-08-29
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
import com.gametable.ui.GametableCanvas.BackgroundColor;
import com.gametable.ui.GametableCanvas.GridModeID;


/**
 * Clean encapsulation of GameTableCoreListenerIF (utility class)
 *
 * @author Eric Maziade
 */
public class GameTableCoreAdapter implements GameTableCoreListenerIF
{
	/*
	 * @see com.gametable.ui.GameTableFrameListener#onHostingStarted()
	 */
	@Override
	public void onHostingStarted() {}
	
	/*
	 * @see com.gametable.ui.GameTableFrameListener#onPlayerJoined(com.gametable.data.Player)
	 */
	@Override
	public void onPlayerJoined(Player player) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onActiveMapChange(boolean)
	 */
	@Override
	public void onActiveMapChange(boolean publicMap) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onAllMapElementsLocked(boolean, boolean, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onAllMapElementsLocked(boolean onPublicMap, boolean locked, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onBackgroundChanged(boolean, com.gametable.data.MapElementTypeIF, com.gametable.ui.GametableCanvas.BackgroundColor, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onBackgroundChanged(boolean isMapElementType, MapElementTypeIF elementType, BackgroundColor color, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onGridModeChanged(com.gametable.ui.GametableCanvas.GridModeID, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onGridModeChanged(GridModeID gridMode, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onMapElementLocked(boolean, com.gametable.data.MapElement, boolean, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onMapElementLocked(boolean onPublicMap, MapElement mapElement, boolean locked, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onMapElementsLocked(boolean, java.util.List, boolean, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onMapElementsLocked(boolean onPublicMap, List<MapElement> mapElements, boolean locked, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onPlayerNameChanged(com.gametable.data.Player, java.lang.String, java.lang.String, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onPlayerNameChanged(Player player, String playerName, String characterName, NetworkEvent netEvent) {}
	
	/*
	 * @see com.gametable.data.GameTableCoreListenerIF#onPointingLocationChanged(com.gametable.data.Player, boolean, com.gametable.data.MapCoordinates, com.gametable.net.NetworkEvent)
	 */
	@Override
	public void onPointingLocationChanged(Player player, boolean pointing, MapCoordinates location, NetworkEvent netEvent) {}
	
}
