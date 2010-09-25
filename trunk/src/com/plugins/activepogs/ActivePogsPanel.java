/*
 * ActivePogsPanel.java: GameTable is in the Public Domain.
 */

package com.plugins.activepogs;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.lf5.viewer.categoryexplorer.TreeModelAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.*;
import com.galactanet.gametable.net.NetworkStatus;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.ImageCache;
import com.galactanet.gametable.util.Images;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;
import com.maziade.tools.XMLUtils;

/**
 * Tree view for active pogs and attributes.
 * 
 * @author iffy
 * 
 * #GT-AUDIT ActivePogsPanel
 */
public class ActivePogsPanel extends JPanel
{
	/**
	 * Cell renderer for the tree.
	 * 
	 * @author Iffy
	 */
	private static class ActivePogTreeCellRenderer extends JComponent implements TreeCellRenderer
	{
		/**
		 * Serial
		 */
		private static final long	serialVersionUID	= 2211176162170052851L;
		
		/**
		 * Current attribute to render
		 */
		String										m_attribute					= null;
		
		/**
		 * Current element to render
		 */
		MapElement								m_element						= null;

		/*
		 * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
		 */
		@Override
		public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean exp, final boolean lf,
				final int r, final boolean focus)
		{
			m_element = null;
			m_attribute = null;
			
			if (value instanceof MapElementNode)
			{
				MapElementNode node = (MapElementNode) value;
				m_element = node.getMapElement();
			}
			else if (value instanceof AttributeNode)
			{
				AttributeNode node = (AttributeNode) value;
				m_element = node.getMapElement();
				m_attribute = node.getAttribute();
			}

			Dimension size = getMySize();
			
			setSize(size);
			setPreferredSize(size);

			return this;
		}

		/*
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		@Override
		protected void paintComponent(final Graphics g)
		{
			if (m_element == null)
			{
				return;
			}

			final MapElementTypeIF pogType = m_element.getMapElementType();
			final Graphics2D g2 = (Graphics2D) g;
			
			g2.addRenderingHints(Images.getRenderingHints());
			g2.setColor(UIManager.getColor("Label.foreground"));
			
			if (m_attribute == null)
			{
				Image icon = pogType.getListIcon();
				if (icon != null)
				{
					g2.drawImage(icon, SPACE + (g_iconSize - icon.getWidth(null)) / 2, SPACE + (g_iconSize - icon.getHeight(null)) / 2, this);
				}

				final String label = getLabel();
				if ((label != null) && (label.length() > 0))
				{
					
					g2.setFont(getBaseFont());
					
					final FontMetrics fm = g2.getFontMetrics();
					final Rectangle stringBounds = fm.getStringBounds(label, g2).getBounds();
					final int drawX = SPACE + g_iconSize + POG_TEXT_PADDING;
					final int drawY = SPACE + (g_iconSize - stringBounds.height) / 2 - stringBounds.y;
					g2.drawString(label, drawX, drawY);
				}
			}
			else
			{
				final String label = getLabel();
				final String value = getValue();
				final Rectangle keyBounds = g2.getFontMetrics(getKeyNameFont()).getStringBounds(label, g2).getBounds();
				final Rectangle valueBounds = g2.getFontMetrics(getBaseFont()).getStringBounds(value, g2).getBounds();
				final int drawX = SPACE;
				final int drawY = SPACE + Math.max(Math.abs(keyBounds.y), Math.abs(valueBounds.y));
				g2.setFont(getKeyNameFont());
				g2.drawString(label, drawX, drawY);
				g2.setFont(getBaseFont());
				g2.drawString(value, drawX + keyBounds.width, drawY);
			}
			
		}

		/**
		 * Get the attribute's label
		 * @return
		 */
		private String getLabel()
		{
			if (m_attribute != null)
			{
				return m_attribute + ": ";
			}
			return m_element.getName();
		}

