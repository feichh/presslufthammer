package de.tuberlin.dima.presslufthammer.qexec.grouping;

import java.io.IOException;

import de.tuberlin.dima.presslufthammer.data.columnar.ColumnReader;
import de.tuberlin.dima.presslufthammer.data.columnar.ColumnWriter;

public class StringSumGroupField extends GroupField {
    String value;
    int repetition = -1;
    int definition = -1;
    boolean isNull = true;

    public StringSumGroupField(ColumnWriter writer) {
        super(writer);
    }

    @Override
    public void addValue(ColumnReader reader) {
        if (reader.isNull()) {
            // ignore
        } else {
            isNull = false;
            value += " - " + reader.getString();
        }
        if (repetition == -1) {
            repetition = reader.getCurrentRepetition();
        }
        if (reader.getCurrentDefinition() > definition) {
            definition = reader.getCurrentDefinition();
        }
    }

    @Override
    public void emit() throws IOException {
        if (repetition == -1) {
            return;
        }
        if (isNull) {
            writer.writeNull(repetition, definition);
        } else {
            writer.writeString(value, repetition, definition);
        }
    }

}
