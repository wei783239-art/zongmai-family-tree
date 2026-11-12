# 电子族谱项目交接

## 2026年11月12日接手后的最新状态

用户已明确授权助手直接完成后续开发。当前正式路由已切换到 `DashboardView.vue`、`TreeView.vue`、`MembersView.vue`、`EventsView.vue`、`AlbumView.vue`、`SettingsView.vue`，保留原页面源码供核对。新增可交互G6图谱、人物资料编辑、收养关系、家族账号管理、最后管理员保护、审核申请、大事记、相册文献、头像、私密资料过滤、统计、CSV/JSON及PNG导出、公开简介与系统账号启停。

前端固定G6 5.1.1，实际使用JavaScript。认证继续使用Session与CSRF。新增 `application-demo.yml` 使用H2文件数据库，独立于原有MySQL。Java打包文件更新为 `backend/target/family-tree.jar`。

2026年11月12日完成前端视觉重做：新增宣纸、墨玉、朱砂印章主题与Element Plus全局变量，侧栏改为印章品牌与深色导航，顶部导航显示当前页面，家族总览加入家族主视觉、统计卡和世代分布，世系图、成员档案、大事记、相册、审核设置以及登录、注册、公开页统一为卡片式布局。侧栏标识改为 `frontend/src/assets/brand-seal.svg`，登录页移除了旧的心理健康英文文案和不匹配的AI配图。生产构建已更新，最终21项浏览器验收在本机Edge中全部通过。

从项目根目录运行 `tools/Start-Demo.ps1` 启动5175前端和9080演示后端。默认启动方式已经切换为 `vite preview` 托管 `frontend/dist` 的生产预览，即21项浏览器验收使用的同一页面；只有显式传入 `-Development` 才改用Vite开发服务器。演示账号为 `demo_admin`、`demo_member`、`demo_outsider`，初始密码 `Demo123!`。演示数据包含36位虚构人物、五代、57条关系与2件事件。MySQL版本使用 `tools/Start-MySQL.ps1` 并交互输入数据库密码。新增字段通过 `SchemaUpgrade` 检查补充，首次升级需要数据库DDL权限。

已完成H2环境42项HTTP检查、独立MySQL 8.4.9环境42项HTTP检查、4项图谱算法检查及21项生产构建浏览器操作验收。无头Chrome在本机运行到后半程时被系统关闭，最终复核改用本机Edge完成，21项全部通过。独立MySQL验证还实际执行了 `demo-data.sql`。原有业务MySQL的密码未取得，原库升级后的现场验证仍需用户输入正确密码启动。

最终收尾时已核对交付压缩包：`交付包/宗脉电子族谱-源码与演示包.zip` 内的源码、`backend/target/family-tree.jar`、`frontend/dist`、`demo-data.sql`、交付文档、截图和三份验收记录均与交付清单的记录一致，且不含本地数据库、`node_modules`、测试日志或临时MySQL数据目录。可使用 `python tools/verify_delivery.py` 重新核对。此前验收生成的五个临时MySQL数据目录已清理，`python tools/verify_mysql.py` 会重新创建一次性实例并在结束时删除。`tools/Start-Demo.ps1` 与 `tools/Start-MySQL.ps1` 已改为带UTF-8 BOM保存，Windows PowerShell 5.1和PowerShell 7都能正确解析中文提示；默认生产预览启动已在5.1下实测，`frontend/dist` 原样返回且 `/api` 代理正常。

交付资料统一位于 `项目文档/交付资料`，含部署使用说明、需求数据库与接口说明、测试报告、课程报告、演示脚本、实际截图和原始测试记录。`README.md` 已更新到当前实现。课程报告需要按学校格式补充作者信息与个人分析。复杂亲属称谓、GEDCOM和AI问答仍为课题选做拓展。

以下各节记录2026年11月11日的历史交接，可用于回查此前实现与教学过程，当前功能状态以本节和交付资料为准。

交接日期为 2026-11-11，供没有本聊天历史的新工作区继续教学与开发。本文根据本次聊天、旧交接文档和交接时保存的源码更新。

项目根目录为 `C:\Users\pjl18\Documents\ChatGPT\电子族谱`，下文源码路径相对于该目录。正式前端为该目录下的 `frontend/`。用户曾提供错误的桌面前端副本 `C:\Users\pjl18\Desktop\大学项目\电子族谱前端\code\ai-vue`，后续以工作区前端为准。

