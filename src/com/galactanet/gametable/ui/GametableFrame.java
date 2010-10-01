/*
 * GametableFrame.java: GameTable is in the Public Domain.
 */

package com.galactanet.gametable.ui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.data.prefs.PreferenceDescriptor;
import com.galactanet.gametable.data.prefs.Preferences;
import com.galactanet.gametable.module.Module;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.ui.GametableCanvas.BackgroundColor;
import com.galactanet.gametable.ui.GametableCanvas.GridModeID;
import com.galactanet.gametable.ui.chat.ChatLogEntryPane;
import com.galactanet.gametable.ui.chat.ChatPanel;
import com.galactanet.gametable.ui.chat.SlashCommands;
import com.galactanet.gametable.ui.net.*;
import com.galactanet.gametable.ui.net.NetGroupAction.Action;
import com.galactanet.gametable.util.*;
import com.maziade.tools.XMLUtils;
import com.maziade.tools.XMLUtils.XMLOutputProperties;
import com.plugins.activepogs.ActivePogsModule;
import com.plugins.cards.CardModule;
import com.plugins.dicemacro.DiceMacroModule;
import com.plugins.network.NetworkModule;
import com.plugins.network.PacketSourceState;

/**
 * The main Gametable Frame class. This class handles the display of the application objects and the response to user
 * input
 * 
 * @author sephalon
 * 
 *         #GT-AUDIT GametableFrame
 */
public class GametableFrame extends JFrame implements ActionListener, MapElementRepositoryIF
{
	/**
	 * This class provides a mechanism to store the active tool in the gametable canvas
	 */
	class ToolButtonAbstractAction extends AbstractAction
	{
		/**
     * 
     */
		private static final long	serialVersionUID	= 6185807427550145052L;

		int												m_id;																		// Which user triggers this action

		/**
		 * Constructor
		 * 
		 * @param id Id from the control triggering this action
		 */
		ToolButtonAbstractAction(final int id)
		{
			m_id = id;
		}

		public void actionPerformed(final ActionEvent event)
		{
			if (getFocusOwner() instanceof JTextField)
			{
				return; // A JTextField is not an active tool.
				// No need to save the active tool in the gametable canvas
			}
			getGametableCanvas().setActiveTool(m_id); // In any other case, save the active tool in the gametable canvas
		}
	}

	/**
	 * Action listener for tool buttons.
	 */
	class ToolButtonActionListener implements ActionListener
	{
		int	m_id;

		ToolButtonActionListener(final int id)
		{
			m_id = id;
		}

		/*
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(final ActionEvent e)
		{
			getGametableCanvas().setActiveTool(m_id);
		}
	}

	/**
	 * Xml strings with a font definitions, standard tags for messages
	 */
	public final static String		ALERT_MESSAGE_FONT				= "<b><font color=\"#FF0000\">";

	/**
	 * Array of standard colors used
	 */
	public final static Integer[]	COLORS										= { new Integer(new Color(0, 0, 0).getRGB()),
			new Integer(new Color(198, 198, 198).getRGB()), new Integer(new Color(0, 0, 255).getRGB()), new Integer(new Color(0, 255, 0).getRGB()),
			new Integer(new Color(0, 255, 255).getRGB()), new Integer(new Color(255, 0, 0).getRGB()), new Integer(new Color(255, 0, 255).getRGB()),
			new Integer(new Color(255, 255, 0).getRGB()), new Integer(new Color(255, 255, 255).getRGB()), new Integer(new Color(0, 0, 132).getRGB()),
			new Integer(new Color(0, 132, 0).getRGB()), new Integer(new Color(0, 132, 132).getRGB()), new Integer(new Color(132, 0, 0).getRGB()),
			new Integer(new Color(132, 0, 132).getRGB()), new Integer(new Color(132, 132, 0).getRGB()), new Integer(new Color(132, 132, 132).getRGB()) };

	/**
	 * The version of the communications protocol used by this build. This needs to change whenever an incompatibility
	 * arises between versions.
	 */
	public final static String		DIEROLL_MESSAGE_FONT			= "<b><font color=\"#990022\">";
	public final static String		EMOTE_MESSAGE_FONT				= "<font color=\"#004477\">";

	public final static String		END_ALERT_MESSAGE_FONT		= "</b></font>";
	public final static String		END_DIEROLL_MESSAGE_FONT	= "</b></font>";

	public final static String		END_EMOTE_MESSAGE_FONT		= "</font>";
	public final static String		END_PRIVATE_MESSAGE_FONT	= "</font>";

	public final static String		END_SAY_MESSAGE_FONT			= "</font>";
	public final static String		END_SYSTEM_MESSAGE_FONT		= "</font>";

	public final static String		PRIVATE_MESSAGE_FONT			= "<font color=\"#009900\">";

	public final static String		SAY_MESSAGE_FONT					= "<font color=\"#007744\">";
	public final static String		SYSTEM_MESSAGE_FONT				= "<font color=\"#666600\">";

	private final static boolean	DEBUG_FOCUS								= false;

	/**
	 * Default Character name for when there is no prefs file.
	 */
	private static final String		DEFAULT_CHARACTER_NAME		= "Anonymous";

	/**
	 * The global gametable instance.
	 */
	private static GametableFrame	g_gameTableFrame;

	private static List<Module>						g_modules		= new ArrayList<Module>();

	// The current file path used by save and open.
	// NULL if unset.
	private static File							m_mapExportSaveFolder			= new File("./saves");

	/**
	 * @return
	 */
	private final static String	MENU_ACCELERATOR	= getMenuAccelerator();

	
	// private final static boolean USE_NEW_CHAT_PANE = true;

	/**
     * 
     */
	private static final long			serialVersionUID				= -1997597054204909759L;
	/**
	 * todo #Plugins Might be nice if it returned an interface to clean up API for plugins
	 * 
	 * @return The global GametableFrame instance.
	 */
	public static GametableFrame getGametableFrame()
	{
		if (g_gameTableFrame == null)
		{
			// Constructor auto-assigns g_gameTableFrame
			new GametableFrame();
		}
		
		return g_gameTableFrame;
	}

	/**
	 * Register a new module with GameTable
	 * 
	 * @param module
	 */
	public static void registerModule(Module module)
	{
		g_modules.remove(module);
		g_modules.add(module);
	}

	private static String getMenuAccelerator()
	{
		switch (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
		{
		case Event.CTRL_MASK:
			return "ctrl";
		case Event.META_MASK:
			return "meta";
			// case Event.ALT_MASK:
			// return "alt";
		case Event.SHIFT_MASK:
			return "shift";
		default: // Others?
			return "ctrl";
		}
	}

	// Language variables
	private Language								m_languageResource				= new Language(GametableApp.LANGUAGE);

	
	public double										grid_multiplier						= 5.0;

	public String										grid_unit									= "ft";

	public File											m_actingFilePublic;

	public String										m_characterName						= DEFAULT_CHARACTER_NAME;																			// The
																																																														// character
																																																														// name

	public Color										m_drawColor								= Color.BLACK;

	public int											m_nextPlayerId = 1;	// Player ID 0 is reserved for host
	// the id that will be assigned to the change made
	public int											m_nextStateId;

	public String										m_playerName							= System.getProperty("user.name");
	
	private javax.swing.Action						m_actionLoadMap;

	private javax.swing.Action						m_actionLoadPrivateMap;

	private javax.swing.Action						m_actionLoadPublicMap;

	private boolean									m_bMaximized;																																						// Is
																																																														// the
																																																														// frame
																																																														// maximized?

	// map
																																																														// pane
																																																														// is
																																																														// really
																																																														// a
																																																														// split
																																																														// between
																																																														// the
																																																														// map
																																																														// and
																																																														// the
																																																														// chat
	// and the chat pane
	private final JPanel						m_canvasPane							= new JPanel(new BorderLayout());															// This

	// is
																																																														// the
																																																														// map
	private ChatPanel								m_chatPanel								= null;																												// Panel

	// main
																																																														// toolbar
	private final JComboBox					m_colorCombo							= new JComboBox(COLORS);																				// Combo
																																																														// box
																																																														// for
																																																														// colore
	private JMenuItem								m_disconnectMenuItem;

	private PeriodicExecutorThread	m_executorThread;
	// is
																																																														// the
																																																														// pane
																																																														// containing
																																																														// the
																																																														// map
	private final GametableCanvas		m_gametableCanvas					= new GametableCanvas();																				// This

	private JMenuItem								m_startNetworkingMenuItem;

	

	private final JCheckBoxMenuItem	m_noGridModeMenuItem			= new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_NONE);
	private final JCheckBoxMenuItem	m_hexGridModeMenuItem			= new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_HEX);



	private long										m_lastTickTime						= 0;
	private List<GameTableFrameListener>	m_listeners	= new ArrayList<GameTableFrameListener>();
	private final JSplitPane				m_mapChatSplitPane				= new JSplitPane();																						// The

	// The map-pog split pane goes in the center
	private final JSplitPane				m_mapPogSplitPane					= new JSplitPane();																						// Split

	// which player I am
	private int											m_myPlayerIndex;
	
	/**
	 * List of connected players
	 */
	private final List<Player>						m_players;
	
	/**
	 * Instance of player listener to react to player data changes
	 */
	private final PlayerListenerIF	m_playerListener;
	
	/**
	 * Unmodifiable list, synchronized to the players list
	 */
	private final List<Player>						m_unmodifiablePlayers;
	
	private MapElementTypeLibrary		m_pogLibrary							= null;

	// Pog
																																																														// pane
																																																														// is
																																																														// tabbed
	private PogPanel								m_pogPanel								= null;																												// one
																																																														// tab
																																																														// is
																																																														// the
																																																														// Pog
																																																														// Panel
																																																														// between
																																																														// Pog
																																																														// pane
																																																														// and
																																																														// map
																																																														// pane
	private final PogWindow					m_pogsTabbedPane					= new PogWindow();																							// The

	private final Preferences				m_preferences							= new Preferences();
																																																														private final JCheckBox					m_randomRotate						= new JCheckBox(getLanguageResource().RANDOM_ROTATE);					// #randomrotate
																																																														private final JCheckBox					m_showNamesCheckbox				= new JCheckBox(getLanguageResource().SHOW_POG_NAMES);

