package storage;

import java.io.*;
import java.net.*;
import java.util.*;

import common.*;
import rmi.*;
import naming.*;


/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    private File root = null;
    private int client_port = 0;
    private int command_port = 0;
    private Skeleton<Storage> storageSkeleton = null;
    private Skeleton<Command> commandSkeleton = null;
    private String hostname = null;
    private boolean started = false;
    private boolean stopping = false;
    private String separator = File.separator;
    private static final int bufferSize = 8192;
    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
    {
        if (root == null) {
            throw new NullPointerException("Error: null directory");
        }
        this.root = root.getAbsoluteFile();
        this.client_port = client_port;
        this.command_port = command_port;
    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
        which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        this(root, 0, 0);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if (started) {
            throw new RMIException("Storage server running");
        }
        if (stopping) {
            throw new RMIException("Storage server stopping");
        }

        if (client_port == 0) {
            storageSkeleton = new Skeleton<>(Storage.class, this);
        } else {
            storageSkeleton = new Skeleton<>(Storage.class, this,
                new InetSocketAddress(client_port));
        }

        if (command_port == 0) {
            commandSkeleton = new Skeleton<>(Command.class, this);
        } else {
            commandSkeleton = new Skeleton<>(Command.class, this,
                new InetSocketAddress(command_port));
        }
        storageSkeleton.start();
        commandSkeleton.start();

        Storage storageStub = Stub.create(Storage.class, storageSkeleton, hostname);
        Command commandStub = Stub.create(Command.class, commandSkeleton, hostname);

        // Path[] paths = getLocalPaths();
        Path[] paths = Path.list(root);
        Path[] duplicates = naming_server.register(storageStub, commandStub, paths);

        for (Path path : duplicates) {
            delete(path);
        }
        started = true;
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        synchronized(this) {
            stopping = true;
        }
        try {
            storageSkeleton.stop();
            commandSkeleton.stop();
            synchronized(this) {
                stopping = false;
                started = false;
            }
            stopped(null);
        } catch (Throwable throwableStop) {
            stopped(throwableStop);
        }
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File localFile = file.toFile(root);
        if (!localFile.exists() || localFile.isDirectory()) {
            throw new FileNotFoundException("Cannot get size for a directory.");
        }
        return localFile.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        File localFile = file.toFile(root);
        if (!localFile.exists() || localFile.isDirectory()) {
            throw new FileNotFoundException("Cannot read a directory");
        }

        if (!localFile.canRead()) {
            throw new IOException("File cannot be read.");
        }

        if (offset < 0 || offset > Integer.MAX_VALUE || length < 0
                || offset + length > localFile.length())
            throw new IndexOutOfBoundsException("invalid offset and/or length");

        RandomAccessFile fileReader = new RandomAccessFile(localFile, "r");

        byte[] bytes = new byte[length];
        fileReader.seek(offset);
        fileReader.readFully(bytes, 0, length);

        return bytes;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        if (file == null || data == null) {
            throw new NullPointerException("file or data is null");
        }
        File localFile = file.toFile(root);
        if (!localFile.exists() || localFile.isDirectory()) {
            throw new FileNotFoundException("Cannot write a directory");
        }

        if (!localFile.canWrite()) {
            throw new IOException("File cannot be written.");
        }

        if (offset < 0 || offset > Integer.MAX_VALUE)
            throw new IndexOutOfBoundsException("Invalid offset and/or length");

        RandomAccessFile fileWriter = new RandomAccessFile(localFile, "rw");

        try {
            fileWriter.seek(offset);
            fileWriter.write(data);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        if (file.isRoot()) {
            return false;
        }
        File localFile = file.toFile(root);
        File parent = localFile.getParentFile();

        try {
            parent.mkdirs();
            return localFile.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        if (path.isRoot()) {
            return false;
        }
        File localFile = path.toFile(root);
        boolean success = false;
        success = localDelete(localFile);
        if (success) {
            removeParent(localFile.getParentFile());
        }
        return success;
    }

    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        server.size(file);
        File localFile = file.toFile(root);
        if (localFile.isDirectory()) {
            throw new FileNotFoundException("Cannot copy directory");
        }
        if (localFile.exists()) {
            localDelete(localFile);
        }
        if (!create(file)) {
            throw new IOException("Cannot create file for copying");
        }

        RandomAccessFile fileWriter = new RandomAccessFile(localFile, "rw");

        long fileSize = server.size(file);
        byte[] fileBuffer;
        long offset;
        for (offset = 0; offset + bufferSize <= fileSize; offset += bufferSize) {
            fileBuffer = server.read(file, offset, bufferSize);
            fileWriter.write(fileBuffer);
        }
        int remaining = (int) (fileSize % bufferSize);
        if (remaining > 0) {
            fileBuffer = server.read(file, offset, remaining);
            fileWriter.write(fileBuffer);
        }
        fileWriter.close();
        return true;
    }

    // private Path[] getLocalPaths() {
    //     List<String> pathList = getPathList(root);
    //     Path[] paths = new Path[pathList.size()];
    //     String prefix = root.getAbsolutePath();

    //     for(int i = 0; i < paths.length; ++i) {
    //         String pathStr = pathList.get(i).replace(prefix, "").replace(separator, "/");
    //         paths[i] = new Path(pathStr);
    //     }

    //     return paths;
    // }

    // private List<String> getPathList(File file) {
    //     List<String> pathList = new ArrayList<>();

    //     if(!file.isDirectory()) {
    //         pathList.add(file.getAbsolutePath());
    //     } else {
    //         File[] files = file.listFiles();
    //         for(File f : files) {
    //             pathList.addAll(getPathList(f));
    //         }
    //     }
    //     return pathList;
    // }

    private boolean localDelete(File localFile) {
        if (localFile.isDirectory()) {
            File[] list = localFile.listFiles();
            for (File f : list) {
                localDelete(f);
            }
        }
        boolean success = false;
        success = localFile.delete();
        return success;
    }
    private void removeParent(File directory) {
        if (directory == root) {
            return;
        }
        File[] list = directory.listFiles();
        if (list.length == 0) {
            File parent = directory.getParentFile();
            directory.delete();
            removeParent(parent);
        }
    }
}