package com.rentacaresv.shared.ui;

import com.rentacaresv.rental.domain.photo.RentalPhotoType;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel de upload de fotos para rentas (entrega/devolución)
 * Contiene 3 tabs: Exteriores, Interiores, Accesorios
 */
public class RentalPhotoUploadPanel extends VerticalLayout {

    private final String title;
    private final boolean isDelivery;
    
    @Getter
    private final PhotoUploadPanel exteriorPanel;
    @Getter
    private final PhotoUploadPanel interiorPanel;
    @Getter
    private final PhotoUploadPanel accessoriesPanel;
    
    private final Tab exteriorTab;
    private final Tab interiorTab;
    private final Tab accessoriesTab;
    
    private final Map<RentalPhotoType, PhotoUploadPanel> panelMap = new HashMap<>();

    public RentalPhotoUploadPanel(String title, boolean isDelivery) {
        this.title = title;
        this.isDelivery = isDelivery;
        
        setPadding(false);
        setSpacing(true);
        
        // Título con icono
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(Alignment.CENTER);
        Icon cameraIcon = VaadinIcon.CAMERA.create();
        cameraIcon.setSize("18px");
        H4 titleLabel = new H4(title);
        titleLabel.getStyle().set("margin", "0");
        titleLayout.add(cameraIcon, titleLabel);
        titleLayout.getStyle().set("margin-bottom", "1rem");
        
        // TabSheet
        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();
        
        // Tab 1: Exteriores
        exteriorPanel = new PhotoUploadPanel("Fotos Exteriores", 10);
        exteriorTab = createTab(VaadinIcon.CAR, "Exteriores", 0);
        tabSheet.add(exteriorTab, exteriorPanel);
        
        // Tab 2: Interiores
        interiorPanel = new PhotoUploadPanel("Fotos Interiores", 10);
        interiorTab = createTab(VaadinIcon.WORKPLACE, "Interiores", 0);
        tabSheet.add(interiorTab, interiorPanel);
        
        // Tab 3: Accesorios
        accessoriesPanel = new PhotoUploadPanel("Fotos Accesorios", 10);
        accessoriesTab = createTab(VaadinIcon.TOOLS, "Accesorios", 0);
        tabSheet.add(accessoriesTab, accessoriesPanel);
        
        // Mapear paneles a tipos de foto
        if (isDelivery) {
            panelMap.put(RentalPhotoType.DELIVERY_EXTERIOR, exteriorPanel);
            panelMap.put(RentalPhotoType.DELIVERY_INTERIOR, interiorPanel);
            panelMap.put(RentalPhotoType.DELIVERY_ACCESSORIES, accessoriesPanel);
        } else {
            panelMap.put(RentalPhotoType.RETURN_EXTERIOR, exteriorPanel);
            panelMap.put(RentalPhotoType.RETURN_INTERIOR, interiorPanel);
            panelMap.put(RentalPhotoType.RETURN_ACCESSORIES, accessoriesPanel);
        }
        
        // Configurar callbacks para actualizar contadores
        exteriorPanel.setOnPhotoAdded(upload -> updateTabCounter(exteriorTab, exteriorPanel, VaadinIcon.CAR, "Exteriores"));
        exteriorPanel.setOnPhotoDeleted(item -> updateTabCounter(exteriorTab, exteriorPanel, VaadinIcon.CAR, "Exteriores"));
        
        interiorPanel.setOnPhotoAdded(upload -> updateTabCounter(interiorTab, interiorPanel, VaadinIcon.WORKPLACE, "Interiores"));
        interiorPanel.setOnPhotoDeleted(item -> updateTabCounter(interiorTab, interiorPanel, VaadinIcon.WORKPLACE, "Interiores"));
        
        accessoriesPanel.setOnPhotoAdded(upload -> updateTabCounter(accessoriesTab, accessoriesPanel, VaadinIcon.TOOLS, "Accesorios"));
        accessoriesPanel.setOnPhotoDeleted(item -> updateTabCounter(accessoriesTab, accessoriesPanel, VaadinIcon.TOOLS, "Accesorios"));
        
        add(titleLayout, tabSheet);
    }