## 目标

用户是中文编程初学者，希望在老师指导下亲手完成“宗脉 · 电子族谱管理系统”，理解代码并完成毕业设计或综合实训交付。

课题文件为 `C:\Users\pjl18\Desktop\大学项目\电子族谱\电子族谱-项目课题.docx`。项目内另有 `项目文档/电子族谱系统需求文档.docx` 和 `项目文档/电子族谱系统项目计划书.docx`。

必须交付的范围包括真实登录与权限、家族管理、成员档案、父母子女和配偶关系、可交互族谱图、姓名搜索、祖先或后代追溯、至少 30 位虚构人物组成 4–5 代世系，以及源码、数据库脚本、测试结果、部署使用说明、演示材料和课程报告或论文。

大事记、相册、审核、操作日志、统计与导出为推荐扩展。复杂亲属称谓、GEDCOM、AI 问答和扇形图属于后续拓展。

当前技术为 Java 17、Spring Boot 3.3.5、JdbcTemplate、Spring Security、MySQL，以及 Vue 3、JavaScript、Vite、Axios、Element Plus、Vue Router、Pinia。认证采用 Session + CSRF。课题中的 JWT、TypeScript、MyBatis-Plus 是原方案中的技术建议，继续开发时围绕现有实现推进，并在交付文档中写清实际选择。

## 当前停点

**前端已接入真实认证、家族列表与创建、人物资料增删改查、姓名搜索、后端分页，以及亲子和配偶关系的查询、新增、删除。最近新增了人物祖先与后代查询组件。**

最后一次排错发现 `frontend/src/api/family.js` 中 `listAncestors` 和 `listDescendants` 各重复声明两次。已给用户手动删除重复定义的指导。交接时读取磁盘，两个函数均已各保留一份，Node 语法检查通过。

`PersonLineage.vue` 曾是 0 字节空文件，现已保存完整组件。成员表格已导入并传入 `familyId`、`row` 和 `savingPerson`，显示“祖先”“后代”按钮。

**祖先、后代功能在最后去重修复后的浏览器验证结果尚未收到用户反馈。接手时先确认这一小项，再进入族谱图。** 不能把组件解析通过写成整个追溯流程已经验收。

族谱图尚未开始实现。`frontend/src/views/knowledge.vue` 虽然菜单叫“族谱关系图”，实际仍是旧知识文章分类和搜索占位代码，调用 `/knowledge/catagory/tree`。当前没有安装 G6 或 D3，也没有 `/api/families/{familyId}/tree` 接口。

用户最后要求编写本交接文档。下一步已经向用户预告为“开始接入族谱图”，具体图形库与实现方案尚未约定。

## 协作方式

- 默认由助手读取实际文件、简要讲解原理、给出精确位置和可复制代码，用户亲手编辑、保存、打包和测试。用户明确说过“这些改动不需要你来改，你把代码复制出来我来改”。
- 本交接文档由用户明确要求直接编写。用户也曾单独授权助手修改 Codex 上下文配置。继续修改业务代码时仍按手动教学方式配合。
- 用户后来取消了“每次最多修改一两处”的限制。最新偏好是兼顾速度和质量，每次输出控制长度，避免一次给大量文件和代码。相关错误可以同批集中修正。
- 给代码时说明是替换整个函数、整个文件，还是插在某个标签或方法前后。需要行号时先读当前文件，避免使用旧行号。
- 用户经常遗漏保存、拼错属性与变量、漏掉函数、重复粘贴函数。编译器读取磁盘内容，发现异常先核对真实保存的源码。
- 用户说“下一步”通常表示沿当前项目继续推进，但不能据此宣称上一轮每一项边界测试都已通过。
- 用户会把“结果正确”作为简短验证反馈。交接时保留反馈范围，不扩大为全项目验收。
- 用户原先说过前端主体页面暂缓，本轮已经明确推进了真实业务页面。继续围绕课题功能教学，视觉重设计尚未提出。
- 用户经常关闭 PowerShell。后端新窗口需要重新设置正确的 `DB_PASSWORD`。
- 用户曾说“给我打开前端和后端的代码”，实际指启动命令。
- Git 文件当前仍主要是 untracked，尚无固定提交可作为验收基线。提交、分支切换和清理尚未获本轮授权。

## 已改文件

