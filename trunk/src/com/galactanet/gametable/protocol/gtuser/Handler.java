/*
 * Handler.java: GameTable is in the Public Domain.
 * 
 */


package com.galactanet.gametable.protocol.gtuser;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;



/**
 * Hyperlink protocol handler
 * 
 * @author iffy
 * 
 * #GT-AUDIT Handler
 */
public class Handler extends URLStreamHandler
{
    /**
     * Constructor
     */
    public Handler()
    {
    }

    /*
     * @see java.net.URLStreamHandler#openConnection(java.net.URL)
     */
    protected URLConnection openConnection(final URL u) throws IOException
    {
        return null;
    }
}
