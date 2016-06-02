package naming;

import common.*;
import rmi.RMIException;

import java.util.*;
import java.awt.PageAttributes;
import java.io.*;
import java.util.concurrent.locks.*;

enum Mode{
	CREATEDIR, CREATEFILE, DELETE, ISDIR
}

public class HashTree{
	private HashNode root;
	private LinkedList<ServerStub> stubList;
	public HashTree(LinkedList<ServerStub> stubList){
		this.stubList = stubList;
		this.root = new HashNode();
	}

	String[] list(Path directory) throws FileNotFoundException {
		lock(directory, false);
		Iterator<String> itr = directory.iterator();
		String name=null;
		HashNode subRoot = root;
		while (itr.hasNext()) {
			name = itr.next();
			subRoot = subRoot.getChild(name);
		}
		if(subRoot.hTable==null){
			unlock(directory, false);
			throw new FileNotFoundException(name+" is not a directory.");
		}
		String[] filelist = new String[subRoot.hTable.keySet().size()];
		filelist = subRoot.hTable.keySet().toArray(filelist);
		unlock(directory, false);
		return filelist;
	}
	
	public boolean createDirectory(Path directory) throws FileNotFoundException {
		lock(directory.parent(), true);
		Iterator<String> itr = directory.iterator();
		boolean flag = cdHelper(Mode.CREATEDIR, root, itr, null);
		unlock(directory.parent(), true);
		return flag;
	}

	public boolean createFile(Path file, ServerStub sServer) throws FileNotFoundException {
		lock(file.parent(), true);
		Iterator<String> itr = file.iterator();
		boolean flag = cdHelper(Mode.CREATEFILE, root, itr, sServer);
		unlock(file.parent(), true);
		return flag;
	}

	public boolean createFileRecursive(Path file, ServerStub sServer) {
		Iterator<String> itr = file.iterator();
		HashNode currNode = root;
		String currPath = itr.next();
		boolean created = true;
		Deque<HashNode> stack = new ArrayDeque<>();
		while (itr.hasNext()) {
			currNode.lockRead();
			stack.push(currNode);
			if (currNode.hasFile(currPath)) {
				created = false;
				break;
			}
			if (!currNode.hasDirectory(currPath)) {
				currNode.unlockRead();
				currNode.lockWrite();
				currNode.create(currPath, null);
				currNode.unlockWrite();
				currNode.lockRead();
			}
			currNode = currNode.getChild(currPath);
			currPath = itr.next();
		}
		if (created) {
			currNode.lockRead();
			stack.push(currNode);
			if (currNode.hasFile(currPath) || currNode.hasDirectory(currPath)) {
				created = false;
			} else {
				currNode.unlockRead();
				currNode.lockWrite();
				currNode.create(currPath, sServer);
				currNode.unlockWrite();
				currNode.lockRead();
			}
		}
		while (!stack.isEmpty()) {
			stack.pop().unlockRead();
		}
		return created;
	}

	public boolean delete(Path path) throws FileNotFoundException {
		lock(path.parent(), true);
		Iterator<String> itr = path.iterator();	
		boolean flag = cdHelper(Mode.DELETE, root, itr, null,path);
		unlock(path.parent(), true);
		if(!flag){
			throw new FileNotFoundException("The file "+path.toString()+" doesn't exist.");
		}
		return flag;
	}

	public boolean isDirectory(Path path) throws FileNotFoundException {
		lock(path, false);
		Iterator<String> itr = path.iterator();	
		boolean flag = cdHelper(Mode.ISDIR, root, itr, null);
		unlock(path, false);
		return flag;
	}
	
	public void lock(Path path, boolean exclusive) throws FileNotFoundException{
		if(path.isRoot()){
			if(exclusive)root.lockWrite();
			else root.lockRead();
			return;
		}
		Iterator<String> itr = path.iterator();
		lock_helper(root, itr, exclusive,path);
	}
	
