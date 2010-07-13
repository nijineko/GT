/*
 * GridMode.java
 * 
 * @created 2005-09-02
 * 
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.galactanet.gametable.data;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.MapElementRendererIF;
import com.maziade.tools.XMLUtils;

/**
 * Immutable line segment
 * 
 * @author sephalon
 * 
 * @audited by themaze75
 */
public class LineSegment implements MapElementRendererIF, XMLSerializeIF
{
	/**
	 * Color of the line segment
	 */
    private final Color m_color;
    
    /**
     * End of the segment 
     */
    private final MapCoordinates m_end;
    
    /**
     * Start of the segment
     */
    private final MapCoordinates m_start;
    
    /**
     * Normalized rectangle made from the start and end points 
     */
    private final MapRectangle m_bounds;
    
    /**
     * Constructor
     * @param parent XML parent element
     */
    public LineSegment(Element parent)
    {
    	Element colorEl = XMLUtils.getFirstChildElementByTagName(parent, "color");
    	String rgb = XMLUtils.getElementValue(colorEl);
    	Color c;
    	try
    	{
	    	int iRGB = Integer.parseInt(rgb, 8);
	    	c = new Color(iRGB, true);
    	}
    	catch (NumberFormatException e)
    	{
    		c = Color.BLACK;
    	}    	
    	m_color = c;
    	
    	Element startEl = XMLUtils.getFirstChildElementByTagName(parent, "start");
    	m_start = new MapCoordinates(startEl);
    	
    	Element endEl = XMLUtils.getFirstChildElementByTagName(parent, "end");
    	m_end = new MapCoordinates(endEl);
    	
    	m_bounds = new MapRectangle(m_start, m_end);
    }

    /**
     * Constructor
     * @param dis Data input stream to read from
     * @throws IOException
     */
    public LineSegment(final DataInputStream dis) throws IOException
    {
    	m_start = new MapCoordinates(dis.readInt(), dis.readInt());
      m_end   = new MapCoordinates(dis.readInt(), dis.readInt());
      
      final int col = dis.readInt();
      m_color = new Color(col);
      
      m_bounds = new MapRectangle(m_start, m_end);
    }

    /**
     * Constructor
     * @param segment Line segment to copy from
     */
    public LineSegment(final LineSegment segment)
    {
        m_start = segment.m_start;
        m_end = segment.m_end;
        m_color = segment.m_color;
        
        m_bounds = new MapRectangle(m_start, m_end);
    }

    /**
     * Constructor 
     * @param start Start position
     * @param end End position
     * @param color Line color
     */
    public LineSegment(final MapCoordinates start, final MapCoordinates end, final Color color)
    {
        m_start = start;
        m_end = end;
        m_color = color;
        
        m_bounds = new MapRectangle(m_start, m_end);
    }
    
    /**
     * Checks if a given point is on a given rectangle's edge
     * @param pos Coordinates to look for
     * @param rect Rectangle to test against
     * @return Valid coordinates (pos) or null
     */
    private MapCoordinates confirmOnRectEdge(final MapCoordinates pos, final Rectangle rect)
    {
        // garbage in, garbage out
        if (pos == null)
        {
            return null;
        }

        // if the point is within the rect than that counts
        if (rect.contains(pos.x, pos.y))
        {
            return pos;
        }

        // return the point if it's on the edge. null if it isn't
        if ((pos.x == rect.x) || (pos.x == rect.x + rect.width))
        {
            if ((pos.y > rect.y) && (pos.y < rect.y + rect.height))
            {
                return pos;
            }
        }

        if ((pos.y == rect.y) || (pos.y == rect.y + rect.height))
        {
            if ((pos.x > rect.x) && (pos.x < rect.x + rect.width))
            {
                return pos;
            }
        }

        return null;
    }

    /** 
     * Returns a rectangle identifying the space taken by the LineSegment
     * @return 
     */
    public MapRectangle getBounds()
    {
    	return m_bounds;
    }

