/*
 * JTable �� ���÷� ������ ���� ��Ʈ�ѷ�
 * */
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel{
	Vector columnName; //�÷��� ������ ���� ����
	Vector<Vector> list; //���ڵ带 ���� ������ ����
	
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

	//tableModel�� ���� cell ������ �����ϰ� �ϴ� �޼��� �������̵� �ؾ���
	//row, col�� ��ġ�� ���� ���������ϰ� �Ѵ�
	public boolean isCellEditable(int row, int col) {
		//seq�� �Ժη� �������� ���ϰ� 
		boolean flag=false;
		if(col==0){
			flag=false;
		}else{
			flag=true;
		}
		
		return flag;
	}
	
	//������ ���氪�� �ݿ��ϴ� �޼��� �������̵�
	
	public void setValueAt(Object value, int row, int col) {
		//��, ȣ���� �����Ѵ�!!
		Vector vec=list.get(row); 
		vec.set(col, value);
		this.fireTableCellUpdated(row, col); //ȣ���ؾ����� �������� �ν�
	
	
	}
	
	
	public Object getValueAt(int row, int col) {
		//������ ���� 
		Vector vec=list.get(row);
		return vec.elementAt(col);
	}

}