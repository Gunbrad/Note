package com.example.note.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 颜色筛选适配器
 */
public class ColorFilterAdapter extends RecyclerView.Adapter<ColorFilterAdapter.ColorViewHolder> {
    
    private List<String> colors = new ArrayList<>();
    private String selectedColor = null;
    private OnColorSelectedListener listener;
    
    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }
    
    public ColorFilterAdapter() {
        // 添加"全部"选项（null表示不筛选）
        colors.add(null);
        // 添加预定义颜色
        colors.addAll(ColorUtils.getPresetColors());
    }
    
    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }
    
    public void setSelectedColor(String color) {
        this.selectedColor = color;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_filter, parent, false);
        return new ColorViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        String color = colors.get(position);
        holder.bind(color, isSelected(color));
    }
    
    @Override
    public int getItemCount() {
        return colors.size();
    }
    
    private boolean isSelected(String color) {
        if (selectedColor == null && color == null) {
            return true;
        }
        return selectedColor != null && selectedColor.equals(color);
    }
    
    class ColorViewHolder extends RecyclerView.ViewHolder {
        private final View colorCircle;
        private final ImageView checkIcon;
        
        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorCircle = itemView.findViewById(R.id.color_circle);
            checkIcon = itemView.findViewById(R.id.check_icon);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    String color = colors.get(position);
                    setSelectedColor(color);
                    listener.onColorSelected(color);
                }
            });
        }
        
        public void bind(String color, boolean isSelected) {
            if (color == null) {
                // "全部"选项显示为灰色圆圈
                colorCircle.setBackgroundResource(R.drawable.circle_background);
                colorCircle.getBackground().setTint(Color.GRAY);
            } else {
                // 显示对应颜色
                colorCircle.setBackgroundResource(R.drawable.circle_background);
                colorCircle.getBackground().setTint(Color.parseColor(color));
            }
            
            // 显示/隐藏选中图标
            checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }
    }
}