下面记录本会话涉及的新增或修改范围。当前仓库缺少可用于比较的已提交基线，不能把此表当作精确 Git diff。

| 文件 | 当前用途与改动 |
|---|---|
| `frontend/vite.config.js` | 移除 `mockLogin`、模拟账号常量及插件调用，`/api` 代理指向 `http://localhost:8080`，路径别名使用 `fileURLToPath`。顶部仍有两行失效的 mock 说明注释 |
| `frontend/src/api/admin.js` | 真实 `login`、`getCurrentUser`、`logout`、`register` 封装。旧 `categoryTree` 仍被占位图谱页使用 |
| `frontend/src/utils/request.js` | 写请求先取得 CSRF，再发送业务请求，成功响应直接返回 `response.data`，统一显示接口错误，按 HTTP 401 处理失效登录 |
| `frontend/src/views/login.vue` | 用户名改为 `username`，调用真实登录，移除 token 存取和包含密码的表单日志，补 loading 与重复点击保护 |
| `frontend/src/views/register.vue` | 从占位页改为真实注册表单，用户名和密码必填，详细规则由后端校验，成功后跳转登录页 |
| `frontend/src/stores/admin.js` | 在折叠状态之外增加 `user`，保存当前账号身份 |
| `frontend/src/main.js` | 先安装 Pinia，再安装 Router，供首次路由守卫使用 Store |
| `frontend/src/router/index.js` | `/back` 标记 `requiresAuth`，进入时调用 `/auth/me` 并更新 Store，身份查询失败返回登录页 |
| `frontend/src/components/Navbar.vue` | 显示真实用户名，调用后端退出并清理 Store，折叠图标读取 Pinia 状态 |
| `frontend/src/views/dashboard.vue` | 家族列表、加载失败和空状态、创建家族弹窗、刷新、通过 query.familyId 进入成员页。创建表单当前只录入名称与姓氏 |
| `frontend/src/views/consultations.vue` | 人物完整资料的新增、编辑、删除，姓名查询，后端分页，加载和空状态，挂载关系与追溯组件 |
| `frontend/src/components/FamilyRelations.vue` | 新组件。关系列表，父母子女与配偶下拉选择，提交新关系，确认删除关系，复用全家族人物列表 |
| `frontend/src/components/PersonLineage.vue` | 新组件。每个人物的祖先和后代按钮，弹窗显示查询结果，含加载、失败和空状态 |
| `frontend/src/api/family.js` | 保留家族、人物、分页、亲子关系等封装，新增配偶、祖先、后代封装。最新重复声明已移除 |
| `frontend/package.json`、`frontend/package-lock.json` | 用户安装 `@vue/tsconfig` 修复配置依赖缺失，并执行 `npm audit fix` 更新依赖锁定结果 |
| `backend/src/main/java/com/zongmai/familytree/RegisterRequest.java` | 用户要求密码下限从 12 改为 6，当前 `min = 6`、`max = 64`，错误提示已同步 |
| `backend/src/main/java/com/zongmai/familytree/AuthController.java` | 清理未使用的 `java.security.Security` 导入，真实认证主体逻辑沿用既有实现 |
| `HANDOFF.md` | 更新为本次交接 |

工作区外另有一次明确授权的配置修改。`C:\Users\pjl18\.codex\config.toml` 的 `model_context_window` 已由 1000000 改为 500000，写入后读回确认。是否已经重启 Codex、当前聊天是否加载了该配置，尚未确认。

## 当前实现与数据约定

### 登录与权限

- `sys_user` 是登录账号，`person` 是族谱人物，两者编号体系独立。
- `family_user` 用 `family_id`、`user_id`、`role` 记录账号与家族关联，角色为 `MEMBER` 和 `FAMILY_ADMIN`。
- 启用账号可创建家族，创建者自动获得该家族的 `FAMILY_ADMIN` 关联。家族列表仅返回当前账号关联的家族。
- `MEMBER` 可读取，`FAMILY_ADMIN` 可维护。无关联账号访问目标家族返回 403。不存在的家族也可能先被权限检查拒绝为 403。
- Controller 类使用 `@PreAuthorize("denyAll()")`，每个家族接口单独声明规则。若新增图谱等接口，要配置对应的 `canRead` 或 `canManage`。
- 新建账号没有家族时，列表返回空数组。当前前端还没有账号与家族关联管理界面或邀请流程。

