# 上传清单（CurseForge / Modrinth）

两个平台都按下面填，差别只在步骤。

## 共用物料

| 物料 | 路径 |
|---|---|
| 产物 | `prefinal/build/libs/prefinal-1.0.jar`（`gradlew build` 生成） |
| 图标 512×512 | `artworks/icon-512.png` |
| 图标 1024×1024（Modrinth 建议源图） | `artworks/icon-1024.png` |
| 页面正文（中文，权威） | `docs/store-description.md` |
| 页面正文（英文，机翻） | `docs/store-description.en.md` |
| 更新日志 | `CHANGELOG.md` |
| 协议 | `LICENSE`（MIT） |
| 第三方说明 | `NOTICE.md` |

## 通用字段

| 字段 | 值 |
|---|---|
| 项目类型 | Mod |
| mod id | `prefinal` |
| 游戏版本 | 1.7.10 |
| 加载器 | Forge（10.13.4.1614） |
| 版本号 | 1.0 |
| 发布类型 | Release |
| 协议 | MIT |
| 环境 | 仅客户端（Modrinth: client `required` / server `unsupported`） |
| 依赖 | 无强制依赖；建议环境注明“OptiFine HD U E7 + 光影包” |
| 分类 | CurseForge：Bug Fixes / Utility；Modrinth：`utility`、`optimization` |
| 梗概 | 见 `docs/store-description.md` 顶部「摘要」 |

## CurseForge 步骤

1. 右上角头像 → **Author Dashboard** → **Create Project** → 选 **Mods** → 游戏 **Minecraft**；
2. 名称 `PreFinal`，Slug 建议 `prefinal`，Summary 粘贴摘要；
3. Description 粘贴正文 Markdown；
4. Upload Image → `artworks/icon-512.png`；
5. Category：`Bug Fixes`（可加 `Utility`）；Game Version：`1.7.10`；Mod Loader：`Forge`；
6. License：MIT（协议文本可上传 `LICENSE`）；
7. **Upload File** → `prefinal-1.0.jar`，File Type `Release`，Game Version `1.7.10`，Mod Loader `Forge`；
8. Changelog 粘贴 `CHANGELOG.md` 的 1.0 段；
9. 提交审核（CurseForge 人工审核，通常几小时到一两天）。

## Modrinth 步骤

1. 右上头像 → **Create a project** → 类型 **Mod**；
2. Name `PreFinal`，Slug `prefinal`，Summary 粘贴摘要；
3. Description 用正文 Markdown（Modrinth 直接支持 Markdown）；
4. Icon 上传 `artworks/icon-512.png`；
5. 分类：`utility`、`optimization`；Environment：Client `required`、Server `unsupported`；
6. License：MIT；
7. Versions → **Create version**：Version number `1.0`，Release channel **Release**，
   Loaders 勾 `Forge`，Game version `1.7.10`，上传 `prefinal-1.0.jar`，Changelog 粘贴；
8. 提交（Modrinth 审核较快，一般几分钟到几小时）。

## 提交前自检

- [ ] `gradlew.bat build` 通过，jar 内含 `mcmod.info` 与 `logo.png`
- [ ] jar 内 `mcmod.info` 的 `version` 为 `1.0`、`authorList` 为 `linakoer`
- [ ] 干净客户端（仅 Forge + OptiFine）能启动，日志无 `Duplicate mod` / 崩溃
- [ ] 关闭 `fix.enableGlStateManagerFix` 能复现 0x0500（反向验证修复确实起作用）
- [ ] 英文文档已标注为机器翻译，且指向中文原文

## 后续维护

改版本号时同步修改：`prefinal/build.gradle` 的 `version`、`PreFinal.java` 的 `@Mod(version=...)`、
`CHANGELOG.md`，然后 `gradlew build` 重新上传。
