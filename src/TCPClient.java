package src;

import java.io.*;
import java.net.*;
import java.util.*;

class TCPClient extends Client
{
	private Socket connection;
	private DataOutputStream out;
	private BufferedReader in;
	private boolean signin, connectionLive, verbose;

	public TCPClient(Socket sock, boolean v, ChatServer s)
	{
		connection = sock; server = s;
		ip = sock.getInetAddress().getHostAddress();
		signin = true; connectionLive = true;
		verbose = v;
		username = "";
		initRandom();
		received = 0;
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
				/* print IP only or username with IP */
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
			System.out.println("RCVD from " + ip + ": " + message);
		}

		if(message.startsWith("ME IS"))
		{
			/* make sure a username is provided */
			if(message.length() == 5)
			{
				sendData("ERROR: Please enter a username");
				return;
			}

			/* try to add the client to the mapping */
			String name = (message.substring(6)).toLowerCase();
			if(server.addClient(name, this))
			{
				signin = false;
				sendData("OK");
				username = name;
				
				/* create fullname with IP address */
				fullname = username + " (" + ip + "):";
			}
			else
			{
				sendData("ERROR: Bad username/IP address");
			}
		}
		else
		{
			sendData("ERROR: You must sign in first");
		}
	}

	public void parseMessage(String command) throws IOException
	{
		if(verbose)
		{
			System.out.print("RCVD from " + fullname + " ");
		}
		
		/* call the according command function */
		if(command.startsWith("SEND"))
		{
			send(command);
		}
		else if(command.startsWith("BROADCAST"))
		{
			broadcast(command);
		}
		else if(command.startsWith("WHO HERE"))
		{
			whoHere(command);
		}
		else if(command.startsWith("LOGOUT"))
		{
			logout(command);
		}
		else
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Bad command");
		}

	}

	private void send(String command) throws IOException
	{
		String[] header = command.split(" ");
		
		/* check for correct arguments */
		if(header.length < 3)
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Needs <from-user> <target-user>");
			return;
		}

		String user = header[1].toLowerCase();
		String toUser = header[2].toLowerCase();
		
		/* check argument and username match */
		if(!user.equals(username))
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Bad <from-user>");
			return;
		}
		/* check username exists */
		else if(server.findClient(toUser) == null)
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Bad <target-user>");
			return;
		}
		/* more than one username found to send to */
		else if(header.length > 3)
		{
			sendManyUsers(command);
			return;
		}
		
		/* read in entire message content */
		String wholeMessage = getMessage(command);
		if(wholeMessage == null)
			return;

		if(verbose)
		{
			System.out.print("SENT to " + header[2] + " (");
			System.out.println(server.findClient(header[2]).getIP() + "):");
		}
		server.sendMessageToClient(toUser, wholeMessage, username);
	}
	
	private void sendManyUsers(String command) throws IOException
	{
		String[] header = command.split(" ");
		
		String user = header[1].toLowerCase();
		
		/* combine header to create string of all names */
		String allnames = arrayToString(header, " ", 2);
		
		/* read in entire message content */
		String wholeMessage = getMessage(command);
		if(wholeMessage == null)
			return;
		
		for(int i=2; i<header.length; i++)
		{
			if(verbose)
			{
				System.out.print("SENT to " + header[i] + " (");
				System.out.println(server.findClient(header[i]).getIP() + "):");
			}
			server.sendMessageToClient(header[i], wholeMessage, username);
		}
	}
	
	private void broadcast(String command) throws IOException
	{
		String[] header = command.split(" ");
		
		/* check for correct arguments */
		if(header.length != 2)
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[1].toLowerCase();
		if(!user.equals(username))
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Bad <from-user>");
			return;
		}
		
		/* read in entire message content */
		String wholeMessage = getMessage(command);
		if(wholeMessage == null)
			return;
		
		/* send to each client */
		for(String key : server.getClients())
		{
			if(verbose)
			{
				System.out.print("SENT to " + key + " (");
				System.out.println(server.findClient(key).getIP() + "):");
			}
			server.sendMessageToClient(key, wholeMessage, username);
		}
	}

	/* print a table of all users in the mapping */
	private void whoHere(String command)
	{
		String[] header = command.split(" ");
				
		/* check for correct arguments */
		if(header.length != 3)
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[2].toLowerCase();
		if(!user.equals(username))
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Bad from-user");
			return;
		}
		
		if(verbose)
		{
			System.out.println(command);
		}
		
		sendData("Here are the users");
		sendData("==================");
		for(String key : server.getClients())
		{
			sendData("  " + key);
		}
	}

	/* log client out */
	private void logout(String command)
	{
		String[] header = command.split(" ");
		
		/* check for correct arguments */
		if(header.length != 2)
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[1].toLowerCase();
		if(!user.equals(username))
		{
			if(verbose)
			{
				System.out.println(command);
			}
			sendData("ERROR: Bad from-user");
			return;
		}
		
		if(verbose)
		{
			System.out.println(command);
		}
		
		sendData("Logged out. Bye");
		closeConnection();
	}

	/* get chunked or single message */
	private String getMessage(String command) throws IOException
	{
		/* first print command to log */
		if(verbose)
		{
			System.out.println("\n  " + command);
		}
		
		boolean chunked = false;
		String wholeMessage = "FROM " + username + '\n', sizeString = "";
		while(true)
		{
			chunked = false;
			sizeString = readData().trim();
			Integer size = 0;
			
			/* print the size */
			if(verbose)
			{
				System.out.println("  " + sizeString);
			}
			
			/* message is chunked */
			if(sizeString.startsWith("C"))
			{
				chunked = true;
				try
				{
					size = Integer.parseInt(sizeString.substring(1));
					if(size == 0)
					{
						/* concat new message to whole message */
						wholeMessage = wholeMessage.concat("C0");
						break;
					}
				}
				catch(NumberFormatException ex)
				{
					sendData("ERROR: Bad length");
					return null;
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
						return null;
					}
				}
				catch(NumberFormatException ex)
				{
					sendData("ERROR: Bad length");
					return null;
				}
			}
			
			/* read in size number of characters */
			char[] buff = new char[size];
			in.read(buff, 0, size);
			String message = new String(buff);
			
			/* print the message */
			if(verbose)
			{
				System.out.print("  " + message);
			}
			
			/* concat new message to whole message */
			wholeMessage = wholeMessage.concat(sizeString + '\n' + message);
			if(!chunked)
				break;
		}

		return wholeMessage.trim();
	}

	/* read in a line of text */
	private String readData() throws IOException
	{
		String sentence = new String();
		sentence = in.readLine();
		if(sentence == null) throw new IOException();
		return sentence;
	}
	
	/* send message internally */
	public void sendData(String message)
	{
		try
		{
			out.writeBytes(message + '\n');
			if(verbose)
			{
				if(username.equals(""))
				{
					System.out.println("SENT to " + ip + ": " + message);
				}
				else
				{
					System.out.println("SENT to " + fullname + " " + message);
				}
			}
		}
		catch(IOException ex)
		{
			closeConnection();
		}
	}

	/* send message from another client */
	public void sendData(String message, String fromUser)
	{
		try
		{
			out.writeBytes(message + '\n');
			
			/* add user to list of previous users */
			if(users.size() == 3)
				users.remove(0);
			users.add(fromUser);
			received++;
			
			/* send random message after 3 sends */
			if(received == 3)
			{
				received = 0;
				sendRandomMessage();
			}
		}
		catch(IOException ex)
		{
			closeConnection();
		}
	}

	private void sendRandomMessage()
	{
		/* pick a user and a message */
		Random rand = new Random();
		int n_user = rand.nextInt(3);
		int n_message = rand.nextInt(10);

		String user = users.get(n_user);
		String message = random[n_message];
		
		/* create header string and size string */
		String header = "FROM " + user;
		String size = Integer.toString(message.length());
		
		try
		{
			out.writeBytes(header + '\n' + size + '\n' + message + '\n');
			
			if(verbose)
			{
				System.out.print("SENT (randomly!) to " + username);
				System.out.println(" (" + ip + "):");
				System.out.println("  " + header);
				System.out.println("  " + size);
				System.out.println("  " + message);
			}
		}
		catch(IOException ex)
		{
			closeConnection();
		}
	}
}
