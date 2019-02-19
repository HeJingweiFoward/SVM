package modelSelect;

import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

public class MSDriverTest {
	private static final Logger LOG = Logger.getLogger(MSDriver.class);
	//reduce数量
	public static int Count=16;
	
	public static void main(String[] args) throws Exception {
		
		System.setProperty("HADOOP_USER_NAME", "hadoop");
		
		args=new String[5];
		//参数
		args[0]="hdfs://192.168.2.151:9000/test/hjw/SvmIn";
		args[1]="hdfs://192.168.2.151:9000/test/hjw/SvmOut";//输出到HBase
		args[2]="4";//c
		args[3]="8";//g
		//交叉验证折数
		args[4]="4";


		
	   Configuration firstConf = new Configuration();
	   String[] otherArgs = new GenericOptionsParser(firstConf, args).getRemainingArgs();
	   
	   // otherArgs[0]: input path otherArgs[1]: output path otherArgs[2]: num of subset power of 2
	   if (otherArgs.length < 4) {
	       System.err.println("Usage: please give the number of c and g");// how to call
	       System.exit(2);
	    }
	   
	   FileSystem fs = FileSystem.get(new URI("hdfs://192.168.2.151:9000"),new Configuration());          
	    Path path1 = new Path(otherArgs[1]);  
	      
	    //用HBase替代，不用删除
	    if(fs.exists(path1)){  
	        fs.delete(path1,true);
	    }  
	    
	    int cnum = Integer.valueOf(otherArgs[2]);
	    int gnum = Integer.valueOf(otherArgs[3]);
	    int count = 0;

	    
	   for(int i =0;i<cnum;i++)
	   {
		   for(int j = gnum;j > 0 ;j--)
		   {
			   //System.out.println( String.valueOf(Math.pow(2,i)) + "----" +  String.valueOf(Math.pow(10,-j)));
			   String sparam = String.valueOf(Math.pow(2,i)) + "," +  String.valueOf(Math.pow(10,-j));			   
			   count++;
			   
			   Path path = new Path(otherArgs[0] +"/"  + String.valueOf(count));
			   FSDataOutputStream out = fs.create(path,true);				
			   out.writeBytes(sparam);
			   out.flush();
			   out.close();			
		   }
	    }
	   
	
		   //run的参数
		   String[] iargs=new String[6];
		   //输入，输出路径
		   for(int m=0;m<2;m++){
		      iargs[m]=otherArgs[m];
		   }
		   //实际训练节点个数  ， 实际应该为count，这里为了HBase统计数据方便，采用Count；这里count和Count的效果一样。
			//实际每次分配的Reduce个数，考虑到64无法除尽的情况，即最后一次分配的Reduce数量
		  iargs[2] = String.valueOf(Count);
		  //输出目录控制
		  iargs[3] = "";
		  //折数
		  iargs[4]=otherArgs[4];
		  //设置的一次最多的Reduce数目   对应后面的     cnf.set("ReduceNum",args[5]);
		  iargs[5]=String.valueOf(Count);
		  
		  Configuration IJobConf = new Configuration();	      

		  
		  IterModelSelTest ig = new IterModelSelTest();           
		  ToolRunner.run(IJobConf,ig, iargs);  		
		  //删除参数文件
		  for(int k = 1;k<=count;k++)
			  fs.delete(new Path(otherArgs[0] +"/"  + String.valueOf(k)),true);
		  
 
	   
	}
}
