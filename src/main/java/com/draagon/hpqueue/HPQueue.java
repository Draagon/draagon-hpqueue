package com.draagon.hpqueue;

/**
 * High Performance Queue to replace the
 * java.util.concurrent.ConcurrentLinkedQueue
 *
 * @author Doug
 *
 * @param <E> The type of Objects to be used in the queue
 */
public class HPQueue<E> {

    public static class HPQueueLevel {

        private Object[] array;

        public HPQueueLevel(int size) {
            array = new Object[size];
        }

        public Object[] get() {
            return array;
        }
    }
    
    private static final int SPREAD = 2;
    private static final int MAX_SPREAD = 4;
    private static final int DEFAULT_MAX_SIZE = 0;
    private static final int DEFAULT_START_SIZE = 1024;
    
    private int readLevel;
    private int readIndex;
    private int writeLevel;
    private int writeIndex;
    private int startSize;
    private int maxSize;
    
    private HPQueueLevel[] levels;
    private int levelHeight = 0;
    private int size = 0;

    public HPQueue() {
        this(DEFAULT_START_SIZE, DEFAULT_MAX_SIZE);
    }

    public HPQueue(int startSize) {
        this(startSize, DEFAULT_MAX_SIZE);
    }

    public HPQueue(int startSize, int maxSize) {
        
        levels = new HPQueueLevel[SPREAD];

        this.startSize = startSize;
        this.maxSize = maxSize;

        for (int i = 0; i < SPREAD; i++) {
            levels[ i] = new HPQueueLevel(startSize);
        }
        
        readLevel = 0;
        readIndex = 0;
        writeLevel = 0;
        writeIndex = 0;
        levelHeight = 0;
        size = 0;
    }

    //public boolean add(E object) {
     //   return push(object);
    //}

    public synchronized boolean push(E object) {
        //synchronized( this )
        //{
        // Can we add anymore?
        if (maxSize > 0 && size == maxSize) {
            return false;
        }

        // Write the element and increase the size
        levels[ writeLevel].get()[ writeIndex] = object;
        size++;

        writeIndex++;
        if (writeIndex == startSize) {
            writeIndex = 0;
            writeLevel++;

            // If we're up a level, then drop down a level, unless the bottom level is not open
            if (readLevel > 0 && writeLevel > readLevel) {
                writeLevel = 0;
            } else if (writeLevel > readLevel) {
                levelHeight++;

                // Add an extra row to the Queue if needed
                if (levelHeight == levels.length) {
                    HPQueueLevel[] newLevels = new HPQueueLevel[levelHeight + 1];
                    System.arraycopy(levels, 0, newLevels, 0, levels.length);
                    newLevels[ levelHeight] = new HPQueueLevel(startSize);
                    levels = newLevels;

                    //System.out.println( "ADDED TO END" );
                    //dump();
                }
            } else if (writeLevel == readLevel) {
                // Insert one on the inside
                HPQueueLevel[] newLevels = new HPQueueLevel[levels.length + SPREAD];
                System.arraycopy(levels, 0, newLevels, 0, writeLevel);
                System.arraycopy(levels, writeLevel, newLevels, writeLevel + SPREAD, (levels.length - writeLevel));
                for (int i = 0; i < SPREAD; i++) {
                    newLevels[ writeLevel + i] = new HPQueueLevel(startSize);
                }
                levels = newLevels;

                // Increment the read values
                readLevel += SPREAD;
                levelHeight += SPREAD;

                //System.out.println( "ADDED INSIDE" );
                //dump();
            }
        }

        return true;
        //}
    }

    //public E remove() {
    //    return pop();
    //}

    //public E poll() {
    //    return pop();
    //}

