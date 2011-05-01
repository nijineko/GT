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

import com.gametable.GametableApp;
import com.gametable.data.ChatEngineIF.MessageType;
import com.gametable.ui.chat.SlashCommand;
import com.gametable.util.UtilityFunctions;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class Emote extends SlashCommand
{
	/**
	 * 
	 */
	public Emote()
	{
		super("emote", "Display an emote");
	}
	
	/*
	 * @see com.gametable.ui.chat.SlashCommand#getCommandNameAliases()
	 */
	@Override
	public String[] getCommandNameAliases()
	{
		return new String[] { "em", "me" };
	}
	
	/*
	 * @see com.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		 if (words.length < 2)
     {
         // tell them the usage and bail
         return 
         	"/emote usage: /emote &lt;action&gt;<br/>" +
         "Examples:<br/>" +
         "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/emote gets a beer.<br/>";
     }

     // get the portion of the text after the emote command
     final int start = text.indexOf(words[0]) + words[0].length();
     final String emote = text.substring(start).trim();

     // simply post text that's an emote instead of a character action
     final String toPost = 
    	 EMOTE_MESSAGE_FONT + 
    	 UtilityFunctions.emitUserLink(GametableApp.getCore().getPlayer()) +
       " " + emote + 
       END_EMOTE_MESSAGE_FONT;
     
     GametableApp.getCore().sendMessageBroadcast(MessageType.CHAT, toPost);
     
     return null;
	}
	
  protected final static String    EMOTE_MESSAGE_FONT       = "<font color=\"#004477\">";
  protected final static String    END_EMOTE_MESSAGE_FONT   = "</font>";
}
