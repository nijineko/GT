/*
 * SelectMode.java
 *
 * Copyright (C) 1999-2011 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.gametable.ui.modes;

import java.awt.*;
import java.awt.event.InputEvent;

import com.gametable.GametableApp;
import com.gametable.data.*;
import com.gametable.data.GameTableCore.MapType;
import com.gametable.ui.*;
import com.gametable.util.UtilityFunctions;

/**
 * Map tool for erasing lines.
 * 
 * @author iffy
 */
public class SelectMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final SelectMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new SelectMode();

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	private SelectMode()
	{
		super("Select");
		m_frame = GametableApp.getUserInterface();
		m_core = GametableApp.getCore();
		m_frame.registerUIMode(this, new ModeListener());
		
		UIModeAction action = new UIModeAction(
				this, 
				"Select",
				"Select element by clicking on them",
				0,
				"select.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorSelect = m_frame.createMapCursor(CURSOR_SELECT);
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
	 * Untint tinted elements
	 */
	private void clearTints()
	{
		m_frame.highlightAllMapElementInstances(false);
	}

	/**
	 * Set tints to matching elements
	 * 
	 * @param bIgnoreLock
	 */
	private void setTints(final boolean bIgnoreLock)
	{
		final MapRectangle selRect = new MapRectangle(m_mouseAnchor, m_mouseFloat);

		for (MapElement mapElement : m_core.getMap(GameTableCore.MapType.ACTIVE).getMapElements())
		{
			final int size = (int) (mapElement.getFaceSize() * GameTableMap.getBaseTileSize());

			MapCoordinates bottomRight = mapElement.getPosition().delta(size, size);

			final MapRectangle rect = new MapRectangle(mapElement.getPosition(), bottomRight);

			if (selRect.intersects(rect) && (!m_core.isMapElementLocked(mapElement) || bIgnoreLock))
			{
				m_frame.highlightMapElementInstance(mapElement, true);
			}
			else
			{
				m_frame.highlightMapElementInstance(mapElement, false);
			}
		}

		m_frame.repaint();
	}

	/**
	 * Box cursor name
	 */
	public static final String CURSOR_SELECT = "select_cursor";

	/**
	 * Single instance of this mode
	 */
	private static SelectMode			g_mode	= null;

	/**
	 * Pointer to game table core
	 */
	private final GameTableCore		m_core;

	/**
	 * Select cursor
	 */
	private final Cursor m_cursorSelect;

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
			m_frame.repaint();

		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			m_frame.unselectAllMapElementInstances(MapType.ACTIVE);
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
				boolean bIgnoreLock = true;
				
				if ((modifierMask & InputEvent.SHIFT_DOWN_MASK) == 0) // not holding shift
					bIgnoreLock = false;

				// Select highlighted, unlocked elements
				for (MapElement mapElement : m_core.getMap(GameTableCore.MapType.ACTIVE).getMapElements())
				{
					if (m_frame.isHighlighted(mapElement) && (!m_core.isMapElementLocked(mapElement) || bIgnoreLock))
					{
						m_frame.selectMapElementInstance(mapElement, MapType.ACTIVE, true);
					}
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
				setTints((modifierMask & InputEvent.SHIFT_DOWN_MASK) != 0);
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
			m_frame.setMapCursor(m_cursorSelect);
		}
	}
}
