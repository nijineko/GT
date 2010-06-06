/*
 * FloatingWindow.java: GameTable is in the Public Domain.
 */
package com.galactanet.gametable.ui;

import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

/**
 * @author Eric Maziade
 * FloatingWindow is a JFrame class used to wrap docked panels and float them.
 */
public class FloatingWindow extends JFrame
{
    /**
     * @param title window title
     * @param defaultPosition default position for this window
     * @param defaultOnTop true to set to "always on top" by default
     */
    public FloatingWindow(String title, Rectangle defaultPosition, boolean defaultOnTop)
    {
        // TODO restore size and location from settings, based on "window name"
        setTitle(title);
        setSize(defaultPosition.getSize());
        setLocation(defaultPosition.getLocation());
        setFocusable(true);
        setAlwaysOnTop(defaultOnTop);
        
        // TODO add menu item to dock + always on top.  add hook for custom menu items?
        // setJMenuBar(getNewWindowMenuBar());
    }
    
    /**
     * Stores the parent previous to the floating window
     * @param parent Container presently holding the control to float
     * @param autoHideDivider if true and parent is JSplitPane, the divider is automatically hidden
     */
    public void setPreviousParent(Container parent, boolean autoHideDivider)
    {
        m_previousParent = parent;
        if (parent instanceof JSplitPane)
        {
            JSplitPane pane = (JSplitPane) parent;
            int size = pane.getDividerSize();
            if (size > 0)
                m_previousParentDividerSize = size;
            
            pane.setDividerSize(0);
        }
    }
    
    /**
     * @param autoRestoreDivider if true and parent was JSplitPane, previously store divider size will be restored
     * @return Container UI as stored through setPreviousParent
     */
    public Container getPreviousParent(boolean autoRestoreDivider)
    {
        if (autoRestoreDivider && m_previousParentDividerSize > 0 && (m_previousParent instanceof JSplitPane))
        {
            JSplitPane pane = (JSplitPane) m_previousParent;
            pane.setDividerSize(m_previousParentDividerSize);
        }
        
        return m_previousParent;
    }

    /** 
     * Builds and return the window menu
     * @return the menu bar just built
     */
//    private JMenuBar getNewWindowMenuBar()
//    {
//        final JMenuBar menuBar = new JMenuBar();
//        menuBar.add(getFileMenu());
//        menuBar.add(getWindowMenu());
//
//        return menuBar;
//    }
    
    private Container m_previousParent = null;
    
    /**
     * Size of the divider, if previous parent was a JSplitPane
     */
    private int m_previousParentDividerSize = 0; 
}
