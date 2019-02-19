package parallelSVM.Iterative_svm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Vector;

import libsvm.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import parallelSVM.io.*;
import parallelSVM.SvmTrainer;

class Itergsv extends Configured implements  Tool {
    public Job ijob;
    public int run(String[] args) throws Exception {
      ijob = Job.getInstance(this.getConf(), "Iterative SVM: training");
      ijob.addCacheFile(new URI("hdfs://datanode1:9000/SVM/gsvs/global_sv.csv"));
      ijob.addFileToClassPath( new Path("hdfs://datanode1:9000/SVM/libsvm.jar"));
      ijob.addFileToClassPath( new Path("hdfs://datanode1:9000/SVM/javaml-0.1.7.jar"));
      
      ijob.setJarByClass(this.getClass());
      ijob.setNumReduceTasks(Integer.parseInt(args[2]));
      ijob.setReducerClass(GSVReducer.class);
      ijob.setMapperClass(GSVOutputMapper.class);
      ijob.setMapOutputKeyClass(IntWritable.class);
      ijob.setMapOutputValueClass(Text.class);
      ijob.setOutputKeyClass(NullWritable.class);
      ijob.setOutputValueClass(Text.class);
      ijob.setInputFormatClass(TrainingSubsetInputFormat.class);
      FileInputFormat.addInputPaths(ijob, args[1] + "/subsets");
      FileOutputFormat.setOutputPath(ijob, new Path(args[1] + "/gsv/gsv-"+args[3]));
      return ijob.waitForCompletion(true) ? 0 : 1;
    }

    public static void moveFiles(Path from, Path to, Configuration conf) throws IOException {
      FileSystem fs = from.getFileSystem(conf); // get file system
      for (FileStatus status : fs.listStatus(from)) { // list all files in 'from' folder
        Path file = status.getPath(); // get path to file in 'from' folder
        Path dst = new Path(to, file.getName()); // create new file name
        fs.rename(file, dst); // move file from 'from' folder to 'to' folder
      }
    }


    public static class GSVOutputMapper extends Mapper<Object, Text, IntWritable, Text> {
      private Text mergedData = new Text();
      private IntWritable partitionIndex = new IntWritable();
      public void map(Object offset, Text wholeSubset,Context context) throws IOException, InterruptedException {
        String[] subsetRecords = wholeSubset.toString().split("\n");
        for (int i = 0; i < subsetRecords.length; i++) {
          mergedData.set(subsetRecords[i]);
          int taskId = context.getTaskAttemptID().getTaskID().getId();
          partitionIndex.set((int) Math.floor(taskId));
          context.write(partitionIndex, mergedData);
        }
      }
    }
    
    public static class GSVReducer extends Reducer<IntWritable, Text, NullWritable, Text> {
      private Text gSvs = new Text();
      //private Path getPath=null;
      public void setup(Context context) throws IOException {
//        Configuration conf = context.getConfiguration();
//        FileSystem fs = FileSystem.get(conf);
        //URI[] cacheFiles = context.getCacheFiles();       
        //getPath = new Path(cacheFiles[0].getPath());
      }
      
       public void reduce(IntWritable subsetId, Iterable<Text> v_subsetTrainingDataset,Context context) throws IOException, InterruptedException {
    	Vector<String> svRecordsSub = new Vector<String>();
    	Vector<String> svRecordsGvs = new Vector<String>();
    	    	
    	//获取subset中的训练向量
    	for (Text trainingData : v_subsetTrainingDataset) {
         	svRecordsSub.addElement(trainingData.toString());
        }
    	  
    	//获取全局支持向量
        URI[] localFiles = context.getCacheFiles();
        Path pt=null;
        
        for(URI temp: localFiles){
            if(temp.getPath().contains("global_sv.csv")){
               	pt=new Path(temp.getPath());
            }
        }
        
        FileSystem fs = null;
         try {
      	  fs = FileSystem.get(new URI("hdfs://datanode1:9000"),context.getConfiguration());
      	  
      	  if(fs != null)
          {	
  	        BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(pt)));
  	        try {
  	            String line;
  	            //line=br.readLine();
  	            while ((line =br.readLine())!=null&&line.length()>1){
  	                 System.out.println(line);
  	                 svRecordsGvs.addElement(line);
  	              }
  	            } finally {
  	                // you should close out the BufferedReader
  	                br.close();
  	            }
          }
      	  
      	 } catch (URISyntaxException e) {
      	    // TODO Auto-generated catch block
      	    e.printStackTrace();
      	 }  
        
        //训练子集中的向量加上全局的支持向量，去掉重复值
         for(String gv :svRecordsGvs)
         {
        	 if(!svRecordsSub.contains(gv))
        	 {
        		 svRecordsSub.add(gv);   
        	 }        		 
         }
         
        
        String[] svRecords = svRecordsSub.toArray(new String[svRecordsSub.size()]);
        SvmTrainer svmTrainer = new SvmTrainer(svRecords);
        svm_model model = svmTrainer.train();           
        int[] svIndices = model.sv_indices;
        FSDataOutputStream fos =fs.append(pt);
         
          for (int i = 0; i < svIndices.length; i++) {
             gSvs.set(svRecordsSub.get(svIndices[i] - 1).toString());
             if (!svRecordsGvs.contains(svRecordsSub.get(svIndices[i] - 1))){
                 context.write(NullWritable.get(), gSvs);
                 fos.writeBytes(svRecordsSub.get(svIndices[i] - 1).toString() + "\n");
                 context.getCounter("Itergsv", "gsvNumAdd").increment(1);
              }
           }
            
            fos.close();
          }
        }
    }
