package com.example.note.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.note.R;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 创建笔记本对话框
 * 用于输入新笔记本的名称
 */
public class CreateNotebookDialog extends DialogFragment {
    
    public interface OnNotebookNameListener {
        void onNotebookNameConfirmed(String name);
    }
    
    private OnNotebookNameListener listener;
    private EditText nameEditText;
    private TextInputLayout nameInputLayout;
    private Button positiveButton;
    
    public static CreateNotebookDialog newInstance() {
        return new CreateNotebookDialog();
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNotebookNameListener) {
            listener = (OnNotebookNameListener) context;
        }
    }
    
    public void setOnNotebookNameListener(OnNotebookNameListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_create_notebook, null);
        
        initViews(view);
        setupValidation();
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("创建新笔记")
                .setView(view)
                .setPositiveButton("下一步", null) // 先设为null，后面手动设置
                .setNegativeButton("取消", (d, which) -> dismiss())
                .create();
        
        dialog.setOnShowListener(d -> {
            positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String name = nameEditText.getText().toString().trim();
                if (validateName(name)) {
                    if (listener != null) {
                        listener.onNotebookNameConfirmed(name);
                    }
                    dismiss();
                }
            });
            
            // 初始状态下禁用确定按钮
            positiveButton.setEnabled(false);
        });
        
        return dialog;
    }
    
    private void initViews(View view) {
        nameInputLayout = view.findViewById(R.id.name_input_layout);
        nameEditText = view.findViewById(R.id.name_edit_text);
        
        // 自动获取焦点
        nameEditText.requestFocus();
    }
    
    private void setupValidation() {
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = s.toString().trim();
                boolean isValid = validateName(name);
                
                if (positiveButton != null) {
                    positiveButton.setEnabled(isValid);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private boolean validateName(String name) {
        if (name.isEmpty()) {
            nameInputLayout.setError("请输入笔记名称");
            return false;
        }
        
        if (name.length() > 50) {
            nameInputLayout.setError("笔记名称不能超过50个字符");
            return false;
        }
        
        // 检查是否包含非法字符
        if (name.contains("/") || name.contains("\\") || name.contains(":") || 
            name.contains("*") || name.contains("?") || name.contains("\"") || 
            name.contains("<") || name.contains(">") || name.contains("|")) {
            nameInputLayout.setError("笔记名称不能包含特殊字符");
            return false;
        }
        
        nameInputLayout.setError(null);
        return true;
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}