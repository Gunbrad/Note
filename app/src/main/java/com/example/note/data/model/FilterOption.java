package com.example.note.data.model;

/**
 * 供 UI 使用的筛选候选项
 * 根据修改建议.md的要求实现
 */
public final class FilterOption {
    public final String value;     // 实际值；空字符串代表空值
    public final String display;   // 显示文本，空值显示为 "（空）"
    public final int count;        // 计数
    public boolean checked;        // 勾选状态（UI 层可变）
    
    public FilterOption(String v, int c, boolean chk) {
        this.value = v == null ? "" : v;
        this.display = (this.value.isEmpty() ? "（空）" : this.value);
        this.count = c;
        this.checked = chk;
    }
    
    /**
     * 便捷构造函数，默认为选中状态
     */
    public FilterOption(String value, int count) {
        this(value, count, true);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FilterOption that = (FilterOption) obj;
        return value.equals(that.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public String toString() {
        return "FilterOption{" +
                "value='" + value + '\'' +
                ", display='" + display + '\'' +
                ", count=" + count +
                ", checked=" + checked +
                '}';
    }
}