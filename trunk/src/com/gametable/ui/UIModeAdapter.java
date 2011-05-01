/*
 * UIModeAdapter.java
 *
 * @created 2011-03-06
 *
 * Copyright (C) 1999-2011 Open Source Game Table Project
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

package com.gametable.ui;


/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class UIModeAdapter implements UIModeListener
{

	/*
	 * @see com.gametable.ui.UIModeListener#cancelMode()
	 */
	@Override
	public void cancelMode() {}

	/*
	 * @see com.gametable.ui.UIModeListener#mouseButtonPressed(com.gametable.data.MapCoordinates, int)
	 */
	@Override
	public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, int modifierMask) {}

	/*
	 * @see com.gametable.ui.UIModeListener#mouseButtonReleased(com.gametable.data.MapCoordinates, int)
	 */
	@Override
	public void mouseButtonReleased(GametableCanvas canvas, int mouseX, int mouseY, int modifierMask) {}

	/*
	 * @see com.gametable.ui.UIModeListener#mouseMoved(com.gametable.data.MapCoordinates, int)
	 */
	@Override
	public void mouseMoved(GametableCanvas canvas, int mouseX, int mouseY, int modifierMask) {}
	
	/*
	 * @see com.gametable.ui.UIModeListener#selectMode()
	 */
	@Override
	public void selectMode() {}	
}
