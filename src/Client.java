package src;

import java.io.*;
import java.net.*;
import java.util.*;

class Client implements Runnable
{
	private Socket connection;
	private ChatServer server;
	private DataOutputStream out;
	private InputStream stream;
	private BufferedReader in;
	private String ip;
	private String username;
	private boolean signin;
	private boolean connectionLive;
	private boolean verbose;
	
	public Client(Socket sock, boolean v, ChatServer s)
	{
		connection = sock;
		server = s;
		ip = sock.getInetAddress().getHostAddress();
		signin = true;
		connectionLive = true;
		verbose = v;
		username = "";
		
		try
		{
			out = new DataOutputStream(sock.getOutputStream());
			stream = sock.getInputStream();
			in = new BufferedReader(new InputStreamReader(stream));
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	/* checks client is signed */
	public void run()
	{
		while(connectionLive)
		{
			try
			{
				String sentence = readData();
				
				if(signin)
					parseSignin(sentence);
				else
					parseMessage(sentence);
			}
			catch(IOException ex)
			{
				System.err.println(ex);
				System.out.println("run()");
				closeConnection();
			}
		}
	}
	
	public String getIP()
	{
		return ip;
	}
	
	public void closeConnection()
	{
		try
		{
			connectionLive = false;
			connection.close();
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	public void sendData(String message, boolean toClient)
	{
		try
		{
			out.writeBytes(message + "\n");
			if(toClient)
				System.out.println("SENT to " + ip + " " + message);
		}
		catch(IOException ex)
		{
			System.err.println(ex);
			System.out.println("sendData()");
			closeConnection();
		}
	}
	
	public String readData() throws IOException
	{
		String sentence = new String();
		sentence = in.readLine();
		return sentence;
	}
	
	public void parseSignin(String message)
	{
		message = message.trim();
		
		if(verbose && !message.equals(""))
		{
			System.out.print("RCVD from " + ip);
			System.out.println(": " + message);
		}
		
		/* check if they are trying to sign in */
		if(signin && findString(message, "ME IS"))
		{
			if(message.length() == 5)
			{
				sendData("ERROR: Please enter a username", true);
				return;
			}
			
			String name = (message.substring(6)).toLowerCase();
			if(server.addClient(name, this))
			{
				signin = false;
				sendData("OK", true);
				username = name;
			}
			else
			{
				sendData("ERROR: Bad username/IP address", true);
			}
		}
		/* must be signed in first */
		else
		{
			sendData("ERROR: You must sign in first", true);
		}
	}
	
	public void parseMessage(String message) throws IOException
	{
		message = message.trim();
		String[] header = message.split(" ");
		
		if(header[0].equals("SEND"))
		{
			if(header.length != 3)
			{
				sendData("ERROR: Needs <from-user> <target-user>", true);
				return;
			}
			else if(server.findClient(header[2]) == null)
			{
				sendData("ERROR: Bad target-user", true);
				return;
			}
			
			header[1] = header[1].toLowerCase();
			header[2] = header[2].toLowerCase();
			
			boolean chunked = false;
			while(true)
			{
				String sizeString = readData();

				Integer size = 0;
				if(sizeString.substring(0,1).equals("C"))
				{
					chunked = true;
					try
					{
						size = Integer.parseInt(sizeString.substring(1));
						if(size == 0)
							break;
					}
					catch(NumberFormatException ex)
					{
						System.err.println(ex);
						sendData("ERROR: Bad length", true);
						return;
					}
				}
				else if(!chunked)
				{
					try
					{
						size = Integer.parseInt(sizeString);
						if(size > 99)
						{
							sendData("ERROR: Bad length", true);
							return;
						}
					}
					catch(NumberFormatException ex)
					{
						System.err.println(ex);
						sendData("ERROR: Bad length", true);
						return;
					}
				}
				
				// +2 for \n
				byte[] buff = new byte[size];
				byte[] enter = new byte[2];
				stream.read(buff, 0, size);
				stream.read(enter, 0, 2);
				
				if(verbose)
				{
					System.out.print("RCVD from " + username);
					System.out.println(" (" + ip + "):");
				}
				
				if(!chunked)
				{
					/* new String()? */
					if(verbose)
					{
						System.out.println("  SEND " + header[1] + " " + header[2]);
						System.out.println("  " + sizeString);
						System.out.println("  " + new String(buff));
						
						System.out.print("SENT to " + header[2] + " (");
						System.out.println(server.findClient(header[2]).getIP() + "):");
						System.out.println("  FROM " + header[1]);
						
					}
					server.sendMessageToClient(header[2], sizeString, new String(buff));
					break;
				}
				else
				{
					
				}
			}
		}
		else if(header[0].equals("LOGOUT"))
		{
			if(header.length != 2)
			{
				sendData("ERROR: Needs <from-user>", true);
				return;
			}
			
			sendData("Logged out. Bye", true);
			if(verbose)
			{
				System.out.println("SENT to " + ip + ": Logged out. Bye");
			}
		}
		else
			sendData("ERROR: Bad command", true);
			
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