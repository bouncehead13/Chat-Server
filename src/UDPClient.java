/*
 *  Benjamin Ciummo
 *  Matt Hancock
 *  Eric Lowry
 */
package src;

import java.io.*;
import java.net.*;
import java.util.*;

class UDPClient extends Client
{
	private DatagramSocket connection;
	private DatagramPacket recvMessage;
	private ChatServer server;
	private boolean verbose;

	public UDPClient(DatagramPacket packet, DatagramSocket sock, boolean v, ChatServer s, ArrayList<String> u, Integer r)
	{
		server = s;
		connection = sock; 
		recvMessage = packet;
		IPAddress = packet.getAddress();
		port = packet.getPort();
		ip = packet.getAddress().getHostAddress();
		verbose = v;
		username = server.findClientName(ip);
		received = r;
	}
	
	// Parse the packet
	public void run()
	{
		parseUDPMessage(new String(recvMessage.getData()));
	}
	
	// Parse the packet
	private void parseUDPMessage(String s)
	{	
		if (s.startsWith("ME IS"))
		{
			parseSignInUDP(s, 3, 2);
		}
		else if(s.startsWith("SEND"))
		{
			send(s);
		}
		else if(s.startsWith("BROADCAST"))
		{
			broadcast(s);
		}
		else if(s.startsWith("LOGOUT"))
		{
			logout(s.trim().split(" "));
		}
		else if(s.startsWith("WHO HERE"))
		{
			whoHere(s.trim().split(" "));
		}
		else
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad command");
		}
	}
	
	// send a message to the sending client
	private void send(String message)
	{
		String messageByParts[] = message.split("\\n", 3);
		String header[] = messageByParts[0].split(" ");
		
		// check correct number of command args
		if(header.length < 3)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Needs <from-user> <target-user>");
			return;
		}
		
		String user = header[1].toLowerCase();
		String toUser = header[2].toLowerCase();
		
		// check (and update if necessary) the user and their IP
		if(!checkIP(user))
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad <from-user>");
			return;
		}
		// target user doesn't exist
		else if(server.findClient(toUser) == null)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad <target-user>");
			return;
		}
		// more than one target user
		else if(header.length > 3)
		{
			sendMultipleUsers(header, messageByParts);
			return;
		}
		
		// check if the message length is OK
		try
		{
			Integer size = Integer.parseInt(messageByParts[1]);
			if(size > 99)
			{
				if(verbose)
				{
					System.out.println("RCVD from " + ip + ": Bad request");
				}
				sendData("ERROR: Bad length");
				return;
			}
		}
		catch(NumberFormatException ex)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad length");
			return;
		}
		
		// build the message and send it
		String sendMessage = "FROM " + user + '\n' + messageByParts[1] + '\n' + messageByParts[2];
		
		// server output
		if(verbose)
		{
			System.out.println("RCVD from " + user + " (" + ip + "):");

			System.out.println("  SEND " + user + " " + header[2]);
			String[] sentences = sendMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);

			System.out.print("SENT to " + header[2] + " (");
			System.out.println(server.findClient(header[2]).getIP() + "):");
		}

		server.sendMessageToClient(toUser, sendMessage, user);
	}
	
	// send with multiple targets
	private void sendMultipleUsers(String[] header, String[] messageByParts)
	{
		String user = header[1].toLowerCase();
		String allnames = arrayToString(header, " ", 2);
		
		// get the message size and check if it's OK
		try
		{
			Integer size = Integer.parseInt(messageByParts[1]);
			if(size > 99)
			{
				if(verbose)
				{
					System.out.println("RCVD from " + ip + ": Bad request");
				}
				sendData("ERROR: Bad length");
				return;
			}
		}
		catch(NumberFormatException ex)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad length");
			return;
		}
		
		// build the message
		String sendMessage = "FROM " + user + '\n' + messageByParts[1] + '\n' + messageByParts[2];
		
		// server output
		if(verbose)
		{
			System.out.println("RCVD from " + user + " (" + ip + "):");
			
			System.out.println("  SEND " + user + " " + allnames);
			String[] sentences = sendMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
		}
		// send to the target users
		for(int i=2; i<header.length; i++)
		{
			if(verbose)
			{
				System.out.print("SENT to " + header[i] + " (");
				System.out.println(server.findClient(header[i]).getIP() + "):");
			}
			server.sendMessageToClient(header[i], sendMessage, user);
		}
	}
	
	// broadcast request
	private void broadcast(String message)
	{
		String messageByParts[] = message.split("\\n", 3);
		String header[] = messageByParts[0].split(" ");
		
		// check for the correct number of arguments
		if(header.length != 2)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Needs <from-user> <target-user>");
			return;
		}
		
		String user = header[1].toLowerCase();
		
		// check (and update if necessary) the user and their IP
		if(!checkIP(user))
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad <from-user>");
			return;
		}
		
		// get the size of the message and check if it's OK
		try
		{
			Integer size = Integer.parseInt(messageByParts[1]);
			if(size > 99)
			{
				if(verbose)
				{
					System.out.println("RCVD from " + ip + ": Bad request");
				}
				sendData("ERROR: Bad length");
				return;
			}
		}
		catch(NumberFormatException ex)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad length");
			return;
		} 
		String sendMessage = "FROM " + user + '\n' + messageByParts[1] + '\n' + messageByParts[2];
		
		// server output
		if(verbose)
		{
			System.out.println("RCVD from " + user + "  (" + ip + "):");

			System.out.println("  BROADCAST");
			String[] sentences = sendMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
		}
		// send to all of the current clients
		for(String key : server.getClients())
		{
			if(verbose)
			{
				System.out.print("SENT to " + key + " (");
				System.out.println(server.findClient(key).getIP() + "):");
			}
			server.sendMessageToClient(key, sendMessage, username);
		}
	}
	
	/* print a table of all users in the mapping */
	private void whoHere(String[] header)
	{
		/* check for correct arguments */
		if(header.length != 3)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[2].toLowerCase();
		if(!checkIP(user))
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad from-user");
			return;
		}
		
		// server output
		if(verbose)
		{
			System.out.println("RCVD from " + username + " (" + ip + "): WHO HERE " + username);
		}

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
		/* check for correct arguments */
		if(header.length != 2)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[1].toLowerCase();
		if(!checkIP(user))
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad from-user");
			return;
		}
		
		//server output
		if(verbose)
		{
			System.out.println("RCVD from " + username + " (" + ip + "): LOGOUT " + username);
		}
		
		sendData("Logged out. Bye");
		closeConnection(user);
	}
	
	// UDP allows users to change their IP, so we need to silently update their IP if it changes
	private boolean checkIP(String user)
	{
		Client client = server.findClient(user);
		
		if(client != null)
		{
			if(!ip.equals(client.getIP()))
			{
				// remove the old client (with the old IP) and add the new
				Client c = server.removeClient(user);
				received = c.getReceived();
				users = c.getList();
				server.addClient(user, this);
			}
		}
		// client didn't exist
		else
		{
			return false;
		}

		return true;
	}

	// attempt a sign in request
	private void parseSignInUDP(String message, int argc, int fromUserIndex)
	{
		message = message.trim();
		String[] info = message.split(" ");
		
		// server output
		if(verbose)
		{
			if(username.equals(""))
			{
				System.out.println("RCVD from " + ip + ": " + message);
			}
			else
			{
				System.out.println("RCVD from " + username + " (" + ip + "): " + message);
			}
		}
		
		if (info.length != argc)
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Needs <from-user>");
			return;
		}

		String fromUser = info[fromUserIndex];
		// attempt to find the client's username
		Client client = server.findClient(fromUser);
		
		// not already in the list, add it
		if(client == null)
		{
			server.addClient(fromUser, this);
			sendData("OK");
		}
		// duplicate username
		else
		{
			if(verbose)
			{
				System.out.println("RCVD from " + ip + ": Bad request");
			}
			sendData("ERROR: Bad username");
		}
	}

	/* remove from mapping */
	private void closeConnection(String user)
	{
		server.removeClient(user);
	}

	/* send message internally */
	public void sendData(String message)
	{
		try
		{
			String sendMessage = message + '\n';
			DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length, IPAddress, port);
			connection.send(sendPacket);
			if(verbose)
			{
				if(username.equals(""))
				{
					System.out.println("SENT to " + ip + ": " + message);
				}
				else
				{
					System.out.println("SENT to " + username + " (" + ip + "): " + message);
				}
			}
		}
		catch(IOException ex)
		{
			// nothing to do
		}
	}

	/* send message from another client */
	public void sendData(String message, String fromUser)
	{
		try
		{	
			// build the packet and send it
			DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, IPAddress, port);
			connection.send(sendPacket);

			// update the 3 most recent senders
			if(users.size() == 3)
				users.remove(0);
			users.add(fromUser);
			received++;

			// every 3 messages send a random one
			if(received == 3)
			{
				received = 0;
				sendRandomMessage();
			}
		}
		catch(IOException ex)
		{
			// nothing to do
		}
	}

	// send a random message
	private void sendRandomMessage()
	{
		// pick a random message
		Random rand = new Random();
		int n_user = rand.nextInt(3);
		int n_message = rand.nextInt(10);

		String user = users.get(n_user);
		String message = random[n_message];

		// build the random message from a random user
		String header = "FROM " + user;
		String size = Integer.toString(message.length());
		
		String returnMessage = header + '\n' + size + '\n' + message + '\n';
		try
		{
			// build the packet and send the message
			DatagramPacket sendPacket = new DatagramPacket(returnMessage.getBytes(), returnMessage.getBytes().length, IPAddress, port);
			connection.send(sendPacket);
			
			if(verbose)
			{
				System.out.println("SENT (randomly!) to (" + ip + "):");
				System.out.println("  FROM " + user);
				System.out.println("  " + message.length());
				System.out.println("  " + message);
			}
		}
		catch(IOException ex)
		{
			// nothing to do
		}
	}
}