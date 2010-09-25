/*
 * EraseTool.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.tools;

import java.awt.*;
import java.util.ArrayList;

import com.galactanet.gametable.data.LineSegment;
import com.galactanet.gametable.data.MapCoordinates;
import com.galactanet.gametable.data.MapRectangle;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Images;



/**
 * Map tool for drawing boxes.
 * 
 * @author iffy
 * 
 * #GT-AUDIT BoxTool
 */
public class BoxTool extends NullTool
{
    private GametableCanvas m_canvas;
    private MapCoordinates           m_mouseAnchor;
    private MapCoordinates            m_mouseFloat;

    private MapCoordinates            m_mousePosition;

    /**
     * Default Constructor.
     */
    public BoxTool()
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
        m_mousePosition = modelPos;
        m_mouseAnchor = m_mousePosition;
        if ((modifierMask & MODIFIER_CTRL) == 0)
        {
            m_mouseAnchor = m_canvas.snapPoint(m_mouseAnchor);
        }
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
            // we're going to add 4 lines
            final Color drawColor = GametableFrame.getGametableFrame().m_drawColor;
            final MapCoordinates topLeft = m_mouseAnchor;
            final MapCoordinates bottomRight = m_mouseFloat;
            final MapCoordinates topRight = new MapCoordinates(bottomRight.x, topLeft.y);
            final MapCoordinates bottomLeft = new MapCoordinates(topLeft.x, bottomRight.y);

            final LineSegment top = new LineSegment(topLeft, topRight, drawColor);
            final LineSegment left = new LineSegment(topLeft, bottomLeft, drawColor);
            final LineSegment right = new LineSegment(topRight, bottomRight, drawColor);
            final LineSegment bottom = new LineSegment(bottomLeft, bottomRight, drawColor);

            java.util.List<LineSegment> lines = new ArrayList<LineSegment>(4);
            lines.add(top);
            lines.add(left);
            lines.add(right);
            lines.add(bottom);

            m_canvas.getActiveMap().addLineSegments(lines);
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
            m_mousePosition = modelPos;
            m_mouseFloat = m_mousePosition;
            if ((modifierMask & MODIFIER_CTRL) == 0)
            {
                m_mouseFloat = m_canvas.snapPoint(m_mouseFloat);
            }
            m_canvas.repaint();
        }
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#paint(java.awt.Graphics)
     */
    /*
     * Modified from original to have separate distance indicators for each dimension.
     */
    @Override
		public void paint(final Graphics g)
    {
        if (m_mouseAnchor != null)
        {
            final Graphics2D g2 = (Graphics2D)g.create();

            g2.addRenderingHints(Images.getRenderingHints());

            final Color drawColor = GametableFrame.getGametableFrame().m_drawColor;
            g2.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), 102));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            final Rectangle drawRect = createRectangle(m_canvas.modelToDraw(m_mouseAnchor), m_canvas
                .modelToDraw(m_mouseFloat));
            g2.draw(drawRect);
            g2.dispose();

            final MapRectangle modelRect = new MapRectangle(m_mouseAnchor, m_mouseFloat);

            double squaresWidth = m_canvas.modelToSquares(modelRect.width);
            double squaresHeight = m_canvas.modelToSquares(modelRect.height);

            if (squaresWidth >= 0.75)
            {
                squaresWidth = Math.round(squaresWidth * 100) / 100.0;

                final Graphics2D g3 = (Graphics2D)g.create();

                g3.setFont(Font.decode("sans-12"));

                // String s1 = squaresWidth + " x " + squaresHeight + "u";
                final String sw = Double.toString(squaresWidth) + GametableFrame.getGametableFrame().grid_unit;

                final FontMetrics fm = g3.getFontMetrics();
                final Rectangle rect = fm.getStringBounds(sw, g3).getBounds();

                rect.grow(3, 1);

                /*
                 * Point drawPoint = m_canvas.modelToDraw(m_mousePosition); drawPoint.y -= rect.height + rect.y + 10;
                 * Point viewPoint = m_canvas.modelToView(m_canvas.drawToModel(drawPoint)); if (viewPoint.y -
                 * rect.height < 0) { drawPoint = m_canvas.modelToDraw(m_mousePosition); drawPoint.y -= rect.y - 24; }
                 * 
                 * if (viewPoint.x + rect.width >= m_canvas.getWidth()) { drawPoint.x -= rect.width + 10; }
                 */
                final Point drawPoint = m_canvas.modelToDraw(m_mouseAnchor);
                final Point mousePoint = m_canvas.modelToDraw(m_mouseFloat);
                drawPoint.x = (drawPoint.x + mousePoint.x) / 2;
                drawPoint.y = mousePoint.y - 10;
                g3.translate(drawPoint.x, drawPoint.y);
                g3.setColor(new Color(0x00, 0x99, 0x00, 0xAA));
                g3.fill(rect);
                g3.setColor(new Color(0x00, 0x66, 0x00));
                g3.draw(rect);
                g3.setColor(new Color(0xFF, 0xFF, 0xFF, 0xCC));
                g3.drawString(sw, 0, 0);
                g3.dispose();

            }

            if (squaresHeight > 0.75)
            {
                final Point drawPoint = m_canvas.modelToDraw(m_mouseAnchor);
                final Point mousePoint = m_canvas.modelToDraw(m_mouseFloat);
                final Graphics2D g4 = (Graphics2D)g.create();
                squaresHeight = Math.round(squaresHeight * 100) / 100.0;
                g4.setFont(Font.decode("sans-12"));
                final String sh = Double.toString(squaresHeight) + GametableFrame.getGametableFrame().grid_unit;
                final FontMetrics fm2 = g4.getFontMetrics();
                final Rectangle rect2 = fm2.getStringBounds(sh, g4).getBounds();
                rect2.grow(3, 1);
                drawPoint.x = mousePoint.x + 10;
                drawPoint.y = (drawPoint.y + mousePoint.y) / 2;
                g4.translate(drawPoint.x, drawPoint.y);
                g4.setColor(new Color(0x00, 0x99, 0x00, 0xAA));
                g4.fill(rect2);
                g4.setColor(new Color(0x00, 0x66, 0x00));
                g4.draw(rect2);
                g4.setColor(new Color(0xFF, 0xFF, 0xFF, 0xCC));
                g4.drawString(sh, 0, 0);
                g4.dispose();
            }
        }
    }
}
