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

import java.util.List;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.GameTableMap;
import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.ui.chat.SlashCommand;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class PogList extends SlashCommand
{
	/**
	 * 
	 */
	public PogList()
	{
		super("poglist", "lists pogs by attribute");
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
	// macro command. this requires at least 2 parameters
    if (words.length < 2)
    {
        // tell them the usage and bail
        return 
	        "/poglist usage: /poglist &lt;attribute name&gt;<br/>" +
	        "Examples<br/>:" +
	        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/poglist HP<br/>" +
	        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/poglist Initiative<br/>" +
	        "Note: attribute names are case, whitespace, and punctuation-insensitive.";
    }

    final String name = UtilityFunctions.stitchTogetherWords(words, 1);
    final GameTableMap map = GametableApp.getCore().getMap(GameTableCore.MapType.ACTIVE);
    final List<MapElement> pogs = map.getMapElements();
    final StringBuffer buffer = new StringBuffer();
    
    buffer.append("<b><u>Pogs with \'" + name + "\' attribute</u></b><br>");
    
    int tally = 0;
    
    for (int i = 0, size = pogs.size(); i < size; ++i)
    {
        final MapElement pog = pogs.get(i);
        final String value = pog.getAttribute(name);
        if ((value != null) && (value.length() > 0))
        {
            String pogText = pog.getName();
            if ((pogText == null) || (pogText.length() == 0))
            {
                pogText = "&lt;unknown&gt;";
            }

            buffer.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>");
            buffer.append(pogText);
            buffer.append(":</b> ");
            buffer.append(value);
            buffer.append("<br>");
            ++tally;
        }
    }
    buffer.append("<b>" + tally + " pog" + (tally != 1 ? "s" : "") + " found.</b>");
    
    return buffer.toString();
	}
}
