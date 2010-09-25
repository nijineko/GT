/*
 * Connection.java: GameTable is in the Public Domain.
 */

package com.plugins.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.galactanet.gametable.net.NetworkConnectionIF;
import com.galactanet.gametable.net.NetworkModuleIF;
import com.galactanet.gametable.util.Log;

/**
 * Represents a two-way network connection. The paradigm for this network stuff is to make operations as efficient as
 * possible for the application, even if that means sacrificing efficiency on the network. For example, the send and
 * receive buffers will always be kept "flipped" such that nothing need be done for the application to read or write to
 * the appropriate buffers.
 * 
 * @author iffy
 * 
 *         #GT-AUDIT Connection
 */
public class PeerNetworkConnection implements NetworkConnectionIF
{
	protected interface State
	{
		public int	CONNECTED						= 1;
		public int	FLUSHING						= 3;
		public int	LOGGED_IN						= 2;
		public int	PENDING_CONNECTION	= 0;
	}

	private static final int		DEFAULT_BUFFER_SIZE	= 1024;

	private final SocketChannel	channel;
	private SelectionKey				key;
	private final NetworkModule	m_networkModule;
	private final List<byte[]>	queue								= new LinkedList<byte[]>();
	private ByteBuffer					receiveBuffer;
	private ByteBuffer					sendBuffer;
	private int									state								= State.PENDING_CONNECTION;
	private NetworkThread				m_networkThread;

