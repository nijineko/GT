/*
 * XPropertyTableRow.java
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
package com.maziade.props.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.maziade.props.XProperties.XPropertyInfo;
import com.maziade.props.ui.editors.BooleanEditor;
import com.maziade.props.ui.editors.EnumEditor;
import com.maziade.props.ui.editors.FolderEditor;
import com.maziade.props.ui.editors.NumberEditor;
import com.maziade.props.ui.editors.PropertyEditorIF;
import com.maziade.props.ui.editors.TextEditor;


/**
 * TODO wtf? why is this not a control?
 * @author Eric Maziade
 * 
 */
public class XPropertyTableRow extends JPanel
{
	/**
	 * UID
	 */
	private static final long	serialVersionUID	= 8334231661049468392L;
		
	private final String			PATH_ERROR_ICON 	= "com/maziade/props/ui/icon-warning.png";	// TODO move to resource, move to proper package, review icons
	private final String			PATH_SPACER_ICON 	= "com/maziade/props/ui/icon-spacer.png";
	private int								m_index;
	private XPropertyInfo			m_property;
	private boolean						m_error = false;
	private JLabel						m_errorLabel;
	private JLabel						m_propertyLabel;
	private PropertyEditorIF 	m_propertyEditor;
		
	private XPropertyTable m_table;

	/**
	 * Constructor
	 */
	public XPropertyTableRow(XPropertyTable table,  XPropertyInfo property)
	{
		  
		m_table = table;
		m_property = property;
		initComponent();
	}

	/*
	@Override
	public Dimension getMaximumSize()
	{
		Dimension d = getPreferredSize();
		d.width = getParent().getWidth();
		return d;
	}
	*/

	/**
	 * Initialize component
	 */
	private void initComponent()
	{
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				m_table.onRowClicked(XPropertyTableRow.this);					
			}
		});		
		
		setLayout(new BorderLayout());

		m_errorLabel = new JLabel();	// TODO ??
		
		m_propertyLabel = new JLabel();
		m_propertyLabel.setText(m_property.getFullDisplayName());
		m_propertyLabel.setToolTipText(m_property.getToolTip());
		URL logoURL = this.getClass().getClassLoader().getResource(PATH_SPACER_ICON);
		m_propertyLabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(logoURL)));
		//add(m_propertyLabel);
					
		//-----------------------------------------------------------------------------
		// Container that keep the buttons aligned to the top
		
		// TODO enhance model to allow for module property types to be added
		switch (m_property.getType())
		{
		case BOOLEAN:
			m_propertyEditor = new BooleanEditor();
			break;
			
		case ENUM:
			m_propertyEditor = new EnumEditor();
			break;
			
		case FOLDER:
			m_propertyEditor = new FolderEditor();
			break;
			
		case NUMBER:
			m_propertyEditor = new NumberEditor();
			break;
			
		case TEXT:
			m_propertyEditor = new TextEditor();
			break;
		}
		
		m_propertyEditor.init(this);
		
		JComponent c = m_propertyEditor.getEditorComponent();
		add(c, BorderLayout.CENTER);
		
		//setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
	}
	
		
	/**
	 * Get the row's property label
	 * @return property label
	 */
	public JLabel getPropertyLabel()
	{
		return m_propertyLabel;
	}
	
	/**
	 * Get the row's property editor //TODO reconsider this class as JTable.  Should it be within the XPropertyTable or should it actually be the data model?
	 * @return
	 */
	public PropertyEditorIF getPropertyEditor()
	{
		return m_propertyEditor;
	}

	/**
	 * Validate if a row contain valid data
	 * 
	 * @return true if the row contain valid values or false if not
	 */
	public boolean rowIsValid()
	{
		return m_propertyEditor.isDataValid();
	}
	
	/**
	 * Set error message to display when mousing over the error icon
	 * @param message
	 */
	public void setErrorTip(String message)
	{
		m_propertyLabel.setToolTipText(message);
	}

	/**
	 * Validate row data when the focus on a value component is lost
	 * 
	 * @param e
	 */
	public void validateData()
	{
		if (rowIsValid())
		{
			applyModification();
			m_errorLabel.setVisible(false);
			if (m_error)
			{
				m_error = false;
				m_table.removeError(this);
			}
		}
		else
		{
			//m_errorLabel.setVisible(true);
			URL logoURL = this.getClass().getClassLoader().getResource(PATH_ERROR_ICON);
			//m_errorLabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(logoURL)));
			m_propertyLabel.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(logoURL)));
			
			m_error = true;
			m_table.addError(this);
		}			
	}

	/**
	 * Set the new value for the property
	 */
	private void applyModification()
	{
		m_propertyEditor.commit();
	}

	/**
	 * Define the index of the row and by this index define the color of the row
	 * 
	 * @param i
	 */
	public void setIndex(int i)
	{
		m_index = i;
		
		// Uses substance skin properties to determine background color
		
		// TODO allow hook for alternating background colors
		/*
		Color c = SubstanceColorUtilities.getStripedBackground(m_rowPane, m_index);
		setColor(c);
		m_rowPane.setBackground(c);
		*/
	}

	/**
	 * Return the modified property
	 * 
	 * @return property
	 */
	public XPropertyInfo getProperty()
	{
		return m_property;
	}

	/**
	 * Return the row index
	 * 
	 * @return m_index
	 */
	public int getIndex()
	{
		return m_index;
	}
}
