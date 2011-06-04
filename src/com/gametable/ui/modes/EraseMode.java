/*
 * EraseMode.java
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
import java.awt.event.KeyEvent;

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.GameTableMap;
import com.gametable.data.MapCoordinates;
import com.gametable.data.MapRectangle;
import com.gametable.ui.*;
import com.gametable.util.UtilityFunctions;

/**
 * Mode for erasing lines
 * 
 * @author iffy
 * 
 * #GT-AUDIT EraseTool
 */
public class EraseMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static EraseMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new EraseMode("Erase", false);

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	protected EraseMode(String text, boolean color)
	{
		super(text);
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());
		m_bEraseColor = color;
		
		UIModeAction action;
		if (color)
		{
			action = new UIModeAction(
					this, 
					"Color Eraser",
					"Erase drawings matching the selected color from the current map",
					KeyEvent.VK_8,
					"red_eraser.png");
		}
		else
		{
			action = new UIModeAction(
				this, 
				"Eraser",
				"Erase drawings on the map",
				KeyEvent.VK_7,
				"eraser.png");
		}
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorErase = m_frame.createMapCursor(CURSOR_ERASE);
		m_cursorEraseColor = m_frame.createMapCursor(CURSOR_ERASE_COLOR);
	}

	/*
	 * @see com.gametable.ui.UIMode#isActive()
	 */
	@Override
	public boolean isActive()
	{
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
			final Graphics2D g2 = (Graphics2D)g.create();

			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {
					2f
			}, 0f));
			Rectangle rect = UtilityFunctions.createRectangle(canvas.modelToView(m_mouseAnchor), canvas.modelToView(m_mouseFloat));
			g2.draw(rect);

			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {
					2f
			}, 2f));
			rect = UtilityFunctions.createRectangle(canvas.modelToView(m_mouseAnchor), canvas.modelToView(m_mouseFloat));
			g2.draw(rect);

			g2.dispose();
		}
	}

	/**
	 * Box cursor name
	 */
	public static final String CURSOR_ERASE = "eraser_cursor";

	/**
	 * Box cursor name
	 */
	public static final String CURSOR_ERASE_COLOR = "red_eraser_cursor";

	/**
	 * Single instance of this mode
	 */
	private static EraseMode			g_mode	= null;

	/**
	 * Determines if we are in 'erase color' mode
	 */
	private final boolean m_bEraseColor;

	/**
	 * Circle cursor
	 */
	private final Cursor m_cursorErase;
	
	/**
	 * Circle cursor
	 */
	private final Cursor m_cursorEraseColor;
	
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
	 * Position of the mouse when it started dragging
	 */
	private MapCoordinates				m_mousePosition;
	
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
			m_mouseAnchor = null;
			m_mouseFloat = null;
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			m_mousePosition = canvas.viewToModel(mouseX, mouseY);	// TODO #Useful?;
			m_mouseAnchor = m_mousePosition;
			if ((modifierMask & InputEvent.CTRL_DOWN_MASK) == 0)
			{
				m_mouseAnchor = m_frame.snapToGrid(m_mouseAnchor);
			}
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
				GameTableCore core = GametableApp.getCore();
				GameTableMap activeMap = core.getMap(GameTableCore.MapType.ACTIVE);

				if (m_bEraseColor)
				{
					activeMap.removeLineSegments(new MapRectangle(m_mouseAnchor, m_mouseFloat), true,
							m_frame.getDrawColor().getRGB());
				}
				else
				{
					activeMap.removeLineSegments(new MapRectangle(m_mouseAnchor, m_mouseFloat), false, 0);
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
				m_mousePosition = canvas.viewToModel(mouseX, mouseY);	// TODO #Useful?;
				m_mouseFloat = m_mousePosition;
				if ((modifierMask & InputEvent.CTRL_DOWN_MASK) == 0)
				{
					m_mouseFloat = m_frame.snapToGrid(m_mouseFloat);
				}

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
			
			if (m_bEraseColor)
				m_frame.setMapCursor(m_cursorEraseColor);
			else
				m_frame.setMapCursor(m_cursorErase);
		}
	}
}
