/*
 * GameTableFrameListener.java
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
 * todo: comment
 *
 * @author Eric Maziade
 */
public interface GameTableCoreListenerIF
{
	/**
	 * Network game started with current user as host
	 */
	public void onHostingStarted();
	
	/**
	 * A player joined the network game
	 * @param player
	 */
	public void onPlayerJoined(Player player);

	/**
	 * The active map has been changed
	 * @param publicMap true if the map has been change to the public map
	 */
	public void onActiveMapChange(boolean publicMap);
	
	/**
	 * Called when the background is changed
	 * @param isMapElementType True if it was changed to a new map element type, false for a new color
	 * @param elementType MapElementType to use for background tile
	 * @param color BackgroundColor to use for color
	 * @param netEvent NetworkEvent if the change was triggered by network call or null
	 */
	public void onBackgroundChanged(boolean isMapElementType, MapElementTypeIF elementType, BackgroundColor color, NetworkEvent netEvent);
	
	/**
	 * Called when the grid mode has changed
	 * @param gridMode New grid mode ID
	 * @param netEvent NetworkEvent if the change was triggered by network call or null
	 */
	public void onGridModeChanged(GridModeID gridMode, NetworkEvent netEvent);
	
	/**
	 * Called when all map elements have been locked or unlocked
	 * @param onPublicMap If true, the operation occurred on the public map, otherwise on the private map 
	 * @param locked True if elements where locked, false if unlocked
	 * @param netEvent NetworkEvent if the change was triggered by network call or null
	 */
	public void onAllMapElementsLocked(boolean onPublicMap, boolean locked, NetworkEvent netEvent);
	
	/**
	 * Called when a batch of map elements have been locked or unlocked
	 * @param onPublicMap If true, the operation occurred on the public map, otherwise on the private map
	 * @param mapElements List of map elements that were locked or unlocked
	 * @param locked True if elements where locked, false if unlocked
	 * @param netEvent NetworkEvent if the change was triggered by network call or null
	 */
	public void onMapElementsLocked(boolean onPublicMap, List<MapElement> mapElements, boolean locked, NetworkEvent netEvent);
	
	/**
	 * Called when a single map element has been locked or unlocked
	 * @param onPublicMap If true, the operation occurred on the public map, otherwise on the private map
	 * @param mapElement Map elements that was locked or unlocked
	 * @param locked True if element was locked, false if unlocked
	 * @param netEvent NetworkEvent if the change was triggered by network call or null
	 */
	public void onMapElementLocked(boolean onPublicMap, MapElement mapElement, boolean locked, NetworkEvent netEvent);
	
	/**
	 * Called when a player changes his name(s)
	 * @param player Player that has been changed
	 * @param playerName New player name
	 * @param characterName New character name
	 * @param netEvent Network event that triggered the change or null.
	 */
	public void onPlayerNameChanged(Player player, String playerName, String characterName, NetworkEvent netEvent);
	
	/**
	 * Called when a player's pointing location information changes
	 * 
	 * @param player Player instance who's data has changed
	 * @param pointing True if player is now pointing
	 * @param location Current position in map coordinates
	 * @param netEvent Network event information or null if the event was not triggered by network
	 */
	public void onPointingLocationChanged(Player player, boolean pointing, MapCoordinates location, NetworkEvent netEvent);
}
