/*
 * GametableCore.java
 *
 * @created 2010-10-14
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

package com.gametable.data;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gametable.GametableApp;
import com.gametable.data.ChatEngineIF.MessageType;
import com.gametable.data.MapElementTypeIF.Layer;
import com.gametable.data.grid.GridModeID;
import com.gametable.data.grid.HexGridMode;
import com.gametable.data.grid.SquareGridMode;
import com.gametable.data.net.*;
import com.gametable.data.net.NetGroupAction.Action;
import com.gametable.module.Module;
import com.gametable.net.*;
import com.gametable.ui.BackgroundColor;
import com.gametable.ui.GametableFrame;
import com.gametable.util.Log;
import com.gametable.util.SelectionHandler;
import com.gametable.util.UtilityFunctions;
import com.maziade.props.XProperties;
import com.maziade.tools.XMLUtils;
import com.maziade.tools.XMLUtils.XMLOutputProperties;

/**
 * Core data class
 *
 * @author Eric Maziade
 */
public class GameTableCore implements MapElementRepositoryIF
{
	private final static String PROPERTY_BUNDLE = "com.gametable.data";
	public static final String RESOURCE_PATH = "com.gametable.data.resources";

	/**
	 * Constructor - should only be called by GametableApp 
	 */
	public GameTableCore()
	{
		m_publicMap = new GameTableMap(true);
		m_privateMap = new GameTableMap(false);
		m_activeMap = m_publicMap;
		
		GameTableMapListenerIF mapListener = new MapListener();
		m_publicMap.addListener(mapListener);
		m_privateMap.addListener(mapListener);
		
		m_publicMap.addMapElementListener(new MapElementListener(true));
		m_privateMap.addMapElementListener(new MapElementListener(false));
		
		m_lockedElements = new SelectionHandler();
		m_players = new ArrayList<Player>();
		m_unmodifiablePlayers = Collections.unmodifiableList(m_players);
		
		m_playerListener = new PlayerAdapter() {
			@Override
			public void onPointingLocationChanged(Player player, boolean pointing, MapCoordinates location, NetworkEvent netEvent)
			{
				if (shouldPropagateChanges(netEvent))
				{
					if (pointing)
						sendBroadcast(NetShowPointingMarker.makePacket(player, location, true));
					else
						sendBroadcast(NetShowPointingMarker.makePacket(player, MapCoordinates.ORIGIN, false));
				}
				
				for (GameTableCoreListenerIF listener : m_listeners)
					listener.onPointingLocationChanged(player, pointing, location, netEvent);
			}
		};
	}
	
	/**
	 * Initialize the core - NB Properties are not loaded automatically
	 * @throws IOException
	 */
	public void initialize() throws IOException
	{
		Thread.setDefaultUncaughtExceptionHandler(new CoreUncaughtExceptionHandler());

		m_squareGridMode = new SquareGridMode();
		m_hexGridMode = new HexGridMode();
		m_noGridMode = new GridMode();
		
		m_squareGridMode.initialize();
		m_gridMode = m_squareGridMode;

		// todo #Plugins Automated module loading mechanism
		
		File files[] = new File(PLUGIN_FOLDER).listFiles();
		for (File f : files)
		{
			if (f.isDirectory())
				continue;
			
			try
			{
				JarFile jar = new JarFile(f);
				Enumeration <JarEntry> entries = jar.entries();
				while (entries.hasMoreElements())
				{
					JarEntry entry = entries.nextElement();
					
					String name = entry.getName();
					int pos = name.lastIndexOf('/');
					if (pos > -1)
					{
						name = name.substring(pos + 1);
						if (name.equals(PLUGIN_CONFIG_FILE))
						{
							Document xml = XMLUtils.parseXMLDocument(jar.getInputStream(entry), null);
							
							Element classEl = XMLUtils.getFirstChildElementByTagName(xml.getDocumentElement(), "class");
							if (classEl != null)
							{
								String className = XMLUtils.getNodeValue(classEl);
								try
								{
									loadJarModule(f, className);	
								}
								catch (Exception e)
								{
									// TODO #Module - better error handling
									e.printStackTrace();
								}
							}
						}
					}
				}
				
				registerDetectedModules();
				
			}
			catch (Exception e)
			{
				// TODO #Module - better error handling
				e.printStackTrace();
			}
		}
		
		initializeGroupManager();

		initializeMapElementTypeLibrary();
		
		for (Module module : g_modules)
			module.onInitializeCore(this);
				
		addPlayer(new Player(m_playerName, m_characterName, -1, true));
	}
	
	/**
	 * Load a jar dynamically by hacking into the class loader and adding a new valid URL
	 * @param jarFile Jar file to load
	 * @param class Name name of main module class
	 * @return Module instance or null 
	 * @throws Exception
	 */
	private void loadJarModule(File jarFile, String className) throws Exception 
	{
		URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		
	  // Before we start hacking away, see if the module is available from currently loaded source code (very useful for debugging)
		Class<?> moduleClass = null;
		
		
		try
		{
			moduleClass = classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e)
		{
			// Yeah, ok - we'll have to load dynamically
		}

	  // Not found - then lets add the JAR to the list of supported URLS in the system class loader
	  if (moduleClass == null)
	  {	  
				URL url = jarFile.toURI().toURL();
				
			  Class<?> clazz= URLClassLoader.class;
		
			  // Use reflection to call 'addURL'
			  java.lang.reflect.Method method= clazz.getDeclaredMethod("addURL", new Class[] { URL.class });
			  method.setAccessible(true);
			  method.invoke(classLoader, new Object[] { url });
			  
			  // Try again to load the class - this time we let the exception trigger
			  moduleClass = classLoader.loadClass(className);			  
	  }
	  
	  registerModule(moduleClass);
	}
	
	/**
	 * 
	 * @param moduleClass
	 */
	@SuppressWarnings("unchecked")
	private void registerModule(Class<?> moduleClass)
	{	
		if (moduleClass == null)
			return;
		
		// TODO #Plugins Handle plugin configurations - is this a new plugin?  was this plugin disabled?  These would be useful
		
		Log.log(Log.SYS, "Detected dynamic module : " + moduleClass.getCanonicalName());
		
		// See if it is a network module
		if (NetworkModuleIF.class.isAssignableFrom(moduleClass))
		{
			try
			{
				Method getMethod = moduleClass.getMethod("getNetworkModule");
	  		NetworkModuleIF module = (NetworkModuleIF)getMethod.invoke(null);
	  		initializeNetworkModule(module);
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
				Log.log(Log.SYS, "getNetworkModule method failed in " + moduleClass.getCanonicalName() + " " + e.getMessage());
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
				Log.log(Log.SYS, "getNetworkModule method failed in " + moduleClass.getCanonicalName() + " " + e.getMessage());
			}
			catch (NoSuchMethodException e)
			{
				e.printStackTrace();
				Log.log(Log.SYS, "getNetworkModule method not found in " + moduleClass.getCanonicalName());				
			}
		}
		
		// If it is a module module, we need to store it to initialize it later (second pass)
		if (Module.class.isAssignableFrom(moduleClass))
			g_moduleClasses.add((Class<Module>)moduleClass);
	}
	
