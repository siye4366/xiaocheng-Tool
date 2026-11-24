# 版本规则

校程（英文名 xiaocheng-Tool）使用语义化版本 MAJOR.MINOR.PATCH，应用显示名始终为「校程」。

## 何时递增

| 位 | 递增条件 | 例子 |
|---|---|---|
| MAJOR | 不兼容变更：数据结构、minSdk、包名/签名、大改版 | 2.0.0 |
| MINOR | 新增用户可见功能 | 1.1.0 |
| PATCH | 只修 Bug、文案、样式 | 1.0.1 |

## versionCode

    versionCode = MAJOR * 10000 + MINOR * 100 + PATCH

| 版本 | versionCode |
|---|---|
| 1.0.0 | 10000 |
| 1.0.1 | 10001 |
| 1.1.0 | 10100 |
| 2.0.0 | 20000 |

## 命名规范

| 对象 | 规则 | 示例 |
|---|---|---|
| 应用显示名 | 固定「校程」 | 校程 |
| 安装包文件 | xiaocheng-v版本号.apk | xiaocheng-v1.2.2.apk |
| Git tag | v版本号 | v1.2.2 |
| GitHub Release 标题 | v版本号 | v1.2.2 |
| Release 附件 | 与安装包同名 | xiaocheng-v1.2.2.apk |

发布时 tag、Release 标题、APK 文件名三者的版本号必须一致。
