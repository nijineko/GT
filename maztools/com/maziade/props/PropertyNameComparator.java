/*
 * PropertyNameComparator.java
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
package com.maziade.props;

import java.util.Comparator;

import com.maziade.props.XProperties.XPropertyInfo;
import com.maziade.tools.Strings;

/**
 * This class compares two properties based on their names
 *  
 * @author Eric Maziade
 *
 */
public class PropertyNameComparator implements Comparator<XPropertyInfo>
{
	/**
	 * Single instance of comparator object
	 */
	private static PropertyNameComparator	g_comparator	= null;

	/**
	 * Get property name comparator instance.
	 * @return comparator single instance
	 */
	static public PropertyNameComparator getComparator()
	{

		if (g_comparator == null)
			g_comparator = new PropertyNameComparator();

		return g_comparator;
	}

	/**
	 * Constructor is private 
	 * @see PropertyNameComparator.getComparator();
	 */
	private PropertyNameComparator()
	{
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(XPropertyInfo o1, XPropertyInfo o2)
	{

		int rc = Strings.compareToIgnoreCase(o1.getGroupDisplayName(), o2.getGroupDisplayName());
		if (rc != 0)
			return rc;
		else if (o1.getPosition() < o2.getPosition())
			return -1;
		else if (o1.getPosition() > o2.getPosition())
			return 1;
		else
			return Strings.compareToIgnoreCase(o1.getDisplayName(), o2.getDisplayName());
	}
}