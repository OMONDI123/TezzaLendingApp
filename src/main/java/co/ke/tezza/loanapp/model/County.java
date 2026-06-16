package co.ke.tezza.loanapp.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class County {
	private long id;
	private String name;
	private List<SubCounty> subCounties=new ArrayList<>();

}
