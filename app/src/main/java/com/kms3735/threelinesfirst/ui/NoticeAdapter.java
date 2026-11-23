package com.kms3735.threelinesfirst.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.data.Notice;
import com.kms3735.threelinesfirst.data.NoticeItem;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** ① 안내문 목록의 RecyclerView 어댑터. 한 칸에 무슨 안내문인지, 마감까지 남은 날, 할 일 진행을 보여 준다 */
public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeHolder> {

    public interface OnNoticeClick {
        void onClick(Notice notice);
    }

    static class NoticeHolder extends RecyclerView.ViewHolder {
        final TextView title, deadline, info;
        /** 테마 기본 글자색. 주황으로 바꿨던 칸을 다시 쓸 때 되돌린다 */
        final ColorStateList normal;

        NoticeHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtTitle);
            deadline = itemView.findViewById(R.id.txtDeadline);
            info = itemView.findViewById(R.id.txtInfo);
            normal = deadline.getTextColors();
        }
    }

    private final Context context;
    private final OnNoticeClick onClick;
    private List<NoticeItem> items = new ArrayList<>();

    public NoticeAdapter(Context context, OnNoticeClick onClick) {
        this.context = context;
        this.onClick = onClick;
    }

    public void setItems(List<NoticeItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoticeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notice, parent, false);
        return new NoticeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeHolder holder, int position) {
        NoticeItem item = items.get(position);
        Notice n = item.notice;
        holder.itemView.setOnClickListener(v -> onClick.onClick(n));

        ColorStateList normal = holder.normal;
        ColorStateList urgent = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.warning_text));

        if (!n.isSummarized()) {
            holder.title.setText(R.string.list_not_summarized);
            holder.deadline.setText(R.string.list_tap_to_retry);
            holder.deadline.setTextColor(normal);
            holder.info.setVisibility(View.GONE);
            return;
        }

        holder.title.setText(n.line1What);

        // 마감까지 남은 날. 사흘 안이거나 지났으면 주황색으로 눈에 띄게
        if (n.deadline == null || n.deadline.isEmpty()) {
            holder.deadline.setText(R.string.deadline_none);
            holder.deadline.setTextColor(normal);
        } else {
            LocalDate d = LocalDate.parse(n.deadline);
            long left = ChronoUnit.DAYS.between(LocalDate.now(), d);
            String date = d.getMonthValue() + "월 " + d.getDayOfMonth() + "일";
            if (left > 0) holder.deadline.setText(context.getString(R.string.deadline_left, date, left));
            else if (left == 0) holder.deadline.setText(context.getString(R.string.deadline_today, date));
            else holder.deadline.setText(context.getString(R.string.deadline_passed, date));
            holder.deadline.setTextColor(left <= 3 ? urgent : normal);
        }

        // 할 일 진행, 원문 대조 경고
        StringBuilder info = new StringBuilder();
        if (item.todoTotal > 0) info.append(context.getString(R.string.todo_progress, item.todoTotal, item.todoDone));
        if (n.mismatches != null && !n.mismatches.isEmpty()) {
            if (info.length() > 0) info.append('\n');
            info.append(context.getString(R.string.list_mismatch));
        }
        holder.info.setText(info);
        holder.info.setVisibility(info.length() > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
