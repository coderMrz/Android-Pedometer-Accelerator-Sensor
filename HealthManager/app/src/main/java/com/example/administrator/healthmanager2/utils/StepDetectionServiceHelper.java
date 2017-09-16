package com.example.administrator.healthmanager2.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.administrator.healthmanager2.Factory;
import com.example.administrator.healthmanager2.R;
import com.example.administrator.healthmanager2.receivers.MotivationAlertReceiver;
import com.example.administrator.healthmanager2.receivers.StepCountPersistenceReceiver;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Administrator on 2017/3/15.
 */

public class StepDetectionServiceHelper {

    private static final String LOG_CLASS = StepDetectionServiceHelper.class.getName();

    /**
     * Starts the step detection, persistence service and notification service if they are enabled in settings.
     *
     * @param context The application context.
     */
    public static void startAllIfEnabled(Context context) {
        Log.i(LOG_CLASS, "Started all services");
        // Start the step detection if enabled or training is active
        if (isStepDetectionEnabled(context)) {
            StepDetectionServiceHelper.startStepDetection(context);
            // schedule stepCountPersistenceService
            StepDetectionServiceHelper.schedulePersistenceService(context);
        }

        if(isMotivationAlertEnabled(context)){
            // set motivation alert
            setMotivationAlert(context);
            setMotivationAlert1(context);
            setMotivationAlert2(context);
        }
    }

    public static void stopAllIfNotRequired(Context context){
        stopAllIfNotRequired(true, context);
    }

    public static void stopAllIfNotRequired(boolean forceSave, Context context){
        // Start the step detection if enabled or training is active
        if (!isStepDetectionEnabled(context)) {
            Log.i(LOG_CLASS, "Stopping all services");
            StepDetectionServiceHelper.stopStepDetection(context);
            // schedule stepCountPersistenceService
            StepDetectionServiceHelper.cancelPersistenceService(forceSave, context);
        }else{
            Log.i(LOG_CLASS, "Not stopping services b.c. they are required");
        }

        if(!isMotivationAlertEnabled(context)){
            // cancel motivation alert
            cancelMotivationAlert(context);
        }
    }
    /**
     * Starts the step detection service
     *
     * @param context The application context
     */
    public static void startStepDetection(Context context) {
        Log.i(LOG_CLASS, "Started step detection service.");
        Intent      stepDetectorServiceIntent = new Intent(context, Factory.getStepDetectorServiceClass(context.getPackageManager()));
        context.getApplicationContext().startService(stepDetectorServiceIntent);
    }

    /**
     * Stops the step detection service
     *
     * @param context The application context
     */
    public static void stopStepDetection(Context context){
        Log.i(LOG_CLASS, "Stopping step detection service.");
        Intent stepDetectorServiceIntent= new Intent(context, Factory.getStepDetectorServiceClass(context.getPackageManager()));
        if(!context.getApplicationContext().stopService(stepDetectorServiceIntent)){
            Log.w(LOG_CLASS, "Stopping of service failed or it is not running.");
        }
    }

