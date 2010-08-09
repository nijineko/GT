/*
 * Macro.java
 * 
 * @created 2010-08-08
 * 
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.plugins.dicemacro.slashcmds;

import com.galactanet.gametable.ui.chat.SlashCommand;
import com.plugins.dicemacro.DiceMacroModule;

/**
 * #GT-COMMENT
 */
public class Macro extends SlashCommand
{
	/**
	 * Constructor
	 */
	public Macro()
	{
		super("macro", "macro a die roll");
	}

	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand(java.lang.String[], java.lang.String)
	 */
	@Override
	public String processCommand(String[] words, String text)
	{
		// macro command. this requires at least 2 parameters
		if (words.length < 3)
		{
			return "/macro usage: /macro &lt;macroName&gt; &lt;dice roll in standard format&gt;<br/>" + "Examples:<br/>"
					+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/macro Attack d20+8<br/>" + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/macro SneakDmg d4 + 2 + 4d6<br/>"
					+ "Note: Macros will replace existing macros with the same name.";
		}

		// the second word is the name
		final String name = words[1];

		// all subsequent "words" are the die roll macro
		DiceMacroModule.getModule().addMacro(name, text.substring("/macro ".length() + name.length() + 1), null);

		return null;
	}

}
