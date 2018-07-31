package org.opencds.cqf.builders;

/*
    These builders are based off of work performed by Philips Healthcare.
    I simplified their work with this generic base class and added/expanded builders.

    Tip of the hat to Philips Healthcare developer nly98977
*/

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class BaseBuilder<T> {

    protected T complexProperty;

    public BaseBuilder(T complexProperty) {
        this.complexProperty = complexProperty;
    }

    public T build() {
        return complexProperty;
    }

    Date createDate(String dateString) {
        LocalDate localeDate = LocalDate.parse(dateString);

        return Date.from(localeDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