    /**
     * Returns line segments representing this segment cropped by the box specified by the given coordinates
     * @param start Point defining a rectangle
     * @param end	Point defining a rectangle
     * @return Array of line segments
     */
    public LineSegment[] crop(final MapCoordinates start, final MapCoordinates end)
    {
        final Rectangle r = new Rectangle();
        int x = start.x;
        int y = start.y;
        int width = end.x - start.x;
        int height = end.y - start.y;
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
        r.setBounds(x, y, width, height);

        if (r.contains(m_start.x, m_start.y) && r.contains(m_end.x, m_end.y))
        {
            // totally inside. we dead
            return null;
        }

        // find the intersections (There are 4 possibles.)
        MapCoordinates leftInt = getIntersection(r.x, true);
        MapCoordinates rightInt = getIntersection(r.x + r.width, true);
        MapCoordinates topInt = getIntersection(r.y, false);
        MapCoordinates bottomInt = getIntersection(r.y + r.height, false);

        leftInt = confirmOnRectEdge(leftInt, r);
        rightInt = confirmOnRectEdge(rightInt, r);
        topInt = confirmOnRectEdge(topInt, r);
        bottomInt = confirmOnRectEdge(bottomInt, r);

        // figure out which of our points is leftmost, rightmost, etc.
        MapCoordinates leftMost = m_start;
        MapCoordinates rightMost = m_end;
        if (m_end.x < m_start.x)
        {
            leftMost = m_end;
            rightMost = m_start;
        }

        MapCoordinates topMost = m_start;
        MapCoordinates bottomMost = m_end;
        if (m_end.y < m_start.y)
        {
            topMost = m_end;
            bottomMost = m_start;
        }

        // now we can start making some lines
        final List<LineSegment> returnLines = new ArrayList<LineSegment>();

        // first off, if we didn't intersect the rect at all, it's just us
        if ((leftInt == null) && (rightInt == null) && (topInt == null) && (bottomInt == null))
        {
            returnLines.add(this);
        }

        if (leftInt != null)
        {
        	 returnLines.add( new LineSegment(leftMost, leftInt, m_color));
        }
        if (rightInt != null)
        {
        	returnLines.add( new LineSegment(rightMost, rightInt, m_color));
        }
        if (topInt != null)
        {
        	returnLines.add( new LineSegment(topMost, topInt, m_color));
        }
        if (bottomInt != null)
        {
        	returnLines.add( new LineSegment(bottomMost, bottomInt, m_color));
        }

        if (returnLines.size() == 0)
        {
            // this shouldn't happen, actually.
            // but, play it safe.
            return null;
        }

        final Object[] objArray = returnLines.toArray();
        final LineSegment[] ret = new LineSegment[objArray.length];
        for (int i = 0; i < objArray.length; i++)
        {
            ret[i] = (LineSegment)(objArray[i]);
        }
        return ret;
    }

    /*
    * @see com.galactanet.gametable.ui.MapElementRendererIF#drawInformationOverlayToCanvas(java.awt.Graphics, boolean, com.galactanet.gametable.ui.GametableCanvas)
    */
    @Override
    public void drawInformationOverlayToCanvas(Graphics g, boolean mouseOver, GametableCanvas canvas)
    {
    	// no rendering to do 		
    }
    
