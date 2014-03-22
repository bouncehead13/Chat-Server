package src;

import java.io.*;
import java.net.*;
import java.util.*;

class Client implements Runnable
{
	private Socket connection;
	private String username;
	private ChatServer server;
	
	public Client(Socket sock, ChatServer s)
	{
		connection = sock;
		server = s;
	}
	
	public void run()
	{
		boolean signin = true;
		
		while(true)
		{
			try
			{
				InputStream stream = connection.getInputStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(stream));
				String sentence = in.readLine();
				
				if(sentence.length() > 5 && sentence.substring(0, 5).equals("ME IS"))
				{
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
					else
					{
						sendData("Got data: " + sentence);
					}
				}
			}
			catch(IOException ex)
			{
				System.err.println(ex);
			}
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