/*
 * AttributeNode.java
 *
 * @created 2010-08-07
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

package com.gametable.plugins.activepogs;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.gametable.data.MapElement;

/**
 * A Leaf TreeNode representing a Pog's attribute.
 * 
 * @author Iffy
 */
public class AttributeNode extends DefaultMutableTreeNode
{
	/**
	 * Serial number 
	 */
	private static final long	serialVersionUID	= -7669642437687369530L;

	/**
	 * Constructor 
	 * @param atttributeName
	 */
	public AttributeNode(final String atttributeName)
	{
		super(atttributeName, false);
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
			return true;

		final AttributeNode node = (AttributeNode) o;
		return (node.getMapElement().equals(getMapElement()) && node.getAttribute().equals(getAttribute()));
	}
	
	/**
	 * Get the map element this attribute is linked to 
	 * @return
	 */
	public MapElement getMapElement()
	{
		TreeNode parentNode = getParent();
		
		if (parent instanceof MapElementNode)
			return ((MapElementNode)parentNode).getMapElement();
	
		return null;
	}

	/**
	 * @return Returns the attribute for this node.
	 */
	public String getAttribute()
	{
		return (String) getUserObject();
	}
}

