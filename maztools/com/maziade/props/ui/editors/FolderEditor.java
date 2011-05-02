/*
 * FolderEditor.java
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

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.maziade.props.ui.XPropertyTableRow;

public class FolderEditor extends JPanel implements PropertyEditorIF
{	
	@Override
	public void init (XPropertyTableRow propertyRow)
	{
		m_propertyRow = propertyRow;
		setLayout(new BorderLayout());

		String value = m_propertyRow.getProperty().getValue();
		m_fileValue = new File(value);

		m_fileLabel = new JLabel();
		m_fileLabel.setText(value);
		add(m_fileLabel, BorderLayout.CENTER);
		
		JButton filePickerButton = new JButton("...");
		filePickerButton.setSize(filePickerButton.getPreferredSize());
		filePickerButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				openFolderPicker();
			}
		});
		
		add(filePickerButton, BorderLayout.EAST);
		
		setOpaque(false);							
	}
	
	@Override
	public JComponent getEditorComponent()
	{		
		return this;
	}
	
	@Override
	public boolean isDataValid()
	{
		return m_fileValue != null;
	}
	
	/**
	 * Open folder picker
	 */
	private void openFolderPicker()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (m_fileValue != null)
			fileChooser.setSelectedFile(m_fileValue);

		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION)
		{
			m_fileValue = fileChooser.getSelectedFile();
			m_fileLabel.setText(m_fileValue.getPath());
			m_fileLabel.setToolTipText(m_fileLabel.getText());
			m_propertyRow.getProperty().setValue(m_fileValue.getPath());
		}
	}	
	
	@Override
	public void commit()
	{
		// nothing to do - value commited automatically		
	}
	
	private XPropertyTableRow m_propertyRow;
	private JLabel m_fileLabel;
	private File m_fileValue;
	
	/**
	 * UID
	 */
	private static final long	serialVersionUID	= -9120226627424040807L;
}
