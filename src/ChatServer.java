package src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class ChatServer extends RecursiveAction
{
	private Integer port;
	private ServerSocket server;
	private ExecutorService executor;
	private Hashtable<String, Client> clients;
	private boolean verbose;
	
	public ChatServer(Integer p, boolean v)
	{
		port = p;
		verbose = v;
		executor = Executors.newCachedThreadPool();
		clients = new Hashtable<String, Client>();
	}
	
	@Override
	protected void compute()
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
	}
	
	/* server sending message back to client */
	public void sendMessageToClient(String name, String size, String message)
	{
		/* FINISH THIS SECTION */
		synchronized(this)
		{
			if(verbose)
			{
				System.out.println("  " + size);
				System.out.println("  " + message);
			}
			
			Client c = clients.get(name);
			c.sendData(message);
		}
	}
	
	/* check if username or ip address is in the map */
	/* FINISH THIS */
	public boolean addClient(String userid, Client c)
	{
		synchronized(this)
		{
			if(!clients.containsKey(userid))
			{
				Enumeration<Client> values = clients.elements();
				while (values.hasMoreElements())
				{
					if(c.getIP().equals(values.nextElement().getIP()))
					   return false;
				}
				
				clients.put(userid, c);
				return true;
			}
			else
				return false;
		}
	}
	
	public Client findClient(String userid)
	{
		Client c = clients.get(userid);
		return c;
	}
}
