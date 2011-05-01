/*
 * NetworkAdapter.java
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
 * NetworkListenerIF utility class that implements all methods of a listener
 *
 * @author Eric Maziade
 */
public class NetworkAdapter implements NetworkListenerIF
{

	/*
	 * @see com.gametable.data.net.NetworkListenerIF#connectionEstablished(com.gametable.data.net.Connection)
	 */
	@Override
	public void connectionEstablished(NetworkConnectionIF conn)
	{
	}

	/*
	 * @see com.gametable.data.net.NetworkListenerIF#connectionEnded()
	 */
	@Override
	public void connectionEnded()
	{
	}
	
	/*
	 * @see com.gametable.data.net.NetworkListenerIF#connectionDropped(com.gametable.data.net.Connection)
	 */
	@Override
	public void connectionDropped(NetworkConnectionIF conn)
	{
	}
	
	/*
	 * @see com.gametable.data.net.NetworkListenerIF#networkStatusChange(com.gametable.data.net.NetworkStatus)
	 */
	@Override
	public void networkStatusChange(NetworkStatus status)
	{
	}
}
