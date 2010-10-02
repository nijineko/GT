/*
 * EraseTool.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.tools;

import java.awt.*;

import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.data.MapRectangle;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;



/**
 * Map tool for erasing lines.
 * 
 * @author iffy
 * 
 * #GT-AUDIT EraseTool
 */
public class EraseTool extends NullTool
{
    private final boolean   m_bEraseColor;
    private GametableCanvas m_canvas;
    private MapCoordinates           m_mouseAnchor;

    private MapCoordinates           m_mouseFloat;

    /**
     * Default Constructor.
     */
    public EraseTool()
    {
        this(false);
    }

    /**
     * Constructor specifying color mode.
     */
    public EraseTool(final boolean color)
    {
        m_bEraseColor = color;
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

    @Override
		public void endAction()
    {
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
        m_mouseAnchor = modelPos;
        m_mouseFloat = m_mouseAnchor;
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int)
     */
    @Override
		public void mouseButtonReleased(MapCoordinates modelPos, final int modifierMask)
    {
        if ((m_mouseAnchor != null) && !m_mouseAnchor.equals(m_mouseFloat))
        {
            if (m_bEraseColor)
            {
            	m_canvas.getActiveMap().removeLineSegments(new MapRectangle(m_mouseAnchor, m_mouseFloat), true,
                    GametableFrame.getGametableFrame().m_drawColor.getRGB());
            }
            else
            {
            	m_canvas.getActiveMap().removeLineSegments(new MapRectangle(m_mouseAnchor, m_mouseFloat), false, 0);
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

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {
                2f
            }, 0f));
            Rectangle rect = NullTool.createRectangle(m_canvas.modelToDraw(m_mouseAnchor), m_canvas.modelToDraw(m_mouseFloat));
            g2.draw(rect);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[] {
                2f
            }, 2f));
            rect = NullTool.createRectangle(m_canvas.modelToDraw(m_mouseAnchor), m_canvas.modelToDraw(m_mouseFloat));
            g2.draw(rect);

            g2.dispose();
        }
    }
}