	/**
	 * Register modules that were detected in the previous pass
	 */
	private void registerDetectedModules()
	{
		for (Class<Module> moduleClass : g_moduleClasses)
		{
			try
			{
			  Method getMethod = moduleClass.getMethod("getModule");
			  	
		  	if (getMethod != null)
		  	{
		  		Module module = (Module)getMethod.invoke(null);
		  		
		  		if (module != null)
		  		{
			  		g_modules.remove(module);	  		
			  		g_modules.add(module);
		  		}
		  	}
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
				Log.log(Log.SYS, "getModule method failed in " + moduleClass.getCanonicalName() + " " + e.getMessage());
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
				Log.log(Log.SYS, "getModule method failed in " + moduleClass.getCanonicalName() + " " + e.getMessage());
			}
			catch (NoSuchMethodException e)
			{
				e.printStackTrace();
				Log.log(Log.SYS, "getModule method not found in " + moduleClass.getCanonicalName());				
			}
		}
	}
	
	/**
	 * Verifies whether changes to the data should be propagated over the network
	 * 
	 * @param netEvent Network event that might have triggered the changes
	 * @return True to propagate, false otherwise.
	 */
	private boolean shouldPropagateChanges(NetworkEvent netEvent)
	{
		return getNetworkStatus() != NetworkStatus.DISCONNECTED && netEvent == null && !isLoggingIn();
	}

	/**
	 * Holds data about which element is locked
	 */
	private SelectionHandler		m_lockedElements;
		
	/**
	 * List of connected players
	 */
	private final List<Player>						m_players;
	
	
	/**
	 * Unmodifiable list, synchronized to the players list
	 */
	private final List<Player>						m_unmodifiablePlayers;

	/**
	 * Instance of player listener to react to player data changes
	 */
	private final PlayerListenerIF	m_playerListener;
	
	/**
	 * Initialize network module
	 */
	private void initializeNetworkModule(NetworkModuleIF networkModule)
	{
		m_networkModule = networkModule;

		m_networkModule.registerMessageType(NetAddLineSegments.getMessageType());
		m_networkModule.registerMessageType(NetAddMapElement.getMessageType());
		m_networkModule.registerMessageType(NetClearLineSegments.getMessageType());
		m_networkModule.registerMessageType(NetEraseLineSegments.getMessageType());
		m_networkModule.registerMessageType(NetFlipMapElement.getMessageType());
		m_networkModule.registerMessageType(NetGroupAction.getMessageType());
		
		m_networkModule.registerMessageType(NetLoadMap.getMessageType());
 
		m_networkModule.registerMessageType(NetLockMapElements.getMessageType());
		m_networkModule.registerMessageType(NetLoginComplete.getMessageType());
		m_networkModule.registerMessageType(NetLoginRejected.getMessageType());
		m_networkModule.registerMessageType(NetRemoveMapElement.getMessageType());
		m_networkModule.registerMessageType(NetSendChatText.getMessageType());
		
		m_networkModule.registerMessageType(NetRequestFile.getMessageType());
		m_networkModule.registerMessageType(NetSendFile.getMessageType());
		
		m_networkModule.registerMessageType(NetSendPlayerInfoToHost.getMessageType(m_networkResponder));
		m_networkModule.registerMessageType(NetSendPlayerInfo.getMessageType());
		m_networkModule.registerMessageType(NetSendPlayersList.getMessageType(m_networkResponder));
		
		m_networkModule.registerMessageType(NetSetBackground.getMessageType());
		m_networkModule.registerMessageType(NetSetGridMode.getMessageType());
		m_networkModule.registerMessageType(NetSetMapElementAngle.getMessageType());
		m_networkModule.registerMessageType(NetSetMapElementPosition.getMessageType());
		m_networkModule.registerMessageType(NetSetMapElementData.getMessageType());
		m_networkModule.registerMessageType(NetSetMapElementLayer.getMessageType());
		m_networkModule.registerMessageType(NetSetMapElementSize.getMessageType());
		m_networkModule.registerMessageType(NetSetMapElementType.getMessageType());
		m_networkModule.registerMessageType(NetShowPointingMarker.getMessageType());
		
		//--------------------------
		NetworkListenerIF listener = new NetworkAdapter() {
			/*
			 * @see com.gametable.data.net.NetworkAdapter#connectionEstablished(com.gametable.data.net.Connection)
			 */
			@Override
			public void connectionEstablished(NetworkConnectionIF conn)
			{
				onNetworkConnectionEstablished(conn);
			}
			
			/*
			 * @see com.gametable.data.net.NetworkAdapter#connectionEnded()
			 */
			@Override
			public void connectionEnded()
			{
				onNetworkConnectionClosed();
			}
			
			/*
			 * @see com.gametable.data.net.NetworkAdapter#connectionDropped(com.gametable.data.net.Connection)
			 */
			@Override
			public void connectionDropped(NetworkConnectionIF conn)
			{
				onNetworkConnectionDropped(conn);
			}
		};
		
		m_networkModule.addListener(listener);
	}
	
