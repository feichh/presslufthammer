package de.tuberlin.dima.presslufthammer.transport;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import de.tuberlin.dima.presslufthammer.query.Query;
import de.tuberlin.dima.presslufthammer.transport.messages.SimpleMessage;
import de.tuberlin.dima.presslufthammer.transport.messages.Type;
import de.tuberlin.dima.presslufthammer.util.ShutdownStopper;
import de.tuberlin.dima.presslufthammer.util.Stoppable;
import de.tuberlin.dima.presslufthammer.xml.DataSource;
import de.tuberlin.dima.presslufthammer.xml.DataSourcesReader;
import de.tuberlin.dima.presslufthammer.xml.DataSourcesReaderImpl;

/**
 * @author feichh
 * @author Aljoscha Krettek
 * 
 */
public class Coordinator extends ChannelNode implements Stoppable {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private int port;
	private ServerBootstrap bootstrap;
	private Channel acceptChannel;
	private ChannelGroup innerChannels = new DefaultChannelGroup();
	private ChannelGroup leafChannels = new DefaultChannelGroup();
	private ChannelGroup clientChannels = new DefaultChannelGroup();
	private final CoordinatorHandler handler = new CoordinatorHandler(this);
	private Channel rootChannel = null;
	private final Map<Byte, QueryHandler> queries = new HashMap<Byte, QueryHandler>();
	private Map<String, DataSource> dsMap;
	private byte priorQID = 0;

	public Coordinator(int port, String dataSources) {
		this.port = port;
		DataSourcesReader dsReader = new DataSourcesReaderImpl();
		try {
			dsMap = dsReader.readFromXML(dataSources);
			log.info("Read datasources from {}.", dataSources);
			log.info(dsMap.toString());
		} catch (Exception e) {
			log.warn("Error reading datasources from {}.", dataSources);
			if (dsMap == null) {
				dsMap = Maps.newHashMap();
			}
		}
	}

	public void start() {
		ChannelFactory factory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		bootstrap = new ServerBootstrap(factory);

		bootstrap.setPipelineFactory(new CoordinatorPipelineFac(this.handler));

		try {
			acceptChannel = bootstrap.bind(new InetSocketAddress(port));
		} catch (ChannelException ce) {
			Throwable cause = ce.getCause();
			log.error("Failed to start coordinator at port {}: {}.", port,
					cause.getMessage());
			return;
		}

		if (acceptChannel.isBound()) {
			Runtime.getRuntime().addShutdownHook(new ShutdownStopper(this));
			log.info("Coordinator launched at: " + port);
		} else {
			bootstrap.releaseExternalResources();
			log.error("Failed to start coordinator at port {}.", port);
		}

	}

	@Override
	public void stop() {
		log.info("Stopping coordinator.");
		handler.getOpenChannels().close().awaitUninterruptibly();
		bootstrap.releaseExternalResources();
		log.info("Coordinator stopped.");
	}

	@Override
	public void query(SimpleMessage query) {
		// TODO
		query(query, null);
	}

	public void query(SimpleMessage query, Channel client) {
		log.info("Received query({}) from client {}.", query,
				client.getLocalAddress());

		query.setPayload(getQueryBytes(query));

		if (isServing()) {
			if (rootChannel != null) {
				log.debug("Handing query to root node of our node tree.");
				// clientChans.add(client);// optional
				byte qid = nextQID();
				query.setQueryID(qid);
				queries.put(qid, new QueryHandler(1, query, client));
				rootChannel.write(query);

			} else {
				log.debug("Querying leafs directly.");
				byte qid = nextQID();
				query.setQueryID(qid);
				queries.put(qid, new QueryHandler(leafChannels.size(), query,
						client));
				for (Channel c : leafChannels) {
					c.write(query);
				}
			}

		} else {
			log.info("Query cannot be processed because we have no leafs.");
		}
	}

	private byte[] getQueryBytes(SimpleMessage query) {
		// TODO
		Query test = new Query("0:ALL:Document:0:ANYTHING:NOTHING");
		return test.toString().getBytes();
	}

	//
	// public void query(Query query, Channel client) {
	// log.info("Received query({}) from client {}.", query,
	// client.getLocalAddress());
	//
	// if (isServing()) {
	// if (rootChannel != null) {
	// log.debug("Handing query to root node of our node tree.");
	// // clientChans.add(client);// optional
	// byte qid = nextQID();
	// query.setId(qid);
	// queries.put(qid, new QueryHandler(1, query, client));
	// rootChannel.write(query);
	//
	// } else {
	// log.debug("Querying leafs directly.");
	// byte qid = nextQID();
	// query.setId(qid);
	// queries.put(qid, new QueryHandler(leafChannels.size(), query,
	// client));
	// for (Channel c : leafChannels) {
	// c.write(query);
	// }
	// }
	//
	// } else {
	// log.info("Query cannot be processed because we have no leafs.");
	// }
	// }

	/**
	 * Returns {@code true} if this coordinator is connected to at least one
	 * leaf.
	 */
	public boolean isServing() {
		return handler != null && !leafChannels.isEmpty();
	}

	public void addClient(Channel channel) {
		// TODO
		log.debug("Adding client channel: {}.", channel.getRemoteAddress());
		clientChannels.add(channel);
	}

	public void addInner(Channel channel) {
		// TODO
		log.info("Adding inner node: {}", channel.getRemoteAddress());
		innerChannels.add(channel);
		if (rootChannel == null) {
			log.debug("new root node connected.");
			rootChannel = channel;
			SimpleMessage rootInfo = getRootInfo();
			for (Channel chan : leafChannels) {
				chan.write(rootInfo);
			}
		}
	}

	public void addLeaf(Channel channel) {
		// TODO
		log.info("Adding leaf node: {}", channel.getRemoteAddress());
		leafChannels.add(channel);
		if (rootChannel != null) {
			channel.write(getRootInfo());
		}
	}

	private SimpleMessage getRootInfo() {
		// TODO
		Type type = Type.INFO;
		byte[] payload = rootChannel.getRemoteAddress().toString().getBytes();
		return new SimpleMessage(type, (byte) 0, payload);
	}

	public void removeChannel(Channel channel) {
		// TODO
		if (rootChannel == channel) {
			rootChannel = null;
		}
		channel.close();
		// log.debug( "" + openChannels.remove( channel));
	}

	public void handleResult(SimpleMessage resultMSG) {
		// TODO
		byte qid = resultMSG.getQueryID();
		QueryHandler qhand = queries.get(qid);
		if (qhand != null) {
			qhand.addPart(resultMSG);
		}
	}

	private byte nextQID() {
		return ++priorQID;
	}

	private static void printUsage() {
		System.out.println("Parameters:");
		System.out.println("port datasources");
	}

	public static void main(String[] args) {
		// Print usage if necessary.
		if (args.length < 2) {
			printUsage();
			return;
		}

		int port = Integer.parseInt(args[0]);
		String datasources = args[1];

		Coordinator coordinator = new Coordinator(port, datasources);
		coordinator.start();
	}
}
