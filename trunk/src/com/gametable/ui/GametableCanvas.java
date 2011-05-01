/*
 * GametableCanvas.java
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
 * 
 */

package com.gametable.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

import com.gametable.GametableApp;
import com.gametable.data.*;
import com.gametable.data.GameTableCore.MapType;
import com.gametable.data.MapElementTypeIF.Layer;
import com.gametable.net.NetworkEvent;
import com.gametable.ui.tools.HandMode;
import com.gametable.ui.tools.MapElementMode;
import com.gametable.util.ImageCache;
import com.gametable.util.Images;
import com.gametable.util.UtilityFunctions;

/**
 * Component handling the display of the public and private maps.
 * 
 * This component is passed along to other components when required.
 * 
 * All methods dealing with pixels are found within GametableCanvas
 * 
 * @author sogetsu
 * 
 *         #GT-AUDIT GametableCanvas
 */
public class GametableCanvas extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener
{
	/**
	 * Constructor
	 * 
	 * @param frame gametable frame
	 */
	protected GametableCanvas(GametableFrame frame)
	{
		m_frame = frame;
		m_core = GametableApp.getCore();

		setFocusable(true);
		setRequestFocusEnabled(true);

		addMouseListener(this);
		addMouseMotionListener(this);
		addFocusListener(new FocusListener() {
			/*
			 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
			 */
			@Override
			public void focusGained(final FocusEvent e)
			{
				final JPanel panel = (JPanel) getParent();
				panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), LineBorder.createBlackLineBorder()));
			}

