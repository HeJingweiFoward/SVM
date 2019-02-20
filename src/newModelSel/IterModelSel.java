package newModelSel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;


public class IterModelSel  extends Configured implements  Tool{
	 public Job ijob;
	 
	 public int run(String[] args) throws Exception {
		  Configuration cnf = HBaseConfiguration.create();	 
		  cnf.set("hbase.zookeeper.quorum", "192.168.2.151:2181,192.168.2.152:2181,192.168.2.153:2181,192.168.2.154:2181");// zookeeper地址
		  cnf.set("hbase.zookeeper.property.clientPort", "2181");
          cnf.set("mapreduce.app-submission.cross-platform", "true");    
          cnf.set("mapreduce.job.jar", "e:\\2\\SVMCas.jar");         
          cnf.set("mapreduce.job.running.reduce.limit",args[2]);//设置最多允许reduce 的运行个数
          cnf.setInt("nrFlod",Integer.valueOf(args[3]));  //交叉验证重数    
          cnf.set("ExperimentNum", args[4]); //第几次实验  
          
		  ijob = Job.getInstance(cnf, "ModelSelect");
		  ijob.addCacheFile(new URI("hdfs://datanode1:9000/SVM/DataSet/a8a"));
	      ijob.addFileToClassPath( new Path("hdfs://datanode1:9000/SVM/libsvm.jar"));
          int reduceNum = Integer.parseInt(args[1]);
	      ijob.setNumReduceTasks(reduceNum);//reduce的并发个数   
          
          ijob.setJarByClass(this.getClass());
	      ijob.setMapperClass(ModelSelectMapper.class);
	      ijob.setMapOutputKeyClass(IntWritable.class);
	      ijob.setMapOutputValueClass(Text.class);
	      ijob.setOutputKeyClass(NullWritable.class);
	      ijob.setOutputValueClass(Text.class);
	      TableMapReduceUtil.initTableReducerJob("SVMResultT1", ModelSelectReducer.class, ijob);
	      FileInputFormat.addInputPaths(ijob, args[0]);
	      return ijob.waitForCompletion(true) ? 0 : 1;
	 }
	 
	 public static class ModelSelectMapper extends Mapper<Object, Text, IntWritable, Text> {
	      private Text param = new Text();
	      private IntWritable partitionIndex = new IntWritable();
	      public void map(Object offset, Text wholeSubset,Context context) throws IOException, InterruptedException {
	        String sparam = wholeSubset.toString();	        
	        param.set(sparam);
	        int taskId = context.getTaskAttemptID().getTaskID().getId();
	        partitionIndex.set((int) Math.floor(taskId));
	        context.write(partitionIndex, param);
	      }
	 }
	 
	 public static Vector<String>  ReadTrainDS(FileSystem fs,Path pt) throws IOException {
		    Vector<String> svRecords = new Vector<String>();
	      
		    if(fs != null)
	          {	
	  	        BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(pt)));
	  	        
	  	        try {
	  	            String line;
	  	            //line=br.readLine();
	  	            while ((line =br.readLine())!=null&&line.length()>1){
	  	                 //System.out.println(line);
	  	                 svRecords.addElement(line);
	  	              }
	  	            } finally {
	  	                // you should close out the BufferedReader
	  	                br.close();
	  	            } 
	          }
	         
	         return svRecords;
		 
	 }
	 
	 public static class ModelSelectReducer  extends TableReducer<IntWritable, Text, ImmutableBytesWritable> {
		
	    public void reduce(IntWritable msId, Iterable<Text> params ,Context context) throws IOException, InterruptedException {
	          String sparam = ""; 
	    	  String[] vparam = new String[2];
	    	  
	    	   for (Text param : params) {
	    		   sparam = param.toString();
	    		   vparam = sparam.toString().split(",");
	    	  }   	  
	    	   
	    	   Vector<String> svRecords = new Vector<String>();
		       //获取训练文件
		       URI[] localFiles = context.getCacheFiles();
		       Path pt= new Path(localFiles[0].getPath());
		       FileSystem fs = null;
		        
		        try {
		      	    fs = FileSystem.get(new URI("hdfs://datanode1:9000"),context.getConfiguration());
		      	  
			      	if(fs != null)
			        {	
			      		 svRecords = ReadTrainDS(fs,pt);
			        }
		      	  
		      	 } catch (URISyntaxException e) {
		      	    // TODO Auto-generated catch block
		      	    e.printStackTrace();
		      	 }  
		         
		     
	    	    String[] ssvRecords = svRecords.toArray(new String[svRecords.size()]);   
		        double c = Double.valueOf(vparam[0]);
		        double g  = Double.valueOf(vparam[1]);
		        int nrFold =  context.getConfiguration().getInt("nrFlod",4);
		        MSSvmTrainer svmTrainer = new MSSvmTrainer(ssvRecords,c,g,nrFold);
		        long start=System.currentTimeMillis(); 
		        String acc = svmTrainer.do_cross_validation();
		       //去掉"Time Cost:"
		    	String costTime = ""+(System.currentTimeMillis()-start) ; 		
		    	Put put = new Put((""+System.currentTimeMillis()).getBytes());		    	
				put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("C"),
						Bytes.toBytes(vparam[0]));
				put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("G"),
						Bytes.toBytes(vparam[1]));
				put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("CostTime"),
						Bytes.toBytes(costTime));
				put.addColumn(Bytes.toBytes("BaseInfo"),
						Bytes.toBytes("CVAccuracy"), Bytes.toBytes(acc));				
				put.addColumn(Bytes.toBytes("Parameters"), Bytes.toBytes("ExperimentNum"), Bytes.toBytes(context.getConfiguration().get("ExperimentNum")));
				context.write(new ImmutableBytesWritable((""+System.currentTimeMillis()).getBytes()), put);	                
	    	}	 
	 }
}
