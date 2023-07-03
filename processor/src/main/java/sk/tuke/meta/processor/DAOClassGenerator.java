package sk.tuke.meta.processor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.*;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.*;

@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_15)
public class DAOClassGenerator extends AbstractProcessor {
    private static final String TEMPLATE_PATH = "sk/tuke/meta/persistence/" ;
    private VelocityEngine velocity;
    private boolean runOnce = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        velocity = new VelocityEngine();
        velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.apache.velocity.runtime.log.NullLogChute");
        velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocity.setProperty("classpath.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        try {
            velocity.init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize VelocityEngine: " + e.getMessage());
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var entityTypes = roundEnv.getElementsAnnotatedWith(Entity.class);

        if(!this.runOnce) {
            for(var entityType : entityTypes) {
                try {
                    generateDAOClass((TypeElement) entityType, roundEnv);
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR, e.getMessage()
                    );
                }
            }

            try {
                generatePersistenceManager(entityTypes);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR, e.getMessage()
                );
            }
            this.runOnce = true;
        }



        return true;
    }

    private void generatePersistenceManager(Set<?extends Element> entityTypes) throws IOException {
        var jfo = processingEnv.getFiler().createSourceFile("GeneratedPersistenceManager");

        try (Writer writer = jfo.openWriter()) {
            var template = velocity.getTemplate(TEMPLATE_PATH + "GeneratedPersistenceManager.java.vm");
            var context = new VelocityContext();
            context.put("entities", entityTypes);
            context.put("simpleEntities", entityTypes.stream().map(Element::getSimpleName).toArray());
            template.merge(context, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Map<String, String>> getAllTablesWithForeignKeys(RoundEnvironment roundEnv, TypeElement entityType) {
        var entityTypes = roundEnv.getElementsAnnotatedWith(Entity.class);
        List<Map<String, String>> results = new ArrayList<>();

        for(var en : entityTypes) {
            Map<String, String> variable = new HashMap<>();
            Element[] tableElements = en.getEnclosedElements().stream()
                    .filter(element -> element.getKind() == ElementKind.FIELD)
                    .toArray(Element[]::new);

            if(!entityType.asType().toString().equals(en.asType().toString())) {
                for (Element tableElement : tableElements) {
                    Annotation manyToOneAnnotation = tableElement.getAnnotation(ManyToOne.class);
                    if(manyToOneAnnotation != null){
                        System.out.println(tableElement.asType().toString());
                        Column annotation = tableElement.getAnnotation(Column.class);
                        if(annotation != null) {
                            variable.put("columnName", annotation.name());
                        } else {
                            variable.put("columnName", tableElement.getSimpleName().toString());
                        }

                        // get Table name
                        Table tableAnnotation = en.getAnnotation(Table.class);
                        if(tableAnnotation != null){
                            variable.put("table", tableAnnotation.name());
                        } else {
                            String[] pieces = en.asType().toString().split("\\.");
                            String referencedTableName = pieces[pieces.length - 1];
                            variable.put("table", referencedTableName);
                        }

                        results.add(variable);
                    }
                }
            }
        }
        return results;
    }

    private void generateDAOClass(TypeElement entityType, RoundEnvironment roundEnv) throws IOException {
        var jfo = processingEnv.getFiler().createSourceFile(entityType.toString() + "DAO");

        try (Writer writer = jfo.openWriter()) {
            var template = velocity.getTemplate(TEMPLATE_PATH + "DAO.java.vm");
            var context = new VelocityContext();
            context.put("package", entityType.getEnclosingElement().toString());
            context.put("entity", entityType.getSimpleName().toString());
            String tableName = getTableName(entityType);
            context.put("table", tableName);
            List<Map<String, Object>> tableElements = getElements(entityType, roundEnv);
            context.put("variables", tableElements);
            context.put("primaryKey", getIdName(entityType));

            System.out.println(getElementsTypesAndAliases(entityType));
            context.put("elementsAndAliases", getElementsTypesAndAliases(entityType));
            context.put("elementsToSetToNull", getAllTablesWithForeignKeys(roundEnv, entityType));
            template.merge(context, writer);
        }
    }

    private String getTableName(TypeElement entityType) {
        String tableName = entityType.getSimpleName().toString();
        Table tableAnnotation = entityType.getAnnotation(Table.class);

        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        }

        return tableName;
    }

    private String getIdName(TypeElement entityType) {
        Element[] tableElements = entityType.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .toArray(Element[]::new);

        String idName = "";

        for (Element tableElement : tableElements) {
            Annotation idAnnotation = tableElement.getAnnotation(Id.class);

            if (idAnnotation != null) {
                idName = tableElement.getSimpleName().toString();
                break;
            }
        }

        return idName;
    }

    private List<Map<String, String>> getElementsTypesAndAliases(TypeElement entityType) {
        Element[] tableElements = entityType.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .toArray(Element[]::new);

        List<Map<String, String>> variables = new ArrayList<>();

        for (Element tableElement : tableElements) {
            Map<String, String> variable = new HashMap<>();

            Annotation transientAnnotation = tableElement.getAnnotation(Transient.class);
            Annotation idAnnotation = tableElement.getAnnotation(Id.class);
            Column columnAnnotation = tableElement.getAnnotation(Column.class);

            // TRANSIENT
            if(transientAnnotation != null) {
                continue;
            }

            String columnName = tableElement.getSimpleName().toString();

            // ID ANNOTATION
            if (idAnnotation != null) {
                variable.put("element", "Id");
            } else {
                String firstChar = columnName.substring(0, 1).toUpperCase();
                String capitalizedString = firstChar + columnName.substring(1);
                variable.put("element", capitalizedString);
            }

            // CHANGE COLUMN NAME
            if(columnAnnotation != null) {
                columnName = columnAnnotation.name();
            }
            variable.put("alias", columnName);

            // TYPE
            if (tableElement.asType().getKind() == TypeKind.INT || tableElement.asType().getKind() == TypeKind.LONG) {
                variable.put("type", "Integer");
            } else if (tableElement.asType().getKind() == TypeKind.FLOAT || tableElement.asType().getKind() == TypeKind.DOUBLE) {
                variable.put("type", "Float");
            } else if (tableElement.asType().getKind() == TypeKind.BOOLEAN) {
                variable.put("type", "Integer");
            } else if (tableElement.asType().getKind() == TypeKind.DECLARED) {
                if ("java.lang.String".equals(tableElement.asType().toString())) {
                    variable.put("type", "String");
                } else if ("java.util.Date".equals(tableElement.asType().toString())) {
                    variable.put("type", "Integer");
                } else {
                    variable.put("type", tableElement.asType().toString() );
                }
            }

            // add to list
            variables.add(variable);
        }

        return variables;
    }

    private List<Map<String, Object>> getElements(TypeElement entityType, RoundEnvironment roundEnv) {
        Element[] tableElements = entityType.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .toArray(Element[]::new);

        List<Map<String, Object>> variables = new ArrayList<>();

        for (Element tableElement : tableElements) {
            Map<String, Object> variable = new HashMap<>();

            Annotation transientAnnotation = tableElement.getAnnotation(Transient.class);
            Annotation manyToOneAnnotation = tableElement.getAnnotation(ManyToOne.class);
            Annotation idAnnotation = tableElement.getAnnotation(Id.class);
            Column columnAnnotation = tableElement.getAnnotation(Column.class);

            String columnName = tableElement.getSimpleName().toString();

            // TRANSIENT
            if(transientAnnotation != null) {
                continue;
            }

            // CHANGE COLUMN NAME
            if(columnAnnotation != null) {
                columnName = columnAnnotation.name();
            }
            variable.put("name", columnName);

            // ID ANNOTATION
            if (idAnnotation != null) {
                variable.put("type", "INTEGER PRIMARY KEY AUTOINCREMENT");
                variables.add(variable);
                continue;
            }

            // MANY TO ONE ANNOTATION
            if (manyToOneAnnotation != null) {
                String[] pieces = tableElement.asType().toString().split("\\.");
                String referencedTableName = pieces[pieces.length - 1];
                Element referencedTable = null;

                // GET ALL CLASSES ANNOTATED WITH TABLE
                for(Element element: roundEnv.getElementsAnnotatedWith(Table.class)) {
                    if(tableElement.asType().toString().equals(element.toString())) {
                        referencedTableName = element.getAnnotation(Table.class).name();
                    }
                }

                // SET REFERENCED TABLE
                for(Element element: roundEnv.getElementsAnnotatedWith(Entity.class)) {
                    if(tableElement.asType().toString().equals(element.toString())) {
                        referencedTable = element;
                    }
                }

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

                variable.put("element", columnName);
                variable.put("referencedTable", referencedTableName);
                variable.put("referencedTableID", idFieldName);
                variables.add(variable);
                continue;
            }


            // ADD type
            TypeMirror fieldType = tableElement.asType();

            if (fieldType.getKind() == TypeKind.INT || fieldType.getKind() == TypeKind.LONG) {
                variable.put("type", "INTEGER");
            } else if (fieldType.getKind() == TypeKind.FLOAT || fieldType.getKind() == TypeKind.DOUBLE) {
                variable.put("type", "REAL");
            } else if (fieldType.getKind() == TypeKind.BOOLEAN) {
                variable.put("type", "INTEGER");
            } else if (fieldType.getKind() == TypeKind.DECLARED) {
                TypeElement fieldTypeElement = (TypeElement) ((DeclaredType) fieldType).asElement();
                String fieldTypeName = fieldTypeElement.getQualifiedName().toString();

                if ("java.lang.String".equals(fieldTypeName)) {
                    variable.put("type", "TEXT");
                } else if ("java.util.Date".equals(fieldTypeName)) {
                    variable.put("type", "INTEGER");
                } else {
                    throw new IllegalArgumentException("Unrecognized field type: " + fieldTypeName);
                }
            } else {
                throw new IllegalArgumentException("Unrecognized field type: " + fieldType.getKind());
            }

            // ADD NULL, NOT NULL AND UNIQUE
            if (columnAnnotation != null) {
                List<String> additionalAnnotations = new ArrayList<>();

                if(columnAnnotation.nullable()) {
                    additionalAnnotations.add("NULL");
                } else {
                    additionalAnnotations.add("NOT NULL");
                }

                if(columnAnnotation.unique()) {
                    additionalAnnotations.add("UNIQUE");
                }

                variable.put("additionalAnnotations", additionalAnnotations);
            }

            // add to list
            variables.add(variable);
        }

        return variables;
    }
}
