import { LitElement, html, css } from 'lit';

/**
 * ModernDateRangePicker - A custom inline HTML calendar component.
 * Replaces external libraries to avoid Shadow DOM stacking issues in Vaadin.
 */
class ModernDateRangePicker extends LitElement {
    static get properties() {
        return {
            startDate: { type: String },
            endDate: { type: String },
            disabledDates: { type: Array },
            currentMonth: { type: Object }, // Date object for the first month displayed
        };
    }

    constructor() {
        super();
        this.startDate = null;
        this.endDate = null;
        this.disabledDates = [];
        this.currentMonth = new Date();
        this.currentMonth.setDate(1);
    }

    static get styles() {
        return css`
      :host {
        display: block;
        font-family: var(--lumo-font-family);
        background: var(--lumo-base-color);
        border: 1px solid var(--lumo-contrast-20pct);
        border-radius: var(--lumo-border-radius-l);
        padding: 1rem;
        user-select: none;
      }

      .calendar-container {
        display: flex;
        gap: 2rem;
        flex-wrap: wrap;
        justify-content: center;
      }

      .month {
        flex: 1;
        min-width: 280px;
        max-width: 320px;
      }

      .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
        font-weight: bold;
        color: var(--lumo-header-text-color);
      }

      .nav-btn {
        cursor: pointer;
        padding: 0.5rem;
        border-radius: 50%;
        transition: background 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .nav-btn:hover {
        background: var(--lumo-contrast-10pct);
      }

      .days-grid {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: 2px;
      }

      .weekday {
        font-size: 0.8rem;
        color: var(--lumo-secondary-text-color);
        text-align: center;
        padding-bottom: 0.5rem;
      }

      .day {
        aspect-ratio: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        font-size: 0.9rem;
        border-radius: var(--lumo-border-radius-m);
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        position: relative;
        z-index: 1;
      }

      .day:not(.empty):not(.disabled):hover {
        background: var(--lumo-primary-color-10pct);
      }

      .day.disabled {
        color: var(--lumo-disabled-text-color);
        cursor: not-allowed;
        background: var(--lumo-contrast-5pct);
        text-decoration: line-through;
      }

      .day.today {
        font-weight: bold;
        color: var(--lumo-primary-text-color);
        box-shadow: inset 0 0 0 2px var(--lumo-primary-color-50pct);
      }

      .day.selected {
        background: var(--lumo-primary-color) !important;
        color: var(--lumo-primary-contrast-color);
        border-radius: 50%;
      }

      .day.in-range {
        background: var(--lumo-primary-color-10pct);
        border-radius: 0;
      }

      .day.range-start {
        border-top-right-radius: 0;
        border-bottom-right-radius: 0;
      }

      .day.range-end {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
      }

      .footer-info {
        margin-top: 1rem;
        padding-top: 1rem;
        border-top: 1px solid var(--lumo-contrast-10pct);
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.85rem;
      }

      .legend {
        display: flex;
        gap: 1rem;
      }

      .legend-item {
        display: flex;
        align-items: center;
        gap: 0.3rem;
      }

      .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
      }

      .dot.available { background: var(--lumo-contrast-20pct); }
      .dot.selected { background: var(--lumo-primary-color); }
      .dot.disabled { background: var(--lumo-disabled-text-color); }
    `;
    }

    render() {
        const nextMonth = new Date(this.currentMonth);
        nextMonth.setMonth(nextMonth.getMonth() + 1);

        return html`
      <div class="calendar-container">
        ${this.renderMonth(this.currentMonth, true)}
        ${this.renderMonth(nextMonth, false)}
      </div>

      <div class="footer-info">
        <div class="legend">
          <div class="legend-item"><span class="dot available"></span> Disponible</div>
          <div class="legend-item"><span class="dot selected"></span> Seleccionado</div>
          <div class="legend-item"><span class="dot disabled"></span> Ocupado</div>
        </div>
        <div>
          ${this.startDate ? html`Desde: <b>${this.startDate}</b>` : html`Selecciona una fecha`}
          ${this.endDate ? html` hasta: <b>${this.endDate}</b>` : ''}
        </div>
      </div>
    `;
    }

