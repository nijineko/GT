/*
 * MapElementTypeLibrary.java
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
package com.galactanet.gametable.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * Basic library, used as root.
 * 
 * @author iffy
 * 
 * @audited by themaze75
 */
public class MapElementTypeLibrary
{
	/**
	 * The fully qualified name for this library, build from its parent's names
	 */
	private String m_fullyQualifiedName = null;
	
    /**
     * A list of child libraries, sorted by name.
     */
    private final List<MapElementTypeLibrary> m_childLibraries;
    
    /**
     * Listener that dispatches messages from all sub libraries
     */
    protected final MapElementTypeLibraryAdapterOmni m_listeners;
    
    /**
     * Unmodifiable, synchronized list of children
     */
    private final List<MapElementTypeLibrary> m_childLibrariesUnmodifiable; 

    /**
     * The short name of this library. Unique within the parent library.
     */
    private String m_libraryName = "root";

    /**
     * Instance of the root library
     */
    private static MapElementTypeLibrary g_masterLibrary = null;
    
    /**
     * Get the main library
     * 
     */
    public static MapElementTypeLibrary getMasterLibrary()
    {
    	if (g_masterLibrary == null)
    		g_masterLibrary = new MapElementTypeLibrary();
    	
    	return g_masterLibrary;
    }

    /**
     * Protected constructor
     */
    protected MapElementTypeLibrary()
    {
    	m_listeners = new MapElementTypeLibraryAdapterOmni();
      m_childLibraries = new ArrayList<MapElementTypeLibrary>();
    	m_childLibrariesUnmodifiable = Collections.unmodifiableList(m_childLibraries);
    }
    
    /**
     * Adds a sub library to this library
     * @param library Library implementation
     */
    public void addSubLibrary(MapElementTypeLibrary library)
    {
    	m_childLibraries.add(library);
    	
    	library.addListener(m_listeners);
    	
    	m_listeners.onLibraryAdded(this, library);
    }

    /**
     * Gets a sub library
     * 
     * @param libraryName Name of library to get.
     * 
     * @return Library found or null.
     *
     */
    public MapElementTypeLibrary getSubLibrary(final String libraryName)
    {
    	for (MapElementTypeLibrary library : m_childLibraries)
    	{
    		if (library.getName().equals(libraryName))
    			return library;
    	}
    	
      return null;
    }
    
    /**
     * Gets a library from FQL
     * 
     * @param fullyQualifiedLibraryName Fully qualified library name
     * 
     * @return Library found or null.
     * 
     */
    public MapElementTypeLibrary getLibraryFromFQN(final String fullyQualifiedLibraryName)
    {
    	for (MapElementTypeLibrary library : m_childLibraries)
    	{
    		if (library.getFullyQualifiedName().equals(fullyQualifiedLibraryName))
    			return library;
    		
    		MapElementTypeLibrary search = library.getLibraryFromFQN(fullyQualifiedLibraryName);
    		if (search != null)
    			return search;
    	}
    	
      return null;
    }

    /**
     * @return Returns an unmodifiable list of sub libraries
     */
    public List<MapElementTypeLibrary> getSubLibraries()
    {
    	return m_childLibrariesUnmodifiable;    
    }

    /**
     * @return Returns the name of this library.
     */
    public String getName()
    {
        return m_libraryName;
    }

    /**
     * @return Returns the parent library.  Null if this is the root library.
     */
    public MapElementTypeLibrary getParent()
    {
        return null;
    }

    /**
     * Looks through this library and its children to find a specified type
     *    
     * @param fullyQualifiedTypeName Unique type name
     * @return MapElementType or null, if none found
     */
    public MapElementTypeIF getMapElementType(final String fullyQualifiedTypeName)
    {
    	for (MapElementTypeLibrary library : m_childLibraries)
    	{
    		MapElementTypeIF type = library.getMapElementType(fullyQualifiedTypeName);
    		if (type != null)
    			return type;
    	}
    	
    	return null;
    }

    /**
     * @return Returns a list of element types directly under this library
     */
    public List<MapElementTypeIF> getElementTypes()
    {
        return Collections.emptyList();
    }
    
