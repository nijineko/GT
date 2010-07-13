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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.galactanet.gametable.data.MapElementTypeIF.Layer;

/**
 * Library containing MapElementTypes
 * 
 * @author iffy
 * 
 * #GT-AUDIT PogLibrary
 */
public class BasicMapElementTypeLibrary extends MapElementTypeLibrary
{
    /**
     * Extract library name from path name
     * 
     * @param file Directory to use to calculate name. 
     * @return Name of this library node.
     */
    private String getNameFromDirectory(final File file)
    {
    	return file.getName();
    }

    /**
     * Default layer associated with library
     */
    private Layer        m_defaultLayer       = MapElementTypeIF.Layer.POG;

    /**
     * Local path to elements within this library
     */
    private final File  m_libraryPath;
    
    /**
     * The parent library.
     */
    private MapElementTypeLibrary m_parentLibrary            = null;

    /**
     * The list of element types found in this library.
     */
    private final List<BasicMapElementType> m_types              = new ArrayList<BasicMapElementType>();

    /**
     * Constructor
     * @param parent Parent library.  If null, will use root library
     * @param path Access path for this library
     * @param defaultLayer Default layer for types of this library
     * 
     * @throws IOException
     */
    public BasicMapElementTypeLibrary(MapElementTypeLibrary parent, File path, Layer defaultLayer) throws IOException    
    {
    	super();
    	
    	if (!path.exists() && !path.mkdir()) 
    	{
    		throw new IOException("Failed to find or create directory " + path.getAbsolutePath());                  
    	}
    	
	    if (!path.canRead() || !path.isDirectory())
	    {
	        throw new IOException("Cannot read from " + path.getAbsolutePath());
	    }

	    if (parent == null)
	    	m_parentLibrary = MapElementTypeLibrary.getMasterLibrary();
	    else
	    	m_parentLibrary = parent;
	    
      m_libraryPath = path.getAbsoluteFile();
      
      setLibraryName(getNameFromDirectory(m_libraryPath));
      
      m_defaultLayer = defaultLayer;
      
      refresh(true);
    }

  /*
   * @see com.galactanet.gametable.data.MapElementTypeLibrary#refresh(boolean)
   */
  @Override
	public void refresh(boolean recurse) throws IOException
	{
		if (!m_libraryPath.exists())
			return;

		final File[] files = m_libraryPath.listFiles();

		String errors = null;
		Throwable lastException = null;

		for (File file : files)
		{
			if (file.getName().startsWith("."))
				continue; // skip files starting with a period (.svn, etc.)

			if (file.isFile() && file.canRead())
			{
				addElementType(file, 1, m_defaultLayer, false);
			}
			else if (file.isDirectory() && file.canRead() && recurse)
			{
				try
				{
					addSubLibrary(file);
				}
				catch (IOException e)
				{
					lastException = e;
					if (errors != null)
						errors += "\n" + e.getMessage();
				}
			}
		}

		// Go through all types and reload them
//		for (BasicMapElementType type : m_types)
//		{
//			try
//			{
//				type.load();
//			}
//			catch (Exception e)	// TODO should be IOException.  Should be thrown by load in the model
//			{
//				lastException = e;
//				if (errors != null)
//					errors += "\n" + e.getMessage();
//			}
//		}

		try
		{
			super.refresh(recurse);
		}
		catch (IOException e)
		{
			lastException = e;
			if (errors != null)
				errors += "\n" + e.getMessage();
		}

		if (errors != null)
			throw new IOException(errors, lastException);
	}

    /**
     * Adds library to this library, ensuring it doesn't already exist.
     * 
     * @param path Library path name
     * 
     * @return Newly created library
     */
    private BasicMapElementTypeLibrary addSubLibrary(File path) throws IOException
    {
    	// Check for existence first
    	
    	String libName = getNameFromDirectory(path);
    	MapElementTypeLibrary lib = getSubLibrary(libName);
    	if (lib != null)
    		return null;           
    	
    	BasicMapElementTypeLibrary child = new BasicMapElementTypeLibrary(this, path, m_defaultLayer);      
    	addSubLibrary(child);
    	
    	return child;
    }

    /**
     * Adds element type to library
     * @param imageFile Image file to load
     * @param faceSize Face size 
     * @param defaultLayer Default layer 
     * @param skipUnloaded If true, types that are not loaded are not automatically added to the list
     * @return MapElementType or null 
     */
    private MapElementTypeIF addElementType(File imageFile, final int faceSize, final Layer defaultLayer, boolean skipUnloaded)
    {
    	//String typeFQN = getFullyQualifiedName() + MapElementTypeLibrary.TYPE_SEPARATOR + imageFile.getName();
    	
    	if (m_types.contains(imageFile))
    		return null;
    	      
    	BasicMapElementType type = new BasicMapElementType(this, imageFile, faceSize, defaultLayer);
            
      if (!skipUnloaded || type.isLoaded())
      {
          addElementType(type);
      }
      
      return type;      
    }

    /**
     * Add a loaded element type to the library
     * @param type
     */
		public void addElementType(BasicMapElementType type)
		{
			// Log.log(Log.SYS, new Exception(this + " added: " + pog));
			m_types.add(type);
			Collections.sort(m_types, m_typeComparator);
		}
		
		/*
		* @see com.galactanet.gametable.data.MapElementTypeLibrary#getElementType(java.lang.String)
		*/
		@Override
		public MapElementTypeIF getElementType(String fullyQualifiedTypeName)
		{
			if (fullyQualifiedTypeName.startsWith(getFullyQualifiedName()))
			{
				for (BasicMapElementType type : m_types)
					if (type.getFullyQualifiedName().equals(fullyQualifiedTypeName))
						return type;
			}
			
			return super.getElementType(fullyQualifiedTypeName);
		}

    /*
    * @see com.galactanet.gametable.data.MapElementTypeLibrary#getElementTypes()
    */
    @Override
    public List<MapElementTypeIF> getElementTypes()
    {
    	return new ArrayList<MapElementTypeIF>(m_types);
    }

    /**
     * @return Returns the parent library.
     */
    @Override
    public MapElementTypeLibrary getParent()
    {
        return m_parentLibrary;
    }
    
    /*
    * @see com.galactanet.gametable.data.MapElementTypeLibrary#removeElementType(com.galactanet.gametable.data.MapElementType)
    */
    @Override
    public boolean removeElementType(MapElementTypeIF type)
    {
    	return m_types.remove(type);
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "[BasicMapElementType " + getFullyQualifiedName() + "]";
    }
    
    /**
     * Comparator to keep element types ordered
     */
    private Comparator<MapElementTypeIF> m_typeComparator = new Comparator<MapElementTypeIF>()        
    {            
      @Override            
      public int compare(MapElementTypeIF pa, MapElementTypeIF pb)
      {
          return pa.getDisplayLabel().compareTo(pb.getDisplayLabel());
      }
  };	
}
