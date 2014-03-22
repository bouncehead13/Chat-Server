package src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class ChatServer extends RecursiveTask<Integer>
{
	private Integer port;
	private Map<String, String> clients;
	private ServerSocket server;
	private ExecutorService executor;
	private boolean verbose;
	
	public ChatServer(Integer p, boolean v)
	{
		port = p;
		verbose = v;
		executor = Executors.newFixedThreadPool(32);
		clients = new HashMap<String, String>();
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
				executor.submit(new Client(connection, verbose, this));
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
		String ip = s.getInetAddress().getHostAddress();
		if(!clients.containsKey(userid) && !clients.containsValue(ip))
		{
			clients.put(userid, ip);
			return true;
		}
		else
		   return false;
	}
}