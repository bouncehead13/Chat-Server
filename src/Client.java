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
	
	public boolean sendData(String message)
	{
		try
		{
			out.writeBytes(message + "\n");
			return true;
		}
		catch(IOException ex)
		{
			System.err.println(ex);
			System.out.println("sendData()");
			closeConnection();
			return false;
		}
	}
	
	public String readData() throws IOException
	{
		System.out.println("Reading in...");
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
				boolean send = sendData("ERROR: Please enter a username");
				if(verbose && send)
					System.out.println("SENT to " + ip + ": ERROR: Please enter a username");
				return;
			}
			
			String name = (message.substring(6)).toLowerCase();
			if(server.addClient(name, this))
			{
				signin = false;
				boolean send = sendData("OK");
				if(verbose && send)
					System.out.println("SENT to " + ip + ": OK");
				username = name;
			}
			else
			{
				boolean send = sendData("ERROR: Bad username/IP address");
				if(verbose && send)
					System.out.println("SENT to " + ip + ": ERROR: Bad username/IP address");
			}
		}
		/* must be signed in first */
		else
		{
			boolean send = sendData("ERROR: You must sign in first");
			if(verbose && send)
				System.out.println("SENT to " + ip + ": ERROR: You must sign in first");
		}
	}
	
	public void parseMessage(String message) throws IOException
	{
		System.out.println("Message parse with [" + message + "]");
		message = message.trim();
		String[] header = message.split(" ");
		
		if(header.length != 3)
		{
			boolean send = sendData("ERROR: Needs <from-user> <target-user>");
			if(verbose && send)
				System.out.println("SENT to " + ip + ": ERROR: Needs <from-user> <target-user>");
		}
		else if(server.findClient(header[2]) == null)
		{
			boolean send = sendData("ERROR: Bad target-user");
			if(verbose && send)
				System.out.println("SENT to " + ip + ": ERROR: Bad target-user");
		}
		else if(header[0].equals("SEND"))
		{
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
						sendData("ERROR: Bad length");
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
							sendData("ERROR: Bad length");
							return;
						}
					}
					catch(NumberFormatException ex)
					{
						System.err.println(ex);
						sendData("ERROR: Bad length");
						return;
					}
				}
				
				// +2 for \n
				byte[] buff = new byte[size+2];
				stream.read(buff, 0, size+2);
				
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
		else
			sendData("ERROR: Bad message");
			
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