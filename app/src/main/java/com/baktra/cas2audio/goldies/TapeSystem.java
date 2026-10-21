package com.baktra.cas2audio.goldies;

import java.util.Objects;

public final class TapeSystem {

    private final String name;
    private final String country;
    private final String prefixCode;

    public TapeSystem(String name, String country, String prefixCode) {
        this.name = name;
        this.country = country;
        this.prefixCode = prefixCode;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getPrefixCode() {
        return prefixCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, country, prefixCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TapeSystem that = (TapeSystem) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(country, that.country) &&
                Objects.equals(prefixCode, that.prefixCode);
    }
}