/*
 * MapElementMode.java
 * 
 * Copyright (C) 1999-2011 Open Source Game Table Project
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

package com.gametable.ui.modes;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.gametable.GametableApp;
import com.gametable.data.*;
import com.gametable.data.GameTableCore.MapType;
import com.gametable.data.MapElementTypeIF.Layer;
import com.gametable.data.prefs.PropertyDescriptor;
import com.gametable.ui.*;
import com.gametable.ui.GametableCanvas.GridModeID;
import com.gametable.util.UtilityFunctions;
import com.maziade.props.XProperties;

/**
 * The basic Map Element interaction tool.
 * Holds the map element context menu and basic interactions (selection, move, etc.)
 * 
 * @author iffy
 * 
 *         #GT-AUDIT MapElementMode (.1)
 */
public class MapElementMode extends UIMode
{
	private final static String PROPERTY_BUNDLE = "com.gametable.ui.modes";
	public static final String RESOURCE_PATH = "com.gametable.ui.modes.resources";
	
	/**
	 * Gets the instance of this mode
	 * 
	 * @return
	 */
	public static final MapElementMode getUIMode()
	{
		if (g_mode == null)
			g_mode = new MapElementMode();

		return g_mode;
	}

	/**
	 * Default Constructor.
	 */
	private MapElementMode()
	{
		super("Map Pointer");

		m_core = GametableApp.getCore();
		m_frame = GametableApp.getUserInterface();
		m_frame.registerUIMode(this, new ModeListener());

		initializeProperties();
		
		UIModeAction action = new UIModeAction(
				this, 
				"Default",
				"The default mode for the user interface",
				KeyEvent.VK_1,
				"arrow.png");
		
		m_frame.addUserInterfaceAction(action);
		
		m_cursorHand = m_frame.createMapCursor(CURSOR_HAND, CURSOR_HAND_HOTSPOT);
		m_cursorGrab = m_frame.createMapCursor(CURSOR_GRAB, CURSOR_GRAB_HOTSPOT);
	}

	/*
	 * @see com.gametable.ui.UIMode#isActive()
	 */
	@Override
	public boolean isActive()
	{
		// If we have an anchor point, then we're currently drawing a box
		return (m_grabbedMapElement != null) || (m_startScroll != null);
	}

	/*
	 * @see com.gametable.ui.UIMode#paintTool(java.awt.Graphics2D, com.gametable.ui.GametableCanvas)
	 */
	@Override
	public void paintTool(Graphics2D g, GametableCanvas canvas)
	{
		if ((m_ghostMapElement != null) && canvas.isPointVisible(m_mousePosition.x, m_mousePosition.y))
		{
			canvas.drawGhostlyToCanvas(m_ghostMapElement, g);
		}
	}

	/**
	 * @return A vector to adjust the drag position when snapping for odd-sized elements.
	 */
	private Point getSnapDragAdjustment(MapElement mapElement)
	{
		final Point adjustment = new Point();
		final int width = mapElement.getWidth();
		final int height = mapElement.getHeight();

		if (width < height)
		{
			adjustment.x = -(height - width) / 2;
		}
		else if (width > height)
		{
			adjustment.y = -(width - height) / 2;
		}

		return adjustment;
	}

	/**
	 * Handle mouse hovering
	 */
	private void hoverCursorCheck()
	{
		if (m_core.getProperties().getBooleanPropertyValue(PROPERTY_DRAG))
		{
			final MapElement mapElement = m_core.getMap(GameTableCore.MapType.ACTIVE).getMapElementAt(m_mouseMapPosition);
			if (mapElement != null)
				m_frame.setMapCursor(null);	// Set default cursor
			else
				m_frame.setMapCursor(m_cursorHand);

			if (m_lastMousedOver != mapElement)
				m_frame.repaint();
			
			m_lastMousedOver = mapElement;
		}
		else
			m_frame.setMapCursor(null);	// Set default cursor
	}

	/**
	 * Initialize the mode's properties
	 */
	private void initializeProperties()
	{
		XProperties properties = m_core.getProperties();

		// TODO #Property groups
		properties.addBooleanProperty(PROPERTY_DRAG, true, true, PropertyDescriptor.GROUP_TOOLS, -1, RESOURCE_PATH);
	}

