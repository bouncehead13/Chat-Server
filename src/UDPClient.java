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
	private Integer received;

	public UDPClient(DatagramPacket packet, DatagramSocket sock, boolean v, ChatServer s)
	{
		server = s;
		connection = sock; 
		recvMessage = packet;
		IPAddress = packet.getAddress();
		port = packet.getPort();
		ip = packet.getAddress().getHostAddress();
		verbose = v;
		username = "";
		received = 0;
	}

	public void run()
	{
		parseUDPMessage(new String(recvMessage.getData()));
	}
	
	public void parseUDPMessage(String s)
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
			sendData("ERROR: Bad command");
		}
	}
	
	public void send(String message)
	{
		String messageByParts[] = message.split("\\n", 3);
		String header[] = messageByParts[0].split(" ");
		
		if(header.length != 3)
		{
			sendData("ERROR: Needs <from-user> <target-user>");
			return;
		}
		
		String user = header[1].toLowerCase();
		String toUser = header[2].toLowerCase();
		
		if(!checkIP(user))
		{
			sendData("ERROR: Bad <from-user>");
			return;
		}
		else if(server.findClient(toUser) == null)
		{
			sendData("ERROR: Bad <target-user>");
			return;
		}
		
		try
		{
			Integer size = Integer.parseInt(messageByParts[1]);
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
		String sendMessage = "FROM " + user + '\n' + messageByParts[1] + '\n' + messageByParts[2];
		
		if(verbose)
		{
			System.out.println("RCVD from " + user);

			System.out.println("  SEND " + user + " " + header[2]);
			String[] sentences = sendMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);

			System.out.print("SENT to " + header[2] + " (");
			System.out.println(server.findClient(header[2]).getIP() + "):");
		}

		server.sendMessageToClient(toUser, sendMessage, user);
	}
	
	public void broadcast(String message)
	{
		String messageByParts[] = message.split("\\n", 3);
		String header[] = messageByParts[0].split(" ");
		
		if(header.length != 2)
		{
			sendData("ERROR: Needs <from-user> <target-user>");
			return;
		}
		
		String user = header[1].toLowerCase();
		
		if(!checkIP(user))
		{
			sendData("ERROR: Bad <from-user>");
			return;
		}
		
		try
		{
			Integer size = Integer.parseInt(messageByParts[1]);
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
		String sendMessage = "FROM " + user + '\n' + messageByParts[1] + '\n' + messageByParts[2];
		
		if(verbose)
		{
			System.out.println("RCVD from " + fullname);

			System.out.println("  BROADCAST");
			String[] sentences = sendMessage.split("\\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);
		}
		
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
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[2].toLowerCase();
		if(!checkIP(user))
		{
			sendData("ERROR: Bad from-user");
			return;
		}

		if(verbose)
		{
			System.out.print("RCVD from " + ip + ": ");
			System.out.println("WHO HERE " + username);
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
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[1].toLowerCase();
		if(!checkIP(user))
		{
			sendData("ERROR: Bad from-user");
			return;
		}

		sendData("Logged out. Bye");
		closeConnection(user);
	}
	
	public boolean checkIP(String user)
	{
		Client client = server.findClient(user);
		
		if(client != null)
		{
			if(ip != client.getIP())
			{
				server.updateIP(user, recvMessage.getAddress());
			}
		}
		else
		{
			return false;
		}

		return true;
	}

	public void parseSignInUDP(String message, int argc, int fromUserIndex)
	{
		message = message.trim();
		String[] info = message.split(" ");
		
		if(verbose)
		{
			System.out.print("RCVD from " + ip);
			System.out.println(": " + message);
		}
		
		if (info.length != argc)
		{
			sendData("ERROR: Needs <from-user>");
			return;
		}

		String fromUser = info[fromUserIndex];

		Client client = server.findClient(fromUser);
		
		if(client == null)
		{
			server.addClient(fromUser, this);
			sendData("OK");
		}
		else
		{
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
			DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, IPAddress, port);
			connection.send(sendPacket);

			if(users.size() == 3)
				users.remove(0);
			users.add(fromUser);
			received++;

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

	private void sendRandomMessage()
	{
		Random rand = new Random();
		int n_user = rand.nextInt(3);
		int n_message = rand.nextInt(10);

		String user = users.get(n_user);
		String message = random[n_message];

		String header = "FROM " + user;
		String size = Integer.toString(message.length());
		
		message = header + '\n' + size + '\n' + message + '\n';
		try
		{
			DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, IPAddress, port);
			connection.send(sendPacket);
			
			if(verbose)
			{
				System.out.print("SENT (randomly!) to (" + ip + "):");
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