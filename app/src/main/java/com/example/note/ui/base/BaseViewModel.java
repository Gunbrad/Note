package com.example.note.ui.base;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ViewModel基类
 * 提供通用的状态管理和错误处理功能
 */
public abstract class BaseViewModel extends AndroidViewModel {
    
    protected static final String TAG = "BaseViewModel";
    
    // 加载状态
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;
    
    // 错误信息
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;
    
    // 成功消息
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public final LiveData<String> successMessage = _successMessage;
    
    // 网络状态
    private final MutableLiveData<Boolean> _isNetworkAvailable = new MutableLiveData<>(true);
    public final LiveData<Boolean> isNetworkAvailable = _isNetworkAvailable;
    
    // 空状态
    private final MutableLiveData<Boolean> _isEmpty = new MutableLiveData<>(false);
    public final LiveData<Boolean> isEmpty = _isEmpty;
    
    // 刷新状态
    private final MutableLiveData<Boolean> _isRefreshing = new MutableLiveData<>(false);
    public final LiveData<Boolean> isRefreshing = _isRefreshing;
    
    // 初始化状态
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    
    public BaseViewModel(@NonNull Application application) {
        super(application);
    }
    
    /**
     * 设置加载状态
     */
    protected void setLoading(boolean loading) {
        if (_isLoading.getValue() != loading) {
            _isLoading.postValue(loading);
            Log.d(getLogTag(), "Loading state changed: " + loading);
        }
    }
    
    /**
     * 设置刷新状态
     */
    protected void setRefreshing(boolean refreshing) {
        if (_isRefreshing.getValue() != refreshing) {
            _isRefreshing.postValue(refreshing);
            Log.d(getLogTag(), "Refreshing state changed: " + refreshing);
        }
    }
    
    /**
     * 设置空状态
     */
    protected void setEmpty(boolean empty) {
        if (_isEmpty.getValue() != empty) {
            _isEmpty.postValue(empty);
            Log.d(getLogTag(), "Empty state changed: " + empty);
        }
    }
    
    /**
     * 设置网络状态
     */
    protected void setNetworkAvailable(boolean available) {
        if (_isNetworkAvailable.getValue() != available) {
            _isNetworkAvailable.postValue(available);
            Log.d(getLogTag(), "Network state changed: " + available);
        }
    }
    
    /**
     * 显示错误信息
     */
    protected void showError(String message) {
        if (message != null && !message.trim().isEmpty()) {
            _errorMessage.postValue(message.trim());
            Log.e(getLogTag(), "Error: " + message);
        }
    }
    
    /**
     * 显示错误信息（从异常）
     */
    protected void showError(Exception exception) {
        if (exception != null) {
            String message = exception.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "发生未知错误";
            }
            showError(message);
        }
    }
    
    /**
     * 显示成功信息
     */
    protected void showSuccess(String message) {
        if (message != null && !message.trim().isEmpty()) {
            _successMessage.postValue(message.trim());
            Log.d(getLogTag(), "Success: " + message);
        }
    }
    
    /**
     * 清除错误信息
     */
    public void clearError() {
        _errorMessage.postValue(null);
    }
    
    /**
     * 清除成功信息
     */
    public void clearSuccess() {
        _successMessage.postValue(null);
    }
    
    /**
     * 清除所有消息
     */
    public void clearMessages() {
        clearError();
        clearSuccess();
    }
    
    /**
     * 初始化ViewModel
     * 子类可以重写此方法进行初始化操作
     */
    public void initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            Log.d(getLogTag(), "Initializing ViewModel");
            onInitialize();
        }
    }
    
    /**
     * 子类重写此方法进行初始化
     */
    protected void onInitialize() {
        // 子类实现
    }
    
    /**
     * 刷新数据
     * 子类可以重写此方法实现刷新逻辑
     */
    public void refresh() {
        Log.d(getLogTag(), "Refreshing data");
        setRefreshing(true);
        onRefresh();
    }
    
    /**
     * 子类重写此方法实现刷新逻辑
     */
    protected void onRefresh() {
        // 默认实现：结束刷新状态
        setRefreshing(false);
    }
    
    /**
     * 重试操作
     * 子类可以重写此方法实现重试逻辑
     */
    public void retry() {
        Log.d(getLogTag(), "Retrying operation");
        clearMessages();
        onRetry();
    }
    
    /**
     * 子类重写此方法实现重试逻辑
     */
    protected void onRetry() {
        // 默认实现：重新初始化
        onInitialize();
    }
    
    /**
     * 获取日志标签
     */
    protected String getLogTag() {
        return getClass().getSimpleName();
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized.get();
    }
    
    /**
     * 获取当前加载状态
     */
    public boolean isCurrentlyLoading() {
        Boolean loading = _isLoading.getValue();
        return loading != null && loading;
    }
    
    /**
     * 获取当前刷新状态
     */
    public boolean isCurrentlyRefreshing() {
        Boolean refreshing = _isRefreshing.getValue();
        return refreshing != null && refreshing;
    }
    
    /**
     * 获取当前空状态
     */
    public boolean isCurrentlyEmpty() {
        Boolean empty = _isEmpty.getValue();
        return empty != null && empty;
    }
    
    /**
     * 获取当前网络状态
     */
    public boolean isCurrentlyNetworkAvailable() {
        Boolean available = _isNetworkAvailable.getValue();
        return available == null || available;
    }
    
    /**
     * 执行异步操作的辅助方法
     */
    protected void executeAsync(Runnable operation) {
        executeAsync(operation, true);
    }
    
    /**
     * 执行异步操作的辅助方法
     * @param operation 要执行的操作
     * @param showLoading 是否显示加载状态
     */
    protected void executeAsync(Runnable operation, boolean showLoading) {
        if (showLoading) {
            setLoading(true);
        }
        
        try {
            operation.run();
        } catch (Exception e) {
            Log.e(getLogTag(), "Error executing async operation", e);
            showError(e);
        } finally {
            if (showLoading) {
                setLoading(false);
            }
        }
    }
    
    /**
     * 处理Repository回调的辅助方法
     */
    protected <T> void handleRepositoryCallback(T result, Exception error, String successMessage) {
        setLoading(false);
        setRefreshing(false);
        
        if (error != null) {
            showError(error);
        } else {
            if (successMessage != null) {
                showSuccess(successMessage);
            }
        }
    }
    
    /**
     * 处理Repository回调的辅助方法（无成功消息）
     */
    protected <T> void handleRepositoryCallback(T result, Exception error) {
        handleRepositoryCallback(result, error, null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(getLogTag(), "ViewModel cleared");
    }
}