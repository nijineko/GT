package com.galactanet.gametable.ui.tools;


import java.awt.*;

import com.galactanet.gametable.data.GametableMap;
import com.galactanet.gametable.data.Pog;
import com.galactanet.gametable.ui.GametableCanvas;

/**
 * Map tool for erasing lines.
 * 
 * @author iffy
 */
public class SelectTool extends NullTool
{
    private static Rectangle createRectangle(final Point a, final Point b)
    {
        final int x = Math.min(a.x, b.x);
        final int y = Math.min(a.y, b.y);
        final int width = Math.abs(b.x - a.x) + 1;
        final int height = Math.abs(b.y - a.y) + 1;

        return new Rectangle(x, y, width, height);
    }

    private GametableCanvas m_canvas;
    private Point           m_mouseAnchor;
    private Point           m_mouseFloat;

    private GametableMap    m_map;

    /**
     * Default Constructor.
     */
    public SelectTool()
    {
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#activate(com.galactanet.gametable.GametableCanvas)
     */
    public void activate(final GametableCanvas canvas)
    {
        m_canvas = canvas;
        m_mouseAnchor = null;
        m_mouseFloat = null;
    }

    // turns off all the tinting for the pogs
    public void clearTints()
    {
        for (int i = 0; i < m_map.getNumPogs(); i++)
        {
            m_map.getPog(i).setTinted(false);
        }
    }

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
    public boolean isBeingUsed()
    {
        return (m_mouseAnchor != null);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
     */
    public void mouseButtonPressed(final int x, final int y, final int modifierMask)
    {
        if (m_canvas.isPublicMap()) {
            m_map = m_canvas.getPublicMap();            
        } else {
            m_map = m_canvas.getPrivateMap();
        }

        m_map.clearSelectedPogs();
        m_canvas.repaint();
        
        m_mouseAnchor = new Point(x, y);
        m_mouseFloat = m_mouseAnchor;
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int, int)
     */
    public void mouseButtonReleased(final int x, final int y, final int modifierMask)
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
            
System.out.println("start");            
            for (int i = 0; i < m_map.getNumPogs(); i++)
            {
System.out.println("lop " + i);                
                final Pog pog = m_map.getPog(i);
                if (pog.isTinted() && (!pog.isLocked() || bIgnoreLock)) {
                    m_map.addSelectedPog(pog);
                }
                
            }
System.out.println("end");            
        }
        endAction();
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
     */
    public void mouseMoved(final int x, final int y, final int modifierMask)
    {
        if (m_mouseAnchor != null)
        {
            m_mouseFloat = new Point(x, y);
            setTints((modifierMask & MODIFIER_SHIFT) != 0);
            m_canvas.repaint();
        }
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#paint(java.awt.Graphics)
     */
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
        final Rectangle selRect = createRectangle(m_mouseAnchor, m_mouseFloat);

        for (int i = 0; i < m_map.getNumPogs(); i++)
        {
            final Pog pog = m_map.getPog(i);

            final int size = pog.getFaceSize() * GametableCanvas.BASE_SQUARE_SIZE;
            final Point tl = new Point(pog.getPosition());
            final Point br = new Point(pog.getPosition());
            br.x += size;
            br.y += size;
            final Rectangle pogRect = createRectangle(tl, br);

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
