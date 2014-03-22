package src;

import java.io.*;
import java.net.*;
import java.util.*;

class Client implements Runnable
{
	private Socket connection;
	
	public Client(Socket sock)
	{
		connection = sock;
	}
	
	public void run()
	{
		
	}
}