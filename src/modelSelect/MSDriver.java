package modelSelect;

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
	//reduce����
	public static int Count=64;
	
	public static void main(String[] args) throws Exception {
		
		System.setProperty("HADOOP_USER_NAME", "hadoop");
		
		args=new String[6];
		//����
		args[0]="hdfs://192.168.2.151:9000/test/hjw/SvmIn";
		args[1]="";//�����HBase
		args[2]="8";//c
		args[3]="8";//g
		//һ���������е�Reduce�������
		args[4]="16";
		//�ڼ���ʵ��
		args[5]="4";

	   Configuration firstConf = new Configuration();
	   String[] otherArgs = new GenericOptionsParser(firstConf, args).getRemainingArgs();
	   
	   // otherArgs[0]: input path otherArgs[1]: output path otherArgs[2]: num of subset power of 2
	   if (otherArgs.length < 4) {
	       System.err.println("Usage: please give the number of c and g");// how to call
	       System.exit(2);
	    }
	   
	   FileSystem fs = FileSystem.get(new URI("hdfs://192.168.2.151:9000"),new Configuration());          

	    
	    int cnum = Integer.valueOf(otherArgs[2]);
	    int gnum = Integer.valueOf(otherArgs[3]);
	    int count = 0;

	    
	   for(int i =0;i<cnum;i++)
	   {
		   for(int j = gnum;j > 0 ;j--)
		   {

			   //String sparam = String.valueOf(Math.pow(2,i)) + "," +  String.valueOf(Math.pow(10,-j));	
			 //String sparam = String.valueOf(i) + "," +  String.valueOf(Math.pow(10,-j));			 //ExperimentNum:2  
			   String sparam = String.valueOf(1 + (i-1)*0.125) + "," +  String.valueOf(0.05 + j*0.0125); //ExperimentNum:3,4

			   count++;
			   
			   Path path = new Path(otherArgs[0] +"/"  + String.valueOf(count));
			   FSDataOutputStream out = fs.create(path,true);				
			   out.writeBytes(sparam);
			   out.flush();
			   out.close();			
			   
		   }
	    }
	   

		   //run�Ĳ���
		   String[] iargs=new String[5];
		   //���룬���·��
		   for(int m=0;m<2;m++){
		      iargs[m]=otherArgs[m];
		   }
		   //һ���������е�Reduce�������
		   iargs[2]=otherArgs[4];
		   //�ڼ���ʵ��
		  iargs[3] = otherArgs[5];
		  //����Reduce����
		  iargs[4]=String.valueOf(Count) ;
			  
		  Configuration IJobConf = new Configuration();	      

		  
		  IterModelSel ig = new IterModelSel();           
		  ToolRunner.run(IJobConf,ig, iargs);  		
		  //ɾ�������ļ�
		  for(int k = 1;k<=count;k++)
			  fs.delete(new Path(otherArgs[0] +"/"  + String.valueOf(k)),true);
		  
	
	}
}
	 
