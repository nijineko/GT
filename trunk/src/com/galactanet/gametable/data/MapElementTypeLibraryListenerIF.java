/*
 * MapElementTypeLibraryListenerIF.java
 *
 * @created 2010-10-10
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
 * Allows to react to changes in a type library - also monitors sub-libraries
 *
 * @author Eric Maziade
 */
public interface MapElementTypeLibraryListenerIF
{
	/**
	 * Called when a new library has been added to the model
	 * @param parentLibrary Parent library
	 * @param newLibrary New library
	 */
	public void onLibraryAdded(MapElementTypeLibrary parentLibrary, MapElementTypeLibrary newLibrary);
	
	/**
	 * Called when a new type has been added to the library 
	 * @param parentLibrary Parent library
	 * @param newType New type
	 */
	public void onMapElementTypeAdded(MapElementTypeLibrary parentLibrary, MapElementTypeIF newType);
	
	/**
	 * Called when a type has been updated 
	 * @param parentLibrary Parent library
	 * @param type Updated type.  The type instance might have changed due to the update - validate through type's getFullyQualifiedName
	 */
	public void onMapElementTypeUpdated(MapElementTypeLibrary parentLibrary, MapElementTypeIF type);
	
	/**
	 * Called when a type has been removed from the library
	 * @param parentLibrary Parent library
	 * @param removedType Removed tyoe
	 */
	public void onMapElementTypeRemoved(MapElementTypeLibrary parentLibrary, MapElementTypeIF removedType);
}
