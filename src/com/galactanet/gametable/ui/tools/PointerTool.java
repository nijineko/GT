/*
 * PointerTool.java: GameTable is in the Public Domain.
 */

package com.galactanet.gametable.ui.tools;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.data.prefs.PropertyDescriptor;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.SetPogAttributeDialog;
import com.galactanet.gametable.ui.GametableCanvas.GridModeID;
import com.galactanet.gametable.util.UtilityFunctions;
import com.maziade.props.XPropertyType;

/**
 * The basic pog interaction tool.
 * 
 * @author iffy
 * 
 *         #GT-AUDIT PointerTool
 */
public class PointerTool extends NullTool
{
	GridMode											m_gridMode;
	private final GameTableCore	m_core;
	private final GametableFrame m_frame;

	private class DeletePogAttributeActionListener implements ActionListener
	{
		private final String	key;

		DeletePogAttributeActionListener(final String name)
		{
			key = name;
		}

		public void actionPerformed(final ActionEvent e)
		{
			m_menuPog.removeAttribute(key);
		}
	}

	private class EditPogAttributeActionListener implements ActionListener
	{
		private final String	key;

		EditPogAttributeActionListener(final String name)
		{
			key = name;
		}

		public void actionPerformed(final ActionEvent e)
		{
			final SetPogAttributeDialog dialog = new SetPogAttributeDialog(true);
			dialog.loadValues(key, m_menuPog.getAttribute(key));
			dialog.setLocationRelativeTo(m_canvas);
			dialog.setVisible(true);
			if (!dialog.isConfirmed())
			{
				return;
			}

			final String name = dialog.getName();
			final String value = dialog.getValue();

			if ((name != null) && (name.length() > 0))
				m_menuPog.setAttribute(name, value);
			
			m_menuPog.removeAttribute(key);
		}
	}

	private static final String											PREF_DRAG		= PointerTool.class.getName() + ".drag";

	private static final List<PropertyDescriptor>	PREFERENCES	= createPreferenceList();

	/**
	 * @return The static, unmodifiable list of preferences for this tool.
	 */
	private static final List<PropertyDescriptor> createPreferenceList()
	{
		final List<PropertyDescriptor> retVal = new ArrayList<PropertyDescriptor>();
		retVal.add(new PropertyDescriptor(PREF_DRAG, XPropertyType.BOOLEAN, Boolean.TRUE.toString(), true, PropertyDescriptor.GROUP_TOOLS, -1));
		// "Drag map when not over Pog"
		return Collections.unmodifiableList(retVal);
	}

	private GametableCanvas	m_canvas;
	private GameTableMap		m_to;
	private boolean					m_clicked	= true;
	private MapElement			m_ghostPog;
	private MapElement			m_grabbedMapElement;
	private Point						m_grabOffset;
	private MapElement			m_lastPogMousedOver;
	private MapElement			m_menuPog	= null;
	private MapCoordinates	m_mousePosition;
	private boolean					m_snapping;

	private Point						m_startMouse;

	private MapCoordinates	m_startScroll;

	/**
	 * Constructor
	 */
	public PointerTool()
	{
		m_frame = GametableApp.getUserInterface();
		m_core = GameTableCore.getCore();
	}

	/*
	 * @see com.galactanet.gametable.AbstractTool#activate(com.galactanet.gametable.GametableCanvas)
	 */
	@Override
	public void activate(final GametableCanvas canvas)
	{
		m_canvas = canvas;
		m_grabbedMapElement = null;
		m_ghostPog = null;
		m_grabOffset = null;
		m_mousePosition = null;
		m_startScroll = null;
		m_startMouse = null;
	}

	@Override
	public void endAction()
	{
		m_grabbedMapElement = null;
		m_ghostPog = null;
		m_grabOffset = null;
		m_startScroll = null;
		m_startMouse = null;
		hoverCursorCheck();
		m_canvas.repaint();
	}

	/*
	 * @see com.galactanet.gametable.Tool#getPreferences()
	 */
	@Override
	public List<PropertyDescriptor> getPreferences()
	{
		return PREFERENCES;
	}

	private void hoverCursorCheck()
	{		
		if (m_core.getProperties().getBooleanPropertyValue(PREF_DRAG))
		{
			final MapElement pog = m_core.getMap(GameTableCore.MapType.ACTIVE).getMapElementAt(m_mousePosition);
			if (pog != null)
			{
				m_canvas.setToolCursor(0);
			}
			else
			{
				m_canvas.setToolCursor(1);
			}

			if (m_lastPogMousedOver != pog)
			{
				m_canvas.repaint();
			}
			m_lastPogMousedOver = pog;
		}
	}

