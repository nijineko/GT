/*
 * PenTool.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.tools;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.LineSegment;



/**
 * Tool for freehand drawing on the map.
 * 
 * @author iffy
 * 
 * #GT-AUDIT PenTool
 */
public class PenTool extends NullTool
{
    private GametableCanvas m_canvas;
    private PenAsset        m_penAsset;

    /**
     * Default Constructor.
     */
    public PenTool()
    {
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#activate(com.galactanet.gametable.GametableCanvas)
     */
    public void activate(final GametableCanvas canvas)
    {
        m_canvas = canvas;
        m_penAsset = null;
    }

    public void endAction()
    {
        m_penAsset = null;
        m_canvas.repaint();
    }

    /*
     * @see com.galactanet.gametable.Tool#isBeingUsed()
     */
    public boolean isBeingUsed()
    {
        return (m_penAsset != null);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
     */
    public void mouseButtonPressed(final int x, final int y, final int modifierMask)
    {
        // @revise move m_drawColor into some more reasonable access point
        m_penAsset = new PenAsset(GametableFrame.getGametableFrame().m_drawColor);
        m_penAsset.addPoint(x, y);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int)
     */
    public void mouseButtonReleased(final int x, final int y, final int modifierMask)
    {
        if (m_penAsset != null)
        {
            m_penAsset.smooth();
            List<LineSegment> lines = m_penAsset.getLineSegments();
            m_canvas.addLineSegments(lines);            
        }
        
        endAction();
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
     */
    public void mouseMoved(final int x, final int y, final int modifierMask)
    {
        if (m_penAsset != null)
        {
            m_penAsset.addPoint(x, y);
            m_canvas.repaint();
        }
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#paint(java.awt.Graphics)
     */
    public void paint(final Graphics g)
    {
        if (m_penAsset != null)
        {
            final Graphics2D g2 = (Graphics2D)g.create();
            // g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            m_penAsset.draw(g2, m_canvas);
            g2.dispose();
        }
    }
}