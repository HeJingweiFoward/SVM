package modelSelect;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

import libsvm.svm_model;
import parallelSVM.MSSvmTrainer;
import parallelSVM.SvmTrainer;
import parallelSVM.io.TrainingSubsetInputFormat;

public class IterModelSel  extends Configured implements  Tool{
	 public Job ijob;
	 
	 public int run(String[] args) throws Exception {

		 Configuration cnf = HBaseConfiguration.create();	 
		 cnf.set("hbase.zookeeper.quorum", "192.168.2.151:2181,192.168.2.152:2181,192.168.2.153:2181,192.168.2.154:2181");// zookeeper地址
		 cnf.set("hbase.zookeeper.property.clientPort", "2181");

		  cnf.set("mapreduce.app-submission.cross-platform", "true");    

          cnf.set("mapreduce.job.jar", "F:\\研二\\科研\\SVMCas.jar");
   	   //一次允许运行的Reduce最大数量
          cnf.set("mapreduce.job.running.reduce.limit", args[2]);
          //第几次实验
          cnf.set("ExperimentNum", args[3]);         
		  ijob = Job.getInstance(cnf, "ModelSelectTest");
	      ijob.addCacheFile(new URI("hdfs://datanode1:9000/SVM/DataSet/a8a"));
	      ijob.addFileToClassPath( new Path("hdfs://datanode1:9000/SVM/libsvm.jar"));      
	      ijob.setJarByClass(this.getClass());
	      int reduceNum = Integer.parseInt(args[4]);
	      ijob.setNumReduceTasks(reduceNum);	    
	      
	      ijob.setMapperClass(ModelSelectMapper.class);
	      TableMapReduceUtil.initTableReducerJob("SVMResultT1", ModelSelectReducer.class, ijob);
	      ijob.setMapOutputKeyClass(IntWritable.class);
	      ijob.setMapOutputValueClass(Text.class);
	      ijob.setOutputKeyClass(NullWritable.class);
	      ijob.setOutputValueClass(Text.class);
	      ijob.setInputFormatClass(TrainingSubsetInputFormat.class);
	      FileInputFormat.addInputPaths(ijob, args[0]);

	      return ijob.waitForCompletion(true) ? 0 : 1;
	 }
	 //map操作的是FileInputFormat.addInputPaths(ijob, args[0])；c，g
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
	  	                 System.out.println(line);
	  	                 svRecords.addElement(line);
	  	              }
	  	            } finally {
	  	                // you should close out the BufferedReader
	  	                br.close();
	  	            } 
	          }
	         
	         return svRecords;
		 
	 }
	 

	 public static class ModelSelectReducer extends TableReducer<IntWritable, Text, ImmutableBytesWritable>
	 {
		
	    public void reduce(IntWritable msId, Iterable<Text> params ,Context context) throws IOException, InterruptedException {
	          String sparam = ""; 
	          //c,g
	    	  String[] vparam = new String[2];
	   
	    	   for (Text param : params) {
	    		   sparam = param.toString();
	    		   vparam = sparam.toString().split(",");
	    	  }   	  
	    	   
	    	   Vector<String> svRecords = new Vector<String>();
		   
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
		        MSSvmTrainer svmTrainer = new MSSvmTrainer(ssvRecords,c,g);
		        long start=System.currentTimeMillis(); 
		        //做交叉验证  去掉"Cross Validation Accuracy = "
		        String acc = svmTrainer.do_cross_validation();
		        Text test = new Text();		 
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
