/*
 * Who.java
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

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.ChatEngineIF.MessageType;
import com.galactanet.gametable.ui.chat.SlashCommand;
import com.plugins.dicemacro.DiceMacro;
import com.plugins.dicemacro.DiceMacroModule;

/**
 *  #GT-COMMENT
 */
public class Roll extends SlashCommand
{
	/**
	 * Constructor
	 */
	public Roll()
	{
		super("roll", "roll dice");
	}

	/**
	 * Constructor for private roll override
	 * 
	 * @param command
	 * @param description
	 */
	protected Roll(String command, String description)
	{
		super(command, description);
	}

	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#getCommandNameAliases()
	 */
	@Override
	public String[] getCommandNameAliases()
	{
		return new String[] { "r" };
	}

	/*
	 * @see com.galactanet.gametable.ui.chat.SlashCommand#processCommand()
	 */
	@Override
	public String processCommand(String words[], String text)
	{
		// req. 1 param
		if (words.length < 2)
		{
			return "" + words[0] + " usage: " + words[0] + " &lt;Dice Roll in standard format&gt;<br/>" + "or: " + words[0]
					+ " &lt;Macro Name&gt; [&lt;+/-&gt; &lt;Macro Name or Dice Roll&gt;]...<br/>" + "Examples:<br/>" + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
					+ words[0] + " 2d6 + 3d4 + 8<br/>" + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + words[0] + " My Damage + d4<br/>"
					+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + words[0] + " d20 + My Damage + My Damage Bonus<br/>"
					+ "The /roll & /proll commands will not combine multi-dice Macros by adding or subracting.<br/>"
					+ "By doing so you will only add the last dice of the macro, to the first die of the next.<br/>"
					+ "They will make a mulit-dice command by doing /roll mydice,mydice2 and such.";
		}

		final String remaining = text.substring((words[0] + " ").length());
		final StringBuffer roll = new StringBuffer();
		int ci = 0;
		char c;
		boolean isLast = false;
		String term;
		for (int i = 0; i < remaining.length(); ++i)
		{
			c = remaining.charAt(i);
			isLast = (i == (remaining.length() - 1));
			if ((c == '+') || (c == '-') || (c == ',') || isLast)
			{
				if (isLast)
					term = remaining.substring(ci);
				else
					term = remaining.substring(ci, i);
				if (term.length() > 0)
				{
					final DiceMacro macro = DiceMacroModule.getModule().findMacro(term);
					if (macro != null)
						roll.append(macro.getMacro());
					else
						roll.append(term); // No Macro assume its a normal die term. And let the dicemacro figure it out.
				}
				if (!isLast)
				{
					roll.append(c);
				}
				ci = i + 1;
			}
		}

		final DiceMacro rmacro = new DiceMacro(roll.toString(), remaining, null);

		if (rmacro.isInitialized())
		{
			if (words[0].equals("/r") || words[0].equals("/roll"))
				rmacro.doMacro(false);
			else
				rmacro.doMacro(true);
		}
		else
		{
			GameTableCore.getCore().sendMessageLocal(MessageType.MECHANIC, "<b><font color=\"#880000\">Error in Macro String.</font></b>");
		}

		return null;
	}
}
