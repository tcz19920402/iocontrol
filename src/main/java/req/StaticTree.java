package req;

import req.Rand.RandomGenerator;
import req.Rand.UniformGenerator;
import util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StaticTree{
	static Log log=Log.get();
	RandomGenerator uniform;
	List<RandTreeNode> emptyDirs=new ArrayList<>();
	List<RandTreeNode> nonEmptyDirs=new ArrayList<>();  //  it's pointless to ls empty dirs
	List<RandTreeNode> files=new ArrayList<>();
	String sep=null;

	public void shuffleFiles(String file) throws IOException{
		List<Integer> list=parseShuffle(file);
		if(list.size()!=files.size())
			throw new IllegalArgumentException("Internal file size: "+files.size()+" shuffle size: "+list.size());
		for(int i=0;i<files.size();++i){
			RandTreeNode n1=files.get(i);
			int j=i;
			for(;;){
				int k=list.get(j);
				list.set(j,j);
				if(k==i) break;
				files.set(j,files.get(k));
				j=k;
			}
			files.set(j,n1);
		}
	}

	static public List<Integer> parseShuffle(String file) throws IOException{
		try(BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8"))){
			List<Integer> result=new ArrayList<>();
			for(String line;(line=br.readLine())!=null;){
				if(line.trim().length()==0) continue;
				try{
					result.add(Integer.parseInt(line));
				}catch(NumberFormatException e){
					log.w(e);
				}
			}
			return result;
		}
	}

	protected RandTreeNode emptyNode(){
		return new RandTreeNode();
	}

	protected StaticTree(){
	}

	protected StaticTree(RandomGenerator uniform){
		this.uniform=uniform;
	}

	public StaticTree(RandomGenerator uniform,String sep){
		this.uniform=uniform;
		this.sep=sep;
	}

	public static StaticTree getStaticTree(String filename,UniformGenerator uniform) throws IOException{
		StaticTree tree=new StaticTree(uniform);
		new TreeParser<StaticTree, RandTreeNode>().parse(tree,filename,false);
		return tree;
	}

	protected String randName(){
		return String.format("%8X%8X",uniform.nextInt(),uniform.nextInt());
	}

	protected class RandTreeNode{
		RandTreeNode parent=null;
		String name=null;
		long size=0;

		protected RandTreeNode(){
		}

		protected RandTreeNode(RandTreeNode parent,String name,long size){
			this.parent=parent;
			this.name=name;
			this.size=size;
		}

		protected void setParent(RandTreeNode p){
			parent=p;
		}

		protected RandTreeNode(RandTreeNode parent,String name){
			this.parent=parent;
			this.name=name;
		}

		@Override
		public String toString(){
			if(parent!=null)
				return parent.toString()+sep+name;
			else return name;
		}
	}

	public int getNonEmptyDirSize(){
		return nonEmptyDirs.size();
	}

	public int getFileSize(){
		return files.size();
	}

	public Request ls(int index){
		if(index<nonEmptyDirs.size())
			return new Request(nonEmptyDirs.get(index).toString());
		else return null;
	}

	public Request fileInfo(int index){
		if(index>=files.size()) return null;
		RandTreeNode result=files.get(index);
		return new Request(result.toString(),result.size);
	}

	public void updateFileSize(int index,long newSize){
		files.get(index).size=newSize;
	}

	protected static class TreeParser<Tree extends StaticTree,Node extends StaticTree.RandTreeNode>{
		static Pattern reg=Pattern.compile("(\\[\\s*(\\d+)\\s+(\\d+)\\]\\s+)?(.+)");
		Deque<Node> ancestors=new ArrayDeque<>();
		Deque<Node> pendingFiles=new ArrayDeque<>();

		protected void rollback(int indent,Tree tree,boolean fillEmpty){
			if(ancestors.size()>0){
				Node last=ancestors.removeLast();
				if(ancestors.size()==0) tree.emptyDirs.add(last);
				else{
					if(tree.sep==null){
						pendingFiles.add(last);
					}else{
						if(last.name.endsWith(tree.sep)){    //  dir
							if(fillEmpty) tree.emptyDirs.add(last);  //  empty dir
						}else tree.files.add(last); //  file
					}
					while(ancestors.size()>indent) ancestors.removeLast();
				}
			}
		}

		protected void vacuum(Tree tree,boolean fillEmpty){
			Node node;
			while((node=pendingFiles.pollFirst())!=null){
				if(!node.name.endsWith(tree.sep)) tree.files.add(node);
				else if(fillEmpty) tree.emptyDirs.add(node);
			}
		}

		protected void parse(Tree tree,String filename,RandomGenerator uniform,boolean fillEmpty) throws IOException{
			tree.uniform=uniform;
			parse(tree,filename,fillEmpty);
		}

		protected void parse(Tree tree,String filename,boolean fillEmpty) throws IOException{
			long count=1;
			boolean pattern=true;
			try(BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(filename),"UTF8"))){
				for(String line;(line=br.readLine())!=null;++count){
					//  skip blank line
					if(line.trim().length()==0) continue;
					int indent=0;
					String name=line;
					while(name.startsWith("└── ") || name.startsWith("├── ") || name.startsWith("│   ") || name.startsWith("    ")){
						name=name.substring(4);
						++indent;
					}
					name=name.trim();
					Node node=(Node)tree.emptyNode();
					//  set name and possibly size
					if(indent==0 && line.startsWith("directory")){  //  root
						name=name.replaceFirst("directory\\s*","");
					}else if(pattern && indent>0){   //  non root, check [size,modify time] part
						Matcher m=reg.matcher(line);
						if(m.matches()){
							name=m.group(4);
							if(m.group(1)!=null){
								node.size=Long.parseLong(m.group(2));
								//  don't care about modify time
							}else{
								pattern=false;
							}
						}else throw new InvalidParameterException(String.format("%d: %s",count,line));
					}
					node.name=name;
					//  cannot find direct parent
					if(ancestors.size()<indent) throw new InvalidParameterException(String.format("%d: %s",count,line));
					//  push to lists
					boolean firstChild=(ancestors.size()==indent);
					if(!firstChild){
						rollback(indent,tree,fillEmpty);
					}
					if(indent>0){
						Node parent=ancestors.getLast();
						node.setParent(parent);
						if(firstChild){
							tree.nonEmptyDirs.add(parent);
							if(tree.sep==null) pendingFiles.remove(parent);
							if(tree.sep==null && indent>1){
								tree.sep=parent.name.substring(parent.name.length()-1);
								vacuum(tree,fillEmpty);
							}
						}
					}
					if(tree.sep!=null && !name.endsWith(tree.sep))
						tree.files.add(node);
					else ancestors.add(node);
				}
				rollback(0,tree,fillEmpty);
				vacuum(tree,fillEmpty);
			}
		}
	}
}
