/*
 * PogWindow.java: GameTable is in the Public Domain.
 */


package com.gametable.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

/**
 * PogWindow is the tabbed pane that holds the Pog Library, Active Pog and Macros tabs.
 
 * 
 */
public class PogWindow extends JTabbedPane
{
	private final GametableFrame m_frame;
	
	/**
	 * 
	 */
	public PogWindow(GametableFrame frame)
	{
		m_frame = frame;
	}
	
	/**
	 * Add a tab to this window
	 * @param component
	 * @param title
	 */
	public void addTab(Component component, String title)
	{
		add(component, title);
	}
	
    /**
     * @return true if pog window is currently docked
     */
    public boolean isDocked()
    {
        return m_docked;
    }
    
    /**
     * Float or dock the pog window, depending on its status
     */
    public void toggleDockStatus()
    {
        if (m_docked)
            floatWindow();
        else
            dockWindow();
    }
 
    /**
     * Float the pog window (if docked)
     */
    public void floatWindow()
    {
        if (!m_docked)
            return;
        
        Container p = getParent();
        
        if (m_floatWindow == null)
            m_floatWindow = new FloatingWindow("Pog Window", new Rectangle(0, 80, 195, 500), false);
        
        m_floatWindow.setPreviousParent(p, true);
        
        p.remove(this);
        
        m_floatWindow.add(this);
        m_floatWindow.setVisible(true);
  
        m_frame.validate();
        
        m_docked = false;        
    }

    /**
     * Dock the pog window (if floating)
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
                ((JSplitPane)parent).add(this, JSplitPane.LEFT);
            }   
        }

        m_frame.validate();
        
        m_docked = true;
    }
    
    private FloatingWindow          m_floatWindow           = null;
    private boolean                 m_docked               = true;
}
