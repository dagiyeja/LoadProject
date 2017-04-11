package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

import util.file.FileUtil;

public class LoadMain extends JFrame implements ActionListener, TableModelListener, Runnable{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open, bt_load, bt_excel, bt_del;
	JTable table;
	JScrollPane scroll;
	JFileChooser chooser;
	FileReader reader;
	BufferedReader buffr;
	
	//������ â�� ������ �̹� ������ Ȯ���س���!!
	DBManager manager=DBManager.getInstance(); //�����ڷ� ���͵� ��
	Connection con;
	Vector<Vector> list;
	Vector<String> columnName;
	MyModel myModel;
	Thread thread; //excel ��Ͻ� ���� ������
	//�־���? ������ ���� �ʹ� ���� ���, ��Ʈ��ũ ���°� ���� ���� ��� insert�� while�� �ӵ��� �����󰣴� 
	//���� �������� ���� �Ϻη� �ð� ������ ������ insert �õ��Ұ���
	
	//���� ���Ͽ� ���� ������ �������� �����尡 ����� �� �մ� ���·� �����س���!
	StringBuffer insertSql=new StringBuffer();
	String seq;
	
	
	public LoadMain() {
		p_north=new JPanel();
		t_path=new JTextField(20);
		bt_open=new JButton("CSV ���Ͽ���");
		bt_load=new JButton("�ε��ϱ�");
		bt_excel=new JButton("�����ε�");
		bt_del=new JButton("�����ϱ�");
		
		table=new JTable();
		scroll=new JScrollPane(table);
		chooser=new JFileChooser("C:/animal");
		
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_excel);
		p_north.add(bt_load);
		p_north.add(bt_del);
		
		add(p_north, BorderLayout.NORTH);
		add(scroll);
		
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_excel.addActionListener(this);
		bt_del.addActionListener(this);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable t=(JTable)e.getSource();
				
				int row=t.getSelectedRow();
				int col=0; //seq�� ù��° �÷��̴ϱ�
				
