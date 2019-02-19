/**
 * Created by pateu14(Patel Udita) on 4/17/2016.
 */
package parallelSVM.Iterative_svm;

import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import parallelSVM.SvmTrainer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;
import parallelSVM.SvmPredict;

public class Driver  {
    private static final Logger LOG = Logger.getLogger(Driver.class);
    static FileSystem fs;
    
    static final String svm_type_table[] = {
       "c_svc","nu_svc","one_class","epsilon_svr","nu_svr",
     };

     static final String kernel_type_table[]= {
        "linear","polynomial","rbf","sigmoid","precomputed"
     };
    
    public static void saveModelToHdfs(svm_model model, String pathStr) throws URISyntaxException{
        try {
          Path file = new Path(pathStr);
		  FSDataOutputStream fos = fs.create(file,true);
          svm_parameter param = model.param;
          fos.writeBytes("svm_type "+svm_type_table[param.svm_type]+"\n");
          fos.writeBytes("kernel_type "+kernel_type_table[param.kernel_type]+"\n");
          if(param.kernel_type == svm_parameter.POLY)
            fos.writeBytes("degree "+param.degree+"\n");
          if(param.kernel_type == svm_parameter.POLY ||
                  param.kernel_type == svm_parameter.RBF ||
                  param.kernel_type == svm_parameter.SIGMOID)
            fos.writeBytes("gamma "+param.gamma+"\n");
          if(param.kernel_type == svm_parameter.POLY ||
                  param.kernel_type == svm_parameter.SIGMOID)
            fos.writeBytes("coef0 "+param.coef0+"\n");
          int nr_class = model.nr_class;
          int l = model.l;
          fos.writeBytes("nr_class "+nr_class+"\n");
          fos.writeBytes("total_sv "+l+"\n");
          fos.writeBytes("rho");
          for(int i=0;i<nr_class*(nr_class-1)/2;i++)
              fos.writeBytes(" "+model.rho[i]);
          fos.writeBytes("\n");
          if(model.label != null) {
            fos.writeBytes("label");
            for(int i=0;i<nr_class;i++)
                fos.writeBytes(" "+model.label[i]);
            fos.writeBytes("\n");
          }
          if(model.probA != null) { // regression has probA only
            fos.writeBytes("probA");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
              fos.writeBytes(" "+model.probA[i]);
            fos.writeBytes("\n");
          }
          if(model.probB != null) {
            fos.writeBytes("probB");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
              fos.writeBytes(" "+model.probB[i]);
            fos.writeBytes("\n");
          }
          if(model.nSV != null) {
            fos.writeBytes("nr_sv");
            for(int i=0;i<nr_class;i++)
              fos.writeBytes(" "+model.nSV[i]);
            fos.writeBytes("\n");
          }

          fos.writeBytes("SV\n");
          double[][] sv_coef = model.sv_coef;
          svm_node[][] SV = model.SV;

          for(int i=0;i<l;i++) {
            for(int j=0;j<nr_class-1;j++)
              fos.writeBytes(sv_coef[j][i]+" ");
              svm_node[] p = SV[i];
              if(param.kernel_type == svm_parameter.PRECOMPUTED)
                fos.writeBytes("0:"+(int)(p[0].value));
              else
                for(int j=0;j<p.length;j++)
                  fos.writeBytes(p[j].index+":"+p[j].value+" ");
                fos.writeBytes("\n");
          }

          fos.close();

        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
     

    
    public static void main(String[] args) throws Exception {
      Configuration firstConf = new Configuration();
      String[] otherArgs = new GenericOptionsParser(firstConf, args).getRemainingArgs();
      // otherArgs[0]: input path otherArgs[1]: output path otherArgs[2]: num of subset power of 2
      if (otherArgs.length < 2) {
          System.err.println("Usage: iterative-svm <in> <out> <subsets>");// how to call
          System.exit(2);
      }
      
      final double subsets = Double.valueOf(otherArgs[2]);
      firstConf.setInt("SUBSET_COUNT",(int)subsets);	// set a Int global value
      // pre-partition job count is two for two map-reds that partitions the data
      final int prepartitionJobCount = 2;
       Configuration[] prepartitionConfs = new Configuration[prepartitionJobCount];
      prepartitionConfs[0] = firstConf;
      prepartitionConfs[1] = new Configuration();
      
    //删除在hdfs上已存在的目录   
      FileSystem fileSystem = FileSystem.get(new URI("hdfs://datanode1:9000"),new Configuration());          
      Path path1 = new Path(args[1] + "/tmp");  
      Path path2 =  new Path(args[1] + "/subsets");
      Path path3 =  new Path(args[1] + "/gsv");
      
      if(fileSystem.exists(path1)){  
          fileSystem.delete(path1,true);  
      }  
      
      if(fileSystem.exists(path2)){  
          fileSystem.delete(path2,true);  
      }          
     
      if(fileSystem.exists(path3)){  
          fileSystem.delete(path3,true);  
      } 
      
      
      Preiterative1 pre1=new Preiterative1();
      int res1 = ToolRunner.run(prepartitionConfs[0],pre1, otherArgs); //## change the class.
      System.out.println("The mapper exited with : "+res1);
      
      prepartitionConfs[1].setInt("SUBSET_COUNT",(int)subsets);
	  prepartitionConfs[1].setInt("TOTAL_RECORD_COUNT",
				(int)pre1.getJob().getCounters().findCounter("trainingDataStats","TOTAL_RECORD_COUNT").getValue());
	  prepartitionConfs[1].setInt("CLASS_1_COUNT",
				(int)pre1.getJob().getCounters().findCounter("trainingDataStats","CLASS_1_COUNT").getValue());
	  prepartitionConfs[1].setInt("CLASS_2_COUNT",
				(int)pre1.getJob().getCounters().findCounter("trainingDataStats","CLASS_2_COUNT").getValue());	
      Preiterative2 pre2=new Preiterative2();
      int res2= ToolRunner.run(prepartitionConfs[1],pre2,otherArgs);
      System.out.println("The mapper exited with : "+res2);
      
      /*** Iterative job starts ***/
      System.out.println("===== Beginning job for iter number :=====");
    
      long gsvNumsAdd= 0 ;
     
      int i=1;
      String[] iargs=new String[4];
      for(int j=0;j<otherArgs.length;j++){
          iargs[j]=otherArgs[j];
      }
      do {
        iargs[3]=String.valueOf(i);
        Configuration IJobConf = new Configuration();
        IJobConf.set("USER_OUTPUT_PATH", iargs[1]);
        Itergsv ig = new Itergsv();           
        ToolRunner.run(IJobConf,ig, iargs);        
        gsvNumsAdd=ig.ijob.getCounters().findCounter("Itergsv", "gsvNumAdd").getValue();
        System.out.println("gsvNumsAdd is : "+ gsvNumsAdd );       
        i++;
      }while(gsvNumsAdd > 0);//支持向量不再增加时，退出
      
      //得到最终的全局支持向量
      Configuration conf = new Configuration();
	  conf.set("fs.default.name", "hdfs://datanode1:9000");
	  Vector<String> svRecordsGvs = new Vector<String>();
	  fs =  FileSystem.get(conf);
	  BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(new Path("/SVM/gsvs/global_sv.csv"))));
      
	  try{
		  String line;
      
		  while ((line =br.readLine())!=null&&line.length()>1){
			  System.out.println(line);
			  svRecordsGvs.addElement(line);
         }
	  	} finally {
	  		br.close();
	  	}   
	  
	  String[] svRecords = svRecordsGvs.toArray(new String[svRecordsGvs.size()]);
      SvmTrainer svmTrainer = new SvmTrainer(svRecords);
      svm_model model = svmTrainer.train();  
      //存储模型到hdfs
      saveModelToHdfs(model,"/SVM/IterModel");
      //预测
      SvmPredict.main(new String[] {"/SVM/breastCancerTest","/SVM/IterModel","/SVM/IterPredict"});      
    }
    
  }
