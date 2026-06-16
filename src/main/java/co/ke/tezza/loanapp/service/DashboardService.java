package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;

import org.apache.tools.ant.taskdefs.ManifestTask.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.ke.tezza.loanapp.entity.MADMenu;
import co.ke.tezza.loanapp.entity.MADSubMenu;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MMenuRoleMapping;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.AccessMappingCopyRequest;
import co.ke.tezza.loanapp.model.BestPerformingCollectors;
import co.ke.tezza.loanapp.model.BestPerformingDebtors;
import co.ke.tezza.loanapp.model.BestPerformingDebts;
import co.ke.tezza.loanapp.model.DashBoard;
import co.ke.tezza.loanapp.model.Menu;
import co.ke.tezza.loanapp.model.MenuMapping;
import co.ke.tezza.loanapp.model.MonthlyTrend;
import co.ke.tezza.loanapp.model.PaymentDistribution;
import co.ke.tezza.loanapp.model.PaymentStatisticsByPaymentMethod;
import co.ke.tezza.loanapp.model.SubMenu;
import co.ke.tezza.loanapp.model.TopOverdueDebtors;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;
import co.ke.tezza.loanapp.repository.MADMenuRepository;
import co.ke.tezza.loanapp.repository.MADSubMenuRepository;
import co.ke.tezza.loanapp.repository.MMenuRoleMappingRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;
import javassist.Loader.Simple;

@Service
public class DashboardService {
	@Autowired
	private Utils utils;
	@Autowired
	private MADMenuRepository menuRepository;

	@Autowired
	private MMenuRoleMappingRepository mappingRepository;

	@Autowired
	private MADSubMenuRepository subMenuRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	

	public ResponseEntity<MADMenu> createUpdateMenu(@Valid Menu model) {
		String message = "";
		int code = 201;
		MADMenu menu = menuRepository.findById(model.getId()).orElse(null);
		if (menu != null) {
			menu.setName(model.getTitle());
			menu.setDescription(model.getTitle());
			menu.setMenuIcon(model.getMenuIcon());
			menu.setMenuOrder(model.getMenuOrder());
			menuRepository.save(menu);

			message = "Menu Updated Successfully.";
			code = 200;
		} else {
			menu = new MADMenu();
			menu.setAD_Menu_UU(UUID.randomUUID().toString());
			menu.setName(model.getTitle());
			menu.setDescription(model.getTitle());
			menu.setMenuIcon(model.getMenuIcon());
			menu.setMenuOrder(model.getMenuOrder());
			menuRepository.save(menu);
			menu.setDocumentNo("AD/MENU/" + utils.getCurrentYear() + "/" + menu.getId());
			menuRepository.save(menu);
			message = "Menu Added Successfully.";
			code = 200;
		}
		Set<MADSubMenu> children = new HashSet<>();
		for (int i = 0; i < model.getSubMenus().size(); i++) {
			SubMenu subMenuModel = model.getSubMenus().get(i);
			MADSubMenu sub = subMenuRepository.findById(subMenuModel.getId()).orElse(new MADSubMenu());
			sub.setAD_Sub_Menu_UU(UUID.randomUUID().toString());
			sub.setSubMenuOrder(subMenuModel.getSubMenuOrder());
			sub.setTitle(subMenuModel.getTitle());
			sub.setView(subMenuModel.getView());
			sub.setName(subMenuModel.getTitle());
			sub.setDescription(subMenuModel.getTitle());
			subMenuRepository.save(sub);

			sub.setDocumentNo("AD/SUBMENU/" + utils.getCurrentYear() + "/" + sub.getId());
			subMenuRepository.save(sub);
			children.add(sub);
		}
		menu.setChildren(children);
		menuRepository.save(menu);
		return new ResponseEntity<MADMenu>(message, code, menu);

	}

	public ResponseEntity<Menu> copyMenu() {

		String domainUrl = utils.getSystemDomainUrl(utils.getAD_Org_ID());
		int menusCopied = 0;

		if (domainUrl == null || domainUrl.trim().isEmpty()) {
			throw new SetUpExceptions("Domain URL not configured.");
		}

		List<MADMenu> remoteMenus = getMenusToBeCopied(domainUrl);
		System.out.println("........Found " + remoteMenus.size() + " menus to copy......");

		if (remoteMenus.isEmpty()) {
			throw new SetUpExceptions("No menus found to copy.");
		}
		if (!utils.isSuperUser()) {
			throw new SetUpExceptions("You are not authorized to perform this action.");
		}

		for (MADMenu remoteMenu : remoteMenus) {

			// Avoid duplicate copy (based on externalMenuId)
			MADMenu existingMenu = menuRepository.findTopByIsActiveAndExternalMenuIdAndExternalClientIdAndAdClientIdAndNameOrTitle(
					true, remoteMenu.getId(), remoteMenu.getAdClientId(),utils.getAD_Client_ID(), remoteMenu.getName(), remoteMenu.getTitle());
			System.out.println("Remote Menu: " + remoteMenu.getName() + " exists in my DB?==" + existingMenu != null);

			if (existingMenu != null) {
				continue;
			}
			System.out.println("=============About to copy Remote Menu: " + remoteMenu.getName() + "=============");

			// Create new local menu
			MADMenu newMenu = new MADMenu();
			newMenu.setAD_Menu_UU(UUID.randomUUID().toString());
			newMenu.setTitle(remoteMenu.getName());
			newMenu.setName(remoteMenu.getName());
			newMenu.setDescription(remoteMenu.getDescription());
			newMenu.setMenuIcon(remoteMenu.getMenuIcon());
			newMenu.setMenuOrder(remoteMenu.getMenuOrder());
			newMenu.setExternalMenuId(remoteMenu.getId());
			newMenu.setExternalClientId(remoteMenu.getAdClientId());

			menuRepository.save(newMenu);

			newMenu.setDocumentNo("AD/MENU/" + utils.getCurrentYear() + "/" + newMenu.getId());
			menuRepository.save(newMenu);

			// Copy SubMenus
			Set<MADSubMenu> children = new HashSet<>();

			if (remoteMenu.getChildren() != null) {

				for (MADSubMenu remoteSub : remoteMenu.getChildren()) {

					MADSubMenu newSub = subMenuRepository.findTop1ByIsActiveAndViewEquals(true, remoteSub.getView());
					if (newSub == null) {
						newSub = new MADSubMenu();
					}

					newSub.setAD_Sub_Menu_UU(UUID.randomUUID().toString());
					newSub.setTitle(remoteSub.getTitle());
					newSub.setName(remoteSub.getTitle());
					newSub.setDescription(remoteSub.getDescription());
					newSub.setView(remoteSub.getView());
					newSub.setSubMenuOrder(remoteSub.getSubMenuOrder());

					subMenuRepository.save(newSub);

					newSub.setDocumentNo("AD/SUBMENU/" + utils.getCurrentYear() + "/" + newSub.getId());
					subMenuRepository.save(newSub);

					children.add(newSub);
				}
			}

			newMenu.setChildren(children);
			menuRepository.save(newMenu);
			System.out.println("Copied External Menu Name: " + remoteMenu.getName() + ", Client ID==="
					+ remoteMenu.getAdClientId() + " New Menu Name: " + newMenu.getName() + ", New Menu Client Id=== "
					+ newMenu.getAdClientId() + " new menu id==" + newMenu.getId());
			menusCopied++;
		}

		return new ResponseEntity<Menu>(menusCopied + " Menus Copied Successfully.", 200, new Menu());
	}

