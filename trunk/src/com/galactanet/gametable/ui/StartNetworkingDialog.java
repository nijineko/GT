/*
 * JoinDialog.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.net.NetworkParametersPanel;
import com.galactanet.gametable.util.Log;



/**
 * #GT-COMMENT
 * 
 * @author sephalon
 * 
 * #GT-AUDIT JoinDialog
 */
public class StartNetworkingDialog extends JDialog implements FocusListener
{
    /**
     * 
     */
    private static final long serialVersionUID  = -7877135247158193423L;
    JLabel                    jLabel2           = new JLabel();
    JLabel                    jLabel3           = new JLabel();
    boolean                   m_bAccepted;
    JButton                   m_cancel          = new JButton();
    JTextField                m_charNameEntry   = new JTextField();
    CardLayout                m_hostPanelLayout = new CardLayout(0, 0);
    JPanel                    m_hostPanel       = new JPanel(m_hostPanelLayout);

    JButton                   m_ok              = new JButton();    
    JTextField                m_plrNameEntry    = new JTextField();


    public StartNetworkingDialog()
    {
        try
        {
            initialize();
        }
        catch (final RuntimeException e)
        {
            Log.log(Log.SYS, e);
        }

        // pack yourself
        pack();

        m_ok.requestFocus();
    }

    public void focusGained(final FocusEvent e)
    {
        // only interested in JTextFields
        if (!(e.getSource() instanceof JTextField))
        {
            return;
        }

        final JTextField focused = (JTextField)e.getSource();
        focused.setSelectionStart(0);
        focused.setSelectionEnd(focused.getText().length());
    }

    public void focusLost(final FocusEvent e)
    {
    }

    private void initialize()
    {
        setModal(true);
        setResizable(false);

        m_ok.setText("OK");
        m_ok.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
              if (m_networkPanel != null)
              {
              	if (!m_networkPanel.validateValues())
              		return;
              }
              		              
                m_bAccepted = true;

                // update the default names
                m_core.setPlayerInformation(m_plrNameEntry.getText(), m_charNameEntry.getText());
                
                if (m_networkPanel != null)
                	m_networkPanel.processValues();

                dispose();
            }
        });

        m_cancel.setText("Cancel");
        m_cancel.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                dispose();
            }
        });

        
        jLabel2.setText("Player Name:");
        jLabel3.setText("Char Name:");


        final int PADDING = 5;

        final Box outmostBox = Box.createHorizontalBox();
        getContentPane().add(outmostBox, BorderLayout.CENTER);
        outmostBox.add(Box.createHorizontalStrut(PADDING));
        final Box outerBox = Box.createVerticalBox();
        outmostBox.add(outerBox);
        outmostBox.add(Box.createHorizontalStrut(PADDING));

        outerBox.add(Box.createVerticalStrut(PADDING));

        Box nextBox = Box.createHorizontalBox();
        outerBox.add(nextBox);
        nextBox.add(jLabel2);
        nextBox.add(Box.createHorizontalStrut(5));
        nextBox.add(m_plrNameEntry);

        outerBox.add(Box.createVerticalStrut(PADDING));

        nextBox = Box.createHorizontalBox();
        outerBox.add(nextBox);
        nextBox.add(jLabel3);
        nextBox.add(Box.createHorizontalStrut(5));
        nextBox.add(m_charNameEntry);

        outerBox.add(Box.createVerticalStrut(PADDING * 2));

        outerBox.add(m_hostPanel);
        m_networkPanel = m_core.getNetworkModule().getParametersPanel();
        m_networkPanel.setDefautValues();
        
        nextBox = Box.createVerticalBox();
        nextBox.add(m_networkPanel );
        m_hostPanel.add(nextBox, "join");
        
        
        JPanel panel = new JPanel();
        m_hostPanel.add(panel, "host");

        outerBox.add(Box.createVerticalStrut(PADDING * 2));

        nextBox = Box.createHorizontalBox();
        outerBox.add(nextBox);
        
        outerBox.add(Box.createVerticalStrut(PADDING * 3));
        outerBox.add(Box.createVerticalGlue());

        panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        outerBox.add(panel);
        panel.add(m_ok);
        panel.add(Box.createHorizontalStrut(PADDING));
        panel.add(m_cancel);

        outerBox.add(Box.createVerticalStrut(PADDING));

        // set default values
        m_charNameEntry.setText(m_core.getCharacterName());
        m_plrNameEntry.setText(m_core.getPlayerName());

        // we want to know if any of those text entry areas get focus
        m_plrNameEntry.addFocusListener(this);
        m_plrNameEntry.setPreferredSize(new Dimension(150, m_plrNameEntry.getPreferredSize().height));
        m_charNameEntry.addFocusListener(this);
        m_charNameEntry.setPreferredSize(new Dimension(150, m_charNameEntry.getPreferredSize().height));

        setUpForJoinDlg();
    }

    public void setUpForHostDlg()
    {
        m_hostPanelLayout.show(m_hostPanel, "host");
        setTitle("Host a game");
    }

    public void setUpForJoinDlg()
    {
        m_hostPanelLayout.show(m_hostPanel, "join");
        setTitle("Join a game");
    }
    
    private NetworkParametersPanel m_networkPanel = null; 
    private final GameTableCore m_core = GameTableCore.getCore();
}
