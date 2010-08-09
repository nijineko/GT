/*
 * GametableFrame.java: GameTable is in the Public Domain.
 */

package com.galactanet.gametable.ui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.Group.Action;
import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.data.deck.Card;
import com.galactanet.gametable.data.deck.CardModule;
import com.galactanet.gametable.data.deck.Deck;
import com.galactanet.gametable.data.deck.DeckData;
import com.galactanet.gametable.data.net.*;
import com.galactanet.gametable.data.prefs.PreferenceDescriptor;
import com.galactanet.gametable.data.prefs.Preferences;
import com.galactanet.gametable.module.Module;
import com.galactanet.gametable.ui.GametableCanvas.BackgroundColor;
import com.galactanet.gametable.ui.chat.ChatLogEntryPane;
import com.galactanet.gametable.ui.chat.ChatPanel;
import com.galactanet.gametable.ui.chat.SlashCommands;
import com.galactanet.gametable.util.*;
import com.maziade.tools.XMLUtils;
import com.maziade.tools.XMLUtils.XMLOutputProperties;
import com.plugins.activepogs.ActivePogsModule;
import com.plugins.dicemacro.DiceMacroModule;

/*
 * The main Gametable Frame class.
 * This class handles the display of the application objects and the response to user input.
 * The Main Content Pane contains the following control hierarchy:
 * MainContentPane:
 *  |- m_toolBar
 *  |- m_mapPogSplitPane
 *  |- m_status
 *  
 *  m_toolBar
 *  |- m_colorCombo
 *  |- buttons in the toolbar
 *  
 *  m_mapPogSplitPane
 *  |- m_pogsTabbedPane
 *  |   |- m_pogPanel
 *  |   |- m_activePogsPanel
 *  |   |- m_macroPanel
 *  |- m_mapChatSplitPlane
 *      |- m_canvasPane
 *      |   |- m_gametableCanvas
 *      |- m_chatPanel
 *          |-m_textAreaPanel
 *              |- m_textAndEntryPanel
 *              |- m_newChatLog
 *              |- entryPanel
 *                 |- StyledEntryToolbar
 *                     |- m_textEntry
 */

 /** 
 * @author sephalon
 * 
 * #GT-AUDIT GametableFrame
 */
public class GametableFrame extends JFrame implements ActionListener, MapElementRepositoryIF
{
    /**
     * This class provides a mechanism to store the active tool in the gametable canvas
     */
    class ToolButtonAbstractAction extends AbstractAction
    {
        /**
         * 
         */
        private static final long serialVersionUID = 6185807427550145052L;
       
        int                       m_id;     // Which user triggers this action
        
        /**
         * Constructor
         * @param id Id from the control triggering this action
         */
        ToolButtonAbstractAction(final int id)
        {
            m_id = id;
        }

        public void actionPerformed(final ActionEvent event)
        {
            if (getFocusOwner() instanceof JTextField)
            {
                return;     // A JTextField is not an active tool.
                            // No need to save the active tool in the gametable canvas
            }
            getGametableCanvas().setActiveTool(m_id); // In any other case, save the active tool in the gametable canvas
        }
    }

    /**
     * Action listener for tool buttons.
     */
    class ToolButtonActionListener implements ActionListener
    {
        int m_id;

        ToolButtonActionListener(final int id)
        {
            m_id = id;
        }

        /*
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(final ActionEvent e)
        {
            getGametableCanvas().setActiveTool(m_id);
        }
    }

    /**
     * Array of standard colors used
     */
    public final static Integer[] COLORS                   = {
        new Integer(new Color(0, 0, 0).getRGB()), new Integer(new Color(198, 198, 198).getRGB()),
        new Integer(new Color(0, 0, 255).getRGB()), new Integer(new Color(0, 255, 0).getRGB()),
        new Integer(new Color(0, 255, 255).getRGB()), new Integer(new Color(255, 0, 0).getRGB()),
        new Integer(new Color(255, 0, 255).getRGB()), new Integer(new Color(255, 255, 0).getRGB()),
        new Integer(new Color(255, 255, 255).getRGB()), new Integer(new Color(0, 0, 132).getRGB()),
        new Integer(new Color(0, 132, 0).getRGB()), new Integer(new Color(0, 132, 132).getRGB()),
        new Integer(new Color(132, 0, 0).getRGB()), new Integer(new Color(132, 0, 132).getRGB()),
        new Integer(new Color(132, 132, 0).getRGB()), new Integer(new Color(132, 132, 132).getRGB())
                                                           };

    /**
     * The version of the communications protocol used by this build. This needs to change whenever an incompatibility
     * arises between versions.
     */
    public final static int       COMM_VERSION             = 15;

    private final static boolean  DEBUG_FOCUS              = false;

    /**
     * Default Character name for when there is no prefs file.
     */
    private static final String   DEFAULT_CHARACTER_NAME   = "Anonymous";
    /**
     * Default password for when there is no prefs file.
     */
    private static final String   DEFAULT_PASSWORD         = "";

    public final static int       DEFAULT_PORT             = 6812;
    /**
     * Default server for when there is no prefs file.
     */
    private static final String   DEFAULT_SERVER           = "localhost";

    /**
     * Xml strings with a font definitions, standard tags for messages
     */
    public final static String    ALERT_MESSAGE_FONT       = "<b><font color=\"#FF0000\">";
    public final static String    END_ALERT_MESSAGE_FONT   = "</b></font>";

    public final static String    DIEROLL_MESSAGE_FONT     = "<b><font color=\"#990022\">";
    public final static String    END_DIEROLL_MESSAGE_FONT = "</b></font>";

    public final static String    EMOTE_MESSAGE_FONT       = "<font color=\"#004477\">";
    public final static String    END_EMOTE_MESSAGE_FONT   = "</font>";

    public final static String    PRIVATE_MESSAGE_FONT     = "<font color=\"#009900\">";
    public final static String    END_PRIVATE_MESSAGE_FONT = "</font>";

    public final static String    SAY_MESSAGE_FONT         = "<font color=\"#007744\">";
    public final static String    END_SAY_MESSAGE_FONT     = "</font>";

    public final static String    SYSTEM_MESSAGE_FONT      = "<font color=\"#666600\">";
    public final static String    END_SYSTEM_MESSAGE_FONT  = "</font>";

    /**
     * The global gametable instance.
     */
    private static GametableFrame g_gametableFrame;
    
    /**
     * Constants for Net status
     */
    public enum NetStatus {DISCONNECTED, CONNECTED, HOSTING; }

    public final static int       PING_INTERVAL            = 2500;

    public final static int       REJECT_INVALID_PASSWORD  = 0;
    public final static int       REJECT_VERSION_MISMATCH  = 1;

    private final static boolean  SEND_PINGS               = true;
//    private final static boolean  USE_NEW_CHAT_PANE        = true;

    /**
     * 
     */
    private static final long     serialVersionUID         = -1997597054204909759L;

    /**
     * TODO #Plugins Might be nice if it returned an interface to clean up API for plugins
     * @return The global GametableFrame instance.
     */
    public static GametableFrame getGametableFrame()
    {
        return g_gametableFrame;
    }

    // Language variables
    private Language                 m_languageResource                    = new Language(GametableApp.LANGUAGE);
    
    public double                   grid_multiplier         = 5.0;
    public String                   grid_unit               = "ft";

    // The current file path used by save and open.
    // NULL if unset.
    private static File             m_mapExportSaveFolder   = new File("./saves");

    public File                     m_actingFilePublic;

    
    private boolean                 m_bMaximized;   // Is the frame maximized?

    // all the cards you have
    private final List<Card>              m_cards                  = new ArrayList<Card>();
    public String                   m_characterName          = DEFAULT_CHARACTER_NAME; // The character name
    
    



    // only valid if this client is the host
    private final List<Deck>              m_decks                  = new ArrayList<Deck>(); // List of decks

    private JMenuItem               m_disconnectMenuItem;

    public Color                    m_drawColor              = Color.BLACK;

    private PeriodicExecutorThread  m_executorThread;
 
    JComboBox                       m_gridunit;             // ComboBox for grid units
    /*
     * Added variables below in order to accomodate grid unit multiplier
     */
    JTextField                      m_gridunitmultiplier;
    private final JCheckBoxMenuItem m_hexGridModeMenuItem    = new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_HEX);

    private JMenuItem               m_hostMenuItem;
    public String                   m_ipAddress              = DEFAULT_SERVER;
    private JMenuItem               m_joinMenuItem;
    private long                    m_lastPingTime           = 0;

    private long                    m_lastTickTime           = 0;

    // which player I am
    private int                     m_myPlayerIndex;

    private NetStatus                     m_netStatus  = NetStatus.DISCONNECTED;

    private volatile NetworkThread  m_networkThread;

    // the id that will be assigned to the next player to join
    public int                      m_nextPlayerId;

    // the id that will be assigned to the change made
    public int                      m_nextStateId;
    private final JCheckBoxMenuItem m_noGridModeMenuItem     = new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_NONE);

    public String                   m_password               = DEFAULT_PASSWORD;
    public String                   m_playerName             = System.getProperty("user.name");

    private List<Player>                    m_players                = new ArrayList<Player>();

    private JFrame                  pogWindow                = null;
