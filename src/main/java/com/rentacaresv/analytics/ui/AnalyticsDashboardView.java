package com.rentacaresv.analytics.ui;

import com.rentacaresv.analytics.application.AnalyticsService;
import com.rentacaresv.analytics.application.AnalyticsService.DashboardStats;
import com.rentacaresv.analytics.application.AnalyticsService.StepStat;
import com.rentacaresv.analytics.application.AnalyticsService.VehicleStat;
import com.rentacaresv.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "analytics", layout = MainLayout.class)
@PageTitle("Estadísticas Web")
@Menu(order = 5, icon = LineAwesomeIconUrl.CHART_BAR_SOLID)
@RolesAllowed("ADMIN")
public class AnalyticsDashboardView extends VerticalLayout {

    private final AnalyticsService analyticsService;

    // Período seleccionado (días hacia atrás desde hoy)
    private int selectedDays = 30;

    // Contenedores que se recargan al cambiar período
    private final Div summaryRow      = new Div();
    private final Div funnelSection   = new Div();
    private final Div vehiclesSection = new Div();

    {
        summaryRow.setWidthFull();
        funnelSection.setWidthFull();
        vehiclesSection.setWidthFull();
    }

    public AnalyticsDashboardView(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(buildHeader());
        add(buildPeriodSelector());
        add(summaryRow);
        add(funnelSection);
        add(vehiclesSection);

        loadData();
    }

    // ─── Header ────────────────────────────────────────────────────────────────

    private H2 buildHeader() {
        H2 title = new H2("📊 Estadísticas Web");
        title.getStyle().set("margin", "0 0 0.5rem 0").set("color", "var(--lumo-primary-text-color)");
        return title;
    }

    // ─── Selector de período ───────────────────────────────────────────────────

    private HorizontalLayout buildPeriodSelector() {
        Button btn7   = periodButton("7 días",  7);
        Button btn30  = periodButton("30 días", 30);
        Button btn90  = periodButton("90 días", 90);
        Button btn365 = periodButton("1 año",  365);

        markActive(btn30);

        btn7.addClickListener(e  -> { selectedDays = 7;   markActive(btn7);   markInactive(btn30, btn90, btn365); loadData(); });
        btn30.addClickListener(e -> { selectedDays = 30;  markActive(btn30);  markInactive(btn7, btn90, btn365);  loadData(); });
        btn90.addClickListener(e -> { selectedDays = 90;  markActive(btn90);  markInactive(btn7, btn30, btn365);  loadData(); });
        btn365.addClickListener(e -> { selectedDays = 365; markActive(btn365); markInactive(btn7, btn30, btn90);  loadData(); });

        HorizontalLayout row = new HorizontalLayout(btn7, btn30, btn90, btn365);
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        return row;
    }

    private Button periodButton(String label, int days) {
        Button btn = new Button(label);
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        return btn;
    }

    private void markActive(Button btn) {
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btn.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
    }

    private void markInactive(Button... btns) {
        for (Button b : btns) {
            b.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }
    }

    // ─── Cargar datos ──────────────────────────────────────────────────────────

    private void loadData() {
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(selectedDays);
        DashboardStats stats = analyticsService.getStats(from, to);

        summaryRow.removeAll();
        funnelSection.removeAll();
        vehiclesSection.removeAll();

        summaryRow.add(buildSummaryCards(stats));
        funnelSection.add(buildFunnel(stats));
        vehiclesSection.add(buildTopVehicles(stats.topVehicles()));
    }

    // ─── Tarjetas resumen ──────────────────────────────────────────────────────

    private HorizontalLayout buildSummaryCards(DashboardStats s) {
        HorizontalLayout row = new HorizontalLayout(
                statCard("👥", "Visitantes únicos",  s.uniqueVisitors(),        "#4dabff"),
                statCard("🚗", "Vistas de vehículos", s.vehicleDetailViews(),    "#a78bfa"),
                statCard("📱", "Clicks WhatsApp",      s.whatsappClicks(),        "#4ade80"),
                statCard("📋", "Clicks Reservar",      s.reserveClicks(),         "#fbbf24"),
                statCard("✅", "Reservas enviadas",    s.reservationsCompleted(), "#34d399")
        );
        row.setWidthFull();
        row.setSpacing(true);
        row.getStyle().set("flex-wrap", "wrap").set("margin-bottom", "1rem");
        return row;
    }

    private Div statCard(String emoji, String label, long value, String color) {
        Div card = new Div();
        card.getStyle()
                .set("background", "rgba(255,255,255,0.04)")
                .set("border", "1px solid rgba(255,255,255,0.08)")
                .set("border-radius", "12px")
                .set("padding", "1.2rem 1.5rem")
                .set("min-width", "160px")
                .set("flex", "1")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.4rem");

        Span ico = new Span(emoji + "  " + label);
        ico.getStyle().set("font-size", "0.8rem").set("color", "var(--lumo-secondary-text-color)");

        Span val = new Span(String.valueOf(value));
        val.getStyle()
                .set("font-size", "2rem")
                .set("font-weight", "700")
                .set("color", color)
                .set("line-height", "1");

        card.add(ico, val);
        return card;
    }

