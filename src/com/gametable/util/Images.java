/*
 * Images.java
 * 
 * @created 2010-06-18
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

package com.gametable.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;

import com.gametable.GametableApp;

/**
 * Collection of static methods to use for image manipulations
 * 
 * @author Eric Maziade
 * 
 *         #GT-AUDIT Images
 */
public class Images
{

	private static GraphicsConfiguration	g_defaultGraphicsConfiguration	= null;

	private static RenderingHints					g_renderingHints								= null;

	/**
	 * Creates a new, empty buffered image matching the specified image's size 
	 * 
	 * @param image Image to read from
	 * @return new image buffer
	 */
	public static BufferedImage createBufferedImage(final Image image)
	{
		return createBufferedImage(image.getWidth(null), image.getHeight(null));
		
		/*
		// Determine if the image has transparent pixels; for this method's
		// implementation, see e661 Determining If an Image Has Transparent Pixels
		// final boolean hasAlpha = hasAlpha(outImage);
		final int transparency = Images.getTransparency(image);

		// Create a buffered image with a format that's compatible with the screen
		BufferedImage bimage = null;
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try
		{
			// Create the buffered image
			final GraphicsDevice gs = ge.getDefaultScreenDevice();
			final GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
		}
		catch (final HeadlessException e)
		{
			// The system does not have a screen
		}

		if (bimage == null)
		{
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			
			if (transparency != Transparency.OPAQUE)
				type = BufferedImage.TYPE_INT_ARGB;
				
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}

		return bimage;
		*/
	}

	public static BufferedImage createBufferedImage(final int width, final int height)
	{
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(width, height,
				Transparency.TRANSLUCENT);
	}

	/**
	 * Create a flipped image from the given source
	 * 
	 * @param image image to flip
	 * @param flipH true to flip horizontally
	 * @param flipV true to flip vertically
	 * @return New flipped image (or source image if no transformation was necessary)
	 */
	public static Image flipImage(Image image, boolean flipH, boolean flipV)
	{
		if (!flipH && !flipV)
		{
			return image;
		}

		int width = image.getWidth(null);
		int height = image.getHeight(null);

		int x1 = flipH ? width : 0;
		int x2 = flipH ? 0 : width;

		int y1 = flipV ? height : 0;
		int y2 = flipV ? 0 : height;

		BufferedImage newImage = createBufferedImage(image);
		Graphics g = newImage.getGraphics();

		g.drawImage(image, x1, y1, x2, y2, 0, 0, width, height, null);

		g.dispose();

		return newImage;
	}

	/**
	 * Get the default graphics configuration - useful for creating image buffers
	 * 
	 * @return GraphicsConfiguration object
	 */
	public static GraphicsConfiguration getDefaultGraphicsConfiguration()
	{
		if (Images.g_defaultGraphicsConfiguration == null)
		{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();

			Images.g_defaultGraphicsConfiguration = gd.getDefaultConfiguration();
		}

		return Images.g_defaultGraphicsConfiguration;
	}

	/**
	 * @return The standard set of rendering hits for the app.
	 */
	public static RenderingHints getRenderingHints()
	{
		if (g_renderingHints == null)
		{
			RenderingHints retVal = new RenderingHints(null);

			//retVal.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			retVal.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			
			//retVal.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			retVal.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);	// TODO make antialiasing an option
			
			//retVal.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
			retVal.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			
			retVal.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
			
			retVal.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			
	//		retVal.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			retVal.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			
			retVal.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			retVal.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			g_renderingHints = retVal;
		}

		return g_renderingHints;
	}

	/**
	 * Calculates the new dimension of a rotated image
	 * 
	 * @param width width of a given image
	 * @param height height of a given image
	 * @param angleRads Angle, in rad to rotate to
	 * @param result Object in which to store result. If null, a new instance will be created
	 * @return Dimension object (new instance if result was null)
	 */
	public static Dimension getRotatedSquareSize(int width, int height, double angleRads, Dimension result)
	{
		if (result == null)
			result = new Dimension();

		// Convert to smallest fitting square containing image
		if (height != width)
		{
			int imageSquareSize = Math.max(width, height);
			height = imageSquareSize;
			width = imageSquareSize;
		}

		double sin = Math.abs(Math.sin(angleRads));
		double cos = Math.abs(Math.cos(angleRads));

		result.width = (int) Math.floor(width * cos + height * sin);
		result.height = (int) Math.floor(height * cos + width * sin);

		return result;
	}

