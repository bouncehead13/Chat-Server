package src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class UDPServer implements Runnable
{
	private Integer port;
	private ExecutorService executor;
	private boolean verbose;
	private ChatServer server;
	private DatagramSocket socket;

	public UDPServer(Integer p, boolean v, ChatServer s)
	{
		port = p;
		server = s;
		verbose = v;
		executor = Executors.newCachedThreadPool();
	}

	/* main while loop to accept commands */
	public void run()
	{
		try
		{
			socket = new DatagramSocket(port);
		}
		catch(SocketException ex)
		{
			System.err.println(ex);
		}
		
		try
		{
			while(true)
			{
				byte[] receiveData = new byte[1024];
				DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(packet);
				executor.submit(new UDPClient(packet, socket, verbose, server, new ArrayList<String>(), 0));
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
}