	public List<MADMenu> getMenusToBeCopied(String baseUrl) {

		List<MADMenu> menus = new ArrayList<>();

		try {
			String fullUrl = baseUrl.endsWith("/") ? baseUrl + "dashboard/getAllActiveMenus"
					: baseUrl + "/dashboard/getAllActiveMenus";

			System.out.println("The API CALLED:=====" + fullUrl);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET().build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200 && response.body() != null) {

				JsonNode root = objectMapper.readTree(response.body());

				if (root.isArray()) {

					for (JsonNode menuNode : root) {

						MADMenu menu = new MADMenu();

						menu.setId(menuNode.path("id").asLong());
						menu.setTitle(menuNode.path("title").asText());
						menu.setMenuIcon(menuNode.path("menuIcon").asText());
						menu.setMenuOrder(menuNode.path("menuOrder").asLong());
						menu.setAdClientId(menuNode.path("adClientId").asLong());
						menu.setName(menuNode.path("name").asText());
						menu.setDescription(menuNode.path("description").asText());

						// ---- Handle SubMenus manually ----
						Set<MADSubMenu> children = new HashSet<>();

						JsonNode childrenNode = menuNode.path("children");

						if (childrenNode.isArray()) {

							for (JsonNode subNode : childrenNode) {

								MADSubMenu sub = new MADSubMenu();

								sub.setTitle(subNode.path("title").asText());
								sub.setView(subNode.path("view").asText());
								sub.setSubMenuOrder(subNode.path("subMenuOrder").asLong());

								// 🔥 IMPORTANT: Ignore docStatus completely
								// DO NOT map docStatus

								children.add(sub);
							}
						}

						menu.setChildren(children);
						menus.add(menu);
					}
				}
			}

		} catch (Exception e) {
			System.err.println("Error fetching menus: " + e.getMessage());
		}

