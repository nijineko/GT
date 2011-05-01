/*
 * PlayerListenerIF.java
 *
 * @created 2010-09-15
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

import com.gametable.net.NetworkEvent;

/**
 * Listener for Player events
 *
 * @author Eric Maziade
 */
public interface PlayerListenerIF
{
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
