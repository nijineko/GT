/*
 * MessageDefinition.java
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
package com.maziade.messages;

/**
 * @author Eric Maziade
 *  
 * Defines a message and allows to add messages to the queue.
 *
 */
public class MessageDefinition
{
	/**
	 * Creates a message template
	 * @param id Identifies the message
	 * @param priority message priority
	 */
	public MessageDefinition(MessageID id, MessagePriority priority)
	{
		this(id, priority, false);
	}
	
	/**
	 * Creates a message template
	 * @param id Identifies the message
	 * @param priority message priority
	 * @param clearIDFromQueue when adding this message to the queue, clear all other messages of same ID
	 */
	public MessageDefinition(MessageID id, MessagePriority priority, boolean clearIDFromQueue)
	{
		this(id, priority, clearIDFromQueue, false);
	}
	
	/**
	 * Creates a message template
	 * @param id Identifies the message
	 * @param priority message priority
	 * @param clearIDFromQueue when adding this message to the queue, clear all other messages of same type
	 * @param clearOnlyIfSameListener when adding this message to the queue, clear all other messages of same type
	 * with same listener instance
	 */
	public MessageDefinition(MessageID id, MessagePriority priority, boolean clearIDFromQueue, boolean clearOnlyIfSameListener)
	{
		if (clearOnlyIfSameListener && !clearIDFromQueue)
			throw new IllegalArgumentException("Cannot clear only if same listener when clear type from queue is false");
		
		m_messageID = id;
		m_priority = priority;
		m_clearOnlyIfSameListener = clearOnlyIfSameListener;
		m_clearIDFromQueue = clearIDFromQueue;
	}
	
	/**
	 * Adds a message to the queue
	 * @param listener message listener - will be called when the message pops on the queue
	 * @param debug string
	 */
	public void addMessage(MessageListener listener, String debug)
	{
		addMessage(listener, listener, debug);
	}
	
	/**
	 * Adds a message to the queue
	 * @param listener message listener - will be called when the message pops on the queue
	 * @param parameter message parameter, will be passed to the listener
	 * @param debug string
	 */
	public void addMessage(MessageListener listener, Object parameter, String debug)
	{
		MessageQueue queue = MessageQueue.getMessageQueue();
		
		if (m_clearIDFromQueue)
		{
			if (m_clearOnlyIfSameListener)
			{
				queue.clearMessages(m_messageID, listener);
			}
			else
			{
				queue.clearMessages(m_messageID);
			}
		}
		
		queue.addMessage(m_messageID, m_priority, listener, parameter, debug);
	}
	
	private final MessageID m_messageID;
	private final MessagePriority m_priority;
	private final boolean m_clearIDFromQueue;
	private final boolean m_clearOnlyIfSameListener;
}
