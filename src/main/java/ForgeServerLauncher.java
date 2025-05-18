import java.io.*;
import java.util.*;

public class ForgeServerLauncher {

    /**
     * 主程序入口
     * 该程序主要用于查找并启动最新的Forge或NeoForge版本
     * 它首先检查指定路径是否存在Forge或NeoForge目录，然后找到最新的版本目录，
     * 并尝试从该目录启动相应的游戏版本
     */
    public static void main(String[] args) {
        // 定义NeoForge和Forge的路径
        String neoforgePath = "libraries/net/neoforged/neoforge";
        String forgePath = "libraries/net/minecraftforge/forge";

        // 创建File对象来表示NeoForge和Forge目录
        File neoforgeDir = new File(neoforgePath);
        File forgeDir = new File(forgePath);

        // 用于存储所有版本目录的列表
        List<File> versionDirs = new ArrayList<>();
        
        // 检查NeoForge目录是否存在且为目录
        if (neoforgeDir.exists() && neoforgeDir.isDirectory()) {
            System.out.println("Found NeoForge directory: " + neoforgeDir.getAbsolutePath());
            versionDirs.addAll(findVersionDirectories(neoforgeDir));
        } else {
            System.out.println("NeoForge directory not found or is not a directory: " + neoforgeDir.getAbsolutePath());
        }  
        
        // 检查Forge目录是否存在且为目录
        if (forgeDir.exists() && forgeDir.isDirectory()) {
            System.out.println("Found Forge directory: " + forgeDir.getAbsolutePath());
            versionDirs.addAll(findVersionDirectories(forgeDir));
        } else {
            System.out.println("Forge directory not found or is not a directory: " + forgeDir.getAbsolutePath());
        }

        // 如果没有找到任何有效的版本目录，输出提示并退出程序
        if (versionDirs.isEmpty()) {
            System.out.println("No valid version directories found.");
            return;
        }

        // 获取最新的版本目录
        File latestVersionDir = getLatestVersionDirectory(versionDirs);

        // 查找最新的版本目录中的unix_args.txt文件
        File unixArgsFile = new File(latestVersionDir, "unix_args.txt");
        if (!unixArgsFile.exists()) {
            System.out.println("unix_args.txt not found in the latest version directory.");
            return;
        }

        // 获取unix_args.txt文件的绝对路径
        String unixArgsFilePath = unixArgsFile.getAbsolutePath();
        // 将路径写入到version.txt文件中
        writeToFile("libraries/version.txt", unixArgsFilePath);

        // 从unix_args.txt文件中读取启动命令
        String launchCommand = readFromFile(unixArgsFilePath);
        if (launchCommand == null) {
            System.out.println("Failed to read unix_args.txt.");
            return;
        }

        try {
            // 使用ProcessBuilder启动游戏进程
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", launchCommand);
            processBuilder.directory(latestVersionDir);
            Process process = processBuilder.start();

            // 使用 try-with-resources 关闭输入流和错误流
            try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(
                     new InputStreamReader(process.getErrorStream()))) {

                String line;
                // 读取并打印进程的标准输出
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                // 可选：打印错误输出，便于调试
                while ((line = errorReader.readLine()) != null) {
                    System.err.println("ERROR: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 等待进程结束并获取退出码
            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
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

        // 如果父目录是 forge，则使用 Forge 版本号格式校验
        if ("forge".equals(parentName)) {
            return dir.getName().matches("\\d+\\.\\d+\\.\\d+-\\d+\\.\\d+");
        }
        // 如果父目录是 neoforge，则使用 NeoForge 版本号格式校验
        else if ("neoforge".equals(parentName)) {
            return dir.getName().matches("\\d+\\.\\d+\\.\\d+");
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
            // 如果是目录且名称符合版本号格式，则添加到列表中
            if (dir.isDirectory() && isValidVersionDirectory(dir)) {
                versionDirs.add(dir);
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