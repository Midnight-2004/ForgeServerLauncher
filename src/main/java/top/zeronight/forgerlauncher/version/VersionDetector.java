package top.zeronight.forgerlauncher.version;

import top.zeronight.forgerlauncher.util.ConsoleUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 版本检测器，负责扫描 libraries 目录并识别 Forge/NeoForge 版本
 */
public class VersionDetector {

    private static final String NEOFORGE_PATH = "net/neoforged/neoforge";
    private static final String FORGE_PATH = "net/minecraftforge/forge";

    /**
     * 扫描 libraries 目录，查找所有有效的版本目录
     *
     * @param librariesRoot libraries 根目录
     * @return 所有找到的版本信息列表
     */
    public List<VersionInfo> scanVersions(File librariesRoot) {
        List<VersionInfo> versionInfos = new ArrayList<>();

        // 检查 NeoForge 目录
        File neoforgeDir = new File(librariesRoot, NEOFORGE_PATH);
        if (neoforgeDir.exists() && neoforgeDir.isDirectory()) {
            ConsoleUtils.logDebug("Found NeoForge directory: " + neoforgeDir.getAbsolutePath());
            versionInfos.addAll(findVersionDirectories(neoforgeDir, VersionInfo.ServerType.NEOFORGE));
        } else {
            ConsoleUtils.logWarn("NeoForge directory not found or is not a directory: " + neoforgeDir.getAbsolutePath());
        }

        // 检查 Forge 目录
        File forgeDir = new File(librariesRoot, FORGE_PATH);
        if (forgeDir.exists() && forgeDir.isDirectory()) {
            ConsoleUtils.logDebug("Found Forge directory: " + forgeDir.getAbsolutePath());
            versionInfos.addAll(findVersionDirectories(forgeDir, VersionInfo.ServerType.FORGE));
        } else {
            ConsoleUtils.logWarn("Forge directory not found or is not a directory: " + forgeDir.getAbsolutePath());
        }

        return versionInfos;
    }

    /**
     * 从版本信息列表中获取最新的版本
     *
     * @param versionInfos 版本信息列表
     * @return 最新版本的 Optional，如果列表为空则返回 empty
     */
    public Optional<VersionInfo> getLatestVersion(List<VersionInfo> versionInfos) {
        if (versionInfos.isEmpty()) {
            return Optional.empty();
        }

        VersionInfo latest = null;
        int[] latestVersionNumbers = new int[0];

        for (VersionInfo info : versionInfos) {
            int[] versionNumbers = info.getVersionParts();
            if (latest == null || compareVersionNumbers(versionNumbers, latestVersionNumbers) > 0) {
                latest = info;
                latestVersionNumbers = versionNumbers;
            }
        }

        return Optional.ofNullable(latest);
    }

    /**
     * 查找指定目录中的所有有效版本目录
     *
     * @param parentDir 父目录
     * @param type      服务器类型
     * @return 版本信息列表
     */
    private List<VersionInfo> findVersionDirectories(File parentDir, VersionInfo.ServerType type) {
        List<VersionInfo> versionInfos = new ArrayList<>();
        File[] files = parentDir.listFiles();

        if (files == null) {
            ConsoleUtils.logError("Unable to list files in directory: " + parentDir.getAbsolutePath());
            return versionInfos;
        }

        for (File dir : files) {
            if (dir.isDirectory() && isValidVersionDirectory(dir, type)) {
                ConsoleUtils.logDebug("Checking version directory candidate: " + dir.getAbsolutePath());
                String versionString = dir.getName();
                int[] versionParts = extractVersionNumberParts(dir, type);
                versionInfos.add(new VersionInfo(dir, versionString, versionParts, type));
            }
        }

        return versionInfos;
    }

    /**
     * 判断目录是否为有效的版本目录
     *
     * @param dir  目录对象
     * @param type 服务器类型
     * @return 是否为有效版本目录
     */
    private boolean isValidVersionDirectory(File dir, VersionInfo.ServerType type) {
        String dirName = dir.getName();

        if (type == VersionInfo.ServerType.FORGE) {
            // Forge 格式如 "1.20.1-47.4.0"，需要包含 "-" 且后半部分为版本号
            return dirName.matches("(?i).*\\d+.*") &&
                    dirName.contains("-") &&
                    dirName.split("-")[1].matches("\\d+(\\.\\d+)+");
        } else if (type == VersionInfo.ServerType.NEOFORGE) {
            // NeoForge 格式如 "21.1.65"，纯版本号
            return dirName.matches("\\d+(\\.\\d+)+");
        }

        return false;
    }

    /**
     * 从目录名称中提取版本号部分
     *
     * @param dir  目录对象
     * @param type 服务器类型
     * @return 版本号数组
     */
    private int[] extractVersionNumberParts(File dir, VersionInfo.ServerType type) {
        String name = dir.getName();

        // 如果是 Forge 目录，取最后一个 "-" 后的部分
        if (type == VersionInfo.ServerType.FORGE) {
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
     * 比较两个版本号数组
     *
     * @param v1 第一个版本号数组
     * @param v2 第二个版本号数组
     * @return v1 > v2 返回正数，v1 < v2 返回负数，相等返回 0
     */
    private int compareVersionNumbers(int[] v1, int[] v2) {
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
}
