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

package com.gametable.plugins.dicemacro.slashcmds;

import com.gametable.plugins.dicemacro.DiceMacroModule;
import com.gametable.ui.chat.SlashCommand;

/**
 * #GT-COMMENT
 */
public class MacroDelete extends SlashCommand
{
	/**
	 * Constructor
	 */
	public MacroDelete()
	{
		super("macrodelete", "deletes an unwanted macro");
	}

	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#getCommandNameAliases()
	 */
	@Override
	public String[] getCommandNameAliases()
	{
		return new String[] { "del" };
	}

	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand(java.lang.String[], java.lang.String)
	 */
	@Override
	public String processCommand(String[] words, String text)
	{
		// req. 1 param
		if (words.length < 2)
		{
			return words[0] + " usage: " + words[0] + " &lt;macroName&gt;";
		}

		// find and kill this macro
		DiceMacroModule.getModule().removeMacro(words[1]);

		return null;
	}

}
