/*
 * ModalDialog.java
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
package com.maziade.tools.ui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * @author Eric Maziade
 * 
 * Facilitates implementation of modal JDialogs
 * 
 */

public class ModalDialog extends JDialog
{
	/**
	 * Constructor
	 */
	public ModalDialog(Frame parent)
	{
		super(parent);
		
		setResizable(false);
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);//DO_NOTHING_ON_CLOSE);
		
		m_windowListener = new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent we)
			{
				dialogExitDefault();
			}
		};
		addWindowListener(m_windowListener);
	}

	/**
	 * 
	 * showModal is added as a convenience method that will call "setModal",
	 * "setVisible" and will return a value reflecting the dialog's conclusion
	 * (true = ok, false = cancel).
	 * 
	 * This mimics the way dialogs work in the Windows OS.
	 * 
	 * @return return value
	 */
	public ReturnValue showModal()
	{
		m_return = ReturnValue.OK;
		
		setModal(true);
		//setResizable(false);
		//setLocationByPlatform(true);
		setLocationRelativeTo(getParent());
		setVisible(true);
		
		return m_return;
	}
	
	/**
	 * Sets the return value to be returned by "showModal"
	 * @param value value.  Use the RETURN_... values
	 */
	public void setReturnValue(ReturnValue value)
	{
		m_return = value;
	}	
	
	@Override
	protected JRootPane createRootPane()
	{
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent)
			{
				m_return = ReturnValue.CANCEL;
				dispose();
			}
		};

		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		JRootPane myRootPane = new JRootPane();
		myRootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
		return myRootPane;
	}
	
	/**
	 * Manage the "X" button in title bar the same way than the cancelButton
	 */
	protected void dialogExitDefault()
	{
		setReturnValue(ReturnValue.CANCEL);
		this.dispose();
	}
		
	// --------------------------------------
	private ReturnValue m_return = null;

	public enum ReturnValue { OK, NO, OKALL, CANCEL }
	
	protected WindowAdapter m_windowListener;
	
	private static final long	serialVersionUID	= -6070426399423062226L;
}
