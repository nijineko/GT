/*
 * TestCompositeContext.java
 * 
 * @created 2010-06-21
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

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Composite objects that applies a tint to what is painted on Graphics Context
 * 
 * @author Eric Maziade
 */
public class ColorComposite implements Composite
{
	/**
	 * Byte Pixel representation
	 *
	 * @author Eric Maziade
	 */
	private class BytePixel implements Pixel
	{
		/**
		 * Internal representation of a pixel
		 */
		int					m_pixel;

		/**
		 * Color model
		 */
		ColorModel	m_model;

		/**
		 * Raster buffer
		 */
		Raster			m_raster;

		/**
		 * Constructor
		 * @param r raster object
		 * @param cmodel color model
		 */
		public BytePixel(Raster r, ColorModel cmodel)
		{
			m_raster = r;
			m_model = cmodel;
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getA()
		 */
		@Override
		public int getA()
		{
			return ((m_pixel >> 24) & 0xFF);
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getB()
		 */
		@Override
		public int getB()
		{
			return ((m_pixel & 0xFF));
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getG()
		 */
		@Override
		public int getG()
		{
			return ((m_pixel >> 8) & 0xFF);
		}
		
		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getR()
		 */
		@Override
		public int getR()
		{
			return ((m_pixel >> 16) & 0xFF);
		}
		/*
		 * @see com.gametable.ui.tools.TestCompositeContext.Pixel#getPixel(int, int)
		 */
		@Override
		public void readPixel(int x, int y)
		{
			int s = m_raster.getSample(x, y, 0);
			m_pixel = m_model.getRGB(s);
		}
	}
	
	/**
	 * Byte Pixel representation
	 *
	 * @author Eric Maziade
	 */
	private class BytePixel4 implements Pixel
	{
		/**
		 * Red
		 */
		int					m_r;
		/**
		 * Green
		 */
		int m_g;
		/**
		 * Blue
		 */
		int m_b;
		/**
		 * Alpha
		 */
		int m_a;

		/**
		 * Raster buffer
		 */
		Raster			m_raster;
		
		/**
		 * Number of raster bands
		 */
		int m_bands;

		/**
		 * Constructor
		 * @param r raster object
		 * @param cmodel color model
		 */
		public BytePixel4(Raster r, ColorModel cmodel)
		{
			m_raster = r;
			m_bands = m_raster.getNumBands();
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getA()
		 */
		@Override
		public int getA()
		{
			return m_a;
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getB()
		 */
		@Override
		public int getB()
		{
			return m_b;
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getG()
		 */
		@Override
		public int getG()
		{
			return m_g;
		}
		
		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getR()
		 */
		@Override
		public int getR()
		{
			return m_r;
		}
		/*
		 * @see com.gametable.ui.tools.TestCompositeContext.Pixel#getPixel(int, int)
		 */
		@Override
		public void readPixel(int x, int y)
		{
			if (m_bands >= 3)
			{
				m_r = m_raster.getSample(x, y, 0);
				m_g = m_raster.getSample(x, y, 1);
				m_b = m_raster.getSample(x, y, 2);
			}
			
			if (m_bands >= 4)
				m_a = m_raster.getSample(x, y, 3);
			else
				m_a = 255;
		}
	}

	/**
	 * Composite Context - the object that does the actual work
	 *
	 * @author Eric Maziade
	 */
	private class ColorCompositeContext implements CompositeContext	{

		/**
		 * Destination raster color model
		 */
		private ColorModel			m_dstColorModel;
		
		/**
		 * Source raster color model
		 */
		private ColorModel			m_srcColorModel;

		/**
		 * Constructor
		 * @param srcColorModel source model
		 * @param dstColorModel destination model
		 */
		private ColorCompositeContext(ColorModel srcColorModel, ColorModel dstColorModel)
		{
			m_srcColorModel = srcColorModel;
			m_dstColorModel = dstColorModel;
		}

		/*
		 * @see java.awt.CompositeContext#compose(java.awt.image.Raster, java.awt.image.Raster,
		 * java.awt.image.WritableRaster)
		 */
		@Override
		public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
		{
			Pixel srcPx;

			int type = src.getSampleModel().getDataType();
			switch (type)
			{
			case DataBuffer.TYPE_USHORT:
			case DataBuffer.TYPE_BYTE:
				if (src.getNumBands() == 1)
					srcPx = new BytePixel(src, m_srcColorModel);
				else
					srcPx = new BytePixel4(src, m_srcColorModel);
				break;

			case DataBuffer.TYPE_INT:
				srcPx = new IntPixel(src, m_srcColorModel);
				break;

			default:
				throw new IllegalArgumentException("src not supported data type; " + type);
			}

			if (dstIn.getSampleModel().getDataType() != DataBuffer.TYPE_INT)
				throw new IllegalArgumentException("dstIn not int; " + dstIn.getSampleModel().getDataType());

			IntPixel dstInPx = new IntPixel(dstIn, m_dstColorModel);

			if (dstOut.getSampleModel().getDataType() != DataBuffer.TYPE_INT)
				throw new IllegalArgumentException("dstOut not int; " + dstOut.getSampleModel().getDataType());

			IntPixel dstOutPx = new IntPixel(dstOut, m_dstColorModel);

			for (int y = 0; y < dstIn.getHeight(); y++)
			{
				for (int x = 0; x < dstIn.getWidth(); x++)
				{
					srcPx.readPixel(x, y);
					dstInPx.readPixel(x, y);

					dstInPx.merge(srcPx);

					// Alpha threshold - if pixel being drawn is not over 80 alpha (30%), we do not apply tint
					if (srcPx.getA() > 80)	
						dstInPx.tint();

					dstOutPx.writePixel(x, y, dstInPx);
				}
			}

		}

		/*
		 * @see java.awt.CompositeContext#dispose()
		 */
		@Override
		public void dispose()
		{
		}
	}

	/**
	 * Interger-type pixel representation
	 *
	 * @author Eric Maziade
	 */
	private class IntPixel implements Pixel
	{
		/**
		 * Internal representation of a pixel
		 */
		private int[]						m_pixel;

		/**
		 * If this raster is writable, we keep a pointer here
		 */
		private WritableRaster	m_out = null;

		/**
		 * Raster buffer
		 */
		private Raster					m_raster;

		/**
		 * Constructor
		 * @param r raster buffer
		 * @param model color model
		 */
		public IntPixel(Raster r, ColorModel model)
		{
			if (r instanceof WritableRaster)
				m_out = (WritableRaster) r;

			m_raster = r;
			m_pixel = new int[r.getNumBands()];
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getA()
		 */
		@Override
		public int getA()
		{
			if (m_pixel.length >= 4)
				return m_pixel[3];

			return 255;
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getB()
		 */
		@Override
		public int getB()
		{
			if (m_pixel.length >= 3)
				return m_pixel[2];

			return 0;
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getG()
		 */
		@Override
		public int getG()
		{
			if (m_pixel.length >= 2)
				return m_pixel[1];

			return 0;
		}

		/*
		 * @see com.gametable.ui.tools.ColorComposite.Pixel#getR()
		 */
		@Override
		public int getR()
		{
			if (m_pixel.length >= 1)
				return m_pixel[0];

			return 0;
		}

		/**
		 * Merge a given pixel with this pixel
		 * @param px pixel to merge
		 */
		public void merge(Pixel px)
		{
			if (m_pixel.length >= 3)
			{
				if (px.getA() == 0)
					return;

				if (px.getA() == 255)
				{
					m_pixel[0] = px.getR();
					m_pixel[1] = px.getG();
					m_pixel[2] = px.getB();
					return;
				}

				int alpha = px.getA();
				m_pixel[0] = blend(m_pixel[0], px.getR(), alpha);
				m_pixel[1] = blend(m_pixel[1], px.getG(), alpha);
				m_pixel[2] = blend(m_pixel[2], px.getB(), alpha);
			}
		}

		/*
		 * @see com.gametable.ui.tools.TestCompositeContext.Pixel#getPixel(int, int)
		 */
		@Override
		public void readPixel(int x, int y)
		{
			m_raster.getPixel(x, y, m_pixel);
		}

		/**
		 * Apply tint to this pixel
		 */
		public void tint()
		{
			if (m_pixel.length >= 3)
			{
				m_pixel[0] = blend(m_pixel[0], m_red, m_alpha);
				m_pixel[1] = blend(m_pixel[1], m_green, m_alpha);
				m_pixel[2] = blend(m_pixel[2], m_blue, m_alpha);
			}
		}
		
		/**
		 * Write this pixel to the buffer.  Assumes the constructing raster was writable
		 * @param x coordinate
		 * @param y coordinate
		 * @param px pixel to write (only IntPixel accepted)
		 */
		public void writePixel(int x, int y, IntPixel px)
		{
			m_out.setPixel(x, y, px.m_pixel);
		}
		
		/**
		 * Blend two pixels
		 * @param a First pixel (base)
		 * @param b Second pixel (top)
		 * @param alpha Alpha to apply to top pixel for blending
		 * @return Blended color
		 */
		private int blend(final int a, final int b, final int alpha)
		{
			return ((255 - alpha) * a + alpha * b) / 255;
		}
	}

	/**
	 * Pixel represenation
	 *
	 * @author Eric Maziade
	 */
	private interface Pixel
	{
		/**
		 * @return Alpha value (0-255)
		 */
		public int getA();

		/**
		 * @return Blue value (0-255)
		 */
		public int getB();

		/**
		 * @return Green value (0-255)
		 */
		public int getG();

		/**
		 * @return Red value (0-255)
		 */
		public int getR();

		/**
		 * Read specified pixel from the buffer
		 * @param x
		 * @param y
		 */
		public void readPixel(int x, int y);
	}
	
	/**
	 * Constructor
	 * @param tint Tint to give
	 * @param alpha Alpha to apply to tint
	 */
	public ColorComposite(Color tint, float alpha)
	{
		m_red = tint.getRed();
		m_green = tint.getGreen();
		m_blue = tint.getBlue();
		m_alpha = (int)(255 * alpha);
	}

	/*
	 * @see java.awt.Composite#createContext(java.awt.image.ColorModel, java.awt.image.ColorModel,
	 * java.awt.RenderingHints)
	 */
	@Override
	public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints)	
	{
		if (m_cache == null || m_cache.m_dstColorModel != dstColorModel || m_cache.m_srcColorModel != srcColorModel)
			m_cache = new ColorCompositeContext(srcColorModel, dstColorModel);
		
		return m_cache; 
	}
	
	ColorCompositeContext m_cache = null;
	
	private int m_red;
	private int m_green;
	private int m_blue;
	private int m_alpha;
}