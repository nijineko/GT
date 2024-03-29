/*
 * Log.java: GameTable is in the Public Domain.
 * 
 * @comment {themaze75} Might want to consider using a standardized logging method such as lof4j 
 */


package com.gametable.util;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;



/**
 * Static class providing an interface for logging out to a file or console.
 * 
 * @author iffy
 * 
 * #GT-AUDIT Log
 */
public class Log    // @revise consider using tested logging engine such as log4j
{
    // --- Logging Contexts ---

    private static final DateFormat DATE_FORMAT       = new SimpleDateFormat("yyyy-MM-dd\' \'HH:mm:ss.SSS");

    private static final String     DIVIDER           = " *************************";

    private static final String     DIVIDER_PREFIX    = "**** ";

    // --- Constants ---

    private static final Object     G_LOCK            = new Object();
    private static Map<Integer, Log>              g_loggingContexts = new HashMap<Integer, Log>();
    /**
     * The logging context for network output.
     */
    public static final int         NET               = 1;
    /**
     * The logging context for play output.
     */
    public static final int         PLAY              = 2;

    /**
     * The logging context for general system output.
     */
    public static final int         SYS               = 0;

    /**
     * Static initializer.
     */
    static
    {
        initializeLog(SYS, System.err);
    }

    private static final String getLogName(final int context)
    {
        switch (context)
        {
            case SYS:
                return "SYS";
            case NET:
                return "NET";
            case PLAY:
                return "PLAY";
            default:
                return "UNKNOWN";
        }
    }

    public static final void initializeLog(final int context, final PrintStream ps)
    {
        synchronized (G_LOCK)
        {
            final Log oldLog = g_loggingContexts.get(context);
            if (oldLog != null)
            {
                oldLog.close();
            }

            final Log log = new Log(context);
            g_loggingContexts.put(new Integer(context), log);
            log.setLogTarget(ps);
        }
    }

    public static final void initializeLog(final int context, final String s)
    {
        synchronized (G_LOCK)
        {
            final Log oldLog = g_loggingContexts.get(new Integer(context));
            if (oldLog != null)
            {
                oldLog.close();
            }

            final Log log = new Log(context);
            g_loggingContexts.put(new Integer(context), log);
            log.setLogTarget(s);
        }
    }

    public static final void log(final int context, final String s)
    {
        synchronized (G_LOCK)
        {
            final Log l = g_loggingContexts.get(new Integer(context));
            if (l != null)
            {
                l.log(s);
            }
        }
    }

    public static final void log(final int context, final Throwable t)
    {
        synchronized (G_LOCK)
        {
            final Log l = g_loggingContexts.get(new Integer(context));
            if (l != null)
            {
                l.log(t);
            }
        }
    }

    private final int        context;
    private FileOutputStream fos  = null;
    private final Object     LOCK = new Object();
    private PrintStream      out  = null;

    /**
     * Static class, so don't allow instantiation.
     */
    private Log(final int ctxt)
    {
        context = ctxt;
    }

    public void close()
    {
        if (fos != null)
        {
            try
            {
                fos.close();
            }
            catch (final IOException e)
            {
                log(SYS, e);
            }
        }

        fos = null;
        out = null;
    }

    public void log(final String s)
    {
        synchronized (LOCK)
        {
            out.print(DATE_FORMAT.format(new GregorianCalendar().getTime()));
            out.print(" [");
            out.print(getLogName(context));
            out.print("] ");
            out.println(s);
        }
    }

    public void log(final Throwable t)
    {
        synchronized (LOCK)
        {
            out.print(DATE_FORMAT.format(new GregorianCalendar().getTime()));
            out.print(" [");
            out.print(getLogName(context));
            out.print("] ");
            t.printStackTrace(out);
        }
        t.printStackTrace();
    }

    public void setLogTarget(final PrintStream ps)
    {
        synchronized (LOCK)
        {
            out = ps;
            fos = null;
        }
    }

    public void setLogTarget(final String filename)
    {
        synchronized (LOCK)
        {
            try
            {
                File path = new File(filename);
                if (!path.exists())
                {
                    File parent = path.getParentFile();                    
                    parent.mkdirs();
                    
                    path.createNewFile();
                    path.setWritable(true);
                }
                
                fos = new FileOutputStream(filename, true);
                out = new PrintStream(fos, true);
            }            
            catch (final FileNotFoundException ffne)
            {
                setLogTarget(System.err);
                log(ffne);
            }
            catch (IOException e)
            {
                setLogTarget(System.err);
                log(e);
            }

            out.println(" ");
            out.print(DIVIDER_PREFIX);
            out.print(getLogName(context));
            out.print(' ');
            out.print(DATE_FORMAT.format(new GregorianCalendar().getTime()));
            out.println(DIVIDER);
        }
    }
}
