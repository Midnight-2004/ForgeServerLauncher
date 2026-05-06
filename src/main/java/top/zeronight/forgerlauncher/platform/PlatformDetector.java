package top.zeronight.forgerlauncher.platform;

import java.io.File;

/**
 * 平台检测器，用于识别当前操作系统并提供跨平台抽象
 */
public class PlatformDetector {

    /**
     * 操作系统枚举
     */
    public enum OperatingSystem {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    private static OperatingSystem currentOs;

    /**
     * 检测当前操作系统
     *
     * @return 当前操作系统类型
     */
    public static OperatingSystem detect() {
        if (currentOs != null) {
            return currentOs;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            currentOs = OperatingSystem.WINDOWS;
        } else if (osName.contains("linux")) {
            currentOs = OperatingSystem.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            currentOs = OperatingSystem.MACOS;
        } else {
            currentOs = OperatingSystem.UNKNOWN;
        }

        return currentOs;
    }

    /**
     * 获取当前操作系统的默认参数文件名
     * Windows 使用 win_args.txt，其他系统使用 unix_args.txt
     *
     * @return 参数文件名
     */
    public static String getDefaultArgsFileName() {
        OperatingSystem os = detect();
        if (os == OperatingSystem.WINDOWS) {
            return "win_args.txt";
        }
        return "unix_args.txt";
    }

    /**
     * 获取路径分隔符
     *
     * @return 文件分隔符
     */
    public static String getPathSeparator() {
        return File.separator;
    }

    /**
     * 判断当前是否为 Windows 系统
     *
     * @return 是否为 Windows
     */
    public static boolean isWindows() {
        return detect() == OperatingSystem.WINDOWS;
    }
}
