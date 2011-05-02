/*
 * DlgUtils.java
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
package com.maziade.tools.ui;

import java.awt.Component;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import com.maziade.tools.Strings;

public class DlgUtils
{
	static public void setLabelText(JLabel label, String ressourceName)
	{
		label.setText(DlgUtils.getResourceString(ressourceName));
		String mnemonic = DlgUtils.getResourceString(ressourceName+"_M");
		if(!Strings.isEmpty(mnemonic))
		{
			label.setDisplayedMnemonic(mnemonic.charAt(0));
		}
	}
	
	static public void setAssociatedLabelText(JLabel label, String ressourceName, Component component)
	{
		label.setText(DlgUtils.getResourceString(ressourceName));
		String mnemonic = DlgUtils.getResourceString(ressourceName+"_M");
		if(!Strings.isEmpty(mnemonic))
		{
			label.setDisplayedMnemonic(mnemonic.charAt(0));
			label.setLabelFor(component);
		}
	}
	
	static public void setButtonText(JButton button, String ressourceName)
	{
		button.setText(DlgUtils.getResourceString(ressourceName));
		String mnemonic = DlgUtils.getResourceString(ressourceName+"_M");
		if(!Strings.isEmpty(mnemonic))
		{
			button.setMnemonic(mnemonic.charAt(0));
		}
		
	}
	
	static public void setCheckBoxText(JCheckBox box, String ressourceName)
	{
		box.setText(DlgUtils.getResourceString(ressourceName));
		String mnemonic = DlgUtils.getResourceString(ressourceName+"_M");
		if(!Strings.isEmpty(mnemonic))
		{
			box.setMnemonic(mnemonic.charAt(0));
		}
		
	}
	
	static public void registerEnterKeyAsInputMap(JButton button)
	{
		javax.swing.InputMap map = button.getInputMap();
		if (map != null)
		{
			map.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0, false), "pressed");
			map.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0, true), "released");
		}
	}
	
	/**
	 * TODO review resource architecture
	 * Get a resource string from the set resource bundle
	 * @param key
	 * @return
	 */
	public static String getResourceString(String key)
	{
		try
		{
			return getResourceBundle().getString(key);
		}
		catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
	}
	
	/**
	 * Get the currently set resource bundle.  If none are set, the default bundle is read from 
	 * 	com.maziade.props.messages
	 * @return ResourceBundle
	 */
	public static ResourceBundle getResourceBundle()
	{
		if (g_resourceBundle == null)
			g_resourceBundle = ResourceBundle.getBundle("com.maziade.props.messages");
		
		return g_resourceBundle;
	}
	
	/**
	 * You can provide your own resource bundle to override the default bundle
	 * @param res
	 */
	public static void setResourceBundle(ResourceBundle res)
	{
		if (res == null)
			throw new IllegalArgumentException("Cannot set null resource bundle");
		g_resourceBundle = res;
	}
	
	private static ResourceBundle					g_resourceBundle;	
}