    /**
     * Remove a map element type from this library
     * @param type Type to remove
     * @return true if type has been removed, false if it was not within this library
     */    
    public boolean removeElementType(final MapElementTypeIF type) 
    {
    	return false;       
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "[MapElementTypeLibrary " + getFullyQualifiedName() + "]"; 
    }
    
    /**
     * Refresh the content of this library
     * @param recurse If true, will also refresh child libraries
     * @throws IOException if something fails.  In this case, throwing should be non-breaking; Accumulating errors and throwing them at the end is OK.
     */
    public void refresh(boolean recurse) throws IOException
    {
    	if (recurse)
    	{
    		for (MapElementTypeLibrary library : m_childLibraries)
    			library.refresh(recurse);
    	}
    }
    
    /**
     * Creates a place holder type to use when we absolutely need a specific type
     * @param typeName Unique name for place holder
     * @param faceSize Number of tiles on a face
     */
    public MapElementTypeIF createPlaceholderType(String typeName, int faceSize)
    {
    	if (typeName == null)
    		return null;
    	
    	int pos = typeName.indexOf(TYPE_SEPARATOR);
    	if (pos < 0)
    		return null;
    		
    	String libraryName = typeName.substring(0, pos);
    	
    	String placeHolderFor = "";
    	if (pos < typeName.length())
    		placeHolderFor = typeName.substring(pos + 1);

    	MapElementTypeLibrary lib = getLibraryFromFQN(libraryName);    	
    	
    	// todo If we can't find the library, we'll need to somehow create it!
    	if (lib == null)
    	{
    		throw new IllegalStateException("Library not found. Design not robust enough yet :)");
    	}
    	
    	// todo I can't arbitrarily decide type - I must be able to get type from type FQN. (only one type for now, but I don't want to break compatibility later)
    	BasicMapElementTypeLibrary basicLib = (BasicMapElementTypeLibrary)lib;  
    	
    	BasicMapElementType type = new BasicMapElementType(basicLib, faceSize, placeHolderFor, Layer.UNDERLAY);
    	basicLib.addElementType(type);
    	
    	return type;
    }
    
    /**
     * Rebuilds fully qualified name for this library. 
     */
    private void rebuildFullyQualifiedName()
    {
    	if (this == g_masterLibrary)
    	{
    		m_fullyQualifiedName = "";
    		return;
    	}
    	
			if (getParent() == null)
    	{
    		m_fullyQualifiedName = UtilityFunctions.normalizeName(m_libraryName);
    		return;
    	}
    	
    	String fqn = UtilityFunctions.normalizeName(m_libraryName);
    	
    	MapElementTypeLibrary lib = getParent();
    	
    	while (lib != null && lib != g_masterLibrary)
    	{    		
    		fqn = UtilityFunctions.normalizeName(lib.getName()) + LIBRARY_SEPARATOR + fqn;
    		lib = lib.getParent();
    	}
    	
    	m_fullyQualifiedName = fqn;
    }
    
    /**
     * Sets the display name for this library.  Automatically rebuilds fully qualified name.
     * @param name Library's name
     */
    protected final void setLibraryName(String name)
    {
    	m_libraryName = name;
    	rebuildFullyQualifiedName();
    }
    
    /**
     * Returns the fully qualified name of this library.  This method is final.  To rebuild the FQN, {@link #rebuildFullyQualifiedName()}
     * @return FQN string
     */
    public final String getFullyQualifiedName()
    {
    	return m_fullyQualifiedName;
    }
    
    /**
     * Adds a MapElementTypeLibraryListenerIF to this library
     * @param listener Listener to call when something changes within the library
     */
    public void addListener(MapElementTypeLibraryListenerIF listener)
    {
    	m_listeners.addListener(listener);
    }
    
    /**
     * Removes a listener from this library
     * @param listener Listener to remove
     * @return True if listener was found and removed
     */
    public boolean removeListener(MapElementTypeLibraryListenerIF listener)
    {
    	return m_listeners.removeListener(listener);
    }

    
    /**
     * Character used to separate library names 
     */
    public static final String LIBRARY_SEPARATOR = ":";
    
    /**
     * Character used to separate type names from library names 
     */
    public static final String TYPE_SEPARATOR = "+";
}
