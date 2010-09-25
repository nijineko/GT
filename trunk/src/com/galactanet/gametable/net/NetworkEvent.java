/*
 * NetworkEvent.java
 *
 * @created 2010-09-12
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

package com.galactanet.gametable.net;

import com.galactanet.gametable.data.Player;

/**
 * Defines a network event.
 * 
 * Network event information is passed down by network messages when handling received messages.
 * This information can be handed down to the listeners in order to allow proper behavior when triggers are
 * triggered by receiving network messages. 
 *
 * @author Eric Maziade
 */
public class NetworkEvent
{
	/**
	 * Constructor
	 * @param sendingPlayer Player sending the message
	 * @param broadcasted True if this message was broadcasted 
	 * @param messageType MessageType instance
	 */
	public NetworkEvent(Player sendingPlayer, boolean broadcasted, NetworkMessageTypeIF messageType)
	{
		m_sendingPlayer = sendingPlayer;
		m_broadcast = broadcasted;
		m_messageType = messageType;
	}
	
	/**
	 * @return Player who sent the message
	 */
	public Player getSendingPlayer()
	{
		return m_sendingPlayer;
	}
	
	/**
	 * @return True if the message has been broadcasted
	 */
	public boolean isBroadcast()
	{
		return m_broadcast;
	}
	
	/**
	 * @return Message type
	 */
	public NetworkMessageTypeIF getMessageType()
	{
		return m_messageType;
	}
	
	protected final Player m_sendingPlayer;
	protected final boolean m_broadcast;
	protected final NetworkMessageTypeIF m_messageType;
}
