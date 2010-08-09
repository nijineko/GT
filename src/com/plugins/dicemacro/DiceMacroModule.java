/*
 * DiceMacroModule.java
 * 
 * @created 2010-08-05
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

package com.plugins.dicemacro;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.module.Module;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.PogWindow;
import com.galactanet.gametable.ui.chat.SlashCommands;
import com.galactanet.gametable.util.Language;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;
import com.plugins.dicemacro.slashcmds.Macro;
import com.plugins.dicemacro.slashcmds.MacroDelete;
import com.plugins.dicemacro.slashcmds.PrivateRoll;
import com.plugins.dicemacro.slashcmds.Roll;

/**
 * 
 */
public class DiceMacroModule extends Module
{
	/**
	 * Singleton getter
	 * 
	 * @return DiceMacroModule instance
	 */
	public static DiceMacroModule getModule()
	{
		if (g_module == null)
			g_module = new DiceMacroModule();

		return g_module;
	}

	/**
	 * Constructor
	 */
	private DiceMacroModule()
	{
		SlashCommands.registerChatCommand(new Macro());
		SlashCommands.registerChatCommand(new MacroDelete());
		SlashCommands.registerChatCommand(new PrivateRoll());
		SlashCommands.registerChatCommand(new Roll());
	}

	/*
	 * @see com.galactanet.gametable.module.ModuleIF#getModuleName()
	 */
	@Override
	public String getModuleName()
	{
		return DiceMacroModule.class.getName();
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onInitializeUI()
	 */
	@Override
	public void onInitializeUI()
	{
		g_macroPanel = new MacroPanel();

		GametableFrame frame = GametableFrame.getGametableFrame();
		PogWindow panelBar = frame.getTabbedPane();
		panelBar.addTab(g_macroPanel, frame.getLanguageResource().DICE_MACROS);
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onPlayerAdded(com.galactanet.gametable.data.Player)
	 */
	@Override
	public void onPlayerAdded(Player newPlayer)
	{
		if (g_macroPanel != null)
			g_macroPanel.init_sendTo();
	}

	/**
	 * Macro panel
	 */
	private static MacroPanel	g_macroPanel;

	/**
	 * 
	 */
	private void addMacro(final DiceMacro dm)
	{
		addMacroForced(dm); // adds the macro to the collection of macros
		refreshMacroList();
	}

	/**
	 * @param name name of the macro
	 * @param macro macro content, the code of the macro
	 */
	public void addMacro(final String name, final String macro, final String parent)
	{
		final DiceMacro newMacro = new DiceMacro(); // creates a macro object
		boolean res = newMacro.init(macro, name, parent); // initializes the macro with its name and code
		if (!res) // if the macro creation failed, log the error and exit
		{
			GametableFrame frame = GametableFrame.getGametableFrame();
			frame.getChatPanel().logAlertMessage(frame.getLanguageResource().MACRO_ERROR);
			return;
		}
		addMacro(newMacro); // add the macro to the collection
	}

	/**
	 * 
	 * @param macro macro being added
	 */
	private void addMacroForced(final DiceMacro macro)
	{
		removeMacroForced(macro.getName()); // remove any macro with the same name
		m_macroMap.put(UtilityFunctions.normalizeName(macro.getName()), macro); // store the macro in the macro map making
		// sure conforms to java identifier rules, this is
		// it eliminates special characters from the name
	}

	/**
	 * 
	 * 
	 * Loads macros from the given file, if possible.
	 * 
	 * @param file File to load macros from.
	 * @throws SAXException If an error occurs.
	 */
	private void loadMacros(final File file) throws SAXException
	{
		try
		{
			// macros are contained in an XML document that will be parsed using a SAXParser
			// the SAXParser generates events for XML tags as it founds them.
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			final DiceMacroSaxHandler handler = new DiceMacroSaxHandler();
			parser.parse(file, handler);
			// we are done parsing the file. The handler contains all the macros, ready to be added to the macro map
			// clear the state
			m_macroMap.clear();
			for (DiceMacro macro : handler.getMacros())
			{
				addMacro(macro);
			}
		}
		catch (final IOException ioe)
		{
			throw new SAXException(ioe);
		}
		catch (final ParserConfigurationException pce)
		{
			throw new SAXException(pce);
		}

		refreshMacroList();
	}

	/**

   */
	private void saveMacros()
	{
		GametableFrame frame = GametableFrame.getGametableFrame();
		Language lang = frame.getLanguageResource();

		final File oldFile = m_actingFileMacros;
		m_actingFileMacros = UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "xml", true);
		if (m_actingFileMacros == null)
		{
			m_actingFileMacros = oldFile;
			return;
		}

		try
		{
			saveMacros(m_actingFileMacros);
			frame.getChatPanel().logSystemMessage(lang.MACRO_SAVE_DONE + " " + m_actingFileMacros.getPath());
		}
		catch (final IOException ioe)
		{
			Log.log(Log.SYS, ioe);
		}
	}

	/**
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void saveMacros(final File file) throws IOException
	{
		final XmlSerializer out = new XmlSerializer();
		out.startDocument(new BufferedWriter(new FileWriter(file)));
		out.startElement(DiceMacroSaxHandler.ELEMENT_DICE_MACROS);
		for (DiceMacro macro : m_macroMap.values())
		{
			macro.serialize(out);
		}
		out.endElement();
		out.endDocument();
	}
	
	/**
	 * Refresh the macro list
	 */
	private void refreshMacroList()
	{
		if (g_macroPanel != null)
			g_macroPanel.refreshMacroList();
	}

	/**
	 * 
	 * @param dm
	 */
	protected void removeMacro(final DiceMacro dm)
	{
		removeMacroForced(dm);
		refreshMacroList();
	}

	/**
	 * 
	 * @param name
	 */
	public void removeMacro(final String name)
	{
		removeMacroForced(name);
		refreshMacroList();
	}

	/**
	 * 
	 * @param macro
	 */
	private void removeMacroForced(final DiceMacro macro)
	{
		final String name = UtilityFunctions.normalizeName(macro.getName());
		m_macroMap.remove(name);
	}

	/**
	 * @param macro
	 */
	private void removeMacroForced(final String name)
	{
		final DiceMacro macro = getMacro(name);
		if (macro != null)
		{
			removeMacroForced(macro);
		}
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onSavePreferencesCompleted()
	 */
	@Override
	public void onSavePreferencesCompleted()
	{
		try
		{
			saveMacros(m_actingFileMacros);
		}
		catch (IOException e)
		{
		}
	}

	/**
	 * Invokes the addDieMacro dialog process.
	 */
	protected void addDieMacro()
	{
		Language lang = GametableFrame.getGametableFrame().getLanguageResource();

		// Create and display add macro dialog
		final NewMacroDialog dialog = new NewMacroDialog();
		dialog.setVisible(true);

		// If the user accepted the dialog (closed with Ok)
		if (dialog.isAccepted())
		{
			// extract the macro from the controls and add it
			final String name = dialog.getMacroName();
			final String parent = dialog.getMacroParent();
			final String macro = dialog.getMacroDefinition();
			if (getMacro(name) != null) // if there is a macro with that name
			{
				// Confirm that the macro will be replaced
				final int result = UtilityFunctions.yesNoDialog(GametableFrame.getGametableFrame(), lang.MACRO_EXISTS_1 + name + lang.MACRO_EXISTS_2 + macro
						+ lang.MACRO_EXISTS_3, lang.MACRO_REPLACE);
				if (result == UtilityFunctions.YES)
				{
					addMacro(name, macro, parent);
				}
			}
			else
			// if there is no macro with that name, then add it.
			{
				addMacro(name, macro, parent);
			}
		}
	}

	/**
	 * Pops up a dialog to load macros from a file.
	 */
	private void loadMacros()
	{
		GametableFrame frame = GametableFrame.getGametableFrame();
		Language lang = frame.getLanguageResource();

		final File openFile = UtilityFunctions.doFileOpenDialog(lang.OPEN, "xml", true);

		if (openFile == null)
		{
			// they cancelled out of the open
			return;
		}

		final int result = UtilityFunctions.yesNoDialog(frame, lang.MACRO_LOAD_WARN, lang.MACRO_LOAD_CONFIRM);
		if (result != UtilityFunctions.YES)
		{
			return;
		}

		m_actingFileMacros = openFile;
		if (m_actingFileMacros != null)
		{
			// actually do the load if we're the host or offline
			try
			{
				loadMacros(m_actingFileMacros);
				frame.getChatPanel().logSystemMessage(lang.MACRO_LOAD_DONE + " " + m_actingFileMacros);
			}
			catch (final SAXException saxe)
			{
				Log.log(Log.SYS, saxe);
			}
		}
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onLoadPreferencesCompleted()
	 */
	@Override
	public void onLoadPreferencesCompleted()
	{

		m_actingFileMacros = new File("macros.xml");
		addMacro("d20", "d20", null);
		try
		{
			if (m_actingFileMacros.exists())
			{
				loadMacros(m_actingFileMacros);
			}
		}
		catch (final SAXException se)
		{
			Log.log(Log.SYS, se);
		}
	}

	/**
	 * Singleton instance of the module
	 */
	private static DiceMacroModule				g_module		= null;

	/**
	 * File where macros are stored and loaded
	 */
	private File													m_actingFileMacros;

	/**
	 * Holds the mapping to all macros
	 */
	private final Map<String, DiceMacro>	m_macroMap	= new TreeMap<String, DiceMacro>();

	/**
	 * Get a specific macro by name
	 * 
	 * @param name
	 * @return
	 */
	protected DiceMacro getMacro(final String name)
	{
		final String realName = UtilityFunctions.normalizeName(name);
		return m_macroMap.get(realName);
	}

	/**
	 * @return Gets the list of macros.
	 */
	protected Collection<DiceMacro> getMacros()
	{
		return Collections.unmodifiableCollection(m_macroMap.values());
	}

	/**
	 * return a macro by name. If not found, then create a new one
	 * 
	 * @param term name of the macro to return
	 * @return an existing macro or a newly created one if not found
	 */
	public DiceMacro findMacro(final String term)
	{
		final String name = UtilityFunctions.normalizeName(term); // remove special characters from the name
		DiceMacro macro = getMacro(name);
		if (macro == null) // if no macro by that name
		{
			macro = new DiceMacro(); // create a new macro
			if (!macro.init(term, null, null)) // assign the name to it, but no macro code
			{
				macro = null; // if something went wrong, return null
			}
		}

		return macro;
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onLoadPreferences(org.omg.CORBA.DataInputStream)
	 */
	@Override
	public void onLoadPreferences(DataInputStream in)
	{
		try
		{
			m_actingFileMacros = new File(in.readUTF());
		}
		catch (IOException e)
		{
			Log.log(Log.SYS, e);
		}
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onSavePreferences(java.io.DataOutputStream)
	 */
	@Override
	public void onSavePreferences(DataOutputStream out)
	{
		try
		{
			out.writeUTF(m_actingFileMacros.getAbsolutePath());
		}
		catch (IOException e)
		{
			Log.log(Log.SYS, e);
		}
	}

	/*
	 * @see com.galactanet.gametable.module.Module#getModuleMenu()
	 */
	@Override
	public JMenu getModuleMenu()
	{
		final JMenu menu = new JMenu(GametableFrame.getGametableFrame().getLanguageResource().DICE);
		menu.add(getAddDiceMenuItem());
		menu.add(getDeleteDiceMenuItem());
		menu.add(getLoadDiceMenuItem());
		menu.add(getSaveDiceMenuItem());
		menu.add(getSaveAsDiceMenuItem());
		return menu;
	}

	/**
	 * @return the menu item
	 */
	private JMenuItem getAddDiceMenuItem()
	{
		final JMenuItem item = new JMenuItem(GametableFrame.getGametableFrame().getLanguageResource().MACRO_ADD);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				addDieMacro(); // this calls the function to add a new macro
			}
		});
		return item;
	}

	/**
	 * @return
	 */
	private JMenuItem getLoadDiceMenuItem()
	{
		final JMenuItem item = new JMenuItem(GametableFrame.getGametableFrame().getLanguageResource().MACRO_LOAD);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				loadMacros();
			}
		});
		return item;
	}

	/**
	 * 
	 * @return the new menu item
	 */
	private JMenuItem getDeleteDiceMenuItem()
	{
		final JMenuItem item = new JMenuItem(GametableFrame.getGametableFrame().getLanguageResource().MACRO_DELETE);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				GametableFrame frame = GametableFrame.getGametableFrame();
				Language lang = frame.getLanguageResource();

				final Object[] list = m_macroMap.values().toArray();
				// give them a list of macros they can delete
				final Object sel = JOptionPane.showInputDialog(frame, lang.MACRO_DELETE_INFO, lang.MACRO_DELETE, JOptionPane.PLAIN_MESSAGE, null, list,
						list[0]);
				if (sel != null)
				{
					removeMacro((DiceMacro) sel);
				}
			}
		});
		return item;
	}

	/**
	 * @return a menu item
	 */
	private JMenuItem getSaveAsDiceMenuItem()
	{
		final JMenuItem item = new JMenuItem(GametableFrame.getGametableFrame().getLanguageResource().MACRO_SAVE_AS);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				saveMacros();
			}
		});
		return item;
	}

	/**
	 * 
	 * @return the menu item
	 */
	private JMenuItem getSaveDiceMenuItem()
	{
		final JMenuItem item = new JMenuItem(GametableFrame.getGametableFrame().getLanguageResource().MACRO_SAVE);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					saveMacros(m_actingFileMacros);
				}
				catch (final IOException ioe)
				{
					Log.log(Log.SYS, ioe);
				}
			}
		});
		return item;
	}

}
