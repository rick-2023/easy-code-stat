package com.your.plugin.statistics;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class ShowCodeStatisticsAction extends AnAction {
    
    public ShowCodeStatisticsAction() {
        super("Show Code Statistics", 
              "显示代码统计", 
              AllIcons.Actions.Profile);  // 使用内置的统计图标
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow("Code Statistics");
            if (toolWindow != null) {
                toolWindow.show();
            }
        }
    }
} 