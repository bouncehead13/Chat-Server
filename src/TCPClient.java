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
	private Integer received;

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
			System.out.print("RCVD from " + ip);
			System.out.println(": " + message);
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
				System.out.print("RCVD from " + username);
				System.out.print(" (" + ip + "): ");
				System.out.println(command);
			}
			sendData("ERROR: Bad command");
		}

	}

	private void send(String[] header) throws IOException
	{
		/* check for correct arguments */
		if(header.length != 3)
		{
			sendData("ERROR: Needs <from-user> <target-user>");
			return;
		}

		String user = header[1].toLowerCase();
		String toUser = header[2].toLowerCase();
		/* check argument and username match */
		if(!user.equals(username))
		{
			sendData("ERROR: Bad <from-user>");
			return;
		}
		/* check username exists */
		else if(server.findClient(toUser) == null)
		{
			sendData("ERROR: Bad <target-user>");
			return;
		}

		String wholeMessage = getMessage();
		if(wholeMessage.equals(""))
			return;

		if(verbose)
		{
			System.out.print("RCVD from " + username);
			System.out.println(" (" + ip + "):");

			System.out.println("  SEND " + username + " " + header[2]);
			String[] sentences = wholeMessage.split("\n");
			for(int i=1; i<sentences.length; i++)
				System.out.println("  " + sentences[i]);

			System.out.print("SENT to " + header[2] + " (");
			System.out.println(server.findClient(header[2]).getIP() + "):");
		}
		server.sendMessageToClient(toUser, wholeMessage, username);
	}

	private void broadcast(String[] header) throws IOException
	{
		/* check for correct arguments */
		if(header.length != 2)
		{
			sendData("ERROR: Needs <from-user>");
			return;
		}

		/* check argument and username match */
		String user = header[1].toLowerCase();
		if(!user.equals(username))
		{
			sendData("ERROR: Bad <from-user>");
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
			server.sendMessageToClient(key, wholeMessage, username);
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
		if(!user.equals(username))
		{
			sendData("ERROR: Bad from-user");
			return;
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
		if(!user.equals(username))
		{
			sendData("ERROR: Bad from-user");
			return;
		}

		sendData("Logged out. Bye");
		closeConnection();
	}

	private String getMessage() throws IOException
	{
		boolean chunked = false;
		String wholeMessage = "FROM " + username, sizeString = "";
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
						wholeMessage = wholeMessage.concat('\n' + "C0");
						break;
					}
				}
				catch(NumberFormatException ex)
				{
					System.err.println(ex);
					sendData("ERROR: Bad length");
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
						sendData("ERROR: Bad length");
						return "";
					}
				}
				catch(NumberFormatException ex)
				{
					System.err.println(ex);
					sendData("ERROR: Bad length");
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
					message = message.concat('\n' + sentence);
					total += sentence.length();
				}
			}

			wholeMessage = wholeMessage.concat('\n' + sizeString + '\n' + message);
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
			out.writeBytes(message + '\n');

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
	
	/* remove this code
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
				sendData("ERROR: Bad length");
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
				sendData("ERROR: Bad length");
				return -1;
			}
		}
	}
	*/

	private void sendRandomMessage()
	{
		Random rand = new Random();
		int n_user = rand.nextInt(3);
		int n_message = rand.nextInt(10);

		String user = users.get(n_user);
		String message = random[n_message];

		try
		{
			out.writeBytes(message + '\n');
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