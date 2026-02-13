package com.bizmetry.registry.dto.agent;

import java.util.List;

public class AgentDiscoverRequest {

    private Sorting sorting;
    private Searching searching;
    private List<Filtering> filtering;

    // Getter y Setter para Sorting
    public Sorting getSorting() {
        return sorting;
    }

    public void setSorting(Sorting sorting) {
        this.sorting = sorting;
    }

    // Getter y Setter para Searching
    public Searching getSearching() {
        return searching;
    }

    public void setSearching(Searching searching) {
        this.searching = searching;
    }

    // Getter y Setter para Filtering
    public List<Filtering> getFiltering() {
        return filtering;
    }

    public void setFiltering(List<Filtering> filtering) {
        this.filtering = filtering;
    }

    // Método Builder para crear una instancia de AgentDiscoverRequest
    public static Builder builder() {
        return new Builder();
    }

    // Clase Builder
    public static class Builder {

        private Sorting sorting;
        private Searching searching;
        private List<Filtering> filtering;

        public Builder sorting(Sorting sorting) {
            this.sorting = sorting;
            return this;
        }

        public Builder searching(Searching searching) {
            this.searching = searching;
            return this;
        }

        public Builder filtering(List<Filtering> filtering) {
            this.filtering = filtering;
            return this;
        }

        public AgentDiscoverRequest build() {
            AgentDiscoverRequest request = new AgentDiscoverRequest();
            request.setSorting(this.sorting);
            request.setSearching(this.searching);
            request.setFiltering(this.filtering);
            return request;
        }
    }

    // Clase Sorting
    public static class Sorting {
        private String sortDirection; // ASC o DESC
        private String sortField; // "name" o "timestamp"

        // Getter y Setter para sortDirection
        public String getSortDirection() {
            return sortDirection;
        }

        public void setSortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
        }

        // Getter y Setter para sortField
        public String getSortField() {
            return sortField;
        }

        public void setSortField(String sortField) {
            this.sortField = sortField;
        }

        // Método Builder para Sorting
        public static Builder builder() {
            return new Builder();
        }

        // Clase Builder para Sorting
        public static class Builder {

            private String sortDirection;
            private String sortField;

            public Builder sortDirection(String sortDirection) {
                this.sortDirection = sortDirection;
                return this;
            }

            public Builder sortField(String sortField) {
                this.sortField = sortField;
                return this;
            }

            public Sorting build() {
                Sorting sorting = new Sorting();
                sorting.setSortDirection(this.sortDirection);
                sorting.setSortField(this.sortField);
                return sorting;
            }
        }
    }

    // Clase Searching
    public static class Searching {
        private List<String> terms;
        private String type; // "exact_match" o "partial_match"
        private boolean sensitive; // true o false

        // Getter y Setter para terms
        public List<String> getTerms() {
            return terms;
        }

        public void setTerms(List<String> terms) {
            this.terms = terms;
        }

        // Getter y Setter para type
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        // Getter y Setter para sensitive
        public boolean isSensitive() {
            return sensitive;
        }

        public void setSensitive(boolean sensitive) {
            this.sensitive = sensitive;
        }

        // Método Builder para Searching
        public static Builder builder() {
            return new Builder();
        }

        // Clase Builder para Searching
        public static class Builder {

            private List<String> terms;
            private String type;
            private boolean sensitive;

            public Builder terms(List<String> terms) {
                this.terms = terms;
                return this;
            }

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public Builder sensitive(boolean sensitive) {
                this.sensitive = sensitive;
                return this;
            }

            public Searching build() {
                Searching searching = new Searching();
                searching.setTerms(this.terms);
                searching.setType(this.type);
                searching.setSensitive(this.sensitive);
                return searching;
            }
        }
    }

    // Clase Filtering
    public static class Filtering {
        private String field; // "name", "model", etc.
        private List<String> values; // Valores a filtrar

        // Getter y Setter para field
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        // Getter y Setter para values
        public List<String> getValues() {
            return values;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }

        // Método Builder para Filtering
        public static Builder builder() {
            return new Builder();
        }

        // Clase Builder para Filtering
        public static class Builder {

            private String field;
            private List<String> values;

            public Builder field(String field) {
                this.field = field;
                return this;
            }

            public Builder values(List<String> values) {
                this.values = values;
                return this;
            }

            public Filtering build() {
                Filtering filtering = new Filtering();
                filtering.setField(this.field);
                filtering.setValues(this.values);
                return filtering;
            }
        }
    }
}
