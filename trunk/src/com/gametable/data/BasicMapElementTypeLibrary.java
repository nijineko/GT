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
package com.gametable.data;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.gametable.GametableApp;
import com.gametable.data.MapElementTypeIF.Layer;
import com.gametable.data.net.NetRequestFile;
import com.gametable.data.net.NetRequestFile.FileSourceIF;
import com.gametable.util.UtilityFunctions;

/**
 * Library containing MapElementTypes
 * 
 * @author iffy
 * 
 *         #GT-AUDIT PogLibrary
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

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof BasicMapElementType)
		{
			return getFullyQualifiedName().equals(((BasicMapElementType) obj).getFullyQualifiedName());
		}

		return super.equals(obj);
	}

	/**
	 * Default layer associated with library
	 */
	private Layer														m_defaultLayer	= MapElementTypeIF.Layer.POG;

	/**
	 * Local path to elements within this library
	 */
	private final File											m_libraryPath;

	/**
	 * The parent library.
	 */
	private MapElementTypeLibrary						m_parentLibrary	= null;

	/**
	 * The list of element types found in this library.
	 */
	private final List<BasicMapElementType>	m_types					= new ArrayList<BasicMapElementType>();

	/**
	 * Constructor
	 * 
	 * @param parent Parent library. If null, will use root library
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
    
  	NetRequestFile.registerFileSource(BasicMapElementType.class.getName(), new FileSourceIF() {
			@Override
			public File getFile(String fileSource, String fileName)
			{
				// We registered only for one source, so we can safely ignore it.

				// Split the filename - we're escaping the separator, as it might be a regex character
				String parts[] = fileName.split("\\" + MapElementTypeLibrary.TYPE_SEPARATOR);
				
				if (parts.length != 2)
					return null;
				
				parts[1] = UtilityFunctions.unEscapeString(parts[1]);
				
				MapElementTypeLibrary lib = GametableApp.getCore().getMapElementTypeLibrary().getLibraryFromFQN(parts[0]);
				if (lib == null)
					return null;
				
				// Now data... data is usually a file name that should exist within the library...
				if (lib instanceof BasicMapElementTypeLibrary)
				{
					BasicMapElementTypeLibrary blib = (BasicMapElementTypeLibrary)lib;
					File imageFile = new File(parts[0] + File.separator + parts[1]);
					if (blib.containsBasicMapElementType(imageFile) && imageFile.exists())
						return imageFile;
				}
				
				return null;
			}
		});
  }

	/*
	 * @see com.gametable.data.MapElementTypeLibrary#refresh(boolean)
	 */
	@Override
	public void refresh(boolean recurse) throws IOException
	{
		if (!m_libraryPath.exists())
			return;

		removeNonExistingTypes();

		final File[] files = m_libraryPath.listFiles();

		String errors = null;
		Throwable lastException = null;

		for (File file : files)
		{
			if (file.getName().startsWith("."))
				continue; // skip files starting with a period (.svn, etc.)

			if (file.isFile() && file.canRead())
			{
				if (!containsBasicMapElementType(file))
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
	 * 
	 * @param imageFile Image file to load
	 * @param faceSize Face size
	 * @param defaultLayer Default layer
	 * @param skipUnloaded If true, types that are not loaded are not automatically added to the list
	 * @return MapElementType or null
	 */
	private MapElementTypeIF addElementType(File imageFile, final int faceSize, final Layer defaultLayer, boolean skipUnloaded)
	{
		// String typeFQN = getFullyQualifiedName() + MapElementTypeLibrary.TYPE_SEPARATOR + imageFile.getName();

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
	 * Checks if we contain a file-base basic map element type
	 * 
	 * @param imageFile
	 * @return
	 */
	private boolean containsBasicMapElementType(File imageFile)
	{
		String name;

		try
		{
			name = imageFile.getCanonicalPath();
		}
		catch (IOException e)
		{
			// File does not exist...
			return false;
		}

		for (BasicMapElementType type : m_types)
		{
			if (type.getImageFilename().equals(name))
				return true;
		}

		return false;
	}

	/**
	 * Remove types that no longer match pogs
	 */
	private void removeNonExistingTypes()
	{
		Iterator<BasicMapElementType> iter = m_types.iterator();

		while (iter.hasNext())
		{
			BasicMapElementType type = iter.next();

			File f = new File(type.getImageFilename());
			if (!f.exists())
				iter.remove();
		}
	}

	/**
	 * Add a loaded element type to the library.  If the type is already within the library, it will trigger a replace
	 * 
	 * @param type
	 */
	public void addElementType(BasicMapElementType type)
	{
		// Log.log(Log.SYS, new Exception(this + " added: " + pog));
		boolean replaced = m_types.remove(type);
		
		m_types.add(type);
		Collections.sort(m_types, m_typeComparator);

		if (replaced)
		{
			m_listeners.onMapElementTypeUpdated(this, type);
		}
		else
		{
			m_listeners.onMapElementTypeAdded(this, type);
		}
	}

	/*
	 * @see com.gametable.data.MapElementTypeLibrary#getElementType(java.lang.String)
	 */
	@Override
	public MapElementTypeIF getMapElementType(String fullyQualifiedTypeName)
	{
		if (fullyQualifiedTypeName.startsWith(getFullyQualifiedName()))
		{
			for (BasicMapElementType type : m_types)
				if (type.getFullyQualifiedName().equals(fullyQualifiedTypeName))
					return type;
		}

		return super.getMapElementType(fullyQualifiedTypeName);
	}

	/*
	 * @see com.gametable.data.MapElementTypeLibrary#getElementTypes()
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
	 * @see
	 * com.gametable.data.MapElementTypeLibrary#removeElementType(com.gametable.data.MapElementType)
	 */
	@Override
	public boolean removeElementType(MapElementTypeIF type)
	{
		boolean r = m_types.remove(type);

		m_listeners.onMapElementTypeRemoved(this, type);

		return r;
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
	private Comparator<MapElementTypeIF>	m_typeComparator	= new Comparator<MapElementTypeIF>() {
																														@Override
																														public int compare(MapElementTypeIF pa, MapElementTypeIF pb)
																														{
																															return pa.getDisplayLabel().compareTo(pb.getDisplayLabel());
																														}
																													};
}
