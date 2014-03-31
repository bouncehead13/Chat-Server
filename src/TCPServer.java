package src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class TCPServer implements Runnable
{
	private Integer port;
	private ServerSocket server;
	private ExecutorService executor;
	private boolean verbose;
	private ChatServer chat;
	
	public TCPServer(Integer p, boolean v, ChatServer s)
	{
		port = p;
		verbose = v;
		chat = s;
		executor = Executors.newCachedThreadPool();
	}

	/* main while loop to accept commands */
	public void run()
	{
		try
		{
			server = new ServerSocket(port, 5);
			while(true)
			{
				Socket connection = server.accept();
				executor.submit(new TCPClient(connection, verbose, chat));
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
}