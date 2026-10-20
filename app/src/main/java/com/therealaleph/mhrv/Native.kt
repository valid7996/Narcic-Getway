package com.therealaleph.mhrv

/**
 * JNI bindings of libmhrv_rs.so (MasterHttpRelayVPN-RUST). The package and class name are fixed by the
 * exported symbols (Java_com_therealaleph_mhrv_Native_*), so this object must not be moved or renamed.
 */
object Native {
    init {
        System.loadLibrary("mhrv_rs")
    }

    /** Where the core keeps its CA and cache. Call before any other method. */
    external fun setDataDir(path: String)

    /** Starts the proxy from a JSON config; returns a positive handle, or 0 on failure. */
    external fun startProxy(configJson: String): Long

    external fun stopProxy(handle: Long): Boolean

    /** Writes the MITM CA certificate (PEM) to [destPath], creating the CA on first use. */
    external fun exportCa(destPath: String): Boolean

    external fun version(): String

    /** The log lines produced since the last call, joined with newlines. */
    external fun drainLogs(): String

    /** JSON stats of a running handle, or an empty string. */
    external fun statsJson(handle: Long): String
}
