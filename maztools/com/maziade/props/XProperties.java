/*
 * XProperties.java
 * 
 * @created 23-Oct-06
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.maziade.tools.Strings;
import com.maziade.tools.Utils;

/**
 * 
 * @author Eric Maziade
 * 
 *         Holds property information. Has extra information that allows the properties to be properly displayed and
 *         modified by the property dialog.
 */
public class XProperties
{
	/**
	 * Class holding property information
	 */
	public class XPropertyInfo implements Comparable<XPropertyInfo>
	{
		/**
		 * Properties' resource bundle name 
		 */
		private final String				m_resourceBundleName;
		
		/**
		 * Property's group name
		 */
		private final String				m_group;

		/**
		 * Property name
		 */
		private final String				m_name;

		/**
		 * Property's options if of type ENUM
		 */
		private final Enum<?>[]			m_options;

		/**
		 * Property's position within its group
		 */
		private final int						m_position;

		/**
		 * Property type
		 */
		private final XPropertyType	m_type;

		/**
		 * Property's visibility within the properties dialog
		 */
		private boolean							m_visible;

		/**
		 * Constructor
		 * 
		 * @param name Property's internal name. Will be read from resource bundle.
		 * @param type Property's type
		 * @param visible Property's visibility within the property dialog
		 * @param group Property's group name; Will be read from resource bundle.
		 * @param position Property's position within its group
		 * @param options Available options for properties of type Enum
		 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
		 */
		public XPropertyInfo(String name, XPropertyType type, boolean visible, String group, int position, Enum<?>[] options, String resourceBundleName)
		{
			m_name = name;
			m_type = type;
			m_visible = visible;
			m_options = options;
			m_group = group;
			m_position = position;
			m_resourceBundleName = resourceBundleName;
		}

