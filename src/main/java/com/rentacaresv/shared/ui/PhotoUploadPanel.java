package com.rentacaresv.shared.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import lombok.Getter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel de upload de fotos con límites y vista previa
 */
public class PhotoUploadPanel extends VerticalLayout {

    private final String title;
    private final int maxPhotos;
    private final Upload upload;
    private final MultiFileMemoryBuffer buffer;
    private final FlexLayout previewContainer;
    private final Span counterLabel;
    
    @Getter
    private final List<PhotoPreviewItem> previewItems = new ArrayList<>();
    
    @Getter
    private final List<PendingUpload> pendingUploads = new ArrayList<>();
    
    private Consumer<PendingUpload> onPhotoAdded;
    private Consumer<PhotoPreviewItem> onPhotoDeleted;
    private Consumer<PhotoPreviewItem> onPrimaryChanged;
    
    private int currentCount = 0;

    public PhotoUploadPanel(String title, int maxPhotos) {
        this.title = title;
        this.maxPhotos = maxPhotos;
        
        setPadding(false);
        setSpacing(true);
        
        // Header
        H4 titleLabel = new H4(title);
        titleLabel.getStyle().set("margin", "0");
        
        counterLabel = new Span(String.format("0 / %d fotos", maxPhotos));
        counterLabel.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)");
        
        Div header = new Div(titleLabel, counterLabel);
        header.getStyle()
            .set("display", "flex")
            .set("justify-content", "space-between")
            .set("align-items", "center")
            .set("padding-bottom", "0.5rem")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        // Upload component
        buffer = new MultiFileMemoryBuffer();
        upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/jpg");
        upload.setMaxFileSize(5 * 1024 * 1024); // 5MB
        upload.setMaxFiles(maxPhotos);
        upload.setDropLabel(new Span("Arrastra fotos aquí o haz clic para seleccionar"));
        
        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            String mimeType = event.getMIMEType();
            
            try {
                InputStream inputStream = buffer.getInputStream(fileName);
                
                PendingUpload pendingUpload = new PendingUpload(fileName, mimeType, inputStream);
                pendingUploads.add(pendingUpload);
                
                if (onPhotoAdded != null) {
                    onPhotoAdded.accept(pendingUpload);
                }
                
                currentCount++;
                updateCounter();
                checkLimit();
                
            } catch (Exception e) {
                showError("Error al cargar la foto: " + e.getMessage());
            }
        });
        
        // Preview container
        previewContainer = new FlexLayout();
        previewContainer.setWidthFull();
        previewContainer.getStyle()
            .set("gap", "1rem")
            .set("flex-wrap", "wrap");
        
        add(header, upload, previewContainer);
    }

    /**
     * Agrega una foto existente (ya subida) al preview
     */
    public void addExistingPhoto(Long photoId, String photoUrl, boolean isPrimary) {
        PhotoPreviewItem previewItem = new PhotoPreviewItem(photoId, photoUrl, isPrimary);
        
        previewItem.setOnDelete(() -> {
            previewContainer.remove(previewItem);
            previewItems.remove(previewItem);
            currentCount--;
            updateCounter();
            checkLimit();
            
            if (onPhotoDeleted != null) {
                onPhotoDeleted.accept(previewItem);
            }
        });
        
        previewItem.setOnPrimaryChange(() -> {
            // Desmarcar todas las demás
            for (PhotoPreviewItem item : previewItems) {
                if (item != previewItem) {
                    item.setPrimary(false);
                }
            }
            
            if (onPrimaryChanged != null) {
                onPrimaryChanged.accept(previewItem);
            }
        });
        
        previewItems.add(previewItem);
        previewContainer.add(previewItem);
        currentCount++;
        updateCounter();
        checkLimit();
    }

    /**
     * Limpia todos los previews y uploads pendientes
     */
    public void clear() {
        previewContainer.removeAll();
        previewItems.clear();
        pendingUploads.clear();
        currentCount = 0;
        updateCounter();
        checkLimit();
    }

    public void setOnPhotoAdded(Consumer<PendingUpload> callback) {
        this.onPhotoAdded = callback;
    }

    public void setOnPhotoDeleted(Consumer<PhotoPreviewItem> callback) {
        this.onPhotoDeleted = callback;
    }

    public void setOnPrimaryChanged(Consumer<PhotoPreviewItem> callback) {
        this.onPrimaryChanged = callback;
    }

    private void updateCounter() {
        counterLabel.setText(String.format("%d / %d fotos", currentCount, maxPhotos));
        
        if (currentCount >= maxPhotos) {
            counterLabel.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("font-weight", "bold");
        } else {
            counterLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "normal");
        }
    }

    private void checkLimit() {
        if (currentCount >= maxPhotos) {
            upload.setVisible(false);
            showWarning(String.format("Has alcanzado el límite de %d fotos. Puedes agregar más pero ya tienes muchas.", maxPhotos));
        } else {
            upload.setVisible(true);
        }
    }

    private void showWarning(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /**
     * Clase para almacenar uploads pendientes
     */
    @Getter
    public static class PendingUpload {
        private final String fileName;
        private final String mimeType;
        private final InputStream inputStream;

        public PendingUpload(String fileName, String mimeType, InputStream inputStream) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.inputStream = inputStream;
        }
    }
}
