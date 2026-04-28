from pathlib import Path
import re

ROOT = Path(r"D:\learn\pokemon-factory")
MARKER = "项目注释补全说明"


def detect_newline(text: str) -> str:
    return "\r\n" if "\r\n" in text else "\n"


def module_desc(parts) -> str:
    if "pokemon-factory-frontend" in parts:
        return "前端应用"
    if "battle-factory" in parts:
        return "battle-factory 后端模块"
    if "poke-dex" in parts:
        return "poke-dex 后端模块"
    if "user-module" in parts:
        return "user-module 后端模块"
    if "common" in parts:
        return "common 公共模块"
    if "scripts" in parts:
        return "项目脚本目录"
    return "项目根目录"


def classify(path: Path):
    parts = set(path.parts)
    name = path.name
    stem = path.stem
    posix = "/".join(path.parts).lower()
    suffix = path.suffix.lower()

    if suffix == ".vue":
        if "views" in parts:
            return "页面视图文件", "负责页面级状态编排、接口调用结果承接以及子组件协同展示", "建议优先关注页面状态来源、事件分发与子组件依赖关系"
        if "components" in parts:
            return "界面组件文件", "负责局部交互、展示逻辑与对外事件抛出", "建议结合父组件传入的 props 与 emits 一起阅读"
        return "前端单文件组件", "负责把模板、脚本与样式聚合在同一文件中完成一块可维护的界面能力", "建议按 template、script、style 三部分顺序阅读"

    if suffix in {".js", ".ts"}:
        if "composables" in parts:
            return "前端组合式逻辑文件", "负责抽离可复用状态、派生数据和副作用处理流程", "建议结合调用它的页面或组件一起理解数据流"
        if "services" in parts and "modules" in parts:
            return "前端接口模块文件", "负责按业务域封装请求入口，统一屏蔽接口路径与调用细节", "建议对照 contracts 与 httpClient 一起阅读"
        if "services" in parts and "contracts" in parts:
            return "前端接口契约文件", "负责约束接口数据结构、字段命名和适配规则", "建议重点关注字段标准化与容错处理逻辑"
        if "services" in parts:
            return "前端基础服务文件", "负责请求、缓存、会话或资源地址等基础能力封装", "建议关注对上层暴露的统一调用入口"
        if "router" in parts:
            return "前端路由配置文件", "负责页面路由注册、导航入口和访问组织方式", "建议结合页面文件一起理解路由层级"
        if "config" in parts:
            return "前端配置文件", "负责环境变量、运行参数或构建期配置映射", "建议关注不同环境下的取值来源"
        if name in {"main.js", "main.ts"}:
            return "前端启动入口文件", "负责创建应用实例并挂载全局依赖、样式和路由", "建议从这里把握前端启动链路"
        if name.startswith(("vite.config", "eslint.config", "tailwind.config", "postcss.config")):
            return "前端工程配置文件", "负责构建、校验或样式工具链的项目级配置", "建议在修改工程能力前先理解这里的默认规则"
        return "前端脚本文件", "负责承载页面逻辑、公共方法或工程运行配置", "建议结合其被引用的位置理解上下游数据流"

    if suffix == ".css":
        return "前端样式文件", "负责全局样式基线、主题变量或页面公共视觉规则", "建议结合组件类名与设计约束一起阅读"

    if suffix == ".java":
        if "controller" in parts:
            return "后端控制器文件", "负责承接 HTTP 请求、整理参数并调用业务层返回统一响应", "建议先看接口入口方法，再追踪到 service 层"
        if "service" in parts and "impl" in parts:
            return "后端业务实现文件", "负责落地具体业务流程，通常会组合 Mapper、工具类与领域模型", "建议关注事务边界、核心分支和依赖协作"
        if "service" in parts:
            return "后端业务服务文件", "负责定义或承载模块级业务能力，对上层暴露稳定服务接口", "建议结合控制器和实现类一起阅读"
        if "mapper" in parts:
            return "后端数据访问文件", "负责声明数据库访问接口或对象映射能力", "建议结合对应 XML 或 SQL 结果结构一起理解"
        if "config" in parts:
            return "后端配置文件", "负责模块启动时的 Bean、序列化、数据源或异常处理配置", "建议优先关注对运行期行为有全局影响的配置项"
        if "security" in parts:
            return "后端安全配置文件", "负责认证鉴权、过滤链或安全边界相关逻辑", "建议重点关注请求进入系统前的校验链路"
        if "exception" in parts:
            return "后端异常处理文件", "负责定义业务异常类型或统一异常响应策略", "建议结合控制器返回格式一起阅读"
        if "engine" in parts and "event" in parts:
            return "对战事件机制文件", "负责抽象对战事件、事件总线或事件处理契约", "建议结合 BattleEngine 主流程理解触发时机"
        if "engine" in parts and "effect" in parts:
            return "对战易失状态文件", "负责抽象 volatile 状态、状态规范或状态管理器能力", "建议结合对战状态读写入口一起阅读"
        if "engine" in parts:
            return "对战引擎文件", f"负责 {stem} 所在的对战规则拆分逻辑，用于从主引擎中拆出独立的规则处理职责", "建议先理解该文件的入口方法，再回看 BattleEngine 中的调用位置"
        if "util" in parts:
            return "工具类文件", "负责承载跨模块复用的通用计算或辅助处理逻辑", "建议关注输入输出约束与可复用边界"
        if "response" in parts:
            return "公共响应封装文件", "负责统一接口返回结构、响应码或导入结果表达方式", "建议结合控制器层返回格式一起阅读"
        if "model" in parts:
            return "领域模型文件", "负责表达数据库实体、核心领域对象或计算过程中的数据结构", "建议关注字段语义与上下游使用方式"
        if "dto" in parts:
            return "数据传输对象文件", "负责在不同层之间传递精简且稳定的数据结构", "建议关注字段是否与接口或业务流程一一对应"
        if "vo" in parts:
            return "视图对象文件", "负责封装面向接口返回或查询条件的展示层数据结构", "建议结合控制器和 service 的组装逻辑一起阅读"
        if name.endswith("Application.java"):
            return "模块启动入口文件", "负责 Spring Boot 应用启动与基础自动装配入口定义", "建议把它当作模块运行的总入口理解"
        if name.endswith("Test.java") or "src/test" in posix:
            return "测试文件", "负责验证目标模块的边界条件、回归行为与核心输出稳定性", "建议先看测试名称，再看构造数据与断言意图"
        return "后端源码文件", "负责承载所属模块中的具体实现逻辑或数据结构定义", "建议结合所在包路径理解它在整体架构中的位置"

    if suffix == ".xml":
        if "mapper" in parts:
            return "数据库映射配置文件", "负责声明 SQL、结果映射和 Mapper 接口之间的绑定关系", "建议结合对应 Java Mapper 接口与数据模型一起阅读"
        return "XML 配置文件", "负责提供项目运行期所需的结构化配置或映射定义", "建议关注节点结构与外部引用关系"

    if suffix in {".yml", ".yaml"}:
        return "YAML 配置文件", "负责声明模块运行参数、环境配置和基础设施开关", "建议优先确认不同 profile 或环境变量的覆盖关系"

    if suffix == ".py":
        return "Python 脚本文件", "负责数据库初始化、辅助启动或项目维护类自动化任务", "建议执行前先确认输入路径与副作用范围"

    if suffix == ".ps1":
        return "PowerShell 脚本文件", "负责 Windows 环境下的启动或运维辅助流程", "建议执行前确认当前目录与依赖环境变量"

    if suffix == ".conf" or "Dockerfile" in name or suffix == ".module":
        return "部署与运行配置文件", "负责容器、代理或运行环境相关的基础配置", "建议关注端口、路径与镜像/进程行为设置"

    return "项目源文件", "负责承载当前目录下的实现或配置职责", "建议结合所在模块与调用方一起理解"