		/**
		 * Constructor
		 * 
		 * @param copy This new instance will copy the data from this property.
		 */
		public XPropertyInfo(XPropertyInfo copy)
		{
			m_name = copy.m_name;
			m_type = copy.m_type;
			m_visible = copy.m_visible;
			m_options = copy.m_options;
			m_group = copy.m_group;
			m_position = copy.m_position;
			m_resourceBundleName = copy.m_resourceBundleName;
			setValue(copy.getValue());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(XPropertyInfo o)
		{
			return Strings.compareToIgnoreCase(toString(), o.toString());
		}

		/**
		 * Gets the property's display name Property display name are read from the resource file. Resource entry name
		 * follows the form of : XProperties.entry.[property_name]
		 * 
		 * @return Property name
		 */
		public String getDisplayName()
		{
			if (m_resourceBundleName != null)
			{
				ResourceBundle bundle = ResourceBundle.getBundle(m_resourceBundleName);
				
				if (bundle != null)
				{
					try
					{
						return bundle.getString(m_name);
					}
					catch (MissingResourceException e)
					{
						return '!' + m_name + '!';
					}
				}
			}
			
			return XProperties.getResourceString("XProperties.entry." + m_name);
		}

		/**
		 * Gets the property's full display name
		 * 
		 * @return Group display name and property display name
		 */
		public String getFullDisplayName()
		{
			String group = getGroupDisplayName();
			if (Strings.isEmpty(group))
				return getDisplayName();

			return group + " " + getDisplayName();
		}

		/**
		 * Gets the display name of the property's group. Property group display name are read from the resource file.
		 * Resource entry name follows the form of : XProperties.group.[groupname]
		 * 
		 * @return group name or null if property is not within a group
		 */
		public String getGroupDisplayName()
		{
			if (Strings.isEmpty(m_group))
				return null;
			
			if (m_resourceBundleName != null)
			{
				ResourceBundle bundle = ResourceBundle.getBundle(m_resourceBundleName);

				if (bundle != null)
				{
					try
					{
						return bundle.getString(m_group);
					}
					catch (MissingResourceException e)
					{
						return '!' + m_group + '!';
					}
				}
			}
			
			return XProperties.getResourceString("XProperties.group." + m_group); // TODO document property scheme
		}
		
		/**
		 * Gets the property's resource bundle name
		 * @return
		 */
		public String getResourceBundleName()
		{
			return m_resourceBundleName;
		}

		/**
		 * Gets the property's internal name.
		 * 
		 * @return property name
		 */
		public String getName()
		{
			return m_name;
		}

		/**
		 * Returns a list of options properties of type ENUM can propose.
		 * 
		 * @return Enumeration of options
		 */
		public Enum<?>[] getOptions()
		{
			return m_options;
		}

		/**
		 * Returns the property's position within its group. Properties are sorted by position first, by display name
		 * second.
		 * 
		 * @return position number
		 */
		public int getPosition()
		{
			return m_position;
		}

		/**
		 * Get the tool tip to display upon hovering a property. Tool tips are read from the resource file. Resource entry
		 * name follows the form of : XProperties.entry.[property_name].T
		 * 
		 * @return Tool tip string
		 */
		public String getToolTip()
		{
			if (m_resourceBundleName != null)
			{
				ResourceBundle bundle = ResourceBundle.getBundle(m_resourceBundleName);
				
				if (bundle != null)
				{
					try
					{
						return bundle.getString(m_name + ".tooltip");
					}
					catch (MissingResourceException e)
					{
						return '!' + m_name + '!';
					}
				}
			}
			
			return XProperties.getResourceString("XProperties.entry." + m_name + ".tooltip");
		}
		/**
		 * Returns the property's data type
		 * 
		 * @return Data type
		 */
		public XPropertyType getType()
		{
			return m_type;
		}
		/**
		 * Get the value of this property
		 * 
		 * @return String value of property
		 */
		public String getValue()
		{
			return m_properties.getProperty(m_name);
		}
		/**
		 * Returns the property's visibility status. Invisible properties are not shown in the property dialog.
		 * 
		 * @return true if property should be visible in dialog.
		 */
		public boolean isVisible()
		{
			return m_visible;
		}
		public void setValue(String value)
		{
			m_properties.setProperty(m_name, value);
		}
		/**
		 * Change the property's visible status
		 * 
		 * @param visible
		 */
		public void setVisible(boolean visible)
		{
			m_visible = visible;
		}

	}
	/**
	 * Language resource bundle
	 */
	private static ResourceBundle				g_resourceBundle;

	/**
	 * Build a String value from a Point
	 * 
	 * @param point coordinates
	 * @return Property value in String form
	 */
	public static String fromPoint(Point point)
	{
		if (point == null)
			return "";

		return point.x + "," + point.y;
	}
	
	/**
	 * Build a String value from a Dimension
	 * 
	 * @param dimension Size
	 * @return Property value in String form
	 */
	public static String fromDimension(Dimension dimension)
	{
		if (dimension == null)
			return "";

		return dimension.width + "," + dimension.height;
	}

	/**
	 * Build a String value form a Rectangle
	 * 
	 * @param rect rectangle
	 * @return Property value in String form
	 */
	public static String fromRectangle(Rectangle rect)
	{
		if (rect == null)
			return "";

		return rect.x + "," + rect.y + "," + rect.width + "," + rect.height;
	}

	/**
	 * Get the currently set resource bundle. If none are set, the default bundle is read from com.maziade.props.messages
	 * 
	 * @return ResourceBundle
	 */
	public static ResourceBundle getResourceBundle()
	{
		if (g_resourceBundle == null)
			g_resourceBundle = ResourceBundle.getBundle("com.maziade.props.messages");

		return g_resourceBundle;
	}

	/**
	 * TODO review resource architecture Get a resource string from the set resource bundle
	 * 
	 * @param key
	 * @return
	 */
	public static String getResourceString(String key)
	{
		try
		{
			return getResourceBundle().getString(key);
		}
		catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
	}

