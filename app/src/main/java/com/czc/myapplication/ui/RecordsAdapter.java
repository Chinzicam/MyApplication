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
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水页列表：按日期分组，组头显示日期 + 当日小计。
 * 组头与条目共用同一列表，由 getItemViewType 区分。
 */
public class RecordsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnItemClickListener {
        void onItemClick(Record record);
    }

    private final List<Object> mItems = new ArrayList<>(); // Header 或 Record
    private final CategoryDao mCategoryDao;
    private OnItemClickListener mListener;

    public static class Header {
        public long dayStartMillis;
        public String dateLabel;
        public String weekdayLabel;
        public long incomeCents;
        public long expenseCents;
    }

    public RecordsAdapter(CategoryDao categoryDao) {
        this.mCategoryDao = categoryDao;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    /** 根据记录列表重建分组结构（记录必须已按时间倒序） */
    public void setData(List<Record> records) {
        mItems.clear();
        if (records == null || records.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        Map<Long, Header> headerMap = new LinkedHashMap<>();
        for (Record r : records) {
            long day = DateUtils.startOfDay(r.recordTime);
            Header h = headerMap.get(day);
            if (h == null) {
                h = new Header();
                h.dayStartMillis = day;
                h.dateLabel = DateUtils.formatDate(day);
                h.weekdayLabel = DateUtils.formatWeekday(day);
                headerMap.put(day, h);
            }
            if (r.type == Record.TYPE_INCOME) h.incomeCents += r.amountCents;
            else h.expenseCents += r.amountCents;
        }
        // 按日期倒序输出（records 已倒序，LinkedHashMap 保持首见顺序即倒序）
        for (Map.Entry<Long, Header> e : headerMap.entrySet()) {
            mItems.add(e.getValue());
        }
        // 再按倒序遍历记录插入到对应组头之后：直接顺序插入即可（记录本身倒序，组头顺序与记录一致）
        mItems.clear();
        Map<Long, Integer> headerIndex = new HashMap<>();
        for (Record r : records) {
            long day = DateUtils.startOfDay(r.recordTime);
            Integer idx = headerIndex.get(day);
            if (idx == null) {
                Header h = headerMap.get(day);
                mItems.add(h);
                headerIndex.put(day, mItems.size());
            }
            mItems.add(r);
        }
        notifyDataSetChanged();
    }

    /** 追加分页数据（保留已有分组结构重建） */
    public void appendData(List<Record> more) {
        if (more == null || more.isEmpty()) return;
        List<Record> all = new ArrayList<>();
        for (Object o : mItems) {
            if (o instanceof Record) all.add((Record) o);
        }
        all.addAll(more);
        setData(all);
    }

    public int itemCount() {
        int n = 0;
        for (Object o : mItems) if (o instanceof Record) n++;
        return n;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position) instanceof Header ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(inflater.inflate(R.layout.item_record_header, parent, false));
        }
        return new ItemVH(inflater.inflate(R.layout.item_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object o = mItems.get(position);
        if (holder instanceof HeaderVH) {
            Header h = (Header) o;
            HeaderVH hh = (HeaderVH) holder;
            hh.tvDate.setText(h.dateLabel + " " + h.weekdayLabel);
            StringBuilder sub = new StringBuilder();
            if (h.incomeCents > 0) sub.append("收 ").append(MoneyUtils.format(h.incomeCents)).append("  ");
            if (h.expenseCents > 0) sub.append("支 ").append(MoneyUtils.format(h.expenseCents));
            hh.tvSubtotal.setText(sub.toString());
        } else {
            Record r = (Record) o;
            ItemVH vh = (ItemVH) holder;
            vh.tvCat.setText(mCategoryDao.getName(r.categoryId));
            String remark = r.remark == null ? "" : r.remark.trim();
            vh.tvRemark.setVisibility(remark.isEmpty() ? View.GONE : View.VISIBLE);
            vh.tvRemark.setText(remark);
            vh.tvTime.setText(DateUtils.formatDateTime(r.recordTime));
            String amount = (r.type == Record.TYPE_INCOME ? "+" : "-") + MoneyUtils.format(r.amountCents);
            vh.tvAmount.setText(amount);
            vh.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(
                    r.type == Record.TYPE_INCOME ? R.color.income : R.color.expense));
            vh.itemView.setOnClickListener(v -> {
                if (mListener != null) mListener.onItemClick(r);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvDate, tvSubtotal;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_group_date);
            tvSubtotal = itemView.findViewById(R.id.tv_group_subtotal);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        TextView tvCat, tvRemark, tvTime, tvAmount;
        ItemVH(@NonNull View itemView) {
            super(itemView);
            tvCat = itemView.findViewById(R.id.tv_cat_name);
            tvRemark = itemView.findViewById(R.id.tv_remark);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }
    }
}
