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

package com.gametable.module;

import javax.swing.JMenu;

import org.w3c.dom.Element;

import com.gametable.data.GameTableCore;
import com.gametable.data.Player;
import com.gametable.data.XMLSerializeConverter;
import com.gametable.net.NetworkEvent;
import com.gametable.ui.GametableFrame;
import com.maziade.props.XProperties;


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
	 * Initialize UI components only - use onInitializeCore for non-UI components (it will be called first)
	 */
	public void onInitializeUI(GametableFrame frame) {}
	
	/**
	 * Initialize the core module.
	 * Initialize all non-UI components (use onInitializeUI for UI components).
	 */
	public void onInitializeCore(GameTableCore core) {}
	
	/**
	 * Notifies that the interface has switched to or from public map
	 * @param publicMap true if we have switched to public map
	 */
	public void onActiveMapChange(boolean publicMap) {}
	
	/**
	 * Load from an XML DOM node
	 * @see canSaveToXML
	 * @param node An XML node located by the engine.
	 * @param converter to convert stored MapElementIDs to actual map element IDs	
	 * @param netEvent Network event that triggered the load (or null) 
	 */
	public void loadFromXML(Element node, XMLSerializeConverter converter, NetworkEvent netEvent) {}
	
	/**
	 * Save to an XML DOM node
	 * @see canSaveToXML
	 * @param node An XML node created by the engine.  Data can be added to the node, but the node's attributes should not be modified by modules.
	 * @return false if There is nothing to save (node will be discarded)
	 */
	public boolean saveToXML(Element node) { return false; }
	
	/**
	 * @return true if this module can load and save to XML.  (saveToXML and loadFromXML will not be called if this returns false.  Defaults to false. 
	 */
	public boolean canSaveToXML() 
	{ 
		return false; 
	}

	/**
	 * Called by the system when a new player is added to the session
	 * @param newPlayer Newly added player
	 */
	public void onPlayerAdded(Player newPlayer) {}
	
	/**
	 * Called by the system when a new player is removed from the session
	 * @param oldPlayer Newly removed player
	 */
	public void onPlayerRemoved(Player oldPlayer) {}
	
	/**
	 * Called after the properties file has been stored to disk.
	 * Certain modules might want to keep their own properties. 
	 */
	public void onSavePropertiesCompleted() {}
	
	/**
	 * Called after the properties file has been loaded from disk.
	 * Certain modules might want to keep their own properties. 
	 */
	public void onLoadPropertiesCompleted() {}
	
	/**
	 * Called when properties are initialized - allows to set the global information of the 
	 * properties and decide if they should be displayed in the properties dialog.
	 * @param properties Properties handler
	 */
	public void onInitializeProperties(XProperties properties) {}
	
	/**
	 * Called when properties should be updated - right before save
	 * @param properties Properties handler
	 */
	public void onUpdateProperties(XProperties properties) {}
	
	/**
	 * Properties have been loaded and should be inspected and acted upon by modules
	 * @param properties Properties handler
	 */
	public void onApplyProperties(XProperties properties) {}
	
	/**
	 * TODO Revise how we actually want to handle menu modification by modules (this is temporary solution)
	 * Get this module's menu
	 * @return JMenu or null
	 */
	public JMenu getModuleMenu() { return null; }
}

