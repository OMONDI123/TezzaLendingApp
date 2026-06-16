package co.ke.tezza.loanapp.model;

import co.ke.tezza.loanapp.enums.SupportedClientsEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBConnect {
	private String userName;
	private String database;
	private String url;
	private SupportedClientsEnum client;
	

}