    /*
     * @see com.galactanet.gametable.ui.MapElementRendererIF#drawToCanvas(java.awt.Graphics, com.galactanet.gametable.ui.GametableCanvas)
     */
    @Override
    public boolean drawToCanvas(final Graphics g, final GametableCanvas canvas)
    {
        // convert to draw coordinates
        final Point drawStart = canvas.modelToDraw(m_start);
        final Point drawEnd = canvas.modelToDraw(m_end);

        // don't draw if we're not touching the viewport at any point

        // get the draw coords of the top-left of the viewable area and of the lower right
        final Point portalDrawTL = new Point(canvas.getScrollPosition());
        final Point portalDrawBR = new Point(canvas.getScrollX() + canvas.getWidth(), canvas.getScrollY()
            + canvas.getHeight());
        final Rectangle portalRect = new Rectangle((int)portalDrawTL.getX(), (int)portalDrawTL.getY(),
            (int)portalDrawBR.getX() - (int)portalDrawTL.getX(), (int)portalDrawBR.getY() - (int)portalDrawTL.getY());

        // now we can start comparing to see if the line is in the rect at all
        boolean bDrawLine = false;

        // first off, if either terminus point is in the box, draw the line.
        if (portalRect.contains(drawStart))
        {
            bDrawLine = true;
        }
        else if (portalRect.contains(drawEnd))
        {
            bDrawLine = true;
        }
        else
        {
            // neither point is inside the rect. Now we have to do things the slightly harder way.
            // presume it IS in view, now and work backward
            bDrawLine = true;

            // if both ends of the line are on the outside of one of the walls of the visible
            // area, it can't possibly be onscrene. For instance, if both endpoints'
            // x-values are less than the wall's left side, it can't be on screen.
            if ((drawStart.getX() < portalRect.getX()) && (drawEnd.getX() < portalRect.getX()))
            {
                // the line segment is entirely on the left. don't draw it
                return false;
            }
            if ((drawStart.getX() > portalRect.getX() + portalRect.getWidth())
                && (drawEnd.getX() > portalRect.getX() + portalRect.getWidth()))
            {
                // the line segment is entirely on the right. don't draw it
            	return false;
            }
            if ((drawStart.getY() < portalRect.getY()) && (drawEnd.getY() < portalRect.getY()))
            {
                // the line segment is entirely above. don't draw it
            	return false;
            }
            if ((drawStart.getY() > portalRect.getY() + portalRect.getHeight())
                && (drawEnd.getY() > portalRect.getY() + portalRect.getHeight()))
            {
                // the line segment is entirely below. don't draw it
            	return false;
            }

            // if we're here, it means the line segment:
            // 1) Has neither point within the rect
            // 2) Has points on both sides of a vertical or horizontal wall.

            // in some cases, there will be lines told to draw that didn't need to (if they
            // intersect
            // a vertical or horizontal line that is colinear with one of the edges of the viewable
            // rect).
            // but that inefficiency is preferable to the slope-intersection calculations needed to
            // check
            // to see if the line
        }

        if (!bDrawLine)
        {
        	return false;
        }

        g.setColor(m_color);

        final int width = canvas.getLineStrokeWidth();
        final int halfWidth = width / 2;

        final int dx = Math.abs(m_end.x - m_start.x);
        final int dy = Math.abs(m_end.y - m_start.y);

        int nudgeX = 0;
        int nudgeY = 0;
        if (dx > dy)
        {
            // vertical doubling
            nudgeY = 1;
        }
        else
        {
            // horizontal doubling
            nudgeX = 1;
        }

        int x1 = drawStart.x - nudgeX * halfWidth;
        int y1 = drawStart.y - nudgeY * halfWidth;
        int x2 = drawEnd.x - nudgeX * halfWidth;
        int y2 = drawEnd.y - nudgeY * halfWidth;

        for (int i = 0; i < width; i++)
        {
            g.drawLine(x1, y1, x2, y2);
            x1 += nudgeX;
            y1 += nudgeY;
            x2 += nudgeX;
            y2 += nudgeY;
        }
        
        return true;
    }

    /**
     * @return This line segment's color
     */
    public Color getColor()
    {
        return m_color;
    }
    
    /**
     * Calculates the intersection of this line segment with a given pure vertical or horizontal. 
     * @param pos Either the x or the y, depending on the boolean sent with it.
     * @param vertical Determines the orientation of 'pos'
     * @return Intersecting coordinates or null, if no intersection is found
     */
    public MapCoordinates getIntersection(final int pos, final boolean vertical)
    {
        if (vertical)
        {
            if ((m_start.x < pos) && (m_end.x < pos))
            {
                // completely on one side of it.
                return null;
            }
            if ((m_start.x > pos) && (m_end.x > pos))
            {
                // completely on the other side of it.
                return null;
            }
            if (m_end.x == m_start.x)
            {
                // we're parallel to it so we don't intersect at all
                return null;
            }

            // if we're here, we cross the line
            final double ratio = ((double)(pos - m_start.x)) / (double)(m_end.x - m_start.x);
            final double intersectX = pos;
            final double intersectY = m_start.y + ratio * (m_end.y - m_start.y);
            final MapCoordinates ret = new MapCoordinates((int)intersectX, (int)intersectY);
            
            return ret;
        }

        if ((m_start.y < pos) && (m_end.y < pos))
        {
            // completely on one side of it.
            return null;
        }
        if ((m_start.y > pos) && (m_end.y > pos))
        {
            // completely on the other side of it.
            return null;
        }
        if (m_end.y == m_start.y)
        {
            // we're parallel to it so we don't intersect at all
            return null;
        }

        // if we're here, we cross the line
        final double ratio = ((double)(pos - m_start.y)) / (double)(m_end.y - m_start.y);
        final double intersectY = pos;
        final double intersectX = m_start.x + ratio * (m_end.x - m_start.x);
        final MapCoordinates ret = new MapCoordinates((int)intersectX, (int)intersectY);
        
        return ret;
    }

