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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.galactanet.gametable.data.MapElement.Layer;
import com.galactanet.gametable.data.deck.Card;
import com.galactanet.gametable.data.grid.HexGridMode;
import com.galactanet.gametable.data.grid.SquareGridMode;
import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.data.net.PacketSourceState;
import com.galactanet.gametable.ui.tools.NullTool;
import com.galactanet.gametable.util.Images;
import com.galactanet.gametable.util.UtilityFunctions;



/**
 * The main map view of Gametable.
 * 
 * #GT-AUDIT GametableCanvas
 */
public class GametableCanvas extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener
{
    // grid modes
    public final static int    GRID_MODE_NONE         = 0;
    public final static int    GRID_MODE_SQUARES      = 1;
    public final static int    GRID_MODE_HEX          = 2;

    public final static int    NUM_ZOOM_LEVELS        = 5;

    private static final float KEYBOARD_SCROLL_FACTOR = 0.5f;
    private static final int   KEYBOARD_SCROLL_TIME   = 300;

    private static final Font  MAIN_FONT              = Font.decode("sans-12");
    /**
     * A singleton instance of the NULL tool.
     */
    private static final ToolIF  NULL_TOOL              = new NullTool();

    /**
     * This is the color used to overlay on top of the public layer when the user is on the private layer. It's white
     * with 50% alpha
     */
    private static final Color OVERLAY_COLOR          = new Color(255, 255, 255, 128);

    /**
     *
     */
    private static final long  serialVersionUID       = 6250860728974514790L;

    private Image              m_mapBackground;

    // this is the map (or layer) that all players share
    private final GameTableMap m_publicMap            = new GameTableMap(true);
    // this is the map (or layer) that is private to a specific player
    private final GameTableMap m_privateMap           = new GameTableMap(false);
    // this points to whichever map is presently active
    private GameTableMap       m_activeMap;

    private int                m_activeToolId         = -1;

    private boolean            m_bAltKeyDown;
    private boolean            m_bControlKeyDown;
    private boolean            m_bMouseOnView;
    private boolean            m_bShiftKeyDown;

    // misc flags
    private boolean            m_bSpaceKeyDown;
    private MapCoordinates              m_deltaScroll;
    // some cursors
    private Cursor             m_emptyCursor;
    // the frame
    private GametableFrame     m_gametableFrame;

    GridMode                   m_gridMode;
    SquareGridMode             m_squareGridMode       = new SquareGridMode();
    HexGridMode                m_hexGridMode          = new HexGridMode();
    GridMode                   m_noGridMode           = new GridMode();

    private MapCoordinates              m_mouseModelFloat;

    private boolean            m_newPogIsBeingDragged;
    private MapElementInstance                m_pogMouseOver;
    private Image              m_pointingImage;
    /**
     * the id of the tool that we switched out of to go to hand tool for a right-click
     */
    private int                m_previousToolId;

    /**
     * true if the current mouse action was initiated with a right-click
     */
    private boolean            m_rightClicking;
    private MapCoordinates		m_startScroll;
    private boolean            m_scrolling;
    private long               m_scrollTime;
    private long               m_scrollTimeTotal;
    
    private SelectionHandler	m_selectionPublic;
    private SelectionHandler 	m_selectionPrivate;

    /**
     * This is the number of screen pixels that are used per model pixel. It's never less than 1
     */
    private int                 m_zoom                 = 1;

    // the size of a square at the current zoom level
    private int                 m_squareSize           = 0;
    
    /**
     * Constructor.
     */
    public GametableCanvas()
    {
    	m_selectionPublic = new SelectionHandler();
    	m_selectionPrivate = new SelectionHandler();
    	
        setFocusable(true);
        setRequestFocusEnabled(true);

        addMouseListener(this);
        addMouseMotionListener(this);
        addFocusListener(new FocusListener()
        {
            /*
             * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
             */
            public void focusGained(final FocusEvent e)
            {
                final JPanel panel = (JPanel)getParent();
                panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), LineBorder
                    .createBlackLineBorder()));
            }

