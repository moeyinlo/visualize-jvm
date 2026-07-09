# visualize-jvm 完整 JVMS 26 实现计划

## Summary
- 目标：完整实现 Java SE 26 JVMS，可视化 JVM 执行流程，支持 class/jar classpath、完整类加载/链接/初始化、完整 verifier、完整 opcode interpreter、indy/condy、host delegation、分层 native resolver、simulated JNI、JavaFX GUI。
- 分支策略：直接在本地 `main` 分支小步提交，每个步骤一个 git commit。
- 当前仓库：初始化 Kotlin/Gradle 骨架、README、`jvms26.pdf`。
- 第一执行步骤：创建 `docs/plan.md` 并提交，之后严格按本文件顺序推进。

## Architecture Decisions
- 自写 `:jvm-classfile` 作为核心 ClassFile parser/writer；ASM 只用于测试 oracle 和 fixture 生成。
- `:jvm-runtime` 持有 guest heap、frame、thread、class loader、method area、event/snapshot。
- `:jvm-verifier` 独立实现 JVMS verification，不混入 interpreter。
- `:jvm-interpreter` 完整实现 opcode decode/execute。
- `:jvm-host` 只处理配置为 host-delegated 的类，调用边界是 opaque event。
- `:jvm-native` / `:jvm-jni` 实现 native 分层解析链：
  1. 对白名单类/方法先查 Kotlin intrinsic。
  2. intrinsic miss 后回退到 simulated JNI。
  3. simulated JNI 通过自定义 `JNIEnv` 函数表让 JNI upcall 回到 guest interpreter。
  4. 找不到 native binding 时抛 guest `UnsatisfiedLinkError`。
- GUI 不直接操作 runtime 内部状态，只消费 immutable snapshot 和 event stream。

## Per-Commit Rules
- 每个 commit 只做一个小闭合点，不把多个子系统混在一起。
- 每个 commit 前运行：
  - 对应模块 focused tests。
  - `./gradlew.bat build --console=plain`
  - `git diff --check`
- 每个 commit message 使用 Conventional Commit 风格：`docs:`、`chore(build):`、`feat(...)`、`test(...)`。
- 每个实现 JVMS 条目的 commit 同步更新 `docs/spec-coverage.md`。
- 如果某一步实现预计超过 5 个核心文件或 300 行行为代码，必须继续拆分。

## Commit Sequence

### Phase 0 - Documentation and Build Baseline
1. `docs: add implementation plan` - 创建 `docs/plan.md`，写入本计划。
2. `chore: capture initialized project baseline` - 提交当前 README、Gradle wrapper、`jvms26.pdf`、基础配置。
3. `chore(build): define module layout` - 在 `settings.gradle.kts` 中加入 `jvm-classfile`、`jvm-verifier`、`jvm-runtime`、`jvm-interpreter`、`jvm-host`、`jvm-native`、`jvm-jni`、`jvm-gui`、`jvm-asm-oracle`。
4. `chore(build): add shared Kotlin conventions` - 抽出统一 Kotlin/JVM toolchain、test、compiler options。
5. `chore(build): add JavaFX GUI module configuration` - 配置 JavaFX 依赖和 application entry，但不实现 UI。
6. `docs: add JVMS coverage ledger` - 新建 `docs/spec-coverage.md`，按 JVMS 2/4/5/6/7 章节列覆盖矩阵。
7. `test: add fixture compiler harness` - 建立 javac fixture 编译与 class bytes 加载测试工具。
8. `test: add javap oracle harness` - 增加 `javap -v` 输出对照工具。
9. `test: add ASM oracle harness` - 增加 ASM test-only 依赖，用于生成/解析 fixture 对照。