	private void lock_helper(HashNode subRoot, Iterator<String>itr, boolean exclusive,Path path) throws FileNotFoundException{
		String name = itr.next();
		
		if (itr.hasNext()){	//if name is not the last path component
			subRoot.lockRead();
			if (subRoot.hasDirectory(name)){	//if name exist in subRoot as a directory
				try {
					lock_helper(subRoot.getChild(name), itr, exclusive,path);
				} catch (FileNotFoundException e) {
					subRoot.unlockRead();
					throw e;
				}
			}
			else{	//if name doesn't exist in subRoot, or name is a file in subRoot
				subRoot.unlockRead();
				throw new FileNotFoundException("name doesn't exist in subRoot, or name is a file in subRoot");
			}
		}
		else{	//if name is the last path component
			subRoot.lockRead();
			if(subRoot.hasDirectory(name)==false && subRoot.hasFile(name)==false){
				subRoot.unlockRead();
				throw new FileNotFoundException("The file indicated by iterator doesn't exist.");
			}
			HashNode final_item  = subRoot.getChild(name);
			if(exclusive){
				final_item.lockWrite();
				if(final_item.hTable==null)
					writeDereplicate(final_item, path);
			}
			else{
				final_item.lockRead();
				if(final_item.hTable==null)
					readReplicate(final_item, path);
			}
		}
	}
	
	/**
	 * generate read replicate when necessary
	 * whenever there are accumulated 20 read, select a storage server to generate a new copy of the file, and add the stub pair in the hashnode list.
	 * @param hashNode
	 * @return if there is no suitable storage stub, return false; otherwise, return true.
	 */
	private synchronized boolean readReplicate(HashNode hashNode,Path file){
		synchronized (hashNode) {
			for(ServerStub stubpair:stubList){
				if(hashNode.sServerList.contains(stubpair))
					continue;
				try {
					stubpair.commandStub.copy(file, hashNode.sServerList.get(0).storageStub);
				} catch (RMIException | IOException e) {
					continue;
				}
				hashNode.sServerList.add(stubpair);
				return true;
			}
		}
		return false;
	}
	
	private synchronized boolean writeDereplicate(HashNode hashNode, Path file){
		while(hashNode.sServerList.size()>1) {
			ServerStub removed = hashNode.sServerList.removeLast();
			try {
				removed.commandStub.delete(file);
			} catch (RMIException e) {//ignore
			}
		}
		return true;
	}
	
	public void unlock(Path path, boolean exclusive) throws IllegalArgumentException{
		if(path.isRoot()){
			if(exclusive)root.unlockWrite();
			else root.unlockRead();
			return;
		}
		Iterator<String> itr = path.iterator();
		unlock_helper(root, itr, exclusive);
	}
	
	private void unlock_helper(HashNode subRoot, Iterator<String>itr, boolean exclusive) throws IllegalArgumentException{
		String name = itr.next();
		if (itr.hasNext()){	//if name is not the last path component
			if (subRoot.hasDirectory(name)){	//if name exist in subRoot as a directory
				unlock_helper(subRoot.getChild(name), itr, exclusive);
			}
			else{	//if name doesn't exist in subRoot, or name is a file in subRoot
				throw new IllegalArgumentException("name doesn't exist in subRoot, or name is a file in subRoot");
			}
		}
		else{	//if name is the last path component
			if(subRoot.hasDirectory(name)==false && subRoot.hasFile(name)==false){
				throw new IllegalArgumentException("The file indicated by iterator doesn't exist.");
			}
			HashNode final_item = subRoot.getChild(name);
			if(exclusive){
				final_item.unlockWrite();
			}
			else{
				final_item.unlockRead();
			}
		}
		subRoot.unlockRead();
	}
	
