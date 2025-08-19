package com.example.note.ui.template;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.note.R;
import com.example.note.data.entity.Template;
import com.example.note.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

/**
 * 模板列表适配器
 */
public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> {
    
    public interface OnTemplateClickListener {
        void onTemplateClick(Template template);
    }
    
    private final OnTemplateClickListener listener;
    
    public TemplateAdapter(OnTemplateClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    
    private static final DiffUtil.ItemCallback<Template> DIFF_CALLBACK = new DiffUtil.ItemCallback<Template>() {
        @Override
        public boolean areItemsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                   oldItem.getDescription().equals(newItem.getDescription()) &&
                   oldItem.getRows() == newItem.getRows() &&
                   oldItem.getCols() == newItem.getCols() &&
                   oldItem.isSystem() == newItem.isSystem() &&
                   oldItem.getCreatedAt() == newItem.getCreatedAt();
        }
    };
    
    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_template, parent, false);
        return new TemplateViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Template template = getItem(position);
        holder.bind(template, listener);
    }
    
    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView iconImageView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final TextView sizeTextView;
        private final TextView typeTextView;
        private final TextView dateTextView;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            iconImageView = itemView.findViewById(R.id.icon_image_view);
            nameTextView = itemView.findViewById(R.id.name_text_view);
            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            sizeTextView = itemView.findViewById(R.id.size_text_view);
            typeTextView = itemView.findViewById(R.id.type_text_view);
            dateTextView = itemView.findViewById(R.id.date_text_view);
        }
        
        public void bind(Template template, OnTemplateClickListener listener) {
            // 设置模板名称
            nameTextView.setText(template.getName());
            
            // 设置描述
            if (template.getDescription() != null && !template.getDescription().trim().isEmpty()) {
                descriptionTextView.setText(template.getDescription());
                descriptionTextView.setVisibility(View.VISIBLE);
            } else {
                descriptionTextView.setVisibility(View.GONE);
            }
            
            // 设置尺寸
            sizeTextView.setText(template.getRows() + "×" + template.getCols());
            
            // 设置类型标签
            if (template.isSystem()) {
                typeTextView.setText("系统模板");
                typeTextView.setBackgroundResource(R.drawable.bg_chip_system);
                iconImageView.setImageResource(R.drawable.ic_template_system);
            } else {
                typeTextView.setText("我的模板");
                typeTextView.setBackgroundResource(R.drawable.bg_chip_user);
                iconImageView.setImageResource(R.drawable.ic_template_user);
            }
            
            // 设置创建时间
            dateTextView.setText(DateUtils.formatRelativeTime(template.getCreatedAt()));
            
            // 设置点击事件
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });
        }
    }
}