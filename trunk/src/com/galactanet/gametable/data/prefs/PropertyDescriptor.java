/*
 * PreferenceDescriptor.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.data.prefs;

import com.maziade.props.XPropertyType;

/**
 * PreferenceDescriptor is a simple object that describes a preference field.
 * 
 * @author iffy
 * 
 * #GT-AUDIT PreferenceDescriptor
 */
public class PropertyDescriptor
{
	/**
	 * Adds a property to the property list
	 * 
	 * @param name The property's internal name
	 * @param type The property's type
	 * @param defaultValue The property's default value
	 * @param visible true if the property should be visible to the user in the property panel
	 * @param group Internal name of the property group
	 * @param position The property's position within the group
	 */
	public PropertyDescriptor(String name, XPropertyType type, String defaultValue, boolean visible, String group, int position)
	{		
		m_name = name;
		m_type = type;
		m_defaultValue = defaultValue;
		m_visible = visible;
		m_group = group;
		m_position = position;
	}
	
	public final String m_name;
	public final XPropertyType m_type;
	public final String m_defaultValue;
	public final boolean m_visible;
	public final String m_group;
	public final int m_position;
	
	public final static String GROUP_TOOLS = "tools";
}
