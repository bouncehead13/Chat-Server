package src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class ChatServer extends RecursiveTask<Integer>
{
	private Integer port;
	private Map<String, Integer> clients;
	private ServerSocket server;
	private ExecutorService executor;
	
	public ChatServer(int p)
	{
		port = p;
		executor = Executors.newFixedThreadPool(32);
	}
	
	@Override
	protected Integer compute()
	{
		server = new ServerSocket(port, 5);
		while(true)
		{
			try
			{
				Socket connection = server.accept();
				executor.submit(new Client(connection));
			}
			catch( IOException ex )
			{
				System.err.println( ex );
			}
		}
		
		return 10;
	}
	
	boolean addClient(String userid, Socket s);
}