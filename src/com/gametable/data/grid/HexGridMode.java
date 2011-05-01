/*
 * GridMode.java: GameTable is in the Public Domain.
 */


package com.gametable.data.grid;

import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;

import com.gametable.GametableApp;
import com.gametable.data.GameTableMap;
import com.gametable.data.GridMode;
import com.gametable.data.MapCoordinates;
import com.gametable.ui.GametableCanvas;
import com.gametable.ui.GametableFrame;
import com.gametable.util.ImageCache;



/**
 * @author sephalon
 * 
 * #GT-AUDIT HexGridMode
 */
public class HexGridMode extends GridMode
{
    private final int[]   m_hexImageOffsets = new int[GametableFrame.MAX_ZOOM_LEVEL];  // how far

    // data
    private final Image[] m_hexImages       = new Image[GametableFrame.MAX_ZOOM_LEVEL]; // one hex
    
    private boolean m_initialized = false;

    public HexGridMode()
    {
    }

    /*
    * @see com.gametable.data.GridMode#drawLines(java.awt.Graphics2D, int, int, int, int)
    */
    @Override
    public void drawLines(Graphics2D g, int topLeftX, int topLeftY, int width, int height, GametableCanvas canvas)
    {
    	GametableFrame frame = GametableApp.getUserInterface();
    	
        if (canvas.getZoomLevel() == 4)
        {
            // we don't draw lines at the furthest zoom level
            return;
        }
        // This code works out which is the first square to draw on the visible
        // portion of the map, and how many to draw

        // we are "tiling" an image across the visible area. In the case of square mode,
        // we do this just by drawing lines. For hexes we have an actual image to tile.
        // A trick here we have to deal with is that hexes are not horizontally interchangeable
        // across one unit size. That is to say: If you shift a hex map over 1 hex width to the
        // left or right, it will not look the same as it used to. Because the hexes in row N
        // are 1/2 a hex higher than the nexes in row N-1 and row N+1. Because of this, when in hex
        // mode,
        // we have to make our "tiling square size" twice as wide.

        int tilingSquareX = canvas.getTileSize();
        final int tilingSquareY = tilingSquareX;	// its a square, so same value
        tilingSquareX *= 2;

        int qx = Math.abs(topLeftX) / tilingSquareX;
        if (topLeftX < 0)
        {
            qx++;
            qx = -qx;
        }

        int qy = Math.abs(topLeftY) / tilingSquareY;
        if (topLeftY < 0)
        {
            qy++;
            qy = -qy;
        }

        final int linesXOffset = qx * tilingSquareX;
        final int linesYOffset = qy * tilingSquareY;
        final int vLines = width / tilingSquareX + 2;
        final int hLines = height / tilingSquareY + 2;
        
        int zoomLevel = canvas.getZoomLevel();

        // draw a hex grid
        final Image toTile = m_hexImages[zoomLevel];

        // this offsets the hexes to be "centered" in the square grid that would
        // be there if we were in square mode (Doing things this way means that the
        // x-position treatment of pogs doesn't have to change while in hex mode.
        final int offsetX = -m_hexImageOffsets[zoomLevel] / 2;

        // each tiling of hex images is 4 hexes high. so we incrememt by 4
        for (int j = 0; j < hLines; j += 4)
        {
            // every "tiling" of the hex map draws 4 vertical rows of hexes
            // that works out to be 2 tileSquare sizes (cause each tileSquare width is
            // 2 columns of hexes.
            for (int i = 0; i < vLines; i += 2)
            {
                // the x value:
                // starts at the "linesXOffset" calculated at the top of this routine.
                // that represents the first vertical column of hexes visible on screen.
                // add to that i*m_squareSize, to offset horizontally as we traverse the loop.
                // then add our little offsetX, whose purpose is described in it's declaration.
                final int x = linesXOffset + i * tilingSquareX + offsetX;

                // the y location is much the same, except we need no x offset nudge.
                final int y = linesYOffset + j * tilingSquareY;
                g.drawImage(toTile, x, y, frame);
            }
        }
    }

