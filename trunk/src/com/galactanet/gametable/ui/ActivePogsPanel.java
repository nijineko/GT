/*
 * ActivePogsPanel.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.MapElementType.Layer;
import com.galactanet.gametable.data.net.PacketManager;
import com.galactanet.gametable.module.ModuleIF;
import com.galactanet.gametable.module.ModuleSaveIF;
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
public class ActivePogsPanel extends JPanel implements ModuleIF, ModuleSaveIF
{
    // --- Constants -------------------------------------------------------------------------------------------------

    /**
     * Cell renderer for the tree.
     * 
     * @author Iffy
     */
    private static class ActivePogTreeCellRenderer extends JComponent implements TreeCellRenderer
    {
        /**
         * 
         */
        private static final long serialVersionUID = 2211176162170052851L;
        String                    attribute        = null;
        //boolean                   expanded         = false;
        //boolean                   leaf             = false;
        MapElement                       pog              = null;

        public ActivePogTreeCellRenderer()
        {
        }

        private String getLabel()
        {
            if (attribute != null)
            {
                return attribute + ": ";
            }
            return pog.getName();
        }

        /**
         * @return The computed dimensions for this PogComponent, based on the pog and label.
         */
        private Dimension getMySize()
        {
            if (pog == null)
            {
                return new Dimension(1, 1);
            }

            if (attribute == null)
            {
                int w = g_iconSize;
                int h = g_iconSize;
                final String label = getLabel();
                if ((label != null) && (label.length() > 0))
                {
                    final FontRenderContext frc = new FontRenderContext(null, true, false);
                    final Rectangle stringBounds = FONT_NODE.getStringBounds(label, frc).getBounds();
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
            final Rectangle keyBounds = FONT_KEY.getStringBounds(getLabel(), frc).getBounds();
            final Rectangle valueBounds = FONT_VALUE.getStringBounds(getValue(), frc).getBounds();
            h = Math.max(keyBounds.height, valueBounds.height);
            w = keyBounds.width + valueBounds.width;

            return new Dimension(w + TOTAL_SPACE, h + TOTAL_SPACE);
        }

        /*
         * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object,
         *      boolean, boolean, boolean, int, boolean)
         */
        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
            final boolean exp, final boolean lf, final int r, final boolean focus)
        {
            pog = null;
            attribute = null;
            if (value instanceof PogNode)
            {
                final PogNode node = (PogNode)value;
                pog = node.getPog();
            }
            else if (value instanceof AttributeNode)
            {
                final AttributeNode node = (AttributeNode)value;
                pog = node.getPog();
                attribute = node.getAttribute();
            }
            else
            {
                return this;
            }
            
            // expanded = exp;
            // leaf = lf;

            final Dimension size = getMySize();
            setSize(size);
            setPreferredSize(size);

            return this;
        }

        private String getValue()
        {
            final String value = pog.getAttribute(attribute);
            if (value == null)
            {
                return "";
            }

            return value;
        }

        /*
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        protected void paintComponent(final Graphics g)
        {
            if (pog == null)
            {
                return;
            }

            final MapElementType pogType = pog.getMapElementType();
            final Graphics2D g2 = (Graphics2D)g;
            g2.addRenderingHints(Images.getRenderingHints());
            g2.setColor(Color.BLACK);
            if (attribute == null)
            {
            	Image icon = pogType.getListIcon();
            	if (icon != null)
            	{
	            	g2.drawImage(icon,
	            				SPACE + (g_iconSize - icon.getWidth(null)) / 2, 
	            				SPACE + (g_iconSize - icon.getHeight(null)) / 2,
	            				this);
            	}

                final String label = getLabel();
                if ((label != null) && (label.length() > 0))
                {
                    g2.setFont(FONT_NODE);
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
                final Rectangle keyBounds = g2.getFontMetrics(FONT_KEY).getStringBounds(label, g2).getBounds();
                final Rectangle valueBounds = g2.getFontMetrics(FONT_VALUE).getStringBounds(value, g2).getBounds();
                final int drawX = SPACE;
                final int drawY = SPACE + Math.max(Math.abs(keyBounds.y), Math.abs(valueBounds.y));
                g2.setFont(FONT_KEY);
                g2.drawString(label, drawX, drawY);
                g2.setFont(FONT_VALUE);
                g2.drawString(value, drawX + keyBounds.width, drawY);
            }
            g2.dispose();
        }
    }

    /**
     * A Leaf TreeNode representing a Pog's attribute.
     * 
     * @author Iffy
     */
    private static class AttributeNode extends DefaultMutableTreeNode
    {
        /**
         * 
         */
        private static final long serialVersionUID = -7669642437687369529L;

        public AttributeNode(final String att)
        {
            super(att, false);
        }

        /*
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object o)
        {
            if (o == this)
            {
                return true;
            }

            final AttributeNode node = (AttributeNode)o;
            return (node.getPog().equals(getPog()) && node.getAttribute().equals(getAttribute()));
        }

        /**
         * @return Returns the attribute for this node.
         */
        public String getAttribute()
        {
            return (String)getUserObject();
        }

        // --- Object Implementation ---

        /**
         * @return Returns the pog for this node.
         */
        public MapElement getPog()
        {
            return getPogNodeParent().getPog();
        }

        private PogNode getPogNodeParent()
        {
            return (PogNode)getParent();
        }

        // --- Private Methods ---

        /*
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            return getPog().hashCode() ^ getAttribute().hashCode();
        }
    }

    /**
     * Class to track the status of branches in the pog tree.
     * 
     * @author Iffy
     */
    private class BranchTracker implements TreeExpansionListener
    {
        private boolean   allExpanded    = false;
        private final Set<MapElement> collapsedNodes = new HashSet<MapElement>();
        private final Set<MapElement> expandedNodes  = new HashSet<MapElement>();

        public BranchTracker()
        {
        }

        public void collapseAll(final JTree tree)
        {
            final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            collapseAll(tree, (TreeNode)model.getRoot());
            allExpanded = false;
        }

        private void collapseAll(final JTree tree, final TreeNode node)
        {
            if (node.isLeaf() || !node.getAllowsChildren() || (node.getChildCount() == 0))
            {
                return;
            }

            final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            tree.collapsePath(new TreePath(model.getPathToRoot(node)));
            for (int i = 0, size = node.getChildCount(); i < size; ++i)
            {
                collapseAll(tree, node.getChildAt(i));
            }
        }

        public void expandAll(final JTree tree)
        {
            final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            expandAll(tree, (TreeNode)model.getRoot());
            allExpanded = true;
        }

        private void expandAll(final JTree tree, final TreeNode node)
        {
            if (node.isLeaf() || !node.getAllowsChildren() || (node.getChildCount() == 0))
            {
                return;
            }

            final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            tree.expandPath(new TreePath(model.getPathToRoot(node)));
            for (int i = 0, size = node.getChildCount(); i < size; ++i)
            {
                expandAll(tree, node.getChildAt(i));
            }
        }

        /*
        public void reset()
        {
            expandedNodes.clear();
            collapsedNodes.clear();
            allExpanded = false;
        }
        */

        public void restoreTree(final JTree tree)
        {
            if (allExpanded)
            {
                expandAll(tree);
                return;
            }

            final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            final RootNode root = (RootNode)model.getRoot();

            tree.removeTreeExpansionListener(this);
            try
            {
                for (MapElement pog : expandedNodes)
                {
                    final PogNode node = root.findNodeFor(pog);
                    if (node != null)
                    {
                        final TreePath path = new TreePath(model.getPathToRoot(node));
                        tree.expandPath(path);
                    }
                    else
                    {
                        expandedNodes.remove(pog);
                    }
                }

                for (MapElement pog : collapsedNodes)
                {
                    final PogNode node = root.findNodeFor(pog);
                    if (node != null)
                    {
                        final TreePath path = new TreePath(model.getPathToRoot(node));
                        tree.collapseRow(tree.getRowForPath(path));
                    }
                    else
                    {
                        collapsedNodes.remove(pog);
                    }
                }
            }
            finally
            {
                tree.addTreeExpansionListener(this);
            }
        }

        // --- TreeExpansionListener Implementation ---

        /*
         * @see javax.swing.event.TreeExpansionListener#treeCollapsed(javax.swing.event.TreeExpansionEvent)
         */
        public void treeCollapsed(final TreeExpansionEvent event)
        {
            try
            {
                final PogNode node = (PogNode)event.getPath().getLastPathComponent();
                expandedNodes.remove(node.getPog());
                collapsedNodes.add(node.getPog());
                allExpanded = false;
            }
            catch (final ClassCastException cce)
            {
                // ignore non-pog nodes
            }
        }

        /*
         * @see javax.swing.event.TreeExpansionListener#treeExpanded(javax.swing.event.TreeExpansionEvent)
         */
        public void treeExpanded(final TreeExpansionEvent event)
        {
            try
            {
                final PogNode node = (PogNode)event.getPath().getLastPathComponent();
                expandedNodes.add(node.getPog());
                collapsedNodes.remove(node.getPog());
            }
            catch (final ClassCastException cce)
            {
                // ignore non-pog nodes
            }
        }
    }
    /**
     * A TreeNode representing a library.
     * 
     * @author Iffy
     */
    private static class PogNode extends DefaultMutableTreeNode
    {
        /**
         * 
         */
        private static final long serialVersionUID = -5086776295684411196L;

        public PogNode(final MapElement pog)
        {
            super(pog, true);
            for (String name : getPog().getAttributeNames())
            {
                add(new AttributeNode(name));
            }
        }

        /*
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object o)
        {
            if (o == this)
            {
                return true;
            }

            final PogNode node = (PogNode)o;
            return (node.getPog().equals(getPog()));
        }

        // --- Object Implementation ---

        /**
         * @return Returns the pog for this node.
         */
        public MapElement getPog()
        {
            return (MapElement)getUserObject();
        }

        /*
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            return getPog().hashCode();
        }
    }
    /**
     * Root node class for the tree.
     * 
     * @author iffy
     */
    private class RootNode extends DefaultMutableTreeNode
    {
        /**
         * 
         */
        private static final long serialVersionUID = -2217746931413629754L;

        private RootNode(final GameTableMap map)
        {
            super(map, true);
            for (MapElement pog : m_orderedPogs)
            {                
                add(new PogNode(pog));
            }
        }

        public PogNode findNodeFor(final MapElement pog)
        {
            for (int i = 0, size = getChildCount(); i < size; ++i)
            {
                final PogNode node = (PogNode)getChildAt(i);
                if (pog.equals(node.getPog()))
                {
                    return node;
                }
            }

            return null;
        }

        public GameTableMap getMap()
        {
            return (GameTableMap)getUserObject();
        }
    }

    private static final Color              BACKGROUND_COLOR    = Color.WHITE;

    private static final float              CLICK_THRESHHOLD    = 2f;
    private static final Font               FONT_NODE           = Font.decode("sansserif-12");
    private static final Font               FONT_VALUE          = FONT_NODE;
    private static final Font               FONT_KEY            = FONT_VALUE.deriveFont(Font.BOLD);

    private static final int                POG_BORDER          = 0;
    private static final int                POG_MARGIN          = 0;

    // --- Types -----------------------------------------------------------------------------------------------------

    private static final int                POG_PADDING         = 1;

    private static final int                POG_TEXT_PADDING    = 4;

    /**
     * 
     */
    private static final long               serialVersionUID    = -5840985576215910472L;

    private static final int                SPACE               = POG_PADDING + POG_BORDER + POG_MARGIN;

    private static final int                TOTAL_SPACE         = SPACE * 2;

    // --- Members ---------------------------------------------------------------------------------------------------

    /**
     * The currently grabbed pog.
     */
    private PogNode                         m_grabbedNode       = null;

    /**
     * The offset at which the pog was grabbed.
     */
    private Point                           m_grabOffset        = null;

    /**
     * The position of the currently grabbed pog.
     */
    private Point                           m_grabPosition      = null;

    // --- Pog Dragging Members ---

    private Point                           m_lastPressPosition = null;

    private int                             m_numClicks         = 0;

    private final ActivePogTreeCellRenderer pogRenderer         = new ActivePogTreeCellRenderer();

    /**
     * The main component for this damn thing.
     */
    private JTree                           pogTree;

    /**
     * The scroll pane for the tree.
     */
    private JScrollPane                     scrollPane;
    /**
     * A map of GametableMaps to BranchTrackers for thier pog lists.
     */
    private final Map<GameTableMap, BranchTracker>                       trackers            = new HashMap<GameTableMap, BranchTracker>();
    
    private static int g_iconSize;

    // --- Constructors ----------------------------------------------------------------------------------------------

    /**
     * Constructor
     */
    public ActivePogsPanel()
    {   	
        super(new BorderLayout());
        
        GametableFrame.registerModule(this);
        
      	m_orderedPogs = new TreeSet<MapElement>(new SortOrderComparator());
      	
      	g_iconSize = GametableApp.getIntegerProperty(GametableApp.PROPERTY_ICON_SIZE);        
        add(getScrollPane(), BorderLayout.CENTER);
        add(getToolbar(), BorderLayout.NORTH);

        
        GametableCanvas canvas = GametableFrame.getGametableFrame().getGametableCanvas();     
        GameTableMapListenerIF mapListener = createGameTableMapListener();
        
        // -------------------
        GameTableMap map = canvas.getPublicMap();

        for (MapElement mapElement : map.getMapElements())
        	addOrderedPog(mapElement);
        	
        map.addListener(mapListener);
        
        // -------------------
        map = canvas.getPrivateMap();
        
        for (MapElement mapElement : map.getMapElements())
        	addOrderedPog(mapElement);
        	
        map.addListener(mapListener);
    }

    /**
     * Rebuild the ordered pog list
     */
    public void resetOrderPogList()
    {
      GametableCanvas canvas = GametableFrame.getGametableFrame().getGametableCanvas();     

    	m_orderedPogs.clear();
    	
      // -------------------
      GameTableMap map = canvas.getPublicMap();

      for (MapElement mapElement : map.getMapElements())
      	addOrderedPog(mapElement);
      
      // -------------------
      map = canvas.getPrivateMap();
      
      for (MapElement mapElement : map.getMapElements())
      	addOrderedPog(mapElement);
    	
    }
    

    public void reorderPogs(final Map<MapElementID, Long> changes, boolean notifyNetwork)
    {
    	GametableFrame frame = GametableFrame.getGametableFrame();
    	
        if (notifyNetwork && frame.getGametableCanvas().isPublicMap())
        {
            frame.send(PacketManager.makePogReorderPacket(changes));
            if (frame.getNetStatus() != GametableFrame.NETSTATE_JOINED)
            {
                doPogReorder(changes);
            }
        }
        else
        {
            doPogReorder(changes);
        }
    }
    
    private void doPogReorder(final Map<MapElementID, Long> changes)
    {
    	GametableFrame frame = GametableFrame.getGametableFrame();
    	
    	GameTableMap map = frame.getGametableCanvas().getActiveMap();
    		
    		for (Entry<MapElementID, Long> entry : changes.entrySet())
    		{
    			setSortOrder(entry.getKey(), entry.getValue(), map);
    		}
    	
        frame.refreshActivePogList();
    }
    
    

		private GameTableMapListenerIF createGameTableMapListener()
		{
			GameTableMapListenerIF mapListener = new GameTableMapAdapter() {

				/*
				 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstanceAdded(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElementInstance)
				 */
				@Override
				public void onMapElementInstanceAdded(GameTableMap map, MapElement mapElement)
				{
					addOrderedPog(mapElement);
				}
				
				/*
				 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstanceRemoved(com.galactanet.gametable.data.GameTableMap, com.galactanet.gametable.data.MapElementInstance)
				 */
				@Override
				public void onMapElementInstanceRemoved(GameTableMap map, MapElement mapElement)
				{
					removeOrderedPog(mapElement);
				}
				
				/*
				 * @see com.galactanet.gametable.data.GameTableMapAdapter#onMapElementInstancesCleared(com.galactanet.gametable.data.GameTableMap)
				 */
				@Override
				public void onMapElementInstancesCleared(GameTableMap map)
				{
					m_orderedPogs.clear();
				}
			};
			return mapListener;
		}

    // --- Methods ---------------------------------------------------------------------------------------------------

    private PogNode getClosestPogNode(final int x, final int y)
    {
        final TreePath path = pogTree.getClosestPathForLocation(x, y);
        if (path == null)
        {
            return null;
        }

        for (int i = path.getPathCount(); i-- > 0;)
        {
            final Object val = path.getPathComponent(i);
            if (val instanceof PogNode)
            {
                return (PogNode)val;
            }
        }

        return null;
    }

    // --- Accessor methods ---

    private GameTableMap getMap()
    {
        final DefaultTreeModel model = (DefaultTreeModel)getPogTree().getModel();
        final RootNode root = (RootNode)model.getRoot();

        return root.getMap();
    }

    private PogNode getNextPogNode(final PogNode node)
    {
        int row = getRowForNode(node);
        while (true)
        {
            ++row;
            final TreePath path = pogTree.getPathForRow(row);
            if (path == null)
            {
                return null;
            }

            final Object val = path.getLastPathComponent();
            if (val instanceof PogNode)
            {
                return (PogNode)val;
            }
        }
    }

    private PogNode getPogNode(final int x, final int y)
    {
        final TreePath path = pogTree.getPathForLocation(x, y);
        if (path == null)
        {
            return null;
        }

        for (int i = path.getPathCount(); i-- > 0;)
        {
            final Object val = path.getPathComponent(i);
            if (val instanceof PogNode)
            {
                return (PogNode)val;
            }
        }

        return null;
    }

    private JTree getPogTree()
    {
        if (pogTree == null)
        {
            pogTree = new JTree();
            pogTree.setBackground(BACKGROUND_COLOR);
            pogTree.setRootVisible(false);
            pogTree.setShowsRootHandles(true);
            pogTree.setToggleClickCount(3);
            pogTree.setSelectionModel(null);
            pogTree.setCellRenderer(pogRenderer);
            pogTree.setRowHeight(0);
            pogTree.setFocusable(false);
            pogTree.addMouseListener(new MouseAdapter()
            {
                /*
                 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
                 */
                public void mouseExited(final MouseEvent e)
                {
                }

                /*
                 * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
                 */
                public void mousePressed(final MouseEvent e)
                {
                    m_lastPressPosition = new Point(e.getX(), e.getY());
                    final TreePath path = pogTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null)
                    {
                        return;
                    }

                    final Object val = path.getLastPathComponent();
                    if (val instanceof PogNode)
                    {
                        final PogNode node = (PogNode)val;
                        final Point screenCoords = UtilityFunctions.getScreenCoordinates(pogTree, m_lastPressPosition);
                        
                        Image icon = node.getPog().getMapElementType().getListIcon();
                        
                        final Point localCoords = icon == null ? new Point(0, 0) : new Point(icon.getWidth(null) / 2, icon.getHeight(null) / 2);
                        grabPog(node, screenCoords, localCoords);
                    }
                }

                /*
                 * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
                 */
                public void mouseReleased(final MouseEvent e)
                {
                    releasePog();
                    final Point p = new Point(e.getX(), e.getY());

                    if (p.distance(m_lastPressPosition) <= CLICK_THRESHHOLD)
                    {
                        m_numClicks++;
                    }

                    if (m_numClicks == 2)
                    {
                        final PogNode node = getPogNode(e.getX(), e.getY());
                        if (node != null)
                        {
                            GametableFrame.getGametableFrame().getGametableCanvas().scrollToPog(node.getPog());
                        }
                    }
                }

            });

            pogTree.addMouseMotionListener(new MouseMotionAdapter()
            {
                /*
                 * @see java.awt.event.MouseMotionAdapter#mouseDragged(java.awt.event.MouseEvent)
                 */
                public void mouseDragged(final MouseEvent e)
                {
                    mouseMoved(e);
                }

                /*
                 * @see java.awt.event.MouseMotionAdapter#mouseMoved(java.awt.event.MouseEvent)
                 */
                public void mouseMoved(final MouseEvent e)
                {
                    final Point screenCoords = UtilityFunctions.getScreenCoordinates(pogTree, new Point(e.getX(), e
                        .getY()));
                    moveGrabPosition(screenCoords);
                    m_numClicks = 0;
                }
            });
            refresh();
        }
        return pogTree;
    }

    // --- Dragging methods ---

    private int getRowForNode(final PogNode node)
    {
        final DefaultTreeModel model = (DefaultTreeModel)pogTree.getModel();
        final TreePath path = new TreePath(model.getPathToRoot(node));
        return pogTree.getRowForPath(path);
    }

    private JScrollPane getScrollPane()
    {
        if (scrollPane == null)
        {
            scrollPane = new JScrollPane(getPogTree());
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        return scrollPane;
    }

    private JToolBar getToolbar()
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
        collapseButton.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                getTracker().collapseAll(getPogTree());
            }
        });
        toolbar.add(collapseButton);

        final Image expandImage = ImageCache.getImage(new File("assets/expand.png"));
        final JButton expandButton = new JButton("Expand All", new ImageIcon(expandImage));
        expandButton.setMargin(margin);
        expandButton.setFocusable(false);
        expandButton.addActionListener(new ActionListener()
        {
            /*
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(final ActionEvent e)
            {
                getTracker().expandAll(getPogTree());
            }
        });
        toolbar.add(expandButton);

        return toolbar;
    }

    private BranchTracker getTracker()
    {
        return getTrackerFor(getMap());
    }

    private BranchTracker getTrackerFor(final GameTableMap map)
    {
        BranchTracker tracker = trackers.get(map);
        if (tracker == null)
        {
            tracker = new BranchTracker();
            trackers.put(map, tracker);
        }

        return tracker;
    }

    private void grabPog(final PogNode p, final Point pos, final Point offset)
    {
        m_grabbedNode = p;
        m_grabOffset = offset;
        m_grabPosition = pos;
    }

    private void moveGrabPosition(final Point pos)
    {
        if (m_grabbedNode != null)
        {
            m_grabPosition = pos;
            repaint();
        }
    }

    // --- Component Implementation ---

    /*
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    public void paint(final Graphics g)
    {
        super.paint(g);
        try
        {
            if (m_grabbedNode != null)
            {
                final Graphics2D g2 = (Graphics2D)g;
                final Point treePos = UtilityFunctions.getComponentCoordinates(getPogTree(), m_grabPosition);
                final Point localPos = UtilityFunctions.getComponentCoordinates(this, m_grabPosition);

                PogNode node = getClosestPogNode(treePos.x, treePos.y);
                if (node != null)
                {
                    int row = getRowForNode(node);
                    if (row > -1)
                    {
                        Rectangle bounds = pogTree.getRowBounds(row);
                        Point thisPos = UtilityFunctions.convertCoordinates(getPogTree(), this, new Point(bounds.x,
                            bounds.y));
                        int drawY = thisPos.y;

                        // go to next node if in attributes area
                        if (localPos.y > thisPos.y + bounds.height)
                        {
                            final PogNode nextNode = getNextPogNode(node);
                            if (nextNode == null)
                            {
                                drawY += bounds.height;
                            }
                            else
                            {
                                node = nextNode;
                                row = getRowForNode(node);
                                bounds = pogTree.getRowBounds(row);
                                thisPos = UtilityFunctions.convertCoordinates(getPogTree(), this, new Point(bounds.x,
                                    bounds.y));
                                drawY = thisPos.y;
                            }
                        }

                        final int PADDING = 5;
                        final int drawX = PADDING;
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawLine(drawX, drawY, drawX + pogTree.getWidth() - (PADDING * 2), drawY);
                    }
                }

                g2.translate(localPos.x - m_grabOffset.x, localPos.y - m_grabOffset.y);
                final JComponent comp = (JComponent)pogRenderer.getTreeCellRendererComponent(pogTree, m_grabbedNode,
                    false, false, true, 0, false);
                comp.paint(g2);
                g2.dispose();
            }
        }
        catch (final Throwable t)
        {
            Log.log(Log.SYS, t);
        }
    }

    // --- Initialization methods ---

    public void refresh()
    {
        removeTrackers();
        final GameTableMap map = GametableFrame.getGametableFrame().getGametableCanvas().getActiveMap();
        pogTree.setModel(new DefaultTreeModel(new RootNode(map)));
        final BranchTracker tracker = getTrackerFor(map);
        pogTree.addTreeExpansionListener(tracker);
        tracker.restoreTree(pogTree);
    }

    private void releasePog()
    {
        if (m_grabbedNode != null)
        {
            final Point treePos = UtilityFunctions.getComponentCoordinates(getPogTree(), m_grabPosition);
            final PogNode node = getClosestPogNode(treePos.x, treePos.y);
            if (node != null)
            {
                boolean after = false;
                final int row = getRowForNode(node);
                if (row > -1)
                {
                    final Rectangle bounds = pogTree.getRowBounds(row);
                    if (treePos.y > bounds.y + bounds.height)
                    {
                        after = true;
                    }
                }

                final MapElement sourcePog = m_grabbedNode.getPog();
                MapElement targetPog = node.getPog();
                if (!sourcePog.equals(targetPog))
                {
                    final List<MapElement> pogs = new ArrayList<MapElement>(m_orderedPogs);
                    
                    final int sourceIndex = pogs.indexOf(sourcePog);
                    int targetIndex = pogs.indexOf(targetPog);
                    final Map<MapElementID, Long> changes = new HashMap<MapElementID, Long>();
                    if (sourceIndex < targetIndex)
                    {
                        // Moving a pog down in the list
                        if (!after)
                        {
                            --targetIndex;
                            targetPog = pogs.get(targetIndex);
                        }
                        changes.put(sourcePog.getId(), getSortId(targetPog));
                        for (int i = sourceIndex + 1; i <= targetIndex; ++i)
                        {
                            final MapElement a = pogs.get(i);
                            final MapElement b = pogs.get(i - 1);

                            changes.put(a.getId(), getSortId(b));
                        }
                    }
                    else
                    {
                        // Moving a pog up in the list
                        changes.put(sourcePog.getId(), getSortId(targetPog));
                        for (int i = targetIndex; i < sourceIndex; ++i)
                        {
                            final MapElement a = pogs.get(i);
                            final MapElement b = pogs.get(i + 1);

                            changes.put(a.getId(),getSortId(b));
                        }
                    }
                    
                   reorderPogs(changes, true);
                }
            }

            m_grabbedNode = null;
            m_grabPosition = null;
            m_grabOffset = null;
            repaint();
        }
    }

    private void removeTrackers()
    {
        for(TreeExpansionListener listener : trackers.values())
        {
            pogTree.removeTreeExpansionListener(listener);
        }
    }
    
  	/**
  	 * Adds a pog to the ordered list used by the ActivePogs panel
  	 * 
  	 * @param pog
  	 */
  	public void addOrderedPog(final MapElement pog)
  	{
			if (pog.getLayer() != Layer.POG)
				return;

  		m_orderedPogs.add(pog);
  		m_elementSortIDs.put(pog.getId(), g_nextSortID++);
  	}
  	
  	/**
  	 * Remove ordered pog
  	 * 
  	 * @param pog
  	 */
  	public void removeOrderedPog(final MapElement pog)
  	{
  		m_orderedPogs.remove(pog);
  		m_elementSortIDs.remove(pog.getId());
  	}
  	
  	/**
  	 * Set the sort order for a given pog
  	 * 
  	 * @param pogID pog ID to look for
  	 * @param sortOrder sort order number
  	 */
  	public void setSortOrder(MapElementID pogID, long sortOrder, GameTableMap map)
  	{
  		final MapElement pog = map.getMapElement(pogID);
  		if (pog == null)
  			return;
  		
  		if (g_nextSortID <= sortOrder)
  			g_nextSortID = sortOrder + 1;
  		
  		m_elementSortIDs.put(pogID, sortOrder);

  		// Remove item and put it back in list to sort it according to the new set sort order 
  		m_orderedPogs.remove(pog);
  		m_orderedPogs.add(pog);
  	}
  	
  	/**
  	 * Get the sort ID linked to specified map element
  	 * @param mapElement mapElement
  	 * @return Sort ID
  	 */
  	public long getSortId(MapElement mapElement)
  	{
  		return m_elementSortIDs.get(mapElement.getId());
  	}

  	/**
  	 * List of pogs shown in the active pog panel
  	 */
  	private final TreeSet<MapElement> m_orderedPogs;
  	private final Map<MapElementID, Long> m_elementSortIDs = new HashMap<MapElementID, Long>();
  	private long g_nextSortID = 1;
  	
  	private class SortOrderComparator implements Comparator<MapElement>
  	{
  		/*
  		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
  		 */
  		@Override
  		public int compare(MapElement o1, MapElement o2)
  		{
  			int res = compareSortOrder(o1, o2);
  			if (res != 0)
  				return res;
  			
  			return o1.compareTo(o2);
  		}
  		
  		/**
  		 * Compare only the sort orders
  		 * @param o1
  		 * @param o2
  		 * @return
  		 */
  		public int compareSortOrder(MapElement o1, MapElement o2)
  		{
  			Long i1 = m_elementSortIDs.get(o1.getId());
  			Long i2 = m_elementSortIDs.get(o2.getId());
  			
  			if (i1 == i2)
  				return 0;
  			
  			if (i1 == null)
  				return -1;
  			
  			if (i2 == null)
  				return 1;

  			if (i1 > i2)
  				return 1;

  			return -1;
  		}
  	}
  	
  	/*
  	* @see java.awt.Component#getName()
  	*/
  	@Override
  	public String getModuleName()
  	{
  		return ActivePogsPanel.class.getName();
  	}
  	
  	/*
  	* @see com.galactanet.gametable.module.ModuleSaveIF#loadFromXML(org.w3c.dom.Element)
  	*/
  	@Override
  	public void loadFromXML(Element node)
  	{
  		m_elementSortIDs.clear();
  		m_orderedPogs.clear();
  		g_nextSortID = 0;
  		
  		Element sortEl = XMLUtils.getFirstChildElementByTagName(node, "sort");
  		if (sortEl == null)
  			return;
  		
  		GametableFrame frame = GametableFrame.getGametableFrame();
  		
  		for (Element itemEl : XMLUtils.getChildElementsByTagName(sortEl, "item"))
  		{
  			MapElementID elementID = MapElementID.fromNumeric(UtilityFunctions.parseLong(itemEl.getAttribute("id"), 0));
  			
  			MapElement mapEl = frame.getMapElement(elementID);
  			if (mapEl != null)
  			{
    			long val = UtilityFunctions.parseLong(itemEl.getAttribute("val"), 0);
  				g_nextSortID = Math.max(g_nextSortID, val + 1);
  				
  				m_elementSortIDs.put(elementID, val);
  				m_orderedPogs.add(mapEl);
  			}
  		}
  	}
  	
  	/*
  	* @see com.galactanet.gametable.module.ModuleSaveIF#saveToXML(org.w3c.dom.Element)
  	*/
  	@Override
  	public boolean saveToXML(Element node)
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
}
