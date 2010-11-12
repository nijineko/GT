/*
 * NetworkModule.java
 *
 * @created 2010-09-05
 *
 * Copyright (C) 1999-2010 Open Source Game Table Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.plugins.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import com.galactanet.gametable.GametableApp;
import com.galactanet.gametable.data.GameTableCore;
import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.data.ChatEngineIF.MessageType;
import com.galactanet.gametable.net.*;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.PeriodicExecutorThread;
import com.maziade.props.XProperties;
import com.plugins.network.NetworkThread.Packet;

/**
 * Implements direct connection networking module
 *
 */
public class NetworkModule implements NetworkModuleIF
{
	private class DataPacket extends DataPacketStream
  {
  	/**
		 * Underlying byte array output stream
		 */
		private ByteArrayOutputStream m_baos;
		
		/**
		 * Constructor
		 * @param os output stream 
		 */
		private DataPacket(ByteArrayOutputStream os)
		{
			super(os);
			m_baos = os;
		}
		
		/*
		 * @see com.galactanet.gametable.data.net.DataPacketStream#toByteArray()
		 */
		@Override
		public byte[] toByteArray()
		{			
			return m_baos.toByteArray();
		}
  }

	public final static int				MONITOR_INTERVAL_MILLIS						= 1500;
	public final static int				PING_INTERVAL_MILLIS						= 20000;		
	
	/**
	 * Default IP address to connect to
	 */
	private static final String		DEFAULT_IP_ADDRESS = "localhost";

	/**
	 * Default network password
	 */
	private static final String		DEFAULT_PASSWORD					= "";

	/**
	 * Default port to use
	 */
	protected static final int DEFAULT_PORT = 6812;

	/**
   * Single instance network module
   */
  private static NetworkModule g_networkModule = null;
	
	/**
	 * Hard-coded configuration to send KeepAlive messages to keep network connections alive
	 */
	private final static boolean	SEND_PINGS	= true;
	
	/**
	 * Anything other than GameTableFrame should use the GameTableFrame's method of acquiring NetworkModule
	 * 
	 * @return Single instance network module
	 */
	public static NetworkModuleIF getNetworkModule()
	{
		if (g_networkModule == null)
			g_networkModule = new NetworkModule();
		
		return g_networkModule;
	}
	
	/**
   * Pointer to GameTable core
   */
  private final GameTableCore m_core;
		
	/**
	 * Last time a ping packet has been sent
	 */
	private long										m_lastPingTime						= 0;
	
	/**
	 * List of currently registered listeners
	 */
  private List<NetworkListenerIF> m_listeners = new ArrayList<NetworkListenerIF>();  

  /**
	 * Currently registered messages
	 */
  private Map<Integer, NetworkMessageTypeIF> m_messageTypes = new HashMap<Integer, NetworkMessageTypeIF>();
  
  /**
	 * Current networking status
	 */
	private NetworkStatus								m_networkStatus								= NetworkStatus.DISCONNECTED;
  
	/**
	 * Thread monitoring the network
	 */
	private volatile NetworkThread	m_networkThread;

	/**
   * ID to assign to the next registered message
   */
  private int m_nextMessageID = 4;
  
  /**
   * Hard-coded message ID
   */
  public static int MSG_REQUEST_DICTIONARY = 1;
  
  /**
   * Hard-coded message ID
   */
  public static int MSG_SEND_DICTIONARY = 2;  
  
  /**
   * Hard-coded message ID
   */
  public static int MSG_KEEP_ALIVE = 3;
	
	/**
	 * Session's password
	 */
	private String m_password = DEFAULT_PASSWORD;

	/**
	 * Thread processing messages
	 */
	private volatile PeriodicExecutorThread m_processorThread = null;

	/**
	 * Constructor
	 */
	private NetworkModule()
	{
		m_core = GametableApp.getCore();
		setCoreMessagesTypes(m_messageTypes);
	}
	