	/**
	 * You can provide your own resource bundle to override the default bundle
	 * 
	 * @param res
	 */
	public static void setResourceBundle(ResourceBundle res)
	{
		if (res == null)
			throw new IllegalArgumentException("Cannot set null resource bundle");
		
		g_resourceBundle = res;
	}

	/**
	 * Build a Point value from a String value
	 * 
	 * @param param Property's string value
	 * @param defaultPoint Point object to return if string is invalid or empty
	 * @return Point value
	 */
	public static Point toPoint(String param, java.awt.Point defaultPoint)
	{
		if (param == null)
			return defaultPoint;

		String parts[] = param.split(",");

		if (parts.length != 2)
			return defaultPoint;

		try
		{
			return new Point(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
		}
		catch (NumberFormatException e)
		{
			return defaultPoint;
		}
	}
	
	/**
	 * Build a Dimension value from a String value
	 * 
	 * @param param Property's string value
	 * @param defaultDimension Dimension object to return if string is invalid or empty
	 * @return Dimension value
	 */
	public static Dimension toDimension(String param, java.awt.Dimension defaultDimension)
	{
		if (param == null)
			return defaultDimension;

		String parts[] = param.split(",");

		if (parts.length != 2)
			return defaultDimension;

		try
		{
			return new Dimension(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
		}
		catch (NumberFormatException e)
		{
			return defaultDimension;
		}
	}

	/**
	 * Build a Rectangle object form a String value
	 * 
	 * @param param Property's string value
	 * @param defaultRect rectangle object to return if string is invalid or empty
	 * @return Rectangle value
	 */
	public static Rectangle toRectangle(String param, Rectangle defaultRect)
	{
		if (param == null)
			return defaultRect;

		String parts[] = param.split(",");

		if (parts.length != 4)
			return defaultRect;

		try
		{
			return new Rectangle(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), Integer.valueOf(parts[2]), Integer.valueOf(parts[3]));
		}
		catch (NumberFormatException e)
		{
			return defaultRect;
		}
	}

	/**
	 * Load properties from file
	 * 
	 * @param properties
	 * @param fileName
	 */
	protected static void loadFromFile(Properties properties, File file)
	{
		// -----------------------------------
		// Load properties from file
		File props = file;
		if (props.exists())
		{
			try
			{
				properties.loadFromXML(new FileInputStream(props));
			}
			catch (InvalidPropertiesFormatException e)
			{
				File backup = getSettingsBackupFile(file.getPath());
				try
				{
					Utils.copyFile(props, backup.getAbsolutePath());
				}
				catch (IOException e2)
				{
					Logger.getLogger(XProperties.class).error("Failed taking a backup of " + props + " to " + backup, e2);
				}

				// Get the exception handled without breaking the flow of the method.
				UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
				eh.uncaughtException(Thread.currentThread(), new XPropertiesException(e, props, backup));
			}
			catch (FileNotFoundException e)
			{
				// Should not happen - has already been processed - we'll let the default handler handle this.
				throw new RuntimeException("Properties file not found", e);
			}
			catch (IOException e)
			{
				File backup = getSettingsBackupFile(file.getPath());
				try
				{
					Utils.copyFile(props, backup.getAbsolutePath());
				}
				catch (IOException e2)
				{
					Logger.getLogger(XProperties.class).error("Failed taking a backup of " + props + " to " + backup, e2);
				}

				// Get the exception handled without breaking the flow of the method.
				UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
				eh.uncaughtException(Thread.currentThread(), new XPropertiesException(e, props, backup));
			}
		}
	}

	/**
	 * Generate a properties backup file name
	 * 
	 * @param Basic file name
	 * @return non-existent file for storing backup configuration
	 */
	private static File getSettingsBackupFile(String fileName)
	{
		String ext = ".xml";
		
		int pos = fileName.lastIndexOf('.');
		if (pos > -1)
			ext = fileName.substring(pos);
		
		File props;
		int counter = 1;

		do
		{
						
			props = new File(fileName + "." + Strings.padLeft(counter++, 3, "0") + ".bak" + ext);

		} while (props.exists());

		return props;
	}

	/**
	 * Internal Properties object used for handling properties
	 */
	private Properties									m_properties					= new Properties();

	/**
	 * Mapping properties' name to XPropertyInfo objects
	 */
	private Map<String, XPropertyInfo>	m_propertyMap					= new HashMap<String, XPropertyInfo>();

	/**
	 * Keeping only visible properties in this list TODO thread-safe this sucker
	 */
	private List<XPropertyInfo> m_visiblePropertyList	= new ArrayList<XPropertyInfo>();

	/**
	 * Public constructor
	 */
	public XProperties()
	{
	}

	/**
	 * Copy properties all properties from properties into this instance
	 * 
	 * @param properties Properties to copy from
	 */
	public void copyAllPropertiesFrom(XProperties properties)
	{
		for (XPropertyInfo property : properties.m_propertyMap.values())
		{
			addProperty(new XPropertyInfo(property));
		}
	}

	/**
	 * Get a property value
	 * 
	 * @param propertyName Name of the property
	 * @return Property's string value. En empty string is returned if the property is not found.
	 */
	public String getProperty(String propertyName)
	{
		return getProperty(propertyName, "");
	}

	/**
	 * Get a property value
	 * 
	 * @param propertyName Name of the property
	 * @param defaultValue Default value to return if the property is not found
	 * @return Property's string value
	 */
	public String getProperty(String propertyName, String defaultValue)
	{
		XPropertyInfo prop = m_propertyMap.get(propertyName);

		if (prop == null)
			return defaultValue;

		return prop.getValue();
	}

	/**
	 * Gets a PropertyInfo object from the list of properties
	 * 
	 * @param propertyName Name of the property to look for
	 * @return property object or null if no property matching the name has been found
	 */
	public XPropertyInfo getPropertyInfo(String propertyName)
	{
		return m_propertyMap.get(propertyName);
	}

	/**
	 * Get a list of all property names matching the specified prefix
	 * 
	 * @param startsWith Property name prefix to look for and remove
	 * @return List of property names (never null)
	 */
	public List<String> getPropertyNamesStartingWith(String startsWith)
	{
		List<String> lst = new ArrayList<String>();

		for (XPropertyInfo property : m_propertyMap.values())
		{
			if (property.m_name.startsWith(startsWith))
			{
				lst.add(property.m_name);
			}
		}

		return lst;
	}

	/**
	 * Gets a property value from the list of properties
	 * 
	 * @param propertyName Name of the property
	 * @return property object or null if no property matching the name has been found
	 */
	public String getPropertyValue(String propertyName)
	{
		XPropertyInfo prop = getPropertyInfo(propertyName);

		if (prop == null)
			return null;

		return prop.getValue();
	}

	/**
	 * Get a list of visible properties, in the order in which they should be displayed
	 * 
	 * @return list object
	 */
	public List<XPropertyInfo> getVisibleProperties()
	{
		return Collections.unmodifiableList(m_visiblePropertyList); // TODO consider synchronized unmodifiable instance
	}

	/**
	 * Load the properties
	 */
	public void loadProperties()
	{
		// TODO use another means of storage
		loadFromFile(m_properties, getSaveFile());

		Enumeration<Object> lst = m_properties.keys();
		while (lst.hasMoreElements())
		{
			String key = (String) lst.nextElement();
			if (m_propertyMap.get(key) == null)
			{
				addTextProperty(key, null, false, null, 0, null);
			}
		}
	}

	/**
	 * Remove a property from the property list
	 * 
	 * @param propertyName
	 */
	public void remove(String propertyName)
	{
		XPropertyInfo prop = m_propertyMap.get(propertyName);

		if (prop == null)
			return;

		m_propertyMap.remove(propertyName);
		m_properties.remove(propertyName);
		m_visiblePropertyList.remove(prop);
	}

	/**
	 * Remove all properties starting with the specified string
	 * 
	 * @param startsWith Property name prefix to look for and remove
	 */
	public void removeStartsWith(String startsWith)
	{
		List<XPropertyInfo> removeList = new ArrayList<XPropertyInfo>();

		for (XPropertyInfo property : m_propertyMap.values())
		{
			if (property.m_name.startsWith(startsWith))
			{
				removeList.add(property);
			}
		}

		m_visiblePropertyList.removeAll(removeList);
		for (XPropertyInfo prop : removeList)
		{
			remove(prop.getName());
		}
	}

	/**
	 * Save the properties
	 */
	public void save()
	{
		File props = getSaveFile();
		
		try
		{
			Utils.storePropertiesToXML(m_properties, props, "XProperties File"); // TODO allow specifying description
		}
		catch (FileNotFoundException e)
		{
			Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
		}
		catch (IOException e)
		{
			Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
		}
	}

	// ------------------------------------------------------

	/**
	 * Set a property's value
	 * 
	 * @param propertyName Name of the property's value to set. If the property does not already exists, a new property is
	 *          creating, with TEXT assumed as its type.
	 * @param value Property's new value
	 */
	public void setPropertyValue(String propertyName, String value)
	{
		XPropertyInfo prop = m_propertyMap.get(propertyName);

		if (prop == null)
			addProperty(propertyName, XPropertyType.TEXT, value, false, null, 0, null);
		else
			prop.setValue(value);
	}
	
	/**
	 * Adds a text property to the property list
	 * 
	 * @param name The property's internal name
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
	 */
	
	public XPropertyInfo addTextProperty(String name, String defaultValue, boolean visible, String group, int position, String resourceBundleName)
	{
		return addProperty(name, XPropertyType.TEXT, defaultValue, visible, group, position, resourceBundleName);
	}
	
	/**
	 * Adds a number property to the property list
	 * 
	 * @param name The property's internal name
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
	 */
	public XPropertyInfo addNumberProperty(String name, int defaultValue, boolean visible, String group, int position, String resourceBundleName)
	{
		return addProperty(name, XPropertyType.NUMBER, String.valueOf(defaultValue), visible, group, position, resourceBundleName);
	}
	
	/**
	 * Adds a float property to the property list
	 * 
	 * @param name The property's internal name
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
	 */
	public XPropertyInfo addNumberProperty(String name, float defaultValue, boolean visible, String group, int position, String resourceBundleName)
	{
		return addProperty(name, XPropertyType.NUMBER, String.valueOf(defaultValue), visible, group, position, resourceBundleName);
	}
	
	/**
	 * Adds a boolean property to the property list
	 * 
	 * @param name The property's internal name
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
	 */
	public XPropertyInfo addBooleanProperty(String name, boolean defaultValue, boolean visible, String group, int position, String resourceBundleName)
	{
		return addProperty(name, XPropertyType.BOOLEAN, String.valueOf(defaultValue), visible, group, position, resourceBundleName);
	}
	
	/**
	 * Adds a folder property to the property list
	 * 
	 * @param name The property's internal name
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
	 */
	public XPropertyInfo addFolderProperty(String name, File defaultValue, boolean visible, String group, int position, String resourceBundleName)
	{
		return addProperty(name, XPropertyType.FOLDER, String.valueOf(defaultValue), visible, group, position, resourceBundleName);
	}
	
	/**
	 * Adds an enum property to the property list
	 * 
	 * @param name The property's internal name
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 * @param options The enums options list (as returned by Enum.values())
	 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
	 */
	public XPropertyInfo addEnumProperty(String name, Enum<?> defaultValue, boolean visible, String group, int position, Enum<?>[] options, String resourceBundleName)
	{
		XPropertyInfo prop = new XPropertyInfo(name, XPropertyType.ENUM, visible, group, position, options, resourceBundleName);
		
		if (defaultValue != null)
			prop.setValue(defaultValue.toString());
		
		return addProperty(prop);		
	}

	/**
	 * Adds a property to the property list
	 * 
	 * @param name The property's internal name
	 * @param type The property's type
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 * @param resourceBundleName This should be a fully qualified bundle name.  The bundle will be read using ResourceBundle.getBundle.  
		 * If null, will use the main propertie's resource bundle
	 */
	public XPropertyInfo addProperty(String name, XPropertyType type, String defaultValue, boolean visible, String group, int position, String resourceBundleName)
	{
		if (type == XPropertyType.ENUM)
			throw new IllegalArgumentException();

		XPropertyInfo prop = new XPropertyInfo(name, type, visible, group, position, null, resourceBundleName);

		if (defaultValue != null)
			prop.setValue(defaultValue);

		m_propertyMap.put(name, prop);

		if (prop.m_visible)
		{
			m_visiblePropertyList.add(prop);
			Collections.sort(m_visiblePropertyList, PropertyNameComparator.getComparator());
		}

		return prop;
	}

	/**
	 * Adds a property to the property list
	 * 
	 * @param property PropertyInfo object to add
	 */
	private XPropertyInfo addProperty(XPropertyInfo property)
	{
		m_propertyMap.put(property.m_name, property);

		if (property.m_visible)
		{
			m_visiblePropertyList.add(property);
			Collections.sort(m_visiblePropertyList, PropertyNameComparator.getComparator());
		}

		return property;
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public String getTextPropertyValue(String propertyName)
	{
		return getPropertyValue(propertyName);		
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public int getNumberPropertyValue(String propertyName)
	{
		try
		{
			return Integer.valueOf(getPropertyValue(propertyName));
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public float getNumberFloatPropertyValue(String propertyName)
	{
		try
		{
			return Float.valueOf(getPropertyValue(propertyName));
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}	
	
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public boolean getBooleanPropertyValue(String propertyName)
	{
		try
		{
			return Boolean.valueOf(getPropertyValue(propertyName));
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public File getFolderPropertyValue(String propertyName)
	{
		return new File(getPropertyValue(propertyName));
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public Enum<?> getEnumPropertyValue(String propertyName)
	{
		XPropertyInfo prop = getPropertyInfo(propertyName);
		if (prop == null || prop.getType() != XPropertyType.ENUM)
			return null;
		
		String value = prop.getValue();
		if (value == null)
			return null;
		
		for (Enum<?> option : prop.getOptions())
		{
			if (option.name().equals(value))
				return option;
		}
		
		return null;		
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public void setTextPropertyValue(String propertyName, String value)
	{
		setPropertyValue(propertyName, value);
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public void setNumberPropertyValue(String propertyName, int value)
	{
		setPropertyValue(propertyName, String.valueOf(value));
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public void setNumberPropertyValue(String propertyName, float value)
	{
		setPropertyValue(propertyName, String.valueOf(value));
	}	
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public void setBooleanPropertyValue(String propertyName, boolean value)
	{
		setPropertyValue(propertyName, String.valueOf(value));		
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public void setFolderPropertyValue(String propertyName, File value)
	{
		setPropertyValue(propertyName, value.getPath());
	}
	
	/**
	 * Gets a property value 
	 * @param propertyName The property's internal name
	 * @return value;
	 */
	public void setEnumPropertyValue(String propertyName, Enum<?>  value)
	{
		setPropertyValue(propertyName, value == null ? "" : value.name());
	}
	
	/**
	 * Get the save file to use for saving the properties
	 * @return
	 */
	public File getSaveFile()
	{
		return m_saveFile;
	}
	
	/**
	 * Set the save file to use for saving the properties
	 * @param file
	 */
	public void setSaveFile(File file)
	{
		m_saveFile = file;
	}
	
	private File m_saveFile = new File("properties.xml");
}
