package org.wxd.boot.net.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.wxd.boot.agent.LocalShell;
import org.wxd.boot.agent.io.FileReadUtil;
import org.wxd.boot.agent.io.FileUtil;
import org.wxd.boot.agent.io.TemplatePack;
import org.wxd.boot.append.StreamWriter;
import org.wxd.boot.collection.ObjMap;
import org.wxd.boot.net.SocketSession;
import org.wxd.boot.net.controller.ann.ProtoController;
import org.wxd.boot.net.controller.ann.ProtoMapping;
import org.wxd.boot.str.StringUtil;
import org.wxd.boot.timer.MyClock;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: Troy.Chen(無心道, 15388152619)
 * @version: 2021-05-08 15:01
 **/
@Getter
@Setter
@Accessors(chain = true)
public class ProtoBufCreateController {

    /** proto 源文件目录 */
    private String protoSourcePath;
    /** proto 导出编译文件目录 */
    private String protoOutPath;
    /** 生成handler目录 */
    private String codeOutPath;

    public void buildProtobufToJava(String protoc_path) {
        buildProtobuf("java", protoc_path, "java_out");
    }

    /**
     * @param codeLan
     * @param protoc_path
     * @param code_out_name proto导出明明
     */
    public void buildProtobuf(String codeLan,
                              String protoc_path,
                              String code_out_name) {
        try {
            final File java_out_file = new File(protoOutPath);
            if (!java_out_file.exists()) {
                java_out_file.mkdirs();
            }

            File file = FileUtil.findFile(protoc_path);
            if (file == null) {
                new RuntimeException(protoc_path + " 没有找到; ").printStackTrace(System.err);
                return;
            }

            Collection<File> collect = FileUtil.walkFiles(protoSourcePath, 1, ".proto").toList();
            if (collect.isEmpty()) return;
            final LocalShell localShell = LocalShell.build(file.getAbsoluteFile().getParentFile());

            for (File r : collect) {
                final File absoluteFile = r.getAbsoluteFile();
                final String parent = absoluteFile.getParent();
                String cmd = file.getName() + " --proto_path=" + parent + " --" + code_out_name + "=" + protoOutPath + " " + r.getName();
                localShell.putCmd(cmd);
            }
            localShell.exit();
            final LinkedList<String> errorLines = localShell.getErrorLines();
            if (errorLines != null && !errorLines.isEmpty()) {
                System.out.println("执行异常：");
                String joinError = String.join("\n", errorLines);
                System.out.println(joinError);
            }
            System.out.println("转化 " + codeLan + " 处理完成 !!! 请注意异常");
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
        }
    }

    public TreeMap<String, Integer> createMessageId(String startsWithRegex) {
        final String _startsWithRegex = "^(" + startsWithRegex + ").*";
        Collection<File> lists = FileUtil.walkFiles(protoSourcePath, 1, ".proto").toList();
        TreeMap<String, Integer> msgIdMap = new TreeMap<>();
        for (File protoFile : lists) {

            AtomicReference<String> messagePackage = new AtomicReference<>();
            AtomicReference<String> upLine = new AtomicReference<>();

            FileReadUtil.readLine(protoFile, StandardCharsets.UTF_8, line -> {
                if (line.startsWith("option java_package = \"")) {
                    int indexOf = line.indexOf("\"");
                    int of = line.lastIndexOf('"');
                    String mp = line.substring(indexOf + 1, of);
                    messagePackage.set(mp);
                } else if (line.startsWith("message ")) {
                    String messageName = line.replace("message ", "");
                    boolean matches = messageName.matches(_startsWithRegex);
                    if (matches) {
                        int indexOf = messageName.lastIndexOf('{');
                        messageName = StringUtil.filterLine(messageName.substring(0, indexOf));
                        int msgId = StringUtil.hashcode(messageName);
                        String key = messagePackage.get() + "." + messageName;
                        if (msgIdMap.containsKey(key) || msgIdMap.containsValue(msgId)) {
                            throw new RuntimeException("存在相同的消息名称：" + key + ", id=" + msgId);
                        }
                        msgIdMap.put(key, msgId);
                    }
                }
                upLine.set(line);
            });
        }
        return msgIdMap;
    }

    public void createController(String startsWithRegex,
                                 String serviceClass,
                                 String obj_player_package,
                                 String obj_player_name) {
        TemplatePack templatePack = TemplatePack.build(this.getClass().getClassLoader(), "template/message");
        createController(templatePack, startsWithRegex, serviceClass, obj_player_package, obj_player_name);
    }

