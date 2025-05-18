import java.io.*;
import java.util.*;

public class ForgeServerLauncher {

    public static void main(String[] args) {
        String neoforgePath = "./libraries/net/neoforged/neoforge";
        String forgePath = "./libraries/net/minecraftforge/forge";

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
        File serverJar = findServerJar(latestVersionDir);

        if (serverJar == null) {
            System.out.println("No server JAR file found in the latest version directory.");
            return;
        }

        File unixArgsFile = new File(latestVersionDir, "unix_args.txt");
        if (!unixArgsFile.exists()) {
            System.out.println("unix_args.txt not found in the latest version directory.");
            return;
        }

        String unixArgsFilePath = unixArgsFile.getAbsolutePath();
        writeToFile("./libraries/version.txt", unixArgsFilePath);

        String launchCommand = readFromFile(unixArgsFilePath);
        if (launchCommand == null) {
            System.out.println("Failed to read unix_args.txt.");
            return;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", launchCommand);
            processBuilder.directory(latestVersionDir);
            Process process = processBuilder.start();

            // 使用 try-with-resources 关闭输入流和错误流
            try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(
                     new InputStreamReader(process.getErrorStream()))) {

                String line;
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

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static List<File> findVersionDirectories(File parentDir) {
    List<File> versionDirs = new ArrayList<>();
    File[] files = parentDir.listFiles();
    if (files == null) {
        System.err.println("Unable to list files in directory: " + parentDir.getAbsolutePath());
        return versionDirs; // 返回空列表
    }
    for (File dir : files) {
        if (dir.isDirectory() && (dir.getName().contains("neoforged") || dir.getName().contains("forge"))) {
            versionDirs.add(dir);
        }
    }
    return versionDirs;
    } 

    private static File getLatestVersionDirectory(List<File> versionDirs) {
    versionDirs.sort((dir1, dir2) -> {
        int[] v1 = extractVersionNumberParts(dir1);
        int[] v2 = extractVersionNumberParts(dir2);

        int minLength = Math.min(v1.length, v2.length);
        for (int i = 0; i < minLength; i++) {
            if (v1[i] != v2[i]) {
                return Integer.compare(v1[i], v2[i]);
            }
        }
        return Integer.compare(v1.length, v2.length); // 长度不同则更长的版本更高
    });

    return versionDirs.get(versionDirs.size() - 1);
    }

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

    private static File findServerJar(File versionDir) {
    File[] files = versionDir.listFiles();
    if (files == null) {
        System.err.println("Unable to list files in directory: " + versionDir.getAbsolutePath());
        return null;
    }
    for (File file : files) {
        if (file.isFile() && file.getName().endsWith("-server.jar")) {
            return file;
        }
    }
    return null;
    }

    private static void writeToFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