	protected boolean cdHelper(Mode mode,Path path, ServerStub sServer) throws FileNotFoundException{
		if(mode==Mode.DELETE){
			return cdHelper(mode, root, path.iterator(),sServer,path);
		}
		else {
			return cdHelper(mode, root, path.iterator(),sServer);	
		}
	}
	private boolean cdHelper(Mode mode, HashNode subRoot, Iterator<String> itr, ServerStub sServer) throws FileNotFoundException {
		return cdHelper(mode, subRoot, itr, sServer,null);
	}
	private boolean cdHelper(Mode mode, HashNode subRoot, Iterator<String> itr, ServerStub sServer, Path path) throws FileNotFoundException {
		if(itr.hasNext()==false){
			switch (mode) {
			case CREATEDIR: return false;
			case CREATEFILE: return false;
			case DELETE: return false;
			case ISDIR: return true;
			}
		}
		String n = itr.next();
		if (itr.hasNext()){
			return cdHelper(mode, subRoot.getChild(n), itr, sServer);
		}
		else{
			if (mode == Mode.ISDIR){
				if (subRoot.hasDirectory(n)){
					return true;
				}
				if (subRoot.hasFile(n)){
					return false;
				}
				throw new FileNotFoundException();
			}
			else{
				if (subRoot.hasDirectory(n) || subRoot.hasFile(n)){
					if (mode == Mode.DELETE){
						subRoot.delete(n,path);
						return true;
					}
					else{
						return false;
					}			
				}
				else{
					if (mode == Mode.DELETE){
						return false;
					}
					else{
						subRoot.create(n, sServer);
						return true;
					}
				}
			}
		}	
	}


	public ServerStub getStorage(Path file) throws FileNotFoundException {
		Iterator<String> itr = file.iterator();
		HashNode subRoot = root;
		String n;
		while (itr.hasNext()){
			n = itr.next();
			if (itr.hasNext()){
				if (subRoot.hasDirectory(n)){
					subRoot = subRoot.getChild(n);
				}
				else{
					throw new FileNotFoundException();
				}
			}
			else{
				if (subRoot.hasFile(n)){
					return subRoot.getFileStorage(n);
				}
				else{
					throw new FileNotFoundException();
				}
			}
		}
		return null;
	}

	public class HashNode{
		private int stub_index = 0;
		private LinkedList<ServerStub> sServerList = null;
		private Hashtable<String, HashNode> hTable = null;
		//private ReadWriteLock lock = new ReentrantReadWriteLock(true);
		ReadWriteLock lock = new StampedLock().asReadWriteLock();

		public HashNode(){  // Directory constructor
			hTable = new Hashtable<>();
		}

		public HashNode(ServerStub s){ // File constructor
			sServerList = new LinkedList<ServerStub>();
			sServerList.add(s);
		}

		public boolean hasDirectory(String n){
			HashNode node = hTable.get(n);
			return node != null && node.hTable != null;
		}

		public boolean hasFile(String n){
			HashNode node = hTable.get(n);
			return node != null && node.sServerList != null;
		}

		public HashNode getChild(String n){
			return hTable.get(n);
		}

		public ServerStub getFileStorage(String n){
			HashNode hashNode = hTable.get(n);
			hashNode.stub_index++;
			return hashNode.sServerList.get(hashNode.stub_index%hashNode.sServerList.size());
		}

		public void create(String n, ServerStub s){
			HashNode node;
			if (s == null){
				node = new HashNode();
			}
			else{
				node = new HashNode(s);
			}
			hTable.put(n, node);
		}

		public void delete(String n){
			hTable.remove(n);
		}
		
		public void delete(String n, Path path){
			HashNode child = getChild(n);
			for(ServerStub stubpair:child.collectAllStubs()){
				try {
					stubpair.commandStub.delete(path);
				} catch (RMIException e) {//ignore
				}
			}
			delete(n);
		}
	
		public void lockRead(){
			lock.readLock().lock();
		}

		public void unlockRead(){
			try{lock.readLock().unlock();}
			catch(Throwable t){
				t.printStackTrace();
				throw t;
				
			}
		}

		public void lockWrite(){
			lock.writeLock().lock();
		}

		public void unlockWrite(){
			lock.writeLock().unlock();
		}
		public HashSet<ServerStub> collectAllStubs(){
			if(hTable==null){	//if this node is a file
				return new HashSet<ServerStub>(stubList);
			}
			else{	//if this node is a directory
				HashSet<ServerStub> collector = new HashSet<>();
				for(HashNode hashNode:hTable.values()){
					collector.addAll(hashNode.collectAllStubs());
				}
				return collector;
			}
		}
	}
}
