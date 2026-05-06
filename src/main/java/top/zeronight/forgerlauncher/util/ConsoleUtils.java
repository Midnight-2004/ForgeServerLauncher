package top.zeronight.forgerlauncher.util;

/**
 * 控制台工具类，封装 ANSI 颜色代码和日志打印方法
 */
public class ConsoleUtils {

    // ANSI 颜色代码
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    /**
     * 打印信息日志（绿色）
     */
    public static void logInfo(String message) {
        System.out.println(GREEN + message + RESET);
    }

    /**
     * 打印警告日志（黄色）
     */
    public static void logWarn(String message) {
        System.out.println(YELLOW + message + RESET);
    }

    /**
     * 打印错误日志（红色）
     */
    public static void logError(String message) {
        System.err.println(RED + message + RESET);
    }

    /**
     * 打印调试日志（青色）
     */
    public static void logDebug(String message) {
        System.out.println(CYAN + message + RESET);
    }

    /**
     * 打印带前缀的错误日志到 stderr
     */
    public static void logErrorWithPrefix(String prefix, String message) {
        System.err.println(RED + prefix + message + RESET);
    }
}
