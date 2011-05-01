/*
 * MapElementInstanceRenderer.java
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

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import com.gametable.data.MapElement.Attribute;
import com.gametable.ui.GametableCanvas;
import com.gametable.ui.MapElementRendererIF;
import com.gametable.util.ImageCache;
import com.gametable.util.Images;

/**
 * Object handling the rendering of a MapElementInstance
 * 
 * @author Eric Maziade
 */
public class MapElementRenderer implements MapElementRendererIF
{
	/**
	 * Background color for basic text. TODO @revise move these to some global properties
	 */
	private static final Color		COLOR_BACKGROUND						= new Color(255, 255, 64, 192);

	/**
	 * Background color for attribute text
	 */
	private static final Color		COLOR_ATTRIBUTE_BACKGROUND	= new Color(64, 255, 64, 192);

	/**
	 * Background color for changed text
	 */
	protected static final Color	COLOR_CHANGED_BACKGROUND		= new Color(238, 156, 0, 192);

	/**
	 * Font to use for displaying attribute names
	 */
	private static final Font			FONT_ATTRIBUTE_NAME					= Font.decode("sansserif-bold-12");

	/**
	 * Font to use for displaying attribute values
	 */
	private static final Font			FONT_ATTRIBUTE_VALUE				= Font.decode("sansserif-12");

	/**
	 * Font to use for displaying default text
	 */
	private static final Font			FONT_TEXT										= Font.decode("sansserif-bold-12");

	/**
	 * Constructor - visible only to core data package
	 * 
	 * @param mapElement Map element handled by this instance
	 */
	protected MapElementRenderer(MapElement mapElement)
	{
		m_mapElement = mapElement;
	}

	/*
	 * @see com.gametable.ui.MapElementRendererIF#drawInformationOverlayToCanvas(java.awt.Graphics, boolean,
	 * com.gametable.ui.GametableCanvas)
	 */
	@Override
	public void drawInformationOverlayToCanvas(Graphics g, boolean mouseOver, GametableCanvas canvas)
	{
		drawStringToCanvas((Graphics2D) g, mouseOver, COLOR_BACKGROUND, mouseOver, canvas);
	}

	/*
	 * @see com.gametable.ui.MapElementRendererIF#drawToCanvas(java.awt.Graphics,
	 * com.gametable.ui.GametableCanvas)
	 */
	@Override
	public boolean drawToCanvas(Graphics g, GametableCanvas canvas)
	{
		// First check if element is visible
		final MapRectangle visbleCanvas = canvas == null ? null : canvas.getVisibleCanvasRect(canvas.getZoomLevel());

		if (canvas != null && !visbleCanvas.intersects(m_mapElement.getBounds()))
			return false;

		// convert our model coordinates to draw coordinates
		final Point drawCoords = canvas == null ? new Point(0, 0) : canvas.modelToView(m_mapElement.getPosition());
		final float scale = canvas == null ? 1 : (float) canvas.getTileSize() / (float) GameTableMap.getBaseTileSize();

		drawScaled(g, drawCoords.x, drawCoords.y, scale);

		return true;
	}

	/**
	 * Drawing a scaled version of the element on specified device
	 * 
	 * @param g Graphics device
	 * @param x x Coordinate for the paint
	 * @param y y Coordinate for the paint
	 * @param scale Scaling ratio
	 */
	private void drawScaled(final Graphics g, final int x, final int y, final float scale)
	{
		URI uri = createImageURI();
		
		Image im = uri == null ? null : ImageCache.getCachedImage(uri);
		
		if (im == null)
		{
			im = Images.rotateImage(
				Images.flipImage(
						m_mapElement.getMapElementType().getImage(), m_mapElement.getFlipH(), m_mapElement.getFlipV()
				),
				m_mapElement.getAngle()
			);
			
			ImageCache.cacheImage(uri, im);
		}
		
		final int drawWidth = Math.round(m_mapElement.getWidth() * scale);
		final int drawHeight = Math.round(m_mapElement.getHeight() * scale);

		// Center the image into a square, taking into consideration the height and width
		int mw = 0;
		int mh = 0;
		if (m_mapElement.getAngle() != 0)
		{
			Image image = m_mapElement.getMapElementType().getImage();

			mw = Math.round(drawWidth - (image.getHeight(null) * scale));
			mw = Math.round(drawHeight - (image.getWidth(null) * scale));
		}

		g.drawImage(im, x - mw / 2, y - mh / 2, drawWidth, drawHeight, null);
	}
	
	/**
	 * Creates an URI based on the map element's display properties.  This URI will be used for caching purposes.
	 * @return URI
	 */
	private URI createImageURI()
	{
		// Use the source image - if it was to change, and none of the other elements, I'd like to know.
		Image srcImage = m_mapElement.getMapElementType().getImage();
		int srcHash = srcImage.hashCode();
		
		try
		{
			URI uri = new URI("gti://" + srcHash + "/" + m_mapElement.getAngle() + "/" + m_mapElement.getFlipH() + "/" + m_mapElement.getFlipV());		
			return uri;
		}
		catch (URISyntaxException e)
		{
			return null;	// this will disable caching
		}
	}

