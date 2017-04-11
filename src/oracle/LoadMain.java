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
	
	//윈도우 창이 열리면 이미 접속을 확보해놓자!!
	DBManager manager=DBManager.getInstance(); //생성자로 얻어와도 됨
	Connection con;
	Vector<Vector> list;
	Vector<String> columnName;
	MyModel myModel;
	Thread thread; //excel 등록시 사용될 쓰레드
	//왜쓰지? 데이터 양이 너무 많을 경우, 네트워크 상태가 좋지 않을 경우 insert가 while문 속도를 못따라간다 
	//따라서 안정성을 위해 일부러 시간 지연을 일으켜 insert 시도할거임
	
	//엑셀 파일에 의해 생성된 쿼리문을 쓰레드가 사용할 수 잇는 상태로 저장해놓자!
	StringBuffer insertSql=new StringBuffer();
	String seq;
	
	
	public LoadMain() {
		p_north=new JPanel();
		t_path=new JTextField(20);
		bt_open=new JButton("CSV 파일열기");
		bt_load=new JButton("로드하기");
		bt_excel=new JButton("엑셀로드");
		bt_del=new JButton("삭제하기");
		
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
				int col=0; //seq는 첫번째 컬럼이니까
				
				seq=(String)t.getValueAt(row, col);
				
				
			}	
		});
	
		
		//윈도우와 리스너 연결
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//데이터베이스 자원 해제
				manager.disConnect(con);
				
				//프로세스 종료
				System.exit(0);
				
			}
		});
			
		
		
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
	
	}
	
	public void init(){
		//Connection 얻어다 놓기
		con=manager.getConnection();
		
	}
	
	//파일 탐색기 띄우기
	public void open(){
		int result=chooser.showOpenDialog(this);
		
		//열기를 누르면..목적 파일에 스트림을 생성하자
		if(result==JFileChooser.APPROVE_OPTION){
			//유저가 선택한 파일
			File file=chooser.getSelectedFile();
			
			String ext=FileUtil.getExt(file.getName());
			
			//동물...병원.csv 일 경우 split 사용 주의
			//0123456 7
			//확장자의 시작 값은 i+1 			
			if(!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "CSV 만 선택하세요");
				return; //더이상의 진행을 막는다. 아래 라인을 수행하면 안됨!
			}
		
			t_path.setText(file.getAbsolutePath());
			
			try {
				reader=new FileReader(file); //작은 빨대
				buffr=new BufferedReader(reader); //큰 빨대. 작은빨대 빨아들임
				
				String data;
				//일단 빨대만 꽃아놓은 상태, 문서는 열려있음.-> load를 누르면 빨아들임
			}
			
			 catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	//csv-->oracle로 데이터 이전(migration)하기
	public void load(){
		//버퍼스트림을 이용하여 csv의 데이터를 1줄씩 읽어들여 insert시키자!!
		//레코드 없을 때까지..
		//while문으로 돌리면 너무 빠르므로,
		//네트워크가 감당할 수 없기 때문에 일부러 지연시키면서..
		String data;
		StringBuffer sb=new StringBuffer(); //String과 StringBuffer의 차이->메모리 절감
		PreparedStatement pstmt=null;
		
		try {
			while(true){
				data=buffr.readLine();
				
				if(data==null)break;
				
				String[] value=data.split(","); //.처럼 프로그램의 기능성 문자아니므로 따로 처리해줄 필요없음.
				
				
				//seq줄을 제외하고 insert 하겠다!!
				if(!value[0].equals("seq")){				
					sb.append("insert into hospital(seq, name, addr, regdate, status, dimension, type)");
					sb.append(" values("+value[0]+", '"+value[1]+"', '"+value[2]+"', '"+value[3]+"', '"+value[4]+"', "+value[5]+", '"+value[6]+"')");
					
					//System.out.println(sb.toString());
					pstmt=con.prepareStatement(sb.toString());
					
					int result=pstmt.executeUpdate(); //쿼리 수행
				
					//insert문 끝나면 append 취소
					//기존에 누적된 StringBuffer의 데이터를 모두 지우기
					//쿼리 수행 후 지웠어야 했는데 미리 지워서 nullException이 발생
					sb.delete(0, sb.length()); 
				}else{
					System.out.println("난 1줄이므로 제외");
				}
			}
			
			JOptionPane.showMessageDialog(this, "마이그레이션완료");
			
			//JTable 나오게 처리!
			getList();
			table.setModel(myModel=new MyModel(list, columnName));
			
			//테이블 모델과 리스너와의 연결
			table.getModel().addTableModelListener(this); //현재 사용중인 모델을 알 수 있는 메서드 //setModel이 먼저, 시점 유의
			
			table.updateUI(); //모델 갱신, 최신 데이터 반영
			
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
	
	//엑셀 파일 읽어서 db에 마이그레이션 하기!!
	//java SE 엑셀제어 라이브러리 있다? X
	//open Source 공개 소프트웨어
	//copyright <----> copyleft (apache 단체)
	//POI 라이브러리! http://apache.org
	/*
	 * 엑셀-하나의 시트는 여러 개의 row로 이루어짐. row는 여러개의 컬럼으로 이루어짐
	여러개의 시트가 모여서 하나의 엑셀파일이 됨, poi에서 worksheet 하나의 엑셀파일을 의미

	 * HSSFWorkbook :엑셀 파일
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
				
				//스트림을 poi로 가공한다
				HSSFWorkbook book=null;
				book=new HSSFWorkbook(fis);
				
				HSSFSheet  sheet=null;
				sheet=book.getSheet("sheet1");
				
				int total=sheet.getLastRowNum();
				
				/*----------------------------------------
				 첫번째 row는 데이터가 아닌 컬럼 정보이므로, 
				 이 정보들을 추출하여 insert into table(****)
				 * ----------------------------------------*/
			
				System.out.println("이 파일의 첫번째 row 번호는 "+sheet.getFirstRowNum());
				
				HSSFRow firstRow=sheet.getRow(sheet.getFirstRowNum());  //첫번째 row 컬럼 정보
				
				//Row를 얻었으니, 컬럼을 분석한다
			
				//마지막 컬럼 다음엔 콤마 없도록 처리
			
				 for(int i=0; i< firstRow.getLastCellNum(); i++){
					 HSSFCell cell=firstRow.getCell(i);
						data.delete(0, cols.length()); //StringBuffer 비우기
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
				
				//컬럼 제목 빼고 출력
				for(int a=1; a<=total; a++){
					HSSFRow row=sheet.getRow(a);
					int columnCount=row.getLastCellNum();
					
					//Row를 얻고, 컬럼 분석
					data.delete(0, data.length()); //StringBuffer 비우기
					for(int i=0; i<columnCount;  i++){
						HSSFCell cell=row.getCell(i);
						
						//자료형에 국한되지 않고  모두  String 처리 
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
					//줄이 바뀔때마다 줄바꿈 ->줄(row)만큼 쿼리문 날림
					//System.out.println("insert into hospital("+cols.toString()+") values("+data+")");
					insertSql.append("insert into hospital("+cols.toString()+") values("+data+");");
					
				} 
				
				//모든게 끝났으니, 편안하게 쓰레드에 일 시키자!!
				// Runnable 인터페이스를 인수로 넣으면 Thread의 run을 수행하는 것이 아니라 
				//Runnable 인터페이스를 구현한자의 run()을 수행하게 됨. 따라서 우리꺼 수행
				thread=new Thread(this); 
				thread.start(); 
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
	}

	//모든 레코드 가져오기!!
	public void getList(){
		String sql="select * from hospital order by seq asc";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			
			//컬럼명도 추출!!
			ResultSetMetaData meta=rs.getMetaData();  //rs가 죽기 전에 메타데이터에 자기 정보 넘기도록
			int count=meta.getColumnCount();
			columnName=new Vector();
			
			for(int i=0; i<count; i++){
				columnName.add(meta.getColumnName(i+1));
			}
			
			list=new Vector<Vector>(); //이차원 벡터
		
			//rs를 이차원 벡터로 가공하고 버리자!!
			while(rs.next()){
				Vector vec=new Vector(); //레코드 1건 담을 거임
				
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));
				vec.add(rs.getString("addr"));
				vec.add(rs.getString("regdate"));
				vec.add(rs.getString("status"));
				vec.add(rs.getString("dimension"));
				vec.add(rs.getString("type"));
				
				list.add(vec); //1차원 벡터를 list 벡터에 담는다->2차원 벡터
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
	
	
	//선택한 레코드 삭제
	public void delete(){
		int ans=JOptionPane.showConfirmDialog(LoadMain.this, seq+"삭제할래요?");
		if(ans==JOptionPane.OK_OPTION){
			String sql="delete from hospital where seq="+seq;
			PreparedStatement pstmt=null;
			
			try {
				pstmt=con.prepareStatement(sql);
				int result=pstmt.executeUpdate();
				if(result!=0){
					JOptionPane.showMessageDialog(this,"삭제완료");
					table.updateUI();; //삭제 후 테이블 갱신
					
					getList();
					//방금 완성된 list를 다시 MyModel에 대입!! ->왜냐면 list가 기존의 리스트가 아닌 로드메인에서 만든 리스트니까 
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

	
	// 테이블 모델의 데이터 값에 변경이 발생하면, 
	//그 찰나를 감지하는 리스너!!
	public void tableChanged(TableModelEvent e) {
		int row=table.getSelectedRow();
		int col=table.getSelectedColumn();
		//System.out.println(row);
		
		String column=columnName.elementAt(col); //지금 선택한 컬럼
		
		String value=(String)table.getValueAt(row, col); //지정한 좌표의 값 반환
		
		String seq=(String)table.getValueAt(row,0);
		String sql="update hospital set "+column+"='"+value+"' ";
		sql+=" where seq="+seq;
		System.out.println(sql);
		//System.out.println("당신이 편집한 데이터의 위치는 "+row+","+col);
		
		PreparedStatement pstmt=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			int result=pstmt.executeUpdate();
			if(result!=0){
				JOptionPane.showMessageDialog(this, "수정완료");
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
		//insertSql에 insert문이 몇개인지 알아보자 
		String[] str=insertSql.toString().split(";"); 
		System.out.println("insert문 수는 "+str.length);
		
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
		//기존에 사용했던 StringBuffer 비윅
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
