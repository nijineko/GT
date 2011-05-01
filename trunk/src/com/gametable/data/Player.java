/*
 * Player.java
 * 
 * @created 2010-06-19
 * 
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General private
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General private License for more details.
 * 
 * You should have received a copy of the GNU General private License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.gametable.data;

import java.util.ArrayList;
import java.util.List;

import com.gametable.net.NetworkConnectionIF;
import com.gametable.net.NetworkEvent;

/**
 * This class holds 'player' information. A player is a participant in a networked session.
 * 
 * @author sephalon
 * 
 * @audited by themaze75
 */
public class Player
{
	/**
	 * The player's character name
	 */
	private String					m_characterName;

	/**
	 * The network connection
	 */
	private  NetworkConnectionIF			m_connection;

	/**
	 * Holds display name
	 */
	private String					m_displayName						= null;

	/**
	 * Whether this player is the player hosting the network session
	 */
	private final boolean 	m_hostPlayer						= false;

	/**
	 * The player's unique ID
	 */
	private int	m_id;

	/**
	 * The player's real name
	 */
	private String		m_playerName;

	/**
	 * Whether this player is currently pointing on the map
	 */
	private boolean					m_pointing;

	/**
	 * The current location on the map where the player is pointing
	 */
	private MapCoordinates	m_pointingLocation			= null;


	/**
	 * Constructor
	 * 
	 * @param playerName Name of the player (cannot be set once changed)
	 * @param characterName Name of the player's character
	 * @param id Unique id
	 * @param isHost true if this player is host
	 */
	public Player(String playerName, String characterName, int id, boolean isHost)
	{
		m_playerName = playerName;
		m_characterName = characterName;
		m_id = id;
	}

	/**
	 * @return The name of this player's character
	 */
	public String getCharacterName()
	{
		return m_characterName;
	}

	/**
	 * @return The network connection.
	 */
	public NetworkConnectionIF getConnection()
	{
		return m_connection;
	}

	/**
	 * @return The player's unique ID
	 */
	public int getID()
	{
		return m_id;
	}

	/**
	 * @return The player's name
	 */
	public String getPlayerName()
	{
		return m_playerName;
	}

	/**
	 * @param The location on the map where the player is currently pointing (or null)
	 */
	public MapCoordinates getPointingLocation()
	{
		return m_pointingLocation;
	}

	/**
	 * Checks if this player's name or character name fits a given name (case insensitive)
	 * 
	 * @param name Name to look for
	 * @return true if matching. False otherwise.
	 */
	public boolean isNamed(final String name)
	{
		if (name.equalsIgnoreCase(m_playerName))
			return true;

		if (name.equalsIgnoreCase(m_characterName))
			return true;

		return false;
	}

	/**
	 * @return True if this player is the game's host
	 */
	public boolean isHostPlayer()
	{
		return m_hostPlayer;
	}

	/**
	 * @return True if this player is currently pointing
	 */
	public boolean isPointing()
	{
		return m_pointing;
	}

	/**
	 * Set this character's name
	 * 
	 * @param name Name of this character
	 */
	public void setCharacterName(String name)
	{
		if (name == null)
			m_characterName = "";
		else
			m_characterName = name;

		m_displayName = null;
	}
	
	/**
	 * Set this character's name
	 * 
	 * @param name Name of this character
	 */
	public void setPlayerName(String name)
	{
		if (name == null)
			m_playerName = "";
		else
			m_playerName = name;

		m_displayName = null;
	}

	/**
	 * Sets the player's connection object
	 * 
	 * @param conn The connection to set.
	 */
	public void setConnection(final NetworkConnectionIF conn)
	{
		m_connection = conn;
	}

	/**
	 * Set this player's ID
	 * 
	 * @param id Id of this player
	 */
	public void setId(final int id)
	{
		m_id = id;
	}
	
	/**
	 * Set this player's status as pointing
	 * 
	 * @param pointing True to mark the player as currently pointing
	 * @param modelPos Location pointed to by the player.  Null for no change.
	 */
	public void setPointing(final boolean pointing, final MapCoordinates modelPos)
	{
		setPointing(pointing, modelPos, null);
	}	

	/**
	 * Set this player's status as pointing
	 * 
	 * @param pointing True to mark the player as currently pointing
	 * @param modelPos Location pointed to by the player.  Null for no change.
	 * @parma netEvent Network event information or null
	 */
	public void setPointing(final boolean pointing, final MapCoordinates modelPos, NetworkEvent netEvent)
	{
		m_pointing = pointing;
		
		if (modelPos != null)
			m_pointingLocation = modelPos;
		
		for (PlayerListenerIF listener : m_listeners)
			listener.onPointingLocationChanged(this, pointing, modelPos, netEvent);
	}	

	/**
	 * Returns string representation of this player
	 */
	@Override
	public String toString()
	{
		if (m_displayName == null)
		{
			final String charName = getCharacterName();
			final String playerName = getPlayerName();

			if ((charName == null) || (charName.length() == 0))
			{
				m_displayName = playerName;
			}
			else if ((playerName == null) || (playerName.length() == 0))
			{
				m_displayName = charName;
			}
			else
			{
				m_displayName = charName + " (" + playerName + ")";
			}
		}

		return m_displayName;
	}
	
	/**
   * Adds a listener to this player
   * @param listener Listener to call when something changes within the player
   */
  public void addPlayerListener(PlayerListenerIF listener)
  {
  	m_listeners.add(listener);
  }
  
  /**
   * Removes a listener from this player
   * @param listener Listener to remove
   * @return True if listener was found and removed
   */
  public boolean removePlayerListener(PlayerListenerIF listener)
  {
  	return m_listeners.remove(listener);
  }
  
  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Player obj)
  {
  	if (obj == null)
  		return false;
  	
  	return getID() == obj.getID();
  }
	
  /**
   * Collection of listeners to this player object
   */
	private final List<PlayerListenerIF> m_listeners = new ArrayList<PlayerListenerIF>();
}
