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
import com.kms3735.threelinesfirst.util.KeepWords;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** ① 안내문 목록의 RecyclerView 어댑터. 한 칸에 마감 날짜 칸, 무슨 안내문인지, 남은 날, 할 일 진행을 보여 준다 */
public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeHolder> {

    /** 이 날 수 이하로 남으면 마감 임박(주황) */
    static final int URGENT_DAYS = 3;

    public interface OnNoticeClick {
        void onClick(Notice notice);
    }

    static class NoticeHolder extends RecyclerView.ViewHolder {
        final View dateTile;
        final TextView month, day, title, deadline, info;

        NoticeHolder(@NonNull View itemView) {
            super(itemView);
            dateTile = itemView.findViewById(R.id.dateTile);
            month = itemView.findViewById(R.id.txtMonth);
            day = itemView.findViewById(R.id.txtDay);
            title = itemView.findViewById(R.id.txtTitle);
            deadline = itemView.findViewById(R.id.txtDeadline);
            info = itemView.findViewById(R.id.txtInfo);
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

    /** 마감까지 남은 날 (지났으면 음수). 아직 요약 전이거나 마감일이 없으면 null */
    static Long daysLeft(Notice n) {
        if (!n.isSummarized() || n.deadline == null || n.deadline.isEmpty()) return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(n.deadline));
    }

    /** 남은 날 문구: "2일 남았어요", "오늘까지예요", "마감이 지났어요" */
    static String leftText(Context c, long left) {
        if (left > 0) return c.getString(R.string.deadline_left, left);
        if (left == 0) return c.getString(R.string.deadline_today);
        return c.getString(R.string.deadline_passed);
    }

    /** 남은 날에 맞는 글자색 리소스: 사흘 안이면 주황, 넉넉하면 파랑, 지났으면 회색 */
    private static int toneText(long left) {
        if (left < 0) return R.color.tone_gray_text;
        return left <= URGENT_DAYS ? R.color.warning_text : R.color.tone_blue_text;
    }

    private static int toneBg(long left) {
        if (left < 0) return R.color.tone_gray_bg;
        return left <= URGENT_DAYS ? R.color.warning_bg : R.color.tone_blue_bg;
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
        int sub = ContextCompat.getColor(context, R.color.sub);

        if (!n.isSummarized()) {
            holder.dateTile.setVisibility(View.GONE);
            holder.title.setText(R.string.list_not_summarized);
            holder.deadline.setText(R.string.list_tap_to_retry);
            holder.deadline.setTextColor(sub);
            holder.info.setVisibility(View.GONE);
            return;
        }

        holder.title.setText(KeepWords.of(n.line1What));

        Long left = daysLeft(n);
        if (left == null) {
            holder.dateTile.setVisibility(View.GONE);
            holder.deadline.setText(R.string.deadline_none);
            holder.deadline.setTextColor(sub);
        } else {
            LocalDate d = LocalDate.parse(n.deadline);
            int text = ContextCompat.getColor(context, toneText(left));
            holder.dateTile.setVisibility(View.VISIBLE);
            holder.dateTile.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, toneBg(left))));
            holder.month.setText(context.getString(R.string.month, d.getMonthValue()));
            holder.month.setTextColor(text);
            holder.day.setText(String.valueOf(d.getDayOfMonth()));
            holder.day.setTextColor(text);
            holder.deadline.setText(leftText(context, left));
            holder.deadline.setTextColor(text);
        }

        // 원문과 다른 값이 있으면 그 경고를, 아니면 할 일 진행을 보여 준다
        if (n.mismatches != null && !n.mismatches.isEmpty()) {
            holder.info.setText(R.string.list_mismatch);
            holder.info.setTextColor(ContextCompat.getColor(context, R.color.warning_text));
            holder.info.setVisibility(View.VISIBLE);
        } else if (item.todoTotal > 0) {
            holder.info.setText(context.getString(R.string.todo_progress, item.todoTotal, item.todoDone));
            holder.info.setTextColor(sub);
            holder.info.setVisibility(View.VISIBLE);
        } else {
            holder.info.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