            /*
             * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
             */
            public void focusLost(final FocusEvent e)
            {
                final JPanel panel = (JPanel)getParent();
                panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(1, 1, 1, 1)));
            }

        });

        initializeKeys();

        m_activeMap = m_publicMap;
        
     		updateSquareSize();
     		
        GameTableMapListenerIF mapListener = createGameTableMapListener();
        m_publicMap.addListener(mapListener);
        m_privateMap.addListener(mapListener);
    }
    
    private GameTableMapListenerIF createGameTableMapListener()
		{
			GameTableMapListenerIF mapListener = new GameTableMapAdapter() {
				/*
				 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstanceRemoved(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElementInstance)
				 */
				@Override
				public void onMapElementInstanceRemoved(GameTableMap map, MapElementInstance mapElement)
				{
					unselectMapElementInstance(mapElement, map == m_publicMap);
				}
				
				/*
				 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstancesCleared(com.galactanet.gametable.data.GameTableMap)
				 */
				@Override
				public void onMapElementInstancesCleared(GameTableMap map)
				{
					unselectAllMapElementInstances(map == m_publicMap);
				}
			};
			return mapListener;
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

        getActionMap().put("startPointing", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = -1053248611112843772L;

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

                    pointAt(m_mouseModelFloat);
                }
            }
        });

        getActionMap().put("stopPointing", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = -8422918377090083512L;

            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                m_bSpaceKeyDown = false;
                pointAt(null);
            }
        });

        getActionMap().put("shiftDown", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 3881440237209743033L;

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

        getActionMap().put("shiftUp", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 4458628987043121905L;

            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                m_bShiftKeyDown = false;
                repaint();
            }
        });

        getActionMap().put("controlDown", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 7483132144245136048L;

            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                m_bControlKeyDown = true;
                repaint();
            }
        });

        getActionMap().put("controlUp", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = -3685986269044575610L;

            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                m_bControlKeyDown = false;
                repaint();
            }
        });

        getActionMap().put("altDown", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 1008551504896354075L;

            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                m_bAltKeyDown = true;
                repaint();
            }
        });

        getActionMap().put("altUp", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = -5789160422348881793L;

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

        getActionMap().put("zoomIn", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = -6378089523552259896L;

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

        getActionMap().put("zoomOut", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 3489902228064051594L;

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

        getActionMap().put("scrollUp", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 3255081196222471923L;

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
                final MapCoordinates p = drawToModel(pos.x, pos.y
                    - Math.round(getHeight() * KEYBOARD_SCROLL_FACTOR));
                smoothScrollTo(p);
            }
        });

        getActionMap().put("scrollDown", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 2041156257507421225L;

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
                final MapCoordinates p = 
                	drawToModel(
                			pos.x, 
                			pos.y + Math.round(getHeight() * KEYBOARD_SCROLL_FACTOR));
                
                smoothScrollTo(p);
            }
        });

        getActionMap().put("scrollLeft", new AbstractAction()
        {
            /**
             * 
             */
            private static final long serialVersionUID = -2772860909080008403L;

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
                final MapCoordinates p = drawToModel(
                			pos.x - Math.round(getWidth() * KEYBOARD_SCROLL_FACTOR), 
                			pos.y);
                
                smoothScrollTo(p);
            }
        });

        getActionMap().put("scrollRight", new AbstractAction()
        {
            /**
             *
             */
            private static final long serialVersionUID = -4782758632637647018L;

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
                final MapCoordinates p = drawToModel(
                		pos.x + Math.round(getWidth() * KEYBOARD_SCROLL_FACTOR), 
                		pos.y);
                
                smoothScrollTo(p);
            }
        });
    }

    public void addCardPog(final MapElementInstance toAdd)
    {
        m_privateMap.addMapElementInstance(toAdd);
        m_gametableFrame.refreshActivePogList();
        repaint();
    }
    
    public void addLineSegment(LineSegment line)
    {
    	if (isPublicMap())
      {
          // if we're the host, push it to everyone and add the lines.
          // if we're a joiner, just push it to the host
          // stateID is irrelevant if we're a joiner
          int stateID = -1;
          if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
          {
              stateID = m_gametableFrame.getNewStateId();
          }
          m_gametableFrame.send(PacketManager.makeLinesPacket(line, m_gametableFrame.getMyPlayerId(), stateID));

          // if we're the host or if we're offline, go ahead and add them now
          if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
          {
              doAddLineSegment(line, m_gametableFrame.getMyPlayerId(), stateID);
          }
      }
      else
      {          // state ids are irrelevant on the private layer
          doAddLineSegment(line, m_gametableFrame.getMyPlayerId(), 0);
      }
    }

    public void addLineSegments(List<LineSegment> lines)
    {
        if (isPublicMap())
        {
            // if we're the host, push it to everyone and add the lines.
            // if we're a joiner, just push it to the host
            // stateID is irrelevant if we're a joiner
            int stateID = -1;
            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                stateID = m_gametableFrame.getNewStateId();
            }
            m_gametableFrame.send(PacketManager.makeLinesPacket(lines, m_gametableFrame.getMyPlayerId(), stateID));

            // if we're the host or if we're offline, go ahead and add them now
            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doAddLineSegments(lines, m_gametableFrame.getMyPlayerId(), stateID);
            }
        }
        else
        {
            // state ids are irrelevant on the private layer
            doAddLineSegments(lines, m_gametableFrame.getMyPlayerId(), 0);
        }
    }

    public void addPog(final MapElementInstance toAdd)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makeAddPogPacket(toAdd));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doAddPog(toAdd, true);
            }
        }
        else
        {
            doAddPog(toAdd, false);
        }
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

    public void doAddLineSegments(List<LineSegment> lines, final int authorID, final int stateID)
    {
        if (lines != null)
        {
        	GameTableMap map = getActiveMap();
        	for (LineSegment line : lines)
        		map.addLineSegment(line);
            
        }
        
        repaint();
    }
    
    public void doAddLineSegment(LineSegment line, final int authorID, final int stateID)
    {
        if (line != null)
        {
        	GameTableMap map = getActiveMap();
        	map.addLineSegment(line);            
        }
        
        repaint();
    }

    public void doAddPog(final MapElementInstance toAdd, final boolean bPublicLayerPog)
    {
        GameTableMap map = m_privateMap;
        if (bPublicLayerPog)
        {
            map = m_publicMap;
        }
        map.addMapElementInstance(toAdd);
        m_gametableFrame.refreshActivePogList();
        repaint();
    }

    public void doErase(final MapRectangle r, boolean bColorSpecific, final int color, final int authorID,
        final int stateID)
    {
        final MapCoordinates modelStart = r.topLeft;
        final MapCoordinates modelEnd = new MapCoordinates(modelStart.x + r.width, modelStart.y + r.height);

        final ArrayList<LineSegment> survivingLines = new ArrayList<LineSegment>();
        
        for (LineSegment ls : getActiveMap().getLines())
        {
            if (!bColorSpecific || (ls.getColor().getRGB() == color))
            {
                // we are the color being erased, or we're in erase all
                // mode
                final LineSegment[] result = ls.crop(modelStart, modelEnd);

                if (result != null)
                {
                    // this line segment is still alive
                    for (int j = 0; j < result.length; j++)
                    {
                        survivingLines.add(result[j]);
                    }
                }
            }
            else
            {
                // we are not affected by this erasing because we
                // aren't the color being erased.
                survivingLines.add(ls);
            }
        }

        // now we have just the survivors
        // replace all the lines with this list
        getActiveMap().clearLineSegments();
        for (int i = 0; i < survivingLines.size(); i++)
        {
            getActiveMap().addLineSegment(survivingLines.get(i));
        }
        repaint();
    }

    public void doLockPog(final MapElementInstanceID id, final boolean newLock)
    {
        final MapElementInstance toLock = getActiveMap().getMapElementInstance(id);
        if (toLock == null)
        {
            return;
        }

        toLock.setLocked(newLock);

        // this pog moves to the end of the array
        getActiveMap().removeMapElementInstance(toLock);
        getActiveMap().addMapElementInstance(toLock);
    }

    public void doMovePog(final MapElementInstanceID id, MapCoordinates modelPos)
    {
        final MapElementInstance toMove = getActiveMap().getMapElementInstance(id);
        if (toMove == null)
        {
            return;
        }

        toMove.setPosition(modelPos);

        // this pog moves to the end of the array
        getActiveMap().removeMapElementInstance(toMove);
        getActiveMap().addMapElementInstance(toMove);

        repaint();
    }


    public void doRecenterView(MapCoordinates modelCenter, final int zoomLevel)
    {
        // if you recenter for any reason, your tool action is cancelled
        m_gametableFrame.getToolManager().cancelToolAction();

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

    public void doRemovePog(final MapElementInstanceID id)
    {
        final MapElementInstance toRemove = getActiveMap().getMapElementInstance(id);
        if (toRemove != null)
        {
            PogGroups.removePogFromGroup(toRemove); //#grouping @revise automatic removal from group should be centralized in DATA
            getActiveMap().removeMapElementInstance(toRemove);
        }
        m_gametableFrame.refreshActivePogList();
        repaint();
    }

    public void doRemovePogs(final MapElementInstanceID[] ids, final boolean bDiscardCards)
    {
        // make a list of all the pogs that are cards
        final List<Card> cardsList = new ArrayList<Card>();

        if (bDiscardCards)
        {
            for (MapElementInstanceID i: ids)
            {
                final MapElementInstance toRemove = getActiveMap().getMapElementInstance(i);
                final Card card = Card.getCard(toRemove);
                if (card != null)
                {
                    cardsList.add(card);
                }
            }
        }

        // remove all the offending pogs
        for (int i = 0; i < ids.length; i++)
        {
            doRemovePog(ids[i]);
        }

        if (bDiscardCards)
        {
            // now remove the offending cards
            if (cardsList.size() > 0)
            {
                final Card cards[] = new Card[cardsList.size()];
                for (int i = 0; i < cards.length; i++)
                {
                    cards[i] = cardsList.get(i);
                }
                m_gametableFrame.discardCards(cards);
            }
        }
    }
    
    public void doRemovePogs(List<MapElementInstance> pogs, final boolean bDiscardCards)
    {
        // make a list of all the pogs that are cards
        final List<Card> cardsList = new ArrayList<Card>();

        if (bDiscardCards)
        {
            for (MapElementInstance toRemove : pogs)
            {
            	final Card card = Card.getCard(toRemove);
                if (card != null)
                {
                    cardsList.add(card);
                }
            }
        }

        // remove all the offending pogs
        for (MapElementInstance pog : pogs.toArray(new MapElementInstance[0]))	// convert to array to avoid comodification
        {
            doRemovePog(pog.getId());
        }

        if (bDiscardCards)
        {
            // now remove the offending cards
            if (cardsList.size() > 0)
            {
                final Card cards[] = new Card[cardsList.size()];
                for (int i = 0; i < cards.length; i++)
                {
                    cards[i] = cardsList.get(i);
                }
                m_gametableFrame.discardCards(cards);
            }
        }
    }

    public void doRotatePog(final MapElementInstanceID id, final double newAngle)
    {
        final MapElementInstance toRotate = getActiveMap().getMapElementInstance(id);
        if (toRotate == null)
        {
            return;
        }

        toRotate.setAngle(newAngle);

        // this pog moves to the end of the array
        getActiveMap().removeMapElementInstance(toRotate);
        getActiveMap().addMapElementInstance(toRotate);

        repaint();
    }

    public void doFlipPog(final MapElementInstanceID id, final boolean flipH, final boolean flipV)
    {
        final MapElementInstance toFlip = getActiveMap().getMapElementInstance(id);
        if (toFlip == null)
        {
            return;
        }

        toFlip.setFlip(flipH, flipV);

        // this pog moves to the end of the array
        getActiveMap().removeMapElementInstance(toFlip);
        getActiveMap().addMapElementInstance(toFlip);

        repaint();
    }

    public void doSetPogData(final MapElementInstanceID id, final String s, final Map<String, String> toAdd, final Set<String> toDelete)
    {
        final MapElementInstance pog = getActiveMap().getMapElementInstance(id);
        if (pog == null)
        {
            return;
        }

        if (s != null)
        {
            pog.setName(s);
        }

        if (toDelete != null)
        {
            for(String key : toDelete)
            {
                pog.removeAttribute(key);
            }
        }

        if (toAdd != null)
        {
            for (Entry<String, String> entry : toAdd.entrySet())
            {
                pog.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        m_gametableFrame.refreshActivePogList();
        repaint();
    }
    
    /** **********************************************************************************************
     * 
     * @param id
     * @param layer
     */
    public void doSetPogLayer(final MapElementInstanceID id, final Layer layer)
    {
        final MapElementInstance pog = getActiveMap().getMapElementInstance(id);
        if (pog == null)
        {
            return;
        }
        
        pog.setLayer(layer);
        
        GametableFrame.getGametableFrame().m_activePogsPanel.resetOrderPogList();
        
        repaint();
    }


    public void doSetPogSize(final MapElementInstanceID id, final float size)
    {
        final MapElementInstance pog = getActiveMap().getMapElementInstance(id);
        if (pog == null)
        {
            return;
        }

        pog.setFaceSize(size);
        snapPogToGrid(pog);
        repaint();
    }
    
    /**
     * TODO #grouping?
     */
    public void doSetPogType(final MapElementInstanceID id, final MapElementInstanceID type)
    {
        final MapElementInstance pog = getActiveMap().getMapElementInstance(id);
        final MapElementInstance tpog = getActiveMap().getMapElementInstance(type);
        if ((pog == null) || (tpog == null))
        {
            return;
        }

        pog.setMapElement(tpog.getMapElement());        
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
                g.drawImage(m_mapBackground, i * m_mapBackground.getWidth(null) + linesXOffset, j
                    * m_mapBackground.getHeight(null) + linesYOffset, null);
            }
        }
    }

    public MapCoordinates drawToModel(final int modelX, final int modelY)
    {
        return drawToModel(new Point(modelX, modelY));
    }

    public MapCoordinates drawToModel(final Point drawPoint)
    {
        final double squaresX = (double)(drawPoint.x) / (double)m_squareSize;
        final double squaresY = (double)(drawPoint.y) / (double)m_squareSize;

        final int modelX = (int)(squaresX * GameTableMap.getBaseSquareSize());
        final int modelY = (int)(squaresY * GameTableMap.getBaseSquareSize());

        return new MapCoordinates(modelX, modelY);
    }

    public void erase(final MapRectangle r, final boolean bColorSpecific, final int color)
    {
        if (isPublicMap())
        {
            // if we're the host, push it to everyone and add the lines.
            // if we're a joiner, just push it to the host
            // stateID is irrelevant if we're a joiner
            int stateID = -1;
            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                stateID = m_gametableFrame.getNewStateId();
            }
            m_gametableFrame.send(PacketManager.makeErasePacket(r, bColorSpecific, color, m_gametableFrame
                .getMyPlayerId(), stateID));
            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doErase(r, bColorSpecific, color, m_gametableFrame.getMyPlayerId(), stateID);
            }
        }
        else
        {
            // stateID is irrelevant for the private layer
            doErase(r, bColorSpecific, color, m_gametableFrame.getMyPlayerId(), 0);
        }
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
        return m_gametableFrame.getToolManager().getToolInfo(m_activeToolId).getTool();
    }

    /** 
     * 
     * @return
     */
    public int getActiveToolId() {
        return m_activeToolId;
    }   
    
    public GridMode getGridMode()
    {
        return m_gridMode;
    }

    public int getGridModeId()
    {
        if (m_gridMode == m_squareGridMode)
        {
            return GRID_MODE_SQUARES;
        }
        if (m_gridMode == m_hexGridMode)
        {
            return GRID_MODE_HEX;
        }
        return GRID_MODE_NONE;
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
        return ((m_bControlKeyDown ? ToolIF.MODIFIER_CTRL : 0) | (m_bSpaceKeyDown ? ToolIF.MODIFIER_SPACE : 0) | (m_bShiftKeyDown ? ToolIF.MODIFIER_SHIFT : 0) | (m_bShiftKeyDown ? ToolIF.MODIFIER_ALT : 0));
    }

    private MapCoordinates getPogDragMousePosition()
    {
        final Point screenMousePoint = getPogPanel().getGrabPosition();
        final Point canvasView = UtilityFunctions.getComponentCoordinates(this, screenMousePoint);

        return viewToModel(canvasView);
    }

    private PogPanel getPogPanel()
    {
        return m_gametableFrame.getPogPanel();
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

        //final Point bottomRight = m_canvas.viewToModel(bottomRightX, bottomRightY);
        final MapRectangle visbleCanvas = new MapRectangle(topLeft, canvasW, canvasH);
        
        //System.out.println(topLeft.x + " " + topLeft.y);
        //System.out.println(bottomRight.x + " " + bottomRight.y);

        return visbleCanvas;        
    }

    /**
     * Get actual square size based on current zoom level
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
        m_gametableFrame = frame;
        m_mapBackground = Images.getImage("assets/mapbk.png");

        m_pointingImage = Images.getImage("assets/whiteHand.png");

        //setPrimaryScroll(m_publicMap, 0, 0);

        // set up the grid modes
        m_squareGridMode.init(this);
        m_hexGridMode.init(this);
        m_noGridMode.init(this);
        m_gridMode = m_squareGridMode;

        addMouseWheelListener(this);
        //setZoom(0);
        setActiveTool(0);
    }
    
    public enum BackgroundColor {
    	DEFAULT("Default"),
    	GREEN("Green"),
    	DARK_GREY("Dark Grey"),
    	GREY("Grey"),
    	BLUE("Blue"),
    	BLACK("Black"),
    	WHITE("White"),
    	DARK_BLUE("Dark Blue"),
    	DARK_GREEN("Dark Green"),
    	BROWN("Brown");
    	
    	/**
    	 * Private constructor
    	 * @param text
    	 */
    	private BackgroundColor(String text)
    	{
    		m_text = text;
    	}
    	
    	/**
    	 * Get a text representation for the color
    	 * @return string
    	 */
    	public String getText()
    	{
    		return m_text;
    	}
    	
    	/**
    	 * Get BackgroundColor object from ordinal value
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
    	
    	private final String m_text;
    }
    
    public BackgroundColor cur_bg_col = BackgroundColor.DEFAULT;
    public MapElementInstanceID cur_bg_pog = null; 
    public boolean m_backgroundTypeMapElement = false;
		/**
		 * Current scroll coordinates, relative to scroll origin
		 */
		private Point										m_scrollPos		= new Point(0, 0);

    /** **********************************************************************************************
     * 
     * @param color
     */
    public void changeBackground(final MapElementInstanceID backgroundElementID) 
    {
    	changeBackground(getActiveMap().getMapElementInstance(backgroundElementID));        
    }
    
    /** **********************************************************************************************
     * 
     * @param color
     */
    public void changeBackground(final BackgroundColor color) 
    {
    	Image newBk = null;
         
            switch (color) {        
                case GREEN :
                	newBk = Images.getImage("assets/mapbk_green.png");
                    break;
                case DARK_GREY:
                	newBk = Images.getImage("assets/mapbk_dgrey.png");
                    break;
                case GREY :
                	newBk = Images.getImage("assets/mapbk_grey.png");
                    break;
                case BLUE :
                	newBk = Images.getImage("assets/mapbk_blue.png");
                    break;
                case BLACK :
                	newBk = Images.getImage("assets/mapbk_black.png");
                    break;
                case WHITE :
                	newBk = Images.getImage("assets/mapbk_white.png");
                    break;
                case DARK_BLUE:
                	newBk = Images.getImage("assets/mapbk_dblue.png");
                    break;
                case DARK_GREEN :
                	newBk = Images.getImage("assets/mapbk_dgreen.png");
                    break;
                case BROWN :
                	newBk = Images.getImage("assets/mapbk_brown.png");
                    break;
                default :
                	newBk = Images.getImage("assets/mapbk.png");
                break;
            }
            
            if (newBk != null)
            {            
            	m_mapBackground = newBk;
            cur_bg_col = color;            
            m_backgroundTypeMapElement = false;
            repaint();
            }
    }

    public void changeBackground(final MapElementInstance pog) {
        if(pog == null) return;
        m_mapBackground = pog.getMapElement().getImage();
        cur_bg_pog = pog.getId();
        m_backgroundTypeMapElement = true;
    }
    
    public void changeBackgroundCP(BackgroundColor color) {
        if(m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_NONE) {
            m_gametableFrame.send(PacketManager.makeBGColPacket(color));
            m_gametableFrame.postSystemMessage(
                m_gametableFrame.getMyPlayer().getPlayerName() + " has change the background.");
            return;
        }
        changeBackground(color);        
    }
    
    public void changeBackgroundCP(MapElementInstanceID elementID) {
      if(m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_NONE) {
          m_gametableFrame.send(PacketManager.makeBGColPacket(elementID));
          m_gametableFrame.postSystemMessage(
              m_gametableFrame.getMyPlayer().getPlayerName() + " has change the background.");
          return;
      }
      changeBackground(elementID);        
  }


    private boolean isPointing()
    {
        final Player me = m_gametableFrame.getMyPlayer();
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
        final Component focused = m_gametableFrame.getFocusOwner();
        if (focused instanceof JTextComponent)
        {
            final JTextComponent textComponent = (JTextComponent)focused;
            return textComponent.isEditable();
        }

        return false;
    }

    public void lockPog(final MapElementInstanceID id, final boolean newLock)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makeLockPogPacket(id, newLock));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doLockPog(id, newLock);
            }
        }
        else
        {
            doLockPog(id, newLock);
        }
    }

    /**
     * Convert coordinates from map coordinates to Graphics device coordinates
     * @param modelPoint
     * @return
     */
    
    public Point modelToDraw(final MapCoordinates modelPoint)
    {
        return new Point(modelToDraw(modelPoint.x), modelToDraw(modelPoint.y));
    }
    
    /**
     * Convert a coordinate from model to pixels
     * @param c model coordinate to convert
     * @return pixel coordinate
     */
    private int modelToDraw(int c)
    {
    	final double squaresX = (double)c / (double)GameTableMap.getBaseSquareSize();
    	return (int)Math.round(squaresX * m_squareSize);
    }
    
    /**
     * Convert coordinates from map coordinates to Graphics device coordinates
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
        return (m_gametableFrame.grid_multiplier * m / GameTableMap.getBaseSquareSize());
    }

    public Point modelToView(final MapCoordinates modelPoint)
    {
        final double squaresX = (double)modelPoint.x / (double)GameTableMap.getBaseSquareSize();
        final double squaresY = (double)modelPoint.y / (double)GameTableMap.getBaseSquareSize();

        int viewX = (int)Math.round(squaresX * m_squareSize);
        int viewY = (int)Math.round(squaresY * m_squareSize);

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
        m_gametableFrame.getToolManager().mouseMoved(m_mouseModelFloat, getModifierFlags());
        final MapElementInstance prevPog = m_pogMouseOver;
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
            m_gametableFrame.getToolManager().mouseButtonPressed(m_mouseModelFloat, getModifierFlags());
        }
        else
        {
            m_rightClicking = false;
            if (e.getButton() == MouseEvent.BUTTON1)
            {
                m_gametableFrame.getToolManager().mouseButtonPressed(m_mouseModelFloat, getModifierFlags());
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
        m_gametableFrame.getToolManager().mouseButtonReleased(m_mouseModelFloat,
            getModifierFlags());

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
    
    /** **********************************************************************************************
     * 
     * @param id
     * @param newX
     * @param newY
     */    
    public void movePog(final MapElementInstanceID id, MapCoordinates modelPos) {
             
        final MapElementInstance toMove = getActiveMap().getMapElementInstance(id);
        int diffx = modelPos.x - toMove.getPosition().x;
        int diffy = modelPos.y - toMove.getPosition().y;
        
        GameTableMap map = getActiveMap();
        if(isSelected(toMove)) {            
            
            for (MapElementInstance pog : getSelectedMapElementInstances().toArray(new MapElementInstance[0]))	// converted to array to prevent concurrent modification issues
            {
               if(pog.getId() != id) {
              	 MapCoordinates newPos = pog.getPosition().delta(diffx, diffy);
                   netmovePog(pog.getId(), newPos);
               }
            }
        } else if(toMove.isGrouped()) {        
            List<MapElementInstance> pogs = PogGroups.getGroupPogs(toMove.getGroup());
            MapElementInstance npog;
            
            for (MapElementInstance pog : pogs)
            {
                npog = map.getMapElementInstance(pog.getId());
                if((npog != null) && (npog != toMove)){
                    if(pog.getId() != id) {
                    	MapCoordinates newPos = pog.getPosition().delta(diffx, diffy);                        
                        netmovePog(pog.getId(), newPos);
                    }
                }
            }
        }        
        
        netmovePog(id, modelPos);
    }

    public void netmovePog(final MapElementInstanceID id, MapCoordinates modelPos)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makeMovePogPacket(id, modelPos));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doMovePog(id, modelPos);
            }
        }
        else
        {
            doMovePog(id, modelPos);
        }
    }
    
    public void replacePogs(final MapElement toReplace, final MapElement replaceWith) {
        GameTableMap mapToReplace;
        if (isPublicMap()) mapToReplace = m_publicMap;
        else mapToReplace = m_privateMap;
        
        for (MapElementInstance pog : mapToReplace.getMapElementInstances())
        {
            if(pog.getMapElement() == toReplace) {
                pog.setMapElement(replaceWith);
            }
        }
    }


    public void paintComponent(final Graphics graphics)
    {
        paintComponent(graphics, getWidth(), getHeight());
    }
    
    /**
     * Paint the component to the specified graphics, without limiting to the component's size
     * @param graphics
     * @param width
     * @param height
     */
    private void paintComponent(final Graphics graphics, int width, int height)
    {
        final Graphics2D g = (Graphics2D)graphics.create();
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
     * @param mapToExport instance of the map that should be exported.  If null will use the active map
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
 
    public void paintMap(final Graphics g, final GameTableMap mapToDraw, int width, int height)
    {
    	Graphics2D g2 = (Graphics2D)g;
    	
    	Point scrollPos = getScrollPosition();
    	
        g.translate(-scrollPos.x, -scrollPos.y);

        // we don't draw the matte if we're on the private map)
        if (mapToDraw != m_privateMap)
        {
            drawMatte(g, scrollPos.x, scrollPos.y, width, height);
        }
        
        // draw all the underlays here
        for (MapElementInstance pog : mapToDraw.getMapElementInstances())
        {
            if (pog.getLayer() != Layer.POG)
            {
                pog.getRenderer().drawToCanvas(g, this);
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
                    final MapElementInstance pog = getPogPanel().getGrabbedPog();
                    
                    if (pog.getLayer() != Layer.POG)
                    {                    	
                    	drawGhostlyToCanvas(pog, g);
                    }
                }
            }
        }
        
        // Overlays
        for (MapElementInstance pog : mapToDraw.getMapElementInstances())
        {
            if (pog.getLayer() == Layer.OVERLAY)
            {
                pog.getRenderer().drawToCanvas(g, this);
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
        for (MapElementInstance pog : mapToDraw.getMapElementInstances())
        {
            if (pog.getLayer() == Layer.ENVIRONMENT)
            {
                pog.getRenderer().drawToCanvas(g, this);
            }
        }


        // pogs
        for (MapElementInstance pog : mapToDraw.getMapElementInstances())
        {
            if (pog.getLayer() == Layer.POG)
            {
                pog.getRenderer().drawToCanvas(g, this);
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
                    final MapElementInstance pog = getPogPanel().getGrabbedPog();

                    if (pog.getLayer() == Layer.POG)
                    {
                    	drawGhostlyToCanvas(pog, g);
                    }
                }
            }
        }

        // draw the cursor overlays
        final List<Player> players = m_gametableFrame.getPlayers();

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
        MapElementInstance mouseOverPog = null;
        if (m_bMouseOnView || m_gametableFrame.shouldShowNames())
        {
            mouseOverPog = mapToDraw.getMapElementInstanceAt(m_mouseModelFloat);
            if (m_bShiftKeyDown || m_gametableFrame.shouldShowNames())
            {
                // this shift key is down. Show all pog data
            	for (MapElementInstance pog : mapToDraw.getMapElementInstances())
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
      public void drawGhostlyToCanvas(MapElementInstance el, Graphics g)
      {
          final Graphics2D g2 = (Graphics2D)g.create();
          g2.setComposite(UtilityFunctions.getGhostlyComposite());
          el.getRenderer().drawToCanvas(g2, this);
          g2.dispose();
      }

    // called by the pogs area when a pog is being dragged
    public void pogDrag()
    {
        m_newPogIsBeingDragged = true;
        updatePogDropLoc();

        repaint();
    }

    public void pogDrop()
    {
        m_newPogIsBeingDragged = false;
        updatePogDropLoc();

        final MapElementInstance pog = getPogPanel().getGrabbedPog();
        if (pog != null)
        {
            // only add the pog if it's in the viewport
            if (isPointVisible(getPogDragMousePosition()))
            {
                //#randomrotate
                if(GametableFrame.getGametableFrame().shouldRotatePogs()) {
                    boolean fh = false;
                    boolean fv = UtilityFunctions.getRandom(2) == 0 ? false : true;
                                        
                    int a = UtilityFunctions.getRandom(24) * 15;
                    pog.setAngleFlip(a, fh, fv);                    
                }
                // add this pog to the list
                addPog(pog);
            }
        }

        // make the arrow the current tool
        setActiveTool(0);
    }

    public boolean pogInViewport(final MapElementInstance pog)
    {
        // only add the pog if they dropped it in the visible area
        final int width = pog.getFaceSize() * GameTableMap.getBaseSquareSize();

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
     * @param pointLocation
     */
    private void pointAt(final MapCoordinates pointLocation)
    {
        final Player me = m_gametableFrame.getMyPlayer();

        if (pointLocation == null)
        {
            me.setPointing(false);
            m_gametableFrame.send(PacketManager.makePointPacket(m_gametableFrame.getMyPlayerIndex(), MapCoordinates.ORIGIN, false));
            repaint();
            return;
        }

        me.setPointing(true);
        me.setPointingLocation(pointLocation);

        m_gametableFrame.send(PacketManager.makePointPacket(m_gametableFrame.getMyPlayerIndex(), me.getPointingLocation(), true));

        setToolCursor(-1);

        repaint();
    }

    public void recenterView(MapCoordinates modelCenter, final int zoomLevel)
    {
        m_gametableFrame.send(PacketManager.makeRecenterPacket(modelCenter, zoomLevel));

        if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
        {
            doRecenterView(modelCenter, zoomLevel);
        }
    }

    public void removeCardPogsForCards(final Card discards[])
    {
        // distribute this to each layer
        removeCardPogsForCards(m_privateMap, discards);
        removeCardPogsForCards(m_publicMap, discards);

        m_gametableFrame.refreshActivePogList();
        repaint();
    }
    
  	/**
  	 * Remove pogs linked to cards
  	 * 
  	 * @revise move to Card Module
  	 * @param discards
  	 */
  	private void removeCardPogsForCards(GameTableMap map, final Card discards[])
  	{
  		final List<MapElementInstance> removeList = new ArrayList<MapElementInstance>();

  		for (MapElementInstance pog : map.getMapElementInstances())
  		{
  			final Card pogCard = Card.getCard(pog);
  			if (pogCard != null)
  			{
  				// this is a card pog. Is it out of the discards?
  				for (int j = 0; j < discards.length; j++)
  				{
  					if (pogCard.equals(discards[j]))
  					{
  						// it's the pog for this card
  						removeList.add(pog);
  					}
  				}
  			}
  		}

  		// remove any offending pogs
  		map.removeMapElementInstances(removeList);
  	}

    public void removePog(final MapElementInstanceID id)
    {
        removePog(id, true);
    }

    public void removePog(final MapElementInstanceID id, final boolean bDiscardCards)
    {
        removePog(id, bDiscardCards);
    }

    /*
    * Pass the ability to check NetStatus up the chain of object calls
    */
    public int getNetStatus ( )
    {
        return m_gametableFrame.getNetStatus();
    }

    public void removePogs(final MapElementInstanceID ids[], final boolean bDiscardCards)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makeRemovePogsPacket(ids));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doRemovePogs(ids, bDiscardCards);
            }
        }
        else
        {
            doRemovePogs(ids, bDiscardCards);
        }
    }
    
    public void removePogs(List<MapElementInstance> pogs, final boolean bDiscardCards)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makeRemovePogsPacket(pogs));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doRemovePogs(pogs, bDiscardCards);
            }
        }
        else
        {
            doRemovePogs(pogs, bDiscardCards);
        }
    }


    public void rotatePog(final MapElementInstanceID id, final double newAngle)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makeRotatePogPacket(id, newAngle));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doRotatePog(id, newAngle);
            }
        }
        else
        {
            doRotatePog(id, newAngle);
        }
    }
    
    public void flipPog(final MapElementInstanceID id, final boolean flipH, final boolean flipV)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makeFlipPogPacket(id, flipH, flipV));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doFlipPog(id, flipH, flipV);
            }
        }
        else
        {
            doFlipPog(id, flipH, flipV);
        }
    }
    public void scrollMapTo(MapCoordinates modelPoint)
    {
        final Point target = modelToDraw(modelPoint);
        setPrimaryScroll(getActiveMap(), target.x, target.y);
        repaint();
    }

    public void scrollToPog(final MapElementInstance pog)
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
        m_gametableFrame.setToolSelected(m_activeToolId);
    }

    public void setGridModeByID(final int id)
    {
        switch (id)
        {
            case GRID_MODE_NONE:
            {
                m_gridMode = m_noGridMode;
            }
            break;

            case GRID_MODE_SQUARES:
            {
                m_gridMode = m_squareGridMode;
            }
            break;

            case GRID_MODE_HEX:
            {
                m_gridMode = m_hexGridMode;
            }
            break;
        }
    }

    public void setPogData(MapElementInstanceID id, final String s, final Map<String, String> toAdd, final Set<String> toDelete)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makePogDataPacket(id, s, toAdd, toDelete));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doSetPogData(id, s, toAdd, toDelete);
            }
        }
        else
        {
            doSetPogData(id, s, toAdd, toDelete);
        }
    }
    
    /** **********************************************************************************************
     * 
     * @param id
     * @param size
     */
    public void setPogLayer(final MapElementInstanceID id, final Layer layer)
    {
        if (isPublicMap()) {
            m_gametableFrame.send(PacketManager.makePogLayerPacket(id, layer));
            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED) 
                doSetPogLayer(id, layer);            
        } else {
            doSetPogLayer(id, layer);
        }
        m_gametableFrame.refreshActivePogList();
    }
    


    public void setPogSize(final MapElementInstanceID id, final float size)
    {
        if (isPublicMap())
        {
            m_gametableFrame.send(PacketManager.makePogSizePacket(id, size));

            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
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
    public void setPogType(final MapElementInstance pog, final MapElementInstance type) {        
        if (isPublicMap()) {
            m_gametableFrame.send(PacketManager.makePogTypePacket(pog.getId(), type.getId()));
            if (m_gametableFrame.getNetStatus() != GametableFrame.NETSTATE_JOINED) {
                doSetPogType(pog.getId(),type.getId());
            }
        } else {
            doSetPogType(pog.getId(),type.getId());
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
            setCursor(m_gametableFrame.getToolManager().getToolInfo(m_activeToolId).getCursor(index));
        }
    }
    
    /**
     * Get current zoom level
     * TODO type safe zoom level
     * @return
     */
    public int getZoomLevel()
    {
    	return m_zoom;
    }

    /**
     * Set current zoom level
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

    public void snapPogToGrid(final MapElementInstance pog)
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
            float pos = m_scrollTime / (float)m_scrollTimeTotal;
            if (pos >= 1f)
            {
                scrollMapTo(m_startScroll.delta(m_deltaScroll));
                m_scrolling = false;
            }
            else
            {
                pos = (float)(Math.sin((pos * Math.PI) - (Math.PI / 2)) + 1) / 2;
                
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
        final MapElementInstance grabbedPog = panel.getGrabbedPog();

        // now, snap to grid if they don't have the control key down
        if (!m_bControlKeyDown)
        {
// Removed the adjustment part, because it was actually making the dragging worse
//            final Point adjustment = grabbedPog.getSnapDragAdjustment();
//            grabbedPog.setPosition(canvasModel.x - pogGrabOffset.x + adjustment.x, canvasModel.y - pogGrabOffset.y
//                + adjustment.y);
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
    	final double squaresX = (double)(viewX + scrollPos.x) / (double)m_squareSize;
      final double squaresY = (double)(viewY + scrollPos.y) / (double)m_squareSize;

      final int modelX = (int)(squaresX * GameTableMap.getBaseSquareSize());
      final int modelY = (int)(squaresY * GameTableMap.getBaseSquareSize());

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
        final Graphics2D g2d = (Graphics2D)g;
        final Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {
            2f
        }, 0f));

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
        final Graphics2D g2d = (Graphics2D)g;
        final Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {
            2f
        }, 0f));
        g.drawLine(x, y, x2, y2);
        g2d.setStroke(oldStroke);
    }
    


    
    
    /**
     * Verifies if specified element is selected on the current map
     * @param mapElement element to verify
     * @return true if selected
     */
    public boolean isSelected(MapElementInstance mapElement)
    {
    	return isSelected(mapElement, isPublicMap());
    }

    /**
  	 * Adds a instance to the selected list on the current map
  	 * 
  	 * @param mapElement Instance to add to selection
  	 */
  	public void selectMapElementInstance(MapElementInstance mapElement)
  	{
  		selectMapElementInstance(mapElement, isPublicMap());
  	}

  	/**
  	 * Add multiple instances to the selection on the current map
  	 * 
  	 * @param mapElements List of instance to add to the selection
  	 */
  	public void selectMapElementInstances(final List<MapElementInstance> mapElements)
  	{
  		selectMapElementInstances(mapElements, isPublicMap());
  	}

  	/**
  	 * Remove all instance from selection on the current map
  	 */
  	public void unselectAllMapElementInstances()
  	{
  		unselectAllMapElementInstances(isPublicMap());
  	}

  	/**
  	 * Remove an instance from the selection on the current map
  	 * 
  	 * @param mapElement Instance to remove
  	 */
  	public void unselectMapElementInstance(final MapElementInstance mapElement)
  	{
  		unselectMapElementInstance(mapElement, isPublicMap());
  	}
  	

  	/**
  	 * Gets selected map element instances list on the current map
  	 * 
  	 * @return The list of currently selected instances (unmodifiable). Never null.
  	 */
  	public List<MapElementInstance> getSelectedMapElementInstances()
  	{
  		return getSelectedMapElementInstances(isPublicMap());
  	}
  	
    /**
     * Verifies if specified element is selected
     * @param mapElement element to verify
     * @param publicMap true to query for public map, false for private map
     * @return true if selected
     */
    public boolean isSelected(MapElementInstance mapElement, boolean publicMap)
    {
    	return (publicMap ? m_selectionPublic : m_selectionPrivate).isSelected(mapElement);
    }

    /**
  	 * Adds a instance to the selected list
  	 * 
  	 * @param mapElement Instance to add to selection
  	 * @param publicMap true to query for public map, false for private map
  	 */
  	public void selectMapElementInstance(MapElementInstance mapElement, boolean publicMap)
  	{
  		(publicMap ? m_selectionPublic : m_selectionPrivate).selectMapElementInstance(mapElement);  		
  	}

  	/**
  	 * Add multiple instances to the selection
  	 * 
  	 * @param mapElements List of instance to add to the selection
  	 * @param publicMap true to query for public map, false for private map
  	 */
  	public void selectMapElementInstances(final List<MapElementInstance> mapElements, boolean publicMap)
  	{
  		(publicMap ? m_selectionPublic : m_selectionPrivate).selectMapElementInstances(mapElements);
  	}

  	/**
  	 * Remove all instance from selection
  	 * @param publicMap true to query for public map, false for private map
  	 */
  	public void unselectAllMapElementInstances(boolean publicMap)
  	{
  		(publicMap ? m_selectionPublic : m_selectionPrivate).unselectAllMapElementInstances();
  	}

  	/**
  	 * Remove an instance from the selection
  	 * 
  	 * @param mapElement Instance to remove
  	 * @param publicMap true to query for public map, false for private map
  	 */
  	public void unselectMapElementInstance(final MapElementInstance mapElement, boolean publicMap)
  	{
  		(publicMap ? m_selectionPublic : m_selectionPrivate).unselectMapElementInstance(mapElement);
  	}
  	

  	/**
  	 * Gets selected map element instances list
  	 * 
  	 * @param publicMap true to query for public map, false for private map
  	 * @return The list of currently selected instances (unmodifiable). Never null.
  	 */
  	public List<MapElementInstance> getSelectedMapElementInstances(boolean publicMap)
  	{
  		return (publicMap ? m_selectionPublic : m_selectionPrivate).getSelectedMapElementInstances();
  	}
}
