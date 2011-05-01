/*
 * GridMode.java: GameTable is in the Public Domain.
 */


package com.gametable.data.grid;

import java.awt.Color;
import java.awt.Graphics2D;

import com.gametable.data.GameTableMap;
import com.gametable.data.GridMode;
import com.gametable.data.MapCoordinates;
import com.gametable.ui.GametableCanvas;



/**
 * 
 * @author sephalon
 * 
 * #GT-AUDIT SquareGridMode
 */
public class SquareGridMode extends GridMode
{
    public SquareGridMode()
    {
    }

    /*
    * @see com.gametable.data.GridMode#drawLines(java.awt.Graphics2D, int, int, int, int)
    */
    @Override
    public void drawLines(Graphics2D g, int topLeftX, int topLeftY, int width, int height, GametableCanvas canvas)    
    {
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

        final int tilingSquareX = canvas.getTileSize();
        final int tilingSquareY = tilingSquareX;	// its a square, so same value

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

        g.setColor(Color.GRAY);

        // draw a square grid
        int squareSize = canvas.getTileSize();
        
        if (canvas.getZoomLevel() < 4)
        {
            for (int i = 0; i < vLines; i++)
            {
                g.drawLine(i * squareSize + linesXOffset, topLeftY,
                    i * squareSize + linesXOffset, height + topLeftY);
            }
            for (int i = 0; i < hLines; i++)
            {
                g.drawLine(topLeftX, i * squareSize + linesYOffset, width + topLeftX, i
                    * squareSize + linesYOffset);
            }
        }
    }

    @Override
		public MapCoordinates getSnappedMapCoordinates(final MapCoordinates modelPointIn, final boolean bSnapForPog, final int pogSize)
    {
        int x = getSnappedMapCoordinates(modelPointIn.x);
        int y = getSnappedMapCoordinates(modelPointIn.y);
        // Use old behavior when we're dealing with a pog.
        if( bSnapForPog )
        {
            return new MapCoordinates(x, y);
        }
        // Otherwise allow center snapping as well.
        MapCoordinates closest = null;
        final MapCoordinates candidates[] = new MapCoordinates[5];
        int foo = GameTableMap.getBaseTileSize()/2;
        candidates[0] = new MapCoordinates( x, y ); // Me
        candidates[1] = new MapCoordinates( x-foo, y-foo ); // Nearby center
        candidates[2] = new MapCoordinates( x+foo, y-foo ); // Nearby center
        candidates[3] = new MapCoordinates( x-foo, y+foo ); // Nearby center
        candidates[4] = new MapCoordinates( x+foo, y+foo ); // Nearby center
        
        closest = getClosestPoint(modelPointIn, candidates);
        
        if (closest == null)
        {
            System.out.println("Error snapping to point");
            return new MapCoordinates(x, y);
        }

        return closest;
    }


    private MapCoordinates getClosestPoint(final MapCoordinates target, final MapCoordinates candidates[])
    {
        double minDist = -1.0;
        MapCoordinates winner = null;
        
        for (int i = 0; i < candidates.length; i++)
        {
            final double distance = target.distance(candidates[i]);
            if ((minDist == -1.0) || (distance < minDist))
            {
                minDist = distance;
                winner = candidates[i];
            }
        }

        return winner;
    }
}