				seq=(String)t.getValueAt(row, col);
				
				
			}	
		});
	
		
		//������� ������ ����
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//�����ͺ��̽� �ڿ� ����
				manager.disConnect(con);
				
				//���μ��� ����
				System.exit(0);
				
			}
		});
			
		
		
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
	
	}
	
	public void init(){
		//Connection ���� ����
		con=manager.getConnection();
		
	}
	
	//���� Ž���� ����
	public void open(){
		int result=chooser.showOpenDialog(this);
		
		//���⸦ ������..���� ���Ͽ� ��Ʈ���� ��������
		if(result==JFileChooser.APPROVE_OPTION){
			//������ ������ ����
			File file=chooser.getSelectedFile();
			
			String ext=FileUtil.getExt(file.getName());
			
			//����...����.csv �� ��� split ��� ����
			//0123456 7
			//Ȯ������ ���� ���� i+1 			
			if(!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "CSV �� �����ϼ���");
				return; //���̻��� ������ ���´�. �Ʒ� ������ �����ϸ� �ȵ�!
			}
		
			t_path.setText(file.getAbsolutePath());
			
			try {
				reader=new FileReader(file); //���� ����
				buffr=new BufferedReader(reader); //ū ����. �������� ���Ƶ���
				
				String data;
				//�ϴ� ���븸 �ɾƳ��� ����, ������ ��������.-> load�� ������ ���Ƶ���
			}
			
			 catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	//csv-->oracle�� ������ ����(migration)�ϱ�
	public void load(){
		//���۽�Ʈ���� �̿��Ͽ� csv�� �����͸� 1�پ� �о�鿩 insert��Ű��!!
		//���ڵ� ���� ������..
		//while������ ������ �ʹ� �����Ƿ�,
		//��Ʈ��ũ�� ������ �� ���� ������ �Ϻη� ������Ű�鼭..
		String data;
		StringBuffer sb=new StringBuffer(); //String�� StringBuffer�� ����->�޸� ����
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data=buffr.readLine();
				
				if(data==null)break;
				
				String[] value=data.split(","); //.ó�� ���α׷��� ��ɼ� ���ھƴϹǷ� ���� ó������ �ʿ����.
				
				
				//seq���� �����ϰ� insert �ϰڴ�!!
				if(!value[0].equals("seq")){				
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values("+value[0]+", '"+value[1]+"', '"+value[2]+"', '"+value[3]+"', '"+value[4]+"', "+value[5]+", '"+value[6]+"')");
					
					//System.out.println(sb.toString());
					pstmt=con.prepareStatement(sb.toString());
					
					int result=pstmt.executeUpdate(); //���� ����
				
					//insert�� ������ append ���
					//������ ������ StringBuffer�� �����͸� ��� �����
					//���� ���� �� ������� �ߴµ� �̸� ������ nullException�� �߻�
					sb.delete(0, sb.length()); 
				}else{
					System.out.println("�� 1���̹Ƿ� ����");
				}
			}
			
			JOptionPane.showMessageDialog(this, "���̱׷��̼ǿϷ�");
			
			//JTable ������ ó��!
			getList();
			table.setModel(myModel=new MyModel(list, columnName));
			
			//���̺� �𵨰� �����ʿ��� ����
			table.getModel().addTableModelListener(this); //���� ������� ���� �� �� �ִ� �޼��� //setModel�� ����, ���� ����
			
			table.updateUI(); //�� ����, �ֽ� ������ �ݿ�
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//���� ���� �о db�� ���̱׷��̼� �ϱ�!!
	//java SE �������� ���̺귯�� �ִ�? X
	//open Source ���� ����Ʈ����
	//copyright <----> copyleft (apache ��ü)
	//POI ���̺귯��! http://apache.org
	/*
	 * ����-�ϳ��� ��Ʈ�� ���� ���� row�� �̷����. row�� �������� �÷����� �̷����
	�������� ��Ʈ�� �𿩼� �ϳ��� ���������� ��, poi���� worksheet �ϳ��� ���������� �ǹ�

	 * HSSFWorkbook :���� ����
	 * HSSFSheet: sheet 
	 * HSSFRow : row
	 * HSSFCell : cell 
	 * */
	public void loadExcel(){
		int result=chooser.showOpenDialog(this);
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		StringBuffer cols=new StringBuffer();
		StringBuffer data=new StringBuffer();
		
		if(result==JFileChooser.APPROVE_OPTION){
			File file=chooser.getSelectedFile();
			FileInputStream fis=null;
			
			try {
				fis=new FileInputStream(file);
				
				//��Ʈ���� poi�� �����Ѵ�
				HSSFWorkbook book=null;
				book=new HSSFWorkbook(fis);
				
				HSSFSheet  sheet=null;
				sheet=book.getSheet("sheet1");
				
				int total=sheet.getLastRowNum();
				
				/*----------------------------------------
				 ù��° row�� �����Ͱ� �ƴ� �÷� �����̹Ƿ�, 
				 �� �������� �����Ͽ� insert into table(****)
				 * ----------------------------------------*/
			
				System.out.println("�� ������ ù��° row ��ȣ�� "+sheet.getFirstRowNum());
				
				HSSFRow firstRow=sheet.getRow(sheet.getFirstRowNum());  //ù��° row �÷� ����
				
				//Row�� �������, �÷��� �м��Ѵ�
			
				//������ �÷� ������ �޸� ������ ó��
			
				 for(int i=0; i< firstRow.getLastCellNum(); i++){
					 HSSFCell cell=firstRow.getCell(i);
						data.delete(0, cols.length()); //StringBuffer ����
					 if(i <firstRow.getLastCellNum()-1){
						 System.out.print(cell.getStringCellValue()+",");		
						 cols.append(cell.getStringCellValue()+",");
					 }else{
						 System.out.print(cell.getStringCellValue());
						 cols.append(cell.getStringCellValue());
					 }
				 }
				 System.out.println("");
				
				DataFormatter df=new DataFormatter();  
				
				//�÷� ���� ���� ���
				for(int a=1; a<=total; a++){
					HSSFRow row=sheet.getRow(a);
					int columnCount=row.getLastCellNum();
					
					//Row�� ���, �÷� �м�
					data.delete(0, data.length()); //StringBuffer ����
					for(int i=0; i<columnCount;  i++){
						HSSFCell cell=row.getCell(i);
						
						//�ڷ����� ���ѵ��� �ʰ�  ���  String ó�� 
						String value=df.formatCellValue(cell);						
						//System.out.print(value);
						if(cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
							value="'"+value+"'";
						}
						
						if(i<columnCount-1){
							data.append(value+",");
						}else{
							data.append(value);
						}
					}
					//���� �ٲ𶧸��� �ٹٲ� ->��(row)��ŭ ������ ����
					//System.out.println("insert into hospital("+cols.toString()+") values("+data+")");
					insertSql.append("insert into hospital("+cols.toString()+") values("+data+");");
					
				} 
				
				//���� ��������, ����ϰ� �����忡 �� ��Ű��!!
				// Runnable �������̽��� �μ��� ������ Thread�� run�� �����ϴ� ���� �ƴ϶� 
				//Runnable �������̽��� ���������� run()�� �����ϰ� ��. ���� �츮�� ����
				thread=new Thread(this); 
				thread.start(); 
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
	}

	//��� ���ڵ� ��������!!
	public void getList(){
		String sql="select * from hospital order by seq asc";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			
			//�÷��� ����!!
			ResultSetMetaData meta=rs.getMetaData();  //rs�� �ױ� ���� ��Ÿ�����Ϳ� �ڱ� ���� �ѱ⵵��
			int count=meta.getColumnCount();
			columnName=new Vector();
			
			for(int i=0; i<count; i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list=new Vector<Vector>(); //������ ����
		
			//rs�� ������ ���ͷ� �����ϰ� ������!!
			while(rs.next()){
				Vector vec=new Vector(); //���ڵ� 1�� ���� ����
				
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));
				vec.add(rs.getString("addr"));
				vec.add(rs.getString("regdate"));
				vec.add(rs.getString("status"));
				vec.add(rs.getString("dimension"));
				vec.add(rs.getString("type"));
				
				list.add(vec); //1���� ���͸� list ���Ϳ� ��´�->2���� ����
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			if(rs!=null){
				try {
					rs.close();
				} catch (SQLException e) {					
					e.printStackTrace();
				}
			}
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	//������ ���ڵ� ����
	public void delete(){
		int ans=JOptionPane.showConfirmDialog(LoadMain.this, seq+"�����ҷ���?");
		if(ans==JOptionPane.OK_OPTION){
			String sql="delete from hospital where seq="+seq;
			PreparedStatement pstmt=null;
			
			try {
				pstmt=con.prepareStatement(sql);
				int result=pstmt.executeUpdate();
				if(result!=0){
					JOptionPane.showMessageDialog(this,"�����Ϸ�");
					table.updateUI();; //���� �� ���̺� ����
					
					getList();
					//��� �ϼ��� list�� �ٽ� MyModel�� ����!! ->�ֳĸ� list�� ������ ����Ʈ�� �ƴ� �ε���ο��� ���� ����Ʈ�ϱ� 
					myModel.list=list;
					table.updateUI();
				}
				
			} catch (SQLException e) {
			
				e.printStackTrace();
			}finally{
				if(pstmt!=null){
					try {
						pstmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			
					
			
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		Object obj=e.getSource();
		if(obj==bt_open){
			open();
		}else if(obj==bt_load){
			load();
		}else if(obj==bt_excel){
			loadExcel();
		}else if(obj==bt_del){
			delete();
		}
	}

	
	// ���̺� ���� ������ ���� ������ �߻��ϸ�, 
	//�� ������ �����ϴ� ������!!
	public void tableChanged(TableModelEvent e) {
		int row=table.getSelectedRow();
		int col=table.getSelectedColumn();
		//System.out.println(row);
		
		String column=columnName.elementAt(col); //���� ������ �÷�
		
		String value=(String)table.getValueAt(row, col); //������ ��ǥ�� �� ��ȯ
		
		String seq=(String)table.getValueAt(row,0);
		String sql="update hospital set "+column+"='"+value+"' ";
		sql+=" where seq="+seq;
		System.out.println(sql);
		//System.out.println("����� ������ �������� ��ġ�� "+row+","+col);
		
		PreparedStatement pstmt=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			int result=pstmt.executeUpdate();
			if(result!=0){
				JOptionPane.showMessageDialog(this, "�����Ϸ�");
			}
		} catch (SQLException e1) {
			
			e1.printStackTrace();
		}finally{
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e1) {
					
					e1.printStackTrace();
				}
			}
		}
		
				
		
		
	}
	
	public void run() {
		//insertSql�� insert���� ����� �˾ƺ��� 
		String[] str=insertSql.toString().split(";"); 
		System.out.println("insert�� ���� "+str.length);
		
		PreparedStatement pstmt=null;
		
		for(int i=0; i<str.length; i++){
			//System.out.println(str[i]);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				pstmt=con.prepareStatement(str[i]);
				int result=pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		//������ ����ߴ� StringBuffer ����
		insertSql.delete(0, insertSql.length());
		if(pstmt!=null){
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		new LoadMain();
	}


	
}
