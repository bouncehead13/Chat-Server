/*
 *  Benjamin Ciummo
 *  Matt Hancock
 *  Eric Lowry
 */
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
	private String error;
	
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
		error = "";
		
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
				// If the user is not signed in, attempt a sign in
				if(signin)
				{
					parseSignin(sentence);
				}
				// Already signed in, parse a message
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

	// Parse a sign in attempt
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
				error = "RCVD from " + fullname + " Bad request";
				
			}
			else
			{
				sendData("ERROR: Bad username/IP address");
			}
		}
		// Attempted an action other that sign in
		else
		{
			sendData("ERROR: You must sign in first");
		}
	}

	// Parse a message
	public void parseMessage(String command) throws IOException
	{
		/* call the according command function */
		if(command.startsWith("SEND"))
		{
			send(command.split(" "));
		}
		else if(command.startsWith("BROADCAST"))
		{
			broadcast(command.split(" "));
		}
		else if(command.startsWith("WHO HERE"))
		{
			whoHere(command.split(" "));
		}
		else if(command.startsWith("LOGOUT"))
		{
			logout(command.split(" "));
		}
		else
		{
			if(verbose)
			{
				System.out.print("RCVD from " + fullname + " ");
				System.out.println(command);
			}
			sendData("ERROR: Bad command");
		}
	}

	// Process a send request
	private void send(String[] header) throws IOException
	{
		/* check for correct arguments */
		if(header.length < 3)
		{
			if(verbose)
			{
				System.out.println(error);
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
				System.out.println(error);
			}
			sendData("ERROR: Bad <from-user>");
			return;
		}
		/* check username exists */
		else if(server.findClient(toUser) == null)
		{
			if(verbose)
			{
				System.out.println(error);
			}
			sendData("ERROR: Bad <target-user>");
			return;
		}
		/* more than one username found to send to */
		else if(header.length > 3)
		{
			sendManyUsers(header);
			return;
		}
		
		/* read in entire message content */
		String wholeMessage = getMessage();
		if(wholeMessage == null)
			return;

		// Server output
		if(verbose)
		{
			System.out.println("RCVD from " + fullname);
			
			System.out.println("  SEND " + username + " " + header[2]);
			String[] sentences = wholeMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
			
			System.out.print("SENT to " + header[2] + " (");
			System.out.println(server.findClient(header[2]).getIP() + "):");
		}
		server.sendMessageToClient(toUser, wholeMessage, username);
	}
	
	// Send message to multiple users
	private void sendManyUsers(String[] header) throws IOException
	{
		// from user
		String user = header[1].toLowerCase();
		
		/* combine header to create string of all names */
		String allnames = arrayToString(header, " ", 2);
		
		/* read in entire message content */
		String wholeMessage = getMessage();
		if(wholeMessage == null)
			return;
		
		// Server output
		if(verbose)
		{
			System.out.println("RCVD from " + fullname);
			
			System.out.println("  SEND " + username + " " + allnames);
			String[] sentences = wholeMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
		}
		
		// Send message to each requested user
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
	
	// Process a broadcast request
	private void broadcast(String[] header) throws IOException
	{
		/* check for correct arguments */
		if(header.length != 2)
		{
			if(verbose)
			{
				System.out.println(error);
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
				System.out.println(error);
			}
			sendData("ERROR: Bad <from-user>");
			return;
		}
		
		/* read in entire message content */
		String wholeMessage = getMessage();
		if(wholeMessage == null)
			return;
		
		// Server output
		if(verbose)
		{
			System.out.println("RCVD from " + fullname);
			
			System.out.println("  BROADCAST");
			String[] sentences = wholeMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
		}
		
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
	private void whoHere(String[] header)
	{
		String message = "RCVD from " + fullname + " WHO HERE ";
		
		/* check for correct arguments */
		if(header.length != 3)
		{
			if(verbose)
			{
				System.out.println(error);
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
				System.out.println(error);
			}
			sendData("ERROR: Bad from-user");
			return;
		}
		
		if(verbose)
		{
			System.out.println(message + user);
		}
		
		// Send the list of users to the client
		sendData("Here are the users");
		sendData("==================");
		for(String key : server.getClients())
		{
			sendData("  " + key);
		}
	}

	/* log client out */
	private void logout(String[] header)
	{
		String message = "RCVD from " + username + " (" + ip + "): LOGOUT ";
		
		/* check for correct arguments */
		if(header.length != 2)
		{
			if(verbose)
			{
				System.out.println(error);
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
				System.out.println(error);
			}
			sendData("ERROR: Bad from-user");
			return;
		}
		
		// Server output
		if(verbose)
		{
			System.out.println(message + user);
		}
		
		sendData("Logged out. Bye");
		closeConnection();
	}

	/* get chunked or nonchunked message */
	private String getMessage() throws IOException
	{
		boolean chunked = false;
		// Add the from tag
		String wholeMessage = "FROM " + username + '\n', sizeString = "";
		while(true)
		{
			chunked = false;
			sizeString = readData().trim();
			Integer size = 0;
			
			/* message is chunked */
			if(sizeString.startsWith("C"))
			{
				chunked = true;
				try
				{
					// Get the chunk size
					size = Integer.parseInt(sizeString.substring(1));
					
					// Size of 0, done receiving
					if(size == 0)
					{
						/* concat new message to whole message */
						wholeMessage = wholeMessage.concat("C0");
						break;
					}
					// Chunk > 999 bytes, return an error
					else if(size > 999)
					{
						if(verbose)
						{
							System.out.println(error);
						}
						sendData("ERROR: Bad length1");
						return null;
					}
				}
				// Bad chunk size
				catch(NumberFormatException ex)
				{
					if(verbose)
					{
						System.out.println(error);
					}
					sendData("ERROR: Bad length2");
					return null;
				}
			}
			// non chunked message
			else if(!chunked)
			{
				// attempt to read the size
				try
				{
					size = Integer.parseInt(sizeString);
					if(size > 99)
					{
						if(verbose)
						{
							System.out.println(error);
						}
						sendData("ERROR: Bad length3");
						return null;
					}
				}
				catch(NumberFormatException ex)
				{
					if(verbose)
					{
						System.out.println(error);
					}
					sendData("ERROR: Bad length4");
					return null;
				}
			}
			
			/* read in size number of characters */
			char[] buff = new char[size];
			in.read(buff, 0, size);
			String message = new String(buff);
			
			/* concat new message to whole message */
			wholeMessage = wholeMessage.concat(sizeString + '\n' + message);
			if(!chunked)
				break;
		}

		return wholeMessage;
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
			// Write to out socket
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
		// Write failed, user disconnected
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
		
		// Write failed, user disconnected
		catch(IOException ex)
		{
			closeConnection();
		}
	}

	// Send a random message
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
		
		// send the message
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
