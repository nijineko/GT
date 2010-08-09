/*
 * MapElementAdapter.java
 *
 * @created 2010-08-05
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

import com.galactanet.gametable.data.MapElementTypeIF.Layer;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class MapElementAdapter implements MapElementListenerIF
{
	/*
	 * @see com.galactanet.gametable.data.MapElementListenerIF#onAttributeChanged(com.galactanet.gametable.data.MapElement, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue) {}
	
	/*
	 * @see com.galactanet.gametable.data.MapElementListenerIF#onLayerChanged(com.galactanet.gametable.data.MapElement, com.galactanet.gametable.data.MapElementTypeIF.Layer, com.galactanet.gametable.data.MapElementTypeIF.Layer)
	 */
	@Override
	public void onLayerChanged(MapElement element, Layer newLayer, Layer oldLayer) {}
	
	/*
	 * @see com.galactanet.gametable.data.MapElementListenerIF#onNameChanged(com.galactanet.gametable.data.MapElement, java.lang.String, java.lang.String)
	 */
	@Override
	public void onNameChanged(MapElement element, String newName, String oldName) {}
}
