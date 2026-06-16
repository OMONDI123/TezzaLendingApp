package co.ke.tezza.loanapp.metadata;

import java.util.Optional;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Table;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.ke.tezza.loanapp.entity.MTable;
import co.ke.tezza.loanapp.repository.MTableRepository;

@Component
public class MTableMetaDataListener {
	@Autowired private MTableRepository mTableRepository;
	
	  @PostPersist
	    @PostUpdate
	    public void registerEntity(Object entity) {
	        Table tableAnnotation = entity.getClass().getAnnotation(Table.class);
	        String tableName = (tableAnnotation != null) ? tableAnnotation.name() : entity.getClass().getSimpleName();
	        String entityName = entity.getClass().getSimpleName();
	        String description = "Auto-registered entity: " + entityName;  

	        Optional<MTable> existing = mTableRepository.findAll().stream()
	                .filter(e -> e.getEntityName().equals(entityName))
	                .findFirst();

	        if (existing.isEmpty()) {
				MTable metadata = new MTable(tableName, entityName, description);
	        	mTableRepository.save(metadata);
	        }
	    }

}
