/*
 * MapElementNode.java
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

package com.plugins.activepogs;

import javax.swing.tree.DefaultMutableTreeNode;

import com.galactanet.gametable.data.MapElement;

/**
 * A TreeNode representing a library.
 * 
 * @author Iffy
 */
public class MapElementNode extends DefaultMutableTreeNode implements Comparable<MapElementNode>
{
	/**
	 * Serial
	 */
	private static final long	serialVersionUID	= -5086776295684411196L;

	/**
	 * Constructor
	 * @param element MapElement to link to this node
	 */
	public MapElementNode(MapElement element)
	{
		super(element, true);
		
		rebuildAttributeNodes();
	}
	
	/**
	 * Rebuild the attribute nodes of this element
	 */
	protected void rebuildAttributeNodes()
	{
		removeAllChildren();
		
		for (String name : getMapElement().getAttributeNames())
		{
			add(new AttributeNode(name));
		}		
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o)
	{
		if (o == this)
		{
			return true;
		}

		final MapElementNode node = (MapElementNode) o;
		return (node.getMapElement().equals(getMapElement()));
	}

	/**
	 * @return Returns the Map Element for this node.
	 */
	public MapElement getMapElement()
	{
		return (MapElement) getUserObject();
	}
	
	/**
	 * Set the sort ID
	 * @param sortID
	 */
	protected void setSortID(long sortID)
	{
		m_sortID = sortID;
	}
	
	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MapElementNode o)
	{
		if (o.m_sortID < m_sortID)
			return -1;
		
		if (o.m_sortID > m_sortID)
			return 1;
		
		return 0;
	}
	
	private long m_sortID = 0;
}
