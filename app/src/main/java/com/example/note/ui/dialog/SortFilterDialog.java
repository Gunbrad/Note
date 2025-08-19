package com.example.note.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.ui.adapter.ColorFilterAdapter;
import com.example.note.ui.main.MainViewModel;
import com.google.android.material.button.MaterialButton;

/**
 * 排序筛选对话框
 */
public class SortFilterDialog extends DialogFragment {
    
    private RadioGroup radioGroupSort;
    private RecyclerView recyclerColors;
    private MaterialButton btnClearFilters;
    private MaterialButton btnCancel;
    private MaterialButton btnApply;
    
    private ColorFilterAdapter colorAdapter;
    
    private MainViewModel.SortType currentSortType = MainViewModel.SortType.UPDATED_DESC;
    private String currentFilterColor = null;
    
    private OnSortFilterListener listener;
    
    public interface OnSortFilterListener {
        void onSortFilterApplied(MainViewModel.SortType sortType, String filterColor);
        void onFiltersCleared();
    }
    
    public static SortFilterDialog newInstance(MainViewModel.SortType sortType, String filterColor) {
        SortFilterDialog dialog = new SortFilterDialog();
        Bundle args = new Bundle();
        args.putSerializable("sortType", sortType);
        args.putString("filterColor", filterColor);
        dialog.setArguments(args);
        return dialog;
    }
    
    public void setOnSortFilterListener(OnSortFilterListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSortType = (MainViewModel.SortType) getArguments().getSerializable("sortType");
            currentFilterColor = getArguments().getString("filterColor");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_sort_filter, null);
        
        initViews(view);
        setupSortOptions();
        setupColorFilter();
        setupButtons();
        
        return new AlertDialog.Builder(context)
                .setView(view)
                .create();
    }
    
    private void initViews(View view) {
        radioGroupSort = view.findViewById(R.id.radio_group_sort);
        recyclerColors = view.findViewById(R.id.recycler_colors);
        btnClearFilters = view.findViewById(R.id.btn_clear_filters);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnApply = view.findViewById(R.id.btn_apply);
    }
    
    private void setupSortOptions() {
        // 根据当前排序类型选中对应的RadioButton
        int checkedId = getSortRadioId(currentSortType);
        if (checkedId != -1) {
            radioGroupSort.check(checkedId);
        }
        
        // 设置排序选择监听器
        radioGroupSort.setOnCheckedChangeListener((group, checkedId1) -> {
            currentSortType = getSortTypeFromRadioId(checkedId1);
        });
    }
    
    private void setupColorFilter() {
        colorAdapter = new ColorFilterAdapter();
        colorAdapter.setSelectedColor(currentFilterColor);
        colorAdapter.setOnColorSelectedListener(color -> {
            currentFilterColor = color;
        });
        
        recyclerColors.setLayoutManager(new LinearLayoutManager(getContext(), 
                LinearLayoutManager.HORIZONTAL, false));
        recyclerColors.setAdapter(colorAdapter);
    }
    
    private void setupButtons() {
        btnClearFilters.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFiltersCleared();
            }
            dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSortFilterApplied(currentSortType, currentFilterColor);
            }
            dismiss();
        });
    }
    
    private int getSortRadioId(MainViewModel.SortType sortType) {
        switch (sortType) {
            case TITLE_ASC:
                return R.id.radio_title_asc;
            case TITLE_DESC:
                return R.id.radio_title_desc;
            case CREATED_ASC:
                return R.id.radio_created_asc;
            case CREATED_DESC:
                return R.id.radio_created_desc;
            case UPDATED_ASC:
                return R.id.radio_updated_asc;
            case UPDATED_DESC:
            default:
                return R.id.radio_updated_desc;
        }
    }
    
    private MainViewModel.SortType getSortTypeFromRadioId(int radioId) {
        if (radioId == R.id.radio_title_asc) {
            return MainViewModel.SortType.TITLE_ASC;
        } else if (radioId == R.id.radio_title_desc) {
            return MainViewModel.SortType.TITLE_DESC;
        } else if (radioId == R.id.radio_created_asc) {
            return MainViewModel.SortType.CREATED_ASC;
        } else if (radioId == R.id.radio_created_desc) {
            return MainViewModel.SortType.CREATED_DESC;
        } else if (radioId == R.id.radio_updated_asc) {
            return MainViewModel.SortType.UPDATED_ASC;
        } else {
            return MainViewModel.SortType.UPDATED_DESC;
        }
    }
}