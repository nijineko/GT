/*
 * NetworkParametersPanel.java
 *
 * @created 2010-09-06
 *
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.gametable.plugins.network;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.gametable.net.NetworkParametersPanel;


/**
 * Networking parameters panel
 *
 * @author Eric Maziade
 */
public class PeerNetworkParametersPanel extends NetworkParametersPanel
{  
  private JTextField m_passwordEntry   = new JTextField();
  private JTextField m_portEntry       = new JTextField();
  private JTextField m_ipAddress       = new JTextField();
  private JCheckBox  m_hostGame				 = new JCheckBox();
  private final NetworkModule m_networkModule;
  
  /**
	 * Constructor
	 */
	public PeerNetworkParametersPanel(NetworkModule module)
	{
		m_networkModule = module;
		initialize();
	}
  
	/**
	 * Initialize component
	 */
  private void initialize()
  {
  	// Make text fields "auto-selectable"
  	FocusListener listener = new FocusAdapter() {
  		/*
  		 * @see java.awt.event.FocusAdapter#focusGained(java.awt.event.FocusEvent)
  		 */
  		@Override
  		public void focusGained(FocusEvent e)
  		{
  			JTextField focused = (JTextField)e.getSource();
        focused.setSelectionStart(0);
        focused.setSelectionEnd(focused.getText().length());
  		}
		};
		
    m_ipAddress.addFocusListener(listener);
    m_portEntry.addFocusListener(listener);
    m_passwordEntry.addFocusListener(listener);
    
    // Build the panel
  	setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  	setAlignmentX(CENTER_ALIGNMENT);
  	
  	JLabel label;

  	m_hostGame.setText("Host Game");
  	add(m_hostGame);

  	label = new JLabel("Enter Host Address");
  	label.setLabelFor(m_ipAddress);
  	add(label);
    add(m_ipAddress);
    
  	label = new JLabel("Password: ");
  	label.setLabelFor(m_passwordEntry);
    add(label);
    add(m_passwordEntry);
    
    label = new JLabel("Port: ");
  	label.setLabelFor(m_portEntry);
    add(label);
    add(m_portEntry);
  }
  
  /*
   * @see com.galactanet.gametable.data.net.NetworkParametersPanel#validateValues()
   */
  @Override
  public boolean validateValues()
  {
  	// TODO #Networking Validate values
  	return true;
  }
  
  /*
   * @see com.galactanet.gametable.data.net.NetworkParametersPanel#setDefautValues()
   */
  @Override
  public void setDefautValues()
  {  	
    m_ipAddress.setText(m_networkModule.getIpAddress());
    m_portEntry.setText(String.valueOf(m_networkModule.getPort()));
    m_passwordEntry.setText(m_networkModule.getPassword());
    m_hostGame.setSelected(m_networkModule.getConnectAsHost());
  }
  
  /*
   * @see com.galactanet.gametable.data.net.NetworkParametersPanel#processValues()
   */
  @Override
  public void processValues()
  {
    try
    {
       m_networkModule.setPort(Integer.parseInt(m_portEntry.getText()));
    }
    catch (final NumberFormatException ex)
    {
    	m_networkModule.setPort(NetworkModule.DEFAULT_PORT);
    }
    
    m_networkModule.setIpAddress(m_ipAddress.getText());
    m_networkModule.setPassword(m_passwordEntry.getText());
    m_networkModule.setConnectAsHost(m_hostGame.isSelected());
  }
}
