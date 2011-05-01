/*
 * MapElement.java
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
package com.galactanet.gametable.data;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.net.NetRequestFile;
import com.galactanet.gametable.data.net.NetRequestFile.FileRequestListenerIF;
import com.galactanet.gametable.net.NetworkConnectionIF;
import com.galactanet.gametable.util.ImageCache;
import com.galactanet.gametable.util.Images;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * Holds static information shared by all MapElements - creates MapElementInstance for GameTableMap
 * 
 * @author iffy
 * 
 * @audited by themaze75
 */
public class BasicMapElementType implements MapElementTypeIF
{
	// --- Members ---------------------------------------------------------------------------------------------------

	/**
	 * Cache for place holder images
	 */
	private static Map<Integer, Image>	g_placeHolderCache	= new HashMap<Integer, Image>();

	/**
	 * Get a place holder image for Pogs with no visual representation.
	 * 
	 * @param faceSize Size of a face, in squares
	 * @return Image
	 */
	public static Image getPlaceHolderImage(int faceSize)
	{
		Image image = g_placeHolderCache.get(faceSize);
		if (image != null)
			return image;

		File placeholderImage;

		// Load from the basic images
		// NB : We default to the largest image to get better scaling quality
		
		// @revise I'd pretty much like these images to be within the jar instead of external

		placeholderImage = getPlaceHolderImageFile(faceSize);

		image = ImageCache.getImage(placeholderImage);

		// If it is bigger, we'll resize

		if (faceSize > 3)
		{
			final int size = faceSize * GameTableMap.getBaseTileSize();
			image = Images.getScaledInstance(image, size, size);
		}

		g_placeHolderCache.put(faceSize, image);

		return image;
	}

	private static File getPlaceHolderImageFile(int faceSize)
	{
		File placeholderImage;
		switch (faceSize)
		{
		case 1:
			placeholderImage = new File("assets/pog_unk_1.png");

		case 2:
			placeholderImage = new File("assets/pog_unk_2.png");
			break;

		case 3:
		default:
			placeholderImage = new File("assets/pog_unk_3.png");
			break;
		}
		return placeholderImage;
	}

	/**
	 * The size of one of the sides of this element, in map units.
	 */
	private int						m_faceSize;

	/**
	 * The image used by this element
	 */
	private Image					m_image;

	/**
	 * The name of the image file this element is using
	 */
	private File		m_imageFile;
	
	/**
	 * Canonical image file path
	 */
	private String 	m_imageFileName;

	/**
	 * If non-null, this type is a placeholder for this image
	 */
	private String m_placeHolderFor;

	/**
	 * The layer type this Map Element is linked to
	 */
	private final Layer		m_layerType;

	/**
	 * Scaled image of this map element to use as icon
	 */
	private Image					m_listIcon;

	/**
	 * The load status flag of this element
	 */
	private boolean				m_loaded	= false;
	
	/**
	 * Fully qualified name uniquely identifying the type within the library
	 */
	private String m_fullyQualifiedName = null;
	
	/**
	 * Map element type library we are part of
	 */
	private final BasicMapElementTypeLibrary m_library;

	/**
	 * @revise this constructor forces the use of an image + file name + loading through local path [...] I would advise a
	 *         different method as to allow variety. Perhaps a plain, basic constructor should do? Or an implementing
	 *         classes of MapElement? (MapElement would then be an abstract)
	 * 
	 *         Constructor
	 * 
	 * @param library This type's parent library
	 * @param imageFile Image file to load
	 * @param faceSize Size of one of this element's face in map units
	 * @param layerType Layer type one such element is associated
	 */
	protected BasicMapElementType(BasicMapElementTypeLibrary library, File imageFile, int faceSize, Layer layerType)
	{
		if (layerType == null)
			throw new IllegalArgumentException("Invalid layerType");
		
		m_imageFile = imageFile;
		try
		{
			m_imageFileName = imageFile.getCanonicalPath();
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Invalid file " + imageFile);
		}
		
		m_layerType = layerType;
		m_library = library;
		m_faceSize = faceSize;
		m_placeHolderFor = null;

		//load();
	}
	
