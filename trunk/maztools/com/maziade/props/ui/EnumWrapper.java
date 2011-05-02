/*
 * EnumWrapper.java
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
package com.maziade.props.ui;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.maziade.props.XProperties;

/**
 * Wrapper class for enum values.  Uses the messages resources to return a proper display
 * name on 'toString' 
 * @author Eric Maziade
 *
 */
public class EnumWrapper
{
	public EnumWrapper(Enum<?> value, String resourceBundleName, String propName)
	{
		m_value = value;
		String text = null;
		String resourceName = propName + "." + value.toString();
		
		if (resourceBundleName != null)
		{
			ResourceBundle bundle = ResourceBundle.getBundle(resourceBundleName);
			
			if (bundle != null)
			{
				try
				{
					text = bundle.getString(resourceName);
				}
				catch (MissingResourceException e)
				{
					text = '!' + resourceName + '!';
				}
			}
		}
		
		if (text == null)
			text = XProperties.getResourceString(resourceName);	// TODO document proper resource behavior
		
		m_text = text;
	}
	
	@Override
	public String toString()
	{
		return m_text;
	}
	
	public final String m_text;
	public final Enum<?> m_value;
}
