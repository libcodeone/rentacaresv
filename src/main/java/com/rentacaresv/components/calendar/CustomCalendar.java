package com.rentacaresv.components.calendar;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Tag("div")
@CssImport("./css/calendar.css")
public class CustomCalendar extends Div {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es");

    private YearMonth currentMonth;
    private final Select<MonthOption> monthSelect;
    private final Select<Integer> yearSelect;
    private final Div gridWrapper;
    private final Div grid;
    private final List<CalendarEvent> events = new ArrayList<>();
    private Consumer<CalendarEvent> eventClickListener;

    public CustomCalendar() {
        this.currentMonth = YearMonth.now();
        addClassName("calendar-container");

        // Header (Navigation)
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("calendar-header");
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        // Select para Mes
        monthSelect = new Select<>();
        List<MonthOption> monthOptions = new ArrayList<>();
        for (Month month : Month.values()) {
            monthOptions.add(new MonthOption(month));
        }
        monthSelect.setItems(monthOptions);
        monthSelect.setItemLabelGenerator(MonthOption::getLabel);
        monthSelect.setValue(monthOptions.get(currentMonth.getMonthValue() - 1));
        monthSelect.setWidth("140px");
        monthSelect.addClassName("calendar-month-selector");
        monthSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                currentMonth = YearMonth.of(currentMonth.getYear(), e.getValue().getMonth());
                render();
            }
        });

        // Select para Año
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = IntStream.rangeClosed(currentYear - 5, currentYear + 5)
                .boxed()
                .toList();

        yearSelect = new Select<>();
        yearSelect.setItems(years);
        yearSelect.setValue(currentMonth.getYear());
        yearSelect.setWidth("90px");
        yearSelect.addClassName("calendar-year-selector");
        yearSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                currentMonth = YearMonth.of(e.getValue(), currentMonth.getMonth());
                render();
            }
        });

        // Contenedor para los selectores de mes/año
        HorizontalLayout dateSelectors = new HorizontalLayout(monthSelect, yearSelect);
        dateSelectors.setSpacing(true);
        dateSelectors.setAlignItems(FlexComponent.Alignment.CENTER);

        // Botones de navegación
        Button prevBtn = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> prevMonth());
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevBtn.addClassName("calendar-nav-btn");

        Button todayBtn = new Button("Hoy", e -> resetToToday());
        todayBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        todayBtn.addClassName("calendar-today-btn");

        Button nextBtn = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> nextMonth());
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextBtn.addClassName("calendar-nav-btn");

        HorizontalLayout actions = new HorizontalLayout(prevBtn, todayBtn, nextBtn);
        actions.setSpacing(false);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        header.add(dateSelectors, actions);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Grid wrapper para scroll
        gridWrapper = new Div();
        gridWrapper.addClassName("calendar-grid-wrapper");

        // Grid
        grid = new Div();
        grid.addClassName("calendar-grid");

        gridWrapper.add(grid);

        add(header, gridWrapper);
        render();
    }

    private void prevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        updateSelectors();
        render();
    }

    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        updateSelectors();
        render();
    }

    private void resetToToday() {
        currentMonth = YearMonth.now();
        updateSelectors();
        render();
    }

    private void updateSelectors() {
        // Actualizar mes
        for (int i = 0; i < 12; i++) {
            if (Month.values()[i] == currentMonth.getMonth()) {
                monthSelect.setValue(new MonthOption(currentMonth.getMonth()));
                break;
            }
        }
        yearSelect.setValue(currentMonth.getYear());
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events.clear();
        this.events.addAll(events);
        render();
    }

    public void addEventClickListener(Consumer<CalendarEvent> listener) {
        this.eventClickListener = listener;
    }

    private void render() {
        grid.removeAll();

        // Weekday Headers
        String[] days = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };
        for (String day : days) {
            Div header = new Div();
            header.addClassName("calendar-weekday-header");
            header.setText(day);
            grid.add(header);
        }

        // Days
        LocalDate firstDay = currentMonth.atDay(1);
        int dayOfWeek = firstDay.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        LocalDate startDate = firstDay.minusDays(dayOfWeek - 1);

        // 6 weeks * 7 days = 42 cells to ensure full month coverage
        for (int i = 0; i < 42; i++) {
            LocalDate date = startDate.plusDays(i);
            Div dayCell = new Div();
            dayCell.addClassName("calendar-day");

            if (!YearMonth.from(date).equals(currentMonth)) {
                dayCell.addClassName("other-month");
            }
            if (date.equals(LocalDate.now())) {
                dayCell.addClassName("today");
            }

            Span dayNumber = new Span(String.valueOf(date.getDayOfMonth()));
            dayNumber.addClassName("day-number");
            dayCell.add(dayNumber);

            // Contenedor de eventos visibles (máximo 3)
            Div eventsContainer = new Div();
            eventsContainer.addClassName("calendar-events-container");

            // Obtener eventos del día
            List<CalendarEvent> dayEvents = getEventsForDate(date);

            // Mostrar hasta 3 eventos en la celda
            int visibleCount = Math.min(dayEvents.size(), 3);
            for (int j = 0; j < visibleCount; j++) {
                CalendarEvent event = dayEvents.get(j);
                Div eventBadge = createEventBadge(event, true);
                eventsContainer.add(eventBadge);
            }

            // Indicador si hay más eventos
            if (dayEvents.size() > 3) {
                Div moreIndicator = new Div();
                moreIndicator.addClassName("calendar-more-indicator");
                moreIndicator.setText("+" + (dayEvents.size() - 3) + " más");
                eventsContainer.add(moreIndicator);
            }

            dayCell.add(eventsContainer);

            // Crear popup de hover si hay eventos
            if (!dayEvents.isEmpty()) {
                Div hoverPopup = createHoverPopup(date, dayEvents);
                dayCell.add(hoverPopup);
                dayCell.addClassName("has-events");
            }

            grid.add(dayCell);
        }
    }

    private List<CalendarEvent> getEventsForDate(LocalDate date) {
        List<CalendarEvent> dayEvents = new ArrayList<>();
        for (CalendarEvent event : events) {
            if (!date.isBefore(event.getStart()) && date.isBefore(event.getEnd())) {
                dayEvents.add(event);
            }
        }
        return dayEvents;
    }

    private Div createEventBadge(CalendarEvent event, boolean compact) {
        Div eventBadge = new Div();
        eventBadge.addClassName(compact ? "calendar-event" : "calendar-event-full");
        eventBadge.setText(event.getTitle());
        eventBadge.getStyle().set("background-color", event.getColor());

        eventBadge.addClickListener(e -> {
            if (eventClickListener != null) {
                eventClickListener.accept(event);
            }
        });

        return eventBadge;
    }

    private Div createHoverPopup(LocalDate date, List<CalendarEvent> dayEvents) {
        Div popup = new Div();
        popup.addClassName("calendar-hover-popup");

        // Header del popup con la fecha
        Div popupHeader = new Div();
        popupHeader.addClassName("popup-header");
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES);
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
        popupHeader.setText(dayName + " " + date.getDayOfMonth());
        popup.add(popupHeader);

        // Contador de eventos
        Div eventCount = new Div();
        eventCount.addClassName("popup-event-count");
        eventCount.setText(dayEvents.size() + (dayEvents.size() == 1 ? " evento" : " eventos"));
        popup.add(eventCount);

        // Lista de eventos
        Div eventsList = new Div();
        eventsList.addClassName("popup-events-list");

        for (CalendarEvent event : dayEvents) {
            Div eventItem = createEventBadge(event, false);
            eventsList.add(eventItem);
        }

        popup.add(eventsList);

        return popup;
    }

    // Clase auxiliar para los meses con equals/hashCode
    public static class MonthOption {
        private final Month month;
        private final String label;

        public MonthOption(Month month) {
            this.month = month;
            String name = month.getDisplayName(TextStyle.FULL, LOCALE_ES);
            this.label = name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        public Month getMonth() {
            return month;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MonthOption that = (MonthOption) o;
            return month == that.month;
        }

        @Override
        public int hashCode() {
            return month.hashCode();
        }
    }

    public static class CalendarEvent {
        private Long id;
        private String title;
        private LocalDate start;
        private LocalDate end;
        private String color;
        private Object data;

        public CalendarEvent(Long id, String title, LocalDate start, LocalDate end, String color, Object data) {
            this.id = id;
            this.title = title;
            this.start = start;
            this.end = end;
            this.color = color;
            this.data = data;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public String getColor() {
            return color;
        }

        public Object getData() {
            return data;
        }
    }
}
