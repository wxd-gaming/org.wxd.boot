package org.wxd.boot.agent;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-11-01 16:16
 **/
public enum JDKVersion {

    Jdk_1_8(8, "1.8"),
    Jdk_11(11, "11"),
    Jdk_16(16, "16"),
    Jdk_17(17, "17"),
    Jdk_19(19, "19"),
    Jdk_21(21, "21"),
    ;

    private static JDKVersion runTimeJDKVersion;
    private final int version;
    private final String versionString;
    private String curVersionString;

    JDKVersion(int version, String versionString) {
        this.version = version;
        this.versionString = versionString;
    }

    public int getVersion() {
        return version;
    }

    /**
     * System.getProperty("java.version");
     */
    public String getCurVersionString() {
        return curVersionString;
    }

    public String getVersionString() {
        return versionString;
    }

    /**
     * 获取当前运行的默认版本
     *
     * @return
     */
    public static JDKVersion runTimeJDKVersion() {
        if (runTimeJDKVersion == null) {
            final String jdk_version = jdk_version();
            final JDKVersion[] values = JDKVersion.values();
            for (JDKVersion value : values) {
                if (jdk_version.startsWith(value.getVersionString())) {
                    runTimeJDKVersion = value;
                    runTimeJDKVersion.curVersionString = jdk_version;
                    break;
                }
            }
        }
        if (runTimeJDKVersion == null) {
            runTimeJDKVersion = Jdk_1_8;
        }
        return runTimeJDKVersion;
    }

    public static String jdk_version() {
        return System.getProperty("java.version");
    }

}
