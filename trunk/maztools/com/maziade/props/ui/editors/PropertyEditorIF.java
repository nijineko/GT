/*
 * PropertyEditorIF.java
 * 
 * @created 2006
 * 
 * Copyright (C) 1999-2011 Eric Maziade
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
package com.maziade.props.ui.editors;

import javax.swing.JComponent;

import com.maziade.props.ui.XPropertyTableRow;

/**
 * @author Eric Maziade
 * 
 * Interface exposing functionalities relevant to property editors
 *
 */
public interface PropertyEditorIF
{
	/**
	 * Initialize the property editor with the given property
	 * @param property Initial property
	 */
	public void init(XPropertyTableRow propertyRow);
	
	/**
	 * Get the editor UI
	 * @return component
	 */
	public JComponent getEditorComponent();
	
	/**
	 * Validate data in the editor.
	 * Also gives you a chance to add an error message string to the property row
	 * @return false if data is invalid
	 */
	public boolean isDataValid();
	
	/**
	 * Moves data from the editor component to the property
	 */
	public void commit();	
}