    /**
     * Calculates the part of this line segment that is within the box defined by two give sets of coordinates
     * @param start Coordinates defining a rectangle
     * @param end Coordinates defining a rectangle
     * @return Cropped line segment, or null if no portion of this segment fits the box
     */
    public LineSegment getPortionInsideRect(final MapCoordinates start, final MapCoordinates end)
    {
        final Rectangle r = new Rectangle();
        int x = start.x;
        int y = start.y;
        int width = end.x - start.x;
        int height = end.y - start.y;
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
        r.setBounds(x, y, width, height);

        if (r.contains(m_start.x, m_start.y) && r.contains(m_end.x, m_end.y))
        {
            // totally inside. we are unaffected
            // return a copy of ourselves
            return new LineSegment(this);
        }

        // find the intersections (There are 4 possibles. one for each side of the rect)
        final MapCoordinates intersections[] = new MapCoordinates[4];
        intersections[0] = getIntersection(r.x, true);
        intersections[1] = getIntersection(r.x + r.width, true);
        intersections[2] = getIntersection(r.y, false);
        intersections[3] = getIntersection(r.y + r.height, false);

        boolean bFoundNonNull = false;
        for (int i = 0; i < 4; i++)
        {
            intersections[i] = confirmOnRectEdge(intersections[i], r);
            if (intersections[i] != null)
            {
                bFoundNonNull = true;
            }
        }

        // first off, if we didn't intersect the rect at all, we have no part at all
        // (We checked for "completely inside rect" above
        if (!bFoundNonNull)
        {
            return null;
        }

        // we can have no more than 2 intersections.
        MapCoordinates validIntersection1 = null;
        MapCoordinates validIntersection2 = null;
        for (int i = 0; i < 4; i++)
        {
            if (intersections[i] != null)
            {
                if (validIntersection1 == null)
                {
                    validIntersection1 = intersections[i];
                }
                else
                {
                    validIntersection2 = intersections[i];
                }
            }
        }

        // did we find 2 intersections? Cause if we did, we're done
        if (validIntersection2 != null)
        {
            // we found 2 intersections. Make a LineSegment out of them and we're golden
            return new LineSegment(validIntersection1, validIntersection2, m_color);
        }

        // if we're here, it means we found exactly 1 intersection. That means our start or end point
        // is inside the rect.
        if (r.contains(m_start.x, m_start.y))
        {
            return new LineSegment(validIntersection1, m_start, m_color);
        }

        if (r.contains(m_end.x, m_end.y))
        {
            return new LineSegment(validIntersection1, m_end, m_color);
        }

        // it should be impossible to get here.
        System.out.println("invalid end to LineSegment.getPortionInsideRect");
        
        return null; // defensive coding return
    }

    /**
     * Writes information to data output stream
     * @param dos
     * @throws IOException
     */
    public void writeToPacket(final DataOutputStream dos) throws IOException
    {
        dos.writeInt(m_start.x);
        dos.writeInt(m_start.y);
        dos.writeInt(m_end.x);
        dos.writeInt(m_end.y);
        dos.writeInt(m_color.getRGB());
    }

    /*
     * @see com.galactanet.gametable.data.XMLSerializeIF#deserialize(org.w3c.dom.Element, com.galactanet.gametable.data.XMLSerializeConverter)
     */
    @Override
    public void deserialize(Element parent, XMLSerializeConverter converter)
    { 
    	throw new NotImplementedException();    	
    }
    
    /*
     * @see com.galactanet.gametable.data.XMLSerializer#serialize(org.w3c.dom.Element)
     */
    @Override
    public void serialize(Element parent)
    {
    	Document doc = parent.getOwnerDocument();
    	Element colorEl = doc.createElement("color");
    	colorEl.appendChild(doc.createTextNode(Integer.toHexString(m_color.getRGB())));
    	parent.appendChild(colorEl);
    	
    	Element startEl = doc.createElement("start");
    	m_start.serialize(startEl);
    	parent.appendChild(startEl);
    	
    	Element endEl = doc.createElement("end");
    	m_end.serialize(endEl);
    	parent.appendChild(endEl);
    }
}
