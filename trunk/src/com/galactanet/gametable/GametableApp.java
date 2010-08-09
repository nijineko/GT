/*
 * GametableApp.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable;

import java.awt.Toolkit;
import java.util.Properties;

import javax.swing.UIManager;

import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;



/**
 * Main class for the GametableApp. It provides the entry point for the execution of the application.
 *  * @author sephalon
 * 
 * #GT-AUDIT GametableApp
 */
public class GametableApp
{
    /**
     * Name of the networking log file
     */
    private static final String NET_LOG_FILE  = "logs/gt.net.log";
    
    /**
     * Name of the play log file
     */
    private static final String PLAY_LOG_FILE = "logs/gt.play.html";
    
    /**
     * Name of the system log file
     */
    private static final String SYS_LOG_FILE  = "logs/gt.sys.log";
    
    /**
     * String to describe gametable's chat version
     */
    public static final String VERSION        = "Gametable \"3.0\" dev";
    
    /**
     * Language code
     */
    public static final String  LANGUAGE = "En";
    
    /**
     * Property key for default icon size
     */
    public static final String PROPERTY_ICON_SIZE = GametableApp.class.getName() + ".PROPERTY_ICON_SIZE"; 
    

    /**
     * Main method
     * This is the entry point of the application. 
     * 
     * @param args String[] Like every Java program, it receives parameters from the command line
     */
    static public void main(final String[] args)
    {
        try
        {
        	GametableApp.setProperty(PROPERTY_ICON_SIZE, 32);

					System.setProperty("java.protocol.handler.pkgs", "com.galactanet.gametable.ui.handler"); // Register the package as a protocol handler
					
					Log.initializeLog(Log.SYS, SYS_LOG_FILE);           // Initialize system log
					Log.initializeLog(Log.NET, NET_LOG_FILE);           // Initialize network log
					Log.initializeLog(Log.PLAY, PLAY_LOG_FILE);         // Initialize play log
					Log.log(Log.SYS, VERSION);                          // Write the version name to the system log
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());    // Set the Look and Feel
					Toolkit.getDefaultToolkit().setDynamicLayout(true); // Turns dynamic layout on
					new GametableFrame().setVisible(true);              // Creates an instance of the main UI object and shows it.
                                                                // The app won't end until the main frame is closed
        }
        catch (final Throwable t)
        {
            Log.log(Log.SYS, t); // Log any error into the system log
        }
    }
    
    /**
     * Sets a system property in the application
     * @param key Property key name
     * @param value Property value
     */
    static public void setProperty(String key, String value)
    {
    	g_properties.setProperty(key, value);
    }
    
    /**
     * Sets a system property in the application
     * @param key Property key name
     * @param value Property value
     */
    static public void setProperty(String key, int value)
    {
    	g_properties.setProperty(key, String.valueOf(value));
    }
    
    /**
     * Get a system property value from the application
     * @param key Property key name
     * @return String value
     */
    static public String getProperty(String key)
    {
    	return g_properties.getProperty(key);
    }
    
    /**
     * Get a system property value from the application
     * @param key Property key name
     * @return Integer value (parsed from string)
     */
    static public int getIntegerProperty(String key)
    {
    	try
    	{
    		return Integer.valueOf(g_properties.getProperty(key));
    	}
    	catch (NumberFormatException e)
    	{
    		return 0;
    	}
    }
    
    private static Properties g_properties = new Properties();
}
