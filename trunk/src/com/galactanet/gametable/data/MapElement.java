/*
 * Pog.java: GameTable is in the Public Domain.
 */

package com.galactanet.gametable.data;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.galactanet.gametable.data.MapElementTypeIF.Layer;
import com.galactanet.gametable.data.net.PacketSourceState;
import com.galactanet.gametable.ui.MapElementRendererIF;
import com.galactanet.gametable.util.Images;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;
import com.maziade.tools.XMLUtils;

/**
 * Represents an instance of a MapElement on the Map.
 * 
 * @author sephalon
 * 
 * @audited by themaze75
 */
public class MapElement implements Comparable<MapElement>, XMLSerializeIF
{
	/**
	 * TODO Attribute system to be replaced by more flexible properties system
	 */
	protected static class Attribute
	{
		public boolean	changed	= true;
		public String		name;
		public String		value;

		public Attribute(final String n, final String v)
		{
			name = n;
			value = v;
		}
	}

	/**
	 * A bit field representing the surface of the image the cursor responds to
	 * 
	 */
	public BitSet													m_hitMap;

	/**
	 * True if this pog is notifying the world that it's text had changed.
	 * 
	 * @deprecated
	 */
	@Deprecated
	protected boolean											m_bTextChangeNotifying	= false;

	/**
	 * The angle, in degrees, at which this element is painted
	 */
	private double												m_angle									= 0d;

	/**
	 * Name/value pairs of the attributes assigned to this element.
	 */
	private final Map<String, Attribute>	m_attributes						= new TreeMap<String, Attribute>();

	/**
	 * Marks whether this element is in a corrupted state and should be bypassed
	 */
	private boolean												m_corrupted							= false;

	/**
	 * Size of this element, in map units
	 */
	private Dimension											m_elementSize						= new Dimension();

	/**
	 * Scale for displaying this element at the proper size.
	 * 
	 * The scale ratio is automatically configured when the face size of this element is set.
	 */
	private float													m_faceSizeScale					= 1f;
	
	/**
	 * Number of tiles taken by a face of this map element instance
	 */
	private float	m_faceSize = 1f;

	/**
	 * Whether this element should be displayed as flipped horizontally
	 */
	private boolean												m_flipH									= false;

	/**
	 * Whether this element should be displayed as flipped vertically
	 */
	private boolean												m_flipV									= false;

	/**
	 * The unique id for this MapElementInstance
	 */
	private final MapElementID		m_id;

	/**
	 * The layer in which this element is placed
	 */
	private Layer													m_layer									= Layer.UNDERLAY;

	/**
	 * The parent element from which this element was instantiated
	 */
	private MapElementTypeIF										m_mapElementType;

	/**
	 * The primary label for the element.
	 */
	private String												m_name									= "";

	/**
	 * The normalized name for the element. Used for internal representation, mainly when saving and loading maps.
	 */
	private String												m_nameNormalized				= "";

	/**
	 * Position of the element in map coordinates.
	 */
	private MapCoordinates								m_position							= MapCoordinates.ORIGIN;

	/**
	 * Renderer instance In future versions, we could allow a plug-in to supply its own renderer
	 */
	private MapElementRenderer		m_renderer							= null;
	