### Phase 1 - ClassFile Reader Foundation
10. `feat(classfile): add offset-aware byte reader` - 提供 unsigned byte/u1/u2/u4 读取、当前位置、slice、EOF diagnostic。
11. `feat(classfile): parse classfile magic` - 识别 `0xCAFEBABE`，错误时报告 offset。
12. `feat(classfile): parse minor and major version` - 读出 minor/major 并建模。
13. `feat(classfile): validate Java 26 version range` - 支持 major 45..70 和 preview minor 65535 规则。
14. `feat(classfile): model constant pool indexes` - 建立 1-based CP index、long/double 双槽占位。
15. `feat(classfile): parse CONSTANT_Utf8` - 实现 modified UTF-8 解码与非法字节测试。
16. `feat(classfile): parse integer float long double constants` - 覆盖数值常量。
17. `feat(classfile): parse string and class constants` - 解析 `CONSTANT_String`、`CONSTANT_Class`。
18. `feat(classfile): parse name and type constants` - 解析 `CONSTANT_NameAndType`。
19. `feat(classfile): parse field method interface refs` - 解析三类 member ref。
20. `feat(classfile): parse method handle constants` - 解析 reference_kind/reference_index 并做基础校验。
21. `feat(classfile): parse method type constants` - 解析 descriptor index。
22. `feat(classfile): parse dynamic and invokedynamic constants` - 解析 bootstrap index/name-and-type。
23. `feat(classfile): parse module and package constants` - 解析 Java 9+ module/package 常量。
24. `feat(classfile): validate constant pool cross references` - 校验 CP 引用类型与索引有效性。

### Phase 2 - ClassFile Members and Attributes
25. `feat(classfile): parse class access flags` - 解析 class/interface/module/annotation/enum/record 等 flags。
26. `feat(classfile): parse this super and interfaces` - 建模继承与接口表。
27. `feat(classfile): parse field declarations` - 解析 fields_count、field_info。
28. `feat(classfile): parse method declarations` - 解析 methods_count、method_info。
29. `feat(classfile): add attribute parser registry` - 建立按 name 分发 attribute parser 的机制。
30. `feat(classfile): preserve unknown attributes` - 保留 unknown attribute bytes 以支持 round-trip。
31. `feat(classfile): parse ConstantValue attribute` - 解析字段常量值。
32. `feat(classfile): parse Code attribute header` - max_stack、max_locals、code bytes。
33. `feat(classfile): parse Code exception table` - 解析 try/catch handler 表。
34. `feat(classfile): parse nested Code attributes` - 允许 Code 内嵌 attributes。
35. `feat(classfile): parse StackMapTable attribute` - 解析全部 frame variants。
36. `feat(classfile): parse Exceptions attribute` - 解析 throws 表。
37. `feat(classfile): parse InnerClasses and EnclosingMethod` - 覆盖嵌套类元数据。
38. `feat(classfile): parse Synthetic Deprecated and SourceFile` - 覆盖简单 attributes。
39. `feat(classfile): parse SourceDebugExtension` - 保留调试扩展文本。
40. `feat(classfile): parse LineNumberTable` - 解析行号映射。
41. `feat(classfile): parse LocalVariableTable` - 解析局部变量调试信息。
42. `feat(classfile): parse LocalVariableTypeTable` - 解析泛型局部变量类型。
43. `feat(classfile): parse Signature attribute` - 解析 signature 原始文本并接入 grammar 校验。
44. `feat(classfile): parse RuntimeVisibleAnnotations` - 解析 runtime visible annotation。
45. `feat(classfile): parse RuntimeInvisibleAnnotations` - 解析 runtime invisible annotation。
46. `feat(classfile): parse parameter annotations` - 覆盖 visible/invisible parameter annotations。
47. `feat(classfile): parse AnnotationDefault` - 覆盖 annotation default value。
48. `feat(classfile): parse type annotations` - 解析 target_info/type_path。
49. `feat(classfile): parse BootstrapMethods` - 解析 bootstrap method handle 和参数表。
50. `feat(classfile): parse MethodParameters` - 覆盖方法参数 metadata。
51. `feat(classfile): parse Module attributes` - 覆盖 Module/ModulePackages/ModuleMainClass。
52. `feat(classfile): parse NestHost and NestMembers` - 覆盖 nestmate metadata。
53. `feat(classfile): parse Record attribute` - 覆盖 record components。
54. `feat(classfile): parse PermittedSubclasses` - 覆盖 sealed classes metadata。

