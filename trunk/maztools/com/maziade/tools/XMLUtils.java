/*
 * XMLUtils.java
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

/**
 * @author Eric Maziade
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML Utilities
 * @author Eric Maziade
 *
 */
public class XMLUtils
{
	public static final String SYSTEM_PROP_XML_FACTORY = "com.maziade.tools.XMLUtils.XMLFactory";
	
	/**
	 * Gets the node value from the given node's first child, if possible.
	 * @param el element to get value from
	 * @return value or null if there's nothing
	 */
	public static String getElementValue(Element el)
	{
		return getNodeValue(el);
	}
	
	/**
	 * Create a new Element node with specified text value
	 * @param doc Parent document 
	 * @param tagName Name to set for new element
	 * @param data String data to set within new element
	 * @return New element instance
	 */
	public static Element createElementValue(Document doc, String tagName, String data)
	{
		Element el = doc.createElement(tagName);
		el.appendChild(doc.createTextNode(data));
		return el;
	}
	
	/**
	 * Gets the node value from the given node's first child, if possible.
	 * @param el element to get value from
	 * @return value or null if there's nothing
	 */
	public static String getNodeValue(Node el)
	{
		if (el == null)
			return null;
		
		Node node = el.getFirstChild();
		if (node == null)
			return null;
		
		return node.getNodeValue();
	}
	
	/**
	 * Look for the first child node with specified node name and return its node value. 
   *
	 * @param parent element to look into
	 * @param nodeName name of the node to look for
	 * @param defaultValue value to return if the node has no value 
	 * @return value
	 */
	public static String getFirstChildNodeValue(Element parent, String nodeName, String defaultValue)
	{
		return getFirstChildNodeValue(parent, nodeName, defaultValue, defaultValue);
	}
	
	/**
	 * Look for the first child node with specified node name and return its node value. 
   *
	 * @param parent element to look into
	 * @param nodeName name of the node to look for
	 * @param defaultValue value to return if the node has no value
	 * @param notFoundValue value to return if the node is not found 
	 * @return value
	 */
	public static String getFirstChildNodeValue(Element parent, String nodeName, String defaultValue, String notFoundValue)
	{
		Element el = getFirstChildElementByTagName(parent, nodeName);
		
		if (el != null)
		{
			String val = XMLUtils.getElementValue(el);
			if (val == null)
				return defaultValue;
				
			return val;
		}
		
		return notFoundValue;
	}
	
	/**
	 * Get the root node matching specified tag name
	 * @param doc document
	 * @return root element or null, if not found
	 */
	public static Element getRootElement(Document doc)	
	{
		return getRootElement(doc, null);
	}
	
	/**
	 * Get the document root and validates name
	 * @param doc document
	 * @param nodeName tagName name of root element, will return null if root does not match
	 * @return root element or null, if not found
	 */
	public final static Element getRootElement(Document doc, String nodeName)
	{
		Element root = doc.getDocumentElement();
		if (nodeName != null && !Utils.equals(root.getNodeName(), nodeName))
				return null;
		
		return root;
	}
	
	/**
	 * Finds the first child sub element of a given node (or null if none)
	 * @param node The parent node to search
	 * @param tagName The name of the tag to look for
	 * @return The resulting child element (or null)
	 */
	public static Element getFirstChildElementByTagName(Element node, String tagName)
	{
		NodeList list = node.getElementsByTagName(tagName);
		int max = list.getLength();

		for (int i = 0; i < max; i++)
		{
			Element subNode = (Element)list.item(i);
			if (subNode.getParentNode() == node)
				return subNode;
		}

		return null;
	}
	
	/**
	 * Finds the first child sub element of a given node and return its content -
	 * assumes it contains no other children
	 * @param node The parent node to search
	 * @param tagName The name of the tag to look for
	 * @return The resulting child element (or null)
	 */
	public static String getFirstChildElementContent(Element node, String tagName)
	{
		return getFirstChildElementContent(node, tagName, null);
	}
	