//    private boolean                 b_pogWindowDocked        = true;
//    private boolean                 b_chatWindowDocked       = true;

    private MapElementTypeLibrary              m_pogLibrary             = null;
    
    

    public int                      m_port                   = DEFAULT_PORT;
    private final Preferences       m_preferences            = new Preferences();

    private final JCheckBox         m_showNamesCheckbox      = new JCheckBox(getLanguageResource().SHOW_POG_NAMES);
    private final JCheckBox         m_randomRotate           = new JCheckBox(getLanguageResource().RANDOM_ROTATE);   //#randomrotate
    private final JCheckBoxMenuItem m_squareGridModeMenuItem = new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_SQUARE);
    

    
 
 
    private JCheckBoxMenuItem       m_togglePrivateMapMenuItem;

    

    private final ButtonGroup       m_toolButtonGroup        = new ButtonGroup();

    private JToggleButton           m_toolButtons[]          = null;

    private final ToolManager       m_toolManager               = new ToolManager();
    private final List<String>      m_typingPlayerNames         = new ArrayList<String>();
    // window size and position
    private Point                   m_windowPos;
    private Dimension               m_windowSize;

    // Controls in the Frame
    // The toolbar goes at the top of the pane
    private final JToolBar          m_toolBar                = new JToolBar(); // The main toolbar
    private final JComboBox         m_colorCombo             = new JComboBox(COLORS); // Combo box for colore

    // The map-pog split pane goes in the center
    private final JSplitPane        m_mapPogSplitPane        = new JSplitPane();    // Split between Pog pane and map pane
    private final PogWindow         m_pogsTabbedPane         = new PogWindow();   // The Pog pane is tabbed
    private PogPanel                m_pogPanel               = null;                // one tab is the Pog Panel

    private final JSplitPane        m_mapChatSplitPane       = new JSplitPane();    // The map pane is really a split between the map and the chat
                                                                                    // and the chat pane
    private final JPanel            m_canvasPane             = new JPanel(new BorderLayout()); // This is the pane containing the map
    private final GametableCanvas   m_gametableCanvas        = new GametableCanvas(); // This is the map
    private ChatPanel               m_chatPanel              = null; // Panel for chat
    private ChatLogEntryPane        m_textEntry              = null;
    
    // The status goes at the bottom of the pane
    private final JLabel            m_status                 = new JLabel(" "); // Status Bar

    /**
     * Construct the frame
     */
    public GametableFrame()
    {
        g_gametableFrame = this;

        try
        {
            initialize(); // Create the menu, controls, etc.
        }
        catch (final Exception e)
        {
            Log.log(Log.SYS, e);
        }
    }

    /**
     * actionPerformed is an event handler for some of the controls in the frame
     */
    public void actionPerformed(final ActionEvent e)
    {
        /*
         * Added in order to accomodate grid unit multiplier
         */
        if (e.getSource() == m_gridunit)
        {
            // If the event is triggered by the grid unit drop down,
            // get the selected unit
            grid_unit = (String)(m_gridunit.getSelectedItem());
        }

        if (e.getSource() == m_colorCombo)
        {
            // If the event is triggered by the color drow down,
            // Get the selected color
            final Integer col = (Integer)m_colorCombo.getSelectedItem();
            m_drawColor = new Color(col.intValue());
        }
        else if (e.getSource() == m_noGridModeMenuItem)
        {
            // If the event is triggered by the "No Grid Mode" menu item then
            // remove the grid from the canvas
            // Set the Gametable canvas in "No Grid" mode
            getGametableCanvas().m_gridMode = getGametableCanvas().m_noGridMode;
            send(PacketManager.makeGridModePacket(GametableCanvas.GRID_MODE_NONE));
            // Check an uncheck menu items
            updateGridModeMenu();
            // Repaint the canvas
            getGametableCanvas().repaint();
            // Notify other players
            postSystemMessage(getMyPlayer().getPlayerName() + getLanguageResource().MAP_GRID_CHANGE);
        }
        else if (e.getSource() == m_squareGridModeMenuItem)
        {
            // If the event is triggered by the "Square Grid Mode" menu item, 
            // adjust the canvas accordingly
            // Set the Gametable canvas in "Square Grid mode"
            getGametableCanvas().m_gridMode = getGametableCanvas().m_squareGridMode;
            send(PacketManager.makeGridModePacket(GametableCanvas.GRID_MODE_SQUARES));
            // Check and uncheck menu items
            updateGridModeMenu();
            // Repaint the canvas
            getGametableCanvas().repaint();
            // Notify other players
            postSystemMessage(getMyPlayer().getPlayerName() + getLanguageResource().MAP_GRID_CHANGE);
        }
        else if (e.getSource() == m_hexGridModeMenuItem)
        {
            // If the event is triggered by the "Hex Grid Mode" menu item,
            // adjust the canvas accordingly
            // Set the Gametable canvas in "Hex Grid Mode"
            getGametableCanvas().m_gridMode = getGametableCanvas().m_hexGridMode;
            send(PacketManager.makeGridModePacket(GametableCanvas.GRID_MODE_HEX));
            // Check and uncheck menu items
            updateGridModeMenu();
            // Repaint the canvas
            getGametableCanvas().repaint();
            // Notify other players
            postSystemMessage(getMyPlayer().getPlayerName() + getLanguageResource().MAP_GRID_CHANGE);
        }
    }


    // --- Menus ---

  


    /**
     * adds a player to the player list
     * @param player
     */
    public void addPlayer(final Player player)
    {
        m_players.add(player);
        
        for (Module module : g_modules)
        	module.onPlayerAdded(player);
        
        // TODO Chat panel as plugin
        m_chatPanel.init_sendTo();
    }

    /**
     * handles a new pog packet
     * @param pog the Pog received
     * @param bPublicLayerPog currently ignored
     */
    public void addPogPacketReceived(final MapElement pog, final boolean bPublicLayerPog)
    {
        // Check for loaded pog, or copied pog.
    	
        // getGametableCanvas().doAddPog(pog, bPublicLayerPog);
        /*
         * Changed by Rizban Changed to publish to active map rather than public map. 
         * TODO: For some reason, all saved pogs are saved with data saying they are on the 
         * public map, regardless of which map they were on when saved. Check to see if there 
         *  is a reason for saving pogs with this information, if not, remove all instances.
         */
        // add the pog to the canvas indicating if the active map is the public map
        getGametableCanvas().doAddPog(pog,
            (getGametableCanvas().getActiveMap() == getGametableCanvas().getPublicMap() ? true : false));

        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            send(PacketManager.makeAddPogPacket(pog));
        }
    }

    /**
     * Updates the frame size and position based on the preferences stored.
     */
    public void applyWindowInfo()
    {
        final Point locCopy = new Point(m_windowPos);
        setSize(m_windowSize);
        m_windowPos = locCopy; // this copy and use of a local variable seems unnecessary
        setLocation(locCopy);
        if (m_bMaximized) // if the preferences says the screen should be maximized
        {
            setExtendedState(MAXIMIZED_BOTH); // Maximize screen
        }
        else
        {
            setExtendedState(NORMAL); // Otherwise, set the screen to normal
        }
    }

    /** *************************************************************************************
     * Changes The background color of the map. Each color is a png in the jar file
     * @param color
     */
    public void changeBGPacketRec(final MapElementTypeIF type) {
        m_gametableCanvas.changeBackground(type);
        if(m_netStatus == NetStatus.HOSTING) {
            send(PacketManager.makeBGColPacket(type));
        }
    }
    
    /** *************************************************************************************
     * Changes The background color of the map. Each color is a png in the jar file
     * @param color
     */
    public void changeBGPacketRec(BackgroundColor color) {
        m_gametableCanvas.changeBackground(color);
        if(m_netStatus == NetStatus.HOSTING) {
            send(PacketManager.makeBGColPacket(color));
        }
    }
    
    /**
     * clear all cards of a given deck
     * @param deckName name of the deck whose cards will be deleted
     */
    public void clearDeck(final String deckName)
    {
        // if you're the host, send out the packet to tell everyone to
        // clear their decks. If you're a joiner, don't. Either way
        // clear out your own hand of the offending cards
        if (m_netStatus == NetStatus.HOSTING)
        {
            send(PacketManager.makeClearDeckPacket(deckName));
        }

        for (int i = 0; i < m_cards.size(); i++) //for each card
        {
            final Card card = m_cards.get(i);
            if (card.getDeckName().equals(deckName)) // if it belongs to the deck to erase
            {
                // this card has to go.
                m_cards.remove(i); // remove the card
                i--; // to keep up with the changed list
            }
        }
    }

    /**
     * grumpy function that throws an exception if we are not the host of a network game
     * @throws IllegalStateException
     */
    public void confirmHost() throws IllegalStateException
    {
        if (m_netStatus != NetStatus.HOSTING)
        {
            throw new IllegalStateException(getLanguageResource().CONFIRM_HOST_FAIL);
        }
    }

    // --- MenuItems ---
    
    /**
     * throws an exception is the current status is not NetStatus.JOINED
     * @throws IllegalStateException
     */
    public void confirmJoined() throws IllegalStateException
    {
        if (m_netStatus != NetStatus.CONNECTED)
        {
            throw new IllegalStateException("confirmJoined failure");
        }
    }

    /**
     * handles a drop of the network connection
     * @param conn network connection
     */
    public void connectionDropped(final Connection conn)
    {
        if (m_netStatus == NetStatus.CONNECTED) // if we were connected before
        {
            // we lost our connection to the host
            m_chatPanel.logAlertMessage(getLanguageResource().CONNECTION_LOST);
            disconnect(); // do any disconnection processing

            m_netStatus = NetStatus.DISCONNECTED; // change the status to reflect we are not connected
            return;
        }

        // find the player who owns that connection
        final Player dead = getPlayerFromConnection(conn);
        if (dead != null) // if we found the player
        {
            // remove this player
            m_players.remove(dead);
            sendCastInfo(); //send updated list of players
            // notify other users
            postSystemMessage(dead.getPlayerName() + getLanguageResource().CONNECTION_LEFT);
        }
        else // if we didn't find the player then the connection failed while login in
        {
            postAlertMessage(getLanguageResource().CONNECTION_REJECTED);
        }
    }

    /** 
     * Makes a Copy of the pog on the Canvas
     * @param pog
     */
    public void copyPog(final MapElement pog) {
        final MapElement nPog = new MapElement(pog);
        final boolean priv = !(getGametableCanvas().isPublicMap());

        if ((m_netStatus == NetStatus.DISCONNECTED) || priv)  {
            addPogPacketReceived(nPog, !priv);
        } else {
            send(PacketManager.makeAddPogPacket(nPog));
        }
    }
    
    /**
     * interprets and execute the deck commands
     * @param words array of words in the deck command
     */
    public void deckCommand(final String[] words)
    {
        // we need to be in a network game to issue deck commands
        // otherwise log the error and exit
        if (m_netStatus == NetStatus.DISCONNECTED)
        {
            m_chatPanel.logAlertMessage(getLanguageResource().DECK_NOT_CONNECTED);
            return;
        }

        // words[0] will be "/deck". IF it weren't we wouldn't be here.
        if (words.length < 2)
        {
            // they just said "/deck". give them the help text and return
            showDeckUsage();
            return;
        }

        final String command = words[1]; // since words[0] is the word "deck" we are interested in the
                                         // next word, that's why we take words[1]

        if (command.equals("create")) // create a new deck
        {
            if (m_netStatus != NetStatus.HOSTING) // verify that we are the host of the network game
            {
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_NOT_HOST_CREATE);
                return;
            }

            // create a new deck.
            if (words.length < 3) // we were expecting the deck name
            {
                // not enough parameters
                showDeckUsage();
                return;
            }

            final String deckFileName = words[2];
            String deckName;
            if (words.length == 3)
            {
                // they specified the deck, but not a name. So we
                // name it after the type
                deckName = deckFileName;
            }
            else
            {
                // they specified a name
                deckName = words[3];
            }

            // if the name is already in use, puke out an error
            if (getDeck(deckName) != null)
            {
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_ALREADY_EXISTS + " '" + deckName + "'.");
                return;
            }

            // create the deck stored in an xml file
            final DeckData dd = new DeckData();
            final File deckFile = new File("decks" + File.separator + deckFileName + ".xml");
            boolean result = dd.init(deckFile);

            if (!result)
            {
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_ERROR_CREATE);
                return;
            }

            // create a deck and add it
            final Deck deck = new Deck();
            deck.init(dd, 0, deckName);
            m_decks.add(deck);

            // alert all players that this deck has been created
            sendDeckList();
            postSystemMessage(getMyPlayer().getPlayerName() + " " + getLanguageResource().DECK_CREATE_SUCCESS_1 + " " + deckFileName + " " + getLanguageResource().DECK_CREATE_SUCCESS_2 + " " + deckName);

        }
        else if (command.equals("destroy")) // remove a deck
        {
            if (m_netStatus != NetStatus.HOSTING)
            {
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_NOT_HOST_DESTROY);
                return;
            }

            if (words.length < 3)
            {
                // they didn't specify a deck
                showDeckUsage();
                return;
            }

            // remove the deck named words[2]
            final String deckName = words[2];
            final int toRemoveIdx = getDeckIdx(deckName); // get the position of the deck in the deck list

            if (toRemoveIdx != -1) // if we found the deck
            {
                // we can successfully destroy the deck
                m_decks.remove(toRemoveIdx);

                // tell the players
                clearDeck(deckName);
                sendDeckList();
                postSystemMessage(getMyPlayer().getPlayerName() + getLanguageResource().DECK_DESTROY + deckName);
            }
            else
            {
                // we couldn't find a deck with that name
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_NONE + " '" + deckName + "'.");
            }
        }
        else if (command.equals("shuffle")) // shuffle the deck
        {
            if (m_netStatus != NetStatus.HOSTING) // only if you are the host
            {
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_NOT_HOST_SHUFFLE);
                return;
            }

            if (words.length < 4)
            {
                // not enough parameters
                showDeckUsage();
                return;
            }

            final String deckName = words[2];
            final String operation = words[3];

            // first get the deck
            final Deck deck = getDeck(deckName);
            if (deck == null)
            {
                // and report the error if not found
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_NONE+ " '" + deckName + "'.");
                return;
            }

            if (operation.equals("all"))
            {
                // collect and shuffle all the cards in the deck.
                clearDeck(deckName); // let the other players know about the demise of those cards
                deck.shuffleAll();
                postSystemMessage(getMyPlayer().getPlayerName() + getLanguageResource().DECK_CARDS_COLLECT_ALL_1 + deckName
                    + getLanguageResource().DECK_CARDS_COLLECT_ALL_2);
                postSystemMessage(deckName + " " + getLanguageResource().DECK_HAS + " " + deck.cardsRemaining() + " " + getLanguageResource().DECK_CARDS + ".");
            }
            else if (operation.equals("discards"))
            {
                // shuffle only the cards in the discard pile.
                deck.shuffle();
                postSystemMessage(getMyPlayer().getPlayerName() + getLanguageResource().DECK_SHUFFLE + deckName
                    + " " + getLanguageResource().DECK+ ".");
                postSystemMessage(deckName + getLanguageResource().DECK_HAS + deck.cardsRemaining() + " " + getLanguageResource().DECK_CARDS + ".");
            }
            else
            {
                // the shuffle operation is illegal
                m_chatPanel.logAlertMessage("'" + operation
                    + "' " + getLanguageResource().DECK_SHUFFLE_INVALID);
                return;
            }
        }
        else if (command.equals("draw")) // draw a card from the deck
        {
            // before chesking net status we check to see if the draw command was
            // legally done
            if (words.length < 3)
            {
                // not enough parameters
                showDeckUsage();
                return;
            }

            // ensure that desired deck exists -- this will work even if we're not the
            // host. Because we'll have "dummy" decks in place to track the names
            final String deckName = words[2];
            final Deck deck = getDeck(deckName);
            if (deck == null)
            {
                // that deck doesn't exist
                m_chatPanel.logAlertMessage(getLanguageResource().DECK_NONE + " '" + deckName + "'.");
                return;
            }

            int numToDraw = 1;
            // they optionally can specify a number of cards to draw
            if (words.length >= 4)
            {
                // numToDrawStr is never used.
                // String numToDrawStr = words[3];

                // note the number of cards to draw
                try
                {
                    numToDraw = Integer.parseInt(words[3]);
                    if (numToDraw <= 0)
                    {
                        // not allowed
                        throw new Exception();
                    }
                }
                catch (final Exception e)
                {
                    // it's ok not to specify a number of cards to draw. It's not
                    // ok to put garbage in that field
                    m_chatPanel.logAlertMessage("'" + words[3] + "' " + getLanguageResource().DECK_CARDS_INVALID_NUMBER);
                }
            }

            drawCards(deckName, numToDraw);
        }
        else if (command.equals("hand")) // this shows the cards in our hand
        {
            if (m_cards.size() == 0)
            {
                m_chatPanel.logSystemMessage(getLanguageResource().DECK_HAND_EMPTY);
                return;
            }

            
            m_chatPanel.logSystemMessage(getLanguageResource().DECK_YOU_HAVE + " " + m_cards.size() + " " + getLanguageResource().DECK_CARDS + ":");
            
            for (int i = 0; i < m_cards.size(); i++) // for each card
            {
                final int cardIdx = i + 1;
                final Card card = m_cards.get(i); // get the card
                // craft a message
                final String toPost = "" + cardIdx + ": " + card.getCardName() + " (" + card.getDeckName() + ")";
                // log the message
                m_chatPanel.logSystemMessage(toPost);
            }
        }
        else if (command.equals("discard")) // discard a card
        {
            // discard the nth card from your hand
            // 1-indexed
            if (words.length < 3)
            {
                // note enough parameters
                showDeckUsage();
                return;
            }

            final String param = words[2];

            // the parameter can be "all" or a number
            Card discards[];
            if (param.equals("all"))
            {
                // discard all cards
                discards = new Card[m_cards.size()];

                for (int i = 0; i < discards.length; i++)
                {
                    discards[i] = m_cards.get(i);
                }
            }
            else
            {
                // discard the specified card
                int idx = -1;
                try
                {
                    idx = Integer.parseInt(param);
                    idx--; // make it 0-indexed

                    if (idx < 0) // we can't discard a card with a negative index
                    {
                        throw new Exception();
                    }

                    if (idx >= m_cards.size()) // we can't discard a card higher than what we have
                    {
                        throw new Exception();
                    }
                }
                catch (final Exception e)
                {
                    // they put in some illegal value for the param
                    m_chatPanel.logAlertMessage(getLanguageResource().DECK_CARD_NONE + " '" + param + "'.");
                    return;
                }
                discards = new Card[1];
                discards[0] = m_cards.get(idx);
            }

            // now we have the discards[] filled with the cards to be
            // removed
            discardCards(discards);
        }
        else if (command.equals("decklist"))
        {
            // list off the decks
            // we keep "dummy" decks for joiners,
            // so either a host of a joiner is safe to use this code:
            if (m_decks.size() == 0)
            {
                m_chatPanel.logSystemMessage(getLanguageResource().DECK_NO_DECKS);
                return;
            }

            m_chatPanel.logSystemMessage(getLanguageResource().DECK_THERE_ARE + " " + m_decks.size() + " " + getLanguageResource().DECK_DECKS);
            for (int i = 0; i < m_decks.size(); i++)
            {
                final Deck deck = m_decks.get(i);
                m_chatPanel.logSystemMessage("---" + deck.m_name);
            }
        }
        else
        {
            // they selected a deck command that doesn't exist
            showDeckUsage();
        }
    }

    /**
     * handles the reception of a list of decks
     * @param deckNames array of string with the names of decks received
     */
    public void deckListPacketReceived(final String[] deckNames)
    {
        // if we're the host, this is a packet we should never get
        if (m_netStatus == NetStatus.HOSTING)
        {
            throw new IllegalStateException(getLanguageResource().DECK_ERROR_HOST_DECKLIST);
        }

        // set up out bogus decks to have the appropriate names
        m_decks.clear();

        for (int i = 0; i < deckNames.length; i++)
        {
            final Deck bogusDeck = new Deck();
            bogusDeck.initPlaceholderDeck(deckNames[i]);
            m_decks.add(bogusDeck);
        }
    }

    /**
     * remove cards from our deck
     * @param discards array of cards to discard
     */
    public void discardCards(final Card discards[])
    {
        if (m_netStatus == NetStatus.CONNECTED)
        {
            // if we are not the host we have bogus decks, so we send a package to
            // notify of the discards. It will be processed by the host
            send(PacketManager.makeDiscardCardsPacket(getMyPlayer().getPlayerName(), discards));
        }
        else if (m_netStatus == NetStatus.HOSTING)
        {
            // we are the host, so we can process the discard of the cards
            doDiscardCards(getMyPlayer().getPlayerName(), discards);
        }

        // and in either case, we remove the cards from ourselves.
        // @revise This is an expensive algorithm, it would be better to get to the card to remove directly by index or something like that.
        for (int i = 0; i < m_cards.size(); i++) // for each card we have
        {
            final Card handCard = m_cards.get(i); 
            for (int j = 0; j < discards.length; j++) // compare with each card to discard
            {
                if (handCard.equals(discards[j])) // if they are the same
                {
                    // we need to dump this card
                    m_cards.remove(i);
                    i--; // to keep up with the iteration
                    break;
                }
            }
        }
    }

    /**
     * disconnect from the network game, if connected
     */
    public void disconnect()
    {
        if (m_netStatus == NetStatus.DISCONNECTED)
        {
            m_chatPanel.logAlertMessage(getLanguageResource().CONNECTION_NO_DISCONNECT);
            return;
        }

        if (m_networkThread != null)
        {
            // stop the network thread
            m_networkThread.closeAllConnections();
            m_networkThread.interrupt();
            m_networkThread = null;
        }

        m_hostMenuItem.setEnabled(true); // enable the menu item to host a game
        m_joinMenuItem.setEnabled(true); // enable the menu item to join an existing game
        m_disconnectMenuItem.setEnabled(false); // disable the menu item to disconnect from the game 

        // make me the only player in the game
        m_players = new ArrayList<Player>();
        final Player me = new Player(m_playerName, m_characterName, -1);
        m_players.add(me);
        m_myPlayerIndex = 0;
        setTitle(GametableApp.VERSION);

        // we might have disconnected during initial data receipt
        PacketSourceState.endHostDump();

        m_netStatus = NetStatus.DISCONNECTED;
        m_chatPanel.logSystemMessage(getLanguageResource().DISCONNECTED);
        updateStatus();
    }

    /**
     * discards cards from a deck
     * @param playerName who is discarding the cards
     * @param discards array of cards to discard
     */
    public void doDiscardCards(final String playerName, final Card discards[])
    {
        if (discards.length == 0) // nothing to discard
        {
            // this shouldn't happen, but let's not freak out.
            return;
        }

        // only the host should get this
        if (m_netStatus != NetStatus.HOSTING)
        {
            throw new IllegalStateException(getLanguageResource().DECK_ERROR_DODISCARD);
        }

        // tell the decks about the discarded cards
        for (int i = 0; i < discards.length; i++)
        {
            final String deckName = discards[i].getDeckName();
            final Deck deck = getDeck(deckName);
            if (deck == null)
            {
                // don't panic. Just ignore it. It probably means
                // a player discarded a card right as the host deleted the deck
            }
            else
            {
                deck.discard(discards[i]);
            }
        }

        // finally, remove any card pogs that are hanging around based on these cards
        m_gametableCanvas.removeCardPogsForCards(discards);

        // tell everyone about the cards that got discarded
        if (discards.length == 1)
        {
            postSystemMessage(playerName + " " + getLanguageResource().DECK_DISCARDS + ": " + discards[0].getCardName());
        }
        else
        {
            postSystemMessage(playerName + " " + getLanguageResource().DECK_DISCARDS + " " + discards.length + " " + getLanguageResource().DECK_CARDS);
            for (int i = 0; i < discards.length; i++)
            {
                postSystemMessage("---" + discards[i].getCardName());
            }
        }
    }

    /**
     * draw cards from a deck. Non-host players request it from the host. The
     * host is the one that actually draws from the deck
     * @param deckName deck to draw cards from
     * @param numToDraw how many cards are requested
     */
    public void drawCards(final String deckName, final int numToDraw)
    {
        if (m_netStatus == NetStatus.CONNECTED)
        {
            // joiners send a request for cards
            send(PacketManager.makeRequestCardsPacket(deckName, numToDraw));
            return;
        }

        // if we're here, we're the host. So we simply draw the cards
        // and give it to ourselves.
        final Card drawnCards[] = getCards(deckName, numToDraw);
        if (drawnCards != null)
        {
            receiveCards(drawnCards);

            // also, we need to add the desired cards to our own private layer
            for (int i = 0; i < drawnCards.length; i++)
            {
                final MapElement cardPog = makeCardPog(drawnCards[i]);
                if (cardPog != null)
                {
                    // add this pog card to our own private layer
                    m_gametableCanvas.addCardPog(cardPog);
                }
            }
        }
    }

    /**
     * erases everything from the game canvas
     */
    public void eraseAll()
    {
        eraseAllLines();
        eraseAllPogs();
    }

    /**
     * erases the canvas
     */
    public void eraseAllLines()
    {
        // erase with a rect big enough to nail everything
        final MapRectangle toErase = new MapRectangle(
        		new MapCoordinates(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2),
        		Integer.MAX_VALUE, Integer.MAX_VALUE 
        		);

        // go to town
        getGametableCanvas().erase(toErase, false, 0);

        repaint();
    }

    /**
     * erases all pogs, also clearing the array of active pogs
     * @revise move to MODEL.
     */
    public void eraseAllPogs()
    {
      // make an int array of all the IDs
    	List<MapElement> pogs = getGametableCanvas().getActiveMap().getMapElements();
      getGametableCanvas().removePogs(pogs, true);
    }

    /**
     * handles a packet to erase part of the canvas
     * @param r the area to erase
     * @param bColorSpecific erase it by painting it on a color
     * @param color color to erase the area
     * @param authorID who request the erase operation
     * @param state 
     */
    public void erasePacketReceived(final MapRectangle r, final boolean bColorSpecific, final int color,
        final int authorID, final int state)
    {
        int stateId = state;
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            // and give it a genuine state ID first
            stateId = getNewStateId();
            send(PacketManager.makeErasePacket(r, bColorSpecific, color, authorID, stateId));
        }

        // erase the lines
        getGametableCanvas().doErase(r, bColorSpecific, color, authorID, stateId);
    }


    /**
     * creates the "About" menu item
     * @return the menu item
     */
    private JMenuItem getAboutMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().ABOUT); // creates a menu item with the "About" label
        item.setAccelerator(KeyStroke.getKeyStroke("F1")); // assign a shortcut
        item.addActionListener(new ActionListener() // when the user selects it this is what happens
        {
            public void actionPerformed(final ActionEvent e)
            {
                // show the about message
                UtilityFunctions.msgBox(GametableFrame.this, GametableApp.VERSION
                    + " " + getLanguageResource().ABOUT2 + "\n"
                    + getLanguageResource().ABOUT3, getLanguageResource().VERSION);
            }
        });
        return item;
    }


    /**
     * get a number of cards from a deck
     * @param deckName name of the deck to draw from
     * @param num number of cards to draw
     * @return array with the cards drawn
     */
    public Card[] getCards(final String deckName, final int num)
    {
        int numCards = num;

        // get the deck
        final Deck deck = getDeck(deckName);
        if (deck == null)
        {
            // the deck doesn't exist. There are various ways this could happen,
            // mostly due to split-second race conditions where the host deletes the deck while
            // a card request was incoming. We just return null in this edge case.
            return null;
        }

        if (numCards <= 0)
        {
            // invalid
            throw new IllegalArgumentException("drawCards: " + numCards);
        }

        // We can't draw more cards than there are
        final int remain = deck.cardsRemaining();
        if (numCards > remain)
        {
            numCards = remain;
        }

        // make the return value
        final Card ret[] = new Card[numCards];

        // draw the cards
        for (int i = 0; i < numCards; i++)
        {
            ret[i] = deck.drawCard();
        }

        // now that the cards are drawn, check the deck status
        if (deck.cardsRemaining() == 0)
        {
            // no more cards in the deck, alert them
            postSystemMessage(deckName + " " + getLanguageResource().DECK_OUT_OF_CARDS);
        }

        return ret;
    }

    public ChatPanel getChatPanel()
    {
        return m_chatPanel;
    }

    /**
     * creates the "Clear Map" menu item
     * @return the new menu item
     */
    private JMenuItem getClearMapMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().MAP_CLEAR);
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                // confirm the erase operation
                final int res = UtilityFunctions.yesNoDialog(GametableFrame.this,
                    getLanguageResource().MAP_CLEAR_WARNING, getLanguageResource().MAP_CLEAR);
                if (res == UtilityFunctions.YES)
                {
                    eraseAllPogs();
                    eraseAllLines();
                    repaint();
                }
            }
        });

        return item;
    }

    /**
     * gets a deck by name or null if it doesn't exist
     * @param name name of the deck to get
     * @return the requested deck or null if it doesn't exist
     */
    public Deck getDeck(final String name)
    {
        final int idx = getDeckIdx(name);
        if (idx == -1)
        {
            return null;
        }
        // Doesn't get here if idx == -1; no need for else
        // else
        {
            final Deck d = m_decks.get(idx);
            return d;
        }
    }

    /**
     * gets the position in the list of decks of a deck with a given name or -1 if not found
     * @param name name of the deck to locate
     * @return the position of the deck in the list or -1 if not found
     */
    public int getDeckIdx(final String name)
    {
        for (int i = 0; i < m_decks.size(); i++)
        {
            final Deck d = m_decks.get(i);
            if (d.m_name.equals(name))
            {
                return i;
            }
        }
        return -1;
    }

    
    /** 
     * creates the "Modules" menu 
     * @return the new menu
     */
    private JMenu getModulesMenu()
    {
    	JMenu modulesMenu = null;
    	
    	for (Module module : g_modules)
    	{
    		JMenu menu = module.getModuleMenu();
    		if (menu != null)
    		{
    			if (modulesMenu == null)
    				modulesMenu = new JMenu("Modules");
    			
    			modulesMenu.add(menu);
    		}
    	}
    	
    	return modulesMenu;
    }

    /**
     * creates the "Disconnect" menu item
     * @return the new menu item
     */
    private JMenuItem getDisconnectMenuItem()
    {
        if (m_disconnectMenuItem == null)
        {
            final JMenuItem item = new JMenuItem(getLanguageResource().DISCONNECT);
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    disconnect();
                }
            });
            m_disconnectMenuItem = item;
        }
        return m_disconnectMenuItem;
    }

    /**
     * Builds and returns the File Menu
     * @return the file menu just built
     */
    private JMenu getFileMenu()
    {
    	// todo Add File->New
    	
      final JMenu menu = new JMenu(getLanguageResource().FILE);

  		JMenuItem item = new JMenuItem(m_actionLoadMap);			
      item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
      menu.add(item);
      
      item = new JMenuItem(m_actionLoadPublicMap);			
      //item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
      menu.add(item);
      
      item = new JMenuItem(m_actionLoadPrivateMap);			
      //item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
      menu.add(item);
      
      menu.add(getSaveMapMenuItem());
      menu.add(getSaveAsMapMenuItem());
      menu.add(getScanForPogsMenuItem());
      menu.add(getQuitMenuItem());

      return menu;
    }

    /**
     * @return Returns the gametableCanvas.
     */
    public GametableCanvas getGametableCanvas()
    {
        return m_gametableCanvas;
    }

    /**
     * creates the "Grid Mode" menu
     * @return the new menu
     */
    private JMenu getGridModeMenu()
    {
        final JMenu menu = new JMenu(getLanguageResource().MAP_GRID_MODE);
        menu.add(m_noGridModeMenuItem);
        menu.add(m_squareGridModeMenuItem);
        menu.add(m_hexGridModeMenuItem);

        return menu;
    }

    /**
     * creates the "Help" menu
     * @return the new menu
     */
    private JMenu getHelpMenu()
    {
        final JMenu menu = new JMenu(getLanguageResource().HELP);
        menu.add(getAboutMenuItem());
        
        /*
        menu.add(new JMenuItem(new AbstractAction("ISHOST") {					
					@Override
					public void actionPerformed(ActionEvent e)
					{
						Player p = getMyPlayer();
						System.out.println(p.isHostPlayer() ? "host" : "guest");
						System.out.println(p.getConnection() == null ? "disconnected" : "connected");
					}
        }));
        */
        
        return menu;
    }

    /**
     * creates the "host" menu item
     * @return the new menu item
     */
    private JMenuItem getHostMenuItem()
    {
        if (m_hostMenuItem == null)
        {
            final JMenuItem item = new JMenuItem(getLanguageResource().HOST);
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    host(); // this displays the dialog to host a game
                }
            });

            m_hostMenuItem = item;
        }
        return m_hostMenuItem;
    }

    /**
     * creates the "Join" menu item
     * @return the new menu item
     */
    private JMenuItem getJoinMenuItem()
    {
        if (m_joinMenuItem == null)
        {
            final JMenuItem item = new JMenuItem(getLanguageResource().JOIN);
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    join(); // this displays the dialog to join a game
                }
            });
            m_joinMenuItem = item;
        }
        return m_joinMenuItem;
    }

    /**
     * creates the "list players" menu item
     * @return the new menu item
     */
    private JMenuItem getListPlayersMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().LIST_PLAYERS);
        item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " W"));
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {                
                SlashCommands.parseSlashCommand("/who"); // selecting this menu item is the same as issuing the command /who
            }
        });
        return item;
    }

    private JMenu getWindowMenu()
    {
        final JMenu menu = new JMenu(getLanguageResource().WINDOW);

        menu.add(getPogWindowMenuItem());
        menu.add(getChatWindowMenuItem());
        menu.addSeparator();
        menu.add(getMechanicsToggle());
//        menu.add(getPrivChatWindowMenuItem());

        return menu;
    }


    /**
     * Builds and returns the main menu bar
     * @return The menu bar just built
     */
    private JMenuBar getMainMenuBar()
    {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(getFileMenu());
        menuBar.add(getNetworkMenu());
        menuBar.add(getMapMenu());
        menuBar.add(getGroupingMenu()); //#grouping
        
        JMenu menu = getModulesMenu();
        if (menu != null)
        	menuBar.add(menu);
        
        menuBar.add(getWindowMenu());
        menuBar.add(getHelpMenu());

        return menuBar;
    }

    /**
     * bulds and returns the "Map" menu
     * @return the menu just built
     */
    private JMenu getMapMenu()
    {
        final JMenu menu = new JMenu(getLanguageResource().MAP);
        menu.add(getClearMapMenuItem());
        menu.add(getRecenterAllPlayersMenuItem());
        menu.add(getGridModeMenu());
        menu.add(getTogglePrivateMapMenuItem());
        menu.add(getExportMapMenuItem());
        JMenu cbgitem = new JMenu(getLanguageResource().MAP_BG_CHANGE);
        
        for (BackgroundColor color : BackgroundColor.values())
        	cbgitem.add(getChangeBGMenuItem(color));
        
        menu.add(cbgitem);
        menu.addSeparator();
        menu.add(getLockMenuItem(true));
        menu.add(getLockMenuItem(false));
        menu.addSeparator();
        menu.add(getRemoveSelectedMenuItem());
        menu.add(getUnPublishSelectedMenuItem(false));
        menu.add(getUnPublishSelectedMenuItem(true)); 
        menu.add(getLoadPogMenuItem());
        return menu;
    }

    /**
     * Get the "Export Map" menu item
     * @return JMenuItem
     */
    private JMenuItem getExportMapMenuItem()
    {
        JMenuItem item = new JMenuItem(getLanguageResource().MAP_EXPORT);
        item.addActionListener(new ActionListener() {
           /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(ActionEvent e)
            {
              exportMap();
            }
        });

      return item;
    }

    private JMenuItem getLoadPogMenuItem() {
        JMenuItem item = new JMenuItem(getLanguageResource().POG_LOAD);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
              loadPog();
            }
        });

      return item; 
    }
    
    private JMenuItem getLockMenuItem(final boolean lock)
    {
        String str;
        if(lock) str = getLanguageResource().MAP_LOCK_ALL;
        else str = getLanguageResource().MAP_UNLOCK_ALL;
        final JMenuItem item = new JMenuItem(str);
        item.addActionListener(new ActionListener() {           
            public void actionPerformed(ActionEvent e)
            {
               doLockMap(lock);
            }
        });

      return item;
    }

    /**
     * Export map to JPeg file
     */
    private void exportMap()
    {
        File out = getMapExportFile();
        if (out == null)
            return;

        try
        {
            m_gametableCanvas.exportMap(null, out);
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, e.getMessage(), getLanguageResource().MAP_SAVE_IMG_FAIL, JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Ask the user to choose a file name for the exported map
     * @return File object or null if the user did not choose
     */
    private File getMapExportFile()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getLanguageResource().MAP_SAVE_IMG);  // no external resource for text
        if (m_mapExportSaveFolder != null)
        {
            chooser.setSelectedFile(m_mapExportSaveFolder);
        }

        FileFilter filter = new FileFilter()
        {
            /*
             * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
             */
            public boolean accept(File f)
            {
                String fileName = f.getPath().toLowerCase();
                return f.isDirectory() || fileName.endsWith(".jpeg") || fileName.endsWith(".jpg");
            }

            /*
             * @see javax.swing.filechooser.FileFilter#getDescription()
             */
            public String getDescription()
            {
                return "JPeg Files (*.jpg, *.jpeg)";
            }
        };

        chooser.setFileFilter(filter);

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return null;
        }

        // Add extension to file name if user did not do so
        File out = chooser.getSelectedFile();
        String fileName = out.getAbsolutePath().toLowerCase();

        if (!(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")))
        {
            out = new File(out.getAbsolutePath() + ".jpg");
        }

        // If file exists, confirm before overwrite
        if (out.exists())
        {
            if (JOptionPane.showConfirmDialog(this,
                getLanguageResource().MAP_SAVE_OVERWRITE + " " + out.getName() + "?",
                getLanguageResource().MAP_SAVE_EXISTS,
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            {
                return null;
            }
        }

        if (out.exists() && !out.canWrite())
        {
            JOptionPane.showMessageDialog(this,
                getLanguageResource().MAP_SAVE_NO_ACCESS+ " " + out.getName(),
                getLanguageResource().MAP_SAVE_FILE_FAIL,
                JOptionPane.ERROR_MESSAGE);

            return null;
        }

        // Figure out path name and keep it for next open of dialog
        String pathName = out.getAbsolutePath();
        int idx = pathName.lastIndexOf(File.separator);
        if (idx > -1)
        {
            pathName = pathName.substring(0, idx) + File.separator + ".";
        }

        m_mapExportSaveFolder = new File(pathName);

        return out;
    }
    
    /**
     * @return The player representing this client.
     */
    public Player getMyPlayer()
    {
    	if (m_players.size() == 0)
    		return null;
    	
      return m_players.get(getMyPlayerIndex());
    }

    /**
     * @return The id of the player representing this client.
     */
    public int getMyPlayerId()
    {
    	Player p = getMyPlayer();
    	if (p == null)
    		return 0;
    	
      return p.getId();
    }

    /**
     * @return Returns the myPlayerIndex.
     */
    public int getMyPlayerIndex()
    {
        return m_myPlayerIndex;
    }

    /**
     * @return Returns the m_netStatus.
     */
    public NetStatus getNetStatus()
    {
        return m_netStatus;
    }

    /**
     * builds and return the "Network" menu
     * @return the newly built menu
     */
    private JMenu getNetworkMenu()
    {
        final JMenu menu = new JMenu(getLanguageResource().NETWORK);
        menu.add(getListPlayersMenuItem());
        menu.add(getHostMenuItem());
        menu.add(getJoinMenuItem());
        menu.add(getDisconnectMenuItem());

        return menu;
    }

    /**
     * gets and id for a state
     * 
     * @return the next id number
     */
    public int getNewStateId()
    {
        return m_nextStateId++;
    }	

    /**
     * Load map
     * @param loadPublic Load public map from file
     * @param loadPrivate Load private map from file
     */
		private void loadMap(boolean loadPublic, boolean loadPrivate)
		{
			final File openFile = UtilityFunctions.doFileOpenDialog(getLanguageResource().OPEN, "xml", true);
		
			if (openFile != null)
			{
				loadFromXML(openFile, loadPublic, loadPrivate);
				
				if (m_netStatus == NetStatus.HOSTING)
				{
					// Send data to other connected players (host only)
					send(PacketManager.makePublicMapPacket(m_gametableCanvas.getPublicMap()));
				}	
			}
		}

    /**
     * gets the player associated with a connection
     * @param conn connection to use to find the player
     * @return the player object associated with the connection
     */
    public Player getPlayerFromConnection(final Connection conn)
    {
        for (int i = 0; i < m_players.size(); i++)
        {
            final Player plr = m_players.get(i);
            if (conn == plr.getConnection())
            {
                return plr;
            }
        }

        return null;
    }

    /**
     * Finds the index of a given player.
     * 
     * @param player Player to find index of.
     * @return Index of the given player, or -1.
     */
    public int getPlayerIndex(final Player player)
    {
        return m_players.indexOf(player);
    }

    /**
     * @return Returns the player list.
     */
    public List<Player> getPlayers()
    {
        return Collections.unmodifiableList(m_players);
    }
    
    /** *************************************************************************************
     * #grouping
     * @return
     */
    private JMenu getGroupingMenu() {
        final JMenu menu = new JMenu("Grouping");
        menu.add(getSelectGroupMenuItem());
        menu.add(getGroupSelectedMenuItem());
        menu.add(getUngroupSelectedMenuItem());        
        menu.add(getDeleteGroupMenuItem(0));
        menu.add(getDeleteGroupMenuItem(1));
        menu.add(getDeleteGroupMenuItem(2));
        return menu;
    }

    /** *************************************************************************************
     * #grouping
     * @return
     */ 
    private JMenuItem getDeleteGroupMenuItem(final int all) {
        String g = "Delete Group";
        if(all == 1) g = "Delete Unused Groups";
        if(all == 2) g = "Delete All Groups";
        final JMenuItem item = new JMenuItem(g);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if(getActiveGroupManager().getGroupCount() < 1) {
                    JOptionPane.showMessageDialog(getGametableFrame(), "No Groups Defined.", 
                        "No Groups", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if(all == 1) getActiveGroupManager().deleteEmptyGroups();
                if(all == 2) getActiveGroupManager().deleteAllGroups();
                else {
                    GroupingDialog gd = new GroupingDialog(false);
                    gd.setVisible(true);
                    if (gd.isAccepted()) {
                    	Group group = gd.getGroup();
                    	if(group != null)
                        group.deleteGroup();                        
                    }
                }
            }
        });
        return item;
    }

    /** *************************************************************************************
     * #grouping
     * @return
     */ 
    private JMenuItem getGroupSelectedMenuItem() {
        final JMenuItem item = new JMenuItem("Group Selected");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                GroupingDialog gd = new GroupingDialog(true);
                gd.setVisible(true);
                if (gd.isAccepted()) {
                    Group g = gd.getGroup();
                    if(g != null)
                    g.addElements(getGametableCanvas().getSelectedMapElementInstances());
                }
            }
        });
        return item;
    }

    /** *************************************************************************************
     * #grouping
     * @return
     */
    private JMenuItem getSelectGroupMenuItem() {
        final JMenuItem item = new JMenuItem("Select Group");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {                
                if(getActiveGroupManager().getGroupCount() < 1) {
                    JOptionPane.showMessageDialog(getGametableFrame(), "No Groups Defined.", 
                        "No Groups", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                GroupingDialog gd = new GroupingDialog(false);
                Group g = gd.getGroup();
                gd.setVisible(true);
                if (gd.isAccepted() && g != null) {
                	
                    List<MapElement> pogs = g.getElements();                        
                    getGametableCanvas().selectMapElementInstances(pogs, true);
                    getGametableCanvas().repaint();
                }
            }
        });
        return item;
    }

    /** *************************************************************************************
     * #grouping
     * @return
     */
    private JMenuItem getUngroupSelectedMenuItem() {
        final JMenuItem item = new JMenuItem("UnGroup Selected");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                
                for (MapElement pog : getGametableCanvas().getSelectedMapElementInstances())
                {
                	Group g = getActiveGroupManager().getGroup(pog);
                	if (g != null)
                		g.removeElement(pog);	
                }
            }
        });
        return item;
    }

    /**
     * @return The root pog library.
     */
    public MapElementTypeLibrary getPogLibrary()
    {
        return m_pogLibrary;
    }

    /**
     * @return The pog panel.
     */
    public PogPanel getPogPanel()
    {
        return m_pogPanel;
    }

    /**
     * @return The deck panel.
     */
    public JFrame getPogWindow()
    {
        return pogWindow;
    }

    /**
     * @return The preferences object.
     */
    public Preferences getPreferences()
    {
        return m_preferences;
    }
    
    /**
     * build and returns the "Quit" menu item
     * @return the newly built menu item
     */
    private JMenuItem getQuitMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().QUIT);
        item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " Q"));
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                if (pogWindow != null) // close the pog window
                {
                    pogWindow.dispose();
                }
                saveAll();  // TODO confirm before save (check if modified)
                dispose();
                System.exit(0);
            }
        });

        return item;
    }

    /**
     * builds and return the "Undock Pog Window" menu item
     * @return the menu item just built
     */
    private JMenuItem getPogWindowMenuItem()
    {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().POG_WINDOW_DOCK);
        item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " P"));
        if (!m_pogsTabbedPane.isDocked())
            item.setState(true);
        else
            item.setState(false);

        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                m_pogsTabbedPane.toggleDockStatus();
            }
        });

        return item;
    }

