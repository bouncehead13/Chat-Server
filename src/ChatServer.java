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
			boolean chunked = false;
			String[] sentences = message.split("\n");
			
			/* determine if it is a chunkced message */
			if(sentences[1].startsWith("C"))
			{
				chunked = true;
			}
			
			if(verbose)
			{
				
				for(int i=0; i<sentences.length; i++)
					System.out.println("  " + sentences[i]);
			}
			
			Client c = clients.get(name);
			
			/* if chunked, only send to TCP connections */
			if(chunked)
			{
				if(c instanceof TCPClient)
				{
					c.sendData(message, fromUser);
				}
			}
			
			/* send to either TCP or UDP when not chunked */
			else
			{
				c.sendData(message, fromUser);
			}
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
	
	public String findClientName(String ip)
	{
		Iterator itr=clients.keySet().iterator();
		while(itr.hasNext())
		{
			String key = (String) itr.next();
			String value = clients.get(key).getIP();
			if(value.equals(ip))
				return key;
		}
		
		return "";
	}
	
	/* remove client from mapping */
	public Client removeClient(String userid)
	{
		return clients.remove(userid);
	}
	
	public void updateIP(String user, InetAddress ip)
	{
		findClient(user).changeIP(ip);
	}
}