		/**
		 * @return The computed dimensions for this element
		 */
		private Dimension getMySize()
		{
			if (m_element == null)
			{
				return new Dimension(g_iconSize, g_iconSize);
			}

			if (m_attribute == null)
			{
				int w = g_iconSize;
				int h = g_iconSize;
				final String label = getLabel();
				if ((label != null) && (label.length() > 0))
				{
					final FontRenderContext frc = new FontRenderContext(null, true, false);
					final Rectangle stringBounds = getBaseFont().getStringBounds(label, frc).getBounds();
					w += stringBounds.width + POG_TEXT_PADDING;
					if (stringBounds.height > h)
					{
						h = stringBounds.height;
					}
				}

				return new Dimension(w + TOTAL_SPACE, h + TOTAL_SPACE);
			}

			int w = 0;
			int h = 0;

			final FontRenderContext frc = new FontRenderContext(null, true, false);
			final Rectangle keyBounds = getKeyNameFont().getStringBounds(getLabel(), frc).getBounds();
			final Rectangle valueBounds = getBaseFont().getStringBounds(getValue(), frc).getBounds();
			h = Math.max(keyBounds.height, valueBounds.height);
			w = keyBounds.width + valueBounds.width;

			return new Dimension(w + TOTAL_SPACE, h + TOTAL_SPACE);
		}

