package com.rentacaresv.vehicle.ui;

import com.rentacaresv.catalog.application.CatalogService;
import com.rentacaresv.catalog.application.VehicleBrandDTO;
import com.rentacaresv.catalog.application.VehicleModelDTO;
import com.rentacaresv.shared.ui.PhotoUploadPanel;
import com.rentacaresv.vehicle.application.CreateVehicleCommand;
import com.rentacaresv.vehicle.application.VehicleDTO;
import com.rentacaresv.vehicle.application.VehiclePhotoService;
import com.rentacaresv.vehicle.application.VehicleService;
import com.rentacaresv.vehicle.domain.FuelType;
import com.rentacaresv.vehicle.domain.TransmissionType;
import com.rentacaresv.vehicle.domain.VehicleStatus;
import com.rentacaresv.vehicle.domain.VehicleType;
import com.rentacaresv.vehicle.domain.photo.PhotoType;
import com.rentacaresv.vehicle.domain.photo.VehiclePhoto;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para crear/editar vehículos CON FOTOS
 * Usa catálogos para marca y modelo con autocompletado
 */
@Slf4j
public class VehicleFormDialog extends Dialog {

    private final VehicleService vehicleService;
    private final VehiclePhotoService vehiclePhotoService;
    private final CatalogService catalogService;
    private final BeanValidationBinder<CreateVehicleCommand> binder;
    private CreateVehicleCommand command;
    private VehicleDTO vehicleToEdit;

    // Datos de catálogos
    private List<VehicleBrandDTO> allBrands;
    private List<VehicleModelDTO> allModels;

    // Campos del formulario
    private TextField licensePlate;
    private ComboBox<VehicleBrandDTO> brandCombo;
    private ComboBox<VehicleModelDTO> modelCombo;
    private ComboBox<VehicleType> vehicleTypeCombo;
    private IntegerField year;
    private TextField color;
    private ComboBox<TransmissionType> transmissionType;
    private ComboBox<FuelType> fuelType;
    private IntegerField passengerCapacity;
    private IntegerField mileage;

    // Campo de estado (solo en edición)
    private ComboBox<VehicleStatus> statusField;

    // Precios
    private NumberField priceNormal;
    private NumberField priceVip;
    private NumberField priceMoreThan15Days;
    private NumberField priceMonthly;

    private TextArea notes;

    // Paneles de fotos
    private PhotoUploadPanel exteriorPhotosPanel;
    private PhotoUploadPanel interiorPhotosPanel;

    // Tabs para actualizar contadores
    private Tab exteriorTab;
    private Tab interiorTab;

    // Botones
    private Button saveButton;
    private Button cancelButton;

    private final boolean isEdit;

    public VehicleFormDialog(VehicleService vehicleService, VehiclePhotoService vehiclePhotoService,
            CatalogService catalogService, VehicleDTO vehicleToEdit) {
        this.vehicleService = vehicleService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.catalogService = catalogService;
        this.vehicleToEdit = vehicleToEdit;
        this.isEdit = vehicleToEdit != null;
        this.command = new CreateVehicleCommand();

        this.binder = new BeanValidationBinder<>(CreateVehicleCommand.class);

        // Cargar catálogos
        loadCatalogs();

        configureDialog();
        createForm();
        configureButtons();

        if (isEdit) {
            populateForm(vehicleToEdit);
            loadExistingPhotos(vehicleToEdit.getId());
        }
    }

    private void loadCatalogs() {
        allBrands = new ArrayList<>(catalogService.findActiveBrands());
        allModels = new ArrayList<>(catalogService.findActiveModels());
    }

    private void configureDialog() {
        setModal(true);
        setDraggable(false);
        setResizable(false);
        setWidth("900px");
        setMaxHeight("95vh");
    }

