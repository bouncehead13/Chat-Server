package src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

class ChatServer extends RecursiveAction
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
		executor = Executors.newCachedThreadPool();
		clients = new HashMap<String, String>();
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
				executor.submit(new Client(connection, this));
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	/* client wants to send a message to a specified user */
	public void sendMessageToUser(Client c, String message)
	{
		/* FINISH THIS SECTION */
		synchronized(this)
		{
			if(verbose)
			{
				
			}
		}
	}
	
	/* server sending message back to client */
	public void sendMessageToClient(Client c, String message)
	{
		try
		{
			c.getOutputStream().writeBytes(message + "\n");
			
			if(verbose)
			{
				String ip = c.getIP();
				System.out.print("SENT to " + ip);
				System.out.println(": " + message);
			}
		}
		catch(IOException ex)
		{
			c.closeConnection();
			System.out.println("sendMessageToClient() error");
			System.err.println(ex);
		}
	}
	
	/* read a message from the client */
	public void readMessageFromClient(Client c, String message)
	{
		message = message.trim();
		
		/* FINISH THIS SECTION */
		if(verbose)
		{
			
		}
	}
	
	/* read data from the client */
	public void readDataFromClient(Client c, String message)
	{
		message = message.trim();
		
		if(verbose)
		{
			String ip = c.getIP();
			System.out.print("RCVD from " + ip);
			System.out.println(": " + message);
		}
		
		/* check if they are trying to sign in */
		if(c.getSignin() && findString(message, "ME IS"))
		{
			if(message.length() == 5)
			{
				sendMessageToClient(c, "Please enter a username");
				return;
			}
			
			String username = (message.substring(6)).toLowerCase();
			if(addClient(username, c))
			{
				c.signinGood();
				sendMessageToClient(c, "OK");
			}
			else
			{
				sendMessageToClient(c, "ERROR: Bad username");
			}
		}
		/* must be signed in first */
		else
		{
			sendMessageToClient(c, "You must sign in first");
		}
	}
	
	/* check if username or ip address is in the map */
	private boolean addClient(String userid, Client c)
	{
		String ip = c.getIP();
		synchronized(this)
		{
			if(!clients.containsKey(userid) && !clients.containsValue(ip))
			{
				clients.put(userid, ip);
				return true;
			}
			else
				return false;
		}
	}
	
	/* check if the word is in the string 's' */
	private boolean findString(String s, String check)
	{
		Integer size = check.length();
		if(s.length() >= size && s.substring(0, size).equals(check))
			return true;
		else
			return false;
	}
}