### Phase 3 - ClassFile Validation and Writer
55. `feat(classfile): validate binary names` - 校验 binary/internal class names。
56. `feat(classfile): validate unqualified names` - 校验字段/方法未限定名规则。
57. `feat(classfile): validate field descriptors` - 实现字段 descriptor grammar。
58. `feat(classfile): validate method descriptors` - 实现方法 descriptor grammar。
59. `feat(classfile): validate signature grammar` - 实现 class/method/field signature grammar。
60. `feat(classfile): report diagnostics with byte offsets` - 所有 parser errors 带 path 和 byte offset。
61. `feat(classfile): add classfile writer skeleton` - 建立 writer API，不重排未知结构。
62. `feat(classfile): write constant pool` - 输出 CP 并保持双槽规则。
63. `feat(classfile): write fields and methods` - 输出 fields/methods。
64. `feat(classfile): write attributes` - 输出 known/unknown attributes。
65. `test(classfile): add round trip tests` - 对 fixture bytes 做 parse/write/parse。
66. `test(classfile): add javap differential tests` - 与 `javap -v` 对照核心结构。

### Phase 4 - Runtime Core Model
67. `feat(runtime): add primitive value model` - 建模 int/long/float/double/byte/short/char/boolean。
68. `feat(runtime): add reference and null value model` - 建模 ref/null。
69. `feat(runtime): add slot model for category one and two values` - 区分 category 1/2 slots。
70. `feat(runtime): add object identity model` - guest object refs 和 identity hash 基础。
71. `feat(runtime): add array object model` - primitive/ref arrays。
72. `feat(runtime): add class mirror model` - guest `java.lang.Class` mirror。
73. `feat(runtime): add heap allocation` - heap allocator 和 object table。
74. `feat(runtime): add field storage` - instance field layout/storage。
75. `feat(runtime): add static field storage` - static slots 和 preparation 初值。
76. `feat(runtime): add frame local variables` - frame locals API。
77. `feat(runtime): add operand stack` - push/pop/peek/category validation。
78. `feat(runtime): add program counter` - pc state 和 instruction offset。
79. `feat(runtime): add JVM thread stack` - push/pop frames。
80. `feat(runtime): add method area` - loaded class metadata registry。
81. `feat(runtime): add runtime constant pool state` - resolved/unresolved entries。
82. `feat(runtime): add structured VM exception model` - guest exception throwing path。
83. `feat(runtime): add monitor state model` - monitor owner/count/wait-set 基础。
84. `feat(runtime): add execution event bus` - 事件 listener contract。
85. `feat(runtime): add immutable execution snapshot` - GUI/debugger 只读 snapshot。

