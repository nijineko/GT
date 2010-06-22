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

import com.galactanet.gametable.data.*;
import com.galactanet.gametable.data.MapElement.Layer;
import com.galactanet.gametable.data.prefs.PreferenceDescriptor;
import com.galactanet.gametable.ui.GametableCanvas;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.SetPogAttributeDialog;
import com.galactanet.gametable.util.UtilityFunctions;



/**
 * The basic pog interaction tool.
 * 
 * @author iffy
 * 
 * #GT-AUDIT PointerTool
 */
public class PointerTool extends NullTool
{
    GridMode                   m_gridMode;
    
    private class DeletePogAttributeActionListener implements ActionListener
    {
        private final String key;

        DeletePogAttributeActionListener(final String name)
        {
            key = name;
        }

        public void actionPerformed(final ActionEvent e)
        {
            final Set<String> toDelete = new HashSet<String>();
            toDelete.add(key);
            m_canvas.setPogData(m_menuPog.getId(), null, null, toDelete);
        }
    }

    private class EditPogAttributeActionListener implements ActionListener
    {
        private final String key;

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
            final Set<String> toDelete = new HashSet<String>();
            toDelete.add(key);
            
            final Map<String, String> toAdd = new HashMap<String, String>();
            if ((name != null) && (name.length() > 0))
            {
                toAdd.put(name, value);
            }
            