Session Cookie 为 `JSESSIONID`，HttpOnly、SameSite Lax，闲置超时 30 分钟。后端内存保存会话，重启后需要重新登录。

`GET /api/auth/csrf` 返回 `{headerName,token}`。登录会清除旧 CSRF，后续写请求需要当前会话的新令牌。当前前端在每次 POST、PUT、PATCH、DELETE 前，用原始 Axios 获取 CSRF，再向业务请求附加返回的请求头。

CSRF 获取使用原始 `axios.get('/auth/csrf', {baseURL: service.defaults.baseURL, timeout: service.defaults.timeout})`，因此拿到的是 Axios 响应。业务请求经过 service 的响应拦截器，调用方直接拿到 JSON 数据。此前把两种返回值混用或用 silentError 静默内层错误的示例已经替换。

浏览器通过前端 `/api` 代理请求后端，Cookie 自动随同源请求发送。PowerShell WebSession 与浏览器会话各自保存 Cookie。

`/auth/me` 返回 `{id,username}`。路由守卫处理其 401，所以请求拦截器对该接口的 401 直接拒绝 Promise。其他业务 401 返回登录页，登录失败留在登录表单并显示错误。请求层的通用兜底文案为“请求失败，请稍后重试”。

当前注册用户名为 3–32 位英文字母、数字或下划线。密码为 6–64 个 Unicode 码点，UTF-8 编码后最多 72 字节，BCrypt 成本参数为 12。重复用户名返回 409。登录没有另外增加密码最少长度限制。

### 家族和人物

- 家族表单当前只提交 `name`、`surname`，后端还支持 `ancestral_hall`、`hometown`、`description`。始祖字段由单独接口维护。
- 正式成员地址为 `/back/consultations?familyId=实际家族编号`。直接点击侧栏成员档案而未带编号时，页面提示先从总览选择家族。
- 人物保存字段为 `name`、`gender`、`generation`、`birth_date`、`death_date`、`birth_place`、`biography`。
- 性别为 MALE、FEMALE、UNKNOWN，也支持后端已有 NULL 数据。世代可空，填写时必须为正整数。出生、逝世日期可空，逝世日期不能早于出生日期。
- Element Plus 日期输入使用 `value-format="YYYY-MM-DD"`，后端 LocalDate 使用严格的 `uuuu-MM-dd` 解析。
- PUT 会覆盖当前 DTO 中的全部资料。现有表单已回填全部七个字段并统一提交，增加新字段时同样注意保留已有资料。
- 姓名搜索由后端 `LIKE` 查询完成。分页返回 `{records,total,page,size}`，前端使用 `records` 展示。
- `searchText` 是输入内容，`searchQuery` 是已提交条件，当前包含 `name`、`page`、`size`。搜索、重置、调整每页条数会回到第一页。
- 监听 `[familyId,searchQuery]` 发起查询，onCleanup 标记旧请求失效，避免旧响应覆盖新查询。查询发现页码超过最后一页时自动调整。
- 新增后回到当前搜索结果第一页，编辑后刷新当前页。删除后也重新查询总数和当前页，最后一页删空时回退到有效页码。
- 成员分页页使用 `pagePersonsByFamily`。关系下拉选项继续使用全量 `listPersonsByFamily`，未来绘制完整家族图也需要完整节点数据。
- 人物作为始祖或存在关联关系时，删除可能返回 409，后端会提供具体提示。

### 亲属关系与追溯

`FamilyRelations.vue` 由成员页传入字符串 familyId。打开后同时获取全家族人物和关系，选项显示人物姓名与编号，以区分同名人物。

亲子新增请求为 `{parentPersonId,childPersonId}`，配偶新增为 `{personId,spousePersonId}`，编号以 JSON 数字提交。关系删除使用关系的 `id`。关系列表返回 `id`、`from_person_id`、`from_person_name`、`to_person_id`、`to_person_name`、`relation_type`、`status`。

PARENT_CHILD 的方向为父母到子女。后端检查自关联、家族归属、重复关系与亲子环路。SPOUSE 按双方编号大小规范化，交换双方重复提交仍返回 409。保存中锁定表单与窗口关闭，删除带确认。

祖先和后代查询只沿 PARENT_CHILD 递归，UNION DISTINCT 去重，排除起点自己，返回平面人物列表。排序字段为 generation、id。配偶关系不参与该递归。

