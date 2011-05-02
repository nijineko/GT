/*
 * WindowPositionTool.java
 * 
 * @created 2006
 * 
 * Copyright (C) 1999-2011 Eric Maziade
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
package com.maziade.tools.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

/**  
 * @author Eric Maziade
 * 
 * Helper tool to manage storing and restoring window position to properties 
 * 
 *
 */
public class WindowPositionTool
{
	/**
	 * Parameter string from a rectangle object
	 * @param rect rectangle
	 * @return string
	 */
	protected static String fromRectangle(Rectangle rect)
	{
		if (rect == null)
			return "";
		
		return rect.x + "," + rect.y + "," + rect.width + "," + rect.height;
	}
	
	/**
	 * Parameter string from a Point object
	 * @param point point
	 * @return string
	 */
	protected static String fromPoint(Point point)
	{
		if (point == null)
			return "";
		
		return point.x + "," + point.y;
	}
	
	/**
	 * Rectangle object from parameter string
	 * @param param parameter string
	 * @param defaultRect rectangle object to return if string is invalid or empty
	 * @return Rectangle object
	 */
	protected static Rectangle toRectangle(String param, Rectangle defaultRect)
	{
		if (param == null)
			return defaultRect;
		
		String parts[] = param.split(",");
		
		if (parts.length != 4)
			return defaultRect;
		
		try
		{
			return new Rectangle(
					Integer.valueOf(parts[0]), Integer.valueOf(parts[1]),
					Integer.valueOf(parts[2]), Integer.valueOf(parts[3]));
		}
		catch (NumberFormatException e)
		{
			return defaultRect;
		}
	}
	
	/**
	 * Point object from parameter string
	 * @param param parameter string
	 * @param defaultPoint point object to return if string is invalid or empty
	 * @return Point object
	 */
	protected static Point toPoint(String param, java.awt.Point defaultPoint)
	{
		if (param == null)
			return defaultPoint;
		
		String parts[] = param.split(",");
		
		if (parts.length != 2)
			return defaultPoint;
		
		try
		{
			return new Point(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
		}
		catch (NumberFormatException e)
		{
			return defaultPoint;
		}
	}
	
	/**
	 * Constructor
	 * @param window window to track
	 * @param properties properties object to use
	 */
	public WindowPositionTool(Window window, Properties properties)
	{
		m_window = window;
		m_properties =  properties;
		
		window.addWindowListener( new WindowAdapter() {
    	@Override
    	public void windowClosing(WindowEvent e) {
    		storeProperties(m_properties);
    	}    	
    });
		
		window.addComponentListener(new ComponentAdapter() {
			@Override
    	public void componentResized(ComponentEvent e) {
    		onWindowResized();
    	}    	
    });
	}

	
	/**
   * Store window information to properties
   * NB: You can override this method and add a store to file mechanism, if you wish.
   */
	
	public void storeProperties(Properties properties)
  {
  	Rectangle bounds = m_window.getBounds();
  	
  	if (m_window instanceof Frame)
  	{
  		Frame frame = (Frame)m_window;
  		int state = frame.getExtendedState();
  		
  		if (state != Frame.NORMAL && m_lastBounds != null)
  			bounds = m_lastBounds;
  		
  		properties.setProperty(PROPERTY_WINDOW_STATE, String.valueOf(state));
  	}
  	
  	properties.setProperty(PROPERTY_WINDOW_POSITION, fromRectangle(bounds));	 
  }
  
  /**
   * Restore window information from properties
   */
  public void restoreProperties(Properties properties)
  {
  	String extendedState = properties.getProperty(PROPERTY_WINDOW_STATE);
  	int state;
  	
  	if (extendedState == null)
  		state = DEFAULT_EXTENDED_STATE;
  	else
  		try
  		{
  			state = Integer.valueOf(extendedState);
  		}
  		catch ( NumberFormatException e)
  		{
  			state = DEFAULT_EXTENDED_STATE;
  		}
  	if (m_window instanceof Frame)
  	{
  		Frame frame = (Frame)m_window;
  		frame.setExtendedState(state);
  	}
  	
  	// restore size and position from stored parameters     
  	Rectangle rect = toRectangle(properties.getProperty(PROPERTY_WINDOW_POSITION), null);
  	if (rect != null)
  	{  	
	  	// Make sure top left corner cannot be outside of screen
	  	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	  	rect.x = Math.min(rect.x,  screenSize.width - 15);
	  	rect.y = Math.min(rect.y,  screenSize.height - 15);
	  	rect.x = Math.max(rect.x,  0);
	  	rect.y = Math.max(rect.y,  0);
  	}
	  	
	  if (rect != null)
	  {
	  	m_window.setBounds(rect);
	  }
	  else
	  {
	  	m_window.setLocationByPlatform(true);
	  	//m_window.setLocationRelativeTo(m_window.getParent());
	  	m_window.setSize(DEFAULT_SIZE);	  	
	  }
  }
  
  /**
   * Window just got resized
   */
  private void onWindowResized()
  {
  	if (m_window instanceof Frame)
  	{
  		Frame frame = (Frame)m_window;
  		int state = frame.getExtendedState();
  		if (state == Frame.NORMAL)
  		{
  			m_lastBounds = frame.getBounds();
  		}
  	}
  }
  
  private Window m_window;
  private Properties m_properties = null;
  private Rectangle m_lastBounds = null;

  // Override this before creating instance if you want to provide your prefix
  public static String PROPERTY_PREFIX = WindowPositionTool.class.getName() + ".";
  
  // You can override this on subclasses or directly after instance creation if you wish
  
  public String PROPERTY_WINDOW_POSITION 	= PROPERTY_PREFIX + "BOUNDS";
  public String PROPERTY_WINDOW_STATE 		= PROPERTY_PREFIX + "EXTENDEDSTATE";
  
  public Dimension	DEFAULT_SIZE 						= new Dimension(400, 400);	// Default size if no size is stored
  public int 				DEFAULT_EXTENDED_STATE 	= Frame.NORMAL;							// Default state of the window, if not defined
}
