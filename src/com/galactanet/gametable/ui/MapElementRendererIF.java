/*
 * MapElementRendererIF.java
 *
 * @created 2010-06-19
 *
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.galactanet.gametable.ui;

import java.awt.Graphics;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public interface MapElementRendererIF
{
	/**
	 * Renders an element onto the canvas
	 * @param g Graphics device to render into
	 * @param canvas Canvas to get scaling information.  If canvas is null, no scaling should be done.
	 */
  public void drawToCanvas(Graphics g, GametableCanvas canvas);

  /**
   * Draw the information overlay to the canvas.  Information overlay is optional information normally displayed
   * on mouse over or when user turns on display.
   *  
   * @param g graphics device
   * @param mouseover true if mouse is over the element
   * @param canvas
   */
  public void drawInformationOverlayToCanvas(final Graphics g, final boolean mouseOver, GametableCanvas canvas);
  

}