//    private JMenuItem getPrivChatWindowMenuItem()
//    {
//        final JMenuItem item = new JMenuItem("Open Private Chat");
//        item.setAccelerator(KeyStroke.getKeyStroke("ctrl T"));
//
//        item.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(final ActionEvent e)
//            {
//                m_chatPanel.openPrivChatWindowDialog();
//            }
//        });
//
//        return item;
//    }

    /** *************************************************************************************
     * 
     * @param color
     * @return
     */
    private JMenuItem getChangeBGMenuItem(final BackgroundColor color) {
        final String cstr = color.getText();
        
        JMenuItem item = new JMenuItem(cstr);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                m_gametableCanvas.changeBackgroundCP(color);
            }
        });

        return item; 
    }
    
    /** 
     * builds and return the "Undock Chat Window"
     * @return the menu item 
     */
    private JMenuItem getChatWindowMenuItem()
    {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().CHAT_WINDOW_DOCK);
        item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " L"));

        if (!m_chatPanel.isDocked())
            item.setState(true);
        else
            item.setState(false);

        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                m_chatPanel.toggleDockStatus();
            }
        });

        return item;
    }
    
    private JMenuItem getMechanicsToggle()
    {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().MECHANICS_WINDOW_USE);
        item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " M"));
        if (m_chatPanel.getUseMechanicsLog())
            item.setState(true);
        else
            item.setState(false);
        
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                toggleMechanicsWindow();
            }
        });

        return item;
    }
    
    /** 
     * @return
     */
    private final static String   MENU_ACCELERATOR         = getMenuAccelerator();
    private static String getMenuAccelerator()
    {
        switch (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
            case Event.CTRL_MASK:
                return "ctrl";
            case Event.META_MASK:
                return "meta";
            //case Event.ALT_MASK:
            //    return "alt";
            case Event.SHIFT_MASK:
                return "shift";
            default: // Others?
                return "ctrl";
        }
    }
    
    /**
     * builds and returns the "Recenter all Player" menu item
     * @return a menu item
     */
    private JMenuItem getRecenterAllPlayersMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().MAP_CENTER_PLAYERS);
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                // confirm the operation
                final int result = UtilityFunctions.yesNoDialog(GametableFrame.this,
                    getLanguageResource().MAP_CENTER_PLAYERS_WARN, getLanguageResource().MAP_CENTER);
                if (result == UtilityFunctions.YES)
                {
                    // get our view center
                    final int viewCenterX = getGametableCanvas().getWidth() / 2;
                    final int viewCenterY = getGametableCanvas().getHeight() / 2;

                    // convert to model coordinates
                    final MapCoordinates modelCenter = getGametableCanvas().viewToModel(viewCenterX, viewCenterY);
                    getGametableCanvas().recenterView(modelCenter, m_gametableCanvas.getZoomLevel());
                    postSystemMessage(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_CENTER_DONE);
                }
            }
        });
        return item;
    }

    
        /** *************************************************************************************
     * 
     * @return
     */
    private JMenuItem getRemoveSelectedMenuItem() {
        final JMenuItem item = new JMenuItem("Remove Selected");

        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {   
                getGametableCanvas().removePogs(getGametableCanvas().getSelectedMapElementInstances(), false);
            }
        });

        return item;
    }

    /** *************************************************************************************
     * 
     * @return
     */
    private JMenuItem getUnPublishSelectedMenuItem(final boolean copy) {
        String s = "Un/Publish Selected";
        if(copy) s = "Copy Selected to Opposite Map";
        final JMenuItem item = new JMenuItem(s);

        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                MapElement npog;
                GameTableMap map = getGametableCanvas().getActiveMap();
                GameTableMap to;
                
                if (map == getGametableCanvas().getPublicMap()) 
                	to = getGametableCanvas().getPrivateMap();
                else 
                	to = getGametableCanvas().getPublicMap();

                for (MapElement pog : getGametableCanvas().getSelectedMapElementInstances())
                {
                  to.addMapElementInstance(pog);
                  map.removeMapElementInstance(pog);
                  if(copy) 
                  {
                      npog = new MapElement(pog);
                      map.addMapElementInstance(npog);
                  }
                }                
            }
        });

        return item;
    }

 
    /**
     * builds and returns the "Save map as" menu item
     * @return the menu item
     */
    public JMenuItem getSaveAsMapMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().MAP_SAVE_AS);
        item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " shift pressed S"));
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
            	m_actingFilePublic = UtilityFunctions.doFileSaveDialog(getLanguageResource().SAVE_AS, "xml", true);
            	if (m_actingFilePublic != null)
            		saveToXML(m_actingFilePublic);

            		/*
            	
            	
                if (getGametableCanvas().isPublicMap())
                {
                    m_actingFilePublic = UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true);
                    if (m_actingFilePublic != null)
                    {
                        saveState(getGametableCanvas().getActiveMap(), m_actingFilePublic);
                    }
                }
                else
                {
                    m_actingFilePrivate = UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true);
                    if (m_actingFilePrivate != null)
                    {
                        saveState(getGametableCanvas().getActiveMap(), m_actingFilePrivate);
                    }
                }
                */
            }
        });

        return item;
    }


    /**
     * builds and returns the "save map" menu item
     * @return the menu item
     */
    public JMenuItem getSaveMapMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().MAP_SAVE);
        item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed S"));
        item.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
            	if (m_actingFilePublic == null)
                  m_actingFilePublic = UtilityFunctions.doFileSaveDialog(getLanguageResource().SAVE_AS, "xml", true);
            	
            	if (m_actingFilePublic != null)
            		saveToXML(m_actingFilePublic);
              
            	/*
                if (getGametableCanvas().isPublicMap())
                {
                    if (m_actingFilePublic == null)
                    {
                        m_actingFilePublic = UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true);
                    }

                    if (m_actingFilePublic != null)
                    {
                        // save the file
                        saveState(getGametableCanvas().getActiveMap(), m_actingFilePublic);
                    }
                }
                else
                {
                    if (m_actingFilePrivate == null)
                    {
                        m_actingFilePrivate = UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true);
                    }

                    if (m_actingFilePrivate != null)
                    {
                        // save the file
                        saveState(getGametableCanvas().getActiveMap(), m_actingFilePrivate);
                    }
                }
                */
            }
        });

        return item;
    }

    /**
     * builds and returns the "Scan for pogs" menu item
     * @return the menu item
     */
    private JMenuItem getScanForPogsMenuItem()
    {
        final JMenuItem item = new JMenuItem(getLanguageResource().POG_SCAN);
        item.setAccelerator(KeyStroke.getKeyStroke("F5"));
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent e)
            {
                reacquirePogs();
            }
        });

        return item;
    }

    /**
     * builds and returns the "Edit private map" menu item
     * @return the menu item
     */
    private JMenuItem getTogglePrivateMapMenuItem()
    {
        if (m_togglePrivateMapMenuItem == null)
        {
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().MAP_PRIVATE_EDIT);
            item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " F"));
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    toggleLayer();
                }
            });

            m_togglePrivateMapMenuItem = item;
        }

        return m_togglePrivateMapMenuItem;
    }

    /**
     * gets the tools manager
     * @return the tool manager
     */
    public ToolManager getToolManager()
    {
        return m_toolManager;
    }

    /**
     * handles a "grid mode" packet
     * @param gridMode grid mode
     */
    public void gridModePacketReceived(final int gridMode)
    {
        // note the new grid mode
        getGametableCanvas().setGridModeByID(gridMode);
        updateGridModeMenu(); // sets the appropriate checks in the menu

        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            send(PacketManager.makeGridModePacket(gridMode));
        }

        repaint(); // repaint the canvas
    }
    
    /** *************************************************************************************
     * 
     * @param openLink
     * @param closeLink
     */
    public void groupPacketReceived(Action action, final String group, final MapElementID pog, final int player) 
    {
        if(m_netStatus == NetStatus.HOSTING) 
        {
            send(PacketManager.makeGroupPacket(action, group, pog, player));            
        }
        
        // If Im the one who sent the packet, ignore it. 
        if(player == getMyPlayerId()) return;
        getActiveGroupManager().packetReceived(action, group, pog);
    }
    
    /** *************************************************************************************
     * 
     * @param openLink
     * @param closeLink
     */
    public void groupPacketReceived(Action action, final String group, final String newGroupName, final int player) 
    {
        if(m_netStatus == NetStatus.HOSTING) 
        {
            send(PacketManager.makeRenameGroupPacket(group, newGroupName, player));            
        }
        
        // If Im the one who sent the packet, ignore it. 
        if(player == getMyPlayerId()) return;
        getActiveGroupManager().renamePacketReceived(group, newGroupName);
    }

    /**
     * attempt to host a network name by prompting details of the game session
     */
    public void host()
    {
        host(false);
    }

    /**
     * host a game
     * @param force if force do not ask for details, otherwise display the host dialog
     */
    public void host(boolean force)
    {
        if (m_netStatus == NetStatus.HOSTING) // if we are already the host
        {
            m_chatPanel.logAlertMessage(getLanguageResource().HOST_ERROR_HOST);
            return;
        }
        if (m_netStatus == NetStatus.CONNECTED) // if we are connected to a game and not hosting
        {
            m_chatPanel.logAlertMessage(getLanguageResource().HOST_ERROR_JOIN);
            return;
        }

        if (!force)
        {
            // get relevant info from the user
            if (!runHostDialog())
            {
                return;
            }
        }

        // clear out all players
        m_nextPlayerId = 0;
        m_players = new ArrayList<Player>();
        final Player me = new Player(m_playerName, m_characterName, m_nextPlayerId); // this means the host is always
                                                                                     // player 0
        m_nextPlayerId++;
        m_players.add(me);
        me.setHostPlayer(true);
        m_myPlayerIndex = 0;

        m_networkThread = new NetworkThread(m_port);
        m_networkThread.start();
        // TODO: fix hosting failure detection

        m_netStatus = NetStatus.HOSTING; // our status is now hosting
        final String message = "Hosting on port: " + m_port;
        m_chatPanel.logSystemMessage(message);

        m_chatPanel.logMechanics("<a href=\"http://www.whatismyip.com\">" + getLanguageResource().IP_CHECK + "</a> (" + getLanguageResource().IP_CHECK2 + ")");

        Log.log(Log.NET, message);

        m_hostMenuItem.setEnabled(false); // disable the host menu item
        m_joinMenuItem.setEnabled(false); // disable the join menu item
        m_disconnectMenuItem.setEnabled(true); // enable the disconnect menu item
        setTitle(GametableApp.VERSION + " - " + me.getCharacterName());

        // also, all decks clear
        m_decks.clear();
        m_cards.clear();
    }

    /**
     * handles a failure in the host thread
     */
    public void hostThreadFailed()
    {
        m_chatPanel.logAlertMessage(getLanguageResource().HOST_ERROR_FAIL);
        m_networkThread.interrupt();
        m_networkThread = null;
        disconnect();
    }

    /**
     * Performs initialization. This draws all the controls in the Frame and sets up listener to react
     * to user actions.
     * 
     * @throws IOException
     */
    private void initialize() throws IOException
    {
    	ImageCache.startCacheDaemon();
    	
    	buildActions();
    	
    	SlashCommands.registerDefaultChatCommands();
    	
    	// todo #Plugins Automated module loading mechanism
    	
    	registerModule(new CardModule());
    	registerModule(DiceMacroModule.getModule());
    	registerModule(ActivePogsModule.getModule());
    	
        if (DEBUG_FOCUS) // if debugging
        {
            final KeyboardFocusManager man = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            man.addPropertyChangeListener(new PropertyChangeListener()
            {
                /*
                 * If debugging,show changes to properties in the console
                 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
                 */
                public void propertyChange(final PropertyChangeEvent e)
                {
                    System.out.println(e.getPropertyName() + ":\n    " + e.getOldValue() + "\n -> " + e.getNewValue());
                }

            });
        }
                
        // Configure chat panel
        m_chatPanel = new ChatPanel();
        m_textEntry = m_chatPanel.getTextEntry();
 
        setContentPane(new JPanel(new BorderLayout())); // Set the main UI object with a Border Layout
        setDefaultCloseOperation(EXIT_ON_CLOSE);        // Ensure app ends with this frame is closed
        setTitle(GametableApp.VERSION);                 // Set frame title to the current version
        setJMenuBar(getMainMenuBar());                  // Set the main MenuBar
        
        // Set this class to handle events from changing grid types
        m_noGridModeMenuItem.addActionListener(this);   
        m_squareGridModeMenuItem.addActionListener(this);
        m_hexGridModeMenuItem.addActionListener(this);

        
        // Configure the panel containing the map and the chat window
        m_mapChatSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        m_mapChatSplitPane.setContinuousLayout(true);
        m_mapChatSplitPane.setResizeWeight(1.0);
        m_mapChatSplitPane.setBorder(null);

        // Configure the panel that splits the map and the pog list
        m_mapPogSplitPane.setContinuousLayout(true);
        m_mapPogSplitPane.setBorder(null);

        // Configure the color dropdown
        m_colorCombo.setMaximumSize(new Dimension(100, 21));
        m_colorCombo.setFocusable(false);
        
        // Configure the toolbar
        m_toolBar.setFloatable(false);
        m_toolBar.setRollover(true);
        m_toolBar.setBorder(new EmptyBorder(2, 5, 2, 5));
        m_toolBar.add(m_colorCombo, null);
        m_toolBar.add(Box.createHorizontalStrut(5));

        initializeTools();

        m_toolBar.add(Box.createHorizontalStrut(5));

        /*
         * Added in order to accomodate grid unit multiplier
         */
        m_gridunitmultiplier = new JTextField("5", 3);
        m_gridunitmultiplier.setMaximumSize(new Dimension(42, 21));
        
        // Configure the units dropdown
        final String[] units = {
            "ft", "m", "u"
        };
        m_gridunit = new JComboBox(units);
        m_gridunit.setMaximumSize(new Dimension(42, 21));
        m_toolBar.add(m_gridunitmultiplier);
        m_toolBar.add(m_gridunit);
        // Add methods to react to changes to the unit multiplier
        // todo : this is definitely better somewhere else
        m_gridunitmultiplier.getDocument().addDocumentListener(new DocumentListener()
        {
            //todo exceptions should be captured
            public void changedUpdate(final DocumentEvent e)
            {
                grid_multiplier = Double.parseDouble(m_gridunitmultiplier.getText());
            }

            public void insertUpdate(final DocumentEvent e)
            {
                grid_multiplier = Double.parseDouble(m_gridunitmultiplier.getText());
            }

            public void removeUpdate(final DocumentEvent e)
            {
                // Commented out to fix grid measurement size error
                //grid_multiplier = Double.parseDouble(m_gridunitmultiplier.getText());
            }
        });
        m_gridunit.addActionListener(this);

        // Configure the checkbox to show names
        m_showNamesCheckbox.setFocusable(false);
        // @revise consider to place this somewhere else
        m_showNamesCheckbox.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                m_gametableCanvas.repaint();
            }
        });
        m_toolBar.add(Box.createHorizontalStrut(5));
        m_toolBar.add(m_showNamesCheckbox);
        m_toolBar.add(Box.createHorizontalStrut(5));
        m_toolBar.add(m_randomRotate); //#randomrotate

        getContentPane().add(m_toolBar, BorderLayout.NORTH);

        getGametableCanvas().init(this);
        m_pogLibrary = MapElementTypeLibrary.getMasterLibrary();
        
        m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("pogs"), Layer.POG));        
        m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("environment"), Layer.ENVIRONMENT));
        m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("overlays"), Layer.OVERLAY));
        m_pogLibrary.addSubLibrary(new BasicMapElementTypeLibrary(m_pogLibrary, new File("underlays"), Layer.UNDERLAY));

        // pogWindow
        m_pogPanel = new PogPanel(m_pogLibrary, getGametableCanvas());
        m_pogsTabbedPane.addTab(m_pogPanel, getLanguageResource().POG_LIBRARY);
        
        for (Module module : g_modules)
        {
        	// todo #Plugins consider abstracting "PogsTabbedPane" through an interface. 
        	module.onInitializeUI();        	
        }
        
        m_pogsTabbedPane.setFocusable(false);
        m_canvasPane.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED), new EmptyBorder(1, 1, 1, 1)));
        m_canvasPane.add(getGametableCanvas(), BorderLayout.CENTER);
        m_mapChatSplitPane.add(m_canvasPane, JSplitPane.TOP);
        m_mapChatSplitPane.add(m_chatPanel, JSplitPane.BOTTOM);

        m_mapPogSplitPane.add(m_pogsTabbedPane, JSplitPane.LEFT);
        m_mapPogSplitPane.add(m_mapChatSplitPane, JSplitPane.RIGHT);
        getContentPane().add(m_mapPogSplitPane, BorderLayout.CENTER);
        m_status.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        getContentPane().add(m_status, BorderLayout.SOUTH);
        updateStatus();

        m_disconnectMenuItem.setEnabled(false); // when the program starts we are disconnected so disable this menu item

        final ColorComboCellRenderer renderer = new ColorComboCellRenderer();
        m_colorCombo.setRenderer(renderer);

        // load the primary map
        //getGametableCanvas().setActiveMap(getGametableCanvas().getPrivateMap());
        PacketSourceState.beginFileLoad();
        File autoSave = getAutoSaveXMLFile();
        if (autoSave.exists())
        	loadFromXML(autoSave, true, true);
        
        //loadState(new File("autosavepvt.grm"));
        PacketSourceState.endFileLoad();

        /*
        getGametableCanvas().setActiveMap(getGametableCanvas().getPublicMap());
        loadState(new File("autosave.grm"));
        */
        //loadPrefs();

        addPlayer(new Player(m_playerName, m_characterName, -1));
        m_myPlayerIndex = 0;

        m_colorCombo.addActionListener(this);
        updateGridModeMenu();

        addComponentListener(new ComponentAdapter()
        {
            /*
             * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
             */
            public void componentMoved(final ComponentEvent e)
            {
                updateWindowInfo();
            }

            /*
             * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
             */
            public void componentResized(final ComponentEvent event)
            {
                updateWindowInfo();
            }

        });

        // handle window events
        addWindowListener(new WindowAdapter()
        {
            /*
             * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
             */
            public void windowClosed(final WindowEvent e)
            {
                saveAll();
            }

            /*
             * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
             */
            public void windowClosing(final WindowEvent e)
            {
                saveAll();
            }
        });

        /*
         * // change the default component traverse settings // we do this cause we don't really care about those //
         * settings, but we want to be able to use the tab key KeyboardFocusManager focusMgr =
         * KeyboardFocusManager.getCurrentKeyboardFocusManager(); Set set = new
         * HashSet(focusMgr.getDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS)); set.clear(); //
         * set.add(KeyStroke.getKeyStroke('\t', 0, false));
         * focusMgr.setDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set); set = new
         * HashSet(focusMgr.getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS)); set.clear(); //
         * set.add(KeyStroke.getKeyStroke('\t', 0, false));
         * focusMgr.setDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set);
         */

        m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SLASH"),
            "startSlash");
        m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ENTER"),
            "startText");
        m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("control pressed R"), "reply");

        m_gametableCanvas.getActionMap().put("startSlash", new AbstractAction()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                if (m_gametableCanvas.isTextFieldFocused())
                {
                    return;
                }

                // only do this at the start of a line
                if (m_textEntry.getText().length() == 0)
                {
                    // furthermore, only do the set text and focusing if we don't have
                    // focus (otherwise, we end up with two slashes. One from the user typing it, and
                    // another from us setting the text, cause our settext happens first.)
                    if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != m_textEntry)
                    {
                        m_textEntry.setText("/");
                    }
                }
                m_textEntry.requestFocus();
            }
        });

        m_gametableCanvas.getActionMap().put("startText", new AbstractAction()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                if (m_gametableCanvas.isTextFieldFocused())
                {
                    return;
                }

                if (m_textEntry.getText().length() == 0)
                {
                    // furthermore, only do the set text and focusing if we don't have
                    // focus (otherwise, we end up with two slashes. One from the user typing it, and
                    // another from us setting the text, cause our settext happens first.)
                    if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != m_textEntry)
                    {
                        m_textEntry.setText("");
                    }
                }
                m_textEntry.requestFocus();
            }
        });

