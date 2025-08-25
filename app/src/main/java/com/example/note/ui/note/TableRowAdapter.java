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
 * 使用ColumnWidthProvider提供基于缩放的动态尺寸
 */
public class TableRowAdapter extends RecyclerView.Adapter<TableRowAdapter.TableRowViewHolder> {
    
    private int rowCount = 0;
    private List<Column> columns = new ArrayList<>();
    private Map<String, Cell> cellsMap; // key: "rowIndex_columnIndex"
    private int rowHeightDp = 44;
    private DataCellAdapter.OnCellChangeListener cellChangeListener;
    private ColumnWidthProvider widthProvider;
    private int globalHorizontalOffset = 0;
    
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
    
    public void setColumnWidthProvider(ColumnWidthProvider widthProvider) {
        this.widthProvider = widthProvider;
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
    
    /**
     * 实时更新可见ViewHolder的列宽，不触发重绑定
     */
    public void updateColumnWidthRealtime(RecyclerView recyclerView, int columnIndex, float widthDp) {
        if (columnIndex >= 0 && columnIndex < columns.size()) {
            // 更新列数据
            columns.get(columnIndex).setWidth(widthDp);
            
            // 直接更新所有可见ViewHolder的该列宽度
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (holder instanceof TableRowViewHolder) {
                    TableRowViewHolder rowHolder = (TableRowViewHolder) holder;
                    updateRowCellWidth(rowHolder, columnIndex);
                }
            }
        }
    }
    