	/*
	 * @see com.galactanet.gametable.Tool#isBeingUsed()
	 */
	@Override
	public boolean isBeingUsed()
	{
		return (m_grabbedMapElement != null) || (m_startScroll != null);
	}

	/*
	 * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
	 */
	@Override
	public void mouseButtonPressed(MapCoordinates modelPos, final int modifierMask)
	{
		m_clicked = true;
		m_mousePosition = modelPos;
		m_grabbedMapElement = m_core.getMap(GameTableCore.MapType.ACTIVE).getMapElementAt(m_mousePosition);
		if (m_grabbedMapElement != null)
		{
			m_ghostPog = new MapElement(m_grabbedMapElement);
			m_grabOffset = new Point(m_grabbedMapElement.getPosition().x - m_mousePosition.x, m_grabbedMapElement.getPosition().y - m_mousePosition.y);
			setSnapping(modifierMask);
		}
		else if (m_core.getProperties().getBooleanPropertyValue(PREF_DRAG))
		{
			m_startScroll = m_canvas.drawToModel(m_canvas.getScrollPosition());
			m_startMouse = m_canvas.modelToView(modelPos);
			m_canvas.setToolCursor(2);
		}
	}

	/*
	 * @see com.galactanet.gametable.AbstractTool#mouseButtonReleased(int, int)
	 */
	@Override
	public void mouseButtonReleased(MapCoordinates modelPos, final int modifierMask)
	{
		if (m_grabbedMapElement != null)
		{
			if (m_clicked)
			{
				popupContextMenu(modelPos, modifierMask);
			}
			else
			{
				if (!m_core.isMapElementLocked(m_grabbedMapElement))
				{
					if (!m_canvas.isPointVisible(m_mousePosition))
					{
						// We moved outside canvas, consider the mapElement as removed
						m_core.getMap(GameTableCore.MapType.ACTIVE).removeMapElement(m_grabbedMapElement);
					}
					else
					{
						m_canvas.moveLinkedMapElements(m_grabbedMapElement.getID(), m_ghostPog.getPosition());
					}
				}
			}
		}
		endAction();
	}

