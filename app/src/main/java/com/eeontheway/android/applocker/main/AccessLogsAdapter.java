package com.eeontheway.android.applocker.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.lock.LockConfigManager;
import com.eeontheway.android.applocker.lock.AccessLog;

import java.io.IOException;

/**
 * 日志列表的RecyleView的适配器
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
class AccessLogsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int LOAD_MORE_COUNT = 20;

    private Context context;
    private LockConfigManager lockConfigManager;
    private ItemSelectedListener listener;

    /**
     * Adapter的构造函数
     * @param context 上下文
     * @param manager 锁定配置管理器
     */
    public AccessLogsAdapter(Context context, LockConfigManager manager) {
        this.context = context;
        lockConfigManager = manager;
    }

    /**
     * 加载拍摄的照片
     * @param logInfo 日志信息
     * @return 拍摄的照片
     */
    public Bitmap loadPhoto (AccessLog logInfo) {
        Bitmap bitmap = null;

        if (logInfo.getPhotoPath() == null) {
            return bitmap;
        }

        if (logInfo.isPhotoInInternal()) {
            // 加载内部图像文件
            bitmap = BitmapFactory.decodeFile(logInfo.getPhotoPath());
        } else {
            // 加载相册中的文件
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(),
                        Uri.parse(logInfo.getPhotoPath()));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // 如果相册中的图像已经被删除，则加载缺省的图像
                if (bitmap == null) {
                    bitmap = BitmapFactory.decodeResource(context.getResources(),
                                                                R.drawable.icon_person);
                }
            }
        }

        return bitmap;
    }

    /**
     * 设置点击的回调处理器
     * @param listener 回调处理器
     */
    public void setItemSelectedListener (ItemSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * 检查是否有任何App被选中
     * @return true/false
     */
    public boolean isAnyItemSelected () {
        return lockConfigManager.selectedAccessLogs() > 0;
    }

    /**
     * 移除任何选中的访问日志
     * @return 移除的数量
     */
    public int removeSelectedAccessLogs () {
        return lockConfigManager.deleteSelectedAccessLogs();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_app_lock_log_list, parent, false);
        return new ItemTimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AccessLog logInfo = lockConfigManager.getAccessLog(position);
        ItemTimeViewHolder viewHolder = (ItemTimeViewHolder)holder;

        viewHolder.cb_selected.setChecked(logInfo.isSelected());
        viewHolder.iv_photo.setImageBitmap(loadPhoto(logInfo));
        viewHolder.tv_appName.setText(logInfo.getAppName());
        viewHolder.tv_time.setText(logInfo.getTime());
        viewHolder.tv_error_count.setText(context.getString(R.string.password_err_counter,
                                                        logInfo.getPasswordErrorCount()));
    }

    @Override
    public int getItemCount() {
        return lockConfigManager.getAcessLogsCount();
    }

    /**
     * 某个项选中事件
     */
    interface ItemSelectedListener {
        void onItemClicked(int pos);
        void onItemLongClicked(int pos);
        void onItemSelected(int pos, boolean selected);
    }

    /**
     * 时间配置的ViewHolder
     */
    class ItemTimeViewHolder extends RecyclerView.ViewHolder {
        public CheckBox cb_selected;
        public ImageView iv_photo;
        public TextView tv_appName;
        public TextView tv_time;
        public TextView tv_error_count;

        public ItemTimeViewHolder(final View itemView) {
            super(itemView);

            cb_selected = (CheckBox) itemView.findViewById(R.id.cb_selected);
            iv_photo = (ImageView) itemView.findViewById(R.id.iv_photo);
            tv_appName = (TextView) itemView.findViewById(R.id.tv_appName);
            tv_time = (TextView) itemView.findViewById(R.id.tv_time);
            tv_error_count = (TextView) itemView.findViewById(R.id.tv_error_count);
        }
    }
}