`PersonLineage.vue` 接收 `familyId`、`person`、`disabled`。成员表格的操作列当前容纳编辑、删除、祖先、后代。组件弹窗使用 append-to-body，避免嵌在表格单元格内影响显示。

## 关键接口

表内路径均带 `/api` 前缀。

| 方法与路径 | 请求或返回重点 |
|---|---|
| GET `/api/auth/csrf` | 返回 headerName、token |
| POST `/api/auth/register` | username、password，成功 201 |
| POST `/api/auth/login` | username、password，成功返回 message、user |
| GET `/api/auth/me` | 返回 id、username，未登录 401 |
| POST `/api/auth/logout` | CSRF 必需，成功 200 |
| GET、POST `/api/families` | 当前账号家族列表、创建家族 |
| GET、PUT、DELETE `/api/families/{familyId}` | 家族详情、修改、删除 |
| GET、POST `/api/families/{familyId}/persons` | 全量人物列表与可选 name 筛选、新增人物 |
| GET `/api/families/{familyId}/persons/page` | page 从 1 开始，size 为 1–100，支持 name |
| GET、PUT、DELETE `/api/families/{familyId}/persons/{personId}` | 人物详情、覆盖更新、删除 |
| GET、POST `/api/families/{familyId}/relations` | 关系列表、新增亲子关系 |
| POST `/api/families/{familyId}/relations/spouse` | 新增配偶关系 |
| DELETE `/api/families/{familyId}/relations/{relationId}` | 删除关系 |
| GET `/api/families/{familyId}/persons/{personId}/ancestors` | 祖先列表 |
| GET `/api/families/{familyId}/persons/{personId}/descendants` | 后代列表 |
| PUT、DELETE `/api/families/{familyId}/ancestor` | 指定始祖 `{personId}`、取消始祖，前端尚未接入 |
| GET `/api/health` | 健康检查，200，当前不访问数据库 |

后端包名为 `com.zongmai.familytree`。`ParentChildRelationRequset` 的实际拼写就是 Requset，引用一致。前端 `updateFamliy`、部分 Java 方法名也有历史拼写，重命名时要同步调用方。

## 验证结果

### 用户反馈与历史结果

| 项目 | 已有证据与范围 |
|---|---|
| 后端家族级权限 | 旧工作区用户明确报告 58 项 HTTP 状态检查与数据断言全部通过，涵盖管理员、只读成员、无关联账号、Session、CSRF、家族删除失败事务回滚等。本轮未重跑整套写库脚本 |
| 浏览器真实登录与 Session | 用户明确反馈登录成功，并在同一浏览器打开 `/api/auth/me` 得到正确身份 |
| 真实退出 | 用户按退出、刷新身份接口得到 401、再次登录的步骤验证后反馈结果正确 |
| 路由与用户名 | 修复缺少 getCurrentUser 导出及 requiresAuth 拼写后，用户反馈结果正确并继续 |
| 注册 | 数据库认证错误修复后，用户反馈结果正确并继续。已给出 5 字符拒绝、6 字符成功的测试数据，未保存逐条测试日志 |
| 家族、人物、搜索、分页、关系 | 用户逐轮操作并多次反馈结果正确或继续，期间发现并修正了多个保存与拼写问题。源码已核对，未形成完整的逐项前端验收报告 |
| npm 依赖审计 | 用户安装 @vue/tsconfig 后报告三项高危依赖，执行 npm audit fix，再明确反馈 found 0 vulnerabilities。这是当时的审计结果，交接时未重新联网审计 |
| 最新祖先与后代窗口 | 空文件和重复导出已修复，尚缺最后一次修复后的浏览器查询反馈 |

### 本次交接实际执行的检查

- `frontend/src/api/family.js` 通过 `node --input-type=module --check`，最后的重复导出问题已消除。
- 使用当前项目的 `@vue/compiler-sfc` 对 login.vue、register.vue、dashboard.vue、consultations.vue、Navbar.vue、FamilyRelations.vue、PersonLineage.vue 运行 parse、compileScript、compileTemplate，七个文件均通过。
- HTTP 读取前端首页 `http://localhost:5173/` 返回 200。
- HTTP 读取前端转换后的 PersonLineage.vue 模块返回 200。
- 后端 `http://localhost:8080/api/health` 返回 200。
- 使用无 Cookie 的独立请求访问 `/api/auth/me` 返回预期 401。

