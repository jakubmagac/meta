package sk.tuke.meta.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.*;
import java.io.IOException;
import java.lang.annotation.Annotation;

public class CreateTableQueryBuilder {
    private final Element table;
    private final RoundEnvironment roundEnv;

    CreateTableQueryBuilder(Element table, RoundEnvironment roundEnv) {
        this.table = table;
        this.roundEnv = roundEnv;
    }

    public String createTableFor(ProcessingEnvironment processingEnv) throws IOException {
        String tableName = this.table.getSimpleName().toString();

        Table tableAnnotation = this.table.getAnnotation(Table.class);
        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        }

        String query = "create table if not exists " + tableName + "(\n";

        Element[] tableElements =
                this.table.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .toArray(Element[]::new);

        for(Element field : tableElements) {
            Annotation transientAnnotation = field.getAnnotation(Transient.class);
            Annotation idAnnotation = field.getAnnotation(Id.class);
            Column columnAnnotation = field.getAnnotation(Column.class);
            Annotation manyToOneAnnotation = field.getAnnotation(ManyToOne.class);
            String columnName = field.getSimpleName().toString();

            // IF FIELD IS TRANSIENT, SKIP IT
            if (transientAnnotation != null) {
                continue;
            }

            // CHANGE COLUMN NAME IF ANNOTATION IS PRESENT
            if(columnAnnotation != null ) {
                if(!columnAnnotation.name().isEmpty()) {
                    columnName = columnAnnotation.name();
                }
            }

            // ID ANNOTATION
            if (idAnnotation != null) {
                query += "\t" + columnName + " INTEGER PRIMARY KEY AUTOINCREMENT";
                continue;
            }

            // MANY TO ONE ANNOTATION
            if (manyToOneAnnotation != null) {
                query += ",\n\t" + columnName + " INTEGER, \n\tFOREIGN KEY ("
                        + columnName + ") REFERENCES ";

                String[] pieces = field.asType().toString().split("\\.");
                String referencedTableName = pieces[pieces.length - 1];
                Element referencedTable = null;

                // GET ALL CLASSES ANNOTATET WITH TABLE
                for(Element element: roundEnv.getElementsAnnotatedWith(Table.class)) {
                    if(field.asType().toString().equals(element.toString())) {
                        referencedTableName = element.getAnnotation(Table.class).name();
                    }
                }

                // SET REFERENCED TABLE
                for(Element element: roundEnv.getElementsAnnotatedWith(Entity.class)) {
                    if(field.asType().toString().equals(element.toString())) {
                        referencedTable = element;
                    }
                }

                query += referencedTableName + "(";

                String idFieldName = null;

                if(referencedTable != null) {
                    for(Element otherField : referencedTable.getEnclosedElements()) {
                        Annotation idReferencedAnnotation = otherField.getAnnotation(Id.class);
                        if(idReferencedAnnotation != null) {
                           columnAnnotation = otherField.getAnnotation(Column.class);
                           columnName = otherField.getSimpleName().toString();

                           if(columnAnnotation != null) {
                                columnName = columnAnnotation.name();
                           }

                           idFieldName = columnName;
                        }
                    }
                }

                if (idFieldName == null) {
                    throw new IllegalArgumentException("No field annotated with @Id found in " + referencedTableName);
                }

                query += idFieldName + ")";
                continue;
            }

            // NOT ANNOTATED FIELDS
            query += ",\n\t" + columnName;

            // ADD type
            TypeMirror fieldType = field.asType();

            if (fieldType.getKind() == TypeKind.INT || fieldType.getKind() == TypeKind.LONG) {
                query += " INTEGER";
            } else if (fieldType.getKind() == TypeKind.FLOAT || fieldType.getKind() == TypeKind.DOUBLE) {
                query += " REAL";
            } else if (fieldType.getKind() == TypeKind.BOOLEAN) {
                query += " INTEGER";
            } else if (fieldType.getKind() == TypeKind.DECLARED) {
                TypeElement fieldTypeElement = (TypeElement) ((DeclaredType) fieldType).asElement();
                String fieldTypeName = fieldTypeElement.getQualifiedName().toString();

                if ("java.lang.String".equals(fieldTypeName)) {
                    query += " TEXT";
                } else if ("java.util.Date".equals(fieldTypeName)) {
                    query += " INTEGER";
                } else {
                    throw new IllegalArgumentException("Unrecognized field type: " + fieldTypeName);
                }
            } else {
                throw new IllegalArgumentException("Unrecognized field type: " + fieldType.getKind());
            }

            // ADD NULL, NOT NULL AND UNIQUE
            if (columnAnnotation != null) {
                if(columnAnnotation.nullable()) {
                    query += " NULL";
                } else {
                    query += " NOT NULL";
                }

                if(columnAnnotation.unique()) {
                    query += " UNIQUE";
                }
            }
        };

        query += "\n);\n";
        return query;
    }
}
