package com.reboot.pao_chueh.reboot;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends Activity {
    public static final int FUNCTION_TYPE_REBOOT = 0;
    public static final int FUNCTION_TYPE_SLEEP = 1;
    public static final int FUNCTION_TYPE_SHUTDOWN=2;
    public static final int DEFAULT_DELAY = 10;
    public static final int DEFAULT_COUNTDOWN = 1000;
    public static final String COUNTDOWN_NAME = "countdown";
    public static final String DELAY_NAME = "delay";
    public static final String RUNNING_NAME = "running";
    public static final String FUNCTION_TYPE_NAME = "reboot_type";
    public static final String SETTINGS_NAME = "RebootSettings";
    public static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";
    //	public static final String EXTRA_KEY_CONFIRM = "android.intent.extra.KEY_CONFIRM";
    public static final String SYS_REBOOT_PATH = "/system/bin/reboot";
    public static final String PREFIX_SU = "/system/xbin/su - ";
    public static final String NVTEST_PATH = "/system/bin/nvtest";
    public static final int SLEEP_TIME = 40000;

    private SharedPreferences settings;
    private Handler handler = new Handler();
    private int thermal = 0;

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        Log.d("REBOOT", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupPreference();
        setupRadioButton();
        setupEditText();
        RebootWakeLock.acquireCpuWakeLock(this);

        final ToggleButton tbtn = (ToggleButton) findViewById(R.id.ButtonToggle);
        tbtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                handleRunner(tbtn.isChecked());
            }
        });

        if (settings.getBoolean(RUNNING_NAME, false)
                && settings.getInt(COUNTDOWN_NAME, DEFAULT_COUNTDOWN) > 0) {
            handleRunner(true);
            tbtn.setChecked(true);
        }

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    private void logThermal () {
		/*try {
			Log.d("Reboot", "execute logcat & nvtest");
			Runtime.getRuntime().exec("/system/bin/logcat");
			Process nvtest = Runtime.getRuntime().exec(
					PREFIX_SU + NVTEST_PATH + "  dfs_monitor.so -n 1 -t 0");
			String tcoreLine = Utils.getKeywordOutput(nvtest, "Tcore");
			thermal = Utils.parseThermal(tcoreLine);
			Log.d("Reboot", tcoreLine);
			Log.d("Reboot", "thermal: " + thermal);
		} catch (IOException e) {
			Log.d("Reboot", e.getMessage());
		}*/
    }

    //FIXME can't write data to external storage, seem android issue.
    private void tryWriteThermalLog (String content, boolean append) {
        File temperatureLog = new File(Environment.getDataDirectory(),
                "reboot-thermal-log.csv");
        Log.d("Reboot", temperatureLog.getAbsolutePath());

        try {
            if (!temperatureLog.exists())
                temperatureLog.createNewFile();
            FileWriter writer = new FileWriter(temperatureLog, append);
            writer.append(content + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.d("Reboot", temperatureLog.getAbsolutePath());
            Log.d("Reboot", e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRunner(boolean isRunning) {
        final TextView timerText = (TextView) findViewById(R.id.TextTimer);
        logThermal();

        boolean append = settings.getBoolean(RUNNING_NAME, false);
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String dateStr = format.format(date);
        tryWriteThermalLog(dateStr + "," + thermal, append);

        writePreferences();
        int delay = settings.getInt(DELAY_NAME, DEFAULT_DELAY);
        timerText.setText(String.valueOf(delay));
        handler.removeCallbacks(updateTimerTask);
        toggleAll(!isRunning);
        SharedPreferences.Editor editor = settings.edit();
        if (isRunning) {
            handler.postDelayed(updateTimerTask, 1000);
        }
        editor.putBoolean(RUNNING_NAME, isRunning);
        editor.commit();
    }

    private void writePreferences() {
        SharedPreferences.Editor editor = settings.edit();
        EditText editDelay = (EditText) findViewById(R.id.EditDelay);
        EditText editCountdown = (EditText) findViewById(R.id.EditCountdown);
        int delay = Integer.parseInt(editDelay.getText().toString());
        int countdown = Integer.parseInt(editCountdown.getText().toString());
        RadioGroup group = (RadioGroup) findViewById(R.id.GroupRadioType);

        switch (group.getCheckedRadioButtonId()) {
            case R.id.RadioReboot:
                editor.putInt(FUNCTION_TYPE_NAME, FUNCTION_TYPE_REBOOT);
                break;

            case R.id.RadioSleep:
                editor.putInt(FUNCTION_TYPE_NAME, FUNCTION_TYPE_SLEEP);
                break;
            case R.id.RadioShutDown:
                editor.putInt(FUNCTION_TYPE_NAME, FUNCTION_TYPE_SHUTDOWN );
                break;


            default:
                break;
        }

        editor.putInt(DELAY_NAME, delay);
        editor.putInt(COUNTDOWN_NAME, countdown);
        editor.commit();
    }

    private void toggleAll(boolean state) {
        ArrayList<Integer> items = new ArrayList<Integer>();
        items.add(new Integer(R.id.RadioReboot));
        items.add(new Integer(R.id.RadioSleep));
        items.add(new Integer(R.id.RadioShutDown));
        items.add(new Integer(R.id.EditCountdown));
        items.add(new Integer(R.id.EditDelay));

        for (Integer id : items) {
            View view = (View) findViewById(id.intValue());
            view.setEnabled(state);
        }

        if (state == true) {
            setupRadioButton();
        }
    }

    private void setupRadioButton() {
        int rebootType = settings.getInt(FUNCTION_TYPE_NAME, FUNCTION_TYPE_REBOOT);
        RadioButton btn = (RadioButton) findViewById(R.id.RadioReboot);
        btn.setChecked(rebootType == FUNCTION_TYPE_REBOOT ? true : false);

        btn = (RadioButton) findViewById(R.id.RadioSleep);
        //TODO disable sleep/wakeup
        btn.setEnabled(false);
        //btn.setChecked(rebootType == FUNCTION_TYPE_SLEEP ? true : false);
        btn = (RadioButton) findViewById(R.id.RadioShutDown);
        btn.setChecked(rebootType == FUNCTION_TYPE_SHUTDOWN ? true : false);

    }

    private void setupEditText() {
        EditText edit = (EditText) findViewById(R.id.EditCountdown);
        edit.setText(String.valueOf(settings.getInt(COUNTDOWN_NAME,
                DEFAULT_COUNTDOWN)));
        edit = (EditText) findViewById(R.id.EditDelay);
        edit.setText(String.valueOf(settings.getInt(DELAY_NAME,
                DEFAULT_DELAY)));
    }

    private void setupPreference() {
        settings = getSharedPreferences(SETTINGS_NAME, MODE_WORLD_WRITEABLE);
        if (settings.contains(RUNNING_NAME) == false) {
            SharedPreferences.Editor edit = settings.edit();
            edit.putBoolean(RUNNING_NAME, false);
            edit.putInt(COUNTDOWN_NAME, DEFAULT_COUNTDOWN);
            edit.putInt(DELAY_NAME, DEFAULT_DELAY);
            edit.putInt(FUNCTION_TYPE_NAME, FUNCTION_TYPE_REBOOT);
        }
    }

    private Runnable updateTimerTask = new Runnable() {
        public void run() {
            TextView text = (TextView) findViewById(R.id.TextTimer);
            int sec = Integer.parseInt(text.getText().toString());
            if (sec == 0) {
                SharedPreferences.Editor editor = settings.edit();
                int origCount = settings.getInt(COUNTDOWN_NAME,
                        DEFAULT_COUNTDOWN);
                editor.putInt(COUNTDOWN_NAME, origCount - 1);
                editor.commit();
                setupEditText();
                handler.removeCallbacks(this);
                performFunction();
                return;
            }

            text.setText(String.valueOf(sec - 1));
            handler.postAtTime(this, SystemClock.uptimeMillis() + 1000);
        }
    };


    private void performFunction() {
        RadioGroup group = (RadioGroup) findViewById(R.id.GroupRadioType);

        switch (group.getCheckedRadioButtonId()) {
            case R.id.RadioReboot:
                reboot();
                break;

            case R.id.RadioSleep:
                sleep ();
                break;
            case R.id.RadioShutDown:
                shutdown();
                break;
            default:
                break;
        }
    }

    private void sleep () {
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent intent = new Intent (this, AlarmReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, 0);

        am.cancel(pending);
        am.set(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + SLEEP_TIME, pending);
        //pm.goToSleep(SystemClock.uptimeMillis()+1);
    }

    private void reboot() {
		/*File ecShutdown = new File("/proc/ec_shutdown");
		try {
			if (ecShutdown.exists() && ecShutdown.canWrite()) {
				String value = "3\n";
				FileWriter write = new FileWriter(ecShutdown);
				BufferedWriter out = new BufferedWriter(write);
				out.write(value);
				out.close();

				Intent intent = new Intent(ACTION_REQUEST_SHUTDOWN);
				intent.putExtra(EXTRA_KEY_CONFIRM, false);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				this.startActivity(intent);
			} else {
				Log.d("REBOOT", "Can't do EC reboot");
			}
		} catch (IOException e) {
			Log.e("REBOOT", e.getMessage());
		}*/
        Intent intent = new Intent(Intent.ACTION_REBOOT);
        intent.putExtra("nowait", 1);
        intent.putExtra("interval", 1);
        intent.putExtra("window", 0);
        try {
            Log.d("REBOOT", "singhome: reboot sendBroadcast");
            //sendBroadcast(intent);
            startActivity(intent);
        } catch (Exception e) {
            Log.d("REBOOT", e.getMessage());
        }


    }

    private void shutdown() {
                       /*
				Intent intent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
				intent.putExtra("nowait", 1);
				intent.putExtra("interval", 1);
				intent.putExtra("window", 0);
				try {
					Log.d("SHUTDOWN", "singhome: ACTION_REQUEST_SHUTDOWN sendBroadcast");
                            	sendBroadcast(intent);
                            } catch (Exception e) {
                            	Log.d("SHUTDOWN", e.getMessage());
                            }*/

        Intent intent = new Intent(Intent.ACTION_SHUTDOWN);
//                  intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    protected void onDestroy() {
        RebootWakeLock.releaseCpuLock();
        super.onDestroy();
    }

    protected void onPause() {
        writePreferences();
        RebootWakeLock.releaseCpuLock();
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        Log.d("REBOOT", "onResume");

        int functionType = settings.getInt(FUNCTION_TYPE_NAME, FUNCTION_TYPE_REBOOT);
        EditText edit = (EditText)findViewById(R.id.EditCountdown);
        int countdown = Integer.parseInt(edit.getText().toString());

        if (settings.getBoolean(RUNNING_NAME, false)
                &&  functionType == FUNCTION_TYPE_SLEEP
                && countdown > 0) {
            TextView timerText = (TextView) findViewById(R.id.TextTimer);
            int delay = settings.getInt(DELAY_NAME, DEFAULT_DELAY);
            timerText.setText(String.valueOf(delay));
            handleRunner(true);
        }
        else if (settings.getBoolean(RUNNING_NAME, false)
                &&  functionType == FUNCTION_TYPE_SLEEP
                && countdown <= 0) {
            ((ToggleButton)findViewById(R.id.ButtonToggle)).setChecked(false);
            handler.removeCallbacks(updateTimerTask);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
