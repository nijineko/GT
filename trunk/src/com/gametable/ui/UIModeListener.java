/*
 * UIModeIF.java
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

package com.gametable.ui;

import java.awt.event.InputEvent;

/**
 * TODO audit
 * 
 * Listener interface used by UIModes to react to user events
 * 
 * @author iffy
 */
public interface UIModeListener 
{
  /**
   * Called by UI when the mouse button is pressed on the map.
   * 
   * @param canvas Game table canvas (holds UI to work with pixels)
   * @param mouseY The x location of the mouse on the map when the button was pressed (view coordinates) .
   * @param mouseX The Y location of the mouse on the map when the button was pressed (view coordinates) .
   * @param modifierMask The mask of modifier keys held during this event.  See {@link InputEvent#getModifiersEx()}
   */
  void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, int modifierMask);

  /**
   * Called by UI when the mouse button is released on the map.
   * 
   * @param canvas Game table canvas (holds UI to work with pixels)
   * @param mouseX The x location of the mouse on the map component when the button was released (view coordinates) .
   * @param mouseY The x location of the mouse on the map component when the button was released (view coordinates) .
   * @param modifierMask The mask of modifier keys held during this event.  See {@link InputEvent#getModifiersEx()}
   */
  void mouseButtonReleased(GametableCanvas canvas, int mouseX, int mouseY, int modifierMask);

  /**
   * Called by UI when the mouse is moved around on the map.
   * 
   * @param canvas Game table canvas (holds UI to work with pixels)
   * @param mouseX The x location of the mouse on the map component (view coordinates) 
   * @param mouseY The x location of the mouse on the map component (view coordinates) 
   * @param modifierMask The mask of modifier keys held during this event.  See {@link InputEvent#getModifiersEx()}
   */
  void mouseMoved(GametableCanvas canvas, int mouseX, int mouseY, int modifierMask);
  
  /**
   * Called by the UI when the mode is changed to this mode
   */
  void selectMode();

  /**
   * Called by the UI when this mode's action has been canceled.
   * A "roll back" of any "non-commited" changes is expected upon this call.
   */
  void cancelMode();
}
