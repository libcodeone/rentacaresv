package com.rentacaresv.contract.ui;

import com.rentacaresv.contract.domain.AccessoryCatalog;
import com.rentacaresv.contract.domain.AccessoryCategory;
import com.rentacaresv.contract.domain.VehicleDiagram;
import com.rentacaresv.contract.infrastructure.AccessoryCatalogRepository;
import com.rentacaresv.contract.infrastructure.VehicleDiagramRepository;
import com.rentacaresv.shared.storage.FileStorageService;
import com.rentacaresv.shared.storage.FolderType;
import com.rentacaresv.vehicle.domain.VehicleType;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Vista para gestionar el catálogo de accesorios y diagramas de vehículos
 */
@PageTitle("Catálogo de Contratos")
@Route(value = "contract-catalog", layout = MainLayout.class)
@Menu(order = 8, icon = LineAwesomeIconUrl.LIST_ALT_SOLID)
@RolesAllowed("ADMIN")
@Slf4j
public class ContractCatalogView extends VerticalLayout {

    private final AccessoryCatalogRepository accessoryRepository;
    private final VehicleDiagramRepository diagramRepository;
    private final FileStorageService fileStorageService;

    private Grid<AccessoryCatalog> accessoryGrid;
    private Grid<VehicleDiagram> diagramGrid;
    private VerticalLayout accessoriesContent;
    private VerticalLayout diagramsContent;

