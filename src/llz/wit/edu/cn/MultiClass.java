package llz.wit.edu.cn;
import java.io.IOException;
import SVMSourceCode.*;
import libsvm.*;


public class MultiClass {

	public static void MultiClassLibSVM() throws IOException
	{
		 String[] arg = { "-s","0","-t","0","-c","10",
				          "DataSet\\MultiClassData", //ѵ����
			              "Results\\MultiClassModel.txt"}; // ���SVMѵ��ģ��
		 
		 String[] parg = {"DataSet\\MultiClassData", //��������
				   "Results\\MultiClassModel.txt", // ����ѵ��ģ��
				   "Results\\MultiClassPredict.txt" }; //Ԥ����
			System.out.println("........SVM���п�ʼ..........");
			long start=System.currentTimeMillis(); 
			svm_train t = new svm_train();
			svm_model multi_model = t.run(arg);		
			svm_predict dp = new svm_predict();
		
			
			System.out.println("��ʱ:"+(System.currentTimeMillis()-start)); 
			
			//Ԥ��
			svm_predict.main(parg); 			
	}
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		MultiClassLibSVM();

	}

}
