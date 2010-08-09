/*
 * Who.java
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

package com.galactanet.gametable.ui.chat.commands;

import java.util.Collection;

import com.galactanet.gametable.ui.chat.SlashCommand;
import com.galactanet.gametable.ui.chat.SlashCommands;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class Help extends SlashCommand
{
	/**
	 * 
	 */
	public Help()
	{
		super("help", "List all slash commands");
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#getCommandNameAliases()
	 */
	@Override
	public String[] getCommandNameAliases()
	{
		return new String[] { "?", "/" };
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#compareTo(com.galactanet.gametable.ui.chat.SlashCommand)
	 */
	@Override
	public int compareTo(SlashCommand o)
	{
		// Help always at bottom
		
		if (o instanceof Help)
			return 0;
		
		return 1;
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		Collection<SlashCommand> commands = SlashCommands.getCommands();
		
		StringBuffer help = new StringBuffer();
		help.append("<b><u>Slash Commands</u></b><br>");
		
		for(SlashCommand command : commands)
		{
			help.append("<b>/");
			help.append(command.getCommandName());
			help.append("</b> ");
			help.append(command.getDescription());
			help.append("<br/>");
		}
		
		return help.toString();
	}
}
