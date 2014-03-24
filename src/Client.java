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
	private boolean connectionLive;
	
	public Client(Socket sock, ChatServer s)
	{
		connection = sock;
		server = s;
		ip = sock.getInetAddress().getHostAddress();
		signin = true;
		connectionLive = true;
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
	
	/* checks client is signed */
	public void run()
	{
		signInClient();
		listenForData();
	}
	
	/* main function */
	private void listenForData()
	{
		System.out.println("Listening...");
		while(connectionLive)
		{
			readMessage();
		}
	}
	
	/* waits until the client signs in */
	private void signInClient()
	{
		while(signin && connectionLive)
		{
			readData();
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
			server.readDataFromClient(this, sentence);
		}
		catch(IOException ex)
		{
			System.err.println(ex);
		}
	}
	
	/* read the message from the client data stream */
	private void readMessage()
	{
		/* FINISH THIS SECTION */
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