### Phase 5 - Class Loading, Linking, Initialization
86. `feat(runtime): load classpath class files` - 支持单个 `.class` 加入 classpath。
87. `feat(runtime): load classpath directories` - 支持目录 classpath。
88. `feat(runtime): load classpath jars` - 支持 jar classpath。
89. `feat(runtime): model bootstrap class loader` - bootstrap loader identity。
90. `feat(runtime): model user class loader` - user-defined loader identity。
91. `feat(runtime): derive class from classfile bytes` - 从 classfile 创建 runtime class。
92. `feat(runtime): create array classes` - 按 JVMS 创建 array class。
93. `feat(runtime): enforce loading constraints` - loading constraints 表。
94. `feat(runtime): add module and package metadata` - module/layer/package 基础。
95. `feat(runtime): prepare instance fields` - instance field layout 初始化。
96. `feat(runtime): prepare static fields` - static field 默认值与 ConstantValue。
97. `feat(runtime): resolve class and interface refs` - 类/接口解析。
98. `feat(runtime): resolve field refs` - 字段解析。
99. `feat(runtime): resolve method refs` - 方法解析。
100. `feat(runtime): resolve interface method refs` - 接口方法解析。
101. `feat(runtime): resolve method type refs` - MethodType 解析。
102. `feat(runtime): resolve method handle refs` - MethodHandle 解析。
103. `feat(runtime): resolve dynamic constants` - condy 解析与缓存。
104. `feat(runtime): resolve invokedynamic call sites` - indy CallSite 绑定与缓存。
105. `feat(runtime): enforce access control` - package/protected/private/public 检查。
106. `feat(runtime): implement method overriding` - overriding 规则。
107. `feat(runtime): implement method selection` - invoke selection 规则。
108. `feat(runtime): implement class initialization state machine` - uninitialized/in-progress/initialized/error。
109. `feat(runtime): implement class initialization locking` - 多线程初始化锁。
110. `feat(runtime): implement VM startup` - main method 启动路径。
111. `feat(runtime): implement VM termination` - 正常/异常终止事件。

### Phase 6 - Verifier
112. `feat(verifier): add verification type model` - Top/Integer/Float/Long/Double/Null/Object/Uninitialized。
113. `feat(verifier): add verification type lattice` - assignability/merge。
114. `feat(verifier): parse stack map frames into verifier state` - StackMapTable -> frame states。
115. `feat(verifier): build method control flow graph` - branch/exception edges。
116. `feat(verifier): verify max locals and max stack` - limits 检查。
117. `feat(verifier): verify local variable transfers` - locals state transfer。
118. `feat(verifier): verify operand stack transfers` - stack state transfer。
119. `feat(verifier): verify uninitialized this rules` - `<init>` 规则。
120. `feat(verifier): verify new object initialization` - new/uninitialized object flow。
121. `feat(verifier): verify exception handler edges` - handler frame 状态。
122. `feat(verifier): verify protected member access` - protected rules。
123. `feat(verifier): verify load and store instructions` - load/store verifier。
124. `feat(verifier): verify arithmetic instructions` - arithmetic verifier。
125. `feat(verifier): verify conversion instructions` - conversion verifier。
126. `feat(verifier): verify branch instructions` - branch verifier。
127. `feat(verifier): verify switch instructions` - switch verifier。
128. `feat(verifier): verify return instructions` - returns verifier。
129. `feat(verifier): verify field instructions` - get/put field/static verifier。
130. `feat(verifier): verify invoke instructions` - invoke verifier。
131. `feat(verifier): verify object and array instructions` - object/array verifier。
132. `feat(verifier): verify monitor instructions` - monitor verifier。
133. `feat(verifier): verify athrow` - throw verifier。
134. `feat(verifier): verify wide jsr and ret` - legacy verifier。
135. `feat(verifier): implement legacy type inference verification` - old classfile fallback。
136. `test(verifier): add valid verifier corpus` - 合法 class corpus。
137. `test(verifier): add rejected verifier corpus` - 非法 class corpus。

### Phase 7 - Interpreter Decoder and Basic Instructions
138. `feat(interpreter): add opcode metadata table` - 256 opcode metadata。
139. `feat(interpreter): add bytecode decoder` - decode instruction operands。
140. `feat(interpreter): decode tableswitch alignment` - tableswitch padding。
141. `feat(interpreter): decode lookupswitch alignment` - lookupswitch padding。
142. `feat(interpreter): decode wide instructions` - wide modifier。
143. `feat(interpreter): execute nop and constants` - nop/aconst/iconst/lconst/fconst/dconst/bipush/sipush。
144. `feat(interpreter): execute ldc variants` - ldc/ldc_w/ldc2_w。
145. `feat(interpreter): execute load instructions` - xload/xload_n/aload。
146. `feat(interpreter): execute store instructions` - xstore/xstore_n/astore。
147. `feat(interpreter): execute iinc` - iinc including wide。
148. `feat(interpreter): execute stack manipulation instructions` - pop/dup/swap family。

