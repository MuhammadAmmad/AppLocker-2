package com.eeontheway.android.applocker.main;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.lock.BaseLockCondition;
import com.eeontheway.android.applocker.lock.GpsLockCondition;
import com.eeontheway.android.applocker.lock.LockConfigManager;
import com.eeontheway.android.applocker.lock.TimeLockCondition;

/**
 * 反馈列表RecyleView的适配器
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
class LockConditionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM_TYPE_TIME = 0;
    private static final int ITEM_TYPE_GPS = 1;

    private Context context;
    private LockConfigManager lockConfigManager;
    private ItemSelectedListener listener;

    /**
     * Adapter的构造函数
     * @param context 上下文
     * @param manager 锁定配置管理器
     */
    public LockConditionAdapter(Context context, LockConfigManager manager) {
        this.context = context;
        lockConfigManager = manager;
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
        return lockConfigManager.selectedLockCondition() > 0;
    }

    /**
     * 移除任何不锁定的APP
     * @return 移除的数量
     */
    public int removeSelectedLockConfig () {
        return lockConfigManager.deleteSelectedLockCondition();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_TIME) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_time_lock_config, parent, false);
            return new ItemTimeViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_postion_lock_config, parent, false);
            return new ItemPostionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BaseLockCondition config = lockConfigManager.getLockCondition(position);

        if (config instanceof TimeLockCondition) {
            TimeLockCondition timeLockCondition = (TimeLockCondition)config;
            ItemTimeViewHolder viewHolder = (ItemTimeViewHolder)holder;

            viewHolder.tv_start_time.setText(
                    context.getString(R.string.start_time, timeLockCondition.getStartTime()));
            viewHolder.tv_end_time.setText(
                    context.getString(R.string.end_time, timeLockCondition.getEndTime()));
            viewHolder.tv_day.setText(timeLockCondition.getDayString(context));
            viewHolder.cb_enable.setChecked(timeLockCondition.isEnable());
            viewHolder.cb_selected.setChecked(timeLockCondition.isSelected());
        } else {
            GpsLockCondition gpsLockCondition = (GpsLockCondition)config;
            ItemPostionViewHolder viewHolder = (ItemPostionViewHolder)holder;

            viewHolder.cb_enable.setChecked(gpsLockCondition.isEnable());
            viewHolder.cb_selected.setChecked(gpsLockCondition.isSelected());
        }
    }

    @Override
    public int getItemCount() {
        return lockConfigManager.getLockConditionCount();
    }


    @Override
    public int getItemViewType(int position) {
        BaseLockCondition config = lockConfigManager.getLockCondition(position);
        if (config instanceof TimeLockCondition) {
            return ITEM_TYPE_TIME;
        } else {
            return ITEM_TYPE_GPS;
        }
    }

    /**
     * 某个项选中事件
     */
    interface ItemSelectedListener {
        void onItemClicked (int pos);
        void onItemLongClicked (int pos);
        void onItemSelected (int pos, boolean selected);
    }

    /**
     * 时间配置的ViewHolder
     */
    class ItemTimeViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cb_enable;
        private TextView tv_start_time;
        private TextView tv_end_time;
        private TextView tv_day;
        private View ll_item;
        private CheckBox cb_selected;

        public ItemTimeViewHolder(final View itemView) {
            super(itemView);

            cb_enable = (CheckBox) itemView.findViewById(R.id.cb_enable);
            tv_start_time = (TextView)itemView.findViewById(R.id.tv_start_time);
            tv_end_time = (TextView)itemView.findViewById(R.id.tv_end_time);
            tv_day = (TextView)itemView.findViewById(R.id.tv_day);
            ll_item = itemView.findViewById(R.id.ll_item);
            cb_selected = (CheckBox)itemView.findViewById(R.id.cb_selected);

            // 选中按钮点击事件
            cb_selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int pos = getAdapterPosition();
                    BaseLockCondition config = lockConfigManager.getLockCondition(pos);
                    config.setSelected(isChecked);

                    if (listener != null) {
                        listener.onItemSelected(pos, isChecked);
                    }
                }
            });

            // 普通点击事件，切换锁定使能
            ll_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();

                    // 切换使能状态
                    BaseLockCondition oldConfig = lockConfigManager.getLockCondition(pos);
                    BaseLockCondition newConfig = (BaseLockCondition)oldConfig.clone();
                    newConfig.setEnable(!oldConfig.isEnable());
                    lockConfigManager.updateLockCondition(oldConfig, newConfig);

                    if (listener != null) {
                        listener.onItemClicked(getAdapterPosition());
                    }
                }
            });

            // 长点击事件
            ll_item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener != null) {
                        listener.onItemLongClicked(getAdapterPosition());
                    }
                    return false;
                }
            });
        }
    }

    /**
     * 位置信息的ViewHolder
     */
    class ItemPostionViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cb_enable;
        private View ll_item;
        private CheckBox cb_selected;

        public ItemPostionViewHolder(View itemView) {
            super(itemView);

            cb_enable = (CheckBox) itemView.findViewById(R.id.cb_enable);
            ll_item = itemView.findViewById(R.id.ll_item);
            cb_selected = (CheckBox)itemView.findViewById(R.id.cb_selected);
        }
    }
}