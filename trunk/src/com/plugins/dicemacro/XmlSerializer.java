/*
 * XmlSerializer.java: GameTable is in the Public Domain.
 */

package com.plugins.dicemacro;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.galactanet.gametable.util.UtilityFunctions;

/**
 * A class for serializing xml to a string buffer.
 * 
 * @author iffy
 * 
 *         #GT-AUDIT XmlSerializer
 */
public class XmlSerializer
{
	// --- Members ---------------------------------------------------------------------------------------------------

	boolean				bTagOpen	= false;
	Writer				out;
	List<String>	tagStack	= new LinkedList<String>();

	// --- Constructors ----------------------------------------------------------------------------------------------

	public XmlSerializer()
	{
	}

	// --- Methods ---------------------------------------------------------------------------------------------------

	public void addAttribute(final String name, final String value) throws IOException
	{
		out.write(' ');
		out.write(name);
		out.write("=\"");
		UtilityFunctions.xmlEncode(out, new StringReader(value));
		out.write('"');
	}

	public void addAttributes(final Map<String, String> attributes) throws IOException
	{
		for (Entry<String, String> entry : attributes.entrySet())
		{
			addAttribute(entry.getKey(), entry.getValue());
		}
	}

	public void addText(final String text) throws IOException
	{
		checkTagClose();
		UtilityFunctions.xmlEncode(out, new StringReader(text));
	}

	private void checkTagClose() throws IOException
	{
		if (bTagOpen)
		{
			out.write('>');
			bTagOpen = false;
		}
	}

	public void endDocument() throws IOException
	{
		out.flush();
	}

	public void endElement() throws IOException
	{
		if (bTagOpen)
		{
			out.write("/>");
			popTag();
		}
		else
		{
			out.write("</");
			out.write(popTag());
			out.write('>');
		}
		bTagOpen = false;
	}

	private String popTag()
	{
		return tagStack.remove(0);
	}

	/* --- Private Methods ------------------------------------------------- */

	private void pushTag(final String name)
	{
		tagStack.add(0, name);
	}

	public void startDocument(File file) throws IOException
	{
		out = new OutputStreamWriter(new FileOutputStream (file), "UTF-8");
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		tagStack.clear();
	}

	public void startElement(final String name) throws IOException
	{
		checkTagClose();
		out.write('<');
		out.write(name);
		pushTag(name);
		bTagOpen = true;
	}

}