### Phase 8 - Interpreter Arithmetic, Branching, Objects
149. `feat(interpreter): execute int arithmetic`
150. `feat(interpreter): execute long arithmetic`
151. `feat(interpreter): execute float arithmetic`
152. `feat(interpreter): execute double arithmetic`
153. `feat(interpreter): execute numeric conversions`
154. `feat(interpreter): execute comparisons`
155. `feat(interpreter): execute conditional branches`
156. `feat(interpreter): execute goto and goto_w`
157. `feat(interpreter): execute tableswitch`
158. `feat(interpreter): execute lookupswitch`
159. `feat(interpreter): execute jsr ret legacy flow`
160. `feat(interpreter): execute object allocation`
161. `feat(interpreter): execute primitive array allocation`
162. `feat(interpreter): execute reference array allocation`
163. `feat(interpreter): execute multidimensional array allocation`
164. `feat(interpreter): execute arraylength`
165. `feat(interpreter): execute array load instructions`
166. `feat(interpreter): execute array store instructions`
167. `feat(interpreter): execute checkcast and instanceof`

### Phase 9 - Interpreter Fields, Methods, Exceptions, Monitors
168. `feat(interpreter): execute getstatic and putstatic`
169. `feat(interpreter): execute getfield and putfield`
170. `feat(interpreter): execute invokestatic`
171. `feat(interpreter): execute invokespecial`
172. `feat(interpreter): execute invokevirtual`
173. `feat(interpreter): execute invokeinterface`
174. `feat(interpreter): execute invokedynamic`
175. `feat(interpreter): execute return instructions`
176. `feat(interpreter): execute athrow`
177. `feat(interpreter): unwind exceptions through handlers`
178. `feat(interpreter): execute monitorenter`
179. `feat(interpreter): execute monitorexit`
180. `feat(interpreter): reject reserved opcodes`
181. `feat(interpreter): emit before and after instruction events`
182. `test(interpreter): add HotSpot differential execution tests`

### Phase 10 - Host Delegation
183. `feat(runtime): add class execution policy`
184. `feat(host): add host delegated class mirror`
185. `feat(host): add isolated host class loader`
186. `feat(host): resolve host methods`
187. `feat(host): resolve host fields`
188. `feat(host): bridge primitive values`
189. `feat(host): bridge string values`
190. `feat(host): bridge array values`
191. `feat(host): bridge class mirrors`
192. `feat(host): bridge throwable values`
193. `feat(host): maintain guest host identity map`
194. `feat(host): invoke host static methods`
195. `feat(host): invoke host instance methods`
196. `feat(host): access host fields`
197. `feat(host): translate host exceptions`
198. `feat(host): emit opaque host boundary events`
199. `test(host): add JDK delegation tests`
200. `test(host): add whitelist delegation tests`

### Phase 11 - Layered Native Resolver
201. `feat(native): add native method resolver contract`
202. `feat(native): add native resolution policy`
203. `feat(native): add intrinsic whitelist policy`
204. `feat(native): add intrinsic registry`
205. `feat(native): add native frame model`
206. `feat(native): emit native call events`
207. `feat(native): resolve whitelisted intrinsic first`
208. `feat(native): fall back to simulated JNI on intrinsic miss`
209. `feat(native): throw guest UnsatisfiedLinkError when unresolved`
210. `test(native): intrinsic hit uses Kotlin implementation`
211. `test(native): intrinsic miss enters simulated JNI`
212. `test(native): non whitelisted method skips intrinsic lookup`

