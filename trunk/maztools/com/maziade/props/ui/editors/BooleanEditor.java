/*
 * BooleanEditor.java
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

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.maziade.props.ui.XPropertyTableRow;

public class BooleanEditor extends JCheckBox implements PropertyEditorIF
{	
	@Override
	public void init(XPropertyTableRow propertyRow)
	{		
		m_propertyRow = propertyRow;
		
		setSize(getPreferredSize());
		setBackground(null);
		setContentAreaFilled(false);
		
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e)
			{
				m_propertyRow.validateData();
			}
		});
		
		setSelected(Boolean.valueOf(propertyRow.getProperty().getValue()));			
	}
	
	@Override
	public JComponent getEditorComponent()
	{		
		return this;
	}

	@Override
	public boolean isDataValid()
	{
		return true;
	}
	
	@Override
	public void commit()
	{
		m_propertyRow.getProperty().setValue(String.valueOf(isSelected()));		
	}
	
	private XPropertyTableRow m_propertyRow;
	
	/**
	 * Serial UID
	 */
	private static final long	serialVersionUID	= -9069726099902473141L;
}