    public void createController(TemplatePack templatePack,
                                 String startsWithRegex,
                                 String serviceClass,
                                 String obj_player_package,
                                 String obj_player_name) {
        final String _startsWithRegex = "^(" + startsWithRegex + ").*";
        Collection<File> lists = FileUtil.walkFiles(protoSourcePath, 1, ".proto").toList();
        for (File protoFile : lists) {

            String name = FileUtil.fileName(protoFile);
            System.out.println(name);
            AtomicReference<String> messagePackage = new AtomicReference<>();
            AtomicReference<String> upLine = new AtomicReference<>();

            FileReadUtil.readLine(protoFile, StandardCharsets.UTF_8, line -> {
                if (line.startsWith("option java_package = \"")) {
                    int indexOf = line.indexOf("\"");
                    int of = line.lastIndexOf('"');
                    String mp = line.substring(indexOf + 1, of);
                    messagePackage.set(mp);
                    System.out.println(messagePackage);
                } else if (line.startsWith("message ")) {
                    String message = line.replace("message ", "");
                    boolean matches = message.matches(_startsWithRegex);
                    if (matches) {
                        int indexOf = message.lastIndexOf('{');
                        message = StringUtil.filterLine(message.substring(0, indexOf));

                        String savePack = messagePackage.get().replace(".message", ".controller");
                        String saveClassName = message + "Controller";

                        String saveFileName = codeOutPath + "/"
                                + savePack.replace(".", "/")
                                + "/" + saveClassName + ".java";

                        if (FileUtil.exists(saveFileName)) {
                            return;
                        }

                        ObjMap objMap = new ObjMap();

                        Set<String> imports = new TreeSet<>();
                        imports.add(messagePackage.get() + "." + message);

                        imports.add(ObjMap.class.getName());
                        imports.add(SocketSession.class.getName());
                        imports.add(MessageController.class.getName());
                        imports.add(ProtoController.class.getName());
                        imports.add(ProtoMapping.class.getName());
                        imports.add(IController.class.getName());
                        imports.add(Slf4j.class.getName());

                        objMap.put("imports", imports);
                        objMap.put("filePath", protoFile.getName());
                        objMap.put("comment", upLine.get().replace("//", ""));
                        objMap.put("times", MyClock.nowString());
                        objMap.put("messageName", message);
                        objMap.put("service", "service");
                        objMap.put("savePack", savePack);
                        objMap.put("saveClassName", saveClassName);

                        if (StringUtil.emptyOrNull(serviceClass)) {
                            objMap.put("serviceClass", "");
                        } else {
                            objMap.put("serviceClass", "service = \"" + serviceClass + "\"");
                        }

                        if (message.startsWith("Req")) {
                            String resMessage = message.replace("Req", "Res");
                            imports.add(messagePackage.get() + "." + resMessage);
                            objMap.put("res", resMessage + ".Builder res4Builder = " + resMessage + ".newBuilder();");
                        } else {
                            objMap.put("res", "");
                        }

                        if (StringUtil.notEmptyOrNull(obj_player_package)) {
                            imports.add(obj_player_package);
                        }

                        if (StringUtil.notEmptyOrNull(obj_player_name)) {
                            String lowerFirst = StringUtil.lowerFirst(obj_player_name);
                            objMap.put("userInfo", obj_player_name + " " + lowerFirst + " = (" + obj_player_name + ") param.get(" + MessageController.class.getSimpleName() + ".OBJ_Player);");
                        }

                        templatePack.ftl2File("proto-controller.ftl", objMap, saveFileName);


                        try (StreamWriter out = new StreamWriter()) {
                            out.writeLn("==============================================================================================================");
                            out.writeLn("消息文件：" + protoFile.getName());
                            out.writeLn("消息名称：" + messagePackage.get() + "." + message);
                            out.writeLn("消息ID：" + StringUtil.hashcode(objMap.getString("messageName")));
                            out.writeLn("文件包名：" + objMap.getString("savePack"));
                            out.writeLn("处理文件：" + objMap.getString("saveClassName") + ".java");
                            out.writeLn("==============================================================================================================");
                            System.out.println(out.toString());
                            System.out.flush();
                        }
                    }
                }
                upLine.set(line);
            });

        }
    }

}
