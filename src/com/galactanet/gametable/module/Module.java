/*
 * ModuleIF.java
 *
 * @created 2010-07-07
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

package com.galactanet.gametable.module;

import org.w3c.dom.Element;

import com.galactanet.gametable.data.XMLSerializeConverter;


/**
 * Base abstract class for modules
 *
 * @author Eric Maziade
 */
public abstract class Module
{
	/**
	 * Get a unique identifier for the module.  Usually the complete base class name for the module.
	 * @return String
	 */
	public abstract String getModuleName();
	
	/**
	 * Initialize the user interface - this is the opportunity to add custom UI to the environment.
	 */
	public void onInitializeUI() {}
	
	/**
	 * Notifies that the interface has switched to or from public map
	 * @param publicMap true if we have switched to public map
	 */
	public void onToggleActiveMap(boolean publicMap) {}
	
	/**
	 * Load from an XML DOM node
	 * @param node An XML node located by the engine.
	 * @param converter to convert stored MapElementIDs to actual map element IDs
	 */
	public void loadFromXML(Element node, XMLSerializeConverter converter) {}
	
	/**
	 * Save to an XML DOM node
	 * @param node An XML node created by the engine.  Data can be added to the node, but the node's attributes should not be modified by modules.
	 * @return false if There is nothing to save (node will be discarded)
	 */
	public boolean saveToXML(Element node) { return false; }
	
	/**
	 * Verifies if this module can load and save to XML (save and load will not be called if this returns false)
	 */
	public boolean canSaveToXML() { return false; }
	
}

