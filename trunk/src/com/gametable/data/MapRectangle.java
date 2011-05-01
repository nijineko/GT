/*
 * MapRectangle.java
 * 
 * @created 2010-06-19
 * 
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.gametable.data;

import java.awt.Rectangle;

/**
 * Immutable Rectangle using Map coordinates
 * 
 * @author Eric Maziade
 */
public class MapRectangle
{
	public final int						height;

	public final MapCoordinates	topLeft;

	public final int						width;

	/**
	 * Creates a new Rectangle from two coordinates
	 * 
	 * @param modelTopLeft Top left map position
	 * @param modelWidth width
	 * @param modelHeight height
	 */
	public MapRectangle(MapCoordinates modelTopLeft, int modelWidth, int modelHeight)
	{
		width = modelWidth;
		height = modelHeight;

		topLeft = modelTopLeft;
	}

	/**
	 * Creates a new Rectangle from two coordinates
	 */
	public MapRectangle(MapCoordinates a, MapCoordinates b)
	{
		final int x = Math.min(a.x, b.x);
		final int y = Math.min(a.y, b.y);

		width = Math.abs(b.x - a.x); // +1
		height = Math.abs(b.y - a.y); // +1

		topLeft = new MapCoordinates(x, y);
	}

	/**
	 * Create a new MapRectangle containing the coordinates of this rectangle and the supplied rectangle
	 * 
	 * @param r Rectangle to combine with
	 * @return Combined rectangle
	 */
	public MapRectangle add(MapRectangle r)
	{
		int x = Math.min(topLeft.x, r.topLeft.x);
		int y = Math.min(topLeft.y, r.topLeft.y);

		int w = Math.max(topLeft.x + width, r.topLeft.x + r.width) - x;
		int h = Math.max(topLeft.y + height, r.topLeft.y + r.height) - y;

		if (x == topLeft.x && y == topLeft.y && w == width && h == height)
			return this;

		if (x == r.topLeft.x && y == r.topLeft.y && w == r.width && h == r.height)
			return r;

		return new MapRectangle(new MapCoordinates(x, y), w, h);
	}
	
	/**
	 * Verifies if the specified point is contained within the rectangle
	 * @param pt point to verify
	 * @return true or false
	 */
	public boolean contains(MapCoordinates modelPoint)
	{		
		if (modelPoint.x > topLeft.x)
			return false;

		if (modelPoint.y > topLeft.y)
			return false;

		if (modelPoint.x < topLeft.x + width)
			return false;

		if (modelPoint.y < topLeft.y + height)
			return false;
		
		return true;
	}

	/**
	 * Determines if this Rectangle intersects with the supplied rectangle.
	 * 
	 * @param r The supplied rectangle
	 * @return true if the rectangles are intersecting
	 */
	public boolean intersects(MapRectangle r)
	{
		return intersects(r.topLeft.x, r.topLeft.y, r.width, r.height);
	}
	/**
	 * Determines if this Rectangle intersects with the supplied rectangle.
	 * 
	 * @param r The supplied rectangle
	 * @return true if the rectangles are intersecting
	 * @deprecated
	 */
	@Deprecated
	public boolean intersects(Rectangle r)
	{
		return intersects(r.x, r.y, r.width, r.height);
	}
	/**
	 * Determines if this Rectangle intersects with the supplied rectangle.
	 * 
	 * @param rX x Coordinate
	 * @param rY y Coordinate
	 * @param rWidth Width
	 * @param rHeight Height
	 * @return true if the rectangles are intersecting
	 */
	private boolean intersects(int rX, int rY, int rWidth, int rHeight)
	{
		int thisWidth = this.width;
		int thisHeight = this.height;

		if (rWidth <= 0 || rHeight <= 0 || thisWidth <= 0 || thisHeight <= 0)
		{
			return false;
		}

		int thisX = this.topLeft.x;
		int thisY = this.topLeft.y;

		rWidth += rX;
		rHeight += rY;
		thisWidth += thisX;
		thisHeight += thisY;

		return ((rWidth < rX || rWidth > thisX) && (rHeight < rY || rHeight > thisY) && (thisWidth < thisX || thisWidth > rX) && (thisHeight < thisY || thisHeight > rY));
	}
}
