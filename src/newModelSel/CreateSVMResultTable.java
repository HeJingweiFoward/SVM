package newModelSel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.exceptions.DeserializationException;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;




public class CreateSVMResultTable {

	Connection connection;
	Admin admin;

	public CreateSVMResultTable() throws IOException {
		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum",
				"192.168.2.151:2181,192.168.2.152:2181,192.168.2.153:2181,192.168.2.154:2181");// zookeeper��ַ
		conf.set("hbase.zookeeper.property.clientPort", "2181");// zookeeper�˿�
		// ȡ��һ�����ݿ����Ӷ���
		connection = ConnectionFactory.createConnection(conf);
		admin = connection.getAdmin();
	}

	// ����һ�ű�
	public void CreateTable(String strTableName) throws IOException {
		TableName tablename = TableName.valueOf(strTableName);
		if (admin.tableExists(tablename)) {
			System.out.println(tablename + "���Ѵ��ڣ�����ɾ���ñ����´�����");
			admin.disableTable(tablename);
			admin.deleteTable(tablename);
		}
		// ������
		HTableDescriptor hTableDescriptor = new HTableDescriptor(tablename);
		// ��������
		HColumnDescriptor family = new HColumnDescriptor("BaseInfo");

		hTableDescriptor.addFamily(family);// ��������
		admin.createTable(hTableDescriptor);// ������
		System.out.println(tablename + "           Hbase�����ɹ�!");
		connection.close();
	}

	
	
	// �����¼
	public void Add(String tableName) throws IOException {

		System.out.println("---------------��ʼ��������-----------------");

		// ȡ��һ�����ݱ����
		Table table = connection.getTable(TableName.valueOf(tableName));

		// ��Ҫ�������ݿ�����ݼ���
		List<Put> putList = new ArrayList<Put>();

		Put put;

		// �������ݼ���
		for (int i = 0; i < 10; i++) {
			put = new Put(Bytes.toBytes(String.valueOf(i)));
			put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("C"),
					Bytes.toBytes("C" +String.valueOf(i)));
			put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("G"),
					Bytes.toBytes("G" +String.valueOf(i)));
			put.addColumn(Bytes.toBytes("BaseInfo"), Bytes.toBytes("CostTime"),
					Bytes.toBytes("CT" +String.valueOf(i)));
			put.addColumn(Bytes.toBytes("BaseInfo"),
					Bytes.toBytes("CVAccuracy"), Bytes.toBytes("CVA" +String.valueOf(i)));
			putList.add(put);
		}

		// �����ݼ��ϲ��뵽���ݿ�
		table.put(putList);

		System.out.println("---------------�������ݽ���-----------------");

	}
   //���м���ѯ������
	public void queryTableByRowKey(String tableName) throws IOException{
		 
        System.out.println("---------------���м���ѯ�����ݿ�ʼ-----------------");
 
        // ȡ�����ݱ����
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // �½�һ����ѯ������Ϊ��ѯ����
        Get get = new Get("0".getBytes());
 
        // ���м���ѯ����
        Result result = table.get(get);
 
        byte[] row = result.getRow();
        System.out.println("row key :" + new String(row));
 
        List<Cell> listCells = result.listCells();
        for (Cell cell : listCells) {
 
        	System.out.println(new String (CellUtil.cloneFamily(cell) )+
					"    "+new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
        }
 
        System.out.println("---------------���м���ѯ�����ݽ���-----------------");
 
    }

	public class CTP
	{
		public Long getCostTime() {
			return CostTime;
		}
		public void setCostTime(Long costTime) {
			CostTime = costTime;
		}
		public String getC() {
			return C;
		}
		public void setC(String c) {
			C = c;
		}
		public String getG() {
			return G;
		}
		public void setG(String g) {
			G = g;
		}
		public Long CostTime;
		public String C;
		public String G;
	}
	
	
	public void queryTableByCondition(String reduceNum,String tableName) throws IOException{
		 
        System.out.println("---------------��������ѯ�����ݿ�ʼ-----------------");
 
        // ȡ�����ݱ����
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // ����һ����ѯ������
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ReduceNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(reduceNum));
 
        // ����һ�����ݱ�ɨ����
        Scan scan = new Scan();
 
        // ����ѯ���������뵽���ݱ�ɨ��������
        scan.setFilter(filter);
 
        // ִ�в�ѯ��������ȡ�ò�ѯ���
        ResultScanner scanner = table.getScanner(scan);
 
        // ѭ�������ѯ���
        
        List<Long>costTimeList=new ArrayList<Long>();
        for (Result result : scanner) {
            byte[] row = result.getRow();
            System.out.println("row key:" + new String(row));

            List<Cell> listCells = result.listCells();
            
            for (Cell cell : listCells) {               
                String colName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());
            	if(colName.equals("CostTime"))
            	{
                System.out.println(new String (CellUtil.cloneFamily(cell) )+
						"    "+new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
                
                costTimeList.add(Long.parseLong( new String (CellUtil.cloneValue(cell))));
            	}
   
        }
        }

        long costTimeSum=0;
        Collections.sort(costTimeList);
        for (Long costTime : costTimeList) {
			System.out.println(costTime);
			costTimeSum+=costTime;
		}
        double costTimeAvg=costTimeSum*1.0/costTimeList.size();
        System.out.print("Reduce���ʱ�䣺");
        System.out.println(costTimeList.get(0));
        System.out.print("Reduce�ʱ�䣺");
        System.out.println(costTimeList.get(costTimeList.size()-1));
        System.out.print("Reduce������");
        System.out.println(costTimeList.size());
        System.out.print("Reduceƽ��ʱ�䣺");
        System.out.println(String.format("%.2f", costTimeAvg));
        
        
        System.out.println("---------------��������ѯ�����ݽ���-----------------");

    }
	
	public void queryTableByExperimentNum(String experimentNum,String tableName) throws IOException{
		 
        System.out.println("---------------��������ѯ�����ݿ�ʼ-----------------");
 
        // ȡ�����ݱ����
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // ����һ����ѯ������
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ExperimentNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(experimentNum));
 
        // ����һ�����ݱ�ɨ����
        Scan scan = new Scan();
 
        // ����ѯ���������뵽���ݱ�ɨ��������
        scan.setFilter(filter);
 
        // ִ�в�ѯ��������ȡ�ò�ѯ���
        ResultScanner scanner = table.getScanner(scan);
 
        // ѭ�������ѯ���
        
        List<Long>costTimeList=new ArrayList<Long>();
        for (Result result : scanner) {
            byte[] row = result.getRow();
            System.out.println("row key:" + new String(row));

            List<Cell> listCells = result.listCells();
            
            for (Cell cell : listCells) {               
                String colName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());
            	if(colName.equals("CostTime"))
            	{
                System.out.print("    "+new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell))+"    ");
                
                costTimeList.add(Long.parseLong( new String (CellUtil.cloneValue(cell))));
            	}
            	
              	if(colName.equals("C"))
            	{
                System.out.print(new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
                
            	}
              	if(colName.equals("G"))
            	{
                System.out.println(new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
                
            	}
   
        }
        }

        long costTimeSum=0;
        Collections.sort(costTimeList);
        for (Long costTime : costTimeList) {
			System.out.println(costTime);
			costTimeSum+=costTime;
		}
        double costTimeAvg=costTimeSum*1.0/costTimeList.size();
        System.out.print("Reduce���ʱ�䣺");
        System.out.println(costTimeList.get(0));
        System.out.print("Reduce�ʱ�䣺");
        System.out.println(costTimeList.get(costTimeList.size()-1));
        System.out.print("Reduce������");
        System.out.println(costTimeList.size());
        System.out.print("Reduceƽ��ʱ�䣺");
        System.out.println(String.format("%.2f", costTimeAvg));
        
        
        System.out.println("---------------��������ѯ�����ݽ���-----------------");

    }
	
	
	public void queryAccuracyByExperimentNum(String experimentNum,String tableName) throws IOException{
		 
        System.out.println("---------------��������ѯ�����ݿ�ʼ-----------------");
 
        // ȡ�����ݱ����
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // ����һ����ѯ������
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ExperimentNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(experimentNum));
 
        // ����һ�����ݱ�ɨ����
        Scan scan = new Scan();
 
        // ����ѯ���������뵽���ݱ�ɨ��������
        scan.setFilter(filter);
 
        // ִ�в�ѯ��������ȡ�ò�ѯ���
        ResultScanner scanner = table.getScanner(scan);
 
        // ѭ�������ѯ���
        
        List<Double>accuracyList=new ArrayList<Double>();
        for (Result result : scanner) {
            byte[] row = result.getRow();
            System.out.println("row key:" + new String(row));

            List<Cell> listCells = result.listCells();
            
            for (Cell cell : listCells) {               
                String colName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());
            	if(colName.equals("CVAccuracy"))
            	{
                System.out.print("    "+new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell))+"    ");
                
                accuracyList.add(Double.parseDouble( new String (CellUtil.cloneValue(cell)).replace("%", "")));
            	}
            	
              	if(colName.equals("C"))
            	{
                System.out.print(new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
                
            	}
              	if(colName.equals("G"))
            	{
                System.out.println(new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
                
            	}
   
        }
        }

        Collections.sort(accuracyList);
        System.out.print("��;��ȣ�");
        System.out.println(accuracyList.get(0)+"%");
        System.out.print("��߾��ȣ�");
        System.out.println(accuracyList.get(accuracyList.size()-1)+"%");
        System.out.print("Reduce������");
        System.out.println(accuracyList.size());

        
        
        System.out.println("---------------��������ѯ�����ݽ���-----------------");

    }
	
	 /**
     * ɾ���У���������
     */
	public void deleteByCondition(String reduceNum,String tableName) throws IOException, DeserializationException{
		 
        System.out.println("---------------ɾ���У��������� ��ʼ-----------------");
 
        // ����1������queryTableByCondition()����ȡ����Ҫɾ���������б� 
     // ȡ�����ݱ����
        Table table = connection.getTable(TableName.valueOf("SVMResult"));
 
        // ����һ����ѯ������
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ReduceNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(reduceNum));
 
        // ����һ�����ݱ�ɨ����
        Scan scan = new Scan();
 
        // ����ѯ���������뵽���ݱ�ɨ��������
        scan.setFilter(filter);
 
        // ִ�в�ѯ��������ȡ�ò�ѯ���
        ResultScanner scanner = table.getScanner(scan);
 
        // ����2��ѭ������1�Ĳ�ѯ�������ÿ���������deleteByRowKey()����
        for (Result result : scanner) {
            byte[] row = result.getRow();
           
            deleteByRowKey(row,tableName);
        }
        System.out.println("---------------ɾ���У��������� ����-----------------");
 
    }


    public void deleteByRowKey(byte[] rowKey,String tableName) throws IOException{
 
        System.out.println("---------------ɾ���� ��ʼ-----------------");
 
        // ȡ�ô����������ݱ����
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // ����ɾ����������
        Delete delete = new Delete(rowKey);
 
        // ִ��ɾ������
        table.delete(delete);
        System.out.println("rowkey:" + new String(rowKey));
        System.out.println("---------------ɾ���� ����-----------------");
    }

	
	// ��ѯ��������
	public void queryTable(String tableName) throws IOException {

		System.out.println("---------------��ѯ��������-----------------");

		// ȡ�����ݱ����
		Table table = connection.getTable(TableName.valueOf(tableName));

		// ȡ�ñ�����������
		ResultScanner scanner = table.getScanner(new Scan());

		// ѭ��������е�����
		for (Result result : scanner) {

			byte[] row = result.getRow();
			System.out.println("row key :" + new String(row));

			List<Cell> listCells = result.listCells();
			for (Cell cell : listCells) {

				byte[] familyArray = cell.getFamilyArray();
				byte[] qualifierArray = cell.getQualifierArray();
				byte[] valueArray = cell.getValueArray();

				System.out.println(new String (CellUtil.cloneFamily(cell) )+
						"    "+new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
			}
		}

		System.out.println("---------------��ѯ�����������-----------------");

	}

	//��ձ�
    public void truncateTable(String strTableName) throws IOException{
    	 
        System.out.println("---------------��ʼ��ձ�-----------------");
 
        // ȡ��Ŀ�����ݱ�ı�������
        TableName tableName = TableName.valueOf(strTableName);
 
        // ���ñ�״̬Ϊ��Ч
        admin.disableTable(tableName);
        // ���ָ���������
        admin.truncateTable(tableName, true);
 
        System.out.println("---------------��ձ����-----------------");

    }
    
    //��������
    public void AddFamily(String strTableName,String strColumnDescriptor) throws IOException {

        System.out.println("---------------�½����� ��ʼ-----------------");

        // ȡ��Ŀ�����ݱ�ı�������
        TableName tableName = TableName.valueOf(strTableName);

        // �����������
        HColumnDescriptor columnDescriptor = new HColumnDescriptor(strColumnDescriptor);

        // ���´�����������ӵ�ָ�������ݱ�
        admin.addColumn(tableName, columnDescriptor);

        System.out.println("---------------�½����� ����-----------------");

	}
    
    
	public static void main(String[] args) throws IOException, DeserializationException {
		// TODO Auto-generated method stub
		CreateSVMResultTable createSVMResultTable = new CreateSVMResultTable();

		//���ڼ���ʵ���ѯʱ��
		createSVMResultTable.queryTableByExperimentNum("9","SVMResultT1");
		//���ڼ���ʵ���ѯ����
		//createSVMResultTable.queryAccuracyByExperimentNum("8", "SVMResultT1");
	}

}
