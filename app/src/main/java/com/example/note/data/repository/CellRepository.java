package com.example.note.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.note.data.dao.CellDao;
import com.example.note.data.database.AppDatabase;
import com.example.note.data.entity.Cell;
import com.example.note.util.DateUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 单元格仓库类
 * 提供单元格数据的统一访问接口
 */
public class CellRepository {
    
    private static final String TAG = "CellRepository";
    private static volatile CellRepository INSTANCE;
    
    private final CellDao cellDao;
    private final Executor executor;
    
    private CellRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        cellDao = database.cellDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 获取Repository实例（单例模式）
     */
    public static CellRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CellRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CellRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 根据笔记本ID获取所有单元格
     */
    public LiveData<List<Cell>> getCellsByNotebookId(long notebookId) {
        return cellDao.getCellsByNotebookId(notebookId);
    }
    
    /**
     * 根据位置获取单元格
     */
    public LiveData<Cell> getCellByPosition(long notebookId, int row, int col) {
        return cellDao.getCellByPosition(notebookId, row, col);
    }
    
    /**
     * 获取指定行的所有单元格
     */
    public LiveData<List<Cell>> getCellsByRow(long notebookId, int row) {
        return cellDao.getCellsByRow(notebookId, row);
    }
    
    /**
     * 获取指定列的所有单元格
     */
    public LiveData<List<Cell>> getCellsByColumn(long notebookId, int col) {
        return cellDao.getCellsByColumn(notebookId, col);
    }
    
    /**
     * 获取指定区域的单元格
     */
    public LiveData<List<Cell>> getCellsByRange(long notebookId, int startRow, int endRow, int startCol, int endCol) {
        return cellDao.getCellsByRange(notebookId, startRow, endRow, startCol, endCol);
    }
    
