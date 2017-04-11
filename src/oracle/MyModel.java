/*
 * JTable 이 수시로 정보를 얻어가는 컨트롤러
 * */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	Vector columnName; //컬럼의 제목을 담을 벡터
	Vector<Vector> list; //레코드를 담을 이차원 벡터
	
	public MyModel(Vector list, Vector columnName) {
		this.list=list;
		this.columnName=columnName;
	}


	public int getColumnCount() {
		return columnName.size();
	}


	public String getColumnName(int col) {
		
		return (String)columnName.elementAt(col);
	}

	public int getRowCount() {
		return list.size();
	}

	//tableModel을 쓰면 cell 편집이 가능하게 하는 메서드 오버라이드 해야함
	//row, col에 위치한 셀을 편집가능하게 한다
	public boolean isCellEditable(int row, int col) {
		//seq는 함부로 변경하지 못하게 
		boolean flag=false;
		if(col==0){
			flag=false;
		}else{
			flag=true;
		}
		
		return flag;
	}
	
	//각셀의 변경값을 반영하는 메서드 오버라이드
	
	public void setValueAt(Object value, int row, int col) {
		//층, 호수를 변경한다!!
		Vector vec=list.get(row); 
		vec.set(col, value);
		this.fireTableCellUpdated(row, col); //호출해야지만 변경됬는지 인식
	
	
	}
	
	
	public Object getValueAt(int row, int col) {
		//이차원 벡터 
		Vector vec=list.get(row);
		return vec.elementAt(col);
	}

}