            m_canvas.setPogData(m_menuPog.getId(), null, toAdd, toDelete);
        }
    }

    private static final String PREF_DRAG   = "com.galactanet.gametable.tools.PointerTool.drag";

    private static final List<PreferenceDescriptor>   PREFERENCES = createPreferenceList();

    /**
     * @return The static, unmodifiable list of preferences for this tool.
     */
    private static final List<PreferenceDescriptor> createPreferenceList()
    {
        final List<PreferenceDescriptor> retVal = new ArrayList<PreferenceDescriptor>();
        retVal.add(new PreferenceDescriptor(
            PREF_DRAG, 
            "Drag map when not over Pog", 
            PreferenceDescriptor.TYPE_FLAG,
            "true"));
        return Collections.unmodifiableList(retVal);
    }

    private GametableCanvas m_canvas;
    private GameTableMap    m_from;
    private GameTableMap    m_to;
    private boolean         m_clicked = true;
    private MapElementInstance             m_ghostPog;
    private MapElementInstance             m_grabbedPog;
    private Point           m_grabOffset;
    private MapElementInstance             m_lastPogMousedOver;
    private MapElementInstance             m_menuPog = null;
    private MapCoordinates           m_mousePosition;
    private boolean         m_snapping;

    private Point           m_startMouse;

    private MapCoordinates           m_startScroll;

    /**
     * Constructor
     */
    public PointerTool()
    {
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#activate(com.galactanet.gametable.GametableCanvas)
     */
    @Override
		public void activate(final GametableCanvas canvas)
    {
        m_canvas = canvas;
        m_grabbedPog = null;
        m_ghostPog = null;
        m_grabOffset = null;
        m_mousePosition = null;
        m_startScroll = null;
        m_startMouse = null;
    }

    @Override
		public void endAction()
    {
        m_grabbedPog = null;
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
		public List<PreferenceDescriptor> getPreferences()
    {
        return PREFERENCES;
    }

    private void hoverCursorCheck()
    {
        if (GametableFrame.getGametableFrame().getPreferences().getBooleanValue(PREF_DRAG))
        {
            final MapElementInstance pog = m_canvas.getActiveMap().getMapElementInstanceAt(m_mousePosition);
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
        return (m_grabbedPog != null) || (m_startScroll != null);
    }

    /*
     * @see com.galactanet.gametable.AbstractTool#mouseButtonPressed(int, int)
     */
    @Override
		public void mouseButtonPressed(MapCoordinates modelPos, final int modifierMask)
    {
        m_clicked = true;
        m_mousePosition = modelPos;
        m_grabbedPog = m_canvas.getActiveMap().getMapElementInstanceAt(m_mousePosition);
        if (m_grabbedPog != null)
        {
            m_ghostPog = new MapElementInstance(m_grabbedPog);
            m_grabOffset = new Point(m_grabbedPog.getPosition().x - m_mousePosition.x, m_grabbedPog.getPosition().y - m_mousePosition.y);
            setSnapping(modifierMask);
        }
        else if (GametableFrame.getGametableFrame().getPreferences().getBooleanValue(PREF_DRAG))
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
        if (m_grabbedPog != null)
        {
            if (m_clicked)
            {
                popupContextMenu(modelPos, modifierMask);
            }
            else
            {
                if (!m_grabbedPog.isLocked())
                {
                    // Dont need this, it is done in move pog, and why move it if we are removing it?
                    //m_grabbedPog.setPosition(m_ghostPog.getPosition());
                    if (!m_canvas.isPointVisible(m_mousePosition))
                    {
                        // they removed this pog
                        m_canvas.removePog(m_grabbedPog.getId());
                    }
                    else
                    {
                        m_canvas.movePog(m_grabbedPog.getId(), m_ghostPog.getPosition());
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
        if ((m_grabbedPog != null) && !m_grabbedPog.isLocked())
        {
            m_clicked = false;
            if (m_snapping)
            {
                final Point adjustment = getSnapDragAdjustment(m_ghostPog);
                m_ghostPog.setPosition(
                		m_mousePosition.delta(m_grabOffset.x + adjustment.x, m_grabOffset.y + adjustment.y));
                		
                m_canvas.snapPogToGrid(m_ghostPog);
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
        else if ((m_grabbedPog != null) && m_grabbedPog.isLocked())
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
    private Point getSnapDragAdjustment(MapElementInstance mapElement)
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
        m_menuPog = m_grabbedPog;
        final JPopupMenu menu = new JPopupMenu("Pog");
        if ((modifierMask & MODIFIER_SHIFT) > 0) // holding shift
        {   
            final int xLocation;
            final int yLocation;
            final int pogSize = m_menuPog.getFaceSize();
            MapCoordinates pogPos = m_menuPog.getPosition();
            final int tempSize = pogSize;
            final int m_gridModeId = m_canvas.getGridModeId();

            if (m_gridModeId == 1) //square mode
            {
                xLocation =  (pogPos.x / 64) + ( ((tempSize % 2 == 0) ? pogSize - 1 : pogSize) / 2);
                yLocation = ((pogPos.y / 64) + ( ((tempSize % 2 == 0) ? pogSize - 1 : pogSize) / 2)) * -1;
            }
            else if (m_gridModeId == 2) //hex mode - needs work to get it to display appropriate numbers
            {
                xLocation = pogPos.x;
                yLocation = pogPos.y * -1;
            }
            else //no grid
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
            JMenuItem item = new JMenuItem(m_menuPog.isLocked() ? "Unlock" : "Lock");
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    m_canvas.lockPog(m_menuPog.getId(), !m_menuPog.isLocked());
                    //System.out.println(m_menuPog.isLocked());
                }
            });
            menu.add(item);
            item = new JMenuItem(m_canvas.isSelected(m_menuPog) ? "Unselect" : "Select");
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {  
                    if (m_canvas.isPublicMap()) m_from = m_canvas.getPublicMap();
                    else m_from = m_canvas.getPrivateMap();                            
                    
                    if(m_canvas.isSelected(m_menuPog)) m_canvas.selectMapElementInstance(m_menuPog, false);
                    else m_canvas.selectMapElementInstance(m_menuPog, true);                                     
                }
            });
            menu.add(item);
            
            if(Group.getGroup(m_menuPog) != null) {
                item = new JMenuItem("UnGroup");
                item.addActionListener(new ActionListener()
                {
                    public void actionPerformed(final ActionEvent e)
                    {
                    	Group group = Group.getGroup(m_menuPog);
                    	if (group != null)
                    		group.removeElement(m_menuPog);
                    }
                });
                menu.add(item);                
            }
            item = new JMenuItem(m_canvas.isPublicMap() ? "Unpublish" : "Publish");
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    final MapElementInstance pog = m_menuPog;
                    if (m_canvas.isPublicMap())
                    {
                        m_from = m_canvas.getPublicMap();
                        m_to = m_canvas.getPrivateMap();
                    }
                    else
                    {
                        m_from = m_canvas.getPrivateMap();
                        m_to = m_canvas.getPublicMap();
                    }

                    // this pog gets copied
                    final MapElementInstance newPog = new MapElementInstance(pog);
                    m_canvas.setActiveMap(m_to);
                    m_canvas.addPog(newPog);
                    m_canvas.lockPog(newPog.getId(), pog.isLocked());
                    m_canvas.setActiveMap(m_from);

                    if ((modifierMask & MODIFIER_CTRL) == 0) // not holding control
                    {
                        // remove the pogs that we moved
                        m_canvas.removePog(pog.getId(), false);
                    }
                }
            });
            menu.add(item);
            item = new JMenuItem("Set Name...");
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    final String s = (String)JOptionPane.showInputDialog(GametableFrame.getGametableFrame(),
                        "Enter new name for this Pog:", "Set Pog Name", JOptionPane.PLAIN_MESSAGE, null, null, m_menuPog.getName());

                    if (s != null)
                    {
                        m_canvas.setPogData(m_menuPog.getId(), s, null, null);
                    }

                }
            });
            menu.add(item);
            item = new JMenuItem("Set Attribute...");
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    final SetPogAttributeDialog dialog = new SetPogAttributeDialog(false);
                    dialog.setLocationRelativeTo(m_canvas);
                    dialog.setVisible(true);
                    
                    if(dialog.isConfirmed()) {
                        final Map<String, String> toAdd = dialog.getAttribs();
                        m_canvas.setPogData(m_menuPog.getId(), null, toAdd, null);
                    }
                }
            });
            menu.add(item);
            if (m_menuPog.getAttributeNames().size() > 0)
            {
                final JMenu editMenu = new JMenu("Edit Attribute");

                final JMenu removeMenu = new JMenu("Remove Attribute");
                final Set<String> nameSet = m_grabbedPog.getAttributeNames();
                
                for(String key : nameSet)
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
                //item.setAccelerator(KeyStroke.getKeyStroke("ctrl shift pressed S"));
                item.addActionListener(new ActionListener()
                {
                    public void actionPerformed(final ActionEvent e)
                    {
                        final MapElementInstance pog = m_menuPog;
                            GametableFrame.getGametableFrame().copyPog(pog);
                      
                    }
                });

          
                menu.add(item);           
            // -------------------------------------
            // Save Pog
           
                item = new JMenuItem("Save Pog...");
                //item.setAccelerator(KeyStroke.getKeyStroke("ctrl shift pressed S"));
                item.addActionListener(new ActionListener()
                {
                    public void actionPerformed(final ActionEvent e)
                    {
                        final MapElementInstance pog = m_menuPog;
                        final File spaf = UtilityFunctions.doFileSaveDialog("Save As", "pog", true);
                        if (spaf != null)
                        {
                            GametableFrame.getGametableFrame().savePog(spaf, pog);
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
                    public void actionPerformed(final ActionEvent e) {
                        m_canvas.setPogLayer(m_menuPog.getId(), Layer.UNDERLAY);
                    }
                });
                
                if (layer == Layer.UNDERLAY) item.setEnabled(false);
                
                m_item.add(item);
                item = new JMenuItem("Overlay");
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        m_canvas.setPogLayer(m_menuPog.getId(), Layer.OVERLAY);
                    }
                });
                if(layer == Layer.OVERLAY) item.setEnabled(false);
                m_item.add(item);
                item = new JMenuItem("Environment");
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        m_canvas.setPogLayer(m_menuPog.getId(), Layer.ENVIRONMENT);
                    }
                });
                if(layer == Layer.ENVIRONMENT) item.setEnabled(false);
                m_item.add(item);
                item = new JMenuItem("Pog");
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        m_canvas.setPogLayer(m_menuPog.getId(), Layer.POG);
                    }
                });
                if(layer == Layer.POG) item.setEnabled(false);
                m_item.add(item);
                menu.add(m_item);                    
                
                menu.addSeparator();
            // -------------------------------------
            
            final JMenu sizeMenu = new JMenu("Face Size");
            item = new JMenuItem("Reset");
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    m_canvas.setPogSize(m_menuPog.getId(), -1);
                }
            });
            sizeMenu.add(item);

            item = new JMenuItem("Custom");
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(final ActionEvent e)
                {
                    final String ns = (String)JOptionPane.showInputDialog(GametableFrame.getGametableFrame(),
                        "New Size in Squares", 
                        "Pog Size", 
                        JOptionPane.PLAIN_MESSAGE, null, null, "");

                    if (ns != null) {
                        final int is = Integer.parseInt(ns);
                        if(is >= 1) m_canvas.setPogSize(m_menuPog.getId(), is);
                    }
                }
            });
            sizeMenu.add(item);
        
            item = new JMenuItem("0.5 squares");
            item.addActionListener(new ActionListener()
             {
                public void actionPerformed(final ActionEvent e)
                {
                    m_canvas.setPogSize(m_menuPog.getId(), 0.5f);
                }
             });
             sizeMenu.add(item);

             item = new JMenuItem("1 squares");
             item.addActionListener(new ActionListener()
             {
                 public void actionPerformed(final ActionEvent e)
                 {
                     m_canvas.setPogSize(m_menuPog.getId(), 1);
                 }
                   });
             sizeMenu.add(item);

             item = new JMenuItem("2 squares");
             item.addActionListener(new ActionListener()
             {
                 public void actionPerformed(final ActionEvent e)
                 {
                     m_canvas.setPogSize(m_menuPog.getId(), 2);
                 }
             });
             sizeMenu.add(item);

             item = new JMenuItem("3 squares");
             item.addActionListener(new ActionListener()
             {
                 public void actionPerformed(final ActionEvent e)
                 {
                     m_canvas.setPogSize(m_menuPog.getId(), 3);
                 }
             });
             sizeMenu.add(item);

             item = new JMenuItem("4 squares");
             item.addActionListener(new ActionListener()
             {
                 public void actionPerformed(final ActionEvent e)
                 {
                     m_canvas.setPogSize(m_menuPog.getId(), 4);
                 }
             });
             sizeMenu.add(item);

             item = new JMenuItem("6 squares");
             item.addActionListener(new ActionListener()
             {
                 public void actionPerformed(final ActionEvent e)
                 {
                     m_canvas.setPogSize(m_menuPog.getId(), 6);
                 }
             });
             sizeMenu.add(item);

             menu.add(sizeMenu);

              final JMenu rotateMenu = new JMenu("Rotation");
              
              item = new JMenuItem("0");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 0);
                  }
              });
              rotateMenu.add(item);

              item = new JMenuItem("60");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 60);
                  }
              });
              rotateMenu.add(item);

              item = new JMenuItem("90");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 90);
                  }
              });

              rotateMenu.add(item);
              item = new JMenuItem("120");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 120);
                  }
              });
              rotateMenu.add(item);

              item = new JMenuItem("180");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 180);
                  }
              });
              rotateMenu.add(item);

              item = new JMenuItem("240");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 240);
                  }
              });
              rotateMenu.add(item);

              item = new JMenuItem("270");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 270);
                  }
              });
              rotateMenu.add(item);

              item = new JMenuItem("300");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.rotatePog(m_menuPog.getId(), 300);
                  }
              });
              rotateMenu.add(item);

              menu.add(rotateMenu);
              
              final JMenu flipMenu = new JMenu("Flip");
              item = new JMenuItem("Reset");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.flipPog(m_menuPog.getId(), false, false);
                  }
              });
              flipMenu.add(item);

              item = new JMenuItem("Vertical");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.flipPog(m_menuPog.getId(), m_menuPog.getFlipH(), !m_menuPog.getFlipV());
                  }
              });
              flipMenu.add(item);

              item = new JMenuItem("Horizontal");
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                      m_canvas.flipPog(m_menuPog.getId(), !m_menuPog.getFlipH(), m_menuPog.getFlipV());
                  }
              });
              flipMenu.add(item);

              menu.add(flipMenu);
              
              item = new JMenuItem("Change Image");        
              item.addActionListener(new ActionListener()
              {
                  public void actionPerformed(final ActionEvent e)
                  {
                  	List<MapElementInstance> pogs = m_canvas.getSelectedMapElementInstances();
                  	
                      int size = pogs.size();
                      if(size > 1) {
                          JOptionPane.showMessageDialog(GametableFrame.getGametableFrame(), "You must only have 1 Pog selected.", 
                              "Image Selection", JOptionPane.INFORMATION_MESSAGE);
                          return;                    
                      } else if(size == 0) {
                          JOptionPane.showMessageDialog(GametableFrame.getGametableFrame(), "No Pogs Selected.", 
                              "Image Selection", JOptionPane.INFORMATION_MESSAGE);
                          return;
                      }
                      
                      MapElementInstance pog = pogs.get(0);                
                      GametableFrame.getGametableFrame().getGametableCanvas().setPogType(m_menuPog, pog);
                      m_canvas.unselectAllMapElementInstances();
                  }
              });
              menu.add(item);
              
              // --------------------------------
              if(layer == Layer.UNDERLAY) {
                  menu.addSeparator();
                  item = new JMenuItem("Set as Background");
                  item.addActionListener(new ActionListener()
                  {
                      public void actionPerformed(final ActionEvent e)
                      {
                          final int result = UtilityFunctions.yesNoDialog(GametableFrame.getGametableFrame(),
                              "Are you sure you wish to change the background to this pog's Image?", "Change Background?");
                          if (result == UtilityFunctions.YES)
                              m_canvas.changeBackgroundCP(m_menuPog.getId());                        
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
