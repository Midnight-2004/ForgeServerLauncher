import java.io.*;
import java.util.*;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.ProcessHandle;
import java.util.List;

public class ForgeServerLauncher {

    /**
     * 主程序入口
     * 该程序主要用于查找并启动最新的Forge或NeoForge版本
     * 它首先检查指定路径是否存在Forge或NeoForge目录，然后找到最新的版本目录，
     * 并尝试从该目录启动相应的游戏版本
     */
    public static void main(String[] args) {
        // 打印当前工作目录，用于调试和确认程序运行环境
        System.out.println("Current working directory: " + new File(".").getAbsolutePath());

        // 定义Forge和NeoForge的目录路径
        String neoforgePath = "libraries/net/neoforged/neoforge";
        String forgePath = "libraries/net/minecraftforge/forge";

        // 创建Forge和NeoForge目录对象
        File neoforgeDir = new File(neoforgePath);
        File forgeDir = new File(forgePath);

        // 存储所有找到的版本目录
        List<File> versionDirs = new ArrayList<>();

        // 检查NeoForge目录是否存在并添加符合条件的版本目录
        if (neoforgeDir.exists() && neoforgeDir.isDirectory()) {
            System.out.println("Found NeoForge directory: " + neoforgeDir.getAbsolutePath());
            versionDirs.addAll(findVersionDirectories(neoforgeDir));
        } else {
            System.out.println("NeoForge directory not found or is not a directory: " + neoforgeDir.getAbsolutePath());
        }

        // 检查Forge目录是否存在并添加符合条件的版本目录
        if (forgeDir.exists() && forgeDir.isDirectory()) {
            System.out.println("Found Forge directory: " + forgeDir.getAbsolutePath());
            versionDirs.addAll(findVersionDirectories(forgeDir));
        } else {
            System.out.println("Forge directory not found or is not a directory: " + forgeDir.getAbsolutePath());
        }

        // 如果没有找到任何有效的版本目录，打印提示并退出程序
        if (versionDirs.isEmpty()) {
            System.out.println("No valid version directories found.");
            return;
        }

        // 获取最新的版本目录
        File latestVersionDir = getLatestVersionDirectory(versionDirs);

        // 定位并读取unix_args.txt文件
        File unixArgsFile = new File(latestVersionDir, "unix_args.txt");
        if (!unixArgsFile.exists()) {
            System.out.println("unix_args.txt not found in the latest version directory.");
            return;
        }

        // 从文件中读取启动参数
        List<String> launchArguments = readArgumentsFromFile(unixArgsFile.getAbsolutePath());
        if (launchArguments.isEmpty()) {
            System.out.println("Failed to read arguments from unix_args.txt.");
            return;
        }

        // 获取当前进程信息与命令行参数
        ProcessHandle currentProcess = ProcessHandle.current();
        ProcessHandle.Info info = currentProcess.info();
        List<String> fullCommandLine = Optional.ofNullable(info)
                                            .flatMap(ProcessHandle.Info::arguments)
                                            .map(Arrays::asList)
                                            .orElse(Collections.emptyList());
        
        if (fullCommandLine.isEmpty()) {
            System.err.println("Failed to retrieve command line arguments.");
            return;
        }

        // 提取 Java 路径
        String javaPath = fullCommandLine.get(0);

        // 提取 JVM 参数（直到 -jar 为止）
        List<String> jvmArgs = new ArrayList<>();
        int jarIndex = -1;

        for (int i = 1; i < fullCommandLine.size(); i++) {
            String arg = fullCommandLine.get(i);
            if ("-jar".equals(arg)) {
                jarIndex = i + 1;
                break;
            }
            jvmArgs.add(arg);
        }

        // 处理传入的额外参数，跳过-jar及其后的一个参数
        List<String> additionalArgs = new ArrayList<>();
        String noguiArg = null;
        boolean skipNext = false;

        // 添加 JVM 参数（如 -Xmx4G）
        for (String arg : jvmArgs) {
            if (arg.startsWith("-X") || arg.startsWith("--")) {
                additionalArgs.add(arg);
            }
        }

        // 构造最终参数列表
        List<String> finalArguments = new ArrayList<>();
        finalArguments.add(javaPath);       // 使用真实 java 路径
        finalArguments.addAll(jvmArgs);     // 添加 JVM 参数（如 -Xmx4G）

        skipNext = (jarIndex != -1);
        noguiArg = null;

        for (String arg : args) {
            if (skipNext) {
                skipNext = false;
                continue;
            }

            if ("-jar".equals(arg)) {
                skipNext = true;
            } else if ("nogui".equalsIgnoreCase(arg) || "-nogui".equalsIgnoreCase(arg) || "--nogui".equalsIgnoreCase(arg)) {
                noguiArg = arg;
            } else {
                finalArguments.add(arg);
            }
        }

        // 添加来自 unix_args.txt 的参数（去掉第一个 "java"）
        finalArguments.addAll(launchArguments.subList(1, launchArguments.size()));

        // 最后再加 nogui（如果有的话）
        if (noguiArg != null) {
            finalArguments.add(noguiArg);
        }

        // 替换 launchArguments
        launchArguments.clear();
        launchArguments.addAll(finalArguments);

        try {
            // 获取JAR文件所在目录并设置为工作目录
            File jarDir = new File(ForgeServerLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            System.out.println("Working directory for subprocess: " + jarDir.getAbsolutePath());

            // 使用ProcessBuilder启动子进程
            ProcessBuilder processBuilder = new ProcessBuilder(launchArguments);
            processBuilder.directory(jarDir); // 设置工作目录

            Process process = processBuilder.start();

            // 线程 1：读取子进程 stdout 并打印
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            }).start();

            // 线程 2：读取子进程 stderr 并打印
            new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println("ERROR: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // 线程 3：读取控制台输入，并写入子进程 stdin
            Thread inputThread = new Thread(() -> {
                try {
                    InputStream userInput = System.in;
                    OutputStream processInput = process.getOutputStream();

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = userInput.read(buffer)) != -1) {
                        processInput.write(buffer, 0, bytesRead);
                        processInput.flush();
                    }
                } catch (IOException e) {
                    // 忽略或记录日志
                }
            });
            inputThread.setDaemon(true); // 守护线程，允许 JVM 主动退出
            inputThread.start();

            // 等待子进程结束并获取退出码然后强制结束进程
            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
            System.exit(0);

        } catch (Exception e) {
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
                Matcher matcher = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"").matcher(line.trim());
                while (matcher.find()) {
                    String token = matcher.group(1) != null ? matcher.group(1) : matcher.group();

                    // 处理 @argfile 语法
                    if (token.startsWith("@")) {
                        String argFilePath = token.substring(1);
                        File argFile = new File(argFilePath);
                        if (argFile.exists()) {
                            arguments.addAll(readArgumentsFromFile(argFile.getAbsolutePath()));
                        } else {
                            System.err.println("Arg file not found: " + argFilePath);
                        }
                    } else {
                        arguments.add(token);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

            // 在最前面插入 java 命令
            //arguments.add(0, javaPath);

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