	public static Image getScaledInstance(final Image image, final float scale)
	{
		if (image == null)
		{
			return null;
		}

		waitForImage(image);

		final int width = Math.round(image.getWidth(null) * scale);
		final int height = Math.round(image.getHeight(null) * scale);
		
		return Images.getScaledInstance(image, width, height);
	}

	public static Image getScaledInstance(final Image image, final int width, final int height)
	{
		if (image == null)
			return null;
		
		BufferedImage img = createBufferedImage(width, height);
		Graphics2D g = img.createGraphics();
		g.setRenderingHints(getRenderingHints());
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		waitForImage(img);
		
		return img;
	}

	/**
	 * ***************************************************************** Returns the Tranparency of the specified Image
	 * 
	 * @param image
	 * @return int value of the transparency
	 */
	// public static boolean hasAlpha(final Image image)
	public static int getTransparency(final Image image)
	{
		// If buffered image, the color model is readily available
		if (image instanceof BufferedImage)
		{
			final BufferedImage bimage = (BufferedImage) image;
			// return bimage.getColorModel().hasAlpha();
			return bimage.getColorModel().getTransparency();
		}

		// Use a pixel grabber to retrieve the image's color model;
		// grabbing a single pixel is usually sufficient
		final PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try
		{
			pg.grabPixels();
		}
		catch (final InterruptedException e)
		{
		}

		// Get the image's color model
		final ColorModel cm = pg.getColorModel();
		// return cm.hasAlpha();
		return cm.getTransparency();
	}

	/**
	 * Do a rotation on a given image
	 * 
	 * @param srcImage image to rotate - will not be modified
	 * @param angleDegrees number of degrees to rotate
	 * 
	 * @return New rotated, square image
	 */
	public static Image rotateImage(final Image srcImage, final double angleDegrees)
	{
		if (angleDegrees == 0)
		{
			return srcImage;
		}
		
		final int srcHeight = srcImage.getHeight(null);
		final int srcWidth = srcImage.getWidth(null);

		// Perform the actual rotation
		BufferedImage result = createBufferedImage(srcImage);
		
		BufferedImage srcBuffer = toBufferedImage(srcImage);
		AffineTransform xform = new AffineTransform();
		
		Graphics2D g = result.createGraphics();
		g.setRenderingHints(Images.getRenderingHints());
		
		xform.setToRotation(Math.toRadians(angleDegrees), srcWidth / 2, srcHeight / 2);
		
		g.drawImage(srcBuffer, xform, null);
		
		g.dispose();
		
		return result;
	}


	/**
	 * Creates a buffered image containing the specified image
	 * 
	 * If the specified image is already a BufferedImage, the same image is returned
	 * 
	 * @param image
	 * @return
	 */
	public static BufferedImage toBufferedImage(final Image image)
	{
		if (image instanceof BufferedImage)
		{
			return (BufferedImage) image;
		}

		BufferedImage b = createBufferedImage(image);

		// Copy image to buffered image
		final Graphics g = b.createGraphics();

		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return b;
	}

	public static void waitForImage(final Image image)
	{
		final MediaTracker tracker = new MediaTracker(GametableApp.getUserInterface());
		tracker.addImage(image, 0);
		try
		{
			tracker.waitForAll();
		}
		catch (final Exception e)
		{
			Log.log(Log.SYS, e);
		}
	}

	/**
	 * Checks the given binary data to see if it is a valid PNG file. 
	 * It does this by checking the PNG signature.
	 * 
	 * @param data binary data to check
	 * @return true if the binary data is a valid PNG file, false otherwise.
	 */
	public static boolean isPngData(final byte[] data)
	{
	    for (int i = 0; i < PNG_SIGNATURE.length; i++)
	    {
	        if (data[i] != PNG_SIGNATURE[i])
	        {
	            return false;
	        }
	    }
	
	    return true;
	}


  /**
   * The PNG signature to verify PNG data with.
   */
  private static final byte[]        PNG_SIGNATURE            = {
      (byte)(137 & 0xFF), 80, 78, 71, 13, 10, 26, 10
                                                              };
}
