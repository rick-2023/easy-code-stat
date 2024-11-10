package com.your.plugin.statistics;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public final class CodeStatisticsService {
    private final Project project;
    private final Map<String, FileStatistics> fileStatistics = new HashMap<>();
    private final Map<String, ClassStatistics> classStatistics = new HashMap<>();
    private volatile boolean isStopped = false;

    public CodeStatisticsService(Project project) {
        this.project = project;
    }

    public static CodeStatisticsService getInstance(Project project) {
        return project.getService(CodeStatisticsService.class);
    }

    public interface ProgressCallback {
        void onProgress(double progress, String status, StatisticsResult statistics);
    }

    public static class StatisticsResult {
        public final Map<String, FileStatistics> fileStats;
        public final Map<String, ClassStatistics> classStats;

        public StatisticsResult(Map<String, FileStatistics> fileStats, 
                              Map<String, ClassStatistics> classStats) {
            this.fileStats = fileStats;
            this.classStats = classStats;
        }
    }

    public void stopStatistics() {
        isStopped = true;
    }

    public void startStatistics(ProgressCallback callback) {
        isStopped = false;
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                fileStatistics.clear();
                classStatistics.clear();
                AtomicInteger totalFiles = new AtomicInteger(0);
                AtomicInteger processedFiles = new AtomicInteger(0);

                // 在读操作中统计总文件数
                ReadAction.run(() -> {
                    ProjectFileIndex.getInstance(project).iterateContent(file -> {
                        if (isStopped) return false;
                        if (file.isValid() && !file.isDirectory()) {
                            totalFiles.incrementAndGet();
                        }
                        return true;
                    });
                });

                if (!isStopped) {
                    // 在读操作中处理每个文件
                    ReadAction.run(() -> {
                        ProjectFileIndex.getInstance(project).iterateContent(file -> {
                            if (isStopped) return false;
                            if (file.isValid() && !file.isDirectory()) {
                                processFile(file);
                                int processed = processedFiles.incrementAndGet();
                                double progress = (double) processed / totalFiles.get();
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    callback.onProgress(progress,
                                        String.format("Processing: %d/%d files", processed, totalFiles.get()),
                                        null);
                                });
                            }
                            return true;
                        });
                    });
                }

                // 在 EDT 中更新最终结果
                StatisticsResult result = new StatisticsResult(
                    new HashMap<>(fileStatistics),
                    new HashMap<>(classStatistics)
                );
                ApplicationManager.getApplication().invokeLater(() -> 
                    callback.onProgress(1.0, 
                        isStopped ? "Statistics stopped!" : "Statistics completed!", 
                        result)
                );
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() ->
                    callback.onProgress(0, "Error: " + e.getMessage(), null)
                );
            }
        });
    }

    private void processFile(VirtualFile file) {
        try {
            String extension = file.getExtension();
            if (extension == null) extension = "no_extension";
            extension = extension.toLowerCase();
            
            // 处理文件级别统计
            FileStatistics fileStats = fileStatistics.computeIfAbsent(
                extension, 
                FileStatistics::new
            );

            // 读取文件内容和统计信息
            List<String> lines = new ArrayList<>();
            int[] stats = processFileContent(file, lines);
            int totalLines = stats[0];
            int codeLines = stats[1];
            int commentLines = stats[2];
            int blankLines = stats[3];

            fileStats.addFile(file.getLength(), totalLines, codeLines, commentLines, blankLines);

            // 如果是Java文件，在读操作中处理类级别统计
            if ("java".equals(extension)) {
                ReadAction.run(() -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile instanceof PsiJavaFile) {
                        processJavaFile((PsiJavaFile) psiFile, lines);
                    }
                });
            }
        } catch (Exception e) {
            // 忽略文件处理错误，继续处理下一个文件
        }
    }

    private int[] processFileContent(VirtualFile file, List<String> lines) {
        int totalLines = 0;
        int codeLines = 0;
        int commentLines = 0;
        int blankLines = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean inMultiLineComment = false;
            
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                totalLines++;
                String trimmedLine = line.trim();
                
                if (trimmedLine.isEmpty()) {
                    blankLines++;
                    continue;
                }
                
                if (trimmedLine.startsWith("/*")) {
                    inMultiLineComment = true;
                    commentLines++;
                    continue;
                }
                
                if (inMultiLineComment) {
                    commentLines++;
                    if (trimmedLine.endsWith("*/")) {
                        inMultiLineComment = false;
                    }
                    continue;
                }
                
                if (trimmedLine.startsWith("//")) {
                    commentLines++;
                } else {
                    codeLines++;
                }
            }
        } catch (Exception e) {
            // 处理异常
        }
        
        return new int[]{totalLines, codeLines, commentLines, blankLines};
    }

    private void processJavaFile(PsiJavaFile javaFile, List<String> lines) {
        PsiClass[] classes = javaFile.getClasses();
        for (PsiClass psiClass : classes) {
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName == null) continue;

            ClassStatistics classStats = new ClassStatistics(qualifiedName);
            
            // 统计方法信息
            PsiMethod[] methods = psiClass.getMethods();
            for (PsiMethod method : methods) {
                String methodName = method.getName();
                int methodStartLine = getLineNumber(lines, method.getTextRange().getStartOffset());
                int methodEndLine = getLineNumber(lines, method.getTextRange().getEndOffset());
                int methodLines = methodEndLine - methodStartLine + 1;
                
                // 检查方法是否未使用
                boolean isUnused = isMethodUnused(method);
                classStats.addMethod(methodName, methodLines, isUnused);
            }

            // 统计字段信息
            PsiField[] fields = psiClass.getFields();
            classStats.setFieldCount(fields.length);

            classStatistics.put(qualifiedName, classStats);
        }
    }

    private boolean isMethodUnused(PsiMethod method) {
        // 如果是main方法、构造方法或者有Override注解，则认为是使用的
        if (method.isConstructor() || 
            "main".equals(method.getName()) ||
            method.hasAnnotation("java.lang.Override")) {
            return false;
        }

        // 检查方法的访问修饰符
        if (method.hasModifierProperty(PsiModifier.PRIVATE)) {
            // 对于私有方法，在类内搜索使用
            PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                PsiReference[] references = ReadAction.compute(() ->
                    containingClass.getReferences());
                return references.length == 0;
            }
        } else {
            // 对于非私有方法，在整个项目中搜索使用
            PsiReference[] references = ReadAction.compute(() ->
                method.getReferences());
            return references.length == 0;
        }
        
        return false;
    }

    private int getLineNumber(List<String> lines, int offset) {
        int currentOffset = 0;
        for (int i = 0; i < lines.size(); i++) {
            currentOffset += lines.get(i).length() + 1; // +1 for newline
            if (currentOffset >= offset) {
                return i + 1;
            }
        }
        return lines.size();
    }
} 