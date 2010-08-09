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

import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.chat.SlashCommand;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class Tell extends SlashCommand
{
	/**
	 * 
	 */
	public Tell()
	{
		super("tell", "send a private message to another player");
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#getCommandNameAliases()
	 */
	@Override
	public String[] getCommandNameAliases()
	{
		return new String[] { "send", "t" };
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		 // send a private message to another player
    if (words.length < 3)
    {
        // tell them the usage and bail
        return 
        	words[0] + " usage: " + words[0] + " &lt;player name&gt; &lt;message&gt;<br/>" +
        	"Examples:<br/>" +
        	"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + words[0] + "<br/>" +
        	" Dave I am the most awesome programmer on Gametable!<br/>" +
        	"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + words[0] + " Andy No you're not, you suck!";
    }

    // they have a legitimate /tell or /send
    final String toName = words[1];

    // see if there is a player or character with that name
    // and note the "proper" name for them (which is their player name)
    Player toPlayer = null;
    for (int i = 0; i < GametableFrame.getGametableFrame().getPlayers().size(); i++)
    {
        final Player player = GametableFrame.getGametableFrame().getPlayers().get(i);
        if (player.hasName(toName))
        {
            toPlayer = player;
            break;
        }
    }

    if (toPlayer == null)
    {
        // nobody by that name is in the session
    		GametableFrame.getGametableFrame().getChatPanel().logAlertMessage("There is no player or character named \"" + toName + "\" in the session.");
        return null;
    }

    // now get the message portion
    // we have to do this with the original text, cause the words[] array
    // will have stripped a lot of whitespace if they had multiple spaces, etc.
    // indexOf(toName) will get us to the start of the player name it's being sent to
    // we then add the length of the name to get past that
    
    final int start = text.indexOf(toName) + toName.length();
    final String toSend = text.substring(start).trim();

    GametableFrame.getGametableFrame().tell(toPlayer, toSend);
    
    return null;
	}
}
