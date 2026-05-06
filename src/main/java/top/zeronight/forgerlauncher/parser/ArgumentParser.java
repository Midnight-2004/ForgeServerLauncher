package top.zeronight.forgerlauncher.parser;

import top.zeronight.forgerlauncher.util.ConsoleUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数解析器，负责解析启动参数文件（unix_args.txt / win_args.txt）
 */
public class ArgumentParser {

    private static final Pattern ARG_PATTERN = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"");

    /**
     * 从文件中读取内容并按空格拆分为参数列表
     * 支持 @argfile 递归引用和引号内空格
     *
     * @param filePath 文件路径
     * @return 参数列表
     */
    public List<String> parseArgsFile(String filePath) {
        List<String> arguments = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            ConsoleUtils.logError("Arguments file not found: " + filePath);
            return arguments;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = ARG_PATTERN.matcher(line.trim());
                while (matcher.find()) {
                    String token = matcher.group(1) != null ? matcher.group(1) : matcher.group();

                    // 处理 @argfile 语法
                    if (token.startsWith("@")) {
                        String argFilePath = token.substring(1);
                        File argFile = new File(argFilePath);
                        if (argFile.exists()) {
                            arguments.addAll(parseArgsFile(argFile.getAbsolutePath()));
                        } else {
                            ConsoleUtils.logError("Arg file not found: " + argFilePath);
                        }
                    } else {
                        arguments.add(token);
                    }
                }
            }
        } catch (IOException e) {
            ConsoleUtils.logError("Error reading arguments file: " + e.getMessage());
        }

        return arguments;
    }

    /**
     * 根据平台尝试加载参数文件
     * 优先加载指定平台的文件，如果不存在则返回空列表
     *
     * @param baseDir        基础目录
     * @param platformFileName 平台特定的文件名
     * @return 参数列表，如果文件不存在则返回空列表
     */
    public List<String> parseArgsWithPlatformFallback(File baseDir, String platformFileName) {
        File argsFile = new File(baseDir, platformFileName);
        if (!argsFile.exists()) {
            ConsoleUtils.logWarn("Platform-specific args file not found: " + argsFile.getAbsolutePath());
            return new ArrayList<>();
        }
        return parseArgsFile(argsFile.getAbsolutePath());
    }
}