    // ─── Embudo de conversión ──────────────────────────────────────────────────

    private VerticalLayout buildFunnel(DashboardStats s) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();

        H3 title = new H3("Embudo de conversión");
        title.getStyle().set("margin", "1rem 0 0.6rem 0").set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.9rem").set("text-transform", "uppercase").set("letter-spacing", "0.08em");
        section.add(title);

        Div container = new Div();
        container.getStyle()
                .set("background", "rgba(255,255,255,0.03)")
                .set("border", "1px solid rgba(255,255,255,0.07)")
                .set("border-radius", "12px")
                .set("padding", "1rem")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.5rem");

        long baseline = Math.max(s.uniqueVisitors(), 1);

        container.add(funnelRow("👥 Visitantes únicos",   s.uniqueVisitors(),        baseline, "#4dabff"));
        container.add(funnelRow("🚗 Vistas de detalle",    s.vehicleDetailViews(),    baseline, "#a78bfa"));
        container.add(funnelRow("📱 Click WhatsApp",        s.whatsappClicks(),        baseline, "#4ade80"));
        container.add(funnelRow("📋 Click Reservar",        s.reserveClicks(),         baseline, "#fbbf24"));

        // Pasos del wizard
        for (StepStat step : s.reservationFunnel()) {
            container.add(funnelRow("  └ Paso " + step.stepNumber() + ": " + step.stepName(), step.count(), baseline, "#94a3b8"));
        }

        container.add(funnelRow("✅ Reservas completadas", s.reservationsCompleted(), baseline, "#34d399"));

        section.add(container);
        return section;
    }

    private Div funnelRow(String label, long value, long baseline, String color) {
        double pct = baseline > 0 ? (value * 100.0 / baseline) : 0;
        String pctStr = String.format("%.1f%%", pct);

        Div row = new Div();
        row.getStyle().set("display", "flex").set("align-items", "center").set("gap", "0.8rem");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("min-width", "240px")
                .set("font-size", "0.85rem")
                .set("color", "var(--lumo-body-text-color)")
                .set("white-space", "nowrap");

        // Barra
        Div barBg = new Div();
        barBg.getStyle()
                .set("flex", "1")
                .set("background", "rgba(255,255,255,0.06)")
                .set("border-radius", "4px")
                .set("height", "10px")
                .set("overflow", "hidden");

        Div barFill = new Div();
        barFill.getStyle()
                .set("width", pctStr)
                .set("height", "100%")
                .set("background", color)
                .set("border-radius", "4px")
                .set("transition", "width 0.4s ease");
        barBg.add(barFill);

        Span countSpan = new Span(value + "  (" + pctStr + ")");
        countSpan.getStyle()
                .set("min-width", "110px")
                .set("text-align", "right")
                .set("font-size", "0.85rem")
                .set("color", color)
                .set("font-weight", "600");

        row.add(labelSpan, barBg, countSpan);
        return row;
    }

    // ─── Top vehículos ─────────────────────────────────────────────────────────

    private VerticalLayout buildTopVehicles(List<VehicleStat> vehicles) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();

        H3 title = new H3("Top vehículos más vistos");
        title.getStyle().set("margin", "1rem 0 0.6rem 0").set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.9rem").set("text-transform", "uppercase").set("letter-spacing", "0.08em");
        section.add(title);

        if (vehicles.isEmpty()) {
            Span empty = new Span("Sin datos en este período");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "0.85rem");
            section.add(empty);
            return section;
        }

        Grid<VehicleStat> grid = new Grid<>();
        grid.setItems(vehicles);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.getStyle()
                .set("border-radius", "12px")
                .set("overflow", "hidden");

        // Nombre: ocupa todo el espacio sobrante
        grid.addColumn(VehicleStat::vehicleName)
                .setHeader("Vehículo")
                .setFlexGrow(1)
                .setAutoWidth(false)
                .setSortable(true);

        // Columnas numéricas: ancho fijo para que siempre sean legibles
        grid.addColumn(VehicleStat::views)
                .setHeader("Vistas 🚗")
                .setWidth("130px").setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(VehicleStat::waClicks)
                .setHeader("WhatsApp 📱")
                .setWidth("140px").setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(VehicleStat::reserveClicks)
                .setHeader("Reservas 📋")
                .setWidth("140px").setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(v -> {
            if (v.views() == 0) return "—";
            double rate = (v.reserveClicks() * 100.0) / v.views();
            return String.format("%.1f%%", rate);
        })
                .setHeader("Conversión %")
                .setWidth("130px").setFlexGrow(0)
                .setSortable(false);

        section.add(grid);
        return section;
    }
}
