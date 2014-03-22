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
	private boolean verbose;
	
	public ChatServer(Integer p, boolean v)
	{
		port = p;
		verbose = v;
		executor = Executors.newFixedThreadPool(32);
		clients = new HashMap<String, Integer>();
	}
	
	@Override
	protected Integer compute()
	{
		try
		{
			server = new ServerSocket(port, 5);
			while(true)
			{
				Socket connection = server.accept();
				executor.submit(new Client(connection, this));
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
		
		return 10;
	}
	
	boolean addClient(String userid, Socket s)
	{
		if(!clients.containsKey(userid) && !clients.containsValue(s.getPort()))
		{
			clients.put(userid, s.getPort());
			return true;
		}
		else
		   return false;
	}
}