			/*
			 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
			 */
			@Override
			public void focusLost(final FocusEvent e)
			{
				final JPanel panel = (JPanel) getParent();
				panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(1, 1, 1, 1)));
			}

		});

		initializeKeys();

		updateTileSize();

		GameTableMapListenerIF mapListener = new CanvasMapListener();
		MapElementListenerIF mapElementListener = new CanvasMapElementListener();

		GameTableMap publicMap = m_core.getMap(GameTableCore.MapType.PUBLIC);
		publicMap.addListener(mapListener);
		publicMap.addMapElementListener(mapElementListener);

		GameTableMap privateMap = m_core.getMap(GameTableCore.MapType.PRIVATE);
		privateMap.addListener(mapListener);
		privateMap.addMapElementListener(mapElementListener);
	}

	/**
	 * Draw a faded copy of the map element onto the provided canvas
	 * 
	 * @param g
	 * @param canvas
	 */
	public void drawGhostlyToCanvas(MapElement el, Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		g2.setComposite(UtilityFunctions.getGhostlyComposite());
		el.getRenderer().drawToCanvas(g2, this);
		g2.dispose();
	}

	/**
	 * Return visible line width, based on zoom level
	 * 
	 * @return line width in pixel
	 */
	public int getLineStrokeWidth()
	{
		switch (m_zoom)
		{
		case 0:
			return 3;

		case 1:
			return 2;

		case 2:
			return 2;

		case 3:
			return 1;

		default:
			return 1;
		}
	}

	/**
	 * Gets the current scroll position
	 * 
	 * @return
	 */
	public Point getScrollPosition()
	{
		return m_scrollPosition;
	}

	/**
	 * Gets the X coordinate of the scroll position
	 * 
	 * @return
	 */
	public int getScrollX()
	{
		return m_scrollPosition.x;
	}

	/**
	 * Gets the X coordinate of the scroll position
	 * 
	 * @return
	 */
	public int getScrollY()
	{
		return m_scrollPosition.y;
	}

	/**
	 * Get actual square size based on current zoom level
	 * 
	 * @return Size of a square, in pixels (view coordinates)
	 */
	public int getTileSize()
	{
		return m_tileSize;
	}

	/**
	 * Get the visible range within the map, based on the given zoom level
	 * 
	 * @param zoomLevel
	 * @return rectangle in model coordinates
	 */
	public MapRectangle getVisibleCanvasRect(final int zoomLevel)
	{
		final MapCoordinates topLeft = viewToModel(0, 0);

		int canvasW = 0;
		int canvasH = 0;

		switch (zoomLevel)
		{
		case 0:
			canvasW = getWidth();
			canvasH = getHeight();
			break;

		case 1:
			canvasW = (getWidth() * 4) / 3;
			canvasH = (getHeight() * 4) / 3;
			break;

		case 2:
			canvasW = getWidth() * 2;
			canvasH = getHeight() * 2;
			break;

		case 3:
			canvasW = getWidth() * 4;
			canvasH = getHeight() * 4;
			break;

		case 4:
			canvasW = getWidth() * 8;
			canvasH = getHeight() * 8;
			break;
		}

		final MapRectangle visbleCanvas = new MapRectangle(topLeft, canvasW, canvasH);

		return visbleCanvas;
	}

	/**
	 * Get current zoom level
	 * 
	 * @return
	 */
	public int getZoomLevel()
	{
		return m_zoom;
	}

	/**
	 * Verifies if view coordinates are visible within the canvas.
	 * 
	 * @param viewX
	 * @param viewY
	 * @return
	 */
	public boolean isPointVisible(int viewX, int viewY)
	{
		if (viewX < 0 || viewY < 0)
			return false;

		if (viewX > getWidth())
			return false;

		if (viewY > getHeight())
			return false;

		return true;
	}

	/**
	 * Convert coordinates from map coordinates to Graphics device coordinates
	 * 
	 * @param modelPoint Map coordinates
	 * @return view coordinates
	 */
	public Point modelToView(final MapCoordinates modelPoint)
	{
		return new Point(modelToView(modelPoint.x), modelToView(modelPoint.y));
	}

	/**
	 * Convert coordinates from map coordinates to Graphics device coordinates
	 * 
	 * @param modelPoint
	 * @return
	 */
	public Rectangle modelToView(MapRectangle modelRect)
	{
		Point topLeft = modelToView(modelRect.topLeft);
		Point bottomRight = new Point(modelToView(modelRect.topLeft.x + modelRect.width), modelToView(modelRect.topLeft.y + modelRect.height));
		
		return new Rectangle(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
	}

	/*
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(final MouseEvent e)
	{
		// Ignored - java's click support is not sufficient for our needs
	}

	/*
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseDragged(final MouseEvent e)
	{
		// Push to our own drag handling
		mouseMoved(e);
	}

	/*
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseEntered(final MouseEvent e)
	{
		m_bMouseOnView = true;
	}

	/*
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseExited(final MouseEvent e)
	{
		m_bMouseOnView = false;
	}

	/*
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseMoved(final MouseEvent e)
	{
		int x = e.getX() + m_scrollPosition.x;
		int y = e.getY() + m_scrollPosition.y;

		m_mousePositionModel = viewToModel(x, y);
		if (isPointing())
			return;

		UIModeListener listener = m_frame.getUIModeListener(null);
		if (listener != null)
			listener.mouseMoved(this, x, y, e.getModifiersEx());
	}

	/*
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed(final MouseEvent e)
	{
		int x = e.getX() + m_scrollPosition.x;
		int y = e.getY() + m_scrollPosition.y;

		requestFocus();
		m_mousePositionModel = viewToModel(x, y);
		if (isPointing())
		{
			return;
		}

		// this code deals with making a right click automatically be the hand tool
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			m_rightClicking = true;
			m_modePrevious = m_frame.setUIMode(HandMode.getUIMode());

			UIModeListener listener = m_frame.getUIModeListener(null);
			if (listener != null)
				listener.mouseButtonPressed(this, x, y, e.getModifiersEx());
		}
		else
		{
			m_rightClicking = false;
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				UIModeListener listener = m_frame.getUIModeListener(null);
				if (listener != null)
					listener.mouseButtonPressed(this, x, y, e.getModifiersEx());
			}
		}

	}

	/*
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased(final MouseEvent e)
	{
		int x = e.getX() + m_scrollPosition.x;
		int y = e.getY() + m_scrollPosition.y;

		m_mousePositionModel = viewToModel(x, y);
		if (isPointing())
			return;

		UIModeListener listener = m_frame.getUIModeListener(null);
		if (listener != null)
			listener.mouseButtonReleased(this, x, y, e.getModifiersEx());

		if (m_rightClicking)
		{
			// return to arrow too
			if (m_modePrevious != null)
				m_frame.setUIMode(m_modePrevious);

			m_rightClicking = false;
		}
	}

	/*
	 * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
	 */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e)
	{
		if (e.getWheelRotation() < 0)
		{
			// zoom in
			centerZoom(-1);
		}
		else if (e.getWheelRotation() > 0)
		{
			// zoom out
			centerZoom(1);
		}
		repaint();
	}

	/**
	 * Move the canvas' scroll position
	 * 
	 * @param dx
	 * @param dy
	 */
	public void moveScrollPosition(int dx, int dy)
	{
		setScrollPosition(m_scrollPosition.x + dx, m_scrollPosition.y + dy);
		repaint();
	}

	/*
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(final Graphics graphics)
	{
		paintComponent(graphics, getWidth(), getHeight());
	}

	/**
	 * Set the scroll position
	 * 
	 * @param x x coordinates of the scroll position
	 * @param y y coordinates of the scroll position
	 */
	public void setScrollPosition(int x, int y)
	{
		m_scrollPosition.setLocation(x, y);
	}

	/**
	 * Set the scroll position
	 *
	 * @param newPos coordinates of the new scroll position
	 * 	 
	 */
	public void setScrollPosition(Point newPos)
	{
		m_scrollPosition.setLocation(newPos);
	}

	/**
	 * Snap view coordinates to the grid.  Adjusts the coordinates to fit on the tiles' current grid mode.
	 * 
	 * @param mapPos
	 * @return Snapped coordinates
	 */
	public Point snapToGrid(final Point point)
	{
		MapCoordinates c = viewToModel(point.x, point.y);
		c = m_core.getGridMode().getSnappedMapCoordinates(c);
		return modelToView(c);
	}

	/**
	 * Converts a distance in pixels (view coordinates) to a distance in map coordinates
	 * 
	 * @param pixels Number of pixels
	 * @return Number of map units
	 * 
	 */
	public int viewToModel(int pixels)
	{
		double squares = pixels / (double) m_tileSize;
		return (int) (squares * GameTableMap.getBaseTileSize());
	}

	/**
	 * Convert view coordinates to map coordinates
	 * 
	 * @param viewX
	 * @param viewY
	 * @return
	 */
	public MapCoordinates viewToModel(final int viewX, final int viewY)
	{
		return new MapCoordinates(viewToModel(viewX), viewToModel(viewY));
	}

	/**
	 * Converts coordinates in pixels (view) to map coordinates
	 * 
	 * @param viewPoint
	 * @return
	 */
	public MapCoordinates viewToModel(final Point viewPoint)
	{
		return viewToModel(viewPoint.x, viewPoint.y);
	}

	/**
	 * Do the actual centering
	 * 
	 * @param modelCenter
	 * @param zoomLevel
	 */
	protected void centerView(MapCoordinates modelCenter, final int zoomLevel)
	{
		// if you re-center for any reason, your tool action is canceled

		UIModeListener listener = m_frame.getUIModeListener(null);
		if (listener != null)
			listener.cancelMode();

		// make the sent in x and y our center, ad the sent in zoom.
		// So start with the zoom
		setZoomLevel(zoomLevel);

		final Point viewCenter = modelToView(modelCenter);

		// find where the top left would have to be, based on our size
		final int tlX = viewCenter.x - getWidth() / 2;
		final int tlY = viewCenter.y - getHeight() / 2;

		// that is our new scroll position
		final MapCoordinates newModelPoint = viewToModel(tlX, tlY);
		// scrollMapTo(newModelPoint);

		smoothScrollTo(newModelPoint);
	}

	/**
	 * export the map to a jpeg image
	 * 
	 * @param mapToExport instance of the map that should be exported. If null will use the active map
	 * @param outputFile file where to save the result
	 * @throws IOException if file saving causes an error
	 */
	protected void exportMap(GameTableMap mapToExport, File outputFile) throws IOException
	{
		if (mapToExport == null)
			mapToExport = m_core.getMap(GameTableCore.MapType.ACTIVE);

		MapRectangle mapBoundsModel = mapToExport.getBounds();
		Rectangle mapBounds = modelToView(mapBoundsModel);

		int squareSize = getTileSize();
		mapBounds.grow(squareSize, squareSize);

		BufferedImage image = new BufferedImage(mapBounds.width, mapBounds.height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();

		Point scrollPos = getScrollPosition();

		setScrollPosition(mapBounds.x, mapBounds.y);

		paintComponent(g, mapBounds.width, mapBounds.height);

		setScrollPosition(scrollPos);

		ImageIO.write(image, "jpg", outputFile);
	}

	/**
	 * Initialize canvas (called by frame, when proper)
	 */
	protected void init()
	{
		m_mapBackground = ImageCache.getImage(new File("assets/mapbk.png"));
		m_pointingImage = ImageCache.getImage(new File("assets/whiteHand.png"));

		addMouseWheelListener(this);

		m_frame.setUIMode(MapElementMode.getUIMode());
	}

	/**
	 * Verifies if map coordinates are visible within the canvas. Plug-ins should call the method from GametableFrame
	 * 
	 * @param modelPoint
	 * @return
	 * 
	 * TODO #Move to Frame?
	 */
	protected boolean isPointVisible(final MapCoordinates modelPoint)
	{
		
		MapRectangle rect = new MapRectangle(
				viewToModel(0, 0),
				viewToModel(getWidth()),
				viewToModel(getHeight())
				);
		
		return rect.contains(modelPoint);
	}

	/**
	 * Verifies if a text component currently has focus
	 * @return true if a text component currently has focus
	 */
	protected boolean isTextFieldFocused()
	{
		final Component focused = m_frame.getFocusOwner();
		if (focused instanceof JTextComponent)
		{
			final JTextComponent textComponent = (JTextComponent) focused;
			return textComponent.isEditable();
		}

		return false;
	}

	/**
	 * called by the pogs area when a pog is being dragged
	 * TODO #PogPanel Should not be here
	 */
	protected void mapElementDrag()
	{
		m_newMapElementIsBeingDragged = true;
		updateMapElementDropLocation();

		repaint();
	}

	/**
	 * Called by the frame when it is notified of a background change
	 * 
	 * @param isMapElementType
	 * @param elementType
	 * @param color
	 */
	protected void onBackgroundChanged(boolean isMapElementType, MapElementTypeIF elementType, BackgroundColor color)
	{
		if (isMapElementType)
		{
			m_mapBackground = elementType.getImage();
		}
		else
		{
			Image newBk = null;

			// @revise should all 'assets/' images be within a jar instead?

			switch (color)
			{
			case GREEN:
				newBk = ImageCache.getImage(new File("assets/mapbk_green.png"));
				break;
				
			case DARK_GREY:
				newBk = ImageCache.getImage(new File("assets/mapbk_dgrey.png"));
				break;
				
			case GREY:
				newBk = ImageCache.getImage(new File("assets/mapbk_grey.png"));
				break;
				
			case BLUE:
				newBk = ImageCache.getImage(new File("assets/mapbk_blue.png"));
				break;
				
			case BLACK:
				newBk = ImageCache.getImage(new File("assets/mapbk_black.png"));
				break;
				
			case WHITE:
				newBk = ImageCache.getImage(new File("assets/mapbk_white.png"));
				break;
				
			case DARK_BLUE:
				newBk = ImageCache.getImage(new File("assets/mapbk_dblue.png"));
				break;
				
			case DARK_GREEN:
				newBk = ImageCache.getImage(new File("assets/mapbk_dgreen.png"));
				break;
				
			case BROWN:
				newBk = ImageCache.getImage(new File("assets/mapbk_brown.png"));
				break;
				
			default:
				newBk = ImageCache.getImage(new File("assets/mapbk.png"));
				break;
			}

			if (newBk != null)
				m_mapBackground = newBk;
		}

		repaint();
	}

	/**
	 * Called at the end of a drag & drop operation from the MapElementPanel
	 * TODO #PogPanel - This should not be here.
	 */
	protected void onReleaseMapElement()
	{
		m_newMapElementIsBeingDragged = false;
		updateMapElementDropLocation();

		final MapElement pog = getPogPanel().getGrabbedPog();
		if (pog != null)
		{
			// only add the pog if it's in the viewport
			if (isPointVisible(getDraggedMapElementMousePosition()))
			{
				// #randomrotate
				if (m_frame.shouldRotateMapElements())
				{
					boolean fh = false;
					boolean fv = UtilityFunctions.getRandom(2) == 0 ? false : true;

					int a = UtilityFunctions.getRandom(24) * 15;
					pog.setAngleFlip(a, fh, fv);
				}
				// add this pog to the list
				m_core.getMap(GameTableCore.MapType.ACTIVE).addMapElement(pog);
			}
		}

		// make the arrow the current tool
		m_frame.setUIMode(null);
	}

	/**
	 * TODO #PogPanel Should not be here
	 * @param toReplace
	 * @param replaceWith
	 */
	protected void replacePogs(final MapElementTypeIF toReplace, final MapElementTypeIF replaceWith)
	{
		GameTableMap mapToReplace = m_core.getMap(GameTableCore.MapType.ACTIVE);

		for (MapElement pog : mapToReplace.getMapElements())
		{
			if (pog.getMapElementType() == toReplace)
			{
				pog.setMapElementType(replaceWith);
			}
		}
	}

	/**
	 * Scroll map to given position
	 * 
	 * @param modelPoint
	 */
	protected void scrollMapTo(MapCoordinates modelPoint)
	{
		final Point target = modelToView(modelPoint);
		setScrollPosition(target.x, target.y);
		repaint();
	}

	/**
	 * Scroll the map so the specified map element is centered on screen
	 * @param mapElement
	 */
	protected void scrollToMapElement(final MapElement mapElement)
	{
		MapCoordinates modelPos = new MapCoordinates(
				mapElement.getPosition().x + (mapElement.getWidth() / 2), 
				mapElement.getPosition().y + (mapElement.getHeight() / 2));
		
		final Point viewPos = modelToView(modelPos);
		
		viewPos.x -= (getWidth() / 2);
		viewPos.y -= (getHeight() / 2);
		
		modelPos = viewToModel(viewPos);
		smoothScrollTo(modelPos);
	}

	/**
	 * Set the current zoom level
	 * 
	 * @param zoomLevel
	 * 
	 * TODO #ZoomLevel Consider using enums instead of numbers
	 */
	protected void setZoomLevel(int zoomLevel)
	{
		if (zoomLevel < 0)
			zoomLevel = 0;

		if (zoomLevel >= GametableFrame.MAX_ZOOM_LEVEL)
			zoomLevel = GametableFrame.MAX_ZOOM_LEVEL - 1;

		if (m_zoom != zoomLevel)
		{
			m_zoom = zoomLevel;
			updateTileSize();
			repaint();
		}
	}

	/**
	 * Tick of the internal status timer
	 * @param ms
	 */
	protected void tick(final long ms)
	{
		if (m_scrolling)
		{
			m_scrollTime += ms;
			float pos = m_scrollTime / (float) m_scrollTimeTotal;
			if (pos >= 1f)
			{
				scrollMapTo(m_startScroll.delta(m_scrollDelta));
				m_scrolling = false;
			}
			else
			{
				pos = (float) (Math.sin((pos * Math.PI) - (Math.PI / 2)) + 1) / 2;

				MapCoordinates point = m_startScroll.delta(Math.round(m_scrollDelta.x * pos), Math.round(m_scrollDelta.y * pos));
				scrollMapTo(point);
			}
		}
	}

	/**
	 * Modify zoom level, keeping the map centered
	 * @param delta
	 */
	private void centerZoom(final int delta)
	{
		// can't do this at all if we're dragging
		if (m_newMapElementIsBeingDragged)
		{
			return;
		}
		// note the model location of the center
		final MapCoordinates modelCenter = viewToModel(getWidth() / 2, getHeight() / 2);

		// do the zoom
		setZoomLevel(m_zoom + delta);

		// note the view location of the model center
		final Point viewCenter = modelToView(modelCenter);

		// note the present actual center
		final int presentCenterX = getWidth() / 2;
		final int presentCenterY = getHeight() / 2;

		// set up the scroll to enforce the center being where it's supposed to be
		Point pos = getScrollPosition();
		final int scrX = pos.x - (presentCenterX - viewCenter.x);
		final int scrY = pos.y - (presentCenterY - viewCenter.y);
		
		setScrollPosition(scrX, scrY);
	}

	//TODO #Cleanup
//	private MapCoordinates drawToModel(final int modelX, final int modelY)
//	{
//		return drawToModel(new Point(modelX, modelY));
//	}
//
//	private MapCoordinates drawToModel(final Point drawPoint)
//	{
//		final double squaresX = (double) (drawPoint.x) / (double) m_squareSize;
//		final double squaresY = (double) (drawPoint.y) / (double) m_squareSize;
//
//		final int modelX = (int) (squaresX * GameTableMap.getBaseSquareSize());
//		final int modelY = (int) (squaresY * GameTableMap.getBaseSquareSize());
//
//		return new MapCoordinates(modelX, modelY);
//	}

	//
	// protected ToolIF getActiveTool()
	// {
	// if (m_activeToolId < 0)
	// {
	// return NULL_TOOL;
	// }
	// return m_frame.getToolManager().getToolInfo(m_activeToolId).getTool();
	// }

	/**
	 * Draw the map background
	 * @param g graphics device
	 * @param topLeftX
	 * @param topLeftY
	 * @param width
	 * @param height
	 */
	private void drawMapBackground(final Graphics g, final int topLeftX, final int topLeftY, final int width, final int height)
	{
		if (m_mapBackground != null)
		{
			// background image
			int qx = Math.abs(topLeftX) / m_mapBackground.getWidth(null);
			if (topLeftX < 0)
			{
				qx++;
				qx = -qx;
			}

			int qy = Math.abs(topLeftY) / m_mapBackground.getHeight(null);
			if (topLeftY < 0)
			{
				qy++;
				qy = -qy;
			}

			final int linesXOffset = qx * m_mapBackground.getWidth(null);
			final int linesYOffset = qy * m_mapBackground.getHeight(null);
			final int vLines = width / m_mapBackground.getWidth(null) + 2;
			final int hLines = height / m_mapBackground.getHeight(null) + 2;

			for (int i = 0; i < vLines; i++)
			{
				for (int j = 0; j < hLines; j++)
				{
					g.drawImage(m_mapBackground, i * m_mapBackground.getWidth(null) + linesXOffset, j * m_mapBackground.getHeight(null) + linesYOffset, null);
				}
			}
		}
	}

	/**
	 * Return the current position of the map element that is being dragged
	 */
	private MapCoordinates getDraggedMapElementMousePosition()
	{
		final Point screenMousePoint = getPogPanel().getGrabPosition();
		final Point canvasView = UtilityFunctions.getComponentCoordinates(this, screenMousePoint);

		return viewToModel(canvasView);
	}

	/**
	 * TODO #PogPanel Move this
	 * @return
	 */
	private PogPanel getPogPanel()
	{
		return m_frame.getPogPanel();
	}

	/**
	 * Initializes all the keys for the canvas.
	 * 
	 * TODO #WishList Make keystrokes configurable
	 */
	private void initializeKeys()
	{
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SPACE"), "startPointing");
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released SPACE"), "stopPointing");

		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("shift pressed SHIFT"), "shiftDown");
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released SHIFT"), "shiftUp");

		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control pressed CONTROL"), "controlDown");
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released CONTROL"), "controlUp");

		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt pressed ALT"), "altDown");
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released ALT"), "altUp");

		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SUBTRACT"), "zoomIn");
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed MINUS"), "zoomIn");

		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ADD"), "zoomOut");
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed PLUS"), "zoomOut");
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed EQUALS"), "zoomOut");

		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed UP"), "scrollUp");
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed KP_UP"), "scrollUp");
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed DOWN"), "scrollDown");
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed KP_DOWN"), "scrollDown");
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed LEFT"), "scrollLeft");
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed KP_LEFT"), "scrollLeft");
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed RIGHT"), "scrollRight");
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("pressed KP_RIGHT"), "scrollRight");

		getActionMap().put("startPointing", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				if (!m_bMouseOnView || m_frame.getUIMode().isActive())
				{
					// no pointing if the mouse is outside the view area, or the active tool is
					// being used.
					return;
				}

				// we're only interested in doing this if they aren't already
				// holding the space key.
				if (m_bSpaceKeyDown == false)
				{
					m_bSpaceKeyDown = true;

					showPointerAt(m_mousePositionModel);
				}
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= -1053248611112843772L;
		});

		getActionMap().put("stopPointing", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				m_bSpaceKeyDown = false;
				showPointerAt(null);
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= -8422918377090083512L;
		});

		getActionMap().put("shiftDown", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				m_bShiftKeyDown = true;
				repaint();
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= 3881440237209743033L;
		});

		getActionMap().put("shiftUp", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				m_bShiftKeyDown = false;
				repaint();
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= 4458628987043121905L;
		});

		getActionMap().put("controlDown", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				m_bControlKeyDown = true;
				repaint();
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= 7483132144245136048L;
		});

		getActionMap().put("controlUp", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				m_bControlKeyDown = false;
				repaint();
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= -3685986269044575610L;
		});

		getActionMap().put("altDown", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				m_bAltKeyDown = true;
				repaint();
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= 1008551504896354075L;
		});

		getActionMap().put("altUp", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (m_bAltKeyDown)
				{
					m_bAltKeyDown = false;
					repaint();
				}
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= -5789160422348881793L;
		});

		getActionMap().put("zoomIn", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				centerZoom(1);
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= -6378089523552259896L;
		});

		getActionMap().put("zoomOut", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				centerZoom(-1);
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= 3489902228064051594L;
		});

		getActionMap().put("scrollUp", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				if (m_scrolling)
				{
					return;
				}

				Point pos = getScrollPosition();
				final MapCoordinates p = viewToModel(pos.x, pos.y - Math.round(getHeight() * KEYBOARD_SCROLL_FACTOR));
				smoothScrollTo(p);
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= 3255081196222471923L;
		});

		getActionMap().put("scrollDown", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				if (m_scrolling)
				{
					return;
				}

				Point pos = getScrollPosition();
				final MapCoordinates p = viewToModel(pos.x, pos.y + Math.round(getHeight() * KEYBOARD_SCROLL_FACTOR));

				smoothScrollTo(p);
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= 2041156257507421225L;
		});

		getActionMap().put("scrollLeft", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				if (m_scrolling)
				{
					return;
				}

				Point pos = getScrollPosition();
				final MapCoordinates p = viewToModel(pos.x - Math.round(getWidth() * KEYBOARD_SCROLL_FACTOR), pos.y);

				smoothScrollTo(p);
			}

			/**
             * 
             */
			private static final long	serialVersionUID	= -2772860909080008403L;
		});

		getActionMap().put("scrollRight", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				if (m_scrolling)
				{
					return;
				}

				Point pos = getScrollPosition();
				final MapCoordinates p = viewToModel(pos.x + Math.round(getWidth() * KEYBOARD_SCROLL_FACTOR), pos.y);

				smoothScrollTo(p);
			}

			private static final long	serialVersionUID	= -4782758632637647018L;
		});
	}

	/**
	 * @return true if the current player is currently pointing
	 */
	private boolean isPointing()
	{
		final Player me = m_core.getPlayer();
		if (me == null)
			return false;

		return me.isPointing();
	}

	/**
	 * Convert a coordinate from model to pixels
	 * 
	 * NB: Since this one is not type safe, we'll keep it private
	 * 
	 * @param c model coordinate to convert
	 * @return pixel coordinate
	 */
	private int modelToView(int c)
	{
		final double squaresX = (double) c / (double) GameTableMap.getBaseTileSize();
		return (int) Math.round(squaresX * m_tileSize);
	}

	/**
	 * Paint the component to the specified graphics, without limiting to the component's size
	 * 
	 * @param graphics
	 * @param width
	 * @param height
	 */
	private void paintComponent(final Graphics graphics, int width, int height)
	{
		final Graphics2D g = (Graphics2D) graphics.create();
		g.addRenderingHints(Images.getRenderingHints());
		g.setFont(MAIN_FONT);

		// if they're on the private layer, we draw it first, then the private layer
		// on top of it at half alpha.
		// if they're on the priavet layer, we draw the private layer on white at half alpha,
		// then the private layer at full alpha

		if (m_core.isActiveMapPublic())
		{
			// they are on the private map. Draw the private map as normal,
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			paintMap(g, m_core.getMap(GameTableCore.MapType.PUBLIC), width, height);
		}
		else
		{
			// they're on the private map. First, draw the private map as normal.
			// Then draw a 50% alpha sheet over it. then draw the private map
			paintMap(g, m_core.getMap(GameTableCore.MapType.PUBLIC), width, height);

			g.setColor(OVERLAY_COLOR); // OVERLAY_COLOR is white with 50% alpha
			g.fillRect(0, 0, width, height);

			// now draw the private layer
			paintMap(g, m_core.getMap(GameTableCore.MapType.PRIVATE), width, height);
		}
		g.dispose();
	}

	/**
	 * Paint a map on the graphics device
	 * @param g
	 * @param mapToDraw
	 * @param width
	 * @param height
	 */
	private void paintMap(final Graphics g, final GameTableMap mapToDraw, int width, int height)
	{
		Graphics2D g2 = (Graphics2D) g;

		boolean isActiveMap = mapToDraw == m_core.getMap(GameTableCore.MapType.ACTIVE);

		Point scrollPos = getScrollPosition();

		g.translate(-scrollPos.x, -scrollPos.y);

		// we don't draw the matte if we're on the private map)
		if (mapToDraw.isPublicMap())
		{
			drawMapBackground(g, scrollPos.x, scrollPos.y, width, height);
		}

		// draw all the underlays here
		for (MapElement mapElement : mapToDraw.getMapElements())
		{
			if (mapElement.getLayer() != Layer.POG)
			{
				renderMapElement(g2, mapElement);
			}
		}

		// we don't draw the underlay being dragged if we're not
		// drawing the current map
		if (isActiveMap)
		{
			// if they're dragging an underlay, draw it here
			// there could be a pog drag in progress
			if (m_newMapElementIsBeingDragged)
			{
				MapCoordinates mousePos = getDraggedMapElementMousePosition();
				if (isPointVisible(mousePos))
				{
					final MapElement mapElement = getPogPanel().getGrabbedPog();

					if (mapElement.getLayer() != Layer.POG)
					{
						drawGhostlyToCanvas(mapElement, g);
					}
				}
			}
		}

		// Overlays
		for (MapElement mapElement : mapToDraw.getMapElements())
		{
			if (mapElement.getLayer() == Layer.OVERLAY)
			{
				renderMapElement(g2, mapElement);
			}
		}

		// we don't draw the grid if we're on the private map)
		if (mapToDraw.isPublicMap())
		{
			m_core.getGridMode().drawLines(g2, scrollPos.x, scrollPos.y, width, height, this);
		}

		// lines
		for (LineSegment ls : mapToDraw.getLines())
		{
			// LineSegments police themselves, performance wise. If they won't touch the current
			// viewport, they don't draw
			ls.drawToCanvas(g, this);
		}

		// env
		for (MapElement mapElement : mapToDraw.getMapElements())
		{
			if (mapElement.getLayer() == Layer.ENVIRONMENT)
			{
				renderMapElement(g2, mapElement);
			}
		}

		// pogs
		for (MapElement mapElement : mapToDraw.getMapElements())
		{
			if (mapElement.getLayer() == Layer.POG)
			{
				renderMapElement(g2, mapElement);
			}
		}

		// we don't draw the pog being dragged if we're not
		// drawing the current map
		if (isActiveMap)
		{
			// there could be a pog drag in progress
			if (m_newMapElementIsBeingDragged)
			{
				if (isPointVisible(getDraggedMapElementMousePosition()))
				{
					final MapElement pog = getPogPanel().getGrabbedPog();

					if (pog.getLayer() == Layer.POG)
					{
						drawGhostlyToCanvas(pog, g);
					}
				}
			}
		}

		// draw the cursor overlays
		final List<Player> players = m_core.getPlayers();

		for (Player plr : players)
		{
			if (plr.isPointing())
			{
				// draw this player's point cursor
				final Point pointingAt = modelToView(plr.getPointingLocation());

				// 5px offset to align with mouse pointer
				final int drawX = pointingAt.x;
				int drawY = pointingAt.y - 5;
				g.drawImage(m_pointingImage, drawX, drawY, null);
				final FontMetrics fm = g.getFontMetrics();
				drawY -= fm.getHeight() + 2;
				final Rectangle r = fm.getStringBounds(plr.getCharacterName(), g).getBounds();
				r.height -= fm.getLeading();
				r.width -= 1;
				final int padding = 3;
				r.grow(padding, 0);
				g.setColor(new Color(192, 192, 192, 128));
				g.fillRect(drawX - padding, drawY, r.width, r.height);
				g.setColor(Color.BLACK);
				g.drawRect(drawX - padding, drawY, r.width - 1, r.height - 1);
				g.drawString(plr.getCharacterName(), drawX, drawY + fm.getAscent() - fm.getLeading());
			}
		}

		// mousing around
		MapElement mouseOverPog = null;
		if (m_bMouseOnView || m_frame.shouldShowNames())
		{
			mouseOverPog = mapToDraw.getMapElementAt(m_mousePositionModel);
			if (m_bShiftKeyDown || m_frame.shouldShowNames())
			{
				// this shift key is down. Show all pog data
				for (MapElement pog : mapToDraw.getMapElements())
				{
					if (pog != mouseOverPog)
					{
						pog.getRenderer().drawInformationOverlayToCanvas(g, false, this);
					}
				}
			}

			if (mouseOverPog != null)
			{
				mouseOverPog.getRenderer().drawInformationOverlayToCanvas(g, true, this);
			}
		}

		if (isActiveMap)
		{
			UIMode mode = m_frame.getUIMode();
			if (mode != null)
				mode.paintTool(g2, this);
		}

		g.translate(scrollPos.x, scrollPos.y);
	}

	/**
	 * Render a given map element
	 * @param g graphics device
	 * @param mapElement Map element to render
	 */
	private void renderMapElement(Graphics2D g, MapElement mapElement)
	{
		Composite oldComposite = g.getComposite();

		if (m_frame.isSelected(mapElement, MapType.ACTIVE))
		{
			g.setComposite(UtilityFunctions.getSelectedComposite());
		}
		else if (m_frame.isHighlighted(mapElement))
		{
			g.setComposite(UtilityFunctions.getHilightedComposite());
		}

		try
		{
			mapElement.getRenderer().drawToCanvas(g, this);
		}
		finally
		{
			g.setComposite(oldComposite);
		}
	}

	/**
	 * Sets the pointer location for a given player in map coordinates
	 * 
	 * @param pointLocation
	 */
	private void showPointerAt(final MapCoordinates pointLocation)
	{
		final Player me = m_core.getPlayer();
		if (me != null)
		{
			if (pointLocation == null)
			{
				me.setPointing(false, null);
				return;
			}

			me.setPointing(true, pointLocation);
		}
	}

	/**
	 * Start the smooth scroll process
	 * @param pos
	 */
	private void smoothScrollTo(MapCoordinates pos)
	{
		m_startScroll = viewToModel(getScrollPosition());
		m_scrollDelta = new MapCoordinates(pos.x - m_startScroll.x, pos.y - m_startScroll.y);
		m_scrollTime = 0;
		m_scrollTimeTotal = SMOOTH_SCROLL_TIME;
		m_scrolling = true;
	}

	/**
	 * Snap a map element to the grid
	 * @param mapElement
	 */
	private void snapMapElementToGrid(final MapElement mapElement)
	{
		m_core.getGridMode().snapMapElementToGrid(mapElement);
	}

	/**
	 * Part of the drag & drop process
	 */
	private void updateMapElementDropLocation()
	{
		final PogPanel panel = getPogPanel();
		final Point screenMousePoint = panel.getGrabPosition();
		final Point pogGrabOffset = panel.getGrabOffset();

		// convert to our coordinates
		final Point canvasView = UtilityFunctions.getComponentCoordinates(this, screenMousePoint);

		// now convert to model coordinates
		final MapCoordinates canvasModel = viewToModel(canvasView);
		final MapElement grabbedPog = panel.getGrabbedPog();

		// now, snap to grid if they don't have the control key down
		if (!m_bControlKeyDown)
		{
			// Removed the adjustment part, because it was actually making the dragging worse
			// final Point adjustment = grabbedPog.getSnapDragAdjustment();
			// grabbedPog.setPosition(canvasModel.x - pogGrabOffset.x + adjustment.x, canvasModel.y - pogGrabOffset.y
			// + adjustment.y);
			grabbedPog.setPosition(new MapCoordinates(canvasModel.x - pogGrabOffset.x, canvasModel.y - pogGrabOffset.y));
			snapMapElementToGrid(grabbedPog);
		}
		else
		{
			grabbedPog.setPosition(new MapCoordinates(canvasModel.x - pogGrabOffset.x, canvasModel.y - pogGrabOffset.y));
		}
	}

	/**
	 * Recalculate m_squareSize
	 */
	private void updateTileSize()
	{
		int ret = GameTableMap.getBaseTileSize();
		switch (m_zoom)
		{
		case 0:
			ret = GameTableMap.getBaseTileSize();
			break;

		case 1:
			ret = (GameTableMap.getBaseTileSize() / 4) * 3;
			break;

		case 2:
			ret = GameTableMap.getBaseTileSize() / 2;
			break;

		case 3:
			ret = GameTableMap.getBaseTileSize() / 4;
			break;

		case 4:
			ret = GameTableMap.getBaseTileSize() / 8;
			break;
		}

		m_tileSize = ret;
	}

	/**
	 * Proportion of the visible map to scroll by when scrolling using keyboard
	 * (ex: 0.5f is half the visible portion of the map)
	 */
	private static final float		KEYBOARD_SCROLL_FACTOR	= 0.5f;

	/**
	 * Main font 
	 * TODO #Properties
	 */
	private static final Font			MAIN_FONT								= Font.decode("sans-12");

	/**
	 * This is the color used to overlay on top of the private layer when the user is on the private layer. It's white
	 * with 50% alpha
	 */
	private static final Color		OVERLAY_COLOR						= new Color(255, 255, 255, 128);

	/**
	 * Serialize version
	 */
	private static final long			serialVersionUID				= 6250860728974514790L;

	/**
	 * Smooth scroll duration
	 */
	private static final int			SMOOTH_SCROLL_TIME		= 300;

	/**
	 * ALT key is down
	 */
	private boolean								m_bAltKeyDown;

	/**
	 * CTRL key is down
	 */
	private boolean								m_bControlKeyDown;

	/**
	 * true when mouse is over the map
	 */
	private boolean								m_bMouseOnView;

	/**
	 * SHIFT key is down
	 */
	private boolean								m_bShiftKeyDown;

	/**
	 * SPACE key is down
	 */
	private boolean								m_bSpaceKeyDown;

	/**
	 * Reference to the engine's core 
	 */
	private final GameTableCore		m_core;

	/**
	 * Reference to the frame
	 */
	private final GametableFrame	m_frame;

	/**
	 * Image to use as background for the map
	 */
	private Image									m_mapBackground;

	/**
	 * The mode we switched out of to go to hand tool for a right-click
	 */
	private UIMode								m_modePrevious;

	/**
	 * Current mouse location in model coordinates
	 */
	private MapCoordinates				m_mousePositionModel;

	/**
	 * TODO #PogPanel Move?
	 */
	private boolean								m_newMapElementIsBeingDragged;

	/**
	 * "Pointing" icon
	 */
	private Image									m_pointingImage;

	/**
	 * True if the current mouse action was initiated with a right-click
	 */
	private boolean								m_rightClicking;

	/**
	 * Distance to travel in a smooth scroll operation
	 */
	private MapCoordinates				m_scrollDelta;

	/**
	 * Flag noting if a smooth scroll animation is in progress
	 */
	private boolean								m_scrolling;

	/**
	 * Current scroll coordinates, relative to scroll origin for a smooth scroll animation
	 */
	private Point									m_scrollPosition				= new Point(0, 0);

	/**
	 * Current time marker of a smooth scroll animation
	 */
	private long									m_scrollTime;

	/**
	 * Duration of a smooth scroll animation
	 */
	private long									m_scrollTimeTotal;

	/**
	 * Start position of a 'smooth scroll' animation
	 */
	private MapCoordinates				m_startScroll;

	/**
	 * The size of a tile at the current zoom level
	 */
	private int										m_tileSize						= 0;

	/**
	 * This is the number of screen pixels that are used per model 'pixel'. It's never less than 1
	 */
	private int										m_zoom									= 1;

	/**
	 * TODO Extract to top level class in UI
	 */
	public static enum BackgroundColor
	{
		BLACK("Black"), BLUE("Blue"), BROWN("Brown"), DARK_BLUE("Dark Blue"), DARK_GREEN("Dark Green"), DARK_GREY("Dark Grey"), DEFAULT("Default"), GREEN(
				"Green"), GREY("Grey"), WHITE("White");

		/**
		 * Get BackgroundColor object from ordinal value
		 * 
		 * @param ordinal
		 * @return
		 */
		public static BackgroundColor fromOrdinal(int ordinal)
		{
			for (BackgroundColor val : values())
				if (val.ordinal() == ordinal)
					return val;

			return null;
		}

		/**
		 * Private constructor
		 * 
		 * @param text
		 */
		private BackgroundColor(String text)
		{
			m_text = text;
		}

		/**
		 * Get a text representation for the color
		 * 
		 * @return string
		 */
		public String getText()
		{
			return m_text;
		}

		private final String	m_text;
	}

	// grid modes
	// TODO Extract to top class, move to data
	public static enum GridModeID
	{
		HEX, NONE, SQUARES;

		/**
		 * Convert from ordinal to enum
		 * 
		 * @param value
		 * @return
		 */
		public static GridModeID fromOrdinal(int value)
		{
			for (GridModeID gid : GridModeID.values())
			{
				if (gid.ordinal() == value)
					return gid;
			}

			return NONE;
		}
	}

	private class CanvasMapElementListener extends MapElementAdapter
	{
		/*
		 * @see com.gametable.data.MapElementAdapter#onAngleChanged(com.gametable.data.MapElement,
		 * com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAngleChanged(MapElement element, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see com.gametable.data.MapElementAdapter#onAttributeChanged(com.gametable.data.MapElement,
		 * java.lang.String, java.lang.String, java.lang.String, boolean, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
				repaint();
		}

		/*
		 * @see
		 * com.gametable.data.MapElementAdapter#onAttributesChanged(com.gametable.data.MapElement,
		 * java.util.Map, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAttributesChanged(MapElement element, Map<String, String> attributes, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see
		 * com.gametable.data.MapElementAdapter#onElementTypeChanged(com.gametable.data.MapElement,
		 * com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onElementTypeChanged(MapElement element, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see com.gametable.data.MapElementAdapter#onFaceSizeChanged(com.gametable.data.MapElement,
		 * com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onFaceSizeChanged(MapElement element, NetworkEvent netEvent)
		{
			snapMapElementToGrid(element);
			repaint();
		}

		/*
		 * @see com.gametable.data.MapElementAdapter#onFlipChanged(com.gametable.data.MapElement,
		 * com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onFlipChanged(MapElement element, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see com.gametable.data.MapElementAdapter#onLayerChanged(com.gametable.data.MapElement,
		 * com.gametable.data.MapElementTypeIF.Layer, com.gametable.data.MapElementTypeIF.Layer,
		 * com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onLayerChanged(MapElement element, Layer newLayer, Layer oldLayer, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see com.gametable.data.MapElementAdapter#onNameChanged(com.gametable.data.MapElement,
		 * java.lang.String, java.lang.String, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onNameChanged(MapElement element, String newName, String oldName, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see com.gametable.data.MapElementAdapter#onPositionChanged(com.gametable.data.MapElement,
		 * com.gametable.data.MapCoordinates, com.gametable.data.MapCoordinates,
		 * com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onPositionChanged(MapElement element, MapCoordinates newPosition, MapCoordinates oldPosition, NetworkEvent netEvent)
		{
			repaint();
		}
	}

	private class CanvasMapListener extends GameTableMapAdapter
	{
		/*
		 * @see
		 * com.gametable.data.GameTableMapAdapter#onClearLineSegments(com.gametable.data.GameTableMap,
		 * com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onClearLineSegments(GameTableMap map, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see
		 * com.gametable.data.GameTableMapAdapter#onLineSegmentsCropped(com.gametable.data.GameTableMap
		 * , com.gametable.data.MapRectangle, boolean, int, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onEraseLineSegments(GameTableMap map, MapRectangle rect, boolean colorSpecific, int color, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see
		 * com.gametable.data.GameTableMapAdapter#onLineSegmentAdded(com.gametable.data.GameTableMap,
		 * com.gametable.data.LineSegment, boolean)
		 */
		@Override
		public void onLineSegmentAdded(GameTableMap map, LineSegment lineSegment, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
				repaint();
		}

		/*
		 * @see
		 * com.gametable.data.GameTableMapAdapter#onLineSegmentsAdded(com.gametable.data.GameTableMap,
		 * java.util.List)
		 */
		@Override
		public void onLineSegmentsAdded(GameTableMap map, List<LineSegment> lineSegments, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @seecom.gametable.data.GameTableMapAdapter#onMapElementInstanceAdded(com.gametable.data.
		 * GameTableMap, com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onMapElementAdded(GameTableMap map, MapElement mapElement, NetworkEvent netEvent)
		{
			repaint();
		}

		/*
		 * @see com.gametable.data.GameTableMapAdapter#onMapElementInstanceRemoved(com.gametable.data
		 * .GameTableMap, com.gametable.data.MapElement, boolean)
		 */
		@Override
		public void onMapElementRemoved(GameTableMap map, MapElement mapElement, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				m_frame.selectMapElementInstance(mapElement, MapType.ACTIVE, false);
				m_frame.highlightMapElementInstance(mapElement, false);

				repaint();
			}
		}

		/*
		 * @see com.gametable.data.GameTableMapAdapter#onMapElementInstancesCleared(com.gametable.data
		 * .GameTableMap)
		 */
		@Override
		public void onMapElementsCleared(GameTableMap map, NetworkEvent netEvent)
		{
			m_frame.unselectAllMapElementInstances(MapType.ACTIVE);
			m_frame.highlightAllMapElementInstances(false);

			repaint();
		}

		/*
		 * @see
		 * com.gametable.data.GameTableMapAdapter#onMapElementInstancesRemoved(com.gametable.data.
		 * GameTableMap, java.util.List, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onMapElementsRemoved(GameTableMap map, List<MapElement> mapElements, NetworkEvent netEvent)
		{
			for (MapElement mapElement : mapElements)
			{
				m_frame.selectMapElementInstance(mapElement, MapType.ACTIVE, false);
				m_frame.highlightMapElementInstance(mapElement, false);
			}

			repaint();
		}
	}
}
