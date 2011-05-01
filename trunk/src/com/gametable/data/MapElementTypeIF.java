/*
 * MapElementType.java
 * 
 * @created 2006-01-05
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

import java.awt.Image;

import com.gametable.net.NetworkConnectionIF;

/**
 * Holds static information shared by all MapElements - creates MapElementInstance for GameTableMap
 * 
 * @author iffy
 * 
 * @audited by themaze75
 */
public interface MapElementTypeIF
{
	// @revise Convert this layer type to a more flexible model
	public enum Layer
	{
		ENVIRONMENT, OVERLAY, POG, UNDERLAY;

		/**
		 * Get Type value from ordinal (numeric) value
		 * 
		 * @param i
		 * @return
		 */
		public static Layer fromOrdinal(int i)
		{
			for (Layer t : Layer.values())
			{
				if (t.ordinal() == i)
					return t;
			}

			return null;
		}
	}

	/**
	 * @return A display label for this MapElement
	 */
	public String getDisplayLabel();

	/**
	 * @return The face size in number of map tiles
	 */
	public int getFaceSize();
	
	/**
	 * @return A fully qualified name that allows us to retrace or rebuild a type from the library
	 */	
	public String getFullyQualifiedName();
	
	/**
	 * Get a rendered image of this map element
	 * @return Image object
	 */
	public Image getImage();

	/**
	 * Returns the layer type associated with this MapElement
	 * 
	 * @return Layer type
	 */
	public Layer getLayerType();
	
	/**
	 * Get an icon representation of this map element (good for giving visual clues on lists)
	 * 
	 * @param size Size of the icon, in pixels. Square icon is assumed.
	 * @return Image
	 */
	public Image getListIcon();

	/**
	 * Verifies if this MapElement has been loaded
	 * 
	 * Mostly used in network communications to see if image transfer is required
	 * 
	 * @return true If this Map Element has been fully loaded. See {@link #load()}
	 */
	public boolean isLoaded();

	/**
	 * If the data does not reside locally, now is the time to request it from the network
	 * 
	 * @param conn Network connection from which we can request data 
	 */
	public void loadDataFromNetwork(NetworkConnectionIF conn);

	/**
	 * Loads (or reloads) the MapElement
	 */
	public void load();
}