    @SuppressWarnings("unchecked")
    public synchronized E pop() {
        //synchronized( this )
        //{
        if (size == 0) {
            return null;
        }

        E e = (E) levels[ readLevel].get()[ readIndex];

        if (e == null) {
            dump();
            throw new IllegalStateException("HPQueue serious error, popped null!");
        }

        // clear out the array entry
        levels[ readLevel].get()[ readIndex] = null;

        size--;

        if (size == 0) {
            readLevel = 0;
            readIndex = 0;
            writeLevel = 0;
            writeIndex = 0;
            levelHeight = 0;

            if ((levels.length - levelHeight) >= MAX_SPREAD) {
                // Shrink the array to the SPREAD
                HPQueueLevel[] newLevels = new HPQueueLevel[levelHeight + SPREAD];
                System.arraycopy(levels, 0, newLevels, 0, newLevels.length);
                levels = newLevels;

                //System.out.println( "REMOVED FROM END" );

                //dump();
            }
        } else {
            readIndex++;

            if (readIndex == startSize) {
                readIndex = 0;
                readLevel++;

                if (readLevel > levelHeight) {
                    levelHeight = writeLevel;
                    readLevel = 0;

                    if ((levels.length - levelHeight) >= MAX_SPREAD) {
                        // Shrink the array to the SPREAD
                        HPQueueLevel[] newLevels = new HPQueueLevel[levelHeight + SPREAD];
                        System.arraycopy(levels, 0, newLevels, 0, newLevels.length);
                        levels = newLevels;

                        //System.out.println( "REMOVED FROM END" );

                        //dump();
                    }
                } else if (readLevel > writeLevel && (readLevel - writeLevel) >= MAX_SPREAD) {
                    // Shrink the array to the SPREAD
                    int dropped = readLevel - writeLevel - SPREAD;
                    int newSize = levels.length - dropped;

                    HPQueueLevel[] newLevels = new HPQueueLevel[newSize];
                    System.arraycopy(levels, 0, newLevels, 0, writeLevel + SPREAD);
                    System.arraycopy(levels, readLevel, newLevels, writeLevel + SPREAD, (levels.length - readLevel));
                    levels = newLevels;

                    // Increment the read values
                    readLevel -= dropped;
                    levelHeight -= dropped;

                    //System.out.println( "REMOVED INSIDE" );
                    //dump();
                }
            }
        }

        return e;
        //}
    }

    @SuppressWarnings("unchecked")
    public synchronized E peek() {
        //synchronized( this )
        //{
        if (size == 0) {
            return null;
        }

        return (E) levels[ readLevel].get()[ readIndex];
        //}
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        if (size == 0) {
            return true;
        } else {
            //if ( peek() == null ) {
            //	throw new IllegalStateException( "WOAH!" );
            //}
            return false;
        }
    }

    /*
     * public boolean contains( Object object ) { // TODO: Handle this :( return
     * false;
	}
     */
    public synchronized void clear() {
        readLevel = 0;
        readIndex = 0;
        writeLevel = 0;
        writeIndex = 0;
        levelHeight = 0;
        size = 0;

        // Trim the height of the arrays
        if ((levels.length - levelHeight) >= MAX_SPREAD) {
            // Shrink the array to the SPREAD
            HPQueueLevel[] newLevels = new HPQueueLevel[levelHeight + SPREAD];
            System.arraycopy(levels, 0, newLevels, 0, newLevels.length);
            levels = newLevels;
        }

        // Wipe out all the object references
        for (int i = 0; i < levels.length; i++) {
            for (int j = 0; j < startSize; j++) {
                levels[ i].get()[ j] = null;
            }
        }
    }

    public void dump() {
        StringBuffer b = new StringBuffer();

        for (int i = 0; i < levels.length; i++) {
            b.append(i).append(' ');

            for (int j = 0; j < startSize; j++) {
                char c;
                Object o = levels[ i].get()[ j];
                if (o == null) {
                    c = 'O';
                } else {
                    c = '*';
                }

                if (i == readLevel && j == readIndex) {
                    if (readLevel == writeLevel && readIndex == writeIndex) {
                        c = 'X';
                    } else {
                        c = 'R';
                    }
                } else if (i == writeLevel && j == writeIndex) {
                    c = 'W';
                }

                if (i > levelHeight) {
                    c = '-';
                }

                b.append(c);
            }
            b.append("\n");
        }

        //b.append( "Read Index   = " ).append( readIndex ).append( '\n' );
        //b.append( "Read Level   = " ).append( readLevel ).append( '\n' );
        //b.append( "Write Index  = " ).append( writeIndex ).append( '\n' );
        //b.append( "Write Level  = " ).append( writeLevel ).append( '\n' );
        //b.append( "Level Height = " ).append( levelHeight ).append( '\n' );
        //b.append( "Array Size   = " ).append( levels.length ).append( '\n' );
        b.append("Size = ").append(size).append('\n');
        System.out.println(b.toString());
    }

    public String toString() {
        return "HPQueue";
    }
}