	/**
	 * Finds the first child sub element of a given node and return its content -
	 * assumes it contains no other children
	 * @param node The parent node to search
	 * @param tagName The name of the tag to look for
	 * @param defaultValue The value to return if child element is not found
	 * @return The resulting child element (or defaultValue)
	 */
	public static String getFirstChildElementContent(Element node, String tagName, String defaultValue)
	{
		Element el = getFirstChildElementByTagName(node, tagName);
		if (el == null)
			return null;
		
		Node sub = el.getFirstChild();
		while (sub != null)
		{
			if (sub.getNodeType() == Node.TEXT_NODE)
				return ((Text)sub).getData();
			
			sub = sub.getNextSibling();
		}
		
		return defaultValue;
	}
	
	/**
	 * Finds the first child element that matches a given description
	 * @param node root node to look in
	 * @param tagName name of the tag to look for
	 * @param attributeName name of the attribute to query
	 * @param attributeValue value the attribute should have
	 * @return Element or null
	 */
	public static Element findFirstChildElement(Element node, String tagName, String attributeName, String attributeValue)
	{
		Node child = node.getFirstChild();
		while(child != null)
		{
			if (child.getNodeType() == Node.ELEMENT_NODE && Strings.areStringsEqual(child.getNodeName(), tagName))
			{
				if (Strings.areStringsEqual(((Element)child).getAttribute(attributeName), attributeValue))
					return (Element)child;
			}
			
			child = child.getNextSibling();
		}

		return null;
	}
	
	/**
	 * Finds the first child element that matches a given description
	 * @param node root node to look in
	 * @param tagName name of the tag to look for
	 * @param attributeName name of the attribute to query
	 * @param attributeValue value the attribute should have
	 * @return list of elements (can be empty) 
	 */ 
	public static List<Element> findChildElements(Element node, String tagName, String attributeName, String attributeValue)
	{
		List<Element> elements = new ArrayList<Element>();
		
		Node child = node.getFirstChild();
		while(child != null)
		{
			if (child.getNodeType() == Node.ELEMENT_NODE && Strings.areStringsEqual(child.getNodeName(), tagName))
			{
				if (Strings.areStringsEqual(((Element)child).getAttribute(attributeName), attributeValue))
					elements.add((Element)child);
			}
			
			child = child.getNextSibling();
		}

		return elements;
	}
	
	/**
	 * Finds the first child element that matches a given description
	 * @param node root node to look in
	 * @param tagName name of the tag to look for
	 * @param filter user-defined filter for element finding 
	 * @return first found element or null  
	 */ 
	public static Element findFirstChildElement(Element node, String tagName, FindElementFilterIF filter)
	{
		Node child = node.getFirstChild();
		while(child != null)
		{
			if (child.getNodeType() == Node.ELEMENT_NODE && Strings.areStringsEqual(child.getNodeName(), tagName))
			{
				Element element = (Element)child;
				if (filter.elementMatches(element))
					return element;
			}
			
			child = child.getNextSibling();
		}

		return null;
	}
	
	/**
	 * Finds the first child element that matches a given description
	 * @param node root node to look in
	 * @param tagName name of the tag to look for
	 * @param filter user-defined filter for element finding 
	 * @return list of elements (can be empty) 
	 */ 
	public static List<Element> findChildElements(Element node, String tagName, FindElementFilterIF filter)
	{
		List<Element> elements = new ArrayList<Element>();
		
		Node child = node.getFirstChild();
		while(child != null)
		{
			if (child.getNodeType() == Node.ELEMENT_NODE && Strings.areStringsEqual(child.getNodeName(), tagName))
			{
				Element element = (Element)child;
				if (filter.elementMatches(element))				
					elements.add(element);
			}
			
			child = child.getNextSibling();
		}

		return elements;
	}

	
		/**
	 * Remove all of a given node's children matching a name
	 * @param node node to remove children from
	 * @param name name of the children node to remove
	 */
	public static void removeNodeChildren(Node node, String name)
	{
		Node child = node.getFirstChild();
		while (child != null)
		{	
			Node old = child;
			child = child.getNextSibling();			
			
			if (Strings.areStringsEqual(old.getNodeName(), name))
				node.removeChild(old);
		}		 
	}
	
	
	/**
	 * Remove all of a given node's children
	 * @param node node to remove children from
	 */
	public static void removeNodeChildren(Node node)
	{
		Node child = node.getFirstChild();
		while (child != null)
		{	
			Node old = child;
			child = child.getNextSibling();
			node.removeChild(old);
		}		 
	}
	
