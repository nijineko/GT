/*
 * RelativePathEntityResolver.java
 * 
 * @created 2006-04-08
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
package com.maziade.tools;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Eric Maziade
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RelativePathEntityResolver implements EntityResolver
{
	
	/**
	 * Constructor
	 * @param relativePath
	 */
	public RelativePathEntityResolver(String relativePath)
	{
		m_path = new File(relativePath).getAbsolutePath();
		if (!m_path.endsWith(File.separator))
			m_path = m_path + File.separator;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
	 */
	public InputSource resolveEntity(String publicId, String systemId)
		throws SAXException, IOException
	{
		URL systemUrl;
		
		/*
		if (systemId.indexOf("://") < 0)
		{						
			File file = new File(m_path + systemId);
			systemUrl = file.toURL();
		}
		else
		*/
		 
		systemUrl = new URL(systemId);
		
		if (systemUrl.getProtocol().equals("file"))
		{
			URL userUrl = new File(System.getProperty("user.dir")).toURI().toURL();
			
			systemId = Utils.unEscapeString(systemUrl.getFile());
			String userId = userUrl.getFile();
	
			if (systemId.startsWith(userId))
			{
				String resPath = m_path + Utils.replaceAll(systemId.substring(userId.length()), "/", File.separator); 
				InputSource is = new InputSource(new FileReader(resPath));
				is.setSystemId(systemId);
				return is;
			}			
		}
		
		return null;	// open using systemID as normal URI
	}
	
	private String m_path = null;

}
