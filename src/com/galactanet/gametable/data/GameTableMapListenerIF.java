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

package com.galactanet.gametable.data;

/**
 * Listener interface to receive GameTableMap notifications
 *
 * @author Eric Maziade
 */
public interface GameTableMapListenerIF
{
	/**
	 * Called when a map element instance has been added to the map
	 * @param map The triggering map
	 * @param mapElement The map element that has been added
	 */
	public void onMapElementInstanceAdded(GameTableMap map, MapElementInstance mapElement);
	
	/**
	 * Called when all map element instances have been removed in one call
	 * @param map The triggering map
	 */
	public void onMapElementInstancesCleared(GameTableMap map);
	
	/**
	 * Called when a map element instance has been removed from the map
	 * @param map The triggering map
	 * @param mapElement The map element that has been removed
	 */
	public void onMapElementInstanceRemoved(GameTableMap map, MapElementInstance mapElement);

}
