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
import java.util.HashMap;
import java.util.Map;

import com.galactanet.gametable.GametableApp;
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
public class BasicMapElementType implements MapElementType
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
			final int size = faceSize * GameTableMap.getBaseSquareSize();
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
	private final File		m_imageFile;

	/**
	 * Scale ration used for the last image scaling through drawScaledImage. Used to verify cache validity.
	 * 
	 * @revise I'm thinking of revising the caching model
	 */
	private float					m_lastScale;

	/**
	 * Cache of the scaled element image, as last rendered by drawScaledImage
	 * 
	 * @revise I'm thinking of revising the caching model
	 */
	private Image					m_lastScaledImage;

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
		m_layerType = layerType;
		m_library = library;
		m_faceSize = faceSize;

		//load();
	}
	
	/**
	 * Creates a basic place holder type
	 * 
	 * @param library This type's parent library
	 * @param faceSize Size of one of this element's face in map units
	 * @param layerType Layer type one such element is associated
	 */
	public BasicMapElementType(BasicMapElementTypeLibrary library, int faceSize, Layer layerType)
	{
		if (layerType == null)
			throw new IllegalArgumentException("Invalid layerType");

		m_imageFile = new File("");
		m_layerType = layerType;
		m_library = library;
		m_faceSize = faceSize;

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
			m_fullyQualifiedName = m_library.getFullyQualifiedName() + MapElementTypeLibrary.TYPE_SEPARATOR + m_imageFile.getName();
		
		return m_fullyQualifiedName;
	}

	/**
	 * @return Returns the image filename used by this pog.
	 */
	public String getImageFilename()
	{
		return m_imageFile.getAbsolutePath();
	}

	/**
	 * Get the height of the image
	 * 
	 * @return height in pixels
	 */
	public int getImageHeight()
	{
		if (m_image == null)
			return GameTableMap.getBaseSquareSize();

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
			return GameTableMap.getBaseSquareSize();

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
	 * TODO doesn't feel useful
	 * @return A normalized display label (extra / illegal characters removed)
	 */
	public String getNormalizedDisplayLabel()
	{
		return UtilityFunctions.normalizeName(getDisplayLabel());
	}

	/**
	 * Verifies if this MapElement has been loaded
	 * 
	 * Mostly used in network communications to see if image transfer is required
	 * 
	 * @revise Generalize process for multiple MapElement classes
	 * 
	 * @return true If this Map Element has been fully loaded. See {@link #load()}
	 */
	@Override
	public boolean isLoaded()
	{
		return m_loaded;
	}

	/**
	 * Loads (or reloads) the MapElement
	 */
	@Override
	public void load()
	{
		// Backup previous image in case of failure
		final Image oldImage = m_image;

		// Load image from file name
		m_image = ImageCache.getImage(m_imageFile);

		m_listIcon = null;

		// Clear the last scaled image buffer @revise buffering strategy
		m_lastScaledImage = null;
		m_lastScale = 0f;

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
			// File loaded okay, calculate facing
			m_loaded = true;

			// Largest dimension is face size in pixels
			int pixelSize = Math.max(m_image.getWidth(null), m_image.getHeight(null));

			// Convert to map size (squares)
			m_faceSize = (int) Math.ceil(pixelSize / (float) GameTableMap.getBaseSquareSize());
		}

		if (m_faceSize < 1)
			m_faceSize = 1;
	}
	
	/**
	 * Loads (or reloads) the MapElement
	 */
	private void loadPlaceholder()
	{
		if (m_faceSize < 1)
			m_faceSize = 1;

		// Load place holder image		
		m_image = getPlaceHolderImage(m_faceSize);

		m_listIcon = null;

		// Clear the last scaled image buffer @revise buffering strategy
		m_lastScaledImage = null;
		m_lastScale = 0f;

		m_loaded = true;
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
	
	/**
	 * Provides a scaled version of this MapElement
	 * 
	 * @param Uniform scale ratio to apply to regular-sized element, 1 being 100%
	 * @return Scaled image
	 */
	protected Image getScaledImage(final float scale)
	{
		// If no scale requested, return regular image
		if (scale == 1.0f)
			return getImage();

		// @revise buffering strategy. Would it be worth to retain more than one scaled version of the image? Do we want a
		// central buffering system?

		// Compare requested scale with buffered scaling ratio - if the buffered image is not valid, let us generate a new
		// one

		// NB: float ratios are converted to integer ratios on a 100 scale to prevent having to regenerate images due to
		// float imprecision
		if ((m_lastScaledImage == null) || (Math.round(m_lastScale * 100) != Math.round(scale * 100)))
		{
			m_lastScale = scale;
			m_lastScaledImage = Images.getScaledInstance(m_image, m_lastScale);
		}

		return m_lastScaledImage;
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (m_imageFile != null && obj != null && (obj instanceof File))
			return m_imageFile.equals(obj);

		return super.equals(obj);
	}
}
