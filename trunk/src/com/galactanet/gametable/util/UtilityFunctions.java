/*
 * UtilityFunctions.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.util;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.galactanet.gametable.ui.handler.gtuser.Handler;



/**
 * A class full of various and sundry static utility functions.
 * 
 * @author sephalon
 * 
 * #GT-AUDIT UtilityFunctions
 */
public class UtilityFunctions
{
    public final static int            CANCEL                   = -1;

    static private String              lastDir                  = null;

    public final static int            NO                       = 0;

    /**
     * The PNG signature to verify PNG data with.
     */
    private static final byte[]        PNG_SIGNATURE            = {
        (byte)(137 & 0xFF), 80, 78, 71, 13, 10, 26, 10
                                                                };

    private static final Random        RANDOM                   = getRandomInstance();

    // constants
    public static final char           UNIVERSAL_SEPARATOR      = '/';

    public final static int            YES                      = 1;
    
    /** 
     * Reverses a standard int Array
     * @param array
     */
    public static void arrayReverse(int a[])
    {
        int end = a.length - 1;
        int mid = a.length / 2;
        
        for (int i = 0; i < mid; i++)
        {
            int tmp = a[i];
            int i2 = end - i;
            a[i] = a[i2];
            a[i2] = tmp;
        }        
    }

    /**
     * @param source Component to get coordinates relative from.
     * @param destination Component to get coordinates relative to.
     * @param sourcePoint Source-relative coordinates to convert.
     * @return destination-relative coordinates of the given source-relative coordinates.
     */
    public static Point convertCoordinates(final Component source, final Component destination, final Point sourcePoint)
    {
        return getComponentCoordinates(destination, getScreenCoordinates(source, sourcePoint));
    }

    public static File doFileOpenDialog(final String title, final String extension, final boolean filterFiles)
    {
        final JFileChooser chooser = new JFileChooser();

        prepareFileDialog(chooser, title, filterFiles, extension);

        final int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            lastDir = chooser.getSelectedFile().getParent();
            return chooser.getSelectedFile();
        }

