package com.example.ai_poweredtextanalyzer.Objects;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class represent a file uploaded by the user.
 */
public class File {
    private final String id;
    private final String filename;
    private final String uploadedAt;

    public File(String id, String filename, String uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.uploadedAt = parseUploadDate(uploadedAt);
    }

    public String getId() { return id; }
    public String getFilename() { return filename; }
    public String getUploadedAt() { return uploadedAt; }

    private String parseUploadDate(String rawUploadedAt) {
        String formattedDate = rawUploadedAt; // fallback in case parsing fails
        try {
            SimpleDateFormat parser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            parser.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date date = parser.parse(rawUploadedAt);

            if (date != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);
                formattedDate = formatter.format(date);
            }
        } catch (ParseException e) {
            if (e.getMessage() != null) {
                Log.e("error", e.getMessage());
            }
        }

        return formattedDate;
    }
}