这些检查覆盖源码解析与基础服务响应。本次交接没有运行浏览器自动化、完整前端构建、Maven 打包、JUnit 套件或需要写入数据库的整套业务测试，也没有查询当前数据库全部账号、人物与家族数据。

## 已定位并修复的故障

- 注册、登录 500 曾出现 `Access denied for user 'family_app'@'localhost' (using password: YES)`。MySQL 当时运行，正确设置后端窗口的 DB_PASSWORD 并重启后，用户确认恢复。网站测试密码与数据库账号密码分别管理。
- 后端未启动时，前端代理 `/api/auth/me` 返回 502。旧路由守卫 return false 取消首次导航导致白屏，现改为转到登录页。
- `getCurrentUser` 只导入未导出曾使路由模块加载失败；现已补齐。
- 多次模板或变量拼写错误已经指导修正，包括 uesname、loadig、ractive、v-id、el-sketon、filterabel、realtions、reequiresAuth、upadte、createDialogVisiable、confirmButtontext。
- `personDialogVisible = false` 曾错误赋值给 ref 常量，现脚本使用 `.value`。
- 搜索曾把 `searchQuery.value` 写成 `searchQuery,value`，现已修正。
- 分页曾把 `Math.max` 写成不存在的 MatcherPatternPathDynamic.max，查询失败后隐藏新增按钮与表格，现已修正。
- 分页改造后遗留的 listPersonsByFamily 调用、删除后的 search.value 已移除或修正，当前通过 searchQuery 对象重新赋值触发刷新。
- 日期格式曾写成 YYYT-MM-DD，现出生与逝世日期均为 YYYY-MM-DD。
- PersonLineage.vue 曾为空文件，现内容完整。最后两个 API 函数重复声明的问题已通过语法检查确认修复。

JS 错误发生在 HTTP 写入成功后的刷新阶段时，数据库可能已经保存或删除成功。排错后先查看现有记录，再决定是否重试写入。

## 尚未解决或尚未验收的点

1. 祖先、后代最新去重修复后的浏览器查询结果待用户确认，优先做甲→乙→丙的两级追溯验证。
2. 可交互族谱图尚未实现，当前 knowledge.vue 是旧模板，没有图谱组件、图形库或图谱数据接口。
3. 家族详细信息编辑、删除及始祖设置目前已有后端能力，前端只完成列表和名称、姓氏创建。
4. 家族账号关联管理 API 与 UI、邀请、最后管理员保护、系统管理员角色尚未实现。当前普通成员关联仍依赖历史测试 SQL 准备。
5. 前端写按钮尚未根据家族角色隐藏或禁用，adminStore.user 只含账号 id、username。实际写权限由后端检查，MEMBER 点击写操作会被 403 拒绝。角色能力信息尚需设计并接入 UI。
6. 30 位以上虚构人物和 4–5 代完整演示数据未统一准备，也未核实目前已有多少测试人物与家族。
7. 大事记页 emotional.vue 仍是占位内容。相册、审核、日志、统计、导出、头像上传等尚待安排。
8. 逐人物 PUBLIC/FAMILY/PRIVATE 权限和游客公开访问尚未落实；当前为家族级权限。
9. 停用账号已有登录和家族检查，但完整会话撤销、账号管理和限流尚待完善。旧权限脚本没有完整验证 DISABLED 分支。
10. 亲子环路检查主要针对顺序请求，并发冲突、极深递归、婚姻历史和收养等边界尚未解决或充分测试。
11. DB 与部分框架 500 异常尚未全部统一为带 message 的 JSON，前端会显示通用兜底提示。发生 500 应读取后端 ERROR、SQLException 和 Caused by。
12. 顶部旧 mock 注释、Navbar 旧退出说明、BackendLayout 旧 provide 折叠状态等有遗留注释或冗余，可在相关维护时整理。
13. README.md 与旧功能进度文件已经落后，完整测试报告、数据库迁移与演示数据脚本、部署文档、截图和论文仍待更新。

## 下一步