	/**
	 * Incoming Connection Constructor
	 */
	protected PeerNetworkConnection(NetworkModule module, final SocketChannel chan) throws IOException
	{
		m_networkModule = module;
		channel = chan;
		channel.configureBlocking(false);
		sendBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
		receiveBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Outgoing Connection Constructor
	 */
	protected PeerNetworkConnection(NetworkModule module, final String addr, final int port) throws IOException
	{
		m_networkModule = module;

		channel = SocketChannel.open(new InetSocketAddress(addr, port));
		if (channel.finishConnect())
		{
			markConnected();
		}
		channel.configureBlocking(false);
		sendBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
		receiveBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Closes this connection.
	 */
	public void close()
	{
		boolean term = false;
		synchronized (sendBuffer)
		{
			sendBuffer.flip();
			if (sendBuffer.hasRemaining())
			{
				state = State.FLUSHING;
				sendBuffer.compact();
			}
			else
			{
				term = true;
			}
		}

		if (term)
		{
			terminate();
		}
	}

	public void markLoggedIn()
	{
		if (state == State.CONNECTED)
		{
			state = State.LOGGED_IN;
		}
	}

	/**
	 * Waits until the connection is established
	 */
	public void waitForConnection()
	{
		while (!isConnected()) // this waits until the connection is established
		{
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
				// We've been interrupted - but we'll keep on waiting...
			}
		}
	}

	/**
	 * @return Returns the channel.
	 */
	protected SocketChannel getChannel()
	{
		return channel;
	}

	/**
	 * @return Returns the key.
	 */
	protected SelectionKey getKey()
	{
		return key;
	}

	/**
	 * @return
	 */
	protected boolean hasPackets()
	{
		synchronized (queue)
		{
			return queue.size() > 0;
		}
	}

	/**
	 * @return True if this connection is connected.
	 */
	protected boolean isConnected()
	{
		// System.out.println(this + " connected: " + channel.socket().isConnected());
		return channel.socket().isConnected() && (m_networkThread != null);
	}

	protected boolean isDead()
	{
		// System.out.println(this + " connected2: " + channel.socket().isConnected());
		return (!channel.socket().isConnected() && (state != State.PENDING_CONNECTION));
	}

	/**
	 * @return True if this connection is logged in.
	 */
	protected boolean isLoggedIn()
	{
		return isConnected() && (state == State.LOGGED_IN);
	}

	/**
	 * @return
	 */
	protected byte[] receivePacket()
	{
		synchronized (queue)
		{
			if (!hasPackets())
			{
				return null;
			}

			return queue.remove(0);
		}
	}

	/*
	 * @see com.galactanet.gametable.data.net.Connection#sendPacket(byte[])
	 */
	@Override
	public void sendPacket(final byte[] packet)
	{
		final int size = packet.length + 4;
		Log.log(Log.NET, "Sending : " + m_networkModule.getNetworkMessageName(packet) + ", length = " + packet.length);
		
		synchronized (sendBuffer)
		{
			if (sendBuffer.limit() - sendBuffer.position() <= size)
			{
				int newCapacity = sendBuffer.capacity();
				while (newCapacity - sendBuffer.position() <= size)
				{
					newCapacity *= 2;
				}
				final ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
				sendBuffer.flip();
				newBuffer.put(sendBuffer);
				sendBuffer = newBuffer;
			}

			try
			{
				sendBuffer.putInt(packet.length);
				sendBuffer.put(packet);
			}
			catch (final BufferOverflowException boe)
			{
				Log.log(Log.NET, boe);
			}

			m_networkThread.markForWriting(this);
		}
	}

	/**
	 * @return
	 */
	private byte[] readPacket()
	{
		synchronized (receiveBuffer)
		{
			receiveBuffer.flip();
			if (receiveBuffer.remaining() < 4)
			{
				receiveBuffer.compact();
				return null;
			}

			final int size = receiveBuffer.getInt(0);
			if (receiveBuffer.remaining() < size + 4)
			{
				receiveBuffer.compact();
				return null;
			}

			final byte[] retVal = new byte[size];
			receiveBuffer.getInt();
			receiveBuffer.get(retVal);
			receiveBuffer.compact();

			return retVal;
		}
	}

	private void terminate()
	{
		Log.log(Log.NET, "Connection.terminate();");
		try
		{
			key.cancel();
			channel.socket().close();
			channel.close();
		}
		catch (final IOException e)
		{
			Log.log(Log.NET, e);
		}
	}

	/**
	 * Mark this connection as connected
	 */
	protected void markConnected()
	{
		if (state == State.PENDING_CONNECTION)
		{
			state = State.CONNECTED;
		}
	}

	/**
	 * Reads as much as it can from the net without blocking.
	 * 
	 * @throws IOException
	 */
	protected void readFromNet() throws IOException
	{
		synchronized (receiveBuffer)
		{
			while (true)
			{
				if (receiveBuffer.limit() == receiveBuffer.position())
				{
					final ByteBuffer newBuffer = ByteBuffer.allocateDirect(receiveBuffer.capacity() * 2);
					receiveBuffer.flip();
					newBuffer.put(receiveBuffer);
					receiveBuffer = newBuffer;
				}

				final int count = channel.read(receiveBuffer);
				if (count < 1)
				{
					break;
				}
			}
		}

		while (true)
		{
			final byte[] packet = readPacket();
			if (packet == null)
			{
				break;
			}
			Log.log(Log.NET, "Read: " + m_networkModule.getNetworkMessageName(packet) + ", length = " + packet.length);

			queue.add(packet);
		}
	}

	/**
	 * Register the network connection in the network thread
	 * @param thread
	 * @param ops
	 * @throws ClosedChannelException
	 */
	protected void register(final NetworkThread thread, final int ops) throws ClosedChannelException
	{
		m_networkThread = thread;
		key = channel.register(m_networkThread.getSelector(), ops, this);
		if (isConnected())
		{
			markConnected();
			key.interestOps(ops & ~SelectionKey.OP_CONNECT);
		}
	}

	/**
	 * Writes as much to the net as it can without blocking.
	 * 
	 * @throws IOException
	 */
	protected void writeToNet() throws IOException
	{
		synchronized (sendBuffer)
		{
			sendBuffer.flip();
			while (sendBuffer.hasRemaining())
			{
				if (channel.write(sendBuffer) < 1)
				{
					break;
				}
			}

			if (!sendBuffer.hasRemaining())
			{
				key.interestOps(SelectionKey.OP_READ);
				if (state == State.FLUSHING)
				{
					terminate();
				}
			}

			sendBuffer.compact();
		}
	}
	
	/*
	 * @see com.galactanet.gametable.data.net.NetworkConnectionIF#getNetworkModule()
	 */
	@Override
	public NetworkModuleIF getNetworkModule()
	{
		return m_networkModule;
	}
}
