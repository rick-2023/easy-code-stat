package com.easy.code.stat.listeners;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class PluginUpdateListener implements DynamicPluginListener {
    
    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (isPluginId(pluginDescriptor)) {
            // 在插件卸载前清理资源
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow("Code Statistics");
                if (toolWindow != null) {
                    toolWindow.remove();
                }
            }
        }
    }

    @Override
    public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        if (isPluginId(pluginDescriptor)) {
            // 在插件加载后重新初始化
            ApplicationManager.getApplication().invokeLater(() -> {
                for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                    ToolWindowManager.getInstance(project)
                            .getToolWindow("Code Statistics")
                            .show();
                }
            });
        }
    }

    private boolean isPluginId(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        return "com.easy.code.stat".equals(pluginDescriptor.getPluginId().getIdString());
    }
} 