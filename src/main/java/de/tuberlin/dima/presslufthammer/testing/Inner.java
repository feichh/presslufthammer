/**
 * 
 */
package de.tuberlin.dima.presslufthammer.testing;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tuberlin.dima.presslufthammer.pressluft.Pressluft;

/**
 * @author feichh
 * 
 */
public class Inner extends ChannelNode
{
	private final Logger	log					= LoggerFactory.getLogger( getClass());

	ChannelGroup					openChannels	= new DefaultChannelGroup();
	ChannelGroup					childChannels	= new DefaultChannelGroup();
	Channel								coordChannel;

	/**
	 * @param coord
	 * @param cport
	 * @param port
	 * @throws InterruptedException if interrupted
	 */
	public Inner( InetAddress coord, int cport, int port) throws InterruptedException
	{
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory( Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// bootstrap.setPipelineFactory( new BasicPipelineFac());

		ChannelFuture connectFuture = bootstrap.connect( new InetSocketAddress(
				coord, cport));

		Channel channel = connectFuture.awaitUninterruptibly().getChannel();
		
		System.out.println(channel.isReadable() + " " + channel.isWritable());
		
		ChannelFuture writeFuture = channel.write( new Pressluft( de.tuberlin.dima.presslufthammer.pressluft.Type.REGINNER, new byte[] { (byte) 0,}));
		
		writeFuture.await();
//
//		channel.close().awaitUninterruptibly();
//		bootstrap.releaseExternalResources();
	}

	public static void main( String[] args) throws Exception
	{

		Inner in = new Inner( InetAddress.getByName( "localhost"), 44444,
				44440);

	}
}