/*
 * PriviteMessageDialog.java: GameTable is in the Public Domain.
 */


package com.gametable.ui.chat;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;

import javax.swing.*;

import com.gametable.GametableApp;
import com.gametable.data.Player;
import com.gametable.util.Log;

/**
 * #GT-COMMENT
 * 
 * #GT-AUDIT PrivateMessageDialog
 */
public class PrivateMessageDialog extends JDialog implements FocusListener
{
    JLabel                    jLabel1            = new JLabel();
    boolean                   m_bAccepted;
    JButton                   m_cancel           = new JButton();
    JComboBox                 m_pmSendTo         = new JComboBox();
    JPanel                    m_pmPanel          = new JPanel(new CardLayout(0, 0));
    public int                m_pmToID           = -1;


    JButton                   m_ok               = new JButton();
    
    private final ChatPanel m_chatPanel;

    public PrivateMessageDialog(ChatPanel chatPanel)
    {
    	m_chatPanel = chatPanel;
    	
        try
        {
            initialize();
        }
        catch (final RuntimeException e)
        {
            Log.log(Log.SYS, e);
        }
        
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
                m_bAccepted = true;

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

        jLabel1.setText("Send to Player:");
        m_pmSendTo = m_chatPanel.getpmSendTo();
            
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
        nextBox.add(jLabel1);
        nextBox.add(Box.createHorizontalStrut(5));
        nextBox.add(m_pmSendTo);

        outerBox.add(Box.createVerticalStrut(PADDING));

        outerBox.add(m_pmPanel);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        outerBox.add(panel);
        panel.add(m_ok);
        panel.add(Box.createHorizontalStrut(PADDING));
        panel.add(m_cancel);

        outerBox.add(Box.createVerticalStrut(PADDING));

        // we want to know if any of those text entry areas get focus
        m_pmSendTo.addFocusListener(this);
        m_pmSendTo.setPreferredSize(new Dimension(m_pmSendTo.getPreferredSize().width,
            m_pmSendTo.getPreferredSize().height));
        m_pmSendTo.addItemListener(new SendToListener());

        setTitle("Send Private Message");
    }

    class SendToListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            final String toName = (String)e.getItem();
            for (int i = 0; i < GametableApp.getCore().getPlayers().size(); i++)
            {
                final Player player = GametableApp.getCore().getPlayers().get(i);
                if (player.isNamed(toName))
                {
                    m_pmToID = player.getID();
                    break;
                }
            }

            if (m_pmToID == -1)
            {
                // nobody by that name is in the session
//                logAlertMessage("There is no player or character named \"" + toName + "\" in the session.");
                return;
            }
        }
    }
}
