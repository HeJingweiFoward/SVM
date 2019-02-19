package modelSelect;

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
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

import parallelSVM.MSSvmTrainer;
import parallelSVM.io.TrainingSubsetInputFormat;



public class IterModelSelTest extends Configured implements Tool {
 public Job ijob;
	 
	 public int run(String[] args) throws Exception {

		 
		  Configuration cnf = this.getConf();
		 //Configuration cnf=new Configuration();
		 //换为HBaseConfiguration
		 //Configuration cnf = HBaseConfiguration.create();	 
		 cnf.set("hbase.zookeeper.quorum", "192.168.2.151:2181,192.168.2.152:2181,192.168.2.153:2181,192.168.2.154:2181");// zookeeper地址
		 cnf.set("hbase.zookeeper.property.clientPort", "2181");// zookeeper端口
		  //集群交叉提交  为了解决跨平台的问题
		  cnf.set("mapreduce.app-submission.cross-platform", "true");    
		  //导出的jar包位置
          cnf.set("mapreduce.job.jar", "F:\\研二\\科研\\SVMCas.jar");
          
          //Reduce的数量
          cnf.set("ReduceNum",args[5]);
		  cnf.set("Fold", args[4]);
          
		  ijob = Job.getInstance(cnf, "ModelSelectTest2");
          /*添加缓存路径  将需要缓存的文件分发到各个执行任务的子节点的机器中，
		  各个节点可以自行读取本地文件系统上的数据进行处理。*/
		  //缓存普通文件到task运行节点的工作目录
	      ijob.addCacheFile(new URI("hdfs://datanode1:9000/SVM/DataSet/a8a"));
	      //缓存普通文件到task运行节点的classpath中
	      ijob.addFileToClassPath( new Path("hdfs://datanode1:9000/SVM/libsvm.jar"));      
	      ijob.setJarByClass(this.getClass());
	      //设置Reduce的数量
	      int reduceNum = Integer.parseInt(args[2]);
	      ijob.setNumReduceTasks(reduceNum);	    
	      ijob.setMapperClass(ModelSelectMapper.class);
	      ijob.setReducerClass(ModelSelectReducer.class);
	     /* TableMapReduceUtil.initTableReducerJob("SVMResult", ModelSelectReducer.class, ijob);*/
	      ijob.setMapOutputKeyClass(IntWritable.class);
	      ijob.setMapOutputValueClass(Text.class);
	      ijob.setOutputKeyClass(NullWritable.class);
	      ijob.setOutputValueClass(Text.class);
	      //设置InputFormatClass
	      ijob.setInputFormatClass(TrainingSubsetInputFormat.class);
	      //该文件夹下所有的文件，不包括子文件夹   c,g; args[0]:input path
	      FileInputFormat.addInputPaths(ijob, args[0]);
	      //args[1]:output path   用HBase替代
	      FileOutputFormat.setOutputPath(ijob, new Path(args[1]));
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
	 
	 public static class ModelSelectReducer extends Reducer<IntWritable, Text, NullWritable, Text> 
	/* public static class ModelSelectReducer extends TableReducer<IntWritable, Text, ImmutableBytesWritable>*/
	 {
		
	    public void reduce(IntWritable msId, Iterable<Text> params ,Context context) throws IOException, InterruptedException {
	          String sparam = ""; 
	          //c,g
	    	  String[] vparam = new String[2];
	    	  //这里应该只循环一次
	    	   for (Text param : params) {
	    		   sparam = param.toString();
	    		   vparam = sparam.toString().split(",");
	    	  }   	  
	    	   
	    	   Vector<String> svRecords = new Vector<String>();
		       //获取训练文件,获取缓存的训练文件 ,训练文件为同一份，每次用不同的c,g做十折交叉验证
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
		        test.set(sparam + "\n" + acc  + "\n" +  costTime);
		        //记录交叉验证的准确率及时间
		        context.write(NullWritable.get(),test);
		        //改用HBase
				/*Configuration configuration=context.getConfiguration();
				
				Put put = new Put((configuration.get("ReduceNum")+";"+sparam).getBytes());// put实例化，每一个词存一行
				// 列族为content,列修饰符为count，列值为数目
				put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("C"),
						Bytes.toBytes(vparam[0]));
				put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("G"),
						Bytes.toBytes(vparam[1]));
				put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("CostTime"),
						Bytes.toBytes(costTime));
				put.addColumn(Bytes.toBytes("BaseInfo"),
						Bytes.toBytes("CVAccuracy"), Bytes.toBytes(acc));
	
				put.addColumn(Bytes.toBytes("Parameters"), Bytes.toBytes("ReduceNum"), Bytes.toBytes(configuration.get("ReduceNum")));
				put.addColumn(Bytes.toBytes("Parameters"), Bytes.toBytes("Fold"), Bytes.toBytes(configuration.get("Fold")));
				context.write(new ImmutableBytesWritable(sparam.getBytes()), put);// 输出求和后的<key,value>
*/				
    
	    	}	 
	 }
}