    /**
     * 搜索单元格内容
     */
    public LiveData<List<Cell>> searchCells(long notebookId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new MutableLiveData<>();
        }
        return cellDao.searchCells(notebookId, query.trim());
    }
    
    /**
     * 获取包含图片的单元格
     */
    public LiveData<List<Cell>> getCellsWithImages(long notebookId) {
        return cellDao.getCellsWithImages(notebookId);
    }
    
    /**
     * 获取非空单元格
     */
    public LiveData<List<Cell>> getNonEmptyCells(long notebookId) {
        return cellDao.getNonEmptyCells(notebookId);
    }
    
    /**
     * 根据列筛选和排序获取单元格
     */
    public LiveData<List<Cell>> getCellsByColumnWithFilter(long notebookId, int colIndex, 
                                                          String filterType, String filterValue, 
                                                          double minValue, double maxValue, String sortOrder) {
        return cellDao.getCellsByColumnWithFilter(notebookId, colIndex, filterType, filterValue, minValue, maxValue, sortOrder);
    }
    
    /**
     * 获取指定列的所有单元格并排序
     */
    public LiveData<List<Cell>> getCellsByColumnSorted(long notebookId, int colIndex, String sortOrder) {
        return cellDao.getCellsByColumnSorted(notebookId, colIndex, sortOrder);
    }
    
    /**
     * 获取所有单元格并按指定列排序
     */
    public LiveData<List<Cell>> getAllCellsSortedByColumn(long notebookId, int sortColIndex, String sortOrder) {
        return cellDao.getAllCellsSortedByColumn(notebookId, sortColIndex, sortOrder);
    }
    
    /**
     * 获取最大行索引
     */
    public void getMaxRowIndex(long notebookId, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                Integer maxRow = cellDao.getMaxRowIndex(notebookId);
                if (callback != null) {
                    callback.onSuccess(maxRow != null ? maxRow : -1);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get max row index", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取最大列索引
     */
    public void getMaxColumnIndex(long notebookId, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                Integer maxCol = cellDao.getMaxColumnIndex(notebookId);
                if (callback != null) {
                    callback.onSuccess(maxCol != null ? maxCol : -1);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get max column index", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取单元格总数
     */
    public LiveData<Integer> getCellCount(long notebookId) {
        return cellDao.getCellCount(notebookId);
    }
    
    /**
     * 获取非空单元格总数
     */
    public LiveData<Integer> getNonEmptyCellCount(long notebookId) {
        return cellDao.getNonEmptyCellCount(notebookId);
    }
    
    /**
     * 创建或更新单元格
     */
    public void saveCell(Cell cell, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            try {
                cell.touch();
                long id = cellDao.insert(cell);
                
                if (callback != null) {
                    callback.onSuccess(id);
                }
                
                Log.d(TAG, "Cell saved: " + cell.getNotebookId() + ", (" + cell.getRowIndex() + ", " + cell.getColIndex() + ")");
            } catch (Exception e) {
                Log.e(TAG, "Failed to save cell", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 批量保存单元格
     */
    public void saveCells(List<Cell> cells, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                long now = DateUtils.now();
                for (Cell cell : cells) {
                    cell.setUpdatedAt(now);
                }
                
                cellDao.insertAll(cells);
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Batch saved " + cells.size() + " cells");
            } catch (Exception e) {
                Log.e(TAG, "Failed to batch save cells", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新单元格内容
     */
    public void updateCellContent(long notebookId, int row, int col, String content, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Cell cell = cellDao.getCellByPositionSync(notebookId, row, col);
                int result = 0;
                if (cell != null) {
                    result = cellDao.updateContent(cell.getId(), content, DateUtils.now());
                }
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Cell content updated: (" + row + ", " + col + ")");
                } else {
                    // 如果更新失败，可能是单元格不存在，创建新单元格
                    Cell newCell = new Cell(notebookId, row, col);
                    newCell.setContent(content);
                    saveCell(newCell, new RepositoryCallback<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                callback.onError(error);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update cell content", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新单元格图片
     */
    public void updateCellImage(long notebookId, int row, int col, String imageId, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Cell cell = cellDao.getCellByPositionSync(notebookId, row, col);
                int result = 0;
                if (cell != null) {
                    result = cellDao.updateImage(cell.getId(), imageId, DateUtils.now());
                }
                
                if (result > 0) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    Log.d(TAG, "Cell image updated: (" + row + ", " + col + ")");
                } else {
                    // 如果更新失败，可能是单元格不存在，创建新单元格
                    Cell newCell = new Cell(notebookId, row, col);
                    newCell.setImageId(imageId);
                    saveCell(newCell, new RepositoryCallback<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                callback.onError(error);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update cell image", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新单元格格式
     */
    public void updateCellFormat(long notebookId, int row, int col, String textColor, String backgroundColor, 
                                boolean isBold, boolean isItalic, float textSize, String textAlignment, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Cell cell = cellDao.getCellByPositionSync(notebookId, row, col);
                if (cell != null) {
                    int result = cellDao.updateFormat(cell.getId(), textColor, backgroundColor, 
                                                    isBold, isItalic, textSize, textAlignment, DateUtils.now());
                    
                    if (result > 0) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                        Log.d(TAG, "Cell format updated: (" + row + ", " + col + ")");
                    } else {
                        if (callback != null) {
                            callback.onError(new Exception("Failed to update cell format"));
                        }
                    }
                } else {
                    // 如果更新失败，可能是单元格不存在，创建新单元格
                    Cell newCell = new Cell(notebookId, row, col);
                    newCell.setTextColor(textColor);
                    newCell.setBackgroundColor(backgroundColor);
                    newCell.setBold(isBold);
                    newCell.setItalic(isItalic);
                    newCell.setTextSize(textSize);
                    newCell.setTextAlignment(textAlignment);
                    saveCell(newCell, new RepositoryCallback<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                callback.onError(error);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update cell format", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 清空单元格
     */
    public void clearCell(long notebookId, int row, int col, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Cell cell = cellDao.getCellByPositionSync(notebookId, row, col);
                if (cell != null) {
                    int result = cellDao.clearCell(cell.getId(), DateUtils.now());
                    
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    
                    Log.d(TAG, "Cell cleared: (" + row + ", " + col + ")");
                } else {
                    if (callback != null) {
                        callback.onSuccess(null); // 单元格不存在，视为已清空
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear cell", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除单元格
     */
    public void deleteCell(long notebookId, int row, int col, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Cell cell = cellDao.getCellByPositionSync(notebookId, row, col);
                if (cell != null) {
                    int result = cellDao.delete(cell);
                    
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                    
                    Log.d(TAG, "Cell deleted: (" + row + ", " + col + ")");
                } else {
                    if (callback != null) {
                        callback.onSuccess(null); // 单元格不存在，视为已删除
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete cell", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除指定行
     */
    public void deleteRow(long notebookId, int row, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 删除指定行的所有单元格
                cellDao.deleteCellsByRow(notebookId, row);
                
                // 调整后续行的索引
                cellDao.adjustRowIndexAfterDelete(notebookId, row, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Row deleted: " + row);
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete row", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除指定列
     */
    public void deleteColumn(long notebookId, int col, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 删除指定列的所有单元格
                cellDao.deleteCellsByColumn(notebookId, col);
                
                // 调整后续列的索引
                cellDao.adjustColumnIndexAfterDelete(notebookId, col, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column deleted: " + col);
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete column", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 插入行
     */
    public void insertRow(long notebookId, int row, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 调整后续行的索引
                cellDao.insertRow(notebookId, row, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Row inserted at: " + row);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert row", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 插入列
     */
    public void insertColumn(long notebookId, int col, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                // 调整后续列的索引
                cellDao.insertColumn(notebookId, col, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column inserted at: " + col);
            } catch (Exception e) {
                Log.e(TAG, "Failed to insert column", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 移动行
     */
    public void moveRow(long notebookId, int fromRow, int toRow, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                cellDao.moveRow(notebookId, fromRow, toRow, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Row moved from " + fromRow + " to " + toRow);
            } catch (Exception e) {
                Log.e(TAG, "Failed to move row", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 移动列
     */
    public void moveColumn(long notebookId, int fromCol, int toCol, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                cellDao.moveColumn(notebookId, fromCol, toCol, DateUtils.now());
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Column moved from " + fromCol + " to " + toCol);
            } catch (Exception e) {
                Log.e(TAG, "Failed to move column", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除笔记本的所有单元格
     */
    public void deleteAllCells(long notebookId, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                int result = cellDao.deleteCellsByNotebookId(notebookId);
                
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                Log.d(TAG, "Deleted " + result + " cells for notebook: " + notebookId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete all cells", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Repository回调接口
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}