        return null;
    }

    public static File doFileSaveDialog(final String title, final String extension, final boolean filterFiles)
    {
        final JFileChooser chooser = new JFileChooser();

        prepareFileDialog(chooser, title, filterFiles, extension);

        final int returnVal = chooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            //The selected file's name
            String f = chooser.getSelectedFile().getName();
            //The selected file's path
            lastDir = chooser.getSelectedFile().getParent();

            //Check if file extension is .grm
            String ext = "";
            String filename = "";

            //Get the selected file's extension
            int i = f.lastIndexOf(".");
            if (i > 0 && i < f.length() - 1)
            {
               ext = f.substring(i + 1).toLowerCase();
            }
            if (ext.equals("") == false) { //if extension not is missing, do nothing
                return chooser.getSelectedFile();
            }
            
            // If we're here, the filename is missing, so we append it.
            filename = f + "." + extension;
            
            //Create new file using the selected path and file name with right extension
            File saveFile = new File(lastDir + "/" + filename);
            //return the file with proper extension
            return saveFile;
        }
        //Only get here if action was canceled, so return null to cancel save
        return null;
    }

    public static void getCurrentDir (String args[])
    {
        File dir1 = new File (".");
        File dir2 = new File ("..");
        try
        {
            System.out.println ("Current dir : " + dir1.getCanonicalPath());
            System.out.println ("Parent  dir : " + dir2.getCanonicalPath());
        }
        catch(Exception e) 
        {
            e.printStackTrace();
        }
    }
    
    public static String emitUserLink(final String name)
    {
        return emitUserLink(name, name);
    }

    public static String emitUserLink(final String name, final String text)
    {
        try
        {
            final URL url = new URL(Handler.PROTOCOL_NAME, urlEncode(name), "/");

            return "<a class=\"user\" href=\"" + url + "\">" + text + "</a>";
        }
        catch (final MalformedURLException e)
        {
            Log.log(Log.SYS, e);
            return "<a class=\"user\">" + text + "</a>";
        }
    }

    public static String getBodyContent(final String html)
    {
        final int end = html.lastIndexOf("</body>");
        int start = html.indexOf("<body") + "<body".length();
        start = html.indexOf('>', start) + 1;
        return html.substring(start, end).trim();
    }

    /**
     * Gets the canonical/absolute file of the given file.
     * 
     * @param file File to canonicalize.
     * @return Canonicalized file.
     */
    public static File getCanonicalFile(final File file)
    {
        try
        {
            return file.getCanonicalFile();
        }
        catch (final IOException ioe)
        {
            return file.getAbsoluteFile();
        }
    }

    /**
     * @param component Component to get coordinates relative to.
     * @param screenPoint Screen-relative coordinates to convert.
     * @return Component-relative coordinates of the given screen coordinates.
     */
    public static Point getComponentCoordinates(final Component component, final Point screenPoint)
    {
        final Point screenPos = getScreenPosition(component);
        return new Point(screenPoint.x - screenPos.x, screenPoint.y - screenPos.y);
    }  

    public static String getLine(final DataInputStream in)
    {
        try
        {
            boolean bFoundContent = false;
            final StringBuffer buffer = new StringBuffer();
            int count = 0;
            while (true)
            {
                // ready an empty string. If we have tl leave early, we'll return an empty string

                final char ch = (char)(in.readByte());
                if ((ch == '\r') || (ch == '\n'))
                {
                    // if it's just a blank line, then press on
                    // but if we've already had valid characters,
                    // then don't
                    if (bFoundContent)
                    {
                        // it's the end of the line!
                        return buffer.toString();
                    }
                }
                else
                {
                    // it's a non-CR character.
                    bFoundContent = true;
                    buffer.append(ch);
                }

                count++;
                if (count > 300)
                {
                    return buffer.toString();
                }
            }
        }
        catch (final Exception e)
        {
            Log.log(Log.SYS, e);
            return null;
        }
    }

    /**
     * Converts the filename to use File.seperatorChar.
     * 
     * @param path Path to canonicalize.
     * @return Canonicalized Path.
     */
    public static String getLocalPath(final String path)
    {
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0, size = path.length(); i < size; ++i)
        {
            final char c = path.charAt(i);
            if ((c == '/') || (c == '\\'))
            {
                buffer.append(File.separator);
            }
            else
            {
                buffer.append(c);
            }
        }

        return buffer.toString();
    }

    /**
     * Get a random integer between 0 and max -1
     * @param max maximum value (exclduded)
     * @return integer
     */
    public static int getRandom(final int max)
    {
        return RANDOM.nextInt(max);
    }

    /**
     * @return An instance of Random to use for all RNG.
     */
    private static Random getRandomInstance()
    {
        // SHA1PRNG
        Random rand;
        try
        {
            rand = SecureRandom.getInstance("SHA1PRNG");
        }
        catch (final NoSuchAlgorithmException e)
        {
            Log.log(Log.SYS, e);
            rand = new Random();
        }
        rand.setSeed(System.currentTimeMillis());
        return rand;
    }

    /**
     * Gets the child path relative to the parent path.
     * 
     * @param parent Parent path.
     * @param child Child path.
     * @return The relative path.
     */
    public static String getRelativePath(final File parent, final File child)
    {
        String parentPath = getLocalPath(getCanonicalFile(parent).getPath());
        if (parentPath.charAt(parentPath.length() - 1) != File.separatorChar)
        {
            parentPath = parentPath + File.separator;
        }
        final String childPath = getLocalPath(getCanonicalFile(child).getPath());

        return new String(childPath.substring(parentPath.length()));
    }

    /**
     * @param component Component that componentPoint is relative to.
     * @param componentPoint Point to convert to screen coordinates, relative to the given component.
     * @return The screen-relative coordinates of componentPoint.
     */
    public static Point getScreenCoordinates(final Component component, final Point componentPoint)
    {
        final Point screenPos = getScreenPosition(component);
        return new Point(componentPoint.x + screenPos.x, componentPoint.y + screenPos.y);
    }

    /**
     * @param component Component to get screen coordinates of.
     * @return The absolute screen coordinates of this component.
     */
    public static Point getScreenPosition(final Component component)
    {
        final Point retVal = new Point(component.getX(), component.getY());

        final Container container = component.getParent();
        if (container != null)
        {
            final Point parentPos = getScreenPosition(container);
            return new Point(retVal.x + parentPos.x, retVal.y + parentPos.y);
        }
        return retVal;
    }
 
    /**
     * Converts the filename to use UNIVERSAL_SEPERATOR.
     * 
     * @param path Path to canonicalize.
     * @return Canonicalized Path.
     */
    public static String getUniversalPath(final String path)
    {
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0, size = path.length(); i < size; ++i)
        {
            final char c = path.charAt(i);
            if ((c == '/') || (c == '\\'))
            {
                buffer.append(UNIVERSAL_SEPARATOR);
            }
            else
            {
                buffer.append(c);
            }
        }

        return buffer.toString();
    }

    /**
     * Checks to see whether one file is an ancestor of another.
     * 
     * @param ancestor The potential ancestor File.
     * @param child The child file.
     * @return True if ancestor is an ancestor of child.
     */
    public static boolean isAncestorFile(final File ancestor, final File child)
    {
        final File parent = child.getParentFile();
        if (parent == null)
        {
            return false;
        }

        if (parent.equals(ancestor))
        {
            return true;
        }

        final boolean b = isAncestorFile(ancestor, parent);
        return b;
    }

    /**
     * Checks the given binary date to see if it is a valid PNG file. It does this by checking the PNG signature.
     * 
     * @param data binary data to check
     * @return true if the binary data is a valid PNG file, false otherwise.
     */
    public static boolean isPngData(final byte[] data)
    {
        for (int i = 0; i < PNG_SIGNATURE.length; i++)
        {
            if (data[i] != PNG_SIGNATURE[i])
            {
                return false;
            }
        }

        return true;
    }

    public static void launchBrowser(final String url)
    {
        // Use java native support for browser first
        if (Desktop.isDesktopSupported())
        {
            Desktop desktop = Desktop.getDesktop();
            try
            {
                desktop.browse(new URI(url));
            }
            catch (IOException e)
            {
                Log.log(Log.NET, e);
            }
            catch (URISyntaxException e)
            {
                Log.log(Log.NET, e);
            }
            return;
        }
        
        final String osName = System.getProperty("os.name");
        try
        {
            if (osName.startsWith("Mac OS"))
            {
                final Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                final Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {
                    String.class
                });
                openURL.invoke(null, new Object[] {
                    url
                });
            }
            else if (osName.startsWith("Windows"))
            {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
            else
            {
                // assume Unix or Linux
                final String[] browsers = {
                    "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"
                };
                String browser = null;
                for (int count = 0; (count < browsers.length) && (browser == null); count++)
                {
                    if (Runtime.getRuntime().exec(new String[] {
                        "which", browsers[count]
                    }).waitFor() == 0)
                    {
                        browser = browsers[count];
                    }
                }

                if (browser == null)
                {
                    throw new Exception("Could not find web browser");
                }

                Runtime.getRuntime().exec(new String[] {
                    browser, url
                });
            }
        }
        catch (final Exception e)
        {
            Log.log(Log.NET, e);
        }
    }


    public static byte[] loadFileToArray(final File file)
    {
        if (!file.exists())
        {
            return null;
        }

        try
        {
            final DataInputStream infile = new DataInputStream(new FileInputStream(file));
            final byte[] buffer = new byte[1024];
            final ByteArrayOutputStream fileData = new ByteArrayOutputStream();
            while (true)
            {
                final int bytesRead = infile.read(buffer);
                if (bytesRead > 0)
                {
                    fileData.write(buffer, 0, bytesRead);
                }
                else
                {
                    break;
                }
            }
            return fileData.toByteArray();
        }
        catch (final IOException ex)
        {
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    public static byte[] loadFileToArray(final String filename)
    {
        final File file = new File(filename);
        return loadFileToArray(file);
    }

    public static void msgBox(final Component parent, final String msg)
    {
        msgBox(parent, msg, "Error!");
    }

    public static void msgBox(final Component parent, final String msg, final String title)
    {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Normalize an internal name.
     * Removes white space, moves to lower case, removes illegal characters 
     * @param name Name to normalize
     * @return normalized name
     */
    public static String normalizeName(final String name)
    {
        final String in = name.trim().toLowerCase();
        final int len = in.length();
        final StringBuffer out = new StringBuffer(len);
        for (int i = 0; i < len; ++i)
        {
            final char c = in.charAt(i);
            if (Character.isJavaIdentifierPart(c))
            {
                out.append(c);
            }
        }

        return out.toString();
    }

    static private void prepareFileDialog(final JFileChooser chooser, final String title, final boolean filter,
        final String extension)
    {
        if (lastDir != null)
        {
            chooser.setCurrentDirectory(new File(lastDir));
        }

        chooser.setDialogTitle(title);

        if (filter)
        {
            chooser.setFileFilter(new FileFilter()
            {
                public boolean accept(final File file)
                {
                    if (file.getName().endsWith(extension) || file.isDirectory())
                    {
                        return true;
                    }
                    return false;
                }

                public String getDescription()
                {
                    return (extension + " files");
                }
            });
        }

    }

    public static String stitchTogetherWords(final String[] words)
    {
        return stitchTogetherWords(words, 0, words.length);
    }

    public static String stitchTogetherWords(final String[] words, final int offset)
    {
        return stitchTogetherWords(words, offset, words.length - offset);
    }

    public static String stitchTogetherWords(final String[] words, final int offset, final int l)
    {
        final StringBuffer retVal = new StringBuffer();
        int realLength = l;
        if (realLength > words.length - offset)
        {
            realLength = words.length - offset;
        }

        for (int i = offset, max = offset + realLength; i < max; ++i)
        {
            retVal.append(words[i]);
            if (i < (max - 1))
            {
                retVal.append(' ');
            }
        }

        return retVal.toString();
    }
    /**
     * Decodes the given string using the URL decoding method.
     * 
     * @param in String to decode.
     * @return Decoded string.
     */
    public static String urlDecode(final String in)
    {
        try
        {
            return URLDecoder.decode(in, "UTF-8");
        }
        catch (final UnsupportedEncodingException e)
        {
            try
            {
                return URLDecoder.decode(in, "ASCII");
            }
            catch (final UnsupportedEncodingException e2)
            {
                return null;
            }
        }
    }

    /**
     * Encodes the given string using the URL encoding method.
     * 
     * @param in String to encode.
     * @return Encoded string.
     */
    public static String urlEncode(final String in)
    {
        try
        {
            return URLEncoder.encode(in, "UTF-8");
        }
        catch (final UnsupportedEncodingException e)
        {
            try
            {
                return URLEncoder.encode(in, "ASCII");
            }
            catch (final UnsupportedEncodingException e2)
            {
                return null;
            }
        }
    }



    public static String xmlEncode(final String str)
    {
        final StringWriter out = new StringWriter();
        final StringReader in = new StringReader(str);

        try
        {
            UtilityFunctions.xmlEncode(out, in);
        }
        catch (final IOException ioe)
        {
            Log.log(Log.SYS, ioe);
            return null;
        }

        return out.toString();
    }

    /**
     * Encode input data for XML entities.
     * @param out Encoded data output
     * @param in Non-encoded data input
     * @throws IOException
     */
    public static void xmlEncode(final Writer out, final Reader in) throws IOException
    {
        int i = in.read();
        while (i > 0)
        {
            final char c = (char)i;
            
            switch (c)
            {
            case '\'' :
                out.write("&apos;");
                break;

            case '\"':
                out.write("&quot;");
                break;

            case '<':
                out.write("&lt;");
                break;

            case '>':
                out.write("&gt;");
                break;
                
            case '&':
                out.write("&amp;");
                break;
                
            default:
                out.write(c);
            }
            
            i = in.read();
        }
    }

    public static int yesNoCancelDialog(final Component parent, final String msg, final String title)
    {
        final int ret = JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        switch (ret)
        {
            case JOptionPane.YES_OPTION:
            {
                return YES;
            }
            case JOptionPane.NO_OPTION:
            {
                return NO;
            }
            default:
            {
                return CANCEL;
            }
        }
    }

    public static int yesNoDialog(final Component parent, final String msg, final String title)
    {
        final int ret = JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.YES_NO_OPTION);
        switch (ret)
        {
            default:
            {
                return YES;
            }
            case JOptionPane.NO_OPTION:
            {
                return NO;
            }
        }
    }

    /**
     * Private constructor so no one can instantiate this.
     */
    private UtilityFunctions()
    {
        throw new RuntimeException("Do not do this.");
    }
    
    /**
     * Gets the composite object to use to paint ghostly images
     * @return AlphaComposite
     */
    public static Composite getGhostlyComposite()
    {
    	return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    }			
    
    /**
     * Gets the composite object to use to paint ghostly images
     * @return AlphaComposite
     */
    public static Composite getSelectedComposite()
    {
    	return g_selectedComposite;
    }
    
    private static final ColorComposite g_selectedComposite = new ColorComposite(Color.CYAN, 0.5f);
    
    /**
     * Gets the composite object to use to paint ghostly images
     * @return AlphaComposite
     */
    public static Composite getHilightedComposite()
    {
    	return g_highlightedComposite;
    }
    
    private static final ColorComposite g_highlightedComposite = new ColorComposite(Color.GREEN, 0.5f);
			
		/* @param g Context to draw onto.
     * @param x X position to draw at.
     * @param y Y position to draw at.
     * @param opacity 0 for fully transparent - 1 for fully opaque.
     */
    public static void drawTranslucent(Graphics2D g, Image image, final int x, final int y, final float opacity)
    {
    	Composite c = g.getComposite();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
      g.drawImage(image, x, y, null);
      g.setComposite(c);
    } 
}
