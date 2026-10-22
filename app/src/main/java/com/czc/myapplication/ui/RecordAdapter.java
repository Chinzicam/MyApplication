package com.czc.myapplication.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.czc.myapplication.R;
import com.czc.myapplication.data.CategoryDao;
import com.czc.myapplication.data.Record;
import com.czc.myapplication.util.DateUtils;
import com.czc.myapplication.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

/** 今日流水列表（简单条目） */
public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(Record record);
    }

    private final List<Record> mList = new ArrayList<>();
    private final CategoryDao mCategoryDao;
    private OnItemClickListener mListener;

    public RecordAdapter(CategoryDao categoryDao) {
        this.mCategoryDao = categoryDao;
    }

    public void setData(List<Record> data) {
        mList.clear();
        if (data != null) mList.addAll(data);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Record r = mList.get(position);
        holder.tvCat.setText(mCategoryDao.getName(r.categoryId));
        String remark = r.remark == null ? "" : r.remark.trim();
        holder.tvRemark.setVisibility(remark.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvRemark.setText(remark);
        holder.tvTime.setText(DateUtils.formatTime(r.recordTime));
        String amount = (r.type == Record.TYPE_INCOME ? "+" : "-") + MoneyUtils.format(r.amountCents);
        holder.tvAmount.setText(amount);
        holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(
                r.type == Record.TYPE_INCOME ? R.color.income : R.color.expense));
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) mListener.onItemClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCat, tvRemark, tvTime, tvAmount;
        VH(@NonNull View itemView) {
            super(itemView);
            tvCat = itemView.findViewById(R.id.tv_cat_name);
            tvRemark = itemView.findViewById(R.id.tv_remark);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }
    }
}
