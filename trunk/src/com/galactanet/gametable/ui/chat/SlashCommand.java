/*
 * SlashCommand.java
 *
 * @created 2010-08-08
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

package com.galactanet.gametable.ui.chat;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public abstract class SlashCommand implements Comparable<SlashCommand>
{
	/**
	 * Constructor
	 * @param commandName Case sensitive slash command (excluding slash)
	 * @param description Description on how to use the command
	 */
	public SlashCommand(String commandName, String description)
	{
		m_commandName = commandName;
		m_description = description;
	}
	
	/**
	 * @return Case sensitive slash command (excluding slash)
	 */
	public String getCommandName()
	{
		return m_commandName;
	}
	
	/**
	 * @return Description on how to use the command
	 */
	public String getDescription()
	{
		return m_description;
	}
	
	/**
	 * @return A list of alias commands this command can run under. 
	 */
	public String[] getCommandNameAliases()
	{
		return new String[0];
	}
	
	/**
	 * Override this method to react on slash command
	 * @param words Array of words passed as command
	 * @param text Original string of text, including command, unaltered.
	 * @return String to display in chat window (null is ignored)
	 */
	public abstract String processCommand(String words[], String text);
	
	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SlashCommand o)
	{
		return m_commandName.compareTo(o.m_commandName);
	}

	/**
	 * Command name
	 */
	private final String m_commandName;
	
	/**
	 * Command description
	 */
	private final String m_description;
}