    renderMonth(baseDate, showPrev) {
        const monthName = baseDate.toLocaleString('es-ES', { month: 'long', year: 'numeric' });
        const firstDay = new Date(baseDate.getFullYear(), baseDate.getMonth(), 1).getDay();
        const daysInMonth = new Date(baseDate.getFullYear(), baseDate.getMonth() + 1, 0).getDate();

        // Adjust for Monday start (JS getDay is 0:Sunday, 1:Monday...)
        const offset = (firstDay === 0) ? 6 : firstDay - 1;

        const days = [];
        for (let i = 0; i < offset; i++) days.push(null);
        for (let i = 1; i <= daysInMonth; i++) days.push(new Date(baseDate.getFullYear(), baseDate.getMonth(), i));

        return html`
      <div class="month">
        <div class="header">
          ${showPrev ? html`<div class="nav-btn" @click="${this.prevMonth}">❮</div>` : html`<div></div>`}
          <div class="month-title">${monthName.charAt(0).toUpperCase() + monthName.slice(1)}</div>
          ${!showPrev ? html`<div class="nav-btn" @click="${this.nextMonth}">❯</div>` : html`<div></div>`}
        </div>
        <div class="days-grid">
          ${['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'].map(d => html`<div class="weekday">${d}</div>`)}
          ${days.map(date => date ? this.renderDay(date) : html`<div class="day empty"></div>`)}
        </div>
      </div>
    `;
    }

    renderDay(date) {
        const dateStr = date.toISOString().split('T')[0];
        const isToday = dateStr === new Date().toISOString().split('T')[0];
        const isDisabled = this.disabledDates.includes(dateStr);
        const isSelectedStart = this.startDate === dateStr;
        const isSelectedEnd = this.endDate === dateStr;
        const isSelected = isSelectedStart || isSelectedEnd;
        const inRange = this.startDate && this.endDate && dateStr > this.startDate && dateStr < this.endDate;

        let classes = ['day'];
        if (isToday) classes.push('today');
        if (isDisabled) classes.push('disabled');
        if (isSelected) classes.push('selected');
        if (inRange) classes.push('in-range');
        if (isSelectedStart && this.endDate) classes.push('range-start');
        if (isSelectedEnd && this.startDate) classes.push('range-end');

        return html`
      <div class="${classes.join(' ')}" @click="${() => this.handleDayClick(dateStr, isDisabled)}">
        ${date.getDate()}
      </div>
    `;
    }

    handleDayClick(dateStr, isDisabled) {
        if (isDisabled) return;

        if (!this.startDate || (this.startDate && this.endDate)) {
            this.startDate = dateStr;
            this.endDate = null;
        } else if (dateStr < this.startDate) {
            this.startDate = dateStr;
        } else if (dateStr === this.startDate) {
            this.startDate = null;
        } else {
            this.endDate = dateStr;
            // Notify Vaadin
            this.dispatchEvent(new CustomEvent('date-selected', {
                detail: { startDate: this.startDate, endDate: this.endDate },
                bubbles: true,
                composed: true
            }));
        }
        this.requestUpdate();
    }

    prevMonth(e) {
        if (e) e.stopPropagation();
        const newMonth = new Date(this.currentMonth);
        newMonth.setMonth(newMonth.getMonth() - 1);
        this.currentMonth = newMonth;
        this.requestUpdate();
    }

    nextMonth(e) {
        if (e) e.stopPropagation();
        const newMonth = new Date(this.currentMonth);
        newMonth.setMonth(newMonth.getMonth() + 1);
        this.currentMonth = newMonth;
        this.requestUpdate();
    }

    // Called from Java via getElement().callJsFunction
    setDateRange(start, end) {
        this.startDate = start;
        this.endDate = end;
        if (start) {
            this.currentMonth = new Date(start);
            this.currentMonth.setDate(1);
        }
        this.requestUpdate();
    }
}

customElements.define('modern-date-range-picker', ModernDateRangePicker);