	/**
	 * Set the core messages types in a map 
	 * @param types Map to populate
	 */
	private void setCoreMessagesTypes(Map<Integer, NetworkMessageTypeIF> types)
	{
		// Hard coded messages
		NetworkMessageTypeIF type = NetRequestDictionary.getMessageType();
		type.setID(MSG_REQUEST_DICTIONARY);
  	types.put(type.getID(), type);
  	
  	type = NetSendDictionary.getMessageType();
		type.setID(MSG_SEND_DICTIONARY);
  	types.put(type.getID(), type);
  	
  	type = NetKeepAlive.getMessageType();
		type.setID(MSG_KEEP_ALIVE);
  	types.put(type.getID(), type);
	}

	/*
   * @see com.galactanet.gametable.data.net.NetworkModuleIF#addListener(com.galactanet.gametable.data.net.NetworkListenerIF)
   */
  @Override
  public void addListener(NetworkListenerIF listener)
  {
  	m_listeners.add(listener);
  }

	@Override
	public NetworkConnectionIF connect() throws IOException, IllegalStateException
	{
		if (m_networkThread != null)
		{
			if (getNetworkStatus() == NetworkStatus.HOSTING && getConnectAsHost())
			{
				throw new IllegalStateException("You are hosting. If you wish to join a game, disconnect first.");
			}
			
			if (getNetworkStatus() == NetworkStatus.CONNECTED) // we can't join if we are already connected
			{
				throw new IllegalStateException("You are already in a game. You must disconnect before joining another game.");
			}
		}

		
		if (getConnectAsHost())
		{		
			// Hosting
			m_networkThread = new NetworkThread(this, getPort());
			m_networkThread.start();
			
			m_networkStatus = NetworkStatus.HOSTING;
			
			synchronized (m_messageTypeHostMapping)
			{
				m_messageTypeHostMapping.clear();
				m_messageTypeHostMapping.putAll(m_messageTypes);
				rebuildReverseTypeHostMapping();
			}
			
			startProcessorThread();

			// Connection notification is sent once dictionary is received
//			for (NetworkListenerIF listener : m_listeners)
//				listener.connectionEstablished(null);
			
			final String message = "Hosting on port: " + getPort();
			
			m_core.sendMessageLocal(MessageType.SYSTEM, message);
			m_core.sendMessageLocal(MessageType.MECHANIC,
					"<a href=\"http://www.whatismyip.com\">" + 
					"Click here to see the IP address you are hosting on." + 
					"</a> (" + 
					"Making you click the link ensures you have control over your privacy." 
					+ ")");

			Log.log(Log.NET, message);
			
			for (NetworkListenerIF listener : m_listeners)
				listener.networkStatusChange(NetworkStatus.HOSTING);
			
			return null;
		}
		
		// Joining
		m_networkThread = new NetworkThread(this);
		m_networkThread.start();
		final PeerNetworkConnection conn = new PeerNetworkConnection(this, getIpAddress(), getPort());
		m_networkThread.add(conn);
		
		m_networkStatus = NetworkStatus.CONNECTED;
		
		synchronized (m_messageTypeHostMapping)
		{
			m_messageTypeHostMapping.clear();
			
			// Hard coded messages - dictionary will be rebuild by a message call
			setCoreMessagesTypes(m_messageTypeHostMapping);
			rebuildReverseTypeHostMapping();
		}

		startProcessorThread();
		
		conn.waitForConnection();
		
		// Request dictionary from host
		send(NetRequestDictionary.makePacket(), conn);
		
		return conn;
	}

	/*
   * @see com.galactanet.gametable.data.net.NetworkModuleIF#createDataPacketStream(int)
   */
  @Override
  public DataPacketStream createDataPacketStream(NetworkMessageTypeIF messageType) throws IOException
  {
  	DataPacket packet = new DataPacket(new ByteArrayOutputStream());
  	
  	// Broadcast byte (will be overwritten when sending a broadcast)
  	packet.writeByte(0);
  	
  	// Network Message
  	if (m_networkStatus == NetworkStatus.HOSTING)
  	{
  		int id = messageType.getID();
  		packet.writeInt(id);
  		
  		if (id <= 0)
  		{
	  		m_core.sendMessageLocal(MessageType.ALERT, "Creating an unregistered network message " + messageType.getName());
				Log.log(Log.NET, "Creating an unregistered network message " + messageType.getName());
  		}
  	}
  	else
  	{
  		Integer id = m_messageTypeHostReverseMapping.get(messageType);
  		if (id != null)
  		{
  			packet.writeInt(id);
  		}
  		else
  		{
  			// We're using a plugin that the host does not have.
  			packet.writeInt(0);
  			m_core.sendMessageLocal(MessageType.ALERT, "Host does not use " + messageType.getName());
  			Log.log(Log.NET, "Host does not use " + messageType.getName());
  		}
  	}

  	int playerID = m_core.getPlayerID();
  	packet.writeInt(playerID);

  	return packet;
  }


