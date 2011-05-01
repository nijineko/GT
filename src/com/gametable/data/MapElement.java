/*
 * Pog.java: GameTable is in the Public Domain.
 */

package com.gametable.data;

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

import com.gametable.data.MapElementTypeIF.Layer;
import com.gametable.net.NetworkEvent;
import com.gametable.ui.MapElementRendererIF;
import com.gametable.util.Images;
import com.gametable.util.Log;
import com.gametable.util.UtilityFunctions;
import com.maziade.tools.XMLUtils;

/**
 * Represents an instance of a MapElement on the Map.
 * 
 * @author sephalon
 * 
 * @audited by themaze75
 */
public class MapElement implements Comparable<MapElement>
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
		MapElementTypeIF type = MapElementTypeLibrary.getMasterLibrary().getMapElementType(fullyQualifiedTypeName);
		
		if (type == null)
		{
			type = MapElementTypeLibrary.getMasterLibrary().createPlaceholderType(fullyQualifiedTypeName, Math.max(1, (int)getFaceSize()));
		}
		
		m_mapElementType = type;
		
		setFaceSize(UtilityFunctions.parseFloat(XMLUtils.getFirstChildElementContent(parent, "facesize", "1"), 1f));

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
	 * Constructor (network communications only)
	 * 
	 * @param dis Data input stream
	 * @throws IOException
	 */
	public MapElement(final DataInputStream dis) throws IOException
	{
		final MapElementTypeLibrary lib = MapElementTypeLibrary.getMasterLibrary();
		String type_fqn = dis.readUTF();		
		
		// X, Y
		final int x = dis.readInt();
		final int y = dis.readInt();
		m_position = new MapCoordinates(x, y);
		
		// SIZE
		final int size = dis.readInt();

		// ID
		long id = dis.readLong();
		
		// If ID is already in use by another MapElement, we need to reassign it
		MapElementID existingID = MapElementID.get(id);
		if (existingID != null)
			existingID.reassignInternalID();
		
		m_id = MapElementID.fromNumeric(id);

		// NAME
		setName(dis.readUTF());
 
		// SCALE
		try
		{
			m_faceSizeScale = dis.readFloat();
		}
		catch (IOException exp)
		{
			m_faceSizeScale = 1f;
		}

		// ANGLE
		try
		{
			m_angle = dis.readDouble();
		}
		catch (IOException exp)
		{
			m_angle = 0.;
		}
		
		// FLIPS
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
		
		// ATTRIBUTE COUNT AND VALUES
		int attributeCount = dis.readInt();
		for (int i = 0; i < attributeCount; i++)
		{
			String name = dis.readUTF();
			String value = dis.readUTF();
			
			setAttribute(name, value);
		}

		// LAYER TYPE
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

		// POST PROCESSING
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

		MapElementTypeIF type = lib.getMapElementType(type_fqn);
		if (type == null)
		{
			type = lib.createPlaceholderType(type_fqn, size);
		}

		m_mapElementType = type;
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

		return getID().compareTo(pog.getID());
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
		return pog.getID().equals(m_id);
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
	 * @return Value or null, if not found
	 */
	public String getAttribute(String name)
	{
		String normalizedName = UtilityFunctions.normalizeName(name);
		Attribute a = m_attributes.get(normalizedName);
		
		if (a == null)
			return null;
		
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
	public MapElementID getID()
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
	 * {@link #setMapElementType(MapElementTypeIF)}
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
	 * @param name Name of the attribute to remove
	 */
	public void removeAttribute(final String name)
	{
		removeAttribute(name, null);
	}

	/**
	 * Remove an attribute from the element instance
	 * @param name Name of the attribute to remove
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void removeAttribute(final String name, NetworkEvent netEvent)
	{
		final String normalizedName = UtilityFunctions.normalizeName(name);
		m_attributes.remove(normalizedName);
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onAttributeChanged(this, name, null, null, false, netEvent);		
	}
	
	/**
	 * Remove multiple attributes from the element instance
	 * @param names list of attribute names to remove
	 */
	public void removeAttributes(List<String> names)
	{
		removeAttributes(names, null);
	}
	
	/**
	 * Remove multiple attributes from the element instance
	 * @param names list of attribute names to remove
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void removeAttributes(List<String> names, NetworkEvent netEvent)
	{
		Map<String, String> removeList = new HashMap<String, String>();
		
		for (String name : names)
		{
			removeList.put(name, null);
			
			final String normalizedName = UtilityFunctions.normalizeName(name);
			m_attributes.remove(normalizedName);
			
			for (MapElementListenerIF listener : m_listeners)
				listener.onAttributeChanged(this, name, null, null, true, netEvent);
		}		
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onAttributesChanged(this, removeList, netEvent);
	}
	
	/**
	 * Set the display angle for this element
	 * 
	 * @param angle Angle, in degrees
	 */
	public void setAngle(final double angle)
	{
		setAngle(angle, null);
	}

	/**
	 * Set the display angle for this element
	 * 
	 * @param angle Angle, in degrees
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void setAngle(final double angle, NetworkEvent netEvent)
	{
		m_angle = angle;
		reinitializeHitMap();
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onAngleChanged(this, netEvent);
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
		boolean angleChanged = m_angle != angle;
		boolean flipChanged = m_flipH != flipH || m_flipV != flipV;
		
		if (!angleChanged && !flipChanged)
			return;
		
		m_flipH = flipH;
		m_flipV = flipV;
		m_angle = angle;
		reinitializeHitMap();
		
		if (angleChanged)
		{
			for (MapElementListenerIF listener : m_listeners)
				listener.onAngleChanged(this, null);
		}
		
		if (flipChanged)
		{
			for (MapElementListenerIF listener : m_listeners)
				listener.onFlipChanged(this, null);			
		}

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
		setAttribute(name, value, null);
	}

	/**
	 * Set the value of a given attribute for this element. If the attribute does not exist, it is created
	 * 
	 * @param name Description of the attribute
	 * @param value Value for the attribute
	 * @param netEvent Network event that triggered the operation or null
	 * 
	 * @revise Attributes should not be addressed by display name. this will most likely change when we implement with the
	 *         properties package.
	 */
	public void setAttribute(final String name, final String value, NetworkEvent netEvent)
	{
		String old = getAttribute(name);
		
		final String normalizedName = UtilityFunctions.normalizeName(name);
		
		m_attributes.put(normalizedName, new Attribute(name, value));
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onAttributeChanged(this, name, value, old, false, netEvent);
	}
	
	/**
	 * Set the value of a given attribute for this element. If the attribute does not exist, it is created
	 * 
	 * @param attributes Map of attribute names + values
	 * 
	 * @revise Attributes should not be addressed by display name. this will most likely change when we implement with the
	 *         properties package.
	 */
	public void setAttributes(Map<String, String> attributes)
	{
		setAttributes(attributes, null);
	}
	
	/**
	 * Set the value of a given attribute for this element. If the attribute does not exist, it is created
	 * 
	 * @param attributes Map of attribute names + values
	 * @param netEvent Network event that triggered the operation or null
	 * 
	 * @revise Attributes should not be addressed by display name. this will most likely change when we implement with the
	 *         properties package.
	 */
	public void setAttributes(Map<String, String> attributes, NetworkEvent netEvent)
	{
		for (Entry<String, String> entry : attributes.entrySet())
		{
			Attribute attr = new Attribute(entry.getKey(), entry.getValue());
			
			String old = getAttribute(attr.name);
			final String normalizedName = UtilityFunctions.normalizeName(attr.name);			
			m_attributes.put(normalizedName, attr);
			
			for (MapElementListenerIF listener : m_listeners)
				listener.onAttributeChanged(this, attr.name, attr.value, old, true, netEvent);
		}
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onAttributesChanged(this, attributes, netEvent);
	}
	
	/**
	 * Set the number of tiles taken by a side of this element. The element image will be automatically rescaled to fit
	 * the required number of tiles.
	 * 
	 * @param faceSize Number of tiles.
	 */
	public void setFaceSize(final float faceSize)
	{
		setFaceSize(faceSize, null);
	}

	/**
	 * Set the number of tiles taken by a side of this element. The element image will be automatically rescaled to fit
	 * the required number of tiles.
	 * 
	 * @param faceSize Number of tiles. If <=0, will reset to its default size
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void setFaceSize(final float faceSize, NetworkEvent netEvent)
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

		final float targetDimension = GameTableMap.getBaseTileSize() * faceSize;

		float maxDimension = GameTableMap.getBaseTileSize();

		Image image = m_mapElementType.getImage();

		if (image != null)
			maxDimension = Math.max(image.getWidth(null), image.getHeight(null));

		if (maxDimension == 0)
			throw new ArithmeticException("Zero sized pog dimension: " + this);

		m_faceSizeScale = targetDimension / maxDimension;
		m_faceSize = faceSize;
		
		reinitializeHitMap();
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onFaceSizeChanged(this, netEvent); 
	}
	
	/**
	 * Set both flip settings in one single call
	 * 
	 * @param flipH true to flip horizontally
	 * @param flipV false to flip vertically
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void setFlip(final boolean flipH, final boolean flipV, NetworkEvent netEvent)
	{
		m_flipH = flipH;
		m_flipV = flipV;
		reinitializeHitMap();
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onFlipChanged(this, netEvent);
	}

	/**
	 * Set both flip settings in one single call
	 * 
	 * @param flipH true to flip horizontally
	 * @param flipV false to flip vertically
	 */
	public void setFlip(final boolean flipH, final boolean flipV)
	{
		setFlip(flipH, flipV, null);
	}
	
	/**
	 * Set the element's assigned layer
	 * 
	 * @param layer Layer to change this element to
	 */
	public void setLayer(final Layer layer)
	{
		setLayer(layer, null);
	}

	/**
	 * Set the element's assigned layer
	 * 
	 * @param layer Layer to change this element to
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void setLayer(final Layer layer, NetworkEvent netEvent)
	{
		if (layer == m_layer)
			return;
		
		Layer old = layer;
		m_layer = layer;

		for (MapElementListenerIF listener : m_listeners)
			listener.onLayerChanged(this, layer, old, netEvent);
	}
	
	/**
	 * Change the instance's element type - effectively changing this element's picture
	 * 
	 * @param elementType New element type. Must be of same class as current element type.
	 */
	public void setMapElementType(MapElementTypeIF elementType)
	{
		setMapElementType(elementType, null);		
	}

	/**
	 * Change the instance's element type - effectively changing this element's picture
	 * 
	 * @param elementType New element type. Must be of same class as current element type.
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void setMapElementType(MapElementTypeIF elementType, NetworkEvent netEvent)
	{
		if (elementType == null)
			throw new IllegalArgumentException("Cannot set null MapElementType");
		
		if (elementType == m_mapElementType)
			return;
		
		if (!isValidParent(elementType))
			throw new IllegalArgumentException("Invalid parent - cannot replace " + m_mapElementType.getClass().getName() + " by "
					+ elementType.getClass().getName());		
		
		m_mapElementType = elementType;

		reinitializeHitMap();
		
		for (MapElementListenerIF listener : m_listeners)
			listener.onElementTypeChanged(this, netEvent);
	}
	
	/**
	 * Set the display name of this element
	 * 
	 * @param name New name
	 */
	public void setName(String name)
	{
		setName(name, null);
	}

	/**
	 * Set the display name of this element
	 * 
	 * @param name New name
	 * @param netEvent Network event that triggered the operation or null
	 */
	public void setName(String name, NetworkEvent netEvent)
	{
		String old = m_name;
		m_name = name;
		m_nameNormalized = UtilityFunctions.normalizeName(m_name);

		for (MapElementListenerIF listener : m_listeners)
			listener.onNameChanged(this, name, old, netEvent);
	}
	
	/**
	 * Change the position of this element
	 * 
	 * @param pos Map position
	 */
	public void setPosition(final MapCoordinates pos)
	{
		setPosition(pos, null);
	}

	/**
	 * Change the position of this element
	 * 
	 * @param pos Map position
	 * @param netEvent Network event detail, if the change has been triggered by a network call
	 */
	public void setPosition(final MapCoordinates pos, NetworkEvent netEvent)
	{
		if (pos == null)
			throw new IllegalArgumentException("Cannot set null position for a MapElement");
		
		if (!m_position.equals(pos))
		{
			MapCoordinates old = m_position;
			m_position = pos;
			
			for (MapElementListenerIF listener : m_listeners)
				listener.onPositionChanged(this, m_position, old, netEvent);
		}
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "[" + getID() + ":" + getName() + " pos: " + getPosition() + " face-size: " + getFaceSize() + "]";
	}

	/**
	 * Write a packet to output stream
	 * 
	 * @param dos
	 * @throws IOException
	 */
	public void writeToPacket(final DataOutputStream dos) throws IOException
	{
		dos.writeUTF(getMapElementType().getFullyQualifiedName());
		
		// X, Y
		dos.writeInt(m_position.x);
		dos.writeInt(m_position.y);
		
		// FACE
		dos.writeInt(getMapElementType().getFaceSize());	// why? we have an internal float....
		
		// ID
		dos.writeLong(m_id.numeric());
		
		// NAME
		dos.writeUTF(m_name);
		
		// SCALE, ANGLE & FLIPS
		dos.writeFloat(m_faceSizeScale);
		dos.writeDouble(m_angle);
		dos.writeBoolean(m_flipH);
		dos.writeBoolean(m_flipV);
	
		// ATTRIBUTE COUNT
		dos.writeInt(m_attributes.size());

		// ATTRIBUTES
		for (Attribute attribute : m_attributes.values())
		{
			dos.writeUTF(attribute.name);
			dos.writeUTF(attribute.value);
		}

		// LAYER TYPE
		dos.writeInt(m_layer.ordinal());
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
			m_elementSize.setSize(GameTableMap.getBaseTileSize(), GameTableMap.getBaseTileSize());
		}
		else
		{
			m_elementSize.setSize(image.getWidth(null), image.getHeight(null));
			// Images.getRotatedSquareSize(image.getWidth(null), image.getHeight(null), m_angle, m_elementSize);
		}
	}
	
  /**
	 * Store information from your component from inside parent element 
	 * @param parent Parent element, as populated by calling thread.  You can add custom XML data as children.
	 */
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
	
	/**
   * Adds a MapElementListenerIF to this element
   * @param listener Listener to call when something changes within the map
   */
  public void addListener(MapElementListenerIF listener)
  {
  	m_listeners.remove(listener);
  	m_listeners.add(listener);
  }
  
  /**
   * Removes a listener from this element
   * @param listener Listener to remove
   * @return True if listener was found and removed
   */
  public boolean removeListener(MapElementListenerIF listener)
  {
  	return m_listeners.remove(listener);
  }

  /**
   * List of map element listeners
   */
  private List<MapElementListenerIF> m_listeners = new ArrayList<MapElementListenerIF>();
}