//        m_gametableCanvas.getActionMap().put("reply", new AbstractAction()
//        {
//            /*
//             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
//             */
//            public void actionPerformed(final ActionEvent e)
//            {
//                // we don't do this if there's already text in the entry field
//                if (m_textEntry.getText().length() == 0)
//                {
//                    // if they've never received a tell, just tell them that
//                    if (m_lastPrivateMessageSender == null)
//                    {
//                        // they've received no tells yet
//                        m_chatPanel.logAlertMessage("You cannot reply until you receive a /tell from another player.");
//                    }
//                    else
//                    {
//                        startTellTo(m_lastPrivateMessageSender);
//                    }
//                }
//                m_textEntry.requestFocus();
//            }
//        });

        initializeExecutorThread();
        
        // Load frame preferences
        loadPrefs();
    }

		private File getAutoSaveXMLFile()
		{
			return new File("autosave.xml");
		}

    /**
     * starts the execution thread
     */
    private void initializeExecutorThread()
    {
        if (m_executorThread != null)
        {
            m_executorThread.interrupt();
            m_executorThread = null;
        }

        // start the poll thread
        m_executorThread = new PeriodicExecutorThread(new Runnable()
        {
            public void run()
            {
                tick();
            }
        });
        m_executorThread.start();
    }

    /**
     * Initializes the tools from the ToolManager.
     */
    private void initializeTools()
    {
        try
        {
            m_toolManager.initialize();
            final int buttonSize = m_toolManager.getMaxIconSize();
            final int numTools = m_toolManager.getNumTools();
            m_toolButtons = new JToggleButton[numTools];
            for (int toolId = 0; toolId < numTools; toolId++)
            {
                final ToolManager.Info info = m_toolManager.getToolInfo(toolId);
                final Image im = Images.createBufferedImage(buttonSize, buttonSize);
                {
                    final Graphics g = im.getGraphics();
                    final Image icon = info.getIcon();
                    final int offsetX = (buttonSize - icon.getWidth(null)) / 2;
                    final int offsetY = (buttonSize - icon.getHeight(null)) / 2;
                    g.drawImage(info.getIcon(), offsetX, offsetY, null);
                    g.dispose();
                }

                final JToggleButton button = new JToggleButton(new ImageIcon(im));
                m_toolBar.add(button);
                button.addActionListener(new ToolButtonActionListener(toolId));
                button.setFocusable(false);
                m_toolButtonGroup.add(button);
                m_toolButtons[toolId] = button;

                String keyInfo = "";
                if (info.getQuickKey() != null)
                {
                    final String actionId = "tool" + toolId + "Action";
                    getGametableCanvas().getActionMap().put(actionId, new ToolButtonAbstractAction(toolId));
                    final KeyStroke keystroke = KeyStroke.getKeyStroke("ctrl " + info.getQuickKey());
                    getGametableCanvas().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keystroke, actionId);
                    keyInfo = " (Ctrl+" + info.getQuickKey() + ")";
                }
                button.setToolTipText(info.getName() + keyInfo);
                final List<PreferenceDescriptor> prefs = info.getTool().getPreferences();
                for (PreferenceDescriptor desc : prefs)
                {
                    m_preferences.addPreference(desc);
                }
            }
        }
        catch (final IOException ioe)
        {
            Log.log(Log.SYS, getLanguageResource().TOOLBAR_FAIL);
            Log.log(Log.SYS, ioe);
        }
    }

    /**
     * joins a network game
     */
    public void join()
    {
        if (m_netStatus == NetStatus.HOSTING) // we can't join if we're hosting
        {
            m_chatPanel.logAlertMessage(getLanguageResource().JOIN_ERROR_HOST);
            return;
        }
        if (m_netStatus == NetStatus.CONNECTED) // we can't join if we are already connected
        {
            m_chatPanel.logAlertMessage(getLanguageResource().JOIN_ERROR_JOIN);
            return;
        }

        boolean res = runJoinDialog(); // get details of where to connect to
        if (!res)
        {
            // they cancelled out
            return;
        }

        // now we have the ip to connect to. Try to connect to it
        try
        {
            m_networkThread = new NetworkThread();
            m_networkThread.start();
            final Connection conn = new Connection(m_ipAddress, m_port);
            m_networkThread.add(conn);

            // now that we've successfully made a connection, let the host know
            // who we are
            m_players = new ArrayList<Player>();
            final Player me = new Player(m_playerName, m_characterName, -1);
            me.setConnection(conn);
            m_players.add(me);
            m_myPlayerIndex = 0;

            // reset game data
            m_gametableCanvas.setScrollPosition(0, 0);
            m_gametableCanvas.getPublicMap().clearMapElementInstances();
            m_gametableCanvas.getPublicMap().clearLineSegments();
            // PacketManager.g_imagelessPogs.clear();

            // send the packet
            while (!conn.isConnected()) // this waits until the connection is established
            {
            }
            conn.sendPacket(PacketManager.makePlayerPacket(me, m_password)); // send my data to the host

            PacketSourceState.beginHostDump();

            // and now we're ready to pay attention
            m_netStatus = NetStatus.CONNECTED;

            m_chatPanel.logSystemMessage(getLanguageResource().JOINED);

            m_hostMenuItem.setEnabled(false); // disable the host menu item
            m_joinMenuItem.setEnabled(false); // disable the join menu item
            m_disconnectMenuItem.setEnabled(true); // enable the disconnect menu item
            setTitle(GametableApp.VERSION + " - " + me.getCharacterName());
        }
        catch (final Exception ex)
        {
            Log.log(Log.SYS, ex);
            m_chatPanel.logAlertMessage(getLanguageResource().CONNECT_FAIL);
            setTitle(GametableApp.VERSION);
            PacketSourceState.endHostDump();
        }
    }

    /**
     * close a connection indicating a reason
     * @param conn connection to close
     * @param reason code for the reason to be booted
     */
    public void kick(final Connection conn, final int reason)
    {
        send(PacketManager.makeRejectPacket(reason), conn);
        conn.close();
    }

    /**
     * handle a lines packet (a player painted lines)
     * @param lines array of line segments
     * @param authorID author
     * @param state
     */
    public void linesPacketReceived(List<LineSegment> lines, final int authorID, final int state)
    {
        int stateId = state;
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            // and give it a genuine state ID first
            stateId = getNewStateId();
            send(PacketManager.makeLinesPacket(lines, authorID, stateId));
        }

        // add the lines to the array
        getGametableCanvas().doAddLineSegments(lines, authorID, stateId);
    }

    private void toggleMechanicsWindow() 
    {       
        m_chatPanel.toggleMechanicsWindow();
        validate();
    }
    



    public void loadPog() {
        final File openFile = UtilityFunctions.doFileOpenDialog(getLanguageResource().OPEN, ".pog", true);

        if (openFile == null) { // they cancelled out of the open
            return;
        } 
        try {
            final FileInputStream infile = new FileInputStream(openFile);
            final DataInputStream dis = new DataInputStream(infile);
            final MapElement nPog = new MapElement(dis);

            if (!nPog.getMapElementType().isLoaded()) { // we need this image
                PacketManager.requestPogImage(null, nPog);
            }
            m_gametableCanvas.addPog(nPog);
        }
        catch (final IOException ex1) {
            Log.log(Log.SYS, ex1);
        }
    }
    /**
     * loads preferences from file
     */
    public void loadPrefs()
    {
        final File file = new File("prefs.prf");
        if (!file.exists()) // if the file doesn't exist, set some hard-coded defaults and return
        {
            // DEFAULTS
            m_mapChatSplitPane.setDividerLocation(0.7);
            m_mapPogSplitPane.setDividerLocation(150);
            m_windowSize = new Dimension(800, 600);
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            m_windowPos = new Point((screenSize.width - m_windowSize.width) / 2,
                (screenSize.height - m_windowSize.height) / 2);
            m_bMaximized = false;
            getGametableCanvas().setPrimaryScroll(getGametableCanvas().getPublicMap(), 0,0);
            getGametableCanvas().setZoomLevel(0);
            applyWindowInfo();
            
            m_showNamesCheckbox.setSelected(false);
            m_randomRotate.setSelected(false); //#randomrotate
            
            for (Module module : g_modules)
            	module.onLoadPreferencesCompleted();

            return;
        }

        // try reading preferences from file
        try
        {
            final FileInputStream prefFile = new FileInputStream(file);
            final DataInputStream prefDis = new DataInputStream(prefFile);

            m_playerName = prefDis.readUTF();
            m_characterName = prefDis.readUTF();
            m_ipAddress = prefDis.readUTF();
            m_port = prefDis.readInt();
            m_password = prefDis.readUTF();
            getGametableCanvas().setPrimaryScroll(getGametableCanvas().getPublicMap(), prefDis.readInt(),
                prefDis.readInt());
            getGametableCanvas().setZoomLevel(prefDis.readInt());

            m_windowSize = new Dimension(prefDis.readInt(), prefDis.readInt());
            m_windowPos = new Point(prefDis.readInt(), prefDis.readInt());
            m_bMaximized = prefDis.readBoolean();
            applyWindowInfo();

            // divider locations
            m_mapChatSplitPane.setDividerLocation(prefDis.readInt());
            m_mapPogSplitPane.setDividerLocation(prefDis.readInt());

            m_showNamesCheckbox.setSelected(prefDis.readBoolean());

            // new divider locations
            m_chatPanel.setChatSplitPaneDivider(prefDis.readInt());            
            m_chatPanel.setUseMechanicsLog(prefDis.readBoolean());
            
            for (Module module : g_modules)
            	module.onLoadPreferences(prefDis);
            
            prefDis.close();
            prefFile.close();
        }
        catch (final FileNotFoundException ex1)
        {
            Log.log(Log.SYS, ex1);
        }
        catch (final IOException ex1)
        {
            Log.log(Log.SYS, ex1);
        }
        
        for (Module module : g_modules)
        	module.onLoadPreferencesCompleted();
    }

    public void loadState(final byte saveFileData[])
    {
        // let it know we're receiving initial data (which we are. Just from a file instead of the host)
        try
        {
            // now we have to pick out the packets and send them in for processing one at a time
            final DataInputStream walker = new DataInputStream(new ByteArrayInputStream(saveFileData));
            int read = 0;
            int packetNum = 0;
            while (read < saveFileData.length)
            {
                final int packetLen = walker.readInt();
                read += 4;

                final byte[] packet = new byte[packetLen];
                walker.read(packet);
                read += packetLen;

                // dispatch the packet
                PacketManager.readPacket(null, packet);
                packetNum++;
            }
        }
        catch (final FileNotFoundException ex)
        {
            Log.log(Log.SYS, ex);
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
        }

        repaint();
        refreshPogList();
    }
    
    public void loadFromXML(File file, boolean loadPublic, boolean loadPrivate)
    {
    	Document doc;
			try
			{
				doc = XMLUtils.parseXMLDocument(file);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(this, "Error loading " + file.getName() + " : " + e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
				// TODO proper error handling
				return;
			}
			
    	Element root = doc.getDocumentElement();
    	if (!root.getTagName().equals("gt"))
    	{
    		JOptionPane.showMessageDialog(this, "Invalid file format", null, JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	
    	try
    	{
	    	PacketSourceState.beginFileLoad();
	    	
	    	if (loadPrivate && loadPublic)
	    		MapElementID.clear();
	    	
	    	XMLSerializeConverter converter= new XMLSerializeConverter();
	    	
	    	if (loadPublic)
	    	{
	    		Element publicEl = XMLUtils.getFirstChildElementByTagName(root, "public_map");
	    		m_gametableCanvas.getPublicMap().deserialize(publicEl, converter);
	    	}
	    	
	    	if (loadPrivate)
	    	{
	    		Element privateEl = XMLUtils.getFirstChildElementByTagName(root, "private_map");
	    		m_gametableCanvas.getPrivateMap().deserialize(privateEl, converter);
	    	}
	    	
	    	if (loadPublic && loadPrivate)
	    	{
	        loadGridFromXML(root, converter);
	        loadLockedElementsFromXML(root, converter);
	      	
		    	// Hook for modules to load data from save file
		    	Element modulesEl = XMLUtils.getFirstChildElementByTagName(root, "modules");
		    	
		    	if (modulesEl != null)
		    	{
			    	for (Module module : g_modules)
			    	{
			    		if (module.canSaveToXML())
			    		{
			    			Element moduleEl = XMLUtils.findFirstChildElement(modulesEl, "module", "name", module.getModuleName());
			    			
			    			if (moduleEl != null)
			    			{			    				
			    				module.loadFromXML(moduleEl, converter);
			    			}
			    		}    	
			    	}
		    	}
	    	}
    	}
    	finally
    	{
    		PacketSourceState.endFileLoad();
    	}
    }

    /**
     * Store locked element list
     * @param root
     * @param converter
     */
    private void loadLockedElementsFromXML(Element root, XMLSerializeConverter converter)
    {
    	m_gametableCanvas.lockAllMapElementInstances(false);
    	Element listEl = XMLUtils.getFirstChildElementByTagName(root, "locked");
    	if (listEl == null)
    		return;
    	
    	for (Element el : XMLUtils.getChildElementsByTagName(listEl, "id"))
    	{
    		long id = UtilityFunctions.parseLong(XMLUtils.getNodeValue(el), 0);
    		MapElementID elID = converter.getMapElementID(id);
    		if (elID != null)
    		{
    			MapElement mapEl = getMapElement(elID);
    			if (mapEl != null)
    				m_gametableCanvas.lockMapElementInstance(mapEl, true);
    		}
    	}
    }
    
		private void loadGridFromXML(Element root, XMLSerializeConverter converter)
		{
			// grid 
			Element gridEl = XMLUtils.getFirstChildElementByTagName(root, "grid");
			if (gridEl != null)
			{
				int gridMode = UtilityFunctions.parseInt(gridEl.getAttribute("modeid"), GametableCanvas.GRID_MODE_SQUARES);
				m_gametableCanvas.setGridModeByID(gridMode);
			
				// grid background
				Element bkEl = XMLUtils.getFirstChildElementByTagName(gridEl, "background");
				String typeFQN = bkEl.getAttribute("element_type");
				MapElementTypeIF type = MapElementTypeLibrary.getMasterLibrary().getElementType(typeFQN);
				if (type != null)
				{
					m_gametableCanvas.changeBackground(type);
				}
				else				
				{
					String color = bkEl.getAttribute("color");
					BackgroundColor bkColor = BackgroundColor.DEFAULT;
					
					try
					{
						bkColor = BackgroundColor.valueOf(color);
					}
					catch (IllegalArgumentException e)
					{
						// stick to default
					}
					catch (NullPointerException e)
					{
						// stick to default	    					
					}
					
					getGametableCanvas().changeBackground(bkColor);
				}
			}
		}

    public void lockPogPacketReceived(final MapElementID id, final boolean newLock)
    {
        getGametableCanvas().doLockPog(id, newLock);

        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            send(PacketManager.makeLockPogPacket(id, newLock));
        }
    }

    public void lockAllPogPacketReceived(final boolean lock) {
        lockMap(getGametableCanvas().getPublicMap(),lock);
        if(m_netStatus == NetStatus.HOSTING) {
            m_networkThread.send(PacketManager.makeLockAllPogPacket(lock));
        }
    }

    private void lockMap(final GameTableMap mapToLock, final boolean lock) 
    {
    	for (MapElement pog : mapToLock.getMapElements())
    	{       
    		m_gametableCanvas.lockMapElementInstance(pog, lock);
      }
    }

    private void doLockMap(final boolean lock)
    {
        final GameTableMap mapToLock = m_gametableCanvas.getActiveMap();
        boolean priv = true;
        if(mapToLock == getGametableCanvas().getPublicMap()) priv = false;

        if(priv || (m_netStatus == NetStatus.DISCONNECTED))
        {
            lockMap(mapToLock, lock);
            if(lock) m_chatPanel.logMechanics(getLanguageResource().MAP_LOCK_ALL_DONE);
            else m_chatPanel.logMechanics(getLanguageResource().MAP_UNLOCK_ALL_DONE);
        } else {
            if(lock) postSystemMessage(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_LOCK_ALL_DONE2);
            else postSystemMessage(getMyPlayer().getPlayerName() + " " + getLanguageResource().MAP_UNLOCK_ALL_DONE2);           
            send(PacketManager.makeLockAllPogPacket(lock));
        }       
    }

    public void loginCompletePacketReceived()
    {
        // this packet is never redistributed.
        // all we do in response to this allow pog text
        // highlights. The pogs don't know the difference between
        // inital data and actual player changes.
        PacketSourceState.endHostDump();
    }

    // makes a card pog out of the sent in card
    @Deprecated
    public MapElement makeCardPog(final Card card)
    {
        // there might not be a pog associated with this card
        if (card.getCardFile().length() == 0)
        {
            return null;
        }

        final MapElementTypeIF newPogType = getPogLibrary().getElementType("pogs" + File.separator + card.getCardFile());

        // there could be a problem with the deck definition. It's an easy mistake
        // to make. So rather than freak out, we just return null.
        if (newPogType == null)
        {
            return null;
        }

        final MapElement newPog = new MapElement(newPogType);

        // make it a card pog
        Card.setCard(newPog, card);
        return newPog;
    }

    public void movePogPacketReceived(final MapElementID id, MapCoordinates newPos)
    {
        getGametableCanvas().doMovePog(id, newPos);

        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            send(PacketManager.makeMovePogPacket(id, newPos));
        }
    }

//    public void openPrivateMessageWindow()
//    {
//    }

    public void packetReceived(final Connection conn, final byte[] packet)
    {
        // synch here. after we get the packet, but before we process it.
        // this is all the synching we need on the comm end of things.
        // we will also need to synch every user entry point
        PacketManager.readPacket(conn, packet);
    }

    /** *************************************************************************************************************
     * 
     */
    public void pingPacketReceived()
    {
        // do nothing for now
    }

    /** *************************************************************************************************************
     * 
     * @param connection
     * @param player
     * @param password
     */
    public void playerJoined(final Connection connection, final Player player, final String password)
    {
        confirmHost();

        if (!m_password.equals(password))
        {
            // rejected!
            kick(connection, REJECT_INVALID_PASSWORD);
            return;
        }

        // now we can associate a player with the connection
        connection.markLoggedIn();
        player.setConnection(connection);

        // set their ID
        player.setId(m_nextPlayerId);
        m_nextPlayerId++;

        // tell everyone about the new guy
        postSystemMessage(player.getPlayerName() + " " + getLanguageResource().PLAYER_JOINED);
        addPlayer(player);

        sendCastInfo();

        // tell the new guy the entire state of the game
        // lines

        send(PacketManager.makeLinesPacket(getGametableCanvas().getPublicMap().getLines(), -1, -1), player);

        // pogs
        for (MapElement pog : getGametableCanvas().getPublicMap().getMapElements())
        {
            send(PacketManager.makeAddPogPacket(pog), player);
        }

        // finally, have the player recenter on the host's view
        final int viewCenterX = getGametableCanvas().getWidth() / 2;
        final int viewCenterY = getGametableCanvas().getHeight() / 2;

        // convert to model coordinates
        final MapCoordinates modelCenter = getGametableCanvas().viewToModel(viewCenterX, viewCenterY);
        send(PacketManager.makeRecenterPacket(modelCenter, m_gametableCanvas.getZoomLevel()), player);

        // let them know we're done sending them data from the login
        send(PacketManager.makeLoginCompletePacket(), player);

        // tell them the decks that are in play
        sendDeckList();
    }

    public void pogDataPacketReceived(final MapElementID id, final String s, final Map<String, String> toAdd, final Set<String> toDelete)
    {
        getGametableCanvas().doSetPogData(id, s, toAdd, toDelete);

        if (m_netStatus == NetStatus.HOSTING)
        {
            m_networkThread.send(PacketManager.makePogDataPacket(id, s, toAdd, toDelete));
        }
    }
    
    /** *************************************************************************************
     * @param id
     * @param size
     */
    public void pogLayerPacketReceived(final MapElementID id, final Layer layer)
    {
        getGametableCanvas().doSetPogLayer(id, layer);
        if (m_netStatus == NetStatus.HOSTING)
        {
            m_networkThread.send(PacketManager.makePogLayerPacket(id, layer));
        }
    }

    public void pogSizePacketReceived(final MapElementID id, final float size)
    {
        getGametableCanvas().doSetPogSize(id, size);

        if (m_netStatus == NetStatus.HOSTING)
        {
            m_networkThread.send(PacketManager.makePogSizePacket(id, size));
        }
    }

    public void pogTypePacketReceived(final MapElementID id, final MapElementID type)
    {
        getGametableCanvas().doSetPogType(id, type);

        if (m_netStatus == NetStatus.HOSTING)
        {
            m_networkThread.send(PacketManager.makePogTypePacket(id, type));
        }
    }
    
    public void pointPacketReceived(final int plrIdx, MapCoordinates modelPos, final boolean bPointing)
    {
        // we're not interested in point packets of our own hand
        if (plrIdx != getMyPlayerIndex())
        {
            final Player plr = m_players.get(plrIdx);
            plr.setPointingLocation(modelPos);
            plr.setPointing(bPointing);
        }

        if (m_netStatus == NetStatus.HOSTING)
        {
            send(PacketManager.makePointPacket(plrIdx, modelPos, bPointing));
        }

        getGametableCanvas().repaint();
    }

    public void postAlertMessage(final String text)
    {
        postMessage(ALERT_MESSAGE_FONT + text + END_ALERT_MESSAGE_FONT);
    }

    public void postMessage(final String text)
    {
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if you're the host, push to all players
            send(PacketManager.makeTextPacket(text));

            // add it to your own text log
            m_chatPanel.logMessage(text);
        }
        else if (m_netStatus == NetStatus.CONNECTED)
        {
            // if you're a player, just post it to the GM
            send(PacketManager.makeTextPacket(text));
        }
        else
        {
            // if you're offline, just add it to the log
            m_chatPanel.logMessage(text);
        }
    }
    
    public void postMechanics(final String text)
    {
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if you're the host, push to all players
            send(PacketManager.makeMechanicsPacket(text));

            // add it to your own text log
            m_chatPanel.logMechanics(text);
        }
        else if (m_netStatus == NetStatus.CONNECTED)
        {
            // if you're a player, just post it to the GM
            send(PacketManager.makeMechanicsPacket(text));
        }
        else
        {
            // if you're offline, just add it to the log
            m_chatPanel.logMechanics(text);
        }
    }
    
    public void postPrivMechanics(final String toName, final String text)
    {
        if (m_netStatus == NetStatus.HOSTING) {
            for (int i = 0; i < m_players.size(); i++) {
                final Player player = m_players.get(i);
                if (player.hasName(toName)) {
                    send(PacketManager.makePrivMechanicsPacket(toName, text), player);
                }
            }
            if (getMyPlayer().hasName(toName)) {
                m_chatPanel.logMechanics(text);
            }
        } else if (m_netStatus == NetStatus.CONNECTED) {
            send(PacketManager.makePrivMechanicsPacket(toName, text));
        } else  {
            if (getMyPlayer().hasName(toName)) {
                m_chatPanel.logMechanics(text);
            }
        }
    }
    
    public void postPrivateMessage(final String fromName, final String toName, final String text)
    {
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if you're the host, push to the appropriate player(s)
            for (int i = 0; i < m_players.size(); i++)
            {
                final Player player = m_players.get(i);
                if (player.hasName(toName))
                {
                    // send the message to this player
                    send(PacketManager.makePrivateTextPacket(fromName, toName, text), player);
                }
            }

            // add it to your own text log if we're the right player
            if (getMyPlayer().hasName(toName))
            {
                m_chatPanel.logPrivateMessage(fromName, toName, text);
            }
        }
        else if (m_netStatus == NetStatus.CONNECTED)
        {
            // if you're a player, just post it to the GM
            send(PacketManager.makePrivateTextPacket(fromName, toName, text));
        }
        else
        {
            // if you're offline, post it to yourself if you're the
            // person you sent it to.
            if (getMyPlayer().hasName(toName))
            {
                m_chatPanel.logPrivateMessage(fromName, toName, text);
            }
        }
    }

    public void postSystemMessage(final String text)
    {
        postMechanics(SYSTEM_MESSAGE_FONT + text + END_SYSTEM_MESSAGE_FONT);
    }

    public void privMechanicsPacketReceived(final String toName, final String text)
    {
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if you're the host, push to all players
            postPrivMechanics(toName, text);
        }
        else
        {
            // otherwise, just add it
            m_chatPanel.logMechanics(text);
        }
    }
    
    public void privateTextPacketReceived(final String fromName, final String toName, final String text)
    {
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if you're the host, push to all players
            postPrivateMessage(fromName, toName, text);
        }
        else
        {
            // otherwise, just add it
            m_chatPanel.logPrivateMessage(fromName, toName, text);
        }
    }

    /**
     * Reacquires pogs and then refreshes the pog list.
     */
    public void reacquirePogs()    
    {
    	try
    	{
    		m_pogLibrary.refresh(true);
    	}
    	catch (IOException e)
    	{
    		// .todo proper error handling
    		Log.log(Log.SYS, e.getMessage());
    	}
    	
      refreshPogList();
    }

    public void receiveCards(final Card cards[])
    {
        if (cards.length == 0)
        {
            // drew 0 cards. Ignore.
            return;
        }

        // all of these cards get added to your hand
        for (int i = 0; i < cards.length; i++)
        {
            m_cards.add(cards[i]);
            final String toPost = getLanguageResource().DECK_DREW + " " + cards[i].getCardName() + " (" + cards[i].getDeckName() + ")";
            m_chatPanel.logSystemMessage(toPost);
        }

        // make sure we're on the private layer
        if (m_gametableCanvas.getActiveMap() != m_gametableCanvas.getPrivateMap())
        {
            // we call toggleLayer rather than setActiveMap because
            // toggleLayer cleanly deals with drags in action and other
            // interrupted actions.
            toggleLayer();
        }

        // tell everyone that you drew some cards
        if (cards.length == 1)
        {
            postSystemMessage(getMyPlayer().getPlayerName() + " " + getLanguageResource().DECK_DRAW_PLAYER + " " + cards[0].getDeckName() + " " + getLanguageResource().DECK);
        }
        else
        {
            postSystemMessage(getMyPlayer().getPlayerName() + " " + getLanguageResource().DECK_DRAWS + " " + cards.length + " " + getLanguageResource().DECK_DRAWS2 + " "
                + cards[0].getDeckName() + " " + getLanguageResource().DECK + ".");
        }

    }

    public void recenterPacketReceived(MapCoordinates modelPoint, final int zoom)
    {
        getGametableCanvas().doRecenterView(modelPoint, zoom);

        if (m_netStatus == NetStatus.HOSTING)
        {
            m_networkThread.send(PacketManager.makeRecenterPacket(modelPoint, zoom));
        }
    }

    /**
     * Refreshes the pog list.
     */
    public void refreshPogList()
    {
        m_pogPanel.populateChildren();
        getGametableCanvas().repaint();
    }

    public void rejectPacketReceived(final int reason)
    {
        confirmJoined();

        // you got rejected!
        switch (reason)
        {
            case REJECT_INVALID_PASSWORD:
            {
                m_chatPanel.logAlertMessage(getLanguageResource().JOIN_BAD_PASS);
            }
            break;

            case REJECT_VERSION_MISMATCH:
            {
                m_chatPanel.logAlertMessage(getLanguageResource().JOIN_BAD_VERSION);
            }
            break;
        }
        disconnect();
    }


    public void removePogsPacketReceived(final MapElementID ids[])
    {
        getGametableCanvas().doRemovePogs(ids, false);

        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            send(PacketManager.makeRemovePogsPacket(ids));
        }
    }

    public void requestCardsPacketReceived(final Connection conn, final String deckName, final int numCards)
    {
        if (m_netStatus != NetStatus.HOSTING)
        {
            // this shouldn't happen
            throw new IllegalStateException("Non-host had a call to requestCardsPacketReceived");
        }
        // the player at conn wants some cards
        final Card cards[] = getCards(deckName, numCards);

        if (cards == null)
        {
            // there was a problem. Probably a race-condition thaty caused a
            // card request to get in after a deck was deleted. Just ignore this
            // packet.
            return;
        }

        // send that player his cards
        send(PacketManager.makeReceiveCardsPacket(cards), conn);

        // also, we need to send that player the pogs for each of those cards
        for (int i = 0; i < cards.length; i++)
        {
            final MapElement newPog = makeCardPog(cards[i]);

            if (newPog != null)
            {
                // make a pog packet, saying this pog should go to the PRIVATE LAYER,
                // then send it to that player. Note that we don't add it to
                // our own layer.
                send(PacketManager.makeAddPogPacket(newPog, false), conn);
            }
        }
    }

    public void rotatePogPacketReceived(final MapElementID id, final double newAngle)
    {
        getGametableCanvas().doRotatePog(id, newAngle);

        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            send(PacketManager.makeRotatePogPacket(id, newAngle));
        }
    }

    public void flipPogPacketReceived(final MapElementID id, final boolean flipH, final boolean flipV)
    {
        getGametableCanvas().doFlipPog(id, flipH, flipV);

        if (m_netStatus == NetStatus.HOSTING)
        {
            // if we're the host, send it to the clients
            send(PacketManager.makeFlipPogPacket(id, flipH, flipV));
        }
    }

    public boolean runHostDialog()
    {
        final JoinDialog dialog = new JoinDialog();
        dialog.setUpForHostDlg();
        dialog.setLocationRelativeTo(m_gametableCanvas);
        dialog.setVisible(true);

        if (!dialog.m_bAccepted)
        {
            // they cancelled out
            return false;
        }
        return true;
    }

    private boolean runJoinDialog()
    {
        final JoinDialog dialog = new JoinDialog();
        dialog.setLocationRelativeTo(m_gametableCanvas);
        dialog.setVisible(true);

        if (!dialog.m_bAccepted)
        {
            // they cancelled out
            return false;
        }
        return true;
    }

    /**
     * Saves everything: both maps, macros, and preferences. 
     * Called on program exit.
     */
    private void saveAll()
    {
    	saveToXML(getAutoSaveXMLFile());    	
        //saveState(getGametableCanvas().getPublicMap(), new File("autosave.grm"));
        //saveState(getGametableCanvas().getPrivateMap(), new File("autosavepvt.grm"));
        savePrefs();
    }


    /** 
     * @param file
     * @param pog
     * Saves a Single pog to a File for later loading.
     */
    public void savePog(final File file, final MapElement pog) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);
        try
        {
            
            pog.writeToPacket(dos);
            
            final byte[] saveFileData = baos.toByteArray();
            final FileOutputStream output = new FileOutputStream(file);
            final DataOutputStream fileOut = new DataOutputStream(output);

            // fileOut.writeInt(saveFileData.length);
            fileOut.write(saveFileData);
            output.close();
            fileOut.close();
            baos.close();
            dos.close();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            // failed to save. give up
        }
    }
    public void savePrefs()
    {
        try
        {
            final FileOutputStream prefFile = new FileOutputStream("prefs.prf");
            final DataOutputStream prefDos = new DataOutputStream(prefFile);

            prefDos.writeUTF(m_playerName);
            prefDos.writeUTF(m_characterName);
            prefDos.writeUTF(m_ipAddress);
            prefDos.writeInt(m_port);
            prefDos.writeUTF(m_password);
            
            Point pos = m_gametableCanvas.getScrollPosition();
            prefDos.writeInt(pos.x);
            prefDos.writeInt(pos.y);
            prefDos.writeInt(m_gametableCanvas.getZoomLevel());

            prefDos.writeInt(m_windowSize.width);
            prefDos.writeInt(m_windowSize.height);
            prefDos.writeInt(m_windowPos.x);
            prefDos.writeInt(m_windowPos.y);
            prefDos.writeBoolean(m_bMaximized);

            // divider locations
            prefDos.writeInt(m_mapChatSplitPane.getDividerLocation());
            prefDos.writeInt(m_mapPogSplitPane.getDividerLocation());

            prefDos.writeBoolean(m_showNamesCheckbox.isSelected());

            // new divider location
            prefDos.writeInt(m_mapChatSplitPane.getDividerLocation());
            
            prefDos.writeBoolean(m_chatPanel.getUseMechanicsLog());
            
            for (Module module : g_modules)
            	module.onSavePreferences(prefDos);
            
            prefDos.close();
            prefFile.close();

            for (Module module : g_modules)
            	module.onSavePreferencesCompleted();
        }
        catch (final FileNotFoundException ex1)
        {
            Log.log(Log.SYS, ex1);
        }
        catch (final IOException ex1)
        {
            Log.log(Log.SYS, ex1);
        }
    }
    
    public void saveToXML(final File file)
    {
    	Document doc;
			try
			{
				doc = XMLUtils.createDocument();
			}
			catch (IOException e)
			{
				Log.log(Log.SYS, e.getMessage());
				JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
    	Element root = doc.createElement("gt");
    	doc.appendChild(root);
    	
    	Element publicEl = doc.createElement("public_map");
    	root.appendChild(publicEl);
    	m_gametableCanvas.getPublicMap().serialize(publicEl);
    	
    	Element privateEl = doc.createElement("private_map");
    	root.appendChild(privateEl);
    	m_gametableCanvas.getPrivateMap().serialize(privateEl);

    	storeLockedElementsToXML(doc, root);
      storeGridToXML(doc, root);
      	
    	// Hook for modules to add elements to save file
    	Element modulesEl = doc.createElement("modules");
    	
    	for (Module module : g_modules)
    	{
    		if (module.canSaveToXML())
    		{
    			Element moduleEl = doc.createElement("module");
    			moduleEl.setAttribute("name", module.getModuleName());
    			
    			if (module.saveToXML(moduleEl))
    				modulesEl.appendChild(moduleEl);
    		}    	
    	}
    	
    	// If non-empty, append
    	if (modulesEl.getFirstChild() != null)
    		root.appendChild(modulesEl);

    	//-------------------------------------
    	try
			{
    		XMLOutputProperties props = new XMLOutputProperties();
    		props.indentXML = false;
    		props.encoding = "UTF-8";
    		XMLUtils.saveDocument(file, doc, props);
			}
			catch (IOException e)
			{
				Log.log(Log.SYS, e.getMessage());
				JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
				return;
			}

    }
        
    /**
     * Store locked element list
     * @param doc
     * @param root
     */
    private void storeLockedElementsToXML(Document doc, Element root)
    {
    	List<MapElement> elements = m_gametableCanvas.getlockedMapElementInstances();
    	if (elements.size() == 0)
    		return;
    	
    	Element listEl = doc.createElement("locked");
    	for (MapElement el : elements)
    	{
    		listEl.appendChild(XMLUtils.createElementValue(doc, "id", String.valueOf(el.getId().numeric())));
    	}
    	
    	root.appendChild(listEl);
    }

    /**
     * Store grid information
     * @param doc
     * @param root
     */
		private void storeGridToXML(Document doc, Element root)
		{
			// grid 
    	Element gridEl = doc.createElement("grid");
    	gridEl.setAttribute("modeid", String.valueOf(getGametableCanvas().getGridModeId()));
    	root.appendChild(gridEl);
      
    	// grid background
    	Element bkEl = doc.createElement("background");
    	gridEl.appendChild(bkEl);
    	if (m_gametableCanvas.m_backgroundTypeMapElement && m_gametableCanvas.m_bg_elementType != null)
    	{    		
    		bkEl.setAttribute("element_type", m_gametableCanvas.m_bg_elementType.getFullyQualifiedName());
    	}
    	else
    	{
    		bkEl.setAttribute("color", m_gametableCanvas.cur_bg_col.name());
    	}
		}

    public GroupManager getActiveGroupManager()
    {
    	 return getGametableCanvas().getActiveMap().getGroupManager();
    }

    /**
     * Sends a public message to all players.
     * 
     * @param text Message to send.
     */
    public void say(final String text)
    {
        postMessage(SAY_MESSAGE_FONT + UtilityFunctions.emitUserLink(getMyPlayer().getCharacterName()) + ": "
            + END_SAY_MESSAGE_FONT + text);
    }

    public void send(final byte[] packet)
    {
        if (m_networkThread != null)
        {
            m_networkThread.send(packet);
        }
    }

    public void send(final byte[] packet, final Connection connection)
    {
        if (m_networkThread != null)
        {
            m_networkThread.send(packet, connection);
        }
    }

    public void send(final byte[] packet, final Player player)
    {
        if (player.getConnection() == null)
        {
            return;
        }
        send(packet, player.getConnection());
    }

    public void sendCastInfo()
    {
        // and we have to push this data out to everyone
        for (int i = 0; i < m_players.size(); i++)
        {
            final Player recipient = m_players.get(i);
            final byte[] castPacket = PacketManager.makeCastPacket(recipient);
            send(castPacket, recipient);
        }
    }

    void sendDeckList()
    {
        send(PacketManager.makeDeckListPacket(m_decks));
    }

    public void setToolSelected(final int toolId)
    {
        m_toolButtons[toolId].setSelected(true);
    }

    public boolean shouldShowNames()
    {
        return m_showNamesCheckbox.isSelected();
    }

    /** *************************************************************************************
     * #randomrotate
     * @return
     */
    public boolean shouldRotatePogs() {
        return m_randomRotate.isSelected();        
    }
    
    public void showDeckUsage()
    {
        m_chatPanel.logSystemMessage("/deck usage: ");
        m_chatPanel.logSystemMessage("---/deck create [decktype] [deckname]: create a new deck. [decktype] is the name of a deck in the decks directory. It will be named [deckname]");
        m_chatPanel.logSystemMessage("---/deck destroy [deckname]: remove the specified deck from the session.");
        m_chatPanel.logSystemMessage("---/deck shuffle [deckname] ['all' or 'discards']: shuffle cards back in to the deck.");
        m_chatPanel.logSystemMessage("---/deck draw [deckname] [number]: draw [number] cards from the specified deck.");
        m_chatPanel.logSystemMessage("---/deck hand [deckname]: List off the cards (and their ids) you have from the specified deck.");
        m_chatPanel.logSystemMessage("---/deck /discard [cardID]: Discard a card. A card's ID can be seen by using /hand.");
        m_chatPanel.logSystemMessage("---/deck /discard all: Discard all cards that you have.");
        m_chatPanel.logSystemMessage("---/deck decklist: Lists all the decks in play.");
    }

    public void startTellTo(final String name)
    {
        m_textEntry.setText("/tell " + name + "<b> </b>");
        m_textEntry.requestFocus();
        m_textEntry.toggleStyle("bold");
        m_textEntry.toggleStyle("bold");
    }

    /**
     * Sends a private message to the target player.
     * 
     * @param target Player to address message to.
     * @param text Message to send.
     */
    public void tell(final Player target, final String text)
    {
        if (target.getId() == getMyPlayer().getId())
        {
            m_chatPanel.logMessage(PRIVATE_MESSAGE_FONT + getLanguageResource().TELL_SELF + " " + END_PRIVATE_MESSAGE_FONT + text);
            return;
        }

        final String fromName = getMyPlayer().getCharacterName();
        final String toName = target.getCharacterName();

        postPrivateMessage(fromName, toName, text);

        // and when you post a private message, you get told about it in your
        // own chat log
        m_chatPanel.logMessage(PRIVATE_MESSAGE_FONT + getLanguageResource().TELL + " " + UtilityFunctions.emitUserLink(toName) + ": "
            + END_PRIVATE_MESSAGE_FONT + text);
    }

    public void textPacketReceived(final String text)
    {
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if you're the host, push to all players
            postMessage(text);
        }
        else
        {
            // otherwise, just add it
            m_chatPanel.logMessage(text);
        }
    }
    
    public void mechanicsPacketReceived(final String text)
    {
        if (m_netStatus == NetStatus.HOSTING)
        {
            // if you're the host, push to all players
            postMechanics(text);
        }
        else
        {
            // otherwise, just add it
            m_chatPanel.logMechanics(text);
        }
    }

    private void tick()
    {
        final long now = System.currentTimeMillis();
        long diff = now - m_lastTickTime;
        if (m_lastTickTime == 0)
        {
            diff = 0;
        }
        m_lastTickTime = now;
        tick(diff);
    }

    private void tick(final long ms)
    {
        // System.out.println("tick(" + ms + ")");
        final NetworkThread thread = m_networkThread;
        if (thread != null)
        {
            final Set<Connection> lostConnections = thread.getLostConnections();
            for (Connection connection : lostConnections)
            {
                connectionDropped(connection);
            }
            
            for (Packet packet : thread.getPackets())
            {
                packetReceived(packet.getSource(), packet.getData());
            }

            if (SEND_PINGS)
            {
                m_lastPingTime += ms;
                if (m_lastPingTime >= PING_INTERVAL)
                {
                    send(PacketManager.makePingPacket());
                    m_lastPingTime -= PING_INTERVAL;
                }
            }
            updateStatus();
        }
        m_gametableCanvas.tick(ms);
    }

    /**
     * Toggles between the two layers.
     */
    public void toggleLayer()
    {
        // toggle the map we're on
    	boolean publicMap = getGametableCanvas().isPublicMap(); 
      if (publicMap)
      {
          getGametableCanvas().setActiveMap(getGametableCanvas().getPrivateMap());
      }
      else
      {
          getGametableCanvas().setActiveMap(getGametableCanvas().getPublicMap());
      }

        // if they toggled the layer, whatever tool they're using is cancelled
        getToolManager().cancelToolAction();
        getGametableCanvas().requestFocus();
        
        for (Module module : g_modules)
        	module.onToggleActiveMap(!publicMap);
        
        repaint();
    }

    public void typingPacketReceived(final String playerName, final boolean typing)
    {
        if (typing)
        {
            m_typingPlayerNames.add(playerName);
        }
        else
        {
            m_typingPlayerNames.remove(playerName);
        }

        if (m_netStatus == NetStatus.HOSTING)
        {
            send(PacketManager.makeTypingPacket(playerName, typing));
        }
    }

    public void updateCast(final Player[] players, final int ourIdx)
    {
        // you should only get this if you're a joiner
        confirmJoined();

        // set up the current cast
        m_players = new ArrayList<Player>();
        for (int i = 0; i < players.length; i++)
        {
            addPlayer(players[i]);
        }

        m_myPlayerIndex = ourIdx;
    }

    public void updateGridModeMenu()
    {
        if (getGametableCanvas().m_gridMode == getGametableCanvas().m_noGridMode)
        {
            m_noGridModeMenuItem.setState(true);
            m_squareGridModeMenuItem.setState(false);
            m_hexGridModeMenuItem.setState(false);
        }
        else if (getGametableCanvas().m_gridMode == getGametableCanvas().m_squareGridMode)
        {
            m_noGridModeMenuItem.setState(false);
            m_squareGridModeMenuItem.setState(true);
            m_hexGridModeMenuItem.setState(false);
        }
        else if (getGametableCanvas().m_gridMode == getGametableCanvas().m_hexGridMode)
        {
            m_noGridModeMenuItem.setState(false);
            m_squareGridModeMenuItem.setState(false);
            m_hexGridModeMenuItem.setState(true);
        }
    }

    public void updateStatus()
    {
        switch (m_netStatus)
        {
            case DISCONNECTED:
                m_status.setText(" " + getLanguageResource().DISCONNECTED);
                m_actionLoadMap.setEnabled(true);
                m_actionLoadPrivateMap.setEnabled(true);
                m_actionLoadPublicMap.setEnabled(true);
                break;

            case CONNECTED:
                m_status.setText(" " + getLanguageResource().CONNECTED + ": ");
                m_actionLoadMap.setEnabled(false);
                m_actionLoadPrivateMap.setEnabled(true);
                m_actionLoadPublicMap.setEnabled(false);
                break;

            case HOSTING:
                m_status.setText(" " + getLanguageResource().HOSTING + ": ");
                m_actionLoadMap.setEnabled(true);
                m_actionLoadPrivateMap.setEnabled(true);
                m_actionLoadPublicMap.setEnabled(true);
                break;

            default:
                m_status.setText(" " + getLanguageResource().UNKNOWN_STATE + " ");
                m_actionLoadMap.setEnabled(false);
                m_actionLoadPrivateMap.setEnabled(true);
                m_actionLoadPublicMap.setEnabled(false);
                break;
        }

        if (m_netStatus != NetStatus.DISCONNECTED)
        {
            m_status.setText(m_status.getText() + m_players.size() + (m_players.size() == 1 ? " player" : " players")
                + " " + getLanguageResource().CONNECTED);
            switch (m_typingPlayerNames.size())
            {
                case 0:
                {
                }
                break;

                case 1:
                {
                    m_status.setText(m_status.getText() + m_typingPlayerNames.get(0) + " " + getLanguageResource().IS_TYPING);
                }
                break;

                case 2:
                {
                    m_status.setText(m_status.getText() + m_typingPlayerNames.get(0) + " " + getLanguageResource().AND + " " + m_typingPlayerNames.get(1) + " " + getLanguageResource().ARE_TYPING);
                }
                break;

                default:
                {
                    for (int i = 0; i < m_typingPlayerNames.size() - 1; i++)
                    {
                        m_status.setText(m_status.getText() + m_typingPlayerNames.get(i) + ", ");
                    }
                    m_status.setText(m_status.getText() + " " + getLanguageResource().AND + " " + m_typingPlayerNames.get(m_typingPlayerNames.size() - 1) + " " + getLanguageResource().ARE_TYPING);
                }
            }
        }
    }

    /**
     * Records the current state of the window.
     */
    public void updateWindowInfo()
    {
        // we only update our internal size and
        // position variables if we aren't maximized.
        if ((getExtendedState() & MAXIMIZED_BOTH) != 0)
        {
            m_bMaximized = true;
        }
        else
        {
            m_bMaximized = false;
            m_windowSize = getSize();
            m_windowPos = getLocation();
        }
    }
    
    /*
    * @see com.galactanet.gametable.data.MapElementRepositoryIF#getMapElement(com.galactanet.gametable.data.MapElementID)
    */
    @Override
    public MapElement getMapElement(MapElementID id)
    {
    	MapElement el = m_gametableCanvas.getPublicMap().getMapElement(id);
    	if (el == null)
    		el = m_gametableCanvas.getPrivateMap().getMapElement(id);

    	return el;
    }
    
    /**
     * Register a new module with GameTable
     * @param module
     */
    public static void registerModule(Module module)
    {
    	g_modules.remove(module);
    	g_modules.add(module);
    }
    
    /**
     * @revise Temporary holding - we'll want something more gobal
     */
    private void buildActions()
    {
	    m_actionLoadMap = new AbstractAction(getLanguageResource().MAP_OPEN) {				
				@Override
				public void actionPerformed(ActionEvent e)
				{
					loadMap(true, true);
				}
			};
			
			m_actionLoadPrivateMap = new AbstractAction("Load Private Map") {				
				@Override
				public void actionPerformed(ActionEvent e)
				{
					loadMap(false, true);
				}
			};
			
			m_actionLoadPublicMap = new AbstractAction("Load Public Map") {				
				@Override
				public void actionPerformed(ActionEvent e)
				{
					loadMap(true, false);
				}
			};
    }

		/**
		 * @return Returns the lang.
		 */
		public Language getLanguageResource()
		{
			return m_languageResource;
		}
		
		/**
		 * 
		 * @return
		 */
		public PogWindow getTabbedPane()
		{
			return m_pogsTabbedPane;
		}

		private javax.swing.Action m_actionLoadMap;
    private javax.swing.Action m_actionLoadPrivateMap;
    private javax.swing.Action m_actionLoadPublicMap;
    
    private static List<Module> g_modules = new ArrayList<Module>();
    
}