	/**
	 * Creates a basic place holder type
	 * 
	 * @param library This type's parent library
	 * @param faceSize Size of one of this element's face in map units
	 * @param placeHolderFor Name of the image this type is a place holder for
	 * @param layerType Layer type one such element is associated
	 */
	public BasicMapElementType(BasicMapElementTypeLibrary library, int faceSize, String placeHolderFor, Layer layerType)
	{
		if (layerType == null)
			throw new IllegalArgumentException("Invalid layerType");

		m_imageFile = new File("");
		m_imageFileName = "";
		m_layerType = layerType;
		m_library = library;
		m_faceSize = faceSize;
		m_placeHolderFor = placeHolderFor;

		loadPlaceholder();
	}

	/**
	 * @return A display label for this MapElement
	 */
	@Override
	public String getDisplayLabel()
	{
		String label = getImageFilename();
		final int start = label.lastIndexOf(File.separator) + 1;
		int end = label.lastIndexOf('.');
		if (end < 0)
			end = label.length();

		label = label.substring(start, end);

		return new String(label);
	}

	/**
	 * @return The face size in map units (squares / hexes)
	 */
	@Override
	public int getFaceSize()
	{
		return m_faceSize;
	}

	/**
	 * Get the basic image linked with this Map Element
	 * 
	 * @return Image object
	 */
	@Override
	public Image getImage()
	{
		if (!m_loaded)
			load();
		
		return m_image;
	}
	
	/*
	 * @see com.galactanet.gametable.data.MapElementType#getFullyQualifiedName()
	 */
	@Override
	public String getFullyQualifiedName()
	{
		if (m_fullyQualifiedName == null)
		{
			if (m_placeHolderFor != null)
				m_fullyQualifiedName = m_library.getFullyQualifiedName() + MapElementTypeLibrary.TYPE_SEPARATOR + UtilityFunctions.escapeString(m_placeHolderFor);
			else
				m_fullyQualifiedName = m_library.getFullyQualifiedName() + MapElementTypeLibrary.TYPE_SEPARATOR + UtilityFunctions.escapeString(m_imageFile.getName());				
		}
		
		return m_fullyQualifiedName;
	}

	/**
	 * @return Returns the image filename used by this pog.
	 */
	public String getImageFilename()
	{
		return m_imageFileName;
	}

	/**
	 * Get the height of the image
	 * 
	 * @return height in pixels
	 */
	public int getImageHeight()
	{
		if (m_image == null)
			return GameTableMap.getBaseTileSize();

		return m_image.getHeight(null);
	}

	/**
	 * Get the width of the image
	 * 
	 * @return width in pixels
	 */
	public int getImageWidth()
	{
		if (m_image == null)
			return GameTableMap.getBaseTileSize();

		return m_image.getWidth(null);
	}

	/**
	 * Returns the layer type associated with this MapElement
	 * 
	 * @return Layer type
	 */
	@Override
	public Layer getLayerType()
	{
		return m_layerType;
	}

	/**
	 * Get an icon representation of this map element (good for giving visual clues on lists)
	 * 
	 * @param size Size of the icon, in pixels. Square icon is assumed.
	 * @return Image
	 */
	@Override
	public Image getListIcon()
	{
		if (m_listIcon == null)
		{
			if (!isLoaded())
				load();
			
			int maxDim = Math.max(getImageWidth(), getImageHeight());
			float scale = GametableApp.getIntegerProperty(GametableApp.PROPERTY_ICON_SIZE) / (float) maxDim;
			m_listIcon = Images.getScaledInstance(m_image, scale);
		}

		return m_listIcon;
	}

	/**
	 * Get the height of the list icon
	 * 
	 * @return height in pixels
	 */
	public int getListIconHeight()
	{
		return getListIcon().getHeight(null);
	}