	/**
	 * TODO #MapElementUI Add a hook to the map element's context menu
	 * 
	 * Pops up a Map Element's context menu.
	 * 
	 * @param x X location of mouse.
	 * @param y Y location of mouse.
	 */
	private void popupContextMenu(final int modifierMask)
	{
		m_menuMapElement = m_grabbedMapElement;

		final JPopupMenu menu = new JPopupMenu("Pog");

		if ((modifierMask & InputEvent.SHIFT_DOWN_MASK) > 0) // holding shift
		{
			final int xLocation;
			final int yLocation;

			final float elementFaceSize = m_menuMapElement.getFaceSize();
			MapCoordinates elementPosition = m_menuMapElement.getPosition();
			final float tempSize = elementFaceSize;
			final GridModeID gridModeId = m_core.getGridModeID();

			if (gridModeId == GridModeID.SQUARES) // square mode
			{
				xLocation = (int) ((elementPosition.x / GameTableMap.getBaseTileSize()) + (((tempSize % 2 == 0) ? elementFaceSize - 1 : elementFaceSize) / 2));
				yLocation = (int) (((elementPosition.y / GameTableMap.getBaseTileSize()) + (((tempSize % 2 == 0) ? elementFaceSize - 1 : elementFaceSize) / 2)) * -1);
			}
			else if (gridModeId == GridModeID.HEX) // hex mode - needs work to get it to display appropriate numbers
			{
				xLocation = elementPosition.x;
				yLocation = elementPosition.y * -1;
			}
			else
			// no grid
			{
				xLocation = elementPosition.x;
				yLocation = elementPosition.y * -1;
			}

			menu.add(new JMenuItem("X: " + xLocation));
			menu.add(new JMenuItem("Y: " + yLocation));
		}
		else
		{
			menu.add(new JMenuItem("Cancel"));

			JMenuItem item = new JMenuItem(m_core.isMapElementLocked(m_menuMapElement) ? "Unlock" : "Lock");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_core.lockMapElement(GameTableCore.MapType.ACTIVE, m_menuMapElement, !m_core.isMapElementLocked(m_menuMapElement));
				}
			});
			menu.add(item);

			item = new JMenuItem(m_frame.isSelected(m_menuMapElement, MapType.ACTIVE) ? "Unselect" : "Select");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					if (m_frame.isSelected(m_menuMapElement, MapType.ACTIVE))
						m_frame.selectMapElementInstance(m_menuMapElement, MapType.ACTIVE, false);
					else
						m_frame.selectMapElementInstance(m_menuMapElement, MapType.ACTIVE, true);
				}
			});
			menu.add(item);

			if (m_core.getGroupManager(GameTableCore.MapType.ACTIVE).getGroup(m_menuMapElement) != null)
			{
				item = new JMenuItem("UnGroup");
				item.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent e)
					{
						Group group = m_core.getGroupManager(GameTableCore.MapType.ACTIVE).getGroup(m_menuMapElement);
						if (group != null)
							group.removeElement(m_menuMapElement);
					}
				});
				menu.add(item);
			}
			item = new JMenuItem(m_core.isActiveMapPublic() ? "Unpublish" : "Publish");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final MapElement mapElement = m_menuMapElement;

					GameTableMap destinationMap;

					// We want public if we're working on private, and vice-versa
					if (m_core.isActiveMapPublic())
						destinationMap = m_core.getMap(GameTableCore.MapType.PRIVATE);
					else
						destinationMap = m_core.getMap(GameTableCore.MapType.PUBLIC);

					// this element gets copied
					final MapElement newElement = new MapElement(mapElement);
					destinationMap.addMapElement(newElement);
					m_core.lockMapElement(GameTableCore.MapType.ACTIVE, newElement, m_core.isMapElementLocked(mapElement));

					if ((modifierMask & InputEvent.CTRL_DOWN_MASK) == 0) // not holding control
					{
						// remove the element that we moved
						m_core.getMap(GameTableCore.MapType.ACTIVE).removeMapElement(mapElement);
					}
				}
			});
			menu.add(item);
			item = new JMenuItem("Set Name...");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final String s = (String) JOptionPane.showInputDialog(m_frame, "Enter new name for this map element:", "Set name", JOptionPane.PLAIN_MESSAGE,
							null, null, m_menuMapElement.getName());

					if (s != null)
					{
						m_menuMapElement.setName(s);
					}

				}
			});
			menu.add(item);
			item = new JMenuItem("Set Attribute...");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final MapElementAttributeDialog dialog = new MapElementAttributeDialog(false);
					dialog.setLocationRelativeTo(m_frame);
					dialog.setVisible(true);

					if (dialog.isConfirmed())
					{
						final Map<String, String> toAdd = dialog.getAttribs();
						m_menuMapElement.setAttributes(toAdd);
					}
				}
			});
			menu.add(item);
			if (m_menuMapElement.getAttributeNames().size() > 0)
			{
				final JMenu editMenu = new JMenu("Edit Attribute");

				final JMenu removeMenu = new JMenu("Remove Attribute");
				final Set<String> nameSet = m_grabbedMapElement.getAttributeNames();

				for (String key : nameSet)
				{
					item = new JMenuItem(key);
					item.addActionListener(new DeleteAttributeActionListener(key));
					removeMenu.add(item);

					item = new JMenuItem(key);
					item.addActionListener(new EditAttributeActionListener(key));
					editMenu.add(item);
				}
				menu.add(editMenu);
				menu.add(removeMenu);
			}

			// -------------------------------------
			// Copy element
			
			menu.addSeparator();
			item = new JMenuItem("Copy Pog...");
			// item.setAccelerator(KeyStroke.getKeyStroke("ctrl shift pressed S"));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final MapElement mapElement = m_menuMapElement;
					m_frame.copyMapElement(mapElement);

				}
			});

			menu.add(item);
			// -------------------------------------
			// Save element

			item = new JMenuItem("Save Pog...");
			// item.setAccelerator(KeyStroke.getKeyStroke("ctrl shift pressed S"));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final MapElement mapElement = m_menuMapElement;
					final File spaf = UtilityFunctions.doFileSaveDialog("Save As", "pog", true);
					if (spaf != null)
					{
						m_frame.saveMapElement(mapElement, spaf);
					}
				}
			});
			menu.add(item);

			// -------------------------------------
			// Layers
			Layer layer = m_menuMapElement.getLayer();
			JMenu m_item = new JMenu("Change Layer");
			item = new JMenuItem("Underlay");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setLayer(Layer.UNDERLAY);
				}
			});

			if (layer == Layer.UNDERLAY)
				item.setEnabled(false);

			m_item.add(item);
			item = new JMenuItem("Overlay");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setLayer(Layer.OVERLAY);
				}
			});
			if (layer == Layer.OVERLAY)
				item.setEnabled(false);
			m_item.add(item);
			item = new JMenuItem("Environment");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setLayer(Layer.ENVIRONMENT);
				}
			});
			if (layer == Layer.ENVIRONMENT)
				item.setEnabled(false);
			m_item.add(item);
			item = new JMenuItem("Pog");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setLayer(Layer.POG);
				}
			});
			if (layer == Layer.POG)
				item.setEnabled(false);
			m_item.add(item);
			menu.add(m_item);

			menu.addSeparator();
			// -------------------------------------

			final JMenu sizeMenu = new JMenu("Face Size");
			item = new JMenuItem("Reset");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFaceSize(-1);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("Custom");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final String ns = (String) JOptionPane.showInputDialog(m_frame, "New Size in Squares", "Pog Size", JOptionPane.PLAIN_MESSAGE, null, null,
							"");

					if (ns != null)
					{
						final int is = Integer.parseInt(ns);
						if (is >= 1)
							m_menuMapElement.setFaceSize(is);
					}
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("0.5 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFaceSize(0.5f);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("1 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFaceSize(1);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("2 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFaceSize(2);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("3 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFaceSize(3);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("4 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFaceSize(4);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("6 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFaceSize(6);
				}
			});
			sizeMenu.add(item);

			menu.add(sizeMenu);

			final JMenu rotateMenu = new JMenu("Rotation");

			item = new JMenuItem("0");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(0);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("60");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(60);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("90");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(90);
				}
			});

			rotateMenu.add(item);
			item = new JMenuItem("120");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(120);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("180");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(180);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("240");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(240);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("270");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(270);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("300");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setAngle(300);
				}
			});
			rotateMenu.add(item);

			menu.add(rotateMenu);

			final JMenu flipMenu = new JMenu("Flip");
			item = new JMenuItem("Reset");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFlip(false, false);
				}
			});
			flipMenu.add(item);

			item = new JMenuItem("Vertical");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFlip(m_menuMapElement.getFlipH(), !m_menuMapElement.getFlipV());
				}
			});
			flipMenu.add(item);

			item = new JMenuItem("Horizontal");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuMapElement.setFlip(!m_menuMapElement.getFlipH(), m_menuMapElement.getFlipV());
				}
			});
			flipMenu.add(item);

			menu.add(flipMenu);

			item = new JMenuItem("Change Image");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					List<MapElement> elements = m_frame.getSelectedMapElementInstances(MapType.ACTIVE);

					int size = elements.size();
					if (size > 1)
					{
						JOptionPane.showMessageDialog(m_frame, "You must only have 1 Pog selected.", "Image Selection", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					else if (size == 0)
					{
						JOptionPane.showMessageDialog(m_frame, "No Pogs Selected.", "Image Selection", JOptionPane.INFORMATION_MESSAGE);
						return;
					}

					MapElement mapElement = elements.get(0);
					m_menuMapElement.setMapElementType(mapElement.getMapElementType());
					m_frame.unselectAllMapElementInstances(MapType.ACTIVE);
				}
			});
			menu.add(item);

			// --------------------------------
			if (layer == Layer.UNDERLAY)
			{
				menu.addSeparator();
				item = new JMenuItem("Set as Background");
				item.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent e)
					{
						final int result = UtilityFunctions.yesNoDialog(m_frame, "Are you sure you wish to change the background to this pog's Image?",
								"Change Background?");
						if (result == UtilityFunctions.YES)
							m_core.setBackgroundMapElementType(m_menuMapElement.getMapElementType(), null);
					}
				});
				menu.add(item);
			}

		}

		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		Point framePos = m_frame.getLocationOnScreen();

		menu.show(m_frame, mousePos.x - framePos.x, mousePos.y - framePos.y);
	}

	/**
	 * Sets the snapping status based on the specified modifiers.
	 * 
	 * @param modifierMask the set of modifiers passed into the event.
	 */
	private void setSnapping(final int modifierMask)
	{
		if ((modifierMask & InputEvent.CTRL_DOWN_MASK) > 0)
		{
			m_snapping = false;
		}
		else
		{
			m_snapping = true;
		}
	}

	/**
	 * Grab cursor name
	 */
	public static final String CURSOR_GRAB = "grab.png";

	/**
	 * Hand cursor name
	 */
	public static final String CURSOR_HAND = "hand.png";

	/**
	 * Grab cursor hot spot
	 */
	private static final Point CURSOR_GRAB_HOTSPOT = new Point(8, 8);	// TODO #Cursor file desc?

	/**
	 * Hand cursor hot spot
	 */
	private static final Point CURSOR_HAND_HOTSPOT = new Point(8, 8);

	/**
	 * Single instance of this mode
	 */
	private static MapElementMode		g_mode						= null;

	private static final String		PROPERTY_DRAG					= PROPERTY_BUNDLE + ".drag";

	private boolean								m_clicked					= true;

	/**
	 * Game table core instance
	 */
	private final GameTableCore		m_core;

	/**
	 * Grab cursor
	 */
	private final Cursor m_cursorGrab;

	/**
	 * Hand cursor
	 */
	private final Cursor m_cursorHand;

	/**
	 * Instance to game table frame
	 */
	private final GametableFrame	m_frame;

	/**
	 * Transitive copy of the map element - a 'ghostly' image of the map element is draw on the location where we are
	 * dragging
	 */
	private MapElement						m_ghostMapElement;

	/**
	 * Map element that was clicked on during a cursor operation
	 */
	private MapElement						m_grabbedMapElement;

	/**
	 * The distance between current position and original position
	 */
	private Point									m_grabOffset;

	/**
	 * The last map element that the cursor hovered over
	 */
	private MapElement						m_lastMousedOver;

	/**
	 * The map element for whom a context menu has been opened
	 */
	private MapElement						m_menuMapElement	= null;

	/**
	 * Position of the mouse when it started dragging
	 */
	private Point				m_mousePosition = new Point();
	
	/**
	 * Position of the mouse on the map
	 */
	MapCoordinates m_mouseMapPosition; 

	/**
	 * Remembers whether we were snapping coordinates to the grid or not
	 */
	private boolean								m_snapping;

	/**
	 * Start map coordinates for a drag
	 */
	private Point				m_startMapPosition;

	/**
	 * Start map coordinates for the scroll position
	 */
	private MapCoordinates				m_startScroll;

	private class DeleteAttributeActionListener implements ActionListener
	{
		DeleteAttributeActionListener(final String name)
		{
			key = name;
		}

		public void actionPerformed(final ActionEvent e)
		{
			m_menuMapElement.removeAttribute(key);
		}

		private final String	key;
	}
	
	private class EditAttributeActionListener implements ActionListener
	{
		EditAttributeActionListener(final String name)
		{
			key = name;
		}

		public void actionPerformed(final ActionEvent e)
		{
			final MapElementAttributeDialog dialog = new MapElementAttributeDialog(true);
			dialog.loadValues(key, m_menuMapElement.getAttribute(key));
			dialog.setLocationRelativeTo(m_frame);
			dialog.setVisible(true);
			if (!dialog.isConfirmed())
			{
				return;
			}

			final String name = dialog.getName();
			final String value = dialog.getValue();

			if ((name != null) && (name.length() > 0))
				m_menuMapElement.setAttribute(name, value);

			m_menuMapElement.removeAttribute(key);
		}

		private final String	key;
	}
	
	/**
	 * Listener
	 */
	private class ModeListener implements UIModeListener
	{
		/*
		 * @see com.gametable.ui.UIModeListener#cancelMode()
		 */
		@Override
		public void cancelMode()
		{
			m_grabbedMapElement = null;
			m_ghostMapElement = null;
			m_grabOffset = null;
			m_startScroll = null;
			m_startMapPosition = null;
			hoverCursorCheck();
			m_frame.repaint();
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonPressed(int, int)
		 */
		@Override
		public void mouseButtonPressed(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			m_clicked = true;
			m_mousePosition.setLocation(mouseX, mouseY);
			m_mouseMapPosition = canvas.viewToModel(mouseX, mouseY);
			m_grabbedMapElement = m_core.getMap(GameTableCore.MapType.ACTIVE).getMapElementAt(m_mouseMapPosition);

			if (m_grabbedMapElement != null)
			{
				m_ghostMapElement = new MapElement(m_grabbedMapElement);
				MapCoordinates grabPos = m_grabbedMapElement.getPosition();
				
				m_grabOffset = new Point(grabPos.x - m_mouseMapPosition.x, grabPos.y - m_mouseMapPosition.y);
				setSnapping(modifierMask);
			}
			else if (m_core.getProperties().getBooleanPropertyValue(PROPERTY_DRAG))
			{
				m_startScroll = m_frame.getMapScrollPosition();
				m_startMapPosition = new Point(mouseX, mouseY);

				m_frame.setMapCursor(m_cursorGrab);
			}
		}

		/*
		 * @see com.gametable.AbstractTool#mouseButtonReleased(int, int)
		 */
		@Override
		public void mouseButtonReleased(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			if (m_grabbedMapElement != null)
			{
				if (m_clicked)
				{
					popupContextMenu(modifierMask);
				}
				else
				{
					if (!m_core.isMapElementLocked(m_grabbedMapElement))
					{
						if (!canvas.isPointVisible(m_mousePosition.x, m_mousePosition.y))
						{
							// We moved outside canvas, consider the mapElement as removed
							m_core.getMap(GameTableCore.MapType.ACTIVE).removeMapElement(m_grabbedMapElement);
						}
						else
						{
							m_frame.moveLinkedMapElements(m_grabbedMapElement.getID(), m_ghostMapElement.getPosition());
						}
					}
				}
			}
			cancelMode();
		}

		/*
		 * @see com.gametable.AbstractTool#mouseMoved(int, int)
		 */
		@Override
		public void mouseMoved(GametableCanvas canvas, int mouseX, int mouseY, final int modifierMask)
		{
			setSnapping(modifierMask);
			m_mousePosition.setLocation(mouseX, mouseY);
			m_mouseMapPosition = canvas.viewToModel(mouseX, mouseY);
			
			if ((m_grabbedMapElement != null) && !m_core.isMapElementLocked(m_grabbedMapElement))
			{
				m_clicked = false;
				if (m_snapping)
				{
					final Point adjustment = getSnapDragAdjustment(m_ghostMapElement);
					m_ghostMapElement.setPosition(m_mouseMapPosition.delta(m_grabOffset.x + adjustment.x, m_grabOffset.y + adjustment.y));

					m_core.getGridMode().snapMapElementToGrid(m_ghostMapElement);
				}
				else
				{
					m_ghostMapElement.setPosition(m_mouseMapPosition.delta(m_grabOffset.x, m_grabOffset.y));
				}

				m_frame.repaint();
			}
			else if (m_startScroll != null)
			{
				canvas.moveScrollPosition(
					-(m_mousePosition.x - m_startMapPosition.x), 
					-(m_mousePosition.y - m_startMapPosition.y));
			}
			else if ((m_grabbedMapElement != null) && m_core.isMapElementLocked(m_grabbedMapElement))
			{
				m_clicked = false;
			}
			else
			{
				hoverCursorCheck();
			}
		}

		/*
		 * @see com.gametable.ui.UIModeListener#selectMode()
		 */
		@Override
		public void selectMode()
		{
			m_grabbedMapElement = null;
			m_ghostMapElement = null;
			m_grabOffset = null;
			m_startScroll = null;
			m_startMapPosition = null;
			m_frame.setMapCursor(null);
		}
	}	
}