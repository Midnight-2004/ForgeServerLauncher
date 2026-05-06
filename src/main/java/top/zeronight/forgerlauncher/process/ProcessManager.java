package top.zeronight.forgerlauncher.process;

import top.zeronight.forgerlauncher.util.ConsoleUtils;

import java.io.*;
import java.util.List;

/**
 * 进程管理器，负责构建 ProcessBuilder、启动子进程并管理其生命周期
 */
public class ProcessManager {

    /**
     * 启动服务器子进程
     *
     * @param fullCommand 完整的启动命令列表
     * @param workDir     工作目录
     */
    public void launchServer(List<String> fullCommand, File workDir) {
        try {
            ConsoleUtils.logDebug("Working directory for subprocess: " + workDir.getAbsolutePath());

            // 使用 ProcessBuilder 启动子进程
            ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
            processBuilder.directory(workDir);

            Process process = processBuilder.start();

            // 线程 1：转发子进程 stdout 到 System.out
            Thread stdoutThread = new Thread(() ->
                    forwardStream(process.getInputStream(), System.out, null)
            );
            stdoutThread.setDaemon(true);
            stdoutThread.start();

            // 线程 2：转发子进程 stderr 到 System.err（带红色前缀）
            Thread stderrThread = new Thread(() ->
                    forwardStream(process.getErrorStream(), System.err, "ERROR: ")
            );
            stderrThread.setDaemon(true);
            stderrThread.start();

            // 线程 3：转发 System.in 到子进程 stdin
            Thread stdinThread = new Thread(() ->
                    forwardInputToProcess(System.in, process.getOutputStream())
            );
            stdinThread.setDaemon(true);
            stdinThread.start();

            // 等待子进程结束并获取退出码
            int exitCode = process.waitFor();
            ConsoleUtils.logInfo("Process exited with code: " + exitCode);
            System.exit(0);

        } catch (Exception e) {
            ConsoleUtils.logError("Failed to launch server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 转发输入流到输出流
     *
     * @param source 源输入流
     * @param target 目标输出流
     * @param prefix 每行前缀（可为 null）
     */
    private void forwardStream(InputStream source, PrintStream target, String prefix) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (prefix != null) {
                    target.println(ConsoleUtils.RED + prefix + line + ConsoleUtils.RESET);
                } else {
                    target.println(line);
                }
            }
        } catch (IOException e) {
            ConsoleUtils.logError("Error forwarding stream: " + e.getMessage());
        }
    }

    /**
     * 转发用户输入到子进程
     *
     * @param userInput     用户输入流（System.in）
     * @param processInput  子进程输入流
     */
    private void forwardInputToProcess(InputStream userInput, OutputStream processInput) {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = userInput.read(buffer)) != -1) {
                processInput.write(buffer, 0, bytesRead);
                processInput.flush();
            }
        } catch (IOException e) {
            // 忽略或记录日志
        }
    }
}
