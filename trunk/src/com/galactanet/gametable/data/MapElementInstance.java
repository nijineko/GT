/*
 * Pog.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.data;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import com.galactanet.gametable.data.MapElement.Layer;
import com.galactanet.gametable.data.deck.Card;
import com.galactanet.gametable.data.net.PacketSourceState;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.ui.MapElementRendererIF;
import com.galactanet.gametable.ui.PogLibrary;
import com.galactanet.gametable.util.Images;
import com.galactanet.gametable.util.UtilityFunctions;



/**
 * Represents an instance of a MapElement on the Map.
 * 
 * @author sephalon
 * 
 * #GT-AUDIT MapElementInstance
 */
public class MapElementInstance implements Comparable<MapElementInstance>
{
	/**
	 * TODO @revise Attribute system to be replaced by more flexible properties system
	 */
    protected static class Attribute
    {
        public boolean changed = true;
        public String  name;
        public String  value;

        public Attribute(final String n, final String v)
        {
            name = n;
            value = v;
        }
    }

    // --- Static Members --------------------------------------------------------------------------------------------

    /**
     * TODO @revise not here - in the UI that actually cares about sorting
     * Global min sort id for pogs.
     */
    public static long         g_nextSortId               = 0;

    // --- Members ---------------------------------------------------------------------------------------------------

    private MapElementInstanceRenderer m_renderer = null;
    private Layer              m_layer                    = Layer.UNDERLAY;
    private double             m_angle                    = 0d;
    private boolean            m_forceGridSnap            = false;


    /**
     * Whether this element should be displayed as flipped horizontally
     */
    private boolean	m_flipH                   = false;
    
    /**
     * Whether this element should be displayed as flipped vertically
     */
    private boolean	m_flipV                   = false;
    
    
    /**
     * Name/value pairs of the attributes assigned to this pog.
     */
    private final Map<String, Attribute>          m_attributes               = new TreeMap<String, Attribute>();
    
    /**
     * Size of this element, in pixel
     */
    private Dimension m_elementSize = new Dimension(); // @revise abhorrent to model

    /**
     * Marks whether this element is in a corrupted state and should be bypassed
     */
    private boolean             m_corrupted               = false;

    /**
     * True if this pog is notifying the world that it's text had changed.
     * @deprecated
     */
    protected boolean            m_bTextChangeNotifying     = false;

    /**
     * Is this pog tinted?
     */
    private boolean            m_bTinted                  = false;	// uh oh...
    
    private boolean            m_bSelected                = false; // #grouping
    private String             m_group                  	= null; //#grouping

    // null in most cases. If it's not null, it's a
    // card in a deck
    private Card      m_card;

    /**
     * The unique id for this MapElementInstance
     */
    private final MapElementInstanceID	m_id;

    /**
     * Locked state for this pog.
     */
    private boolean            m_locked                   = false;

    /**
     * The parent element from which this element was instantiated
     */
    private MapElement            m_parentElement;

    /**
     * Position of the pog on the map in map coordinates.
     */
    private MapCoordinates              m_position                 = MapCoordinates.ORIGIN;

    /**
     * Scale for this Element.  The scale ratio is automatically configured when the face size of this element is set. 
     */
    private float              m_scale                    = 1f;

    /**
     * The sort order for this pog.  Pogs are sorted according to this number.
     */
    private long               m_sortOrder                = 0;

    /**
     * The primary label for the element.
     */
    private String             m_name                     = "";
    
    /**
     * The normalized name for the pog
     */
    private String 							m_nameNormalized					= "";

    /**
     * A bit field representing the surface of the image the cursor responds to
     * @review move / share with instance (cache in here, stored with scaled image?)
     */
    public BitSet              m_hitMap;

    // --- Constructors ----------------------------------------------------------------------------------------------

