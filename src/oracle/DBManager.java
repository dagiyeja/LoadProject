package oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public
class DBManager {
	static private DBManager instance;
	private String driver="oracle.jdbc.OracleDriver";
	private String url="jdbc:oracle:thin:@localhost:1521:XE";
	private String user="batman";
	private String password="1234";
	
	Connection con; //���� ��, �� ���� ��� ��ü
 	
	//new ���� ����
	/*
	 * 1.����̹� �ε�
	 * 2.����
	 * 3.��������
	 * 4.�ݳ�
	 * */
	private DBManager(){
		try {
			Class.forName(driver);
			con=DriverManager.getConnection(url,user,password);
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	static public DBManager getInstance(){
		if(instance==null){ //singleton�������� �ۼ�
			instance=new DBManager();
		
		}return instance;
	}
	
	//���� ��ü ��ȯ
	public Connection getConnection(){
		return con;
	}
	
	//���� ����
	public void disConnect(Connection con){
		if(con!=null){
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}