/*
 * XPropertiesException.java
 * 
 * @created 2006
 * 
 * Copyright (C) 1999-2011 Eric Maziade
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.maziade.props;

import java.io.File;

/**
 * @author Eric Maziade
 * 
 *         Describes a generic exception used by properties
 * 
 */
public class XPropertiesException extends RuntimeException
{
	private static final long	serialVersionUID	= 5629472525819417695L;

	/**
	 * Stored backup of corrupted settings file
	 */
	private final File	m_backupFile;

	/**
	 * Corrupted settings file
	 */
	private final File	m_settingsFile;

	/**
	 * Constructor
	 * 
	 * @param source
	 * @param settings
	 * @param backup
	 */
	public XPropertiesException(Throwable cause, java.io.File settings, File backup)
	{
		super(XProperties.getResourceString("XPropertiesException.msg") + "\n" + XProperties.getResourceString("XPropertiesException.backup") + "\n"
				+ backup.getAbsolutePath(), cause);

		m_settingsFile = settings;
		m_backupFile = backup;
	}

	/**
	 * @return file where the corrupted settings file was stored
	 */
	public File getBackupFile()
	{
		return m_backupFile;
	}

	/**
	 * @return settings file that was corrupted
	 */
	public File getSettingsFile()
	{
		return m_settingsFile;
	}
}