	/**
	 * Looks through children of 'root' for elements named 'tagName'
	 * @param root root element to search in
	 * @param tagName name of the elements to look for
	 * @return List of elements (never null)
	 */
	static public List<Element> getChildElementsByTagName(Element root, String tagName)
	{
		if (root == null)
			return Collections.emptyList();
		
		List<Element> result = new ArrayList<Element>();
		Node cur = root.getFirstChild();
		while (cur != null)
		{
			if (cur.getNodeType() == Node.ELEMENT_NODE)
			{
				if (Strings.areStringsEqual(cur.getNodeName(), tagName))
					result.add((Element)cur);
			}
			
			cur = cur.getNextSibling();
		}
		
		return result;
	}

	/**
	 * Gets a context parameter value from a document source
	 * @param src Document soure (eg parsed web.xml)   *
	 * @param paramName Name of the parameter to look for
	 * @param sDefault String to return if null [optional: default is null]
	 * @return Parameter value or null
	 */
	static public String getContextParameterValue(Document src, String paramName, String sDefault)
	{
		String  value = null;
		boolean found = false;

		Element param;
		NodeList params = src.getElementsByTagName("context-param");

		for (int i = 0; i < params.getLength(); i++)
		{
			value = null;
			param = (Element)params.item(i);

			if (param != null)
			{
				Element node;
				NodeList nl = param.getElementsByTagName("*");

				for (int j = 0; j < nl.getLength(); j++)
				{
					node = (Element)nl.item(j);

					if (node != null)
					{
						if (Strings.areStringsEqual(node.getNodeName(), "param-name"))
						{
							Node txt = node.getFirstChild();

							if ((txt != null) && (txt.getNodeType() == Node.TEXT_NODE))
							{
								if (Strings.areStringsEqual(((Text)txt).getData(), paramName))
									found = true;
							}
						}
						else if (Strings.areStringsEqual(node.getNodeName(), "param-value"))
						{
							Node txt = node.getFirstChild();

							if ((txt != null) && (txt.getNodeType() == Node.TEXT_NODE))
							{
								value = Strings.antiNull(((Text)txt).getData());
							}
						}
					}
				}

				if (found)
					return value;
			}
		}

		return sDefault;
	}

	static public String getContextParameterValue(Document src, String paramName)
		{ return getContextParameterValue(src, paramName, null); }
		

	/**
	 * Creates an empty XML document
	 * @return empty XML document
	 * @throws IOException
	 */
	static public Document createDocument() throws IOException
	{
		try
		{
			DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = fact.newDocumentBuilder();
			Document doc = builder.newDocument(); 

			return doc;
		}
		catch (ParserConfigurationException ex)
		{
			IOException ex2 = new IOException("Failed to create XML parser");
			ex2.initCause(ex);
			throw ex2;
		}		
	}
	
