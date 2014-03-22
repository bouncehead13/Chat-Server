package src;

import java.io.*;
import java.net.*;
import java.util.*;

class Client implements Runnable
{
	private Socket connection;
	private String username;
	private boolean verbose;
	private ChatServer server;
	
	public Client(Socket sock, boolean v, ChatServer s)
	{
		connection = sock;
		verbose = v;
		server = s;
	}
	
	public void run()
	{
		boolean signin = true;
		
		while(signin)
		{
			String sentence = readData();
			
			if(findString(sentence, "ME IS"))
			{
				if(sentence.length() == 5)
				{
					sendData("Please enter a username");
					continue;
				}
				
				username = (sentence.substring(6));
				if(server.addClient(username, connection))
				{
					signin = false;
					sendData("OK");
				}
				else
				{
					sendData("ERROR: Bad userid");
				}
			}
			else
			{
				if(signin)
				{
					sendData("You must sign in first");
				}
			}
		}
		
		listenForData();
	}
	
	void listenForData()
	{
		while(true)
		{
			
		}
	}
	
	boolean findString(String s, String check)
	{
		Integer size = check.length();
		if(s.length() >= size && s.substring(0, size).equals(check))
			return true;
		else
			return false;
	}
	
	String readData()
	{
		try
		{
			InputStream stream = connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String sentence = in.readLine();
			return sentence;
		}
		catch(IOException ex)
		{
			System.err.println(ex);
			return "";
		}
	}
	
	public void sendData(String s)
	{
		try
		{
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			out.writeBytes(s + "\n");
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
}