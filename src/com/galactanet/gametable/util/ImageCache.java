/*
 * ImageCache.java
 * 
 * @created 2010-06-22
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

package com.galactanet.gametable.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

/**
 * Static class handling all image caching
 * 
 * @author Eric Maziade
 */
public class ImageCache
{
	/**
	 * Holds information relative to a cached item
	 * 
	 * @author Eric Maziade
	 */
	private static class CacheInfo
	{
		/**
		 * Cached image
		 */
		Image	image;

		/**
		 * The time at which this item was last accessed
		 */
		long	timestamp;
	}

	/**
	 * Caching information
	 */
	private static Map<URI, CacheInfo>	g_imageCache	= new HashMap<URI, CacheInfo>();

	/**
	 * Timer daemon that periodically cleans up cache
	 */
	private static Timer								g_timer				= null;

	/**
	 * Time before a cached item expires. Also the delay between runs of the cleanup process
	 */
	private static final long	TIMEOUT_DELAY_MS	= 1000 * 60 * 5;	// 5 minute life time

	/**
	 * Stores an image to cache
	 * 
	 * @param uri Object that uniquely identifies the cached image
	 * @param image Image to cache
	 */
	public static void cacheImage(URI uri, Image image)
	{
		CacheInfo info;

		info = new CacheInfo();
		info.timestamp = System.currentTimeMillis();
		info.image = image;

		synchronized (g_imageCache)
		{
			ImageCache.g_imageCache.put(uri, info);
		}
	}

	/**
	 * Tries to locate an image directly from cache. No load attempts are made if the image is not found.
	 * 
	 * @param uri URI uniquely identifying the image to load
	 * @return Image or null
	 */
	public static Image getCachedImage(URI uri)
	{
		CacheInfo info = g_imageCache.get(uri);

		if (info == null)
			return null;

		info.timestamp = System.currentTimeMillis();
		return info.image;
	}

	/**
	 * Gets an image from cache. Automatically loads the image if not found within cache
	 * 
	 * @param imageFile Image to load from disk
	 * @return Image retrieved, or null.
	 */
	
	public static Image getImage(File imageFile)
	{
		URI uri = imageFile.toURI();

		Image image = getCachedImage(uri);

		if (image != null)
			return image;

		image = loadImage(imageFile);
		if (image == null)
		{
			return null;
		}

		cacheImage(uri, image);

		return image;
	}

	/**
	 * Start the daemon that will maintain the image cache
	 */
	public static void startCacheDaemon()
	{
		// Only one time
		if (g_timer != null)
			return;

		g_timer = new Timer(true);
		TimerTask task = new TimerTask() {

			@Override
			public void run()
			{
				cleanup();
			}
		};

		g_timer.scheduleAtFixedRate(task, TIMEOUT_DELAY_MS, TIMEOUT_DELAY_MS);
	}

	/**
	 * Cleanup cache
	 */
	private static void cleanup()
	{
		synchronized (g_imageCache)
		{
			Iterator<CacheInfo> infos = g_imageCache.values().iterator();
			long deathTime = System.currentTimeMillis() - TIMEOUT_DELAY_MS;

			while (infos.hasNext())
			{
				CacheInfo info = infos.next();

				// Remove from list if expired
				if (info.timestamp <= deathTime)
					infos.remove();
			}
		}
	}

	/**
	 * Load image from file
	 * 
	 * @param imageFile Image file to load
	 * @return Buffered Image or null
	 */
	private static BufferedImage loadImage(File imageFile)
	{
		try
		{
			ImageInputStream stream = ImageIO.createImageInputStream(imageFile);
			
			if (PNGUtils.isPNG(stream))
			{
				return PNGUtils.loadPNGImage(stream);
			}
						
			return ImageIO.read(stream);
			
			/*
			BufferedImage i2 = Images.createBufferedImage(img);
			Graphics g = i2.getGraphics();
			g.drawImage(img, 0, 0, null);
			return i2;
			*/
		}
		catch (IOException e)
		{
			// @revise - Is this how we want to handle exceptions??
			Log.log(Log.SYS, e);
			return null;
		}
	}

	/**
	 * Private constructor - so no one instantiates the static class
	 */
	private ImageCache()
	{
	}
}
