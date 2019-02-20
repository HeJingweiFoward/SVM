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
	   
	           
        int cnum = Integer.valueOf(otherArgs[1]); //c��������
	    int gnum = Integer.valueOf(otherArgs[2]); //g��������
	    int reduceNums = Integer.valueOf(otherArgs[3]);//����ִ�е�Reduce�ĸ���
	    int reduceNumsAllowed = Integer.valueOf(otherArgs[4]);//����ִ�е�Reduce����
	    int nrFold = Integer.valueOf(otherArgs[5]);//������֤��������Ĭ��Ϊ4	
	    String  experiemntNum = otherArgs[6]; //ʵ�����
	    int count = 0;
	    
	    FileSystem fs = FileSystem.get(new URI("hdfs://datanode1:9000"),new Configuration());  
	    System.out.println("........SVM����ģ��ѡ�񽻲���֤���п�ʼ..........");
		long start=System.currentTimeMillis(); 		
	    
	    for(int i=0;i<cnum;i++)
		   {
			   for(int j = 0;j < gnum ;j++)
			   {
			      String sparam = String.valueOf(0.5 + i*0.25) + "," +  String.valueOf(0.05 + j*0.0125);
			      count++;
			      //���ɲ����ļ�
			      Path path = new Path(otherArgs[0] +"/"  + String.valueOf(count));
			      FSDataOutputStream out = fs.create(path,true);				
			      out.writeBytes(sparam);
			      out.flush();
			      out.close();			
			   
			   if(count == reduceNums)
			   {
				  String[] iargs=new String[5];
				  iargs[0] = otherArgs[0];//�����ļ�Ŀ¼			
				  iargs[1] = String.valueOf(reduceNums);//����ִ�е�Reduce�ĸ��� 				  
				  iargs[2] = String.valueOf(reduceNumsAllowed);//����ִ�е�Reduce����
				  iargs[3] = String.valueOf(nrFold);//������֤����
		    	  iargs[4] = experiemntNum;//ʵ�����
				  Configuration IJobConf = new Configuration();	      
				  IterModelSel ig = new IterModelSel();           
				  ToolRunner.run(IJobConf,ig, iargs);  		
				  //ɾ�������ļ�
				  for(int k = 1;k<=count;k++)
					  fs.delete(new Path(otherArgs[0] +"/"  + String.valueOf(k)),true);				  
				  count = 0; 	   				  		 
			   }
		   }
	    }
	   
	   if(count != 0)
	   {
		  String[] iargs=new String[5];
		  iargs[0] = otherArgs[0];//�����ļ�Ŀ¼			
		  iargs[1] = String.valueOf(reduceNums);//����ִ�е�Reduce�ĸ��� 				  
		  iargs[2] = String.valueOf(reduceNumsAllowed);//����ִ�е�Reduce����
		  iargs[3] = String.valueOf(nrFold);//������֤����
	      iargs[4] = experiemntNum;//ʵ�����
		  Configuration IJobConf = new Configuration();	      
		  IterModelSel ig = new IterModelSel();           
		  ToolRunner.run(IJobConf,ig, iargs);  	
	   }
	   
	   System.out.println("��ʱ:"+(System.currentTimeMillis()-start)); 
	
	}
}
	 
