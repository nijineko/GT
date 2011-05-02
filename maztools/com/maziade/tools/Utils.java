/*
 * Utils.java
 * 
 * @created 2004
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Utils
{
	/**
	 * Copies an open file to a given filename
	 *
	 * @param in Opened file to copy
	 * @param sOut Destination file name	 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFile(File in, String sOut) throws FileNotFoundException, IOException
	{
		copyFile(in, sOut, 4096);

	}
	
	/**
	 * Copies an open file to a given stream
	 * TODO make a batch of nio copyFile methods that use FileOutputStream and FileInputStream instead of basic OutputStreams
	 * @param in Opened file to copy
	 * @param sout Destination file	 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFile(File in, File out) throws FileNotFoundException, IOException
	{
		if (!out.exists()) 
			out.createNewFile();
		
		FileChannel source = null;
		FileChannel destination = null;
		
		try 
		{
			source = new FileInputStream(in).getChannel();
		  destination = new FileOutputStream(out).getChannel();
		  destination.transferFrom(source, 0, source.size());
		}
		finally 
		{
		  if (source != null)
		   source.close();
		  
		  if(destination != null)
		   destination.close();
		}
	}

	/**
	 * Copies an open file to a given stream
	 *
	 * @param in Opened file to copy
	 * @param sout Destination stream	 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFile(File in, OutputStream sout) throws FileNotFoundException, IOException
	{
		// ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
		copyFile(in, sout, 4096);

	}

	/**
	 * Copies an open file to a given filename
	 *
	 * @param in Opened file to copy
	 * @param sOut Destination file name	 
	 * @param bufSize buffer size
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static  void copyFile(File in, String sOut, int bufSize) throws FileNotFoundException, IOException
	{
		FileInputStream sin = null;

		try
		{
			sin = new FileInputStream(in);
			copyFile(sin, sOut, bufSize);
		}
		finally
		{
			if (sin != null)
				sin.close();
		}
	}

	/**
	 * Copies an open file to a given stream
	 *
	 * @param in Opened file to copy
	 * @param sout Destination stream	 
	 * @param bufSize buffer size
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static  void copyFile(File in, OutputStream sout, int bufSize) throws FileNotFoundException, IOException
	{
		FileInputStream sin = null;

		try
		{
			sin = new FileInputStream(in);
			copyFile(sin, sout, bufSize);
		}
		finally
		{
			if (sin != null)
				sin.close();
		}
	}

	/**
	 * Copies an open file to a given filename
	 *
	 * @param in input stream
	 * @param sOut Destination file name 	 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFile(InputStream sin, String sOut) throws FileNotFoundException, IOException
	{
		copyFile(sin, sOut, 4096);
	}

	/**
	 * Copies an open file to a given stream
	 *
	 * @param in input stream
	 * @param sout Destination stream 	 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFile(InputStream sin, OutputStream sout) throws FileNotFoundException, IOException
	{
		copyFile(sin, sout, 4096);
	}

	/**
	 * Copies an open file to a given filename
	 *
	 * @param in input stream
	 * @param sOut Destination file name	 
	 * @param bufSize buffer size
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static  void copyFile(InputStream sin, String sOut, int bufSize) throws FileNotFoundException, IOException
	{
		FileOutputStream sout = null;

		File out = new File(sOut);
		if (out.exists())
			out.delete();

		try
		{
			sout = new FileOutputStream(out);
			copyFile(sin, sout, bufSize);
		}
		finally
		{
			if (sout != null)
				sout.close();
		}
	}

	/**
	 * Copies an open file to a given filename
	 *
	 * @param in input stream
	 * @param sOut Destination stream	 
	 * @param bufSize buffer size
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFile(InputStream sin, OutputStream sout, int bufSize) throws FileNotFoundException, IOException
	{
		byte buf[] = new byte[bufSize];
		int iRead;

		do
		{
			iRead = sin.read(buf, 0, bufSize);
			if (iRead > 0)
				sout.write(buf, 0, iRead);

		} while (iRead != -1);
	}
	
	
	/**
	 * Compare two objects and return true if they are equal.
	 * @param o first object to compare
	 * @param o2 second object to compare	 
	 * @return true if objects are equal.	 
	 */
	public static boolean equals(Object o, Object o2)
	{
		if (o == null)
		{
			if (o2 == null)
				return true;

			return false;
		}
		else if (o2 == null)
			return false;

		return (o.equals(o2));
	}
	
	/**
	 * Get the global properties of the tools
	 * @return Properties object
	 */
	public static Properties getProperties()
	{
		if (g_properties == null)
			g_properties = new Properties();

		return g_properties;
	}
	
	/**
	 * Find last occurrence of a char in a string buffer
	 * @param buf string buffer
	 * @param sub char we are looking for
	 * @param startPos position where we start (-1 for end)
	 * @return position
	 */
	static public int lastIndexOf(StringBuffer buf, char sub, int startPos)
	{
		if (startPos < 0 || startPos > buf.length())
			startPos = buf.length() - 1;

		for (int i = startPos; i > -1; i--)
		{
			if (buf.charAt(i) == sub)
				return i;
		}

		return -1;
	}
	
	/**
	 * Returns a string where all occurrences of a substring has	been replaced by a new substring
	 * @param str	Source string (can be null)
	 * @param sub	Substring to look for
	 * @param newSub Substring to replace <i>sub</i> with
	 *
	 * @return String with replaced substrings
	 */
	public static String replaceAll(String str, String sub, String newSub)
	{
		if (str == null)
			return "";

		if (sub.equals(newSub))
			return str;

		StringBuffer buf = new StringBuffer(str);

		int i = str.lastIndexOf(sub);

		while (i > -1)
		{
			buf.replace(i, sub.length() + i, newSub);

			if (i > 0)
				i = str.lastIndexOf(sub, i - 1);
			else
				break;
		}

		return buf.toString();
	}

	/**
	 * Modifies a string so that all occurrences of a substring has	been replaced by a new substring
	 * @param str	Source string buffer 
	 * @param sub	Substring to look for
	 * @param newSub Substring to replace <i>sub</i> with	 
	 */
	public static void replaceAll(StringBuffer str, char sub, String newSub)
	{
		if (str == null)
			return;

		if (newSub.length() == 1 && (newSub.charAt(0) == sub))
			return;

		int i = lastIndexOf(str, sub, -1);

		while (i > -1)
		{
			str.replace(i, i + 1, newSub);

			if (i > 0)
				i = lastIndexOf(str, sub, i - 1);
			else
				break;
		}
	}

	/**
	 * Returns a string where all occurrences of a substring has	been replaced by a new substring
	 * @param str	Source string buffer 
	 * @param sub	Substring to look for
	 * @param newSub Substring to replace <i>sub</i> with	 
	 */
	public static void replaceAll(StringBuffer str, String sub, char newSub)
	{
		if (str == null)
			return;

		String newSubstr = new String(new char[] {newSub});
		if (sub.equals(newSubstr))
			return;

		int i = str.lastIndexOf(sub);

		while (i > -1)
		{
			str.replace(i, sub.length() + i, newSubstr);

			if (i > 0)
				i = str.lastIndexOf(sub, i - 1);
			else
				break;
		}
	}
	
	/**
	 * Returns a string where all occurrences of a substring has	been replaced by a new substring
	 * @param str	Source string buffer 
	 * @param sub	Substring to look for
	 * @param newSub Substring to replace <i>sub</i> with	 
	 */
	public static void replaceAll(StringBuffer str, String sub, String newSub)
	{
		if (str == null)
			return;

		if (sub.equals(newSub))
			return;

		int i = str.lastIndexOf(sub);

		while (i > -1)
		{
			str.replace(i, sub.length() + i, newSub);

			if (i > 0)
				i = str.lastIndexOf(sub, i - 1);
			else
				break;
		}
	}
		
	/**
	 * Saves properties to XML file - sorts by property name first
	 * @param props
	 * @param outFile
	 * @param comment
	 * @param encoding
	 * @throws IOException
	 */
	public static void storePropertiesToXML(Properties props, File outFile, String comment) throws IOException
	{
		Document doc = XMLUtils.createDocument();
		
		Element properties = (Element)doc.appendChild(doc.createElement("properties"));

		if (comment != null)
		{
			Element comments = (Element) properties.appendChild(doc.createElement("comment"));
			comments.appendChild(doc.createTextNode(comment));
		}

		ArrayList<String> keys = new ArrayList<String>(props.stringPropertyNames());
		Collections.sort(keys);
		
		for (String key : keys)
		{
			Element entry = (Element) properties.appendChild(doc.createElement("entry"));
			entry.setAttribute("key", key);
			entry.appendChild(doc.createTextNode(props.getProperty(key)));
		}
		
		XMLUtils.saveDocument(outFile, doc, "UTF-8", "http://java.sun.com/dtd/properties.dtd");
	}
	
	/**
	 * Returns an escaped (URLencoded) string (changes some chars to %xx)
	 * Uses UTF-8
	 * @param in string to escape
	 *
	 * @return escaped string
	 */
	public static String escapeString(String in)
	{
		return escapeString(in, "UTF-8");
	}
	
	/**
	 * Returns an escaped (URLencoded) string (changes some chars to %xx)
	 * @param in string to escape
	 * @param encoding encoding to use, such as UTF-8
	 *
	 * @return escaped string
	 */
	public static String escapeString(String in, String encoding)
	{
		if (in == null)
			return "";

		try
		{
				return URLEncoder.encode(in, encoding);
		}
		catch (UnsupportedEncodingException e)
		{
			// Can't happen.
			return "";
		}
	}
	
	/**
	 * Returns an unescaped (URLDecoded) string (reverts %xx chars to actual chars)
	 * Uses UTF-8 as encoding
	 * 
	 * @param in string to unescape
	 *
	 * @return unescaped string
	 *
	 */
	static public String unEscapeString(String in)
	{
		return unEscapeString(in, "UTF-8");		
	}
	
	/**
	 * Returns an unescaped (URLDecoded) string (reverts %xx chars to actual chars)
	 * @param in string to unescape
	 * @param encoding Such as UTF-8
	 *
	 * @return unescaped string
	 *
	 */
	static public String unEscapeString(String in, String encoding)
	{
		if (in == null)
			return "";

		try
		{
			return URLDecoder.decode(in, encoding);
		}
		catch(UnsupportedEncodingException e)
		{
			// can't happen
			return "";
		}

	}
	
	private static Properties g_properties = null;
}
