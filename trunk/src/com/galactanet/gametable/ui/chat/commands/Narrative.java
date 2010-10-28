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

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.ChatEngineIF.MessageType;
import com.galactanet.gametable.ui.chat.SlashCommand;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class Narrative extends SlashCommand
{
	/**
	 * Constructor 
	 */
	public Narrative()
	{
		super("as", "Display a narrative of a character saying something");
	}
	
	/**
	 * Constructor 
	 */
	protected Narrative(String command, String description)
	{
		super(command, description);
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		if (words.length < 3)
    {
        // tell them the usage and bail
        return
        	"/as usage: /as &lt;name&gt; &lt;text&gt;<br/>" +
	        "Examples:<br/>" +
	        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/as Balthazar Prepare to meet your doom!.<br/>" +
	        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/as Lord_Doom Prepare to meet um... me!<br/>" +
	        "Note: Underscore characters in the name you specify will be turned into spaces"; 
    }
		
		final StringBuffer speakerName = new StringBuffer(UtilityFunctions.unquote(words[1]));

    for (int i = 0; i < speakerName.length(); i++)
    {
        if (speakerName.charAt(i) == '_')
        {
            speakerName.setCharAt(i, ' ');
        }
    }

    // get the portion of the text after the emote command
    final int start = text.indexOf(words[1]) + words[1].length();
    final String toSay = text.substring(start).trim();

    // simply post text that's an emote instead of a character action
    final StringBuffer toPost = new StringBuffer();
    toPost.append(Emote.EMOTE_MESSAGE_FONT + speakerName);
    
    if	(!m_emoteas) 
    	toPost.append(":");
    
    toPost.append(" " + Emote.END_EMOTE_MESSAGE_FONT + toSay);
    
    GameTableCore.getCore().sendMessageBroadcast(MessageType.CHAT, toPost.toString());    
    
    return null;
	}
	
	/**
	 * Narrative emote mode
	 */
	protected boolean m_emoteas = false;
}