	/*
	 * @see com.galactanet.gametable.AbstractTool#mouseMoved(int, int)
	 */
	@Override
	public void mouseMoved(MapCoordinates modelPos, final int modifierMask)
	{
		setSnapping(modifierMask);
		m_mousePosition = modelPos;
		if ((m_grabbedMapElement != null) && !m_core.isMapElementLocked(m_grabbedMapElement))
		{
			m_clicked = false;
			if (m_snapping)
			{
				final Point adjustment = getSnapDragAdjustment(m_ghostPog);
				m_ghostPog.setPosition(m_mousePosition.delta(m_grabOffset.x + adjustment.x, m_grabOffset.y + adjustment.y));

				m_canvas.snapMapElementToGrid(m_ghostPog);
			}
			else
			{
				m_ghostPog.setPosition(m_mousePosition.delta(m_grabOffset.x, m_grabOffset.y));
			}
			m_canvas.repaint();
		}
		else if (m_startScroll != null)
		{
			final Point mousePosition = m_canvas.modelToView(modelPos);
			final MapCoordinates modelDelta = m_canvas.drawToModel(m_startMouse.x - mousePosition.x, m_startMouse.y - mousePosition.y);
			m_canvas.scrollMapTo(m_startScroll.delta(modelDelta));
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
	 * @see com.galactanet.gametable.AbstractTool#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(final Graphics g)
	{
		if ((m_ghostPog != null) && m_canvas.isPointVisible(m_mousePosition))
		{
			m_canvas.drawGhostlyToCanvas(m_ghostPog, g);
		}
	}

	/**
	 * @return A vector to adjust the drag position when snapping for odd-sized pogs.
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
	 * Pops up a pog context menu.
	 * 
	 * @param x X location of mouse.
	 * @param y Y location of mouse.
	 */
	private void popupContextMenu(MapCoordinates modelPos, final int modifierMask)
	{
		m_menuPog = m_grabbedMapElement;
		final JPopupMenu menu = new JPopupMenu("Pog");
		if ((modifierMask & MODIFIER_SHIFT) > 0) // holding shift
		{
			final int xLocation;
			final int yLocation;
			final float pogSize = m_menuPog.getFaceSize();
			MapCoordinates pogPos = m_menuPog.getPosition();
			final float tempSize = pogSize;
			final GridModeID m_gridModeId = m_core.getGridModeID();

			if (m_gridModeId == GridModeID.SQUARES) // square mode
			{
				xLocation = (int) ((pogPos.x / 64) + (((tempSize % 2 == 0) ? pogSize - 1 : pogSize) / 2));
				yLocation = (int) (((pogPos.y / 64) + (((tempSize % 2 == 0) ? pogSize - 1 : pogSize) / 2)) * -1);
			}
			else if (m_gridModeId == GridModeID.HEX) // hex mode - needs work to get it to display appropriate numbers
			{
				xLocation = pogPos.x;
				yLocation = pogPos.y * -1;
			}
			else
			// no grid
			{
				xLocation = pogPos.x;
				yLocation = pogPos.y * -1;
			}

			menu.add(new JMenuItem("X: " + xLocation));
			menu.add(new JMenuItem("Y: " + yLocation));
		}
		else
		{
			menu.add(new JMenuItem("Cancel"));
			JMenuItem item = new JMenuItem(m_core.isMapElementLocked(m_menuPog) ? "Unlock" : "Lock");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_core.lockMapElement(GameTableCore.MapType.ACTIVE, m_menuPog, !m_core.isMapElementLocked(m_menuPog));
					// System.out.println(m_menuPog.isLocked());
				}
			});
			menu.add(item);
			item = new JMenuItem(m_canvas.isSelected(m_menuPog) ? "Unselect" : "Select");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					if (m_canvas.isSelected(m_menuPog))
						m_canvas.selectMapElementInstance(m_menuPog, false);
					else
						m_canvas.selectMapElementInstance(m_menuPog, true);
				}
			});
			menu.add(item);

			if (m_core.getGroupManager(GameTableCore.MapType.ACTIVE).getGroup(m_menuPog) != null)
			{
				item = new JMenuItem("UnGroup");
				item.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent e)
					{
						Group group = m_core.getGroupManager(GameTableCore.MapType.ACTIVE).getGroup(m_menuPog);
						if (group != null)
							group.removeElement(m_menuPog);
					}
				});
				menu.add(item);
			}
			item = new JMenuItem(m_core.isActiveMapPublic() ? "Unpublish" : "Publish");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final MapElement pog = m_menuPog;
					if (m_core.isActiveMapPublic())
						m_to = m_core.getMap(GameTableCore.MapType.PRIVATE);
					else
						m_to = m_core.getMap(GameTableCore.MapType.PUBLIC);

					// this pog gets copied
					final MapElement newPog = new MapElement(pog);
					m_to.addMapElement(newPog);
					m_core.lockMapElement(GameTableCore.MapType.ACTIVE, newPog, m_core.isMapElementLocked(pog));

					if ((modifierMask & MODIFIER_CTRL) == 0) // not holding control
					{
						// remove the pogs that we moved
						m_core.getMap(GameTableCore.MapType.ACTIVE).removeMapElement(pog);
					}
				}
			});
			menu.add(item);
			item = new JMenuItem("Set Name...");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final String s = (String) JOptionPane.showInputDialog(m_frame, "Enter new name for this Pog:", "Set Pog Name", JOptionPane.PLAIN_MESSAGE,
							null, null, m_menuPog.getName());

					if (s != null)
					{
						m_menuPog.setName(s);
					}

				}
			});
			menu.add(item);
			item = new JMenuItem("Set Attribute...");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final SetPogAttributeDialog dialog = new SetPogAttributeDialog(false);
					dialog.setLocationRelativeTo(m_canvas);
					dialog.setVisible(true);

					if (dialog.isConfirmed())
					{
						final Map<String, String> toAdd = dialog.getAttribs();
						m_menuPog.setAttributes(toAdd);
					}
				}
			});
			menu.add(item);
			if (m_menuPog.getAttributeNames().size() > 0)
			{
				final JMenu editMenu = new JMenu("Edit Attribute");

				final JMenu removeMenu = new JMenu("Remove Attribute");
				final Set<String> nameSet = m_grabbedMapElement.getAttributeNames();

				for (String key : nameSet)
				{
					item = new JMenuItem(key);
					item.addActionListener(new DeletePogAttributeActionListener(key));
					removeMenu.add(item);

					item = new JMenuItem(key);
					item.addActionListener(new EditPogAttributeActionListener(key));
					editMenu.add(item);
				}
				menu.add(editMenu);
				menu.add(removeMenu);
			}

			// -------------------------------------
			// Copy Pog
			menu.addSeparator();
			item = new JMenuItem("Copy Pog...");
			// item.setAccelerator(KeyStroke.getKeyStroke("ctrl shift pressed S"));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final MapElement pog = m_menuPog;
					m_frame.copyMapElement(pog);

				}
			});

			menu.add(item);
			// -------------------------------------
			// Save Pog

			item = new JMenuItem("Save Pog...");
			// item.setAccelerator(KeyStroke.getKeyStroke("ctrl shift pressed S"));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					final MapElement pog = m_menuPog;
					final File spaf = UtilityFunctions.doFileSaveDialog("Save As", "pog", true);
					if (spaf != null)
					{
						m_frame.savePog(spaf, pog);
					}
				}
			});
			menu.add(item);

			// -------------------------------------
			// Pog Layers
			Layer layer = m_menuPog.getLayer();
			JMenu m_item = new JMenu("Change Layer");
			item = new JMenuItem("Underlay");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setLayer(Layer.UNDERLAY);
				}
			});

			if (layer == Layer.UNDERLAY)
				item.setEnabled(false);

			m_item.add(item);
			item = new JMenuItem("Overlay");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setLayer(Layer.OVERLAY);
				}
			});
			if (layer == Layer.OVERLAY)
				item.setEnabled(false);
			m_item.add(item);
			item = new JMenuItem("Environment");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setLayer(Layer.ENVIRONMENT);
				}
			});
			if (layer == Layer.ENVIRONMENT)
				item.setEnabled(false);
			m_item.add(item);
			item = new JMenuItem("Pog");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setLayer(Layer.POG);
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
					m_menuPog.setFaceSize(-1);
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
							m_menuPog.setFaceSize(is);
					}
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("0.5 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFaceSize( 0.5f);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("1 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFaceSize( 1);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("2 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFaceSize( 2);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("3 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFaceSize( 3);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("4 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFaceSize( 4);
				}
			});
			sizeMenu.add(item);

			item = new JMenuItem("6 squares");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFaceSize( 6);
				}
			});
			sizeMenu.add(item);

			menu.add(sizeMenu);

			final JMenu rotateMenu = new JMenu("Rotation");

			item = new JMenuItem("0");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(0);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("60");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(60);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("90");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(90);
				}
			});

			rotateMenu.add(item);
			item = new JMenuItem("120");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(120);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("180");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(180);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("240");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(240);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("270");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(270);
				}
			});
			rotateMenu.add(item);

			item = new JMenuItem("300");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setAngle(300);
				}
			});
			rotateMenu.add(item);

			menu.add(rotateMenu);

			final JMenu flipMenu = new JMenu("Flip");
			item = new JMenuItem("Reset");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFlip(false, false);
				}
			});
			flipMenu.add(item);

			item = new JMenuItem("Vertical");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFlip(m_menuPog.getFlipH(), !m_menuPog.getFlipV());
				}
			});
			flipMenu.add(item);

			item = new JMenuItem("Horizontal");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					m_menuPog.setFlip(!m_menuPog.getFlipH(), m_menuPog.getFlipV());
				}
			});
			flipMenu.add(item);

			menu.add(flipMenu);

			item = new JMenuItem("Change Image");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e)
				{
					List<MapElement> pogs = m_canvas.getSelectedMapElementInstances();

					int size = pogs.size();
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

					MapElement pog = pogs.get(0);
					m_menuPog.setMapElementType(pog.getMapElementType());
					m_canvas.unselectAllMapElementInstances();
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
							m_core.setBackgroundMapElementType(m_menuPog.getMapElementType(), null);
					}
				});
				menu.add(item);
			}

		}
		final Point mousePosition = m_canvas.modelToView(modelPos);
		menu.show(m_canvas, mousePosition.x, mousePosition.y);
	}

	/**
	 * Sets the snapping status based on the specified modifiers.
	 * 
	 * @param modifierMask the set of modifiers passed into the event.
	 */
	private void setSnapping(final int modifierMask)
	{
		if ((modifierMask & MODIFIER_CTRL) > 0)
		{
			m_snapping = false;
		}
		else
		{
			m_snapping = true;
		}
	}
}
