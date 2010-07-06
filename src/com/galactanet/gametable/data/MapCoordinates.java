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

import org.w3c.dom.Element;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.galactanet.gametable.util.XMLSerializeIF;
import com.maziade.tools.XMLUtils;


/**
 * _Immutable_ point class replacement to enforce type safety regarding map coordinates.
 *
 * @author Eric Maziade
 * 
 * @audited by themaze75
 */
public class MapCoordinates implements XMLSerializeIF
{
	public static final MapCoordinates ORIGIN = new MapCoordinates(0, 0);
	
	/**
	 * Constructor
	 * @param parent Parent XML element
	 */
	public MapCoordinates(Element parent)
	{
		Element loc = XMLUtils.getFirstChildElementByTagName(parent, "loc");
		if (loc == null)
		{
			x = 0;
			y = 0;
			return;
		}

		int i = 0;
		try
		{
			i = Integer.parseInt(loc.getAttribute("x"));
		}
		catch (NumberFormatException e)
		{
			i = 0;
		}
		
		x = i;
		i = 0;
		
		try
		{
			i = Integer.parseInt(loc.getAttribute("y"));
		}
		catch (NumberFormatException e)
		{
			i = 0;
		}
		
		y = i;
	}
	
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
	
	/*
	 * @see com.galactanet.gametable.data.XMLSerializer#deserialize(org.w3c.dom.Element)
	 */
	@Override
	public void deserialize(Element parent)
	{
		// Use constructor instead
		throw new NotImplementedException();		
	}
	
	/*
	 * @see com.galactanet.gametable.data.XMLSerializer#serialize(org.w3c.dom.Element)
	 */
	@Override
	public void serialize(Element parent)
	{
		Element el = parent.getOwnerDocument().createElement("loc");
		
		// Cheating a bit here from XML specs for simplicity
		el.setAttribute("x", String.valueOf(x));
		el.setAttribute("y", String.valueOf(y));
		
		parent.appendChild(el);
	}	
	
	/**
	 * x Coordinate
	 */
	public final int x;
	
	/**
	 * y Coordinate
	 */
	public final int y;
}
