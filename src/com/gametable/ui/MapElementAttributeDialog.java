/*
 * SetPogAttributeDialog.java: GameTable is in the Public Domain.
 */


package com.gametable.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;



/**
 * #GT-COMMENT
 * 
 * @author iffy
 * 
 * #GT-AUDIT SetPogAttributeDialog
 */
public class MapElementAttributeDialog extends JDialog
{

    /**
     * 
     */
    private static final long serialVersionUID      = 1204064083654715689L;
    private JPanel            bottomPanel           = null;
    private JButton           okButton              = null;
    private JButton           cancelButton          = null;
    private JButton           applyButton          = null;

    private JPanel            centerPanel           = null;
    private JPanel            contentPanel          = null;
    private JPanel            horizontalSpacerPanel = null;

    private JLabel            valueLabel            = null;
    private JLabel            nameLabel             = null;
    private JTextField        valueTextField        = null;
    private JTextField        nameTextField         = null;

    private boolean           confirmed             = false;
    private boolean           b_edit                = false;

    private String            value                 = null;
    private String            name                  = null;
    private Map<String,String>           toAdd                 = new HashMap<String, String>();
    
    /**
     * This is the default constructor
     */
    public MapElementAttributeDialog(final boolean edit)
    {
        super();
        b_edit = edit;
        initialize();
    }
    
    private void applyAttrib() {
        name = nameTextField.getText();
        value = valueTextField.getText();
        nameTextField.setText("");
        valueTextField.setText("");
        if((name == null) || (name.length() < 1))
            return;
        toAdd.put(name, value);
    }
       
    private JButton getApplyButton() {
        if (applyButton == null)
        {
            applyButton = new JButton();
            applyButton.setText("Add Another");
            applyButton.setSelected(false);
            applyButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(final java.awt.event.ActionEvent e)
                {
                    applyAttrib();
                    nameTextField.grabFocus();
                }
            });
        }
        return applyButton;
    }

    public Map<String, String> getAttribs() {
        return toAdd;
    }


    /**
     * This method initializes bottomPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getBottomPanel()
    {
        if (bottomPanel == null)
        {
            final FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.RIGHT);
            bottomPanel = new JPanel();
            bottomPanel.setLayout(flowLayout);
            bottomPanel.add(getOkButton(), null);
            bottomPanel.add(getCancelButton(), null);
            if(!b_edit) bottomPanel.add(getApplyButton());
        }
        return bottomPanel;
    }

    /**
     * This method initializes cancelButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getCancelButton()
    {
        if (cancelButton == null)
        {
            cancelButton = new JButton();
            cancelButton.setText("Cancel");
            cancelButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(final java.awt.event.ActionEvent e)
                {
                    dispose();
                }
            });
        }
        return cancelButton;
    }

    /**
     * This method initializes centerPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getCenterPanel()
    {
        if (centerPanel == null)
        {
            final GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = 1;
            final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.gridy = 3;
            valueLabel = new JLabel();
            valueLabel.setText("Value: ");
            final GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.gridy = 0;
            nameLabel = new JLabel();
            nameLabel.setText("Name: ");
            final GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
            gridBagConstraints21.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints21.gridy = 3;
            gridBagConstraints21.weightx = 1.0;
            gridBagConstraints21.gridx = 2;
            final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.gridx = 2;
            centerPanel = new JPanel();
            centerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
            centerPanel.setLayout(new GridBagLayout());
            centerPanel.add(getNameTextField(), gridBagConstraints1);
            centerPanel.add(getValueTextField(), gridBagConstraints21);
            centerPanel.add(nameLabel, gridBagConstraints3);
            centerPanel.add(valueLabel, gridBagConstraints4);
            centerPanel.add(getHorizontalSpacerPanel(), gridBagConstraints);
        }
        return centerPanel;
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getContentPanel()
    {
        if (contentPanel == null)
        {
            contentPanel = new JPanel();
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(getBottomPanel(), java.awt.BorderLayout.SOUTH);
            contentPanel.add(getCenterPanel(), java.awt.BorderLayout.CENTER);
        }
        return contentPanel;
    }

    /**
     * This method initializes horizontalSpacerPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getHorizontalSpacerPanel()
    {
        if (horizontalSpacerPanel == null)
        {
            horizontalSpacerPanel = new JPanel();
            horizontalSpacerPanel.setPreferredSize(new java.awt.Dimension(5, 5));
        }
        return horizontalSpacerPanel;
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return nameTextField.getText();
    }

    /**
     * This method initializes nameTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getNameTextField()
    {
        if (nameTextField == null)
        {
            nameTextField = new JTextField();
            nameTextField.setPreferredSize(new java.awt.Dimension(150, 20));
        }
        return nameTextField;
    }

    /**
     * This method initializes okButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getOkButton()
    {
        if (okButton == null)
        {
            okButton = new JButton();
            okButton.setText("Ok");
            okButton.setSelected(true);
            okButton.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(final java.awt.event.ActionEvent e)
                {
                    if(!b_edit) applyAttrib();
                    confirmed = true;
                    dispose();
                }
            });
        }
        return okButton;
    }

    /**
     * @return Returns the value.
     */
    public String getValue()
    {
        return valueTextField.getText();
    }

    /**
     * This method initializes valueTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getValueTextField()
    {
        if (valueTextField == null)
        {
            valueTextField = new JTextField();
            valueTextField.setPreferredSize(new java.awt.Dimension(150, 20));
        }
        return valueTextField;
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize()
    {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);
        setTitle("Set Pog Attribute");
        setContentPane(getContentPanel());
        pack();
    }

    /**
     * @return Whether this dialog was confirmed or not.
     */
    public boolean isConfirmed()
    {
        return confirmed;
    }

    /**
     * Loads the dialog with some existing values.
     * 
     * @param newName
     * @param newValue
     */
    public void loadValues(final String newName, final String newValue)
    {
        getNameTextField().setText(newName);
        getValueTextField().setText(newValue);
    }

    /*
     * @see java.awt.Component#setVisible(boolean)
     */
    public void setVisible(final boolean b)
    {
        if (b && (isVisible() != b))
        {
            confirmed = false;
        }

        super.setVisible(b);
    }

} // @jve:decl-index=0:visual-constraint="10,10"
