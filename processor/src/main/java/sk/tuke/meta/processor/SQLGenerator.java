package sk.tuke.meta.processor;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import javax.persistence.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("javax.persistence.Entity")
public class SQLGenerator extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("ANOTACNY PROCESOR RUN");
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Entity.class);
        String fileName = "output.sql";
        String sqlQuery = "";

        for (Element annotatedElement : elements) {
            try {
                CreateTableQueryBuilder queryBuilder = new CreateTableQueryBuilder(
                        annotatedElement,
                        roundEnv
                );
                sqlQuery += queryBuilder.createTableFor(processingEnv);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Cannot generate table: " + e.getMessage());
            }
        }

        try {
            File file = new File(processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", fileName).toUri());

            if (file.exists()) {
                System.out.println("File already exists: " + file.getAbsolutePath());
            } else {
                FileWriter writer = new FileWriter(file);
                writer.write(sqlQuery);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


}
