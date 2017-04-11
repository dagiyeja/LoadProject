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
	
	Connection con; //접속 후, 그 정보 담는 객체
 	
	//new 막기 위함
	/*
	 * 1.드라이버 로드
	 * 2.접속
	 * 3.쿼리실행
	 * 4.반납
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
		if(instance==null){ //singleton패턴으로 작성
			instance=new DBManager();
		
		}return instance;
	}
	
	//접속 객체 반환
	public Connection getConnection(){
		return con;
	}
	
	//접속 해제
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
