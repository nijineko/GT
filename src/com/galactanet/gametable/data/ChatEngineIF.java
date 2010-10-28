/*
 * ChatEngineIF.java
 *
 * @created 2010-10-14
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

package com.galactanet.gametable.data;

/**
 * Interface to allow UI to respond to requests to display information in a chat panel
 *
 * @author Eric Maziade
 */
public interface ChatEngineIF
{
	public static enum MessageType { 
		CHAT, ALERT, MECHANIC, SYSTEM;
		
		/**
		 * Convert from ordinal value
		 * @param ordinal ordinal value
		 * @return MessageType or null
		 */
		public static MessageType fromOrdinal(int ordinal)
		{
			for (MessageType type : MessageType.values())
			{
				if (type.ordinal() == ordinal)
					return type;
			}
			
			return null;
		}
	}
	
	/**
	 * Display a message on the chat panel
	 * @param type The type of message to display (might affect rendering)
	 * @param text Text to display
	 */
	public void displayMessage(MessageType type, String text);
	
	/**
	 * Called by the core to notify the chat engine that the player composition has changed
	 */
	public void onPlayersChanged();
	
	/**
	 * Clear all messages from the user interface
	 */
	public void clearMessages();
}
