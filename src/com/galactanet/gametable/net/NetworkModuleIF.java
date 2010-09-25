/*
 * NetworkIF.java
 *
 * @created 2010-09-05
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

package com.galactanet.gametable.net;

import java.io.IOException;

/**
 * Network Module specific interface
 *
 * @author Eric Maziade
 */
public interface NetworkModuleIF
{
	/**
	 * Connect to the network.
	 * Network parameters should have been set either directly through the implementation module or
	 * by the user through the parameters panel
	 *  
	 * @return Connection object (if joining, null if hosting)
	 * 
	 * @throws IOException
	 * @throws IllegalStateException If we are already connected to a game (either as host or player)
	 */
	public NetworkConnectionIF connect() throws IOException, IllegalStateException;
	
	/**
	 * Disconnect from network
	 */
	public void disconnect();
	
	/**
	 * Get the network status from the network module
	 * @return network status
	 */
	public NetworkStatus getNetworkStatus();
	
	/**
   * Register a message in the network module
   * @param message Message to register
   */
	public void registerMessageType(NetworkMessageTypeIF message);
	
	/**
	 * Get the network connection password
	 * @return
	 */
	public String getPassword();
	
	/**
	 * Broadcasts a packet through the network connection.  Everybody but the sending party receives the message.
	 * @param packet Data packet
	 */
	public void sendBroadcast(byte[] packet);
	
	/**
	 * Sends a packet through the specific network connection 
	 * @param packet Data packet
	 * @param connection Network connection
	 */
	public void send(byte[] packet, NetworkConnectionIF connection);
	
	/**
	 * Gets a network parameters panel that can be added to the "join" dialog 
	 * @return NetworkParametersPanel instance
	 */
	public NetworkParametersPanel getParametersPanel();
	
	/**
	 * Adds a listener to the network module
	 * @param listener Listener to add to the module
	 */
	public void addListener(NetworkListenerIF listener);
	
	/**
	 * Removes a listener from the network module
	 * @param listener listener to remove
	 * @return true if the listener was found and removed
	 */
	public boolean removeListener(NetworkListenerIF listener);
	
	/**
   * Get network message name by reading from data stream.  Used for logging and debugging purposes.
   * @param packet
   * @return Message name or "ERROR"
   */
	public String getNetworkMessageName(final byte[] packet);
	
	/**
	 * Create a DataPacketStream instance 
	 * @param messageType Message type to help build the header
	 * @return DataPacketStream instance to populate
	 * 
	 * @throws IOException If an error occurs
	 */
	public DataPacketStream createDataPacketStream(NetworkMessageTypeIF messageType) throws IOException;
}
