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

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.ui.chat.SlashCommand;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class Who extends SlashCommand
{
	/**
	 * 
	 */
	public Who()
	{
		super("who", "lists connected players");
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		final StringBuffer buffer = new StringBuffer();
		
    buffer.append("<b><u>Who's connected</u></b><br>");
    
    for (int i = 0, size = GametableApp.getCore().getPlayers().size(); i < size; ++i)
    {
        final Player player = GametableApp.getCore().getPlayers().get(i);
        buffer.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        buffer.append(UtilityFunctions.emitUserLink(player));
        buffer.append("<br>");
    }
    
    buffer.append("<b>");
    buffer.append(GametableApp.getCore().getPlayers().size());
    buffer.append(" player");
    buffer.append((GametableApp.getCore().getPlayers().size() > 1 ? "s" : ""));
    buffer.append("</b>");
    
    return buffer.toString();
		
	}
}
