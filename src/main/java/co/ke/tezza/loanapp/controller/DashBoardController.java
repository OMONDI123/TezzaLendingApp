package co.ke.tezza.loanapp.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.GsonBuilder;

import co.ke.tezza.loanapp.entity.MADMenu;
import co.ke.tezza.loanapp.entity.MADSubMenu;
import co.ke.tezza.loanapp.entity.MMenuRoleMapping;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.AccessMappingCopyRequest;
import co.ke.tezza.loanapp.model.DashBoard;
import co.ke.tezza.loanapp.model.Menu;
import co.ke.tezza.loanapp.model.MenuMapping;
import co.ke.tezza.loanapp.model.ReportParams;
import co.ke.tezza.loanapp.service.DashboardService;
import co.ke.tezza.loanapp.service.JasperReportingServices;
import co.ke.tezza.loanapp.util.HibernateProxyTypeAdapter;
import co.ke.tezza.loanapp.util.Utils;

@RestController
@RequestMapping("/dashboard")
public class DashBoardController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private Utils utils;

	@Autowired
	private DashboardService dashboardService;
	@Autowired private JasperReportingServices jasper;
	
	@PostMapping(value = "/printBestPerformingDebts", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','printBestPerformingDebts')")
	public String printBestPerformingDebts(@RequestBody ReportParams model, @RequestParam(value="netPayments",required = true,defaultValue = "true") boolean netPayments) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(jasper.generateBestPerformingDebtsReport(model, netPayments));
	}
	
	
	@PostMapping(value = "/printTopOverdueDebts", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','printTopOverdueDebts')")
	public String printTopOverdueDebts(@RequestBody ReportParams model, @RequestParam(value="byDays",required = true,defaultValue = "true") boolean byDays) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(jasper.generateTopOverdueDebtsReport(model, byDays));
	}

	@PostMapping(value = "/mappOrUpdateMenuMapping", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','mappOrUpdateMenuMapping')")
	public String mappOrUpdateMenuMapping(@RequestBody MenuMapping model) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(dashboardService.mappOrUpdateMenuMapping(model));
	}
	
	@PostMapping(value = "/copyMenuMapping", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','copyMenuMapping')")
	public String copyMenuMapping(@RequestBody AccessMappingCopyRequest model) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(dashboardService.copyMappedMenus(model));
	}

	@PostMapping(value = "/createUpdateMenu", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','createUpdateMenu')")
	public String createUpdateMenu(@RequestBody Menu model) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(dashboardService.createUpdateMenu(model));
	}
	
	

	@PostMapping(value = "/copyMenus", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','copyMenus')")
	public String copyMenus() {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(dashboardService.copyMenu());
	}

	@PutMapping(value = "/deleteMenuMapping/{menuMappingId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','deleteMenuMapping/{menuMappingId}')")
	public String deleteMenuMapping(@PathVariable("menuMappingId") Long menuMappingId) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(dashboardService.deleteMappedmenu(menuMappingId));
	}

	@PutMapping(value = "/deleteMenu/{AD_Menu_ID}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','deleteMenu/{AD_Menu_ID}')")
	public String deleteMenu(@PathVariable("AD_Menu_ID") Long AD_Menu_ID) {
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(dashboardService.deleteMenu(AD_Menu_ID));
	}

	@GetMapping(value = "/getMappedMenus/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','getMappedMenus/{page}/{size}')")
	public String getMappedMenus(@PathVariable("page") int page, @PathVariable("size") int size,
			@QueryParam("search") String search) {
		Page<MMenuRoleMapping> menuMapping = dashboardService.getMappedMenus(page, size, search);
		logger.debug("Called DashBoardController.getMappedMenus");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(menuMapping);
	}

	@GetMapping(value = "/getAllMenusByOrganization/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','getAllMenusByOrganization/{page}/{size}')")
	public String getAllMenus(@PathVariable("page") int page, @PathVariable("size") int size,
			@QueryParam("search") String search) {
		Page<MADMenu> menus = dashboardService.getMenus(page, size, search);
		logger.debug("Called DashBoardController.getAllMenus");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create().toJson(menus);
	}

	@GetMapping(value = "/getAllSubMenus/{page}/{size}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','getAllSubMenus/{page}/{size}')")
	public String getAllSubMenus(@PathVariable("page") int page, @PathVariable("size") int size,
			@QueryParam("search") String search) {
		Page<MADSubMenu> subMenus = dashboardService.getSuMenus(page, size, search);
		logger.debug("Called DashBoardController.getAllSubMenus");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(subMenus);
	}
	
	
	@GetMapping(value = "/getAllActiveMenus", produces = MediaType.APPLICATION_JSON_VALUE)
	//@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','getAllActiveMenus')")
	public String getAllActiveMenus() {
		
		logger.debug("Called DashBoardController.getAllActiveMenus");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(dashboardService.getAllActiveMenusList());
	}

	@GetMapping(value = "/getMappedMenusByRoleId", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','getMappedMenusByRoleId')")
	public String getMappedMenusByRoleId() {
		List<MMenuRoleMapping> menuMapping = dashboardService.getMappedMenusByRoleId();
		logger.debug("Called DashBoardController.getMappedMenusByRoleId");
		return new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create()
				.toJson(menuMapping);
	}

	@GetMapping(value = "/getDashBoardStatistics", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','getDashBoardStatistics')")
	public String getDashBoardStatistics(
	        @RequestParam(required = false) String dateFrom,
	        @RequestParam(required = false) String dateTo) {
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	    if (dateFrom == null) {
	        LocalDate firstDay = LocalDate.of(LocalDate.now().getYear(), 1, 1);
	        dateFrom = sdf.format(Date.from(firstDay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
	    }

	    if (dateTo == null) {
	        LocalDateTime endOfYear = LocalDate.of(LocalDate.now().getYear(), 12, 31).atTime(LocalTime.MAX);
	        dateTo = sdf.format(Date.from(endOfYear.atZone(ZoneId.systemDefault()).toInstant()));
	    }

	    DashBoard dashBoard = dashboardService.getDashBoardStatistics(dateFrom, dateTo);

	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(dashBoard);
	}
	
	@GetMapping(value = "/getAdminDashBoardStatistics", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("@securityControllAccessService.hasAccessToAPIEndpoint('DashBoardController','getAdminDashBoardStatistics')")
	public String getAdminDashBoardStatistics(@RequestParam(required = false, value = "AD_Org_ID") Long AD_Org_ID,
	        @RequestParam(required = false,value = "dateFrom") String dateFrom,
	        @RequestParam(required = false,value = "dateTo") String dateTo) {
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	    if (dateFrom == null) {
	        LocalDate firstDay = LocalDate.of(LocalDate.now().getYear(), 1, 1);
	        dateFrom = sdf.format(Date.from(firstDay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
	    }

	    if (dateTo == null) {
	        LocalDateTime endOfYear = LocalDate.of(LocalDate.now().getYear(), 12, 31).atTime(LocalTime.MAX);
	        dateTo = sdf.format(Date.from(endOfYear.atZone(ZoneId.systemDefault()).toInstant()));
	    }

	    DashBoard dashBoard = dashboardService.getAdminDashBoardStatistics(dateFrom, dateTo,AD_Org_ID);

	    return new GsonBuilder()
	            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
	            .create()
	            .toJson(dashBoard);
	}



	
}
