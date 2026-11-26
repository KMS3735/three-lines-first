package com.kms3735.threelinesfirst.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.kms3735.threelinesfirst.R;
import com.kms3735.threelinesfirst.data.AppDatabase;
import com.kms3735.threelinesfirst.data.Notice;
import com.kms3735.threelinesfirst.ui.ResultActivity;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 마감 3일 전과 당일 아침 9시에 알림을 띄운다.
 * 몇 분 늦어도 되므로 정확한 알람 권한(SCHEDULE_EXACT_ALARM) 없이 setWindow로 15분 안에 울리게 예약한다.
 * (그냥 set이나 setAndAllowWhileIdle은 남은 시간의 75%까지 늦어질 수 있어, 2주 뒤 알림이 열흘 넘게 밀린다)
 * 휴대폰을 다시 켜면 예약이 사라지므로 BOOT_COMPLETED를 받아 저장된 안내문으로 다시 예약한다.
 */
public class DeadlineAlarm extends BroadcastReceiver {

    private static final String ACTION_ALARM = "com.kms3735.threelinesfirst.DEADLINE_ALARM";
    private static final String EXTRA_NOTICE_ID = "notice_id";
    private static final String EXTRA_DAYS_LEFT = "days_left";
    private static final String CHANNEL_ID = "deadline";
    /** 마감 며칠 전에 알릴지. 순서가 PendingIntent 요청 코드가 된다 */
    private static final int[] DAYS_BEFORE = {3, 0};
    private static final int HOUR = 9;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    /** 요약을 저장한 뒤 부른다. 이미 지난 시각은 건너뛴다 */
    public static void schedule(Context context, Notice n) {
        cancel(context, n.id);
        if (n.deadline == null || n.deadline.isEmpty()) return;
        LocalDate deadline = LocalDate.parse(n.deadline);
        AlarmManager am = context.getSystemService(AlarmManager.class);
        long now = System.currentTimeMillis();
        for (int i = 0; i < DAYS_BEFORE.length; i++) {
            long at = deadline.minusDays(DAYS_BEFORE[i]).atTime(HOUR, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (at <= now) continue;
            am.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pending(context, n.id, i, DAYS_BEFORE[i]));
        }
    }

    /** 안내문을 지우면 예약도 지운다 */
    public static void cancel(Context context, long noticeId) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        for (int i = 0; i < DAYS_BEFORE.length; i++) {
            am.cancel(pending(context, noticeId, i, DAYS_BEFORE[i]));
        }
    }

    private static PendingIntent pending(Context context, long noticeId, int index, int daysLeft) {
        Intent intent = new Intent(context, DeadlineAlarm.class)
                .setAction(ACTION_ALARM)
                .putExtra(EXTRA_NOTICE_ID, noticeId)
                .putExtra(EXTRA_DAYS_LEFT, daysLeft);
        int requestCode = (int) noticeId * DAYS_BEFORE.length + index;
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action);
        if (!boot && !ACTION_ALARM.equals(action)) return;

        // DB는 메인 스레드에서 읽지 않는다. goAsync로 리시버를 잠시 살려 둔다
        PendingResult result = goAsync();
        Context app = context.getApplicationContext();
        long noticeId = intent.getLongExtra(EXTRA_NOTICE_ID, -1);
        int daysLeft = intent.getIntExtra(EXTRA_DAYS_LEFT, 0);
        new Thread(() -> {
            try {
                if (boot) {
                    for (Notice n : AppDatabase.get(app).noticeDao().getWithDeadline()) schedule(app, n);
                } else {
                    // 알림 글은 인텐트가 아니라 DB에서 읽는다. 지운 안내문이면 띄우지 않는다
                    Notice n = AppDatabase.get(app).noticeDao().get(noticeId);
                    if (n != null && n.isSummarized()) notify(app, n, daysLeft);
                }
            } finally {
                result.finish();
            }
        }).start();
    }

    @SuppressLint("MissingPermission") // Android 13 이상은 바로 위에서 권한을 확인함
    private static void notify(Context context, Notice n, int daysLeft) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.alarm_channel), NotificationManager.IMPORTANCE_HIGH));

        Intent open = new Intent(context, ResultActivity.class)
                .putExtra(ResultActivity.EXTRA_NOTICE_ID, n.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent tap = PendingIntent.getActivity(context, (int) n.id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = daysLeft > 0
                ? context.getString(R.string.alarm_days_left, daysLeft)
                : context.getString(R.string.alarm_today);
        String body = n.line1What + "\n" + n.line2Todo;
        NotificationManagerCompat.from(context).notify((int) n.id, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(n.line1What)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build());
    }
}
