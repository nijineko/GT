/*
 * TextEditor.java
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

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.maziade.props.XProperties;
import com.maziade.props.ui.XPropertyTableRow;

public class TextEditor extends JTextField implements PropertyEditorIF
{
	@Override
	public void init(XPropertyTableRow propertyRow)
	{		
		m_propertyRow = propertyRow;
		setBorder(BorderFactory.createEmptyBorder());
		setText(propertyRow.getProperty().getValue());
		
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e)
			{
				m_propertyRow.validateData();
			}
		});
		
		getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				m_propertyRow.validateData();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				m_propertyRow.validateData();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				m_propertyRow.validateData();
			}
		});
	}
	
	@Override
	public JComponent getEditorComponent()
	{
		return this;
	}
	
	@Override
	public boolean isDataValid()
	{
		if (getText().trim().length() > 0)
		{
			return true;
		}
		
		m_propertyRow.setErrorTip(XProperties.getResourceString("XPropertiesDialog.ERROR_EMPTY_FIELD") + " " + m_propertyRow.getProperty().getToolTip());
		return false;
	}
	
	@Override
	public void commit()
	{
		m_propertyRow.getProperty().setValue(getText());		
	}
	
	private XPropertyTableRow m_propertyRow;
	
	/**
	 * UID
	 */
	private static final long	serialVersionUID	= 7182543949327397809L;
}
