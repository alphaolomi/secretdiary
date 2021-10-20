package com.linkesoft.secretdiary.data;

import static androidx.security.crypto.EncryptedFile.FileEncryptionScheme;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;

import com.linkesoft.secretdiary.App;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Signature;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class DiaryEntry {

    private final Date date;
    private String moodEmoji; // e.g. 😀 😐 😠
    private File file;

    public static final DateFormat yyyymmddDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final DateFormat humanReadableDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    DiaryEntry(Date date) {
        this.date = date;
        moodEmoji = "😀";
    }

    DiaryEntry(String fileName) {
        // get date and mood from file name
        file = new File(App.appContext().getFilesDir(), fileName);
        String[] nameParts = fileName.split(" ");
        Date date;
        if (nameParts.length > 1) {
            try {
                date = yyyymmddDateFormat.parse(nameParts[0]);
            } catch (ParseException e) {
                Log.w(getClass().getSimpleName(), "could not parse file name " + fileName);
                date = new Date(file.lastModified());
            }
            moodEmoji = nameParts[1];
        } else {
            Log.w(getClass().getSimpleName(), "could not parse file name " + fileName);
            date = new Date(file.lastModified());
            moodEmoji = "😀";
        }
        this.date = date;
    }

    public String title() {
        return humanReadableDateFormat.format(date);
    }

    public String key() {
        return yyyymmddDateFormat.format(date);
    }

    public void toggleMood() {
        switch (moodEmoji) {
            case "😀":
                moodEmoji = "😐";
                break;
            case "😐":
                moodEmoji = "😠";
                break;
            default:
                moodEmoji = "😀";
        }
        Log.v(getClass().getSimpleName(), "Mood set to " + moodEmoji);
        File newFile = new File(App.appContext().getFilesDir(), fileName());
        if (file != null) {
            file.renameTo(newFile);
        }
        file = newFile;
    }

    public void delete() {
        if (file != null && file.exists())
            file.delete();
    }

    public String fileName() {
        return DiaryEntry.yyyymmddDateFormat.format(date) + " " + moodEmoji;
    }

    public String getMoodEmoji() {
        return moodEmoji;
    }

    // verschlüsseltes Lesen/Schreiben von Dateien

    public String getText() throws UserNotAuthenticatedException {
        Context context = App.appContext();

        File file = new File(context.getFilesDir(), fileName());
        if (!file.exists())
            return ""; // neue Datei
        FileInputStream inputStream = null;
        String text = "";
        // decrypt text
        Log.v(getClass().getSimpleName(), "Decrypting " + file);
        try {
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    context,
                    file,
                    Diary.encryptionKey(),
                    FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
            inputStream = encryptedFile.openFileInput();
            byte[] bytes = new byte[(int) file.length()];
            int nRead = inputStream.read(bytes);
            if (nRead >= 0)
                text = new String(Arrays.copyOf(bytes, nRead)); // encrypted file could be longer than actual payload
            else
                text = "";
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Could not read entry", e);
            if (e.getCause() instanceof UserNotAuthenticatedException) {
                throw (UserNotAuthenticatedException) e.getCause();
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioException) {
                    ; // ignore
                }
            }
        }
        return text;
    }

    public void setText(String text) {
        Context context = App.appContext();
        File file = new File(context.getFilesDir(), fileName()); // data/data/com.linkesoft.secretdiary/files/yyyy-mm-dd.txt
        file.delete();
        // write to the encrypted file
        Log.v(getClass().getSimpleName(), "Encrypting " + file);
        FileOutputStream outputStream = null;
        try {
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    context,
                    file,
                    Diary.encryptionKey(),
                    FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
            outputStream = encryptedFile.openFileOutput();
            byte[] bytes = text.getBytes();
            outputStream.write(bytes);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Security exception", e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ioException) {
                    ; // ignore
                }
            }
        }
    }

    // digitale Signatur

    private byte[] getSignature(String text) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(Diary.privateKey());
            signature.update(text.getBytes());
            return signature.sign();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "signature error", e);
            throw new RuntimeException(e);
        }
    }

    public File getSignatureFile(String text) {
        byte[] signatureBytes = getSignature(text);
        File file = new File(App.appContext().getCacheDir(), fileName() + ".sha256");
        file.delete();
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
            out.write(signatureBytes);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        file.deleteOnExit();
        return file;
    }

    // verifizieren per Code
    // alternativ
    // openssl dgst -sha256  -keyform der -verify  publickey.der -signature entry.txt.sha256 entry.txt
    static boolean verifySignedEntry(String text, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(Diary.publicKey());
            signature.update(text.getBytes());
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            Log.e("Diary", "signature error", e);
            throw new RuntimeException(e);
        }
    }


}
