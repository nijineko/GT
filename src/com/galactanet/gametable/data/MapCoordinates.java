/*
 * MapCoordinates.java
 *
 * @created 2010-06-19
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
 * _Immutable_ point class replacement to enforce type safety regarding map coordinates.
 *
 * @author Eric Maziade
 * 
 * #GT-AUDIT MapCoordinates
 */
public class MapCoordinates
{
	public static final MapCoordinates ORIGIN = new MapCoordinates(0, 0);
	
	/**
	 * Constructor 
	 * @param x Coordinate in map units
	 * @param y Coordinate in map units
	 */
	public MapCoordinates(int mapX, int mapY)
	{
		this.x = mapX;
		this.y = mapY;
	}	
	
	/**
	 * Create a new instance of MapCoordinates, moved by specified deltas
	 * @param deltaX
	 * @param deltaY
	 * @return
	 */
	public MapCoordinates delta(int deltaX, int deltaY)
	{
		if (deltaX == 0 && deltaY == 0)
			return this;
		
		return new MapCoordinates(x + deltaX, y + deltaY);
	}
	
	/**
	 * Create a new instance of MapCoordinates, moved by specified deltas
	 * @param delta
	 * @return
	 */
	public MapCoordinates delta(MapCoordinates delta)
	{
		return delta(delta.x, delta.y);
	}
	
	/**
	 * Calculates the distance between this coordinate and another one
	 * @param pt Point to calculate distance to
	 * @return distance in map coordinates 
	 */
	public double distance(MapCoordinates pt)
	{
		double dx = pt.x - this.x;
		double dy = pt.y - this.y;
		
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	
	public final int x;
	public final int y;
}
