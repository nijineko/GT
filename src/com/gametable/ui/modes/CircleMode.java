/*
 * CircleMode.java
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
import com.gametable.data.LineSegment;
import com.gametable.data.MapCoordinates;
import com.gametable.ui.*;

/**
 * Tool for drawing circles on the map.
 * 
 * @author iffy 
 * #GT-AUDIT CircleTool
 */
public class CircleMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final CircleMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new CircleMode();

		return g_mode;
	}

	/**
	 * Default Constructor yse getUIMode to get instance
	 */
	private CircleMode()
	{
		super("Draw Circle");
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());
		
		UIModeAction action = new UIModeAction(
				this, 
				"Circle",
				"Draw a circle on the current map",
				KeyEvent.VK_0,
				"circle.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorCircle = m_frame.createMapCursor(CURSOR_CIRCLE, CURSOR_CIRCLE_HOTSPOT);
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
			int circleDiameter = 0;
			int circleRadius = 0;

			Graphics2D g2 = (Graphics2D)g.create();

			//g2.addRenderingHints(UtilityFunctions.STANDARD_RENDERING_HINTS);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			GameTableCore core = GametableApp.getCore();

			double dist = core.getGridMode().getDistance(m_mouseFloat.x, m_mouseFloat.y, m_mouseAnchor.x,
					m_mouseAnchor.y);
			double squaresDistance = m_frame.mapDistanceToGridUnits(dist);
			squaresDistance = Math.round(squaresDistance * 100) / 100.0;

			Color drawColor = m_frame.getDrawColor();

			g2.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), 102));
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Point drawAnchor = canvas.modelToView(m_mouseAnchor);
			Point drawFloat = canvas.modelToView(m_mouseFloat);
			// draw line out to circle circumference
			g2.drawLine(drawAnchor.x, drawAnchor.y, drawFloat.x, drawFloat.y);
			// get the length of the hypotenuse
			circleRadius = (int)(java.lang.Math.sqrt(java.lang.Math.pow(Math.abs(drawFloat.x - drawAnchor.x), 2)
					+ java.lang.Math.pow(Math.abs(drawFloat.y - drawAnchor.y), 2)));
			// locate the upper left corner for the demo circle
			circleDiameter = 2 * circleRadius;
			// draw the circle
			g2.drawOval((drawAnchor.x - circleRadius), (drawAnchor.y - circleRadius), circleDiameter, circleDiameter);

			double indicatorThreshold = .10 * m_frame.getGridUnitConversionRate();
			if (squaresDistance >= indicatorThreshold)
			{
				Graphics2D g3 = (Graphics2D)g.create();
				g3.setFont(Font.decode("sans-12"));

				String s = String.valueOf(squaresDistance) + m_frame.getGridUnit();
				FontMetrics fm = g3.getFontMetrics();
				Rectangle rect = fm.getStringBounds(s, g3).getBounds();

				rect.grow(3, 1);
				// display the radius
				Point drawPoint = new Point((drawAnchor.x + drawFloat.x) / 2, (drawAnchor.y + drawFloat.y) / 2);
				g3.translate(drawPoint.x, drawPoint.y);
				g3.setColor(new Color(0x00, 0x99, 0x00, 0xAA));
				g3.fill(rect);
				g3.setColor(new Color(0x00, 0x66, 0x00));
				g3.draw(rect);
				g3.setColor(new Color(0xFF, 0xFF, 0xFF, 0xCC));
				g3.drawString(s, 0, 0);
				g3.dispose();
			}
			
			// don't forget the penAsset
			//m_penAsset.draw(g2, m_canvas);
			g2.dispose();
		}
	}

	/**
	 * Box cursor name
	 */
	public static final String CURSOR_CIRCLE = "circle_cursor.png";

	/**
	 * Box cursor hot spot
	 */
	private static final Point CURSOR_CIRCLE_HOTSPOT = new Point(7, 7);
	
	/**
	 * Singleton instance of the mode
	 */
	private static CircleMode g_mode;

	/**
	 * Circle cursor
	 */
	private final Cursor m_cursorCircle;

	/**
	 * Frame instance
	 */
	private final GametableFrame m_frame;
	

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
			m_mousePosition = canvas.viewToModel(mouseX, mouseY	); // TODO #Useful?
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
			PenAsset penAsset = new PenAsset(m_frame.getDrawColor());

			// Figure the radius of the circle and use a PenAsset as if we had drawn
			// the circle with the PenTool.
			if ((m_mouseAnchor != null) && !m_mouseAnchor.equals(m_mouseFloat))
			{        
				double rad = m_mouseAnchor.distance(m_mouseFloat);
				// With this loop, all circles are composed of the same number of segments regardless of size. 
				// Maybe make theta increment dependent on radius?
				for (double theta = 0; theta < 2 * Math.PI; theta += .1)
				{
					penAsset.addPoint(m_mouseAnchor.delta((int)(Math.cos(theta) * rad), (int)(Math.sin(theta) * rad)));
				}
				penAsset.addPoint(m_mouseAnchor.delta((int)rad, 0));
				// The call to smooth() reduces the number of line segments in the
				// circle, drawing it faster but making it rougher. Uncomment if
				// redrawing takes too long.
				// m_penAsset.smooth();
				java.util.List<LineSegment> lines = penAsset.getLineSegments();

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
			if (m_mouseAnchor != null)
			{
				m_mousePosition = canvas.viewToModel(mouseX, mouseY);	// TODO #Useful?
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
			m_frame.setMapCursor(m_cursorCircle );
		}

	}
	
}
