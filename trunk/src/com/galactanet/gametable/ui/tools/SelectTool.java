package com.galactanet.gametable.ui.tools;


import java.awt.*;

import com.galactanet.gametable.data.GameTableMap;
import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.data.MapElementInstance;
import com.galactanet.gametable.data.MapRectangle;
import com.galactanet.gametable.ui.GametableCanvas;

/**
 * Map tool for erasing lines.
 * 
 * @author iffy
 */
public class SelectTool extends NullTool
{
    private GametableCanvas m_canvas;
    private MapCoordinates           m_mouseAnchor;
    private MapCoordinates           m_mouseFloat;

    /**
     * Default Constructor.
     */
    public SelectTool()
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
    	// @revise move to MODEL ?
    	for (MapElementInstance pog : m_canvas.getActiveMap().getMapElementInstances())
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
        m_canvas.unselectAllMapElementInstances();
        
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
            boolean bIgnoreLock = true;
            if ((modifierMask & MODIFIER_SHIFT) == 0) // not holding control
            {
                bIgnoreLock = false;
            }

            // first off, copy all the pogs/underlays over to the public layer
            
            for (MapElementInstance pog : m_canvas.getActiveMap().getMapElementInstances())
            {
                if (pog.isTinted() && (!pog.isLocked() || bIgnoreLock)) {
                    m_canvas.selectMapElementInstance(pog);
                }                
            }
        }
        endAction();
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
     */
    @Override
		public void mouseMoved(MapCoordinates modelPos, final int modifierMask)
    {
        if (m_mouseAnchor != null)
        {
            m_mouseFloat = modelPos;
            setTints((modifierMask & MODIFIER_SHIFT) != 0);
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
    public void setTints(final boolean bIgnoreLock)
    {
        final MapRectangle selRect = new MapRectangle(m_mouseAnchor, m_mouseFloat);

        for (MapElementInstance pog : m_canvas.getActiveMap().getMapElementInstances())
        {
        	final int size = pog.getFaceSize() * GameTableMap.getBaseSquareSize();
          
          MapCoordinates bottomRight = pog.getPosition().delta(size, size);
          
          final MapRectangle pogRect = new MapRectangle(pog.getPosition(), bottomRight);

            if (selRect.intersects(pogRect) && (!pog.isLocked() || bIgnoreLock))
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
