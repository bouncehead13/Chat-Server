package src;

import java.io.*;
import java.net.*;
import java.util.*;

class Client implements Runnable
{
	private Socket connection;
	private ChatServer server;
	private DataOutputStream out;
	private BufferedReader in;
	private String ip, username;
	private boolean signin, connectionLive;
	private boolean verbose;
	private String[] random;
	private ArrayList<String> users;
	
	public Client(Socket sock, boolean v, ChatServer s)
	{
		connection = sock;
		server = s;
		ip = sock.getInetAddress().getHostAddress();
		signin = true;
		connectionLive = true;
		verbose = v;
		username = "";
		initRandom();
		users = new ArrayList<String>();
		
		try
		{
			out = new DataOutputStream(sock.getOutputStream());
			InputStream stream = sock.getInputStream();
			in = new BufferedReader(new InputStreamReader(stream));
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	/* main while loop to accept commands */
	public void run()
	{
		while(connectionLive)
		{
			try
			{
				String sentence = readData();
				if(signin)
				{
					parseSignin(sentence);
				}
				else
				{
					parseMessage(sentence);
				}
			}
			catch(IOException ex)
			{
				if(username.equals(""))
				{
					System.out.println("Lost connection to " + ip);
				}
				else
				{
					System.out.println("Lost connection to " + username + " (" + ip + ")");
				}
				closeConnection();
			}
		}
	}
	
	public String getIP()
	{
		return ip;
	}
	
	/* close the socket and remove from mapping */
	private void closeConnection()
	{
		try
		{
			connectionLive = false;
			server.removeClient(username);
			connection.close();
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	public void parseSignin(String message)
	{
		message = message.trim();
		
		if(verbose && !message.equals(""))
		{
			System.out.print("RCVD from " + ip);
			System.out.println(": " + message);
		}
		
		if(message.startsWith("ME IS"))
		{
			/* make sure a username is provided */
			if(message.length() == 5)
			{
				sendData("ERROR: Please enter a username", true);
				return;
			}
			
			/* try to add the client to the mapping */
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
		else
		{
			sendData("ERROR: You must sign in first", true);
		}
	}
	
	public void parseMessage(String command) throws IOException
	{
		/* split command by spaces */
		command = command.trim();
		String[] header = command.split(" ");
		
		/* call the according command function */
		if(header[0].equals("SEND"))
		{
			send(header);
		}
		else if(header[0].equals("BROADCAST"))
		{
			broadcast(header);
		}
		else if(header[0].equals("WHO") && header[1].equals("HERE"))
		{
			whoHere(header);
		}
		else if(header[0].equals("LOGOUT"))
		{
			logout(header);
		}
		else
		{
			if(verbose)
			{
				System.out.print("RCVD from " + username);
				System.out.print(" (" + ip + "): ");
				System.out.println(command);
			}
			sendData("ERROR: Bad command", true);
		}
			
	}
	
	private void send(String[] header) throws IOException
	{
		/* check for correct arguments */
		if(header.length != 3)
		{
			sendData("ERROR: Needs <from-user> <target-user>", true);
			return;
		}
		
		String user = header[1].toLowerCase();
		String toUser = header[2].toLowerCase();
		/* check argument and username match */
		if(!user.equals(username))
		{
			sendData("ERROR: Bad <from-user>", true);
			return;
		}
		/* check username exists */
		else if(server.findClient(toUser) == null)
		{
			sendData("ERROR: Bad <target-user>", true);
			return;
		}
		
		String wholeMessage = getMessage();
		if(wholeMessage.equals(""))
			return;
		
		if(verbose)
		{
			System.out.print("RCVD from " + username);
			System.out.println(" (" + ip + "):");
			
			System.out.println("  SEND " + header[1] + " " + header[2]);
			String[] sentences = wholeMessage.split("\n");
			for(int i=0; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
			
			System.out.print("SENT to " + header[2] + " (");
			System.out.println(server.findClient(header[2]).getIP() + "):");
			System.out.println("  FROM " + username);
		}
		server.sendMessageToClient(toUser, wholeMessage);
	}
	
	private void broadcast(String[] header) throws IOException
	{
		/* check for correct arguments */
		if(header.length != 2)
		{
			sendData("ERROR: Needs <from-user>", true);
			return;
		}
		
		/* check argument and username match */
		String user = header[1].toLowerCase();
		if(!user.equals(username))
		{
			sendData("ERROR: Bad <from-user>", true);
			return;
		}
		
		String wholeMessage = getMessage();
		if(wholeMessage.equals(""))
			return;
		
		if(verbose)
		{
			System.out.print("RCVD from " + username);
			System.out.println(" (" + ip + "):");
			
			System.out.println("  BROADCAST");
			String[] sentences = wholeMessage.split("\n");
			for(int i=0; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
		}
		
		for(String key : server.getClients())
		{
			if(verbose)
			{
				System.out.print("SENT to " + key + " (");
				System.out.println(server.findClient(key).getIP() + "):");
				System.out.println("  FROM " + username);
			}
			server.sendMessageToClient(key, wholeMessage);
		}
	}
	
	/* print a table of all users in the mapping */
	private void whoHere(String[] header)
	{
		/* check for correct arguments */
		if(header.length != 3)
		{
			sendData("ERROR: Needs <from-user>", true);
			return;
		}
		
		/* check argument and username match */
		String user = header[2].toLowerCase();
		if(!user.equals(username))
		{
			sendData("ERROR: Bad from-user", true);
			return;
		}
		
		sendData("Here are the users", true);
		sendData("==================", true);
		for(String key : server.getClients())
		{
			sendData("  " + key, true);
		}
	}
	
	/* log client out */
	private void logout(String[] header)
	{
		/* check for correct arguments */
		if(header.length != 2)
		{
			sendData("ERROR: Needs <from-user>", true);
			return;
		}
		
		/* check argument and username match */
		String user = header[1].toLowerCase();
		if(!user.equals(username))
		{
			sendData("ERROR: Bad from-user", true);
			return;
		}
		
		sendData("Logged out. Bye", true);
		closeConnection();
	}
	
	private String getMessage() throws IOException
	{
		boolean chunked = false;
		String wholeMessage = "", sizeString = "";
		while(true)
		{
			chunked = false;
			sizeString = readData().trim();
			
			Integer size = 0;
			if(sizeString.startsWith("C"))
			{
				chunked = true;
				try
				{
					size = Integer.parseInt(sizeString.substring(1));
					if(size == 0)
					{
						wholeMessage = wholeMessage.concat("C0");
						break;
					}
				}
				catch(NumberFormatException ex)
				{
					System.err.println(ex);
					sendData("ERROR: Bad length", true);
					return "";
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
						return "";
					}
				}
				catch(NumberFormatException ex)
				{
					System.err.println(ex);
					sendData("ERROR: Bad length", true);
					return "";
				}
			}
			
			String message = readData();
			if(!chunked && message.length() > size)
			{
				message = message.substring(0, size);
			}
			else if(chunked)
			{
				int total = message.length();
				while(total != size)
				{
					String sentence = readData();
					message = message.concat("\n" + sentence);
					total += sentence.length();
				}
			}
			
			wholeMessage = wholeMessage.concat(sizeString + "\n" + message + "\n");
			if(!chunked)
				break;
		}
		
		return wholeMessage;
	}
	
	private String readData() throws IOException
	{
		String sentence = new String();
		sentence = in.readLine();
		if(sentence == null) throw new IOException();
		return sentence;
	}
	
	public void sendData(String message, boolean toClient)
	{
		try
		{
			String messageToClient = message;
			if(!toClient)
			{
				messageToClient = convertMessage(message);
			}
			
			out.writeBytes(messageToClient + "\n");
			if(verbose && toClient)
			{
				if(username.equals(""))
				{
					System.out.println("SENT to " + ip + " " + message);
				}
				else
				{
					System.out.println("SENT to " + username + " (" + ip + "): " + message);
				}
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
			System.out.println("sendData()");
			closeConnection();
		}
	}
	
	private String convertMessage(String message)
	{
		String newMessage = "";
		String[] sentences = message.split("\n");
		Integer counter = 0;
		Integer compareCounter = 0;
		for(int i=0; i<sentences.length; i++)
		{
			if(counter == compareCounter)
			{
				counter = getInteger(sentences[i]);
				compareCounter = 0;
			}
			else if(i > 1)
			{
				newMessage = newMessage.concat("\n" + sentences[i]);
				compareCounter += sentences[i].length();
			}
			else
			{
				newMessage = newMessage.concat(sentences[i]);
				compareCounter += sentences[i].length();
			}
		}
		
		return newMessage;
	}
	
	private Integer getInteger(String s)
	{
		if(s.startsWith("C"))
		{
			try
			{
				return Integer.parseInt(s.substring(1));
			}
			catch(NumberFormatException ex)
			{
				System.err.println(ex);
				sendData("ERROR: Bad length", true);
				return -1;
			}
		}
		else
		{
			try
			{
				return Integer.parseInt(s);
			}
			catch(NumberFormatException ex)
			{
				System.err.println(ex);
				sendData("ERROR: Bad length", true);
				return -1;
			}
		}
	}
	
	private void initRandom()
	{
		random = new String[10];
		random[0] = "Hey, you're kinda hot";
		random[1] = "No way!";
		random[2] = "I like Justin Bieber....a lot";
		random[3] = "Praise the sun!";
		random[4] = "Garbage!";
		random[5] = "I'm Mr. Meeseeks, look at me!";
		random[6] = "Deus Vult!";
		random[7] = "I love Network Programming :)";
		random[8] = "I'm Olaf, and I like warm hugs!";
		random[9] = "Instragram #selfie";
	}
}
