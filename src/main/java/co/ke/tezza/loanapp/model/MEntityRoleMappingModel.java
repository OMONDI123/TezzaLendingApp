package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MEntityRoleMappingModel {

	private long id;

	private long AD_Table_ID;

	private List<Long> roleIds = new ArrayList<>();

}
