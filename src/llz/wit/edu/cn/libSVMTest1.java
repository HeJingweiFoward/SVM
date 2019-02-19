package llz.wit.edu.cn;
import libsvm.*;
import java.io.*;
import java.util.*;
import SVMSourceCode.svm_predict;
import SVMSourceCode.svm_train;


public class libSVMTest1 {
//	public static String[] str_trained = {"-g","2.0","-c","32","-t","2","-m","500.0","-h","0","E:\\test\\train\\IF_IDF\\allTrainVSM.txt"};
	
	static void alaTest() throws IOException {
		// TODO Auto-generated method stub
		 String[] arg = { "DataSet\\a8a", //训练集
			"Results\\model.txt" }; // 存放SVM训练模型
		 
		 String[] parg = {"DataSet\\a8a.t", //测试数据
				   "Results\\model.txt", // 调用训练模型
				   "Results\\predict.txt" }; //预测结果
			System.out.println("........SVM运行开始..........");
			long start=System.currentTimeMillis(); 
			svm_train.main(arg); //训练
			System.out.println("用时:"+(System.currentTimeMillis()-start)); 
			//预测
			svm_predict.main(parg); 	
	}
	
	
	static void breastcancerTest() throws IOException {
		
		 String[] arg = {"-s","0","-t","2","-c","1","-g","0","-v","10","DataSet\\breastCancer", //训练集
			"Results\\BCModel.txt" }; // 存放SVM训练模型
		 
		 String[] parg = {"DataSet\\breastCancer1", //测试数据
				   "Results\\BCModel.txt", // 调用训练模型
				   "Results\\BCPredict.txt" }; //预测结果
		 
		 System.out.println("........SVM运行开始.........."); 
		 long start=System.currentTimeMillis(); 
		 svm_train t = new svm_train();
		 svm_model model = t.run(arg);	
		 System.out.println("用时:"+(System.currentTimeMillis()-start)); 
		 
		 if(model != null)
		 {
			 System.out.println("Tatal SVS:"+model.l); 
			 svm_predict.main(parg);
		 }
		 
	}
	
	
	static void covTypeTest() throws IOException {
		
		 String[] arg = { "DataSet\\covtypebinaryscale", //训练集
			"Results\\CovTypeModel.txt" }; // 存放SVM训练模型
		 
		 System.out.println("........SVM运行开始.........."); 
		 long start=System.currentTimeMillis(); 
		 svm_train t = new svm_train();
		 svm_model model = t.run(arg);		
			
		 System.out.println("用时:"+(System.currentTimeMillis()-start)); 
		 
	}

	public static void main(String[] args) throws IOException {
		alaTest();
		//breastcancerTest();
		//covTypeTest();
	}
}