	private final JCheckBoxMenuItem	m_squareGridModeMenuItem	= new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_SQUARE);
																																																														// The status goes at the bottom of the pane
	private final JLabel						m_status									= new JLabel(" ");																							// Status
																																																														// Bar
																																																														// for
																																																														// chat
	private ChatLogEntryPane				m_textEntry								= null;
																																																														private JCheckBoxMenuItem				m_togglePrivateMapMenuItem;
																																																														// Controls in the Frame
	// The toolbar goes at the top of the pane
	private final JToolBar					m_toolBar									= new JToolBar();																							// The

	private final ButtonGroup				m_toolButtonGroup					= new ButtonGroup();

	private JToggleButton						m_toolButtons[]						= null;

	private final ToolManager				m_toolManager							= new ToolManager();

	/**
	 * List of players currently typing
	 */
	private final List<Player>			m_typingPlayers	= new ArrayList<Player>();

	// window size and position
	private Point										m_windowPos;

	private Dimension								m_windowSize;

	private JFrame									pogWindow									= null;
	// private boolean b_pogWindowDocked = true;
	// private boolean b_chatWindowDocked = true;

	JComboBox												m_gridunit;																																							// ComboBox

	// for
																																																														// grid
																																																														// units
	/*
	 * Added variables below in order to accommodate grid unit multiplier
	 */
	JTextField											m_gridunitmultiplier;

	// --- MenuItems ---

	/**
	 * Construct the frame
	 */
	private GametableFrame()
	{
		g_gameTableFrame = this;
		m_players = new ArrayList<Player>();
		m_unmodifiablePlayers = Collections.unmodifiableList(m_players);
		m_playerListener = new PlayerAdapter() {
			/*
			 * @see com.galactanet.gametable.data.PlayerAdapter#onPointingLocationChanged(com.galactanet.gametable.data.Player, boolean, com.galactanet.gametable.data.MapCoordinates, com.galactanet.gametable.net.NetworkEvent)
			 */
			@Override
			public void onPointingLocationChanged(Player player, boolean pointing, MapCoordinates location, NetworkEvent netEvent)
			{
				if (shouldPropagateChanges(netEvent))
				{
					if (pointing)
						sendBroadcast(NetShowPointingMarker.makePacket(getPlayerIndex(player), location, true));
					else
						sendBroadcast(NetShowPointingMarker.makePacket(getPlayerIndex(player), MapCoordinates.ORIGIN, false));
				}
				
				repaint();
			}
		};

		try
		{
			initialize(); // Create the menu, controls, etc.
		}
		catch (final Exception e)
		{
			Log.log(Log.SYS, e);
		}
	}

	/**
	 * actionPerformed is an event handler for some of the controls in the frame
	 */
	public void actionPerformed(final ActionEvent e)
	{
		/*
		 * Added in order to accomodate grid unit multiplier
		 */
		if (e.getSource() == m_gridunit)
		{
			// If the event is triggered by the grid unit drop down,
			// get the selected unit
			grid_unit = (String) (m_gridunit.getSelectedItem());
		}

		if (e.getSource() == m_colorCombo)
		{
			// If the event is triggered by the color drow down,
			// Get the selected color
			final Integer col = (Integer) m_colorCombo.getSelectedItem();
			m_drawColor = new Color(col.intValue());
		}
		else if (e.getSource() == m_noGridModeMenuItem)
		{
			// If the event is triggered by the "No Grid Mode" menu item then
			// remove the grid from the canvas
			// Set the Gametable canvas in "No Grid" mode
			setGridMode(GridModeID.NONE);

			// Notify other players
			sendSystemMessageBroadcast(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_GRID_CHANGE);
		}
		else if (e.getSource() == m_squareGridModeMenuItem)
		{
			// If the event is triggered by the "Square Grid Mode" menu item,
			// adjust the canvas accordingly
			// Set the Gametable canvas in "Square Grid mode"
			getGametableCanvas().m_gridMode = getGametableCanvas().m_squareGridMode;
			sendBroadcast(NetSetGridMode.makePacket(GridModeID.SQUARES));
			// Check and uncheck menu items
			updateGridModeMenu();
			// Repaint the canvas
			getGametableCanvas().repaint();
			// Notify other players
			sendSystemMessageBroadcast(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_GRID_CHANGE);
		}
		else if (e.getSource() == m_hexGridModeMenuItem)
		{
			// If the event is triggered by the "Hex Grid Mode" menu item,
			// adjust the canvas accordingly
			// Set the Gametable canvas in "Hex Grid Mode"
			getGametableCanvas().m_gridMode = getGametableCanvas().m_hexGridMode;
			sendBroadcast(NetSetGridMode.makePacket(GridModeID.HEX));
			// Check and uncheck menu items
			updateGridModeMenu();
			// Repaint the canvas
			getGametableCanvas().repaint();
			// Notify other players
			sendSystemMessageBroadcast(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_GRID_CHANGE);
		}
	}

	/**
	 * Adds a GameTableFrameListener to this map
	 * 
	 * @param listener Listener to call
	 */
	public void addListener(GameTableFrameListener listener)
	{
		m_listeners.remove(listener);
		m_listeners.add(listener);
	}

	/**
	 * Adds a player to the player list
	 * 
	 * @param player
	 */
	private void addPlayer(final Player player)
	{
		m_players.add(player);
		player.addPlayerListener(m_playerListener);

		for (Module module : g_modules)
			module.onPlayerAdded(player);

		// todo #Plugins Chat panel as plugin (?)
		m_chatPanel.init_sendTo();
	}
	
	/**
	 * Remove a player from the list
	 * @param player
	 */
	private void removePlayer(Player player)
	{
		if (!m_players.remove(player))
			return;
		
		player.removePlayerListener(m_playerListener);
		
		for (Module module : g_modules)
			module.onPlayerRemoved(player);
	}
	
	/**
	 * Remove all players 
	 */
	private void clearAllPlayers()
	{
		// Making a copy so we can clear the list before calling listeners
		List<Player> players = new ArrayList<Player>(m_players);	
		m_players.clear();
		m_nextPlayerId = 1;
		
		for (Player player : players)
		{
			player.removePlayerListener(m_playerListener);
			
			for (Module module : g_modules)
				module.onPlayerRemoved(player);
		}
	}

	/**
	 * Updates the frame size and position based on the preferences stored.
	 */
	public void applyWindowInfo()
	{
		final Point locCopy = new Point(m_windowPos);
		setSize(m_windowSize);
		m_windowPos = locCopy; // this copy and use of a local variable seems unnecessary
		setLocation(locCopy);
		if (m_bMaximized) // if the preferences says the screen should be maximized
		{
			setExtendedState(MAXIMIZED_BOTH); // Maximize screen
		}
		else
		{
			setExtendedState(NORMAL); // Otherwise, set the screen to normal
		}
	}
	
	/**
	 * Center the view on the map at given coordinates
	 * @param modelCenter Coordinates we want to center on 
	 * @param zoomLevel Requested zoom level
	 */
	public void centerView(MapCoordinates modelCenter, final int zoomLevel)
	{
		if (getNetworkStatus() != NetworkStatus.DISCONNECTED)
			sendBroadcast(NetRecenterMap.makePacket(modelCenter, zoomLevel));
		
		m_gametableCanvas.centerView(modelCenter, zoomLevel);
	}


	/**
	 * grumpy function that throws an exception if we are not the host of a network game
	 * 
	 * @throws IllegalStateException
	 */
	public void confirmHost() throws IllegalStateException
	{
		if (getNetworkStatus() != NetworkStatus.HOSTING)
		{
			throw new IllegalStateException(getLanguageResource().CONFIRM_HOST_FAIL);
		}
	}

	/**
	 * throws an exception is the current status is not NetworkStatus.JOINED
	 * 
	 * @throws IllegalStateException
	 */
	public void confirmJoined() throws IllegalStateException
	{
		if (getNetworkStatus() != NetworkStatus.CONNECTED)
		{
			throw new IllegalStateException("confirmJoined failure");
		}
	}

	/**
	 * Handles a drop of the network connection
	 * 
	 * @param conn network connection
	 */
	private void onNetworkConnectionDropped(final NetworkConnectionIF conn)
	{
		if (getNetworkStatus() == NetworkStatus.CONNECTED) // if we were connected before
		{
			// we lost our connection to the host
			m_chatPanel.logAlertMessage(getLanguageResource().CONNECTION_LOST);
			disconnect(); // do any disconnection processing
			return;
		}

		// find the player who owns that connection
		final Player dead = getPlayerFromConnection(conn);
		if (dead != null) // if we found the player
		{
			// remove this player
			removePlayer(dead);			
			sendPlayersInformation(); // send updated list of players
			// notify other users
			sendSystemMessageBroadcast(dead.getPlayerName() + " " + getLanguageResource().CONNECTION_LEFT);
		}
		else
		// if we didn't find the player then the connection failed while login in
		{
			sendAlertMessageLocal(getLanguageResource().CONNECTION_REJECTED);
		}
	}

	/**
	 * Creates a copy of a specified map element and places it on the current map
	 *  
	 * @param mapElement MapElement instance to copy
	 */
	public void copyMapElement(final MapElement mapElement)
	{
		final MapElement newMapElement = new MapElement(mapElement);
		getGametableCanvas().getActiveMap().addMapElement(newMapElement);
	}

	/**
	 * Disconnect from a joined game or terminate a hosted game. 
	 */
	public void disconnect()
	{
		if (getNetworkStatus() == NetworkStatus.DISCONNECTED)
			return;
		
		getNetworkModule().disconnect();
	}
	
	private void onNetworkConnectionClosed()
	{
		m_startNetworkingMenuItem.setEnabled(true); // enable the menu item to join an existing game
		m_disconnectMenuItem.setEnabled(false); // disable the menu item to disconnect from the game

		// Make me the only player in the game
		clearAllPlayers();
		final Player me = new Player(m_playerName, m_characterName, -1, true);
		addPlayer(me);
		m_myPlayerIndex = 0;
		
		setTitle(GametableApp.VERSION);

		// TODO #Networking what is this dumping thing?
		// we might have disconnected during initial data receipt
		PacketSourceState.endHostDump();

		m_chatPanel.logSystemMessage(getLanguageResource().DISCONNECTED);
		updateStatus();
	}

	/**
	 * erases everything from the game canvas
	 */
	public void eraseAll()
	{
		getGametableCanvas().getActiveMap().clearLineSegments();
		eraseAllPogs();
	}

	public GroupManager getActiveGroupManager()
	{
		return getGametableCanvas().getActiveMap().getGroupManager();
	}
	
	public GroupManager getPublicGroupManager()
	{
		return getGametableCanvas().getPublicMap().getGroupManager();
	}

	public ChatPanel getChatPanel()
	{
		return m_chatPanel;
	}

	/**
	 * @return Returns the gametableCanvas.
	 */
	public GametableCanvas getGametableCanvas()
	{
		return m_gametableCanvas;
	}

	/**
	 * @return Returns the lang.
	 */
	public Language getLanguageResource()
	{
		return m_languageResource;
	}

	/*
	 * @see com.galactanet.gametable.data.MapElementRepositoryIF#getMapElement(com.galactanet.gametable.data.MapElementID)
	 */
	@Override
	public MapElement getMapElement(MapElementID id)
	{
		MapElement el = m_gametableCanvas.getPublicMap().getMapElement(id);
		if (el == null)
			el = m_gametableCanvas.getPrivateMap().getMapElement(id);

		return el;
	}
	
	/**
	 * Get the requested GameTableMap instance
	 * @param type active, private or public map
	 * @return GameTableMap
	 */
	public GameTableMap getGameTableMap(GameTableMapType type)
	{
		switch (type)
		{
		case ACTIVE:
			return m_gametableCanvas.getActiveMap();
			
		case PRIVATE:
			return m_gametableCanvas.getPrivateMap();
			
		case PUBLIC:
			return m_gametableCanvas.getPublicMap();
		}
		
		throw new IllegalArgumentException("There is no map of type " + type);
	}

	/**
	 * @return The player representing this client.
	 */
	public Player getMyPlayer()
	{
		if (m_players.size() == 0)
			return null;

		return m_players.get(getMyPlayerIndex());
	}

	/**
	 * @return The id of the player representing this client.
	 */
	public int getMyPlayerId()
	{
		Player p = getMyPlayer();
		if (p == null)
			return 0;

		return p.getID();
	}
	
	/**
	 * Get requested player by ID
	 * @param playerID ID of player we are looking for
	 * @return player object or null
	 */
	public Player getPlayerByID(int playerID)
	{
		for (Player player : m_players)
		{
			if (player.getID() == playerID)
				return player;
		}
		
		return null;
	}

	/**
	 * @return Returns the myPlayerIndex.
	 */
	public int getMyPlayerIndex()
	{
		return m_myPlayerIndex;
	}

	/**
	 * gets and id for a state
	 * 
	 * @return the next id number
	 */
	public int getNewStateId()
	{
		return m_nextStateId++;
	}

	/**
	 * gets the player associated with a connection
	 * 
	 * @param conn connection to use to find the player
	 * @return the player object associated with the connection
	 */
	public Player getPlayerFromConnection(final NetworkConnectionIF conn)
	{
		for (int i = 0; i < m_players.size(); i++)
		{
			final Player plr = m_players.get(i);
			if (conn == plr.getConnection())
			{
				return plr;
			}
		}

		return null;
	}

	/**
	 * Finds the index of a given player.
	 * 
	 * @param player Player to find index of.
	 * @return Index of the given player, or -1.
	 */
	public int getPlayerIndex(final Player player)
	{
		return m_players.indexOf(player);
	}

	/**
	 * Get requested player by index
	 * @param playerIndex index of player we are looking for
	 * @return player object or null
	 */
	public Player getPlayer(int playerIndex)
	{
		if (playerIndex < 0 || playerIndex >= m_players.size())
			return null;
		
		return m_players.get(playerIndex);
	}

	/**
	 * @return Returns the player list.
	 */
	public List<Player> getPlayers()
	{
		return m_unmodifiablePlayers;
	}

	/**
	 * @return The root pog library.
	 */
	public MapElementTypeLibrary getPogLibrary()
	{
		return m_pogLibrary;
	}

	/**
	 * @return The pog panel.
	 */
	public PogPanel getPogPanel()
	{
		return m_pogPanel;
	}

	/**
	 * @return The pog window
	 */
	public JFrame getPogWindow()
	{
		return pogWindow;
	}

	/**
	 * @return The preferences object.
	 */
	public Preferences getPreferences()
	{
		return m_preferences;
	}

	/**
	 * builds and returns the "Save map as" menu item
	 * 
	 * @return the menu item
	 */
	public JMenuItem getSaveAsMapMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().MAP_SAVE_AS);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " shift pressed S"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				m_actingFilePublic = UtilityFunctions.doFileSaveDialog(getLanguageResource().SAVE_AS, "xml", true);
				if (m_actingFilePublic != null)
					saveToXML(m_actingFilePublic);

				/*
				 * 
				 * 
				 * if (getGametableCanvas().isPublicMap()) { m_actingFilePublic =
				 * UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true); if (m_actingFilePublic != null) {
				 * saveState(getGametableCanvas().getActiveMap(), m_actingFilePublic); } } else { m_actingFilePrivate =
				 * UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true); if (m_actingFilePrivate != null) {
				 * saveState(getGametableCanvas().getActiveMap(), m_actingFilePrivate); } }
				 */
			}
		});

		return item;
	}

	/**
	 * builds and returns the "save map" menu item
	 * 
	 * @return the menu item
	 */
	public JMenuItem getSaveMapMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().MAP_SAVE);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed S"));
		item.addActionListener(new ActionListener() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (m_actingFilePublic == null)
					m_actingFilePublic = UtilityFunctions.doFileSaveDialog(getLanguageResource().SAVE_AS, "xml", true);

				if (m_actingFilePublic != null)
					saveToXML(m_actingFilePublic);

				/*
				 * if (getGametableCanvas().isPublicMap()) { if (m_actingFilePublic == null) { m_actingFilePublic =
				 * UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true); }
				 * 
				 * if (m_actingFilePublic != null) { // save the file saveState(getGametableCanvas().getActiveMap(),
				 * m_actingFilePublic); } } else { if (m_actingFilePrivate == null) { m_actingFilePrivate =
				 * UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true); }
				 * 
				 * if (m_actingFilePrivate != null) { // save the file saveState(getGametableCanvas().getActiveMap(),
				 * m_actingFilePrivate); } }
				 */
			}
		});

		return item;
	}

	/**
	 * 
	 * @return
	 */
	public PogWindow getTabbedPane()
	{
		return m_pogsTabbedPane;
	}

	/**
	 * gets the tools manager
	 * 
	 * @return the tool manager
	 */
	public ToolManager getToolManager()
	{
		return m_toolManager;
	}
	
	/**
	 * Changes the currently used grid mode
	 * @param gridMode grid mode
	 */
	public void setGridMode(GridModeID gridMode)
	{
		setGridMode(gridMode, null);
	}

	/**
	 * Changes the currently used grid mode
	 * @param gridMode grid mode
	 * @param netEvent Network event that triggered the change.  Null for none.
	 */
	public void setGridMode(GridModeID gridMode, NetworkEvent netEvent)
	{
		// Request change of grid mode from canvas
		getGametableCanvas().setGridModeByID(gridMode);
		
		// Update menus @revise menu system
		updateGridModeMenu();
		
		// todo trigger listeners?

		repaint();
		
		if (shouldPropagateChanges(netEvent))
		{
			sendBroadcast(NetSetGridMode.makePacket(gridMode));
		}
	}
	
	private void onNetworkConnectionEstablished(NetworkConnectionIF conn)
	{
		if (getNetworkStatus() == NetworkStatus.HOSTING)
		{
			// clear out all players
			clearAllPlayers();
			final Player me = new Player(m_playerName, m_characterName, 0, true); // this means the host is always
			m_nextPlayerId = 1;
			addPlayer(me);
			m_myPlayerIndex = 0;

			me.setIsHostPlayer(true);
			
			m_startNetworkingMenuItem.setEnabled(false); // disable the join menu item
			m_disconnectMenuItem.setEnabled(true); // enable the disconnect menu item
			
			setTitle(GametableApp.VERSION + " - " + me.getCharacterName());

			// Advise listeners 
			for (GameTableFrameListener listener : m_listeners)
				listener.onHostingStarted();			
		}
		else
		{
			// Player connection

			try
			{
				m_loggingIn = true;
				
				// now that we've successfully made a connection, let the host know who we are
				clearAllPlayers();
				final Player me = new Player(m_playerName, m_characterName, -1, true);
				me.setConnection(conn);
				addPlayer(me);
				m_myPlayerIndex = 0;

				// reset game data
				m_gametableCanvas.setScrollPosition(0, 0);
				m_gametableCanvas.getPublicMap().clearMapElementInstances();
				m_gametableCanvas.getPublicMap().clearLineSegments();
				
				// Send player info to the host
				send(NetSendPlayerInfo.makePacket(me, m_networkModule.getPassword()), conn);
				
				m_loggingIn = false;

				// TODO #Networking What is this?
				PacketSourceState.beginHostDump();

				// and now we're ready to pay attention

				m_chatPanel.logSystemMessage(getLanguageResource().JOINED);

				m_startNetworkingMenuItem.setEnabled(false); // disable the join menu item
				m_disconnectMenuItem.setEnabled(true); // enable the disconnect menu item
				
				setTitle(GametableApp.VERSION + " - " + me.getCharacterName());						
			}
			catch (final Exception ex)
			{
				Log.log(Log.SYS, ex);
				m_chatPanel.logAlertMessage(getLanguageResource().CONNECT_FAIL);
				setTitle(GametableApp.VERSION);
				PacketSourceState.endHostDump();
			}
		}
	}
	
	/**
	 * Checks whether the system is currently into the 'logging in' process of connecting to a network game.
	 * Some networking components will not want to propagate changes to the network while logging in.
	 * @return true while logging in
	 */
	public boolean isLoggingIn()
	{
		return m_loggingIn;
	}


	/**
	 * joins a network game
	 */
	private void startNetworkGame()
	{
		boolean res = showNetworkingDialog();
		
		// Was the dialog accepted?
		if (!res)
			return;

		try
		{
			m_hostConnection = m_networkModule.connect();
		}
		catch (IllegalStateException e)
		{
			m_chatPanel.logAlertMessage(e.getMessage());
			return;
		}
		catch (final Exception ex)
		{
			Log.log(Log.SYS, ex);
			m_chatPanel.logAlertMessage(getLanguageResource().CONNECT_FAIL);
		}
	}

	public void loadFromXML(File file, boolean loadPublic, boolean loadPrivate)
	{
		Document doc;
		try
		{
			doc = XMLUtils.parseXMLDocument(file);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this, "Error loading " + file.getName() + " : " + e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
			// todo proper error handling
			return;
		}

		Element root = doc.getDocumentElement();
		if (!root.getTagName().equals("gt"))
		{
			JOptionPane.showMessageDialog(this, "Invalid file format", null, JOptionPane.ERROR_MESSAGE);
			return;
		}

		try
		{
			PacketSourceState.beginFileLoad();

			if (loadPrivate && loadPublic)
				MapElementID.clear();

			XMLSerializeConverter converter = new XMLSerializeConverter();

			if (loadPublic)
			{
				Element publicEl = XMLUtils.getFirstChildElementByTagName(root, "public_map");
				m_gametableCanvas.getPublicMap().deserialize(publicEl, converter, null);
			}

			if (loadPrivate)
			{
				Element privateEl = XMLUtils.getFirstChildElementByTagName(root, "private_map");
				m_gametableCanvas.getPrivateMap().deserialize(privateEl, converter, null);
			}

			if (loadPublic && loadPrivate)
			{
				loadGridFromXML(root, converter);
				loadLockedElementsFromXML(root, converter);

				// Hook for modules to load data from save file
				Element modulesEl = XMLUtils.getFirstChildElementByTagName(root, "modules");

				if (modulesEl != null)
				{
					for (Module module : g_modules)
					{
						if (module.canSaveToXML())
						{
							Element moduleEl = XMLUtils.findFirstChildElement(modulesEl, "module", "name", module.getModuleName());

							if (moduleEl != null)
							{
								module.loadFromXML(moduleEl, converter);
							}
						}
					}
				}
			}
		}
		finally
		{
			PacketSourceState.endFileLoad();
		}
	}

	public void loadPog()
	{
		final File openFile = UtilityFunctions.doFileOpenDialog(getLanguageResource().OPEN, ".pog", true);

		if (openFile == null)
		{ // they cancelled out of the open
			return;
		}
		try
		{
			final FileInputStream infile = new FileInputStream(openFile);
			final DataInputStream dis = new DataInputStream(infile);
			final MapElement nPog = new MapElement(dis);

			if (!nPog.getMapElementType().isLoaded())
			{ // we need this image
				NetRequestImage.requestMapElementImageFile(null, nPog);
			}
			m_gametableCanvas.getActiveMap().addMapElement(nPog);
		}
		catch (final IOException ex1)
		{
			Log.log(Log.SYS, ex1);
		}
	}

	/**
	 * loads preferences from file
	 */
	public void loadPrefs()
	{
		final File file = getPreferenceFile();
		if (!file.exists()) // if the file doesn't exist, set some hard-coded defaults and return
		{
			// DEFAULTS
			m_mapChatSplitPane.setDividerLocation(0.7);
			m_mapPogSplitPane.setDividerLocation(150);
			m_windowSize = new Dimension(800, 600);
			final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			m_windowPos = new Point((screenSize.width - m_windowSize.width) / 2, (screenSize.height - m_windowSize.height) / 2);
			m_bMaximized = false;
			getGametableCanvas().setPrimaryScroll(getGametableCanvas().getPublicMap(), 0, 0);
			getGametableCanvas().setZoomLevel(0);
			applyWindowInfo();

			m_showNamesCheckbox.setSelected(false);
			m_randomRotate.setSelected(false); // #randomrotate

			for (Module module : g_modules)
				module.onLoadPreferencesCompleted();

			return;
		}

		// try reading preferences from file
		try
		{
			final FileInputStream prefFile = new FileInputStream(file);
			final DataInputStream prefDis = new DataInputStream(prefFile);

			m_playerName = prefDis.readUTF();
			m_characterName = prefDis.readUTF();
			
		// TODO #Networking Load networking preferences

			getGametableCanvas().setPrimaryScroll(getGametableCanvas().getPublicMap(), prefDis.readInt(), prefDis.readInt());
			getGametableCanvas().setZoomLevel(prefDis.readInt());

			m_windowSize = new Dimension(prefDis.readInt(), prefDis.readInt());
			m_windowPos = new Point(prefDis.readInt(), prefDis.readInt());
			m_bMaximized = prefDis.readBoolean();
			applyWindowInfo();

			// divider locations
			m_mapChatSplitPane.setDividerLocation(prefDis.readInt());
			m_mapPogSplitPane.setDividerLocation(prefDis.readInt());

			m_showNamesCheckbox.setSelected(prefDis.readBoolean());

			// new divider locations
			m_chatPanel.setChatSplitPaneDivider(prefDis.readInt());
			m_chatPanel.setUseMechanicsLog(prefDis.readBoolean());

			for (Module module : g_modules)
				module.onLoadPreferences(prefDis);

			prefDis.close();
			prefFile.close();
		}
		catch (final FileNotFoundException ex1)
		{
			Log.log(Log.SYS, ex1);
		}
		catch (final IOException ex1)
		{
			Log.log(Log.SYS, ex1);
		}

		for (Module module : g_modules)
			module.onLoadPreferencesCompleted();
	}

	// private JMenuItem getPrivChatWindowMenuItem()
	// {
	// final JMenuItem item = new JMenuItem("Open Private Chat");
	// item.setAccelerator(KeyStroke.getKeyStroke("ctrl T"));
	//
	// item.addActionListener(new ActionListener()
	// {
	// public void actionPerformed(final ActionEvent e)
	// {
	// m_chatPanel.openPrivChatWindowDialog();
	// }
	// });
	//
	// return item;
	// }


	public void lockAllPogPacketReceived(final boolean lock)
	{
		getGametableCanvas().lockAllMapElements(true, lock);
		
		if (getNetworkStatus() == NetworkStatus.HOSTING)
		{
			sendBroadcast(NetLockMapElements.makeLockAllPacket(lock));
		}
	}

	/**
	 * A player just joined this hosted session.  Used by NetSendPlayerInfo.
	 * @param sourceConnection Network source connection
	 * @param player Player object
	 * @param password Password entered by the player to join
	 */
	public void onPlayerJoined(final NetworkConnectionIF sourceConnection, final Player player, final String password)
	{
		confirmHost();

		if (!m_networkModule.getPassword().equals(password))
		{
			// Reject connection due to invalid password			
			send(NetLoginRejected.makePacket(NetLoginRejected.RejectReason.INVALID_PASSWORD), sourceConnection);
			sourceConnection.close();
			return;
		}

		// now we can associate a player with the connection
		sourceConnection.markLoggedIn();
		player.setConnection(sourceConnection);

		// set their ID
		player.setId(m_nextPlayerId++);

		// tell everyone about the new guy
		sendSystemMessageBroadcast(player.getPlayerName() + " " + getLanguageResource().PLAYER_JOINED);
		addPlayer(player);

		sendPlayersInformation();

		send(NetSetGridMode.makePacket(m_gametableCanvas.getGridModeId()), player);
		
		MapElementTypeIF type = m_gametableCanvas.getBackgroundMapElementType();
		if (type != null)
		{
			send(NetSetBackground.makePacket(type), player);
		}
		else
		{
			send(NetSetBackground.makePacket(m_gametableCanvas.getBackgroundColor()), player);
		}

		// Send all existing line segments to the new player
		send(NetAddLineSegments.makePacket(getGametableCanvas().getPublicMap().getLines()), player);

		// Send all current map elements to the new player
		for (MapElement pog : getGametableCanvas().getPublicMap().getMapElements())
			send(NetAddMapElement.makePacket(pog), player);
		
		// finally, have the player recenter on the host's view
		final int viewCenterX = getGametableCanvas().getWidth() / 2;
		final int viewCenterY = getGametableCanvas().getHeight() / 2;

		// convert to model coordinates
		final MapCoordinates modelCenter = getGametableCanvas().viewToModel(viewCenterX, viewCenterY);
		send(NetRecenterMap.makePacket(modelCenter, m_gametableCanvas.getZoomLevel()), player);

		// TODO #Networking see if this is necessary... network module already knows of this and we have the following listener call to handle modules.
		
		// let them know we're done sending them data from the login
		send(NetLoginComplete.makePacket(), player);

		for (GameTableFrameListener listener : m_listeners)
			listener.onPlayerJoined(player);
	}

	/**
	 * Send an alert message to the chat window
	 * @param text Alert text
	 */
	public void sendAlertMessageLocal(final String text)
	{
		sendChatMessageLocal(ALERT_MESSAGE_FONT + text + END_ALERT_MESSAGE_FONT);
	}
	
	/**
	 * Show a message on the mechanics window, without sending over network
	 * @param text
	 */
	public void sendMechanicsMessageLocal(final String text)
	{
		m_chatPanel.addMechanicsMessage(text);
	}

	/**
	 * Sends a mechanics message specifically to a given player
	 * @param toName
	 * @param text
	 */
	public void sendMechanicsMessage(final String toName, final String text)
	{
		if (getNetworkStatus() == NetworkStatus.DISCONNECTED)
		{
			if (getMyPlayer().hasName(toName))
				m_chatPanel.addMechanicsMessage(text);
		}
		else
		{
			sendBroadcast(NetSendMechanicsText.makePacket(toName, text));
		}
	}

	/**
	 * Sends a mechanics message so that all players see it
	 * @param text
	 */
	public void sendMechanicsMessageBroadcast(final String text)
	{
		if (getNetworkStatus() != NetworkStatus.DISCONNECTED)
			sendBroadcast(NetSendMechanicsText.makeBroadcastPacket(text));
		
		// Broadcasting does not send back to self
		m_chatPanel.addMechanicsMessage(text);
	}

	/**
	 * Send a chat message to all players
	 * @param text message to send
	 */
	public void sendChatMessageBroadcast(final String text)
	{
		m_chatPanel.logMessage(text);
		if (getNetworkStatus() != NetworkStatus.DISCONNECTED)
		{
			sendBroadcast(NetSendChatText.makeBroadcastPacket(text));
		}
	}
	
	/**
	 * Send a chat message to current player only
	 * @param text message to send
	 */
	public void sendChatMessageLocal(final String text)
	{
		m_chatPanel.logMessage(text);
	}

	/**
	 * Send a chat message to a specific player name
	 * @param fromName Name of the originating player 
	 * @param toName Name of the target player
	 * @param text Message to display
	 */
	public void sendChatMessage(final String fromName, final String toName, final String text)
	{
		if (getMyPlayer().hasName(toName))
		{
			m_chatPanel.logPrivateMessage(fromName, toName, text);
		}
		else switch(getNetworkStatus())
		{
		case  HOSTING:		
			// Send directly to player
			for (Player player : m_players)
			{
				if (player.hasName(toName))
				{
					send(NetSendChatText.makePacket(fromName, toName, text), player);
					return;
				}
			}
			break;
			
		case CONNECTED:
			// Send to host - he will be dispatching
			send(NetSendChatText.makePacket(fromName, toName, text), getHostConnection());
			break;
			
		case DISCONNECTED:
			// No one to send to
			break;
		}
	}
	
	/**
	 * @return Connection to the host.  Will be 'null' if current player is host.
	 */
	public NetworkConnectionIF getHostConnection()
	{
		return m_hostConnection;
	}
	
	/**
	 * Shows a system message through the mechanics window
	 * @param text
	 */
	public void sendSystemMessageLocal(final String text)
	{
		sendMechanicsMessageLocal(SYSTEM_MESSAGE_FONT + text + END_SYSTEM_MESSAGE_FONT);
	}
	
	/**
	 * Shows a system message to a specific user
	 * @param toName Name of the player to send the message to
	 * @param text
	 */
	public void sendSystemMessageLocal(String toName, String text)
	{
		sendMechanicsMessage(toName, SYSTEM_MESSAGE_FONT + text + END_SYSTEM_MESSAGE_FONT);
	}

	/**
	 * Broadcast a system message through the mechanics window
	 * @param text
	 */
	public void sendSystemMessageBroadcast(final String text)
	{
		sendMechanicsMessageBroadcast(SYSTEM_MESSAGE_FONT + text + END_SYSTEM_MESSAGE_FONT);
	}

	/**
	 * Reacquires pogs and then refreshes the pog list.
	 */
	public void reacquirePogs()
	{
		try
		{
			m_pogLibrary.refresh(true);
		}
		catch (IOException e)
		{
			// .todo proper error handling
			Log.log(Log.SYS, e.getMessage());
		}

		refreshMapElementList();
	}
	
	/**
	 * Refreshes the pog list.
	 */
	public void refreshMapElementList()
	{
		m_pogPanel.populateChildren();
		getGametableCanvas().repaint();
	}

	/**
	 * Removes a listener from this map
	 * 
	 * @param listener Listener to remove
	 * @return True if listener was found and removed
	 */
	public boolean removeListener(GameTableFrameListener listener)
	{
		return m_listeners.remove(listener);
	}
	
	public boolean runHostDialog()
	{
		final StartNetworkingDialog dialog = new StartNetworkingDialog();
		dialog.setUpForHostDlg();
		dialog.setLocationRelativeTo(m_gametableCanvas);
		dialog.setVisible(true);

		if (!dialog.m_bAccepted)
		{
			// they cancelled out
			return false;
		}
		return true;
	}

	/**
	 * @param file
	 * @param pog Saves a Single pog to a File for later loading.
	 */
	public void savePog(final File file, final MapElement pog)
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(baos);
		try
		{

			pog.writeToPacket(dos);

			final byte[] saveFileData = baos.toByteArray();
			final FileOutputStream output = new FileOutputStream(file);
			final DataOutputStream fileOut = new DataOutputStream(output);

			// fileOut.writeInt(saveFileData.length);
			fileOut.write(saveFileData);
			output.close();
			fileOut.close();
			baos.close();
			dos.close();
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
			// failed to save. give up
		}
	}

	public void savePrefs()
	{
		try
		{
			final FileOutputStream prefFile = new FileOutputStream(getPreferenceFile());
			final DataOutputStream prefDos = new DataOutputStream(prefFile);

			prefDos.writeUTF(m_playerName);
			prefDos.writeUTF(m_characterName);

			// TODO #Networking Save networking preferences
			
			Point pos = m_gametableCanvas.getScrollPosition();
			prefDos.writeInt(pos.x);
			prefDos.writeInt(pos.y);
			prefDos.writeInt(m_gametableCanvas.getZoomLevel());

			prefDos.writeInt(m_windowSize.width);
			prefDos.writeInt(m_windowSize.height);
			prefDos.writeInt(m_windowPos.x);
			prefDos.writeInt(m_windowPos.y);
			prefDos.writeBoolean(m_bMaximized);

			// divider locations
			prefDos.writeInt(m_mapChatSplitPane.getDividerLocation());
			prefDos.writeInt(m_mapPogSplitPane.getDividerLocation());

			prefDos.writeBoolean(m_showNamesCheckbox.isSelected());

			// new divider location
			prefDos.writeInt(m_mapChatSplitPane.getDividerLocation());

			prefDos.writeBoolean(m_chatPanel.getUseMechanicsLog());

			for (Module module : g_modules)
				module.onSavePreferences(prefDos);

			prefDos.close();
			prefFile.close();

			for (Module module : g_modules)
				module.onSavePreferencesCompleted();
		}
		catch (final FileNotFoundException ex1)
		{
			Log.log(Log.SYS, ex1);
		}
		catch (final IOException ex1)
		{
			Log.log(Log.SYS, ex1);
		}
	}

	public void saveToXML(final File file)
	{
		Document doc;
		try
		{
			doc = XMLUtils.createDocument();
		}
		catch (IOException e)
		{
			Log.log(Log.SYS, e.getMessage());
			JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Element root = doc.createElement("gt");
		doc.appendChild(root);

		Element publicEl = doc.createElement("public_map");
		root.appendChild(publicEl);
		m_gametableCanvas.getPublicMap().serialize(publicEl);

		Element privateEl = doc.createElement("private_map");
		root.appendChild(privateEl);
		m_gametableCanvas.getPrivateMap().serialize(privateEl);

		storeLockedElementsToXML(doc, root);
		storeGridToXML(doc, root);

		// Hook for modules to add elements to save file
		Element modulesEl = doc.createElement("modules");

		for (Module module : g_modules)
		{
			if (module.canSaveToXML())
			{
				Element moduleEl = doc.createElement("module");
				moduleEl.setAttribute("name", module.getModuleName());

				if (module.saveToXML(moduleEl))
					modulesEl.appendChild(moduleEl);
			}
		}

		// If non-empty, append
		if (modulesEl.getFirstChild() != null)
			root.appendChild(modulesEl);

		// -------------------------------------
		try
		{
			XMLOutputProperties props = new XMLOutputProperties();
			props.indentXML = false;
			props.encoding = "UTF-8";
			XMLUtils.saveDocument(file, doc, props);
		}
		catch (IOException e)
		{
			Log.log(Log.SYS, e.getMessage());
			JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

	}

	/**
	 * Sends a public message to all players.
	 * 
	 * @param text Message to send.
	 */
	public void say(final String text)
	{
		sendChatMessageBroadcast(SAY_MESSAGE_FONT + UtilityFunctions.emitUserLink(getMyPlayer().getCharacterName()) + ": " + END_SAY_MESSAGE_FONT + text);
	}

	/**
	 * Broadcasts a packet through the networking module.  Everyone but logged player receives the message.
	 * @param packet
	 */
	public void sendBroadcast(final byte[] packet)
	{
		m_networkModule.sendBroadcast(packet);
	}
	
	/**
	 * Sends a packet through the networking module through the specified connection
	 * @param packet
	 * @param player
	 */
	public void send(final byte[] packet, final NetworkConnectionIF conn)
	{
		m_networkModule.send(packet, conn);
	}

	/**
	 * Sends a packet through the networking module to the specified player
	 * @param packet
	 * @param player
	 */
	private void send(final byte[] packet, final Player player)
	{
		if (player.getConnection() == null)
		{
			throw new IllegalStateException("You do not have direct connection to player " + player.getCharacterName());
		}
		
		send(packet, player.getConnection());		
	}
	
	/**
	 * Get the network status from the network module
	 * @return network status
	 */
	public NetworkStatus getNetworkStatus()
	{
		return m_networkModule.getNetworkStatus();
	}
	
	/**
	 * Send players information to all connected players
	 */
	private void sendPlayersInformation()
	{
		// Since the player list message includes a pointer telling which player the recipient is,
		// We will not be broadcasting the list, but sending a specific message to each player
		
		Player me = getMyPlayer();
		
		for (Player recipient : m_players)
		{
			if (me == recipient)
				continue;
			
			final byte[] castPacket = NetSendPlayersList.makePacket(recipient);
			send(castPacket, recipient);
		}
	}

	public void setToolSelected(final int toolId)
	{
		m_toolButtons[toolId].setSelected(true);
	}

	// public void openPrivateMessageWindow()
	// {
	// }

	/**
	 * ************************************************************************************* #randomrotate
	 * 
	 * @return
	 */
	public boolean shouldRotatePogs()
	{
		return m_randomRotate.isSelected();
	}

	public boolean shouldShowNames()
	{
		return m_showNamesCheckbox.isSelected();
	}

	public void startTellTo(String name)
	{
        if (name.contains(" ") || name.contains("\"") || name.contains("\t"))
        {
            name = "\"" + name.replace("\"", "\\\"") + "\"";
        }
	        
		m_textEntry.setText("/tell " + name + "<b> </b>");
		m_textEntry.requestFocus();
		m_textEntry.toggleStyle("bold");
		m_textEntry.toggleStyle("bold");
	}

	/**
	 * Sends a private message to the target player.
	 * 
	 * @param target Player to address message to.
	 * @param text Message to send.
	 */
	public void tell(final Player target, final String text)
	{
		if (target.getID() == getMyPlayer().getID())
		{
			m_chatPanel.logMessage(PRIVATE_MESSAGE_FONT + getLanguageResource().TELL_SELF + " " + END_PRIVATE_MESSAGE_FONT + text);
			return;
		}

		final String fromName = getMyPlayer().getCharacterName();
		final String toName = target.getCharacterName();

		sendChatMessage(fromName, toName, text);

		// and when you post a private message, you get told about it in your
		// own chat log
		m_chatPanel.logMessage(PRIVATE_MESSAGE_FONT + getLanguageResource().TELL + " " + UtilityFunctions.emitUserLink(toName) + ": "
				+ END_PRIVATE_MESSAGE_FONT + text);
	}

	/**
	 * Toggles between the two layers.
	 */
	public void toggleLayer()
	{
		// toggle the map we're on
		boolean publicMap = getGametableCanvas().isPublicMap();
		if (publicMap)
		{
			getGametableCanvas().setActiveMap(getGametableCanvas().getPrivateMap());
		}
		else
		{
			getGametableCanvas().setActiveMap(getGametableCanvas().getPublicMap());
		}

		// if they toggled the layer, whatever tool they're using is cancelled
		getToolManager().cancelToolAction();
		getGametableCanvas().requestFocus();

		for (Module module : g_modules)
			module.onToggleActiveMap(!publicMap);

		repaint();
	}

	/**
	 * Sets the player information for the game
	 * @param players List of players currently connected
	 * @param ourIdx Index of our player information
	 */
	public void setPlayersInformation(final Player[] players, final int ourIdx)
	{
		// you should only get this if you're a joiner
		confirmJoined();

		// set up the current cast
		clearAllPlayers();
		for (int i = 0; i < players.length; i++)
			addPlayer(players[i]);

		m_myPlayerIndex = ourIdx;
	}

	public void updateGridModeMenu()
	{
		if (getGametableCanvas().m_gridMode == getGametableCanvas().m_noGridMode)
		{
			m_noGridModeMenuItem.setState(true);
			m_squareGridModeMenuItem.setState(false);
			m_hexGridModeMenuItem.setState(false);
		}
		else if (getGametableCanvas().m_gridMode == getGametableCanvas().m_squareGridMode)
		{
			m_noGridModeMenuItem.setState(false);
			m_squareGridModeMenuItem.setState(true);
			m_hexGridModeMenuItem.setState(false);
		}
		else if (getGametableCanvas().m_gridMode == getGametableCanvas().m_hexGridMode)
		{
			m_noGridModeMenuItem.setState(false);
			m_squareGridModeMenuItem.setState(false);
			m_hexGridModeMenuItem.setState(true);
		}
	}

	/**
	 * Updates the networking status
	 */
	private void updateStatus()
	{
		String newStatusText = "";
		
		switch (getNetworkStatus())
		{
		case DISCONNECTED:
			newStatusText = getLanguageResource().DISCONNECTED;
			m_actionLoadMap.setEnabled(true);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadPublicMap.setEnabled(true);
			break;

		case CONNECTED:
			newStatusText = getLanguageResource().CONNECTED;
			m_actionLoadMap.setEnabled(false);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadPublicMap.setEnabled(false);
			break;

		case HOSTING:
			newStatusText = getLanguageResource().HOSTING;
			m_actionLoadMap.setEnabled(true);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadPublicMap.setEnabled(true);
			break;

		default:
			newStatusText = getLanguageResource().UNKNOWN_STATE;
			m_actionLoadMap.setEnabled(false);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadPublicMap.setEnabled(false);
			break;
		}

		if (getNetworkStatus() != NetworkStatus.DISCONNECTED)
		{
			if (m_players.size() > 1)
				newStatusText += "; " + m_players.size() + " players " + getLanguageResource().CONNECTED;
			else
				newStatusText += "; " + m_players.size() + " player " + getLanguageResource().CONNECTED;
			
			int count = m_typingPlayers.size();
			boolean started = false;
			
			for (Player player : m_typingPlayers)
			{
				if (started)
					newStatusText += " AND";
				else
				{
					newStatusText += ";";					
					started = true;
				}
				
				newStatusText += " " + player.getCharacterName();
			}
			
			if (count == 1)
				newStatusText += " " + getLanguageResource().IS_TYPING;
			else if (count > 1)
				newStatusText += " " + getLanguageResource().ARE_TYPING;
		}
		
		m_status.setText(newStatusText);
	}

	/**
	 * Records the current state of the window.
	 */
	public void updateWindowInfo()
	{
		// we only update our internal size and
		// position variables if we aren't maximized.
		if ((getExtendedState() & MAXIMIZED_BOTH) != 0)
		{
			m_bMaximized = true;
		}
		else
		{
			m_bMaximized = false;
			m_windowSize = getSize();
			m_windowPos = getLocation();
		}
	}

	/**
	 * @revise Temporary holding - we'll want something more gobal
	 */
	private void buildActions()
	{
		m_actionLoadMap = new AbstractAction(getLanguageResource().MAP_OPEN) {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				loadMap(true, true);
			}
		};

		m_actionLoadPrivateMap = new AbstractAction("Load Private Map") {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				loadMap(false, true);
			}
		};

		m_actionLoadPublicMap = new AbstractAction("Load Public Map") {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				loadMap(true, false);
			}
		};
	}

	private void doLockMap(final boolean lock)
	{
		final GameTableMap mapToLock = m_gametableCanvas.getActiveMap();
		boolean priv = true;
		if (mapToLock == getGametableCanvas().getPublicMap())
			priv = false;

		if (priv || (getNetworkStatus() == NetworkStatus.DISCONNECTED))
		{
			getGametableCanvas().lockAllMapElements(!priv, lock);
			
			if (lock)
				sendMechanicsMessageLocal(getLanguageResource().MAP_LOCK_ALL_DONE);
			else
				sendMechanicsMessageLocal(getLanguageResource().MAP_UNLOCK_ALL_DONE);
		}
		else
		{
			if (lock)
				sendSystemMessageBroadcast(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_LOCK_ALL_DONE2);
			else
				sendSystemMessageBroadcast(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_UNLOCK_ALL_DONE2);
			
			sendBroadcast(NetLockMapElements.makeLockAllPacket(lock));
		}
	}

	/**
	 * erases all pogs, also clearing the array of active pogs
	 * 
	 * @revise move to MODEL.
	 */
	private void eraseAllPogs()
	{
		// make an int array of all the IDs
		List<MapElement> pogs = getGametableCanvas().getActiveMap().getMapElements();
		getGametableCanvas().removePogs(pogs);
	}

	/**
	 * Export map to JPeg file
	 */
	private void exportMap()
	{
		File out = getMapExportFile();
		if (out == null)
			return;

		try
		{
			m_gametableCanvas.exportMap(null, out);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), getLanguageResource().MAP_SAVE_IMG_FAIL, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * creates the "About" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getAboutMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().ABOUT); // creates a menu item with the "About" label
		item.setAccelerator(KeyStroke.getKeyStroke("F1")); // assign a shortcut
		item.addActionListener(new ActionListener() // when the user selects it this is what happens
				{
					public void actionPerformed(final ActionEvent e)
					{
						// show the about message
						UtilityFunctions.msgBox(GametableFrame.this, GametableApp.VERSION + " " + getLanguageResource().ABOUT2 + "\n"
								+ getLanguageResource().ABOUT3, getLanguageResource().VERSION);
					}
				});
		return item;
	}
	
	private File getPreferenceFile()
	{
		return new File(GametableApp.USER_FILES_PATH, "prefs.prf");
	}

	private File getAutoSaveXMLFile()
	{
		return new File(GametableApp.USER_FILES_PATH, "autosave.xml");
	}

	/**
	 * *************************************************************************************
	 * 
	 * @param color
	 * @return
	 */
	private JMenuItem getChangeBGMenuItem(final BackgroundColor color)
	{
		final String cstr = color.getText();

		JMenuItem item = new JMenuItem(cstr);
		item.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				changeBackground(color, null);
			}
		});

		return item;
	}

	/**
	 * builds and return the "Undock Chat Window"
	 * 
	 * @return the menu item
	 */
	private JMenuItem getChatWindowMenuItem()
	{
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().CHAT_WINDOW_DOCK);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " L"));

		if (!m_chatPanel.isDocked())
			item.setState(true);
		else
			item.setState(false);

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				m_chatPanel.toggleDockStatus();
			}
		});

		return item;
	}

	/**
	 * creates the "Clear Map" menu item
	 * 
	 * @return the new menu item
	 */
	private JMenuItem getClearMapMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().MAP_CLEAR);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				// confirm the erase operation
				final int res = UtilityFunctions.yesNoDialog(GametableFrame.this, getLanguageResource().MAP_CLEAR_WARNING, getLanguageResource().MAP_CLEAR);
				if (res == UtilityFunctions.YES)
				{
					eraseAllPogs();
					getGametableCanvas().getActiveMap().clearLineSegments();
				}
			}
		});

		return item;
	}

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenuItem getDeleteGroupMenuItem(final int all)
	{
		String g = "Delete Group";
		if (all == 1)
			g = "Delete Unused Groups";
		if (all == 2)
			g = "Delete All Groups";
		final JMenuItem item = new JMenuItem(g);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				if (getActiveGroupManager().getGroupCount() < 1)
				{
					JOptionPane.showMessageDialog(getGametableFrame(), "No Groups Defined.", "No Groups", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				if (all == 1)
				{
					getActiveGroupManager().deleteEmptyGroups();
				}
				if (all == 2)
				{
					getActiveGroupManager().deleteAllGroups();
				}
				else
				{
					GroupingDialog gd = new GroupingDialog(false);
					gd.setVisible(true);
					if (gd.isAccepted())
					{
						Group group = gd.getGroup();
						if (group != null)
							group.deleteGroup();
					}
				}
			}
		});
		return item;
	}

	/**
	 * creates the "Disconnect" menu item
	 * 
	 * @return the new menu item
	 */
	private JMenuItem getDisconnectMenuItem()
	{
		if (m_disconnectMenuItem == null)
		{
			final JMenuItem item = new JMenuItem(getLanguageResource().DISCONNECT);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					disconnect();
				}
			});
			m_disconnectMenuItem = item;
		}
		return m_disconnectMenuItem;
	}

	/**
	 * Get the "Export Map" menu item
	 * 
	 * @return JMenuItem
	 */
	private JMenuItem getExportMapMenuItem()
	{
		JMenuItem item = new JMenuItem(getLanguageResource().MAP_EXPORT);
		item.addActionListener(new ActionListener() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent e)
			{
				exportMap();
			}
		});

		return item;
	}

	/**
	 * Builds and returns the File Menu
	 * 
	 * @return the file menu just built
	 */
	private JMenu getFileMenu()
	{
		// todo Add File->New

		final JMenu menu = new JMenu(getLanguageResource().FILE);

		JMenuItem item = new JMenuItem(m_actionLoadMap);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
		menu.add(item);

		item = new JMenuItem(m_actionLoadPublicMap);
		// item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
		menu.add(item);

		item = new JMenuItem(m_actionLoadPrivateMap);
		// item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
		menu.add(item);

		menu.add(getSaveMapMenuItem());
		menu.add(getSaveAsMapMenuItem());
		menu.add(getScanForPogsMenuItem());
		menu.add(getQuitMenuItem());

		return menu;
	}

	/**
	 * creates the "Grid Mode" menu
	 * 
	 * @return the new menu
	 */
	private JMenu getGridModeMenu()
	{
		final JMenu menu = new JMenu(getLanguageResource().MAP_GRID_MODE);
		menu.add(m_noGridModeMenuItem);
		menu.add(m_squareGridModeMenuItem);
		menu.add(m_hexGridModeMenuItem);

		return menu;
	}

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenu getGroupingMenu()
	{
		final JMenu menu = new JMenu("Grouping");
		menu.add(getSelectGroupMenuItem());
		menu.add(getGroupSelectedMenuItem());
		menu.add(getUngroupSelectedMenuItem());
		menu.add(getDeleteGroupMenuItem(0));
		menu.add(getDeleteGroupMenuItem(1));
		menu.add(getDeleteGroupMenuItem(2));
		return menu;
	}

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenuItem getGroupSelectedMenuItem()
	{
		final JMenuItem item = new JMenuItem("Group Selected");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				GroupingDialog gd = new GroupingDialog(true);
				gd.setVisible(true);
				if (gd.isAccepted())
				{
					Group g = gd.getGroup();
					if (g != null)
						g.addElements(getGametableCanvas().getSelectedMapElementInstances());
				}
			}
		});
		return item;
	}

	/**
	 * creates the "Help" menu
	 * 
	 * @return the new menu
	 */
	private JMenu getHelpMenu()
	{
		final JMenu menu = new JMenu(getLanguageResource().HELP);
		menu.add(getAboutMenuItem());

		/*
		 * menu.add(new JMenuItem(new AbstractAction("ISHOST") {
		 * 
		 * @Override public void actionPerformed(ActionEvent e) { Player p = getMyPlayer();
		 * System.out.println(p.isHostPlayer() ? "host" : "guest"); System.out.println(p.getConnection() == null ?
		 * "disconnected" : "connected"); } }));
		 */

		return menu;
	}

	/**
	 * creates the "list players" menu item
	 * 
	 * @return the new menu item
	 */
	private JMenuItem getListPlayersMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().LIST_PLAYERS);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " W"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				SlashCommands.parseSlashCommand("/who"); // selecting this menu item is the same as issuing the command /who
			}
		});
		return item;
	}

	private JMenuItem getLoadPogMenuItem()
	{
		JMenuItem item = new JMenuItem(getLanguageResource().POG_LOAD);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				loadPog();
			}
		});

		return item;
	}

	private JMenuItem getLockMenuItem(final boolean lock)
	{
		String str;
		if (lock)
			str = getLanguageResource().MAP_LOCK_ALL;
		else
			str = getLanguageResource().MAP_UNLOCK_ALL;
		final JMenuItem item = new JMenuItem(str);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				doLockMap(lock);
			}
		});

		return item;
	}

	/**
	 * Builds and returns the main menu bar
	 * 
	 * @return The menu bar just built
	 */
	private JMenuBar getMainMenuBar()
	{
		final JMenuBar menuBar = new JMenuBar();
		menuBar.add(getFileMenu());
		menuBar.add(getNetworkMenu());
		menuBar.add(getMapMenu());
		menuBar.add(getGroupingMenu()); // #grouping

		JMenu menu = getModulesMenu();
		if (menu != null)
			menuBar.add(menu);

		menuBar.add(getWindowMenu());
		menuBar.add(getHelpMenu());

		return menuBar;
	}

	/**
	 * Ask the user to choose a file name for the exported map
	 * 
	 * @return File object or null if the user did not choose
	 */
	private File getMapExportFile()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(getLanguageResource().MAP_SAVE_IMG); // no external resource for text
		if (m_mapExportSaveFolder != null)
		{
			chooser.setSelectedFile(m_mapExportSaveFolder);
		}

		FileFilter filter = new FileFilter() {
			/*
			 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
			 */
			public boolean accept(File f)
			{
				String fileName = f.getPath().toLowerCase();
				return f.isDirectory() || fileName.endsWith(".jpeg") || fileName.endsWith(".jpg");
			}

			/*
			 * @see javax.swing.filechooser.FileFilter#getDescription()
			 */
			public String getDescription()
			{
				return "JPeg Files (*.jpg, *.jpeg)";
			}
		};

		chooser.setFileFilter(filter);

		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return null;
		}

		// Add extension to file name if user did not do so
		File out = chooser.getSelectedFile();
		String fileName = out.getAbsolutePath().toLowerCase();

		if (!(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")))
		{
			out = new File(out.getAbsolutePath() + ".jpg");
		}

		// If file exists, confirm before overwrite
		if (out.exists())
		{
			if (JOptionPane.showConfirmDialog(this, getLanguageResource().MAP_SAVE_OVERWRITE + " " + out.getName() + "?",
					getLanguageResource().MAP_SAVE_EXISTS, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			{
				return null;
			}
		}

		if (out.exists() && !out.canWrite())
		{
			JOptionPane.showMessageDialog(this, getLanguageResource().MAP_SAVE_NO_ACCESS + " " + out.getName(), getLanguageResource().MAP_SAVE_FILE_FAIL,
					JOptionPane.ERROR_MESSAGE);

			return null;
		}

		// Figure out path name and keep it for next open of dialog
		String pathName = out.getAbsolutePath();
		int idx = pathName.lastIndexOf(File.separator);
		if (idx > -1)
		{
			pathName = pathName.substring(0, idx) + File.separator + ".";
		}

		m_mapExportSaveFolder = new File(pathName);

		return out;
	}

	/**
	 * bulds and returns the "Map" menu
	 * 
	 * @return the menu just built
	 */
	private JMenu getMapMenu()
	{
		final JMenu menu = new JMenu(getLanguageResource().MAP);
		menu.add(getClearMapMenuItem());
		menu.add(getRecenterAllPlayersMenuItem());
		menu.add(getGridModeMenu());
		menu.add(getTogglePrivateMapMenuItem());
		menu.add(getExportMapMenuItem());
		JMenu cbgitem = new JMenu(getLanguageResource().MAP_BG_CHANGE);

		for (BackgroundColor color : BackgroundColor.values())
			cbgitem.add(getChangeBGMenuItem(color));

		menu.add(cbgitem);
		menu.addSeparator();
		menu.add(getLockMenuItem(true));
		menu.add(getLockMenuItem(false));
		menu.addSeparator();
		menu.add(getRemoveSelectedMenuItem());
		menu.add(getUnPublishSelectedMenuItem(false));
		menu.add(getUnPublishSelectedMenuItem(true));
		menu.add(getLoadPogMenuItem());
		return menu;
	}

	private JMenuItem getMechanicsToggle()
	{
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().MECHANICS_WINDOW_USE);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " M"));
		if (m_chatPanel.getUseMechanicsLog())
			item.setState(true);
		else
			item.setState(false);

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				toggleMechanicsWindow();
			}
		});

		return item;
	}

	/**
	 * creates the "Modules" menu
	 * 
	 * @return the new menu
	 */
	private JMenu getModulesMenu()
	{
		JMenu modulesMenu = null;

		for (Module module : g_modules)
		{
			JMenu menu = module.getModuleMenu();
			if (menu != null)
			{
				if (modulesMenu == null)
					modulesMenu = new JMenu("Modules");

				modulesMenu.add(menu);
			}
		}

		return modulesMenu;
	}

	/**
	 * builds and return the "Network" menu
	 * 
	 * @return the newly built menu
	 */
	private JMenu getNetworkMenu()
	{
		final JMenu menu = new JMenu(getLanguageResource().NETWORK);
		menu.add(getListPlayersMenuItem());
		
		if (m_startNetworkingMenuItem == null)
		{
			final JMenuItem item = new JMenuItem("Join / Host Network Game");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					startNetworkGame();
				}
			});
			m_startNetworkingMenuItem = item;
		}
		
		menu.add(m_startNetworkingMenuItem);
		menu.add(getDisconnectMenuItem());

		return menu;
	}

	/**
	 * builds and return the "Undock Pog Window" menu item
	 * 
	 * @return the menu item just built
	 */
	private JMenuItem getPogWindowMenuItem()
	{
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().POG_WINDOW_DOCK);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " P"));
		if (!m_pogsTabbedPane.isDocked())
			item.setState(true);
		else
			item.setState(false);

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				m_pogsTabbedPane.toggleDockStatus();
			}
		});

		return item;
	}

	/**
	 * build and returns the "Quit" menu item
	 * 
	 * @return the newly built menu item
	 */
	private JMenuItem getQuitMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().QUIT);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " Q"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				if (pogWindow != null) // close the pog window
				{
					pogWindow.dispose();
				}
				saveAll(); // .todo confirm before save (check if modified)
				dispose();
				System.exit(0);
			}
		});

		return item;
	}

	/**
	 * builds and returns the "Recenter all Player" menu item
	 * 
	 * @return a menu item
	 */
	private JMenuItem getRecenterAllPlayersMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().MAP_CENTER_PLAYERS);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				// confirm the operation
				final int result = UtilityFunctions.yesNoDialog(GametableFrame.this, getLanguageResource().MAP_CENTER_PLAYERS_WARN,
						getLanguageResource().MAP_CENTER);
				if (result == UtilityFunctions.YES)
				{
					// get our view center
					final int viewCenterX = getGametableCanvas().getWidth() / 2;
					final int viewCenterY = getGametableCanvas().getHeight() / 2;

					// convert to model coordinates
					final MapCoordinates modelCenter = getGametableCanvas().viewToModel(viewCenterX, viewCenterY);
					centerView(modelCenter, m_gametableCanvas.getZoomLevel());
					sendSystemMessageBroadcast(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_CENTER_DONE);
				}
			}
		});
		return item;
	}

	/**
	 * *************************************************************************************
	 * 
	 * @return
	 */
	private JMenuItem getRemoveSelectedMenuItem()
	{
		final JMenuItem item = new JMenuItem("Remove Selected");

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				getGametableCanvas().removePogs(getGametableCanvas().getSelectedMapElementInstances());
			}
		});

		return item;
	}

	/**
	 * builds and returns the "Scan for pogs" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getScanForPogsMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().POG_SCAN);
		item.setAccelerator(KeyStroke.getKeyStroke("F5"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				reacquirePogs();
			}
		});

		return item;
	}

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenuItem getSelectGroupMenuItem()
	{
		final JMenuItem item = new JMenuItem("Select Group");

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				if (getActiveGroupManager().getGroupCount() < 1)
				{
					JOptionPane.showMessageDialog(getGametableFrame(), "No Groups Defined.", "No Groups", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				GroupingDialog gd = new GroupingDialog(false);
				Group g = gd.getGroup();
				gd.setVisible(true);
				if (gd.isAccepted() && g != null)
				{

					List<MapElement> pogs = g.getMapElements();
					getGametableCanvas().selectMapElementInstances(pogs, true);
					getGametableCanvas().repaint();
				}
			}
		});
		return item;
	}

	/**
	 * builds and returns the "Edit private map" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getTogglePrivateMapMenuItem()
	{
		if (m_togglePrivateMapMenuItem == null)
		{
			final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().MAP_PRIVATE_EDIT);
			item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " F"));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					toggleLayer();
				}
			});

			m_togglePrivateMapMenuItem = item;
		}

		return m_togglePrivateMapMenuItem;
	}

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenuItem getUngroupSelectedMenuItem()
	{
		final JMenuItem item = new JMenuItem("UnGroup Selected");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{

				for (MapElement pog : getGametableCanvas().getSelectedMapElementInstances())
				{
					Group g = getActiveGroupManager().getGroup(pog);
					if (g != null)
						g.removeElement(pog);
				}
			}
		});
		return item;
	}

	/**
	 * *************************************************************************************
	 * 
	 * @return
	 */
	private JMenuItem getUnPublishSelectedMenuItem(final boolean copy)
	{
		String s = "Un/Publish Selected";
		if (copy)
			s = "Copy Selected to Opposite Map";
		final JMenuItem item = new JMenuItem(s);

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				MapElement npog;
				GameTableMap map = getGametableCanvas().getActiveMap();
				GameTableMap to;

				if (map == getGametableCanvas().getPublicMap())
					to = getGametableCanvas().getPrivateMap();
				else
					to = getGametableCanvas().getPublicMap();

				for (MapElement pog : getGametableCanvas().getSelectedMapElementInstances())
				{
					to.addMapElement(pog);
					map.removeMapElementInstance(pog);
					if (copy)
					{
						npog = new MapElement(pog);
						map.addMapElement(npog);
					}
				}
			}
		});

		return item;
	}

	private JMenu getWindowMenu()
	{
		final JMenu menu = new JMenu(getLanguageResource().WINDOW);

		menu.add(getPogWindowMenuItem());
		menu.add(getChatWindowMenuItem());
		menu.addSeparator();
		menu.add(getMechanicsToggle());
		// menu.add(getPrivChatWindowMenuItem());

		return menu;
	}

	/**
	 * Performs initialization. This draws all the controls in the Frame and sets up listener to react to user actions.
	 * 
	 * @throws IOException
	 */
	private void initialize() throws IOException
	{
		ImageCache.startCacheDaemon();

		buildActions();
		
		initializeNetworkModule();
		
		SlashCommands.registerDefaultChatCommands();

		// todo #Plugins Automated module loading mechanism

		registerModule(CardModule.getModule());
		registerModule(DiceMacroModule.getModule());
		registerModule(ActivePogsModule.getModule());

		if (DEBUG_FOCUS) // if debugging
		{
			final KeyboardFocusManager man = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			man.addPropertyChangeListener(new PropertyChangeListener() {
				/*
				 * If debugging,show changes to properties in the console
				 * 
				 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
				 */
				public void propertyChange(final PropertyChangeEvent e)
				{
					System.out.println(e.getPropertyName() + ":\n    " + e.getOldValue() + "\n -> " + e.getNewValue());
				}

			});
		}

		// Configure chat panel
		m_chatPanel = new ChatPanel();
		m_textEntry = m_chatPanel.getTextEntry();

		setContentPane(new JPanel(new BorderLayout())); // Set the main UI object with a Border Layout
		setDefaultCloseOperation(EXIT_ON_CLOSE); // Ensure app ends with this frame is closed
		setTitle(GametableApp.VERSION); // Set frame title to the current version
		setJMenuBar(getMainMenuBar()); // Set the main MenuBar

		// Set this class to handle events from changing grid types
		m_noGridModeMenuItem.addActionListener(this);
		m_squareGridModeMenuItem.addActionListener(this);
		m_hexGridModeMenuItem.addActionListener(this);

		// Configure the panel containing the map and the chat window
		m_mapChatSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		m_mapChatSplitPane.setContinuousLayout(true);
		m_mapChatSplitPane.setResizeWeight(1.0);
		m_mapChatSplitPane.setBorder(null);

		// Configure the panel that splits the map and the pog list
		m_mapPogSplitPane.setContinuousLayout(true);
		m_mapPogSplitPane.setBorder(null);

		// Configure the color dropdown
		m_colorCombo.setMaximumSize(new Dimension(100, 21));
		m_colorCombo.setFocusable(false);

		// Configure the toolbar
		m_toolBar.setFloatable(false);
		m_toolBar.setRollover(true);
		m_toolBar.setBorder(new EmptyBorder(2, 5, 2, 5));
		m_toolBar.add(m_colorCombo, null);
		m_toolBar.add(Box.createHorizontalStrut(5));

		initializeTools();

		m_toolBar.add(Box.createHorizontalStrut(5));

		/*
		 * Added in order to accomodate grid unit multiplier
		 */
		m_gridunitmultiplier = new JTextField("5", 3);
		m_gridunitmultiplier.setMaximumSize(new Dimension(42, 21));

		// Configure the units dropdown
		final String[] units = { "ft", "m", "u" };
		m_gridunit = new JComboBox(units);
		m_gridunit.setMaximumSize(new Dimension(42, 21));
		m_toolBar.add(m_gridunitmultiplier);
		m_toolBar.add(m_gridunit);

		// Add methods to react to changes to the unit multiplier
		// todo : this is definitely better somewhere else
		m_gridunitmultiplier.getDocument().addDocumentListener(new DocumentListener() {
			// todo exceptions should be captured
			public void changedUpdate(final DocumentEvent e)
			{
				grid_multiplier = Double.parseDouble(m_gridunitmultiplier.getText());
			}

			public void insertUpdate(final DocumentEvent e)
			{
				grid_multiplier = Double.parseDouble(m_gridunitmultiplier.getText());
			}

			public void removeUpdate(final DocumentEvent e)
			{
				// Commented out to fix grid measurement size error
				// grid_multiplier = Double.parseDouble(m_gridunitmultiplier.getText());
			}
		});
		m_gridunit.addActionListener(this);

		// Configure the checkbox to show names
		m_showNamesCheckbox.setFocusable(false);

		// @revise consider to place this somewhere else in UI
		m_showNamesCheckbox.addActionListener(new ActionListener() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				m_gametableCanvas.repaint();
			}
		});
		m_toolBar.add(Box.createHorizontalStrut(5));
		m_toolBar.add(m_showNamesCheckbox);
		m_toolBar.add(Box.createHorizontalStrut(5));
		m_toolBar.add(m_randomRotate); // #randomrotate

		getContentPane().add(m_toolBar, BorderLayout.NORTH);

		getGametableCanvas().init(this);
		
		initializeGroupManager();

		m_pogLibrary = MapElementTypeLibrary.getMasterLibrary();

		m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("pogs"), Layer.POG));
		m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("environment"), Layer.ENVIRONMENT));
		m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("overlays"), Layer.OVERLAY));
		m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("underlays"), Layer.UNDERLAY));

		// pogWindow
		m_pogPanel = new PogPanel(m_pogLibrary, getGametableCanvas());
		m_pogsTabbedPane.addTab(m_pogPanel, getLanguageResource().POG_LIBRARY);

		for (Module module : g_modules)
		{
			// todo #Plugins consider abstracting "PogsTabbedPane" through an interface.
			module.onInitializeUI();
		}

		m_pogsTabbedPane.setFocusable(false);
		m_canvasPane.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(1, 1, 1, 1)));
		m_canvasPane.add(getGametableCanvas(), BorderLayout.CENTER);
		m_mapChatSplitPane.add(m_canvasPane, JSplitPane.TOP);
		m_mapChatSplitPane.add(m_chatPanel, JSplitPane.BOTTOM);

		m_mapPogSplitPane.add(m_pogsTabbedPane, JSplitPane.LEFT);
		m_mapPogSplitPane.add(m_mapChatSplitPane, JSplitPane.RIGHT);
		getContentPane().add(m_mapPogSplitPane, BorderLayout.CENTER);
		m_status.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		getContentPane().add(m_status, BorderLayout.SOUTH);
		updateStatus();

		m_disconnectMenuItem.setEnabled(false); // when the program starts we are disconnected so disable this menu item

		final ColorComboCellRenderer renderer = new ColorComboCellRenderer();
		m_colorCombo.setRenderer(renderer);

		// load the primary map
		// getGametableCanvas().setActiveMap(getGametableCanvas().getPrivateMap());
		PacketSourceState.beginFileLoad();
		File autoSave = getAutoSaveXMLFile();
		if (autoSave.exists())
			loadFromXML(autoSave, true, true);

		// loadState(new File("autosavepvt.grm"));
		PacketSourceState.endFileLoad();

		/*
		 * getGametableCanvas().setActiveMap(getGametableCanvas().getPublicMap()); loadState(new File("autosave.grm"));
		 */
		// loadPrefs();

		addPlayer(new Player(m_playerName, m_characterName, -1, true));
		m_myPlayerIndex = 0;

		m_colorCombo.addActionListener(this);
		updateGridModeMenu();

		addComponentListener(new ComponentAdapter() {
			/*
			 * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
			 */
			public void componentMoved(final ComponentEvent e)
			{
				updateWindowInfo();
			}

			/*
			 * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
			 */
			public void componentResized(final ComponentEvent event)
			{
				updateWindowInfo();
			}

		});

		// handle window events
		addWindowListener(new WindowAdapter() {
			/*
			 * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
			 */
			public void windowClosed(final WindowEvent e)
			{
				saveAll();
			}

			/*
			 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
			 */
			public void windowClosing(final WindowEvent e)
			{
				saveAll();
			}
		});

		/*
		 * // change the default component traverse settings // we do this cause we don't really care about those //
		 * settings, but we want to be able to use the tab key KeyboardFocusManager focusMgr =
		 * KeyboardFocusManager.getCurrentKeyboardFocusManager(); Set set = new
		 * HashSet(focusMgr.getDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS)); set.clear(); //
		 * set.add(KeyStroke.getKeyStroke('\t', 0, false));
		 * focusMgr.setDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set); set = new
		 * HashSet(focusMgr.getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS)); set.clear(); //
		 * set.add(KeyStroke.getKeyStroke('\t', 0, false));
		 * focusMgr.setDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
		 */

		m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SLASH"), "startSlash");
		m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ENTER"), "startText");
		m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control pressed R"), "reply");

		m_gametableCanvas.getActionMap().put("startSlash", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (m_gametableCanvas.isTextFieldFocused())
				{
					return;
				}

				// only do this at the start of a line
				if (m_textEntry.getText().length() == 0)
				{
					// furthermore, only do the set text and focusing if we don't have
					// focus (otherwise, we end up with two slashes. One from the user typing it, and
					// another from us setting the text, cause our settext happens first.)
					if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != m_textEntry)
					{
						m_textEntry.setText("/");
					}
				}
				m_textEntry.requestFocus();
			}
		});

		m_gametableCanvas.getActionMap().put("startText", new AbstractAction() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (m_gametableCanvas.isTextFieldFocused())
				{
					return;
				}

				if (m_textEntry.getText().length() == 0)
				{
					// furthermore, only do the set text and focusing if we don't have
					// focus (otherwise, we end up with two slashes. One from the user typing it, and
					// another from us setting the text, cause our settext happens first.)
					if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != m_textEntry)
					{
						m_textEntry.setText("");
					}
				}
				m_textEntry.requestFocus();
			}
		});

		// m_gametableCanvas.getActionMap().put("reply", new AbstractAction()
		// {
		// /*
		// * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		// */
		// public void actionPerformed(final ActionEvent e)
		// {
		// // we don't do this if there's already text in the entry field
		// if (m_textEntry.getText().length() == 0)
		// {
		// // if they've never received a tell, just tell them that
		// if (m_lastPrivateMessageSender == null)
		// {
		// // they've received no tells yet
		// m_chatPanel.logAlertMessage("You cannot reply until you receive a /tell from another player.");
		// }
		// else
		// {
		// startTellTo(m_lastPrivateMessageSender);
		// }
		// }
		// m_textEntry.requestFocus();
		// }
		// });

		initializeExecutorThread();

		// Load frame preferences
		loadPrefs();
	}

	/**
	 * Initialize group manager
	 */
	private void initializeGroupManager()
	{
		getGametableCanvas().getPublicMap().getGroupManager().addListener(new GroupManagerListener());
	}
	
	/**
	 * Initialize network module
	 */
	private void initializeNetworkModule()
	{
		m_networkResponder = new NetworkResponder();
		
		m_networkModule = NetworkModule.getNetworkModule();	// .todo Allow other network modules

		m_networkModule.registerMessageType(NetAddLineSegments.getMessageType());
		m_networkModule.registerMessageType(NetAddMapElement.getMessageType());
		m_networkModule.registerMessageType(NetClearLineSegments.getMessageType());
		m_networkModule.registerMessageType(NetEraseLineSegments.getMessageType());
		m_networkModule.registerMessageType(NetFlipMapElement.getMessageType());
		m_networkModule.registerMessageType(NetGroupAction.getMessageType());
		// NetLoadMap
		// NetLockMapElement
		m_networkModule.registerMessageType(NetLoginComplete.getMessageType());
		m_networkModule.registerMessageType(NetLoginRejected.getMessageType());
		m_networkModule.registerMessageType(NetSetMapElementPosition.getMessageType());
		m_networkModule.registerMessageType(NetRecenterMap.getMessageType());
		// NetRemoveMapElement
		// NetRequestImage
		m_networkModule.registerMessageType(NetSetAngleMapElement.getMessageType());
		m_networkModule.registerMessageType(NetSendChatText.getMessageType());
		// NetSendImage
		m_networkModule.registerMessageType(NetSendMechanicsText.getMessageType());	
		m_networkModule.registerMessageType(NetSendPlayerInfo.getMessageType());
		m_networkModule.registerMessageType(NetSendPlayersList.getMessageType());
		m_networkModule.registerMessageType(NetSendTypingFlag.getMessageType(m_networkResponder));
		m_networkModule.registerMessageType(NetSetBackground.getMessageType());
		m_networkModule.registerMessageType(NetSetGridMode.getMessageType());
		// NetSetMapElementData
		// NetSetMapElementLayer
		// NetSetMapElementSize
		// NetSetMapElementType
		m_networkModule.registerMessageType(NetShowPointingMarker.getMessageType());
		
		
		//--------------------------
		NetworkListenerIF listener = new NetworkAdapter() {
			/*
			 * @see com.galactanet.gametable.data.net.NetworkAdapter#connectionEstablished(com.galactanet.gametable.data.net.Connection)
			 */
			@Override
			public void connectionEstablished(NetworkConnectionIF conn)
			{
				onNetworkConnectionEstablished(conn);
			}
			
			/*
			 * @see com.galactanet.gametable.data.net.NetworkAdapter#connectionEnded()
			 */
			@Override
			public void connectionEnded()
			{
				onNetworkConnectionClosed();
			}
			
			/*
			 * @see com.galactanet.gametable.data.net.NetworkAdapter#connectionDropped(com.galactanet.gametable.data.net.Connection)
			 */
			@Override
			public void connectionDropped(NetworkConnectionIF conn)
			{
				onNetworkConnectionDropped(conn);
			}
			
			/*
			 * @see com.galactanet.gametable.data.net.NetworkAdapter#networkStatusChange(com.galactanet.gametable.data.net.NetworkStatus)
			 */
			@Override
			public void networkStatusChange(NetworkStatus status)
			{
				updateStatus();
			}
		};
		
		m_networkModule.addListener(listener);
	}

	/**
	 * Initializes the tools from the ToolManager.
	 */
	private void initializeTools()
	{
		try
		{
			m_toolManager.initialize();
			final int buttonSize = m_toolManager.getMaxIconSize();
			final int numTools = m_toolManager.getNumTools();
			m_toolButtons = new JToggleButton[numTools];
			for (int toolId = 0; toolId < numTools; toolId++)
			{
				final ToolManager.Info info = m_toolManager.getToolInfo(toolId);
				final Image im = Images.createBufferedImage(buttonSize, buttonSize);
				{
					final Graphics g = im.getGraphics();
					final Image icon = info.getIcon();
					final int offsetX = (buttonSize - icon.getWidth(null)) / 2;
					final int offsetY = (buttonSize - icon.getHeight(null)) / 2;
					g.drawImage(info.getIcon(), offsetX, offsetY, null);
					g.dispose();
				}

				final JToggleButton button = new JToggleButton(new ImageIcon(im));
				m_toolBar.add(button);
				button.addActionListener(new ToolButtonActionListener(toolId));
				button.setFocusable(false);
				m_toolButtonGroup.add(button);
				m_toolButtons[toolId] = button;

				String keyInfo = "";
				if (info.getQuickKey() != null)
				{
					final String actionId = "tool" + toolId + "Action";
					getGametableCanvas().getActionMap().put(actionId, new ToolButtonAbstractAction(toolId));
					final KeyStroke keystroke = KeyStroke.getKeyStroke("ctrl " + info.getQuickKey());
					getGametableCanvas().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keystroke, actionId);
					keyInfo = " (Ctrl+" + info.getQuickKey() + ")";
				}
				button.setToolTipText(info.getName() + keyInfo);
				final List<PreferenceDescriptor> prefs = info.getTool().getPreferences();
				for (PreferenceDescriptor desc : prefs)
				{
					m_preferences.addPreference(desc);
				}
			}
		}
		catch (final IOException ioe)
		{
			Log.log(Log.SYS, getLanguageResource().TOOLBAR_FAIL);
			Log.log(Log.SYS, ioe);
		}
	}

	private void loadGridFromXML(Element root, XMLSerializeConverter converter)
	{
		// grid
		Element gridEl = XMLUtils.getFirstChildElementByTagName(root, "grid");
		if (gridEl != null)
		{
			GridModeID gridMode = GridModeID.SQUARES;
			try
			{
				gridMode = GridModeID.valueOf(gridEl.getAttribute("modeid"));  
			}
			catch (Throwable e)
			{
				// stick with default if value is not parsable
			}
				
			//, GridModeID.NONE.ordinal()));
			m_gametableCanvas.setGridModeByID(gridMode);

			// grid background
			Element bkEl = XMLUtils.getFirstChildElementByTagName(gridEl, "background");
			String typeFQN = bkEl.getAttribute("element_type");
			MapElementTypeIF type = MapElementTypeLibrary.getMasterLibrary().getElementType(typeFQN);
			if (type != null)
			{
				changeBackground(type, null);
			}
			else
			{
				String color = bkEl.getAttribute("color");
				BackgroundColor bkColor = BackgroundColor.DEFAULT;

				try
				{
					bkColor = BackgroundColor.valueOf(color);
				}
				catch (IllegalArgumentException e)
				{
					// stick to default
				}
				catch (NullPointerException e)
				{
					// stick to default
				}

				changeBackground(bkColor, null);
			}
		}
	}

	/**
	 * Store locked element list
	 * 
	 * @param root
	 * @param converter
	 */
	private void loadLockedElementsFromXML(Element root, XMLSerializeConverter converter)
	{
		m_gametableCanvas.lockAllMapElements(m_gametableCanvas.isPublicMap(), false);
		Element listEl = XMLUtils.getFirstChildElementByTagName(root, "locked");
		if (listEl == null)
			return;

		for (Element el : XMLUtils.getChildElementsByTagName(listEl, "id"))
		{
			long id = UtilityFunctions.parseLong(XMLUtils.getNodeValue(el), 0);
			MapElementID elID = converter.getMapElementID(id);
			if (elID != null)
			{
				MapElement mapEl = getMapElement(elID);
				if (mapEl != null)
					m_gametableCanvas.lockMapElement(mapEl, true);
			}
		}
	}

	/**
	 * Load map
	 * 
	 * @param loadPublic Load public map from file
	 * @param loadPrivate Load private map from file
	 */
	private void loadMap(boolean loadPublic, boolean loadPrivate)
	{
		final File openFile = UtilityFunctions.doFileOpenDialog(getLanguageResource().OPEN, "xml", true);

		if (openFile != null)
		{
			loadFromXML(openFile, loadPublic, loadPrivate);

			if (getNetworkStatus() == NetworkStatus.HOSTING)
			{
				// Send data to other connected players (host only)
				sendBroadcast(NetLoadMap.makePacket(m_gametableCanvas.getPublicMap()));
			}
		}
	}

	/**
	 * Show the networking dialog
	 * @return
	 */
	private boolean showNetworkingDialog()
	{
		final StartNetworkingDialog dialog = new StartNetworkingDialog();
		dialog.setLocationRelativeTo(m_gametableCanvas);
		dialog.setVisible(true);

		// Check for cancellation
		if (!dialog.m_bAccepted)
			return false;
		
		return true;
	}

	/**
	 * Saves everything: both maps, macros, and preferences. Called on program exit.
	 */
	private void saveAll()
	{
		saveToXML(getAutoSaveXMLFile());
		// saveState(getGametableCanvas().getPublicMap(), new File("autosave.grm"));
		// saveState(getGametableCanvas().getPrivateMap(), new File("autosavepvt.grm"));
		savePrefs();
	}

	/**
	 * Store grid information
	 * 
	 * @param doc
	 * @param root
	 */
	private void storeGridToXML(Document doc, Element root)
	{
		// grid
		Element gridEl = doc.createElement("grid");
		gridEl.setAttribute("modeid", String.valueOf(getGametableCanvas().getGridModeId()));
		root.appendChild(gridEl);

		// grid background
		Element bkEl = doc.createElement("background");
		gridEl.appendChild(bkEl);
		
		MapElementTypeIF type = m_gametableCanvas.getBackgroundMapElementType();
		if (type != null)
		{
			bkEl.setAttribute("element_type",type.getFullyQualifiedName());
		}
		else
		{
			bkEl.setAttribute("color", m_gametableCanvas.getBackgroundColor().name());
		}
	}

	/**
	 * Store locked element list
	 * 
	 * @param doc
	 * @param root
	 */
	private void storeLockedElementsToXML(Document doc, Element root)
	{
		List<MapElement> elements = m_gametableCanvas.getlockedMapElementInstances();
		if (elements.size() == 0)
			return;

		Element listEl = doc.createElement("locked");
		for (MapElement el : elements)
		{
			listEl.appendChild(XMLUtils.createElementValue(doc, "id", String.valueOf(el.getID().numeric())));
		}

		root.appendChild(listEl);
	}
	
	/**
	 * starts the execution thread
	 */
	private void initializeExecutorThread()
	{
		if (m_executorThread != null)
		{
			m_executorThread.interrupt();
			m_executorThread = null;
		}

		// start the poll thread
		m_executorThread = new PeriodicExecutorThread(new Runnable() {
			public void run()
			{
				tick();
			}
		});
		m_executorThread.start();
	}
	
	/**
	 * Tick of the internal timer
	 */
	private void tick()
	{
		final long now = System.currentTimeMillis();
		long diff = now - m_lastTickTime;
		if (m_lastTickTime == 0)
		{
			diff = 0;
		}
		m_lastTickTime = now;
		tick(diff);
	}
	
	/**
	 * Tick of the internal status timer
	 * @param ms
	 */
	private void tick(final long ms)
	{
		m_gametableCanvas.tick(ms);
	}

	private void toggleMechanicsWindow()
	{
		m_chatPanel.toggleMechanicsWindow();
		validate();
	}

	/**
	 * Get the installed network module
	 * @return
	 */
	public NetworkModuleIF getNetworkModule()
	{
		return m_networkModule;
	}
	
	private NetworkModuleIF m_networkModule;
	
	/**
	 * GroupManagerListener implementation
	 *
	 * @author Eric Maziade
	 */
	private class GroupManagerListener implements GroupManagerListenerIF
	{		
		@Override
		public void onRemoveMapElementFromGroup(Group group, MapElementID mapElementID, NetworkEvent netEvent)
		{
			// We don't want to re-broadcast if the source is not user action
			if (netEvent == null)			
				send(Action.REMOVE_ELEMENT, group, mapElementID);
		}
		
		@Override
		public void onRemoveGroup(Group group, NetworkEvent netEvent)
		{
			if (netEvent == null)
				send(Action.DELETE, group, null);
		}
		
		@Override
		public void onGroupRename(Group group, String oldGroupName, NetworkEvent netEvent)
		{
			if (netEvent == null)
				sendRename(group, oldGroupName, group.getName());
		}
		
		@Override
		public void onAddMapElementToGroup(Group group, MapElementID mapElementID, NetworkEvent netEvent)
		{
			if (netEvent == null)
				send(Action.ADD_ELEMENT, group, mapElementID);
		}

		/**
		 * Send a network packet
		 * 
		 * @param action Network action to perform
		 * @param groupName Name of the affected group
		 * @param elementID Unique element ID, if the action is related to an element.
		 */
		private void send(NetGroupAction.Action action, final Group group, final MapElementID elementID)
		{
			if (getNetworkStatus() == NetworkStatus.DISCONNECTED)
				return;
			
			GametableCanvas canvas = getGametableCanvas();

			// Make sure we are not processing the packet
			// Ignore if editing the protected map (publish action will handle networking when needed)
			if (canvas.isPublicMap() && !PacketSourceState.isNetPacketProcessing())
			{
				final int player = getMyPlayerId();
				GametableFrame.this.sendBroadcast(NetGroupAction.makePacket(action, group == null ? "" : group.getName(), null, elementID, player));
			}
		}

		/**
		 * Send a network packet
		 * 
		 * @param group group
		 * @param newName new name
		 */
		private void sendRename(final Group group, String oldName, String newName)
		{
			if (getNetworkStatus() == NetworkStatus.DISCONNECTED)
				return;
			
			GametableCanvas canvas = getGametableCanvas();

			// Make sure we are not processing the packet
			// Ignore if editing the protected map (publish action will handle networking when needed)
			if (canvas.isPublicMap() && !PacketSourceState.isNetPacketProcessing())
			{
				final int player = getMyPlayerId();
				GametableFrame.this.sendBroadcast(NetGroupAction.makeRenamePacket(oldName, newName, player));
			}
		}
	}
	
	/**
	 * Verifies whether changes to the data should be propagated over the network
	 * 
	 * @param netEvent Network event that might have triggered the changes
	 * @return True to propagate, false otherwise.
	 */
	protected boolean shouldPropagateChanges(NetworkEvent netEvent)
	{
		return getNetworkStatus() != NetworkStatus.DISCONNECTED && netEvent == null && !isLoggingIn();
	}
	
	/**
	 * Flag denoting that we are currently in the 'logging in' phase of connecting to a hosted game.
	 */
	private boolean m_loggingIn = false;
	
	/**
	 * Set the color of the background
	 * @param color
	 * @param netEvent Triggering network event or null
	 */
	public void changeBackground(BackgroundColor color, NetworkEvent netEvent)
	{
		m_gametableCanvas.changeBackground(color, netEvent);
	}
	
	/**
	 * Set the background's tile
	 * @param type
	 * @param netEvent Triggering network event or null
	 */
	public void changeBackground(MapElementTypeIF type, NetworkEvent netEvent)
	{
		m_gametableCanvas.changeBackground(type, netEvent);
	}

	/**
	 * Connection to host (if not hosting)
	 */
	private NetworkConnectionIF m_hostConnection = null;
	
	/**
	 * Class to opens up hidden functionality to the UI's networking message package
	 *
	 * @author Eric Maziade
	 */
	public class NetworkResponder
	{
		/**
		 * Updates the typing status
		 * @param playerID ID of the player typing
		 * @param typing True if typing, false otherwise
		 */
		public void updateTypingStatus(int playerID, boolean typing)
		{
			Player player = getPlayerByID(playerID);
			if (player != null)
			{			
				if (typing)
				{
					if (!m_typingPlayers.contains(player))
						m_typingPlayers.add(player);
				}
				else
					m_typingPlayers.remove(player);
			}
			
			updateStatus();
		}
	}
	
	/**
	 * Instance of network message handling class
	 */
	private NetworkResponder m_networkResponder;
	
	public static enum GameTableMapType { PUBLIC, PRIVATE, ACTIVE }
}