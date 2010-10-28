/*
 * MapFormatException.java
 *
 * @created 2010-10-25
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

package com.galactanet.gametable.data;

import java.io.File;
import java.io.IOException;

/**
 * Thrown when the XML file format is non-compliant 
 *
 * @author Eric Maziade
 */
public class MapFormatException extends IOException
{
	/**
	 * Constructor
	 * @param source source file of invalid format
	 */
	public MapFormatException(File source)
	{
		m_file = source;
	}
	
	/**
	 * @return Source file
	 */
	public File getSourceFile()
	{
		return m_file;
	}
	
	private final File m_file;

}
