/*
 * EnumEditor.java
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
package com.maziade.props.ui.editors;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.ListDataListener;

import com.maziade.props.XProperties.XPropertyInfo;
import com.maziade.props.ui.EnumWrapper;
import com.maziade.props.ui.XPropertyTableRow;
import com.maziade.tools.Utils;

public class EnumEditor extends JComboBox implements PropertyEditorIF
{
	@Override
	public void init(XPropertyTableRow propertyRow)
	{
		m_propertyRow = propertyRow;
		
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e)
			{
				m_propertyRow.validateData();
			}
		});

		EnumComboBoxModel model = new EnumComboBoxModel(propertyRow.getProperty().getOptions(), propertyRow.getProperty()); 
		setModel(model);
		addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					enumValueChanged();
			}
		});
		
		model.setSelectedItem(propertyRow.getProperty().getValue());
	}
	
	@Override
	public JComponent getEditorComponent()
	{		
		return this;
	}
	
	/**
	 * 
	 */
	private void enumValueChanged()
	{
		EnumWrapper ew = (EnumWrapper)getSelectedItem();
		
		if (ew != null)
		{
			m_propertyRow.getProperty().setValue(ew.m_value.name());
		}			
	}
	
	/**
	 * Combo box model for enum values
	 * @author Eric Maziade
	 *
	 */
	private class EnumComboBoxModel implements ComboBoxModel
	{
		/**
		 * Constructor
		 * @param values
		 */
		public EnumComboBoxModel(Enum<?>[] values, XPropertyInfo propertyInfo)
		{
			if (values == null)
			{
				m_values = new EnumWrapper[0];
			}
			else
			{
				m_values = new EnumWrapper[values.length];
				int idx = 0;
				
				for (Enum<?> e : values)
					m_values[idx++] = new EnumWrapper(e, propertyInfo.getResourceBundleName(), propertyInfo.getName());
			}
			
			m_cbModel = new DefaultComboBoxModel(m_values);
		}

		@Override
		public Object getSelectedItem()
		{
			return m_cbModel.getSelectedItem();
		}

		@Override
		public void setSelectedItem(Object anItem)
		{
			m_cbModel.setSelectedItem(anItem);
		}
		
		/**
		 * Select option by Enum name
		 * @param optionName
		 */
		public void setSelectedItem(String optionName)
		{
			for (EnumWrapper ew : m_values)
			{
				if (Utils.equals(ew.m_value.name(), optionName))
				{
					setSelectedItem(ew);
					return;
				}
			}
		}

		@Override
		public void addListDataListener(ListDataListener l)
		{
			m_cbModel.addListDataListener(l);
		}

		@Override
		public Object getElementAt(int index)
		{
			return m_cbModel.getElementAt(index);
		}

		@Override
		public int getSize()
		{
			return m_cbModel.getSize();
		}

		@Override
		public void removeListDataListener(ListDataListener l)
		{
			m_cbModel.removeListDataListener(l);			
		}

		private final DefaultComboBoxModel m_cbModel;		
		private final EnumWrapper [] m_values;		
	}
	
	@Override
	public boolean isDataValid()
	{
		return true;
	}
	
	@Override
	public void commit()
	{
		// auto set when value changes		
	}
	
	private XPropertyTableRow m_propertyRow;
	
	/**
	 * UID 
	 */
	private static final long	serialVersionUID	= -6425049883278529498L;


}