	/*
	 * @see com.galactanet.gametable.data.net.NetworkIF#disconnect()
	 */
	@Override
	public void disconnect()
	{
		m_networkStatus = NetworkStatus.DISCONNECTED;
		
		if (m_networkThread != null)
		{
			// stop the network thread
			m_networkThread.closeAllConnections();
			m_networkThread.interrupt();
			m_networkThread = null;
		}
		
		stopProcessorThread();
		
		for (NetworkListenerIF listener : m_listeners)
			listener.networkStatusChange(NetworkStatus.DISCONNECTED);
		
		for (NetworkListenerIF listener : m_listeners)
			listener.connectionEnded();
	}
	
	/**
	 * Checks if the configuration wants us to run as host
	 * @return
	 */
	public boolean getConnectAsHost()
	{
		return m_core.getProperties().getBooleanPropertyValue(PROP_CONNECT_AS_HOST);
	}
	
	/**
	 * @return The currently configured IP Address
	 */
	public String getIpAddress()
	{
		return m_core.getProperties().getTextPropertyValue(PROP_ID_ADDRESS);
	}
	
	/*
   * @see com.galactanet.gametable.data.net.NetworkModuleIF#getNetworkMessageName(byte[])
   */
	@Override
	public String getNetworkMessageName(final byte[] packet)
  {
      try
      {
          final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet));
          dis.readByte();	// broadcast byte
          int id = dis.readInt();
          
          NetworkMessageTypeIF type = m_messageTypeHostMapping.get(id);
          if (type != null)
          	return type.getName();
          