	/**
	 * Constructor
	 * @param parent Parent XML element
	 */
	public MapElement(Element parent, XMLSerializeConverter converter)
	{
		MapElementID id;
		try
		{
			long l = Long.valueOf(XMLUtils.getFirstChildElementContent(parent, "id"));
			id = MapElementID.acquire();
			converter.storeMapElementID(l, id);		
		}
		catch (NumberFormatException e)
		{
			Log.log(Log.SYS, "Invalid in element definition node : " + XMLUtils.getFirstChildElementContent(parent, "id") );
			id = MapElementID.acquire();
		}
		m_id = id;
		
		m_angle = UtilityFunctions.parseFloat(XMLUtils.getFirstChildElementContent(parent, "angle"), 0f);
		m_name = XMLUtils.getFirstChildElementContent(parent, "name", "");
		
		setFaceSize(UtilityFunctions.parseFloat(XMLUtils.getFirstChildElementContent(parent, "facesize", "1"), 1f));
		
		String layerName = XMLUtils.getFirstChildElementContent(parent, "layer", Layer.UNDERLAY.name());
		try
		{
			m_layer = Layer.valueOf(layerName);
		}
		catch (IllegalArgumentException e)
		{
			m_layer = Layer.UNDERLAY;
		}
		
		Element flip = XMLUtils.getFirstChildElementByTagName(parent, "flip");
		if (flip != null)
		{
			m_flipH = !UtilityFunctions.areStringsEquals(flip.getAttribute("h"), "false");
			m_flipV = !UtilityFunctions.areStringsEquals(flip.getAttribute("v"), "false");
		}
		
		Element pos = XMLUtils.getFirstChildElementByTagName(parent, "pos");
		if (pos != null)
			m_position = new MapCoordinates(pos);

		// Normalized type name
		String fullyQualifiedTypeName = XMLUtils.getFirstChildElementContent(parent, "type");
		MapElementTypeIF type = MapElementTypeLibrary.getMasterLibrary().getElementType(fullyQualifiedTypeName);
		
		if (type == null)
		{
			type = MapElementTypeLibrary.getMasterLibrary().createPlaceholderType(fullyQualifiedTypeName, Math.max(1, (int)getFaceSize()));
		}
		
		m_mapElementType = type;

		// Load back values
		Element values = XMLUtils.getFirstChildElementByTagName(parent, "values");
		if (values != null)
		{
			for (Element value : XMLUtils.getChildElementsByTagName(values, "value"))
			{
				String normalized = value.getAttribute("name");
				String name = XMLUtils.getFirstChildElementContent(value, "name", normalized);
				String val =  XMLUtils.getFirstChildElementContent(value, "value", "");
				
				setAttribute(name, val);
			}
		}			

		reinitializeHitMap();
	}

	/**
	 * Constructor
	 * 
	 * @param dis Data input stream
	 * @throws IOException
	 */
	public MapElement(final DataInputStream dis) throws IOException
	{
		final MapElementTypeLibrary lib = MapElementTypeLibrary.getMasterLibrary();
		String type_fqn = dis.readUTF();		
		
		final int x = dis.readInt();
		final int y = dis.readInt();
		m_position = new MapCoordinates(x, y);
		final int size = dis.readInt();

		long id = dis.readLong();
		m_id = MapElementID.fromNumeric(id);

		setName(dis.readUTF());
		// boolean underlay =

		try
		{
			m_faceSizeScale = dis.readFloat();
		}
		catch (IOException exp)
		{
			m_faceSizeScale = 1f;
		}

		try
		{
			m_angle = dis.readDouble();
		}
		catch (IOException exp)
		{
			m_angle = 0.;
		}
		try
		{
			m_flipH = dis.readBoolean();
			m_flipV = dis.readBoolean();
		}
		catch (IOException exp)
		{
			m_flipH = false;
			m_flipV = false;
		}

		Layer layer;
		try
		{
			int ord = dis.readInt();
			layer = Layer.fromOrdinal(ord);
		}
		catch (IOException exp)
		{
			layer = Layer.UNDERLAY;
		}

		// special case pseudo-hack check
		// through reasons unclear to me, sometimes a pog will get
		// a size of around 2 billion. A more typical size would
		// be around 1.
		if ((size > 100) || (m_faceSizeScale > 100.0))
		{
			m_corrupted = true;
			return;
		}

		stopDisplayPogDataChange();

		MapElementTypeIF type = lib.getElementType(type_fqn);
		if (type == null)
		{
			type = lib.createPlaceholderType(type_fqn, size);
		}

		m_mapElementType = type;
		m_layer = type.getLayerType();

		m_layer = layer; // Saving here as the init updates the layer for newly dropped pogs.
		reinitializeHitMap();
	}

	/**
	 * Creates a new instance based on type TODO Use MapElement as factory - all constructors should be protected
	 * 
	 * @param type
	 */
	public MapElement(final MapElementTypeIF type)
	{
		m_id = MapElementID.acquire();
		m_mapElementType = type;
		m_layer = type.getLayerType();
	}

