package com.example.sukarelawanmy.util;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateUtils {
    public static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(timestamp.toDate());
    }
}
