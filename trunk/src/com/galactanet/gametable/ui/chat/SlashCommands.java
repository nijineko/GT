/*
 * SlashCommands.java: GameTable is in the Public Domain.
 * 
 * #GT-AUDIT SlashCommands
 */

package com.galactanet.gametable.ui.chat;

import java.util.*;

import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.chat.commands.*;
import com.galactanet.gametable.util.UtilityFunctions;

/**
 * Handles chat window's slash commands
 *
 */
public class SlashCommands
{
	/**
	 * Collection of slash commands
	 */
	private static Map<String, SlashCommand>	m_commands	= new HashMap<String, SlashCommand>();
	
	/**
	 * List of "master" commands
	 */
	private static List<SlashCommand>	m_masterCommands	= new ArrayList<SlashCommand>();
	
	/**
	 * Keeps track of whether new slash commands have been added since last sort
	 */
	private static boolean m_masterCommandDirty = false;

	/**
	 * Get a list of available slash commands
	 * 
	 * @return Commands collection
	 */
	static public synchronized Collection<SlashCommand> getCommands()
	{
		if (m_masterCommandDirty)
		{
			Collections.sort(m_masterCommands);
			m_masterCommandDirty = false;
		}
		
		return m_masterCommands;
	}

	/**
	 * Parses for slash commands
	 * 
	 * @param text chat command to parse
	 */
	public static synchronized void parseSlashCommand(final String text)
	{
		if (text == null)
			return;
		
		// get the command
		final String[] words = UtilityFunctions.breakIntoWords(text, true, true);
		//final String[] words = text.split("\\s");
		if (words.length == 0)
			return;

		SlashCommand command = m_commands.get(words[0]);
		if (command != null)
		{
			String message = command.processCommand(words, text);
			if (message != null)
				GametableFrame.getGametableFrame().getChatPanel().logSystemMessage(message);
		}
	}

	/**
	 * Register a slash command for the chat panel
	 * 
	 * @param command
	 */
	public static synchronized void registerChatCommand(SlashCommand command)
	{
		m_commands.put("/" + command.getCommandName(), command);
		m_masterCommands.add(command);
		m_masterCommandDirty = true;

		// Register aliases
		for (String commandName : command.getCommandNameAliases())
			m_commands.put("/" + commandName, command);
	}

	/**
	 * Register the default chat commands
	 */
	public static synchronized void registerDefaultChatCommands()
	{
		registerChatCommand(new ClearLog());
		registerChatCommand(new Emote());
		registerChatCommand(new EmoteAs());
		registerChatCommand(new Goto());
		registerChatCommand(new Help());
		registerChatCommand(new Narrative());
		registerChatCommand(new PogList());
		registerChatCommand(new Tell());
		registerChatCommand(new Who());
	}
}