	/**
	 * Verifies if a specified XML file is already in the cache
	 * @param file file to verify
	 * @return true if in cache, false if not
	 */
	static public boolean isXMLDocumentCached(File file)
	{		
		try
		{
			long stamp = file.lastModified();
			String id = file.getCanonicalPath(); 
			
			// 1 - Look for document in cache
			CachedXML xml = m_xmlCache.get(id);
			
			// 2 - If found verify expiration
			if (xml == null)
				return false;
			
			if (xml.stamp != stamp)
				return false;
			
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
	
	/**
	 * Remove a cached XML file from cache
	 * @param file file to remove from cache
	 * @return true if the file was found and removed from cache
	 */
	static public boolean removeXMLFromCache(File file)
	{
		try
		{			
			String id = file.getCanonicalPath(); 
			
			// 1 - Look for document in cache
			CachedXML xml = m_xmlCache.remove(id);
			
			// 2 - If found verify expiration
			if (xml == null)
				return false;		
			
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
	
	/**
	 * Parses an XML document from a given file name.  Stores the resulting document
	 * in cache - will reparse only if the file date has changed. 
	 * @param file File to parse
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document getCachedXMLDocument(File file) throws IOException
	{
		long stamp = file.lastModified();
		String id = file.getCanonicalPath(); 
		
		// 1 - Look for document in cache
		CachedXML xml = m_xmlCache.get(id);
		
		// 2 - If found and not expired, return it
		if (xml != null)
		{
			if (xml.stamp == stamp)
			{				
				return xml.doc;
			}
		}
		else		
			xml = new CachedXML();
		
		// 3 - Parse it
		xml.doc = parseXMLDocument(file);
		xml.stamp = stamp;
		
		// 4 - cache it
		m_xmlCache.put(id, xml);
		
		// 5 - return doc
		return xml.doc;		
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param file File to parse
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(File file) throws IOException
	{
		return parseXMLDocument(file, null);
	}

	/**
	 * Parses an XML document from a given file name
	 * @param file File to parse
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(File file, XMLInputProperties properties) throws IOException
	{		
		try
		{
			DocumentBuilderFactory fact;
			
			Properties props = Utils.getProperties();
			String className = props.getProperty(XMLUtils.SYSTEM_PROP_XML_FACTORY);
			
			if (className == null)
				fact = DocumentBuilderFactory.newInstance();
			else
			{
				try
				{
					Class<?> factClass = Class.forName(className);
					fact = (DocumentBuilderFactory)factClass.newInstance();
				}
				catch (Throwable e)
				{
					IOException ex2 = new IOException("Failed to create XML parser");
					ex2.initCause(e);
					throw ex2;					
				}				
			}			
			 
			applyProperties(fact, properties);
			
			DocumentBuilder builder = fact.newDocumentBuilder();
			
			
			try
			{
				//InputStream in = new FileInputStream(file);
				//InputSource is = new InputSource(in);			
										
				//builder.setEntityResolver(new RelativePathEntityResolver(Utils.extractPath(file)));
				Document doc = builder.parse(file);
				
				//in.close();
				
				return doc;
			}
			finally
			{
				//in.close();
			}
		}
		catch (ParserConfigurationException ex)
		{
			IOException ex2 = new IOException("Failed to create XML parser");
			ex2.initCause(ex);
			throw ex2;
		}
		catch (IOException ex)
		{
			throw ex;
		}
		catch (SAXException ex)
		{
			IOException ex2 = new IOException("Parse exception");
			ex2.initCause(ex);
			throw ex2;
		}
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param in  source to read from
	 * @param systemRelativePath system relative path to use to resolve entities. can be null.
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(InputStream in, String systemRelativePath) throws IOException
	{
		return parseXMLDocument(new InputSource(in), systemRelativePath);		
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param in  source to read from
	 * @param systemRelativePath system relative path to use to resolve entities. can be null.
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(InputStream in, String systemRelativePath, XMLInputProperties properties) throws IOException
	{
		return parseXMLDocument(new InputSource(in), systemRelativePath, properties);		
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param in  source to read from
	 * @param systemRelativePath system relative path to use to resolve entities. can be null.
	 * @param encoding encoding to use for input stream
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(InputStream in, String systemRelativePath, String encoding) throws IOException
	{
		InputSource is = new InputSource(in);
		is.setEncoding(encoding);
		return parseXMLDocument(is, systemRelativePath);		
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param in  source to read from
	 * @param systemRelativePath system relative path to use to resolve entities. can be null.
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(Reader in, String systemRelativePath) throws IOException
	{
		return parseXMLDocument(new InputSource(in), systemRelativePath);		
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param in  source to read from
	 * @param systemRelativePath system relative path to use to resolve entities. can be null.
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(Reader in, String systemRelativePath, XMLInputProperties properties) throws IOException
	{
		return parseXMLDocument(new InputSource(in), systemRelativePath, properties);		
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param in  source to read from
	 * @param systemRelativePath system relative path to use to resolve entities. can be null.
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(InputSource is, String systemRelativePath) throws IOException
	{
		return parseXMLDocument(is, systemRelativePath, null);
	}
	
	/**
	 * Parses an XML document from a given file name
	 * @param in  source to read from
	 * @param systemRelativePath system relative path to use to resolve entities. can be null.
	 * @return Resulting XML document
	 * @throws IOException if an error happens during parsing
	 */
	static public Document parseXMLDocument(InputSource is, String systemRelativePath, XMLInputProperties properties) throws IOException
	{
		try
		{
			DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
			
			applyProperties(fact, properties);
			
			DocumentBuilder builder = fact.newDocumentBuilder();
			
			if (systemRelativePath != null)
			{
				is.setSystemId(systemRelativePath);
				builder.setEntityResolver(new RelativePathEntityResolver(systemRelativePath));
			}
			
						
			Document doc = builder.parse(is);

			return doc;
		}
		catch (ParserConfigurationException ex)
		{
			IOException ex2 = new IOException("Failed to create XML parser");
			ex2.initCause(ex);
			throw ex2;
		}
		catch (IOException ex)
		{
			throw ex;
		}
		catch (SAXException ex)
		{
			IOException ex2 = new IOException("Parse exception");
			ex2.initCause(ex);
			throw ex2;
		}
	}

	private static void applyProperties(DocumentBuilderFactory fact, XMLInputProperties properties)
			throws ParserConfigurationException
	{
		if (properties != null)
		{
			if (!properties.validateXML)
			{
				fact.setValidating(false);
				fact.setFeature("http://xml.org/sax/features/namespaces", false);
				fact.setFeature("http://xml.org/sax/features/validation", false);
				fact.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
				fact.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			}
			
			if (properties.ignoreComments)
				fact.setIgnoringComments(true);
		}
	}
	
	/**
	 * Saves a document to file
	 * @param fileName Target fileName
	 * @param doc Source document
	 * @param encoding encoding (null for default - optional)
	 * @throws IOException Thrown if an error occurred
	 */
	static public void saveDocument(String fileName, Node doc, String encoding) throws IOException
	{
		XMLOutputProperties props = new XMLOutputProperties();
		props.encoding = encoding;
		
		saveDocument(fileName, doc, props);
	}
	
	/**
	 * Saves a document to file
	 * @param fileName Target file
	 * @param doc Source document
	 * @throws IOException Thrown if an error occurred
	 */
	static public void saveDocument(String fileName, Node doc) throws IOException
	{ 
		XMLOutputProperties props = new XMLOutputProperties();
		
		saveDocument(fileName, doc, props);
	}
	
	/**
	 * Saves a document to file
	 * @param fileName Target file
	 * @param doc Source document
	 * @param props XML output properties
	 * @throws IOException Thrown if an error occurred 
	 */
	static public void saveDocument(String fileName, Node doc, XMLOutputProperties props) throws IOException
	{
		Writer fw = null;

		try
		{
			if (props.encoding == null)
			{
				fw = new FileWriter(fileName);
			}
			else
			{
				FileOutputStream out1 = new FileOutputStream(fileName);
				OutputStreamWriter out2 = new OutputStreamWriter(out1, props.encoding);
				
				fw = out2;
			}
			
			saveDocument(fw, doc, props);
		}
		finally
		{
			if (fw != null)
				fw.close();
		}
	}
	
	/**
	 * Saves a document to file
	 * @param file Target file
	 * @param doc Source document
	 * @param encoding encoding (null for default - optional)
	 * @throws IOException Thrown if an error occurred 
	 */
	static public void saveDocument(File file, Node doc, String encoding) throws IOException
	{ 
		XMLOutputProperties props = new XMLOutputProperties();
		props.encoding = encoding;
		
		saveDocument(file, doc, props);
	}
	
	/**
	 * Saves a document to file
	 * @param file Target file
	 * @param doc Source document
	 * @param encoding encoding (null for default - optional)
	 * @param dtdURI URI to DTD definition (null to omit)
	 * @throws IOException Thrown if an error occurred 
	 */
	static public void saveDocument(File file, Node doc, String encoding, String dtdURI) throws IOException
	{
		XMLOutputProperties props = new XMLOutputProperties();
		props.encoding = encoding;
		props.dtdURI = dtdURI;
		
		saveDocument(file, doc, props);
	}
	
	/**
	 * Saves a document to file
	 * @param file Target file
	 * @param doc Source document
	 * @throws IOException Thrown if an error occurred 
	 */
	static public void saveDocument(File file, Node doc) throws IOException
	{
		XMLOutputProperties props = new XMLOutputProperties();
		
		saveDocument(file, doc, props); 
	}
	
	/**
	 * Saves a document to file
	 * @param file Target file
	 * @param doc Source document
	 * @param props XML output properties
	 * 
	 * @throws IOException Thrown if an error occurred 
	 */
	static public void saveDocument(File file, Node doc, XMLOutputProperties props) throws IOException
	{
		Writer w = null;

		try
		{
			if (!Strings.isEmpty(props.encoding))
			{
				FileOutputStream fos = new FileOutputStream(file);
				w = new OutputStreamWriter(fos, props.encoding);
			}
			else
				w = new FileWriter(file);
			
			saveDocument(w, doc, props);
		}
		finally
		{
			if (w != null)
				w.close();
		}
	}
	
	/**
	 * Saves a document to writer
	 * @param out Target writer
	 * @param doc Source document
	 * @throws IOException
	 */
	static public void saveDocument(Writer out, Node doc) throws IOException
	{ 
		XMLOutputProperties props = new XMLOutputProperties();
		saveDocument(out, doc, props); 
	}	
	
	/**
	 * Saves a document to writer
	 * @param out Target writer
	 * @param doc Source document
	 * @param encoding encoding (null for default - optional)
	 * @throws IOException
	 */
	static public void saveDocument(Writer out, Node doc, String encoding) throws IOException
	{
		XMLOutputProperties props = new XMLOutputProperties();
		props.encoding = encoding;
		
		saveDocument(out, doc, props); 
	}
	
	/**
	 * Saves a document to writer
	 * @param out Target writer
	 * @param doc Source document
	 * @param encoding encoding (null for default - optional)
	 * @param omitXmlDeclaration true to omit XML declaration
	 * @throws IOException Thrown if an error occurred
	 */
	static public void saveDocument(Writer out, Node doc, String encoding, boolean omitXmlDeclaration) throws IOException
	{
		XMLOutputProperties props = new XMLOutputProperties();
		props.encoding = encoding;
		props.omitXmlDeclaration = omitXmlDeclaration;
		
		saveDocument(out, doc, props);		 
	}
	
	/**
	 * Saves a document to writer
	 * @param out Target writer
	 * @param doc Source document
	 * @param encoding encoding (null for default - optional)
	 * @param omitXmlDeclaration true to omit XML declaration
	 * @param dtdURI URI to DTD definition (null to omit)
	 * @throws IOException Thrown if an error occurred 
	 */
	static public void saveDocument(Writer out, Node doc, String encoding, boolean omitXmlDeclaration, String dtdURI) throws IOException
	{
		XMLOutputProperties props = new XMLOutputProperties();
		props.encoding = encoding;
		props.omitXmlDeclaration = omitXmlDeclaration;
		props.dtdURI = dtdURI;
		
		saveDocument(out, doc, props);
	}	
	
	/**
	 * Saves a document to writer
	 * @param out Target writer
	 * @param doc Source document
	 * @param props XML output properties
	 * 
	 * @throws IOException Thrown if an error occurred 
	 */
	static public void saveDocument(Writer out, Node doc, XMLOutputProperties props) throws IOException
	{
		//---------------------------------------------------------
		// Use a Transformer for output
		TransformerFactory fact2 = null;
		
		try
		{
			fact2 = TransformerFactory.newInstance(); 
		} 
		catch (TransformerFactoryConfigurationError e)
		{
			// failed - will move to attempt #2
		}
		
		if (fact2 == null)
		{
			try
			{
				System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
				fact2 = TransformerFactory.newInstance();
			}
			catch (TransformerFactoryConfigurationError e)
			{
				// failed - will move to attempt #2
				e.printStackTrace();
				throw new RuntimeException("Failed to create transformer factory", e);				
			}
		}
		
		Transformer transformer = null;
		try
		{
			
			transformer = fact2.newTransformer();
		}
		catch (TransformerConfigurationException ex)
		{
			throw (IOException)(new IOException("Failed to create transformer builder").initCause(ex));
		}

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(out);
		transformer.setOutputProperty(OutputKeys.INDENT, props.indentXML ? "yes" : "no");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, props.omitXmlDeclaration ? "yes" : "no");
		if (props.dtdURI != null)
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, props.dtdURI);
		
		if (props.encoding != null)
			transformer.setOutputProperty(OutputKeys.ENCODING, props.encoding);

		try
		{
			transformer.transform(source, result);
		}
		catch (TransformerException ex)
		{
			throw (IOException)(new IOException("Failed during xml transformation").initCause(ex));
		}
	}

	
	static public String xmlToString(Node doc) throws IOException
	{ return xmlToString(doc, null); }
	
	static public String xmlToString(Node doc, String encoding) throws IOException
	{
		StringWriter out = new StringWriter();
		
		saveDocument(out, doc, encoding, false);
		
		return out.toString();
	}
	
	static public String xmlToString(Element doc) throws IOException
	{ return xmlToString(doc, (String)null); }
	
	static public String xmlToString(Element doc, String encoding) throws IOException
	{
		StringWriter out = new StringWriter();
		
		saveDocument(out, doc, encoding, true);
		
		return out.toString();
	}
	
	static public String xmlToString(Element doc, XMLOutputProperties props) throws IOException
	{
		StringWriter out = new StringWriter();
		
		saveDocument(out, doc, props);
		
		return out.toString();
	}
	
	/**
	 * Holds output properties for the saveDocument method family
	 */
	public static class XMLOutputProperties
	{
		/**
		 * Set output encoding.
		 */
		public String	encoding						= null;

		/**
		 * true to omit XML declaration tag output
		 */
		public boolean	omitXmlDeclaration	= false;

		/**
		 * true to format the XML with line breaks and indenting
		 */
		public boolean	indentXML	= true;

		/**
		 * URI pointing to DTD definition. null omits DTD definition.
		 */
		public String	dtdURI							= null;
	}
	
	/**
	 * Holds output properties for the parseDocument method family
	 */
	public static class XMLInputProperties
	{
		/**
		 * If true, do not import comments from XML
		 */
		public boolean ignoreComments = false;
		
		/**
		 * If false, do not validate XML against DTD
		 */
		public boolean validateXML = true;
	}

	
	/**
	 * 
	 * @author Eric Maziade
	 * <p>
	 * Helps filtering out elements
	 */
	public interface FindElementFilterIF {
		public boolean elementMatches(Element element);
	}

	//----------------------------------------------------------
	static private Map<String, CachedXML> m_xmlCache = new HashMap<String, CachedXML>();	
	static private class CachedXML 
	{		
		Document doc = null;		
		long stamp = 0;
	}
}