    public MapElementInstance(final DataInputStream dis) throws IOException
    {
    	String filename = UtilityFunctions.getLocalPath(dis.readUTF());
      final PogLibrary lib = GametableFrame.getGametableFrame().getPogLibrary();
      filename = UtilityFunctions.getRelativePath(lib.getLocation(), new File(filename));

      final int x = dis.readInt();
      final int y = dis.readInt();
      m_position = new MapCoordinates(x, y);
      final int size = dis.readInt();
      
      long id = dis.readLong();
      m_id = MapElementInstanceID.fromNumeric(id);
      
      m_sortOrder = dis.readLong();
      setName(dis.readUTF(), true);
      // boolean underlay =
      
      boolean underlay = dis.readBoolean();

      try {
          m_scale = dis.readFloat();
      }
      catch(IOException exp)
      {
          m_scale = 1f;
      }
      
      try {
          m_angle = dis.readDouble();
      }
      catch(IOException exp)
      {
          m_angle = 0.;
      }
      try {
          m_flipH = dis.readBoolean();
          m_flipV = dis.readBoolean();
      }
      catch(IOException exp)
      {
          m_flipH = false;
          m_flipV = false;
      }
      
      try {
          m_locked = dis.readBoolean();
      }
      catch(IOException exp)
      {
          m_locked = false;
      }
      
      try {
       // read in the card info, if any
          final boolean bCardExists = dis.readBoolean();
          if (bCardExists)
          {
              m_card = new Card();
              m_card.read(dis);
          }
          else
          {
              // no card
              m_card = null;
          }

          final int numAttributes = dis.readInt();
          m_attributes.clear();
          for (int i = 0; i < numAttributes; i++)
          {
              final String key = dis.readUTF();
              final String value = dis.readUTF();
              setAttribute(key, value);
          }
      }
      catch(IOException exp)
      {
          m_card = null;
      }
      
      Layer layer;
      try {
          int ord = dis.readInt();
          layer = Layer.fromOrdinal(ord);
      }
      catch(IOException exp) 
      {
          if (underlay) 
              layer = Layer.UNDERLAY;
          else 
              layer = Layer.POG;
          
          m_forceGridSnap = false;
      }
      
      try {
        String group = dis.readUTF();
        if(group.equals("")) group = null;            
        if(group != null) PogGroups.addPogToGroup(group, this);
      } catch(IOException exp) {
        m_group = null;
      }


      // special case pseudo-hack check
      // through reasons unclear to me, sometimes a pog will get
      // a size of around 2 billion. A more typical size would
      // be around 1.
      if ((size > 100) || (m_scale > 100.0))
      {
          m_corrupted = true;
          return;
      }

      stopDisplayPogDataChange();

      MapElement type = lib.getPog(filename);
      if (type == null)
      {
          type = lib.createPlaceholder(filename, size);
      }
      
      m_parentElement = type;
      m_layer = type.getLayerType();
      
      m_layer = layer; // Saving here as the init updates the layer for newly dropped pogs.
      reinitializeHitMap();
    }

    public MapElementInstance(final MapElementInstance toCopy)
    {
    	this(toCopy, true);
    }
    
    public MapElementInstance(final MapElementInstance orig, final boolean copygroup) 
    {
    	m_id = MapElementInstanceID.acquire();
    	
      m_position = orig.m_position;
      m_parentElement = orig.m_parentElement;
      m_scale = orig.m_scale;
      m_angle = orig.m_angle;
      m_flipH = orig.m_flipH;
      m_flipV = orig.m_flipV;

      setName(orig.m_name, true);
      
      m_layer    = orig.m_layer;
      m_forceGridSnap = orig.m_forceGridSnap;

      if(copygroup) m_group = orig.m_group;

      if (orig.m_card == null)
      {
          m_card = orig.m_card;
      }
      else
      {
      		m_card = new Card();
          m_card.copy(orig.m_card);
      }

      for (Attribute attribute : orig.m_attributes.values())
      {
          setAttribute(attribute.name, attribute.value);
      }
      
      stopDisplayPogDataChange();
      reinitializeHitMap();
    }

    public MapElementInstance(final MapElement type)
    {
    	m_id = MapElementInstanceID.acquire();
    	m_parentElement = type;
      m_layer = type.getLayerType();
    }

