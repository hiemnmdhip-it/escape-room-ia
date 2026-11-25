package com.escaperoom.ia.model;

public enum Idioma {
    ES, EN;

    public String texto(String espanol, String ingles) {
        return this == ES ? espanol : ingles;
    }
}
