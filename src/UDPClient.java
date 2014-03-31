package src;

import java.io.*;
import java.net.*;
import java.util.*;

class UDPClient extends Client
{
	private DatagramSocket connection;
	private DatagramPacket recvMessage;
	private InetAddress IPAddress;
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

	}



	/* close the socket and remove from mapping */
	private void closeConnection()
	{
		server.removeClient(username);
	}

	/* send message internally */
	public void sendData(String message)
	{
		try
		{
			DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, IPAddress, port);
			connection.send(sendPacket);
			if(verbose)
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
			System.err.println(ex);
			System.out.println("sendData()");
			closeConnection();
		}
	}

	private void sendRandomMessage()
	{
		Random rand = new Random();
		int n_user = rand.nextInt(3);
		int n_message = rand.nextInt(10);

		String user = users.get(n_user);
		String message = random[n_message];

		try
		{
			DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, IPAddress, port);
			connection.send(sendPacket);
		}
		catch(IOException ex)
		{
			System.err.println(ex);
			System.out.println("sendData()");
			closeConnection();
		}

		if(verbose)
		{
			System.out.print("SENT (randomly!) to " + username);
			System.out.println(" (" + ip + "):");
			System.out.println("  FROM " + user);
			System.out.println("  " + message.length());
			System.out.println("  " + message);
		}
	}
}