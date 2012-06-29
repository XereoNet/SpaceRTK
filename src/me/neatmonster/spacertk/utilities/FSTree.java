package me.neatmonster.spacertk.utilities;


import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Implementation of an m-ary tree to represent a folder hierarchy.
 */
public class FSTree implements Iterable<String>, Externalizable {
    static final byte serialVersion = 1;
    private static final int BUILDER_SIZE = 128;
    private String name;
    private long size;
    private Map<String, FSTree> children;
    private FSTree parent;
    private Iterator<String> iterator;

    /**
     * Same as FSTree(null, 0L, null);
     */
    public FSTree() {
        children = new HashMap<String, FSTree>();
    }

    /**
     * Constructs an FSTree with an initial file name and size of 0.
     * @param name name of the initial file in the hierarchy.
     */
    public FSTree(String name) {
        this.name = name;
        size = 0L;
        children = new HashMap<String, FSTree>();
    }

    /**
     * Constructs an FSTree with an initial file name and size.
     * @param name name of the initial file in the hierarchy.
     * @param size size of the initial file in the hierarchy in bytes.
     */
    public FSTree(String name, long size) {
        this.name = name;
        this.size = size;
        children = new HashMap<String, FSTree>();
    }

    /**
     * Constructs an FSTree with an initial file name and size.
     * @param name name of the initial file in the hierarchy.
     * @param size size of the initial file in the hierarchy in bytes.
     * @param parent parent of this node.
     */
    public FSTree(String name, long size, FSTree parent) {
        this.name = name;
        this.size = size;
        this.parent = parent;
        children = new HashMap<String, FSTree>();
    }

    /**
     * Add a file to the tree relative to the file that this node represents
     * @param name the name of the file
     * @param size the size of the file
     */
    public void add(String name, long size) {
        StringTokenizer tokenizer = new StringTokenizer(name, "\\/");
        addRecurse(tokenizer, size);
    }

    /**
     * Recursive helper method for add()
     */
    private void addRecurse(StringTokenizer tokenizer, long size){
        iterator = null;
        this.size += size;

        if(tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken();

            if(tokenizer.hasMoreTokens()) {
                FSTree child = children.get(name);

                if(child == null) {
                    child = new FSTree(name, 0L, this);
                    children.put(name, child);
                }
                child.addRecurse(tokenizer, size);
            } else {
                children.put(name, new FSTree(name, size, this));
            }

        }
    }

    /**
     * Enumerate the relative paths of the leaves in this tree.
     * @param leaves list to store the enumeration in.
     */
    public void enumerateLeaves(List<String> leaves) {
        if(!children.isEmpty())
            enumerateRecurse(leaves, new StringBuilder(BUILDER_SIZE));
        else
            leaves.add(name);
    }

    /**
     * Recursive helper method for enumerateLeaves().
     */
    private void enumerateRecurse(List<String> leaves, StringBuilder builder) {
        int pos = builder.length();
        if(pos > 0)
            builder.append(File.separatorChar);
        builder.append(name);

        if(children.isEmpty())
            leaves.add(builder.toString());
        else
            for(FSTree child : children.values())
                child.enumerateRecurse(leaves, builder);

        builder.setLength(pos);
    }


    /**
     * Get the entire size of the folder hierarchy (or file) represented by this FSTree measured in bytes.
     * @return The size of the folder hierarchy (or file) represented by this FSTree measured in bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * Get the name of the root file in the tree.
     * @return the name of the root file in the tree.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the parent node of this node.
     * @return the parent node of this node.
     */
    public FSTree getParent() {
        return parent;
    }

    /**
     * Get an iterator associated with a list returned by enumerateLeaves(List).
     * @return An iterator associated with a list returned by enumerateLeaves(List).
     */
    public Iterator<String> iterator() {
        if(iterator == null)
            iterator = new FSTreeIterator(this);
        return iterator;
    }

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        /*
         * Serialization format:
         * byte 1: serial version
         * byte 2: name length (l)
         * byte 3 to l+3: name
         * byte l+4 to l+67: size of node
         * byte l+68: children count
         * byte l+69 to n: children (recursive)
         */
        if(name.length() > Short.MAX_VALUE)
            throw new IOException("Name of node exceeds maximum allowed size of "+Short.MAX_VALUE);
        if(children.size() > Short.MAX_VALUE)
            throw new IOException("Number of subfolders in "+name+" exceeds maximum allowed number: "+Short.MAX_VALUE);

        oo.writeByte(serialVersion);
        oo.writeByte(name.length());
        oo.writeBytes(name);
        oo.writeLong(size);
        oo.writeByte(children.size());
        for(FSTree child : children.values()) {
            child.writeExternal(oo);
        }
    }

    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        if(oi.readByte() != serialVersion)
            throw new IOException("Serial version mismatch");

        int nameLength = (short)oi.readByte() & 0xFF; //Remove possible negative sign extension from the cast
        byte[] name = new byte[nameLength];

        oi.readFully(name, 0, nameLength);
        this.name = new String(name);

        size = oi.readLong();

        int childCount = (short)oi.readByte() & 0xFF;
        for(int i = 0; i < childCount; i++) {
            FSTree child = new FSTree(null, 0L, this);
            child.readExternal(oi);
            children.put(child.getName(), child);
        }
    }


    private class FSTreeIterator implements Iterator<String> {
        private List<String> leaves;
        private Iterator<String> leavesIterator;

        public FSTreeIterator(FSTree nextNode) {
            leaves = new LinkedList<String>();
            enumerateLeaves(leaves);
            leavesIterator = leaves.iterator();
        }

        @Override
        public boolean hasNext() {
            return leavesIterator.hasNext();
        }

        @Override
        public String next() {
            if(iterator != this)
                throw new ConcurrentModificationException();

            return leavesIterator.next();
        }

        /**
         * Unsupported by this iterator.
         * @throws UnsupportedOperationException when invoked.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
