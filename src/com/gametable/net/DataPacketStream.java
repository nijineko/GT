/*
 * DataPacket.java
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

import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * Abstract class to help handle data stream in network communications
 *
 * @author Eric Maziade
 */
public abstract class DataPacketStream extends DataOutputStream
{
	/**
	 * Creates a new data output stream to write data to the specified 
   * underlying output stream. The counter <code>written</code> is 
   * set to zero.
   *
   * @param out the underlying output stream, to be saved for later use.
	 */
	protected DataPacketStream(OutputStream outputStream)
	{
		super(outputStream);
	}
	
	public abstract byte[] toByteArray();
}
