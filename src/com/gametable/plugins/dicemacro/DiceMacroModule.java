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

package com.gametable.plugins.dicemacro;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
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

import com.gametable.GametableApp;
import com.gametable.data.GameTableCore;
import com.gametable.data.Player;
import com.gametable.data.ChatEngineIF.MessageType;
import com.gametable.module.Module;
import com.gametable.plugins.dicemacro.slashcmds.Macro;
import com.gametable.plugins.dicemacro.slashcmds.MacroDelete;
import com.gametable.plugins.dicemacro.slashcmds.PrivateRoll;
import com.gametable.plugins.dicemacro.slashcmds.Roll;
import com.gametable.ui.GametableFrame;
import com.gametable.ui.PogWindow;
import com.gametable.ui.chat.SlashCommands;
import com.gametable.util.Log;
import com.gametable.util.UtilityFunctions;
import com.maziade.props.XProperties;

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
	}

	/*
	 * @see com.galactanet.gametable.module.ModuleIF#getModuleName()
	 */
	@Override
	public String getModuleName()
	{
		return DiceMacroModule.class.getName();
	}
	
	private GametableFrame m_frame = null;
	
	/*
	 * @see com.galactanet.gametable.module.Module#onInitializeCore(com.galactanet.gametable.data.GametableCore)
	 */
	@Override
	public void onInitializeCore(GameTableCore core)
	{
		SlashCommands.registerChatCommand(new Macro());
		SlashCommands.registerChatCommand(new MacroDelete());
		SlashCommands.registerChatCommand(new PrivateRoll());
		SlashCommands.registerChatCommand(new Roll());
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onInitializeUI()
	 */
	@Override
	public void onInitializeUI(GametableFrame frame)
	{
		g_macroPanel = new MacroPanel(frame);
		m_frame = frame;
		PogWindow panelBar = frame.getTabbedPane();
		panelBar.addTab(g_macroPanel, "Dice Macros");
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
			GametableApp.getCore().sendMessageLocal(MessageType.ALERT, "Error in macro");
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
		GameTableCore core = GametableApp.getCore();

		final File oldFile = m_actingFileMacros;
		m_actingFileMacros = UtilityFunctions.doFileSaveDialog("Save as", "xml", true);
		if (m_actingFileMacros == null)
		{
			m_actingFileMacros = oldFile;
			return;
		}

		try
		{
			saveMacros(m_actingFileMacros);
			core.sendMessageLocal(MessageType.SYSTEM, "Wrote macros to" + " " + m_actingFileMacros.getPath());
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
		out.startDocument(file);
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
	public void onSavePropertiesCompleted()
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
				final int result = UtilityFunctions.yesNoDialog(m_frame, "You already have a macro named \"" + name + "\", are you sure you want to replace it with \"" + macro
						+ "\"?", "Replace Macro?");
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
		final File openFile = UtilityFunctions.doFileOpenDialog("Open", "xml", true);

		if (openFile == null)
		{
			// they canceled out of the open
			return;
		}

		final int result = UtilityFunctions.yesNoDialog(m_frame, 
				"This will load a macro file, replacing all your existing macros. Are you sure you want to do this?", 
				"Confirm Load Macros");
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
				
				GameTableCore core = GametableApp.getCore();
				core.sendMessageLocal(MessageType.SYSTEM, "Loaded macros from" + " " + m_actingFileMacros);
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
	public void onLoadPropertiesCompleted()
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
	 * @see com.galactanet.gametable.module.Module#onInitializeProperties(com.maziade.props.XProperties)
	 */
	@Override
	public void onInitializeProperties(XProperties properties)
	{
		properties.addTextProperty(PROP_ACTING_MACRO_FILE, new File("dice.xml").getAbsolutePath(), false, "macros", -1);
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onApplyProperties(com.maziade.props.XProperties)
	 */
	@Override
	public void onApplyProperties(XProperties properties)
	{
		m_actingFileMacros = new File(properties.getTextPropertyValue(PROP_ACTING_MACRO_FILE));
	}

	/*
	 * @see com.galactanet.gametable.module.Module#onUpdateProperties(com.maziade.props.XProperties)
	 */
	@Override
	public void onUpdateProperties(XProperties properties)
	{
		properties.setPropertyValue(PROP_ACTING_MACRO_FILE, m_actingFileMacros.getAbsolutePath());
	}

	/*
	 * @see com.galactanet.gametable.module.Module#getModuleMenu()
	 */
	@Override
	public JMenu getModuleMenu()
	{
		final JMenu menu = new JMenu("Dice");
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
		final JMenuItem item = new JMenuItem("Add macro");
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
		final JMenuItem item = new JMenuItem("Load macros");
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
		final JMenuItem item = new JMenuItem("Delete macro");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				final Object[] list = m_macroMap.values().toArray();
				// give them a list of macros they can delete
				final Object sel = JOptionPane.showInputDialog(m_frame, "Select Dice Macro to remove:", "Delete macro", JOptionPane.PLAIN_MESSAGE, null, list,
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
		final JMenuItem item = new JMenuItem("Save macros as");
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
		final JMenuItem item = new JMenuItem("Save macros");
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

	/**
	 * Property name holding the file where to save macros
	 */
	private final static String PROP_ACTING_MACRO_FILE = DiceMacro.class.getName() + ".acting_file";
}