          return String.valueOf(id) + " NOT IN DICTIONARY";
      }
      catch (final IOException ioe)
      {
          return "ERROR";
      }
  }
	
	/**
	 * @return Message Type 
	 */
	protected Collection<NetworkMessageTypeIF> getRegisteredMessageTypes()
	{
		return m_messageTypes.values();
	}
	
	/**
	 * Get the network status from the network module
	 * @return network status
	 */
  @Override
	public NetworkStatus getNetworkStatus()
	{
		return m_networkStatus;		
	}
	
	/*
   * @see com.galactanet.gametable.data.net.NetworkModuleIF#getParametersPanel()
   */
  @Override
  public NetworkParametersPanel getParametersPanel()
  {
  	return new PeerNetworkParametersPanel(this);
  }
	
	/*
	 * @see com.galactanet.gametable.data.net.NetworkModuleIF#getPassword()
	 */
	@Override
	public String getPassword()
	{
		return m_password;
	}

	/**
	 * Gets the port through which to connect 
	 * @return
	 */
	public int getPort()
	{
		return m_core.getProperties().getNumberPropertyValue(PROP_PORT);
	}

	/*
	 * @see com.galactanet.gametable.data.net.NetworkIF#registerMessage(com.galactanet.gametable.data.net.NetworkMessageIF)
	 */
	@Override
	public void registerMessageType(NetworkMessageTypeIF message)
	{
  	message.setID(m_nextMessageID++);
  	m_messageTypes.put(message.getID(), message);
	}
	
	/*
   * @see com.galactanet.gametable.data.net.NetworkModuleIF#removeListener(com.galactanet.gametable.data.net.NetworkListenerIF)
   */
  @Override
  public boolean removeListener(NetworkListenerIF listener)
  {
  	return m_listeners.remove(listener);
  }
	
	/*
	 * @see com.galactanet.gametable.net.NetworkModuleIF#send(byte[], com.galactanet.gametable.net.NetworkConnectionIF)
	 */
	@Override
	public void send(final byte[] packet, final NetworkConnectionIF connection)
	{
		if (m_networkThread != null)
		{
			m_networkThread.send(packet, (PeerNetworkConnection)connection);
		}
	}
	
	/*
	 * @see com.galactanet.gametable.data.net.NetworkIF#send(byte[])
	 */
	@Override
	public void sendBroadcast(byte[] packet)
	{
		if (m_networkThread != null)
			m_networkThread.sendBroadcast(packet);
	}
  
	/**
	 * Configure the module to run as host
	 * @param connectAsHost
	 */
	public void setConnectAsHost(boolean connectAsHost)
	{
		m_core.getProperties().setBooleanPropertyValue(PROP_CONNECT_AS_HOST, connectAsHost);
	}
  
  /**
	 * Sets the hosts' ip Address
	 * @param ipAddress
	 */
	public void setIpAddress(String ipAddress)
	{
		m_core.getProperties().setTextPropertyValue(PROP_ID_ADDRESS, ipAddress);
	}
  
  /**
	 * Set the network connection password
	 * @return
	 */
	public void setPassword(String password)
	{
		m_password = password;
	}
  /**
	 * Sets the port through which we will connect
	 * @param port
	 */
	public void setPort(int port)
	{
		m_core.getProperties().setNumberPropertyValue(PROP_PORT, port);
	}
  
  /**
	 * A packet has been received from the network and must be processed
	 * @param connectionSource The origin connection of this packet
	 * @param packet packet to handle
	 */
	protected void processPacket(final NetworkConnectionIF connectionSource, final byte[] packet)
	{
		try
		{
			ByteArrayInputStream is = new ByteArrayInputStream(packet);
			final DataInputStream dis = new DataInputStream(is);
			
			// BROADCAST BYTE
			boolean broadcasted = dis.readByte() == 1;
			
			// MESSAGE TYPE INT
			int type = dis.readInt();
			NetworkMessageTypeIF message;
			
			if (m_networkStatus != NetworkStatus.HOSTING)
			{				
				synchronized(m_messageTypeHostMapping)
				{
					message = m_messageTypeHostMapping.get(type);
				}
				
				if (message == null)
				{
					m_core.sendMessageLocal(MessageType.ALERT, "Host uses missing unnamed network message (" + type + ")");
					Log.log(Log.NET, "Host uses missing unnamed network message (" + type + ")");
					type = 0;
				}
				else
				{
					type = message.getID();
				}
			}
			else
				message = m_messageTypes.get(type);
			
			// PLAYER ID INT
			int sourcePlayerID = dis.readInt();			
			Player sourcePlayer = m_core.getPlayer(sourcePlayerID);

			String name = getNetworkMessageName(type);
			Log.log(Log.NET, "Received: " + name + ", length = " + packet.length);
			
			if (message != null)
			{
				try
				{
					NetworkEvent event = new NetworkEvent(sourcePlayer, broadcasted, message);					
					message.processData(connectionSource, dis, event);			
					
					Player currentPlayer = m_core.getPlayer();
					
					if (broadcasted && sourcePlayer != currentPlayer && m_networkStatus == NetworkStatus.HOSTING)
						sendBroadcast(packet);
				}
				catch (IOException ex)
				{
					Log.log(Log.NET, ex);
				}
			}
		}
		catch (final IOException ex)
		{
			Log.log(Log.SYS, ex);
		}
	}

  /**
   * Get a name for a message ID
   * @param messageID
   * @return Name or UNKNOWN_ID
   */
  private String getNetworkMessageName(final int messageID)
  {
  	NetworkMessageTypeIF message = m_messageTypes.get(messageID);
  	
  	if (message == null)
  		return "UNKNOWN_ID";
  	
  	return message.getName();
  }
  
  /**
	 * Perform network monitoring
	 */
	private void monitorNetwork()
	{
		final NetworkThread thread = m_networkThread;
		if (thread != null)
		{
			final Set<PeerNetworkConnection> lostConnections = thread.getLostConnections();
			for (NetworkConnectionIF connection : lostConnections)
			{
				for (NetworkListenerIF listener : m_listeners)
				{
					listener.connectionDropped(connection);
				}
			}

			for (Packet packet : thread.getPackets())
			{
				SwingUtilities.invokeLater(packet);
			}

			if (SEND_PINGS)
			{
				long delay = System.currentTimeMillis() - m_lastPingTime;
				
				if (delay >= PING_INTERVAL_MILLIS)
				{
					if (m_networkThread.m_firstConnection != null)
						send(NetKeepAlive.makePacket(), m_networkThread.m_firstConnection);
					
					m_lastPingTime = System.currentTimeMillis();
				}
			}
		}
	}
  
  /**
	 * Start the network processing thread
	 */
	private void startProcessorThread()
	{
		m_processorThread = new PeriodicExecutorThread(MONITOR_INTERVAL_MILLIS, new Runnable() {			
			@Override
			public void run()
			{
				monitorNetwork();
			}
			}, "network-monitor");
		
		m_processorThread.start();
	}
  
  /**
	 * Stop the processor thread
	 */
	private void stopProcessorThread()
	{
		if (m_processorThread != null)
		{
			m_processorThread.interrupt();
			m_processorThread = null;
		}
	}

	/**
	 * Set the ID mapping to convert received message IDs from the host to/from our internal representation
	 * @param mapping Mapping
	 */
	protected void setMapping(Map<Integer, NetworkMessageTypeIF> mapping, NetworkConnectionIF conn)
	{
		synchronized (m_messageTypeHostMapping)
		{
			m_messageTypeHostMapping.clear();
			m_messageTypeHostMapping.putAll(mapping);

			rebuildReverseTypeHostMapping();
		}
		
		// We received dictionary - we consider ourselves connected, now
		for (NetworkListenerIF listener : m_listeners)
			listener.networkStatusChange(NetworkStatus.CONNECTED);
		
		for (NetworkListenerIF listener : m_listeners)
			listener.connectionEstablished(conn);
	}

	/**
	 * Rebuild reverse type mapping
	 */
	private void rebuildReverseTypeHostMapping()
	{
		// Build reverse mapping
		m_messageTypeHostReverseMapping.clear();
		
		for (Entry<Integer, NetworkMessageTypeIF> entry : m_messageTypeHostMapping.entrySet())
		{
			m_messageTypeHostReverseMapping.put(entry.getValue(), entry.getKey());
		}
	}

	/**
	 * Message IDs as mapped on the host network
	 */
	private final Map<Integer, NetworkMessageTypeIF> m_messageTypeHostMapping = new HashMap<Integer, NetworkMessageTypeIF>(); 
	
	/**
	 * Message IDs as mapped on the host network
	 */
	private final Map<NetworkMessageTypeIF, Integer> m_messageTypeHostReverseMapping = new HashMap<NetworkMessageTypeIF, Integer>();
	
	/*
	 * @see com.galactanet.gametable.net.NetworkModuleIF#onApplyProperties(com.maziade.props.XProperties)
	 */
	@Override
	public void onApplyProperties(XProperties properties)
	{
		// nothing to do
	}
	
	/*
	 * @see com.galactanet.gametable.net.NetworkModuleIF#onInitializeProperties(com.maziade.props.XProperties)
	 */
	@Override
	public void onInitializeProperties(XProperties properties)
	{
		properties.addTextProperty(PROP_ID_ADDRESS, 				DEFAULT_IP_ADDRESS, true, "network", -1);
		properties.addNumberProperty(PROP_PORT, 						DEFAULT_PORT, true, "network", -1);
//		properties.addTextProperty(PROP_PASSWORD, 					getPassword(), true, "network", -1);
		properties.addBooleanProperty(PROP_CONNECT_AS_HOST, false, true, "network", -1);		
	}

	/**
	 * IP Address property 
	 */
	public final static String PROP_ID_ADDRESS = NetworkModule.class.getName() + ".ip_address";
	
	/**
	 * Port property
	 */
	public final static String PROP_PORT = NetworkModule.class.getName() + ".port";
	
	/**
	 * Hosting property
	 */
	public final static String PROP_CONNECT_AS_HOST = NetworkModule.class.getName() + ".hosting";
	
	/*
	 * @see com.galactanet.gametable.net.NetworkModuleIF#onLoadPropertiesCompleted()
	 */
	@Override
	public void onLoadPropertiesCompleted()
	{
		// nothing to do
	}
	
	/*
	 * @see com.galactanet.gametable.net.NetworkModuleIF#onUpdateProperties(com.maziade.props.XProperties)
	 */
	@Override
	public void onUpdateProperties(XProperties properties)
	{
		// nothing to do
	}
}
