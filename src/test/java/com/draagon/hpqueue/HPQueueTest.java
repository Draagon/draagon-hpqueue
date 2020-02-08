package com.draagon.hpqueue;

import java.util.Random;

import junit.framework.TestCase;

public class HPQueueTest extends TestCase {
	
	private static boolean shouldStop = false;

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public void testQueue() throws Exception
	{
		HPQueue<Long> q = new HPQueue<Long>( 1024, 10240 );

		// WRITERS
		for( int i = 0; i < 250; i++ )
		{
			Thread t = new Thread() {
				HPQueue<Long> q = null;
				
				public boolean equals( Object o ) {
					if ( o instanceof HPQueue )
						q = (HPQueue<Long>) o;
					return super.equals( o );
				}
				public void run() {
					Random r = new Random( System.currentTimeMillis() );
					while( !shouldStop )
					{
						addTest( q, r.nextInt( 20 ) + 1 );
						try {
							sleep( ((long) r.nextInt( 3000 )) + 100L );
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			};

			t.equals( q );
			t.start();
		}

		// READER
		Thread t = new Thread() {
			HPQueue<Long> q = null;
			@SuppressWarnings("unchecked")
			public boolean equals( Object o ) {
				if ( o instanceof HPQueue )
					q = (HPQueue) o;
				return super.equals( o );
			}
			public void run() {
				Random r = new Random( System.currentTimeMillis() );
				while( !shouldStop )
				{
					readTest( q, r.nextInt( 500 ) + 1 );
					try {
						sleep( 100L );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};

		t.equals( q );
		t.start();

		long start = System.currentTimeMillis();
		while( true ) {
			Thread.sleep( 1000L );
			
			long duration = ( System.currentTimeMillis() - start );
				
			System.out.println( "DURATION: " + duration );
			
			if ( duration > ( 1000L * 10L )) break;
		}
		
		shouldStop = true;
		
		System.out.println( "STOPPING!" );

		/*q.dump();

		addTest( q, 1, 25 );

		q.dump();

		readTest( q, 5 );

		q.dump();

		readTest( q, 10 );

		q.dump();

		addTest( q, 26, 5 );

		q.dump();

		addTest( q, 31, 6 );

		q.dump();

		readTest( q, 26 );

		q.dump();

		addTest( q, 42, 50 );

		q.dump();

		readTest( q, 25 );

		q.dump();

		addTest( q, 67, 12 );

		q.dump();

		readTest( q, 20 );

		q.dump();*/
	}

	private static int qid = 1;

	private static synchronized void addTest( HPQueue<Long> q, int n )
	{
		//int s = qid;
		int j = 0;
		for( int i = 0; i < n; i++ )
		{
			Long x = new Long( qid );
			if ( q.push( x ))
			{
				qid++;
				j++;
			}
		}

		//System.out.println( "ADDED: (" + j + "/" + n + ") " + s + " to " + ( qid-1 ) + " [" + q.size() + "]");
		//System.out.println( "SIZE: " + q.size() );

		// q.dump();
		// q.dump();
	}

	private static int qad = 1;

	private static void readTest( HPQueue<Long> q, int n )
	{
		Long first = null;
		Long last = null;

		//String read = "READ: ";
		//System.out.print( "READ: " );

		int i = 0;
		for( ; i < n; i++ )
		{
			Long l = q.pop();
			if ( l == null ) break;

			//read += l + " ";

			if ( l.intValue() != qad++ ) {
				System.out.println( " !!!!!! SEQUENCE ERROR!  Read " + l + " but expected " + qad + " [" + q.size() + "]" );
				//q.dump();
				System.exit( 0 );
			}

			if ( first == null ) first = l;
			if ( l != null ) last = l;
		}

		//System.out.println( read );
		//q.dump();

		// q.dump();
		if ( i > 0 ) {
			System.out.println( "REMOVED: (" + i + ") " + first + " to " + last + " [" + q.size() + "]" );
			//System.out.println( "SIZE: " + q.size() );
			//q.dump();
		}
		//else
		//	System.out.println( "----EMPTY----" );
	}	
}
