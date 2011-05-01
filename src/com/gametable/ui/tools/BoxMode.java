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

package com.gametable.ui.tools;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.LineSegment;
import com.gametable.data.MapCoordinates;
import com.gametable.ui.*;
import com.gametable.util.Images;
import com.gametable.util.UtilityFunctions;

/**
 * Map tool for drawing boxes.
 * 
 * @author iffy (was BoxTool)
 * 
 *         #GT-AUDIT BoxMode (.1)
 */
public class BoxMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final BoxMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new BoxMode();

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	private BoxMode()
	{
		super("Draw Box");
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());
		
		UIModeAction action = new UIModeAction(
				this, 
				"Box",
				"Draw a box on the current map",
				KeyEvent.VK_5,
				"box.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorBox = m_frame.createMapCursor(CURSOR_BOX, CURSOR_BOX_HOTSPOT);		
	}

	/*
	 * @see com.gametable.ui.UIMode#isActive()
	 */
	@Override
	public boolean isActive()
	{
		// If we have an anchor point, then we're currently drawing a box
		return (m_mouseStart != null);
	}

	/*
	 * @see com.gametable.AbstractTool#paint(java.awt.Graphics)
	 */
	@Override
	public void paintTool(Graphics2D g, GametableCanvas canvas)
	{
		if (m_mouseStart != null)
		{
			final Graphics2D g2 = (Graphics2D) g.create();

			g2.addRenderingHints(Images.getRenderingHints());
			
			final Point rectPt1 = m_mouseStart;
			final Point rectPt2 = m_mousePosition;

			final Color drawColor = m_frame.getDrawColor();
			g2.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), 102));			
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			
			final Rectangle drawRect = UtilityFunctions.createRectangle(rectPt1, rectPt2);
			g2.draw(drawRect);
			g2.dispose();

//			double squaresWidth = m_frame.mapDistanceToGridUnits(canvas.viewToModel(m_mouseStart.x - m_mousePosition.x));
//			double squaresHeight = m_frame.mapDistanceToGridUnits(canvas.viewToModel(m_mouseStart.y - m_mousePosition.y));
//
//			// Draw sizes
//			if (squaresWidth >= 0.75)
//			{
//				squaresWidth = Math.round(squaresWidth * 100) / 100.0;
//
//				final Graphics2D g3 = (Graphics2D) g.create();
//
//				g3.setFont(Font.decode("sans-12"));
//
//				// String s1 = squaresWidth + " x " + squaresHeight + "u";
//				final String sw = Double.toString(squaresWidth) + m_frame.getGridUnit();
//
//				final FontMetrics fm = g3.getFontMetrics();
//				final Rectangle rect = fm.getStringBounds(sw, g3).getBounds();
//
//				rect.grow(3, 1);
//				
//				rectPt1.x = (rectPt1.x + rectPt2.x) / 2;
//				rectPt1.y = rectPt2.y - 10;
//				g3.translate(rectPt1.x, rectPt1.y);
//				g3.setColor(new Color(0x00, 0x99, 0x00, 0xAA));
//				g3.fill(rect);
//				g3.setColor(new Color(0x00, 0x66, 0x00));
//				g3.draw(rect);
//				g3.setColor(new Color(0xFF, 0xFF, 0xFF, 0xCC));
//				g3.drawString(sw, 0, 0);
//				g3.dispose();
//
//			}
//
//			if (squaresHeight > 0.75)
//			{
//				
//				final Graphics2D g4 = (Graphics2D) g.create();
//				squaresHeight = Math.round(squaresHeight * 100) / 100.0;
//				g4.setFont(Font.decode("sans-12"));
//				final String sh = Double.toString(squaresHeight) + m_frame.getGridUnit();
//				final FontMetrics fm2 = g4.getFontMetrics();
//				final Rectangle rect2 = fm2.getStringBounds(sh, g4).getBounds();
//				rect2.grow(3, 1);
//				rectPt1.x = rectPt2.x + 10;
//				rectPt1.y = (rectPt1.y + rectPt2.y) / 2;
//				g4.translate(rectPt1.x, rectPt1.y);
//				g4.setColor(new Color(0x00, 0x99, 0x00, 0xAA));
//				g4.fill(rect2);
//				g4.setColor(new Color(0x00, 0x66, 0x00));
//				g4.draw(rect2);
//				g4.setColor(new Color(0xFF, 0xFF, 0xFF, 0xCC));
//				g4.drawString(sh, 0, 0);
//				g4.dispose();
//			}
		}
	}

	/**
	 * Box cursor name
	 */
	public static final String CURSOR_BOX = "box_cursor.png";

	/**
	 * Box cursor hot spot
	 */
	private static final Point CURSOR_BOX_HOTSPOT = new Point(7, 7);

	/**
	 * Single instance of this mode
	 */
	private static BoxMode			g_mode	= null;

	/**
	 * Box cursor
	 */
	private final Cursor m_cursorBox;

	/**
	 * Instance to game table frame
	 */
	private final GametableFrame	m_frame;
	
	/**
	 * Position of the mouse while moving
	 */
	private Point				m_mousePosition;
	
	/**
	 * Position of the mouse when it started dragging
	 */
	private Point				m_mouseStart;

	
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
			m_mouseStart = null;
			m_mousePosition = null;
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			m_mouseStart = new Point(mouseX, mouseY);
			
			if ((modifierMask & InputEvent.CTRL_DOWN_MASK) == 0)		// TODO #XPlatform Is this proper cross-platform method?
			{
				m_mouseStart = canvas.snapToGrid(m_mouseStart);
			}
			
			m_mousePosition = new Point(m_mouseStart);
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonReleased(int, int)
		 */
		@Override
		public void mouseButtonReleased(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			if ((m_mouseStart != null) && !m_mouseStart.equals(m_mousePosition))
			{
				// we're going to add 4 lines
				final Color drawColor = m_frame.getDrawColor();
				final MapCoordinates topLeft = canvas.viewToModel(m_mouseStart);
				final MapCoordinates bottomRight = topLeft.delta(canvas.viewToModel(m_mousePosition.x - m_mouseStart.x), canvas.viewToModel(m_mousePosition.y - m_mouseStart.y));
				final MapCoordinates topRight = new MapCoordinates(bottomRight.x, topLeft.y);
				final MapCoordinates bottomLeft = new MapCoordinates(topLeft.x, bottomRight.y);

				final LineSegment top = new LineSegment(topLeft, topRight, drawColor);
				final LineSegment left = new LineSegment(topLeft, bottomLeft, drawColor);
				final LineSegment right = new LineSegment(topRight, bottomRight, drawColor);
				final LineSegment bottom = new LineSegment(bottomLeft, bottomRight, drawColor);

				java.util.List<LineSegment> lines = new ArrayList<LineSegment>(4);
				lines.add(top);
				lines.add(left);
				lines.add(right);
				lines.add(bottom);

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
			if (m_mouseStart != null)
			{
				m_mousePosition.setLocation(mouseX, mouseY);
				
				if ((modifierMask & InputEvent.CTRL_DOWN_MASK) == 0)	// TODO #XPlatform
					m_mousePosition = canvas.snapToGrid(m_mousePosition);

				m_frame.repaint();
			}
		}

		/*
		 * @see com.gametable.ui.UIModeListener#selectMode()
		 */
		@Override
		public void selectMode()
		{
			m_mousePosition = null;
			m_mouseStart = null;
			m_frame.setMapCursor(m_cursorBox);
		}

	}
}
