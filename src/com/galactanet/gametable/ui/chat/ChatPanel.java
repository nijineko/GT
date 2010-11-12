/*
 * ChatPanel.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui.chat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.ChatEngineIF;
import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.ui.FloatingWindow;
import com.galactanet.gametable.ui.GametableFrame;

/**
 * #GT-COMMENT
 * 
 * @author Rizban
 * 
 * #GT-AUDIT ChatPanel
 */
public class ChatPanel extends JPanel implements ChatEngineIF
{
    private FloatingWindow          m_floatWindow           = null;
    private boolean                 m_docked               = true;

    private boolean                 m_useMechanicsLog      = false;
    public String                   m_lastPrivateMessageSender;     // the name of the last person who sent a private message
    private final ChatLogPane       m_mechanicsLog         = new ChatLogPane(this, 1, false); // 1 = not default (for now)
    private final ChatLogPane       m_chatLog              = new ChatLogPane(this, 0, false); // 0 = default chat log
    private final JSplitPane        m_chatSplitPane        = new JSplitPane();    // The chat pane is really a split between the chat and mechanics

    private final ChatLogEntryPane  m_textEntry;
    private final JPanel            m_textAreaPanel        = new JPanel();
    private final JPanel            m_textAndEntryPanel    = new JPanel();

    // List of players to whom to send a private message
    private final JComboBox         pmSendTo               = new JComboBox();
//    private int                     pmToID                 = 0;
    
    private final static String    ALERT_MESSAGE_FONT       = "<b><font color=\"#FF0000\">";
    private final static String    END_ALERT_MESSAGE_FONT   = "</b></font>";

    private final static String    SYSTEM_MESSAGE_FONT      = "<font color=\"#666600\">";
    private final static String    END_SYSTEM_MESSAGE_FONT  = "</font>";

    public final static int       NETSTATE_HOST            = 1;
    public final static int       NETSTATE_JOINED          = 2;
    public final static int       NETSTATE_NONE            = 0;
    
    private final GametableFrame m_frame;

    public ChatPanel(GametableFrame frame)
    {
  		m_frame = frame;
  		m_textEntry = new ChatLogEntryPane(m_frame);
	
      initialize();
    }
    
    private void initialize()
    {
        this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        this.setLayout(new BorderLayout());

        // Configure chat typing panel
        m_textAndEntryPanel.setLayout(new BorderLayout());

        final JPanel entryPanel = new JPanel(new BorderLayout(0, 0));
        entryPanel.add(new StyledEntryToolbar(m_textEntry), BorderLayout.NORTH);
        entryPanel.add(m_textEntry.getComponentToAdd(), BorderLayout.SOUTH);
        m_textAndEntryPanel.add(entryPanel, BorderLayout.SOUTH);
        
        if(m_useMechanicsLog) {
            m_chatSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            m_chatSplitPane.setContinuousLayout(true);
            m_chatSplitPane.setResizeWeight(1.0);
            m_chatSplitPane.setBorder(null);        
            m_chatSplitPane.add(m_chatLog.getComponentToAdd(),JSplitPane.LEFT);
            m_chatSplitPane.add(m_mechanicsLog.getComponentToAdd(), JSplitPane.RIGHT);
            m_textAndEntryPanel.add(m_chatSplitPane, BorderLayout.CENTER);
        } else {        
            m_textAndEntryPanel.add(m_chatLog.getComponentToAdd(), BorderLayout.CENTER);
        }
        
        m_textAreaPanel.setLayout(new BorderLayout());
        m_textAreaPanel.add(m_textAndEntryPanel, BorderLayout.CENTER);
        this.add(m_textAreaPanel, BorderLayout.CENTER);
    }

    private void init_sendTo() {
        pmSendTo.removeAllItems();
        for(int i = 0;i < GametableApp.getCore().getPlayers().size(); i++) {
            final Player player = GametableApp.getCore().getPlayers().get(i);
            pmSendTo.addItem(player.getCharacterName());
        }
    }

    public JSplitPane getChatSplitPane()
    {
        return m_chatSplitPane;
    }

    public FloatingWindow getChatWindow()
    {
        return m_floatWindow;
    }

    public JComboBox getpmSendTo()
    {
        return pmSendTo;
    }

    public ChatLogEntryPane getTextEntry()
    {
        return m_textEntry;
    }

    public boolean getUseMechanicsLog()
    {
        return m_useMechanicsLog;
    }

    /**
     * Add a formatted alert message to the mechanics window
     * @param text
     */
    private void addAlertMessage(final String text)
    {
    	addMechanicsMessage(ALERT_MESSAGE_FONT + text + END_ALERT_MESSAGE_FONT);
    }

    /**
     * Add text to the chat window (no formatting)
     * @param text
     */
    private void addChatMessage(final String text)
    {
        m_chatLog.addText(text);
    }

    /**
     * Add text to the mechanics window (no formatting)
     * @param text message to show
     */
    private void addMechanicsMessage(final String text)
    {
        if (m_useMechanicsLog) 
        	m_mechanicsLog.addText(text);
        else 
        	addChatMessage(text);
    }

    /**
     * Add a formatted message to the mechanics window
     * @param text
     */
    private void addSystemMessage(final String text)
    {
        addMechanicsMessage(SYSTEM_MESSAGE_FONT + text + END_SYSTEM_MESSAGE_FONT);
    }

