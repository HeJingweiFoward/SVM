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
				"192.168.2.151:2181,192.168.2.152:2181,192.168.2.153:2181,192.168.2.154:2181");// zookeeper地址
		conf.set("hbase.zookeeper.property.clientPort", "2181");// zookeeper端口
		// 取得一个数据库连接对象
		connection = ConnectionFactory.createConnection(conf);
		admin = connection.getAdmin();
	}

	// 创建一张表
	public void CreateTable(String strTableName) throws IOException {
		TableName tablename = TableName.valueOf(strTableName);
		if (admin.tableExists(tablename)) {
			System.out.println(tablename + "表已存在，现在删除该表重新创建！");
			admin.disableTable(tablename);
			admin.deleteTable(tablename);
		}
		// 表描述
		HTableDescriptor hTableDescriptor = new HTableDescriptor(tablename);
		// 列族描述
		HColumnDescriptor family = new HColumnDescriptor("BaseInfo");

		hTableDescriptor.addFamily(family);// 创建列族
		admin.createTable(hTableDescriptor);// 创建表
		System.out.println(tablename + "           Hbase表创建成功!");
		connection.close();
	}

	
	
	// 插入记录
	public void Add(String tableName) throws IOException {

		System.out.println("---------------开始插入数据-----------------");

		// 取得一个数据表对象
		Table table = connection.getTable(TableName.valueOf(tableName));

		// 需要插入数据库的数据集合
		List<Put> putList = new ArrayList<Put>();

		Put put;

		// 生成数据集合
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

		// 将数据集合插入到数据库
		table.put(putList);

		System.out.println("---------------插入数据结束-----------------");

	}
   //按行键查询表数据
	public void queryTableByRowKey(String tableName) throws IOException{
		 
        System.out.println("---------------按行键查询表数据开始-----------------");
 
        // 取得数据表对象
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // 新建一个查询对象作为查询条件
        Get get = new Get("0".getBytes());
 
        // 按行键查询数据
        Result result = table.get(get);
 
        byte[] row = result.getRow();
        System.out.println("row key :" + new String(row));
 
        List<Cell> listCells = result.listCells();
        for (Cell cell : listCells) {
 
        	System.out.println(new String (CellUtil.cloneFamily(cell) )+
					"    "+new String (CellUtil.cloneQualifier(cell))+"        " + new String (CellUtil.cloneValue(cell)));
        }
 
        System.out.println("---------------按行键查询表数据结束-----------------");
 
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
		 
        System.out.println("---------------按条件查询表数据开始-----------------");
 
        // 取得数据表对象
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // 创建一个查询过滤器
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ReduceNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(reduceNum));
 
        // 创建一个数据表扫描器
        Scan scan = new Scan();
 
        // 将查询过滤器加入到数据表扫描器对象
        scan.setFilter(filter);
 
        // 执行查询操作，并取得查询结果
        ResultScanner scanner = table.getScanner(scan);
 
        // 循环输出查询结果
        
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
        System.out.print("Reduce最短时间：");
        System.out.println(costTimeList.get(0));
        System.out.print("Reduce最长时间：");
        System.out.println(costTimeList.get(costTimeList.size()-1));
        System.out.print("Reduce总数：");
        System.out.println(costTimeList.size());
        System.out.print("Reduce平均时间：");
        System.out.println(String.format("%.2f", costTimeAvg));
        
        
        System.out.println("---------------按条件查询表数据结束-----------------");

    }
	
	public void queryTableByExperimentNum(String experimentNum,String tableName) throws IOException{
		 
        System.out.println("---------------按条件查询表数据开始-----------------");
 
        // 取得数据表对象
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // 创建一个查询过滤器
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ExperimentNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(experimentNum));
 
        // 创建一个数据表扫描器
        Scan scan = new Scan();
 
        // 将查询过滤器加入到数据表扫描器对象
        scan.setFilter(filter);
 
        // 执行查询操作，并取得查询结果
        ResultScanner scanner = table.getScanner(scan);
 
        // 循环输出查询结果
        
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
        System.out.print("Reduce最短时间：");
        System.out.println(costTimeList.get(0));
        System.out.print("Reduce最长时间：");
        System.out.println(costTimeList.get(costTimeList.size()-1));
        System.out.print("Reduce总数：");
        System.out.println(costTimeList.size());
        System.out.print("Reduce平均时间：");
        System.out.println(String.format("%.2f", costTimeAvg));
        
        
        System.out.println("---------------按条件查询表数据结束-----------------");

    }
	
	
	public void queryAccuracyByExperimentNum(String experimentNum,String tableName) throws IOException{
		 
        System.out.println("---------------按条件查询表数据开始-----------------");
 
        // 取得数据表对象
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // 创建一个查询过滤器
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ExperimentNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(experimentNum));
 
        // 创建一个数据表扫描器
        Scan scan = new Scan();
 
        // 将查询过滤器加入到数据表扫描器对象
        scan.setFilter(filter);
 
        // 执行查询操作，并取得查询结果
        ResultScanner scanner = table.getScanner(scan);
 
        // 循环输出查询结果
        
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
        System.out.print("最低精度：");
        System.out.println(accuracyList.get(0)+"%");
        System.out.print("最高精度：");
        System.out.println(accuracyList.get(accuracyList.size()-1)+"%");
        System.out.print("Reduce总数：");
        System.out.println(accuracyList.size());

        
        
        System.out.println("---------------按条件查询表数据结束-----------------");

    }
	
	 /**
     * 删除行（按条件）
     */
	public void deleteByCondition(String reduceNum,String tableName) throws IOException, DeserializationException{
		 
        System.out.println("---------------删除行（按条件） 开始-----------------");
 
        // 步骤1：调用queryTableByCondition()方法取得需要删除的数据列表 
     // 取得数据表对象
        Table table = connection.getTable(TableName.valueOf("SVMResult"));
 
        // 创建一个查询过滤器
        Filter filter = new SingleColumnValueFilter(Bytes.toBytes("Parameters"), Bytes.toBytes("ReduceNum"), 
                                                    CompareOp.EQUAL, Bytes.toBytes(reduceNum));
 
        // 创建一个数据表扫描器
        Scan scan = new Scan();
 
        // 将查询过滤器加入到数据表扫描器对象
        scan.setFilter(filter);
 
        // 执行查询操作，并取得查询结果
        ResultScanner scanner = table.getScanner(scan);
 
        // 步骤2：循环步骤1的查询结果，对每个结果调用deleteByRowKey()方法
        for (Result result : scanner) {
            byte[] row = result.getRow();
           
            deleteByRowKey(row,tableName);
        }
        System.out.println("---------------删除行（按条件） 结束-----------------");
 
    }


    public void deleteByRowKey(byte[] rowKey,String tableName) throws IOException{
 
        System.out.println("---------------删除行 开始-----------------");
 
        // 取得待操作的数据表对象
        Table table = connection.getTable(TableName.valueOf(tableName));
 
        // 创建删除条件对象
        Delete delete = new Delete(rowKey);
 
        // 执行删除操作
        table.delete(delete);
        System.out.println("rowkey:" + new String(rowKey));
        System.out.println("---------------删除行 结束-----------------");
    }

	
	// 查询整表数据
	public void queryTable(String tableName) throws IOException {

		System.out.println("---------------查询整表数据-----------------");

		// 取得数据表对象
		Table table = connection.getTable(TableName.valueOf(tableName));

		// 取得表中所有数据
		ResultScanner scanner = table.getScanner(new Scan());

		// 循环输出表中的数据
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

		System.out.println("---------------查询整表数据完成-----------------");

	}

	//清空表
    public void truncateTable(String strTableName) throws IOException{
    	 
        System.out.println("---------------开始清空表-----------------");
 
        // 取得目标数据表的表名对象
        TableName tableName = TableName.valueOf(strTableName);
 
        // 设置表状态为无效
        admin.disableTable(tableName);
        // 清空指定表的数据
        admin.truncateTable(tableName, true);
 
        System.out.println("---------------清空表完成-----------------");

    }
    
    //增加列族
    public void AddFamily(String strTableName,String strColumnDescriptor) throws IOException {

        System.out.println("---------------新建列族 开始-----------------");

        // 取得目标数据表的表名对象
        TableName tableName = TableName.valueOf(strTableName);

        // 创建列族对象
        HColumnDescriptor columnDescriptor = new HColumnDescriptor(strColumnDescriptor);

        // 将新创建的列族添加到指定的数据表
        admin.addColumn(tableName, columnDescriptor);

        System.out.println("---------------新建列族 结束-----------------");

	}
    
    
	public static void main(String[] args) throws IOException, DeserializationException {
		// TODO Auto-generated method stub
		CreateSVMResultTable createSVMResultTable = new CreateSVMResultTable();

		//按第几次实验查询时间
		createSVMResultTable.queryTableByExperimentNum("9","SVMResultT1");
		//按第几次实验查询精度
		//createSVMResultTable.queryAccuracyByExperimentNum("8", "SVMResultT1");
	}

}
