package com.reboot.pao_chueh.reboot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;

public class Utils {	
	public static void disableKeyLock (Context context) {
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		KeyguardLock lock = km.newKeyguardLock("phone");
		lock.disableKeyguard();
	}
	
	public static String getKeywordOutput (Process p, String keyword) {
		InputStream err = p.getErrorStream();
		StringBuffer sb = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(err));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				if (line.contains(keyword))
					sb.append(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	public static int parseThermal (String logContent) {
		Pattern pattern = Pattern.compile(".*Tcore =\\s+(\\d+)\\s+C");
		Matcher matcher = pattern.matcher(logContent);
		if (matcher.matches()) {
			return Integer.valueOf(matcher.group(1));
		}
		return -1;
	}
}
