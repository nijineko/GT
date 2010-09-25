/*
 * NetworkMessage.java
 *
 * @created 2010-08-30
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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Defines a network message.  Network messages should be registered with the current network implementation in order to be used.
 *
 * @author Eric Maziade
 */
public interface NetworkMessageTypeIF
{
	/**
	 * The framework will assign an ID to this network message definition
	 * @param id numeric ID
	 */
	public void setID(int id);
	
	/**
	 * @return the numeric ID, as assigned by the framework
	 */	
	public int getID();
	
	/**
	 * Gets the string representation of the networking message.  The framework converts this to an ID.
	 * It is assumed that the returned string is interned (as per String.intern()) 
	 * @return
	 */
	public String getName();
	
	/**
	 * Read and process a received packet
	 * @param connectionSource Network connection where the package originated from
	 * @param dis Data input stream from which to read the data packet
	 * @param event Network event information
	 * @throws IOException
	 */
	public void processData(NetworkConnectionIF connectionSource, DataInputStream dis, NetworkEvent event) throws IOException;	
}