    private void createForm() {
        H3 title = new H3(isEdit ? "Editar Vehículo" : "Nuevo Vehículo");
        title.getStyle().set("margin", "0 0 1rem 0");

        // TabSheet para organizar el formulario
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        // Tab 1: Información Básica
        VerticalLayout basicInfoTab = createBasicInfoTab();
        tabSheet.add(createTabWithIcon(VaadinIcon.INFO_CIRCLE, "Información Básica"), basicInfoTab);

        // Tab 2: Fotos Exteriores
        exteriorPhotosPanel = new PhotoUploadPanel("Fotos Exteriores", 10);
        configurePhotoPanel(exteriorPhotosPanel, PhotoType.EXTERIOR);
        exteriorTab = createTabWithIcon(VaadinIcon.CAR, "Exteriores (0/10)");
        tabSheet.add(exteriorTab, exteriorPhotosPanel);

        // Tab 3: Fotos Interiores
        interiorPhotosPanel = new PhotoUploadPanel("Fotos Interiores", 10);
        configurePhotoPanel(interiorPhotosPanel, PhotoType.INTERIOR);
        interiorTab = createTabWithIcon(VaadinIcon.WORKPLACE, "Interiores (0/10)");
        tabSheet.add(interiorTab, interiorPhotosPanel);

        // Layout principal
        VerticalLayout content = new VerticalLayout(title, tabSheet);
        content.setPadding(true);
        content.setSpacing(true);
        content.setSizeFull();

        add(content);
    }

    private Tab createTabWithIcon(VaadinIcon iconType, String label) {
        HorizontalLayout tabContent = new HorizontalLayout();
        tabContent.setAlignItems(FlexComponent.Alignment.CENTER);
        tabContent.setSpacing(true);

        Icon icon = iconType.create();
        icon.setSize("16px");

        tabContent.add(icon);
        tabContent.add(new Span(label));

        return new Tab(tabContent);
    }