1. 先确认最后的追溯修复。用已有虚构关系甲→乙→丙，检查甲的后代包含乙、丙，丙的祖先包含甲、乙，乙的两侧分别为甲与丙。不要为确认历史状态重跑旧权限脚本。
2. 开始族谱图的数据接入。保持 familyId 跟随家族选择，替换 knowledge.vue 的旧知识分类逻辑，并设计从家族总览或成员页进入图谱的入口。
3. 获取完整家族人物与关系作为节点和边。人物分页结果只包含当前页，绘制全家族图需要全量或专门图谱接口。可复用现有两个读取接口，或新增受 canRead 保护的 `/api/families/{familyId}/tree`，具体方案下一轮结合用户节奏确定。
4. 选择图形库并阅读其当前版本文档，再给用户安装命令和对应版本 API。课题建议 G6 或 D3，目前均未安装。人物可能有两位父母和配偶连接，布局要支持这些关系与多个分支，按真实关系生成连接。
5. 先显示人物姓名与亲子、配偶连线，再分轮加入缩放、拖动、选中节点、详情侧栏、指定人物或始祖展开、折叠和操作后刷新。generation 可作为展示资料，亲缘计算继续依据关系。
6. 随后补齐家族资料维护、角色 UI 与账号关联管理、演示数据和交付材料。原交接推荐的账号关联管理还未实施，当前聊天已经把近期方向推进到族谱图。

## 启动与排错

前后端使用两个 PowerShell 窗口。已有服务运行时确认端口，避免重复启动。后端通常为 8080，前端通常为 5173，以 Vite 日志为准。

Java 有修改时，先保存，在原后端窗口 Ctrl+C 停止旧进程，再打包。

```powershell
cd 'C:\Users\pjl18\Documents\ChatGPT\电子族谱\backend'
mvn -DskipTests clean package
```

看到 BUILD SUCCESS 后，输入 MySQL 数据库账号 family_app 的密码。Read-Host 输入完成后再执行下一段。

```powershell
$securePassword = Read-Host -Prompt '请输入 MySQL 账号 family_app 的密码' -AsSecureString
```

```powershell
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
java -jar '.\target\family-tree-0.0.1-SNAPSHOT.jar'
```

新 PowerShell 窗口需要重新设置环境变量。只修改前端时，后端已有 JAR 可直接启动。数据库密码不写入文档，用户在本机交互输入。当前 MySQL 服务历史确认名称为 MySQL84，数据库为 family_tree，客户端路径为 `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`。

前端窗口运行。

```powershell
cd 'C:\Users\pjl18\Documents\ChatGPT\电子族谱\frontend'
npm run dev
```

访问 `http://localhost:5173/auth/login` 登录。健康接口成功仅说明后端能够响应，注册或登录才会实际触发账号数据库查询。浏览器地址栏发送 GET，登录与注册接口需要 POST。

发生空白或加载失败时，优先核对磁盘文件、编译错误、前端 Network 的状态与响应、后端对应错误。当前会话反复出现保存遗漏和拼写问题，先集中读取相关文件能更快定位。

## 测试账号与数据保护

旧交接记录的本地虚构测试账号为 `reg_test_20261102`，密码 `RegisterTest123!`，历史最近确认的 sys_user.id 为 2。本会话中用户用它验证过浏览器登录，交接时未重新查库确认账号编号。

本轮给用户的注册边界测试数据为 `study_test_5 / Ab123` 和 `study_test_6 / Ab1234`。前者预期因 5 字符被拒绝，后者用于 6 字符注册。用户曾在修复数据库密码后反馈结果正确，但这两个用户名的当前存在状态和实际 ID 未通过本次交接查库核实。

用户本轮通过网页创建的家族、人物和关系的具体 ID 没有统一记录，也没有执行统一清理。接手时读取实际 API 返回的 ID，保留已有测试成果。

旧权限脚本为 `backend/test-family-permissions.ps1`。上轮成功测试的家族 13 PermissionTest、PermissionMember 和临时账号关联已经清理。脚本默认目标 13 已失效，重测要创建新的 PermissionTest 家族，读取实际 ID，再显式传入 `-FamilyId`。脚本会写入和清理测试数据，计数可能随准备状态变化。

历史演示家族 1、人物 1/2/4、关系 1 应保留。旧记录为人物 1 林一、2 林二、4 林三，关系 1 是 1→2 的 PARENT_CHILD，始祖为人物 1。本轮没有重新核实这些记录，也未确认账号 2 目前是否关联家族 1。自增 ID 不连续正常，使用实际编号。

换工作区时若仍使用本机原目录，可以沿用现有环境。复制源码到其他机器时，还需要迁移或初始化 MySQL 数据、family_app 账号及授权，并准备 Java、Maven、Node 和依赖。现有 schema.sql 包含表结构，未负责创建 family_app 登录账号。
