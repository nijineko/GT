/*
 * GametableFrame.java: GameTable is in the private Domain.
 */

package com.gametable.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
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

import com.gametable.GametableApp;
import com.gametable.data.*;
import com.gametable.data.ChatEngineIF.MessageType;
import com.gametable.data.GameTableCore.MapType;
import com.gametable.data.grid.GridModeID;
import com.gametable.data.net.NetLoadMap;
import com.gametable.module.Module;
import com.gametable.net.*;
import com.gametable.ui.GametableCanvas.ZoomLevel;
import com.gametable.ui.chat.ChatLogEntryPane;
import com.gametable.ui.chat.ChatPanel;
import com.gametable.ui.chat.SlashCommands;
import com.gametable.ui.modes.*;
import com.gametable.ui.net.NetRecenterMap;
import com.gametable.ui.net.NetSendTypingFlag;
import com.gametable.util.*;
import com.maziade.messages.MessageDefinition;
import com.maziade.messages.MessageID;
import com.maziade.messages.MessageListener;
import com.maziade.messages.MessagePriority;
import com.maziade.props.XProperties;
import com.maziade.props.ui.XPropertiesDialog;
import com.maziade.tools.ui.ModalDialog.ReturnValue;

/**
 * The main Gametable Frame class. This class handles the display of the application objects and the response to user
 * input
 * 
 * @author sephalon
 * 
 *         #GT-AUDIT GametableFrame
 * 
 * @revise extract interface to help prevent unwanted operations on the frame
 */
public class GametableFrame extends JFrame implements ActionListener, MessageListener
{
	
	private static final String PROPERTY_BUNDLE_NAME = "com.gametable.ui.GametableFrame";	// Set as string not to hard link to class name (in case of refactoring)

	public static final String RESOURCE_PATH = "com.gametable.ui.resources";
	
	private static String getMenuAccelerator()
	{
		switch (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
		{
		case Event.CTRL_MASK:
			return "ctrl";
		case Event.META_MASK:
			return "meta";
			// case Event.ALT_MASK:
			// return "alt";
		case Event.SHIFT_MASK:
			return "shift";
		default: // Others?
			return "ctrl";
		}
	}

	/**
	 * Constructor - should only be called by GameTableApp
	 * 
	 * @throws IOException
	 */
	public GametableFrame() throws IOException
	{
		m_core = GametableApp.getCore();
	}

	/**
	 * actionPerformed is an event handler for some of the controls in the frame
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		/*
		 * Added in order to accomodate grid unit multiplier
		 */
		if (e.getSource() == m_gridunit)
		{
			// If the event is triggered by the grid unit drop down,
			// get the selected unit
			m_gridUnit = (String) (m_gridunit.getSelectedItem());
		}

		if (e.getSource() == m_colorCombo)
		{
			// If the event is triggered by the color drow down,
			// Get the selected color
			final Integer col = (Integer) m_colorCombo.getSelectedItem();
			m_drawColor = new Color(col.intValue());
		}
		else if (e.getSource() == m_noGridModeMenuItem)
		{
			// If the event is triggered by the "No Grid Mode" menu item then
			// remove the grid from the canvas
			// Set the Gametable canvas in "No Grid" mode
			m_core.setGridMode(GridModeID.NONE);

			// Notify other players
			m_core.sendMessageBroadcast(MessageType.SYSTEM, m_core.getPlayer().getPlayerName() + " " + "changes the grid mode.");
		}
		else if (e.getSource() == m_squareGridModeMenuItem)
		{
			// If the event is triggered by the "Square Grid Mode" menu item,
			// adjust the canvas accordingly
			// Set the Gametable canvas in "Square Grid mode"

			m_core.setGridMode(GridModeID.SQUARES);

			// Check and uncheck menu items
			updateGridModeMenu();
			// Repaint the canvas
			getGametableCanvas().repaint();
			// Notify other players
			m_core.sendMessageBroadcast(MessageType.SYSTEM, m_core.getPlayer().getPlayerName() + " " + "changes the grid mode.");
		}
		else if (e.getSource() == m_hexGridModeMenuItem)
		{
			// If the event is triggered by the "Hex Grid Mode" menu item,
			// adjust the canvas accordingly
			// Set the Gametable canvas in "Hex Grid Mode"
			m_core.setGridMode(GridModeID.HEX);
			// Check and uncheck menu items
			updateGridModeMenu();
			// Repaint the canvas
			getGametableCanvas().repaint();
			// Notify other players
			m_core.sendMessageBroadcast(MessageType.SYSTEM, m_core.getPlayer().getPlayerName() + " " + "changes the grid mode.");
		}
	}

	/**
	 * Add a user interface action that can be performed by the user.
	 * 
	 * @param action Action that can be triggered by the user.
	 * 
	 *          Frame can use any of the standard values set into the Action object. Additionally, you can set
	 *          ACTION_DEFAULT_AS_BUTTON to true to request that this action be added to the toolbar (user settings could
	 *          override this)
	 *
	 */
	public void addUserInterfaceAction(Action action)
	{
		// action.putValue(ACTION_DEFAULT_AS_BUTTON, Boolean.valueOf(defaultAsButton));
		m_moduleActions.add(action);
	}

	/**
	 * Loads preferences from file
	 * 
	 * #TODO Visibility used by core
	 */
	public void applyProperties()
	{
		XProperties props = m_core.getProperties();

		Point pt = XProperties.toPoint(props.getTextPropertyValue(PROP_SCROLL_POSITION), new Point(0, 0));
		getGametableCanvas().setScrollPosition(pt.x, pt.y);
		
		ZoomLevel level = ZoomLevel.LEVEL1;
		
		try
		{
			level = ZoomLevel.valueOf(props.getTextPropertyValue(PROP_ZOOM_LEVEL));
		}
		catch (IllegalArgumentException e)
		{
			// Ignore, keep default
		}
		catch (NullPointerException e)
		{
			// Ignore, keep default
		}
		
		getGametableCanvas().setZoomLevel(level);

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		m_windowSize = XProperties.toDimension(props.getTextPropertyValue(PROP_WINDOW_SIZE), new Dimension(DEFAULT_WINDOWSIZE_WIDTH,
				DEFAULT_WINDOWSIZE_HEIGHT));

		m_windowPos = XProperties.toPoint(props.getTextPropertyValue(PROP_WINDOW_POSITION), new Point((screenSize.width - m_windowSize.width) / 2,
				(screenSize.height - m_windowSize.height) / 2));
		m_bMaximized = props.getBooleanPropertyValue(PROP_MAXIMIZED);
		applyWindowInfo();

		// divider locations
		float f = props.getNumberFloatPropertyValue(PROP_CHAT_SPLIT);
		if (f > 0 && f <= 1)
			m_mapChatSplitPane.setDividerLocation(f);
		else
			m_mapChatSplitPane.setDividerLocation(props.getNumberPropertyValue(PROP_CHAT_SPLIT));

		m_mapPogSplitPane.setDividerLocation(props.getNumberPropertyValue(PROP_COLUMN_SPLIT));

		m_showNamesCheckbox.setSelected(props.getBooleanPropertyValue(PROP_SHOW_NAMES_ON_MAP));
		m_randomRotate.setSelected(props.getBooleanPropertyValue(PROP_RANDOM_ROTATE));

		m_chatPanel.setUseMechanicsLog(props.getBooleanPropertyValue(PROP_USE_CHAT_MECHANICS));

	}

	// private final static boolean USE_NEW_CHAT_PANE = true;

	/**
	 * Center the view on the map at given coordinates
	 * 
	 * @param modelCenter Coordinates we want to center on
	 * @param zoomLevel Requested zoom level
	 * @param netEvent Network event
	 */
	public void centerView(MapCoordinates modelCenter, final ZoomLevel zoomLevel, NetworkEvent netEvent)
	{
		if (shouldPropagateChanges(netEvent))
			m_core.sendBroadcast(NetRecenterMap.makePacket(modelCenter, zoomLevel));

		m_gametableCanvas.centerView(modelCenter, zoomLevel);
	}

	/**
	 * Creates a copy of a specified map element and places it on the current map
	 * 
	 * @param mapElement MapElement instance to copy
	 */
	public void copyMapElement(final MapElement mapElement)
	{
		final MapElement newMapElement = new MapElement(mapElement);
		m_core.getMap(GameTableCore.MapType.ACTIVE).addMapElement(newMapElement);
	}
	
