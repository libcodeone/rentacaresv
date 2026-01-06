package com.rentacaresv.shared.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Componente para vista previa de fotos con checkbox para marcar como principal
 */
public class PhotoPreviewItem extends VerticalLayout {

    @Getter
    private final String photoUrl;
    
    @Getter
    private final Long photoId;
    
    private final Checkbox primaryCheckbox;
    private final Image thumbnail;
    private final Button deleteButton;
    
    private Runnable onDelete;
    private Runnable onPrimaryChange;

    public PhotoPreviewItem(Long photoId, String photoUrl, boolean isPrimary) {
        this.photoId = photoId;
        this.photoUrl = photoUrl;
        
        setSpacing(false);
        setPadding(false);
        setAlignItems(Alignment.CENTER);
        setWidth("150px");
        
        // Thumbnail
        thumbnail = new Image(photoUrl, "Photo");
        thumbnail.setWidth("140px");
        thumbnail.setHeight("100px");
        thumbnail.getStyle()
            .set("object-fit", "cover")
            .set("border-radius", "4px")
            .set("border", "1px solid var(--lumo-contrast-20pct)");
        
        // Checkbox para marcar como principal
        primaryCheckbox = new Checkbox("Principal");
        primaryCheckbox.setValue(isPrimary);
        primaryCheckbox.addValueChangeListener(e -> {
            if (e.getValue() && onPrimaryChange != null) {
                onPrimaryChange.run();
            }
        });
        
        // Botón eliminar
        deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.addClickListener(e -> {
            if (onDelete != null) {
                onDelete.run();
            }
        });
        
        HorizontalLayout controls = new HorizontalLayout(primaryCheckbox, deleteButton);
        controls.setWidthFull();
        controls.setJustifyContentMode(JustifyContentMode.BETWEEN);
        controls.setAlignItems(Alignment.CENTER);
        
        add(thumbnail, controls);
    }

    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnPrimaryChange(Runnable onPrimaryChange) {
        this.onPrimaryChange = onPrimaryChange;
    }

    public void setPrimary(boolean primary) {
        primaryCheckbox.setValue(primary);
    }

    public boolean isPrimary() {
        return primaryCheckbox.getValue();
    }
}
