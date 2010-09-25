/*
 * NetworkThread.java: GameTable is in the Public Domain.
 */


package com.plugins.network;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import com.galactanet.gametable.data.Player;
import com.galactanet.gametable.net.NetworkConnectionIF;
import com.galactanet.gametable.ui.GametableFrame;
import com.galactanet.gametable.util.Log;
import com.galactanet.gametable.util.UtilityFunctions;



/**
 * Multiplexed network thread for all the network stuff.
 * 
 * @author iffy
 * 
 * #GT-AUDIT NetworkThread
 */
public class NetworkThread extends Thread
{
	/**
	 * Encapsulation for a packet as it moves through the system.
	 * 
	 * @author iffy
	 * 
	 * @audited themaze75
	 */
	protected class Packet implements Runnable
	{
		/**
		 * Network connection source
		 */
		private final NetworkConnectionIF	m_connectionSource;

		/**
		 * Packet data
		 */
		private final byte[]							m_data;

		/**
		 * Constructor
		 * 
		 * @param data Packet data
		 * @param connectionSource Network connection source
		 */
		public Packet(final byte[] data, final NetworkConnectionIF connectionSource)
		{
			m_data = data;
			m_connectionSource = connectionSource;
		}

		/**
		 * @return Returns the packet's data.
		 */
		public byte[] getData()
		{
			return m_data;
		}

		/**
		 * @return Returns the packet's source connection
		 */
		public NetworkConnectionIF getSourceConnection()
		{
			return m_connectionSource;
		}

		/*
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			((NetworkModule) m_connectionSource.getNetworkModule()).processPacket(getSourceConnection(), getData());
		}
		
	}
	
	/**
	 * Private command class to set the interest ops between selections.
	 * 
	 * @author iffy
	 */
	private class MarkForWriting implements Runnable
	{
		private final PeerNetworkConnection connection;

		public MarkForWriting(final PeerNetworkConnection c)
		{
			connection = c;
		}

		public void run()
		{
			try
			{
				connection.getKey().interestOps(connection.getKey().interestOps() | SelectionKey.OP_WRITE);
			}
			catch (final Throwable t)
			{
				Log.log(Log.NET, t);
			}
		}
	}

	/**
	 * Private command class to set the interest ops between selections.
	 * 
	 * @author iffy
	 */
	private class RegisterPeerNetworkConnection implements Runnable
	{
		private final PeerNetworkConnection connection;
		private final int        interestOps;

		/**
		 * Constructor
		 * @param c
		 * @param ops
		 */
		public RegisterPeerNetworkConnection(final PeerNetworkConnection c, final int ops)
		{
			connection = c;
			interestOps = ops;
		}

		/*
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				connection.register(NetworkThread.this, interestOps);
			}
			catch (final Throwable t)
			{
				Log.log(Log.NET, t);
			}
		}
	}

	private final Set<PeerNetworkConnection>           m_connections     = new HashSet<PeerNetworkConnection>();
	private final Set<PeerNetworkConnection>           lostNetworkConnections = new HashSet<PeerNetworkConnection>();
	private final List<Runnable>          pendingCommands = new LinkedList<Runnable>();


	private Selector            selector;
	private final int           serverPort;
	private ServerSocketChannel serverSocketChannel;
	private boolean             startServer     = false;
	private final NetworkModule m_networkModule;

	/**
	 * Client Constructor.
	 */
	protected NetworkThread(NetworkModule module)
	{
		super(NetworkThread.class.getName());
		m_frame = GametableFrame.getGametableFrame();
		m_networkModule = module;
		setPriority(NORM_PRIORITY + 1);
		serverPort = -1;
		startServer = false;
	}

	/**
	 * Server Constructor.
	 */
	public NetworkThread(NetworkModule module, final int port)
	{
		super(NetworkThread.class.getName());
		m_frame = GametableFrame.getGametableFrame();
		m_networkModule = module;
		
		setPriority(NORM_PRIORITY + 1);
		serverPort = port;
		startServer = true;
	}

