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
		 String[] arg = { "DataSet\\a8a", //ѵ����
			"Results\\model.txt" }; // ���SVMѵ��ģ��
		 
		 String[] parg = {"DataSet\\a8a.t", //��������
				   "Results\\model.txt", // ����ѵ��ģ��
				   "Results\\predict.txt" }; //Ԥ����
			System.out.println("........SVM���п�ʼ..........");
			long start=System.currentTimeMillis(); 
			svm_train.main(arg); //ѵ��
			System.out.println("��ʱ:"+(System.currentTimeMillis()-start)); 
			//Ԥ��
			svm_predict.main(parg); 	
	}
	
	
	static void breastcancerTest() throws IOException {
		
		 String[] arg = {"-s","0","-t","2","-c","1","-g","0","-v","10","DataSet\\breastCancer", //ѵ����
			"Results\\BCModel.txt" }; // ���SVMѵ��ģ��
		 
		 String[] parg = {"DataSet\\breastCancer1", //��������
				   "Results\\BCModel.txt", // ����ѵ��ģ��
				   "Results\\BCPredict.txt" }; //Ԥ����
		 
		 System.out.println("........SVM���п�ʼ.........."); 
		 long start=System.currentTimeMillis(); 
		 svm_train t = new svm_train();
		 svm_model model = t.run(arg);	
		 System.out.println("��ʱ:"+(System.currentTimeMillis()-start)); 
		 
		 if(model != null)
		 {
			 System.out.println("Tatal SVS:"+model.l); 
			 svm_predict.main(parg);
		 }
		 
	}
	
	
	static void covTypeTest() throws IOException {
		
		 String[] arg = { "DataSet\\covtypebinaryscale", //ѵ����
			"Results\\CovTypeModel.txt" }; // ���SVMѵ��ģ��
		 
		 System.out.println("........SVM���п�ʼ.........."); 
		 long start=System.currentTimeMillis(); 
		 svm_train t = new svm_train();
		 svm_model model = t.run(arg);		
			
		 System.out.println("��ʱ:"+(System.currentTimeMillis()-start)); 
		 
	}

	public static void main(String[] args) throws IOException {
		alaTest();
		//breastcancerTest();
		//covTypeTest();
	}
}
