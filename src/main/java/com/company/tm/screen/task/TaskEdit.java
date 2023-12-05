package com.company.tm.screen.task;

import com.company.tm.entity.Project;
import com.company.tm.entity.Subtask;
import com.company.tm.entity.Task;
import com.company.tm.entity.User;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import io.jmix.ui.component.BrowserFrame;
import io.jmix.ui.component.FileStorageResource;
import io.jmix.ui.component.FileStorageUploadField;
import io.jmix.ui.component.SingleFileUploadField;
import io.jmix.ui.model.CollectionPropertyContainer;
import io.jmix.ui.model.DataContext;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import io.jmix.ui.upload.TemporaryStorage;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@UiController("tm_Task.edit")
@UiDescriptor("task-edit.xml")
@EditedEntityContainer("taskDc")
public class TaskEdit extends StandardEditor<Task> {

    @Autowired
    private CurrentUserSubstitution currentUserSubstitution;

    @Autowired
    private BrowserFrame attachmentBrowserFrame;
    @Autowired
    private FileStorageUploadField importSubtasksField;
    @Autowired
    private TemporaryStorage temporaryStorage;
    @Autowired
    private DataContext dataContext;
    @Autowired
    private CollectionPropertyContainer<Subtask> subtasksDc;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        refreshAttachmentPreview();
    }

    @Subscribe(id = "taskDc", target = Target.DATA_CONTAINER)
    public void onTaskDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<Task> event) {
        if ("attachment".equals(event.getProperty())) {
            refreshAttachmentPreview();
        }
        if ("project".equals(event.getProperty())) {
            Project newProject = ((Project) event.getValue());
            if (newProject != null) {
                event.getItem().setPriority(newProject.getDefaultTaskPriority());
            }
        }
    }

    @Subscribe
    public void onInitEntity(InitEntityEvent<Task> event) {
        User user = (User) currentUserSubstitution.getEffectiveUser();
        event.getEntity().setAssignee(user);
    }

    private void refreshAttachmentPreview() {
        Task task = getEditedEntity();
        if (task.getAttachment() != null) {
            attachmentBrowserFrame.setSource(FileStorageResource.class)
                    .setFileReference(task.getAttachment())
                    .setMimeType(task.getAttachment().getContentType());
        }
    }

    @Subscribe("importSubtasksField")
    public void onAttachmentFieldFileUploadSucceed(final SingleFileUploadField.FileUploadSucceedEvent event) throws IOException {
        UUID fileId = importSubtasksField.getFileId();
        if (fileId == null) {
            return;
        }

        File file = temporaryStorage.getFile(fileId);
        if (file != null) {
            processFile(file);
            temporaryStorage.deleteFile(fileId);
        }

    }

    private void processFile(File file) throws IOException {
        List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        for (String line : lines) {
            Subtask subtask = dataContext.create(Subtask.class);
            subtask.setName(line);
            subtask.setTask(getEditedEntity());
            subtasksDc.getMutableItems().add(subtask);
        }
    }
}