	/**
	 * Add a network connection to be monitored by the thread
	 * @param connection
	 */
	protected void add(final PeerNetworkConnection connection)
	{
		boolean connected = false;
		try
		{
			connected = connection.getChannel().finishConnect();
		}
		catch (final IOException ioe)
		{
			Log.log(Log.NET, ioe);
		}

		if (connected)
		{
			connection.markConnected();
			connection.markLoggedIn();
			add(connection, SelectionKey.OP_READ);
		}
		else
		{
			add(connection, SelectionKey.OP_CONNECT);
		}
	}

	/**
	 * Add a connection to monitor 
	 * @param connection
	 * @param ops
	 */
	private void add(final PeerNetworkConnection connection, final int ops)
	{
		synchronized (m_connections)
		{
			if (m_firstConnection == null)
				m_firstConnection = connection;
			
			m_connections.add(connection);
			synchronized (pendingCommands)
			{
				pendingCommands.add(new RegisterPeerNetworkConnection(connection, ops));
			}
		}

		if (selector != null)
		{
			selector.wakeup();
		}
	}

	public void closeAllConnections()
	{
		try
		{
			synchronized (m_connections)
			{				
				for (PeerNetworkConnection connection : m_connections)
					connection.close();

				m_connections.clear();
				m_firstConnection = null;
			}

			if (selector != null)
			{
				selector.close();
				selector = null;
			}

			if (serverSocketChannel != null)
			{
				serverSocketChannel.close();
				serverSocketChannel = null;
			}
		}
		catch (final Throwable t)
		{
			Log.log(Log.NET, t);
		}
	}

	private void cullLostNetworkConnections()
	{
		final Set<PeerNetworkConnection> lost = new HashSet<PeerNetworkConnection>();
		synchronized (m_connections)
		{
			for (PeerNetworkConnection connection : m_connections)
			{
				if (connection.isDead())
					lost.add(connection);
			}
		}

		for (PeerNetworkConnection connection : lost)
			remove(connection);
	}

	public Set<PeerNetworkConnection> getNetworkConnections()
	{
		final Set<PeerNetworkConnection> retVal = new HashSet<PeerNetworkConnection>();
		synchronized (m_connections)
		{
			retVal.addAll(m_connections);
			m_connections.clear();
		}

		return retVal;
	}

	public Set<PeerNetworkConnection> getLostConnections()
	{
		cullLostNetworkConnections();

		Set<PeerNetworkConnection> retVal = null;
		synchronized (lostNetworkConnections)
		{
			retVal = new HashSet<PeerNetworkConnection>(lostNetworkConnections);
			lostNetworkConnections.clear();
			return retVal;
		}
	}

	public List<Packet> getPackets()
	{
		final List<Packet> retVal = new ArrayList<Packet>();
		synchronized (m_connections)
		{
			for (PeerNetworkConnection connection : m_connections)
			{
				while (connection.hasPackets())
				{
					final byte[] data = connection.receivePacket();
					if (data == null)
					{
						break;
					}

					retVal.add(new Packet(data, connection));
				}
			}
		}

		return retVal;
	}

	/**
	 * @return Returns this thread's selector.
	 */
	 public Selector getSelector()
	{
		return selector;
	}

	public void markForWriting(final PeerNetworkConnection c)
	{
		synchronized (pendingCommands)
		{
			pendingCommands.add(new MarkForWriting(c));
		}

		selector.wakeup();
	}

	public void remove(final PeerNetworkConnection connection)
	{
		connection.close();
		synchronized (m_connections)
		{
			m_connections.remove(connection);
			
			if (connection == m_firstConnection)
				m_firstConnection = null;
				
			synchronized (lostNetworkConnections)
			{
				lostNetworkConnections.add(connection);
			}
		}
	}

