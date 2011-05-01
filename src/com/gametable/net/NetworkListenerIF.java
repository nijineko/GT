/*
 * NetworkListenerIF.java
 *
 * @created 2010-09-06
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

package com.gametable.net;

/**
 * Networking listener interface
 *
 * @author Eric Maziade
 */
public interface NetworkListenerIF
{
	/**
	 * The connection has been established through the module
	 * @param conn Network connection object
	 */
	public void connectionEstablished(NetworkConnectionIF conn);
	
	/**
	 * The connection to the network has ended 
	 */
	public void connectionEnded();
	
	/**
	 * A network connection has been dropped
	 * @param conn Network connection object
	 */
	public void connectionDropped(NetworkConnectionIF conn);
	
	/**
	 * The network status has just been changed
	 * @param status New network status
	 */
	public void networkStatusChange(NetworkStatus status);

}
