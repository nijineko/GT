/*
 * java: GameTable is in the Public Domain.
 */

package com.galactanet.gametable.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.data.grid.HexGridMode;
import com.galactanet.gametable.data.grid.SquareGridMode;
import com.galactanet.gametable.net.NetworkEvent;
import com.galactanet.gametable.net.NetworkStatus;
import com.galactanet.gametable.ui.net.*;
import com.galactanet.gametable.ui.tools.NullTool;
import com.galactanet.gametable.util.ImageCache;
import com.galactanet.gametable.util.Images;
import com.galactanet.gametable.util.UtilityFunctions;
import com.plugins.network.PacketSourceState;

/**
 * The main map view of Gametable.
 * 
 * #GT-AUDIT GametableCanvas
 */
public class GametableCanvas extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener
{
	// grid modes
	public static enum GridModeID
	{
		NONE, SQUARES, HEX;

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

	public final static int			NUM_ZOOM_LEVELS					= 5;

	private static final float	KEYBOARD_SCROLL_FACTOR	= 0.5f;
	private static final int		KEYBOARD_SCROLL_TIME		= 300;

	private static final Font		MAIN_FONT								= Font.decode("sans-12");
	/**
	 * A singleton instance of the NULL tool.
	 */
	private static final ToolIF	NULL_TOOL								= new NullTool();

	/**
	 * This is the color used to overlay on top of the public layer when the user is on the private layer. It's white with
	 * 50% alpha
	 */
	private static final Color	OVERLAY_COLOR						= new Color(255, 255, 255, 128);

	/**
     *
     */
	private static final long		serialVersionUID				= 6250860728974514790L;

	private Image								m_mapBackground;

	// this is the map (or layer) that all players share
	private final GameTableMap	m_publicMap;
	// this is the map (or layer) that is private to a specific player
	private final GameTableMap	m_privateMap;
	// this points to whichever map is presently active
	private GameTableMap				m_activeMap;

	private int									m_activeToolId					= -1;

	private boolean							m_bAltKeyDown;
	private boolean							m_bControlKeyDown;
	private boolean							m_bMouseOnView;
	private boolean							m_bShiftKeyDown;

	// misc flags
	private boolean							m_bSpaceKeyDown;
	private MapCoordinates			m_deltaScroll;
	// some cursors
	private Cursor							m_emptyCursor;
	// the frame
	private GametableFrame			m_frame;

	GridMode										m_gridMode;
	SquareGridMode							m_squareGridMode				= new SquareGridMode();
	HexGridMode									m_hexGridMode						= new HexGridMode();
	GridMode										m_noGridMode						= new GridMode();

	private MapCoordinates			m_mouseModelFloat;

	private boolean							m_newPogIsBeingDragged;
	private MapElement					m_pogMouseOver;
	private Image								m_pointingImage;
	/**
	 * the id of the tool that we switched out of to go to hand tool for a right-click
	 */
	private int									m_previousToolId;

	/**
	 * true if the current mouse action was initiated with a right-click
	 */
	private boolean							m_rightClicking;
	private MapCoordinates			m_startScroll;
	private boolean							m_scrolling;
	private long								m_scrollTime;
	private long								m_scrollTimeTotal;

	private SelectionHandler		m_selectionPublic;
	private SelectionHandler		m_selectionPrivate;
	private SelectionHandler		m_highlightedElements;	

	/**
	 * This is the number of screen pixels that are used per model pixel. It's never less than 1
	 */
	private int									m_zoom									= 1;

	// the size of a square at the current zoom level
	private int									m_squareSize						= 0;

	/**
	 * Constructor.
	 */
	public GametableCanvas()
	{
		// @revise It would make the interface lighter if we proposed 'getHandler' methods instead of exposing 8+ member for
		// every selection handler. Con: extra level of indirection.
		
		m_publicMap = new GameTableMap(true);
		m_privateMap = new GameTableMap(false);

		m_selectionPublic = new SelectionHandler();
		m_selectionPrivate = new SelectionHandler();
		m_highlightedElements = new SelectionHandler();

		setFocusable(true);
		setRequestFocusEnabled(true);

		addMouseListener(this);
		addMouseMotionListener(this);
		addFocusListener(new FocusListener() {
			/*
			 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
			 */
			public void focusGained(final FocusEvent e)
			{
				final JPanel panel = (JPanel) getParent();
				panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), LineBorder.createBlackLineBorder()));
			}

