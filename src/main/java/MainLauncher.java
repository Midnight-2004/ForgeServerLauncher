public class MainLauncher {

    public static void main(String[] args) {
        // 获取操作系统名称
        String osName = System.getProperty("os.name").toLowerCase();

        // 根据操作系统类型调用对应的启动逻辑
        if (osName.contains("win")) {
            // Windows系统调用WindowsLauncher
            WindowsLauncher.main(args);
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
            // Unix/Linux/Mac系统调用UnixLauncher
            UnixLauncher.main(args);
        } else {
            System.err.println("Unsupported operating system: " + osName);
            System.exit(1);
        }
    }
}
