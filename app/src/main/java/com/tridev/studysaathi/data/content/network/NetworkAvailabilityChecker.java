package com.tridev.studysaathi.data.content.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NetworkAvailabilityChecker
        implements AutoCloseable {

    @NonNull
    private final Context applicationContext;

    @Nullable
    private final ConnectivityManager connectivityManager;

    @NonNull
    private final Handler mainThreadHandler;

    @NonNull
    private final List<NetworkStateListener> listeners;

    @NonNull
    private final AtomicBoolean monitoring;

    @NonNull
    private final AtomicBoolean closed;

    @Nullable
    private ConnectivityManager.NetworkCallback networkCallback;

    @NonNull
    private volatile NetworkState latestNetworkState;

    public NetworkAvailabilityChecker(
            @NonNull Context context
    ) {
        applicationContext =
                context.getApplicationContext();

        connectivityManager =
                applicationContext.getSystemService(
                        ConnectivityManager.class
                );

        mainThreadHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        listeners =
                new CopyOnWriteArrayList<>();

        monitoring =
                new AtomicBoolean(
                        false
                );

        closed =
                new AtomicBoolean(
                        false
                );

        latestNetworkState =
                createCurrentNetworkState();
    }

    /**
     * Returns the latest known network state.
     *
     * When continuous monitoring has not started,
     * this method performs a fresh network check.
     */
    @NonNull
    public NetworkState getCurrentNetworkState() {
        if (!monitoring.get()) {
            latestNetworkState =
                    createCurrentNetworkState();
        }

        return latestNetworkState;
    }

    /**
     * Returns true only when Android has validated
     * actual internet connectivity.
     */
    public boolean isInternetAvailable() {
        return getCurrentNetworkState()
                .isInternetAvailable();
    }

    /**
     * Returns true when the connection is suitable
     * for small metadata searches such as Google
     * Books or Open Library requests.
     */
    public boolean canSearchBooksOnline() {
        return getCurrentNetworkState()
                .isSuitableForOnlineBookSearch();
    }

    /**
     * Returns true when the current network is
     * suitable for a larger authorized book or
     * PDF download without using metered data.
     */
    public boolean isLargeDownloadRecommended() {
        return getCurrentNetworkState()
                .isLargeDownloadRecommended();
    }

    /**
     * Starts continuous network monitoring.
     *
     * Calling this method repeatedly is safe.
     */
    public void startMonitoring() {
        if (closed.get()) {
            dispatchState(
                    NetworkState.closed()
            );

            return;
        }

        if (connectivityManager == null) {
            latestNetworkState =
                    NetworkState.serviceUnavailable();

            dispatchState(
                    latestNetworkState
            );

            return;
        }

        if (!monitoring.compareAndSet(
                false,
                true
        )) {
            dispatchState(
                    getCurrentNetworkState()
            );

            return;
        }

        networkCallback =
                new ConnectivityManager.NetworkCallback() {

                    @Override
                    public void onAvailable(
                            @NonNull Network network
                    ) {
                        refreshAndDispatch();
                    }

                    @Override
                    public void onCapabilitiesChanged(
                            @NonNull Network network,
                            @NonNull NetworkCapabilities
                                    networkCapabilities
                    ) {
                        refreshAndDispatch();
                    }

                    @Override
                    public void onLosing(
                            @NonNull Network network,
                            int maxMsToLive
                    ) {
                        refreshAndDispatch();
                    }

                    @Override
                    public void onLost(
                            @NonNull Network network
                    ) {
                        refreshAndDispatch();
                    }

                    @Override
                    public void onUnavailable() {
                        latestNetworkState =
                                NetworkState.disconnected();

                        dispatchState(
                                latestNetworkState
                        );
                    }
                };

        try {
            connectivityManager
                    .registerDefaultNetworkCallback(
                            networkCallback
                    );

            latestNetworkState =
                    createCurrentNetworkState();

            dispatchState(
                    latestNetworkState
            );

        } catch (SecurityException exception) {
            monitoring.set(
                    false
            );

            networkCallback =
                    null;

            latestNetworkState =
                    NetworkState.permissionMissing();

            dispatchState(
                    latestNetworkState
            );

        } catch (RuntimeException exception) {
            monitoring.set(
                    false
            );

            networkCallback =
                    null;

            latestNetworkState =
                    NetworkState.monitoringFailed();

            dispatchState(
                    latestNetworkState
            );
        }
    }

    /**
     * Stops continuous monitoring while keeping
     * this checker reusable.
     */
    public void stopMonitoring() {
        if (!monitoring.compareAndSet(
                true,
                false
        )) {
            return;
        }

        ConnectivityManager.NetworkCallback
                callbackToRemove =
                networkCallback;

        networkCallback =
                null;

        if (connectivityManager == null
                || callbackToRemove == null) {

            return;
        }

        try {
            connectivityManager
                    .unregisterNetworkCallback(
                            callbackToRemove
                    );

        } catch (IllegalArgumentException ignored) {
            /*
             * Callback was already removed or was
             * never successfully registered.
             */

        } catch (SecurityException ignored) {
            /*
             * ACCESS_NETWORK_STATE permission will
             * be added through the manifest step.
             */
        }
    }

    public void addListener(
            @NonNull NetworkStateListener listener
    ) {
        if (!listeners.contains(
                listener
        )) {
            listeners.add(
                    listener
            );
        }

        dispatchStateToListener(
                listener,
                getCurrentNetworkState()
        );
    }

    public void removeListener(
            @NonNull NetworkStateListener listener
    ) {
        listeners.remove(
                listener
        );
    }

    public boolean isMonitoring() {
        return monitoring.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

    private void refreshAndDispatch() {
        latestNetworkState =
                createCurrentNetworkState();

        dispatchState(
                latestNetworkState
        );
    }

    @NonNull
    private NetworkState createCurrentNetworkState() {
        if (closed.get()) {
            return NetworkState.closed();
        }

        if (connectivityManager == null) {
            return NetworkState.serviceUnavailable();
        }

        try {
            Network activeNetwork =
                    connectivityManager
                            .getActiveNetwork();

            if (activeNetwork == null) {
                return NetworkState.disconnected();
            }

            NetworkCapabilities capabilities =
                    connectivityManager
                            .getNetworkCapabilities(
                                    activeNetwork
                            );

            if (capabilities == null) {
                return NetworkState.disconnected();
            }

            boolean internetCapable =
                    capabilities.hasCapability(
                            NetworkCapabilities
                                    .NET_CAPABILITY_INTERNET
                    );

            boolean validated =
                    capabilities.hasCapability(
                            NetworkCapabilities
                                    .NET_CAPABILITY_VALIDATED
                    );

            boolean captivePortal =
                    capabilities.hasCapability(
                            NetworkCapabilities
                                    .NET_CAPABILITY_CAPTIVE_PORTAL
                    );

            boolean metered =
                    connectivityManager
                            .isActiveNetworkMetered();

            boolean vpn =
                    capabilities.hasTransport(
                            NetworkCapabilities
                                    .TRANSPORT_VPN
                    );

            boolean suspended =
                    isNetworkSuspended(
                            capabilities
                    );

            TransportType transportType =
                    detectTransportType(
                            capabilities
                    );

            int downstreamBandwidthKbps =
                    Math.max(
                            0,
                            capabilities
                                    .getLinkDownstreamBandwidthKbps()
                    );

            int upstreamBandwidthKbps =
                    Math.max(
                            0,
                            capabilities
                                    .getLinkUpstreamBandwidthKbps()
                    );

            return new NetworkState(
                    true,
                    internetCapable,
                    validated,
                    metered,
                    captivePortal,
                    suspended,
                    vpn,
                    false,
                    false,
                    false,
                    transportType,
                    downstreamBandwidthKbps,
                    upstreamBandwidthKbps
            );

        } catch (SecurityException exception) {
            return NetworkState.permissionMissing();

        } catch (RuntimeException exception) {
            return NetworkState.checkFailed();
        }
    }

    private boolean isNetworkSuspended(
            @NonNull NetworkCapabilities capabilities
    ) {
        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.P) {

            return false;
        }

        return !capabilities.hasCapability(
                NetworkCapabilities
                        .NET_CAPABILITY_NOT_SUSPENDED
        );
    }

    @NonNull
    private TransportType detectTransportType(
            @NonNull NetworkCapabilities capabilities
    ) {
        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_VPN
        )) {
            return TransportType.VPN;
        }

        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
        )) {
            return TransportType.WIFI;
        }

        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
        )) {
            return TransportType.CELLULAR;
        }

        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
        )) {
            return TransportType.ETHERNET;
        }

        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_BLUETOOTH
        )) {
            return TransportType.BLUETOOTH;
        }

        return TransportType.OTHER;
    }

    private void dispatchState(
            @NonNull NetworkState networkState
    ) {
        latestNetworkState =
                networkState;

        mainThreadHandler.post(() -> {
            for (NetworkStateListener listener :
                    listeners) {

                try {
                    listener.onNetworkStateChanged(
                            networkState
                    );

                } catch (RuntimeException ignored) {
                    /*
                     * One listener must not prevent
                     * updates from reaching others.
                     */
                }
            }
        });
    }

    private void dispatchStateToListener(
            @NonNull NetworkStateListener listener,
            @NonNull NetworkState networkState
    ) {
        mainThreadHandler.post(() -> {
            try {
                listener.onNetworkStateChanged(
                        networkState
                );

            } catch (RuntimeException ignored) {
                /*
                 * Listener errors are isolated from
                 * the network monitoring component.
                 */
            }
        });
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(
                false,
                true
        )) {
            return;
        }

        stopMonitoring();

        listeners.clear();

        latestNetworkState =
                NetworkState.closed();
    }

    public interface NetworkStateListener {

        void onNetworkStateChanged(
                @NonNull NetworkState networkState
        );
    }

    public enum TransportType {

        NONE(
                "No Network",
                "कोई नेटवर्क नहीं"
        ),

        WIFI(
                "Wi-Fi",
                "वाई-फाई"
        ),

        CELLULAR(
                "Mobile Data",
                "मोबाइल डेटा"
        ),

        ETHERNET(
                "Ethernet",
                "ईथरनेट"
        ),

        VPN(
                "VPN",
                "वीपीएन"
        ),

        BLUETOOTH(
                "Bluetooth Network",
                "ब्लूटूथ नेटवर्क"
        ),

        OTHER(
                "Other Network",
                "अन्य नेटवर्क"
        );

        @NonNull
        private final String englishLabel;

        @NonNull
        private final String hindiLabel;

        TransportType(
                @NonNull String englishLabel,
                @NonNull String hindiLabel
        ) {
            this.englishLabel =
                    englishLabel;

            this.hindiLabel =
                    hindiLabel;
        }

        @NonNull
        public String getEnglishLabel() {
            return englishLabel;
        }

        @NonNull
        public String getHindiLabel() {
            return hindiLabel;
        }
    }

    public static final class NetworkState {

        private final boolean connectedToNetwork;

        private final boolean internetCapable;

        private final boolean validated;

        private final boolean metered;

        private final boolean captivePortal;

        private final boolean suspended;

        private final boolean vpn;

        private final boolean permissionMissing;

        private final boolean serviceUnavailable;

        private final boolean internalError;

        @NonNull
        private final TransportType transportType;

        private final int downstreamBandwidthKbps;

        private final int upstreamBandwidthKbps;

        private NetworkState(
                boolean connectedToNetwork,
                boolean internetCapable,
                boolean validated,
                boolean metered,
                boolean captivePortal,
                boolean suspended,
                boolean vpn,
                boolean permissionMissing,
                boolean serviceUnavailable,
                boolean internalError,
                @NonNull TransportType transportType,
                int downstreamBandwidthKbps,
                int upstreamBandwidthKbps
        ) {
            this.connectedToNetwork =
                    connectedToNetwork;

            this.internetCapable =
                    internetCapable;

            this.validated =
                    validated;

            this.metered =
                    metered;

            this.captivePortal =
                    captivePortal;

            this.suspended =
                    suspended;

            this.vpn =
                    vpn;

            this.permissionMissing =
                    permissionMissing;

            this.serviceUnavailable =
                    serviceUnavailable;

            this.internalError =
                    internalError;

            this.transportType =
                    transportType;

            this.downstreamBandwidthKbps =
                    Math.max(
                            0,
                            downstreamBandwidthKbps
                    );

            this.upstreamBandwidthKbps =
                    Math.max(
                            0,
                            upstreamBandwidthKbps
                    );
        }

        @NonNull
        private static NetworkState disconnected() {
            return new NetworkState(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    TransportType.NONE,
                    0,
                    0
            );
        }

        @NonNull
        private static NetworkState permissionMissing() {
            return new NetworkState(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    false,
                    false,
                    TransportType.NONE,
                    0,
                    0
            );
        }

        @NonNull
        private static NetworkState serviceUnavailable() {
            return new NetworkState(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    false,
                    TransportType.NONE,
                    0,
                    0
            );
        }

        @NonNull
        private static NetworkState checkFailed() {
            return new NetworkState(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    TransportType.NONE,
                    0,
                    0
            );
        }

        @NonNull
        private static NetworkState monitoringFailed() {
            return checkFailed();
        }

        @NonNull
        private static NetworkState closed() {
            return checkFailed();
        }

        public boolean isConnectedToNetwork() {
            return connectedToNetwork;
        }

        public boolean isInternetCapable() {
            return internetCapable;
        }

        public boolean isValidated() {
            return validated;
        }

        public boolean isMetered() {
            return metered;
        }

        public boolean isCaptivePortal() {
            return captivePortal;
        }

        public boolean isSuspended() {
            return suspended;
        }

        public boolean isVpn() {
            return vpn;
        }

        public boolean isPermissionMissing() {
            return permissionMissing;
        }

        public boolean isServiceUnavailable() {
            return serviceUnavailable;
        }

        public boolean hasInternalError() {
            return internalError;
        }

        @NonNull
        public TransportType getTransportType() {
            return transportType;
        }

        public int getDownstreamBandwidthKbps() {
            return downstreamBandwidthKbps;
        }

        public int getUpstreamBandwidthKbps() {
            return upstreamBandwidthKbps;
        }

        public boolean isInternetAvailable() {
            return connectedToNetwork
                    && internetCapable
                    && validated
                    && !captivePortal
                    && !suspended
                    && !permissionMissing
                    && !serviceUnavailable
                    && !internalError;
        }

        public boolean isSuitableForOnlineBookSearch() {
            return isInternetAvailable();
        }

        public boolean isLargeDownloadRecommended() {
            return isInternetAvailable()
                    && !metered;
        }

        public boolean requiresUserAttention() {
            return captivePortal
                    || suspended
                    || permissionMissing
                    || serviceUnavailable
                    || internalError
                    || !isInternetAvailable();
        }

        @NonNull
        public String getHindiStatusMessage() {
            if (permissionMissing) {
                return "नेटवर्क स्टेट अनुमति उपलब्ध नहीं है।";
            }

            if (serviceUnavailable) {
                return "डिवाइस पर नेटवर्क सेवा उपलब्ध नहीं है।";
            }

            if (internalError) {
                return "नेटवर्क की स्थिति जाँची नहीं जा सकी।";
            }

            if (!connectedToNetwork) {
                return "डिवाइस किसी नेटवर्क से जुड़ा नहीं है।";
            }

            if (captivePortal) {
                return "Wi-Fi में पहले sign-in करना आवश्यक है।";
            }

            if (suspended) {
                return "नेटवर्क फिलहाल अस्थायी रूप से रुका हुआ है।";
            }

            if (!internetCapable) {
                return "नेटवर्क जुड़ा है, लेकिन internet क्षमता उपलब्ध नहीं है।";
            }

            if (!validated) {
                return "नेटवर्क जुड़ा है, लेकिन internet सत्यापित नहीं हुआ है।";
            }

            if (metered) {
                return "Internet उपलब्ध है। यह metered connection है।";
            }

            return "Internet उपलब्ध है और connection सामान्य है।";
        }

        @NonNull
        public String getEnglishStatusMessage() {
            if (permissionMissing) {
                return "Network-state permission is unavailable.";
            }

            if (serviceUnavailable) {
                return "Network service is unavailable on this device.";
            }

            if (internalError) {
                return "The network state could not be checked.";
            }

            if (!connectedToNetwork) {
                return "The device is not connected to a network.";
            }

            if (captivePortal) {
                return "The Wi-Fi network requires sign-in.";
            }

            if (suspended) {
                return "The network is temporarily suspended.";
            }

            if (!internetCapable) {
                return "A network is connected, but it has no internet capability.";
            }

            if (!validated) {
                return "A network is connected, but internet access is not validated.";
            }

            if (metered) {
                return "Internet is available on a metered connection.";
            }

            return "Internet is available and the connection is normal.";
        }
    }
}