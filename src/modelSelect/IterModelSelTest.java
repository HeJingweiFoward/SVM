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
		 //��ΪHBaseConfiguration
		 //Configuration cnf = HBaseConfiguration.create();	 
		 cnf.set("hbase.zookeeper.quorum", "192.168.2.151:2181,192.168.2.152:2181,192.168.2.153:2181,192.168.2.154:2181");// zookeeper��ַ
		 cnf.set("hbase.zookeeper.property.clientPort", "2181");// zookeeper�˿�
		  //��Ⱥ�����ύ  Ϊ�˽����ƽ̨������
		  cnf.set("mapreduce.app-submission.cross-platform", "true");    
		  //������jar��λ��
          cnf.set("mapreduce.job.jar", "F:\\�ж�\\����\\SVMCas.jar");
          
          //Reduce������
          cnf.set("ReduceNum",args[5]);
		  cnf.set("Fold", args[4]);
          
		  ijob = Job.getInstance(cnf, "ModelSelectTest2");
          /*��ӻ���·��  ����Ҫ������ļ��ַ�������ִ��������ӽڵ�Ļ����У�
		  �����ڵ�������ж�ȡ�����ļ�ϵͳ�ϵ����ݽ��д���*/
		  //������ͨ�ļ���task���нڵ�Ĺ���Ŀ¼
	      ijob.addCacheFile(new URI("hdfs://datanode1:9000/SVM/DataSet/a8a"));
	      //������ͨ�ļ���task���нڵ��classpath��
	      ijob.addFileToClassPath( new Path("hdfs://datanode1:9000/SVM/libsvm.jar"));      
	      ijob.setJarByClass(this.getClass());
	      //����Reduce������
	      int reduceNum = Integer.parseInt(args[2]);
	      ijob.setNumReduceTasks(reduceNum);	    
	      ijob.setMapperClass(ModelSelectMapper.class);
	      ijob.setReducerClass(ModelSelectReducer.class);
	     /* TableMapReduceUtil.initTableReducerJob("SVMResult", ModelSelectReducer.class, ijob);*/
	      ijob.setMapOutputKeyClass(IntWritable.class);
	      ijob.setMapOutputValueClass(Text.class);
	      ijob.setOutputKeyClass(NullWritable.class);
	      ijob.setOutputValueClass(Text.class);
	      //����InputFormatClass
	      ijob.setInputFormatClass(TrainingSubsetInputFormat.class);
	      //���ļ��������е��ļ������������ļ���   c,g; args[0]:input path
	      FileInputFormat.addInputPaths(ijob, args[0]);
	      //args[1]:output path   ��HBase���
	      FileOutputFormat.setOutputPath(ijob, new Path(args[1]));
	      return ijob.waitForCompletion(true) ? 0 : 1;
	 }
	 //map��������FileInputFormat.addInputPaths(ijob, args[0])��c��g
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
	    	  //����Ӧ��ֻѭ��һ��
	    	   for (Text param : params) {
	    		   sparam = param.toString();
	    		   vparam = sparam.toString().split(",");
	    	  }   	  
	    	   
	    	   Vector<String> svRecords = new Vector<String>();
		       //��ȡѵ���ļ�,��ȡ�����ѵ���ļ� ,ѵ���ļ�Ϊͬһ�ݣ�ÿ���ò�ͬ��c,g��ʮ�۽�����֤
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
		        //��������֤  ȥ��"Cross Validation Accuracy = "
		        String acc = svmTrainer.do_cross_validation();
		        Text test = new Text();		 
		        //ȥ��"Time Cost:"
		    	String costTime = ""+(System.currentTimeMillis()-start) ; 
		        test.set(sparam + "\n" + acc  + "\n" +  costTime);
		        //��¼������֤��׼ȷ�ʼ�ʱ��
		        context.write(NullWritable.get(),test);
		        //����HBase
				/*Configuration configuration=context.getConfiguration();
				
				Put put = new Put((configuration.get("ReduceNum")+";"+sparam).getBytes());// putʵ������ÿһ���ʴ�һ��
				// ����Ϊcontent,�����η�Ϊcount����ֵΪ��Ŀ
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
				context.write(new ImmutableBytesWritable(sparam.getBytes()), put);// �����ͺ��<key,value>
*/				
    
	    	}	 
	 }
}
