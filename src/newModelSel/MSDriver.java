package newModelSel;

import java.net.URI;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;



public class MSDriver {
	private static final Logger LOG = Logger.getLogger(MSDriver.class);
	
	public static void main(String[] args) throws Exception {
		System.setProperty("HADOOP_USER_NAME", "hadoop");
	   Configuration firstConf = new Configuration();
	   String[] otherArgs = new GenericOptionsParser(firstConf, args).getRemainingArgs();
	   
       if (otherArgs.length < 6) {
	       System.err.println("Usage: please give the number of c and g");// how to call
	       System.exit(2);
	    }
	   
	           
        int cnum = Integer.valueOf(otherArgs[1]); //c参数个数
	    int gnum = Integer.valueOf(otherArgs[2]); //g参数个数
	    int reduceNums = Integer.valueOf(otherArgs[3]);//并发执行的Reduce的个数
	    int reduceNumsAllowed = Integer.valueOf(otherArgs[4]);//允许执行的Reduce个数
	    int nrFold = Integer.valueOf(otherArgs[5]);//交叉验证的重数。默认为4	
	    String  experiemntNum = otherArgs[6]; //实验次数
	    int count = 0;
	    
	    FileSystem fs = FileSystem.get(new URI("hdfs://datanode1:9000"),new Configuration());  
	    System.out.println("........SVM最优模型选择交叉验证运行开始..........");
		long start=System.currentTimeMillis(); 		
	    
	    for(int i=0;i<cnum;i++)
		   {
			   for(int j = 0;j < gnum ;j++)
			   {
			      String sparam = String.valueOf(0.5 + i*0.25) + "," +  String.valueOf(0.05 + j*0.0125);
			      count++;
			      //生成参数文件
			      Path path = new Path(otherArgs[0] +"/"  + String.valueOf(count));
			      FSDataOutputStream out = fs.create(path,true);				
			      out.writeBytes(sparam);
			      out.flush();
			      out.close();			
			   
			   if(count == reduceNums)
			   {
				  String[] iargs=new String[5];
				  iargs[0] = otherArgs[0];//输入文件目录			
				  iargs[1] = String.valueOf(reduceNums);//并发执行的Reduce的个数 				  
				  iargs[2] = String.valueOf(reduceNumsAllowed);//允许执行的Reduce个数
				  iargs[3] = String.valueOf(nrFold);//交叉验证重数
		    	  iargs[4] = experiemntNum;//实验次数
				  Configuration IJobConf = new Configuration();	      
				  IterModelSel ig = new IterModelSel();           
				  ToolRunner.run(IJobConf,ig, iargs);  		
				  //删除参数文件
				  for(int k = 1;k<=count;k++)
					  fs.delete(new Path(otherArgs[0] +"/"  + String.valueOf(k)),true);				  
				  count = 0; 	   				  		 
			   }
		   }
	    }
	   
	   if(count != 0)
	   {
		  String[] iargs=new String[5];
		  iargs[0] = otherArgs[0];//输入文件目录			
		  iargs[1] = String.valueOf(reduceNums);//并发执行的Reduce的个数 				  
		  iargs[2] = String.valueOf(reduceNumsAllowed);//允许执行的Reduce个数
		  iargs[3] = String.valueOf(nrFold);//交叉验证重数
	      iargs[4] = experiemntNum;//实验次数
		  Configuration IJobConf = new Configuration();	      
		  IterModelSel ig = new IterModelSel();           
		  ToolRunner.run(IJobConf,ig, iargs);  	
	   }
	   
	   System.out.println("用时:"+(System.currentTimeMillis()-start)); 
	
	}
}
	 
