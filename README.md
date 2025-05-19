# ForgeServerLauncher

一个Forge/NeoForge服务器的简单启动器，支持 Forge 1.17.1+ / NeoForge 1.20.1+ 的服务端。

A simple launcher for Forge/NeoForge servers, supporting Forge 1.17.1+ / NeoForge 1.20.1+ server versions.  

## How to use

> [!CAUTION]
> 本程序不支持在 Windows 系统上运行!

1. 前往 [releases](https://github.com/Midnight-2004/ForgeServerLauncher/releases/latest) 下载 `ForgeServerLauncher-x.x.x.jar` 文件

2. 将下载到的jar文件放入服务端的根目录

3. 将你启动服务端的命令由原来的启动`unix_args.txt`更改为如下所示：

```shell
java -Xms1024M -Xmx4096M -server -jar ForgeServerLauncher-1.0.0.jar nogui
```

> [!NOTE]
> 可以添加jvm调优参数，程序会正确传递参数并启动服务器。

4. 程序会搜索最高版本的Forge / NeoForge 服务端并启动。

## License

[GPL-3.0 license](./LICENSE)
