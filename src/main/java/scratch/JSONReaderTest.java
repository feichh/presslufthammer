package scratch;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.common.io.Resources;

import de.tuberlin.dima.presslufthammer.data.FieldStriper;
import de.tuberlin.dima.presslufthammer.data.PrimitiveType;
import de.tuberlin.dima.presslufthammer.data.SchemaNode;
import de.tuberlin.dima.presslufthammer.data.columnar.inmemory.InMemoryWriteonlyTablet;
import de.tuberlin.dima.presslufthammer.data.hierarchical.json.JSONRecordFile;

public class JSONReaderTest {

    public static void main(String[] args) throws FileNotFoundException,
            IOException {
        SchemaNode predicate = SchemaNode.createRecord("predicate");
        predicate.addField(SchemaNode.createPrimitive("lemma",
                PrimitiveType.STRING));
        predicate.addField(SchemaNode.createPrimitive("text",
                PrimitiveType.STRING));

        SchemaNode arguments = SchemaNode.createRecord("arguments");
        arguments.addField(SchemaNode.createPrimitive("text",
                PrimitiveType.STRING));
        arguments.addField(SchemaNode.createPrimitive("role",
                PrimitiveType.STRING));
        arguments.setRepeated();
        predicate.addField(arguments);

        SchemaNode penises = SchemaNode.createPrimitive("numbers",
                PrimitiveType.INT64);
        penises.setRepeated();

        SchemaNode schemaRoot = SchemaNode.createRecord("PredicateOuter");
        schemaRoot.addField(penises);
        schemaRoot.addField(predicate);

        System.out.println(predicate.toString());
        JSONRecordFile records = new JSONRecordFile(schemaRoot,
                Resources.getResource("sentences-reducedPunctuation-json-1-2").getFile());
        FieldStriper striper = new FieldStriper(schemaRoot);
        striper.dissectRecords(records.recordIterator(), new InMemoryWriteonlyTablet(schemaRoot), -1);
    }
}
