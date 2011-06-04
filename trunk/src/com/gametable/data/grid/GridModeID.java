/*
 * GridModeID.java
 *
 * @created 2011-06-04
 *
 * Copyright (C) 1999-2011 Open Source Game Table Project
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

package com.gametable.data.grid;

public enum GridModeID
{
	HEX, NONE, SQUARES;

	/**
	 * Convert from ordinal to enum
	 * 
	 * @param value
	 * @return
	 */
	public static GridModeID fromOrdinal(int value)
	{
		GridModeID values[] = GridModeID.values();
		if (value < 0 && value >= values.length)
			return NONE;
		
		return values[value];			
	}
}