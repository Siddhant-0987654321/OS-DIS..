package common;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
//import java.nio.file.Path;
/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Comparable<Path>, Serializable
{
	private static final long serialVersionUID = -5684216814040987708L;
	private static final String DELIMETER = "/";
	//private java.nio.file.Path sys_path;
	String str_path;
	
    /** Creates a new path which represents the root directory. */
    public Path()
    {
    	this(DELIMETER);
    }
    
    private static java.nio.file.Path str2JavaPath(String str){
    	return Paths.get(str).normalize();
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
    	this(str2JavaPath(path.str_path).resolve(component).toString());
    	if(component.contains("/")){
    		throw new IllegalArgumentException("Component includes the separator. ("+component+")");
    	}
    	if(component.equals("")){
    		throw new IllegalArgumentException("Component is the empty string.");
    	}
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
    	if(path.length()==0 || path.charAt(0)!=DELIMETER.charAt(0)){
    		throw new IllegalArgumentException("The path string does not begin with a forward slash ("+Paths.get(path).toString()+").");
    	}
    	if(path.contains(":")){
    		throw new IllegalArgumentException("The path string contains a colon character ("+Paths.get(path).toString()+").");
    	}
    	this.str_path = str2JavaPath(path).toString();
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
    	if(isRoot()){
    		return new ArrayList<String>().iterator();
    	}
    	String[] components = str_path.split(DELIMETER);
    	return Arrays.asList(components).subList(1, components.length).iterator();
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
    	LinkedList<Path> pathLinkedList = recursive_list(directory,new Path()); 
    	Path[] paths = new Path[pathLinkedList.size()];
    	for (int i=0;i<pathLinkedList.size();i++){
    		paths[i]=pathLinkedList.get(i);
    	}
    	return paths;
    }
    private static LinkedList<Path> recursive_list(File directory, Path prefix){
    	LinkedList<Path> linkedList = new LinkedList<Path>();
    	File[] files = directory.listFiles();
    	for (File file : files){
    		if(file.isFile()){
    			linkedList.add(new Path(prefix,file.getName()));
    		}
    		else if(file.isDirectory()){
    			linkedList.addAll(recursive_list(file,new Path(prefix,file.getName())));
    		}
    	}
    	return linkedList;
    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
    	return str2JavaPath(str_path).getNameCount()==0;
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
    	java.nio.file.Path parentPath = str2JavaPath(str_path).getParent();
    	if (parentPath == null)
    		throw new IllegalArgumentException("The path represents the root directory, and therefore has no parent.");
    	return new Path(parentPath.toString());
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
    	java.nio.file.Path lastPath = str2JavaPath(str_path).getFileName();
    	if(lastPath == null)
    		throw new IllegalArgumentException("The path represents the root directory, and therefore has no last component");
        return lastPath.toString();
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if it is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
    	int len_this = str2JavaPath(str_path).getNameCount();
    	int len_other = str2JavaPath(other.str_path).getNameCount();
    	if(len_other<=len_this){
    		if(len_other==0)
    			return true;
    		if(str2JavaPath(str_path).subpath(0, len_other).equals(str2JavaPath(other.str_path).subpath(0, len_other)))
    			return true;
    	}
    	return false;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
    	return root.toPath().resolve(str_path.substring(1)).toFile();
    }

    /** Compares this path to another.

        <p>
        An ordering upon <code>Path</code> objects is provided to prevent
        deadlocks between applications that need to lock multiple filesystem
        objects simultaneously. By convention, paths that need to be locked
        simultaneously are locked in increasing order.

        <p>
        Because locking a path requires locking every component along the path,
        the order is not arbitrary. For example, suppose the paths were ordered
        first by length, so that <code>/etc</code> precedes
        <code>/bin/cat</code>, which precedes <code>/etc/dfs/conf.txt</code>.

        <p>
        Now, suppose two users are running two applications, such as two
        instances of <code>cp</code>. One needs to work with <code>/etc</code>
        and <code>/bin/cat</code>, and the other with <code>/bin/cat</code> and
        <code>/etc/dfs/conf.txt</code>.

        <p>
        Then, if both applications follow the convention and lock paths in
        increasing order, the following situation can occur: the first
        application locks <code>/etc</code>. The second application locks
        <code>/bin/cat</code>. The first application tries to lock
        <code>/bin/cat</code> also, but gets blocked because the second
        application holds the lock. Now, the second application tries to lock
        <code>/etc/dfs/conf.txt</code>, and also gets blocked, because it would
        need to acquire the lock for <code>/etc</code> to do so. The two
        applications are now deadlocked.

        @param other The other path.
        @return Zero if the two paths are equal, a negative number if this path
                precedes the other path, or a positive number if this path
                follows the other path.
     */
    @Override
    public int compareTo(Path other)
    {
    	return str_path.compareTo(other.str_path);
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
    	if(other instanceof Path){
    		if (str_path.equals(((Path) other).str_path)){
    			return true;
    		}
    	}
    	return false;
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
    	return str_path.hashCode();
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
    	return str_path;
    }
}