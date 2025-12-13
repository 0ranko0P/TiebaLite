# <p align="center">Tieba Lite</p>
<p align="center">
    <a href="https://github.com/HuanCheng65/TiebaLite/actions/workflows/build.yml">
        <img alt="Build Status" src="https://github.com/HuanCheng65/TiebaLite/actions/workflows/build.yml/badge.svg?branch=4.0-dev">
    </a>
    <a href="https://t.me/tblite_discuss">
        <img alt="Status" src="https://img.shields.io/badge/-Telegram-blue?logo=telegram&style=flat">
    </a>
</p>

贴吧 Lite 是一个**非官方**的贴吧客户端。

## 说明

**本软件及源码仅供学习交流使用，严禁用于商业用途。**

## 构建

1. 克隆仓库
```shell
git clone https://github.com/0ranko0P/TiebaLite.git
cd TiebaLite
```

2. 配置应用签名 (非必须)

编辑 `signing.properties.example` 填写密钥库路径，密钥别名与密码。保存为 `signing.properties`

3. 开始构建
```shell
./gradle assembleRelease
```

成功后，构建的产物在 `app/build/outputs/apk` 下。


## 友情链接

+ [Starry-OvO/aiotieba: Asynchronous I/O Client for Baidu Tieba](https://github.com/Starry-OvO/aiotieba)
+ [n0099/tbclient.protobuf: 百度贴吧客户端 Protocol Buffers 定义文件合集](https://github.com/n0099/tbclient.protobuf)