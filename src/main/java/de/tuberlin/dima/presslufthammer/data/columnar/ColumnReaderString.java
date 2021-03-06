package de.tuberlin.dima.presslufthammer.data.columnar;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.tuberlin.dima.presslufthammer.data.SchemaNode;

/**
 * Column reader implementation for String columns.
 * 
 * @author Aljoscha Krettek
 * 
 */
public final class ColumnReaderString extends ColumnReader {
    String currentValue = null;

    /**
     * {@inheritDoc}
     */
    public ColumnReaderString(SchemaNode schema, DataInputStream inputStream)
            throws IOException {
        super(schema, inputStream);
    }

    /**
     * {@inheritDoc}
     */
    public ColumnReaderString(SchemaNode schema, InputStream inputStream)
            throws IOException {
        super(schema, inputStream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void advance() throws IOException {
        if (hasNext()) {
            if (nextDefinition >= schema.getMaxDefinition()) {
                // not NULL
                currentValue = in.readUTF();
            }
            advanceLevels();
        } else {
            throw new RuntimeException(
                    "Has no next value, advance should not have been called.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString() {
        if (isNull()) {
            throw new RuntimeException(
                    "Current value is NULL, getDouble should not have been called.");
        }

        return currentValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToColumn(ColumnWriter writer) throws IOException {
        if (isNull()) {
            writer.writeNull(currentWriteRepetition, currentDefinition);
        } else {
            writer.writeString(currentValue, currentWriteRepetition,
                    currentDefinition);
        }
        currentWriteRepetition = nextRepetition;
    }
}
