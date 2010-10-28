/*
 * DnDDialog.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.*;

import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.Group;
import com.galactanet.gametable.data.GroupManager;
import com.galactanet.gametable.util.Log;

/** **********************************************************************************************
 * DnD Dialog for quick edits of DnD Attributes
 */
public class GroupingDialog extends JDialog implements FocusListener
{
    
    private static final long serialVersionUID = 7635834193550405597L;

    private boolean         bAccepted;
    private final JButton   b_cancel    = new JButton();
    private final JButton   b_ok        = new JButton();
    
    
    private JLabel          label       = new JLabel("Select Group ");        
    private final JComboBox m_groups      = new JComboBox();
    private JTextField      newGroup    = new JTextField(20);
    private JLabel          nlabel      = new JLabel("New Group "); 
    private boolean m_newGroupMode = false;

    public GroupingDialog(final boolean newGroupMode) {    
        try {        	 
            initialize(newGroupMode);
        }
        catch (final Exception e) {
            Log.log(Log.SYS, e);
        }

        // pack yourself
        pack();
        // center yourself
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension frameSize = getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        
    }

    /** **********************************************************************************************
     * 
     */
    public void focusGained(final FocusEvent e) {
        // only interested in JTextFields
        if (!(e.getSource() instanceof JTextField))
        {
            return;
        }

        final JTextField focused = (JTextField)e.getSource();
        focused.setSelectionStart(0);
        focused.setSelectionEnd(focused.getText().length());
    }

    /** **********************************************************************************************
     * 
     */
    public void focusLost(final FocusEvent e) {
    }

    /** **********************************************************************************************
     * 
     */
    private void initialize(final boolean newGroupMode) {
    	m_newGroupMode = newGroupMode;
        setTitle("Select Group");
        setResizable(false);

        b_ok.setText("Ok");
        b_ok.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                bAccepted = true;
                dispose();
            }
        });

        b_cancel.setText("Cancel");
        b_cancel.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                dispose();
            }
        });

        final int PADDING = 5;

        final Box outmostBox = Box.createHorizontalBox();
        getContentPane().add(outmostBox, BorderLayout.CENTER);
        outmostBox.add(Box.createHorizontalStrut(PADDING));
        final Box outerBox = Box.createVerticalBox();
        outmostBox.add(outerBox);
        outmostBox.add(Box.createHorizontalStrut(PADDING));
        outerBox.add(Box.createVerticalStrut(PADDING));        
       
       
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if(newGroupMode) {
            panel.add(nlabel);
            outerBox.add(Box.createHorizontalStrut(PADDING));
            panel.add(newGroup);            
            outerBox.add(panel);
            outerBox.add(Box.createVerticalStrut(PADDING));

            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        }
        panel.add(label);
        outerBox.add(Box.createHorizontalStrut(PADDING));
        panel.add(m_groups);            
        outerBox.add(panel);
        outerBox.add(Box.createVerticalStrut(PADDING));
        
        panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        outerBox.add(panel);
        panel.add(b_ok);
        panel.add(Box.createHorizontalStrut(PADDING));
        panel.add(b_cancel);        
        outerBox.add(Box.createVerticalStrut(PADDING));
        
        List<String> groupNames = GameTableCore.getCore().getGroupManager(GameTableCore.MapType.ACTIVE).getGroupNames(null);
        
        for (String groupName : groupNames)
            m_groups.addItem(groupName);

        
        m_groups.setMinimumSize(new Dimension(10,20));
        setModal(true);
    }

 
    /** **********************************************************************************************
     * 
     * @return
     */
    public boolean isAccepted() {
        return bAccepted;
    }
    
    /** **********************************************************************************************
     * 
     * @return
     */
    public Group getGroup() {
      String n = newGroup.getText();
      GroupManager manager = GameTableCore.getCore().getGroupManager(GameTableCore.MapType.ACTIVE);
      if((n == null) || (n.length() == 0)) return manager.getGroup((String)m_groups.getSelectedItem(), m_newGroupMode);
      return manager.getGroup(n, m_newGroupMode);
    }
}
