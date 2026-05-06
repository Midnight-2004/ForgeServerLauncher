package top.zeronight.forgerlauncher;

import top.zeronight.forgerlauncher.parser.ArgumentParser;
import top.zeronight.forgerlauncher.parser.JvmArgsReader;
import top.zeronight.forgerlauncher.platform.PlatformDetector;
import top.zeronight.forgerlauncher.process.ProcessManager;
import top.zeronight.forgerlauncher.util.ConsoleUtils;
import top.zeronight.forgerlauncher.version.VersionDetector;
import top.zeronight.forgerlauncher.version.VersionInfo;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Forge/NeoForge 服务器启动器主类
 * 负责编排版本检测、参数解析和进程启动流程
 */
public class ForgeServerLauncher {

    public static void main(String[] args) {
        // 打印当前工作目录
        ConsoleUtils.logInfo("Current working directory: " + new File(".").getAbsolutePath());

        // 检测当前操作系统
        PlatformDetector.OperatingSystem currentOs = PlatformDetector.detect();
        ConsoleUtils.logDebug("Detected operating system: " + currentOs);

        // 扫描版本目录
        VersionDetector versionDetector = new VersionDetector();
        List<VersionInfo> versionInfos = versionDetector.scanVersions(new File("libraries"));

        if (versionInfos.isEmpty()) {
            ConsoleUtils.logError("No valid version directories found.");
            return;
        }

        // 获取最新版本
        Optional<VersionInfo> latestVersionOpt = versionDetector.getLatestVersion(versionInfos);
        if (!latestVersionOpt.isPresent()) {
            ConsoleUtils.logError("Failed to determine the latest version.");
            return;
        }

        VersionInfo latestVersion = latestVersionOpt.get();
        ConsoleUtils.logInfo("Selected latest version: " + latestVersion.getVersionString() + " (" + latestVersion.getType() + ")");
        File latestVersionDir = latestVersion.getDirectory();

        // 获取平台特定的参数文件名并读取启动参数
        String argsFileName = PlatformDetector.getDefaultArgsFileName();
        ArgumentParser argParser = new ArgumentParser();
        List<String> launchArguments = argParser.parseArgsWithPlatformFallback(latestVersionDir, argsFileName);

        if (launchArguments.isEmpty()) {
            ConsoleUtils.logError("Failed to read arguments from " + argsFileName + ".");
            return;
        }

        // 获取 Java 可执行文件路径
        String javaExecutable = getJavaExecutablePath();
        if (javaExecutable == null) {
            ConsoleUtils.logError("Failed to determine Java executable path.");
            return;
        }

        ConsoleUtils.logInfo("Using Java from current process: " + javaExecutable);

        // 读取 JVM 参数
        JvmArgsReader jvmArgsReader = new JvmArgsReader();

        // 从 user_jvm_args.txt 读取用户自定义参数
        List<String> userJvmArgs = jvmArgsReader.readUserJvmArgs("user_jvm_args.txt");

        // 从当前进程提取 -Xmx/-Xms 参数
        List<String> allJvmArgs = jvmArgsReader.extractJvmArgsFromCommandLine(
                java.lang.ProcessHandle.current().info()
        );
        List<String> xmxXmsArgs = jvmArgsReader.extractXmxXmsArgs(allJvmArgs);

        // 从命令行提取其他 JVM 参数
        List<String> cliJvmArgs = jvmArgsReader.extractOtherJvmArgs(allJvmArgs);

        // 合并所有 JVM 参数
        List<String> mergedJvmArgs = jvmArgsReader.mergeJvmArgs(userJvmArgs, xmxXmsArgs, cliJvmArgs);

        // 构建最终启动命令
        List<String> finalCommand = buildFinalCommand(
                javaExecutable,
                mergedJvmArgs,
                args,
                launchArguments
        );

        // 获取 JAR 所在目录作为工作目录
        File workDir = getJarDirectory();
        if (workDir == null) {
            ConsoleUtils.logError("Failed to determine JAR directory.");
            return;
        }

        // 启动服务器
        ProcessManager processManager = new ProcessManager();
        processManager.launchServer(finalCommand, workDir);
    }

    /**
     * 获取 Java 可执行文件路径
     *
     * @return Java 可执行文件路径，失败返回 null
     */
    private static String getJavaExecutablePath() {
        java.lang.ProcessHandle currentProcess = java.lang.ProcessHandle.current();
        java.lang.ProcessHandle.Info info = currentProcess.info();

        if (info.command().isPresent()) {
            return info.command().get();
        } else {
            // 回退到 JAVA_HOME
            String javaHome = System.getProperty("java.home");
            return javaHome + File.separator + "bin" + File.separator + "java";
        }
    }

    /**
     * 构建最终的启动命令列表
     *
     * @param javaExecutable  Java 可执行文件路径
     * @param jvmArgs         合并后的 JVM 参数
     * @param cliArgs         命令行传入的参数
     * @param launchArguments 从 args 文件读取的启动参数
     * @return 完整的启动命令列表
     */
    private static List<String> buildFinalCommand(String javaExecutable, List<String> jvmArgs,
                                                   String[] cliArgs, List<String> launchArguments) {
        List<String> command = new ArrayList<>();

        // 添加 Java 可执行文件
        command.add(javaExecutable);

        // 添加 JVM 参数
        command.addAll(jvmArgs);

        // 处理命令行参数，跳过 -jar 及其后的参数
        String noguiArg = null;
        boolean skipNext = false;

        for (String arg : cliArgs) {
            if (skipNext) {
                skipNext = false;
                continue;
            }

            if ("-jar".equals(arg)) {
                skipNext = true;
            } else if ("nogui".equalsIgnoreCase(arg) || "-nogui".equalsIgnoreCase(arg) || "--nogui".equalsIgnoreCase(arg)) {
                noguiArg = arg;
            } else {
                command.add(arg);
            }
        }

        // 添加从 args 文件读取的参数
        command.addAll(launchArguments);

        // 最后添加 nogui（如果有的话）
        if (noguiArg != null) {
            command.add(noguiArg);
        }

        return command;
    }

    /**
     * 获取 JAR 文件所在目录
     *
     * @return JAR 所在目录，失败返回 null
     */
    private static File getJarDirectory() {
        try {
            return new File(ForgeServerLauncher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException e) {
            ConsoleUtils.logError("Failed to get JAR directory: " + e.getMessage());
            return null;
        }
    }
}
