package co.ke.tezza.loanapp.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MAPIEndpointsMappingModel {

	private long id;

	private long AD_Endpoint_ID;

	private List<Long> roleIds;

}
