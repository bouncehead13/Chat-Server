/*
 *  Benjamin Ciummo
 *  Matt Hancock
 *  Eric Lowry
 */
package src;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class RunServer
{
	private List<Integer> ports;
	
	public static void main(String[] args)
	{
		/* check correct number of arguments */
		if(args.length < 1)
		{
			System.out.println("Not enough arguments");
			System.exit(1);
		}
		
		/* initialize variables */
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Integer> ports = new ArrayList<Integer>();
		boolean verbose = false;
		
		/* check for verbose flag */
		for(int i=0; i<args.length; i++)
		{
			if(args[i].equals("-v"))
				verbose = true;
		}
		
		/* check there is another argument after verbose */
		if(verbose && args.length < 2)
		{
			System.out.println("Not enough arguments");
			System.exit(1);
		}
		
		/* create a server universe for each port */
		for(int i=0; i<args.length; i++)
		{
			if(!args[i].equals("-v"))
			{
				try
				{
					Integer port = Integer.parseInt(args[i]);
					ports.add(port);
					executor.submit(new ChatServer(port, verbose));
				}
				catch(NumberFormatException e)
				{
					System.out.println(args[i] + " not a valid port, ignoring it");
				}
			}
		}
	}
}