		/**
		 * This element's value
		 * @return
		 */
		private String getValue()
		{
			final String value = m_element.getAttribute(m_attribute);
			if (value == null)
			{
				return "";
			}

			return value;
		}
	}

	private static final float											CLICK_THRESHHOLD		= 2f;
	
	private static int															g_iconSize;

	private static final int												POG_BORDER					= 0;

	private static final int												POG_MARGIN					= 0;

	private static final int												POG_PADDING					= 1;

	private static final int												POG_TEXT_PADDING		= 4;

	/**
	 * Serial number 
	 */
	private static final long												serialVersionUID		= -5840985576215910472L;

	private static final int												SPACE								= POG_PADDING + POG_BORDER + POG_MARGIN;

	private static final int												TOTAL_SPACE					= SPACE * 2;

	private long													g_nextSortID			= 1;

	// --- Pog Dragging Members ---

	private final Map<MapElementID, Long>	m_elementSortIDs	= new HashMap<MapElementID, Long>();

	/**
	 * The currently grabbed pog.
	 */
	private MapElementNode																	m_grabbedNode				= null;

	/**
	 * The offset at which the pog was grabbed.
	 */
	private Point																		m_grabOffset				= null;

	/**
	 * The position of the currently grabbed pog.
	 */
	private Point																		m_grabPosition			= null;

	private Point																		m_lastPressPosition	= null;
	private int																			m_numClicks					= 0;

	
	private GameTableMapTreeModel										m_privateTreeModel;

	private GameTableMapTreeModel										m_publicTreeModel;
	/**
	 * The main component for this damn thing.
	 */
	private JTree																		m_tree;
	private final ActivePogTreeCellRenderer					pogRenderer					= new ActivePogTreeCellRenderer();

	/**
	 * The scroll pane for the tree.
	 */
	private JScrollPane															scrollPane;

	/**
	 * Constructor
	 */
	protected ActivePogsPanel()
	{
		super(new BorderLayout());
		
		g_iconSize = GametableApp.getIntegerProperty(GametableApp.PROPERTY_ICON_SIZE);
		
		// Force create tree
		createTree();
		
		scrollPane = new JScrollPane(m_tree);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		add(scrollPane, BorderLayout.CENTER);
		add(createToolbar(), BorderLayout.NORTH);
	}

	/*
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(final Graphics g)
	{
		super.paint(g);
		try
		{
			if (m_grabbedNode != null)
			{
				final Graphics2D g2 = (Graphics2D) g;
				final Point treePos = UtilityFunctions.getComponentCoordinates(m_tree, m_grabPosition);
				final Point localPos = UtilityFunctions.getComponentCoordinates(this, m_grabPosition);

				MapElementNode node = getClosestPogNode(treePos.x, treePos.y);
				if (node != null)
				{
					int row = getRowForNode(node);
					if (row > -1)
					{
						Rectangle bounds = m_tree.getRowBounds(row);
						Point thisPos = UtilityFunctions.convertCoordinates(m_tree, this, new Point(bounds.x, bounds.y));
						int drawY = thisPos.y;

						// go to next node if in attributes area
						if (localPos.y > thisPos.y + bounds.height)
						{
							final MapElementNode nextNode = (MapElementNode)node.getNextSibling();
							if (nextNode == null)
							{
								drawY += bounds.height;
							}
							else
							{
								node = nextNode;
								row = getRowForNode(node);
								bounds = m_tree.getRowBounds(row);
								thisPos = UtilityFunctions.convertCoordinates(m_tree, this, new Point(bounds.x, bounds.y));
								drawY = thisPos.y;
							}
						}

						final int PADDING = 5;
						final int drawX = PADDING;
						g2.setColor(Color.DARK_GRAY);
						g2.drawLine(drawX, drawY, drawX + m_tree.getWidth() - (PADDING * 2), drawY);
					}
				}

				g2.translate(localPos.x - m_grabOffset.x, localPos.y - m_grabOffset.y);
				final JComponent comp = (JComponent) pogRenderer.getTreeCellRendererComponent(m_tree, m_grabbedNode, false, false, true, 0, false);
				comp.paint(g2);
				g2.dispose();
			}
		}
		catch (final Throwable t)
		{
			Log.log(Log.SYS, t);
		}
	}

	/**
	 * Load from an XML DOM node
	 * 
	 * @param node An XML node located by the engine.
	 * @param converter to convert stored MapElementIDs to actual map element IDs
	 */
	protected void loadFromXML(Element node, XMLSerializeConverter converter)
	{
		m_elementSortIDs.clear();
		g_nextSortID = 0;

		Element sortEl = XMLUtils.getFirstChildElementByTagName(node, "sort");
		if (sortEl == null)
			return;

		GametableFrame frame = GametableFrame.getGametableFrame();

		for (Element itemEl : XMLUtils.getChildElementsByTagName(sortEl, "item"))
		{
			long lid = UtilityFunctions.parseLong(itemEl.getAttribute("id"), 0);
			MapElementID elementID = converter.getMapElementID(lid);

			MapElement mapEl = frame.getMapElement(elementID);
			if (mapEl != null)
			{
				long val = UtilityFunctions.parseLong(itemEl.getAttribute("val"), 0);
				g_nextSortID = Math.max(g_nextSortID, val + 1);

				m_elementSortIDs.put(elementID, val);
			}
		}
		
		m_publicTreeModel.reorderElements();
		m_privateTreeModel.reorderElements();
	}
	
	/**
	 * Reorder elements due to user dragging operation
	 * @param changes
	 * @param notifyNetwork
	 */
	protected void reorderElements(final Map<MapElementID, Long> changes, boolean notifyNetwork)
	{
		GametableFrame frame = GametableFrame.getGametableFrame();

		if (notifyNetwork && frame.getGametableCanvas().isPublicMap())
		{
			frame.sendBroadcast(NetSetMapElementOrder.makePacket(changes));
			if (frame.getNetworkStatus() != NetworkStatus.CONNECTED)
			{
				reorderElements(changes);
			}
		}
		else
		{
			reorderElements(changes);
		}
	}

	/**
	 * Save to an XML DOM node
	 * 
	 * @param node An XML node created by the engine. Data can be added to the node, but the node's attributes should not
	 *          be modified by modules.
	 * @return false if There is nothing to save (node will be discarded)
	 */
	protected boolean saveToXML(Element node)
	{
		if (m_elementSortIDs.size() == 0)
			return false;

		Document doc = node.getOwnerDocument();
		Element sort = doc.createElement("sort");
		node.appendChild(sort);

		for (Entry<MapElementID, Long> entry : m_elementSortIDs.entrySet())
		{
			Element item = doc.createElement("item");
			item.setAttribute("id", String.valueOf(entry.getKey().numeric()));
			item.setAttribute("val", String.valueOf(entry.getValue()));
			sort.appendChild(item);
		}

		return true;
	}

	/**
	 * Set which map is to be displayed
	 * @param publicMap
	 */
	protected void showPublicMap(boolean publicMap)
	{
		m_tree.setModel(publicMap ? m_publicTreeModel : m_privateTreeModel);
	}

	/**
	 * Collapse all attributes, leaving only map elements
	 */
	private void collapseTree()
	{	
		expandTreeRoot();
		
		TreeModel model = m_tree.getModel();
		int max = model.getChildCount(model.getRoot());

		for (int i = 0; i < max; i++)
			m_tree.collapseRow(i);
	}

	/**
	 * Create the panel's tool bar
	 * @return
	 */
	private JToolBar createToolbar()
	{
		final JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setMargin(new Insets(2, 2, 2, 2));
		toolbar.setRollover(true);

		final Insets margin = new Insets(2, 2, 2, 2);
		final Image collapseImage = ImageCache.getImage(new File("assets/collapse.png"));
		final JButton collapseButton = new JButton("Collapse All", new ImageIcon(collapseImage));
		collapseButton.setFocusable(false);
		collapseButton.setMargin(margin);
		collapseButton.addActionListener(new ActionListener() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				collapseTree();
			}
		});
		toolbar.add(collapseButton);

		final Image expandImage = ImageCache.getImage(new File("assets/expand.png"));
		final JButton expandButton = new JButton("Expand All", new ImageIcon(expandImage));
		expandButton.setMargin(margin);
		expandButton.setFocusable(false);
		expandButton.addActionListener(new ActionListener() {
			/*
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(final ActionEvent e)
			{
				expandTree();
			}
		});
		toolbar.add(expandButton);

		return toolbar;
	}

	private JTree createTree()
	{
		if (m_tree == null)
		{
			GametableCanvas canvas = GametableFrame.getGametableFrame().getGametableCanvas();
			
			TreeModelListener listener = new TreeModelAdapter() {
				
				/*
				 * @see org.apache.log4j.lf5.viewer.categoryexplorer.TreeModelAdapter#treeNodesInserted(javax.swing.event.TreeModelEvent)
				 */
				@Override
				public void treeNodesInserted(TreeModelEvent e)
				{
					// Make sure the invisible root is expanded
					expandTreeRoot();
				}
				/*
				 * @see org.apache.log4j.lf5.viewer.categoryexplorer.TreeModelAdapter#treeStructureChanged(javax.swing.event.TreeModelEvent)
				 */
				@Override
				public void treeStructureChanged(TreeModelEvent e)
				{
					// Make sure the invisible root is expanded
					expandTreeRoot();
				}
			};

			// -------------------
			GameTableMap map = canvas.getPublicMap();
			m_publicTreeModel = new GameTableMapTreeModel(map, m_elementSortIDs);
			m_publicTreeModel.addTreeModelListener(listener);

			// -------------------
			map = canvas.getPrivateMap();
			m_privateTreeModel = new GameTableMapTreeModel(map, m_elementSortIDs);
			m_privateTreeModel.addTreeModelListener(listener);

			m_tree = new JTree(m_publicTreeModel);
			
			m_tree.setRootVisible(false);
			m_tree.setShowsRootHandles(true);
			m_tree.setToggleClickCount(3);
			m_tree.setSelectionModel(null);
			m_tree.setCellRenderer(pogRenderer);
			m_tree.setRowHeight(0);	// Sets as variable row height
			m_tree.setFocusable(false);
			m_tree.addMouseListener(new MouseAdapter() {

				/*
				 * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
				 */
				@Override
				public void mousePressed(MouseEvent e)
				{
					m_lastPressPosition = new Point(e.getX(), e.getY());
					TreePath path = m_tree.getPathForLocation(e.getX(), e.getY());
					
					if (path == null)
						return;

					Object val = path.getLastPathComponent();
					if (val instanceof MapElementNode)
					{
						MapElementNode node = (MapElementNode) val;
						Point screenCoords = UtilityFunctions.getScreenCoordinates(m_tree, m_lastPressPosition);

						Image icon = node.getMapElement().getMapElementType().getListIcon();

						Point localCoords = icon == null ? new Point(0, 0) : new Point(icon.getWidth(null) / 2, icon.getHeight(null) / 2);
						onMapElementGrabbed(node, screenCoords, localCoords);
					}
				}

				/*
				 * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
				 */
				@Override
				public void mouseReleased(final MouseEvent e)
				{
					onMapElementReleased();
					final Point p = new Point(e.getX(), e.getY());

					if (p.distance(m_lastPressPosition) <= CLICK_THRESHHOLD)
					{
						m_numClicks++;
					}

					if (m_numClicks == 2)
					{
						final MapElementNode node = getPogNode(e.getX(), e.getY());
						if (node != null)
						{
							GametableFrame.getGametableFrame().getGametableCanvas().scrollToPog(node.getMapElement());
						}
					}
				}

			});

			m_tree.addMouseMotionListener(new MouseMotionAdapter() {
				/*
				 * @see java.awt.event.MouseMotionAdapter#mouseDragged(java.awt.event.MouseEvent)
				 */
				@Override
				public void mouseDragged(final MouseEvent e)
				{
					mouseMoved(e);
				}

				/*
				 * @see java.awt.event.MouseMotionAdapter#mouseMoved(java.awt.event.MouseEvent)
				 */
				@Override
				public void mouseMoved(final MouseEvent e)
				{
					Point screenCoords = UtilityFunctions.getScreenCoordinates(m_tree, new Point(e.getX(), e.getY()));
					onMapElementMoved(screenCoords);
					m_numClicks = 0;
				}
			});
		}
		
		return m_tree;
	}

	/**
	 * Expand everything
	 */
	private void expandTree()
	{
		expandTreeRoot();
		TreeModel model = m_tree.getModel();
		int max = model.getChildCount(model.getRoot());

		for (int i = 0; i < max; i++)
			m_tree.expandRow(i);
	}

	/**
	 * 
	 */
	private void expandTreeRoot()
	{
		TreeModel model = m_tree.getModel();
		Object root = model.getRoot();
		TreePath path = new TreePath(root);
		m_tree.expandPath(path);
	}
	
	/**
	 * Find element node closes to coordinates
	 * 
	 * @param x
	 * @param y
	 * 
	 * @return
	 */
	private MapElementNode getClosestPogNode(final int x, final int y)
	{
		final TreePath path = m_tree.getClosestPathForLocation(x, y);
		if (path == null)
		{
			return null;
		}

		for (int i = path.getPathCount(); i-- > 0;)
		{
			final Object val = path.getPathComponent(i);
			if (val instanceof MapElementNode)
			{
				return (MapElementNode) val;
			}
		}

		return null;
	}
	
	/**
	 * Get map element node at specified coordinates
	 * @param x
	 * @param y
	 * @return
	 */
	private MapElementNode getPogNode(final int x, final int y)
	{
		final TreePath path = m_tree.getPathForLocation(x, y);
		if (path == null)
		{
			return null;
		}

		for (int i = path.getPathCount(); i-- > 0;)
		{
			final Object val = path.getPathComponent(i);
			if (val instanceof MapElementNode)
			{
				return (MapElementNode) val;
			}
		}

		return null;
	}

	private int getRowForNode(final MapElementNode node)
	{
		final DefaultTreeModel model = (DefaultTreeModel) m_tree.getModel();
		final TreePath path = new TreePath(model.getPathToRoot(node));
		return m_tree.getRowForPath(path);
	}

	/**
	 * Get the current tree model
	 * @return
	 */
	private GameTableMapTreeModel getTreeModel()
	{
		return (GameTableMapTreeModel)m_tree.getModel();
	}
	
	/**
	 * Map Element grabbed with the mouse
	 * @param p
	 * @param pos
	 * @param offset
	 */
	private void onMapElementGrabbed(final MapElementNode p, final Point pos, final Point offset)
	{
		m_grabbedNode = p;
		m_grabOffset = offset;
		m_grabPosition = pos;
	}

	/**
	 * Map element is moved with the mouse
	 * @param pos
	 */
	private void onMapElementMoved(final Point pos)
	{
		if (m_grabbedNode != null)
		{
			m_grabPosition = pos;
			repaint();
		}
	}
	
	/**
	 * Map element is released from mouse
	 */
	private void onMapElementReleased()
	{
		if (m_grabbedNode != null)
		{
			final Point treePos = UtilityFunctions.getComponentCoordinates(m_tree, m_grabPosition);
			final MapElementNode node = getClosestPogNode(treePos.x, treePos.y);
			if (node != null)
			{
				boolean after = false;
				final int row = getRowForNode(node);
				if (row > -1)
				{
					final Rectangle bounds = m_tree.getRowBounds(row);
					if (treePos.y > bounds.y + bounds.height)
					{
						after = true;
					}
				}

				final MapElement sourceElement = m_grabbedNode.getMapElement();
				MapElement targetElement = node.getMapElement();
				if (!sourceElement.equals(targetElement))
				{
					GameTableMapTreeModel model = getTreeModel();
					model.refreshSortIDs();

					final int sourceIndex = model.indexOf(sourceElement);
					int targetIndex = model.indexOf(targetElement);
					
					final Map<MapElementID, Long> changes = new HashMap<MapElementID, Long>();
					if (sourceIndex < targetIndex)
					{
						// Moving a pog down in the list
						if (!after)
						{
							--targetIndex;
							targetElement = model.get(targetIndex);
						}
						
						changes.put(sourceElement.getID(), m_elementSortIDs.get(targetElement.getID()));
						for (int i = sourceIndex + 1; i <= targetIndex; ++i)
						{
							final MapElement a = model.get(i);
							final MapElement b = model.get(i - 1);

							changes.put(a.getID(), m_elementSortIDs.get(b.getID()));
						}
					}
					else
					{
						// Moving a pog up in the list
						changes.put(sourceElement.getID(), m_elementSortIDs.get(targetElement.getID()));
						for (int i = targetIndex; i < sourceIndex; ++i)
						{
							final MapElement a = model.get(i);
							final MapElement b = model.get(i + 1);

							changes.put(a.getID(), m_elementSortIDs.get(b.getID()));
						}
					}

					reorderElements(changes, true);
				}
			}

			m_grabbedNode = null;
			m_grabPosition = null;
			m_grabOffset = null;
			repaint();
		}
	}
	
	/**
	 * Reorder the elements in the list
	 * @param newElementOrder
	 */
	private void reorderElements(final Map<MapElementID, Long> newElementOrder)
	{
		m_elementSortIDs.putAll(newElementOrder);
		getTreeModel().reorderElements();
	}
	
	/**
	 * @return Basic font to use
	 */
	private static Font getBaseFont()
	{
		if (m_font == null)
			m_font = UIManager.getFont("Label.font");
		
		return m_font;
	}
	
	/**
	 * @return Font to use for key names
	 */
	private static Font getKeyNameFont()
	{
		if (m_fontKeyName == null)
			m_fontKeyName = getBaseFont().deriveFont(Font.BOLD);
		
		return m_fontKeyName;
	}
	
	/**
	 * Base font to use
	 */
	private static Font m_font = null;
	
	/**
	 * Font to use for key names
	 */
	private static Font m_fontKeyName = null;
	
}