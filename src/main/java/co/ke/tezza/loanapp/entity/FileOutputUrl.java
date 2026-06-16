package co.ke.tezza.loanapp.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_output_url")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileOutputUrl extends AuditModel{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String fileUrl;
	private String fullUrl;

}