	/**
	 * Create a cursor from resource
	 * 
	 * @param cursorFileName Name of the cursor file (will look within PATH_CURSORS for the props file containing other information)
	 * @return Cursor instance
	 */
	public Cursor createMapCursor(String cursorName)
	{
		Cursor cursor = m_cursors.get(cursorName);
		if (cursor != null)
			return cursor;
		
		Properties props = new Properties();
		try
		{
			props.load(new FileReader(new File(GametableFrame.PATH_CURSORS + cursorName + ".props")));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
		
		String imageFile = GametableFrame.PATH_CURSORS + props.getProperty("image");

		Image cursorImage = ImageCache.getImage(new File(imageFile));
		if (cursorImage != null)
		{
			Toolkit kit = Toolkit.getDefaultToolkit();
			int w = cursorImage.getWidth(this);
			int h = cursorImage.getHeight(this);
			
			Dimension d = kit.getBestCursorSize(w, h);
			if (d.width !=  w || d.height != h)
			{
				BufferedImage image = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = image.getGraphics();
		    g.drawImage(cursorImage, 0, 0, null);
		    
		    cursorImage = image;
			}
			
			Point cursorHotspot = XProperties.toPoint(props.getProperty("hotspot"), new Point(0, 0));
			cursor = kit.createCustomCursor(cursorImage, cursorHotspot, cursorName);
		}

		if (cursor != null)
			m_cursors.put(cursorName, cursor);

		return cursor;
	}

	/**
	 * Executes a Message. Called from MessageQueue. (This is an event message queue, different from the network
	 * messages.)
	 * 
	 * @param id Identifies the message
	 * @param priority message priority
	 * @param parmeter message parameter
	 * @param debug debug string
	 */
	@Override
	public void executeMessage(MessageID messageID, MessagePriority priority, Object parameter, String debug)
	{
		if (messageID == MSGID_REFRESH_MAP_LIBRARY)
		{
			refreshMapElementList();
		}
	}

	/**
	 * Get the currently selected draw color from the tool bar
	 * 
	 * @return
	 */
	public Color getDrawColor()
	{
		return m_drawColor;
	}

	/**
	 * Get the conversion rate to convert a tile to grid units (ex 5, as in 1 tile = 5 ft)
	 * 
	 * @return conversion rate
	 */
	public double getGridUnitConversionRate()
	{
		return m_gridUnitConversionRate;
	}

	/**
	 * Get the currently set grid unit (ex: ft)
	 * 
	 * @return
	 */
	public String getGridUnit()
	{
		return m_gridUnit;
	}

	/**
	 * Gets the list of highlighted element instances
	 * 
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElement> getHighlightedMapElementInstances()
	{
		return m_highlightedElements.getSelectedMapElements();
	}

	/**
	 * Get the cursor matching the cursor file name from cache. Call createMapCursor to create/override an existing cursor
	 * 
	 * @param cursorFileName Name of the cursor file
	 * 
	 * @return Cursor instance
	 */
	public Cursor getMapCursor(String cursorFileName)
	{
		return m_cursors.get(cursorFileName);
	}

	/**
	 * Get the scroll position in map coordinates
	 * 
	 * @return Map coordinates
	 */
	public MapCoordinates getMapScrollPosition()
	{
		return m_gametableCanvas.viewToModel(m_gametableCanvas.getScrollX(), m_gametableCanvas.getScrollY());
	}

	/**
	 * TODO #MapElementTypeLibraryPanel - Move
	 * 
	 * @return The pog panel.
	 */
	public MapElementTypeLibraryPanel getPogPanel()
	{
		return m_pogPanel;
	}

	/**
	 * Gets selected map element instances list
	 * 
	 * @param mapType Select from active, public or private map
	 * @return The list of currently selected instances (unmodifiable). Never null.
	 */
	public List<MapElement> getSelectedMapElementInstances(MapType mapType)
	{
		return getSelectionHandler(mapType).getSelectedMapElements();
	}

	/**
	 * Get the tabbed pane TODO #PogWindow - It is the tabbed pane. Both bad names
	 * 
	 * @return tabbed pane instance
	 */
	public PogWindow getTabbedPane()
	{
		return m_pogsTabbedPane;
	}

	/**
	 * Get the UI's current mode
	 * 
	 * @return UI mode
	 */
	public UIMode getUIMode()
	{
		return m_modeCurrent;
	}

	/**
	 * Highlight or 'unhighlight' all element instances
	 * 
	 * @param highlight true to highlight, false to 'unhighlight'
	 */
	public void highlightAllMapElementInstances(boolean highlight)
	{
		if (highlight)
			m_highlightedElements.selectMapElements(m_core.getMap(GameTableCore.MapType.ACTIVE).getMapElements(), highlight);
		else
			m_highlightedElements.unselectAllMapElements();

		repaint();
	}

	/**
	 * Set an element as highlighted
	 * 
	 * @param mapElement Map element to highlight
	 * @param highlight true to highlight, false to 'unhighlight'
	 */
	public void highlightMapElementInstance(MapElement mapElement, boolean highlight)
	{
		m_highlightedElements.selectMapElement(mapElement, highlight);
		repaint();
	}

	/**
	 * Highlight or 'unhighlight' a list of instances
	 * 
	 * @param mapElements list of instances to change highlight status
	 * @param highlight true to highlight, false to 'unhighlight'
	 */
	public void highlightMapElementInstances(List<MapElement> mapElements, boolean highlight)
	{
		m_highlightedElements.selectMapElements(mapElements, highlight);
		repaint();
	}

	/**
	 * Performs initialization. Called by the main application only
	 * 
	 * todo #Initialize - make this visible only to application instance
	 */
	public void initialize()
	{
		try
		{
			Thread.setDefaultUncaughtExceptionHandler(new FrameUncaughtExceptionHandler());

			m_selectionPublic = new SelectionHandler();
			m_selectionPrivate = new SelectionHandler();
			m_highlightedElements = new SelectionHandler();

			ImageCache.startCacheDaemon();

			SlashCommands.registerDefaultChatCommands();

			m_networkResponder = new NetworkFrameResponder();

			// Initialize network module
			NetworkModuleIF networkModule = m_core.getNetworkModule();
			networkModule.registerMessageType(NetRecenterMap.getMessageType());
			networkModule.registerMessageType(NetSendTypingFlag.getMessageType(m_networkResponder));

			initializeCoreListeners();

			buildActions();

			if (DEBUG_FOCUS) // if debugging
			{
				final KeyboardFocusManager man = KeyboardFocusManager.getCurrentKeyboardFocusManager();
				man.addPropertyChangeListener(new PropertyChangeListener() {
					/*
					 * If debugging,show changes to properties in the console
					 * 
					 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
					 */
					public void propertyChange(final PropertyChangeEvent e)
					{
						System.out.println(e.getPropertyName() + ":\n    " + e.getOldValue() + "\n -> " + e.getNewValue());
					}

				});
			}

			// Configure chat panel
			m_chatPanel = new ChatPanel(this);

			m_core.registerChatEngine(m_chatPanel);

			m_textEntry = m_chatPanel.getTextEntry();

			setContentPane(new JPanel(new BorderLayout())); // Set the main UI object with a Border Layout
			setDefaultCloseOperation(EXIT_ON_CLOSE); // Ensure app ends with this frame is closed
			setTitle(GametableApp.VERSION); // Set frame title to the current version
			setJMenuBar(getMainMenuBar()); // Set the main MenuBar

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

			initializeToolBar();
			m_toolBar.add(Box.createHorizontalStrut(5));

			/*
			 * Added in order to accomodate grid unit multiplier
			 */
			m_gridunitmultiplier = new JTextField("5", 3);
			m_gridunitmultiplier.setMaximumSize(new Dimension(42, 21));

			// Configure the units dropdown
			final String[] units = { "ft", "m", "u" };
			m_gridunit = new JComboBox(units);
			m_gridunit.setMaximumSize(new Dimension(42, 21));
			m_toolBar.add(m_gridunitmultiplier);
			m_toolBar.add(m_gridunit);

			// Add methods to react to changes to the unit multiplier
			// todo : this is definitely better somewhere else
			m_gridunitmultiplier.getDocument().addDocumentListener(new DocumentListener() {
				// todo exceptions should be captured
				public void changedUpdate(final DocumentEvent e)
				{
					m_gridUnitConversionRate = Double.parseDouble(m_gridunitmultiplier.getText());
				}

				public void insertUpdate(final DocumentEvent e)
				{
					m_gridUnitConversionRate = Double.parseDouble(m_gridunitmultiplier.getText());
				}

				public void removeUpdate(final DocumentEvent e)
				{
					// Commented out to fix grid measurement size error
					// grid_multiplier = Double.parseDouble(m_gridunitmultiplier.getText());
				}
			});
			m_gridunit.addActionListener(this);

			// Configure the checkbox to show names
			m_showNamesCheckbox.setFocusable(false);

			// @revise consider to place this somewhere else in UI
			m_showNamesCheckbox.addActionListener(new ActionListener() {
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
			m_toolBar.add(m_randomRotate); // #randomrotate

			getContentPane().add(m_toolBar, BorderLayout.NORTH);

			// getGametableCanvas().init(this);
			//		
			// initializeGroupManager();
			//
			initializeMapElementTypeLibrary();

			// pogWindow
			m_pogPanel = new MapElementTypeLibraryPanel(m_core.getMapElementTypeLibrary(), getGametableCanvas());
			m_pogsTabbedPane.addTab(m_pogPanel, getLanguageResource().POG_LIBRARY);

			for (Module module : m_core.getRegisteredModules())
			{
				// @revise #Plugins consider abstracting "PogsTabbedPane" through an interface.
				module.onInitializeUI(this);
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

			m_colorCombo.addActionListener(this);
			updateGridModeMenu();

			addComponentListener(new ComponentAdapter() {
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
			addWindowListener(new WindowAdapter() {
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

			m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SLASH"), "startSlash");
			m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed ENTER"), "startText");
			m_gametableCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control pressed R"), "reply");

			m_gametableCanvas.getActionMap().put("startSlash", new AbstractAction() {
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

			m_gametableCanvas.getActionMap().put("startText", new AbstractAction() {
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

			// m_gametableCanvas.getActionMap().put("reply", new AbstractAction()
			// {
			// /*
			// * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			// */
			// public void actionPerformed(final ActionEvent e)
			// {
			// // we don't do this if there's already text in the entry field
			// if (m_textEntry.getText().length() == 0)
			// {
			// // if they've never received a tell, just tell them that
			// if (m_lastPrivateMessageSender == null)
			// {
			// // they've received no tells yet
			// m_chatPanel.logAlertMessage("You cannot reply until you receive a /tell from another player.");
			// }
			// else
			// {
			// startTellTo(m_lastPrivateMessageSender);
			// }
			// }
			// m_textEntry.requestFocus();
			// }
			// });

			m_gametableCanvas.init();

			initializeExecutorThread();

		}
		catch (final Exception e)
		{
			Log.log(Log.SYS, e);
		}
	}

	/**
	 * Initialize properties before load - called by core todo #Initialize visibility - restrict to core
	 */
	public void initializeProperties()
	{
		XProperties props = m_core.getProperties();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		props.addBooleanProperty(PROP_SHOW_NAMES_ON_MAP, false, true, "map_options", -1, RESOURCE_PATH);
		props.addBooleanProperty(PROP_USE_CHAT_MECHANICS, false, true, "map_options", -1, RESOURCE_PATH);
		props.addBooleanProperty(PROP_RANDOM_ROTATE, false, true, "map_options", -1, RESOURCE_PATH);

		props.addTextProperty(PROP_SCROLL_POSITION, XProperties.fromPoint(new Point(0, 0)), false, "window", -1, RESOURCE_PATH);
		props.addNumberProperty(PROP_ZOOM_LEVEL, 0, false, "window", -1, RESOURCE_PATH);
		props.addTextProperty(PROP_WINDOW_SIZE, XProperties.fromDimension(new Dimension(800, 600)), false, "window", -1, RESOURCE_PATH);

		props.addTextProperty(PROP_WINDOW_POSITION, XProperties.fromPoint(new Point((screenSize.width - DEFAULT_WINDOWSIZE_WIDTH) / 2,
				(screenSize.height - DEFAULT_WINDOWSIZE_HEIGHT) / 2)), false, "window", -1, RESOURCE_PATH);
		props.addBooleanProperty(PROP_MAXIMIZED, false, false, "window", -1, RESOURCE_PATH);
		props.addNumberProperty(PROP_CHAT_SPLIT, 0.7f, false, "window", -1, RESOURCE_PATH);
		props.addNumberProperty(PROP_COLUMN_SPLIT, 150, false, "window", -1, RESOURCE_PATH);
	}

	/**
	 * Checks if a specific map element is marked as highlighted
	 * 
	 * @param mapElement Map element to highlight
	 * @return true if highlighted
	 */
	public boolean isHighlighted(MapElement mapElement)
	{
		return m_highlightedElements.isSelected(mapElement);
	}

	/**
	 * Verifies if map coordinates are currently visible on screen
	 * 
	 * @param mapPos Position on the map
	 * @return true if visible, false otherwise
	 */
	public boolean areMapCoordinatesVisible(final MapCoordinates mapPos)
	{
		return m_gametableCanvas.areMapCoordinatesVisible(mapPos);
	}

	/**
	 * Verifies if specified element is selected
	 * 
	 * @param mapElement element to verify
	 * @param mapType Select from active, public or private map
	 * @return true if selected
	 */
	public boolean isSelected(MapElement mapElement, MapType mapType)
	{
		return getSelectionHandler(mapType).isSelected(mapElement);
	}

	/**
	 * Converts a distance in map units to a "physical world" distance
	 * 
	 * @param distance Distance in grid units (a tile is broken into GameTableMap.getBaseSquareSize() units)
	 * @return A "physical world" distance (in feet or other - depending on the maps's setting)
	 */
	public double mapDistanceToGridUnits(final double distance)
	{
		return (getGridUnitConversionRate() * distance / GameTableMap.getBaseTileSize());
	}

	/**
	 * Move the specified map element and all linked map elements
	 * 
	 * @param mapElementID ID of an element to move. The UI will move all linked elements as well (grouped or selected)
	 * @param position Destination
	 */
	public void moveLinkedMapElements(final MapElementID mapElementID, MapCoordinates position)
	{
		// TODO #MoveLinkedMapElements Would like a more open architecture here to allow other grouping components to
		// graft themselves here later on - which would be what 'groups' and 'selection' would then be doing.

		GameTableMap activeMap = m_core.getMap(GameTableCore.MapType.ACTIVE);

		final MapElement movingMapElement = activeMap.getMapElement(mapElementID);
		int diffx = position.x - movingMapElement.getPosition().x;
		int diffy = position.y - movingMapElement.getPosition().y;

		Group group = activeMap.getGroupManager().getGroup(movingMapElement);

		if (isSelected(movingMapElement, MapType.ACTIVE))
		{
			// converted to array to prevent concurrent modification issues
			for (MapElement mapElement : getSelectedMapElementInstances(MapType.ACTIVE).toArray(new MapElement[0]))
			{
				if (mapElement.getID() != mapElementID)
				{
					MapCoordinates newPos = mapElement.getPosition().delta(diffx, diffy);
					mapElement.setPosition(newPos);
				}
			}
		}
		else if (group != null)
		{
			List<MapElement> mapElements = group.getMapElements();

			for (MapElement mapElement : mapElements)
			{
				if (mapElement != movingMapElement)
				{
					MapCoordinates newPos = mapElement.getPosition().delta(diffx, diffy);
					mapElement.setPosition(newPos);
				}
			}
		}

		movingMapElement.setPosition(position);
	}

	/**
	 * Register a UI mode for use with the application
	 * 
	 * @param uiMode User Interface Mode descriptor
	 * @param modeListener User Interface Mode listener
	 */
	public void registerUIMode(UIMode uiMode, UIModeListener modeListener)
	{
		m_uiModes.put(uiMode, modeListener);
	}

	/**
	 * Save a map element to file
	 * 
	 * @param mapElement Map element to save
	 * @param file File to save map element to
	 */
	public void saveMapElement(MapElement mapElement, File file)
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			mapElement.writeToPacket(dos);

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

	/**
	 * Scroll map to given map coordinates
	 * 
	 * @param mapPos map coordinates
	 */
	public void scrollMapTo(MapCoordinates mapPos)
	{
		m_gametableCanvas.scrollMapTo(mapPos);
	}

	/**
	 * Scroll the map so the specified map element is centered on screen
	 * 
	 * @param mapElement
	 */
	public void scrollToMapElement(final MapElement mapElement)
	{
		m_gametableCanvas.scrollToMapElement(mapElement);
	}

	/**
	 * Adds a instance to the selected list
	 * 
	 * @param mapElement Instance to add to selection
	 * @param mapType Select from active, public or private map
	 * @param select true to select, false to unselect
	 */
	public void selectMapElementInstance(MapElement mapElement, MapType mapType, boolean select)
	{
		getSelectionHandler(mapType).selectMapElement(mapElement, select);
	}

	/**
	 * Add multiple instances to the selection
	 * 
	 * @param mapElements List of instance to add to the selection
	 * @param mapType Select from active, public or private map
	 * @param select true to select, false to unselect
	 */
	public void selectMapElementInstances(final List<MapElement> mapElements, MapType mapType, boolean select)
	{
		getSelectionHandler(mapType).selectMapElements(mapElements, select);
	}

	/**
	 * Set the cursor for the map
	 * 
	 * @param cursor Cursor instance. Null for default cursor.
	 */
	public void setMapCursor(Cursor cursor)
	{
		m_gametableCanvas.setCursor(cursor);
	}

	/**
	 * Sets the active UI Mode
	 * 
	 * @param mode UIMode instance
	 * @return previous mode
	 */
	public UIMode setUIMode(UIMode mode)
	{
		if (mode == null)
			mode = MapElementMode.getUIMode();

		UIMode oldMode = getUIMode();
		if (oldMode != null)
		{
			UIModeListener listener = m_uiModes.get(oldMode);
			if (listener != null)
				listener.cancelMode();
		}

		UIModeListener listener = m_uiModes.get(mode);
		if (listener != null)
			listener.selectMode();

		m_modeCurrent = mode;

		return oldMode;
	}

	/**
	 * See if the 'random rotate' option has been enabled 
	 * @return true if elements should be rotated randomly
	 * 
	 * TODO #Properties move this to properties
	 * TODO #RandomRotate make RandomRotate a plugin feature
	 */
	public boolean shouldRotateMapElements()
	{
		return m_randomRotate.isSelected();
	}

	/**
	 * See if the 'show names' option has been enabled
	 * @return true if names should be shown
	 * 
	 * TODO #Properties move this to properties
	 */
	public boolean shouldShowNames()
	{
		return m_showNamesCheckbox.isSelected();
	}

	/**
	 * Snap map coordinates to the grid
	 * 
	 * @param mapPos Coordinates to snap
	 * @return Snapped coordinates
	 */
	public MapCoordinates snapToGrid(final MapCoordinates mapPos)
	{
		return m_core.getGridMode().getSnappedMapCoordinates(mapPos);
	}

	/**
	 * Start the application
	 * TODO #Visibiility make this visible to app only
	 */
	public void start()
	{
		setVisible(true);

		// load the primary map
		File autoSave = getAutoSaveXMLFile();
		if (autoSave.exists())
			loadMapFromXML(autoSave, true, true, null);//
	}

	/**
	 * Remove all instance from selection
	 * 
	 * @param mapType Select from active, public or private map
	 */
	public void unselectAllMapElementInstances(MapType mapType)
	{
		getSelectionHandler(mapType).unselectAllMapElements();
	}

	/**
	 * Update properties before save - called by core
	 * TODO #Visibility make visible to core only
	 */
	public void updateProperties()
	{
		XProperties props = m_core.getProperties();

		props.setBooleanPropertyValue(PROP_SHOW_NAMES_ON_MAP, m_showNamesCheckbox.isSelected());
		props.setBooleanPropertyValue(PROP_USE_CHAT_MECHANICS, m_chatPanel.getUseMechanicsLog());
		props.setTextPropertyValue(PROP_SCROLL_POSITION, XProperties.fromPoint(m_gametableCanvas.getScrollPosition()));
		props.setTextPropertyValue(PROP_ZOOM_LEVEL, m_gametableCanvas.getZoomLevel().name());
		props.setTextPropertyValue(PROP_WINDOW_SIZE, XProperties.fromDimension(m_windowSize));
		props.setTextPropertyValue(PROP_WINDOW_POSITION, XProperties.fromPoint(m_windowPos));
		props.setBooleanPropertyValue(PROP_MAXIMIZED, m_bMaximized);
		props.setNumberPropertyValue(PROP_CHAT_SPLIT, m_mapChatSplitPane.getDividerLocation());
		props.setNumberPropertyValue(PROP_COLUMN_SPLIT, m_mapPogSplitPane.getDividerLocation());
	}

	// --- MenuItems ---

	/**
	 * Get the listener for the specified UI mode.
	 * 
	 * Used by GameTableCanvas to send mousing events to registered modes.
	 * 
	 * @param uiMode UI Mode. If null, uses the active mode
	 * 
	 * @return UIModeListener or null if no listener has been registered
	 */
	protected UIModeListener getUIModeListener(UIMode uiMode)
	{
		if (uiMode == null)
			uiMode = getUIMode();

		UIModeListener listener = m_uiModes.get(uiMode);

		return listener;
	}

	/**
	 * Updates the frame size and position based on the preferences stored.
	 */
	private void applyWindowInfo()
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

	/**
	 * @revise Temporary holding - we'll want something more gobal
	 */
	private void buildActions()
	{
		m_actionLoadMap = new AbstractAction("Open map") {
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

		m_actionLoadprivateMap = new AbstractAction("Load private Map") {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				loadMap(true, false);
			}
		};
	}

	/**
	 * Center the view on the map at given coordinates
	 * 
	 * @param modelCenter Coordinates we want to center on
	 * @param zoomLevel Requested zoom level
	 */
	private void centerView(MapCoordinates modelCenter, final ZoomLevel zoomLevel)
	{
		centerView(modelCenter, zoomLevel);
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
			JOptionPane.showMessageDialog(this, e.getMessage(), "Failed saving JPeg File.", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * creates the "About" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getAboutMenuItem()
	{
		final JMenuItem item = new JMenuItem("About"); // creates a menu item with the "About" label
		item.setAccelerator(KeyStroke.getKeyStroke("F1")); // assign a shortcut
		item.addActionListener(new ActionListener() // when the user selects it this is what happens
				{
					public void actionPerformed(final ActionEvent e)
					{
						// show the about message
						UtilityFunctions.msgBox(GametableFrame.this, GametableApp.VERSION + " " + "by the Gametable Community" + "\n"
								+ "Orignal program by Andy Weir and David Ghandehari", "Version");
					}
				});
		return item;
	}

	/**
	 * Get the File for auto save
	 * @return
	 */
	private File getAutoSaveXMLFile()
	{
		return new File(GametableApp.USER_FILES_PATH, "autosave.xml");
	}

	/**
	 * Create change background menu item
	 * 
	 * @param color
	 * @return
	 */
	private JMenuItem getChangeBGMenuItem(final BackgroundColor color)
	{
		final String cstr = color.getText();

		JMenuItem item = new JMenuItem(cstr);
		item.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e)
			{
				m_core.setBackgroundColor(color, null);
			}
		});

		return item;
	}

	/**
	 * Builds and return the "Undock Chat Window"
	 * 
	 * @return the menu item
	 */
	private JMenuItem getChatWindowMenuItem()
	{
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem("Undock Chat Window");
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " L"));

		if (!m_chatPanel.isDocked())
			item.setState(true);
		else
			item.setState(false);

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				m_chatPanel.toggleDockStatus();
			}
		});

		return item;
	}

	/**
	 * creates the "Clear Map" menu item
	 * 
	 * @return the new menu item
	 */
	private JMenuItem getClearMapMenuItem()
	{
		final JMenuItem item = new JMenuItem("Clear map");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				// confirm the erase operation
				final int res = UtilityFunctions.yesNoDialog(GametableFrame.this, 
						"This will clear all lines, pogs, and underlays on the entire layer. Are you sure?", 
						"Clear map");
				
				if (res == UtilityFunctions.YES)
				{
					GameTableMap map = m_core.getMap(GameTableCore.MapType.ACTIVE);
					map.clearMap(null);
				}
			}
		});

		return item;
	}

	/**
	 * #grouping
	 * 
	 * @return
	 */
	private JMenuItem getDeleteGroupMenuItem(final int all)
	{
		String g = "Delete Group";
		if (all == 1)
			g = "Delete Unused Groups";
		if (all == 2)
			g = "Delete All Groups";
		final JMenuItem item = new JMenuItem(g);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				if (m_core.getGroupManager(GameTableCore.MapType.ACTIVE).getGroupCount() < 1)
				{
					JOptionPane.showMessageDialog(GametableFrame.this, "No Groups Defined.", "No Groups", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				if (all == 1)
				{
					m_core.getGroupManager(GameTableCore.MapType.ACTIVE).deleteEmptyGroups();
				}
				if (all == 2)
				{
					m_core.getGroupManager(GameTableCore.MapType.ACTIVE).deleteAllGroups();
				}
				else
				{
					GroupingDialog gd = new GroupingDialog(false);
					gd.setVisible(true);
					if (gd.isAccepted())
					{
						Group group = gd.getGroup();
						if (group != null)
							group.deleteGroup();
					}
				}
			}
		});
		return item;
	}

	/**
	 * creates the "Disconnect" menu item
	 * 
	 * @return the new menu item
	 */
	private JMenuItem getDisconnectMenuItem()
	{
		if (m_disconnectMenuItem == null)
		{
			final JMenuItem item = new JMenuItem(getLanguageResource().DISCONNECT);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_core.disconnect();
				}
			});
			m_disconnectMenuItem = item;
		}
		return m_disconnectMenuItem;
	}

	/**
	 * Get the "Export Map" menu item
	 * 
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

	/**
	 * Builds and returns the File Menu
	 * 
	 * @return the file menu just built
	 */
	private JMenu getFileMenu()
	{
		// todo Add File->New

		final JMenu menu = new JMenu("File");

		JMenuItem item = new JMenuItem(m_actionLoadMap);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
		menu.add(item);

		item = new JMenuItem(m_actionLoadprivateMap);
		// item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
		menu.add(item);

		item = new JMenuItem(m_actionLoadPrivateMap);
		// item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed O"));
		menu.add(item);

		menu.add(getSaveMapMenuItem());
		menu.add(getSaveAsMapMenuItem());
		menu.add(getScanForPogsMenuItem());
		
		menu.add(getPropertiesMenuItem());
		
		menu.add(getQuitMenuItem());

		return menu;
	}

	/**
	 * @return Returns the gametableCanvas.
	 */
	private GametableCanvas getGametableCanvas()
	{
		return m_gametableCanvas;
	}

	/**
	 * creates the "Grid Mode" menu
	 * 
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

	// private JMenuItem getPrivChatWindowMenuItem()
	// {
	// final JMenuItem item = new JMenuItem("Open Private Chat");
	// item.setAccelerator(KeyStroke.getKeyStroke("ctrl T"));
	//
	// item.addActionListener(new ActionListener()
	// {
	// public void actionPerformed(final ActionEvent e)
	// {
	// m_chatPanel.openPrivChatWindowDialog();
	// }
	// });
	//
	// return item;
	// }

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenu getGroupingMenu()
	{
		final JMenu menu = new JMenu("Grouping");
		menu.add(getSelectGroupMenuItem());
		menu.add(getGroupSelectedMenuItem());
		menu.add(getUngroupSelectedMenuItem());
		menu.add(getDeleteGroupMenuItem(0));
		menu.add(getDeleteGroupMenuItem(1));
		menu.add(getDeleteGroupMenuItem(2));
		return menu;
	}

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenuItem getGroupSelectedMenuItem()
	{
		final JMenuItem item = new JMenuItem("Group Selected");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				GroupingDialog gd = new GroupingDialog(true);
				gd.setVisible(true);
				if (gd.isAccepted())
				{
					Group g = gd.getGroup();
					if (g != null)
						g.addElements(getSelectedMapElementInstances(MapType.ACTIVE));
				}
			}
		});
		return item;
	}

	/**
	 * creates the "Help" menu
	 * 
	 * @return the new menu
	 */
	private JMenu getHelpMenu()
	{
		final JMenu menu = new JMenu(getLanguageResource().HELP);
		menu.add(getAboutMenuItem());

		/*
		 * menu.add(new JMenuItem(new AbstractAction("ISHOST") {
		 * 
		 * @Override public void actionPerformed(ActionEvent e) { Player p = m_core.getMyPlayer();
		 * System.out.println(p.isHostPlayer() ? "host" : "guest"); System.out.println(p.getConnection() == null ?
		 * "disconnected" : "connected"); } }));
		 */

		return menu;
	}

	/**
	 * @return Returns the lang.
	 */
	@Deprecated
	private Language getLanguageResource()
	{
		return m_languageResource;
	}

	/**
	 * creates the "list players" menu item
	 * 
	 * @return the new menu item
	 */
	private JMenuItem getListPlayersMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().LIST_PLAYERS);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " W"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				SlashCommands.parseSlashCommand("/who"); // selecting this menu item is the same as issuing the command /who
			}
		});
		return item;
	}

	private JMenuItem getLoadPogMenuItem()
	{
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
		if (lock)
			str = getLanguageResource().MAP_LOCK_ALL;
		else
			str = getLanguageResource().MAP_UNLOCK_ALL;
		final JMenuItem item = new JMenuItem(str);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				m_core.lockAllMapElements(GameTableCore.MapType.ACTIVE, lock);
			}
		});

		return item;
	}

	/**
	 * Builds and returns the main menu bar
	 * 
	 * @return The menu bar just built
	 */
	private JMenuBar getMainMenuBar()
	{
		final JMenuBar menuBar = new JMenuBar();
		menuBar.add(getFileMenu());
		menuBar.add(getNetworkMenu());
		menuBar.add(getMapMenu());
		menuBar.add(getGroupingMenu()); // #grouping

		JMenu menu = getModulesMenu();
		if (menu != null)
			menuBar.add(menu);

		menuBar.add(getWindowMenu());
		menuBar.add(getHelpMenu());

		return menuBar;
	}

	/**
	 * Ask the user to choose a file name for the exported map
	 * 
	 * @return File object or null if the user did not choose
	 */
	private File getMapExportFile()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(getLanguageResource().MAP_SAVE_IMG); // no external resource for text
		if (m_mapExportSaveFolder != null)
		{
			chooser.setSelectedFile(m_mapExportSaveFolder);
		}

		FileFilter filter = new FileFilter() {
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
			if (JOptionPane.showConfirmDialog(this, getLanguageResource().MAP_SAVE_OVERWRITE + " " + out.getName() + "?",
					getLanguageResource().MAP_SAVE_EXISTS, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			{
				return null;
			}
		}

		if (out.exists() && !out.canWrite())
		{
			JOptionPane.showMessageDialog(this, getLanguageResource().MAP_SAVE_NO_ACCESS + " " + out.getName(), getLanguageResource().MAP_SAVE_FILE_FAIL,
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
	 * bulds and returns the "Map" menu
	 * 
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

	private JMenuItem getMechanicsToggle()
	{
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().MECHANICS_WINDOW_USE);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " M"));
		if (m_chatPanel.getUseMechanicsLog())
			item.setState(true);
		else
			item.setState(false);

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				toggleMechanicsWindow();
			}
		});

		return item;
	}

	/**
	 * creates the "Modules" menu
	 * 
	 * @return the new menu
	 */
	private JMenu getModulesMenu()
	{
		JMenu modulesMenu = null;

		for (Module module : m_core.getRegisteredModules())
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
	 * builds and return the "Network" menu
	 * 
	 * @return the newly built menu
	 */
	private JMenu getNetworkMenu()
	{
		final JMenu menu = new JMenu(getLanguageResource().NETWORK);
		menu.add(getListPlayersMenuItem());

		if (m_startNetworkingMenuItem == null)
		{
			final JMenuItem item = new JMenuItem("Join / Host Network Game");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					startNetworkGame();
				}
			});
			m_startNetworkingMenuItem = item;
		}

		menu.add(m_startNetworkingMenuItem);
		menu.add(getDisconnectMenuItem());

		return menu;
	}

	/**
	 * builds and return the "Undock Pog Window" menu item
	 * 
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

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				m_pogsTabbedPane.toggleDockStatus();
			}
		});

		return item;
	}

	/**
	 * build and returns the "Quit" menu item
	 * 
	 * @return the newly built menu item
	 */
	private JMenuItem getQuitMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().QUIT);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " Q"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				if (pogWindow != null) // close the pog window
				{
					pogWindow.dispose();
				}
				saveAll(); // .todo confirm before save (check if modified)
				dispose();
				System.exit(0);
			}
		});

		return item;
	}

	/**
	 * builds and returns the "Recenter all Player" menu item
	 * 
	 * @return a menu item
	 */
	private JMenuItem getRecenterAllPlayersMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().MAP_CENTER_PLAYERS);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				// confirm the operation
				final int result = UtilityFunctions.yesNoDialog(GametableFrame.this, getLanguageResource().MAP_CENTER_PLAYERS_WARN,
						getLanguageResource().MAP_CENTER);
				if (result == UtilityFunctions.YES)
				{
					// get our view center
					final int viewCenterX = getGametableCanvas().getWidth() / 2;
					final int viewCenterY = getGametableCanvas().getHeight() / 2;

					// convert to model coordinates
					final MapCoordinates modelCenter = getGametableCanvas().viewToModel(viewCenterX, viewCenterY);
					centerView(modelCenter, m_gametableCanvas.getZoomLevel());
					m_core.sendMessageBroadcast(MessageType.SYSTEM, m_core.getPlayer().getPlayerName() + " " + getLanguageResource().MAP_CENTER_DONE);
				}
			}
		});
		return item;
	}

	// private void openPrivateMessageWindow()
	// {
	// }

	/**
	 * *************************************************************************************
	 * 
	 * @return
	 */
	private JMenuItem getRemoveSelectedMenuItem()
	{
		final JMenuItem item = new JMenuItem("Remove Selected");

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				m_core.getMap(GameTableCore.MapType.ACTIVE).removeMapElements(getSelectedMapElementInstances(MapType.ACTIVE));
			}
		});

		return item;
	}

	/**
	 * builds and returns the "Save map as" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getSaveAsMapMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().MAP_SAVE_AS);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " shift pressed S"));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				m_actingFileprivate = UtilityFunctions.doFileSaveDialog(getLanguageResource().SAVE_AS, "xml", true);
				if (m_actingFileprivate != null)
					saveMapToXML(m_actingFileprivate);

				/*
				 * 
				 * 
				 * if (getGametableCanvas().isprivateMap()) { m_actingFileprivate =
				 * UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true); if (m_actingFileprivate != null) {
				 * saveState(m_core.getGameTableMap(GameTableMapType.ACTIVE), m_actingFileprivate); } } else {
				 * m_actingFilePrivate = UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true); if (m_actingFilePrivate
				 * != null) { saveState(m_core.getGameTableMap(GameTableMapType.ACTIVE), m_actingFilePrivate); } }
				 */
			}
		});

		return item;
	}

	// private void startTellTo(String name)
	// {
	// if (name.contains(" ") || name.contains("\"") || name.contains("\t"))
	// {
	// name = "\"" + name.replace("\"", "\\\"") + "\"";
	// }
	//	        
	// m_textEntry.setText("/tell " + name + "<b> </b>");
	// m_textEntry.requestFocus();
	// m_textEntry.toggleStyle("bold");
	// m_textEntry.toggleStyle("bold");
	// }

	// /**
	// * Sends a private message to the target player.
	// *
	// * @param target Player to address message to.
	// * @param text Message to send.
	// */
	// private void tell(final Player target, final String text)
	// {
	// if (target.getID() == m_core.getMyPlayer().getID())
	// {
	// m_chatPanel.logMessage(PRIVATE_MESSAGE_FONT + getLanguageResource().TELL_SELF + " " + END_PRIVATE_MESSAGE_FONT +
	// text);
	// return;
	// }
	//
	// final String fromName = m_core.getMyPlayer().getCharacterName();
	// final String toName = target.getCharacterName();
	//
	// m_core.sendMessage(MessageType.CHAT, fromName, toName, text);
	//
	// // and when you post a private message, you get told about it in your
	// // own chat log
	// m_chatPanel.logMessage(PRIVATE_MESSAGE_FONT + getLanguageResource().TELL + " " +
	// UtilityFunctions.emitUserLink(toName) + ": "
	// + END_PRIVATE_MESSAGE_FONT + text);
	// }

	/**
	 * builds and returns the "save map" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getSaveMapMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().MAP_SAVE);
		item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " pressed S"));
		item.addActionListener(new ActionListener() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				if (m_actingFileprivate == null)
					m_actingFileprivate = UtilityFunctions.doFileSaveDialog(getLanguageResource().SAVE_AS, "xml", true);

				if (m_actingFileprivate != null)
					saveMapToXML(m_actingFileprivate);

				/*
				 * if (getGametableCanvas().isprivateMap()) { if (m_actingFileprivate == null) { m_actingFileprivate =
				 * UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm", true); }
				 * 
				 * if (m_actingFileprivate != null) { // save the file
				 * saveState(m_core.getGameTableMap(GameTableMapType.ACTIVE), m_actingFileprivate); } } else { if
				 * (m_actingFilePrivate == null) { m_actingFilePrivate = UtilityFunctions.doFileSaveDialog(lang.SAVE_AS, "grm",
				 * true); }
				 * 
				 * if (m_actingFilePrivate != null) { // save the file
				 * saveState(m_core.getGameTableMap(GameTableMapType.ACTIVE), m_actingFilePrivate); } }
				 */
			}
		});

		return item;
	}

	/**
	 * builds and returns the "Scan for pogs" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getScanForPogsMenuItem()
	{
		final JMenuItem item = new JMenuItem(getLanguageResource().POG_SCAN);
		item.setAccelerator(KeyStroke.getKeyStroke("F5"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				MSG_REFRESH_MAP_LIBRARY.addMessage(GametableFrame.this, "scan");
			}
		});

		return item;
	}
	
	/**
	 * builds and returns the "Scan for pogs" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getPropertiesMenuItem()
	{
		final JMenuItem item = new JMenuItem("Properties...");
		//item.setAccelerator(KeyStroke.getKeyStroke("F5"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				XProperties props = m_core.getProperties();
				XPropertiesDialog dlg = new XPropertiesDialog(GametableFrame.this, props);
				
				// XProperties - resource bundle ResourceBundle TODO #ResourceBundle class for language strings
				
				// Okay, I want a GameTable ResourceBundle class - it should be able to use multiple bundles.  PropertyResourceBundle

				
				if (dlg.showModal() == ReturnValue.OK)
				{
					props.copyAllPropertiesFrom(dlg.getProperties());					
				}
			}
		});

		return item;
	}

	/**
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenuItem getSelectGroupMenuItem()
	{
		final JMenuItem item = new JMenuItem("Select Group");

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				if (m_core.getGroupManager(GameTableCore.MapType.ACTIVE).getGroupCount() < 1)
				{
					JOptionPane.showMessageDialog(GametableFrame.this, "No Groups Defined.", "No Groups", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				GroupingDialog gd = new GroupingDialog(false);
				Group g = gd.getGroup();
				gd.setVisible(true);
				if (gd.isAccepted() && g != null)
				{

					List<MapElement> pogs = g.getMapElements();
					selectMapElementInstances(pogs, MapType.ACTIVE, true);
					getGametableCanvas().repaint();
				}
			}
		});
		return item;
	}

	/**
	 * Get the selection handler for the requested map
	 * 
	 * @param mapType Map type
	 * @return selection handler
	 */
	private SelectionHandler getSelectionHandler(MapType mapType)
	{
		switch (mapType)
		{
		case ACTIVE:
			return m_core.isActiveMapPublic() ? m_selectionPublic : m_selectionPrivate;

		case PRIVATE:
			return m_selectionPrivate;

		case PUBLIC:
			return m_selectionPublic;
		}

		throw new IllegalArgumentException("Invalid map type");
	}

	/**
	 * builds and returns the "Edit private map" menu item
	 * 
	 * @return the menu item
	 */
	private JMenuItem getTogglePrivateMapMenuItem()
	{
		if (m_togglePrivateMapMenuItem == null)
		{
			final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getLanguageResource().MAP_PRIVATE_EDIT);
			item.setAccelerator(KeyStroke.getKeyStroke(MENU_ACCELERATOR + " F"));
			item.addActionListener(new ActionListener() {
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
	 * ************************************************************************************* #grouping
	 * 
	 * @return
	 */
	private JMenuItem getUngroupSelectedMenuItem()
	{
		final JMenuItem item = new JMenuItem("UnGroup Selected");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{

				for (MapElement pog : getSelectedMapElementInstances(MapType.ACTIVE))
				{
					Group g = m_core.getGroupManager(GameTableCore.MapType.ACTIVE).getGroup(pog);
					if (g != null)
						g.removeElement(pog);
				}
			}
		});
		return item;
	}

	/**
	 * *************************************************************************************
	 * 
	 * @return
	 */
	private JMenuItem getUnPublishSelectedMenuItem(final boolean copy)
	{
		String s = "Un/Publish Selected";
		if (copy)
			s = "Copy Selected to Opposite Map";
		final JMenuItem item = new JMenuItem(s);

		item.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e)
			{
				MapElement npog;
				GameTableMap map = m_core.getMap(GameTableCore.MapType.ACTIVE);
				GameTableMap to;

				if (map == m_core.getMap(GameTableCore.MapType.PUBLIC))
					to = m_core.getMap(GameTableCore.MapType.PRIVATE);
				else
					to = m_core.getMap(GameTableCore.MapType.PUBLIC);

				for (MapElement pog : getSelectedMapElementInstances(MapType.ACTIVE))
				{
					to.addMapElement(pog);
					map.removeMapElement(pog);
					if (copy)
					{
						npog = new MapElement(pog);
						map.addMapElement(npog);
					}
				}
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
		// menu.add(getPrivChatWindowMenuItem());

		return menu;
	}

	private void initializeCoreListeners()
	{
		GameTableCoreListenerIF coreListener = new GameTableCoreListenerIF() {

			@Override
			public void onActiveMapChange(boolean privateMap)
			{
				// if they toggled the layer, whatever tool they're using is canceled
				UIMode uiMode = getUIMode();
				if (uiMode != null)
				{
					UIModeListener listener = m_uiModes.get(uiMode);
					if (listener != null)
						listener.cancelMode();
				}

				getGametableCanvas().requestFocus();

				repaint();
			}

			@Override
			public void onAllMapElementsLocked(boolean onprivateMap, boolean locked, NetworkEvent netEvent)
			{
				repaint();
			}

			@Override
			public void onBackgroundChanged(boolean isMapElementType, MapElementTypeIF elementType, BackgroundColor color, NetworkEvent netEvent)
			{
				m_gametableCanvas.onBackgroundChanged(isMapElementType, elementType, color);
			}

			@Override
			public void onGridModeChanged(GridModeID gridMode, NetworkEvent netEvent)
			{
				updateGridModeMenu();
				repaint();
			}

			@Override
			public void onHostingStarted()
			{
				repaint();
			}

			@Override
			public void onMapElementLocked(boolean onprivateMap, MapElement mapElement, boolean locked, NetworkEvent netEvent)
			{
				repaint();
			}

			@Override
			public void onMapElementsLocked(boolean onprivateMap, List<MapElement> mapElements, boolean locked, NetworkEvent netEvent)
			{
				repaint();
			}

			@Override
			public void onPlayerJoined(Player player)
			{
				// finally, have the player recenter on the host's view
				final int viewCenterX = getGametableCanvas().getWidth() / 2;
				final int viewCenterY = getGametableCanvas().getHeight() / 2;

				// convert to model coordinates
				final MapCoordinates modelCenter = getGametableCanvas().viewToModel(viewCenterX, viewCenterY);
				m_core.send(NetRecenterMap.makePacket(modelCenter, m_gametableCanvas.getZoomLevel()), player.getConnection());
			}

			@Override
			public void onPlayerNameChanged(Player player, String playerName, String characterName, NetworkEvent netEvent)
			{
				repaint();
			}

			@Override
			public void onPointingLocationChanged(Player player, boolean pointing, MapCoordinates location, NetworkEvent netEvent)
			{
				repaint();
			}
		};

		m_core.addListener(coreListener);

		m_core.getNetworkModule().addListener(new NetworkListenerIF() {

			@Override
			public void connectionDropped(NetworkConnectionIF conn)
			{
				connectionEnded();
			}

			@Override
			public void connectionEnded()
			{
				m_startNetworkingMenuItem.setEnabled(true); // enable the menu item to join an existing game
				m_disconnectMenuItem.setEnabled(false); // disable the menu item to disconnect from the game

				setTitle(GametableApp.VERSION);

				m_core.sendMessageLocal(MessageType.SYSTEM, "Disconnected");
				updateStatus();
			}

			@Override
			public void connectionEstablished(NetworkConnectionIF conn)
			{
				if (m_core.getNetworkStatus() == NetworkStatus.HOSTING)
				{
					m_startNetworkingMenuItem.setEnabled(false); // disable the join menu item
					m_disconnectMenuItem.setEnabled(true); // enable the disconnect menu item

					setTitle(GametableApp.VERSION + " - " + m_core.getPlayer().getCharacterName());
				}
				else
				{
					// Player connection

					try
					{
						// and now we're ready to pay attention
						m_core.sendMessageLocal(MessageType.SYSTEM, "Joined game");

						m_gametableCanvas.setScrollPosition(0, 0);

						m_startNetworkingMenuItem.setEnabled(false); // disable the join menu item
						m_disconnectMenuItem.setEnabled(true); // enable the disconnect menu item

						setTitle(GametableApp.VERSION + " - " + m_core.getPlayer().getCharacterName());
					}
					catch (final Exception ex)
					{
						Log.log(Log.SYS, ex);
						m_core.sendMessageLocal(MessageType.ALERT, "Failed to connect.");
						setTitle(GametableApp.VERSION);
					}
				}
			}

			@Override
			public void networkStatusChange(NetworkStatus status)
			{
				updateStatus();
			}
		});
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
		m_executorThread = new PeriodicExecutorThread(new Runnable() {
			public void run()
			{
				tick();
			}
		});
		m_executorThread.start();
	}

	private void initializeMapElementTypeLibrary() throws IOException
	{
		MSGID_REFRESH_MAP_LIBRARY = MessageID.acquire(MapElementTypeLibrary.class.getCanonicalName() + ".REFRESH");
		MSG_REFRESH_MAP_LIBRARY = new MessageDefinition(MSGID_REFRESH_MAP_LIBRARY, MessagePriority.LOW);

		MapElementTypeLibraryListenerIF listener = new MapElementTypeLibraryListenerIF() {
			@Override
			public void onLibraryAdded(MapElementTypeLibrary parentLibrary, MapElementTypeLibrary newLibrary)
			{
				// Queue a refresh message
				MSG_REFRESH_MAP_LIBRARY.addMessage(GametableFrame.this, "library-removed");
			}

			@Override
			public void onMapElementTypeAdded(MapElementTypeLibrary parentLibrary, MapElementTypeIF newType)
			{
				// Queue a refresh message
				MSG_REFRESH_MAP_LIBRARY.addMessage(GametableFrame.this, "type-added");
			}

			@Override
			public void onMapElementTypeRemoved(MapElementTypeLibrary parentLibrary, MapElementTypeIF removedType)
			{
				// Queue a refresh message
				MSG_REFRESH_MAP_LIBRARY.addMessage(GametableFrame.this, "type-removed");
			}

			@Override
			public void onMapElementTypeUpdated(MapElementTypeLibrary parentLibrary, MapElementTypeIF type)
			{
				// Queue a refresh message
				MSG_REFRESH_MAP_LIBRARY.addMessage(GametableFrame.this, "type-updated");
			}
		};

		m_core.getMapElementTypeLibrary().addListener(listener);
	}

	/**
	 * Build the toolbar buttons
	 */
	private void initializeToolBar()
	{
		/*
		 * todo #Planning
		 * 
		 * This is where we build the toolbar buttons.
		 * 
		 * Current implementation has one button to be created automatically for every "mode" We want to make it: - Open
		 * (modules could add button) - Configurable (users could change the order and decide which buttons to see) - Make
		 * use of the Action and UndoableEdit interfaces
		 * 
		 * Maybe some of this this can be at a later step not to slow things down even more.
		 * 
		 * If we add an "addToolBarButton" to the frame, where we can specify Action, which contains - name, image and could
		 * support UndoableEdit in the future, we could be all set Interface-wise.
		 */

		ButtonGroup modeButtons = new ButtonGroup(); // TODO #Toobars Toggle button in button group when calling setMode

		for (Action action : m_moduleActions)
		{
			AbstractButton button;

			if (action instanceof UIModeAction)
			{
				button = new JToggleButton(action);
				modeButtons.add(button);
			}
			else
				button = new JButton(action);

			button.setHideActionText(true); // TODO #Toolbars Configure whether we show text on buttons
			button.setFocusable(false);
			m_toolBar.add(button);
		}
	}

	/**
	 * Initializes the tools from the ToolManager.
	 */
	private void initializeTools()
	{
		// TODO #Plugins load non-basic modes as modules
		MapElementMode.getUIMode();
		SelectMode.getUIMode();
		BoxMode.getUIMode();
		CircleMode.getUIMode();
		ColorEraseMode.getUIMode();
		EraseMode.getUIMode();
		HandMode.getUIMode();
		LineMode.getUIMode();
		MeasureMode.getUIMode();
		PenMode.getUIMode();
		PublishMode.getUIMode();
	}

	/**
	 * Load map
	 * 
	 * @param loadprivate Load private map from file
	 * @param loadPrivate Load private map from file
	 */
	private void loadMap(boolean loadprivate, boolean loadPrivate)
	{
		final File openFile = UtilityFunctions.doFileOpenDialog(getLanguageResource().OPEN, "xml", true);

		if (openFile != null)
		{
			// Build an event - will prevent triggering network messages, we'll handle this on our end.
			NetworkEvent netEvent = null;

			if (m_core.getNetworkStatus() == NetworkStatus.HOSTING)
				netEvent = new NetworkEvent(m_core.getPlayer(), true, NetLoadMap.getMessageType());

			loadMapFromXML(openFile, loadprivate, loadPrivate, netEvent);

			if (m_core.getNetworkStatus() == NetworkStatus.HOSTING)
			{
				// Send data to other connected players (host only)
				m_core.sendBroadcast(NetLoadMap.makePacket(m_core.getMap(GameTableCore.MapType.PUBLIC)));
			}
		}
	}

	/**
	 * Load a map file from file (goes through core, but handles errors through UI)
	 * 
	 * @param file XML File from which to load the map
	 * @param loadprivate true to load the private map within the file.
	 * @param loadPrivate true to load the private map within the file.
	 * @param netEvent Network event that triggered the load (or null)
	 */
	private void loadMapFromXML(File file, boolean loadprivate, boolean loadPrivate, NetworkEvent netEvent)
	{
		try
		{
			m_core.loadMapFromXML(file, loadprivate, loadPrivate, netEvent);
		}
		catch (MapFormatException e)
		{
			JOptionPane.showMessageDialog(this, "Invalid file format", null, JOptionPane.ERROR_MESSAGE);
			return;
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this, "Error loading " + file.getName() + " : " + e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
			// .todo better error handling?
			return;
		}
	}

	private void loadPog()
	{
		final File openFile = UtilityFunctions.doFileOpenDialog(getLanguageResource().OPEN, ".pog", true);

		if (openFile == null)
		{ // they cancelled out of the open
			return;
		}
		try
		{
			final FileInputStream infile = new FileInputStream(openFile);
			final DataInputStream dis = new DataInputStream(infile);
			final MapElement nPog = new MapElement(dis);
			m_core.getMap(GameTableCore.MapType.ACTIVE).addMapElement(nPog);
		}
		catch (final IOException ex1)
		{
			Log.log(Log.SYS, ex1);
		}
	}

	/**
	 * Refreshes the pog list.
	 */
	private void refreshMapElementList()
	{
		try
		{
			m_core.getMapElementTypeLibrary().refresh(true);
		}
		catch (IOException e)
		{
			// .todo better error handling?
			Log.log(Log.SYS, e.getMessage());
		}

		m_pogPanel.populateChildren();
		repaint();
	}

	/**
	 * Saves everything: both maps, macros, and preferences. Called on program exit.
	 */
	private void saveAll()
	{
		saveMapToXML(getAutoSaveXMLFile());
		// saveState(m_core.getGameTableMap(GameTableMapType.PUBLIC), new File("autosave.grm"));
		// saveState(m_core.getGameTableMap(GameTableMapType.PRIVATE), new File("autosavepvt.grm"));
		m_core.saveProperties();
	}

	/**
	 * Save the current maps to XML. Goes through core, but handles exceptions through UI)
	 * 
	 * @param file
	 */
	private void saveMapToXML(final File file)
	{
		try
		{
			m_core.saveMapToXML(file);
		}
		catch (IOException e)
		{
			Log.log(Log.SYS, e.getMessage());
			JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	/**
	 * Verifies whether changes to the data should be propagated over the network
	 * 
	 * @param netEvent Network event that might have triggered the changes
	 * @return True to propagate, false otherwise.
	 */
	private boolean shouldPropagateChanges(NetworkEvent netEvent)
	{
		return m_core.getNetworkStatus() != NetworkStatus.DISCONNECTED && netEvent == null && !m_core.isLoggingIn();
	}

	/**
	 * Show the networking dialog
	 * 
	 * @return
	 */
	private boolean showNetworkingDialog()
	{
		final StartNetworkingDialog dialog = new StartNetworkingDialog();
		dialog.setLocationRelativeTo(m_gametableCanvas);
		dialog.setVisible(true);

		// Check for cancellation
		if (!dialog.m_bAccepted)
			return false;

		return true;
	}

	/**
	 * joins a network game
	 */
	private void startNetworkGame()
	{
		boolean res = showNetworkingDialog();

		// Was the dialog accepted?
		if (!res)
			return;

		m_core.networkConnect();
	}

	/**
	 * Tick of the internal timer
	 */
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

	/**
	 * Tick of the internal status timer
	 * 
	 * @param ms
	 */
	private void tick(final long ms)
	{
		m_gametableCanvas.tick(ms);
	}

	/**
	 * Toggles between the two layers.
	 */
	private void toggleLayer()
	{
		// toggle the map we're on
		boolean privateMap = m_core.isActiveMapPublic();
		m_core.setActiveMap(privateMap ? GameTableCore.MapType.PRIVATE : GameTableCore.MapType.PUBLIC);
	}

	private void toggleMechanicsWindow()
	{
		m_chatPanel.toggleMechanicsWindow();
		validate();
	}

	private void updateGridModeMenu()
	{
		switch (m_core.getGridModeID())
		{
		case SQUARES:
			m_noGridModeMenuItem.setState(false);
			m_squareGridModeMenuItem.setState(true);
			m_hexGridModeMenuItem.setState(false);
			break;

		case HEX:
			m_noGridModeMenuItem.setState(false);
			m_squareGridModeMenuItem.setState(false);
			m_hexGridModeMenuItem.setState(true);
			break;

		case NONE:
		default:
			m_noGridModeMenuItem.setState(true);
			m_squareGridModeMenuItem.setState(false);
			m_hexGridModeMenuItem.setState(false);
			break;
		}
	}

	/**
	 * Updates the networking status
	 */
	private void updateStatus()
	{
		String newStatusText = "";

		switch (m_core.getNetworkStatus())
		{
		case DISCONNECTED:
			newStatusText = getLanguageResource().DISCONNECTED;
			m_actionLoadMap.setEnabled(true);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadprivateMap.setEnabled(true);
			break;

		case CONNECTED:
			newStatusText = getLanguageResource().CONNECTED;
			m_actionLoadMap.setEnabled(false);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadprivateMap.setEnabled(false);
			break;

		case HOSTING:
			newStatusText = getLanguageResource().HOSTING;
			m_actionLoadMap.setEnabled(true);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadprivateMap.setEnabled(true);
			break;

		default:
			newStatusText = getLanguageResource().UNKNOWN_STATE;
			m_actionLoadMap.setEnabled(false);
			m_actionLoadPrivateMap.setEnabled(true);
			m_actionLoadprivateMap.setEnabled(false);
			break;
		}

		if (m_core.getNetworkStatus() != NetworkStatus.DISCONNECTED)
		{
			List<Player> players = m_core.getPlayers();

			if (players.size() > 1)
				newStatusText += "; " + players.size() + " players " + getLanguageResource().CONNECTED;
			else
				newStatusText += "; " + players.size() + " player " + getLanguageResource().CONNECTED;

			int count = m_typingPlayers.size();
			boolean started = false;

			for (Player player : m_typingPlayers)
			{
				if (started)
					newStatusText += " AND";
				else
				{
					newStatusText += ";";
					started = true;
				}

				newStatusText += " " + player.getCharacterName();
			}

			if (count == 1)
				newStatusText += " " + getLanguageResource().IS_TYPING;
			else if (count > 1)
				newStatusText += " " + getLanguageResource().ARE_TYPING;
		}

		m_status.setText(newStatusText);
	}

	/**
	 * Records the current state of the window.
	 */
	private void updateWindowInfo()
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

	/**
	 * Array of standard colors used
	 */
	public final static Integer[]				COLORS										= { new Integer(new Color(0, 0, 0).getRGB()),
			new Integer(new Color(198, 198, 198).getRGB()), new Integer(new Color(0, 0, 255).getRGB()), new Integer(new Color(0, 255, 0).getRGB()),
			new Integer(new Color(0, 255, 255).getRGB()), new Integer(new Color(255, 0, 0).getRGB()), new Integer(new Color(255, 0, 255).getRGB()),
			new Integer(new Color(255, 255, 0).getRGB()), new Integer(new Color(255, 255, 255).getRGB()), new Integer(new Color(0, 0, 132).getRGB()),
			new Integer(new Color(0, 132, 0).getRGB()), new Integer(new Color(0, 132, 132).getRGB()), new Integer(new Color(132, 0, 0).getRGB()),
			new Integer(new Color(132, 0, 132).getRGB()), new Integer(new Color(132, 132, 0).getRGB()), new Integer(new Color(132, 132, 132).getRGB()) };

	//public final static int							MAX_ZOOM_LEVEL						= 5;

	public static String								PATH_CURSORS							= "images" + File.separator + "cursors" + File.separator;

	public static String								PATH_ICONS								= "images" + File.separator + "icons" + File.separator;

	/**
	 * The version of the communications protocol used by this build. This needs to change whenever an incompatibility
	 * arises between versions.
	 */

	// based on chat context

	private final static boolean				DEBUG_FOCUS								= false;

	// The current file path used by save and open.
	// NULL if unset.
	private static File									m_mapExportSaveFolder			= new File("./saves");

	/**
	 * @return
	 */
	private final static String					MENU_ACCELERATOR					= getMenuAccelerator();

	/**
	 * Message to refresh the map element library
	 */
	private static MessageDefinition		MSG_REFRESH_MAP_LIBRARY;

	/**
	 * Message to refresh the map element library
	 */
	private static MessageID						MSGID_REFRESH_MAP_LIBRARY;

	private static final String					PROP_CHAT_SPLIT						= PROPERTY_BUNDLE_NAME + ".chat_divider_loc";

	private static final String					PROP_COLUMN_SPLIT					= PROPERTY_BUNDLE_NAME + ".column_divider_loc";

	private static final String					PROP_MAXIMIZED						= PROPERTY_BUNDLE_NAME + ".window_maximized";

	private final static String					PROP_RANDOM_ROTATE				= PROPERTY_BUNDLE_NAME + ".rnd_rotate_elements";

	private static final String					PROP_SCROLL_POSITION			= PROPERTY_BUNDLE_NAME + ".scroll_position";
	private final static String					PROP_SHOW_NAMES_ON_MAP		= PROPERTY_BUNDLE_NAME + ".show_names";

	private final static String					PROP_USE_CHAT_MECHANICS		= PROPERTY_BUNDLE_NAME + ".show_chat_mechanics";

	private static final String					PROP_WINDOW_POSITION			= PROPERTY_BUNDLE_NAME + ".window_position";

	private static final String					PROP_WINDOW_SIZE					= PROPERTY_BUNDLE_NAME + ".window_size";

	private static final String					PROP_ZOOM_LEVEL						= PROPERTY_BUNDLE_NAME + ".zoom_level";

	/**
     * 
     */
	private static final long						serialVersionUID					= -1997597054204909759L;

	public final String									ACTION_DEFAULT_AS_BUTTON	= PROPERTY_BUNDLE_NAME + ".DEFAULT_AS_BUTTON";

	private final int										DEFAULT_WINDOWSIZE_HEIGHT	= 600;

	private final int										DEFAULT_WINDOWSIZE_WIDTH	= 800;

	private File												m_actingFileprivate;

	private javax.swing.Action					m_actionLoadMap;

	private javax.swing.Action					m_actionLoadprivateMap;

	private javax.swing.Action					m_actionLoadPrivateMap;

	private boolean											m_bMaximized;																																																// Is
	// the
	// frame
	// maximized?

	// map
	// pane
	// is
	// really
	// a
	// split
	// between
	// the
	// map
	// and
	// the
	// chat
	// and the chat pane
	private final JPanel								m_canvasPane							= new JPanel(new BorderLayout());																									// This

	// is
	// the
	// map
	private ChatPanel										m_chatPanel								= null;																																						// Panel

	// main
	// toolbar
	private final JComboBox							m_colorCombo							= new JComboBox(COLORS);																														// Combo

	/**
	 * Pointer to the core
	 */
	private final GameTableCore					m_core;

	/**
	 * List of loaded cursors
	 */
	private Map<String, Cursor>					m_cursors									= new HashMap<String, Cursor>();

	// box
	// for
	// colore
	private JMenuItem										m_disconnectMenuItem;

	private Color												m_drawColor								= Color.BLACK;

	private PeriodicExecutorThread			m_executorThread;

	// is
	// the
	// pane
	// containing
	// the
	// map
	private final GametableCanvas				m_gametableCanvas					= new GametableCanvas(this);																												// This

	/**
	 * Converion rate from tiles to 'grid units'
	 */
	private double											m_gridUnitConversionRate	= 5.0;

	/**
	 * Grid unit type
	 */
	private String											m_gridUnit								= "ft";

	private final JCheckBoxMenuItem			m_hexGridModeMenuItem			= new JCheckBoxMenuItem("Hex Grid");

	/**
	 * Handles the list of highlighted element
	 */
	private SelectionHandler						m_highlightedElements;

	// Language variables
	@Deprecated
	private Language										m_languageResource				= new Language(GametableApp.LANGUAGE);

	private long												m_lastTickTime						= 0;

	private final JSplitPane						m_mapChatSplitPane				= new JSplitPane();																																// The

	// The map-pog split pane goes in the center
	private final JSplitPane						m_mapPogSplitPane					= new JSplitPane();																																// Split

	/**
	 * Pointer to the UI's current mode. Null for basic cursor. #TODO Tools "basic" mode should not be null
	 */
	private UIMode											m_modeCurrent							= null;

	private ArrayList<Action>						m_moduleActions						= new ArrayList<Action>();

	/**
	 * Instance of network message handling class
	 */
	private NetworkFrameResponder				m_networkResponder;

	private final JCheckBoxMenuItem			m_noGridModeMenuItem			= new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_NONE);

	// Pog
	// pane
	// is
	// tabbed
	private MapElementTypeLibraryPanel										m_pogPanel								= null;																																						// one

	// tab
	// is
	// the
	// Pog
	// Panel
	// between
	// Pog
	// pane
	// and
	// map
	// pane
	private final PogWindow							m_pogsTabbedPane					= new PogWindow(this);																															// The

	private final JCheckBox							m_randomRotate						= new JCheckBox(getLanguageResource().RANDOM_ROTATE);															// #randomrotate

	/**
	 * Selection on the private map
	 */
	private SelectionHandler						m_selectionPrivate;

	/**
	 * Selection on the public map
	 */
	private SelectionHandler						m_selectionPublic;

	private final JCheckBox							m_showNamesCheckbox				= new JCheckBox(getLanguageResource().SHOW_POG_NAMES);

	private final JCheckBoxMenuItem			m_squareGridModeMenuItem	= new JCheckBoxMenuItem(getLanguageResource().MAP_GRID_SQUARE);

	private JMenuItem										m_startNetworkingMenuItem;

	// The status goes at the bottom of the pane
	private final JLabel								m_status									= new JLabel(" ");																																	// Status

	// Bar
	// for
	// chat
	private ChatLogEntryPane						m_textEntry								= null;

	private JCheckBoxMenuItem						m_togglePrivateMapMenuItem;

	// Controls in the Frame
	// The toolbar goes at the top of the pane
	private final JToolBar							m_toolBar									= new JToolBar();																																	// The

	/**
	 * List of players currently typing
	 */
	private final List<Player>					m_typingPlayers						= new ArrayList<Player>();

	/**
	 * Holds the collection of UIMode objects and linked listeners Listeners are stored here to remain unaccessible
	 * through API - Only the core UI should handle listener methods.
	 */
	private Map<UIMode, UIModeListener>	m_uiModes									= new HashMap<UIMode, UIModeListener>();

	// window size and position
	private Point												m_windowPos;

	private Dimension										m_windowSize;
	private JFrame											pogWindow									= null;
	// private boolean b_pogWindowDocked = true;
	// private boolean b_chatWindowDocked = true;

	JComboBox														m_gridunit;																																																	// ComboBox

	// for
	// grid
	// units
	/*
	 * Added variables below in order to accommodate grid unit multiplier
	 */
	JTextField													m_gridunitmultiplier;

	/**
	 * Class to opens up hidden functionality to the UI's networking message package
	 * 
	 * @author Eric Maziade
	 */
	public class NetworkFrameResponder
	{
		/**
		 * Private constructor
		 */
		private NetworkFrameResponder()
		{
		}

		/**
		 * Updates the typing status
		 * 
		 * @param playerID ID of the player typing
		 * @param typing True if typing, false otherwise
		 */
		public void updateTypingStatus(int playerID, boolean typing)
		{
			Player player = m_core.getPlayer(playerID);
			if (player != null)
			{
				if (typing)
				{
					if (!m_typingPlayers.contains(player))
						m_typingPlayers.add(player);
				}
				else
					m_typingPlayers.remove(player);
			}

			updateStatus();
		}
	}

	/**
	 * Handles uncaught exceptions in the frame's thread
	 * 
	 * @author Eric Maziade
	 */
	private class FrameUncaughtExceptionHandler implements UncaughtExceptionHandler
	{
		/*
		 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
		 */
		@Override
		public void uncaughtException(Thread t, Throwable e)
		{
			Log.log(Log.SYS, "Uncaught exception!" + e.getMessage());
			e.printStackTrace();
			JOptionPane.showMessageDialog(GametableFrame.this, e.getMessage(), "Uncaught exception", JOptionPane.ERROR_MESSAGE);
		}
	}

}