			/*
			 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
			 */
			public void focusLost(final FocusEvent e)
			{
				final JPanel panel = (JPanel) getParent();
				panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(1, 1, 1, 1)));
			}

		});

		initializeKeys();

		m_activeMap = m_publicMap;

		updateSquareSize();

		GameTableMapListenerIF mapListener = new CanvasMapListener();
		
		m_publicMap.addListener(mapListener);
		m_privateMap.addListener(mapListener);
		
		m_publicMap.addMapElementListener(new CanvasMapElementListener(true));
		m_privateMap.addMapElementListener(new CanvasMapElementListener(false));
	}
	
	/**
	 * Verifies whether changes to the data should be propagated over the network
	 * 
	 * @param netEvent Network event that might have triggered the changes
	 * @return True to propagate, false otherwise.
	 */
	private boolean shouldPropagateChanges(NetworkEvent netEvent)
	{
		return m_frame.shouldPropagateChanges(netEvent);
	}

	/**
	 * Initializes all the keys for the canvas.
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
			/**
             * 
             */
			private static final long	serialVersionUID	= -1053248611112843772L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				if (!m_bMouseOnView || getActiveTool().isBeingUsed())
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

					showPointerAt(m_mouseModelFloat);
				}
			}
		});

		getActionMap().put("stopPointing", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= -8422918377090083512L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				m_bSpaceKeyDown = false;
				showPointerAt(null);
			}
		});

		getActionMap().put("shiftDown", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= 3881440237209743033L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				m_bShiftKeyDown = true;
				repaint();
			}
		});

		getActionMap().put("shiftUp", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= 4458628987043121905L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				m_bShiftKeyDown = false;
				repaint();
			}
		});

		getActionMap().put("controlDown", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= 7483132144245136048L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				m_bControlKeyDown = true;
				repaint();
			}
		});

		getActionMap().put("controlUp", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= -3685986269044575610L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				m_bControlKeyDown = false;
				repaint();
			}
		});

		getActionMap().put("altDown", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= 1008551504896354075L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				m_bAltKeyDown = true;
				repaint();
			}
		});

		getActionMap().put("altUp", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= -5789160422348881793L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (m_bAltKeyDown)
				{
					m_bAltKeyDown = false;
					repaint();
				}
			}
		});

		getActionMap().put("zoomIn", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= -6378089523552259896L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				centerZoom(1);
			}
		});

		getActionMap().put("zoomOut", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= 3489902228064051594L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (isTextFieldFocused())
				{
					return;
				}

				centerZoom(-1);
			}
		});

		getActionMap().put("scrollUp", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= 3255081196222471923L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
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
				final MapCoordinates p = drawToModel(pos.x, pos.y - Math.round(getHeight() * KEYBOARD_SCROLL_FACTOR));
				smoothScrollTo(p);
			}
		});

		getActionMap().put("scrollDown", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= 2041156257507421225L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
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
				final MapCoordinates p = drawToModel(pos.x, pos.y + Math.round(getHeight() * KEYBOARD_SCROLL_FACTOR));

				smoothScrollTo(p);
			}
		});

		getActionMap().put("scrollLeft", new AbstractAction() {
			/**
             * 
             */
			private static final long	serialVersionUID	= -2772860909080008403L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
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
				final MapCoordinates p = drawToModel(pos.x - Math.round(getWidth() * KEYBOARD_SCROLL_FACTOR), pos.y);

				smoothScrollTo(p);
			}
		});

		getActionMap().put("scrollRight", new AbstractAction() {
			/**
             *
             */
			private static final long	serialVersionUID	= -4782758632637647018L;

			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
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
				final MapCoordinates p = drawToModel(pos.x + Math.round(getWidth() * KEYBOARD_SCROLL_FACTOR), pos.y);

				smoothScrollTo(p);
			}
		});
	}

	public void addCardPog(final MapElement toAdd)
	{
		m_privateMap.addMapElement(toAdd);
		// m_gametableFrame.refreshActivePogList();
		repaint();
	}

	public void centerZoom(final int delta)
	{
		// can't do this at all if we're dragging
		if (m_newPogIsBeingDragged)
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
		setPrimaryScroll(getActiveMap(), scrX, scrY);
	}

	/**
	 * Do the actual centering
	 * @param modelCenter
	 * @param zoomLevel
	 */
	public void centerView(MapCoordinates modelCenter, final int zoomLevel)
	{
		// if you recenter for any reason, your tool action is cancelled
		m_frame.getToolManager().cancelToolAction();

		// make the sent in x and y our center, ad the sent in zoom.
		// So start with the zoom
		setZoomLevel(zoomLevel);

		final Point viewCenter = modelToView(modelCenter);

		// find where the top left would have to be, based on our size
		final int tlX = viewCenter.x - getWidth() / 2;
		final int tlY = viewCenter.y - getHeight() / 2;

		// that is our new scroll position
		final MapCoordinates newModelPoint = viewToModel(tlX, tlY);
		if (PacketSourceState.isHostDumping())
		{
			scrollMapTo(newModelPoint);
		}
		else
		{
			smoothScrollTo(newModelPoint);
		}
	}

	/**
	 * Set map element layer
	 * 
	 * @param mapElementID ID of the map element for which to set the layer
	 * @param layer 
	 * 
	 *          TODO ! Set listener on canvas to repaint when pog attributes trigger listeners (ex: layers)
	 */
	private void doSetPogLayer(final MapElementID mapElementID, final Layer layer)
	{
		final MapElement mapElement = getActiveMap().getMapElement(mapElementID);
		if (mapElement != null)
			mapElement.setLayer(layer);
	}

	public void doSetPogSize(final MapElementID id, final float size)
	{
		final MapElement pog = getActiveMap().getMapElement(id);
		if (pog == null)
			return;

		pog.setFaceSize(size);
		snapPogToGrid(pog);
		repaint();
	}

	private void doSetPogType(final MapElementID id, final MapElementID type)
	{
		final MapElement pog = getActiveMap().getMapElement(id);
		final MapElement tpog = getActiveMap().getMapElement(type);
		if ((pog == null) || (tpog == null))
		{
			return;
		}

		pog.setElementType(tpog.getMapElementType());
		repaint();
	}

	// topLeftX and topLeftY are the coordinates of where the
	// top left of the map area is in whatever coordinate system g is set up to be
	public void drawMatte(final Graphics g, final int topLeftX, final int topLeftY, final int width, final int height)
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

	public MapCoordinates drawToModel(final int modelX, final int modelY)
	{
		return drawToModel(new Point(modelX, modelY));
	}

	public MapCoordinates drawToModel(final Point drawPoint)
	{
		final double squaresX = (double) (drawPoint.x) / (double) m_squareSize;
		final double squaresY = (double) (drawPoint.y) / (double) m_squareSize;

		final int modelX = (int) (squaresX * GameTableMap.getBaseSquareSize());
		final int modelY = (int) (squaresY * GameTableMap.getBaseSquareSize());

		return new MapCoordinates(modelX, modelY);
	}

	public GameTableMap getActiveMap()
	{
		// if we're processing a packet, we want it to go to the
		// public layer, even if they're presently on the private layer.
		// HOWEVER, if we're in the process of opening a file, then that
		// trumps net packet processing, and we want to return whatever
		// map they're on.

		if (PacketSourceState.isFileLoading())
		{
			return m_activeMap;
		}
		if (PacketSourceState.isNetPacketProcessing())
		{
			return m_publicMap;
		}
		return m_activeMap;
	}

	public ToolIF getActiveTool()
	{
		if (m_activeToolId < 0)
		{
			return NULL_TOOL;
		}
		return m_frame.getToolManager().getToolInfo(m_activeToolId).getTool();
	}

	/**
	 * 
	 * @return
	 */
	public int getActiveToolId()
	{
		return m_activeToolId;
	}

	public GridMode getGridMode()
	{
		return m_gridMode;
	}

	public GridModeID getGridModeId()
	{
		if (m_gridMode == m_squareGridMode)
		{
			return GridModeID.SQUARES;
		}
		if (m_gridMode == m_hexGridMode)
		{
			return GridModeID.HEX;
		}
		return GridModeID.NONE;
	}

	// returns a good line width to draw things
	public int getLineStrokeWidth()
	{
		switch (m_zoom)
		{
		case 0:
			{
				return 3;
			}

		case 1:
			{
				return 2;
			}

		case 2:
			{
				return 2;
			}

		case 3:
			{
				return 1;
			}

		default:
			{
				return 1;
			}
		}
	}

	public int getModifierFlags()
	{
		return ((m_bControlKeyDown ? ToolIF.MODIFIER_CTRL : 0) | (m_bSpaceKeyDown ? ToolIF.MODIFIER_SPACE : 0)
				| (m_bShiftKeyDown ? ToolIF.MODIFIER_SHIFT : 0) | (m_bShiftKeyDown ? ToolIF.MODIFIER_ALT : 0));
	}

	private MapCoordinates getPogDragMousePosition()
	{
		final Point screenMousePoint = getPogPanel().getGrabPosition();
		final Point canvasView = UtilityFunctions.getComponentCoordinates(this, screenMousePoint);

		return viewToModel(canvasView);
	}

	private PogPanel getPogPanel()
	{
		return m_frame.getPogPanel();
	}

	public MapRectangle getVisibleCanvasRect(final int level)
	{
		final MapCoordinates topLeft = viewToModel(0, 0);

		int canvasW = 0;
		int canvasH = 0;

		switch (level)
		{
		case 0:
			{
				canvasW = getWidth();
				canvasH = getHeight();
			}
			break;

		case 1:
			{
				canvasW = (getWidth() * 4) / 3;
				canvasH = (getHeight() * 4) / 3;
			}
			break;

		case 2:
			{
				canvasW = getWidth() * 2;
				canvasH = getHeight() * 2;
			}
			break;

		case 3:
			{
				canvasW = getWidth() * 4;
				canvasH = getHeight() * 4;
			}
			break;

		case 4:
			{
				canvasW = getWidth() * 8;
				canvasH = getHeight() * 8;
			}
			break;
		}

		// final Point bottomRight = m_canvas.viewToModel(bottomRightX, bottomRightY);
		final MapRectangle visbleCanvas = new MapRectangle(topLeft, canvasW, canvasH);

		// System.out.println(topLeft.x + " " + topLeft.y);
		// System.out.println(bottomRight.x + " " + bottomRight.y);

		return visbleCanvas;
	}

	/**
	 * Get actual square size based on current zoom level
	 * 
	 * @return Size of a square, in pixels
	 */
	public int getSquareSize()
	{
		return m_squareSize;
	}

	/**
	 * Recalculate m_squareSize
	 */
	private void updateSquareSize()
	{
		int ret = GameTableMap.getBaseSquareSize();
		switch (m_zoom)
		{
		case 0:
			{
				ret = GameTableMap.getBaseSquareSize();
			}
			break;

		case 1:
			{
				ret = (GameTableMap.getBaseSquareSize() / 4) * 3;
			}
			break;

		case 2:
			{
				ret = GameTableMap.getBaseSquareSize() / 2;
			}
			break;

		case 3:
			{
				ret = GameTableMap.getBaseSquareSize() / 4;
			}
			break;

		case 4:
			{
				ret = GameTableMap.getBaseSquareSize() / 8;
			}
			break;
		}

		m_squareSize = ret;
	}

	public GameTableMap getPrivateMap()
	{
		return m_privateMap;
	}

	public GameTableMap getPublicMap()
	{
		return m_publicMap;
	}

	public void init(final GametableFrame frame)
	{
		m_frame = frame;
		m_mapBackground = ImageCache.getImage(new File("assets/mapbk.png"));

		m_pointingImage = ImageCache.getImage(new File("assets/whiteHand.png"));

		// setPrimaryScroll(m_publicMap, 0, 0);

		// set up the grid modes
		m_squareGridMode.init(this);
		m_hexGridMode.init(this);
		m_noGridMode.init(this);
		m_gridMode = m_squareGridMode;

		addMouseWheelListener(this);
		// setZoom(0);
		setActiveTool(0);
	}

	public enum BackgroundColor
	{
		DEFAULT("Default"), GREEN("Green"), DARK_GREY("Dark Grey"), GREY("Grey"), BLUE("Blue"), BLACK("Black"), WHITE("White"), DARK_BLUE("Dark Blue"), DARK_GREEN(
				"Dark Green"), BROWN("Brown");

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

		private final String	m_text;
	}
	
	/**
	 * @return The current background color or null if background is currently a map element
	 */
	public BackgroundColor getBackgroundColor()
	{
		return m_backgroundTypeMapElement ? null : m_backgroundColor;
	}
	
	/**
	 * @return The current background color or null if background is currently a specified color
	 */
	public MapElementTypeIF getBackgroundMapElementType()
	{
		return m_backgroundTypeMapElement ? m_backgroundElementType : null;
	}

	private BackgroundColor		m_backgroundColor						= BackgroundColor.DEFAULT;
	private MapElementTypeIF	m_backgroundElementType			= null;
	private boolean						m_backgroundTypeMapElement	= false;
	
	/**
	 * Current scroll coordinates, relative to scroll origin
	 */
	private Point						m_scrollPos									= new Point(0, 0);

	/**
	 * Set the color of the background
	 * @param color
	 */
	private void changeBackground(final BackgroundColor color)
	{
		Image newBk = null;

		// @revise all 'assets/' images should be within a jar.

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
		{
			m_mapBackground = newBk;
			m_backgroundColor = color;
			m_backgroundTypeMapElement = false;
			repaint();
		}
	}

	/**
	 * Set the background's tile
	 * @param type
	 */
	private void changeBackground(MapElementTypeIF type)
	{
		m_backgroundElementType = type;
		m_mapBackground = type.getImage();
		m_backgroundTypeMapElement = true;
	}

	/**
	 * Change the canvas' background
	 * @param color
	 * @param netEvent
	 */
	protected void changeBackground(BackgroundColor color, NetworkEvent netEvent)
	{
		if (shouldPropagateChanges(netEvent))
		{
			m_frame.sendBroadcast(NetSetBackground.makePacket(color));
		}

		if (netEvent != null)
		{
			Player player = netEvent.getSendingPlayer();
			m_frame.sendSystemMessageLocal(player.getPlayerName() + " has changed the background.");
		}
		
		changeBackground(color);
	}

	/**
	 * Change the canvas' background
	 * @param type
	 * @param netEvent
	 */
	protected void changeBackground(MapElementTypeIF type, NetworkEvent netEvent)
	{
		if (shouldPropagateChanges(netEvent))
		{
			m_frame.sendBroadcast(NetSetBackground.makePacket(type));
		}
		
		if (netEvent != null)
		{
			Player player = netEvent.getSendingPlayer();
			m_frame.sendSystemMessageLocal(player.getPlayerName() + " has changed the background.");
		}
		
		changeBackground(type);
	}

	private boolean isPointing()
	{
		final Player me = m_frame.getMyPlayer();
		return me.isPointing();
	}

	public boolean isPointVisible(final MapCoordinates modelPoint)
	{
		final MapCoordinates portalTL = viewToModel(0, 0);
		final MapCoordinates portalBR = viewToModel(getWidth(), getHeight());
		if (modelPoint.x > portalBR.x)
		{
			return false;
		}

		if (modelPoint.y > portalBR.y)
		{
			return false;
		}

		if (modelPoint.x < portalTL.x)
		{
			return false;
		}

		if (modelPoint.y < portalTL.y)
		{
			return false;
		}
		return true;
	}

	public boolean isPublicMap()
	{
		return (getActiveMap() == m_publicMap);
	}

	/**
	 * @return
	 */
	public boolean isTextFieldFocused()
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
	 * Convert coordinates from map coordinates to Graphics device coordinates
	 * 
	 * @param modelPoint
	 * @return
	 */

	public Point modelToDraw(final MapCoordinates modelPoint)
	{
		return new Point(modelToDraw(modelPoint.x), modelToDraw(modelPoint.y));
	}

	/**
	 * Convert a coordinate from model to pixels
	 * 
	 * @param c model coordinate to convert
	 * @return pixel coordinate
	 */
	private int modelToDraw(int c)
	{
		final double squaresX = (double) c / (double) GameTableMap.getBaseSquareSize();
		return (int) Math.round(squaresX * m_squareSize);
	}

	/**
	 * Convert coordinates from map coordinates to Graphics device coordinates
	 * 
	 * @param modelPoint
	 * @return
	 */
	public Rectangle modelToDraw(MapRectangle modelRect)
	{
		Point topLeft = modelToDraw(modelRect.topLeft);
		Point bottomRight = new Point(modelToDraw(modelRect.topLeft.x + modelRect.width), modelToDraw(modelRect.topLeft.y + modelRect.height));
		return new Rectangle(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
	}

	/*
	 * Modified to accomodate grid distance factor
	 */
	public double modelToSquares(final double m)
	{
		return (m_frame.grid_multiplier * m / GameTableMap.getBaseSquareSize());
	}

	public Point modelToView(final MapCoordinates modelPoint)
	{
		final double squaresX = (double) modelPoint.x / (double) GameTableMap.getBaseSquareSize();
		final double squaresY = (double) modelPoint.y / (double) GameTableMap.getBaseSquareSize();

		int viewX = (int) Math.round(squaresX * m_squareSize);
		int viewY = (int) Math.round(squaresY * m_squareSize);

		Point pos = getScrollPosition();

		viewX -= pos.x;
		viewY -= pos.y;

		return new Point(viewX, viewY);
	}

	public void mouseClicked(final MouseEvent e)
	{
		// Ignore this because java has sucky mouse clicking
	}

	/** *********************************************************** */
	// MouseListener/MouseMotionListener overrides:
	/** *********************************************************** */
	public void mouseDragged(final MouseEvent e)
	{
		// We handle dragging ourselves - don't tread on me, Java!
		mouseMoved(e);
	}

	public void mouseEntered(final MouseEvent e)
	{
		m_bMouseOnView = true;
	}

	public void mouseExited(final MouseEvent e)
	{
		m_bMouseOnView = false;
	}

	public void mouseMoved(final MouseEvent e)
	{
		m_mouseModelFloat = viewToModel(e.getX(), e.getY());
		if (isPointing())
		{
			return;
		}
		m_frame.getToolManager().mouseMoved(m_mouseModelFloat, getModifierFlags());
		final MapElement prevPog = m_pogMouseOver;
		if (prevPog != m_pogMouseOver)
		{
			repaint();
		}
	}

	public void mousePressed(final MouseEvent e)
	{
		requestFocus();
		m_mouseModelFloat = viewToModel(e.getX(), e.getY());
		if (isPointing())
		{
			return;
		}

		// this code deals with making a right click automatically be the hand tool
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			m_rightClicking = true;
			m_previousToolId = m_activeToolId;
			setActiveTool(1); // HACK -- To hand tool
			m_frame.getToolManager().mouseButtonPressed(m_mouseModelFloat, getModifierFlags());
		}
		else
		{
			m_rightClicking = false;
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				m_frame.getToolManager().mouseButtonPressed(m_mouseModelFloat, getModifierFlags());
			}
		}

	}

	public void mouseReleased(final MouseEvent e)
	{
		m_mouseModelFloat = viewToModel(e.getX(), e.getY());
		if (isPointing())
		{
			return;
		}
		m_frame.getToolManager().mouseButtonReleased(m_mouseModelFloat, getModifierFlags());

		if (m_rightClicking)
		{
			// return to arrow too
			setActiveTool(m_previousToolId);
			m_rightClicking = false;
		}
	}

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
	 * Move the specified map element and all linked map elements
	 * @param mapElementID ID of an element to move.  The UI will move all linked elements as well (grouped or selected)
	 * @param position Destination
	 */
	public void moveLinkedMapElements(final MapElementID mapElementID, MapCoordinates position)
	{
		// TODO #MoveLinkedMapElements  Actually, group implementation should do this.  Selection implementation as well.  
		// But we don't want them to double-move for a grouped + selected combo and would like an open arch to allow other grouping components
		// later on.

		final MapElement movingMapElement = getActiveMap().getMapElement(mapElementID);
		int diffx = position.x - movingMapElement.getPosition().x;
		int diffy = position.y - movingMapElement.getPosition().y;

		Group group = getActiveMap().getGroupManager().getGroup(movingMapElement);

		if (isSelected(movingMapElement))
		{
			// converted to array to prevent concurrent modification issues
			for (MapElement mapElement : getSelectedMapElementInstances().toArray(new MapElement[0]))
			{
				if (mapElement.getID() != mapElementID)
				{
					MapCoordinates newPos = mapElement.getPosition().delta(diffx, diffy);
					mapElement.setPosition(newPos);
				}
			}
		}
		else if (group != null)
		{
			List<MapElement> mapElements = group.getMapElements();

			for (MapElement mapElement : mapElements)
			{
				if (mapElement != movingMapElement)
				{
						MapCoordinates newPos = mapElement.getPosition().delta(diffx, diffy);
						mapElement.setPosition(newPos);
				}
			}
		}

		movingMapElement.setPosition(position);
	}

	public void replacePogs(final MapElementTypeIF toReplace, final MapElementTypeIF replaceWith)
	{
		GameTableMap mapToReplace;
		if (isPublicMap())
			mapToReplace = m_publicMap;
		else
			mapToReplace = m_privateMap;

		for (MapElement pog : mapToReplace.getMapElements())
		{
			if (pog.getMapElementType() == toReplace)
			{
				pog.setElementType(replaceWith);
			}
		}
	}

	public void paintComponent(final Graphics graphics)
	{
		paintComponent(graphics, getWidth(), getHeight());
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

		// if they're on the public layer, we draw it first, then the private layer
		// on top of it at half alpha.
		// if they're on the priavet layer, we draw the public layer on white at half alpha,
		// then the private layer at full alpha

		if (isPublicMap())
		{
			// they are on the public map. Draw the public map as normal,
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			paintMap(g, m_publicMap, width, height);
		}
		else
		{
			// they're on the private map. First, draw the public map as normal.
			// Then draw a 50% alpha sheet over it. then draw the private map
			paintMap(g, getPublicMap(), width, height);

			g.setColor(OVERLAY_COLOR); // OVERLAY_COLOR is white with 50% alpha
			g.fillRect(0, 0, width, height);

			// now draw the private layer
			paintMap(g, m_privateMap, width, height);
		}
		g.dispose();
	}

	/**
	 * export the map to a jpeg image
	 * 
	 * @param mapToExport instance of the map that should be exported. If null will use the active map
	 * @param outputFile file where to save the result
	 * @throws IOException if file saving causes an error
	 */
	public void exportMap(GameTableMap mapToExport, File outputFile) throws IOException
	{
		if (mapToExport == null)
			mapToExport = getActiveMap();

		MapRectangle mapBoundsModel = mapToExport.getBounds();
		Rectangle mapBounds = modelToDraw(mapBoundsModel);

		int squareSize = getSquareSize();
		mapBounds.grow(squareSize, squareSize);

		BufferedImage image = new BufferedImage(mapBounds.width, mapBounds.height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();

		Point scrollPos = getScrollPosition();

		setScrollPosition(mapBounds.x, mapBounds.y);

		paintComponent(g, mapBounds.width, mapBounds.height);

		setScrollPosition(scrollPos);

		ImageIO.write(image, "jpg", outputFile);
	}

	private void paintMap(final Graphics g, final GameTableMap mapToDraw, int width, int height)
	{
		Graphics2D g2 = (Graphics2D) g;

		Point scrollPos = getScrollPosition();

		g.translate(-scrollPos.x, -scrollPos.y);

		// we don't draw the matte if we're on the private map)
		if (mapToDraw != m_privateMap)
		{
			drawMatte(g, scrollPos.x, scrollPos.y, width, height);
		}

		// draw all the underlays here
		for (MapElement pog : mapToDraw.getMapElements())
		{
			if (pog.getLayer() != Layer.POG)
			{
				renderPog((Graphics2D) g, pog);
			}
		}

		// we don't draw the underlay being dragged if we're not
		// drawing the current map
		if (mapToDraw == getActiveMap())
		{
			// if they're dragging an underlay, draw it here
			// there could be a pog drag in progress
			if (m_newPogIsBeingDragged)
			{
				MapCoordinates mousePos = getPogDragMousePosition();
				if (isPointVisible(mousePos))
				{
					final MapElement pog = getPogPanel().getGrabbedPog();

					if (pog.getLayer() != Layer.POG)
					{
						drawGhostlyToCanvas(pog, g);
					}
				}
			}
		}

		// Overlays
		for (MapElement pog : mapToDraw.getMapElements())
		{
			if (pog.getLayer() == Layer.OVERLAY)
			{
				renderPog((Graphics2D) g, pog);
			}
		}

		// we don't draw the grid if we're on the private map)
		if (mapToDraw != m_privateMap)
		{
			m_gridMode.drawLines(g2, scrollPos.x, scrollPos.y, width, height);
		}

		// lines
		for (LineSegment ls : mapToDraw.getLines())
		{
			// LineSegments police themselves, performance wise. If they won't touch the current
			// viewport, they don't draw
			ls.drawToCanvas(g, this);
		}

		// env
		for (MapElement pog : mapToDraw.getMapElements())
		{
			if (pog.getLayer() == Layer.ENVIRONMENT)
			{
				renderPog((Graphics2D) g, pog);
			}
		}

		// pogs
		for (MapElement pog : mapToDraw.getMapElements())
		{
			if (pog.getLayer() == Layer.POG)
			{
				renderPog((Graphics2D) g, pog);
			}
		}

		// we don't draw the pog being dragged if we're not
		// drawing the current map
		if (mapToDraw == getActiveMap())
		{
			// there could be a pog drag in progress
			if (m_newPogIsBeingDragged)
			{
				if (isPointVisible(getPogDragMousePosition()))
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
		final List<Player> players = m_frame.getPlayers();

		for (Player plr : players)
		{
			if (plr.isPointing())
			{
				// draw this player's point cursor
				final Point pointingAt = modelToDraw(plr.getPointingLocation());

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
			mouseOverPog = mapToDraw.getMapElementAt(m_mouseModelFloat);
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

		if (mapToDraw == getActiveMap())
		{
			getActiveTool().paint(g);
		}

		g.translate(scrollPos.x, scrollPos.y);
	}

	/**
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
	 * @param g
	 * @param canvas
	 */
	private void renderPog(Graphics2D g, MapElement el)
	{
		Composite oldComposite = g.getComposite();

		if (isSelected(el))
		{
			g.setComposite(UtilityFunctions.getSelectedComposite());
		}
		else if (isHighlighted(el))
		{
			g.setComposite(UtilityFunctions.getHilightedComposite());
		}

		try
		{
			el.getRenderer().drawToCanvas(g, this);
		}
		finally
		{
			g.setComposite(oldComposite);
		}
	}

	// called by the pogs area when a pog is being dragged
	public void pogDrag()
	{
		m_newPogIsBeingDragged = true;
		updatePogDropLoc();

		repaint();
	}

	/**
	 * Called at the end of a drag & drop operation from the MapElementPanel
	 */
	protected void onReleaseMapElement()
	{
		m_newPogIsBeingDragged = false;
		updatePogDropLoc();

		final MapElement pog = getPogPanel().getGrabbedPog();
		if (pog != null)
		{
			// only add the pog if it's in the viewport
			if (isPointVisible(getPogDragMousePosition()))
			{
				// #randomrotate
				if (GametableFrame.getGametableFrame().shouldRotatePogs())
				{
					boolean fh = false;
					boolean fv = UtilityFunctions.getRandom(2) == 0 ? false : true;

					int a = UtilityFunctions.getRandom(24) * 15;
					pog.setAngleFlip(a, fh, fv);
				}
				// add this pog to the list
				getActiveMap().addMapElement(pog);
			}
		}

		// make the arrow the current tool
		setActiveTool(0);
	}

	public boolean pogInViewport(final MapElement pog)
	{
		// only add the pog if they dropped it in the visible area
		final int width = (int) (pog.getFaceSize() * GameTableMap.getBaseSquareSize());

		// get the model coords of the viewable area
		final MapCoordinates portalTL = viewToModel(0, 0);
		final MapCoordinates portalBR = viewToModel(getWidth(), getHeight());

		if (pog.getPosition().x > portalBR.x)
		{
			return false;
		}
		if (pog.getPosition().y > portalBR.y)
		{
			return false;
		}
		if (pog.getPosition().x + width < portalTL.x)
		{
			return false;
		}
		if (pog.getPosition().y + width < portalTL.y)
		{
			return false;
		}
		return true;
	}

	/**
	 * Sets the pointer location for a given player in map coordinates
	 * 
	 * @param pointLocation
	 */
	private void showPointerAt(final MapCoordinates pointLocation)
	{
		final Player me = m_frame.getMyPlayer();

		if (pointLocation == null)
		{
			me.setPointing(false, null);
			return;
		}

		me.setPointing(true, pointLocation);
		setToolCursor(-1);
	}

	/*
	 * Pass the ability to check NetStatus up the chain of object calls
	 */
	public NetworkStatus getNetworkStatus()
	{
		return m_frame.getNetworkStatus();
	}

	public void scrollMapTo(MapCoordinates modelPoint)
	{
		final Point target = modelToDraw(modelPoint);
		setPrimaryScroll(getActiveMap(), target.x, target.y);
		repaint();
	}

	public void scrollToPog(final MapElement pog)
	{
		MapCoordinates pogModel = new MapCoordinates(pog.getPosition().x + (pog.getWidth() / 2), pog.getPosition().y + (pog.getHeight() / 2));
		final Point pogView = modelToView(pogModel);
		pogView.x -= (getWidth() / 2);
		pogView.y -= (getHeight() / 2);
		pogModel = viewToModel(pogView);
		smoothScrollTo(pogModel);
	}

	public void setActiveMap(final GameTableMap map)
	{
		m_activeMap = map;
	}

	public void setActiveTool(final int index)
	{
		final ToolIF oldTool = getActiveTool();
		oldTool.deactivate();

		m_activeToolId = index;

		final ToolIF tool = getActiveTool();
		tool.activate(this);
		setToolCursor(0);
		m_frame.setToolSelected(m_activeToolId);
	}

	protected void setGridModeByID(final GridModeID id)
	{
		switch (id)
		{
		case NONE:
			m_gridMode = m_noGridMode;
			break;

		case SQUARES:
			m_gridMode = m_squareGridMode;
			break;

		case HEX:
			m_gridMode = m_hexGridMode;
			break;
		}
	}

	/**
	 * **********************************************************************************************
	 * 
	 * @param id
	 * @param size
	 */
	public void setPogLayer(final MapElementID id, final Layer layer)
	{
		if (isPublicMap())
		{
			m_frame.sendBroadcast(NetSetMapElementLayer.makePacket(id, layer));
			if (m_frame.getNetworkStatus() != NetworkStatus.CONNECTED)
				doSetPogLayer(id, layer);
		}
		else
		{
			doSetPogLayer(id, layer);
		}
	}

	public void setPogSize(final MapElementID id, final float size)
	{
		if (isPublicMap())
		{
			m_frame.sendBroadcast(NetSetMapElementSize.makePacket(id, size));

			if (m_frame.getNetworkStatus() != NetworkStatus.CONNECTED)
			{
				doSetPogSize(id, size);
			}
		}
		else
		{
			doSetPogSize(id, size);
		}
	}

	/**
	 * #grouping?
	 */
	public void setPogType(final MapElement pog, final MapElement type)
	{
		if (isPublicMap())
		{
			m_frame.sendBroadcast(NetSetMapElementType.makePacket(pog.getID(), type.getID()));
			if (m_frame.getNetworkStatus() != NetworkStatus.CONNECTED)
			{
				doSetPogType(pog.getID(), type.getID());
			}
		}
		else
		{
			doSetPogType(pog.getID(), type.getID());
		}
	}

	/*
	 * This function will set the scroll for all maps, keeping their relative offsets preserved. The x,y values sent in
	 * will become the scroll values for the desired map. All others maps will preserve offsets from that.
	 */
	public void setPrimaryScroll(final GameTableMap mapToSet, final int x, final int y)
	{
		setScrollPosition(x, y);
		setScrollPosition(x, y);
		/*
		 * int dx = x - mapToSet.getScrollX(); int dy = y - mapToSet.getScrollY(); m_publicMap.setScroll(dx +
		 * mapToSet.getScrollX(), dy + mapToSet.getScrollY()); m_privateMap.setScroll(dx + mapToSet.getScrollX(), dy +
		 * mapToSet.getScrollY());
		 */
	}

	/**
	 * Sets the mouse cursor to be the cursor at the specified index for the currently active tool.
	 * 
	 * @param index The cursor of the given index for this tool. A negative number means no cursor.
	 */
	public void setToolCursor(final int index)
	{
		if (index < 0)
		{
			setCursor(m_emptyCursor);
		}
		else
		{
			setCursor(m_frame.getToolManager().getToolInfo(m_activeToolId).getCursor(index));
		}
	}

	/**
	 * Get current zoom level TODO type safe zoom level
	 * 
	 * @return
	 */
	public int getZoomLevel()
	{
		return m_zoom;
	}

	/**
	 * Set current zoom level
	 * 
	 * @param zl
	 */
	public void setZoomLevel(final int zl)
	{
		int zoomLevel = zl;
		if (zoomLevel < 0)
		{
			zoomLevel = 0;
		}

		if (zoomLevel >= NUM_ZOOM_LEVELS)
		{
			zoomLevel = NUM_ZOOM_LEVELS - 1;
		}

		if (m_zoom != zoomLevel)
		{
			m_zoom = zoomLevel;
			updateSquareSize();
			repaint();
		}
	}

	public void smoothScrollTo(MapCoordinates pos)
	{
		m_startScroll = drawToModel(getScrollPosition());
		m_deltaScroll = new MapCoordinates(pos.x - m_startScroll.x, pos.y - m_startScroll.y);
		m_scrollTime = 0;
		m_scrollTimeTotal = KEYBOARD_SCROLL_TIME;
		m_scrolling = true;
	}

	public void snapPogToGrid(final MapElement pog)
	{
		m_gridMode.snapPogToGrid(pog);
	}

	public MapCoordinates snapPoint(final MapCoordinates modelPoint)
	{
		return m_gridMode.getSnappedPixelCoordinates(modelPoint);
	}

	// --- Drawing ---

	public Point snapViewPoint(final Point viewPoint)
	{
		final MapCoordinates modelPoint = viewToModel(viewPoint);
		final MapCoordinates modelSnap = m_gridMode.getSnappedPixelCoordinates(modelPoint);
		final Point viewSnap = modelToView(modelSnap);
		return viewSnap;
	}

	public void tick(final long ms)
	{
		if (m_scrolling)
		{
			m_scrollTime += ms;
			float pos = m_scrollTime / (float) m_scrollTimeTotal;
			if (pos >= 1f)
			{
				scrollMapTo(m_startScroll.delta(m_deltaScroll));
				m_scrolling = false;
			}
			else
			{
				pos = (float) (Math.sin((pos * Math.PI) - (Math.PI / 2)) + 1) / 2;

				MapCoordinates point = m_startScroll.delta(Math.round(m_deltaScroll.x * pos), Math.round(m_deltaScroll.y * pos));
				scrollMapTo(point);
			}
		}
	}

	public void updatePogDropLoc()
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
			snapPogToGrid(grabbedPog);
		}
		else
		{
			grabbedPog.setPosition(new MapCoordinates(canvasModel.x - pogGrabOffset.x, canvasModel.y - pogGrabOffset.y));
		}
	}

	public MapCoordinates viewToModel(final int viewX, final int viewY)
	{
		Point scrollPos = getScrollPosition();
		final double squaresX = (double) (viewX + scrollPos.x) / (double) m_squareSize;
		final double squaresY = (double) (viewY + scrollPos.y) / (double) m_squareSize;

		final int modelX = (int) (squaresX * GameTableMap.getBaseSquareSize());
		final int modelY = (int) (squaresY * GameTableMap.getBaseSquareSize());

		return new MapCoordinates(modelX, modelY);

	}

	public MapCoordinates viewToModel(final Point viewPoint)
	{
		return viewToModel(viewPoint.x, viewPoint.y);
	}

	/**
	 * Set the scroll position
	 * 
	 * @revise move to VIEW
	 * @param x x coordinates of the scroll position
	 * @param y y coordinates of the scroll position
	 */
	public void setScrollPosition(int x, int y)
	{
		m_scrollPos.setLocation(x, y);
	}

	/**
	 * Set the scroll position
	 * 
	 * @revise move to VIEW
	 * @param x x coordinates of the scroll position
	 * @param y y coordinates of the scroll position
	 */
	public void setScrollPosition(Point newPos)
	{
		m_scrollPos.setLocation(newPos);
	}

	/**
	 * Gets the X coordinate of the scroll position
	 * 
	 * @revise move to VIEW
	 * @return
	 */
	public int getScrollX()
	{
		return m_scrollPos.x;
	}

	/**
	 * Gets the X coordinate of the scroll position
	 * 
	 * @revise move to VIEW
	 * @return
	 */
	public int getScrollY()
	{
		return m_scrollPos.y;
	}

	/**
	 * Gets the X coordinate of the scroll position
	 * 
	 * @revise move to VIEW
	 * @return
	 */
	public Point getScrollPosition()
	{
		return m_scrollPos;
	}

	public static void drawDottedRect(final Graphics g, final int ix, final int iy, final int iWidth, final int iHeight)
	{
		final Graphics2D g2d = (Graphics2D) g;
		final Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] { 2f }, 0f));

		int x = ix;
		int y = iy;
		int width = iWidth;
		int height = iHeight;
		if (width < 0)
		{
			x += width;
			width = -width;
		}
		if (height < 0)
		{
			y += height;
			height = -height;
		}
		g.drawRect(x, y, width, height);
		g2d.setStroke(oldStroke);
	}

	public static void drawDottedLine(final Graphics g, final int x, final int y, final int x2, final int y2)
	{
		final Graphics2D g2d = (Graphics2D) g;
		final Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] { 2f }, 0f));
		g.drawLine(x, y, x2, y2);
		g2d.setStroke(oldStroke);
	}

	/**
	 * Verifies if specified element is selected on the current map
	 * 
	 * @param mapElement element to verify
	 * @return true if selected
	 */
	public boolean isSelected(MapElement mapElement)
	{
		return isSelected(mapElement, isPublicMap());
	}

	/**
	 * Adds a instance to the selected list on the current map
	 * 
	 * @param mapElement Instance to add to selection
	 * @param select true to select, false to unselect
	 */
	public void selectMapElementInstance(MapElement mapElement, boolean select)
	{
		selectMapElementInstance(mapElement, isPublicMap(), select);
	}

	/**
	 * Add multiple instances to the selection on the current map
	 * 
	 * @param mapElements List of instance to add to the selection
	 * @param select true to select, false to unselect
	 */
	public void selectMapElementInstances(final List<MapElement> mapElements, boolean select)
	{
		selectMapElementInstances(mapElements, isPublicMap(), select);
	}

	/**
	 * Remove all instance from selection on the current map
	 */
	public void unselectAllMapElementInstances()
	{
		unselectAllMapElementInstances(isPublicMap());
	}

	/**
	 * Gets selected map element instances list on the current map
	 * 
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElement> getSelectedMapElementInstances()
	{
		return getSelectedMapElementInstances(isPublicMap());
	}

	/**
	 * Verifies if specified element is selected
	 * 
	 * @param mapElement element to verify
	 * @param publicMap true to query for public map, false for private map
	 * @return true if selected
	 */
	public boolean isSelected(MapElement mapElement, boolean publicMap)
	{
		return (publicMap ? m_selectionPublic : m_selectionPrivate).isSelected(mapElement);
	}

	/**
	 * Adds a instance to the selected list
	 * 
	 * @param mapElement Instance to add to selection
	 * @param publicMap true to query for public map, false for private map
	 * @param select true to select, false to unselect
	 */
	public void selectMapElementInstance(MapElement mapElement, boolean publicMap, boolean select)
	{
		(publicMap ? m_selectionPublic : m_selectionPrivate).selectMapElement(mapElement, select);
	}

	/**
	 * Add multiple instances to the selection
	 * 
	 * @param mapElements List of instance to add to the selection
	 * @param publicMap true to query for public map, false for private map
	 * @param select true to select, false to unselect
	 */
	public void selectMapElementInstances(final List<MapElement> mapElements, boolean publicMap, boolean select)
	{
		(publicMap ? m_selectionPublic : m_selectionPrivate).selectMapElements(mapElements, select);
	}

	/**
	 * Remove all instance from selection
	 * 
	 * @param publicMap true to query for public map, false for private map
	 */
	public void unselectAllMapElementInstances(boolean publicMap)
	{
		(publicMap ? m_selectionPublic : m_selectionPrivate).unselectAllMapElements();
	}

	/**
	 * Gets selected map element instances list
	 * 
	 * @param publicMap true to query for public map, false for private map
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElement> getSelectedMapElementInstances(boolean publicMap)
	{
		return (publicMap ? m_selectionPublic : m_selectionPrivate).getSelectedMapElements();
	}

	/**
	 * Set an element as highlighted
	 * 
	 * @param mapElement Map element to highlight
	 * @param highlight true to highlight, false to 'unhighlight'
	 */
	public void highlightMapElementInstance(MapElement mapElement, boolean highlight)
	{
		m_highlightedElements.selectMapElement(mapElement, highlight);
		repaint();
	}

	/**
	 * Highlight or 'unhighlight' all element instances
	 * 
	 * @param highlight true to highlight, false to 'unhighlight'
	 */
	public void highlightAllMapElementInstances(boolean highlight)
	{
		if (highlight)
			m_highlightedElements.selectMapElements(getActiveMap().getMapElements(), highlight);
		else
			m_highlightedElements.unselectAllMapElements();

		repaint();
	}

	/**
	 * Highlight or 'unhighlight' a list of instances
	 * 
	 * @param mapElements list of instances to change highlight status
	 * @param highlight true to highlight, false to 'unhighlight'
	 */
	public void highlightMapElementInstances(List<MapElement> mapElements, boolean highlight)
	{
		m_highlightedElements.selectMapElements(mapElements, highlight);
		repaint();
	}

	/**
	 * Gets the list of highlighted element instances
	 * 
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElement> getHighlightedMapElementInstances()
	{
		return m_highlightedElements.getSelectedMapElements();
	}

	/**
	 * Checks if a specific map element is marked as highlighted
	 * 
	 * @param mapElement Map element to highlight
	 * @return true if highlighted
	 */
	public boolean isHighlighted(MapElement mapElement)
	{
		return m_highlightedElements.isSelected(mapElement);
	}
	
	private class CanvasMapElementListener extends MapElementAdapter
	{
		/**
		 * Constructor
		 * @param publicMap True if this listener is linked to a public map 
		 */
		public CanvasMapElementListener(boolean publicMap)
		{
			m_listenToPublicMap = publicMap;
		}
		
		/*
		 * @see com.galactanet.gametable.data.MapElementAdapter#onPositionChanged(com.galactanet.gametable.data.MapElement, com.galactanet.gametable.data.MapCoordinates, com.galactanet.gametable.data.MapCoordinates, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onPositionChanged(MapElement element, MapCoordinates newPosition, MapCoordinates oldPosition, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetSetMapElementPosition.makePacket(element.getID(), newPosition));
			}

			repaint();
		}
		
		/*
		 * @see com.galactanet.gametable.data.MapElementAdapter#onFlipChanged(com.galactanet.gametable.data.MapElement, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onFlipChanged(MapElement element, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetFlipMapElement.makePacket(element.getID(), element.getFlipH(), element.getFlipV()));
			}

			repaint();
		}
		
		/*
		 * @see com.galactanet.gametable.data.MapElementAdapter#onAngleChanged(com.galactanet.gametable.data.MapElement, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAngleChanged(MapElement element, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetSetAngleMapElement.makePacket(element.getID(), element.getAngle()));
			}

			repaint();
		}
		
		/*
		 * @see com.galactanet.gametable.data.MapElementAdapter#onAttributeChanged(com.galactanet.gametable.data.MapElement, java.lang.String, java.lang.String, java.lang.String, boolean, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				if (m_listenToPublicMap)
				{
					// Broadcast only if we're not triggered by a network event
					if (shouldPropagateChanges(netEvent))
					{
						Map<String, String> add = null;
						List<String> remove = null;
						
						if (newValue == null)
						{
							remove = new ArrayList<String>();
							remove.add(attributeName);
						}
						else
						{
							add = new HashMap<String, String>();
							add.put(attributeName, newValue);
						}
							
						m_frame.sendBroadcast(NetSetMapElementData.makePacket(element, null, add, remove));
					}
				}
					
				repaint();
			}
		}
		
		/*
		 * @see com.galactanet.gametable.data.MapElementAdapter#onAttributesChanged(com.galactanet.gametable.data.MapElement, java.util.Map, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAttributesChanged(MapElement element, Map<String, String> attributes, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
				{
					Map<String, String> add = null;
					List<String> remove = null;
					
					for (Entry<String, String> entry : attributes.entrySet())
					{
						if (entry.getValue() == null)
						{
							if (remove == null)
								remove = new ArrayList<String>();
							
							remove.add(entry.getKey());
						}
						else
						{
							if (add == null)
								add = new HashMap<String, String>();
							
							add.put(entry.getKey(), entry.getValue());
						}
					}
						
					m_frame.sendBroadcast(NetSetMapElementData.makePacket(element, null, add, remove));
				}
			}
				
			repaint();
		}
		
		/*
		 * @see com.galactanet.gametable.data.MapElementAdapter#onNameChanged(com.galactanet.gametable.data.MapElement, java.lang.String, java.lang.String, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onNameChanged(MapElement element, String newName, String oldName, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetSetMapElementData.makeRenamePacket(element, newName));
			}
				
			repaint();
		}
		
		private final boolean m_listenToPublicMap;
	}
	
	private class CanvasMapListener extends GameTableMapAdapter 
	{
		/*
		 * @see
		 * com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstanceRemoved(com.galactanet.gametable.data
		 * .GameTableMap, com.galactanet.gametable.data.MapElement, boolean)
		 */
		@Override
		public void onMapElementRemoved(GameTableMap map, MapElement mapElement, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				selectMapElementInstance(mapElement, map == m_publicMap, false);
				highlightMapElementInstance(mapElement, false);
				
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetRemoveMapElement.makePacket(mapElement));
				
				repaint();				
			}
		}
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstancesRemoved(com.galactanet.gametable.data.GameTableMap, java.util.List, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onMapElementsRemoved(GameTableMap map, List<MapElement> mapElements, NetworkEvent netEvent)
		{
			for (MapElement mapElement : mapElements)
			{
				selectMapElementInstance(mapElement, map == m_publicMap, false);
				highlightMapElementInstance(mapElement, false);
			}
			
			if (shouldPropagateChanges(netEvent) && mapElements.size() > 0)
				m_frame.sendBroadcast(NetRemoveMapElement.makePacket(mapElements));
			
			repaint();
		}

		/*
		 * @see
		 * com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstancesCleared(com.galactanet.gametable.data
		 * .GameTableMap)
		 */
		@Override
		public void onMapElementsCleared(GameTableMap map, NetworkEvent netEvent)
		{
			unselectAllMapElementInstances(map == m_publicMap);
			highlightAllMapElementInstances(false);
			
			if (shouldPropagateChanges(netEvent))
				m_frame.sendBroadcast(NetRemoveMapElement.makeRemoveAllPacket());
			
			repaint();
		}
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapAdapter#onLineSegmentAdded(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.LineSegment, boolean)
		 */
		@Override
		public void onLineSegmentAdded(GameTableMap map, LineSegment lineSegment, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				if (map.isPublicMap())
				{
					// Broadcast only if we're not triggered by a network event
					if (shouldPropagateChanges(netEvent))
						m_frame.sendBroadcast(NetAddLineSegments.makePacket(lineSegment));
				}
				
				repaint();
			}				
		}

		/*
		 * @see com.galactanet.gametable.data.GameTableMapAdapter#onLineSegmentsCropped(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapRectangle, boolean, int, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onEraseLineSegments(GameTableMap map, MapRectangle rect, boolean colorSpecific, int color, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetEraseLineSegments.makePacket(rect, colorSpecific, color));
			}
			
			repaint();
		}
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapAdapter#onLineSegmentsAdded(com.galactanet.gametable.data.GameTableMap, java.util.List)
		 */
		@Override
		public void onLineSegmentsAdded(GameTableMap map, List<LineSegment> lineSegments, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetAddLineSegments.makePacket(lineSegments));
			}
			
			repaint();
		}
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapAdapter#onClearLineSegments(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onClearLineSegments(GameTableMap map, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetClearLineSegments.makePacket());
			}
			
			repaint();
		}		
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstanceAdded(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElement, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onMapElementAdded(GameTableMap map, MapElement mapElement, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					m_frame.sendBroadcast(NetAddMapElement.makePacket(mapElement));
			}
			
			repaint();
		}
	}
}
