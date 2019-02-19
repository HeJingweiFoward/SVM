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
	//reduce����
	public static int Count=16;
	
	public static void main(String[] args) throws Exception {
		
		System.setProperty("HADOOP_USER_NAME", "hadoop");
		
		args=new String[5];
		//����
		args[0]="hdfs://192.168.2.151:9000/test/hjw/SvmIn";
		args[1]="hdfs://192.168.2.151:9000/test/hjw/SvmOut";//�����HBase
		args[2]="4";//c
		args[3]="8";//g
		//������֤����
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
	      
	    //��HBase���������ɾ��
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
	   
	
		   //run�Ĳ���
		   String[] iargs=new String[6];
		   //���룬���·��
		   for(int m=0;m<2;m++){
		      iargs[m]=otherArgs[m];
		   }
		   //ʵ��ѵ���ڵ����  �� ʵ��Ӧ��Ϊcount������Ϊ��HBaseͳ�����ݷ��㣬����Count������count��Count��Ч��һ����
			//ʵ��ÿ�η����Reduce���������ǵ�64�޷�����������������һ�η����Reduce����
		  iargs[2] = String.valueOf(Count);
		  //���Ŀ¼����
		  iargs[3] = "";
		  //����
		  iargs[4]=otherArgs[4];
		  //���õ�һ������Reduce��Ŀ   ��Ӧ�����     cnf.set("ReduceNum",args[5]);
		  iargs[5]=String.valueOf(Count);
		  
		  Configuration IJobConf = new Configuration();	      

		  
		  IterModelSelTest ig = new IterModelSelTest();           
		  ToolRunner.run(IJobConf,ig, iargs);  		
		  //ɾ�������ļ�
		  for(int k = 1;k<=count;k++)
			  fs.delete(new Path(otherArgs[0] +"/"  + String.valueOf(k)),true);
		  
 
	   
	}
}