def build_comment(path: Path, style: str, newline: str) -> str:
    rel = path.relative_to(ROOT)
    module = module_desc(rel.parts)
    category, duty, hint = classify(rel)
    title = f"{path.stem or path.name} 文件说明"
    lines = [
        title,
        f"所属模块：{module}。",
        f"文件类型：{category}。",
        f"核心职责：{duty}。",
        f"阅读建议：{hint}。",
        "项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。",
    ]
    if style == "java":
        body = ["/**"] + [f" * {line}" for line in lines] + [" */"]
    elif style == "html":
        body = ["<!--"] + [f"  {line}" for line in lines] + ["-->"]
    elif style == "block":
        body = ["/*"] + [f" * {line}" for line in lines] + [" */"]
    elif style == "xml":
        body = ["<!--"] + [f"  {line}" for line in lines] + ["-->"]
    else:
        body = [f"# {line}" for line in lines]
    return newline.join(body) + newline + newline


def style_for(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix == ".java":
        return "java"
    if suffix == ".vue":
        return "html"
    if suffix in {".js", ".ts", ".css"}:
        return "block"
    if suffix == ".xml":
        return "xml"
    return "hash"


def strip_leading_block(text: str, prefix_pattern: str) -> str:
    pattern = re.compile(prefix_pattern, re.S)
    m = pattern.match(text)
    return text[m.end():] if m else text


def rewrite_header(path: Path, text: str) -> str:
    newline = detect_newline(text)
    style = style_for(path)
    comment = build_comment(path, style, newline)
    suffix = path.suffix.lower()

    if suffix == ".java":
        m = re.match(r'^(package\s+[^;]+;\s*)', text, re.S)
        if m:
            tail = text[m.end():]
            tail = tail.lstrip("\r\n \t")
            tail = strip_leading_block(tail, r'^/\*\*.*?\*/(?:\r?\n\s*)*')
            return text[:m.end()] + newline + comment + tail
        stripped = strip_leading_block(text.lstrip("\r\n \t"), r'^/\*\*.*?\*/(?:\r?\n\s*)*')
        return comment + stripped

    if suffix == ".xml":
        if text.startswith("<?xml"):
            end = text.find("?>")
            if end != -1:
                head = text[:end + 2]
                tail = text[end + 2:]
                tail = tail.lstrip("\r\n \t")
                tail = strip_leading_block(tail, r'^<!--.*?-->(?:\r?\n\s*)*')
                return head + newline + comment + tail
        stripped = strip_leading_block(text.lstrip("\r\n \t"), r'^<!--.*?-->(?:\r?\n\s*)*')
        return comment + stripped

    if style == "html":
        stripped = strip_leading_block(text.lstrip("\r\n \t"), r'^<!--.*?-->(?:\r?\n\s*)*')
        return comment + stripped

    if style == "block":
        stripped = strip_leading_block(text.lstrip("\r\n \t"), r'^/\*.*?\*/(?:\r?\n\s*)*')
        return comment + stripped

    stripped = re.sub(r'^(?:\s*#.*\r?\n)+\s*', '', text, count=1)
    return comment + stripped.lstrip("\r\n")


def collect_files():
    include_files = []
    patterns = [
        ROOT / 'pokemon-factory-backend',
        ROOT / 'pokemon-factory-frontend' / 'src',
        ROOT / 'scripts',
    ]
    file_suffixes = {'.java', '.vue', '.js', '.ts', '.css', '.py', '.ps1', '.xml', '.yml', '.yaml', '.conf'}
    extra_files = [
        ROOT / 'pokemon-factory-frontend' / 'vite.config.js',
        ROOT / 'pokemon-factory-frontend' / 'eslint.config.js',
        ROOT / 'pokemon-factory-frontend' / 'tailwind.config.js',
        ROOT / 'pokemon-factory-frontend' / 'postcss.config.js',
        ROOT / 'pokemon-factory-frontend' / 'nginx.conf',
        ROOT / 'pokemon-factory-backend' / 'pom.xml',
        ROOT / 'pokemon-factory-backend' / 'Dockerfile.module',
        ROOT / 'docker-compose.local.yml',
    ]
    for base in patterns:
        if base.exists():
            for path in base.rglob('*'):
                if path.is_file() and path.suffix.lower() in file_suffixes and 'target' not in path.parts and 'node_modules' not in path.parts:
                    include_files.append(path)
    for path in extra_files:
        if path.exists():
            include_files.append(path)
    return sorted(set(include_files))


def main():
    modified = 0
    for path in collect_files():
        original = path.read_text(encoding='utf-8')
        updated = rewrite_header(path, original)
        if updated != original:
            path.write_text(updated, encoding='utf-8')
            modified += 1
    print(f'rewritten={modified}')


if __name__ == '__main__':
    main()