		return menus;
	}

	public ResponseEntity<MADMenu> deleteMenu(@Valid long id) {
		String message = "";
		int code = 201;
		MADMenu menu = menuRepository.findById(id).orElse(null);
		if (menu != null) {
			menu.setActive(false);
			menuRepository.save(menu);
			message = "Menu Deleted Successfully.";
			code = 200;
		}
		return new ResponseEntity<MADMenu>(message, code, menu);

	}

	public ResponseEntity<MMenuRoleMapping> deleteMappedmenu(@Valid long id) {
		String message = "";
		int code = 201;
		MMenuRoleMapping menu = mappingRepository.findById(id).orElse(null);
		if (menu != null) {
			menu.setActive(false);
			mappingRepository.save(menu);
			message = "Menu Mapping Deleted Successfully.";
			code = 200;
		}
		return new ResponseEntity<>(message, code, menu);

	}

	public ResponseEntity<AccessMappingCopyRequest> copyMappedMenus(AccessMappingCopyRequest request) {
		MRoles roleFrom = roleRepository.findById(request.getRoleIdFrom()).orElse(null);
		List<MMenuRoleMapping> menus = null;
		if (roleFrom != null) {
			menus = mappingRepository.findByIsActiveAndRoleAndAdClientIdOrderByMenu_MenuOrderAsc(true, roleFrom,
					utils.getAD_Client_ID());
		}
		if (menus.size() > 0) {
			int count = 0;
			for (MMenuRoleMapping model : menus) {
				MRoles roleTo = roleRepository.findById(request.getRoleIdTo()).orElse(null);
				MMenuRoleMapping menu = mappingRepository.findTop1ByIsActiveAndRoleAndMenu(true, roleTo,
						model.getMenu());
				if (menu == null) {

					menu = new MMenuRoleMapping();
					menu.setRole(roleTo);
					menu.setMenu(model.getMenu());
					menu.setAssociatedSubMenus(model.getAssociatedSubMenus());
					menu.setAD_Sub_Menu_Role_UU(UUID.randomUUID().toString());
					mappingRepository.save(menu);
					menu.setDocumentNo("MENU/MAPP/ROLE/" + utils.getCurrentYear() + "/" + menu.getId());
					mappingRepository.save(menu);
					count = count + 1;

				}

			}
			request.setTotalMenuCopied(count);
		}

		return new ResponseEntity<AccessMappingCopyRequest>("Menu mapping copied successfully.", 200, request);

	}

	public void mapMenus(MMenuRoleMapping model) {
		MMenuRoleMapping mapping = null;
		try {

			mapping.setMenu(model.getMenu());

			mapping.setAssociatedSubMenus(model.getAssociatedSubMenus());
			mapping.setAD_Sub_Menu_Role_UU(UUID.randomUUID().toString());
			mappingRepository.save(mapping);
			mapping.setDocumentNo("MENU/MAPP/ROLE/" + utils.getCurrentYear() + "/" + mapping.getId());
			mappingRepository.save(mapping);

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	public List<MMenuRoleMapping> getMappedMenusByRoleId() {
		List<MMenuRoleMapping> menus = null;

		try {
			MRoles role = roleRepository.findById(utils.getAD_Role_ID()).orElse(null);
			if (role != null) {
				menus = mappingRepository.findByIsActiveAndRoleAndAdClientIdOrderByMenu_MenuOrderAsc(true, role,
						utils.getAD_Client_ID());
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return menus;
	}

	public Page<MMenuRoleMapping> getMappedMenus(int page, int size, String search) {
		Page<MMenuRoleMapping> menus = null;

		try {
			if (search != null && !search.isEmpty()) {
				menus = mappingRepository.searchMappedSubMenus(utils.getAD_Org_ID(), "%" + search + "%",
						PageRequest.of(page, size));
			}

			menus = mappingRepository.findByIsActiveAndAdOrgIDOrderByIdAsc(true, utils.getAD_Org_ID(),
					PageRequest.of(page, size));

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return menus;
	}

	public ResponseEntity<MMenuRoleMapping> mappOrUpdateMenuMapping(@Valid MenuMapping model) {

		String message = "";
		int code = 201;
		MMenuRoleMapping mapping = null;
		try {
			mapping = mappingRepository.findById(model.getId()).orElse(null);
			if (mapping != null) {
				MRoles role = roleRepository.findById(model.getAD_Role_ID()).orElse(null);
				if (role != null) {
					mapping.setRole(role);

				}
				Set<MADSubMenu> menus = new HashSet<>();
				for (int i = 0; i < model.getAD_Sub_Menu_IDs().size(); i++) {
					MADSubMenu menu = subMenuRepository.findById(model.getAD_Sub_Menu_IDs().get(i)).orElse(null);
					if (menu != null) {
						menus.add(menu);
					}
				}
				mapping.setMenu(menuRepository.findById(model.getAD_Menu_ID()).get());
				mapping.setAssociatedSubMenus(menus);
				mappingRepository.save(mapping);
				message = "Menus Assigned to Role " + role.getFormattedName() + " have been updated successfully.";

				code = 200;

			} else {
				mapping = new MMenuRoleMapping();
				MRoles role = roleRepository.findById(model.getAD_Role_ID()).orElse(null);
				if (role != null) {
					mapping.setRole(role);

				}
				mapping.setMenu(menuRepository.findById(model.getAD_Menu_ID()).get());
				Set<MADSubMenu> menus = new HashSet<>();
				for (int i = 0; i < model.getAD_Sub_Menu_IDs().size(); i++) {
					MADSubMenu menu = subMenuRepository.findById(model.getAD_Sub_Menu_IDs().get(i)).orElse(null);
					if (menu != null) {
						menus.add(menu);
					}
				}
				mapping.setAssociatedSubMenus(menus);
				mapping.setAD_Sub_Menu_Role_UU(UUID.randomUUID().toString());
				mappingRepository.save(mapping);
				mapping.setDocumentNo("MENU/MAPP/ROLE/" + utils.getCurrentYear() + "/" + mapping.getId());
				mappingRepository.save(mapping);
				message = "Menus Have been Assigned Successfully.to Role " + role.getFormattedName();
				code = 200;
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return new ResponseEntity<MMenuRoleMapping>(message, code, mapping);

	}

	public DashBoard getDashBoardStatistics(String dateFrom, String dateTo) {
		DashBoard dashboard = new DashBoard();

		return dashboard;
	}

	public DashBoard getAdminDashBoardStatistics(String dateFrom, String dateTo, Long orgId) {
		DashBoard dashboard = new DashBoard();
		final long finalOrgId = (orgId == null || orgId <= 0) ? utils.getAD_Org_ID() : orgId;
		SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try {
			
			Date dateF=utils.getStartOfDay(fm.parse(dateFrom));
			Date dateT=utils.getEndOfDay(fm.parse(dateTo));

			String sql = " WITH params AS(\n" + "    SELECT :AD_Org_ID  AS AD_Org_ID,\n"
					+ "    :dateFrom ::timestamp AS dateFrom,\n" + "    :dateTo ::timestamp AS dateTo\n" + "),\n"
					+ "totalDebtsRegistered AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "totalDebtsApproved AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND approvalstage='APPROVED' AND isactive=true),\n"
					+ "totalPendingApprovals AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params)\n"
					+ "    AND approvalstage NOT IN('APPROVED','COMPLETED','AMENDED') AND isactive=true),\n"
					+ "totalRejectedDebts AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND approvalstage ='REJECTED' AND isactive=true),\n"
					+ "totalPartiallyPaidDebts AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND repayment_status='PARTIALLY_PAID' AND isactive=true),\n"
					+ "totalNonPaidDebts AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND repayment_status='PENDING' AND isactive=true),\n"
					+ "totalFullyPaidDebts AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND repayment_status='PAID' AND isactive=true),\n"
					+ "totalAmountApproved AS (SELECT SUM(COALESCE(approved_amount,0.00)) AS total_amount FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND approvalstage='APPROVED' AND isactive=true),\n"
					+ "totalOutstandingDebts AS (SELECT COUNT(*) AS total_debts FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND approvalstage='APPROVED' AND balance>0 AND isactive=true),\n"
					+ "totalOutstandingBalance AS (SELECT SUM(COALESCE(balance,0.00)) AS total_outstanding_balance FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND approvalstage='APPROVED' AND balance>0 AND isactive=true),\n"
					+ "totalPenaltiesCharged AS (SELECT SUM(COALESCE(penalty_earned,0.00)) AS total_penalties FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "totalInterestEarned AS (SELECT SUM(COALESCE(interests_earned,0.00)) AS total_interest FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "totalAmountWaived AS (SELECT SUM(COALESCE(exempted_amount,0.00)) AS total_waived FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "totalInterestsWaived AS (SELECT SUM(COALESCE(exempted_interests,0.00)) AS total_waived FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "penaltiesWaived AS (SELECT SUM(COALESCE(exempted_penalties,0.00)) AS total_waived FROM AD_Loan_Application WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND expected_disbursement_date BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "totalPayments AS (SELECT SUM(COALESCE(amount,0.00)) AS total_paid FROM AD_Payment WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND payment_date_time BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND is_paid=true AND isactive=true AND waiver_write_off=false AND wallet_deposit=false),\n"
					+ "totalWriteOffs AS (SELECT SUM(COALESCE(p.amount,0.00)) AS total_paid FROM AD_Payment p INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID=p.AD_Payment_Method_ID WHERE p.AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND p.payment_date_time BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND  p.waiver_write_off=true  AND p.isactive=true AND pm.payment_type='WRITE_OFF'),    \n"
					+ "totalCreditNoteWaivers AS (SELECT SUM(COALESCE(p.amount,0.00)) AS total_paid FROM AD_Payment p INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID=p.AD_Payment_Method_ID WHERE p.AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND p.payment_date_time BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND p.is_paid=true AND p.isactive=true AND pm.payment_type='CREDIT_NOTE'),    \n"
					+ "totalUnAllocatedSecurityPayments AS (SELECT SUM(COALESCE(amount,0.00)) AS total_paid FROM AD_Payment WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND payment_date_time BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND approvalstage='PENDING_ALLOCATION' \n"
					+ "    AND security_payment=true  AND isactive=true AND waiver_write_off=false AND wallet_deposit=false  ),\n"
					+ "totalAllocatedSecurityPayments AS (SELECT SUM(COALESCE(amount,0.00)) AS total_paid FROM AD_Payment WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND payment_date_time BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND approvalstage='APPROVED' AND security_payment=true\n"
					+ "    AND is_paid=true AND isactive=true AND waiver_write_off=false AND wallet_deposit=false  ),\n"
					+ "totalSecurityPayments AS (SELECT SUM(COALESCE(amount,0.00)) AS total_paid FROM AD_Payment WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND payment_date_time BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) \n"
					+ "    AND security_payment=true AND isactive=true AND waiver_write_off=false AND wallet_deposit=false  ),\n"
					+ "totalCompletedPayments AS (SELECT SUM(COALESCE(amount,0.00)) AS total_paid FROM AD_Payment WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND payment_date_time BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params)\n"
					+ "    AND approvalstage='APPROVED' AND is_paid=true AND isactive=true AND waiver_write_off=false AND wallet_deposit=false  ),\n"
					+ "individualDebtors AS (SELECT COUNT(*) AS total FROM AD_Debtor WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND created BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "institutionDebtors AS (SELECT COUNT(*) AS total FROM AD_Institution_Borrower WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND created BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "groupDebtors AS (SELECT COUNT(*) AS total FROM AD_Group_Borrower WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND created BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true),\n"
					+ "registeredDebtors AS (SELECT (g.total+ins.total+ind.total) AS total_registered_debtors FROM \n"
					+ "    groupDebtors g CROSS JOIN institutionDebtors ins CROSS JOIN individualDebtors ind),\n"
					+ "remindersSent AS (SELECT COUNT(*) AS total FROM AD_Sms WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND times_tosend BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true AND message IS NOT NULL AND response_code='200'),\n"
					+ "remindersFailed AS (SELECT COUNT(*) AS total FROM AD_Sms WHERE AD_Org_ID=(SELECT AD_Org_ID FROM params) \n"
					+ "    AND times_tosend BETWEEN (SELECT dateFrom FROM params) AND (SELECT dateTo FROM params) AND isactive=true AND message IS NOT NULL AND response_code!='200')\n"
					+ "    \n" + "\n" + "SELECT \n" + "    d.total_debts AS totalDebtsRegistered,\n"
					+ "    a.total_debts AS totalDebtsApproved,\n" + "    p.total_debts AS totalPendingApprovals,\n"
					+ "    r.total_debts AS totalRejectedDebts,\n" + "    pp.total_debts AS totalPartiallyPaidDebts,\n"
					+ "    fp.total_debts AS totalFullyPaidDebts,\n" + "    np.total_debts AS nonPaidDebts,\n"
					+ "    aa.total_amount AS totalAmountApproved,\n" + "    od.total_debts AS totalOutstandingDebts,\n"
					+ "    ob.total_outstanding_balance AS totalOutstandingBalance,\n"
					+ "    pc.total_penalties AS totalPenaltiesCharged,\n"
					+ "    ie.total_interest AS totalInterestEarned,\n" + "    aw.total_waived AS totalAmountWaived,\n"
					+ "    iw.total_waived AS totalInterestsWaived,\n" + "    pw.total_waived AS penaltiesWaived,\n"
					+ "    twf.total_paid AS totalAmountWrittenOff, \n" + " "
					+ "    tcw.total_paid AS totalCreditNoteWaivers, \n   tp.total_paid AS totalPayments,\n"
					+ "    usp.total_paid AS totalUnAllocatedSecurityPayments,\n"
					+ "    asp.total_paid AS totalAllocatedSecurityPayments,\n"
					+ "    sp.total_paid AS totalSecurityPayments,\n" + "    cp.total_paid AS totalCompletedPayments,\n"
					+ "    rs.total AS remindersSent,\n" + "    rf.total AS remindersFailed,\n"
					+ "    0.00 AS smsBalance, \n" + "    rd.total_registered_debtors AS registeredDebtors,\n"
					+ "    id.total AS individualDebtors,\n" + "    inst.total AS institutionDebtors,\n"
					+ "    gd.total AS groupDebtors\n" + "FROM totalDebtsRegistered d\n"
					+ "CROSS JOIN totalDebtsApproved a\n" + "CROSS JOIN totalPendingApprovals p\n"
					+ "CROSS JOIN totalRejectedDebts r\n" + "CROSS JOIN totalPartiallyPaidDebts pp\n"
					+ "CROSS JOIN totalFullyPaidDebts fp\n" + "CROSS JOIN totalAmountApproved aa\n"
					+ "CROSS JOIN totalOutstandingDebts od\n" + "CROSS JOIN totalOutstandingBalance ob\n"
					+ "CROSS JOIN totalPenaltiesCharged pc\n" + "CROSS JOIN totalInterestEarned ie\n"
					+ "CROSS JOIN totalAmountWaived aw\n" + "CROSS JOIN totalInterestsWaived iw\n"
					+ "CROSS JOIN penaltiesWaived pw\n" + "CROSS JOIN totalPayments tp\n"
					+ "CROSS JOIN totalUnAllocatedSecurityPayments usp\n"
					+ "CROSS JOIN totalAllocatedSecurityPayments asp\n" + "CROSS JOIN totalSecurityPayments sp\n"
					+ "CROSS JOIN totalCompletedPayments cp\n" + "CROSS JOIN registeredDebtors rd\n"
					+ "CROSS JOIN remindersSent rs\n" + "CROSS JOIN remindersFailed rf\n"
					+ "CROSS JOIN individualDebtors id\n" + "CROSS JOIN institutionDebtors inst\n"
					+ "CROSS JOIN groupDebtors gd\n" + "CROSS JOIN totalNonPaidDebts np\n"
					+ "CROSS JOIN totalWriteOffs twf  " + "CROSS JOIN totalCreditNoteWaivers tcw ;";

			MapSqlParameterSource params = new MapSqlParameterSource().addValue("AD_Org_ID", finalOrgId)
					.addValue("dateFrom", dateF).addValue("dateTo", dateT);

			try {
				dashboard = namedParameterJdbcTemplate.queryForObject(sql, params, new RowMapper<DashBoard>() {
					@Override
					public DashBoard mapRow(ResultSet rs, int rowNum) throws SQLException {
						DashBoard db = new DashBoard();
						db.setTotalDebtsRegistered(safeGetInteger(rs, "totalDebtsRegistered"));
						db.setTotalDebtsApproved(safeGetInteger(rs, "totalDebtsApproved"));
						db.setTotalPendingApprovals(safeGetInteger(rs, "totalPendingApprovals"));
						db.setTotalRejectedDebts(safeGetInteger(rs, "totalRejectedDebts"));
						db.setTotalPartiallyPaidDebts(safeGetInteger(rs, "totalPartiallyPaidDebts"));
						db.setTotalFullyPaidDebts(safeGetInteger(rs, "totalFullyPaidDebts"));
						db.setTotalAmountApproved(safeGetBigDecimal(rs, "totalAmountApproved"));
						db.setTotalOutstandingDebts(safeGetInteger(rs, "totalOutstandingDebts"));
						db.setTotalOutstandingBalance(safeGetBigDecimal(rs, "totalOutstandingBalance"));
						db.setTotalPenaltiesCharged(safeGetBigDecimal(rs, "totalPenaltiesCharged"));
						db.setTotalInterestEarned(safeGetBigDecimal(rs, "totalInterestEarned"));
						db.setTotalAmountWaived(safeGetBigDecimal(rs, "totalAmountWaived"));
						db.setTotalInterestsWaived(safeGetBigDecimal(rs, "totalInterestsWaived"));
						db.setPenaltiesWaived(safeGetBigDecimal(rs, "penaltiesWaived"));
						db.setTotalAmountWrittenOff(safeGetBigDecimal(rs, "totalAmountWrittenOff"));
						db.setTotalPayments(safeGetBigDecimal(rs, "totalPayments"));
						db.setTotalUnAllocatedSecurityPayments(
								safeGetBigDecimal(rs, "totalUnAllocatedSecurityPayments"));
						db.setTotalAllocatedSecurityPayments(safeGetBigDecimal(rs, "totalAllocatedSecurityPayments"));
						db.setTotalSecurityPayments(safeGetBigDecimal(rs, "totalSecurityPayments"));
						db.setTotalCompletedPayments(safeGetBigDecimal(rs, "totalCompletedPayments"));
						db.setRemindersSent(safeGetInteger(rs, "remindersSent"));
						db.setRemindersFailed(safeGetInteger(rs, "remindersFailed"));
						db.setTotalReminders(db.getRemindersFailed() + db.getRemindersSent());
						db.setSmsBalance(safeGetBigDecimal(rs, "smsBalance"));
						db.setRegisteredDebtors(safeGetInteger(rs, "registeredDebtors"));
						db.setIndividualDebtors(safeGetInteger(rs, "individualDebtors"));
						db.setInstitutionDebtors(safeGetInteger(rs, "institutionDebtors"));
						db.setGroupDebtors(safeGetInteger(rs, "groupDebtors"));
						db.setNonPaidDebts(safeGetInteger(rs, "nonPaidDebts"));
						db.setTotalWaiverByCreditNote(safeGetBigDecimal(rs, "totalCreditNoteWaivers"));

						List<TopOverdueDebtors> topOverdueList = getTopOverdueDebtors(dateF, dateT, finalOrgId);
						db.setTopOverdueDebtors(topOverdueList);
						List<TopOverdueDebtors> topOverdueListByAmount = getTopOverdueDebtorsByAmount(dateF, dateT,
								finalOrgId);
						db.setTopOverdueDebtorsByAmount(topOverdueListByAmount);

						Map<String, BigDecimal> paymentStats = getPaymentStatisticsByMethod(dateF, dateT, finalOrgId);
						List<PaymentStatisticsByPaymentMethod> stats=     getPaymentStatisticsByMethodList(dateF, dateT, finalOrgId);
						db.setPaymentStatisticsByPaymentMethod(paymentStats);
						db.setPaymentStatisticsByPaymentMethodObj(stats);
						db.setOrganisationConversionRate(calculatePaymentToApprovedRatio(db));
						List<PaymentDistribution> monthlyDistribution = getMonthlyPaymentDistribution(finalOrgId, dateF,
								dateT);
						List<PaymentDistribution> monthlyDistributionByPaymentMethod = getMonthlyPaymentDistributionByMethod(
								finalOrgId, dateF, dateT);
						List<PaymentDistribution> yearlyDistribution = getYearlyPaymentDistribution(finalOrgId, dateF,
								dateT);
						List<PaymentDistribution> yearlyDistributionByPaymentMethod = getYearlyPaymentDistributionByMethod(
								finalOrgId, dateF, dateT);
						db.setMonthlyPaymentDist(monthlyDistribution);
						db.setMonthlyPaymentDistByPaymentMethod(monthlyDistributionByPaymentMethod);
						db.setYearlyPaymentDistribution(yearlyDistribution);
						db.setYearlyPaymentDistributionByPaymentMethod(yearlyDistributionByPaymentMethod);

						List<BestPerformingDebts> bestPerformingDebts = getBestPerformingDebts(finalOrgId, dateF,
								dateT);
						db.setBestPerformingDebts(bestPerformingDebts);
						List<BestPerformingDebtors> bestPerformingDebtors = getBestPerformingDebtors(finalOrgId, dateF,
								dateT);
						db.setBestPerformingDebtors(bestPerformingDebtors);

						List<BestPerformingCollectors> bestPerformingCollectors = getBestPerformingCollectors(
								finalOrgId, dateF, dateT);
						db.setBestPerformingCollectors(bestPerformingCollectors);
						db.setApprovedAmountollectionRate(calculateApprovedAmountollectionRate(db));
						db.setBestPerformingDebtsByAllPayments(getBestPerformingDebtsByAllPayments(finalOrgId, dateF, dateT));
						return db;
					}
				});

			} catch (EmptyResultDataAccessException e) {
				e.printStackTrace();
				return new DashBoard();
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return dashboard;
	}

	// Safe division with zero check
	private BigDecimal calculateApprovedAmountollectionRate(DashBoard db) {
		BigDecimal totalApproved = db.getTotalAmountApproved();
		BigDecimal totalPayments = db.getTotalPayments();
		BigDecimal totalWriteOffs = db.getTotalAmountWrittenOff();
		BigDecimal totalCreditNoteWaivers = db.getTotalWaiverByCreditNote();
		BigDecimal totalNegativePayments = totalCreditNoteWaivers.add(totalWriteOffs);
		BigDecimal payments = totalPayments.subtract(totalNegativePayments);

		// Check if denominator is zero
		if (totalApproved == null || totalApproved.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		// Safe division
		return payments.divide(totalApproved, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).setScale(2,
				RoundingMode.HALF_UP);
	}

	private BigDecimal calculatePaymentToApprovedRatio(DashBoard db) {
		BigDecimal totalApproved = db.getTotalAmountApproved()
				.add(db.getTotalPenaltiesCharged().add(db.getTotalInterestEarned()));
		BigDecimal totalPayments = db.getTotalPayments();
		BigDecimal totalWriteOffs = db.getTotalAmountWrittenOff();
		BigDecimal totalCreditNoteWaivers = db.getTotalWaiverByCreditNote();
		BigDecimal totalNegativePayments = totalCreditNoteWaivers.add(totalWriteOffs);
		BigDecimal payments = totalPayments.subtract(totalNegativePayments);

		// Check if denominator is zero
		if (totalApproved == null || totalApproved.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}

		// Safe division
		return payments.divide(totalApproved, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).setScale(2,
				RoundingMode.HALF_UP);
	}

	public List<BestPerformingCollectors> getBestPerformingCollectors(long orgId, Date dateFrom, Date dateTo) {
		String sql = "SELECT " + "    u.fullname AS collectorName, "
				+ "    COALESCE(SUM(p.amount), 0) AS totalAmountCollected, " + "    ROUND( " + "        CASE "
				+ "            WHEN EXTRACT(MONTH FROM AGE(:dateTo::date, :dateFrom::date)) > 0 "
				+ "                AND COALESCE(SUM(p.amount), 0) > 0 "
				+ "            THEN COALESCE(SUM(p.amount), 0) / "
				+ "                 EXTRACT(MONTH FROM AGE(:dateTo::date, :dateFrom::date)) " + "            ELSE 0 "
				+ "        END, " + "    2) AS monthlyAverageCollections " + "FROM AD_Payment p "
				+ "INNER JOIN AD_User u ON u.ad_user_id = p.receipted_by " + "WHERE p.is_paid = true "
				+ "    AND p.approvalstage = 'APPROVED' " + "    AND p.payment_date_time BETWEEN :dateFrom AND :dateTo "
				+ "    AND u.isactive = true " + "    AND p.ad_org_id = :orgId "
				+ "    AND p.receipted_by IS NOT NULL  AND p.payment_method NOT IN ('CREDIT_NOTE','WRITE_OFF') AND p.waiver_write_off=false AND p.wallet_deposit=false \n"
				+ "GROUP BY u.ad_user_id, u.fullname " + "HAVING COALESCE(SUM(p.amount), 0) > 0 "
				+ "ORDER BY totalAmountCollected DESC ";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				BestPerformingCollectors collector = new BestPerformingCollectors();
				collector.setCollectorName(rs.getString("collectorName"));
				collector.setTotalAmountCollected(rs.getBigDecimal("totalAmountCollected"));
				collector.setMonthlyAverageCollections(rs.getBigDecimal("monthlyAverageCollections"));
				return collector;
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch best performing collectors for orgId: " + orgId + " from "
					+ dateFrom + " to " + dateTo, e);
		}
	}

	public List<BestPerformingDebtors> getBestPerformingDebtors(long orgId, Date dateFrom, Date dateTo) {
		String sql = "SELECT " + "    CASE "
				+ "        WHEN l.borrower_type = 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name) "
				+ "        WHEN l.borrower_type = 'GROUP' THEN g.group_name "
				+ "        WHEN l.borrower_type = 'INSTITUTION' THEN inst.institution_name " + "        ELSE 'Unknown' "
				+ "    END AS debtorName, " + "    COALESCE(SUM(p.amount), 0) AS totalPaid, " + "    ROUND( "
				+ "        CASE " + "            WHEN EXTRACT(MONTH FROM AGE(:dateTo::date, :dateFrom::date)) > 0 "
				+ "                AND COALESCE(SUM(p.amount), 0) > 0 "
				+ "            THEN COALESCE(SUM(p.amount), 0) / "
				+ "                 EXTRACT(MONTH FROM AGE(:dateTo::date, :dateFrom::date)) " + "            ELSE 0 "
				+ "        END, " + "    2) AS averageMonthlyPayments " + "FROM AD_Loan_Application l "
				+ "LEFT JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID "
				+ "LEFT JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID "
				+ "LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID "
				+ "LEFT JOIN AD_Payment p ON p.AD_Loan_Application_ID = l.AD_Loan_Application_ID "
				+ "    AND p.is_paid = true " + "    AND p.approvalstage = 'APPROVED' "
				+ "    AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "WHERE l.isactive = true "
				+ "    AND l.AD_Org_ID = :orgId "
				+ "    AND l.approvalstage IN ('APPROVED', 'COMPLETE')    AND p.payment_method NOT IN ('CREDIT_NOTE','WRITE_OFF')  AND p.waiver_write_off=false AND p.wallet_deposit=false \n"
				+ "    AND l.expected_disbursement_date BETWEEN :dateFrom AND :dateTo " + "    AND EXISTS ( "
				+ "        SELECT 1 " + "        FROM AD_Payment p2 "
				+ "        WHERE p2.AD_Loan_Application_ID = l.AD_Loan_Application_ID "
				+ "        AND p2.is_paid = true "
				+ "        AND p2.approvalstage = 'APPROVED'   AND p2.payment_method NOT IN ('CREDIT_NOTE','WRITE_OFF') AND p2.waiver_write_off=false \n"
				+ "        AND p2.payment_date_time BETWEEN :dateFrom AND :dateTo " + "    ) " + "GROUP BY "
				+ "    l.borrower_type, " + "    d.first_name, " + "    d.last_name, " + "    g.group_name, "
				+ "    inst.institution_name " + "HAVING COALESCE(SUM(p.amount), 0) > 0 " + "ORDER BY totalPaid DESC ";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				BestPerformingDebtors debtor = new BestPerformingDebtors();
				debtor.setDebtorName(rs.getString("debtorName"));
				debtor.setTotalPaid(rs.getBigDecimal("totalPaid"));
				debtor.setAverageMonthlyPayments(rs.getBigDecimal("averageMonthlyPayments"));
				return debtor;
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch best performing debtors for orgId: " + orgId + " from "
					+ dateFrom + " to " + dateTo, e);
		}
	}

	public List<TopOverdueDebtors> getTopOverdueDebtors(Date dateFrom, Date dateTo, long orgId) {
		List<TopOverdueDebtors> list = new ArrayList<>();
		List<MLoanApplication> loans = loanApplicationRepository
				.findTop10ByIsActiveAndAdOrgIDAndBalanceGreaterThanAndDueDateLessThanAndExpectedDisbursementDateBetweenAndApprovalStageOrderByDueDateAsc(
						true, orgId, BigDecimal.ZERO, new Date(), dateFrom, dateTo, ApprovalStage.APPROVED);
		if (loans.isEmpty()) {
			return new ArrayList<>();
		}
		for (MLoanApplication loan : loans) {
			String borrowername = null;
			int noOfDaysOverdue = 0;
			BigDecimal overdueBalance = BigDecimal.ZERO;

			if (loan != null) {
				borrowername = utils.getBorrowerName(loan);
				noOfDaysOverdue = utils.getNoOfDaysBetweenTwoDates(loan.getDueDate(), new Date()) + 1;
				overdueBalance = loan.getBalance();
				TopOverdueDebtors debtor = new TopOverdueDebtors(noOfDaysOverdue, overdueBalance, borrowername);
				list.add(debtor);
			}
		}
		return list;
	}

	public List<TopOverdueDebtors> getTopOverdueDebtorsByAmount(Date dateFrom, Date dateTo, long orgId) {
		List<TopOverdueDebtors> list = new ArrayList<>();
		List<MLoanApplication> loans = loanApplicationRepository
				.findTop10ByIsActiveAndAdOrgIDAndBalanceGreaterThanAndDueDateLessThanAndExpectedDisbursementDateBetweenAndApprovalStageOrderByBalanceDesc(
						true, orgId, BigDecimal.ZERO, new Date(), dateFrom, dateTo, ApprovalStage.APPROVED);
		if (loans.isEmpty()) {
			return new ArrayList<>();
		}
		for (MLoanApplication loan : loans) {
			String borrowername = null;
			int noOfDaysOverdue = 0;
			BigDecimal overdueBalance = BigDecimal.ZERO;

			if (loan != null) {
				borrowername = utils.getBorrowerName(loan);
				noOfDaysOverdue = utils.getNoOfDaysBetweenTwoDates(loan.getDueDate(), new Date()) + 1;
				overdueBalance = loan.getBalance();
				TopOverdueDebtors debtor = new TopOverdueDebtors(noOfDaysOverdue, overdueBalance, borrowername);
				list.add(debtor);
			}
		}
		return list;
	}

	private Map<String, BigDecimal> getPaymentStatisticsByMethod(Date dateFrom, Date dateTo, long orgId) {
		String sql = "SELECT \n" + "         pm.name AS payment_method,\n"
				+ "    COALESCE(SUM(amount), 0) as total_amount\n" + "FROM AD_Payment p\n"
				+ "INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID=p.AD_Payment_Method_ID\n"
				+ "WHERE p.AD_Org_ID = :orgId\n" + "    AND p.payment_date_time BETWEEN :dateFrom AND :dateTo\n"
				+ "    AND p.is_paid = true AND p.waiver_write_off=false AND p.wallet_deposit=false  \n" + "    AND p.isactive = true\n" + "GROUP BY pm.name\n"
				+ "HAVING COALESCE(SUM(p.amount), 0) > 0\n" + "ORDER BY total_amount DESC;";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		return namedParameterJdbcTemplate.query(sql, params, rs -> {
			Map<String, BigDecimal> stats = new LinkedHashMap<>();
			while (rs.next()) {
				stats.put(rs.getString("payment_method"), rs.getBigDecimal("total_amount"));
			}
			return stats;
		});
	}
	
	private List<PaymentStatisticsByPaymentMethod> getPaymentStatisticsByMethodList(Date dateFrom, Date dateTo, long orgId) {
	    String sql = "SELECT \n" +
	            "    pm.name AS payment_method,\n" +
	            "    COALESCE(SUM(amount), 0) as total_amount\n" +
	            "FROM AD_Payment p\n" +
	            "INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID = p.AD_Payment_Method_ID\n" +
	            "WHERE p.AD_Org_ID = :orgId\n" +
	            "    AND p.payment_date_time BETWEEN :dateFrom AND :dateTo\n" +
	            "    AND p.is_paid = true \n" +
	            "    AND p.waiver_write_off = false \n" +
	            "    AND p.wallet_deposit = false\n" +
	            "    AND p.isactive = true\n" +
	            "GROUP BY pm.name\n" +
	            "HAVING COALESCE(SUM(p.amount), 0) > 0\n" +
	            "ORDER BY total_amount DESC";

	    MapSqlParameterSource params = new MapSqlParameterSource()
	            .addValue("orgId", orgId)
	            .addValue("dateFrom", dateFrom)
	            .addValue("dateTo", dateTo);

	    return namedParameterJdbcTemplate.query(sql, params,
	            (rs, rowNum) -> new PaymentStatisticsByPaymentMethod(
	                    rs.getString("payment_method"),
	                    rs.getBigDecimal("total_amount")
	            ));
	}

	public List<PaymentDistribution> getMonthlyPaymentDistributionByMethod(long orgId, Date dateFrom, Date dateTo) {
		String sql = "WITH date_range AS ( " + "    SELECT :dateFrom::timestamp AS start_date, "
				+ "           :dateTo::timestamp AS end_date, "
				+ "           EXTRACT(YEAR FROM AGE(:dateTo::timestamp, :dateFrom::timestamp)) AS years_diff "
				+ "), monthly_data AS ( " + "    SELECT "
				+ "        TO_CHAR(p.payment_date_time, 'Month') AS month_group, "
				+ "        EXTRACT(MONTH FROM p.payment_date_time) AS sort_order, "
				+ "        pm.name AS payment_method, " + "        p.amount " + "    FROM AD_Payment p "
				+ "    INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID = p.AD_Payment_Method_ID "
				+ "    CROSS JOIN date_range " + "    WHERE p.AD_Org_ID = :orgId "
				+ "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "        AND p.is_paid = true  AND p.waiver_write_off=false AND p.wallet_deposit=false "
				+ "        AND p.isactive = true " + ") "
				+ "SELECT month_group, payment_method, SUM(COALESCE(amount, 0)) AS total_amount, sort_order "
				+ "FROM monthly_data " + "GROUP BY month_group, sort_order, payment_method "
				+ "HAVING SUM(COALESCE(amount, 0)) > 0 " + "ORDER BY sort_order, payment_method";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				PaymentDistribution pd = new PaymentDistribution();
				pd.setMonth(rs.getString("month_group").trim()); // Trim to remove extra spaces
				pd.setPaymentMethod(rs.getString("payment_method"));
				pd.setAmountCollected(rs.getBigDecimal("total_amount"));
				return pd;
			});
		} catch (DataAccessException e) {
			throw new RuntimeException("Failed to fetch payment distribution by method for orgId: " + orgId + " from "
					+ dateFrom + " to " + dateTo, e);
		}
	}

	public List<PaymentDistribution> getMonthlyPaymentDistribution(long orgId, Date dateFrom, Date dateTo) {
		String sql = "WITH date_range AS ( " + "    SELECT :dateFrom::timestamp AS start_date, "
				+ "           :dateTo::timestamp AS end_date, "
				+ "           EXTRACT(YEAR FROM AGE(:dateTo::timestamp, :dateFrom::timestamp)) AS years_diff "
				+ "), monthly_data AS ( " + "    SELECT "
				+ "        TO_CHAR(p.payment_date_time, 'Month') AS month_group, "
				+ "        EXTRACT(MONTH FROM p.payment_date_time) AS sort_order, "
				+ "        p.amount " + "    FROM AD_Payment p " + "    CROSS JOIN date_range "
				+ "    WHERE p.AD_Org_ID = :orgId " + "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo "
				+ "        AND p.is_paid = true  AND p.wallet_deposit = false  AND p.waiver_Or_Write_Off = false  AND p.waiver_write_off=false " + "        AND p.isactive = true "
				+ "        AND p.payment_method NOT IN ('CREDIT_NOTE', 'WRITE_OFF') AND p.waiver_write_off=false AND p.wallet_deposit=false " + // Added exclusion
				") " + "SELECT month_group, SUM(COALESCE(amount, 0)) AS total_amount, sort_order "
				+ "FROM monthly_data " + "GROUP BY month_group, sort_order " + "HAVING SUM(COALESCE(amount, 0)) > 0 "
				+ "ORDER BY sort_order";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				PaymentDistribution pd = new PaymentDistribution();
				pd.setMonth(rs.getString("month_group").trim()); // Trim to remove extra spaces
				pd.setAmountCollected(rs.getBigDecimal("total_amount"));
				return pd;
			});
		} catch (DataAccessException e) {
			throw new RuntimeException(
					"Failed to fetch payment distribution for orgId: " + orgId + " from " + dateFrom + " to " + dateTo,
					e);
		}
	}

	public List<PaymentDistribution> getYearlyPaymentDistribution(long orgId, Date dateFrom, Date dateTo) {
		String sql = "WITH date_range AS ( " + "    SELECT :dateFrom::timestamp AS start_date, "
				+ "           :dateTo::timestamp AS end_date, "
				+ "           EXTRACT(YEAR FROM AGE(:dateTo::timestamp, :dateFrom::timestamp)) AS years_diff "
				+ "), yearly_data AS ( " + "    SELECT "
				+ "        EXTRACT(YEAR FROM p.payment_date_time) AS year_group, "
				+ "        EXTRACT(YEAR FROM p.payment_date_time) AS sort_order, " + "        p.amount "
				+ "    FROM AD_Payment p " + "    CROSS JOIN date_range " + "    WHERE p.AD_Org_ID = :orgId "
				+ "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "        AND p.is_paid = true "
				+ "        AND p.isactive = true AND p.wallet_deposit = false  AND p.waiver_Or_Write_Off = false  AND p.waiver_write_off=false " + "        AND p.payment_method NOT IN ('CREDIT_NOTE', 'WRITE_OFF') "
				+ // Added exclusion
				") " + "SELECT year_group::text, SUM(COALESCE(amount, 0)) AS total_amount " + "FROM yearly_data "
				+ "GROUP BY year_group, sort_order " + "HAVING SUM(COALESCE(amount, 0)) > 0 " + "ORDER BY sort_order";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				PaymentDistribution pd = new PaymentDistribution();
				pd.setYear(rs.getString("year_group"));
				pd.setAmountCollected(rs.getBigDecimal("total_amount"));
				return pd;
			});
		} catch (DataAccessException e) {
			throw new RuntimeException("Failed to fetch yearly payment distribution for orgId: " + orgId + " from "
					+ dateFrom + " to " + dateTo, e);
		}
	}

	public List<PaymentDistribution> getYearlyPaymentDistributionByMethod(long orgId, Date dateFrom, Date dateTo) {
		String sql = "WITH date_range AS ( " + "    SELECT :dateFrom::timestamp AS start_date, "
				+ "           :dateTo::timestamp AS end_date, "
				+ "           EXTRACT(YEAR FROM AGE(:dateTo::timestamp, :dateFrom::timestamp)) AS years_diff "
				+ "), yearly_data AS ( " + "    SELECT "
				+ "        EXTRACT(YEAR FROM p.payment_date_time) AS year_group, "
				+ "        EXTRACT(YEAR FROM p.payment_date_time) AS sort_order, "
				+ "        pm.name AS payment_method, " + "        p.amount " + "    FROM AD_Payment p "
				+ "    INNER JOIN AD_Payment_Method pm ON pm.AD_Payment_Method_ID = p.AD_Payment_Method_ID "
				+ "    CROSS JOIN date_range " + "    WHERE p.AD_Org_ID = :orgId "
				+ "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "        AND p.is_paid = true "
				+ "        AND p.isactive = true  AND p.wallet_deposit = false  AND p.waiver_Or_Write_Off = false  AND p.waiver_write_off=false " + ") "
				+ "SELECT year_group::text, payment_method, SUM(COALESCE(amount, 0)) AS total_amount "
				+ "FROM yearly_data " + "GROUP BY year_group, sort_order, payment_method "
				+ "HAVING SUM(COALESCE(amount, 0)) > 0 " + "ORDER BY sort_order, payment_method";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				PaymentDistribution pd = new PaymentDistribution();
				pd.setYear(rs.getString("year_group"));
				pd.setPaymentMethod(rs.getString("payment_method"));
				pd.setAmountCollected(rs.getBigDecimal("total_amount"));
				return pd;
			});
		} catch (DataAccessException e) {
			throw new RuntimeException("Failed to fetch yearly payment distribution by method for orgId: " + orgId
					+ " from " + dateFrom + " to " + dateTo, e);
		}
	}

	public List<BestPerformingDebts> getBestPerformingDebts(long orgId, Date dateFrom, Date dateTo) {
		String sql = "SELECT " + "    l.documentno AS debtReferenceNo, " + "    CASE "
				+ "        WHEN l.borrower_type = 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name) "
				+ "        WHEN l.borrower_type = 'GROUP' THEN g.group_name "
				+ "        WHEN l.borrower_type = 'INSTITUTION' THEN inst.institution_name " + "        ELSE 'Unknown' "
				+ "    END AS debtorName, " + "    l.approved_amount AS originalAmount, "
				+ "    l.expected_disbursement_date, "
				+ "    (COALESCE(l.interests_earned, 0) + COALESCE(l.penalty_earned, 0)) AS penaltiesAndInterests, "
				+ "    COALESCE(( " + "        SELECT SUM(p.amount) " + "        FROM AD_Payment p "
				+ "        WHERE p.AD_Loan_Application_ID = l.AD_Loan_Application_ID "
				+ "        AND p.is_paid = true AND p.payment_method NOT IN ('CREDIT_NOTE','WRITE_OFF','WAIVER') AND p.waiver_write_off=false AND p.wallet_deposit=false  "
				+ "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "    ), 0) AS payments, "
				+ "    l.balance AS outstandingBalance, "
				+ "    (CURRENT_DATE - l.expected_disbursement_date::date)::integer AS noOfDaysSinceDisbursement "
				+ "FROM AD_Loan_Application l " + "LEFT JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID "
				+ "LEFT JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID "
				+ "LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID "
				+ "WHERE l.isactive = true " + "    AND l.AD_Org_ID = :orgId "
				+ "    AND l.approvalstage IN ('APPROVED', 'COMPLETED') "
				+ "    AND l.expected_disbursement_date BETWEEN :dateFrom AND :dateTo " + "    AND EXISTS ( "
				+ "        SELECT 1 " + "        FROM AD_Payment p "
				+ "        WHERE p.AD_Loan_Application_ID = l.AD_Loan_Application_ID " + "        AND p.is_paid = true "
				+ "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "    ) "
				+ "ORDER BY l.approved_amount DESC"; // Simple initial sort

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			List<BestPerformingDebts> debts = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				BestPerformingDebts debt = new BestPerformingDebts();
				debt.setDebtReferenceNo(rs.getString("debtReferenceNo"));
				debt.setDebtorName(rs.getString("debtorName"));
				debt.setOriginalAmount(rs.getBigDecimal("originalAmount"));
				debt.setPenaltiesAndInterests(rs.getBigDecimal("penaltiesAndInterests"));
				debt.setPayments(rs.getBigDecimal("payments"));
				debt.setOutstandingBalance(rs.getBigDecimal("outstandingBalance"));

				BigDecimal totalLoanAmount = debt.getOriginalAmount().add(debt.getPenaltiesAndInterests());
				debt.setTotal(totalLoanAmount);

				if (debt.getPayments() == null || debt.getPayments().compareTo(BigDecimal.ZERO) == 0) {
					debt.setIndividualPerformance(BigDecimal.ZERO);
				} else {
					BigDecimal performance = debt.getPayments().multiply(new BigDecimal(100)).divide(totalLoanAmount,
							10, RoundingMode.HALF_UP);
					debt.setIndividualPerformance(performance.setScale(2, RoundingMode.HALF_UP));
				}

				debt.setNoOfDaysSinceDisbursement(rs.getInt("noOfDaysSinceDisbursement"));
				return debt;
			});
			// Sort in Java according to business logic
			debts.sort((d1, d2) -> {
			    // First by performance DESC
			    int perfCompare = d2.getIndividualPerformance().compareTo(d1.getIndividualPerformance());
			    if (perfCompare != 0)
			        return perfCompare;

			    // Then by days ASC (fewer days = better)
			    int daysCompare = Integer.compare(d1.getNoOfDaysSinceDisbursement(), d2.getNoOfDaysSinceDisbursement());
			    if (daysCompare != 0)
			        return daysCompare;

			    // Finally by payments DESC
			    return d2.getPayments().compareTo(d1.getPayments());
			});

			return debts;

		} catch (Exception e) {
			throw new RuntimeException(
					"Failed to fetch best performing debts for orgId: " + orgId + " from " + dateFrom + " to " + dateTo,
					e);
		}
	}
	
	
	
	
	
	
	public List<BestPerformingDebts> getBestPerformingDebtsByAllPayments(long orgId, Date dateFrom, Date dateTo) {
		String sql = "SELECT " + "    l.documentno AS debtReferenceNo, " + "    CASE "
				+ "        WHEN l.borrower_type = 'INDIVIDUAL' THEN CONCAT(d.first_name, ' ', d.last_name) "
				+ "        WHEN l.borrower_type = 'GROUP' THEN g.group_name "
				+ "        WHEN l.borrower_type = 'INSTITUTION' THEN inst.institution_name " + "        ELSE 'Unknown' "
				+ "    END AS debtorName, " + "    l.approved_amount AS originalAmount, "
				+ "    l.expected_disbursement_date, "
				+ "    (COALESCE(l.interests_earned, 0) + COALESCE(l.penalty_earned, 0)) AS penaltiesAndInterests, "
				+ "    COALESCE(( " + "        SELECT SUM(p.amount) " + "        FROM AD_Payment p "
				+ "        WHERE p.AD_Loan_Application_ID = l.AD_Loan_Application_ID "
				+ "        AND p.is_paid = true  AND p.waiver_write_off=false AND p.wallet_deposit=false "
				+ "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "    ), 0) AS payments, "
				+ "    l.balance AS outstandingBalance, "
				+ "    (CURRENT_DATE - l.expected_disbursement_date::date)::integer AS noOfDaysSinceDisbursement "
				+ "FROM AD_Loan_Application l " + "LEFT JOIN AD_Debtor d ON d.AD_Debtor_ID = l.AD_Debtor_ID "
				+ "LEFT JOIN AD_Group_Borrower g ON g.AD_Group_Borrower_ID = l.AD_Group_Borrower_ID "
				+ "LEFT JOIN AD_Institution_Borrower inst ON inst.AD_Institution_Borrower_ID = l.AD_Institution_Borrower_ID "
				+ "WHERE l.isactive = true " + "    AND l.AD_Org_ID = :orgId "
				+ "    AND l.approvalstage IN ('APPROVED', 'COMPLETED') "
				+ "    AND l.expected_disbursement_date BETWEEN :dateFrom AND :dateTo " + "    AND EXISTS ( "
				+ "        SELECT 1 " + "        FROM AD_Payment p "
				+ "        WHERE p.AD_Loan_Application_ID = l.AD_Loan_Application_ID " + "        AND p.is_paid = true "
				+ "        AND p.payment_date_time BETWEEN :dateFrom AND :dateTo " + "    ) "
				+ "ORDER BY l.approved_amount DESC"; // Simple initial sort

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId)
				.addValue("dateFrom", dateFrom).addValue("dateTo", dateTo);

		try {
			List<BestPerformingDebts> debts = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
				BestPerformingDebts debt = new BestPerformingDebts();
				debt.setDebtReferenceNo(rs.getString("debtReferenceNo"));
				debt.setDebtorName(rs.getString("debtorName"));
				debt.setOriginalAmount(rs.getBigDecimal("originalAmount"));
				debt.setPenaltiesAndInterests(rs.getBigDecimal("penaltiesAndInterests"));
				debt.setPayments(rs.getBigDecimal("payments"));
				debt.setOutstandingBalance(rs.getBigDecimal("outstandingBalance"));

				BigDecimal totalLoanAmount = debt.getOriginalAmount().add(debt.getPenaltiesAndInterests());
				debt.setTotal(totalLoanAmount);

				if (debt.getPayments() == null || debt.getPayments().compareTo(BigDecimal.ZERO) == 0) {
					debt.setIndividualPerformance(BigDecimal.ZERO);
				} else {
					BigDecimal performance = debt.getPayments().multiply(new BigDecimal(100)).divide(totalLoanAmount,
							10, RoundingMode.HALF_UP);
					debt.setIndividualPerformance(performance.setScale(2, RoundingMode.HALF_UP));
				}

				debt.setNoOfDaysSinceDisbursement(rs.getInt("noOfDaysSinceDisbursement"));
				return debt;
			});

			// Sort in Java according to your business logic
			debts.sort((d1, d2) -> {
			    // First by performance DESC
			    int perfCompare = d2.getIndividualPerformance().compareTo(d1.getIndividualPerformance());
			    if (perfCompare != 0)
			        return perfCompare;

			    // Then by days ASC (fewer days = better)
			    int daysCompare = Integer.compare(d1.getNoOfDaysSinceDisbursement(), d2.getNoOfDaysSinceDisbursement());
			    if (daysCompare != 0)
			        return daysCompare;

			    // Finally by payments DESC
			    return d2.getPayments().compareTo(d1.getPayments());
			});

			return debts;

		} catch (Exception e) {
			throw new RuntimeException(
					"Failed to fetch best performing debts for orgId: " + orgId + " from " + dateFrom + " to " + dateTo,
					e);
		}
	}

	public BigDecimal safeGetBigDecimal(ResultSet rs, String columnLabel) throws SQLException {
		BigDecimal val = rs.getBigDecimal(columnLabel);
		return val != null ? val : BigDecimal.ZERO;
	}

	public Integer safeGetInteger(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getObject(columnLabel) != null ? rs.getInt(columnLabel) : 0;
	}

	private <T> T defaultIfNull(T value, T defaultVal) {
		return value != null ? value : defaultVal;
	}

	public Page<MADMenu> getMenus(int page, int size, String search) {
		// TODO Auto-generated method stub
		if (search != null && !search.isEmpty()) {
			return menuRepository.searchActiveMenusByClientAndNameOrIcon(utils.getAD_Client_ID(), search,
					PageRequest.of(page, size));

		} else {
			return menuRepository.findByIsActiveAndAdClientIdOrderByMenuOrderAsc(true, utils.getAD_Client_ID(),
					PageRequest.of(page, size));
		}
	}

	public List<MADMenu> getAllActiveMenusList() {
		return menuRepository.findByIsActive(true);

	}

	public Page<MADSubMenu> getSuMenus(int page, int size, String search) {
		// TODO Auto-generated method stub
		if (search != null && !search.isEmpty()) {
			return subMenuRepository
					.findByIsActiveAndAdClientIdAndViewContainsIgnoreCaseOrTitleContainsIgnoreCaseOrSubMenuOrderContainsIgnoreCase(
							true, utils.getAD_Client_ID(), search, search, search, PageRequest.of(page, size));

		} else {
			return subMenuRepository.findByIsActiveAndAdClientIdOrderByIdAsc(true, utils.getAD_Client_ID(),
					PageRequest.of(page, size));
		}
	}

}
