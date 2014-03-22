package src;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

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
		ForkJoinPool pool = new ForkJoinPool(args.length);
		List<Integer> ports = new ArrayList<Integer>();
		boolean verbose = false;
		
		/* check forverbose flag */
		for(int i=0; i<args.length; i++)
		{
			if(args[i].equals("-v"))
				verbose = true;
		}
		
		/* create servers */
		for(int i=0; i<args.length; i++)
		{
			if(!args[i].equals("-v"))
			{
				ports.add(Integer.parseInt(args[i]));
				pool.invoke(new ChatServer(ports.get(i), verbose));
			}
		}
	}
}