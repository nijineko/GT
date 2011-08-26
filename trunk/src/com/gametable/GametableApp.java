/*
 * GametableApp.java: GameTable is in the Public Domain.
 */


package com.gametable;

import java.awt.Toolkit;
import java.io.File;
import java.util.Map;
import java.util.Properties;

import javax.swing.UIManager;

import com.gametable.data.GameTableCore;
import com.gametable.ui.GametableFrame;
import com.gametable.util.Log;
import com.gametable.util.Strings;
import com.maziade.tools.Utils;
import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;



/**
 * Main class for the GametableApp. It provides the entry point for the execution of the application.
 *  * @author sephalon
 * 
 * #GT-AUDIT GametableApp
 */
public class GametableApp
{
		/**
		 * Home path for user files, gathered from first argument.  Logs and pogs will be stored there under a OSU-gt path.
		 * If no user path is specified, will use user home path
		 */
		public static File USER_FILES_PATH;
		
    /**
     * Name of the networking log file
     */
    private static final String NET_LOG_FILE  = "logs" + File.separator + "gt.net.log";
    
    /**
     * Name of the play log file
     */
    private static final String PLAY_LOG_FILE = "logs" + File.separator + "gt.play.html";
    
    /**
     * Name of the system log file
     */
    private static final String SYS_LOG_FILE  = "logs" + File.separator + "gt.sys.log";
    
    /**
     * String to describe gametable's chat version
     */
    public static final String VERSION        = "Gametable \"3.0\" nightly 20110818";
    
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
        	if (args.length > 0)
        	{
        		USER_FILES_PATH = new File(args[0]);
        	}
        	else
        	{
        		// TODO !Set log files and copy pog libraries over to user home (if paths do not exist).  Confirm copying.
        		// Display user home in chat window.
//        		for (Map.Entry<Object, Object> entry : System.getProperties().entrySet())
//        		{
//        			System.out.println(entry.getKey() + " = " + entry.getValue());
//        			
//        		}
        		String userHome = System.getProperty("user.home");
        		String appData = System.getenv("APPDATA");
        		
        		if (!com.maziade.tools.Strings.isEmpty(appData))
        			userHome = appData;
        		
        		userHome = userHome + File.separator + "osu-gt";
        		
        		//USER_FILES_PATH = new File(".").getCanonicalFile();
        		USER_FILES_PATH = new File(userHome).getCanonicalFile();
        	}
        	
        	if (!USER_FILES_PATH.exists())
        		USER_FILES_PATH.mkdirs();
        	
        	GametableApp.setProperty(PROPERTY_ICON_SIZE, 32);

					System.setProperty("java.protocol.handler.pkgs", "com.gametable.ui.handler"); // Register the package as a protocol handler
					
					Log.initializeLog(Log.SYS, new File(USER_FILES_PATH, SYS_LOG_FILE).getCanonicalPath());           // Initialize system log
					Log.initializeLog(Log.NET, new File(USER_FILES_PATH, NET_LOG_FILE).getCanonicalPath());           // Initialize network log
					Log.initializeLog(Log.PLAY, new File(USER_FILES_PATH, PLAY_LOG_FILE).getCanonicalPath());         // Initialize play log
					
					Log.log(Log.SYS, VERSION);                          // Write the version name to the system log
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());    // Set the Look and Feel
					Toolkit.getDefaultToolkit().setDynamicLayout(true); // Turns dynamic layout on
					
					// Initialize core
					GameTableCore core = GametableApp.getCore();
					
					// Initialize frame
					m_frame = new GametableFrame();
					m_frame.initialize();

					// Load properties
					core.loadProperties();
					
					// Start the frame (autoload and show)
					m_frame.start();
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
     * TODO #Properties What's that?
     */
    static public void setProperty(String key, String value)
    {
    	g_properties.setProperty(key, value);
    }
    
    /**
     * Sets a system property in the application
     * @param key Property key name
     * @param value Property value
     * TODO #Properties What's that?
     */
    static public void setProperty(String key, int value)
    {
    	g_properties.setProperty(key, String.valueOf(value));
    }
    
    /**
     * 
     * Get a system property value from the application
     * @param key Property key name
     * @return String value
     * TODO #Properties What's that?
     */
    static public String getProperty(String key)
    {
    	return g_properties.getProperty(key);
    }
    
    /**
     * Get a system property value from the application
     * @param key Property key name
     * @return Integer value (parsed from string)
     * TODO #Properties What's that?
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
    
    /**
     * Get the user interface
     * @return Pointer to use interface - could be null (ex: running in server mode)
     */
    static public GametableFrame getUserInterface()
    {
    	return m_frame;
    }
    
    private static Properties g_properties = new Properties();
    private static GametableFrame m_frame = null;


		/**
		 * Get the core interface instance.
		 * @return GameTableCore
		 */
		public static GameTableCore getCore()
		{
			if (GametableApp.g_gameTableCore == null)
			{
				GametableApp.g_gameTableCore = new GameTableCore(); 
		
				try
				{
					GametableApp.g_gameTableCore.initialize(); 
				}
				catch (final Exception e)
				{
					Log.log(Log.SYS, e);
				}
			}
			
			return GametableApp.g_gameTableCore;
		}

		/**
		 * The global core instance.
		 */
		private static GameTableCore	g_gameTableCore;
}
