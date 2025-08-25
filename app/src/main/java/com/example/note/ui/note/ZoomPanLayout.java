package com.example.note.ui.note;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 支持缩放和平移的自定义容器
 * 用于实现表格的拖拽移动视角和双指缩放功能
 * 根据修改建议重新实现
 */
public class ZoomPanLayout extends FrameLayout {
    
    private static final String TAG = "ZoomPanLayout";
    
    // 缩放和平移相关
    private float scale = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    
    // 缩放限制
    private static final float MIN_SCALE = 0.6f;
    private static final float MAX_SCALE = 2.0f;
    
    // 手势检测器
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    
    // 平移相关
    private PointF lastPanPoint = new PointF();
    private boolean isPanning = false;
    
    // 监听器
    private OnViewportChangeListener viewportChangeListener;
    
    public interface OnViewportChangeListener {
        void onViewportChanged(float scale, float offsetX, float offsetY);
    }
    
    public ZoomPanLayout(Context context) {
        super(context);
        init();
    }
    
    public ZoomPanLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ZoomPanLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 初始化缩放手势检测器
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScale = scale * scaleFactor;
                
                // 限制缩放范围
                newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
                
                if (newScale != scale) {
                    // 计算缩放中心点
                    float focusX = detector.getFocusX();
                    float focusY = detector.getFocusY();
                    
                    // 调整偏移量以保持缩放中心点不变
                    float scaleChange = newScale / scale;
                    offsetX = focusX - (focusX - offsetX) * scaleChange;
                    offsetY = focusY - (focusY - offsetY) * scaleChange;
                    
                    scale = newScale;
                    applyTransformation();
                    
                    if (viewportChangeListener != null) {
                        viewportChangeListener.onViewportChanged(scale, offsetX, offsetY);
                    }
                }
                return true;
            }
        });
        
        // 初始化手势检测器（用于拖拽）
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                lastPanPoint.set(e.getX(), e.getY());
                isPanning = true;
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isPanning && !scaleGestureDetector.isInProgress()) {
                    // 更新偏移量
                    offsetX -= distanceX;
                    offsetY -= distanceY;
                    
                    // 限制平移范围（可选，避免内容完全移出视野）
                    limitOffset();
                    
                    applyTransformation();
                    
                    if (viewportChangeListener != null) {
                        viewportChangeListener.onViewportChanged(scale, offsetX, offsetY);
                    }
                }
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // 可以在这里实现惯性滚动
                return true;
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 编辑模式下不处理触摸事件
        if (EditingStateHolder.isEditing()) {
            return false;
        }
        
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);
        
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            isPanning = false;
        }
        
        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 编辑模式下不拦截事件，让EditText正常工作
        if (EditingStateHolder.isEditing()) {
            return false;
        }
        
        // 对ACTION_POINTER_DOWN立即返回true，阻断多指事件传给子视图
        if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            return true;
        }
        
        // 检查是否是多指触摸（缩放手势）
        if (ev.getPointerCount() > 1) {
            return true; // 拦截多指触摸用于缩放
        }
        
        // 对于单指触摸，先让手势检测器处理
        scaleGestureDetector.onTouchEvent(ev);
        
        // 如果正在进行缩放，拦截事件
        if (scaleGestureDetector.isInProgress()) {
            return true;
        }
        
        // 对于单指触摸，检查是否是拖拽手势
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastPanPoint.set(ev.getX(), ev.getY());
                return false; // 不拦截DOWN事件，让子视图有机会处理
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = Math.abs(ev.getX() - lastPanPoint.x);
                float deltaY = Math.abs(ev.getY() - lastPanPoint.y);
                
                // 只有当移动距离超过阈值时才认为是拖拽手势
                float touchSlop = 10; // 可以调整这个值
                if (deltaX > touchSlop || deltaY > touchSlop) {
                    return true; // 拦截MOVE事件用于拖拽
                }
                break;
        }
        
        return false; // 默认不拦截，让子视图处理触摸事件
    }
    
    /**
     * 应用变换到子视图
     */
    private void applyTransformation() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setTranslationX(offsetX);
            child.setTranslationY(offsetY);
        }
    }
    
    /**
     * 限制偏移量范围
     */
    private void limitOffset() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            float childWidth = child.getWidth() * scale;
            float childHeight = child.getHeight() * scale;
            
            float maxOffsetX = Math.max(0, (childWidth - getWidth()) / 2);
            float maxOffsetY = Math.max(0, (childHeight - getHeight()) / 2);
            
            offsetX = Math.max(-maxOffsetX, Math.min(maxOffsetX, offsetX));
            offsetY = Math.max(-maxOffsetY, Math.min(maxOffsetY, offsetY));
        }
    }
    
    /**
     * 设置视口状态
     */
    public void setViewport(float scale, float offsetX, float offsetY) {
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        applyTransformation();
    }
    
    /**
     * 重置视口到初始状态
     */
    public void resetViewport() {
        this.scale = 1.0f;
        this.offsetX = 0f;
        this.offsetY = 0f;
        applyTransformation();
        
        if (viewportChangeListener != null) {
            viewportChangeListener.onViewportChanged(scale, offsetX, offsetY);
        }
    }
    
    /**
     * 获取当前缩放比例
     */
    public float getScale() {
        return scale;
    }
    
    /**
     * 获取当前X偏移量
     */
    public float getOffsetX() {
        return offsetX;
    }
    
    /**
     * 获取当前Y偏移量
     */
    public float getOffsetY() {
        return offsetY;
    }
    
    /**
     * 设置视口变化监听器
     */
    public void setOnViewportChangeListener(OnViewportChangeListener listener) {
        this.viewportChangeListener = listener;
    }
}