    /**
     * Crea un tab con icono
     */
    private Tab createTab(VaadinIcon iconType, String label, int count) {
        HorizontalLayout tabContent = new HorizontalLayout();
        tabContent.setAlignItems(Alignment.CENTER);
        tabContent.setSpacing(true);
        
        Icon icon = iconType.create();
        icon.setSize("16px");
        
        tabContent.add(icon);
        tabContent.add(new com.vaadin.flow.component.html.Span(String.format("%s (%d/10)", label, count)));
        
        Tab tab = new Tab(tabContent);
        return tab;
    }

    /**
     * Obtiene el panel correspondiente a un tipo de foto
     */
    public PhotoUploadPanel getPanelForType(RentalPhotoType photoType) {
        return panelMap.get(photoType);
    }

    /**
     * Obtiene todos los uploads pendientes de todos los paneles
     */
    public Map<RentalPhotoType, List<PhotoUploadPanel.PendingUpload>> getAllPendingUploads() {
        Map<RentalPhotoType, List<PhotoUploadPanel.PendingUpload>> uploads = new HashMap<>();
        
        for (Map.Entry<RentalPhotoType, PhotoUploadPanel> entry : panelMap.entrySet()) {
            List<PhotoUploadPanel.PendingUpload> panelUploads = entry.getValue().getPendingUploads();
            if (!panelUploads.isEmpty()) {
                uploads.put(entry.getKey(), panelUploads);
            }
        }
        
        return uploads;
    }

    /**
     * Verifica si hay al menos una foto en algún panel
     */
    public boolean hasAnyPhotos() {
        return !exteriorPanel.getPreviewItems().isEmpty() ||
               !interiorPanel.getPreviewItems().isEmpty() ||
               !accessoriesPanel.getPreviewItems().isEmpty();
    }

    /**
     * Obtiene el total de fotos en todos los paneles
     */
    public int getTotalPhotoCount() {
        return exteriorPanel.getPreviewItems().size() +
               interiorPanel.getPreviewItems().size() +
               accessoriesPanel.getPreviewItems().size();
    }

    /**
     * Limpia todos los paneles
     */
    public void clearAll() {
        exteriorPanel.clear();
        interiorPanel.clear();
        accessoriesPanel.clear();
        updateAllCounters();
    }

    /**
     * Actualiza el contador de un tab específico
     */
    private void updateTabCounter(Tab tab, PhotoUploadPanel panel, VaadinIcon iconType, String label) {
        int count = panel.getPreviewItems().size();
        
        HorizontalLayout tabContent = new HorizontalLayout();
        tabContent.setAlignItems(Alignment.CENTER);
        tabContent.setSpacing(true);
        
        Icon icon = iconType.create();
        icon.setSize("16px");
        
        tabContent.add(icon);
        tabContent.add(new com.vaadin.flow.component.html.Span(String.format("%s (%d/10)", label, count)));
        
        tab.removeAll();
        tab.add(tabContent);
    }

    /**
     * Actualiza todos los contadores
     */
    private void updateAllCounters() {
        updateTabCounter(exteriorTab, exteriorPanel, VaadinIcon.CAR, "Exteriores");
        updateTabCounter(interiorTab, interiorPanel, VaadinIcon.WORKPLACE, "Interiores");
        updateTabCounter(accessoriesTab, accessoriesPanel, VaadinIcon.TOOLS, "Accesorios");
    }

    /**
     * Carga fotos existentes en el panel correspondiente
     */
    public void loadExistingPhoto(Long photoId, String photoUrl, RentalPhotoType photoType) {
        PhotoUploadPanel panel = panelMap.get(photoType);
        if (panel != null) {
            panel.addExistingPhoto(photoId, photoUrl, false);
            
            // Actualizar el contador del tab correspondiente
            if (photoType.name().contains("EXTERIOR")) {
                updateTabCounter(exteriorTab, exteriorPanel, VaadinIcon.CAR, "Exteriores");
            } else if (photoType.name().contains("INTERIOR")) {
                updateTabCounter(interiorTab, interiorPanel, VaadinIcon.WORKPLACE, "Interiores");
            } else {
                updateTabCounter(accessoriesTab, accessoriesPanel, VaadinIcon.TOOLS, "Accesorios");
            }
        }
    }
}
