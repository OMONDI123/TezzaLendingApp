package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuMapping {

	private long id;
	private long AD_Role_ID;
	private long AD_Menu_ID;

	private List<Long> AD_Sub_Menu_IDs = new ArrayList<>();

}