	/**
	 * Network module
	 */
	private NetworkModuleIF m_networkModule;
	
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
			sendMessageLocal(MessageType.ALERT, "Your connection to the host was lost.");
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
			sendMessageBroadcast(MessageType.SYSTEM, dead.getPlayerName() + " " + "has left the session");
		}
		else
		// if we didn't find the player then the connection failed while login in
		{
			sendMessageLocal(MessageType.ALERT, "Someone tried to log in, but was rejected.");
		}
	}
	
	/**
	 * Handles the closing of a network connection
	 */
	private void onNetworkConnectionClosed()
	{
		// Make me the only player in the game
		clearAllPlayers();
		final Player me = new Player(m_playerName, m_characterName, -1, true);
		addPlayer(me);
	}
	
	/**
	 * Handles the establishment of a new network connection
	 * @param conn New network connection
	 */
	private void onNetworkConnectionEstablished(NetworkConnectionIF conn)
	{
		if (getNetworkStatus() == NetworkStatus.HOSTING)
		{
			// clear out all players
			clearAllPlayers();
			final Player me = new Player(m_playerName, m_characterName, 0, true); // this means the host is always at ID 0
			m_nextPlayerId = 1;
			m_player = me;
			addPlayer(me);

			// Advise listeners 
			for (GameTableCoreListenerIF listener : m_listeners)
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
				m_player = me;

				// reset game data
				getMap(GameTableCore.MapType.PUBLIC).clearMap(null);
				
				// Send player info to the host
				send(NetSendPlayerInfoToHost.makePacket(me, m_networkModule.getPassword()), conn);
				
				m_loggingIn = false;
			}
			catch (final Exception ex)
			{
				Log.log(Log.SYS, ex);
			}
		}
	}


	/**
	 * List of registered modules
	 */
	private static List<Module>						g_modules		= new ArrayList<Module>();
	
	/**
	 * List of registered modules classes
	 */
	private static List<Class<Module>>		g_moduleClasses		= new ArrayList<Class<Module>>();

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
	 * Initialize group manager
	 */
	private void initializeGroupManager()
	{
		getMap(GameTableCore.MapType.PUBLIC).getGroupManager().addListener(new GroupManagerListener());
	}
	
	/**
	 * Initialize the master type library
	 */
	private void initializeMapElementTypeLibrary() throws IOException
	{
		m_mapElementTypeLibrary = MapElementTypeLibrary.getMasterLibrary();
		
		m_mapElementTypeLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_mapElementTypeLibrary, new File("pogs"), Layer.POG));
		m_mapElementTypeLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_mapElementTypeLibrary, new File("environment"), Layer.ENVIRONMENT));
		m_mapElementTypeLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_mapElementTypeLibrary, new File("overlays"), Layer.OVERLAY));
		m_mapElementTypeLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_mapElementTypeLibrary, new File("underlays"), Layer.UNDERLAY));
	}
	
	/**
	 * Pointer to the master type library
	 */
	private MapElementTypeLibrary		m_mapElementTypeLibrary	= null;
	

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

		if (m_chatEngine != null)
			m_chatEngine.onPlayersChanged();
	}
	
	/**
	 * Called by the UI to register its chat engine.
	 * @param chatEngine Chat engine to receive notifications for display
	 */
	public void registerChatEngine(ChatEngineIF chatEngine)
	{
		m_chatEngine = chatEngine;
	}
	
	/**
	 * Player name
	 */
	private String m_playerName							= System.getProperty("user.name");
	
	/**
	 * Character name
	 */
	private String		m_characterName	= "Anonymous";
	
	
	
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
	 * Flag denoting that we are currently in the 'logging in' phase of connecting to a hosted game.
	 */
	private boolean m_loggingIn = false;
	

	/**
	 * Disconnect from a joined game or terminate a hosted game. 
	 */
	public void disconnect()
	{
		if (getNetworkStatus() == NetworkStatus.DISCONNECTED)
			return;
		
		m_networkModule.disconnect();
	}
	
	/**
	 * gets the player associated with a connection
	 * 
	 * @param conn connection to use to find the player
	 * @return the player object associated with the connection
	 */
	public Player getPlayerFromConnection(final NetworkConnectionIF conn)
	{
		for (Player player : m_players)
		{
			if (conn == player.getConnection())
				return player;
		}

		return null;
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
	 * Send current players information to all connected players
	 */
	private void sendPlayersInformation()
	{
		// Since the player list message includes a pointer telling which player the recipient is,
		// We will not be broadcasting the list, but sending a specific message to each player
		
		Player me = getPlayer();
		
		for (Player recipient : m_players)
		{
			if (me == recipient)
				continue;
			
			final byte[] castPacket = NetSendPlayersList.makePacket(recipient);
			send(castPacket, recipient);
		}
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
	 * The ID to be assigned to the next player to connect to our hosted game 
	 */
	private int	m_nextPlayerId = 1;
	
	/**
	 * The current player
	 */
	private Player m_player = null;
	
	/**
	 * Core listeners
	 */
	private List<GameTableCoreListenerIF>	m_listeners	= new ArrayList<GameTableCoreListenerIF>();
	
	/**
	 * Get the requested GameTableMap instance
	 * @param type active, private or public map
	 * @return GameTableMap
	 */
	public GameTableMap getMap(GameTableCore.MapType type)
	{
		switch (type)
		{
		case ACTIVE:
			return m_activeMap;
			
		case PRIVATE:
			return m_privateMap;
			
		case PUBLIC:
			return m_publicMap;
		}
		
		throw new IllegalArgumentException("There is no map of type " + type);
	}

	/**
	 * Sets the currently active map
	 * @param type active, private or public map
	 */
	public void setActiveMap(GameTableCore.MapType type)
	{
		switch (type)
		{
		case ACTIVE:
			// do nothing;
			break;
			
		case PRIVATE:
			m_activeMap = m_privateMap;
			break;
			
		case PUBLIC:
			m_activeMap = m_publicMap;
			break;
			
		default:
			throw new IllegalArgumentException("There is no map of type " + type);
		}
		
		// Notify modules
		for (Module module : g_modules)
			module.onActiveMapChange(isActiveMapPublic());

		// Notify core listeners
		for (GameTableCoreListenerIF listener : m_listeners)
			listener.onActiveMapChange(isActiveMapPublic());
	}
	
	
	/**
	 * @return True if the active map is the public map
	 */
	public boolean isActiveMapPublic()
	{
		return (m_activeMap == m_publicMap);
	}
	
	/**
	 * Pointer to the public map
	 */
	private final GameTableMap	m_publicMap;

	/**
	 * Pointer to the private map
	 */
	private final GameTableMap	m_privateMap;
	
	/**
	 * Pointer to the active map
	 */
	private GameTableMap	m_activeMap;
	
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
			// NB : send checks network event to prevent rebroadcasts 
			send(Action.REMOVE_ELEMENT, group, mapElementID, netEvent);
		}
		
		@Override
		public void onRemoveGroup(Group group, NetworkEvent netEvent)
		{
			// NB : send checks network event to prevent rebroadcasts
			send(Action.DELETE, group, null, netEvent);
		}
		
		@Override
		public void onGroupRename(Group group, String oldGroupName, NetworkEvent netEvent)
		{
			// NB : send checks network event to prevent rebroadcasts
			sendRename(group, oldGroupName, group.getName(), netEvent);
		}
		
		@Override
		public void onAddMapElementToGroup(Group group, MapElementID mapElementID, NetworkEvent netEvent)
		{		
			// NB : send checks network event to prevent rebroadcasts
			send(Action.ADD_ELEMENT, group, mapElementID, netEvent);
		}

		/**
		 * Send a network packet. Checks network event to prevent rebroadcasts
		 * 
		 * @param action Network action to perform
		 * @param groupName Name of the affected group
		 * @param elementID Unique element ID, if the action is related to an element.
		 * @param netEvent Triggering event
		 */
		private void send(NetGroupAction.Action action, final Group group, final MapElementID elementID, NetworkEvent netEvent)
		{
			if (!shouldPropagateChanges(netEvent))
				return;
				
			// Ignore if editing the private map (publish action will handle networking when needed)
			if (isActiveMapPublic())
				GameTableCore.this.sendBroadcast(NetGroupAction.makePacket(action, group == null ? "" : group.getName(), null, elementID, getPlayer()));
		}

		/**
		 * Send a network rename packet. Checks network event to prevent rebroadcasts
		 * @param group Group to rename
		 * @param oldName Old name
		 * @param newName New name 
		 * @param netEvent Triggering event
		 */
		private void sendRename(final Group group, String oldName, String newName, NetworkEvent netEvent)
		{
			if (!shouldPropagateChanges(netEvent))
				return;

			// Ignore if editing the protected map (publish action will handle networking when needed)
			if (isActiveMapPublic())
				GameTableCore.this.sendBroadcast(NetGroupAction.makeRenamePacket(oldName, newName, getPlayer()));
		}
	}
	

	/**
	 * @return The local player
	 */
	public Player getPlayer()
	{
		if (m_player == null)
		{
			m_player = new Player(getPlayerName(), getCharacterName(), 0, true);
			m_players.add(m_player);
		}

		return m_player;
	}
	
	/**
	 * @return This local player's ID.  Returns 0 if no local player is set.
	 */
	public int getPlayerID()
	{
		if (m_player == null)
			return 0;
		
		return m_player.getID();
	}


	/**
	 * Send an alert message to the chat window
	 * @param type The type of message to display
	 * @param text Text to display
	 */
	public void sendMessageLocal(MessageType type, String text)
	{
		if (m_chatEngine != null)
			m_chatEngine.displayMessage(type, text);
	}

	/**
	 * Send a message to all players
	 * @param msgType The type of message to send
	 * @param text message to send
	 */
	public void sendMessageBroadcast(MessageType msgType, String text)
	{
		if (m_chatEngine != null)
			m_chatEngine.displayMessage(msgType, text);
		
		if (getNetworkStatus() != NetworkStatus.DISCONNECTED)
			sendBroadcast(NetSendChatText.makeBroadcastPacket(msgType, text));
	}
	
	/**
	 * Remove all messages from the chat window
	 */
	public void clearMessages()
	{
		if (m_chatEngine != null)
			m_chatEngine.clearMessages();
	}
	
	/**
	 * Sends a message specifically to a given player
	 * @param msgType The type of message to send
	 * @param toName Name of the player to send to
	 * @param text Text to send
	 */
	public void sendMessage(MessageType msgType, Player to, final String text)
	{
		if (getNetworkStatus() == NetworkStatus.DISCONNECTED)
		{
			if (getPlayer().equals(to))
			{
				if (m_chatEngine != null)
					m_chatEngine.displayMessage(msgType, text);
			}
		}
		else
		{
			sendBroadcast(NetSendChatText.makePacket(msgType, to, text));
		}
	}

	/**
	 * Registered chat engine to display the results.
	 * If null, no chat output will be shown
	 */
	private ChatEngineIF m_chatEngine;
	
	/**
	 * @return Connection to the host player.  Will be 'null' if current player is host.
	 */
	public NetworkConnectionIF getHostConnection()
	{
		return m_hostConnection;
	}
	
	/**
	 * Connection to host (null if hosting or disconnected)
	 */
	private NetworkConnectionIF m_hostConnection = null;
	

	/**
	 * Adds a listener to the core
	 * 
	 * @param listener Listener to call
	 */
	public void addListener(GameTableCoreListenerIF listener)
	{
		if (!m_listeners.contains(listener))
			m_listeners.add(listener);
	}

	/**
	 * Get requested player by ID
	 * @param playerID ID of player we are looking for
	 * @return player object or null
	 */
	public Player getPlayer(int playerID)
	{
		for (Player player : m_players)
		{
			if (player.getID() == playerID)
				return player;
		}
		
		return null;
	}
	

	/**
	 * @return Returns the player list (synchronized with the main player list, unmodifiable).
	 */
	public List<Player> getPlayers()
	{
		return m_unmodifiablePlayers;
	}

	/**
	 * @return The root type library.
	 */
	public MapElementTypeLibrary getMapElementTypeLibrary()
	{
		return m_mapElementTypeLibrary;
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
		switch (gridMode)
		{
		case NONE:
			m_gridMode = m_noGridMode;
			break;

		case SQUARES:
			m_gridMode = m_squareGridMode;
			break;

		case HEX:
			m_gridMode = m_hexGridMode;
			break;
		}

		// Make sure grid mode has been initialized
		m_gridMode.initialize();
		
		if (shouldPropagateChanges(netEvent))
			sendBroadcast(NetSetGridMode.makePacket(gridMode));

		for (GameTableCoreListenerIF listener : m_listeners)
			listener.onGridModeChanged(gridMode, netEvent);
	}
	
	/**
	 * @return The currently set grid mode implementation
	 */
	public GridMode getGridMode()
	{
		return m_gridMode;
	}
	
	/**
	 * @return The currently set grid mode
	 */
	public GridModeID getGridModeID()
	{
		if (m_gridMode == m_squareGridMode)
			return GridModeID.SQUARES;
		
		if (m_gridMode == m_hexGridMode)
			return GridModeID.HEX;
		
		return GridModeID.NONE;
	}
	
	/**
	 * Get the group manager instance handling the groups for the requested map type
	 * @param mapType Map type
	 * @return Group Manager 
	 */
	public GroupManager getGroupManager(GameTableCore.MapType mapType)
	{
		return getMap(mapType).getGroupManager();
	}
	
	/**
	 * The currently selected grid mode
	 */
	private GridMode										m_gridMode;
	
	/**
	 * Square grid mode implementation
	 */
	private SquareGridMode							m_squareGridMode;
	
	/**
	 * Hex grid mode implementation
	 */
	private HexGridMode									m_hexGridMode;
	
	/**
	 * No grid mode implementation
	 */
	private GridMode										m_noGridMode;

	
	/**
	 * Lock specified all map elements from a given map
	 * @param mapType Map on which to lock
	 * @param lock True to lock, false to unlock
	 */
	public void lockAllMapElements(GameTableCore.MapType mapType, boolean lock)
	{
		lockAllMapElements(mapType, lock, null);
	}

	/**
	 * Lock specified all map elements from a given map
	 * @param mapType Map on which to lock
	 * @param lock True to lock, false to unlock
	 * @param netEvent Network event that triggered the change or null
	 */
	public void lockAllMapElements(GameTableCore.MapType mapType, boolean lock, NetworkEvent netEvent)
	{
		if (mapType.isPublic() && shouldPropagateChanges(netEvent))
			sendBroadcast(NetLockMapElements.makeLockAllPacket(lock));
		
		GameTableMap map = getMap(mapType);
		m_lockedElements.selectMapElements(map.getMapElements(), lock);
		
		if (netEvent == null && getNetworkStatus() != NetworkStatus.DISCONNECTED)
		{
			if (lock)
				sendMessageBroadcast(MessageType.SYSTEM, getPlayer().getPlayerName() + " " + "has locked the Map.");
			else
				sendMessageBroadcast(MessageType.SYSTEM, getPlayer().getPlayerName() + " " + "has unlocked the Map.");
		}
		
		for (GameTableCoreListenerIF listener : m_listeners)
			listener.onAllMapElementsLocked(mapType.isPublic(), lock, netEvent);
	}
	
	/**
	 * Lock specified map elements
	 * @param mapElements Elements to lock
	 * @param lock True to lock, false to unlock
	 */
	public void lockMapElements(GameTableCore.MapType mapType, List<MapElement> mapElements, boolean lock)
	{
		lockMapElements(mapType, mapElements, lock, null);
	}
	
	/**
	 * Lock specified map elements
	 * @param mapElements Elements to lock
	 * @param lock True to lock, false to unlock
	 * @param netEvent Network event that triggered the change or null
	 */
	public void lockMapElements(GameTableCore.MapType mapType, List<MapElement> mapElements, boolean lock, NetworkEvent netEvent)
	{
		if (mapType.isPublic() && shouldPropagateChanges(netEvent))
			sendBroadcast(NetLockMapElements.makePacket(mapElements, lock));
		
		m_lockedElements.selectMapElements(mapElements, lock);
		
		for (GameTableCoreListenerIF listener : m_listeners)
			listener.onMapElementsLocked(mapType.isPublic(), mapElements, lock, netEvent);
	}
	
	/**
	 * Lock specified map elements
	 * @param mapType Map on which to lock
	 * @param mapElement Element to lock
	 * @param lock True to lock, false to unlock
	 */
	public void lockMapElement(GameTableCore.MapType mapType, MapElement mapElement, boolean lock)
	{
		lockMapElement(mapType, mapElement, lock, null);
	}
	
	/**
	 * Lock specified map elements
	 * @param mapType Map on which to lock
	 * @param mapElement Element to lock
	 * @param lock True to lock, false to unlock
	 * @param netEvent Network event that triggered the change or null
	 */
	public void lockMapElement(GameTableCore.MapType mapType, MapElement mapElement, boolean lock, NetworkEvent netEvent)
	{
		if (mapType.isPublic() && shouldPropagateChanges(netEvent))
			sendBroadcast(NetLockMapElements.makePacket(mapElement, lock));
		
		m_lockedElements.selectMapElement(mapElement, lock);
		
		for (GameTableCoreListenerIF listener : m_listeners)
			listener.onMapElementLocked(mapType.isPublic(), mapElement, lock, netEvent);
}
	
	/**
	 * Gets the list of locked element instances
	 * 
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElement> getLockedMapElements()
	{
		return m_lockedElements.getSelectedMapElements();
	}

	/**
	 * Checks if a specific map element is marked as locked
	 * 
	 * @param mapElement Map element to lock
	 * @return true if locked
	 */
	public boolean isMapElementLocked(MapElement mapElement)
	{
		return m_lockedElements.isSelected(mapElement);
	}
	
	/**
	 * Get the installed network module
	 * @return
	 */
	public NetworkModuleIF getNetworkModule()
	{
		return m_networkModule;
	}
	
	/**
	 * Get the list of registered modules
	 * @return list of registered modules
	 */
	public List<Module> getRegisteredModules()
	{
		return g_modules;
	}
	
	/**
	 * Set the player's information
	 * @param playerName Player's name
	 * @param characterName Players' character name
	 */
	public void setPlayerInformation(String playerName, String characterName)
	{
		setPlayerInformation(playerName, characterName, null);
	}
	
	/**
	 * Set the player's information
	 * @param playerName Player's name
	 * @param characterName Players' character name
	 * @param netEvent Network event that triggered the change or null
	 */
	public void setPlayerInformation(String playerName, String characterName, NetworkEvent netEvent)
	{
    m_characterName = characterName;
    m_playerName = playerName;
    
    Player player = getPlayer();
    player.setCharacterName(m_characterName);
    player.setPlayerName(m_playerName);
    
    if (shouldPropagateChanges(netEvent))
    	sendBroadcast(NetSendPlayerInfo.makePacket(getPlayer()));

    for (GameTableCoreListenerIF listener : m_listeners)
    	listener.onPlayerNameChanged(getPlayer(), playerName, characterName, null);
	}
	
	/**
	 * @return The current user's character name
	 */
	public String getCharacterName()
	{
		return m_characterName;
	}
	
	/**
	 * @return The current user's player name
	 */
	public String getPlayerName()
	{
		return m_playerName;
	}
	
	/**
	 * Removes a listener from the core
	 * 
	 * @param listener Listener to remove
	 * @return True if listener was found and removed
	 */
	public boolean removeListener(GameTableCoreListenerIF listener)
	{
		return m_listeners.remove(listener);
	}
	
	/**
	 * @return The current background color or null if background is currently a map element
	 */
	public BackgroundColor getBackgroundColor()
	{
		return m_backgroundTypeMapElement ? null : m_backgroundColor;
	}
	
	/**
	 * @return The current background color or null if background is currently a specified color
	 */
	public MapElementTypeIF getBackgroundMapElementType()
	{
		return m_backgroundTypeMapElement ? m_backgroundElementType : null;
	}

	/**
	 * Color used for background
	 */
	private BackgroundColor		m_backgroundColor						= BackgroundColor.DEFAULT;
	
	/**
	 * Element type used as background
	 */
	private MapElementTypeIF	m_backgroundElementType			= null;
	
	/**
	 * True if the background is map element type
	 */
	private boolean						m_backgroundTypeMapElement	= false;
	
	/**
	 * Core's map listener
	 */
	private class MapListener implements GameTableMapListenerIF 
	{
		/*
		 * @see
		 * com.gametable.data.GameTableMapAdapter#onMapElementInstanceRemoved(com.gametable.data
		 * .GameTableMap, com.gametable.data.MapElement, boolean)
		 */
		@Override
		public void onMapElementRemoved(GameTableMap map, MapElement mapElement, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetRemoveMapElement.makePacket(mapElement));
			}
		}
		
		/*
		 * @see com.gametable.data.GameTableMapAdapter#onMapElementInstancesRemoved(com.gametable.data.GameTableMap, java.util.List, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onMapElementsRemoved(GameTableMap map, List<MapElement> mapElements, NetworkEvent netEvent)
		{
			if (shouldPropagateChanges(netEvent) && mapElements.size() > 0)
				sendBroadcast(NetRemoveMapElement.makePacket(mapElements));
		}

		/*
		 * @see
		 * com.gametable.data.GameTableMapAdapter#onMapElementInstancesCleared(com.gametable.data
		 * .GameTableMap)
		 */
		@Override
		public void onMapElementsCleared(GameTableMap map, NetworkEvent netEvent)
		{
			if (shouldPropagateChanges(netEvent))
				sendBroadcast(NetRemoveMapElement.makeRemoveAllPacket());
		}
		
		/*
		 * @see com.gametable.data.GameTableMapAdapter#onLineSegmentAdded(com.gametable.data.GameTableMap, com.gametable.data.LineSegment, boolean)
		 */
		@Override
		public void onLineSegmentAdded(GameTableMap map, LineSegment lineSegment, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				if (map.isPublicMap())
				{
					// Broadcast only if we're not triggered by a network event
					if (shouldPropagateChanges(netEvent))
						sendBroadcast(NetAddLineSegments.makePacket(lineSegment));
				}
			}				
		}

		/*
		 * @see com.gametable.data.GameTableMapAdapter#onLineSegmentsCropped(com.gametable.data.GameTableMap, com.gametable.data.MapRectangle, boolean, int, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onEraseLineSegments(GameTableMap map, MapRectangle rect, boolean colorSpecific, int color, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetEraseLineSegments.makePacket(rect, colorSpecific, color));
			}
		}
		
		/*
		 * @see com.gametable.data.GameTableMapAdapter#onLineSegmentsAdded(com.gametable.data.GameTableMap, java.util.List)
		 */
		@Override
		public void onLineSegmentsAdded(GameTableMap map, List<LineSegment> lineSegments, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetAddLineSegments.makePacket(lineSegments));
			}
		}
		
		/*
		 * @see com.gametable.data.GameTableMapAdapter#onClearLineSegments(com.gametable.data.GameTableMap, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onClearLineSegments(GameTableMap map, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetClearLineSegments.makePacket());
			}
		}		
		
		/*
		 * @see com.gametable.data.GameTableMapAdapter#onMapElementInstanceAdded(com.gametable.data.GameTableMap, com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onMapElementAdded(GameTableMap map, MapElement mapElement, NetworkEvent netEvent)
		{
			if (map.isPublicMap())
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetAddMapElement.makePacket(mapElement));
			}
		}
	}
	
	/**
	 * Map element listener for use by the core
	 */
	private class MapElementListener implements MapElementListenerIF
	{
		/**
		 * Constructor
		 * @param publicMap True if this listener is linked to a public map 
		 */
		public MapElementListener(boolean publicMap)
		{
			m_listenToPublicMap = publicMap;
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onPositionChanged(com.gametable.data.MapElement, com.gametable.data.MapCoordinates, com.gametable.data.MapCoordinates, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onPositionChanged(MapElement element, MapCoordinates newPosition, MapCoordinates oldPosition, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetSetMapElementPosition.makePacket(element, newPosition));
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onLayerChanged(com.gametable.data.MapElement, com.gametable.data.MapElementTypeIF.Layer, com.gametable.data.MapElementTypeIF.Layer, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onLayerChanged(MapElement element, Layer newLayer, Layer oldLayer, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetSetMapElementLayer.makePacket(element, newLayer));
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onElementTypeChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onElementTypeChanged(MapElement element, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetSetMapElementType.makePacket(element, element.getMapElementType()));
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onFlipChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onFlipChanged(MapElement element, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetFlipMapElement.makePacket(element.getID(), element.getFlipH(), element.getFlipV()));
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onAngleChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAngleChanged(MapElement element, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetSetMapElementAngle.makePacket(element.getID(), element.getAngle()));
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onFaceSizeChanged(com.gametable.data.MapElement, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onFaceSizeChanged(MapElement element, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetSetMapElementSize.makePacket(element, element.getFaceSize()));
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onAttributeChanged(com.gametable.data.MapElement, java.lang.String, java.lang.String, java.lang.String, boolean, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAttributeChanged(MapElement element, String attributeName, String newValue, String oldValue, boolean batch, NetworkEvent netEvent)
		{
			if (!batch)
			{
				if (m_listenToPublicMap)
				{
					// Broadcast only if we're not triggered by a network event
					if (shouldPropagateChanges(netEvent))
					{
						Map<String, String> add = null;
						List<String> remove = null;
						
						if (newValue == null)
						{
							remove = new ArrayList<String>();
							remove.add(attributeName);
						}
						else
						{
							add = new HashMap<String, String>();
							add.put(attributeName, newValue);
						}
							
						sendBroadcast(NetSetMapElementData.makePacket(element, null, add, remove));
					}
				}				
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onAttributesChanged(com.gametable.data.MapElement, java.util.Map, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onAttributesChanged(MapElement element, Map<String, String> attributes, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
				{
					Map<String, String> add = null;
					List<String> remove = null;
					
					for (Entry<String, String> entry : attributes.entrySet())
					{
						if (entry.getValue() == null)
						{
							if (remove == null)
								remove = new ArrayList<String>();
							
							remove.add(entry.getKey());
						}
						else
						{
							if (add == null)
								add = new HashMap<String, String>();
							
							add.put(entry.getKey(), entry.getValue());
						}
					}
						
					sendBroadcast(NetSetMapElementData.makePacket(element, null, add, remove));
				}
			}
		}
		
		/*
		 * @see com.gametable.data.MapElementAdapter#onNameChanged(com.gametable.data.MapElement, java.lang.String, java.lang.String, com.gametable.net.NetworkEvent)
		 */
		@Override
		public void onNameChanged(MapElement element, String newName, String oldName, NetworkEvent netEvent)
		{
			if (m_listenToPublicMap)
			{
				// Broadcast only if we're not triggered by a network event
				if (shouldPropagateChanges(netEvent))
					sendBroadcast(NetSetMapElementData.makeRenamePacket(element, newName));
			}				
		}
		
		private final boolean m_listenToPublicMap;
	}

	public static enum MapType { 
		PUBLIC, PRIVATE, ACTIVE;
		
		public boolean isPublic()
		{
			if (this == PUBLIC)
				return true;
			
			if (this == ACTIVE && GametableApp.getCore().isActiveMapPublic())
				return true;
			
			return false;
		}
	}

	/**
	 * Set the background's tile
	 * @param elementType Map element type to use as background
	 */
	public void setBackgroundMapElementType(MapElementTypeIF elementType)
	{
		setBackgroundMapElementType(elementType, null);
	}
	
	/**
	 * Set the background's tile
	 * @param elementType Map element type to use as background
	 * @param netEvent Network event that triggered the change
	 */
	public void setBackgroundMapElementType(MapElementTypeIF elementType, NetworkEvent netEvent)
	{
		m_backgroundElementType = elementType;
		m_backgroundTypeMapElement = true;
		
		if (shouldPropagateChanges(netEvent))
			sendBroadcast(NetSetBackground.makePacket(elementType));
		
		if (netEvent != null)
		{
			Player player = netEvent.getSendingPlayer();
			sendMessageLocal(MessageType.SYSTEM, player.getPlayerName() + " has changed the background.");
		}

		for (GameTableCoreListenerIF listener : m_listeners)
			listener.onBackgroundChanged(m_backgroundTypeMapElement, m_backgroundElementType, m_backgroundColor, netEvent);
	}
	
	/**
	 * Set the color of the background
	 * @param color new background color
	 */
	public void setBackgroundColor(BackgroundColor color)
	{
		setBackgroundColor(color, null);
	}
	
	/**
	 * Set the color of the background
	 * @param color new background color
	 * @param netEvent Network event that triggered the change
	 */
	public void setBackgroundColor(BackgroundColor color, NetworkEvent netEvent)
	{
		m_backgroundColor = color;
		m_backgroundTypeMapElement = false;
		
		if (shouldPropagateChanges(netEvent))
			sendBroadcast(NetSetBackground.makePacket(color));

		if (netEvent != null)
		{
			Player player = netEvent.getSendingPlayer();
			sendMessageLocal(MessageType.SYSTEM, player.getPlayerName() + " has changed the background.");
		}
		
		for (GameTableCoreListenerIF listener : m_listeners)
			listener.onBackgroundChanged(m_backgroundTypeMapElement, m_backgroundElementType, m_backgroundColor, netEvent);
	}
	
	/*
	 * @see com.gametable.data.MapElementRepositoryIF#getMapElement(com.gametable.data.MapElementID)
	 */
	@Override
	public MapElement getMapElement(MapElementID id)
	{
		MapElement el = m_publicMap.getMapElement(id);
		if (el == null)
			el = m_privateMap.getMapElement(id);

		return el;
	}

	/**
	 * TODO #Properties Rethink properties 
	 * @return The properties object.
	 */
	public XProperties getProperties()
	{
		return m_properties;
	}

	/**
	 * Application's properties
	 */
	private final XProperties m_properties	= new XProperties();
	
	/**
	 * Initiate a network connection
	 * TODO ! Document
	 */
	public void networkConnect()
	{
		try
		{
			m_hostConnection = m_networkModule.connect();
		}
		catch (IllegalStateException e)
		{
			sendMessageLocal(MessageType.ALERT, e.getMessage());
			return;
		}
		catch (final Exception ex)
		{
			Log.log(Log.SYS, ex);
			sendMessageLocal(MessageType.ALERT, "Failed to connect.");
		}
	}
	
	/**
	 * Responder class to open up functionnality to select interfaces
	 */
	public class NetworkResponderCore 
	{
		/**
		 * Private constructor
		 */
		private NetworkResponderCore()
		{		
		}
		
		/**
		 * Sets the player information for the game
		 * @param players List of players currently connected
		 * @param ourIdx Index of our player information
		 */
		public void setPlayersInformation(final Player[] players, final int ourID)
		{
			// set up the current cast
			clearAllPlayers();
			
			for (int i = 0; i < players.length; i++)
			{
				addPlayer(players[i]);
				if (players[i].getID() == ourID)
					m_player = players[i];
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
			sendMessageBroadcast(MessageType.SYSTEM, player.getPlayerName() + " " + "has joined the session.");
			addPlayer(player);

			sendPlayersInformation();

			send(NetSetGridMode.makePacket(getGridModeID()), player);
			
			MapElementTypeIF type = getBackgroundMapElementType();
			if (type != null)
			{
				send(NetSetBackground.makePacket(type), player);
			}
			else
			{
				send(NetSetBackground.makePacket(getBackgroundColor()), player);
			}

			// Send all existing line segments to the new player
			send(NetAddLineSegments.makePacket(m_publicMap.getLines()), player);

			// Send all current map elements to the new player
			for (MapElement pog : m_publicMap.getMapElements())
				send(NetAddMapElement.makePacket(pog), player);
			
			for (GameTableCoreListenerIF listener : m_listeners)
				listener.onPlayerJoined(player);
			
			// let them know we're done sending them data from the login
			send(NetLoginComplete.makePacket(), player);
		}
	}
	
	private final NetworkResponderCore m_networkResponder = new NetworkResponderCore();
	
	/**
	 * Save properties to disk
	 * Goes through all modules to make sure properties are up to date and saves to disk
	 */
	public void saveProperties()
	{
		updateProperties();

		XProperties props = getProperties();
		props.save();

		for (Module module : getRegisteredModules())
			module.onSavePropertiesCompleted();
	}
	
	/**
	 * 
	 */
	private void updateProperties()
	{
		XProperties props = getProperties();

		props.setTextPropertyValue(PROP_PLAYER_NAME, getPlayerName());
		props.setTextPropertyValue(PROP_CHARACTER_NAME, getCharacterName());
		
		GametableFrame frame = GametableApp.getUserInterface();
		if (frame != null)
		{
			frame.updateProperties();
		}

		for (Module module : getRegisteredModules())
			module.onUpdateProperties(props);
		
		m_networkModule.onUpdateProperties(props);
	}
	
	/**
	 * 
	 */
	private void applyProperties()
	{
		XProperties props = getProperties();
		
		setPlayerInformation(props.getTextPropertyValue(PROP_PLAYER_NAME), props.getTextPropertyValue(PROP_CHARACTER_NAME));

		GametableFrame frame = GametableApp.getUserInterface();
		if (frame != null)
			frame.applyProperties();

		for (Module module : getRegisteredModules())
			module.onApplyProperties(props);
		
		m_networkModule.onApplyProperties(props);
	}
	
	/**
	 * 
	 */
	private void initializeProperties()
	{
		XProperties props = getProperties();
		
		props.addTextProperty(PROP_PLAYER_NAME, getPlayerName(), true, "player", -1, RESOURCE_PATH);
		props.addTextProperty(PROP_CHARACTER_NAME, getCharacterName(), true, "player", -1, RESOURCE_PATH);
		
		GametableFrame frame = GametableApp.getUserInterface();
		if (frame != null)
			frame.initializeProperties();
		
		for (Module module : getRegisteredModules())
			module.onInitializeProperties(props);
		
		m_networkModule.onInitializeProperties(props);
	}
	
	/**
	 * Request a reinitialize and reload of properties
	 */
	public void loadProperties()
	{
		initializeProperties();
		XProperties props = getProperties();
		props.loadProperties();
		
		for (Module module : getRegisteredModules())
			module.onLoadPropertiesCompleted();

		m_networkModule.onLoadPropertiesCompleted();
		
		applyProperties();
	}
	
	/**
	 * Load a map file from file
	 * @param file XML File from which to load the map
	 * @param loadPublic true to load the public map within the file.
	 * @param loadPrivate true to load the private map within the file.
	 * @param netEvent Network event that triggered the load (or null)
	 * 
	 * @throws IOException
	 * @throws MapFormatException
   *
	 */
	public void loadMapFromXML(File file, boolean loadPublic, boolean loadPrivate, NetworkEvent netEvent) throws IOException
	{
		Document doc;
		doc = XMLUtils.parseXMLDocument(file);

		Element root = doc.getDocumentElement();
		if (!root.getTagName().equals("gt"))
			throw new MapFormatException(file);

		if (loadPrivate && loadPublic)
			MapElementID.clear();

		XMLSerializeConverter converter = new XMLSerializeConverter();

		if (loadPublic)
		{
			Element publicEl = XMLUtils.getFirstChildElementByTagName(root, "public_map");
			getMap(GameTableCore.MapType.PUBLIC).deserializeFromXML(publicEl, converter, netEvent);
		}

		if (loadPrivate)
		{
			Element privateEl = XMLUtils.getFirstChildElementByTagName(root, "private_map");
			getMap(GameTableCore.MapType.PRIVATE).deserializeFromXML(privateEl, converter, netEvent);
		}

		if (loadPublic && loadPrivate)
		{
			loadGridFromXML(root, converter, netEvent);
			loadLockedElementsFromXML(root, converter, netEvent);

			// Hook for modules to load data from save file
			Element modulesEl = XMLUtils.getFirstChildElementByTagName(root, "modules");

			if (modulesEl != null)
			{
				for (Module module : getRegisteredModules())
				{
					if (module.canSaveToXML())
					{
						Element moduleEl = XMLUtils.findFirstChildElement(modulesEl, "module", "name", module.getModuleName());

						if (moduleEl != null)
						{
							module.loadFromXML(moduleEl, converter, netEvent);
						}
					}
				}
			}
		}

	}
	
	/**
	 * Load saved grid information from XML file
	 * @param root XML root element
	 * @param converter Converter class
	 * @param netEvent Network event that triggered the load (or null)
	 */
	private void loadGridFromXML(Element root, XMLSerializeConverter converter, NetworkEvent netEvent)
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
				// stick with default if value cannot be parsed			
			}
				
			//, GridModeID.NONE.ordinal()));
			setGridMode(gridMode, netEvent);

			// grid background
			Element bkEl = XMLUtils.getFirstChildElementByTagName(gridEl, "background");
			String typeFQN = bkEl.getAttribute("element_type");
			MapElementTypeIF type = MapElementTypeLibrary.getMasterLibrary().getMapElementType(typeFQN);
			if (type != null)
			{
				setBackgroundMapElementType(type, netEvent);
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

				setBackgroundColor(bkColor, netEvent);
			}
		}
	}
	
	/**
	 * Restore locked element list
	 * 
	 * @param root Root element
	 * @param converter converter helper class
	 * @parma netEvent Network event that triggered the load (or null)
	 */
	private void loadLockedElementsFromXML(Element root, XMLSerializeConverter converter, NetworkEvent netEvent)
	{
		lockAllMapElements(GameTableCore.MapType.ACTIVE, false, netEvent);
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
					lockMapElement(GameTableCore.MapType.ACTIVE, mapEl, true, netEvent);
			}
		}
	}
	
	private final static String PROP_PLAYER_NAME = PROPERTY_BUNDLE + ".player_name";
	private final static String PROP_CHARACTER_NAME = PROPERTY_BUNDLE + ".character_name";
	
	/**
	 * Save the current map to XML file
	 * @param file File where to store the XML
	 * 
	 *  @throws IOException
	 */
	public void saveMapToXML(final File file) throws IOException
	{
		Document doc;
		doc = XMLUtils.createDocument();

		Element root = doc.createElement("gt");
		doc.appendChild(root);

		Element publicEl = doc.createElement("public_map");
		root.appendChild(publicEl);
		getMap(GameTableCore.MapType.PUBLIC).serializeToXML(publicEl);

		Element privateEl = doc.createElement("private_map");
		root.appendChild(privateEl);
		getMap(GameTableCore.MapType.PRIVATE).serializeToXML(privateEl);

		storeLockedElementsToXML(doc, root);
		storeGridToXML(doc, root);

		// Hook for modules to add elements to save file
		Element modulesEl = doc.createElement("modules");

		for (Module module : getRegisteredModules())
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
		XMLOutputProperties props = new XMLOutputProperties();
		props.indentXML = false;
		props.encoding = "UTF-8";
		XMLUtils.saveDocument(file, doc, props);
	}
	

	/**
	 * Store locked element list to XML file
	 * 
	 * @param doc XML Document
	 * @param root Document node in which to store the locked element list
	 */
	private void storeLockedElementsToXML(Document doc, Element root)
	{
		List<MapElement> elements = getLockedMapElements();
		if (elements.size() == 0)
			return;

		Element listEl = doc.createElement("locked");
		for (MapElement el : elements)
			listEl.appendChild(XMLUtils.createElementValue(doc, "id", String.valueOf(el.getID().numeric())));

		root.appendChild(listEl);
	}
	
	/**
	 * Store grid information to XML document
	 * 
	 * @param doc XML document
	 * @param root XML node under which to store grif information
	 */
	private void storeGridToXML(Document doc, Element root)
	{
		// grid
		Element gridEl = doc.createElement("grid");
		gridEl.setAttribute("modeid", String.valueOf(getGridModeID()));
		root.appendChild(gridEl);

		// grid background
		Element bkEl = doc.createElement("background");
		gridEl.appendChild(bkEl);
		
		MapElementTypeIF type = getBackgroundMapElementType();
		if (type != null)
		{
			bkEl.setAttribute("element_type",type.getFullyQualifiedName());
		}
		else
		{
			bkEl.setAttribute("color", getBackgroundColor().name());
		}
	}
	
	/**
	 * Handles uncaught exceptions in the core's thread
	 *
	 * @author Eric Maziade
	 */
	private class CoreUncaughtExceptionHandler implements UncaughtExceptionHandler
	{
		/*
		 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
		 */
		@Override
		public void uncaughtException(Thread t, Throwable e)
		{
			Log.log(Log.SYS, "Uncaught exception!" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static final String PLUGIN_FOLDER = "osu-plugins";
	private static final String PLUGIN_CONFIG_FILE = "osu-plugin.xml";
	
}
