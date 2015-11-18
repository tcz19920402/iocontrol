package req;

import req.Rand.RandomGenerator;
import req.Rand.UniformGenerator;
import util.AutoLock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DynamicTree extends StaticTree{
	ReadWriteLock lock=new ReentrantReadWriteLock();

	protected DynamicTree(){
		super();
	}

	protected DynamicTree(RandomGenerator uniform){
		super(uniform);
	}

	public DynamicTree(RandomGenerator uniform,String sep){
		super(uniform,sep);
	}

	public static DynamicTree getDynamicTree(String filename,UniformGenerator uniform) throws IOException{
		DynamicTree tree=new DynamicTree(uniform);
		new TreeParser<DynamicTree, DynamicRandTreeNode>().parse(tree,filename,true);
		return tree;
	}

	@Override
	protected RandTreeNode emptyNode(){
		return new DynamicRandTreeNode();
	}

	protected class DynamicRandTreeNode extends RandTreeNode{
		Map<String, DynamicRandTreeNode> children=null;

		DynamicRandTreeNode removeUp(){
			if(parent!=null){
				DynamicRandTreeNode p=(DynamicRandTreeNode)parent;
				p.children.remove(this.name);
				if(p.children.size()==0){
					nonEmptyDirs.remove(parent);
					emptyDirs.add(parent);
				}
				return p;
			}
			return null;
		}

		@Override
		protected void setParent(RandTreeNode p){
			super.setParent(p);
			DynamicRandTreeNode parent=(DynamicRandTreeNode)p;
			if(parent.children==null) parent.children=new HashMap<>();
			parent.children.put(name,this);
		}

		DynamicRandTreeNode createFile(int index){
			if(children==null){
				children=new HashMap<>();
			}
			String newName=randName();
			while(children.containsKey(newName)) newName=randName();
			DynamicRandTreeNode child=new DynamicRandTreeNode();
			child.name=newName;
			child.parent=this;
			children.put(newName,child);
			files.add(0,child);
			if(children.size()==1){
				emptyDirs.remove(index);
				nonEmptyDirs.add(this);
			}
			return child;
		}

		DynamicRandTreeNode createDir(int index){
			if(children==null){
				children=new HashMap<>();
			}
			DynamicRandTreeNode child=new DynamicRandTreeNode();
			String newName=randName()+sep;
			while(children.containsKey(newName)) newName=randName()+sep;
			child.name=newName;
			child.parent=this;
			children.put(newName,child);
			emptyDirs.add(0,child);
			if(children.size()==1){
				emptyDirs.remove(index);
				nonEmptyDirs.add(this);
			}
			return child;
		}
	}

	public int getEmptyDirSize(){
		return emptyDirs.size();
	}


	public int getAllDirSize(){
		return emptyDirs.size()+nonEmptyDirs.size();
	}

	@Override
	public Request ls(int index){
		try(AutoLock ignored=AutoLock.lock(lock.readLock())){
			return super.ls(index);
		}
	}

	@Override
	public Request fileInfo(int index){
		try(AutoLock ignored=AutoLock.lock(lock.readLock())){
			return super.fileInfo(index);
		}
	}

	public Request rmdir(int index){    //  index in emptyDirs
		try(AutoLock ignored=AutoLock.lock(lock.writeLock())){
			if(index<emptyDirs.size()){
				DynamicRandTreeNode result=(DynamicRandTreeNode)emptyDirs.get(index);
				result.removeUp();
				emptyDirs.remove(index);
				return new Request(result.toString());
			}else return null;
		}
	}

	public Request delete(int index){   //  index in files
		try(AutoLock ignored=AutoLock.lock(lock.writeLock())){
			if(index<files.size()){
				DynamicRandTreeNode result=(DynamicRandTreeNode)files.get(index);
				result.removeUp();
				files.remove(index);
				return new Request(result.toString(),result.size);
			}else return null;
		}
	}

	private Request create(int index,boolean isDir){
		try(AutoLock ignored=AutoLock.lock(lock.writeLock())){
			DynamicRandTreeNode parent;
			if(index>=emptyDirs.size()){
				index-=emptyDirs.size();
				if(index>=nonEmptyDirs.size()) return null;
				else parent=(DynamicRandTreeNode)nonEmptyDirs.get(index);
			}else parent=(DynamicRandTreeNode)emptyDirs.get(index);
			DynamicRandTreeNode child=isDir ? parent.createDir(index) : parent.createFile(index);
			return new Request(child.toString());
		}
	}

	public Request createFile(int index){   //  index in nonEmptyDirs, then emptyDirs
		return create(index,false);
	}

	public Request createDir(int index){
		return create(index,true);
	}
}
