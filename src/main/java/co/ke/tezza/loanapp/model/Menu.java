package co.ke.tezza.loanapp.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Menu {

	private long id;
	private String title;
	private long menuOrder;
	private String menuIcon;
	List<SubMenu> subMenus;

}
