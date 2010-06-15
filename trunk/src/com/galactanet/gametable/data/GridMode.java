/*
 * GridMode.java
 * 
 * @created 2005-12-19
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

import java.awt.Graphics2D;
import java.awt.Point;

import com.galactanet.gametable.ui.GametableCanvas;

/**
 * @author sephalon
 * 
 * @audited by themaze75
 */
public class GridMode
{
	/**
	 * Canvas using this grid mode
	 */
	protected GametableCanvas	m_canvas;

	/**
	 * Draws the lines to the canvas. Assumes there is a properly offset graphics object passed in
	 * 
	 * @param g graphics device to draw to
	 * @param topLeftX x Coordinate of the region we need to draw lines in
	 * @param topLeftY y Coordinate of the region we need to draw lines in
	 * @param width Width of the region we need to draw lines in
	 * @param height Height of the region we need to draw lines in
	 */
	public void drawLines(Graphics2D g, int topLeftX, int topLeftY, int width, int height)
	{
		// default behavior is to not draw anything
	}

	/**
	 * Get the distance, in squares, between two set of map coordinates.
	 * 
	 * Base implementation does basic hypotenuse trig.
	 * 
	 * @param x1 x Coordinate of point 1
	 * @param y1 y Coordinate of point 1
	 * @param x2 x Coordinate of point 2
	 * @param y2 y Coordinate of point 2
	 * @return Calculated distance, based on modifier
	 */
	public double getDistance(final int x1, final int y1, final int x2, final int y2)
	{
		// Calculate distance deltas
		int dx = x2 - x1;
		int dy = y2 - y1;

		return Math.sqrt(Math.pow(dx * getDistanceMultplierX(), 2) * Math.pow(dy * getDistanceMultplierY(), 2));
	}

	/**
	 * Get multiplier for x coordinates when calculating distance. Some grids might not have the same scale in the x
	 * direction as they do in the y
	 * 
	 * @return Multiplier
	 */
	public double getDistanceMultplierX()
	{
		return 1.0d;
	}

	/**
	 * Get multiplier for y coordinates when calculating distance. Some grids might not have the same scale in the x
	 * direction as they do in the y
	 * 
	 * @return Multiplier
	 */
	public double getDistanceMultplierY()
	{
		return 1.0d;
	}

	/**
	 * Initialize the grid mode
	 * 
	 * @param canvas GameTableCanvas to link to the grid mode
	 */
	public void init(GametableCanvas canvas)
	{
		m_canvas = canvas;
	}

	/**
	 * Updates a pog's position and place it on its proper 'snapped' position on the grid.
	 *  
	 * @param pog Pog to reposition
	 * 
	 */
	public void snapPogToGrid(final Pog pog)
	{
		Point snappedPoint = getSnappedPixelCoordinates(pog.getPosition(), true, GametableCanvas.BASE_SQUARE_SIZE * pog.getFaceSize());


		pog.setPosition(snappedPoint.x, snappedPoint.y);
	}

	/**
	 * Snaps a given set of pixel coordinates to the grid.
	 * 
	 * @param pixelCoordinates Set of pixel coordinates to snap to grid
	 * 
	 * @return new set of pixel coordinates, snapped to grid
	 */
	public Point getSnappedPixelCoordinates(final Point pixelCoordinates)
	{
		return getSnappedPixelCoordinates(pixelCoordinates, false, 0);
	}

	/**
	 * Snaps a given set of pixel coordinates to the grid.
	 *   
	 * @param pixelCoordinates Set of pixel coordinates to snap to grid
	 * 
	 * @param bSnapForPog If true, will return snap locations where a pog of the sent in size could snap to. Note this is
	 *          not the same as ANY snap points, cause you don't want your pogs snapping to the vertex of a hex.
	 *          
	 * @param pogSize Pog's size in pixels. Ignored if bSnapForPog is false.
	 * 
	 * @return new set of pixel coordinates, snapped to grid
	 */
	public Point getSnappedPixelCoordinates(final Point pixelCoordinates, final boolean bSnapForPog, final int pogSize)
	{
		// default behavior is to not snap at all.
		return new Point(pixelCoordinates);
	}

	/**
	 * Snaps a given set of pixel coordinates to the grid.
	 *   
	 * @param pixelCoordinate A pixel coordinate to snap to grid. Either X or Y - the model assumes the scale is the same either way.
	 * 
	 * @return New pixel coordinate, snapped to grid.
	 */
	protected int getSnappedPixelCoordinates(final int pixelCoordinate)
	{
		// NB : The second division is made as integer, actually rounding the pixel coordinates to map coordinates.  
		// The following multiplication converts back to pixels after rounding. 
		
		if (pixelCoordinate < 0)
			return ((pixelCoordinate - GametableCanvas.BASE_SQUARE_SIZE / 2) / GametableCanvas.BASE_SQUARE_SIZE) * GametableCanvas.BASE_SQUARE_SIZE;

		return ((pixelCoordinate + GametableCanvas.BASE_SQUARE_SIZE / 2) / GametableCanvas.BASE_SQUARE_SIZE) * GametableCanvas.BASE_SQUARE_SIZE;
	}
}
