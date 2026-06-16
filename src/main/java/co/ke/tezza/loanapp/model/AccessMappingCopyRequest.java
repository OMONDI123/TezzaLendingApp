package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessMappingCopyRequest {
	private long roleIdFrom;
	private long roleIdTo;
	private int totalMenuCopied;
	private int totalMappedEndpoints;
	private List<Long> accessRightIds=new ArrayList<>();
	private List<Long> menuMappingIds=new ArrayList<>();

}