### Phase 12 - Simulated JNI Core
213. `feat(jni): add simulated JNIEnv function table`
214. `feat(jni): add JNI handle table`
215. `feat(jni): model jobject handles`
216. `feat(jni): model jclass handles`
217. `feat(jni): model jmethodID handles`
218. `feat(jni): model jfieldID handles`
219. `feat(jni): implement local reference frames`
220. `feat(jni): implement global references`
221. `feat(jni): implement weak global references`
222. `feat(jni): implement pending exception state`
223. `feat(jni): implement Throw and ThrowNew`
224. `feat(jni): implement ExceptionOccurred and ExceptionCheck`
225. `feat(jni): implement ExceptionClear`
226. `feat(jni): implement FatalError policy`

### Phase 13 - Simulated JNI Lookup and Upcalls
227. `feat(jni): implement FindClass`
228. `feat(jni): implement GetObjectClass`
229. `feat(jni): implement IsInstanceOf`
230. `feat(jni): implement GetMethodID`
231. `feat(jni): implement GetStaticMethodID`
232. `feat(jni): implement GetFieldID`
233. `feat(jni): implement GetStaticFieldID`
234. `feat(jni): implement CallVoidMethod`
235. `feat(jni): implement CallObjectMethod`
236. `feat(jni): implement Call primitive instance methods`
237. `feat(jni): implement CallNonvirtual methods`
238. `feat(jni): implement CallStatic methods`
239. `feat(jni): implement NewObject`
240. `feat(jni): route JNI upcalls through interpreter`
241. `test(jni): native upcall reenters interpreted method`

### Phase 14 - Simulated JNI Data Helpers
242. `feat(jni): implement GetObjectField and SetObjectField`
243. `feat(jni): implement primitive field access`
244. `feat(jni): implement static field access`
245. `feat(jni): implement NewString`
246. `feat(jni): implement NewStringUTF`
247. `feat(jni): implement GetStringChars`
248. `feat(jni): implement GetStringUTFChars`
249. `feat(jni): implement ReleaseString helpers`
250. `feat(jni): implement NewObjectArray`
251. `feat(jni): implement object array region helpers`
252. `feat(jni): implement primitive array creation`
253. `feat(jni): implement primitive array region helpers`
254. `feat(jni): implement array elements pin copy policy`
255. `feat(jni): implement MonitorEnter and MonitorExit`
256. `test(jni): verify strings arrays fields refs and monitors`

### Phase 15 - VM Intrinsics
257. `feat(native): implement Object.getClass intrinsic`
258. `feat(native): implement Object.hashCode intrinsic`
259. `feat(native): implement Object.clone intrinsic`
260. `feat(native): implement Object.wait notify notifyAll intrinsics`
261. `feat(native): implement System.arraycopy intrinsic`
262. `feat(native): implement System.identityHashCode intrinsic`
263. `feat(native): implement System time intrinsics`
264. `feat(native): implement Class mirror query intrinsics`
265. `feat(native): implement Throwable.fillInStackTrace intrinsic`
266. `feat(native): implement String.intern intrinsic`
267. `feat(native): implement Thread.currentThread intrinsic`
268. `feat(native): implement Thread.sleep intrinsic`
269. `test(native): add VM intrinsic behavior tests`

### Phase 16 - Native Library Loading
270. `feat(jni): add native library descriptor model`
271. `feat(jni): add native symbol name resolver`
272. `feat(jni): add Panama downcall backend skeleton`
273. `feat(jni): bind JNI_OnLoad`
274. `feat(jni): bind Java_ native exports`
275. `feat(jni): pass simulated JNIEnv to native exports`
276. `feat(jni): marshal primitive JNI arguments`
277. `feat(jni): marshal object JNI handles`
278. `feat(jni): marshal JNI return values`
279. `feat(jni): propagate native thrown guest exceptions`
280. `test(jni): run tiny native library with upcall fixture`

### Phase 17 - GUI Foundation
281. `feat(gui): add JavaFX application shell`
282. `feat(gui): add project and classpath panel`
283. `feat(gui): add jar and class import action`
284. `feat(gui): add class tree view`
285. `feat(gui): add member list view`
286. `feat(gui): add bytecode instruction view`
287. `feat(gui): add constant pool view`
288. `feat(gui): add run configuration model`
289. `feat(gui): add debugger control bar`
290. `feat(gui): wire step action to engine`

