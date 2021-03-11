package com.linkesoft.secretdiary.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.linkesoft.secretdiary.App;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class Diary {

    private static final String encryptionKeyStoreAlias = "diary";
    private static final String signatureKeyStoreAlias = "signdiary";

    public static final SortedMap<String, DiaryEntry> entryMap = new TreeMap<>((o1, o2) -> {
        return o2.compareTo(o1); // descending
    });

    public static List<DiaryEntry> entries() {
        return new ArrayList<>(entryMap.values()); // sorted by key
    }

    public static void refresh() {
        entryMap.clear();
        for (String fileName : App.appContext().fileList()) {
            DiaryEntry entry = new DiaryEntry(fileName);
            entryMap.put(entry.key(), entry);
        }
    }

    public static DiaryEntry newEntry() {
        // do we have an entry for today already?
        DiaryEntry entry = entryMap.get(DiaryEntry.yyyymmddDateFormat.format(today()));
        if (entry == null) {
            entry = new DiaryEntry(today());
            entryMap.put(entry.key(), entry);
        }
        return entry;
    }

    static Date today() {
        String yyyymmdd = DiaryEntry.yyyymmddDateFormat.format(new Date());
        try {
            return DiaryEntry.yyyymmddDateFormat.parse(yyyymmdd);
        } catch (ParseException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    // Schlüssel für symmetrische Verschlüsselung
    static MasterKey encryptionKey() throws GeneralSecurityException, IOException {
        MasterKey.Builder builder = new MasterKey.Builder(App.appContext(), encryptionKeyStoreAlias)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM);
        // optional
        //if (App.hasBiometricProtection())
        //    builder.setUserAuthenticationRequired(true, 100); // Sekunden, wirft sonst UserNotAuthenticatedException

        MasterKey masterKey = builder.build();
        return masterKey;
    }

    public static SharedPreferences encryptedPreferences() {
        Context context = App.appContext();
        try {
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(context,
                    "diary_prefs",
                    encryptionKey(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return sharedPreferences;
        } catch (Exception e) {
            Log.e("Diary", "Security exception", e);
            throw new RuntimeException(e);
        }
    }

    // Schlüssel für digitale Signatur

    private static KeyStore keyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null); // KeyStore muss vor Gebrauch geladen werden
        return keyStore;
    }

    private static KeyStore.PrivateKeyEntry privateKeyEntry() {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore().getEntry(signatureKeyStoreAlias, null);
            if (privateKeyEntry == null) {
                Log.v("Diary", "Generating new private/public key pair");
                KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(signatureKeyStoreAlias, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY).setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512).build();
                generator.initialize(spec);
                KeyPair keyPair = generator.generateKeyPair();
                privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore().getEntry(signatureKeyStoreAlias, null);
            }
            // ist der Schlüssel hardware-gesichert?
            KeyFactory factory = KeyFactory.getInstance(privateKeyEntry.getPrivateKey().getAlgorithm(), "AndroidKeyStore");
            KeyInfo keyInfo = factory.getKeySpec(privateKeyEntry.getPrivateKey(), KeyInfo.class);
            Log.v("Diary", "isInsideSecureHardware: " + keyInfo.isInsideSecureHardware());

            return privateKeyEntry;
        } catch (Exception e) {
            Log.e("Diary", "key generate error", e);
            throw new RuntimeException(e);
        }
    }

    static PrivateKey privateKey() {
        return privateKeyEntry().getPrivateKey();
    }

    static PublicKey publicKey() {
        return privateKeyEntry().getCertificate().getPublicKey();
    }

    public static File getPublicKeyFile() {
        try {
            byte[] publicKeyBytes = publicKey().getEncoded();
            File file = new File(App.appContext().getCacheDir(), "secretdiarypublickey.der"); // binär
            file.delete();
            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
            out.write(publicKeyBytes);
            out.close();
            file.deleteOnExit();
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
