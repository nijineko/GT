/*
 * GameTableMapTreeModel.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.net.NetworkEvent;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class GameTableMapTreeModel extends DefaultTreeModel
{
	/**
	 * Constructor
	 * @param map
	 */
	public GameTableMapTreeModel(GameTableMap map, Map<MapElementID, Long> sortIDMap)
	{
		super(new DefaultMutableTreeNode(map));
		
		MapElementListenerIF listener = new MapElementAdapter() {
			/*
			 * @see com.galactanet.gametable.data.MapElementAdapter#onLayerChanged(com.galactanet.gametable.data.MapElement, com.galactanet.gametable.data.MapElementTypeIF.Layer, com.galactanet.gametable.data.MapElementTypeIF.Layer)
			 */
			@Override
			public void onLayerChanged(MapElement element, Layer newLayer, Layer oldLayer)
			{
				NetworkEvent netEvent = null;
				
				if (newLayer == Layer.POG)
				{
					m_gameTableMapListener.onMapElementAdded(m_map, element, netEvent);
				}
				else if (oldLayer == Layer.POG)
				{
					m_gameTableMapListener.onMapElementRemoved(m_map, element, false, netEvent);
				}
			}
			
			/*
			 * @see com.galactanet.gametable.data.MapElementAdapter#onAttributeChanged(com.galactanet.gametable.data.MapElement, java.lang.String, java.lang.String, java.lang.String)
			 */
			@Override
			public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue)
			{
				MapElementNode node = findElementNode(element);
				if (node != null)
				{
					node.rebuildAttributeNodes();
					nodeStructureChanged(node);
				}
			}
			
			/*
			 * @see com.galactanet.gametable.data.MapElementAdapter#onNameChanged(com.galactanet.gametable.data.MapElement, java.lang.String, java.lang.String)
			 */
			@Override
			public void onNameChanged(MapElement element, String newName, String oldName)
			{
				MapElementNode node = findElementNode(element);
				if (node != null)
					nodeChanged(node);
			}
		};
		
		m_map = map;
		m_sortIDs = sortIDMap;
		m_gameTableMapListener = new GameTableMapListener();
		m_map.addListener(m_gameTableMapListener);
		
		m_map.addMapElementListener(listener);
		m_rootNode = (DefaultMutableTreeNode)getRoot();
		
		for (MapElement element : map.getMapElements())
		{
			MapElementNode node = new MapElementNode(element);
			this.insertNodeInto(node, m_rootNode, getChildCount(m_rootNode));
		}
	}
	

	/**
	 * Find an element node in the tree 
	 * @param element
	 * @return
	 */
	public MapElementNode findElementNode(MapElement element)
	{
		if (m_rootNode.getChildCount() == 0)
			return null;
		
		MapElementNode node = (MapElementNode)m_rootNode.getChildAt(0);
		
		while (node != null)
		{
			if (node.getMapElement().equals(element))
				return node;
			
			node = (MapElementNode)node.getNextSibling();
		}

		return null;
	}
	
	/**
	 * @return The GameTableMap associated with this model
	 */
	public GameTableMap getGameTableMap()
	{
		return m_map;
	}
	
	/**
	 * Find row index of element
	 * @param element
	 * @return
	 */
	public int indexOf(MapElement element)
	{
		MapElementNode node = findElementNode(element);
		if (node == null)
			return -1;
			
		return getIndexOfChild(m_rootNode, node);	
	}
	
	/**
	 * Find MapElement at given index
	 * @param index
	 * @return
	 */
	public MapElement get(int index)
	{
		if (index < 0 || index >= m_rootNode.getChildCount())
			return null;
		
		MapElementNode node = (MapElementNode)getChild(m_rootNode, index);
		
		return node.getMapElement();
	}
	
	/**
	 * Refresh the sort ID list
	 */
	protected void refreshSortIDs()
	{
		for (int i=0; i < m_rootNode.getChildCount(); i++)
		{
			MapElementNode node = (MapElementNode)m_rootNode.getChildAt(i);
			m_sortIDs.put(node.getMapElement().getID(), (long)i);
		}
	}
	
	/**
	 * Reorder the elements in the tree based on their sort IDs
	 */
	protected void reorderElements()
	{
		List<MapElementNode> nodes = new ArrayList<MapElementNode>();

		for (int i=0; i < m_rootNode.getChildCount(); i++)
		{
			MapElementNode node = (MapElementNode)m_rootNode.getChildAt(i);
			
			Long sortID = m_sortIDs.get(node.getMapElement().getID());
			if (sortID == null)
				sortID = (long)i;
			
			node.setSortID(sortID);
			
			nodes.add(node);
		}
		
		Collections.sort(nodes);
		
		m_rootNode.removeAllChildren();
		
		for (MapElementNode node : nodes)
			insertNodeInto(node, m_rootNode, 0);
			//m_rootNode.add(node);
	}
	
	/**
	 * Map listener
	 *
	 * @author Eric Maziade
	 */
	private class GameTableMapListener extends GameTableMapAdapter
	{
		/*
		 * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstanceAdded(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElement)
		 */
		@Override
		public void onMapElementAdded(GameTableMap map, MapElement mapElement, NetworkEvent netEvent)
		{
			if (mapElement.getLayer() == Layer.POG)
			{
				MapElementNode node = new MapElementNode(mapElement);
				GameTableMapTreeModel.this.insertNodeInto(node, m_rootNode, getChildCount(m_rootNode));
			}
		}
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstanceRemoved(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElement)
		 */
		@Override
		public void onMapElementRemoved(GameTableMap map, MapElement mapElement, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				MapElementNode node = findElementNode(mapElement);
				
				if (node != null)
				{
					m_sortIDs.remove(mapElement.getID());
					removeNodeFromParent(node);
				}
			}
		}
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstancesRemoved(com.galactanet.gametable.data.GameTableMap, java.util.List, com.galactanet.gametable.net.NetworkEvent)
		 */
		@Override
		public void onMapElementsRemoved(GameTableMap map, List<MapElement> mapElements, NetworkEvent netEvent)
		{
			// Since there's no way to make this more efficient in batch, we'll send it back as non-batch
			for (MapElement mapElement : mapElements)
				onMapElementRemoved(map, mapElement, false, netEvent);
		}
		
		/*
		 * @see com.galactanet.gametable.data.GameTableMapListenerIF#onMapElementInstancesCleared(com.galactanet.gametable.data.GameTableMap)
		 */
		@Override
		public void onMapElementsCleared(GameTableMap map, NetworkEvent netEvent)
		{
			m_rootNode.removeAllChildren();		
		}
	}
	
	private final GameTableMap m_map;
	private final Map<MapElementID, Long> m_sortIDs;
	private DefaultMutableTreeNode m_rootNode;
	private GameTableMapListenerIF m_gameTableMapListener;
}