    private MapCoordinates getClosestPoint(final MapCoordinates target, final MapCoordinates candidates[])
    {
        double minDist = -1.0;
        MapCoordinates winner = null;
        
        for (MapCoordinates candidate : candidates)
        {
            double distance = target.distance(candidate);
            if ((minDist == -1.0) || (distance < minDist))
            {
                minDist = distance;
                winner = candidate;
            }
        }

        return winner;
    }

    // overrides. We don't have the same scale in x as we do in y
    // the y is still 1.0, so we don't override it. But the X is a different story
    @Override
		public double getDistanceMultplierX()
    {
        return 0.866;
    }

    
    
    /*
     * @see com.gametable.data.GridMode#initialize()
     */
    @Override
		public synchronized void initialize()
    {
    	if (m_initialized)
    		return;
    	
    	m_initialized = true;
    	
        // .todo resources in a jar file.  Loading only when necessary.
        
        // init the hex images
        m_hexImages[0] = ImageCache.getImage(new File("assets/hexes_64.png"));
        m_hexImages[1] = ImageCache.getImage(new File("assets/hexes_48.png"));
        m_hexImages[2] = ImageCache.getImage(new File("assets/hexes_32.png"));
        m_hexImages[3] = ImageCache.getImage(new File("assets/hexes_16.png"));
        m_hexImages[4] = null; // no lines are drawn at this zoom level. So there's no hex image
        // for it.

        // magic numbers - these represent the distance in along the x-axis that the corner
        // of the hex is.
        // Note that the top left corner of the first hex is not aligned with the left of
        // the image
        // | ----------
        // | /
        // |/
        // |\
        // | \
        // | ----------
        // That distance is what is represented here.

        m_hexImageOffsets[0] = 19;
        m_hexImageOffsets[1] = 15;
        m_hexImageOffsets[2] = 10;
        m_hexImageOffsets[3] = 5;
        m_hexImageOffsets[4] = 0; // irrelevant. There is no image for this level. Lines aren't
        // drawn at this zoom level.
    }

    private boolean isOffsetColumn(final int col)
    {
        int columnNumber = col;
        if (columnNumber < 0)
        {
            columnNumber = -columnNumber;
        }
        if (columnNumber % 2 == 1)
        {
            return true;
        }
        return false;
    }