	/*
	 * @see java.lang.Thread#run()
	 */
	 public void run()
	{
		try
		{
			selector = Selector.open();

			while (true)
			{
				sleep(1);

				if (startServer)
				{
					serverSocketChannel = ServerSocketChannel.open();
					serverSocketChannel.configureBlocking(false);
					serverSocketChannel.socket().bind(new InetSocketAddress(serverPort));
					serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, this);
					startServer = false;
				}

				synchronized (pendingCommands)
				{
					while (pendingCommands.size() > 0)
					{
						final Runnable r = pendingCommands.remove(0);

						try
						{
							r.run();
						}
						catch (final Exception e)
						{
							Log.log(Log.NET, e);
						}
					}
				}

				if (selector.selectNow() == 0)
				{
					continue;
				}

				sleep(1);

				final Set<SelectionKey> keys = selector.selectedKeys();
				final Iterator<SelectionKey> keyIterator = keys.iterator();
				while (keyIterator.hasNext())
				{
					try
					{
						final SelectionKey key = keyIterator.next();

						if (key.isAcceptable())
						{
							final ServerSocketChannel keyChannel = (ServerSocketChannel)key.channel();
							final SocketChannel newChannel = keyChannel.accept();
							final PeerNetworkConnection connection = new PeerNetworkConnection(m_networkModule, newChannel);
							add(connection, SelectionKey.OP_READ);
						}

						if (key.isConnectable())
						{
							final SocketChannel keyChannel = (SocketChannel)key.channel();
							final PeerNetworkConnection connection = (PeerNetworkConnection)key.attachment();
							try
							{
								while (!keyChannel.finishConnect())
								{
									// keep going
								}
								key.interestOps(SelectionKey.OP_READ);
								connection.markConnected();
								connection.markLoggedIn();
							}
							catch (final IOException ioe)
							{
								Log.log(Log.NET, ioe);
								keyChannel.close();
								connection.close();
							}
						}

						if (key.isReadable())
						{
							final PeerNetworkConnection connection = (PeerNetworkConnection)key.attachment();
							try
							{
								connection.readFromNet();
							}
							catch (final IOException ioe)
							{
								// Channel was closed
								Log.log(Log.NET, ioe.getMessage());
								connection.close();
							}
						}
						
						if (key.isWritable())
						{
							final PeerNetworkConnection connection = (PeerNetworkConnection)key.attachment();
							try
							{
								connection.writeToNet();
							}
							catch (final CancelledKeyException cke)
							{
								// Connection has been closed.
							}
							catch (final IOException ioe)
							{
								Log.log(Log.NET, ioe);
								connection.close();
							}
						}
					}
					catch (final CancelledKeyException cke)
					{
						// Connection has been closed.
					}
					finally
					{
						keyIterator.remove();
					}
				}
			}
		}
		catch (ClosedSelectorException e)
		{
			// Connection closed
		}
		catch (final InterruptedException ie)
		{
			Log.log(Log.SYS, ie);
		}
		catch (final Throwable t)
		{
			Log.log(Log.SYS, t);
		}
		finally
		{
			closeAllConnections();
		}
	}

	 /**
	  * Broadcast message to all peers
	  * @param packet
	  */
	 public void sendBroadcast(final byte[] packet)
	 {
		 // Overwrite broadcast byte
		 packet[0] = 1;
		 
		 // Next int is message id (4 bytes)
		 // (skip)
		 
		 // Next int is player id
		 NetworkConnectionIF playerConnection = null;
		 
		 NetworkConnectionIF myConnection = m_frame.getMyPlayer().getConnection();

		 try
		 {
			 int playerID = UtilityFunctions.toInt(packet, 5);
			 Player player = m_frame.getPlayerByID(playerID);
			 if (player != null)
				 playerConnection = player.getConnection();
		 }
		 catch (EOFException e)
		 {
			 // This is a bug - data packet does not contain player info
			 throw new RuntimeException("Message does not contain sendng player information");
		 }
		 

		 synchronized (m_connections)
		 {
			 for (PeerNetworkConnection connection : m_connections)
			 {
				 if (connection.isLoggedIn())
				 {
					 // Skip source player to prevent causing loops, skip self to prevent useless networking
					 if (connection != playerConnection && connection != myConnection)
						 connection.sendPacket(packet);
				 }
			 }
		 }
	 }

	 /**
	  * Send a message to a particular peer
	  * @param packet
	  * @param connection
	  */
	 public void send(final byte[] packet, final PeerNetworkConnection connection)
	 {
		 connection.sendPacket(packet);
	 }
	 
	 /**
	  * Gametable Frame
	  */
	 private final GametableFrame m_frame;
	 
	 /**
	  * First connection that was added to the network.  When joining, this is the connection to the host
	  */
	 protected PeerNetworkConnection m_firstConnection = null;
}