	/**
	 * Draw name and other attributes on the canvas
	 * 
	 * @param g Graphics device
	 * @param bForceTextInBounds Make sure the text remains on screen
	 * @param backgroundColor background color
	 * @param drawAttributes true to draw all attributes (if false, only changed attributes are drawn)
	 * @param canvas Canvas we are painting on
	 */
	private void drawStringToCanvas(Graphics2D g, boolean bForceTextInBounds, Color backgroundColor, boolean drawAttributes, GametableCanvas canvas)
	{
		String name = m_mapElement.getName();
		if (name == null)
			name = "";

		g.setFont(FONT_TEXT);

		final FontMetrics metrics = g.getFontMetrics();
		final Rectangle stringBounds = metrics.getStringBounds(name, g).getBounds();

		// Add some padding to width and height
		final int totalWidth = stringBounds.width + 6;
		final int totalHeight = stringBounds.height + 1;

		final int squareSize = canvas.getTileSize();

		final Point pogDrawCoords = canvas.modelToView(m_mapElement.getPosition());

		final double ratio = (double) squareSize / (double) GameTableMap.getBaseTileSize();
		final int viewWidth = (int) (ratio * m_mapElement.getHeight());

		final Rectangle backgroundRect = new Rectangle(pogDrawCoords.x + (viewWidth - totalWidth) / 2, pogDrawCoords.y - totalHeight - 4, totalWidth,
				totalHeight);

		Point scrollPos = canvas.getScrollPosition();

		if (bForceTextInBounds)
		{
			// force it to be on the view
			if (backgroundRect.x < scrollPos.x)
				backgroundRect.x = scrollPos.x;

			if (backgroundRect.y < scrollPos.y)
				backgroundRect.y = scrollPos.y;

			if (backgroundRect.x + totalWidth > scrollPos.x + canvas.getWidth())
				backgroundRect.x = scrollPos.x + canvas.getWidth() - totalWidth;

			if (backgroundRect.y + totalHeight > scrollPos.y + canvas.getHeight())
				backgroundRect.y = scrollPos.y + canvas.getHeight() - totalHeight;
		}

		if (name.length() > 0)
		{
			g.setColor(backgroundColor);
			g.fill(backgroundRect);

			final int stringX = backgroundRect.x + (backgroundRect.width - stringBounds.width) / 2;
			final int stringY = backgroundRect.y + (backgroundRect.height - stringBounds.height) / 2 + metrics.getAscent();

			g.setColor(Color.BLACK);
			g.drawString(name, stringX, stringY);

			g.drawRect(backgroundRect.x, backgroundRect.y, backgroundRect.width - 1, backgroundRect.height - 1);
		}

		drawAttributes(g, backgroundRect.x + (backgroundRect.width / 2), backgroundRect.y + backgroundRect.height, !drawAttributes);
	}

	/**
	 * Draw Map Element attributes
	 * 
	 * @param g Graphics device to paint on
	 * @param x x Coordinate for painting
	 * @param y y Coordinate for painting
	 * @param onlyChanged if true, only draw changed attributes
	 */
	private void drawAttributes(final Graphics2D g, final int x, final int y, final boolean onlyChanged)
	{
		Collection<Attribute> attributes = m_mapElement.getAttributes();

		// Check if there are any attributes to display
		if (onlyChanged)
		{
			boolean found = false;
			for (Attribute attribute : attributes)
			{
				if (attribute.changed)
				{
					found = true;
					break;
				}
			}

			if (!found)
				return;
		}
		else
		{
			if (attributes.size() == 0)
				return;
		}

		final FontMetrics nameMetrics = g.getFontMetrics(FONT_ATTRIBUTE_NAME);
		final FontMetrics valueMetrics = g.getFontMetrics(FONT_ATTRIBUTE_VALUE);
		int height = 0;
		int width = 0;

		// Calculate height and width of required text area
		for (Attribute attribute : attributes)
		{
			if (onlyChanged && !attribute.changed)
				continue;

			final Rectangle nameBounds = nameMetrics.getStringBounds(attribute.name + ": ", g).getBounds();
			final Rectangle valueBounds = valueMetrics.getStringBounds(attribute.value, g).getBounds();

			final int attrWidth = nameBounds.width + valueBounds.width;

			if (attrWidth > width)
				width = attrWidth;

			final int attrHeight = Math.max(nameBounds.height, valueBounds.height);

			height += attrHeight;
		}

		final int PADDING = 3;
		final int SPACE = PADDING * 2;
		height += SPACE;
		width += SPACE;

		int drawX = x - width / 2;
		int drawY = y;

		g.setColor(COLOR_ATTRIBUTE_BACKGROUND);
		g.fillRect(drawX, drawY, width, height);
		g.setColor(Color.BLACK);
		g.drawRect(drawX, drawY, width - 1, height - 1);

		drawX += PADDING;
		drawY += PADDING;

		// Draw attribute text
		for (Attribute attribute : attributes)
		{
			if (onlyChanged && !attribute.changed)
				continue;

			final String nameString = attribute.name + ": ";
			final String valueString = attribute.value;
			final Rectangle nameBounds = nameMetrics.getStringBounds(nameString, g).getBounds();
			final Rectangle valueBounds = valueMetrics.getStringBounds(valueString, g).getBounds();
			final int baseline = Math.max(-nameBounds.y, -valueBounds.y);
			g.setFont(FONT_ATTRIBUTE_NAME);
			g.drawString(nameString, drawX, drawY + baseline);

			g.setFont(FONT_ATTRIBUTE_VALUE);
			g.drawString(attribute.value, drawX + nameBounds.width, drawY + baseline);

			drawY += Math.max(nameBounds.height, valueBounds.height);
		}
	}

	/**
	 * Map element to render
	 */
	protected final MapElement	m_mapElement;
}
