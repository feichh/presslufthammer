/**
 * 
 */
package de.tuberlin.dima.presslufthammer.testing;

import java.io.Closeable;
import java.io.IOException;

import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * @author feichh
 *
 */
public abstract class ChannelNode implements Closeable
{
	
	/**
	 * All connected/open netty channels;
	 */
	protected ChannelGroup openChannels	= new DefaultChannelGroup();

	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException
	{
		openChannels.close().awaitUninterruptibly();
	}

}