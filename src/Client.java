package src;

import java.io.*;
import java.net.*;
import java.util.*;

class Client implements Runnable
{
	private Socket connection;
	private ChatServer server;
	private DataOutputStream out;
	private String ip;
	private boolean signin;
	
	public Client(Socket sock, ChatServer s)
	{
		connection = sock;
		server = s;
		ip = sock.getInetAddress().getHostAddress();
		signin = true;
		try
		{
			out = new DataOutputStream(sock.getOutputStream());
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	public DataOutputStream getOutputStream()
	{
		return out;
	}
	
	public String getIP()
	{
		return ip;
	}
	
	public boolean getSignin()
	{
		return signin;
	}
	
	public void signinGood()
	{
		signin = false;
	}
	
	/* checks client is signed */
	public void run()
	{
		while(signin)
		{
			readData();
		}
		
		listenForData();
	}
	
	/* main function */
	private void listenForData()
	{
		System.out.println("Listening...");
		while(true)
		{
			/*
			String[] command = readData().split(" ");
			String size = readData();
			String message = readData();
			
			server.sendMessageToUser(connection, command[0], message, command[1]);
			 */
		}
	}
	
	/* read a line from the client data stream */
	private void readData()
	{
		try
		{
			InputStream stream = connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String sentence = in.readLine();
			server.readMessageFromClient(this, sentence);
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
}