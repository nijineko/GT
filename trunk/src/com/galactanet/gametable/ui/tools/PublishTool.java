package com.galactanet.gametable.ui.tools;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.galactanet.gametable.data.GameTableMap;
import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.data.MapElementInstance;
import com.galactanet.gametable.data.MapRectangle;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.LineSegment;



/**
 * Map tool for erasing lines.
 * 
 * @author iffy
 * 
 * #GT-AUDIT PublishTool
 */
public class PublishTool extends NullTool
{
    private GametableCanvas m_canvas;
    private GameTableMap    m_from;
    private MapCoordinates           m_mouseAnchor;
    private MapCoordinates           m_mouseFloat;
    
    //private int             m_color = (GametableFrame.getGametableFrame().m_drawColor).getRGB();

    // private boolean m_bEraseColor;

    private GameTableMap    m_to;

    /**
     * Default Constructor.
     */
    public PublishTool()
    {
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#activate(com.galactanet.gametable.GametableCanvas)
     */
    @Override
		public void activate(final GametableCanvas canvas)
    {
        m_canvas = canvas;
        m_mouseAnchor = null;
        m_mouseFloat = null;
    }

    // turns off all the tinting for the pogs
    public void clearTints()
    {
    	// @revise move to MODEL?
    	for (MapElementInstance pog : m_from.getPogs())
    		pog.setTinted(false);
    }

    @Override
		public void endAction()
    {
        clearTints();
        m_mouseAnchor = null;
        m_mouseFloat = null;
        m_canvas.repaint();
    }

    /*
     * @see com.galactanet.gametable.Tool#isBeingUsed()
     */
    @Override
		public boolean isBeingUsed()
    {
        return (m_mouseAnchor != null);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
     */
    @Override
		public void mouseButtonPressed(MapCoordinates modelPos, final int modifierMask)
    {

        if (m_canvas.isPublicMap())
        {
            // this tool is not usable on the public map. So we cancel this action
            // GametableFrame.getGametableFrame().getToolManager().cancelToolAction();
            // return;
            
            //  Use from public map to return objects to private map.

            m_from = m_canvas.getPublicMap();
            m_to = m_canvas.getPrivateMap();
        }
        else
        {
            m_from = m_canvas.getPrivateMap();
            m_to = m_canvas.getPublicMap();
        }

        m_mouseAnchor = modelPos;
        m_mouseFloat = m_mouseAnchor;
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int, int)
     */
    @Override
		public void mouseButtonReleased(MapCoordinates modelPos, final int modifierMask)
    {
        if ((m_mouseAnchor != null) && !m_mouseAnchor.equals(m_mouseFloat))
        {
            // GametableFrame frame = GametableFrame.g_gameTableFrame;

            // first off, copy all the pogs/underlays over to the public layer
        	for (MapElementInstance pog : m_from.getPogs())
            {
                if (pog.isTinted() && (!pog.isLocked() || (modifierMask & MODIFIER_SHIFT) != 0))
                {
                    // this pog gets copied
                    final MapElementInstance newPog = new MapElementInstance(pog);

                    m_canvas.setActiveMap(m_to);
                    m_canvas.addPog(newPog);
                    
                    if (pog.isLocked())
                    {
                        newPog.setLocked(true);
                    }
                    
                    m_canvas.setActiveMap(m_from);
                }
            }

            // now, copy over all the line segments. we run through all the
            // line segments on the private layer, and collect a list of the
            // ones that are at least partially in the rect
            final List<LineSegment> lineList = new ArrayList<LineSegment>();

            for (LineSegment ls : m_from.getLines())
            {
                final LineSegment result = ls.getPortionInsideRect(m_mouseAnchor, m_mouseFloat);

                if (result != null)
                {
                    lineList.add(result);
                }
            }

            m_canvas.setActiveMap(m_to);
            m_canvas.addLineSegments(lineList);
            m_canvas.setActiveMap(m_from);

            boolean bDeleteFromPrivate = false;
            if ((modifierMask & MODIFIER_CTRL) == 0) // not holding control
            {
                bDeleteFromPrivate = true;
            }

            // if bDeleteFromPrivate is set, then this is a MOVE, not a COPY,
            // so we have to remove the pieces from the private layer.

            if (bDeleteFromPrivate)
            {
                // remove the pogs that we moved
            	
            	for (MapElementInstance pog : m_from.getPogs().toArray(new MapElementInstance[0]))	// converting list to array to avoid concurrent modifications
                {
                    if (pog.isTinted() && (!pog.isLocked() || (modifierMask & MODIFIER_SHIFT) != 0))
                    {
                        m_canvas.removePog(pog.getId(), false);	// this would cause concurrent modifications if we used the returned list directly 
                    }
                }

                // remove the line segments
                final MapRectangle eraseRect = new MapRectangle(m_mouseAnchor, m_mouseFloat);
                m_canvas.erase(eraseRect, false, -1);
            }
        }
        endAction();
    }

    /*
     * public final static int       NETSTATE_HOST            = 1;
     * public final static int       NETSTATE_JOINED          = 2;
     * public final static int       NETSTATE_NONE            = 0;
     */

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
     */
    @Override
		public void mouseMoved(MapCoordinates modelPos, final int modifierMask)
    {
        if (m_mouseAnchor != null)
        {
            m_mouseFloat = modelPos;
            setTints(modifierMask);
            m_canvas.repaint();
        }
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#paint(java.awt.Graphics)
     */
    @Override
		public void paint(final Graphics g)
    {
        if (m_mouseAnchor != null)
        {
            final Graphics2D g2 = (Graphics2D)g.create();
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {
                2f
            }, 0f));
            final Rectangle rect = createRectangle(m_canvas.modelToDraw(m_mouseAnchor), m_canvas
                .modelToDraw(m_mouseFloat));
            g2.draw(rect);
            g2.dispose();
        }
    }

    // sets all the pogs we're touching to be tinted
    public void setTints(final int modifierMask)
    {
        final MapRectangle selRect = new MapRectangle(m_mouseAnchor, m_mouseFloat);

        for (MapElementInstance pog : m_from.getPogs())
        {
            final int size = pog.getFaceSize() * GameTableMap.getBaseSquareSize();
            
            MapCoordinates bottomRight = pog.getPosition().delta(size, size);
            
            final MapRectangle pogRect = new MapRectangle(pog.getPosition(), bottomRight);

            if (selRect.intersects(pogRect) && (!pog.isLocked() || (modifierMask & MODIFIER_SHIFT) != 0))
            {
                // this pog will be sent
                pog.setTinted(true);
            }
            else
            {
                pog.setTinted(false);
            }
        }
    }
}
