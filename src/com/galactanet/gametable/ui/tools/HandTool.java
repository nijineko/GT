/*
 * HandTool.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.tools;

import java.awt.Point;

import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.ui.GametableCanvas;



/**
 * #GT-COMMENT
 * 
 * @author iffy
 * 
 * #GT-AUDIT HandTool
 */
public class HandTool extends NullTool
{
    private GametableCanvas m_canvas;
    private Point           m_startMouse;
    private MapCoordinates           m_startScroll;

    /**
     * Constructor;
     */
    public HandTool()
    {
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#activate(com.galactanet.gametable.GametableCanvas)
     */
    @Override
		public void activate(final GametableCanvas canvas)
    {
        m_canvas = canvas;
        m_startScroll = null;
        m_startMouse = null;
    }

    @Override
		public void endAction()
    {
        m_startScroll = null;
        m_startMouse = null;
        m_canvas.setToolCursor(0);
    }

    /*
     * @see com.galactanet.gametable.Tool#isBeingUsed()
     */
    @Override
		public boolean isBeingUsed()
    {
        return (m_startScroll != null);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
     */
    @Override
		public void mouseButtonPressed(MapCoordinates modelPos, final int modifierMask)
    {
        m_startScroll = m_canvas
            .drawToModel(m_canvas.getScrollX(), m_canvas.getScrollY());
        m_startMouse = m_canvas.modelToView(modelPos);
        m_canvas.setToolCursor(1);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int)
     */
    @Override
		public void mouseButtonReleased(MapCoordinates modelPos, final int modifierMask)
    {
        endAction();
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
     */
    @Override
		public void mouseMoved(MapCoordinates modelPos, final int modifierMask)
    {
        if (m_startScroll != null)
        {
            final Point mousePosition = m_canvas.modelToView(modelPos);
            final MapCoordinates modelDelta = m_canvas.drawToModel(m_startMouse.x - mousePosition.x, m_startMouse.y - mousePosition.y);
            m_canvas.scrollMapTo(m_startScroll.delta(modelDelta));
        }
    }

}