	/**
	 * Constructor
	 * 
	 * @param toCopy element to copy
	 */
	public MapElement(final MapElement toCopy)
	{
		m_id = MapElementID.acquire();

		m_position = toCopy.m_position;
		m_mapElementType = toCopy.m_mapElementType;
		m_faceSizeScale = toCopy.m_faceSizeScale;
		m_angle = toCopy.m_angle;
		m_flipH = toCopy.m_flipH;
		m_flipV = toCopy.m_flipV;

		setName(toCopy.m_name);

		m_layer = toCopy.m_layer;

		for (Attribute attribute : toCopy.m_attributes.values())
		{
			setAttribute(attribute.name, attribute.value);
		}

		stopDisplayPogDataChange();
		reinitializeHitMap();
	}

	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MapElement pog)
	{
		if (equals(pog))
			return 0;

		String name1 = getName();
		String name2 = pog.getName();

		int res = name1.compareTo(name2);
		if (res != 0)
			return res;

		return getId().compareTo(pog.getId());
	}

	/**
	 * Checks whether this Element "contains" the specified point, where x and y are defined to be relative to the
	 * coordinate system of this element eg: 0, 0 is top left corner.
	 * 
	 * NB : Method is final and calls {@link #contains(int, int)}
	 * 
	 * @param p Point to check for
	 * @return true / false
	 */
	public final boolean contains(MapCoordinates p)
	{
		// Move the coordinates to origin (top left of pog)
		int x = p.x - m_position.x;
		int y = p.y - m_position.y;

		// If it is outside our bounds, it is not contained
		if (x < 0)
			return false;

		if (x >= getWidth())
			return false;

		if (y < 0)
			return false;

		if (y >= getHeight())
			return false;

		// Make sure `hit map' has been generated
		getHitMap();

		// Look within the map to see if we are contained
		final int idx = x + (y * getWidth());
		final boolean value = m_hitMap.get(idx);

		return value;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;

		final MapElement pog = (MapElement) obj;
		return pog.getId().equals(m_id);
	}

	/**
	 * Get the angle at which this element should be displayed
	 * 
	 * @return Angle in degrees
	 */
	public double getAngle()
	{
		return m_angle;
	}

	/**
	 * Gets an attribute value
	 * 
	 * @param name Name of the attribute to look for
	 * @return Value or null, if not fould
	 */
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
	 * Get all attribute names from this element
	 * 
	 * @return List of names
	 */
	public Set<String> getAttributeNames()
	{
		return Collections.unmodifiableSet(m_attributes.keySet());
	}

	/**
	 * Get all attributes from this element
	 * 
	 * @return attributes
	 */
	public Collection<Attribute> getAttributes()
	{
		return m_attributes.values();
	}

	/**
	 * Returns a rectangle identifying the space taken by the element on the map
	 * 
	 * @return Rectangle of map coordinates
	 */
	public MapRectangle getBounds()
	{
		// Make sure hit map is built and dimensions are ok
		getHitMap();

		final MapRectangle pogArea = new MapRectangle(m_position, getWidth(), getHeight());

		return pogArea;
	}

	/**
	 * Get face size of the element, in number of tiles
	 * 
	 * @return Size of a face
	 */
	public float getFaceSize()
	{
		return m_faceSize;		
	}

	/**
	 * Get the scale ratio required to obtain face size
	 * 
	 * @return scale ratio
	 */
	public float getFaceSizeScale()
	{
		return m_faceSizeScale;
	}

	/**
	 * @return True if this element should be displayed as flipped horizontally
	 */
	public boolean getFlipH()
	{
		return m_flipH;
	}

	/**
	 * @return True if this element should displayed as flipped vertically
	 */
	public boolean getFlipV()
	{
		return m_flipV;
	}

	/**
	 * Get the height of this map element, in map units
	 * 
	 * @return map units
	 */
	public int getHeight()
	{
		if (m_faceSizeScale == 1f)
		{
			return m_elementSize.height;
		}

		return Math.round(m_elementSize.height * m_faceSizeScale);
	}

	/**
	 * Get the unique ID of this instance
	 * 
	 * @return Unique Element ID
	 */
	public MapElementID getId()
	{
		return m_id;
	}

	/**
	 * Get the layer under which this element should be displayed
	 * 
	 * @return layer
	 */
	public Layer getLayer()
	{
		return m_layer;
	}

	/**
	 * Gets the map element from which this instance has been created
	 * 
	 * @return MapElement
	 */
	public MapElementTypeIF getMapElementType()
	{
		return m_mapElementType;
	}

	/**
	 * Return the element's display name
	 * 
	 * @return display name
	 */
	public String getName()
	{
		return m_name;
	}

	/**
	 * Return the element's normalized name. Used for internal representation, such as when saving to disk.
	 * 
	 * @return normalized name
	 */
	public String getNormalizedName()
	{
		return m_nameNormalized;
	}

	/**
	 * Get the position of this element on the map
	 * 
	 * @return map coordinates
	 */
	public MapCoordinates getPosition()
	{
		return m_position;
	}

	/**
	 * Get the renderer for this map element instance
	 * 
	 * @return renderer instance
	 */
	public MapElementRendererIF getRenderer()
	{
		if (m_renderer == null)
			m_renderer = new MapElementRenderer(this);

		return m_renderer;
	}

	/**
	 * Get the width of this map element, in map units
	 * 
	 * @return map units
	 */
	public int getWidth()
	{
		if (m_faceSizeScale == 1f)
			return m_elementSize.width;

		return Math.round(m_elementSize.width * m_faceSizeScale);
	}

	/**
	 * Verifies if this element has any attributes defined
	 * 
	 * @return True if attributes are defined for this element
	 */
	public boolean hasAttributes()
	{
		return !m_attributes.isEmpty();
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return m_id.hashCode();
	}

	/**
	 * @return true if this element has been marked as corrupted and should be bypassed
	 */
	public boolean isCorrupted()
	{
		return m_corrupted;
	}

	/**
	 * Verifies that a given element type is valid for this instance. Useful to check before calling
	 * {@link #setElementType(MapElementTypeIF)}
	 * 
	 * @param parent MapElement to test
	 * @return True if valid
	 */
	private boolean isValidParent(MapElementTypeIF parent)
	{
		return true;
		//return m_mapElement.getClass().equals(parent.getClass());
	}

	/**
	 * Remove an attribute from the element instance
	 * 
	 * @param name
	 */
	public void removeAttribute(final String name)
	{
		final String normalizedName = UtilityFunctions.normalizeName(name);
		m_attributes.remove(normalizedName);
	}

	/**
	 * Set the display angle for this element
	 * 
	 * @param angle Angle, in degrees
	 */
	public void setAngle(final double angle)
	{
		m_angle = angle;
		reinitializeHitMap();
	}

	/**
	 * Set angle and flips in one single call
	 * 
	 * @param angle rotation angle in degrees
	 * @param flipH true to flip horizontally
	 * @param flipV false to flip vertically
	 */
	public void setAngleFlip(final double angle, final boolean flipH, final boolean flipV)
	{
		m_flipH = flipH;
		m_flipV = flipV;
		m_angle = angle;
		reinitializeHitMap();
	}

	/**
	 * Set the value of a given attribute for this element. If the attribute does not exist, it is created
	 * 
	 * @param name Description of the attribute
	 * @param value Value for the attribute
	 * 
	 * @revise Attributes should not be addressed by display name. this will most likely change when we implement with the
	 *         properties package.
	 */
	public void setAttribute(final String name, final String value)
	{
		final String normalizedName = UtilityFunctions.normalizeName(name);
		m_attributes.put(normalizedName, new Attribute(name, value));
		displayPogDataChange();
	}

	/**
	 * Set the number of tiles taken by a side of this element. The element image will be automatically rescaled to fit
	 * the required number of tiles.
	 * 
	 * @param faceSize Number of tiles.
	 */
	public void setFaceSize(final float faceSize)
	{
		if (faceSize == m_faceSize)
			return;
		
		if (faceSize <= 0)
		{
			if (m_faceSizeScale != 1)
			{
				m_faceSizeScale = 1;
				reinitializeHitMap();
			}

			return;
		}

		final float targetDimension = GameTableMap.getBaseSquareSize() * faceSize;

		float maxDimension = GameTableMap.getBaseSquareSize();

		Image image = m_mapElementType.getImage();

		if (image != null)
			maxDimension = Math.max(image.getWidth(null), image.getHeight(null));

		if (maxDimension == 0)
			throw new ArithmeticException("Zero sized pog dimension: " + this);

		m_faceSizeScale = targetDimension / maxDimension;
		m_faceSize = faceSize;
		reinitializeHitMap();
	}

	/**
	 * Set both flip settings in one single call
	 * 
	 * @param flipH true to flip horizontally
	 * @param flipV false to flip vertically
	 */
	public void setFlip(final boolean flipH, final boolean flipV)
	{
		m_flipH = flipH;
		m_flipV = flipV;
		reinitializeHitMap();
	}

	/**
	 * Set the element's assigned layer
	 * 
	 * @param layer layer to chagne this element to
	 */
	public void setLayer(final Layer layer)
	{
		m_layer = layer;
	}

	/**
	 * Change the instance's element type - effectively changing this element's picture
	 * 
	 * @param newParent New parent element. Must be of same class as current parent.
	 */
	public void setElementType(MapElementTypeIF newParent)
	{
		if (!isValidParent(newParent))
			throw new IllegalArgumentException("Invalid parent - cannot replace " + m_mapElementType.getClass().getName() + " by "
					+ newParent.getClass().getName());

		m_mapElementType = newParent;

		reinitializeHitMap();
	}

	/**
	 * change the name of this element
	 * 
	 * @param name New name
	 */
	public void setName(String name)
	{
		m_name = name;
		m_nameNormalized = UtilityFunctions.normalizeName(m_name);

		// @revise trigger listener
	}

	/**
	 * Change the position of this element
	 * 
	 * @param pos
	 */
	public void setPosition(final MapCoordinates pos)
	{
		m_position = pos;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "[" + getId() + ":" + getName() + " pos: " + getPosition() + " face-size: " + getFaceSize() + "]";
	}

	/**
	 * Write a packet to output stream
	 * 
	 * @param dos
	 * @throws IOException
	 * 
	 *           TODO @revise Saves to binary file format. Change this for an XML file format. Keep in mind that we want
	 *           to add export in gt2 as well
	 */
	public void writeToPacket(final DataOutputStream dos) throws IOException
	{
		dos.writeUTF(getMapElementType().getFullyQualifiedName());
		
		dos.writeInt(m_position.x);
		dos.writeInt(m_position.y);
		dos.writeInt(getMapElementType().getFaceSize());	// why? we have an internal float....
		dos.writeLong(m_id.numeric());
		dos.writeUTF(m_name);
		dos.writeFloat(m_faceSizeScale);
		dos.writeDouble(m_angle);
		dos.writeBoolean(m_flipH);
		dos.writeBoolean(m_flipV);
	
		dos.writeInt(m_attributes.size());

		for (Attribute attribute : m_attributes.values())
		{
			dos.writeUTF(attribute.name);
			dos.writeUTF(attribute.value);
		}

		dos.writeInt(m_layer.ordinal());
	}

	/**
	 * Called to turn own display the attributes that have changed
	 * 
	 * @revise trigger listeners instead - the view should pop the information overlay, not the model
	 */
	private void displayPogDataChange()
	{
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

	/**
	 * Gets the visibility mapping of this element. Visibility mapping is a bit field raster array. All coordinates
	 * corresponding to a 'true' value are part of the element. The others are transparent pixels and considered outside
	 * of the element.
	 * 
	 * @return BitSet object
	 */
	private BitSet getHitMap()
	{
		if (m_hitMap != null)
			return m_hitMap;

		reinitializeHitMap();
		return m_hitMap;
	}

	/**
	 * Rebuild the hit map
	 */
	private void reinitializeHitMap()
	{
		m_hitMap = null;

		updateElementDimension();

		final Image img = m_mapElementType.getImage();
		if (img == null)
		{
			return;
		}

		int width = getWidth();
		int height = getHeight();

		if (width <= 0 || height < 0)
		{
			return;
		}

		// Create a buffer to receive current element representation
		BufferedImage bufferedImage = Images.createBufferedImage(width, height);

		final Graphics2D g = bufferedImage.createGraphics();

		// Clean the background with a 'marker' color
		g.setColor(new Color(0xff00ff));
		g.fillRect(0, 0, width, height);

		// render at origin, with no scaling
		getRenderer().drawToCanvas(g, null);

		g.dispose();

		final DataBuffer buffer = bufferedImage.getData().getDataBuffer();

		final int len = width * height;

		// Traverse the buffer and set all found marker colors as transparent
		m_hitMap = new BitSet(len);
		m_hitMap.clear();
		for (int i = 0; i < len; ++i)
		{
			final int pixel = buffer.getElem(i) & 0xFFFFFF;
			m_hitMap.set(i, (pixel != 0xFF00FF));
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	private void stopDisplayPogDataChange()
	{
		m_bTextChangeNotifying = false;
		for (Attribute attribute : m_attributes.values())
		{
			attribute.changed = false;
		}
	}

	/**
	 * Recalculate the width and height of this ElementInstance based on its parent element image
	 */
	private void updateElementDimension()
	{
		Image image = m_mapElementType.getImage();

		if (m_mapElementType.getImage() == null)
		{
			m_elementSize.setSize(GameTableMap.getBaseSquareSize(), GameTableMap.getBaseSquareSize());
		}
		else
		{
			m_elementSize.setSize(image.getWidth(null), image.getHeight(null));
			// Images.getRotatedSquareSize(image.getWidth(null), image.getHeight(null), m_angle, m_elementSize);
		}
	}
	
	/*
   * @see com.galactanet.gametable.data.XMLSerializeIF#deserialize(org.w3c.dom.Element, com.galactanet.gametable.data.XMLSerializeConverter)
   */
  @Override
  public void deserialize(Element parent, XMLSerializeConverter converter)
  {
		// Use constructor instead
		throw new NotImplementedException();
	}
	
	/*
	 * @see com.galactanet.gametable.data.XMLSerializer#serialize(org.w3c.dom.Element)
	 */
	@Override
	public void serialize(Element parent)
	{
		Document doc = parent.getOwnerDocument();

		parent.appendChild(XMLUtils.createElementValue(doc, "id", String.valueOf(m_id.numeric())));
		parent.appendChild(XMLUtils.createElementValue(doc, "angle", String.valueOf(m_angle)));
		parent.appendChild(XMLUtils.createElementValue(doc, "name", m_name));
		parent.appendChild(XMLUtils.createElementValue(doc, "facesize", String.valueOf(m_faceSize)));
		parent.appendChild(XMLUtils.createElementValue(doc, "layer", m_layer == null ? "" : m_layer.name()));
		
		parent.appendChild(XMLUtils.createElementValue(doc, "type", m_mapElementType.getFullyQualifiedName()));
		
		Element el = doc.createElement("flip");
		el.setAttribute("h", m_flipH ? "true" : "false");		
		el.setAttribute("v", m_flipV ? "true" : "false");
		parent.appendChild(el);
		
		el = doc.createElement("pos");
		m_position.serialize(el);
		parent.appendChild(el);		
		
		Element values = doc.createElement("values");		
		for (Entry<String, Attribute> entry : m_attributes.entrySet())
		{
			Element value = doc.createElement("value");
			value.setAttribute("name", entry.getKey());
			
			value.appendChild(XMLUtils.createElementValue(doc, "value", entry.getValue().value));
			value.appendChild(XMLUtils.createElementValue(doc, "name", entry.getValue().name));
			
			values.appendChild(value);		
		}
		
		parent.appendChild(values);				
	}
}
