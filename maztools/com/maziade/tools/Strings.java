/*
 * Strings.java
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

public class Strings
{
	/**
	 * Returns an empty string if the given string is null
	 * @param str string to verify
	 *
	 * @return the str or an empty string if str is null
	 */
	public final static  String antiNull(String str)
	{
		return antiNull(str, "");
	}
	
	/**
	 * Convenience method
	 * Returns a specified string if the given string is null
	 * @param str string to verify
	 * @param str2 string to return if str is null
	 *
	 * @return the str or str2 (if str is null)
	 */
	public final static String antiNull(String str, String str2)
	{
		if (str == null)
			return str2;

		return str;
	}
	
	/**
	 * Compare two strings and return true if they are equal.  Case-sensitive.
	 * @param str first string to compare
	 * @param str2 second string to compare
	 *
	 * @return true if strings are equal.
	 *
	 */
	public static  boolean areStringsEqual(String str, String str2)
	{
		if (str == null)
		{
			if (str2 == null)
				return true;

			return false;
		}
		else if (str2 == null)
			return false;

		return (str.equals(str2));
	}

	/**
	 * Compare two strings and return true if they are equal.  Case-insensitive.
	 * @param str first string to compare
	 * @param str2 second string to compare
	 *
	 * @return true if strings are equal.
	 *
	 */
	public static  boolean areStringsEqualIgnoreCase(String str, String str2)
	{
		if (str == null)
		{
			if (str2 == null)
				return true;

			return false;
		}
		else if (str2 == null)
			return false;

		return (str.compareToIgnoreCase(str2) == 0);
	}

	/**
	 * Compare two objects and return less, above or equal to zero.
	 * A null object comes before a non-null object
	 * @param o first object to compare
	 * @param o2 second object to compare	 
	 * @return -1,0,1
	 */
	public static int compareTo(String o, String o2)
	{
		if (o == null)
		{
			if (o2 == null)
				return 0;

			return -1;
		}
		else if (o2 == null)
			return 1;

		return o.compareTo(o2);
	}
	
	/**
	 * Compare with ignore case two strings and return less, above or equal to zero.
	 * A null String comes before a non-null String
	 * @param o first object to compare
	 * @param o2 second object to compare	 
	 * @return -1,0,1
	 */
	public static int compareToIgnoreCase(String o, String o2)
	{
		if (o == null)
		{
			if (o2 == null)
				return 0;

			return -1;
		}
		else if (o2 == null)
			return 1;

		
		return o.compareToIgnoreCase(o2);
	}
	
	/**
	 * Verifies whether is string is empty.
	 * It is empty if it is null or contains only white spaces
	 * @param s string to check
	 * @return true if empty
	 */
	public static boolean isEmpty(String s)
	{
		if (s == null)
			return true;

		return (s.trim().equals(""));
	}	

	/**
	 * Returns a left-padded string
	 * @param str String to pad
	 * @param targetSize Requested end-result string
	 * @param pad String to use to pad
	 *
	 * @return a string as close to the target size as possible
	 */
	public static  String padLeft(int str, int targetSize, String pad)
	{
		return padLeft(String.valueOf(str), targetSize, pad);
	}

	/**
	 * Returns a left-padded string
	 * @param str String to pad
	 * @param targetSize Requested end-result string
	 * @param pad String to use to pad
	 *
	 * @return a string as close to the target size as possible
	 */
	public static  String padLeft(String str, int targetSize, String pad)
	{
		String res = str;

		while (res.length() < targetSize)
			res = pad + res;

		return res;
	}

	/**
	 * Returns a right-padded string
	 * @param str String to pad
	 * @param targetSize Requested end-result string
	 * @param pad String to use to pad
	 *
	 * @return a string as close to the target size as possible
	 */
	public static  String padRight(String str, int targetSize, String pad)
	{
		String res = str;

		while (res.length() < targetSize)
			res += pad;

		return res;
	}

}