	/**
	 * Get the width of the list icon
	 * 
	 * @return width in pixels
	 */
	public int getListIconWidth()
	{
		return getListIcon().getWidth(null);
	}

	/**
	 * Verifies if this MapElement has been loaded
	 * 
	 * Mostly used in network communications to see if image transfer is required
	 * 
	 * @return true If this Map Element has been fully loaded. See {@link #load()}
	 */
	@Override
	public boolean isLoaded()
	{
		return m_loaded;
	}
	
	/*
	 * @see com.galactanet.gametable.data.MapElementTypeIF#loadDataFromNetwork(com.galactanet.gametable.net.NetworkConnectionIF)
	 */
	@Override
	public void loadDataFromNetwork(NetworkConnectionIF conn)
	{
		if (!m_loaded)
		{
			FileRequestListenerIF listener = new FileRequestListenerIF() {
				/*
				 * @see com.galactanet.gametable.ui.net.NetRequestImage.FileRequestListenerIF#onFileRequestReceived(java.lang.String, java.io.File)
				 */
				@Override
				public void onFileRequestReceived(String fqn, File file)
				{
//					BasicMapElementType type = new BasicMapElementType(m_library, file, m_faceSize, m_layerType);
//					type.load();
//					m_library.addElementType(type);
					
					m_imageFile = file;
					
					try
					{
						m_imageFileName = m_imageFile.getCanonicalPath();
					}
					catch (IOException e)
					{
						throw new IllegalArgumentException("Invalid file " + m_imageFile);
					}
					
					m_placeHolderFor = null;
					m_fullyQualifiedName = null;
					load();
					
					// Trigger a refresh through the library's listeners
					m_library.addElementType(BasicMapElementType.this);
				}
			};
			
			File destination = new File( m_library.getName() + File.separator + m_placeHolderFor);
			NetRequestFile.requestFile(conn, BasicMapElementType.class.getName(), getFullyQualifiedName(), destination, listener);
		}
	}

	/**
	 * Loads (or reloads) the MapElement
	 */
	@Override
	public void load()
	{
		// Backup previous image in case of failure
		final Image oldImage = m_image;

		if (m_placeHolderFor == null || m_image == null)
		{
			// Load image from file name
			m_image = ImageCache.getImage(m_imageFile);
		}

		m_listIcon = null;

		// If image load failed, we'll revert to backup or placeholder image
		if (m_image == null)
		{
			m_loaded = false;

			if (oldImage != null)
			{
				m_image = oldImage;
			}
			else
			{
				m_image = getPlaceHolderImage(m_faceSize);
			}
		}
		else
		{
			if (m_placeHolderFor == null)
				m_loaded = true;
			
			// File loaded okay, calculate facing

			// Largest dimension is face size in pixels
			int pixelSize = Math.max(m_image.getWidth(null), m_image.getHeight(null));

			// Convert to map size (squares)
			m_faceSize = (int) Math.ceil(pixelSize / (float) GameTableMap.getBaseTileSize());
		}

		if (m_faceSize < 1)
			m_faceSize = 1;
	}
	
	/**
	 * Loads (or reloads) the MapElement with the placeholder image
	 */
	private void loadPlaceholder()
	{
		if (m_faceSize < 1)
			m_faceSize = 1;

		m_listIcon = null;
		m_loaded = false;
		
		// Load place holder image
		m_image = getPlaceHolderImage(m_faceSize);
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "[PogType@" + hashCode() + " name: " + m_imageFile + " face-size: " + m_faceSize + ", loaded: " + isLoaded() + "]";
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if ((obj instanceof File) && m_imageFile != null && obj != null)
			return m_imageFile.equals(obj);
		
		if (obj instanceof BasicMapElementType && obj != null)
			return getFullyQualifiedName().equals(((BasicMapElementType)obj).getFullyQualifiedName());

		return super.equals(obj);
	}
}
