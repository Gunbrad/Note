package com.example.note.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.note.R;

/**
 * 关于页面Activity
 * 显示应用信息、开发者信息等
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // 设置工具栏
        setupToolbar();
        
        // 设置邮箱点击事件
        setupEmailClick();
    }

    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("关于");
        }
    }

    /**
     * 设置邮箱点击事件
     */
    private void setupEmailClick() {
        TextView emailTextView = findViewById(R.id.tv_email);
        if (emailTextView != null) {
            emailTextView.setOnClickListener(v -> {
                try {
                    // 创建邮件Intent
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:loserben1314@gmail.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "关于民宿表格笔记APP");
                    
                    // 检查是否有邮件应用可以处理这个Intent
                    if (emailIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(emailIntent);
                    } else {
                        // 如果没有邮件应用，显示提示信息
                        Toast.makeText(this, "未找到邮件应用，请手动发送邮件至：loserben1314@gmail.com", 
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    // 处理异常情况
                    Toast.makeText(this, "无法打开邮件应用", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理工具栏返回按钮点击
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}