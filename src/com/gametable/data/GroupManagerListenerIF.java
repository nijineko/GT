/*
 * GroupManagerListenerIF.java
 *
 * @created 2010-09-10
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

package com.gametable.data;

import com.gametable.net.NetworkEvent;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public interface GroupManagerListenerIF
{
	/**
	 * Called when a group is removed from the list of groups
	 * @param group Newly removed group
	 * @param netEvent Source Network Event
	 */
	public void onRemoveGroup(Group group, NetworkEvent netEvent);
	
	/**
	 * Called when an element is added to a group
	 * @param group Group the element is added to
	 * @param mapElementID ID of the element
	 * @param netEvent Source Network Event
	 */
	public void onAddMapElementToGroup(Group group, MapElementID mapElementID, NetworkEvent netEvent);
	
	/**
	 * Called when an element is removed from a group
	 * @param group Group the element removed from
	 * @param mapElementID ID of the element
	 * @param netEvent Source Network Event
	 */
	public void onRemoveMapElementFromGroup(Group group, MapElementID mapElementID, NetworkEvent netEvent);

	/**
	 * Called when a group's name is changed
	 * @param group Group that has changed name
	 * @param oldGroupName Previous name for the group
	 * @param netEvent Source Network Event
	 */
	public void onGroupRename(Group group, String oldGroupName, NetworkEvent netEvent);
}
