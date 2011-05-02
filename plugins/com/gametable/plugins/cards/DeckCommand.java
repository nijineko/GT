/*
 * Deck.java
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

package com.gametable.plugins.cards;

import com.gametable.ui.chat.SlashCommand;

/**
 * todo: comment
 *
 * @author Eric Maziade
 */
public class DeckCommand extends SlashCommand
{
	/**
	 * Constructor 
	 */
	public DeckCommand()
	{
		super("deck", "Various deck actions. type /deck for more details");		
	}
	
	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand(java.lang.String[], java.lang.String)
	 */
	@Override
	public String processCommand(String[] words, String text)
	{
		CardModule.getModule().deckCommand(words); // deck commands. there are many
		return null;
	}
}
