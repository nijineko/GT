/*
 * PogLibrary.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.ui;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.galactanet.gametable.data.MapElement;
import com.galactanet.gametable.data.MapElement.Layer;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;



/**
 * A recursively-defined representation of a set of pogs.
 * 
 * @author iffy
 * 
 * #GT-AUDIT PogLibrary
 */
public class PogLibrary
{
    // --- Constants -------------------------------------------------------------------------------------------------



    // --- Members ---------------------------------------------------------------------------------------------------

    /**
     * Calculated the library name from a directory file.
     * 
     * @param file Directory to use to calculate name.
     * @return Name of this library node.
     */
    private static String getNameFromDirectory(final File file)
    {
    	return file.getName();
    }

    /**
     * In-place sorts the list of pogs by height.
     * 
     * @param toSort List of Pogs to sort.
     */
    private static void sortPogsByLabel(final List<MapElement> toSort)
    {
        Collections.sort(toSort, new Comparator<MapElement>()        
        {            
            @Override            
            public int compare(MapElement pa, MapElement pb)
            {
                return pa.getNormalizedDisplayLabel().compareTo(pb.getNormalizedDisplayLabel());
            }
        });
    }

    /**
     * Set of acquired libraries.
     */
    private final Set<File>  acquiredLibraries = new HashSet<File>();

    /**
     * Set of acquired pog names.
     */
    private final Set<File> acquiredPogs      = new HashSet<File>();

    /**
     * A list of child libraries, sorted by name.
     */
    private final List<PogLibrary> children          = new ArrayList<PogLibrary>();

    /**
     * Whether this is a pog or underlay library.
     */
    private Layer        libraryType       = MapElement.Layer.POG;

    /**
     * The filesystem path to this set of pogs.
     */
    private final File       m_libraryPath;
    
    /**
     * Used to build relative path stringd
     */
    private final String m_rootPathName;

    /**
     * The short name of this library. Unique within the parent library.
     */
    private String     m_libraryName              = null;

    // --- Constructors ----------------------------------------------------------------------------------------------

    /**
     * The parent library.
     */
    private PogLibrary parent            = null;

    /**
     * The list of pogs in this library.
     */
    private final List<MapElement> pogs              = new ArrayList<MapElement>();

    // --- Methods ---------------------------------------------------------------------------------------------------

    /**
     * Root PogLibrary Constructor.
     * 
     * @throws IOException
     */
    public PogLibrary() throws IOException
    {
        m_libraryPath = new File(".").getCanonicalFile();
        m_rootPathName = getRoot().m_libraryPath.getAbsolutePath() + File.separator;
        
        m_libraryName = getNameFromDirectory(m_libraryPath);
        
        addLibrary("pogs", Layer.POG);
        addLibrary("environment", Layer.ENVIRONMENT);
        addLibrary("overlays", Layer.OVERLAY);
        addLibrary("underlays", Layer.UNDERLAY);
    }

    /**
     * Child PogLibrary Constructor.
     */
    public PogLibrary(final PogLibrary mommy, final String directory, final Layer type) throws IOException
    {
        parent = mommy;
        libraryType = type;
        m_libraryPath = new File(directory).getCanonicalFile();
        m_rootPathName = getRoot().m_libraryPath.getAbsolutePath() + File.separator;
        
        if(!m_libraryPath.exists()) {
            if(!m_libraryPath.mkdir()) {
                throw new IOException("Failed to find and create directory " + directory);
            }            
        }
        if (!m_libraryPath.canRead() || !m_libraryPath.isDirectory())
        {
            throw new IOException("cannot read from " + directory);
        }
        m_libraryName = getNameFromDirectory(m_libraryPath);

        acquirePogs();
    }
    
    private String makeRelativePath(File file)
    {
    	return file.getAbsolutePath().replace(m_rootPathName, "");
    }

