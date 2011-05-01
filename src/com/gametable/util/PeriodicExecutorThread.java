/*
 * PeriodicExecutorThread.java: GameTable is in the Public Domain.
 */


package com.gametable.util;

import javax.swing.SwingUtilities;

/**
 * Simple thread to periodically place a task on the AWT event queue.
 * 
 * @author iffy
 * 
 * @audited themaze75
 */
public class PeriodicExecutorThread extends Thread
{
    private static final int DEFAULT_INTERVAL = 25;

    private final Runnable   m_runnableTask;
    private final int m_interval;
    
    /**
     * Constructor;
     */
    public PeriodicExecutorThread(int interval, Runnable r, String name)
    {
        super(name);
        setPriority(NORM_PRIORITY + 1);
        m_runnableTask = r;
        m_interval = interval;
    }

    /**
     * Constructor;
     */
    public PeriodicExecutorThread(final Runnable r)
    {
    	this(DEFAULT_INTERVAL, r, "PeriodicExecutorThread");
    }

    /*
     * @see java.lang.Thread#run()
     */
    @Override
    public void run()
    {
        try
        {
            while (true)
            {
                sleep(m_interval);

                try
                {
                	// Makes sure we remain in swing's UI thread when executing the task
                  SwingUtilities.invokeLater(m_runnableTask);
                }
                catch (final Throwable t)
                {
                    Log.log(Log.SYS, t);
                }
            }
        }
        catch (final InterruptedException ie)
        {
          // Thread has been interrupted (usually due to closed connection)
        }
    }
}