    @Override
		public MapCoordinates getSnappedMapCoordinates(final MapCoordinates modelPointIn, final boolean bSnapForPog, final int pogSize)
    {
    	MapCoordinates modelPoint = modelPointIn;
    	
        if (bSnapForPog)
        {
          // we're snapping for a pog. We've been sent the upper left corner of that
          // pog. We need to know it's center.
        	modelPoint = modelPoint.delta(pogSize / 2, pogSize / 2);
        }

        // in hex mode, we have to snap to any of the vertices of a hex,
        // plus the center. How annoying is that, eh?

        // start with the grid snap location for the x coordinate
        int x = getSnappedMapCoordinates(modelPoint.x);
        
        int baseSquareSize = GameTableMap.getBaseTileSize();

        // from that, get the grid location.
        final int gridX = x / baseSquareSize;

        // note that items in the odd columns are half a grid square down
        int offsetY = 0;
        if (isOffsetColumn(gridX))
        {
            offsetY = baseSquareSize / 2;
        }

        // now work out which "grid" (hex, really) the y value is in
        int y = getSnappedMapCoordinates(modelPoint.y - offsetY);

        // add back the offset
        y += offsetY;

        // add in the x offset needed to put it on the corner of the hex
        x += m_hexImageOffsets[0] / 2; // [0] is the model coordinate size

        // let's number the hexagon points 0 through 5. Let's number them
        // clockwise starting from the upper left one. What we have done so
        // far is snap to the nearest point 0. That's not good enough.
        // There are 3 hexagon points adjacent to a point 0 that are not
        // other hexagon point 0's. And we might be closer to one of them
        // than to the point we just snapped to.
        // so we now have 4 "candidates" for nearest point. The point 0 we just
        // found, and the other three points nearby. Those other three points
        // will be:
        //
        // --Our hex's point 1
        // --Our hex's point 5
        // --Our upstairs neighbor's point 5
        //
        // In addition to that, there are 3 hex centers we need to check:
        // --Our hex center
        // --Our upstairs neighbor's hex center
        // -- Our neighbor to the left's hex center

        MapCoordinates closest = null;

        if (bSnapForPog)
        {
            // we're snapping to valid pog locations. We have been sent the
            // upp left corner of the graphic. We converted that to the center already.
            // now, we need to stick that to either vertices or hex centers, depending
            // on the size of the pog. If it's a size 1 (64 px_ pog, we snap to centers only.
            // if it's size 2, we snap to vertices only. If size 3, back to centers. etc.
            final int face = pogSize / baseSquareSize;
            if (face % 2 == 1)
            {
                // odd faces snap to centers
                final MapCoordinates candidates[] = new MapCoordinates[3];

                final MapCoordinates point1 = new MapCoordinates(x + baseSquareSize - m_hexImageOffsets[0], y); // Our
                // hex's
                // point
                // 1,
                // for
                // use
                // in
                // calculating
                // the
                // center

                candidates[0] = new MapCoordinates(x + (point1.x - x) / 2, y + baseSquareSize / 2); // Our
                // hex
                // center
                candidates[1] = new MapCoordinates(candidates[0].x, candidates[0].y - baseSquareSize); // Our
                // upstairs
                // neighbor's
                // center
                candidates[2] = new MapCoordinates(candidates[0].x - baseSquareSize, candidates[0].y
                    - baseSquareSize / 2); // Our upstairs neighbor's center

                closest = getClosestPoint(modelPoint, candidates);
            }
            else
            {
                // even faces snap to vertices
                final MapCoordinates candidates[] = new MapCoordinates[4];
                candidates[0] = new MapCoordinates(x, y); // Our hex's point 0
                candidates[1] = new MapCoordinates(x + baseSquareSize - m_hexImageOffsets[0], y); // Our
                // hex's
                // point
                // 1
                candidates[2] = new MapCoordinates(x - m_hexImageOffsets[0], y + baseSquareSize / 2); // Our
                // hex's
                // point
                // 5
                candidates[3] = new MapCoordinates(candidates[2].x, candidates[2].y - baseSquareSize); // Our
                // upstairs
                // neighbor's
                // point
                // 5

                closest = getClosestPoint(modelPoint, candidates);
            }

            if (closest != null)
            {
                // offset the values for the pog size
            	closest = closest.delta(-pogSize / 2, -pogSize / 2);
            }
        }
        else
        {
            // we're snapping to any vertex
            final MapCoordinates candidates[] = new MapCoordinates[7];
            candidates[0] = new MapCoordinates(x, y); // Our hex's point 0
            candidates[1] = new MapCoordinates(x + baseSquareSize - m_hexImageOffsets[0], y); // Our
            // hex's
            // point
            // 1
            candidates[2] = new MapCoordinates(x - m_hexImageOffsets[0], y + baseSquareSize / 2); // Our
            // hex's
            // point
            // 5
            candidates[3] = new MapCoordinates(candidates[2].x, candidates[2].y - baseSquareSize); // Our
            // upstairs
            // neighbor's
            // point
            // 5
            candidates[4] = new MapCoordinates(candidates[0].x + (candidates[1].x - candidates[0].x) / 2, y
                + baseSquareSize / 2); // Our hex center
            candidates[5] = new MapCoordinates(candidates[4].x, candidates[4].y - baseSquareSize); // Our
            // upstairs
            // neighbor's
            // center
            candidates[6] = new MapCoordinates(candidates[4].x - baseSquareSize, candidates[4].y
                - baseSquareSize / 2); // Our
            // upstairs
            // neighbor's
            // center

            closest = getClosestPoint(modelPoint, candidates);
        }

        if (closest == null)
        {
            // uh... if we're here something went wrong
            // defensive coding, just return that nearest Point 0
            System.out.println("Error snapping to point");
            return new MapCoordinates(x, y);
        }
        return closest;
    }
}
