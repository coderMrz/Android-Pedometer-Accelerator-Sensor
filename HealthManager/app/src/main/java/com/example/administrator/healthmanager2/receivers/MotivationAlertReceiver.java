package com.example.administrator.healthmanager2.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.example.administrator.healthmanager2.Factory;
import com.example.administrator.healthmanager2.R;
import com.example.administrator.healthmanager2.persistence.StepCountPersistenceHelper;
import com.example.administrator.healthmanager2.services.AbstractStepDetectorService;
import com.example.administrator.healthmanager2.utils.StepDetectionServiceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2017/3/15.
 */

public class MotivationAlertReceiver extends WakefulBroadcastReceiver {
    public static final int NOTIFICATION_ID = 0;
    private static final String LOG_CLASS = MotivationAlertReceiver.class.getName();
    private Context context;
    private AbstractStepDetectorService.StepDetectorBinder myBinder = null;


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (AbstractStepDetectorService.StepDetectorBinder) service;
            motivate();

            context.getApplicationContext().unbindService(mServiceConnection);
        }
    };
    private static int flag;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_CLASS, "Motivate the user!");

        this.context = context;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        float criterion = Float.parseFloat(sharedPref.getString(context.getString(R.string.pref_notification_motivation_alert_criterion), "100"));
        if (criterion < 0 || criterion > 100) {
            Log.e(LOG_CLASS, "Invalid motivation criterion. Cannot notify the user.");
            return;
        }
        if ("alarm0".equals(intent.getAction())) {
            flag=0;
        }

        if ("alarm1".equals(intent.getAction())) {
            flag=1;
        }

        if ("alarm2".equals(intent.getAction())) {
            flag=2;
        }
        // bind to service
        Intent serviceIntent = new Intent(context, Factory.getStepDetectorServiceClass(context.getPackageManager()));
        context.getApplicationContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);


    }

    /**
     * Shows the motivation notification to user
     */
    private void motivate() {


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        float criterion = Float.parseFloat(sharedPref.getString(context.getString(R.string.pref_notification_motivation_alert_criterion), "100"));
        int stepCount = StepCountPersistenceHelper.getStepCountForDay(Calendar.getInstance(), context);
        if (myBinder != null) {
            stepCount += myBinder.stepsSinceLastSave();
        } else {
            Log.w(LOG_CLASS, "Cannot get steps from binder.");
        }
        int dailyGoal = Integer.parseInt(sharedPref.getString(context.getString(R.string.pref_daily_step_goal), "100"));
        if (dailyGoal * criterion / 100 <= stepCount) {
            Log.i(LOG_CLASS, "No motivation required.");
            // Reschedule alarm for tomorrow
            StepDetectionServiceHelper.startAllIfEnabled(context);
            return;
        }
        Set<String> defaultStringSet;
        if(flag==0) {
            defaultStringSet = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_default_notification_motivation_alert_messages)));
            List<String> motivationTexts = new ArrayList<>(sharedPref.getStringSet(context.getString(R.string.pref_notification_motivation_alert_texts),  defaultStringSet));
            if (motivationTexts.size() == 0) {
                Log.e(LOG_CLASS, "Motivation texts are empty. Cannot notify the user.");
                // Reschedule alarm for tomorrow
                StepDetectionServiceHelper.startAllIfEnabled(context);
                return;
            }

            Collections.shuffle(motivationTexts);
            String motivationText = motivationTexts.get(0);

            // Build the notification
            NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context.getApplicationContext())
                    .setSmallIcon(R.drawable.ic_walk_black_24dp)
                    .setContentTitle(context.getString(R.string.motivation_alert_notification_title))
                    .setContentText(motivationText)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setLights(ContextCompat.getColor(context, R.color.colorPrimary), 1000, 1000);
            Notification notification = mBuilder.build();
            /**
             * vibrate属性是一个长整型的数组，用于设置手机静止和振动的时长，以毫秒为单位。
             * 参数中下标为0的值表示手机静止的时长，下标为1的值表示手机振动的时长， 下标为2的值又表示手机静止的时长，以此类推。
             */
            long[] vibrates = { 0, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
            notification.vibrate = vibrates;

            /**
             * 手机处于锁屏状态时， LED灯就会不停地闪烁， 提醒用户去查看手机,下面是绿色的灯光一 闪一闪的效果
             */
            notification.ledARGB = Color.GREEN;// 控制 LED 灯的颜色，一般有红绿蓝三种颜色可选
            notification.ledOnMS = 1000;// 指定 LED 灯亮起的时长，以毫秒为单位
            notification.ledOffMS = 1000;// 指定 LED 灯暗去的时长，也是以毫秒为单位
            notification.flags = Notification.FLAG_SHOW_LIGHTS;// 指定通知的一些行
            // Notify
            notificationManager.notify(NOTIFICATION_ID, notification);
            StepDetectionServiceHelper.startAllIfEnabled(context);
        }
        if(flag==1) {
            defaultStringSet = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_default_notification_motivation_alert_messages1)));
            List<String> motivationTexts = new ArrayList<>(sharedPref.getStringSet(context.getString(R.string.pref_notification_motivation_alert_texts),  defaultStringSet));
            if (motivationTexts.size() == 0) {
                Log.e(LOG_CLASS, "Motivation texts are empty. Cannot notify the user.");
                // Reschedule alarm for tomorrow
                StepDetectionServiceHelper.startAllIfEnabled(context);
                return;
            }

            Collections.shuffle(motivationTexts);
            String motivationText = motivationTexts.get(0);

            // Build the notification
            NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context.getApplicationContext())
                    .setSmallIcon(R.drawable.ic_walk_black_24dp)
                    .setContentTitle(context.getString(R.string.motivation_alert_notification_title))
                    .setContentText(motivationText)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setLights(ContextCompat.getColor(context, R.color.colorPrimary), 1000, 1000);
            Notification notification = mBuilder.build();
            /**
             * vibrate属性是一个长整型的数组，用于设置手机静止和振动的时长，以毫秒为单位。
             * 参数中下标为0的值表示手机静止的时长，下标为1的值表示手机振动的时长， 下标为2的值又表示手机静止的时长，以此类推。
             */
            long[] vibrates = { 0, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
            notification.vibrate = vibrates;

            /**
             * 手机处于锁屏状态时， LED灯就会不停地闪烁， 提醒用户去查看手机,下面是绿色的灯光一 闪一闪的效果
             */
            notification.ledARGB = Color.GREEN;// 控制 LED 灯的颜色，一般有红绿蓝三种颜色可选
            notification.ledOnMS = 1000;// 指定 LED 灯亮起的时长，以毫秒为单位
            notification.ledOffMS = 1000;// 指定 LED 灯暗去的时长，也是以毫秒为单位
            notification.flags = Notification.FLAG_SHOW_LIGHTS;// 指定通知的一些行
            // Notify
            notificationManager.notify(NOTIFICATION_ID, notification);
            StepDetectionServiceHelper.startAllIfEnabled(context);
        }

        if(flag==2) {
            defaultStringSet = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_default_notification_motivation_alert_messages2)));
            List<String> motivationTexts = new ArrayList<>(sharedPref.getStringSet(context.getString(R.string.pref_notification_motivation_alert_texts),  defaultStringSet));
            if (motivationTexts.size() == 0) {
                Log.e(LOG_CLASS, "Motivation texts are empty. Cannot notify the user.");
                // Reschedule alarm for tomorrow
                StepDetectionServiceHelper.startAllIfEnabled(context);
                return;
            }

            Collections.shuffle(motivationTexts);
            String motivationText = motivationTexts.get(0);

            // Build the notification
            NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context.getApplicationContext())
                    .setSmallIcon(R.drawable.ic_walk_black_24dp)
                    .setContentTitle(context.getString(R.string.motivation_alert_notification_title))
                    .setContentText(motivationText)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setLights(ContextCompat.getColor(context, R.color.colorPrimary), 1000, 1000);
            Notification notification = mBuilder.build();
            /**
             * vibrate属性是一个长整型的数组，用于设置手机静止和振动的时长，以毫秒为单位。
             * 参数中下标为0的值表示手机静止的时长，下标为1的值表示手机振动的时长， 下标为2的值又表示手机静止的时长，以此类推。
             */
            long[] vibrates = { 0, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
            notification.vibrate = vibrates;

            /**
             * 手机处于锁屏状态时， LED灯就会不停地闪烁， 提醒用户去查看手机,下面是绿色的灯光一 闪一闪的效果
             */
            notification.ledARGB = Color.GREEN;// 控制 LED 灯的颜色，一般有红绿蓝三种颜色可选
            notification.ledOnMS = 1000;// 指定 LED 灯亮起的时长，以毫秒为单位
            notification.ledOffMS = 1000;// 指定 LED 灯暗去的时长，也是以毫秒为单位
            notification.flags = Notification.FLAG_SHOW_LIGHTS;// 指定通知的一些行
            // Notify
            notificationManager.notify(NOTIFICATION_ID, notification);
            StepDetectionServiceHelper.startAllIfEnabled(context);
        }


    }
}