    /**
     * Ensures that this panel has all the available pogs loaded.
     */
    public boolean acquirePogs()
    {
        if (!m_libraryPath.exists())
        {
            return false;
        }

        boolean retVal = false;

        // We don't want to scour the root library for pogs
        if (getParent() != null)
        {
            final File[] files = m_libraryPath.listFiles();

            for (File file : files)
            {
            	if (file.getName().startsWith("."))
            		continue;	// skip files starting with a period (.svn, etc.)
            	
                final String filename = makeRelativePath(file);

                if (file.isFile() && file.canRead())
                {
                    if (addPog(file, 1, libraryType, true) != null)
                    {
                        retVal = true;
                    }
                }
                else if (file.isDirectory() && file.canRead())
                {
                    if (addLibrary(filename, libraryType) != null)
                    {
                        retVal = true;
                    }
                }
            }
        }

        final int numPogs = pogs.size();
        for (int i = 0; i < numPogs; ++i)
        {
            final MapElement pog = pogs.get(i);
            if (pog.isLoaded())
            {
                pog.load();
            }
        }

        final int size = children.size();
        for (int i = 0; i < size; ++i)
        {
            final PogLibrary child = children.get(i);
            if (child.acquirePogs())
            {
                retVal = true;
            }
        }

        return retVal;
    }

