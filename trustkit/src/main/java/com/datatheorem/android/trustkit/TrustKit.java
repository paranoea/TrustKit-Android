package com.datatheorem.android.trustkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.NetworkSecurityPolicy;
import android.support.annotation.NonNull;

import com.datatheorem.android.trustkit.config.ConfigurationException;
import com.datatheorem.android.trustkit.config.TrustKitConfiguration;
import com.datatheorem.android.trustkit.pinning.TrustKitTrustManagerBuilder;
import com.datatheorem.android.trustkit.reporting.BackgroundReporter;
import com.datatheorem.android.trustkit.utils.TrustKitLog;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.Set;
import java.util.UUID;


public class TrustKit {

    private static final String TRUSTKIT_VENDOR_ID = "TRUSTKIT_VENDOR_ID";
    protected static TrustKit trustKitInstance;

    private final TrustKitConfiguration trustKitConfiguration;
    protected BackgroundReporter backgroundReporter;

    protected TrustKit(@NonNull Context context,
                       @NonNull TrustKitConfiguration trustKitConfiguration) {
        this.trustKitConfiguration = trustKitConfiguration;

        // Try to process the debug-overrides setting and parse the custom CA certificates if the
        // App is debuggable
        // Do not use BuildConfig.DEBUG as it does not work for libraries
        boolean isAppDebuggable = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        Set<Certificate> debugCaCerts = null;
        boolean shouldOverridePins = false;
        if (isAppDebuggable) {
            debugCaCerts = trustKitConfiguration.getDebugCaCertificates();
            if (debugCaCerts != null) {
                TrustKitLog.i("App is debuggable - processing <debug-overrides> configuration.");
            }
            shouldOverridePins = trustKitConfiguration.shouldOverridePins();
        }

        try {
            TrustKitTrustManagerBuilder.initializeBaselineTrustManager(debugCaCerts,
                    shouldOverridePins);
        } catch (CertificateException | NoSuchAlgorithmException | KeyManagementException
                | KeyStoreException | IOException e) {
            throw new ConfigurationException("Could not parse <debug-overrides> certificates");
        }

        // Create the background reporter for sending pin failure reports
        String appPackageName = context.getPackageName();
        String appVersion;
        try {
            appVersion = context.getPackageManager().getPackageInfo(appPackageName, 0).versionName;

        } catch (PackageManager.NameNotFoundException e) {
            appVersion = "N/A";
        }

        if (appVersion == null) {
            appVersion = "N/A";
        }

        String appVendorId = getOrCreateVendorIdentifier(context);
        this.backgroundReporter = new BackgroundReporter(true, appPackageName, appVersion,
                appVendorId);
    }

    @NonNull
    protected static String getOrCreateVendorIdentifier(@NonNull Context appContext) {
        SharedPreferences trustKitSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(appContext);
        // We store the vendor ID in the App's preferences
        String appVendorId = trustKitSharedPreferences.getString(TRUSTKIT_VENDOR_ID, "");
        if (appVendorId.equals("")) {
            // First time the App is running: generate and store a new vendor ID
            TrustKitLog.i("Generating new vendor identifier...");
            appVendorId = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = trustKitSharedPreferences.edit();
            editor.putString(TRUSTKIT_VENDOR_ID, appVendorId);
            editor.apply();
        }
        return appVendorId;
    }

    @NonNull
    public static TrustKit getInstance() {
        if (trustKitInstance == null) {
            throw new IllegalStateException("TrustKit has not been initialized");
        }
        return trustKitInstance;
    }

    public static void initWithNetworkPolicy(@NonNull Context context) {
        // Try to get the default network policy resource ID
        final int networkSecurityConfigId = context.getResources().getIdentifier(
                "network_security_config", "xml", context.getPackageName());
        initWithNetworkPolicy(context, networkSecurityConfigId);
    }

    public static void initWithNetworkPolicy(@NonNull Context context, int policyResourceId) {
        // On Android N, ensure that the system was also able to load the policy
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
                NetworkSecurityPolicy.getInstance() == null) {
            // Android did not find a policy because the supplied resource ID is wrong or the policy
            // file is not properly setup in the manifest, or contains bad data
            throw new ConfigurationException("TrustKit was initialized with a network policy that" +
                    "was not properly configured for Android N ");
        }

        // Then try to load the supplied policy
        TrustKitConfiguration trustKitConfiguration;
        try {
            trustKitConfiguration = TrustKitConfiguration.fromXmlPolicy(context,
                    context.getResources().getXml(policyResourceId)
            );
        } catch (ParseException | XmlPullParserException | IOException e) {
            throw new ConfigurationException("Could not parse network security policy file");
        } catch (CertificateException e) {
            throw new ConfigurationException("Could not find the debug certificate in the network " +
                    "security police file");
        }

        trustKitInstance = new TrustKit(context, trustKitConfiguration);
    }

    public TrustKitConfiguration getConfiguration() { return trustKitConfiguration; }

    public BackgroundReporter getReporter() { return backgroundReporter; }
}
