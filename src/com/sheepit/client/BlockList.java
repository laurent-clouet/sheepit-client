package com.sheepit.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


public class BlockList {
	static BlockList blocklist_instance = null;
	
	private HashMap blockedJobs = null;
	private FileWriter blockListWriter = null;
	private File blockListFile = null;
	private String block_list = null;
	
	private BlockList() {
		blockedJobs = new HashMap();
	}
	
	public void setBlockList(String blockList){
		try{
			blockListFile  = new File(blockList);
			if(blockListFile.exists()){
				load();
			}
			else{
				blockListFile.createNewFile();
			}
			System.out.println(blockListFile.getAbsolutePath());
			blockListWriter = new FileWriter(blockListFile, true);
			
		}
		catch(Exception e){
			System.out.println("Problem on initializing Blocklist "+e.getMessage());
		}
	}
	
	public static BlockList getInstance(){
		if(blocklist_instance == null){
			blocklist_instance = new BlockList();
		}
		return blocklist_instance;
	}
	
	public boolean isBlocked(String sceneMD5){
		boolean result = blockedJobs.containsKey(sceneMD5); 
		return result;
	}
	
	public void blockJob(String sceneMD5, String name){
		blockedJobs.put(sceneMD5, name);
		try {
			blockListWriter.write(sceneMD5 + ";" + name + "\n");
			blockListWriter.flush();
		} catch (IOException e) {
			System.out.println("Problem on writing blocklist");
		}
	}
	
	private void load() throws FileNotFoundException{
		BufferedReader blockListReader = new BufferedReader( new FileReader(blockListFile));
		try{
			String line;
			String lineElements[];
			while((line = blockListReader.readLine()) != null){
				lineElements = line.split(";");
				blockedJobs.put(lineElements[0], lineElements[1]);
				System.out.println("block by list: " + line);
				
			}
			blockListReader.close();
		}
		catch(Exception e){
			System.out.println("Problem on Storing Blocklist");
		}

	}
}
