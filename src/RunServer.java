package src;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

class RunServer
{
	private List<Integer> ports;
	
	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.out.println("Not enough arguments");
			System.exit(1);
		}
		
		ForkJoinPool pool = new ForkJoinPool(args.length);
		List<Integer> ports = new ArrayList<Integer>();
		for(int i=0; i<args.length; i++)
		{
			ports.add(Integer.parseInt(args[i]));
			pool.invoke(new ChatServer(Integer.parseInt(ports[i])));
		}
	}
}