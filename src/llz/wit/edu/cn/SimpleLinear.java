package llz.wit.edu.cn;

import java.awt.print.Book;
import java.io.IOException;
import java.util.*;
import SVMSourceCode.*;
import libsvm.*;

public class SimpleLinear {

	public static void LinearSVM() throws IOException
	{
		 String[] arg = { "-s","0","-t","0","-c","10",
				          "DataSet\\SimpleLinearData", //训练集
			              "Results\\LinearModel.txt"}; // 存放SVM训练模型
		 
		 String[] parg = {"DataSet\\SimpleLinearData", //测试数据
				   "Results\\LinearModel.txt", // 调用训练模型
				   "Results\\LinearPredict.txt" }; //预测结果
			System.out.println("........SVM运行开始..........");
			long start=System.currentTimeMillis(); 
			svm_train t = new svm_train();
			svm_model model = t.run(arg);
			
			int  i =0;
			double[] w = new double[model.SV[0].length];
 			
			for(double[] coef:model.sv_coef)
			{
				for(svm_node[] node:model.SV)
				{
					double ci  = coef[i];
					
					for(int j=0;j<node.length;j++)
					{
						w[j] += ci*node[j].value;
					}
					
					i++;
				}				
			}			
			
			System.out.println("用时:"+(System.currentTimeMillis()-start)); 
			
			System.out.print("w is: (");
			for(double wi:w)
			{
				System.out.print(String.format("%.3f",wi) + ",");
			}
			
			System.out.println(")");
			
			System.out.print("b is: " + model.rho[0]);
	
			
			//预测
			//svm_predict.main(parg); 			
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		LinearSVM();	
	}

}