    /**
     * 实时更新可见ViewHolder的列宽（像素版本），直接使用像素值，避免Provider查询
     */
    public void updateColumnWidthRealtimePx(RecyclerView recyclerView, int columnIndex, int newWidthPx) {
        if (columnIndex >= 0 && columnIndex < columns.size()) {
            // 直接更新所有可见ViewHolder的该列宽度
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                if (holder instanceof TableRowViewHolder) {
                    TableRowViewHolder rowHolder = (TableRowViewHolder) holder;
                    updateRowCellWidthPx(rowHolder, columnIndex, newWidthPx);
                }
            }
        }
    }
    
    /**
     * 更新单行中指定列的宽度
     */
    private void updateRowCellWidth(TableRowViewHolder rowHolder, int columnIndex) {
        RecyclerView rowCellsRecycler = rowHolder.getRowCellsRecycler();
        if (rowCellsRecycler != null) {
            RecyclerView.ViewHolder cellHolder = rowCellsRecycler.findViewHolderForAdapterPosition(columnIndex);
            if (cellHolder != null) {
                View cellView = cellHolder.itemView;
                ViewGroup.LayoutParams layoutParams = cellView.getLayoutParams();
                if (widthProvider != null) {
                    layoutParams.width = widthProvider.getColumnWidthPx(columnIndex);
                    cellView.setLayoutParams(layoutParams);
                    cellView.requestLayout();
                }
            }
        }
    }
    
    /**
     * 更新单行中指定列的宽度（像素版本），直接使用像素值
     */
    private void updateRowCellWidthPx(TableRowViewHolder rowHolder, int columnIndex, int newWidthPx) {
        RecyclerView rowCellsRecycler = rowHolder.getRowCellsRecycler();
        if (rowCellsRecycler != null) {
            RecyclerView.ViewHolder cellHolder = rowCellsRecycler.findViewHolderForAdapterPosition(columnIndex);
            if (cellHolder != null) {
                View cellView = cellHolder.itemView;
                ViewGroup.LayoutParams layoutParams = cellView.getLayoutParams();
                if (layoutParams.width != newWidthPx) {
                    layoutParams.width = newWidthPx;
                    cellView.setLayoutParams(layoutParams);
                    cellView.requestLayout();
                }
            }
        }
    }
    
    public void updateRowHeight(int rowIndex, int heightDp) {
        if (rowIndex >= 0 && rowIndex < rowCount) {
            // 不再设置全局rowHeightDp，而是通知特定行刷新
            // 行高现在由ColumnWidthProvider管理
            notifyItemChanged(rowIndex);
        }
    }
    
    /**
     * 获取指定位置的ViewHolder
     */
    public TableRowViewHolder getViewHolderAt(RecyclerView recyclerView, int position) {
        return (TableRowViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
    }
    
    /**
     * 对所有可见行应用横向滚动
     */
    public void scrollAllRowsBy(RecyclerView recyclerView, int dx) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof TableRowViewHolder) {
                ((TableRowViewHolder) holder).scrollCellsBy(dx);
            }
        }
    }
    
    /**
     * 对所有可见行应用全局横向偏移
     */
    public void applyGlobalOffsetToAllRows(RecyclerView recyclerView, int offsetX) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof TableRowViewHolder) {
                ((TableRowViewHolder) holder).applyGlobalOffset(offsetX);
            }
        }
    }

    /**
     * 对所有可见行应用绝对横向偏移（使用scrollToPositionWithOffset）
     */
    public void applyAbsoluteOffsetToAllRows(RecyclerView recyclerView, int offsetX) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof TableRowViewHolder) {
                ((TableRowViewHolder) holder).applyAbsoluteOffset(offsetX);
            }
        }
    }
    
    /**
     * 对所有可见行应用绝对横向偏移（使用列索引和偏移量）
     */
    public void applyAbsoluteOffsetToAllRowsWithColumnIndex(RecyclerView recyclerView, int firstVisibleColIndex, int offsetInFirstCol) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof TableRowViewHolder) {
                ((TableRowViewHolder) holder).applyAbsoluteOffsetWithColumnIndex(firstVisibleColIndex, offsetInFirstCol);
            }
        }
    }
    
    public void setGlobalHorizontalOffset(int offset) {
        this.globalHorizontalOffset = offset;
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

    @Override
    public void onViewAttachedToWindow(@NonNull TableRowViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (widthProvider != null) {
            int[] positionAndOffset = widthProvider.mapScrollXToPositionAndOffset(globalHorizontalOffset);
            holder.applyAbsoluteOffsetWithColumnIndex(positionAndOffset[0], positionAndOffset[1]);
        } else {
            holder.applyAbsoluteOffset(globalHorizontalOffset);
        }
    }
    
    class TableRowViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView rowCellsRecycler;
        private DataCellAdapter dataCellAdapter;
        
        public TableRowViewHolder(@NonNull View itemView) {
            super(itemView);
            rowCellsRecycler = itemView.findViewById(R.id.row_cells_recycler);
            
            // 设置水平布局管理器，禁用手势滚动但允许程序滚动
            LinearLayoutManager layoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false) {
                @Override
                public boolean canScrollHorizontally() {
                    return false; // 禁止手势滚动，但保留程序滚动能力
                }
            };
            rowCellsRecycler.setLayoutManager(layoutManager);
            
            // 删除addOnItemTouchListener横向拦截逻辑，让父级ZoomableRecyclerHost处理横向手势
            
            // 禁用RecyclerView的触摸事件
            rowCellsRecycler.setNestedScrollingEnabled(false);
            rowCellsRecycler.setHasFixedSize(true);
            
            // 禁用ItemAnimator和过度滚动以防止动画干扰
            rowCellsRecycler.setItemAnimator(null);
            rowCellsRecycler.setOverScrollMode(View.OVER_SCROLL_NEVER);
            
            // 禁用事件拆分，防止多指触控同时作用于多个子视图
            rowCellsRecycler.setMotionEventSplittingEnabled(false);
            
            // 创建数据单元格适配器
            dataCellAdapter = new DataCellAdapter();
            dataCellAdapter.setOnCellChangeListener(cellChangeListener);
            rowCellsRecycler.setAdapter(dataCellAdapter);
        }
        
        /**
         * 横向滚动单元格
         */
        public void scrollCellsBy(int dx) {
            rowCellsRecycler.scrollBy(dx, 0);
        }
        
        /**
         * 应用全局横向偏移
         */
        public void applyGlobalOffset(int offsetX) {
            int currentOffset = rowCellsRecycler.computeHorizontalScrollOffset();
            int deltaX = offsetX - currentOffset;
            rowCellsRecycler.scrollBy(deltaX, 0);
        }

        /**
         * 应用绝对横向偏移（使用scrollToPositionWithOffset）
         */
        public void applyAbsoluteOffset(int offsetX) {
            RecyclerView.LayoutManager lm = rowCellsRecycler.getLayoutManager();
            if (lm instanceof LinearLayoutManager) {
                ((LinearLayoutManager) lm).scrollToPositionWithOffset(0, -offsetX);
            }
        }
        
        /**
         * 应用绝对横向偏移（使用列索引和偏移量）
         */
        public void applyAbsoluteOffsetWithColumnIndex(int firstVisibleColIndex, int offsetInFirstCol) {
            RecyclerView.LayoutManager lm = rowCellsRecycler.getLayoutManager();
            if (lm instanceof LinearLayoutManager) {
                ((LinearLayoutManager) lm).scrollToPositionWithOffset(firstVisibleColIndex, -offsetInFirstCol);
            }
        }
        
        /**
         * 获取行单元格RecyclerView
         */
        public RecyclerView getRowCellsRecycler() {
            return rowCellsRecycler;
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
            
            // 使用ColumnWidthProvider设置行高度
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            if (widthProvider != null) {
                // 使用行索引获取该行的具体高度
                layoutParams.height = widthProvider.getRowHeightPx(rowIndex);
            } else {
                // 回退到原始方式
                layoutParams.height = (int) (rowHeightDp * itemView.getContext().getResources().getDisplayMetrics().density);
            }
            itemView.setLayoutParams(layoutParams);
            
            // 更新适配器数据
            dataCellAdapter.setRowData(rowIndex, rowCells, columns);
            dataCellAdapter.setRowHeight(rowHeightDp);
            
            // 传递ColumnWidthProvider给DataCellAdapter
            if (widthProvider != null) {
                dataCellAdapter.setColumnWidthProvider(widthProvider);
            }
            
            // 应用绝对横向偏移
            if (widthProvider != null) {
                int[] positionAndOffset = widthProvider.mapScrollXToPositionAndOffset(globalHorizontalOffset);
                applyAbsoluteOffsetWithColumnIndex(positionAndOffset[0], positionAndOffset[1]);
            } else {
                applyAbsoluteOffset(globalHorizontalOffset);
            }
        }
    }
}