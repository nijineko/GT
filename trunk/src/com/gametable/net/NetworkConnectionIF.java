/*
 * Connection.java: GameTable is in the Public Domain.
 */

package com.gametable.net;

/**
 * Represents a user connection to the network
 */
public interface NetworkConnectionIF
{
	/**
	 * Closes this connection.
	 */
	public abstract void close();

	/**
	 * Mark this user-connection as 'logged in' (one step further from 'connected')
	 */
	public abstract void markLoggedIn();

	/**
	 * Waits until the connection is established to the network
	 */
	public abstract void waitForConnection();
	
	/**
	 * Send a network packet over that particular connection (to the linked user)
	 * @param packet Packet data to send.
	 */
	public void sendPacket(final byte[] packet);
	
	/**
	 * Convenience method to facilitate acquiring the network module
	 * @return
	 */
	public NetworkModuleIF getNetworkModule();
}
