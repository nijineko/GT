/*
 * XPropertyTable.java
 * 
 * @created 2006
 * 
 * Copyright (C) 1999-2011 Eric Maziade
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.maziade.props.ui;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.maziade.props.XProperties;
import com.maziade.props.XProperties.XPropertyInfo;

/**
 * 
 * @author Eric Maziade
 * 
 * Simulate a JTable and manage rows to allow user to edit a selection of properties.
 */
class XPropertyTable extends JTable implements TableCellRenderer
{
	/**
	 * Serial version UID 
	 */
	private static final long	serialVersionUID	= 1715570335296424890L;
	
	private TableCellEditor m_cellEditor;
	private final XPropertiesDialog m_dialog;
	private List<XPropertyTableRow>		m_propertiesRows;
	private List<XPropertyTableRow>		m_errorRows;

	/**
	 * Constructor
	 */
	public XPropertyTable(XPropertiesDialog dialog)
	{
		super();
		m_dialog = dialog;
		initComponents();
	}

	/**
	 * Initialize every components on the panel
	 */
	private void initComponents()
	{
		m_propertiesRows = new ArrayList<XPropertyTableRow>();
		m_errorRows = new ArrayList<XPropertyTableRow>();
		setModel(new TableModel());
		
		setDefaultRenderer(JLabel.class, this);
		setDefaultRenderer(Component.class, this);
		
		m_cellEditor = new PropertyCellEditor();

		setDefaultEditor(Component.class, m_cellEditor);
		
		JLabel renderer = (JLabel)getTableHeader().getDefaultRenderer();
		getTableHeader().setFont(getTableHeader().getFont().deriveFont(Font.BOLD));
		renderer.setHorizontalAlignment(SwingConstants.LEADING);
		
		TableColumnModel model = getColumnModel();
		TableColumn col = model.getColumn(0);
		col.setHeaderValue(XProperties.getResourceString("XPropertiesDialog.PROPERTY_COLUMN"));		
		
		col = model.getColumn(1);
		col.setHeaderValue(XProperties.getResourceString("XPropertiesDialog.VALUE_COLUMN"));
	}

	/**
	 * Add a property row
	 * 
	 * @param property property to add as row
	 */
	public void addRow(XPropertyInfo property)
	{
		XPropertyTableRow newRow = new XPropertyTableRow(this, property);
		newRow.setIndex(m_propertiesRows.size());
		m_propertiesRows.add(newRow);
		
		validate();
	}
	
	@Override
	public void doLayout()
	{
		int idx = 0;
		for (XPropertyTableRow row : m_propertiesRows)
		{
			setRowHeight(idx, row.getPreferredSize().height);
			idx++;
		}

		super.doLayout();
	}	
	
	/**
	 * Add an error row
	 * OK button is automatically disabled when errors are present.
	 * 
	 * @param r
	 */
	public void addError(XPropertyTableRow r)
	{
		// TODO how does that work?
		m_errorRows.add(r);
		m_dialog.enableOkButton(false);
	}

	/**
	 * Remove an error row
	 * OK button is automatically re-enabled when errors are no longer present
	 * 
	 * @param r
	 */
	public void removeError(XPropertyTableRow r)
	{
		// TODO how does that work?
		m_errorRows.remove(r);
		if (m_errorRows.isEmpty())
			m_dialog.enableOkButton(true);
	}

	/**
	 * A row has been clicked 
	 * @param r Row that has been clicked
	 */
	public void onRowClicked(XPropertyTableRow r)
	{
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		XPropertyTableRow tableRow = m_propertiesRows.get(row);
		if (column == 0)
			return tableRow.getPropertyLabel();
		
		return tableRow;
	}
	
	/**
	 * @author Eric Maziade
	 * DataModel for the property table
	 *
	 */
	private class TableModel extends AbstractTableModel {
		/**
		 * UID 
		 */
		private static final long	serialVersionUID	= 7887028069764466345L;

		@Override
		public int getColumnCount()
		{
			return 2;
		}
		
		@Override
		public int getRowCount()
		{
			return m_propertiesRows.size();
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return (columnIndex == 1);
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			if (columnIndex == 0)
				return JLabel.class;
			
			return Component.class;
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			XPropertyTableRow row = m_propertiesRows.get(rowIndex);
			if (columnIndex == 0)
				return row.getPropertyLabel();
			
			return row.getPropertyEditor();
		}	
	}
	
	private class PropertyCellEditor implements TableCellEditor
	{
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			m_value = value;
			m_tableRow = m_propertiesRows.get(row);
			return m_tableRow;
		}

		@Override
		public void addCellEditorListener(CellEditorListener l)
		{
			m_listeners.add(l);
		}

		@Override
		public void cancelCellEditing()
		{
			m_tableRow.getPropertyEditor().commit();
		}

		@Override
		public Object getCellEditorValue()
		{
			return m_value;
		}

		@Override
		public boolean isCellEditable(EventObject anEvent)
		{
			return true;
		}

		@Override
		public void removeCellEditorListener(CellEditorListener l)
		{
			m_listeners.remove(l);
		}

		@Override
		public boolean shouldSelectCell(EventObject anEvent)
		{
			return true;
		}

		@Override
		public boolean stopCellEditing()
		{
			return true;
		}

		private Object m_value;
		private XPropertyTableRow m_tableRow;
		private List<CellEditorListener> m_listeners = new ArrayList<CellEditorListener>();
	}
}