/*
 * XPropertiesDialog.java
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.maziade.props.XProperties;
import com.maziade.props.XProperties.XPropertyInfo;
import com.maziade.tools.ui.DlgUtils;
import com.maziade.tools.ui.ModalDialog;

/**
 * @author Eric Maziade
 * 
 * Displays XProperties
 * 
 */
public class XPropertiesDialog extends ModalDialog
{
	/**
	 * Default dialog width
	 */
	public static final int				DEFAULT_DIALOG_WIDTH			= 450;
	
	/**
	 * Default dialog height
	 */
	private static final int				DEFAULT_DIALOG_HEIGHT			= 300;//500
	
	/**
	 * Default spacing between controls
	 */
	public static final int				DEFAULT_SPACING						= 10;
	
	/**
	 * Properties on display
	 */
	private final XProperties				m_properties;
	
	/**
	 * TODO I don't remember what this is
	 */
	private XPropertyTable	m_propertiesManager;
	
	/**
	 * Ok Button
	 */
	private JButton									m_okButton;

	/**
	 * Constructor
	 * @param parent Parent window
	 * @param properties Properties to display
	 */
	public XPropertiesDialog(Frame parent, XProperties properties)
	{
		super(parent);
		
		m_properties = new XProperties();
		m_properties.copyAllPropertiesFrom(properties);
		
		initComponents();
	}

	/**
	 * Initializes the property dialog's components
	 */
	private void initComponents()
	{
		// TODO RESTORE PROPERTIES DIALOG SIZE/POS FROM PREFERENCE FILES
		
		setResizable(true);
		
		setMinimumSize(new Dimension(DEFAULT_DIALOG_WIDTH, DEFAULT_DIALOG_HEIGHT));	// TODO what is that?
				
		setTitle(XProperties.getResourceString("XPropertiesDialog.TITLE"));	// TODO incorporate resources (all XProperties.getResourceString calls)	
		
		// initDefaultSettings();	// TODO provide hook to insert default settings
		
		// Create and set up the window
		setLayout(new BorderLayout());
		
		// Create and setup the panel that will contain cards
		Component table = initializeTableComponent();
		add(table, BorderLayout.CENTER);
		
		// Create and setup the footer panel
		JPanel footer = initFooterPanel();
		add(footer, BorderLayout.PAGE_END);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
			    saveDialogSettings();
			    dispose();
			}
		 });
		
		//pack();		
		addAllVisibleProperties();
	}
	
	/**
	 * Initialize table components
	 * @return Component to use as main display
	 */
	private Component initializeTableComponent()
	{
		// Create and setup the panel that will contain rows and add a starter row
		m_propertiesManager = new XPropertyTable(this);
		
		// Add a JScrollPane that will contain the mainPain
		JScrollPane scrollPane = new javax.swing.JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setAutoscrolls(true);
		scrollPane.setViewportView(m_propertiesManager);

		return scrollPane;
	}

	/**
	 * Create and return a footer panel containing all buttons
	 * 
	 * @return footer panel
	 */
	private JPanel initFooterPanel()
	{
		// Create the panel that will contain the bottoms buttons
		JPanel footer = new JPanel();
		footer.setLayout(new FlowLayout(FlowLayout.TRAILING));
		
		// Create the okButton
		m_okButton = new JButton();
		DlgUtils.setButtonText(m_okButton, "XPropertiesDialog.OK");	// TODO implement and explain resource bundle
		
		m_okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				okButtonActionPerformed(evt);
			}
		});
		footer.add(m_okButton);
		getRootPane().setDefaultButton(m_okButton);

		// Create the cancel button
		JButton cancelButton = new JButton();
		DlgUtils.setButtonText(cancelButton, "XPropertiesDialog.CANCEL");
		
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				cancelButtonActionPerformed(evt);
			}
		});
		footer.add(cancelButton);
		
		return footer;
	}
	
	/**
	 * Add all visible properties to the dialog
	 * 
	 */
	private void addAllVisibleProperties()
	{
		List<XPropertyInfo> properties = m_properties.getVisibleProperties();
		
		for( XPropertyInfo property : properties) 
		{			
			m_propertiesManager.addRow(property);		
		}		
	}

	/**
	 * Retrieve the edited properties. Should be called only if the return value is RETURN_OK.
	 * 
	 * @return new properties
	 */
	public XProperties getProperties()
	{
		return m_properties;
	}
	
	/**
	 * Enable / disable ok button
	 * @param enabled
	 */
	public void enableOkButton(boolean enabled)
	{
		m_okButton.setEnabled(enabled);
	}

	/**
	 * Cancel button has been pressed
	 * @param evt event that triggered this action
	 */
	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		setReturnValue(ReturnValue.CANCEL);
		saveDialogSettings();
		this.dispose();
	}

	/**
	 * Ok button has been pressed
	 * @param evt event that triggered this action
	 */
	private void okButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		setReturnValue(ReturnValue.OK);		
		saveDialogSettings();		
		this.dispose();
	}



	

	

	
	/**
	 * Save this dialog's size and position, if required   
	 */	
	public void saveDialogSettings() 
	{			
		// TODO hook to save this dialog's settings
	}
	
	private static final long	serialVersionUID	= -4322946926126430992L;
}