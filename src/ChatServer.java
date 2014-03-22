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
				executor.submit(new Client(connection, this));
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
		
		return 10;
	}
	
	/* client wants to send a message to a specified user */
	public void sendMessageToUser(Client c, String message)
	{
		synchronized(this)
		{
			
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
				System.out.print(": " + message + "\n");
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	/* read a message from the client */
	public void readMessageFromClient(Client c, String message)
	{
		/* check if they sign in */
		if(c.getSignin() && findString(message, "ME IS"))
		{
			if(message.length() == 5)
			{
				sendMessageToClient(c, "Please enter a username");
				return;
			}
			
			String username = (message.substring(6));
			if(addClient(username, c))
			{
				c.signinGood();
				sendMessageToClient(c, "OK");
				return;
			}
			else
			{
				sendMessageToClient(c, "ERROR: Bad userid");
			}
		}
		
		/* must be signed in first */
		if(!c.getSignin())
		{
			
		}
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