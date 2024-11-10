package com.easy.code.stat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.your.plugin.statistics.CodeStatisticsPanel;
import org.jetbrains.annotations.NotNull;

public class CodeStatisticsToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CodeStatisticsPanel statisticsPanel = new CodeStatisticsPanel(project);
        Content content = ContentFactory.getInstance()
                .createContent(statisticsPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
} 