package llz.wit.edu.cn;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;

public class FSTest {
	static FileSystem fs;
	//目录操作
	public static void CreatePath()
	{
		Path dir = new Path("/test/llz");
		try {
			
			//如果目录存在，则先删除后，再建立。用fs.isDirectory(f)  fs.isFile(f)判断是文件还是目录，然后删除
			if(fs.exists(dir))
			{
				fs.delete(dir,true);
			}
			
			fs.mkdirs(dir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	//创建文件
	public static void createFile() throws IOException{
		Path path = new Path("/test/llz/wordcount.txt");
		FSDataOutputStream out = fs.create(path,true);
		
		String data = "I believe, for every drop of rain that falls, A flower grows";		
		out.writeBytes(data);
		out.flush();
		out.close();
	}
	
	//读取文件
	public static void readFile() {
		Path path = new Path("/test/llz//wordcount.txt");
		FSDataInputStream file = null;
		
		try {
			if(fs.isFile(path)){
				ByteBuffer buf = ByteBuffer.allocate(1024);
				 file = fs.open(path);
				 
				int read = 0;			
				while((read = file.read(buf)) != -1){			
					System.out.print(new String(buf.array()));
					buf.clear();
				};
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
		   IOUtils.closeStream(file);
		}
	}
	
	//读取文件列表
	public static void listFiles() throws FileNotFoundException, IOException{
		Path path = new Path("/user");
		// 获取其路径下的所有子文件夹或文件
		FileStatus[] listStatus = fs.listStatus(path);
		for (FileStatus fileStatus : listStatus) {
			System.out.println(fileStatus);
		}
		
		// 展示所有的文件
		RemoteIterator<LocatedFileStatus> listFiles = fs.listFiles(path, true);
		LocatedFileStatus next = null;
		while(listFiles.hasNext()){
			next = listFiles.next();
			System.out.println(next);
		}
	}
	
	public static void queryPosition() throws IOException{
		Path path = new Path("/test/llz/wordcount.txt");
		FileStatus fileStatus = fs.getFileStatus(path);
		
		// 获取文件所在集群位置
		BlockLocation[] fileBlockLocations = fs.getFileBlockLocations(fileStatus, 0, fileStatus.getLen());
		for (BlockLocation blockLocation : fileBlockLocations) {
			System.out.println(blockLocation);//0,60,datanode2,datanode1,datanode3
		}
		
		// 获取checksum
		FileChecksum fileChecksum = fs.getFileChecksum(path);
		//MD5-of-0MD5-of-512CRC32C:cb95b700877b44dab0fcfeb617d7f95d
		System.out.println(fileChecksum);
		
		// 获取集群中的所有节点信息
		DistributedFileSystem dfs = (DistributedFileSystem)fs;
		DatanodeInfo[] dataNodeStats = dfs.getDataNodeStats();
		for (DatanodeInfo datanodeInfo : dataNodeStats) {
			System.out.println(datanodeInfo);//192.168.2.151:50010
		}
	}

	
	public static void main(String[] args) throws IOException, URISyntaxException {
		// TODO Auto-generated method stub
		Configuration conf = new Configuration();
		conf.set("fs.default.name", "hdfs://datanode1:9000");
		 fs =  FileSystem.get(conf);
//		CreatePath();
//		createFile();
//		readFile();
//		listFiles();
		queryPosition();
	}

}
