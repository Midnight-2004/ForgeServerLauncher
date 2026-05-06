package top.zeronight.forgerlauncher.parser;

import top.zeronight.forgerlauncher.util.ConsoleUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.ProcessHandle;

/**
 * JVM 参数读取器，负责从 user_jvm_args.txt 和命令行中提取 JVM 参数
 */
public class JvmArgsReader {

    private static final Pattern ARG_PATTERN = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"");

    /**
     * 从 user_jvm_args.txt 文件中读取 JVM 参数，忽略 # 后的注释
     *
     * @param filePath 文件路径
     * @return JVM 参数列表
     */
    public List<String> readUserJvmArgs(String filePath) {
        List<String> jvmArgs = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            ConsoleUtils.logWarn("user_jvm_args.txt not found, using default JVM arguments.");
            return jvmArgs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 忽略 # 后的注释
                int commentIndex = line.indexOf('#');
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }
                line = line.trim();

                if (!line.isEmpty()) {
                    // 使用正则表达式分割参数，考虑引号内的空格
                    Matcher matcher = ARG_PATTERN.matcher(line);
                    while (matcher.find()) {
                        String token = matcher.group(1) != null ? matcher.group(1) : matcher.group();
                        jvmArgs.add(token);
                    }
                }
            }
        } catch (IOException e) {
            ConsoleUtils.logError("Error reading user_jvm_args.txt: " + e.getMessage());
        }

        return jvmArgs;
    }

    /**
     * 从当前进程信息中提取 JVM 参数（直到 -jar 为止）
     *
     * @param info 进程信息
     * @return JVM 参数列表
     */
    public List<String> extractJvmArgsFromCommandLine(ProcessHandle.Info info) {
        List<String> jvmArgs = new ArrayList<>();

        Optional<String[]> argsOpt = info.arguments();
        if (!argsOpt.isPresent()) {
            ConsoleUtils.logError("Failed to retrieve command line arguments.");
            return jvmArgs;
        }

        String[] args = argsOpt.get();
        for (String arg : args) {
            if ("-jar".equals(arg)) {
                break;
            }
            jvmArgs.add(arg);
        }

        return jvmArgs;
    }

    /**
     * 从 JVM 参数列表中提取 -Xmx 和 -Xms 参数
     *
     * @param jvmArgs JVM 参数列表
     * @return -Xmx/-Xms 参数列表
     */
    public List<String> extractXmxXmsArgs(List<String> jvmArgs) {
        List<String> xmxXmsArgs = new ArrayList<>();
        for (String arg : jvmArgs) {
            if (arg.startsWith("-Xmx") || arg.startsWith("-Xms")) {
                xmxXmsArgs.add(arg);
            }
        }
        return xmxXmsArgs;
    }

    /**
     * 从 JVM 参数列表中提取其他 JVM 相关参数（-X, --, -D, -javaagent 等）
     *
     * @param jvmArgs JVM 参数列表
     * @return 其他 JVM 参数列表
     */
    public List<String> extractOtherJvmArgs(List<String> jvmArgs) {
        List<String> otherArgs = new ArrayList<>();
        for (String arg : jvmArgs) {
            if (arg.startsWith("-X") || arg.startsWith("--") || arg.startsWith("-D") || arg.startsWith("-javaagent")) {
                otherArgs.add(arg);
            }
        }
        return otherArgs;
    }

    /**
     * 合并多类 JVM 参数
     *
     * @param userArgs   用户自定义参数（来自 user_jvm_args.txt）
     * @param xmxXmsArgs -Xmx/-Xms 参数
     * @param cliJvmArgs 命令行传入的其他 JVM 参数
     * @return 合并后的 JVM 参数列表
     */
    public List<String> mergeJvmArgs(List<String> userArgs, List<String> xmxXmsArgs, List<String> cliJvmArgs) {
        List<String> merged = new ArrayList<>();
        merged.addAll(userArgs);
        merged.addAll(xmxXmsArgs);
        merged.addAll(cliJvmArgs);
        return merged;
    }
}
