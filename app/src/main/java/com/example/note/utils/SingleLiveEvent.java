package com.example.note.utils;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单次事件LiveData
 * 用于处理只需要触发一次的事件，如导航、显示Toast等
 * 解决LiveData在配置变更时重复触发的问题
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {
    
    private final AtomicBoolean pending = new AtomicBoolean(false);
    
    @MainThread
    @Override
    public void observe(LifecycleOwner owner, Observer<? super T> observer) {
        // 只允许一个观察者
        if (hasObservers()) {
            throw new IllegalStateException("Multiple observers registered but only one will be notified of changes.");
        }
        
        // 包装观察者，确保只在pending为true时才通知
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }
    
    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        pending.set(true);
        super.setValue(t);
    }
    
    /**
     * 用于无参数事件的便捷方法
     */
    @MainThread
    public void call() {
        setValue(null);
    }
    
    /**
     * 检查是否有待处理的事件
     */
    public boolean hasPendingEvent() {
        return pending.get();
    }
    
    /**
     * 清除待处理的事件
     */
    public void clearPending() {
        pending.set(false);
    }
}