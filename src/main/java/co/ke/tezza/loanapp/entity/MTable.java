package co.ke.tezza.loanapp.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "AD_Table")
@Entity
public class MTable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AD_Table_ID")
	private Long id;

	private String tableName;
	private String entityName;
	private String description;

	@Temporal(TemporalType.TIMESTAMP)
	private Date registeredAt;

	@PrePersist
	protected void onCreate() {
		registeredAt = new Date();
	}

	public MTable(String tableName, String entityName, String description) {
		super();
		this.tableName = tableName;
		this.entityName = entityName;
		this.description = description;
	}

}