    public boolean openPrivChatWindowDialog()
    {
//        if (GametableFrame.getGametableFrame().getNetStatus() != NETSTATE_HOST && 
//              GametableFrame.getGametableFrame().getNetStatus() != NETSTATE_JOINED)
//        {
//            logMechanics(SYSTEM_MESSAGE_FONT + "You must be connected to open a private chat."
//                + END_SYSTEM_MESSAGE_FONT);
//            return false;
//        }
        
        final PrivateMessageDialog pmDialog = new PrivateMessageDialog(this);
        pmDialog.setLocationRelativeTo(m_frame);
        pmDialog.setVisible(true);

        if (!pmDialog.m_bAccepted)
        {
            // they canceled out
            return false;
        }
        return true;
    }

    public void setChatSplitPaneDivider (final int divLoc)
    {
        m_chatSplitPane.setDividerLocation(divLoc);
    }

    public void setUseMechanicsLog(final boolean useMechanicsLog)
    {
    	if (useMechanicsLog)
    		showMechanicsWindow();
    	else
    		hideMechanicsWindow();
    }
    
    /**
     * Show mechanics window 
     */
    private void showMechanicsWindow()
    {
    	if (m_useMechanicsLog)
    		return;
    	
      m_chatSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
      m_chatSplitPane.setContinuousLayout(true);
      m_chatSplitPane.setResizeWeight(1.0);
      m_chatSplitPane.setBorder(null);        
      m_chatSplitPane.add(m_mechanicsLog.getComponentToAdd(), JSplitPane.RIGHT);
      m_chatSplitPane.add(m_chatLog.getComponentToAdd(),JSplitPane.LEFT);
      m_textAndEntryPanel.add(m_chatSplitPane, BorderLayout.CENTER);
      
      m_textAreaPanel.add(m_textAndEntryPanel, BorderLayout.CENTER);
      this.add(m_textAreaPanel, BorderLayout.CENTER);
      
      m_useMechanicsLog = true;      

      validate();
    }
    
    /**
     * Hide the mechanics window
     */
    private void hideMechanicsWindow()
    {
    	if (!m_useMechanicsLog)
    		return;
    	
      m_textAndEntryPanel.remove(m_chatSplitPane);
      m_textAndEntryPanel.validate();

      m_textAndEntryPanel.add(m_chatLog.getComponentToAdd(), BorderLayout.CENTER);
      
      m_textAreaPanel.add(m_textAndEntryPanel, BorderLayout.CENTER);
      this.add(m_textAreaPanel, BorderLayout.CENTER);
      
      m_useMechanicsLog = false;

      validate();
    }
    
    public void toggleMechanicsWindow()
    {
    	setUseMechanicsLog(!m_useMechanicsLog);
    }
    
    /**
     * @return true if chat panel is currently docked
     */
    public boolean isDocked()
    {
        return m_docked;
    }
    
    /**
     * Float or dock the chat panel, depending on its status
     */
    public void toggleDockStatus()
    {
        if (m_docked)
            floatWindow();
        else
            dockWindow();
    }
 
    /**
     * Float the chat panel (if docked)
     */
    public void floatWindow()
    {
        if (!m_docked)
            return;
        
        Container p = getParent();
        
        if (m_floatWindow == null)
            m_floatWindow = new FloatingWindow("Chat Window", new Rectangle(195, 600, 800, 200), true);
        
        m_floatWindow.setPreviousParent(p, true);
        
        p.remove(this);
        
        m_floatWindow.add(this);
        m_floatWindow.setVisible(true);
        
        m_frame.validate();
        
        m_docked = false;        
    }

    /**
     * Dock the chat window (if floating)
     */
    public void dockWindow()
    {
        if (m_docked)
            return;
        
        if (m_floatWindow != null)
        {
            // Hide floating chat window
            m_floatWindow.setVisible(false);
            
            // Remove chat panel from floating window - we'll add it back to its previous parent
            m_floatWindow.remove(this);
            
            Container parent = m_floatWindow.getPreviousParent(true);
            if (parent != null && (parent instanceof JSplitPane))
            {
                ((JSplitPane)parent).add(this, JSplitPane.BOTTOM);
            }   
        }

        m_frame.validate();
        
        m_docked = true;
    }
    
    
    /*
    * @see com.galactanet.gametable.data.ChatEngineIF#clearMessages()
    */
    @Override
    public void clearMessages()
    {
    	m_chatLog.clearText();
    }
    
    /*
    * @see com.galactanet.gametable.data.ChatEngineIF#displayMessage(com.galactanet.gametable.data.ChatEngineIF.MessageType, java.lang.String)
    */
    @Override
    public void displayMessage(MessageType type, String text)
    {
    	switch (type)
    	{
    	case ALERT:
    		addAlertMessage(text);
    		break;
    		
    	case MECHANIC:
    		addMechanicsMessage(text);
    		break;
    		
    	case SYSTEM:
    		addSystemMessage(text);
    		break;
    		
    	case CHAT:
   		default:
   			addChatMessage(text);
   			break;
      	
    	}
    }
    
    /*
    * @see com.galactanet.gametable.data.ChatEngineIF#onPlayersChanged()
    */
    @Override
    public void onPlayersChanged()
    {
    	init_sendTo();
    }
}