    /**
     * Adds library to this library, ensuring it doesn't already exist.
     * 
     * @param libName
     * @param type
     * @return
     */
    private PogLibrary addLibrary(final String libName, final Layer type)
    {
        try
        {
            final File libDir = new File(libName).getAbsoluteFile();
            if (acquiredLibraries.contains(libDir))
            {
                return null;
            }

            final PogLibrary child = new PogLibrary(this, libDir.getPath(), type);
            children.add(child);
            // Log.log(Log.SYS, new Exception(this + " added: " + child));
            acquiredLibraries.add(libDir);
            return child;
        }
        catch (final Exception ex)
        {
            // any exceptions thrown in this process cancel
            // the addition of that one directory.
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    private MapElement addPog(File pogFile, final int facing, final Layer type)
    {
        return addPog(pogFile, facing, type, false);
    }

    private MapElement addPog(File pogFile, final int facing, final Layer type, boolean ignoreOnFail)
    {
        try
        {
            
            if (acquiredPogs.contains(pogFile))
            {
                return null;
            }

            String filePath = makeRelativePath(pogFile);
            
            final MapElement pog = new MapElement(filePath, facing, type);
            if (!ignoreOnFail || !pog.isLoaded())
            {
                // Log.log(Log.SYS, new Exception(this + " added: " + pog));
                pogs.add(pog);
                sortPogsByLabel(pogs);
            }
            acquiredPogs.add(pogFile);
            return pog;
        }
        catch (final Exception ex)
        {
            // any exceptions thrown in this process cancel
            // the addition of that one pog.
            Log.log(Log.SYS, ex);
            return null;
        }
    }

    /**
     * Creates a placeholder PogType for a pog that hasn't been received yet. Delegates to children if appropriate.
     * 
     * @param filename Filename of pog to create placeholder for.
     * @param face What we think the face size of the pog is at this point.
     * @return Placeholder PogType
     */
    public MapElement createPlaceholder(final String filename, final int face)
    {
        // Log.log(Log.SYS, this + ".createPlaceholder(" + filename + ", " + face + ")");
        final File f = new File(filename);
        final File p = f.getParentFile();
        final PogLibrary lib = findDeepestChild(p);
        if (lib == null)
        {
            Log.log(Log.SYS, "unable to create library");
            return null;
        }

        if (lib != this)
        {
            return lib.createPlaceholder(filename, face);
        }

        final File absParent = p.getAbsoluteFile();
        p.mkdirs();
        if (!absParent.equals(getLocation()))
        {
            PogLibrary child = getChild(p.getPath());
            if (child != null)
            {
                return child.createPlaceholder(filename, face);
            }

            File next = absParent;
            while (!next.getParentFile().equals(getLocation()))
            {
                next = next.getParentFile();
            }
            child = addLibrary(next.getAbsolutePath(), libraryType);
            if (child == null)
            {
                return null;
            }
            return child.createPlaceholder(filename, face);
        }

        return addPog(f, face, libraryType);
    }

    private PogLibrary findDeepestChild(final File p)
    {
        final File path = p.getAbsoluteFile();

        // trivial accept
        if (path.equals(getLocation()))
        {
            return this;
        }

        for (PogLibrary child : children)
        {
            final PogLibrary lib = child.findDeepestChild(path);
            if (lib != null)
            {
                return lib;
            }
        }

        if (!UtilityFunctions.isAncestorFile(getLocation(), path))
        {
            return null;
        }

        return this;
    }

    /**
     * @return Returns the pogs in this library.
     */
    public List<MapElement> getAllPogs()
    {
        final int size = children.size();
        if (size < 1)
        {
            return getPogs();
        }

        final List<MapElement> accum = new ArrayList<MapElement>(pogs);   //@revise this copies the array on each call.  can we can maintain a syncrhonized unmodifiable list instead?
        for (PogLibrary child : children)
        {
            accum.addAll(child.getAllPogs());
        }

        return Collections.unmodifiableList(accum);
    }

    /**
     * Gets the child library of the given name.
     * 
     * @param libraryName Name of library to get.
     * @return Library found or null.
     */
    public PogLibrary getChild(final String libraryName)
    {
        final PogLibrary child = getChildExact(libraryName);
        if (child != null)
        {
            return child;
        }

        final File childPath = UtilityFunctions.getCanonicalFile(new File(m_libraryPath.getPath()
            + File.separator + libraryName));

        return getChildExact(childPath.getPath());
    }

    /**
     * Gets a child by the exact name.
     * 
     * @param libraryName Exact name of child to find.
     * @return Child library with the given name, or null if not found.
     */
    private PogLibrary getChildExact(final String libraryName)
    {
        final File file = UtilityFunctions.getCanonicalFile(new File(libraryName));
        final int size = children.size();
        for (int i = 0; i < size; ++i)
        {
            final PogLibrary child = children.get(i);
            if (file.equals(child.getLocation()))
            {
                return child;
            }
        }

        return null;
    }

    /**
     * @return Returns the child libraries of this library.
     */
    public List<PogLibrary> getChildren()
    {
        return Collections.unmodifiableList(children);  // @revise keep that instance - it remains synchronized. 
    }

    // --- Object Implementation ---

    /**
     * @return Returns the location of this library.
     */
    public File getLocation()
    {
        return m_libraryPath;
    }

    // --- Private Methods ---

    /**
     * @return Returns the name of this library.
     */
    public String getName()
    {
        return m_libraryName;
    }

    /**
     * @return Returns the parent library.
     */
    public PogLibrary getParent()
    {
        return parent;
    }

    /**
     * @return Gets the canonical path of this library.
     */
    public String getPath()
    {
        try
        {
            return m_libraryPath.getCanonicalPath();
        }
        catch (final IOException ioe)
        {
            return m_libraryPath.getAbsolutePath();
        }
    }

    /**
     * Gets the pog with the given name.
     * 
     * @param pogName name of pog to fetch
     * @return Pog found or null.
     */
    public MapElement getPog(final String pogName)
    {
        int size = pogs.size();
        for (int i = 0; i < size; ++i)
        {
            final MapElement pogType = pogs.get(i);
            if (pogName.equals(pogType.getImageFilename()))
            {
                return pogType;
            }
        }

        size = children.size();
        for (int i = 0; i < size; ++i)
        {
            final PogLibrary child = children.get(i);
            final MapElement pog = child.getPog(pogName);
            if (pog != null)
            {
                return pog;
            }
        }

        return null;
    }

    /**
     * @return Returns the pogs in this library.
     */
    public List<MapElement> getPogs()
    {
        return Collections.unmodifiableList(pogs);
    }

    /**
     * @return Returns the root library.
     */
    public PogLibrary getRoot()
    {
        if (parent == null)
        {
            return this;
        }

        return parent.getRoot();
    }
    
    public void removePog(final MapElement pt) {
        pogs.remove(pt);       
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "[PogLib " + getLocation() + " (" + pogs.size() + ")]";
    }

}