    private VerticalLayout createBasicInfoTab() {
        // Campos básicos
        licensePlate = new TextField("Placa");
        licensePlate.setPlaceholder("Ej: P123456");
        licensePlate.setRequired(true);
        licensePlate.setMaxLength(20);

        // ComboBox de Marca con búsqueda
        brandCombo = new ComboBox<>("Marca");
        brandCombo.setItems(allBrands);
        brandCombo.setItemLabelGenerator(VehicleBrandDTO::getName);
        brandCombo.setPlaceholder("Buscar o seleccionar marca...");
        brandCombo.setRequired(true);
        brandCombo.setClearButtonVisible(true);
        brandCombo.setAllowCustomValue(true);
        
        // Permitir escribir marcas personalizadas
        brandCombo.addCustomValueSetListener(e -> {
            String customValue = e.getDetail();
            if (customValue != null && !customValue.isBlank()) {
                VehicleBrandDTO customBrand = VehicleBrandDTO.builder()
                        .id(-1L)
                        .name(customValue.trim())
                        .build();
                
                boolean exists = allBrands.stream()
                        .anyMatch(b -> b.getName().equalsIgnoreCase(customValue.trim()));
                
                if (!exists) {
                    allBrands.add(0, customBrand);
                    brandCombo.setItems(allBrands);
                }
                brandCombo.setValue(customBrand);
            }
        });
        
        // Filtrar modelos cuando cambia la marca
        brandCombo.addValueChangeListener(e -> {
            VehicleBrandDTO selectedBrand = e.getValue();
            updateModelCombo(selectedBrand);
        });

        // Botón para agregar nueva marca
        Button addBrandButton = new Button(VaadinIcon.PLUS.create());
        addBrandButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        addBrandButton.getElement().setAttribute("title", "Crear nueva marca");
        addBrandButton.addClickListener(e -> openCreateBrandDialog());

        // Layout de marca con botón +
        HorizontalLayout brandLayout = new HorizontalLayout(brandCombo, addBrandButton);
        brandLayout.setAlignItems(FlexComponent.Alignment.END);
        brandLayout.setSpacing(false);
        brandLayout.setFlexGrow(1, brandCombo);
        brandCombo.setWidthFull();

        // ComboBox de Modelo con búsqueda
        modelCombo = new ComboBox<>("Modelo");
        modelCombo.setPlaceholder("Primero seleccione una marca...");
        modelCombo.setItemLabelGenerator(VehicleModelDTO::getName);
        modelCombo.setRequired(true);
        modelCombo.setClearButtonVisible(true);
        modelCombo.setAllowCustomValue(true);
        modelCombo.setEnabled(false);
        
        // Permitir escribir modelos personalizados
        modelCombo.addCustomValueSetListener(e -> {
            String customValue = e.getDetail();
            if (customValue != null && !customValue.isBlank()) {
                VehicleBrandDTO selectedBrand = brandCombo.getValue();
                Long brandId = selectedBrand != null ? selectedBrand.getId() : -1L;
                
                VehicleModelDTO customModel = VehicleModelDTO.builder()
                        .id(-1L)
                        .brandId(brandId)
                        .name(customValue.trim())
                        .vehicleType(VehicleType.SEDAN.name())
                        .vehicleTypeLabel(VehicleType.SEDAN.getLabel())
                        .build();
                
                List<VehicleModelDTO> currentItems = new ArrayList<>(modelCombo.getListDataView().getItems().toList());
                boolean exists = currentItems.stream()
                        .anyMatch(m -> m.getName().equalsIgnoreCase(customValue.trim()));
                
                if (!exists) {
                    currentItems.add(0, customModel);
                    modelCombo.setItems(currentItems);
                }
                modelCombo.setValue(customModel);
            }
        });
        
        // Cuando se selecciona un modelo del catálogo, autocompletar tipo de vehículo
        modelCombo.addValueChangeListener(e -> {
            VehicleModelDTO selectedModel = e.getValue();
            if (selectedModel != null && selectedModel.getVehicleType() != null && selectedModel.getId() > 0) {
                try {
                    vehicleTypeCombo.setValue(VehicleType.valueOf(selectedModel.getVehicleType()));
                } catch (Exception ignored) {}
            }
        });

        // Botón para agregar nuevo modelo
        Button addModelButton = new Button(VaadinIcon.PLUS.create());
        addModelButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        addModelButton.getElement().setAttribute("title", "Crear nuevo modelo");
        addModelButton.addClickListener(e -> openCreateModelDialog());

        // Layout de modelo con botón +
        HorizontalLayout modelLayout = new HorizontalLayout(modelCombo, addModelButton);
        modelLayout.setAlignItems(FlexComponent.Alignment.END);
        modelLayout.setSpacing(false);
        modelLayout.setFlexGrow(1, modelCombo);
        modelCombo.setWidthFull();

        // ComboBox de Tipo de Vehículo
        vehicleTypeCombo = new ComboBox<>("Tipo de Vehículo");
        vehicleTypeCombo.setItems(VehicleType.values());
        vehicleTypeCombo.setItemLabelGenerator(VehicleType::getLabel);
        vehicleTypeCombo.setRequired(true);
        vehicleTypeCombo.setPlaceholder("Seleccionar tipo...");

        year = new IntegerField("Año");
        year.setMin(1900);
        year.setMax(LocalDateTime.now().getYear() + 1);
        year.setValue(LocalDateTime.now().getYear());
        year.setRequired(true);
        year.setStepButtonsVisible(true);

        color = new TextField("Color");
        color.setPlaceholder("Ej: Blanco");
        color.setMaxLength(30);

        // Combos
        transmissionType = new ComboBox<>("Transmisión");
        transmissionType.setItems(TransmissionType.values());
        transmissionType.setItemLabelGenerator(type -> type == TransmissionType.MANUAL ? "Manual" : "Automática");
        transmissionType.setRequired(true);

        fuelType = new ComboBox<>("Combustible");
        fuelType.setItems(FuelType.values());
        fuelType.setItemLabelGenerator(type -> {
            return switch (type) {
                case GASOLINE -> "Gasolina";
                case DIESEL -> "Diesel";
                case HYBRID -> "Híbrido";
                case ELECTRIC -> "Eléctrico";
            };
        });
        fuelType.setRequired(true);

        passengerCapacity = new IntegerField("Capacidad de Pasajeros");
        passengerCapacity.setMin(1);
        passengerCapacity.setMax(50);
        passengerCapacity.setValue(5);
        passengerCapacity.setRequired(true);
        passengerCapacity.setStepButtonsVisible(true);

        mileage = new IntegerField("Kilometraje");
        mileage.setMin(0);
        mileage.setValue(0);
        mileage.setStepButtonsVisible(true);
        mileage.setSuffixComponent(new Span("km"));

        // Campo de estado (solo visible en modo edición)
        statusField = new ComboBox<>("Estado del Vehículo");
        List<VehicleStatus> statuses = new ArrayList<>(List.of(VehicleStatus.values()));
        if (!isEdit || (vehicleToEdit != null && vehicleToEdit.getStatus() != null
                && !VehicleStatus.RENTED.name().equals(vehicleToEdit.getStatus()))) {
            statuses.remove(VehicleStatus.RENTED);
        }
        statusField.setItems(statuses);
        statusField.setItemLabelGenerator(status -> {
            return switch (status) {
                case AVAILABLE -> "Disponible";
                case RENTED -> "Rentado";
                case MAINTENANCE -> "Mantenimiento";
                case OUT_OF_SERVICE -> "Fuera de servicio";
            };
        });
        statusField.setRequired(true);
        statusField.setHelperText("Cambiar el estado del vehículo");

        // Precios
        priceNormal = createPriceField("Precio Normal (día)");
        priceVip = createPriceField("Precio VIP (día)");
        priceMoreThan15Days = createPriceField("Precio +15 días");
        priceMonthly = createPriceField("Precio Mensual");

        notes = new TextArea("Notas");
        notes.setPlaceholder("Información adicional...");
        notes.setMaxLength(500);
        notes.setHelperText("Máximo 500 caracteres");

        // Layout del formulario
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));

        // Organizar campos - Sección de identificación
        formLayout.add(licensePlate, 2);
        
        // Sección de marca/modelo
        HorizontalLayout brandModelTitleLayout = new HorizontalLayout();
        brandModelTitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon carIcon = VaadinIcon.CAR.create();
        carIcon.setSize("18px");
        H4 brandModelTitle = new H4("Marca y Modelo");
        brandModelTitle.getStyle().set("margin", "0");
        brandModelTitleLayout.add(carIcon, brandModelTitle);
        brandModelTitleLayout.getStyle().set("margin", "0.5rem 0");
        
        formLayout.add(brandModelTitleLayout, 2);
        
        // Agregar layouts con botones + (ocupan todo el ancho de su columna)
        formLayout.add(brandLayout, modelLayout);
        
        formLayout.add(vehicleTypeCombo, year);
        
        // Sección de características
        formLayout.add(color, transmissionType);
        formLayout.add(fuelType, passengerCapacity);
        formLayout.add(mileage, 1);

        // Agregar campo de estado solo en modo edición
        if (isEdit) {
            HorizontalLayout statusTitleLayout = new HorizontalLayout();
            statusTitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            Icon statusIcon = VaadinIcon.COG.create();
            statusIcon.setSize("18px");
            H4 statusTitle = new H4("Estado");
            statusTitle.getStyle().set("margin", "0");
            statusTitleLayout.add(statusIcon, statusTitle);
            statusTitleLayout.getStyle().set("margin", "1rem 0 0.5rem 0");

            formLayout.add(statusTitleLayout, 2);
            formLayout.add(statusField, 2);
        }

        // Sección de precios
        HorizontalLayout pricesTitleLayout = new HorizontalLayout();
        pricesTitleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon priceIcon = VaadinIcon.MONEY.create();
        priceIcon.setSize("18px");
        H4 pricesTitle = new H4("Precios por Categoría");
        pricesTitle.getStyle().set("margin", "0");
        pricesTitleLayout.add(priceIcon, pricesTitle);
        pricesTitleLayout.getStyle().set("margin", "1rem 0 0.5rem 0");

        FormLayout pricesLayout = new FormLayout();
        pricesLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2));
        pricesLayout.add(priceNormal, priceVip, priceMoreThan15Days, priceMonthly);

        // Notas
        formLayout.add(notes, 2);
        formLayout.setColspan(notes, 2);

        // Binding
        setupBinding();

        VerticalLayout layout = new VerticalLayout(formLayout, pricesTitleLayout, pricesLayout);
        layout.setPadding(false);
        layout.setSpacing(true);

        return layout;
    }

    /**
     * Abre el diálogo para crear una nueva marca
     */
    private void openCreateBrandDialog() {
        Dialog brandDialog = new Dialog();
        brandDialog.setModal(true);
        brandDialog.setWidth("400px");
        brandDialog.setHeaderTitle("Nueva Marca");

        TextField brandNameField = new TextField("Nombre de la Marca");
        brandNameField.setWidthFull();
        brandNameField.setRequired(true);
        brandNameField.setPlaceholder("Ej: Toyota, Honda, Kia...");
        brandNameField.setAutofocus(true);

        FormLayout dialogForm = new FormLayout(brandNameField);
        dialogForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Guardar", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(ev -> {
            String name = brandNameField.getValue();
            if (name == null || name.isBlank()) {
                brandNameField.setInvalid(true);
                brandNameField.setErrorMessage("El nombre es requerido");
                return;
            }

            try {
                // Crear la marca en el catálogo
                Long newBrandId = catalogService.createBrand(name.trim(), null, null);
                
                // Recargar catálogos
                loadCatalogs();
                brandCombo.setItems(allBrands);
                
                // Seleccionar la nueva marca
                VehicleBrandDTO newBrand = allBrands.stream()
                        .filter(b -> b.getId().equals(newBrandId))
                        .findFirst()
                        .orElse(null);
                
                if (newBrand != null) {
                    brandCombo.setValue(newBrand);
                }
                
                showSuccessNotification("Marca '" + name + "' creada exitosamente");
                brandDialog.close();
            } catch (Exception ex) {
                showErrorNotification("Error al crear marca: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelBtn.addClickListener(ev -> brandDialog.close());

        HorizontalLayout dialogButtons = new HorizontalLayout(saveBtn, cancelBtn);
        dialogButtons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout dialogContent = new VerticalLayout(dialogForm, dialogButtons);
        dialogContent.setPadding(true);
        dialogContent.setSpacing(true);

        brandDialog.add(dialogContent);
        brandDialog.open();
    }

    /**
     * Abre el diálogo para crear un nuevo modelo
     */
    private void openCreateModelDialog() {
        VehicleBrandDTO selectedBrand = brandCombo.getValue();
        
        if (selectedBrand == null || selectedBrand.getId() <= 0) {
            showErrorNotification("Primero seleccione o cree una marca del catálogo");
            return;
        }

        Dialog modelDialog = new Dialog();
        modelDialog.setModal(true);
        modelDialog.setWidth("500px");
        modelDialog.setHeaderTitle("Nuevo Modelo para " + selectedBrand.getName());

        TextField modelNameField = new TextField("Nombre del Modelo");
        modelNameField.setWidthFull();
        modelNameField.setRequired(true);
        modelNameField.setPlaceholder("Ej: Corolla, Civic, Rio...");
        modelNameField.setAutofocus(true);

        ComboBox<VehicleType> modelTypeCombo = new ComboBox<>("Tipo de Vehículo");
        modelTypeCombo.setItems(VehicleType.values());
        modelTypeCombo.setItemLabelGenerator(VehicleType::getLabel);
        modelTypeCombo.setWidthFull();
        modelTypeCombo.setRequired(true);

        IntegerField yearStartField = new IntegerField("Año Inicio (opcional)");
        yearStartField.setMin(1900);
        yearStartField.setMax(LocalDateTime.now().getYear() + 1);
        yearStartField.setStepButtonsVisible(true);
        yearStartField.setHelperText("Año en que comenzó a fabricarse");

        IntegerField yearEndField = new IntegerField("Año Fin (opcional)");
        yearEndField.setMin(1900);
        yearEndField.setMax(LocalDateTime.now().getYear() + 5);
        yearEndField.setStepButtonsVisible(true);
        yearEndField.setHelperText("Dejar vacío si aún se fabrica");

        FormLayout dialogForm = new FormLayout(modelNameField, modelTypeCombo, yearStartField, yearEndField);
        dialogForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("300px", 2));

        Button saveBtn = new Button("Guardar", VaadinIcon.CHECK.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(ev -> {
            String name = modelNameField.getValue();
            VehicleType type = modelTypeCombo.getValue();
            
            if (name == null || name.isBlank()) {
                modelNameField.setInvalid(true);
                modelNameField.setErrorMessage("El nombre es requerido");
                return;
            }
            
            if (type == null) {
                modelTypeCombo.setInvalid(true);
                modelTypeCombo.setErrorMessage("El tipo es requerido");
                return;
            }

            try {
                // Crear el modelo en el catálogo
                Long newModelId = catalogService.createModel(
                        selectedBrand.getId(),
                        name.trim(),
                        type,
                        yearStartField.getValue(),
                        yearEndField.getValue());
                
                // Recargar catálogos
                loadCatalogs();
                
                // Actualizar combo de modelos
                updateModelCombo(selectedBrand);
                
                // Seleccionar el nuevo modelo
                VehicleModelDTO newModel = allModels.stream()
                        .filter(m -> m.getId().equals(newModelId))
                        .findFirst()
                        .orElse(null);
                
                if (newModel != null) {
                    // Agregar a la lista del combo si no está
                    List<VehicleModelDTO> currentItems = new ArrayList<>(modelCombo.getListDataView().getItems().toList());
                    if (currentItems.stream().noneMatch(m -> m.getId().equals(newModelId))) {
                        currentItems.add(0, newModel);
                        modelCombo.setItems(currentItems);
                    }
                    modelCombo.setValue(newModel);
                    
                    // Auto-seleccionar el tipo de vehículo
                    vehicleTypeCombo.setValue(type);
                }
                
                showSuccessNotification("Modelo '" + name + "' creado exitosamente");
                modelDialog.close();
            } catch (Exception ex) {
                showErrorNotification("Error al crear modelo: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelBtn.addClickListener(ev -> modelDialog.close());

        HorizontalLayout dialogButtons = new HorizontalLayout(saveBtn, cancelBtn);
        dialogButtons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout dialogContent = new VerticalLayout(dialogForm, dialogButtons);
        dialogContent.setPadding(true);
        dialogContent.setSpacing(true);

        modelDialog.add(dialogContent);
        modelDialog.open();
    }

    /**
     * Actualiza el combo de modelos según la marca seleccionada
     */
    private void updateModelCombo(VehicleBrandDTO selectedBrand) {
        if (selectedBrand == null) {
            modelCombo.setItems(List.of());
            modelCombo.setEnabled(false);
            modelCombo.setPlaceholder("Primero seleccione una marca...");
            return;
        }

        modelCombo.setEnabled(true);
        modelCombo.setPlaceholder("Buscar o seleccionar modelo...");

        if (selectedBrand.getId() > 0) {
            // Marca del catálogo - filtrar modelos
            List<VehicleModelDTO> filteredModels = allModels.stream()
                    .filter(m -> m.getBrandId().equals(selectedBrand.getId()))
                    .toList();
            modelCombo.setItems(filteredModels);
        } else {
            // Marca personalizada - mostrar lista vacía para permitir escribir
            modelCombo.setItems(List.of());
        }
        
        modelCombo.clear();
    }

    private void setupBinding() {
        binder.forField(licensePlate).bind("licensePlate");
        
        // Binding para marca (convertir ComboBox a String)
        binder.forField(brandCombo)
                .withConverter(
                        brand -> brand != null ? brand.getName() : null,
                        name -> {
                            if (name == null) return null;
                            return allBrands.stream()
                                    .filter(b -> b.getName().equalsIgnoreCase(name))
                                    .findFirst()
                                    .orElse(VehicleBrandDTO.builder().id(-1L).name(name).build());
                        })
                .bind("brand");
        
        // Binding para modelo (convertir ComboBox a String)
        binder.forField(modelCombo)
                .withConverter(
                        model -> model != null ? model.getName() : null,
                        name -> {
                            if (name == null) return null;
                            return allModels.stream()
                                    .filter(m -> m.getName().equalsIgnoreCase(name))
                                    .findFirst()
                                    .orElse(VehicleModelDTO.builder().id(-1L).name(name).build());
                        })
                .bind("model");
        
        // Binding para tipo de vehículo
        binder.forField(vehicleTypeCombo)
                .withConverter(
                        type -> type != null ? type.name() : null,
                        name -> name != null ? VehicleType.valueOf(name) : null)
                .bind("vehicleType");
        
        binder.forField(year).bind("year");
        binder.forField(color).bind("color");
        binder.forField(transmissionType)
                .withConverter(
                        type -> type != null ? type.name() : null,
                        name -> name != null ? TransmissionType.valueOf(name) : null)
                .bind("transmissionType");
        binder.forField(fuelType)
                .withConverter(
                        type -> type != null ? type.name() : null,
                        name -> name != null ? FuelType.valueOf(name) : null)
                .bind("fuelType");
        binder.forField(passengerCapacity).bind("passengerCapacity");
        binder.forField(mileage).bind("mileage");
        binder.forField(priceNormal)
                .withConverter(
                        value -> value != null ? BigDecimal.valueOf(value) : null,
                        value -> value != null ? value.doubleValue() : null)
                .bind("priceNormal");
        binder.forField(priceVip)
                .withConverter(
                        value -> value != null ? BigDecimal.valueOf(value) : null,
                        value -> value != null ? value.doubleValue() : null)
                .bind("priceVip");
        binder.forField(priceMoreThan15Days)
                .withConverter(
                        value -> value != null ? BigDecimal.valueOf(value) : null,
                        value -> value != null ? value.doubleValue() : null)
                .bind("priceMoreThan15Days");
        binder.forField(priceMonthly)
                .withConverter(
                        value -> value != null ? BigDecimal.valueOf(value) : null,
                        value -> value != null ? value.doubleValue() : null)
                .bind("priceMonthly");
        binder.forField(notes).bind("notes");

        binder.readBean(command);
    }

    private void configurePhotoPanel(PhotoUploadPanel panel, PhotoType photoType) {
        panel.setOnPhotoAdded(pendingUpload -> {
            if (isEdit && vehicleToEdit != null) {
                try {
                    vehiclePhotoService.uploadPhoto(
                            vehicleToEdit.getId(),
                            pendingUpload.getInputStream(),
                            pendingUpload.getFileName(),
                            pendingUpload.getMimeType(),
                            photoType);
                    showSuccessNotification("Foto subida exitosamente");
                    updateTabCounter(photoType);
                } catch (Exception e) {
                    log.error("Error subiendo foto: {}", e.getMessage(), e);
                    showErrorNotification("Error al subir foto: " + e.getMessage());
                }
            }
        });

        panel.setOnPhotoDeleted(previewItem -> {
            if (previewItem.getPhotoId() != null) {
                try {
                    vehiclePhotoService.deletePhoto(previewItem.getPhotoId());
                    showSuccessNotification("Foto eliminada");
                    updateTabCounter(photoType);
                } catch (Exception e) {
                    log.error("Error eliminando foto: {}", e.getMessage(), e);
                    showErrorNotification("Error al eliminar foto");
                }
            }
        });

        panel.setOnPrimaryChanged(previewItem -> {
            if (previewItem.getPhotoId() != null) {
                try {
                    vehiclePhotoService.markAsPrimary(previewItem.getPhotoId());
                    showSuccessNotification("Foto marcada como principal");
                } catch (Exception e) {
                    log.error("Error marcando foto como principal: {}", e.getMessage(), e);
                    showErrorNotification("Error al marcar foto como principal");
                }
            }
        });
    }

    private void loadExistingPhotos(Long vehicleId) {
        List<VehiclePhoto> exteriorPhotos = vehiclePhotoService.getPhotosByType(vehicleId, PhotoType.EXTERIOR);
        for (VehiclePhoto photo : exteriorPhotos) {
            exteriorPhotosPanel.addExistingPhoto(photo.getId(), photo.getPhotoUrl(), photo.getIsPrimary());
        }

        List<VehiclePhoto> interiorPhotos = vehiclePhotoService.getPhotosByType(vehicleId, PhotoType.INTERIOR);
        for (VehiclePhoto photo : interiorPhotos) {
            interiorPhotosPanel.addExistingPhoto(photo.getId(), photo.getPhotoUrl(), photo.getIsPrimary());
        }

        updateTabCounter(PhotoType.EXTERIOR);
        updateTabCounter(PhotoType.INTERIOR);
    }

    private NumberField createPriceField(String label) {
        NumberField field = new NumberField(label);
        field.setPrefixComponent(new Span("$"));
        field.setMin(0.01);
        field.setStep(0.01);
        field.setRequired(true);
        return field;
    }

    private void configureButtons() {
        saveButton = new Button("Guardar", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> save());

        cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> close());

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setWidthFull();
        buttons.getStyle().set("padding", "1rem");

        getFooter().add(buttons);
    }

    private void populateForm(VehicleDTO vehicle) {
        licensePlate.setValue(vehicle.getLicensePlate());
        licensePlate.setReadOnly(true);

        // Buscar marca en el catálogo o crear una temporal
        VehicleBrandDTO brandDTO = allBrands.stream()
                .filter(b -> b.getName().equalsIgnoreCase(vehicle.getBrand()))
                .findFirst()
                .orElse(VehicleBrandDTO.builder().id(-1L).name(vehicle.getBrand()).build());
        
        // Si es marca personalizada, agregarla a la lista
        if (brandDTO.getId() == -1L) {
            allBrands.add(0, brandDTO);
            brandCombo.setItems(allBrands);
        }
        brandCombo.setValue(brandDTO);

        // Buscar modelo o crear uno temporal
        VehicleModelDTO modelDTO = allModels.stream()
                .filter(m -> m.getName().equalsIgnoreCase(vehicle.getModel()))
                .findFirst()
                .orElse(VehicleModelDTO.builder().id(-1L).name(vehicle.getModel()).build());
        
        // Actualizar combo de modelos y establecer valor
        updateModelCombo(brandDTO);
        if (modelDTO.getId() == -1L) {
            List<VehicleModelDTO> currentModels = new ArrayList<>(modelCombo.getListDataView().getItems().toList());
            currentModels.add(0, modelDTO);
            modelCombo.setItems(currentModels);
        }
        modelCombo.setValue(modelDTO);

        // Tipo de vehículo
        if (vehicle.getVehicleType() != null) {
            try {
                vehicleTypeCombo.setValue(VehicleType.valueOf(vehicle.getVehicleType()));
            } catch (Exception e) {
                vehicleTypeCombo.setValue(VehicleType.SEDAN);
            }
        }

        year.setValue(vehicle.getYear());
        if (vehicle.getColor() != null) color.setValue(vehicle.getColor());
        transmissionType.setValue(TransmissionType.valueOf(vehicle.getTransmissionType()));
        fuelType.setValue(FuelType.valueOf(vehicle.getFuelType()));
        passengerCapacity.setValue(vehicle.getPassengerCapacity());
        mileage.setValue(vehicle.getMileage());

        statusField.setValue(VehicleStatus.valueOf(vehicle.getStatus()));

        priceNormal.setValue(vehicle.getPriceNormal().doubleValue());
        priceVip.setValue(vehicle.getPriceVip().doubleValue());
        priceMoreThan15Days.setValue(vehicle.getPriceMoreThan15Days().doubleValue());
        priceMonthly.setValue(vehicle.getPriceMonthly().doubleValue());

        if (vehicle.getNotes() != null) notes.setValue(vehicle.getNotes());
    }

    private void save() {
        try {
            binder.writeBean(command);

            if (isEdit) {
                vehicleService.updateVehicle(vehicleToEdit.getId(), command);

                VehicleStatus newStatus = statusField.getValue();
                if (newStatus != null) {
                    vehicleService.updateVehicleStatus(vehicleToEdit.getId(), newStatus);
                }

                uploadPendingPhotos(vehicleToEdit.getId());

                fireEvent(new SaveEvent(this));
                showSuccessNotification("Vehículo actualizado exitosamente");
                close();
            } else {
                Long vehicleId = vehicleService.createVehicle(command);
                uploadPendingPhotos(vehicleId);

                fireEvent(new SaveEvent(this));
                showSuccessNotification("Vehículo creado exitosamente");
                close();
            }
        } catch (ValidationException e) {
            showErrorNotification("Por favor corrige los errores en el formulario");
        } catch (Exception e) {
            log.error("Error al guardar vehículo: {}", e.getMessage(), e);
            showErrorNotification("Error al guardar: " + e.getMessage());
        }
    }

    private void uploadPendingPhotos(Long vehicleId) {
        for (PhotoUploadPanel.PendingUpload upload : exteriorPhotosPanel.getPendingUploads()) {
            try {
                vehiclePhotoService.uploadPhoto(
                        vehicleId,
                        upload.getInputStream(),
                        upload.getFileName(),
                        upload.getMimeType(),
                        PhotoType.EXTERIOR);
            } catch (Exception e) {
                log.error("Error subiendo foto exterior: {}", e.getMessage(), e);
            }
        }

        for (PhotoUploadPanel.PendingUpload upload : interiorPhotosPanel.getPendingUploads()) {
            try {
                vehiclePhotoService.uploadPhoto(
                        vehicleId,
                        upload.getInputStream(),
                        upload.getFileName(),
                        upload.getMimeType(),
                        PhotoType.INTERIOR);
            } catch (Exception e) {
                log.error("Error subiendo foto interior: {}", e.getMessage(), e);
            }
        }
    }

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void updateTabCounter(PhotoType photoType) {
        if (vehicleToEdit == null) return;

        int count;
        HorizontalLayout tabContent = new HorizontalLayout();
        tabContent.setAlignItems(FlexComponent.Alignment.CENTER);
        tabContent.setSpacing(true);

        if (photoType == PhotoType.EXTERIOR) {
            count = exteriorPhotosPanel.getPreviewItems().size();
            Icon icon = VaadinIcon.CAR.create();
            icon.setSize("16px");
            tabContent.add(icon);
            tabContent.add(new Span(String.format("Exteriores (%d/10)", count)));
            exteriorTab.removeAll();
            exteriorTab.add(tabContent);
        } else {
            count = interiorPhotosPanel.getPreviewItems().size();
            Icon icon = VaadinIcon.WORKPLACE.create();
            icon.setSize("16px");
            tabContent.add(icon);
            tabContent.add(new Span(String.format("Interiores (%d/10)", count)));
            interiorTab.removeAll();
            interiorTab.add(tabContent);
        }
    }

    // Eventos
    public static class SaveEvent extends ComponentEvent<VehicleFormDialog> {
        public SaveEvent(VehicleFormDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
}
