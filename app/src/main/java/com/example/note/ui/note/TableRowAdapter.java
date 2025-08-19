package com.example.note.ui.note;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.data.entity.Cell;
import com.example.note.data.entity.Column;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表格行适配器 - 管理表格的每一行
 */
public class TableRowAdapter extends RecyclerView.Adapter<TableRowAdapter.TableRowViewHolder> {
    
    private int rowCount = 0;
    private List<Column> columns = new ArrayList<>();
    private Map<String, Cell> cellsMap; // key: "rowIndex_columnIndex"
    private int rowHeightDp = 44;
    private DataCellAdapter.OnCellChangeListener cellChangeListener;
    
    public void setData(int rowCount, List<Column> columns, Map<String, Cell> cellsMap) {
        int newRowCount = Math.max(0, rowCount);
        List<Column> newColumns = columns != null ? new ArrayList<>(columns) : new ArrayList<>();
        
        int oldRowCount = this.rowCount;
        boolean columnsChanged = !newColumns.equals(this.columns);
        
        this.columns = newColumns;
        this.cellsMap = cellsMap;
        
        if (oldRowCount == 0 || columnsChanged) {
            // 首次设置或列结构发生变化，使用完整刷新
            this.rowCount = newRowCount;
            notifyDataSetChanged();
        } else if (newRowCount > oldRowCount) {
            // 只是添加了新行，使用精确通知
            this.rowCount = newRowCount;
            notifyItemRangeInserted(oldRowCount, newRowCount - oldRowCount);
        } else if (newRowCount < oldRowCount) {
            // 删除了行，使用精确通知
            this.rowCount = newRowCount;
            notifyItemRangeRemoved(newRowCount, oldRowCount - newRowCount);
        } else {
            // 行数相同，只是数据更新，通知所有项目刷新
            this.rowCount = newRowCount;
            notifyItemRangeChanged(0, newRowCount);
        }
    }
    
    public void setRowHeight(int rowHeightDp) {
        this.rowHeightDp = rowHeightDp;
        notifyDataSetChanged();
    }
    
    public void setCellChangeListener(DataCellAdapter.OnCellChangeListener listener) {
        this.cellChangeListener = listener;
    }
    
    public void updateColumnWidth(int columnIndex, float widthDp) {
        if (columnIndex >= 0 && columnIndex < columns.size()) {
            columns.get(columnIndex).setWidth(widthDp);
            // 通知所有行更新该列的宽度
            for (int i = 0; i < getItemCount(); i++) {
                notifyItemChanged(i);
            }
        }
    }
    
    public void updateRowHeight(int rowIndex, int heightDp) {
        if (rowIndex >= 0 && rowIndex < rowCount) {
            this.rowHeightDp = heightDp;
            notifyItemChanged(rowIndex);
        }
    }
    
    @NonNull
    @Override
    public TableRowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table_row, parent, false);
        return new TableRowViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TableRowViewHolder holder, int position) {
        holder.bind(position);
    }
    
    @Override
    public int getItemCount() {
        return rowCount;
    }
    
    class TableRowViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView rowCellsRecycler;
        private DataCellAdapter dataCellAdapter;
        
        public TableRowViewHolder(@NonNull View itemView) {
            super(itemView);
            rowCellsRecycler = itemView.findViewById(R.id.row_cells_recycler);
            
            // 设置水平布局管理器，禁用滑动
            LinearLayoutManager layoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false) {
                @Override
                public boolean canScrollHorizontally() {
                    return false; // 禁用水平滑动
                }
            };
            rowCellsRecycler.setLayoutManager(layoutManager);
            
            // 禁用RecyclerView的触摸事件
            rowCellsRecycler.setNestedScrollingEnabled(false);
            rowCellsRecycler.setHasFixedSize(true);
            
            // 创建数据单元格适配器
            dataCellAdapter = new DataCellAdapter();
            dataCellAdapter.setOnCellChangeListener(cellChangeListener);
            rowCellsRecycler.setAdapter(dataCellAdapter);
        }
        
        public void bind(int rowIndex) {
            // 获取该行的所有单元格数据
            List<Cell> rowCells = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                String cellKey = rowIndex + "_" + columnIndex;
                Cell cell = cellsMap != null ? cellsMap.get(cellKey) : null;
                
                if (cell == null) {
                    // 创建空单元格
                    cell = new Cell();
                    cell.setRowIndex(rowIndex);
                    cell.setColIndex(columnIndex);
                    cell.setContent("");
                }
                rowCells.add(cell);
            }
            
            // 设置行高度
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.height = (int) (rowHeightDp * itemView.getContext().getResources().getDisplayMetrics().density);
            itemView.setLayoutParams(layoutParams);
            
            // 更新适配器数据
            dataCellAdapter.setRowData(rowIndex, rowCells, columns);
            dataCellAdapter.setRowHeight(rowHeightDp);
        }
    }
}