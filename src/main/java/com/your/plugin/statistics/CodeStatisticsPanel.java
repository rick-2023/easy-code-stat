package com.your.plugin.statistics;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Map;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

public class CodeStatisticsPanel extends JPanel {
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final Project project;
    private final JBTable summaryTable;
    private final JBTable classTable;
    private final DefaultTableModel summaryTableModel;
    private final DefaultTableModel classTableModel;
    private final JPanel progressPanel;
    private final JButton startButton;
    private final JButton stopButton;
    private boolean isScanning = false;

    public CodeStatisticsPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));

        // 创建顶部面板
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // 控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        controlPanel.setBorder(JBUI.Borders.empty(0));
        
        startButton = new JButton("开始统计");
        startButton.setPreferredSize(new Dimension(120, 30));
        startButton.addActionListener(e -> startCodeStatistics());
        
        stopButton = new JButton("停止统计");
        stopButton.setPreferredSize(new Dimension(120, 30));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopCodeStatistics());
        
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        // 进度面板
        progressPanel = new JPanel(new BorderLayout(0, 2));
        progressPanel.setBorder(JBUI.Borders.empty(5, 0));
        progressPanel.setVisible(false);
        
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 2));
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(UIManager.getColor("Panel.background"));
        progressBar.setForeground(UIManager.getColor("ProgressBar.foreground"));
        
        statusLabel = new JLabel("Ready to start code statistics...");
        statusLabel.setFont(JBUI.Fonts.smallFont());
        statusLabel.setBorder(JBUI.Borders.empty(2, 0));
        
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);

        topPanel.add(controlPanel, BorderLayout.NORTH);
        topPanel.add(progressPanel, BorderLayout.CENTER);

        // 创建标签页面板
        JTabbedPane tabbedPane = new JTabbedPane();

        // 汇总统计表格
        String[] summaryColumnNames = {
            "文件类型", "文件数量", "总大小", "最小大小", "最大大小", "平均大小",
            "总行数", "最少行数", "最多行数", "平均行数"
        };
        summaryTableModel = createTableModel(summaryColumnNames);
        summaryTable = createTable(summaryTableModel);
        JBScrollPane summaryScrollPane = new JBScrollPane(summaryTable);
        summaryScrollPane.setBorder(JBUI.Borders.empty());
        
        // 类统计表格
        String[] classColumnNames = {
            "类名", "方法数", "未使用方法数", "总行数", "平均方法行数", 
            "最大方法行数", "字段数", "代码行数", "注释行数"
        };
        classTableModel = createTableModel(classColumnNames);
        classTable = createTable(classTableModel);
        JBScrollPane classScrollPane = new JBScrollPane(classTable);
        classScrollPane.setBorder(JBUI.Borders.empty());

        // 添加标签页
        tabbedPane.addTab("文件统计", summaryScrollPane);
        tabbedPane.addTab("类统计", classScrollPane);

        // 主布局
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private DefaultTableModel createTableModel(String[] columnNames) {
        return new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JBTable createTable(DefaultTableModel model) {
        JBTable table = new JBTable(model);
        table.setShowGrid(false);
        table.setStriped(true);
        table.getTableHeader().setReorderingAllowed(false);
        
        // 创建自定义的排序器
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model) {
            @Override
            public Comparator<?> getComparator(int column) {
                if (model == classTableModel) {
                    // 类统计表格的排序逻辑
                    switch (column) {
                        case 0: // 类名
                            return String.CASE_INSENSITIVE_ORDER;
                        case 1: // 方法数
                        case 2: // 总行数
                        case 3: // 平均方法行数
                        case 4: // 最大方法行数
                        case 5: // 字段数
                        case 6: // 代码行数
                        case 7: // 注释行数
                            return (Comparator<String>) (s1, s2) -> {
                                if (s1.equals("-") || s2.equals("-")) return 0;
                                try {
                                    return Integer.compare(
                                        Integer.parseInt(s1.toString()), 
                                        Integer.parseInt(s2.toString())
                                    );
                                } catch (NumberFormatException e) {
                                    return 0;
                                }
                            };
                    }
                } else {
                    // 文件统计表格的排序逻辑
                    switch (column) {
                        case 0: // 文件类型
                            return String.CASE_INSENSITIVE_ORDER;
                        case 1: // 文件数量
                        case 6: // 总行数
                        case 7: // 最少行数
                        case 8: // 最多行数
                        case 9: // 平均行数
                            return (Comparator<String>) (s1, s2) -> {
                                if (s1.equals("-") || s2.equals("-")) return 0;
                                try {
                                    return Integer.compare(
                                        Integer.parseInt(s1.toString()), 
                                        Integer.parseInt(s2.toString())
                                    );
                                } catch (NumberFormatException e) {
                                    return 0;
                                }
                            };
                        case 2: // 总大小
                        case 3: // 最小大小
                        case 4: // 最大大小
                        case 5: // 平均大小
                            return (Comparator<String>) (s1, s2) -> {
                                if (s1.equals("-") || s2.equals("-")) return 0;
                                return compareSizeStrings(s1, s2);
                            };
                    }
                }
                return super.getComparator(column);
            }
        };
        
        table.setRowSorter(sorter);
        
        // 如果是类统计表格，添加双击事件
        if (model == classTableModel) {
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {  // 双击事件
                        int row = table.convertRowIndexToModel(table.rowAtPoint(e.getPoint()));
                        int col = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
                        
                        if (row >= 0 && col == 0) {  // 确保点击的是类名列
                            String className = (String) model.getValueAt(row, col);
                            navigateToClass(className);
                        }
                    }
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    int col = table.columnAtPoint(e.getPoint());
                    if (col == 0) {  // 类名列
                        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            });
        }
        
        return table;
    }

    private void navigateToClass(String className) {
        if (className == null || className.isEmpty()) return;

        // 在项目范围内查找类
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.projectScope(project));
        
        if (psiClass != null && psiClass.getContainingFile() != null && 
            psiClass.getContainingFile().getVirtualFile() != null) {
            
            // 获取类的虚拟文件
            var virtualFile = psiClass.getContainingFile().getVirtualFile();
            
            // 计算类在文件中的偏移量
            int offset = psiClass.getTextOffset();
            
            // 创建并执行导航
            new OpenFileDescriptor(project, virtualFile, offset).navigate(true);
            
            // 可选：将编辑器请求焦点
            FileEditorManager.getInstance(project).getSelectedTextEditor();
        }
    }

    private void startCodeStatistics() {
        summaryTableModel.setRowCount(0);
        classTableModel.setRowCount(0);
        progressPanel.setVisible(true);
        isScanning = true;
        
        // 更新按钮状态
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        
        CodeStatisticsService.getInstance(project).startStatistics(
            (progress, status, statistics) -> {
                progressBar.setValue((int)(progress * 100));
                String progressText = String.format("%s (%.1f%%)", 
                    status, 
                    progress * 100
                );
                statusLabel.setText(progressText);
                
                if (progress == 1.0) {
                    if (statistics != null) {
                        updateTables(statistics.fileStats, statistics.classStats);
                    }
                    Timer timer = new Timer(1000, e -> {
                        progressPanel.setVisible(false);
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        isScanning = false;
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            }
        );
    }

    private void stopCodeStatistics() {
        if (isScanning) {
            CodeStatisticsService.getInstance(project).stopStatistics();
            stopButton.setEnabled(false);
        }
    }

    private void updateTables(Map<String, FileStatistics> fileStats, 
                            Map<String, ClassStatistics> classStats) {
        // 更新汇总表格
        summaryTableModel.setRowCount(0);
        fileStats.forEach((ext, stat) -> {
            summaryTableModel.addRow(new Object[]{
                ext,
                String.valueOf(stat.getCount()),  // 转换为字符串
                formatSize(stat.getSize()),
                formatSize(stat.getSizeMin()),
                formatSize(stat.getSizeMax()),
                formatSize(stat.getSizeAvg()),
                String.valueOf(stat.getLines()),  // 转换为字符串
                String.valueOf(stat.getLinesMin()),  // 转换为字符串
                String.valueOf(stat.getLinesMax()),  // 转换为字符串
                String.valueOf(stat.getLinesAvg())   // 转换为字符串
            });
        });
        
        // 添加总计行
        long totalSize = fileStats.values().stream().mapToLong(FileStatistics::getSize).sum();
        int totalCount = fileStats.values().stream().mapToInt(FileStatistics::getCount).sum();
        int totalLines = fileStats.values().stream().mapToInt(FileStatistics::getLines).sum();
        
        summaryTableModel.addRow(new Object[]{
            "总计",
            String.valueOf(totalCount),  // 转换为字符串
            formatSize(totalSize),
            "-",
            "-",
            "-",
            String.valueOf(totalLines),  // 转换为字符串
            "-",
            "-",
            "-"
        });

        // 更新类统计表格
        classTableModel.setRowCount(0);
        classStats.forEach((className, stat) -> {
            Map<String, Integer> methodLines = stat.getMethodLines();
            int totalMethodLines = stat.getTotalMethodLines();
            int methodCount = stat.getMethodCount();
            int unusedMethodCount = stat.getUnusedMethodCount();
            int avgMethodLines = methodCount > 0 ? totalMethodLines / methodCount : 0;
            int maxMethodLines = methodLines.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
            
            classTableModel.addRow(new Object[]{
                className,
                String.valueOf(methodCount),
                String.valueOf(unusedMethodCount),  // 添加未使用方法数
                String.valueOf(totalMethodLines),
                String.valueOf(avgMethodLines),
                String.valueOf(maxMethodLines),
                String.valueOf(stat.getFieldCount()),
                String.valueOf(totalMethodLines),
                "0"
            });
        });
    }

    private String formatSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.1fKB", size / 1024.0);
        return String.format("%.1fMB", size / (1024.0 * 1024.0));
    }

    private int compareSizeStrings(String s1, String s2) {
        // 将大小字符串转换为字节数进行比较
        long bytes1 = parseSize(s1);
        long bytes2 = parseSize(s2);
        return Long.compare(bytes1, bytes2);
    }

    private long parseSize(String sizeStr) {
        try {
            if (sizeStr.endsWith("B")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 1));
            } else if (sizeStr.endsWith("KB")) {
                double kb = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 2));
                return (long) (kb * 1024);
            } else if (sizeStr.endsWith("MB")) {
                double mb = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 2));
                return (long) (mb * 1024 * 1024);
            }
        } catch (NumberFormatException e) {
            // 解析失败时返回0
        }
        return 0;
    }
} 