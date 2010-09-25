/*
 * NetworkParametersPanel.java
 *
 * @created 2010-09-06
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

package com.galactanet.gametable.net;

import javax.swing.JPanel;

/**
 * Panel holding network-implementation specific parameters configuration UI 
 *
 * @author Eric Maziade
 */
public abstract class NetworkParametersPanel extends JPanel
{
	/**
	 * Asks the panel to read and set its default values.
	 */
	public abstract void setDefautValues();
	
	/**
	 * Asks the panel to process set values and set them in the network module.
	 */
	public abstract void processValues();
	
	/**
	 * Asks the panel to validate its values.  The panel should do its own error handling display.
	 * @return true if the values are valid, false if they are invalid
	 */
	public abstract boolean validateValues();
}
