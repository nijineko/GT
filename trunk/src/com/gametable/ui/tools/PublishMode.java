/*
 * PublishMode.java
 * 
 * Copyright (C) 1999-2011 Open Source Game Table Project
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

package com.gametable.ui.tools;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import com.gametable.GametableApp;
import com.gametable.data.*;
import com.gametable.ui.*;
import com.gametable.util.UtilityFunctions;

/**
 * Mode for send map elements from the private map to the public map (and vice-versa)
 * 
 * @author iffy
 * 
 *         #GT-AUDIT PublishMode
 */
public class PublishMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final PublishMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new PublishMode();

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	private PublishMode()
	{
		super("Publish");
		m_core = GametableApp.getCore();
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());
		
		UIModeAction action = new UIModeAction(
				this, 
				"Publish",
				"Move elements from the public map to the private map (or vice-versa)",
				KeyEvent.VK_9,
				"publish.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorPublish = m_frame.createMapCursor(CURSOR_PUBLISH, CURSOR_PUBLISH_HOTSPOT);
	}

	/*
	 * @see com.gametable.ui.UIMode#isActive()
	 */
	@Override
	public boolean isActive()
	{
		// If we have an anchor point, then we're currently drawing a box
		return (m_mouseAnchor != null);
	}

	/*
	 * @see com.gametable.ui.UIMode#paintTool(java.awt.Graphics2D, com.gametable.ui.GametableCanvas)
	 */
	@Override
	protected void paintTool(Graphics2D g, GametableCanvas canvas)
	{
		if (m_mouseAnchor != null)
		{
			final Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] { 2f }, 0f));
			final Rectangle rect = UtilityFunctions.createRectangle(canvas.modelToView(m_mouseAnchor), canvas.modelToView(m_mouseFloat));
			g2.draw(rect);
			g2.dispose();
		}
	}

	/**
	 * Remove higlights from the map
	 */
	private void clearTints()
	{
		m_frame.highlightAllMapElementInstances(false);
	}

	/**
	 * Tint selected elements
	 * 
	 * @param modifierMask
	 */
	private void setTints(final int modifierMask)
	{
		final MapRectangle selRect = new MapRectangle(m_mouseAnchor, m_mouseFloat);

		for (MapElement mapElement : m_sourceMap.getMapElements())
		{
			final int size = (int) (mapElement.getFaceSize() * GameTableMap.getBaseTileSize());

			MapCoordinates bottomRight = mapElement.getPosition().delta(size, size);

			final MapRectangle elementRect = new MapRectangle(mapElement.getPosition(), bottomRight);

			if (selRect.intersects(elementRect) && (!m_core.isMapElementLocked(mapElement) || (modifierMask & InputEvent.SHIFT_DOWN_MASK) != 0))
			{
				m_frame.highlightMapElementInstance(mapElement, true);
			}
			else
			{
				m_frame.highlightMapElementInstance(mapElement, false);
			}
		}
	}

	/**
	 * Box cursor name
	 */
	public static final String CURSOR_PUBLISH = "publish_cursor.png";

	/**
	 * Box cursor hot spot
	 */
	private static final Point CURSOR_PUBLISH_HOTSPOT = new Point(7, 7);

	/**
	 * Single instance of this mode
	 */
	private static PublishMode		g_mode	= null;

	/**
	 * Instance to core
	 */
	private final GameTableCore		m_core;

	/**
	 * Publish cursor
	 */
	private final Cursor m_cursorPublish;

	/**
	 * Map to move elements to
	 */
	private GameTableMap					m_destinationMap;

	/**
	 * Instance to game table frame
	 */
	private final GametableFrame	m_frame;

	/**
	 * Position of the anchor point (depending in keys being held during operation)
	 */
	private MapCoordinates				m_mouseAnchor;
	
	/**
	 * Position of the mouse while moving
	 */
	private MapCoordinates				m_mouseFloat;
	
	/**
	 * Map to move elements from
	 */
	private GameTableMap					m_sourceMap;

	
	/**
	 * Listener
	 */
	private class ModeListener implements UIModeListener
	{
		/*
		 * @see com.gametable.ui.UIModeListener#cancelMode()
		 */
		@Override
		public void cancelMode()
		{
			clearTints();
			m_mouseAnchor = null;
			m_mouseFloat = null;
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			// Select publish destinations

			if (m_core.isActiveMapPublic())
			{
				m_sourceMap = m_core.getMap(GameTableCore.MapType.PUBLIC);
				m_destinationMap = m_core.getMap(GameTableCore.MapType.PRIVATE);
			}
			else
			{
				m_sourceMap = m_core.getMap(GameTableCore.MapType.PRIVATE);
				m_destinationMap = m_core.getMap(GameTableCore.MapType.PUBLIC);
			}

			m_mouseAnchor = canvas.viewToModel(mouseX, mouseY);	// TODO #Useful?
			m_mouseFloat = m_mouseAnchor;
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonReleased(int, int)
		 */
		@Override
		public void mouseButtonReleased(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			if ((m_mouseAnchor != null) && !m_mouseAnchor.equals(m_mouseFloat))
			{
				// GametableFrame frame = GametableFrame.g_gameTableFrame;

				// first off, copy all the elements over to the public layer
				for (MapElement mapElement : m_sourceMap.getMapElements())
				{
					if (m_frame.isHighlighted(mapElement) && (!m_core.isMapElementLocked(mapElement) || (modifierMask & InputEvent.SHIFT_DOWN_MASK) != 0))
					{
						// this element gets copied
						final MapElement newElement = new MapElement(mapElement);

						m_destinationMap.addMapElement(newElement);

						if (m_core.isMapElementLocked(mapElement))
						{
							m_core.lockMapElement(GameTableCore.MapType.ACTIVE, newElement, true);
						}
					}
				}

				// now, copy over all the line segments. we run through all the
				// line segments on the private layer, and collect a list of the
				// ones that are at least partially in the rect
				final List<LineSegment> lineList = new ArrayList<LineSegment>();

				for (LineSegment ls : m_sourceMap.getLines())
				{
					final LineSegment result = ls.getPortionInsideRect(m_mouseAnchor, m_mouseFloat);

					if (result != null)
					{
						lineList.add(result);
					}
				}

				m_destinationMap.addLineSegments(lineList);

				boolean bDeleteFromPrivate = false;
				if ((modifierMask & InputEvent.CTRL_DOWN_MASK) == 0) // not holding control
				{
					bDeleteFromPrivate = true;
				}

				// if bDeleteFromPrivate is set, then this is a MOVE, not a COPY,
				// so we have to remove the pieces from the private layer.

				if (bDeleteFromPrivate)
				{
					// remove the element that we moved

					List<MapElement> items = new ArrayList<MapElement>();
					for (MapElement mapElement : m_sourceMap.getMapElements().toArray(new MapElement[0])) // converting list to array to
																																													// avoid concurrent
																																													// modifications
					{
						if (m_frame.isHighlighted(mapElement) && (!m_core.isMapElementLocked(mapElement) || (modifierMask & InputEvent.SHIFT_DOWN_MASK) != 0))
						{
							items.add(mapElement);
						}
					}

					GameTableMap activeMap = m_core.getMap(GameTableCore.MapType.ACTIVE);

					activeMap.removeMapElements(items);

					// remove the line segments
					final MapRectangle eraseRect = new MapRectangle(m_mouseAnchor, m_mouseFloat);
					activeMap.removeLineSegments(eraseRect, false, -1);
				}
			}
			cancelMode();

		}

		/*
		 * @see com.gametable.AbstractTool#mouseMoved(int, int)
		 */
		@Override
		public void mouseMoved(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			if (m_mouseAnchor != null)
			{
				m_mouseFloat = canvas.viewToModel(mouseX, mouseY);	// TODO #Useful?
				setTints(modifierMask);
				m_frame.repaint();
			}
		}

		/*
		 * @see com.gametable.ui.UIModeListener#selectMode()
		 */
		@Override
		public void selectMode()
		{
			m_mouseAnchor = null;
			m_mouseFloat = null;
			m_frame.setMapCursor(m_cursorPublish);
		}
	}
}
