package com.rentacaresv.shared.ui;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.customfield.CustomField;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

@Tag("modern-date-range-picker")
@JsModule("./components/modern-date-range-picker.js")
public class ModernDateRangePicker extends CustomField<DateRange> {

    private DateRange currentValue;

    public ModernDateRangePicker(String label) {
        setLabel(label);

        getElement().addEventListener("date-selected", e -> {
            JsonNode data = e.getEventData();
            String start = data.has("event.detail.startDate") && !data.get("event.detail.startDate").isNull()
                    ? data.get("event.detail.startDate").asText()
                    : null;
            String end = data.has("event.detail.endDate") && !data.get("event.detail.endDate").isNull()
                    ? data.get("event.detail.endDate").asText()
                    : null;

            if (start != null && !start.equals("null") && end != null && !end.equals("null")) {
                DateRange newValue = new DateRange(LocalDate.parse(start), LocalDate.parse(end));
                setModelValue(newValue, true);
                this.currentValue = newValue;
            } else {
                setModelValue(null, true);
                this.currentValue = null;
            }
        }).addEventData("event.detail.startDate").addEventData("event.detail.endDate");
    }

    @Override
    protected DateRange generateModelValue() {
        return this.currentValue;
    }

    @Override
    protected void setPresentationValue(DateRange newPresentationValue) {
        if (newPresentationValue != null && newPresentationValue.getStartDate() != null
                && newPresentationValue.getEndDate() != null) {
            getElement().callJsFunction("setDateRange",
                    newPresentationValue.getStartDate().toString(),
                    newPresentationValue.getEndDate().toString());
        } else {
            getElement().callJsFunction("setDateRange", null, null);
        }
    }

    public void setDisabledDates(List<LocalDate> dates) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode array = mapper.createArrayNode();
        for (LocalDate date : dates) {
            array.add(date.toString());
        }
        getElement().executeJs("this.disabledDates = $0;", array);
    }
}
