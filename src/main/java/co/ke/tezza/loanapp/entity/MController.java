package co.ke.tezza.loanapp.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "AD_Controller")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MController {

	@Id
	@Column(name = "AD_Controller_ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String controllerName;
	private String methodName;
	private String endpoint;
	private String urlPattern;
	private String httpMethod;
	private String description;

}