### Phase 18 - GUI Runtime Visualization
291. `feat(gui): show current frame`
292. `feat(gui): show local variables`
293. `feat(gui): show operand stack`
294. `feat(gui): highlight current instruction`
295. `feat(gui): show class loading events`
296. `feat(gui): show linking events`
297. `feat(gui): show initialization events`
298. `feat(gui): show verifier diagnostics`
299. `feat(gui): show exception unwinding`
300. `feat(gui): show monitor events`
301. `feat(gui): show invokedynamic and condy events`
302. `feat(gui): show host delegation boundary`
303. `feat(gui): show native intrinsic frames`
304. `feat(gui): show simulated JNI calls`
305. `feat(gui): show JNI upcall nesting`
306. `test(gui): add JavaFX smoke test harness`

### Phase 19 - Spec Coverage Gates
307. `test(spec): enforce opcode table coverage`
308. `test(spec): enforce opcode execution coverage`
309. `test(spec): enforce attribute parser coverage`
310. `test(spec): enforce verifier rule coverage`
311. `test(spec): enforce class loading coverage`
312. `test(spec): enforce linking resolution coverage`
313. `test(spec): enforce initialization coverage`
314. `test(spec): enforce native resolver coverage`
315. `test(spec): enforce simulated JNI coverage`
316. `test(spec): add JVMS chapter 3 example corpus`
317. `test(spec): add malformed classfile corpus`
318. `test(spec): add HotSpot runtime differential corpus`

### Phase 20 - Final Documentation and Audit
319. `docs: document module architecture`
320. `docs: document public engine API`
321. `docs: document event stream contract`
322. `docs: document classfile implementation coverage`
323. `docs: document verifier design`
324. `docs: document interpreter design`
325. `docs: document host delegation policy`
326. `docs: document layered native resolver`
327. `docs: document simulated JNI architecture`
328. `docs: document GUI workflow`
329. `docs: complete JVMS coverage ledger`
330. `chore: audit for unsupported normative paths`
331. `test: run final full build and smoke suite`

## Test Cases and Scenarios
- ClassFile: valid Java 26 class parses and round-trips; invalid magic/version/constant pool/attribute offset reports precise diagnostics; `javap -v` and parser model agree on golden fixtures.
- Verifier: valid StackMapTable methods pass; invalid stack/local transitions fail; legacy `jsr/ret` cases are covered.
- Interpreter: every opcode has decode behavior; every non-reserved opcode has execution or precise spec error behavior; HotSpot differential fixtures match return value or thrown exception.
- Host: JDK classes can be delegated; user classes default to interpreted; whitelisted classes can be host delegated; host calls emit opaque boundary events.
- Native/JNI: whitelisted native method with intrinsic uses Kotlin intrinsic; whitelisted native method without intrinsic enters simulated JNI; JNI `Call<Type>Method` re-enters guest interpreter; JNI refs, exceptions, strings, arrays, fields, monitors operate on guest state; missing native binding throws guest `UnsatisfiedLinkError`.
- GUI: load jar/class, browse classes, select method; step bytecode and observe locals/operand stack/pc; observe class loading/linking/init events; observe native intrinsic and simulated JNI upcall nesting; observe host-delegated opaque calls.

## Assumptions
- Work is performed directly on local `main`, as requested.
- `docs/plan.md` is the authoritative plan file.
- “完整 JVMS 26” means full JVM spec semantics; Java SE class library is supported via host delegation, VM intrinsics, and simulated JNI where needed.
- ASM is never exposed in runtime/verifier/interpreter/native public APIs.
- Native execution for interpreted classes is layered: intrinsic first only when policy allows, simulated JNI fallback, then guest `UnsatisfiedLinkError`.