    /**
     * Get the renderer for this map element instance
     * @return renderer instance
     */
   public MapElementRendererIF getRenderer()
   {
  	 if (m_renderer == null)
  		 m_renderer = new MapElementInstanceRenderer(this);
  	 
  	 return m_renderer;
   }

    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(MapElementInstance pog)  
    {
        if (equals(pog))
            return 0;

        long sort1 = getSortOrder();
        long sort2 = pog.getSortOrder();
        
        if (sort1 != sort2)
        {
        	if (sort1 < sort2)
        		return -1;
        	
        	return 1;
        }
        
        String name1 = getName();
        String name2 = pog.getName();
        
        int res = name1.compareTo(name2);
        if (res != 0)
        	return res;
        
        return getId().compareTo(pog.getId());
    }

    private void displayPogDataChange()
    {
    	//@revise trigger listeners instead - the view should pop the information overlay, not the model
    	
        // we don't do this if the game is receiving inital data.
        if (PacketSourceState.isHostDumping())
        {
            return;
        }

        // we also don't do this if the game is loading a file from disk.
        if (PacketSourceState.isFileLoading())
        {
            return;
        }

        m_bTextChangeNotifying = true;
    }




    private void reinitializeHitMap()
    {
    	m_hitMap = null;
    	
    	updateElementDimension();
    	
        final Image img = m_parentElement.getImage();
        if (img == null)  
        {
        	return;
        }

        final int iw = m_elementSize.width;
        final int ih = m_elementSize.height;
        
        if (ih < 0 ||  iw < 0) 
        {
           return;
        }


        final BufferedImage bufferedImage = new BufferedImage(iw,ih, BufferedImage.TYPE_INT_RGB);

        {
            final Graphics2D g = bufferedImage.createGraphics();
            g.setColor(new Color(0xff00ff));
            g.fillRect(0, 0, iw, ih);
            
            g.drawImage(Images.rotateImage(Images.flipImage(img, m_flipH, m_flipV), m_angle)
                , 0,0, iw, ih, null);

            g.dispose();
        }

        final DataBuffer buffer = bufferedImage.getData().getDataBuffer();
        
        final int len = iw * ih;

        m_hitMap = new BitSet(len);
        m_hitMap.clear();
        for (int i = 0; i < len; ++i)
        {
            final int pixel = buffer.getElem(i) & 0xFFFFFF;
            m_hitMap.set(i, (pixel != 0xFF00FF));
        }
    }

   
    




    
    /**
     * TODO @revise move to renderer interface?  We're not using canvas...
     * Returns a rectangle identifying the space taken by the Pog
     * @return 
     */
    public MapRectangle getBounds()
    {
    	// Make sure hit map is built and dimensions are ok
    	getHitMap();
	
      final MapRectangle pogArea = new MapRectangle(m_position, getWidth(), getHeight());
      
      return pogArea;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(final Object obj)
    {
        if (this == obj)
            return true;

        final MapElementInstance pog = (MapElementInstance)obj;
        return pog.getId().equals(m_id);
    }

    public double getAngle()
    {
        return m_angle;
    }
    
    public boolean getForceGridSnap()
    {
        return m_forceGridSnap;
    }

    public boolean getFlipH()
    {
        return m_flipH;
    }

    public boolean getFlipV()
    {
        return m_flipV;
    }

    public String getAttribute(final String name)
    {
        final String normalizedName = UtilityFunctions.normalizeName(name);
        final Attribute a = m_attributes.get(normalizedName);
        if (a == null)
        {
            return null;
        }
        return a.value;
    }
    
    /**
     * Get all attributes from this element
     * @return attributes
     */
    public Collection<Attribute> getAttributes()
    {
    	return m_attributes.values();
    }

    public Set<String> getAttributeNames()
    {
        final Set<String> s = new HashSet<String>();
        
        for (Attribute attribute : m_attributes.values())
        {
            s.add(attribute.name);
        }
        return Collections.unmodifiableSet(s);
    }

    public Card getCard()
    {
        return m_card;
    }

    /**
     * Get face size of the pog, in map coordinates (ex: squares)
     * @return Size of a face
     */
    public int getFaceSize()
    {
        if (m_scale == 1f)
        {
            return m_parentElement.getFaceSize();
        }
        
        int size = Math.round(m_parentElement.getFaceSize() * m_scale);
        if (size < 1)
        	return 1;
        
        return size;        
    }

    public int getHeight()
    {
        if (m_scale == 1f)
        {
            return m_elementSize.height;
        }

        return Math.round(m_elementSize.height * m_scale);
    }

    /**
     * Get the unique ID of this instance
     * @return Unique Element ID
     */
    public MapElementInstanceID getId()
    {
        return m_id;
    }
    
    public Layer getLayer() {
        return m_layer;
    }

    public MapElement getPogType()
    {
        return m_parentElement;
    }

    public MapCoordinates getPosition()
    {
        return m_position;
    }

    /**
     * @return A vector to adjust the drag position when snapping for odd-sized pogs.
     */
    public Point getSnapDragAdjustment()
    {
        final Point adjustment = new Point();
        final int width = getWidth();
        final int height = getHeight();

        if (width < height)
        {
            adjustment.x = -(height - width) / 2;
        }
        else if (width > height)
        {
            adjustment.y = -(width - height) / 2;
        }

        return adjustment;
    }

    public long getSortOrder()
    {
        return m_sortOrder;
    }
    
    /**
     * Return the pog's normalized name
     * @return normalized name
     */
    public String getNormalizedName()
    {
    	return m_nameNormalized;
    }

    /**
     * Return the pog's display name
     * @return display name
     */
    public String getName()
    {
        return m_name;
    }

    public int getWidth()
    {
        if (m_scale == 1f)
        {
            return m_elementSize.width;
        }

        return Math.round(m_elementSize.width * m_scale);
    }


    public int getX()
    {
        return getPosition().x;
    }

    // --- Setters ---

    public int getY()
    {
        return getPosition().y;
    }

    public boolean hasAttributes()
    {
        return !m_attributes.isEmpty();
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return m_id.hashCode();
    }

    public boolean isCardPog()
    {
        if (m_card == null)
        {
            return false;
        }
        return true;
    }

    public boolean isLocked()
    {
        return m_locked;
    }

    public boolean isTinted()
    {
        return m_bTinted;
    }

    /**
     * Verifies if pog is an underlay
     * @revise this is imprecise - pogs are no longer just pog or underlay - they can be from various layer.  Underlay is an improper term.
     * @return
     * @deprecated
     */
    public boolean isUnderlay()
    {
        return m_layer != Layer.POG;
    }

    // --- Drawing ---

    /** ************************ CARD POG STUFF ****************************** */
    
    /**
     * @revise this should not be in Pog class (considering Deck / Card functionality as a plug-in)
     */
    public void makeCardPog(final Card card)
    {
        // note the card info. We copy it, so we aren't affected
        // by future changes to this card instance.
        m_card = card.makeCopy();

        /*
         * Commented out because these attributes become really annoying in play. They pop up whenever the mouse is over
         * the card and it's irritating. 
         */
        // set the appropriate attributes
        String cardName = card.getCardName();
        String cardDesc = card.getCardDesc();
        String deckName = card.getDeckName();
        
        if ( cardName.length() > 0 )
        {
            setName(cardName, true);
        }
        if ( cardDesc.length() > 0 )
        {
            setAttribute("Desc", cardDesc);
        }
        if ( deckName.length() > 0 )
        {
            setAttribute("Deck", deckName);
        }
    }

    private Point modelToPog(MapCoordinates modelPoint)
    {
        // translation for x & y
        int sx = 0;
        int sy = 0;
        if(m_angle != 0) {
            final int dw = getWidth();
            final int dh = getHeight();
            
            Image image = m_parentElement.getImage();
            final int iw = image == null ? 0 : image.getWidth(null);
            final int ih = image == null ? 0 : image.getHeight(null);
            // Point Shift for the drawing
            sx = Math.round((dw - iw)/2 * m_scale);
            sy = Math.round((dh - ih)/2 * m_scale);
        }
       
//        int x = modelPoint.x - m_position.x;
//        x = Math.round(x / m_scale);
//        int y = modelPoint.y - m_position.y;
//        y = Math.round(y / m_scale);

        int x = modelPoint.x - (m_position.x - sx);        
        x = Math.round(x / m_scale);
        
        int y = modelPoint.y - (m_position.y - sy);
        y = Math.round(y / m_scale);

        return new Point(x, y);
    }

    public void removeAttribute(final String name)
    {
        final String normalizedName = UtilityFunctions.normalizeName(name);
        m_attributes.remove(normalizedName);
    }

    public void setAngle(final double angle)
    {
        m_angle = angle;
        reinitializeHitMap();
    }
    
    public void setForceGridSnap(final boolean forceGridSnap)
    {
        m_forceGridSnap = forceGridSnap;
        reinitializeHitMap();
    }
    
    //#randomrotate
    public void setAngleFlip(final double angle,final boolean flipH, final boolean flipV)
    {
        m_flipH = flipH;
        m_flipV = flipV;
        m_angle = angle;
        reinitializeHitMap();
    }

    
    public void setFlip(final boolean flipH, final boolean flipV)
    {
        m_flipH = flipH;
        m_flipV = flipV;
        reinitializeHitMap();
    }

   public void setAttribute(final String name, final String value)
    {
        final String normalizedName = UtilityFunctions.normalizeName(name);
        m_attributes.put(normalizedName, new Attribute(name, value));
        displayPogDataChange();
    }

   /**
    * Set the number of tiles this element should be taking.  The element will be automatically rescaled to fit the specific number of tiles.
    * @param faceSize Number of tiles.
    */
    public void setFaceSize(final float faceSize)
    {
        if (faceSize <= 0)
        {
            if (m_scale != 1)
            {
                m_scale = 1;
                reinitializeHitMap();
            }
            
            return;
        }
            

        final float targetDimension = GameTableMap.getBaseSquareSize() * faceSize;

        float maxDimension = GameTableMap.getBaseSquareSize();
        Image image = m_parentElement.getImage();
        if (image != null)
        	maxDimension = Math.max(image.getWidth(null), image.getHeight(null));

        if (maxDimension == 0)
        {
            throw new ArithmeticException("Zero sized pog dimension: " + this);
        }
        m_scale = targetDimension / maxDimension;
        reinitializeHitMap();
        return;
    }
    
    /**
     * Get the scale ratio required to obtain face size
     * @return scale ratio
     */
    public float getFaceSizeScale()
    {
    	return m_scale;
    }

    // --- Miscellaneous ---

    public void setLocked(final boolean b)
    {
        m_locked = b;
    }
    
    
    public void setLayer(final Layer l) {
        m_layer = l;       
    }
    
    public void setPogType(final MapElement pt) {
        m_parentElement = pt;
        reinitializeHitMap();
    }

    public void setPosition(final MapCoordinates pos)
    {
        m_position = pos;
    }

    public void setSortOrder(final long order)
    {
        m_sortOrder = order;
    }
    
    public void setName(String name)
    {
    	setName(name, false);
    }

    public void setName(String name, boolean silent)
    {
        m_name = name;
        m_nameNormalized = UtilityFunctions.normalizeName(m_name);
        
        if (!silent)
        	displayPogDataChange();
    }
    

    /**
     * Set the pog's selection status.  Only GametableMap should call this method, as it maintains its own representation of what is selected
     * Calling the pog's "setSelected" method will have no direct effect upon the map.
     *     
     * @param selected true to select, false to unselect.
     */
    protected void setSelected(final boolean selected) 
    {
        m_bSelected = selected;
    } 

    /**
     * Returns the selectino state of the pog as last stored by its GameTableMap
     * @return true if selected
     */
    public boolean isSelected()
    {
        return m_bSelected;
    }
    
    // #grouping
    public boolean isGrouped() {
        if(m_group != null) return true;
        return false;
    }
    /** **********************************************************************************************
     * #grouping
     * @return
     */
    public String getGroup() {
        return m_group;
    }
 
   /**
    * Set the pog's group name.  Should only by called by PogGroups
    * #tag:grouping
    * 
    * @param groupName
    */
    protected void setGroup(final String groupName) 
    {
        m_group = groupName;
    }


    // --- Private Helpers ---

    public void setTinted(final boolean b)
    {
        m_bTinted = b;
    }

    private void stopDisplayPogDataChange()
    {
        m_bTextChangeNotifying = false;
        for (Attribute attribute : m_attributes.values())
        {
            attribute.changed = false;
        }
    }

    /**
     * Checks whether this Element "contains" the specified point, where x and y are defined to be relative to the coordinate system of this element
     * eg: 0, 0 is top left corner.
     * 
     * NB : Method is final and calls {@link #contains(int, int)}
     * 
     * @param p Point to check for
     * @return true / false
     */
    public final boolean contains(MapCoordinates p)
    {
    	Point pt = modelToPog(p);
      return contains(pt.x, pt.y);
    }

    /**
     * Checks whether this Element "contains" the specified point, where x and y are defined to be relative to the coordinate system of this element
     * eg: 0, 0 is top left corner. 
     * 
     * @param x X coordinate of point to test 
     * @param y Y coordinate of point to test 
     * @return Returns true if the point is contained within this element
     */
    public boolean contains(int x, int y)
    {
        // if it's not in our rect, then forget it.
        if (x < 0)
        {
            return false;
        }

        if (x >= getWidth())
        {
            return false;
        }

        if (y < 0)
        {
            return false;
        }

        if (y >= getHeight())
        {
            return false;
        }

        // Make sure `hit map' has been generated        
        getHitMap();
        
        // Look within the map to see if we are contained
        
        final int idx = x + (y * getWidth());
        final boolean value = m_hitMap.get(idx);

        return value;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "[" + getId() + ":" + getName() + " pos: " + getPosition() + " face-size: " + getFaceSize() + "]";
    }

    public void writeToPacket(final DataOutputStream dos) throws IOException
    {
        dos.writeUTF(getPogType().getImageFilename());
        dos.writeInt(getX());
        dos.writeInt(getY());
        dos.writeInt(getPogType().getFaceSize());
        dos.writeLong(m_id.numeric());
        dos.writeLong(m_sortOrder);
        dos.writeUTF(m_name);
        dos.writeBoolean(isUnderlay());
        dos.writeFloat(m_scale);
        dos.writeDouble(m_angle);
        dos.writeBoolean(m_flipH);
        dos.writeBoolean(m_flipV);
        dos.writeBoolean(m_locked);

        // write out the card info, if any
        if (m_card != null)
        {
            dos.writeBoolean(true); // we have a valid card
            m_card.write(dos);
        }
        else
        {
            dos.writeBoolean(false); // no card info
        }

        dos.writeInt(m_attributes.size());
        
        for (Attribute attribute : m_attributes.values())
        {
            dos.writeUTF(attribute.name);
            dos.writeUTF(attribute.value);
        }
        
        dos.writeInt(m_layer.ordinal());
        
        dos.writeBoolean(m_forceGridSnap);  //#gridsnap
        if(m_group == null) dos.writeUTF("");
        else dos.writeUTF(m_group);
    }

    public BitSet getHitMap()
    {
        if (m_hitMap != null)
            return m_hitMap;
        
        reinitializeHitMap();
        return m_hitMap;
    }
    
    /**
     * Recalculate the width and height of this ElementInstance based on its current angle 
     */
    private void updateElementDimension()
    {
    	Image image = m_parentElement.getImage();

      if (m_parentElement.getImage() == null)
      {
      	m_elementSize.setSize(GameTableMap.getBaseSquareSize(), GameTableMap.getBaseSquareSize());
      }
      else
      {      
      	m_elementSize.setSize(image.getWidth(null), image.getHeight(null));
      	//Images.getRotatedSquareSize(image.getWidth(null), image.getHeight(null), m_angle, m_elementSize);
      }
    }
 
    /**
     * @return true if this element has been marked as corrupted and should be bypassed
     */
    public boolean isCorrupted()
    {
    	return m_corrupted;
    }
}
