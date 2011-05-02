/*
 * MessageQueue.java
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

/**
 * @author Eric Maziade
 *
 * Synchronous message queue implementation, uses swing's messaging queue through "invokelater" - making it automatically synchronized with UI
 */
public class MessageQueue implements Runnable
{	
	/**
	 * Get singleton instance of the message queue 
	 * @return
	 */
	public static MessageQueue getMessageQueue()
	{
		if (g_messageQueue == null)
			g_messageQueue = new MessageQueue();
		
		return g_messageQueue;
	}
	
	/**
	 * Constructor is private to force singleton pattern
	 */
	private MessageQueue() 
	{
	}
	
	/**
	 * Adds a message to the queue
	 * @param id Identifies the message
	 * @param priority message priority
	 * @param listener message listener - will be called when the message pops on the queue
	 * @param parameter message parameter, will be passed to the listener
	 * @param debug string
	 */
	protected void addMessage(MessageID id, MessagePriority priority, MessageListener listener, Object parameter, String debug)
	{
		if (id == null || priority == null || listener == null)
			throw new IllegalArgumentException("Invalid parmeter: type, priority and listener cannot be null");
		
		Message msg = new Message();
		msg.id = id;		
		msg.listener = listener;
		msg.parameter = parameter;
		msg.debug = debug;
		
		List<Message> lst;
		
		switch (priority)
		{
		case HIGH:
			lst = m_lstMessagesHigh;
			break;
			
		case LOW:
			lst = m_lstMessagesLow;
			break;
			
		default:
			throw new IllegalArgumentException("Invalid priority " + priority);
		}
		
		synchronized (this)
		{
			lst.add(msg);
		}
		
		SwingUtilities.invokeLater(this);
	}
	
	/**
	 * Removes all messages from the queue 
	 */
	public void clearMessages()
	{
		synchronized (m_lstMessagesHigh)
		{
			m_lstMessagesHigh.clear();
		}
		
		synchronized (m_lstMessagesLow)
		{
			m_lstMessagesLow.clear();
		}
	}
	
	/**
	 * Removes all messages belonging to the specified type from the queue
	 * @param id Identifies the message
	 */
	protected void clearMessages(MessageID id)
	{
		clearMessages(id, null);
	}
	
	/**
	 * Removes all messages belonging to the specified type from the queue
	 * @param id Identifies the message
	 */
	protected void clearMessages(MessageID id, MessageListener listener)
	{
		synchronized (m_lstMessagesHigh)
		{
			Iterator<Message> i = m_lstMessagesHigh.iterator();
			while (i.hasNext())
			{
				Message msg = i.next();				
				if (msg.id == id && (listener == null || listener == msg.listener))
					i.remove();
			}
		}
		
		synchronized (m_lstMessagesLow)
		{
			Iterator<Message> i = m_lstMessagesLow.iterator();
			while (i.hasNext())
			{
				Message msg = i.next();		
				if (msg.id == id && (listener == null || listener == msg.listener))
					i.remove();
			}
		}
	}
	
	/**
	 * Verifies if the queue is already polling
	 * @return true if it is polling, false if it is idle
	 */
	public final boolean isQueuePolling()
	{
		return m_polling;
	}
	
	/**
	 * Checks if the queue is empty;
	 * @return
	 */
	public final boolean isQueueEmpty()
	{
		return m_lstMessagesHigh.size() + m_lstMessagesLow.size() == 0;
	}
	
	/**
	 * Run through the high priority queue
	 */
	private void executeHighPriorityQueue()
	{
		while (m_lstMessagesHigh.size() > 0)
		{
			Message msg;
			
			synchronized(m_lstMessagesHigh)
			{
				msg = m_lstMessagesHigh.remove(0);
			}
			
			try
			{
				msg.executeMessage(MessagePriority.HIGH);
			}
			catch (Throwable e)
			{
				rethrowLater(e);
			}
		}
	}
	
	/**
	 * Re-throw an exception so it is handled - without breaking the message queue 
	 */
	private void rethrowLater(Throwable e)
	{
		if (e instanceof RuntimeException)
			SwingUtilities.invokeLater(new Rethrower((RuntimeException)e));
		else
			SwingUtilities.invokeLater(new Rethrower(new RuntimeException(e)));
	}
	
	/**
	 * Called by swing
	 */
	@Override
	public void run()
	{		
		// can be set to false externally to stop the polling
		if (m_polling)
		{
			return;
		}
		
		//while (m_polling)
			m_polling = true;
			
		{
			executeHighPriorityQueue();
			
			while (m_lstMessagesLow.size() > 0)
			{
				Message msg;
				
				synchronized(m_lstMessagesLow)
				{
					msg = m_lstMessagesLow.remove(0);
				}

				try
				{
					msg.executeMessage(MessagePriority.LOW);
				}
				catch (Throwable e)
				{
					rethrowLater(e);					
				}
				
				// always check for high priority queue before running low priority queue
				executeHighPriorityQueue();
			}
		}
		
		m_polling = false;
	}
	
	private boolean m_polling = false;
	private List<Message> m_lstMessagesLow = new LinkedList<Message>();	
	private List<Message> m_lstMessagesHigh = new LinkedList<Message>();
	
	private static MessageQueue g_messageQueue = null;
	
	/**
	 * Holds message information
	 */
	private class Message
	{
		public MessageID id;
		public MessageListener listener;
		public Object parameter;
		public String debug;
		
		public void executeMessage(MessagePriority priority)
		{
			if (m_logger.isTraceEnabled())
				m_logger.trace("executeMessage " + id + " " + debug + " " + parameter);
			
			listener.executeMessage(id, priority, parameter, debug);
		}
	}
	
	private class Rethrower implements Runnable
	{
		private final RuntimeException m_throwable;
		
		public Rethrower(RuntimeException t) { m_throwable = t; }
		
		@Override
		public void run()
		{
			throw m_throwable;
			
		}
	}
	
	private static Logger m_logger = Logger.getLogger(MessageQueue.class);
}