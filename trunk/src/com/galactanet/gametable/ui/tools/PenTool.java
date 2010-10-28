/*
 * PenTool.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.tools;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.LineSegment;
import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;



/**
 * Tool for freehand drawing on the map.
 * 
 * @author iffy
 * 
 * #GT-AUDIT PenTool
 */
public class PenTool extends NullTool
{
	private final GametableFrame m_frame;
    private GametableCanvas m_canvas;
    private PenAsset        m_penAsset;

    /**
     * Default Constructor.
     */
    public PenTool()
    {
    	m_frame = GametableApp.getUserInterface();
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#activate(com.galactanet.gametable.GametableCanvas)
     */
    @Override
		public void activate(final GametableCanvas canvas)
    {
        m_canvas = canvas;
        m_penAsset = null;
    }

    @Override
		public void endAction()
    {
        m_penAsset = null;
        m_canvas.repaint();
    }

    /*
     * @see com.galactanet.gametable.Tool#isBeingUsed()
     */
    @Override
		public boolean isBeingUsed()
    {
        return (m_penAsset != null);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
     */
    @Override
		public void mouseButtonPressed(MapCoordinates modelPos, final int modifierMask)
    {
        m_penAsset = new PenAsset(m_frame.getDrawColor());
        m_penAsset.addPoint(modelPos);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int)
     */
    @Override
		public void mouseButtonReleased(MapCoordinates modelPos, final int modifierMask)
    {
        if (m_penAsset != null)
        {
            m_penAsset.smooth();
            List<LineSegment> lines = m_penAsset.getLineSegments();
            
            GameTableCore.getCore().getMap(GameTableCore.MapType.ACTIVE).addLineSegments(lines);
        }
        
        endAction();
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
     */
    @Override
		public void mouseMoved(MapCoordinates modelPos, final int modifierMask)
    {
        if (m_penAsset != null)
        {
            m_penAsset.addPoint(modelPos);
            m_canvas.repaint();
        }
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#paint(java.awt.Graphics)
     */
    @Override
		public void paint(final Graphics g)
    {
        if (m_penAsset != null)
        {
            final Graphics2D g2 = (Graphics2D)g.create();
            m_penAsset.draw(g2, m_canvas);
            g2.dispose();
        }
    }
}
