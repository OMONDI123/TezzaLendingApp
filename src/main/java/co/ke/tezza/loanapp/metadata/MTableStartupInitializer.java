package co.ke.tezza.loanapp.metadata;

import org.reflections.Reflections;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import co.ke.tezza.loanapp.entity.MTable;
import co.ke.tezza.loanapp.repository.MTableRepository;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Table;

@Component
public class MTableStartupInitializer implements CommandLineRunner {

    private final MTableRepository mTableRepository;

    public MTableStartupInitializer(MTableRepository mTableRepository) {
        this.mTableRepository = mTableRepository;
    }

    @Override
    public void run(String... args) {
        Reflections reflections = new Reflections("co.ke.tezza"); 
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);

        for (Class<?> entityClass : entityClasses) {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            String tableName = (tableAnnotation != null) ? tableAnnotation.name() : entityClass.getSimpleName();
            String entityName = entityClass.getSimpleName();
            String description = "Auto-detected existing entity: " + entityName;

            boolean exists = mTableRepository.findByEntityName(entityName).isPresent();

            if (!exists) {
                mTableRepository.save(new MTable(tableName, entityName, description));
                System.out.println("Registered entity: " + entityName);
            }
        }
    }
}





//
//package co.ke.tezza.loanapp.metadata;
//
//import org.reflections.Reflections;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import co.ke.tezza.loanapp.entity.MTable;
//import co.ke.tezza.loanapp.enums.*;
//import co.ke.tezza.loanapp.repository.MTableRepository;
//
//import javax.persistence.Entity;
//import javax.persistence.Table;
//import javax.persistence.EntityManager;
//import javax.persistence.PersistenceContext;
//import javax.persistence.Query;
//
//import java.lang.reflect.Field;
//import java.util.Set;
//
//@Component
//public class MTableStartupInitializer implements CommandLineRunner {
//
//    private final MTableRepository mTableRepository;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    public MTableStartupInitializer(MTableRepository mTableRepository) {
//        this.mTableRepository = mTableRepository;
//    }
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        Reflections reflections = new Reflections("co.ke.tezza.loanapp.entity");
//        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
//
//        for (Class<?> entityClass : entityClasses) {
//            Table tableAnnotation = entityClass.getAnnotation(Table.class);
//            String tableName = (tableAnnotation != null) ? tableAnnotation.name() : entityClass.getSimpleName();
//            String entityName = entityClass.getSimpleName();
//            String description = "Auto-detected existing entity: " + entityName;
//
//            boolean exists = mTableRepository.findByEntityName(entityName).isPresent();
//            if (!exists) {
//                mTableRepository.save(new MTable(tableName, entityName, description));
//                System.out.println("✅ Registered entity: " + entityName);
//            }
//
//            // 🔁 Check if entity has `approvalStage` field
//            try {
//                Field field = getFieldIfExists(entityClass, "docstatus");
//                if (field != null) {
//                    // 🛠️ Run update query for that table
//                    String jpql = "UPDATE " + entityClass.getSimpleName() + " e SET e.docstatus = :stage";
//                    Query query = entityManager.createQuery(jpql);
//                    query.setParameter("stage", DocStatus.DRAFT);
//                    int updated = query.executeUpdate();
//                    System.out.println("✅ Updated docstatus to APPROVED for " + updated + " record(s) in " + entityName);
//                }
//            } catch (Exception ex) {
//                System.err.println("⚠️ Skipped " + entityName + ": " + ex.getMessage());
//            }
//        }
//    }
//
//    private Field getFieldIfExists(Class<?> clazz, String fieldName) {
//        while (clazz != null && clazz != Object.class) {
//            try {
//                return clazz.getDeclaredField(fieldName);
//            } catch (NoSuchFieldException e) {
//                clazz = clazz.getSuperclass(); // Check parent class
//            }
//        }
//        return null;
//    }
//}

