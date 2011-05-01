/*
 * BoxMode.java
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

package com.galactanet.gametable.ui.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.ui.*;

/**
 * @author iffy
 * 
 * #GT-AUDIT HandTool
 */
public class HandMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final HandMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new HandMode();

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	private HandMode()
	{
		super("Scroll");
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());
		
		UIModeAction action = new UIModeAction(
				this, 
				"Pan",
				"Move the map around by clicking on the map and dragging",
				KeyEvent.VK_2,
				"hand.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorHand = m_frame.createMapCursor(CURSOR_HAND, CURSOR_HAND_HOTSPOT);
		m_cursorGrab = m_frame.createMapCursor(CURSOR_GRAB, CURSOR_GRAB_HOTSPOT);
	}

	/*
	 * @see com.galactanet.gametable.ui.UIMode#isActive()
	 */
	@Override
	public boolean isActive()
	{
		return (m_startScroll != null);
	}

	/*
	 * @see com.galactanet.gametable.ui.UIMode#paintTool(java.awt.Graphics2D, com.galactanet.gametable.ui.GametableCanvas)
	 */
	@Override
	protected void paintTool(Graphics2D g, GametableCanvas canvas)
	{
		// Nothing to paint
	}

	/**
	 * Grab cursor name
	 */
	public static final String CURSOR_GRAB = "grab.png";


	/**
	 * Hand cursor name
	 */
	public static final String CURSOR_HAND = "hand.png";

	/**
	 * Grab cursor hot spot
	 */
	private static final Point CURSOR_GRAB_HOTSPOT = new Point(8, 8);

	/**
	 * Hand cursor hot spot
	 */
	private static final Point CURSOR_HAND_HOTSPOT = new Point(8, 8);

	/**
	 * Single instance of this mode
	 */
	private static HandMode			g_mode	= null;
	
	/**
	 * Grab cursor
	 */
	private final Cursor m_cursorGrab;

	/**
	 * Hand cursor
	 */
	private final Cursor m_cursorHand;

	/**
	 * Instance to game table frame
	 */
	private final GametableFrame	m_frame;

	/**
	 * Start position for scrolling in mouse coordinates
	 */
	private Point           m_startMouse = new Point();
//	private MapCoordinates 					m_startMouse;


	/**
	 * Start position for scrolling in map coordinates
	 */
	private MapCoordinates           m_startScroll;

	/**
	 * Listener
	 */
	private class ModeListener implements UIModeListener
	{
		/*
		 * @see com.galactanet.gametable.ui.UIModeListener#cancelMode()
		 */
		@Override
		public void cancelMode()
		{
			m_startScroll = null;
			
			// TODO #UIMode Canceling "Hand Mode" should auto-return to previous mode
			m_frame.setMapCursor(m_cursorHand);
		}

		/*
		 * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			System.out.println("Hand Mode mouse pressed " + mouseX + " " + mouseY);
			m_startScroll = m_frame.getMapScrollPosition();
			m_startMouse.setLocation(mouseX, mouseY);
			
			m_frame.setMapCursor(m_cursorGrab);
		}

		/*
		 * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int)
		 */
		@Override
		public void mouseButtonReleased(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			cancelMode();
		}

		/*
		 * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
		 */
		@Override
		public void mouseMoved(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			if (m_startScroll != null)
			{
				canvas.moveScrollPosition(
						m_startMouse.x - mouseX, 
						m_startMouse.y - mouseY);
			}
		}

		/*
		 * @see com.galactanet.gametable.ui.UIModeListener#selectMode()
		 */
		@Override
		public void selectMode()
		{
			System.out.println("HAND MODE!");
			m_startScroll = null;
			m_frame.setMapCursor(m_cursorHand);
		}
	}
}