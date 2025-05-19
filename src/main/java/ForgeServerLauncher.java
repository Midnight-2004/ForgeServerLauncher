import java.io.*;
import java.util.*;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForgeServerLauncher {

    /**
     * 主程序入口
     * 该程序主要用于查找并启动最新的Forge或NeoForge版本
     * 它首先检查指定路径是否存在Forge或NeoForge目录，然后找到最新的版本目录，
     * 并尝试从该目录启动相应的游戏版本
     */
    public static void main(String[] args) {
        System.out.println("Current working directory: " + new File(".").getAbsolutePath());

        String neoforgePath = "libraries/net/neoforged/neoforge";
        String forgePath = "libraries/net/minecraftforge/forge";

        File neoforgeDir = new File(neoforgePath);
        File forgeDir = new File(forgePath);

        List<File> versionDirs = new ArrayList<>();

        if (neoforgeDir.exists() && neoforgeDir.isDirectory()) {
            System.out.println("Found NeoForge directory: " + neoforgeDir.getAbsolutePath());
            versionDirs.addAll(findVersionDirectories(neoforgeDir));
        } else {
            System.out.println("NeoForge directory not found or is not a directory: " + neoforgeDir.getAbsolutePath());
        }

        if (forgeDir.exists() && forgeDir.isDirectory()) {
            System.out.println("Found Forge directory: " + forgeDir.getAbsolutePath());
            versionDirs.addAll(findVersionDirectories(forgeDir));
        } else {
            System.out.println("Forge directory not found or is not a directory: " + forgeDir.getAbsolutePath());
        }

        if (versionDirs.isEmpty()) {
            System.out.println("No valid version directories found.");
            return;
        }

        File latestVersionDir = getLatestVersionDirectory(versionDirs);

        File unixArgsFile = new File(latestVersionDir, "unix_args.txt");
        if (!unixArgsFile.exists()) {
            System.out.println("unix_args.txt not found in the latest version directory.");
            return;
        }

        List<String> launchArguments = readArgumentsFromFile(unixArgsFile.getAbsolutePath());
        if (launchArguments.isEmpty()) {
            System.out.println("Failed to read arguments from unix_args.txt.");
            return;
        }

        List<String> additionalArgs = new ArrayList<>();
        boolean skipNext = false;
        for (String arg : args) {
            if ("-jar".equals(arg)) {
                skipNext = true;
            } else if (skipNext) {
                skipNext = false;
            } else {
                additionalArgs.add(arg);
            }
        }

        launchArguments.addAll(additionalArgs);

        try {
            // ✅ 获取 JAR 文件所在目录并设置为工作目录
            File jarDir = new File(ForgeServerLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            System.out.println("Working directory for subprocess: " + jarDir.getAbsolutePath());

            ProcessBuilder processBuilder = new ProcessBuilder(launchArguments);
            processBuilder.directory(jarDir); // 设置为 JAR 所在目录

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                while ((line = errorReader.readLine()) != null) {
                    System.err.println("ERROR: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断目录是否为有效的版本目录
     * @param dir 目录对象
     * @return 是否为有效版本目录
     */
    private static boolean isValidVersionDirectory(File dir) {
        File parent = dir.getParentFile();
        if (parent == null) return false;

        String parentName = parent.getName();
        String dirName = dir.getName();

        // 如果父目录是 forge，则尝试匹配 Forge 格式
        if ("forge".equals(parentName)) {
            return dirName.matches("(?i).*\\d+.*") && 
                   dirName.contains("-") && 
                   dirName.split("-")[1].matches("\\d+(\\.\\d+)+");
        }

        // 如果父目录是 neoforge，则尝试匹配 NeoForge 格式
        if ("neoforge".equals(parentName)) {
            return dirName.matches("\\d+(\\.\\d+)+");
        }

        return false;
    }

    /**
     * 查找指定目录中的所有版本目录
     * @param parentDir 父目录
     * @return 包含所有版本目录的列表
     */
    private static List<File> findVersionDirectories(File parentDir) {
        List<File> versionDirs = new ArrayList<>();
        File[] files = parentDir.listFiles();
        if (files == null) {
            System.err.println("Unable to list files in directory: " + parentDir.getAbsolutePath());
            return versionDirs; // 返回空列表
        }
        for (File dir : files) {
            if (dir.isDirectory()) {
                System.out.println("Checking version directory candidate: " + dir.getAbsolutePath());
                if (isValidVersionDirectory(dir)) {
                    versionDirs.add(dir);
                }
            }
        }
        return versionDirs;
    }

    /**
     * 从目录名称中提取版本号部分
     * @param dir 目录对象
     * @return 版本号数组
     */
    private static int[] extractVersionNumberParts(File dir) {
        String name = dir.getName();

        // 如果是 Forge 目录，如 "1.20.1-47.4.0", 取最后一个 "-" 后的部分
        if (name.contains("forge")) {
            String[] parts = name.split("-");
            name = parts.length > 1 ? parts[parts.length - 1] : "0";
        }

        // 拆分版本号为各段数字
        String[] versionParts = name.split("\\.");
        int[] versionNumbers = new int[versionParts.length];
        for (int i = 0; i < versionParts.length; i++) {
            versionNumbers[i] = Integer.parseInt(versionParts[i].replaceAll("[^\\d]", ""));
        }
        return versionNumbers;
    }

    /**
     * 从版本目录列表中获取最新的版本目录
     * @param versionDirs 版本目录列表
     * @return 最新的版本目录
     */
    private static File getLatestVersionDirectory(List<File> versionDirs) {
        if (versionDirs.isEmpty()) {
            throw new IllegalArgumentException("Version directory list is empty.");
        }

        File latestVersionDir = null;
        int[] latestVersionNumbers = new int[0];

        for (File dir : versionDirs) {
            int[] versionNumbers = extractVersionNumberParts(dir);
            if (latestVersionDir == null || compareVersionNumbers(versionNumbers, latestVersionNumbers) > 0) {
                latestVersionDir = dir;
                latestVersionNumbers = versionNumbers;
            }
        }

        return latestVersionDir;
    }

    /**
     * 比较两个版本号数组
     * @param v1 第一个版本号数组
     * @param v2 第二个版本号数组
     * @return v1 > v2 返回1，v1 < v2 返回-1，v1 == v2 返回0
     */
    private static int compareVersionNumbers(int[] v1, int[] v2) {
        int minLength = Math.min(v1.length, v2.length);
        for (int i = 0; i < minLength; i++) {
            if (v1[i] > v2[i]) {
                return 1;
            } else if (v1[i] < v2[i]) {
                return -1;
            }
        }
        return Integer.compare(v1.length, v2.length);
    }

    /**
     * 将内容写入文件
     * @param filePath 文件路径
     * @param content 要写入的内容
     */
    private static void writeToFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件中读取内容并按空格拆分为参数列表
     * @param filePath 文件路径
     * @return 参数列表
     */
    private static List<String> readArgumentsFromFile(String filePath) {
        List<String> arguments = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 按空格拆分参数，保留带引号的参数整体
                Matcher matcher = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"").matcher(line.trim());
                while (matcher.find()) {
                    String match = matcher.group(1); // 兼容带引号内容
                    if (match != null && !match.isEmpty()) {
                        arguments.add(match);
                    } else if (!matcher.group().isEmpty()) {
                        arguments.add(matcher.group());
                    }
                }
            }

            // 在最前面插入 java 命令
            arguments.add(0, "java");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return arguments;
    }

    /**
     * 从文件中读取内容
     * @param filePath 文件路径
     * @return 文件内容
     */
    private static String readFromFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString().trim();
    }
}