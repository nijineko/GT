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

package com.galactanet.gametable.data;

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
	 * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstanceAdded(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElementInstance)
	 */
	@Override
	public void onMapElementInstanceAdded(GameTableMap map, MapElement mapElement)
	{
	}

	/*
	 * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstanceRemoved(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElementInstance)
	 */
	@Override
	public void onMapElementInstanceRemoved(GameTableMap map, MapElement mapElement)
	{
	}

	/*
	 * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstancesCleared(com.galactanet.gametable.data.GameTableMap)
	 */
	@Override
	public void onMapElementInstancesCleared(GameTableMap map)
	{
	}
}
