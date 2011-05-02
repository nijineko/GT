/*
 * PenMode.java
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

package com.gametable.ui.modes;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.List;

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.LineSegment;
import com.gametable.ui.*;

/**
 * Free hand drawing mode
 * 
 * @author iffy
 * 
 *         #GT-AUDIT PenTool
 */
public class PenMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final PenMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new PenMode();

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	private PenMode()
	{
		super("Drawing Pen");
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());
		
		UIModeAction action = new UIModeAction(
				this, 
				"Draw",
				"Draw freely on the map",
				KeyEvent.VK_3,
				"pen.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorPen = m_frame.createMapCursor(CURSOR_PEN, CURSOR_PEN_HOTSPOT);
	}

	/*
	 * @see com.gametable.ui.UIMode#isActive()
	 */
	@Override
	public boolean isActive()
	{
		return (m_penAsset != null);
	}

	/*
	 * @see com.gametable.ui.UIMode#paintTool(java.awt.Graphics2D, com.gametable.ui.GametableCanvas)
	 */
	@Override
	protected void paintTool(Graphics2D g, GametableCanvas canvas)
	{
		if (m_penAsset != null)
		{
			final Graphics2D g2 = (Graphics2D) g.create();
			m_penAsset.draw(g2, canvas);
			g2.dispose();
		}
	}

	/**
	 * Box cursor name
	 */
	public static final String CURSOR_PEN = "pen_cursor.png";

	/**
	 * Box cursor hot spot
	 */
	private static final Point CURSOR_PEN_HOTSPOT = new Point(7, 7);

	/**
	 * Single instance of this mode
	 */
	private static PenMode				g_mode	= null;

	/**
	 * Pen cursor
	 */
	private final Cursor m_cursorPen;
	
	/**
	 * Instance to game table frame
	 */
	private final GametableFrame	m_frame;
	
	/**
	 * Used pen settings
	 */
	private PenAsset	m_penAsset;

	
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
			m_penAsset = null;
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			m_penAsset = new PenAsset(m_frame.getDrawColor());
			m_penAsset.addPoint(canvas.viewToModel(mouseX, mouseY));	// TODO #Useful?);
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonReleased(int, int)
		 */
		@Override
		public void mouseButtonReleased(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			if (m_penAsset != null)
			{
				m_penAsset.smooth();
				List<LineSegment> lines = m_penAsset.getLineSegments();

				GametableApp.getCore().getMap(GameTableCore.MapType.ACTIVE).addLineSegments(lines);
			}

			cancelMode();
		}

		/*
		 * @see com.gametable.AbstractTool#mouseMoved(int, int)
		 */
		@Override
		public void mouseMoved(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			if (m_penAsset != null)
			{
				m_penAsset.addPoint(canvas.viewToModel(mouseX, mouseY));	// TODO #Useful?);
				m_frame.repaint();
			}
		}

		/*
		 * @see com.gametable.ui.UIModeListener#selectMode()
		 */
		@Override
		public void selectMode()
		{
			m_penAsset = null;
			m_frame.setMapCursor(m_cursorPen);
		}
	}
}
