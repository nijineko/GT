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

package com.gametable.ui.chat.commands;

import java.util.List;

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.Player;
import com.gametable.data.ChatEngineIF.MessageType;
import com.gametable.ui.chat.SlashCommand;
import com.gametable.util.UtilityFunctions;

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
	 * @see com.gametable.ui.chat.SlashCommand#getCommandNameAliases()
	 */
	@Override
	public String[] getCommandNameAliases()
	{
		return new String[] { "send", "t" };
	}
	
	/*
	 * @see com.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		GameTableCore core = GametableApp.getCore();
		
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
    String toName = UtilityFunctions.unquote(words[1]);
    
    // Unquote first name, if quoted
    

    // see if there is a player or character with that name
    // and note the "proper" name for them (which is their player name)
    Player toPlayer = null;
    List<Player> players = core.getPlayers();
    for (Player player : players)
    {
        if (player.isNamed(toName))
        {
            toPlayer = player;
            break;
        }
    }

    if (toPlayer == null)
    {
        // nobody by that name is in the session
    		core.sendMessageLocal(MessageType.ALERT, "There is no player or character named \"" + toName + "\" in the session.");
        return null;
    }

    // now get the message portion
    // we have to do this with the original text, cause the words[] array
    // will have stripped a lot of whitespace if they had multiple spaces, etc.
    // indexOf(toName) will get us to the start of the player name it's being sent to
    // we then add the length of the name to get past that
    
    final int start = text.indexOf(toName) + toName.length();
    final String toSend = text.substring(start).trim();
    
    String message = PRIVATE_MESSAGE_FONT + UtilityFunctions.emitUserLink(core.getPlayer()) + " tells you: " + END_PRIVATE_MESSAGE_FONT + toSend;

    core.sendMessage(MessageType.CHAT, toPlayer, message);
    
    return null;
	}
	
  private final static String    PRIVATE_MESSAGE_FONT     = "<font color=\"#009900\">";
  private final static String    END_PRIVATE_MESSAGE_FONT = "</font>";
}
