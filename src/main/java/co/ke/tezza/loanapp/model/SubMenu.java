package co.ke.tezza.loanapp.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubMenu {
	private long id;
	private String title;
	private String view;
	private long subMenuOrder;
}
