package top.zeronight.forgerlauncher.version;

import java.io.File;

/**
 * 版本信息数据类，封装服务端版本的详细信息
 */
public class VersionInfo {

    /**
     * 服务器类型枚举
     */
    public enum ServerType {
        FORGE,
        NEOFORGE
    }

    private final File directory;
    private final String versionString;
    private final int[] versionParts;
    private final ServerType type;

    public VersionInfo(File directory, String versionString, int[] versionParts, ServerType type) {
        this.directory = directory;
        this.versionString = versionString;
        this.versionParts = versionParts;
        this.type = type;
    }

    public File getDirectory() {
        return directory;
    }

    public String getVersionString() {
        return versionString;
    }

    public int[] getVersionParts() {
        return versionParts;
    }

    public ServerType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "directory=" + directory.getAbsolutePath() +
                ", versionString='" + versionString + '\'' +
                ", type=" + type +
                '}';
    }
}
