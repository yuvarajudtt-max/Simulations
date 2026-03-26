package com.dtt.simulations.POA.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public  class SelectedClaims {
        @JsonProperty("Document")
        private List<String> document;

        public List<String> getDocument() {
            return document;
        }

        public void setDocument(List<String> document) {
            this.document = document;
        }
    }