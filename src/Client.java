package src;

import java.io.*;
import java.net.*;
import java.util.*;

abstract class Client implements Runnable
{
	protected ChatServer server;
	protected String ip, username, fullname;
	protected String[] random;
	protected ArrayList<String> users;
	protected int port;
	protected InetAddress IPAddress;

	public abstract void run();
	public abstract void sendData(String message, String fromUser);
	public abstract void sendData(String message);
	
	public Client()
	{
		initRandom();
		users = new ArrayList<String>();
	}
	
	public String arrayToString(String[] a, String separator)
	{
		String result = "";
		if (a.length > 2)
		{
			result = a[2];
			for (int i=3; i<a.length; i++)
			{
				result = result + separator + a[i];
			}
		}
		return result;
	}
	
	protected void initRandom()
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
	
	public String getIP()
	{
		return ip;
	}
	
	public void changeIP(InetAddress newIP)
	{
		IPAddress = newIP;
		ip = newIP.getHostAddress();
	}
}