    /**
     *  Schedules the step count persistence service.
     *
     * @param context The application context
     */
    public static void schedulePersistenceService(Context context) {
        Intent stepCountPersistenceServiceIntent = new Intent(context, StepCountPersistenceReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 2, stepCountPersistenceServiceIntent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Fire at next half hour
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int unroundedMinutes = calendar.get(Calendar.MINUTE);
        int mod = unroundedMinutes % 30;
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, (30-mod));

        // Set repeating alarm
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTime().getTime(), AlarmManager.INTERVAL_HOUR, sender);
        Log.i(LOG_CLASS, "Scheduled repeating persistence service at start time " + calendar.toString());
    }

    /**
     * Cancel the scheduled persistence service
     * @param forceSave if true the persistence service will be execute now and canceled after
     * @param context The application context
     */
    public static void cancelPersistenceService(boolean forceSave, Context context){
        // force save
        if(forceSave) {
            startPersistenceService(context);
        }
        Intent stepCountPersistenceServiceIntent = new Intent(context, StepCountPersistenceReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 2, stepCountPersistenceServiceIntent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }

    /**
     * Starts the step detection service
     *
     * @param context The application context
     */
    public static void startPersistenceService(Context context) {
        Log.i(LOG_CLASS, "Started persistence service.");
        Intent stepCountPersistenceServiceIntent = new Intent(context, StepCountPersistenceReceiver.class);
        context.sendBroadcast(stepCountPersistenceServiceIntent);
    }

    /**
     * Is the step detection enabled? This could be the case if the permanent step counter or a training
     * session is active
     * @param context The application context
     * @return true if step detection is enabled
     */
    public static boolean isStepDetectionEnabled(Context context) {
        // Get user preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isStepDetectionEnabled = sharedPref.getBoolean(context.getString(R.string.pref_step_counter_enabled), true);
        boolean isWalkingModeLearningActive = sharedPref.getBoolean(context.getString(R.string.pref_walking_mode_learning_active), false);
        return isStepDetectionEnabled ||/* (TrainingPersistenceHelper.getActiveItem(context) != null) ||*/ isWalkingModeLearningActive;
    }

    /**
     * Is the motivation alert notification enabled by user?
     * @param context The application context
     * @return true if enabled
     */
    public static boolean isMotivationAlertEnabled(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(context.getString(R.string.pref_notification_motivation_alert_enabled), true);
    }

    /**
     * Schedules (or updates) the motivation alert notification alarm
     * @param context The application context
     */
   // private static final int INTERVAL = 1000 * 60 * 60 * 24;// 24h
    public static void setMotivationAlert(Context context){


        Intent motivationAlertIntent = new Intent(context, MotivationAlertReceiver.class);
        motivationAlertIntent.setAction("alarm0");
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, motivationAlertIntent, 0);

        long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时间(包括睡眠时间)
        long systemTime = System.currentTimeMillis();

        Calendar calendar =Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // 这里时区需要设置一下，不然会有8个小时的时间差
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 29);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long selectTime = calendar.getTimeInMillis();
        // 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
        if(systemTime > selectTime) {
        //    Toast.makeText(MainActivity.this,"设置的时间小于当前时间", Toast.LENGTH_SHORT).show();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            selectTime = calendar.getTimeInMillis();
        }
// 计算现在时间到设定时间的时间差
        long time = selectTime - systemTime;
        firstTime += time;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
                AlarmManager.INTERVAL_DAY, sender);

    }
    /**
     * Schedules (or updates) the motivation alert notification alarm
     * @param context The application context
     */
    // private static final int INTERVAL = 1000 * 60 * 60 * 24;// 24h
    public static void setMotivationAlert1(Context context){


        Intent motivationAlertIntent = new Intent(context, MotivationAlertReceiver.class);
        motivationAlertIntent.setAction("alarm1");
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, motivationAlertIntent, 0);

        long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时间(包括睡眠时间)
        long systemTime = System.currentTimeMillis();

        Calendar calendar =Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // 这里时区需要设置一下，不然会有8个小时的时间差
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long selectTime = calendar.getTimeInMillis();
        // 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
        if(systemTime > selectTime) {
            //    Toast.makeText(MainActivity.this,"设置的时间小于当前时间", Toast.LENGTH_SHORT).show();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            selectTime = calendar.getTimeInMillis();
        }
// 计算现在时间到设定时间的时间差
        long time = selectTime - systemTime;
        firstTime += time;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
                AlarmManager.INTERVAL_DAY, sender);

    }

    /**
     * Schedules (or updates) the motivation alert notification alarm
     * @param context The application context
     */
    // private static final int INTERVAL = 1000 * 60 * 60 * 24;// 24h
    public static void setMotivationAlert2(Context context){


        Intent motivationAlertIntent = new Intent(context, MotivationAlertReceiver.class);
        motivationAlertIntent.setAction("alarm2");
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, motivationAlertIntent, 0);

        long firstTime = SystemClock.elapsedRealtime(); // 开机之后到现在的运行时间(包括睡眠时间)
        long systemTime = System.currentTimeMillis();

        Calendar calendar =Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // 这里时区需要设置一下，不然会有8个小时的时间差
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long selectTime = calendar.getTimeInMillis();
        // 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
        if(systemTime > selectTime) {
            //    Toast.makeText(MainActivity.this,"设置的时间小于当前时间", Toast.LENGTH_SHORT).show();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            selectTime = calendar.getTimeInMillis();
        }
// 计算现在时间到设定时间的时间差
        long time = selectTime - systemTime;
        firstTime += time;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
                AlarmManager.INTERVAL_DAY, sender);

    }
    /**
     * Cancels the motivation alert (if any)
     * @param context The application context
     */
    public static void cancelMotivationAlert(Context context){
        Log.i(LOG_CLASS, "Canceling motivation alert alarm");
        Intent motivationAlertIntent = new Intent(context, MotivationAlertReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 1, motivationAlertIntent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }
}