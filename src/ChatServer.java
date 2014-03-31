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
	
	/* main while loop to accept new clients */
	@Override
	protected void compute()
	{
		executor.submit(new TCPServer(port, verbose, this));
		executor.submit(new UDPServer(port, verbose, this));
	}
	
	/* server sending message back to client */
	public void sendMessageToClient(String name, String message, String fromUser)
	{
		synchronized(this)
		{
			if(verbose)
			{
				String[] sentences = message.split("\n");
				for(int i=0; i<sentences.length; i++)
					System.out.println("  " + sentences[i]);
			}
			
			Client c = clients.get(name);
			if(c instanceof TCPClient)
				c.sendData(message, fromUser);
		}
	}
	
	/* check if username or ip address is in the map */
	public boolean addClient(String userid, Client c)
	{
		synchronized(this)
		{
			if(!clients.containsKey(userid))
			{				
				clients.put(userid, c);
				return true;
			}
			else
				return false;
		}
	}
	
	/* return a set of usernames */
	public Set<String> getClients()
	{
		return clients.keySet();
	}
	
	/* locate client in mapping */
	public Client findClient(String userid)
	{
		return clients.get(userid);
	}
	
	/* remove client from mapping */
	public void removeClient(String userid)
	{
		clients.remove(userid);
	}
}
