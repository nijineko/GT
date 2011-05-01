/*
 * MeasureMode.java
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

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.MapCoordinates;
import com.gametable.ui.*;
import com.gametable.util.Images;

/**
 * Measures distances on the map
 * 
 * @author ATW
 * 
 *         #GT-AUDIT MeasureMode
 */
public class MeasureMode extends UIMode
{
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final MeasureMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new MeasureMode();

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	private MeasureMode()
	{
		super("Measure");
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());
		
		
		UIModeAction action = new UIModeAction(
				this, 
				"Measure",
				"Measure distances within the map",
				KeyEvent.VK_6,
				"ruler.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorMeasure = m_frame.createMapCursor(CURSOR_MEASURE, CURSOR_MEASURE_HOTSPOT);
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
			GameTableCore core = GametableApp.getCore();

			final Graphics2D g2 = (Graphics2D) g.create();

			g2.addRenderingHints(Images.getRenderingHints());

			final double dist = core.getGridMode().getDistance(m_mouseFloat.x, m_mouseFloat.y, m_mouseAnchor.x, m_mouseAnchor.y);
			double squaresDistance = m_frame.mapDistanceToGridUnits(dist);
			squaresDistance = Math.round(squaresDistance * 100) / 100.0;

			final Color drawColor = m_frame.getDrawColor();
			g2.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), 102));
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			final Point drawAnchor = canvas.modelToView(m_mouseAnchor);
			final Point drawFloat = canvas.modelToView(m_mouseFloat);
			g2.drawLine(drawAnchor.x, drawAnchor.y, drawFloat.x, drawFloat.y);

			if (squaresDistance >= 0.75)
			{
				final Graphics2D g3 = (Graphics2D) g.create();
				g3.setFont(Font.decode("sans-12"));

				final String s = squaresDistance + m_frame.getGridUnit();

				final FontMetrics fm = g3.getFontMetrics();
				final Rectangle rect = fm.getStringBounds(s, g3).getBounds();

				rect.grow(3, 1);

				final Point drawPoint = new Point((drawAnchor.x + drawFloat.x) / 2, (drawAnchor.y + drawFloat.y) / 2);

				/*
				 * Point drawPoint = m_canvas.modelToDraw(m_mousePosition); drawPoint.y -= rect.height + rect.y + 10; Point
				 * viewPoint = m_canvas.modelToView(m_canvas.drawToModel(drawPoint)); if (viewPoint.y - rect.height < 0) {
				 * drawPoint = m_canvas.modelToDraw(m_mousePosition); drawPoint.y -= rect.y - 24; }
				 * 
				 * if (viewPoint.x + rect.width >= m_canvas.getWidth()) { drawPoint.x -= rect.width + 10; }
				 */
				g3.translate(drawPoint.x, drawPoint.y);
				g3.setColor(new Color(0x00, 0x99, 0x00, 0xAA));
				g3.fill(rect);
				g3.setColor(new Color(0x00, 0x66, 0x00));
				g3.draw(rect);
				g3.setColor(new Color(0xFF, 0xFF, 0xFF, 0xCC));
				g3.drawString(s, 0, 0);
				g3.dispose();
			}

			g2.dispose();
		}
	}

	/**
	 * Measure cursor name
	 */
	public static final String CURSOR_MEASURE = "ruler_cursor.png";

	/**
	 * Measure cursor hot spot
	 */
	private static final Point CURSOR_MEASURE_HOTSPOT = new Point(7, 7);

	/**
	 * Single instance of this mode
	 */
	private static MeasureMode		g_mode	= null;

	/**
	 * Measure cursor
	 */
	private final Cursor m_cursorMeasure;

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
			m_frame.repaint();
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			m_mousePosition = canvas.viewToModel(mouseX, mouseY);	// TODO #Useful?
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
			m_frame.setMapCursor(m_cursorMeasure);
		}
	}	
}