package llz.wit.edu.cn;
import java.io.IOException;
import SVMSourceCode.*;
import libsvm.*;


public class MultiClass {

	public static void MultiClassLibSVM() throws IOException
	{
		 String[] arg = { "-s","0","-t","0","-c","10",
				          "DataSet\\MultiClassData", //训练集
			              "Results\\MultiClassModel.txt"}; // 存放SVM训练模型
		 
		 String[] parg = {"DataSet\\MultiClassData", //测试数据
				   "Results\\MultiClassModel.txt", // 调用训练模型
				   "Results\\MultiClassPredict.txt" }; //预测结果
			System.out.println("........SVM运行开始..........");
			long start=System.currentTimeMillis(); 
			svm_train t = new svm_train();
			svm_model multi_model = t.run(arg);		
			svm_predict dp = new svm_predict();
		
			
			System.out.println("用时:"+(System.currentTimeMillis()-start)); 
			
			//预测
			svm_predict.main(parg); 			
	}
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		MultiClassLibSVM();

	}

}
