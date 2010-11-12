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
import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.data.ChatEngineIF.MessageType;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.chat.SlashCommand;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class Goto extends SlashCommand
{
	/**
	 * 
	 */
	public Goto()
	{
		super("goto", "Centers a pog in the map view");
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		if (words.length < 2)
    {
        return (words[0] + " usage: " + words[0] + " &lt;pog name&gt;");
    }
		
		GameTableCore core = GametableApp.getCore();
		GametableFrame frame = GametableApp.getUserInterface();
		if (frame == null)
		{
    	core.sendMessageLocal(MessageType.ALERT, "User interface not initialized");
      return null;
    }
		
    final String name = UtilityFunctions.stitchTogetherWords(words, 1);
    final MapElement pog = core.getMap(GameTableCore.MapType.ACTIVE).getMapElementByName(name);
    if (pog == null)
    {
    	core.sendMessageLocal(MessageType.ALERT, "Unable to find pog named \"" + name + "\".");
      return null;
    }
    
    frame.scrollToPog(pog);
    
    return null;
	}
}
