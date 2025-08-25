package com.example.note.ui.note;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 可缩放的RecyclerView容器
 * 实现地图式的缩放和拖拽功能
 * 不使用Canvas变换，而是通过改变真实布局尺寸来实现缩放
 */
public class ZoomableRecyclerHost extends FrameLayout {
    
    private static final String TAG = "ZoomableRecyclerHost";
    
    // 缩放限制
    private static final float MIN_SCALE = 0.6f;
    private static final float MAX_SCALE = 2.5f;
    
    // 手势检测器
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    
    // 组件引用
    private final ColumnWidthProvider widthProvider;
    private final RecyclerView bodyRV;
    private final RecyclerView headerRV;
    private final RecyclerView frozenRV;
    private final NoteViewModel noteViewModel;
    
    // 状态
    private boolean isScaling = false;
    private boolean syncingScroll = false;
    private boolean syncingHoriz = false;
    private int horizontalOffsetPx = 0; // 仅用于记录 + 推给 ViewModel
    
    // 手势相关
    private final int touchSlop;
    private float lastX, lastY;
    private boolean draggingHoriz = false;
    
    
    

    
    public ZoomableRecyclerHost(Context context, ColumnWidthProvider widthProvider, 
                               RecyclerView bodyRV, RecyclerView headerRV, 
                               RecyclerView frozenRV, NoteViewModel noteViewModel) {
        super(context);
        this.widthProvider = widthProvider;
        this.bodyRV = bodyRV;
        this.headerRV = headerRV;
        this.frozenRV = frozenRV;
        this.noteViewModel = noteViewModel;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        
        // 禁用表头RecyclerView的ItemAnimator和过度滚动
        if (headerRV != null) {
            headerRV.setItemAnimator(null);
            headerRV.setOverScrollMode(OVER_SCROLL_NEVER);

            // ★ 强制统一：移除一切 ItemDecoration，清零左右 padding，关闭 clipToPadding
            for (int i = headerRV.getItemDecorationCount() - 1; i >= 0; i--) {
                headerRV.removeItemDecorationAt(i);
            }
            headerRV.setClipToPadding(false);
            headerRV.setPadding(0, headerRV.getPaddingTop(), 0, headerRV.getPaddingBottom());
            
            // ★ 统一 header item 的 LayoutParams
            headerRV.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(@NonNull android.view.View view) {
                    RecyclerView.ViewHolder vh = headerRV.getChildViewHolder(view);
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;

                    android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
                    // 统一宽度为 Provider 值（确保与行完全一致）
                    int w = widthProvider.getColumnWidthPx(pos);
                    if (lp != null) {
                        lp.width = w;
                        if (lp instanceof RecyclerView.LayoutParams) {
                            RecyclerView.LayoutParams rlp = (RecyclerView.LayoutParams) lp;
                            // 归零左右 margin，避免 header 比行"宽/窄"1~2px
                            if (rlp.leftMargin != 0 || rlp.rightMargin != 0) {
                                rlp.leftMargin = 0;
                                rlp.rightMargin = 0;
                            }
                        }
                        view.setLayoutParams(lp);
                    }
                }
                
                @Override
                public void onChildViewDetachedFromWindow(@NonNull android.view.View view) {}
            });
        }
        
        // 初始化缩放手势检测器
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                return true;
            }
            
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float oldScale = widthProvider.getScale();
                float newScale = clamp(oldScale * detector.getScaleFactor(), MIN_SCALE, MAX_SCALE);
                
                if (oldScale != newScale) {
                    applyScaleKeepingFocus(oldScale, newScale, detector.getFocusX(), detector.getFocusY());
                }
                return true;
            }
            
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
            }
        });
        
        // 初始化手势检测器（单指拖拽和双击）
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!isScaling) {
                    if (draggingHoriz) {
                        if (headerRV != null) headerRV.scrollBy((int) distanceX, 0); // ★ 只滚"真源"
                    } else {
                        bodyRV.scrollBy(0, (int) distanceY); // 纵向与原来一致
                    }
                }
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (draggingHoriz && headerRV != null) {
                    headerRV.fling((int) -velocityX, 0); // 手指向右划（velocityX>0），内容向左 -> 取负
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 双击缩放到预设档位
                float currentScale = widthProvider.getScale();
                float targetScale = (currentScale < 1.0f) ? 1.0f : 0.8f;
                applyScaleKeepingFocus(currentScale, targetScale, e.getX(), e.getY());
                return true;
            }
        });
        
        // 设置bodyRV的滚动监听器，同步其他RecyclerView
        setupScrollSync();
    }
    
    public ZoomableRecyclerHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 这个构造函数用于XML布局，但我们主要通过代码创建
        throw new UnsupportedOperationException("Use the parameterized constructor");
    }
    
    private void setupScrollSync() {
        // bodyRV滚动时同步到frozenRV
        bodyRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (syncingScroll) return;
                
                syncingScroll = true;
                try {
                    // 仅同步纵向到冻结列
                    if (frozenRV != null) frozenRV.scrollBy(0, dy);

                    // 更新视口状态
                    int scrollY = recyclerView.computeVerticalScrollOffset();
                    noteViewModel.updateViewport(
                            widthProvider.getScale(),
                            0.0f,
                            (float) scrollY
                    );
                } finally {
                    syncingScroll = false;
                }
            }
        });
        
        // frozenRV滚动时同步到bodyRV
        if (frozenRV != null) {
            frozenRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (syncingScroll) return;
                    
                    syncingScroll = true;
                    try {
                        // 仅同步纵向到主体表格
                        bodyRV.scrollBy(0, dy);
                        
                        // 更新视口状态
                        int scrollY = bodyRV.computeVerticalScrollOffset();
                        noteViewModel.updateViewport(
                                widthProvider.getScale(),
                                0.0f,
                                (float) scrollY
                        );
                    } finally {
                        syncingScroll = false;
                    }
                }
            });
        }
        
        // 为headerRV添加滚动监听器，同步所有可见行的横向滚动
        if (headerRV != null) {
            headerRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    if (syncingHoriz) return;
                    // 不再用 dx==0 直接返回；有些情况下（边界/settle）也需要对齐
                    syncRowsFromHeaderAnchor();
                }
                
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && !syncingHoriz) {
                        syncRowsFromHeaderAnchor();
                    }
                }
            });
        }

    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX = ev.getX();
                lastY = ev.getY();
                draggingHoriz = false;
                // 停止一切滚动，避免惯性与对齐竞争（与纵向一致）
                bodyRV.stopScroll();
                if (headerRV != null) headerRV.stopScroll();
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 立即拦截多指事件，防止传递给子视图（包括编辑模式下的缩放）
                return true;
            case MotionEvent.ACTION_MOVE:
                // 编辑模式下不拦截单指拖拽事件，让EditText正常工作
                if (EditingStateHolder.isEditing()) {
                    return false;
                }
                
                float dx = Math.abs(ev.getX() - lastX);
                float dy = Math.abs(ev.getY() - lastY);
                if (dx > touchSlop || dy > touchSlop) {
                    if (dx > dy) { 
                        draggingHoriz = true;
                        lastX = ev.getX(); // 更新lastX用于后续计算
                        lastY = ev.getY(); // 更新lastY
                        return true; 
                    } else if (dx > touchSlop * 0.7f) {
                        // 即使垂直移动更大，但水平移动也足够大时，仍然允许水平拖拽
                        // 这样可以让表格区域的水平拖拽更容易触发
                        draggingHoriz = true;
                        lastX = ev.getX();
                        lastY = ev.getY();
                        return true;
                    } else { 
                        draggingHoriz = false; 
                        return false; 
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingHoriz = false;
                break;
        }
        return false;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 始终处理缩放手势，即使在编辑模式下
        boolean scaleHandled = scaleDetector.onTouchEvent(event);
        
        // 编辑模式下不处理单指拖拽和手势，但允许缩放
        if (EditingStateHolder.isEditing()) {
            return scaleHandled;
        }
        
        // 处理单指手势
        boolean gestureHandled = gestureDetector.onTouchEvent(event);
        
        // 手动处理水平拖拽
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (draggingHoriz && !isScaling && event.getPointerCount() == 1) {
                    float currentX = event.getX();
                    float deltaX = lastX - currentX;
                    lastX = currentX;
                    
                    // 边界预检和阻尼效果
                    if (headerRV != null && Math.abs(deltaX) > 0) {
                        int currentScrollX = headerRV.computeHorizontalScrollOffset();
                        int maxScrollX = getRealMaxHorizontalOffset();
                        
                        // 应用阻尼效果
                        float dampedDeltaX = deltaX;
                        if (currentScrollX <= 0 && deltaX > 0) {
                            // 左边界阻尼
                            dampedDeltaX = deltaX * 0.3f;
                        } else if (currentScrollX >= maxScrollX && deltaX < 0) {
                            // 右边界阻尼
                            dampedDeltaX = deltaX * 0.3f;
                        }
                        
                        headerRV.scrollBy((int) dampedDeltaX, 0);
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggingHoriz) {
                    draggingHoriz = false;
                    // 手势结束后强制边界检查和重新对齐
                    post(() -> enforceHorizontalBoundsAndRealign());
                }
                break;
        }
        
        return scaleHandled || gestureHandled || true;
    }
    
    /**
     * 应用缩放并保持焦点不偏移
     */
    private void applyScaleKeepingFocus(float oldScale, float newScale, float focusX, float focusY) {
        if (oldScale == newScale) return;
        
        // 1. 更新ViewModel中的scale
        int currentOffsetX = (int) noteViewModel.getCurrentOffsetX();
        int currentOffsetY = (int) noteViewModel.getCurrentOffsetY();
        noteViewModel.updateViewport(newScale, (float) currentOffsetX, (float) currentOffsetY);
        
        // 2. 通知所有适配器尺寸变化
        notifyAllAdaptersForSizeChange();
        
        // 3. 延迟到下一帧进行对齐，避免使用旧布局信息造成闪跳
        post(() -> {
            // 横向焦点保持
            if (headerRV != null) {
                int oldScrollX = headerRV.computeHorizontalScrollOffset();
                float contentX = (oldScrollX + focusX) / oldScale;
                int newScrollX = Math.round(contentX * newScale - focusX);
                jumpHeaderTo(newScrollX);
            }
            
            // 纵向焦点保持
            int oldScrollY = bodyRV.computeVerticalScrollOffset();
            float contentY = (oldScrollY + focusY) / oldScale;
            int newScrollY = Math.round(contentY * newScale - focusY);
            clampAndScrollToVertical(newScrollY);
            
            // 最终校正：确保横向对齐无误差
            enforceHorizontalBoundsAndRealign();
        });
    }
    








    /**
     * 通知所有适配器尺寸发生变化
     */
    private void notifyAllAdaptersForSizeChange() {
        // 在通知适配器之前，先清除ColumnHeaderAdapter的宽度缓存
        // 这样它会使用最新的缩放因子重新计算宽度
        if (headerRV != null && headerRV.getAdapter() != null) {
            RecyclerView.Adapter adapter = headerRV.getAdapter();
            if (adapter instanceof com.example.note.adapter.ColumnHeaderAdapter) {
                com.example.note.adapter.ColumnHeaderAdapter columnAdapter = 
                    (com.example.note.adapter.ColumnHeaderAdapter) adapter;
                columnAdapter.clearWidthCache();
            }
        }
        
        if (bodyRV.getAdapter() != null) {
            bodyRV.getAdapter().notifyDataSetChanged();
        }
        if (headerRV != null && headerRV.getAdapter() != null) {
            headerRV.getAdapter().notifyDataSetChanged();
        }
        if (frozenRV != null && frozenRV.getAdapter() != null) {
            frozenRV.getAdapter().notifyDataSetChanged();
        }
        
        // 请求重新布局
        bodyRV.requestLayout();
        if (headerRV != null) headerRV.requestLayout();
        if (frozenRV != null) frozenRV.requestLayout();
    }
    
    /**
     * 限制滚动范围并滚动到指定位置（仅纵向）
     */
    private void clampAndScrollToVertical(int targetY) {
        int currentY = bodyRV.computeVerticalScrollOffset();
        int deltaY = targetY - currentY;
        bodyRV.scrollBy(0, deltaY);
    }
    
    /**
     * 限制数值在指定范围内
     */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 整数值限制在指定范围内
     */
    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 获取header的真实视口宽度（可回退Host宽度）
     */
    private int getHeaderViewportWidth() {
        if (headerRV != null && headerRV.getWidth() > 0) {
            return headerRV.getWidth();
        }
        return getWidth();
    }

    /**
     * 获取最大横向偏移量
     */
    private int getMaxHorizontalOffset() {
        return getRealMaxHorizontalOffset();
    }
    
    /**
     * 获取真实的最大横向偏移量，考虑装饰边界
     */
    private int getRealMaxHorizontalOffset() {
        if (headerRV == null) return 0;
        
        int totalWidth = widthProvider.getTotalColumnsWidthPx();
        int viewportWidth = getHeaderViewportWidth();
        
        // 考虑 padding 和装饰
        int paddingHorizontal = headerRV.getPaddingLeft() + headerRV.getPaddingRight();
        int effectiveViewportWidth = Math.max(0, viewportWidth - paddingHorizontal);
        
        return Math.max(0, totalWidth - effectiveViewportWidth);
    }
    
    /**
     * 同步所有可见行的横向滚动
     */
    private void syncAllVisibleRowsHorizontally(int dx) {
        LinearLayoutManager bodyLayoutManager = (LinearLayoutManager) bodyRV.getLayoutManager();
        if (bodyLayoutManager == null) return;
        
        int firstVisiblePosition = bodyLayoutManager.findFirstVisibleItemPosition();
        int lastVisiblePosition = bodyLayoutManager.findLastVisibleItemPosition();
        
        for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
            RecyclerView.ViewHolder viewHolder = bodyRV.findViewHolderForAdapterPosition(i);
            if (viewHolder instanceof TableRowAdapter.TableRowViewHolder) {
                TableRowAdapter.TableRowViewHolder rowHolder = (TableRowAdapter.TableRowViewHolder) viewHolder;
                RecyclerView rowCellsRV = rowHolder.getRowCellsRecycler();
                if (rowCellsRV != null) {
                    rowCellsRV.scrollBy(dx, 0);
                }
            }
        }
    }

    /**
     * 从 header 的真实布局锚点同步所有行；像素级对齐
     */
    private void syncRowsFromHeaderAnchor() {
        if (headerRV == null) return;
        if (!(headerRV.getLayoutManager() instanceof LinearLayoutManager)) return;

        // --- 超界回跳：先把 header 自己拉回合法范围 ---
        int maxX = getRealMaxHorizontalOffset(); // 使用增强的边界检查
        int headerX = headerRV.computeHorizontalScrollOffset();
        
        // 添加2px容差，避免边界附近的频繁调整
        final int TOLERANCE = 2;
        if (headerX < -TOLERANCE || headerX > maxX + TOLERANCE) {
            // 直接把 header 定位到我们定义的边界；内部会同步行并写回 VM
            jumpHeaderTo(clampInt(headerX, 0, maxX));
            return; // 这次由 jumpHeaderTo 完成对齐，避免本方法再算一次
        }

        syncingHoriz = true;
        try {
            // 1) 记录像素偏移
            horizontalOffsetPx = clampInt(headerX, 0, maxX);

            // 2) 以 LayoutManager 的首个可见项 + 像素内偏移作为唯一锚点
            LinearLayoutManager llm = (LinearLayoutManager) headerRV.getLayoutManager();
            int firstIndex = Math.max(0, llm.findFirstVisibleItemPosition());
            int offsetInFirst = 0;
            
            // 检查是否接近最后两列，使用精确映射
            int totalColumns = 0;
            if (noteViewModel.getColumns().getValue() != null) {
                totalColumns = noteViewModel.getColumns().getValue().size();
            }
            
            boolean nearLastColumns = (totalColumns > 2) && (firstIndex >= totalColumns - 2);
            
            if (nearLastColumns) {
                // 对于最后两列，使用mapScrollXToPositionAndOffset进行精确映射
                int[] mappedPos = widthProvider.mapScrollXToPositionAndOffset(horizontalOffsetPx);
                firstIndex = mappedPos[0];
                offsetInFirst = mappedPos[1];
            } else {
                // 常规情况：使用布局锚点
                android.view.View firstView = llm.findViewByPosition(firstIndex);
                if (firstView != null) {
                    // ★★★ 关键：用装饰后(decorated)左边界
                    int decoratedLeft = llm.getDecoratedLeft(firstView);
                    int paddingLeft = headerRV.getPaddingLeft();
                    offsetInFirst = paddingLeft - decoratedLeft; // ≥0

                    if (offsetInFirst < 0) offsetInFirst = 0;

                    // 用装饰后宽度夹紧
                    int decoratedWidth = firstView.getWidth()
                            + llm.getLeftDecorationWidth(firstView)
                            + llm.getRightDecorationWidth(firstView);
                    if (decoratedWidth > 0 && offsetInFirst >= decoratedWidth) {
                        offsetInFirst = decoratedWidth - 1;
                    }
                }
            }

            // 3) 同步所有可见行，并缓存锚点供后续 attach/bind 使用
            if (bodyRV.getAdapter() instanceof TableRowAdapter) {
                TableRowAdapter adapter = (TableRowAdapter) bodyRV.getAdapter();
                adapter.setGlobalHorizontalOffset(horizontalOffsetPx);       // 兼容旧字段
                adapter.applyAbsoluteOffsetToAllRowsWithColumnIndex(bodyRV, firstIndex, offsetInFirst);
            }

            // 4) 写回 VM
            int scrollY = bodyRV.computeVerticalScrollOffset();
            noteViewModel.updateViewport(
                    widthProvider.getScale(),
                    (float) horizontalOffsetPx,
                    (float) scrollY
            );
        } finally {
            syncingHoriz = false;
        }
    }

    /**
     * 当视口尺寸变化（横竖屏/缩放后重算布局）时，重夹紧并对齐
     */
    private void enforceHorizontalBoundsAndRealign() {
        int clamped = clampInt(horizontalOffsetPx, 0, getMaxHorizontalOffset());
        if (clamped != horizontalOffsetPx) {
            jumpHeaderTo(clamped); // 内部会同步行与写回VM
        } else {
            syncRowsFromHeaderAnchor(); // 强制像素级对齐，替换理论映射
        }
    }

    /**
     * 验证并修正对齐误差
     * 检测列头和行单元格的对齐情况，如果误差超过1像素则进行修正
     */
    private void validateAndFixAlignment() {
        if (syncingHoriz || headerRV == null || bodyRV == null) return;
        
        try {
            // 获取列头的当前锚点
            RecyclerView.LayoutManager headerLM = headerRV.getLayoutManager();
            if (!(headerLM instanceof LinearLayoutManager)) return;
            
            LinearLayoutManager headerLinearLM = (LinearLayoutManager) headerLM;
            int headerFirstPos = headerLinearLM.findFirstVisibleItemPosition();
            if (headerFirstPos == RecyclerView.NO_POSITION) return;
            
            android.view.View headerFirstView = headerLinearLM.findViewByPosition(headerFirstPos);
            if (headerFirstView == null) return;
            
            int headerOffset = headerFirstView.getLeft();
            
            // 检查行单元格的对齐情况
            if (bodyRV.getAdapter() instanceof TableRowAdapter) {
                TableRowAdapter adapter = (TableRowAdapter) bodyRV.getAdapter();
                
                // 获取第一个可见行的第一个单元格位置
                RecyclerView.LayoutManager bodyLM = bodyRV.getLayoutManager();
                if (!(bodyLM instanceof LinearLayoutManager)) return;
                
                LinearLayoutManager bodyLinearLM = (LinearLayoutManager) bodyLM;
                int bodyFirstPos = bodyLinearLM.findFirstVisibleItemPosition();
                if (bodyFirstPos == RecyclerView.NO_POSITION) return;
                
                android.view.View bodyFirstView = bodyLinearLM.findViewByPosition(bodyFirstPos);
                if (bodyFirstView == null) return;
                
                // 检查行内第一个单元格的位置
                if (bodyFirstView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup rowView = (android.view.ViewGroup) bodyFirstView;
                    if (rowView.getChildCount() > 0) {
                        android.view.View firstCell = rowView.getChildAt(0);
                        int cellOffset = firstCell.getLeft();
                        
                        // 计算对齐误差
                        int alignmentError = Math.abs(headerOffset - cellOffset);
                        
                        // 如果误差超过1像素，进行修正
                        if (alignmentError > 1) {
                            syncRowsFromHeaderAnchor();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略验证过程中的异常，避免影响正常功能
        }
    }
    
    /**
     * 尺寸变化时调用
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(this::enforceHorizontalBoundsAndRealign);
    }
    
    /**
     * 程序化跳转到指定横向位置
     */
    private void jumpHeaderTo(int targetX) {
        if (headerRV == null) return;

        int maxX = getMaxHorizontalOffset();
        horizontalOffsetPx = clampInt(targetX, 0, maxX);

        int[] po = widthProvider.mapScrollXToPositionAndOffset(horizontalOffsetPx);
        int firstIndex = po[0], offsetInFirst = po[1];

        syncingHoriz = true;
        try {
            // 1) 列头绝对定位
            RecyclerView.LayoutManager lm = headerRV.getLayoutManager();
            if (lm instanceof LinearLayoutManager) {
                ((LinearLayoutManager) lm).scrollToPositionWithOffset(firstIndex, -offsetInFirst);
            }

            // 2) 行绝对定位（与列头同一基准）
            if (bodyRV.getAdapter() instanceof TableRowAdapter) {
                TableRowAdapter adapter = (TableRowAdapter) bodyRV.getAdapter();
                adapter.setGlobalHorizontalOffset(horizontalOffsetPx);
                adapter.applyAbsoluteOffsetToAllRowsWithColumnIndex(bodyRV, firstIndex, offsetInFirst);
            }

            // 3) 写回 VM
            int scrollY = bodyRV.computeVerticalScrollOffset();
            noteViewModel.updateViewport(widthProvider.getScale(),
                    (float) horizontalOffsetPx, (float) scrollY);
        } finally {
            syncingHoriz = false;
        }

        // ★ 用真实布局锚点再校一次，彻底消除取整误差
        post(() -> { 
            if (!syncingHoriz) {
                syncRowsFromHeaderAnchor();
                // 延迟验证对齐，修正可能的1像素误差
                postDelayed(() -> validateAndFixAlignment(), 16);
            }
        });
    }
    

    
    /**
     * 恢复视口状态（从ViewModel加载）
     */
    public void restoreViewport() {
        float scale = noteViewModel.getScale().getValue() != null ? noteViewModel.getScale().getValue() : 1.0f;
        int offsetX = (int) noteViewModel.getCurrentOffsetX();
        int offsetY = (int) noteViewModel.getCurrentOffsetY();
        
        // 先应用缩放
        if (scale != widthProvider.getScale()) {
            noteViewModel.updateViewport(scale, (float) offsetX, (float) offsetY);
            notifyAllAdaptersForSizeChange();
        }
        
        // 延迟恢复滚动位置
        post(() -> {
            // 横向恢复
            jumpHeaderTo(offsetX);
            
            // 纵向恢复
            bodyRV.scrollBy(0, offsetY - bodyRV.computeVerticalScrollOffset());
        });
    }
    

}