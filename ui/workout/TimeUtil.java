package com.travity.ui.workout;

import android.annotation.SuppressLint;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.fragment.app.FragmentManager;

import com.travity.R;
import com.travity.data.Constants;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtil {
    public static String getDurationString(int seconds) {
        String strTime;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        strTime = twoDigitString(minutes) + ":" + twoDigitString(seconds);
        if (hours > 0) {
            strTime = twoDigitString(hours) + ":" + strTime;
        }
        return strTime;
    }

    private static String twoDigitString(int number) {
        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }
        return String.valueOf(number);
    }

    public static String getDayToday() {
        Calendar date = Calendar.getInstance();
        String dayToday = android.text.format.DateFormat.format("EEEE", date).toString();
        return dayToday;
    }

    public static String getFormatMonth(int month, Locale locale) {
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] monthNames = symbols.getMonths();
        return monthNames[month - 1];
    }

    public static boolean isToday(String eventDayString) {
        boolean ret = false;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        Date todayCalendar = cal.getTime();
        String todayYear = (String) DateFormat.format("yyyy", todayCalendar); // 2013
        String todayMonth = (String) DateFormat.format("MM",   todayCalendar); // 06
        String todayDay = (String) DateFormat.format("dd",   todayCalendar); // 20
        String todayString = todayYear + "-" + todayMonth + "-" + todayDay;

        Date todayDate = new Date();
        Date eventDay = new Date();
        try {
            todayDate = simpleDateFormat.parse(simpleDateFormat.format(todayCalendar));
            eventDay = simpleDateFormat.parse(eventDayString);

            //  0 Comes when two date are same,
            //  1 Comes when date1 is higher then date2
            // -1 Comes when date1 is lower then date2
            if (todayDate.compareTo(eventDay) < 0) {
                ret = true;
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public static int CompareDate(String startDate, String endDate) {
        boolean ret = false;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        Date fromDate = new Date();
        Date toDate = new Date();
        try {
            fromDate = simpleDateFormat.parse(startDate);
            toDate = simpleDateFormat.parse(endDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //  0 Comes when two date are same,
        //  1 Comes when date1 is higher then date2
        // -1 Comes when date1 is lower then date2
        return fromDate.compareTo(toDate);
    }

    public static int CompareDate2(String startDate, String endDate) {
        boolean ret = false;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.DATETIME_FORMAT_DATE, Locale.getDefault());

        Date fromDate = new Date();
        Date toDate = new Date();
        try {
            fromDate = simpleDateFormat.parse(startDate);
            toDate = simpleDateFormat.parse(endDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //  0 Comes when two date are same,
        //  1 Comes when date1 is higher then date2
        // -1 Comes when date1 is lower then date2
        return fromDate.compareTo(toDate);
    }

    public static String getConvertToMonthFromDate(String oldDate) {
        SimpleDateFormat sdfOld = new SimpleDateFormat(Constants.DATETIME_FORMAT_DATE, Locale.getDefault());
        SimpleDateFormat sdfNew = new SimpleDateFormat(Constants.DATETIME_FORMAT_MONTH, Locale.getDefault());
        Date newDate = new Date();
        String newString = null;
        try {
            newDate = sdfOld.parse(oldDate);
            String year = (String) DateFormat.format("yyyy", newDate); // 2013
            String month = (String) DateFormat.format("MMM",   newDate); // 06
            String day = (String) DateFormat.format("dd",   newDate); // 20
            newString = month + " " + day + ", " + year;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newString;
    }

    public static String getConvertToDateFromMonth(String oldDate) {
        SimpleDateFormat sdfOld = new SimpleDateFormat(Constants.DATETIME_FORMAT_MONTH, Locale.getDefault());
        SimpleDateFormat sdfNew = new SimpleDateFormat(Constants.DATETIME_FORMAT_DATE, Locale.getDefault());
        Date newDate = new Date();
        String newString = null;
        try {
            newDate = sdfOld.parse(oldDate);
            String year = (String) DateFormat.format("yyyy", newDate); // 2013
            String month = (String) DateFormat.format("MM",   newDate); // 06
            String day = (String) DateFormat.format("dd",   newDate); // 20
            newString = year + "-" + month + "-" + day;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newString;
    }

    public static String getConvertDate(long lDate) {
        String date = String.valueOf(lDate);
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
    }

    public static String convertTime24to12(String clock24) {
        String clock12;

        if (clock24.equals("+")) {
            return "00:00 AM";
        }

        String tmpHour[] = clock24.split(":");
        int hour = Integer.parseInt(tmpHour[0]);
        if (hour > 12) {
            clock12 = String.valueOf(hour - 12) + tmpHour[1] + " PM";
        } else {
            clock12 = clock24 + " AM";
        }
        Log.v("test", "clock12: " + clock12);
        return clock12;
    }
}
