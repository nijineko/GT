/*
 * UIMode.java
 *
 * @created 2011-01-10
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

package com.galactanet.gametable.ui;

import java.awt.Graphics2D;


/**
 * TODO audit
 * Determines a mode in which the user interface can be set.
 * 
 * Ex: "normal" mode handles selection and basic manipulation of map elements.  
 *     While in "Box Mode", the user is creating a new "box" object.
 *     
 * The user interface can only be in one mode.
 *  
 * 
 * @author Eric Maziade
 */
public abstract class UIMode 
{
	/**
	 * Protected constructor - use factory to get instance of an UI mode
	 * @param name
	 */
	protected UIMode(String name)
	{
		m_name = name;
	}
//	
//	/**
//	 * Requests that the ongoing operation be canceled (normally due to a change of mode or change of map)
//	 */
//	public abstract void cancelOperation();
	
	/**
	 * String identifying the UI mode.
	 * @return
	 */
	public final String getModeName()
	{
		return m_name;
	}
	
	/**
	 * Returns 'true' if the mode is considered "active".
	 * 
	 * A mode is active if the user is in the middle of performing an action related to the mode.
	 * 
	 * Fox example, if the user is moving map elements around (during a drag operation), or while drawing a box. 
	 * 
	 * @return true / false
	 */
	public abstract boolean isActive();
	
	/**
   * Called by the painting engine on the active UI mode as the topmost layer.  Allows an UI mode implementation to 
   * draw its current status on the map.
   *      
   * @param g GraphicsDevice on which to paint
   * @param canvas GameTableCanvas object to use to specialized painting functions
   */
  protected abstract void paintTool(Graphics2D g, GametableCanvas canvas);
  
  /**
	 * Holds the name of the mode (internal representation)
	 */
	private final String m_name;
}