    public ContractCatalogView(AccessoryCatalogRepository accessoryRepository,
                                VehicleDiagramRepository diagramRepository,
                                FileStorageService fileStorageService) {
        this.accessoryRepository = accessoryRepository;
        this.diagramRepository = diagramRepository;
        this.fileStorageService = fileStorageService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Catálogo de Contratos");
        title.getStyle().set("margin-top", "0");

        // Tabs para alternar entre accesorios y diagramas
        Tab accessoriesTab = new Tab(VaadinIcon.LIST.create(), new Span("Accesorios"));
        Tab diagramsTab = new Tab(VaadinIcon.CAR.create(), new Span("Diagramas de Vehículos"));

        Tabs tabs = new Tabs(accessoriesTab, diagramsTab);
        tabs.setWidthFull();

        // Contenido de cada tab
        accessoriesContent = createAccessoriesContent();
        diagramsContent = createDiagramsContent();
        diagramsContent.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            accessoriesContent.setVisible(event.getSelectedTab() == accessoriesTab);
            diagramsContent.setVisible(event.getSelectedTab() == diagramsTab);
        });

        add(title, tabs, accessoriesContent, diagramsContent);
    }

    // ========================================
    // SECCIÓN: Accesorios
    // ========================================

    private VerticalLayout createAccessoriesContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setSizeFull();

        // Botón agregar
        Button addButton = new Button("Agregar Accesorio", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openAccessoryDialog(null));

        HorizontalLayout toolbar = new HorizontalLayout(addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        // Grid de accesorios
        accessoryGrid = new Grid<>(AccessoryCatalog.class, false);
        
        // Columna de Acciones (PRIMERA - FIJA)
        accessoryGrid.addComponentColumn(this::createAccessoryActions)
                .setHeader("Acciones")
                .setWidth("120px")
                .setFlexGrow(0)
                .setFrozen(true);
        
        accessoryGrid.addColumn(AccessoryCatalog::getName).setHeader("Nombre").setFlexGrow(2);
        accessoryGrid.addColumn(acc -> acc.getCategory().name()).setHeader("Categoría").setFlexGrow(1);
        accessoryGrid.addColumn(AccessoryCatalog::getDisplayOrder).setHeader("Orden").setWidth("80px");
        accessoryGrid.addColumn(acc -> acc.getIsActive() ? "Activo" : "Inactivo").setHeader("Estado").setWidth("100px");

        accessoryGrid.setItems(accessoryRepository.findAll());
        accessoryGrid.setSizeFull();

        layout.add(toolbar, accessoryGrid);
        layout.setFlexGrow(1, accessoryGrid);

        return layout;
    }

    private HorizontalLayout createAccessoryActions(AccessoryCatalog accessory) {
        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.setTooltipText("Editar");
        editBtn.addClickListener(e -> openAccessoryDialog(accessory));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText("Eliminar");
        deleteBtn.addClickListener(e -> confirmDeleteAccessory(accessory));

        return new HorizontalLayout(editBtn, deleteBtn);
    }

    private void openAccessoryDialog(AccessoryCatalog accessory) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(accessory == null ? "Nuevo Accesorio" : "Editar Accesorio");
        dialog.setWidth("400px");

        TextField nameField = new TextField("Nombre");
        nameField.setWidthFull();
        nameField.setRequired(true);

        ComboBox<AccessoryCategory> categoryCombo = new ComboBox<>("Categoría");
        categoryCombo.setItems(AccessoryCategory.values());
        categoryCombo.setItemLabelGenerator(AccessoryCategory::name);
        categoryCombo.setWidthFull();
        categoryCombo.setRequired(true);

        IntegerField orderField = new IntegerField("Orden de visualización");
        orderField.setMin(0);
        orderField.setValue(0);
        orderField.setWidthFull();

        if (accessory != null) {
            nameField.setValue(accessory.getName());
            categoryCombo.setValue(accessory.getCategory());
            orderField.setValue(accessory.getDisplayOrder());
        }

        FormLayout form = new FormLayout(nameField, categoryCombo, orderField);

        Button saveBtn = new Button("Guardar", e -> {
            if (nameField.isEmpty() || categoryCombo.isEmpty()) {
                Notification.show("Complete los campos requeridos", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            AccessoryCatalog acc = accessory != null ? accessory : new AccessoryCatalog();
            acc.setName(nameField.getValue());
            acc.setCategory(categoryCombo.getValue());
            acc.setDisplayOrder(orderField.getValue());
            acc.setIsActive(true);

            accessoryRepository.save(acc);
            refreshAccessoryGrid();
            dialog.close();

            Notification.show("Accesorio guardado", 2000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void confirmDeleteAccessory(AccessoryCatalog accessory) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Eliminar Accesorio");
        dialog.setText("¿Está seguro de eliminar el accesorio '" + accessory.getName() + "'?");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        dialog.setConfirmText("Eliminar");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            accessoryRepository.delete(accessory);
            refreshAccessoryGrid();
            Notification.show("Accesorio eliminado", 2000, Notification.Position.BOTTOM_CENTER);
        });
        dialog.open();
    }

    private void refreshAccessoryGrid() {
        accessoryGrid.setItems(accessoryRepository.findAll());
    }

    // ========================================
    // SECCIÓN: Diagramas de Vehículos
    // ========================================

    private VerticalLayout createDiagramsContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setSizeFull();

        Paragraph description = new Paragraph(
            "Suba imágenes de diagramas (siluetas) para cada tipo de vehículo. " +
            "Estas imágenes se usarán para marcar daños en los contratos. " +
            "Formatos recomendados: PNG con fondo transparente.");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Grid de diagramas
        diagramGrid = new Grid<>(VehicleDiagram.class, false);
        
        // Columna de Acciones (PRIMERA - FIJA)
        diagramGrid.addComponentColumn(this::createDiagramActions)
                .setHeader("Acciones")
                .setWidth("150px")
                .setFlexGrow(0)
                .setFrozen(true);
        
        diagramGrid.addComponentColumn(this::createDiagramPreview).setHeader("Vista Previa").setWidth("120px");
        diagramGrid.addColumn(diagram -> diagram.getVehicleType().name()).setHeader("Tipo de Vehículo").setFlexGrow(1);
        diagramGrid.addColumn(VehicleDiagram::getName).setHeader("Nombre").setFlexGrow(1);
        diagramGrid.addColumn(diagram -> diagram.getIsActive() ? "Activo" : "Inactivo").setHeader("Estado").setWidth("100px");

        refreshDiagramGrid();
        diagramGrid.setSizeFull();

        // Crear diagramas faltantes
        Button createMissingBtn = new Button("Crear Diagramas Faltantes", VaadinIcon.PLUS.create());
        createMissingBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createMissingBtn.addClickListener(e -> createMissingDiagrams());

        HorizontalLayout toolbar = new HorizontalLayout(createMissingBtn);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        layout.add(description, toolbar, diagramGrid);
        layout.setFlexGrow(1, diagramGrid);

        return layout;
    }

    private Div createDiagramPreview(VehicleDiagram diagram) {
        Div container = new Div();
        container.getStyle()
            .set("width", "100px")
            .set("height", "60px")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-s)");

        String imageUrl = diagram.getDiagramUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Image img = new Image(imageUrl, diagram.getName());
            img.setMaxHeight("50px");
            img.setMaxWidth("90px");
            img.getStyle().set("object-fit", "contain");
            container.add(img);
        } else {
            Span placeholder = new Span("Sin imagen");
            placeholder.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");
            container.add(placeholder);
        }

        return container;
    }

    private HorizontalLayout createDiagramActions(VehicleDiagram diagram) {
        // Botón subir imagen
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/svg+xml");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);
        
        Button uploadBtn = new Button(VaadinIcon.UPLOAD.create());
        uploadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        uploadBtn.setTooltipText("Subir imagen");
        upload.setUploadButton(uploadBtn);
        upload.setDropAllowed(false);

        upload.addSucceededListener(event -> {
            try {
                String fileName = "diagram_" + diagram.getVehicleType().name().toLowerCase() + "_" + System.currentTimeMillis();
                String imageUrl = fileStorageService.uploadFile(
                    buffer.getInputStream(),
                    fileName + getExtension(event.getMIMEType()),
                    event.getMIMEType(),
                    FolderType.SETTINGS,
                    "diagrams"
                );

                diagram.updateImageUrl(imageUrl);
                diagramRepository.save(diagram);
                refreshDiagramGrid();

                Notification.show("Imagen actualizada", 2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception e) {
                log.error("Error subiendo imagen: {}", e.getMessage(), e);
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        // Botón eliminar imagen
        Button deleteImgBtn = new Button(VaadinIcon.TRASH.create());
        deleteImgBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteImgBtn.setTooltipText("Eliminar imagen");
        deleteImgBtn.setEnabled(diagram.getDiagramUrl() != null);
        deleteImgBtn.addClickListener(e -> {
            diagram.setImageUrl(null);
            diagram.setSvgUrl(null);
            diagram.setSvgContent(null);
            diagramRepository.save(diagram);
            refreshDiagramGrid();
            Notification.show("Imagen eliminada", 2000, Notification.Position.BOTTOM_CENTER);
        });

        HorizontalLayout actions = new HorizontalLayout(upload, deleteImgBtn);
        actions.setSpacing(false);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        return actions;
    }

    private String getExtension(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/svg+xml" -> ".svg";
            default -> ".png";
        };
    }

    private void createMissingDiagrams() {
        List<VehicleType> existingTypes = diagramRepository.findAll().stream()
                .map(VehicleDiagram::getVehicleType)
                .toList();

        int created = 0;
        for (VehicleType type : VehicleType.values()) {
            if (!existingTypes.contains(type)) {
                VehicleDiagram diagram = VehicleDiagram.builder()
                        .vehicleType(type)
                        .name("Diagrama " + type.name())
                        .isActive(true)
                        .build();
                diagramRepository.save(diagram);
                created++;
            }
        }

        refreshDiagramGrid();

        if (created > 0) {
            Notification.show(created + " diagrama(s) creado(s)", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show("Todos los tipos de vehículo ya tienen diagrama", 3000, Notification.Position.BOTTOM_CENTER);
        }
    }

    private void refreshDiagramGrid() {
        diagramGrid.setItems(diagramRepository.findAll());
    }
}
