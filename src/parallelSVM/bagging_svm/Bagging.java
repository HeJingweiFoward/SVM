package parallelSVM.bagging_svm;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import parallelSVM.io.*;
import parallelSVM.SvmTrainer;
import libsvm.*;

class Bagging extends Configured implements  Tool{
    public int run(String[] args) throws Exception {
      Job baggingJob = Job.getInstance(this.getConf(),"Bagging SVM: Base models training");
      baggingJob.addFileToClassPath(new Path("hdfs://datanode1:9000/SVM/libsvm.jar"));
      baggingJob.setJarByClass(this.getClass());
      baggingJob.setNumReduceTasks(0);
      baggingJob.setMapperClass(BaseModelOutputMapper.class);
      baggingJob.setOutputKeyClass(NullWritable.class);
      baggingJob.setOutputValueClass(Text.class);
      baggingJob.setInputFormatClass(TrainingSubsetInputFormat.class);
      FileInputFormat.addInputPath(baggingJob, new Path(args[1]+"/base-model-subsets"));
      FileOutputFormat.setOutputPath(baggingJob, new Path(args[1]+"/base-model-SVs"));
      return baggingJob.waitForCompletion(true) ? 0 : 1;
    }
    
    public static class BaseModelOutputMapper extends Mapper<NullWritable, Text, NullWritable, Text>{
      private static final String svm_type_table[] = {
        "c_svc","nu_svc","one_class","epsilon_svr","nu_svr",
      };

      static final String kernel_type_table[]= {
        "linear","polynomial","rbf","sigmoid","precomputed"
      };

      // An identical implementation of svm.svm_save_model in LIBSVM,
      // different in that the file is saved to HDFS instead of a local path.
      private void saveModelToHdfs(svm_model model, String pathStr, int taskId, Context context) throws URISyntaxException{
        try {
          FileSystem fs = FileSystem.get(new URI("hdfs://datanode1:9000"),context.getConfiguration());
		  String pathStrModel  = "/SVM/BaggingModel/model-" +taskId+ ".model";				
		  Path file = new Path(pathStrModel);
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
      private Text supportVector = new Text();
  
      public void map(NullWritable offset, Text trainingSubset,Context context) throws IOException, InterruptedException {
        String[] svRecords = trainingSubset.toString().split("\n");
        SvmTrainer svmTrainer = new SvmTrainer(svRecords);
		svm_model model = svmTrainer.train();        
        int[] svIndices = model.sv_indices; 
        String userOutputPathStr = context.getConfiguration().get("USER_OUTPUT_PATH");
        int taskId = context.getTaskAttemptID().getTaskID().getId();
        try {
			saveModelToHdfs(model,userOutputPathStr,taskId,context);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
          
         for(int i=0; i<svIndices.length; i++) {
             supportVector.set(svRecords[svIndices[i]-1]);
             context.write(NullWritable.get(), supportVector);
          }
        }
    }
}

