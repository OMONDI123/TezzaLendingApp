package co.ke.tezza.loanapp.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.sql.DataSource;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import co.ke.tezza.loanapp.entity.MADSysConfig;
import co.ke.tezza.loanapp.entity.MClient;
import co.ke.tezza.loanapp.entity.MEmail;
import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.entity.MOrg;
import co.ke.tezza.loanapp.entity.MRemindersConfiguration;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MSMSConfig;
import co.ke.tezza.loanapp.entity.MSms;
import co.ke.tezza.loanapp.entity.MSmsSetup;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.entity.MUserClientAudit;
import co.ke.tezza.loanapp.entity.MWFMail;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.enums.MessageStatus;
import co.ke.tezza.loanapp.enums.RelationShipEnum;
import co.ke.tezza.loanapp.enums.SettingCategoriesEnum;
import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import co.ke.tezza.loanapp.enums.SupportedClientsEnum;
import co.ke.tezza.loanapp.model.DBConnect;
import co.ke.tezza.loanapp.model.FileUploads;
import co.ke.tezza.loanapp.model.Guarantors;
import co.ke.tezza.loanapp.repository.MADSysConfigRepository;
import co.ke.tezza.loanapp.repository.MEmailRepository;
import co.ke.tezza.loanapp.repository.MOrgRepository;
import co.ke.tezza.loanapp.repository.MUserClientAuditRepository;
import co.ke.tezza.loanapp.repository.MWFMailRepository;
import co.ke.tezza.loanapp.repository.MclientRepository;
import co.ke.tezza.loanapp.repository.ReminderConfigRepository;
import co.ke.tezza.loanapp.repository.SmsConfigRepository;
import co.ke.tezza.loanapp.repository.SmsRepository;
import co.ke.tezza.loanapp.repository.SmsSetupRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.response.OrgResponse;
import co.ke.tezza.loanapp.response.RoleResponse;
import co.ke.tezza.loanapp.response.User;
import co.ke.tezza.loanapp.response.UserResponse;
import co.ke.tezza.loanapp.web.security.SpringSecurityAuditorAware;

import org.apache.pdfbox.Loader;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Utils {
//	@Value("${documentsgetOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS).getDocumentUploadDir()}")
//	private String getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS).getDocumentUploadDir();
//
//	@Value("${filePath}")
//	private String filePath;

	@Autowired
	private SpringSecurityAuditorAware auditorAware;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MUserClientAuditRepository auditRepository;

	@Autowired
	private MADSysConfigRepository sysConfigRepository;

	@Autowired
	private MclientRepository mclientRepository;
	@Autowired
	private MOrgRepository mOrgRepository;

	@Autowired
	private MWFMailRepository mailRepository;

	@Autowired
	private MEmailRepository emailRepository;
	@Autowired
	private SmsConfigRepository smsConfigRepository;

	@Autowired
	private SmsRepository smsRepository;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	@Autowired
	private ReminderConfigRepository reminderConfigRepository;
	@Autowired
	private SmsSetupRepository smsSetupRepository;
	@Autowired
	private DataSource dataSource;
	@Autowired
	private AfricasTalkingSmsUtil africasTalkingSmsUtil;

	@Autowired
	private AdvantaSmsUtils advantaSmsUtils;

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom random = new SecureRandom();

	private final String CHARACTERS_CODE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private final int CODE_LENGTH = 12;

	private static final LocalTime MORNING_START = LocalTime.of(6, 0);
	private static final LocalTime MORNING_END = LocalTime.of(8, 0);
	private static final LocalTime EVENING_START = LocalTime.of(17, 30);
	private static final LocalTime EVENING_END = LocalTime.of(21, 30);

	public String encryptPdfWithPassword(String inputPath, String userPassword) throws IOException {
		File inputFile = new File(inputPath);
		if (!inputFile.exists()) {
			throw new IOException("PDF file not found: " + inputPath);
		}

		String ownerPassword = "Bunde@#&20260330!!!*";
		AccessPermission ap = new AccessPermission();
		ap.setCanPrint(true);
		ap.setCanExtractContent(true);

		StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
		policy.setEncryptionKeyLength(256);

		File tempFile = File.createTempFile("encrypted_", ".pdf");
		try (PDDocument document = Loader.loadPDF(inputFile)) {
			document.protect(policy);
			document.save(tempFile);
		}

		if (!inputFile.delete()) {
			throw new IOException("Could not delete original file: " + inputPath);
		}
		if (!tempFile.renameTo(inputFile)) {
			throw new IOException("Could not rename temp file to original: " + inputPath);
		}

		return inputPath;
	}

	public String getBalance(long orgId) {
		try {
			MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(orgId, true);

			if (config == null) {
				return "No SMS configuration found";
			}

			switch (config.getSmsProvider()) {

			case ADVANTA:
				return advantaSmsUtils.getSmsBalanceAdvanta(orgId);

			case AFRICAS_TALKING:
				return africasTalkingSmsUtil.getBalance(orgId);

			default:
				return "Unsupported provider: " + config.getSmsProvider();
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "Error: " + e.getMessage();
		}
	}

	public void sendBulkSms(MSms sms) {
		try {
			MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(sms.getAdOrgID(),
					true);

			if (config == null) {
				failSms(sms, "No SMS configuration found");
				return;
			}

			switch (config.getSmsProvider()) {

			case ADVANTA:
				advantaSmsUtils.sendBulkSmsAdvanta(sms);
				break;

			case AFRICAS_TALKING:
				africasTalkingSmsUtil.sendBulkSms(sms); // modern JSON bulk
				break;

			default:
				failSms(sms, "Unsupported provider: " + config.getSmsProvider());
			}

		} catch (Exception e) {
			e.printStackTrace();
			failSms(sms, "Exception: " + e.getMessage());
		}
	}

	public void sendSms(MSms sms) {
		try {
			MSMSConfig config = smsConfigRepository.findTop1ByAdOrgIDAndIsActiveOrderBySmsConfigIdDesc(sms.getAdOrgID(),
					true);

			if (config == null) {
				failSms(sms, "No SMS configuration found");
				return;
			}

			switch (config.getSmsProvider()) {

			case ADVANTA:
				advantaSmsUtils.sendSmsAdvanta(sms);
				break;

			case AFRICAS_TALKING:
				africasTalkingSmsUtil.sendSingleSms(sms); // legacy/single
				break;

			default:
				failSms(sms, "Unsupported provider: " + config.getSmsProvider());
			}

		} catch (Exception e) {
			e.printStackTrace();
			failSms(sms, "Exception: " + e.getMessage());
		}
	}

	private void failSms(MSms sms, String reason) {
		sms.setDocStatus(DocStatus.REJECTED);
		sms.setMessageStatus(MessageStatus.FAILED);
		sms.setApprovalStage(ApprovalStage.CANCELLED);
		sms.setReason(reason);
		sms.setResponseCode("0");
		smsRepository.save(sms);
	}

	public static boolean isWithinBidTime() {
		LocalTime now = LocalTime.now(ZoneId.of("Africa/Nairobi"));

		boolean inMorningWindow = !now.isBefore(MORNING_START) && !now.isAfter(MORNING_END);
		boolean inEveningWindow = !now.isBefore(EVENING_START) && !now.isAfter(EVENING_END);

		return inMorningWindow || inEveningWindow;
	}

	public LocalDateTime getNextDayNineAM() {
		LocalDateTime now = LocalDateTime.now();

		// If current time is after 9:00 PM (21:00), schedule for next day 9:00 AM
		if (now.getHour() >= 21) {
			return now.toLocalDate().plusDays(1).atTime(9, 0);
		}

		// Otherwise send immediately (or return current time)
		return now;
	}

	public Date get100YearDownTheLine() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 100);

		return cal.getTime();

	}

	public Date get100YearUpline() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -100);
		return cal.getTime();
	}

	public String getDefaultUserRole() {
		DBConnect connect = getDBConnect();
		if (connect != null) {
			switch (connect.getClient()) {
			case MEMBERSHIP:
				return "ROLE_MEMBER";
			case SMART_DEBT:
				return "ROLE_DEBTOR";
			default:
				return "ROLE_USER";

			}

		}
		return null;
	}

	public DBConnect getDBConnect() {
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData metaData = conn.getMetaData();

			String dbProduct = metaData.getDatabaseProductName();
			String dbVersion = metaData.getDatabaseProductVersion();
			String url = metaData.getURL();
			String username = metaData.getUserName();

			DBConnect connect = new DBConnect();
			connect.setUrl(url);
			connect.setUserName(username);

			// Determine client based on the database username
			switch (username) {
			case "smartdebtsuser":
				connect.setClient(SupportedClientsEnum.SMART_DEBT);
				break;
			case "membershipappuser":
				connect.setClient(SupportedClientsEnum.MEMBERSHIP);
				break;
			default:
				connect.setClient(null); // Unknown user – client remains null
				break;
			}
			return connect;

		} catch (SQLException e) {
			e.printStackTrace();
			return null; // Connection failed
		}
	}

	/**
	 * Get guarantors for an individual borrower
	 */
	public List<Guarantors> getGuarantorsForIndividualBorrower(Long debtorId) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("debtorId", debtorId);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT g.full_name, g.phone_number,g.email ");
		sql.append("FROM AD_Loan_Application l ");
		sql.append("INNER JOIN AD_Borrower_Guarantors lg ON lg.AD_Loan_Application_ID = l.AD_Loan_Application_ID ");
		sql.append("INNER JOIN AD_Next_Of_Kin g ON g.AD_Next_Of_Kin_ID = lg.AD_Guarantor_ID ");
		sql.append("WHERE l.AD_Debtor_ID = :debtorId ");

		return namedParameterJdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) -> {
			Guarantors guarantor = new Guarantors();
			guarantor.setFullName(rs.getString("full_name"));
			guarantor.setPhoneNumber(rs.getString("phone_number"));
			guarantor.setEmail(rs.getString("email"));

			return guarantor;
		});
	}

	/**
	 * Get guarantors for a group borrower
	 */
	public List<Guarantors> getGuarantorsForGroupBorrower(Long groupBorrowerId) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("groupBorrowerId", groupBorrowerId);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT g.full_name, g.phone_number,g.email  ");
		sql.append("FROM AD_Loan_Application l ");
		sql.append("INNER JOIN AD_Borrower_Guarantors lg ON lg.AD_Loan_Application_ID = l.AD_Loan_Application_ID ");
		sql.append("INNER JOIN AD_Next_Of_Kin g ON g.AD_Next_Of_Kin_ID = lg.AD_Guarantor_ID ");
		sql.append("WHERE l.AD_Group_Borrower_ID = :groupBorrowerId ");

		return namedParameterJdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) -> {
			Guarantors guarantor = new Guarantors();
			guarantor.setFullName(rs.getString("full_name"));
			guarantor.setPhoneNumber(rs.getString("phone_number"));
			guarantor.setEmail(rs.getString("email"));
			return guarantor;
		});
	}

	/**
	 * Get guarantors for an institution borrower
	 */
	public List<Guarantors> getGuarantorsForInstitutionBorrower(Long institutionId) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("institutionId", institutionId);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT g.full_name, g.phone_number,g.email ");
		sql.append("FROM AD_Loan_Application l ");
		sql.append("INNER JOIN AD_Borrower_Guarantors lg ON lg.AD_Loan_Application_ID = l.AD_Loan_Application_ID ");
		sql.append("INNER JOIN AD_Next_Of_Kin g ON g.AD_Next_Of_Kin_ID = lg.AD_Guarantor_ID ");
		sql.append("WHERE l.AD_Institution_ID = :institutionId ");

		return namedParameterJdbcTemplate.query(sql.toString(), parameters, (rs, rowNum) -> {
			Guarantors guarantor = new Guarantors();
			guarantor.setFullName(rs.getString("full_name"));
			guarantor.setPhoneNumber(rs.getString("phone_number"));
			guarantor.setEmail(rs.getString("email"));
			return guarantor;
		});
	}

	/**
	 * Optional: Unified method to get guarantors based on borrower type
	 */
	public List<Guarantors> getGuarantorsByBorrowerType(String borrowerType, Long borrowerId) {
		switch (borrowerType) {
		case "INDIVIDUAL":
			return getGuarantorsForIndividualBorrower(borrowerId);
		case "GROUP":
			return getGuarantorsForGroupBorrower(borrowerId);
		case "INSTITUTION":
			return getGuarantorsForInstitutionBorrower(borrowerId);
		default:
			return List.of();
		}
	}

	public String formaatDateForDisplayString(Date date) {
		if (date == null)
			return "";

		// Step 1: Base date formatting parts
		SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEEE"); // Sunday
		SimpleDateFormat dayNumberFormat = new SimpleDateFormat("d"); // 23
		SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy"); // November 2025
		SimpleDateFormat time24Format = new SimpleDateFormat("HH:mm"); // 18:19
		SimpleDateFormat time12Format = new SimpleDateFormat("hh:mm a"); // 06:19 PM

		String dayName = dayNameFormat.format(date);
		String dayNumber = dayNumberFormat.format(date);
		String suffix = getDayOfMonthSuffix(Integer.parseInt(dayNumber));
		String monthYear = monthYearFormat.format(date);
		String time24 = time24Format.format(date);
		String time12 = time12Format.format(date);

		return String.format("%s %s%s %s %s / %s", dayName, dayNumber, suffix, monthYear, time24, time12);
	}

	// Helper function to get 1st, 2nd, 3rd, 4th etc.
	private static String getDayOfMonthSuffix(int n) {
		if (n >= 11 && n <= 13) {
			return "th";
		}
		switch (n % 10) {
		case 1:
			return "st";
		case 2:
			return "nd";
		case 3:
			return "rd";
		default:
			return "th";
		}
	}

	public LocalDateTime getNextNineThirty() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime todayNineThirty = now.toLocalDate().atTime(9, 30);

		if (now.isBefore(todayNineThirty)) {
			return todayNineThirty;
		}

		return todayNineThirty.plusDays(1);
	}

	public LocalDateTime getNextElevenThirty() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime todayNineThirty = now.toLocalDate().atTime(11, 30);

		if (now.isBefore(todayNineThirty)) {
			return todayNineThirty;
		}

		return todayNineThirty.plusDays(1);
	}

	public LocalDateTime getNextFourThirtyEvening() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime todayNineThirty = now.toLocalDate().atTime(16, 30);

		if (now.isBefore(todayNineThirty)) {
			return todayNineThirty;
		}

		return todayNineThirty.plusDays(1);
	}

	public LocalDateTime getNextTenThirtyMorning() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime todayNineThirty = now.toLocalDate().atTime(10, 30);

		if (now.isBefore(todayNineThirty)) {
			return todayNineThirty;
		}

		return todayNineThirty.plusDays(1);
	}

	public LocalDateTime getNextEightThirtyMorning() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime todayNineThirty = now.toLocalDate().atTime(8, 30);

		if (now.isBefore(todayNineThirty)) {
			return todayNineThirty;
		}

		return todayNineThirty.plusDays(1);
	}

	public OrgResponse mapOrganisation(MOrg org) {
		if (org == null) {
			return null;
		}
		OrgResponse response = new OrgResponse();
		response.setId(org.getId());
		response.setDescription(org.getDescription());
		response.setName(org.getName());
		response.setKraPin(org.getKraPin());
		response.setBoxNo(org.getBoxNo());
		response.setCity(org.getCity());
		response.setCounty(org.getCounty());
		response.setPhysicalAddress(org.getPhysicalAddress());
		response.setLandMark(org.getLandMark());
		response.setStreet(org.getStreet());
		response.setLocation(org.getLocation());
		return response;

	}

	public String getMessage(long orgId, SmsTypeEnum type) {
		String message = null;
		MRemindersConfiguration config = reminderConfigRepository
				.findTop1ByAdOrgIDAndIsActiveAndSmsMessageTemplate_SmsTypeOrderByReminderIdDesc(orgId, true, type);
		if (config != null) {
			if (config.getSmsMessageTemplate() != null) {
				message = config.getSmsMessageTemplate().getMessageTemplate();
			}

		} else {
			MSmsSetup smsTemplate = smsSetupRepository
					.findTop1ByAdOrgIDAndIsActiveAndSmsTypeOrderBySmsSetupIdDesc(orgId, true, type);
			if (smsTemplate != null) {
				message = smsTemplate.getMessageTemplate();
			}
		}
		if (message == null) {
			message = getDefaultMessage(type);
		}

		return message;

	}

	public String getDefaultMessage(SmsTypeEnum type) {
		switch (type) {
		// ========== WALLET DEPOSITS & WITHDRAWALS ==========

		case WALLET_DEPOSIT_NOTIFICATION:
			return "Dear {username}, a deposit of {amount} has been initiated to your wallet. Payment method: {paymentMethod}. Reference: {reference}. Status: {status}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_WITHDRAWAL_NOTIFICATION:
			return "Dear {username}, a withdrawal of {amount} has been initiated from your wallet. Withdrawal method: {withdrawalMethod}. Destination: {destinationAccount}. Reference: {reference}. Status: {status}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}. Reason: {reason}.";

		case WALLET_DEPOSIT_SUCCESS:
			return "Dear {username}, your wallet deposit of {amount} was successful on {depositDate}. Payment method: {paymentMethod}. Transaction ID: {transactionId}. Reference: {reference}. Previous balance: {previousBalance}. New balance: {newBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_WITHDRAWAL_SUCCESS:
			return "Dear {username}, your wallet withdrawal of {amount} was successful on {withdrawalDate}. Withdrawal method: {withdrawalMethod}. Transaction ID: {transactionId}. Reference: {reference}. Destination: {destinationAccount}. Previous balance: {previousBalance}. New balance: {newBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_DEPOSIT_FAILED:
			return "Dear {username}, your wallet deposit of {amount} failed on {attemptDate}. Payment method: {paymentMethod}. Reference: {reference}. Failure reason: {failureReason}. Please {retryAction}. Contact support: {supportContact}.";

		case WALLET_WITHDRAWAL_FAILED:
			return "Dear {username}, your wallet withdrawal of {amount} failed on {attemptDate}. Withdrawal method: {withdrawalMethod}. Reference: {reference}. Failure reason: {failureReason}. Please {retryAction}. Contact support: {supportContact}.";

		case WALLET_LOW_BALANCE_ALERT:
			return "Dear {username}, your wallet balance ({currentBalance}) is below the minimum required ({minimumBalance}). Shortfall: {shortfallAmount}. Alert date: {alertDate}. Please top up your wallet: {topUpLink}. Membership No: {membershipAccountNo}.";

		case WALLET_BALANCE_STATEMENT:
			return "Dear {username}, your wallet balance statement as at {statementDate}. Current balance: {currentBalance}. Total deposits: {totalDeposits}. Total withdrawals: {totalWithdrawals}. Available balance: {availableBalance}. Pending transactions: {pendingTransactions}. Membership No: {membershipAccountNo}.";

		case WALLET_AUTO_DEBIT_SETUP:
			return "Dear {username}, auto-debit has been set up for your wallet. Amount: {autoDebitAmount}. Billing date: {billingDate}. Payment method: {paymentMethod}. Effective date: {effectiveDate}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_AUTO_DEBIT_SUCCESS:
			return "Dear {username}, auto-debit of {amount} was successful on {debitDate}. Purpose: {purpose}. Transaction ID: {transactionId}. Reference: {reference}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_AUTO_DEBIT_FAILED:
			return "Dear {username}, auto-debit of {amount} failed on {attemptDate}. Purpose: {purpose}. Failure reason: {failureReason}. Current balance: {currentBalance}. Required balance: {requiredBalance}. Retry date: {retryDate}. Action required: {actionRequired}. Membership No: {membershipAccountNo}.";

		case WALLET_TRANSACTION_REVERSAL:
			return "Dear {username}, transaction {originalTransactionId} has been reversed. Reversal amount: {reversalAmount}. Reversal date: {reversalDate}. Reason: {reversalReason}. Previous balance: {previousBalance}. Current balance: {currentBalance}. Reference: {reference}. Membership No: {membershipAccountNo}.";

		case WALLET_REFUND_NOTIFICATION:
			return "Dear {username}, a refund of {refundAmount} has been processed to your wallet. Refund date: {refundDate}. Original transaction: {originalTransactionId}. Reason: {refundReason}. Wallet balance: {walletBalance}. Expected date: {expectedDate}. Reference: {reference}. Membership No: {membershipAccountNo}.";

		case WALLET_DEPOSIT_VERIFICATION:
			return "Dear {username}, please verify your wallet deposit of {amount} made on {depositDate}. Payment method: {paymentMethod}. Transaction ID: {transactionId}. Verification code: {verificationCode}. Verification link: {verificationLink}. Expires: {expiryTime}. Membership No: {membershipAccountNo}.";

		case WALLET_WITHDRAWAL_VERIFICATION:
			return "Dear {username}, please verify your wallet withdrawal request of {amount} made on {requestDate}. Withdrawal method: {withdrawalMethod}. Destination: {destinationAccount}. Verification code: {verificationCode}. Verification link: {verificationLink}. Expires: {expiryTime}. Membership No: {membershipAccountNo}.";

		case WALLET_DAILY_SUMMARY:
			return "Dear {username}, wallet summary for {summaryDate}. Opening balance: {openingBalance}. Credits: {totalCredits}. Debits: {totalDebits}. Closing balance: {closingBalance}. Transactions: {transactionCount}. Membership No: {membershipAccountNo}.";

		case WALLET_WEEKLY_SUMMARY:
			return "Dear {username}, wallet summary for week {weekStartDate} to {weekEndDate}. Total deposits: {totalDeposits}. Total withdrawals: {totalWithdrawals}. Average balance: {averageBalance}. Closing balance: {closingBalance}. Transactions: {transactionCount}. Membership No: {membershipAccountNo}.";

		case WALLET_MONTHLY_SUMMARY:
			return "Dear {username}, wallet summary for {month}. Total deposits: {totalDeposits}. Total withdrawals: {totalWithdrawals}. Highest balance: {highestBalance}. Lowest balance: {lowestBalance}. Average balance: {averageBalance}. Closing balance: {closingBalance}. Interest earned: {interestEarned}. Transactions: {transactionCount}. Membership No: {membershipAccountNo}.";

		case WALLET_LIMIT_EXCEEDED:
			return "Dear {username}, transaction limit exceeded. Attempted amount: {attemptedAmount}. Daily limit: {dailyLimit}. Transaction type: {transactionType}. Current daily total: {currentDailyTotal}. Remaining limit: {remainingLimit}. Reset time: {resetTime}. Membership No: {membershipAccountNo}.";

		case WALLET_FRAUD_ALERT:
			return "Dear {username}, suspicious activity detected on your wallet. Alert type: {alertType}. Transaction amount: {transactionAmount}. Time: {transactionTime}. Location: {location}. Device: {deviceInfo}. Action required: {actionRequired}. Contact support: {supportContact}. Membership No: {membershipAccountNo}.";

		case WALLET_PIN_CHANGE_CONFIRMATION:
			return "Dear {username}, your wallet PIN was changed on {changeDate}. Device: {deviceInfo}. IP address: {ipAddress}. If you did not make this change, please {actionRequired}. Contact support: {supportContact}. Membership No: {membershipAccountNo}.";

		case WALLET_KYC_REMINDER:
			return "Dear {username}, please update your KYC information by {dueDate}. Days remaining: {daysRemaining}. Required documents: {requiredDocuments}. Verification link: {verificationLink}. Consequences of delay: {consequencesOfDelay}. Membership No: {membershipAccountNo}.";

		case WALLET_ACCOUNT_UPGRADE:
			return "Dear {username}, your wallet account has been upgraded from {oldTier} to {newTier} on {upgradeDate}. New benefits: {newBenefits}. New limits: {newLimits}. Membership No: {membershipAccountNo}.";

		case WALLET_FEE_CHARGE_NOTIFICATION:
			return "Dear {username}, a fee of {amount} has been charged to your wallet. Fee type: {feeType}. Charge date: {chargeDate}. Description: {description}. Transaction ID: {transactionId}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_INTEREST_CREDITED:
			return "Dear {username}, interest of {interestAmount} has been credited to your wallet. Period: {period}. Interest rate: {interestRate}. Credit date: {creditDate}. Previous balance: {previousBalance}. New balance: {newBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_PROMOTIONAL_CREDIT:
			return "Dear {username}, promotional credit of {creditAmount} has been added to your wallet. Promotion: {promotionName}. Credit date: {creditDate}. Expiry date: {expiryDate}. Terms: {terms}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_SCHEDULED_TRANSACTION:
			return "Dear {username}, a scheduled {transactionType} of {amount} has been set up. Scheduled date: {scheduledDate}. Frequency: {frequency}. Reference: {reference}. Status: {status}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_SCHEDULED_TRANSACTION_SUCCESS:
			return "Dear {username}, your scheduled {transactionType} of {amount} was executed successfully on {executionDate}. Transaction ID: {transactionId}. Reference: {reference}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_SCHEDULED_TRANSACTION_FAILED:
			return "Dear {username}, your scheduled {transactionType} of {amount} failed on {scheduledDate}. Failure reason: {failureReason}. Current balance: {currentBalance}. Required balance: {requiredBalance}. Next attempt: {nextAttemptDate}. Action required: {actionRequired}. Membership No: {membershipAccountNo}.";

		case WALLET_BONUS_CREDITED:
			return "Dear {username}, a bonus of {bonusAmount} has been credited to your wallet. Bonus type: {bonusType}. Credit date: {creditDate}. Reason: {reason}. Valid until: {validUntil}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_CASHBACK_CREDITED:
			return "Dear {username}, cashback of {cashbackAmount} has been credited to your wallet. Original transaction: {originalTransactionId}. Cashback rate: {cashbackRate}. Credit date: {creditDate}. Wallet balance: {walletBalance}. Membership No: {membershipAccountNo}.";

		case WALLET_PENDING_TRANSACTION_REMINDER:
			return "Dear {username}, you have a pending {transactionType} of {amount}. Initiated on: {initiatedDate}. Pending reason: {pendingReason}. Complete here: {completionLink}. Expires: {expiryDate}. Membership No: {membershipAccountNo}.";

		case WALLET_INACTIVITY_ALERT:
			return "Dear {username}, your wallet has been inactive since {lastActivityDate}. Days inactive: {daysInactive}. Inactivity fee: {inactivityFee}. Reactivation offer: {reactivationOffer}. Dormancy date: {dormancyDate}. Membership No: {membershipAccountNo}.";

		case WALLET_STATEMENT_READY:
			return "Dear {username}, your wallet statement for {period} is ready. Statement date: {statementDate}. Opening balance: {openingBalance}. Closing balance: {closingBalance}. Download: {downloadLink}. Membership No: {membershipAccountNo}.";

		case WALLET_SECURITY_ALERT:
			return "Dear {username}, security alert for your wallet. Alert type: {alertType}. Alert time: {alertTime}. Affected amount: {affectedAmount}. Recommended action: {recommendedAction}. Reference: {reference}. Contact support: {supportContact}. Membership No: {membershipAccountNo}.";

		case WALLET_DEVICE_REGISTERED:
			return "Dear {username}, a new device has been registered to your wallet. Device name: {deviceName}. Registration date: {registrationDate}. Device info: {deviceInfo}. Location: {location}. If you did not authorize this, please {actionRequired}. Membership No: {membershipAccountNo}.";

		case WALLET_DEVICE_REMOVED:
			return "Dear {username}, device '{deviceName}' was removed from your wallet on {removalDate}. Reason: {reason}. Contact support if you did not authorize this: {supportContact}. Membership No: {membershipAccountNo}.";

		case WALLET_MAINTENANCE_NOTICE:
			return "Dear {username}, wallet maintenance scheduled from {startTime} to {endTime}. Impacted services: {impactedServices}. Alternative channels: {alternativeChannels}. Contact support: {supportContact}. Membership No: {membershipAccountNo}.";

		case WALLET_SYSTEM_UPDATE:
			return "Dear {username}, wallet system update on {updateDate}. New features: {newFeatures}. Improvements: {improvements}. Release notes: {releaseNotes}. Action required: {actionRequired}. Membership No: {membershipAccountNo}.";
		// ========== INVOICE NOTIFICATIONS ==========
		case INVOICE_GENERATED_NOTIFICATION:
			return "Dear {username}, your invoice {invoiceNo} has been generated. Amount: {amount} {currency}. Due date: {dueDate}. Bill type: {billType}. Download: {attachMentDownloadUrl}.";

		case INVOICE_DUE_REMINDER:
			return "Dear {username}, invoice {invoiceNo} of {amount} is due on {dueDate}. Days remaining: {daysRemaining}. Balance: {balance}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_OVERDUE_REMINDER:
			return "Dear {username}, invoice {invoiceNo} of {amount} is overdue by {daysOverdue} days. Penalty: {penaltyAmount}. Balance: {balance}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_PARTIAL_PAYMENT:
			return "Dear {username}, we received a partial payment of {amountPaid} for invoice {invoiceNo}. Remaining balance: {remainingBalance}. Payment date: {paymentDate}. Method: {paymentMethod}. Transaction ID: {transactionId}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_FULL_PAYMENT:
			return "Dear {username}, we received full payment of {amountPaid} for invoice {invoiceNo}. Payment date: {paymentDate}. Method: {paymentMethod}. Transaction ID: {transactionId}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_PAYMENT_REMINDER:
			return "Dear {username}, this is a reminder that invoice {invoiceNo} of {amount} is due on {dueDate}. Days remaining: {daysRemaining}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_GRACE_PERIOD_EXPIRY_ALERT:
			return "Dear {username}, the grace period for invoice {invoiceNo} ends on {graceEndDate}. Days remaining: {daysRemaining}. Amount: {amount}. Penalty rate: {penaltyRate}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_ADJUSTMENT_NOTIFICATION:
			return "Dear {username}, invoice {invoiceNo} amount has been adjusted from {oldAmount} to {newAmount} on {adjustmentDate}. Reason: {reason}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_CREDIT_NOTE_NOTIFICATION:
			return "Dear {username}, a credit note {creditNoteNo} of {creditAmount} has been issued against invoice {invoiceNo} on {creditDate}. Reason: {reason}. Bill type: {billType}. Remaining balance: {remainingBalance}.";

		case INVOICE_CLOSURE_NOTIFICATION:
			return "Dear {username}, invoice {invoiceNo} has been closed on {closureDate}. Total paid: {totalPaid}. Settlement amount: {settlementAmount}. Bill type: {billType}. Outstanding balance: {outstandingBalance}.";

		case INVOICE_WRITE_OFF_NOTIFICATION:
			return "Dear {username}, an amount of {writeOffAmount} has been written off for invoice {invoiceNo}. Reason: {writeOffReason}. Date: {writeOffDate}. Approved by: {approvedBy}. Bill type: {billType}. Remaining balance: {remainingBalance}.";

		case INTEREST_WAIVER_NOTIFICATION:
			return "Dear {username}, your interest of {waivedInterestAmount} on loan {documentNo} has been waived. Reason: {interestWaiverReason}. Approved by: {approvedBy}. Outstanding balance: {outstandingBalance}.";

		case INTEREST_WRITE_OFF_NOTIFICATION:
			return "Dear {username}, interest of {writtenOffInterestAmount} on loan {documentNo} has been written off. Reason: {writeOffReason}. Date: {writeOffDate}. Approved by: {approvedBy}. Outstanding balance: {outstandingBalance}.";

		case PENALTY_WRITE_OFF_NOTIFICATION:
			return "Dear {username}, penalty of {writtenOffPenaltyAmount} on loan {documentNo} has been written off. Reason: {writeOffReason}. Date: {writeOffDate}. Approved by: {approvedBy}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_INTEREST_WRITE_OFF_NOTIFICATION:
			return "Dear {guarantorName}, interest of {writtenOffInterestAmount} on loan {documentNo} (borrower: {borrowerName}) has been written off. Reason: {writeOffReason}. Date: {writeOffDate}. Approved by: {approvedBy}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_PENALTY_WRITE_OFF_NOTIFICATION:
			return "Dear {guarantorName}, penalty of {writtenOffPenaltyAmount} on loan {documentNo} (borrower: {borrowerName}) has been written off. Reason: {writeOffReason}. Date: {writeOffDate}. Approved by: {approvedBy}. Outstanding balance: {outstandingBalance}.";
		case LOAN_APPLICATION_OR_DEBT_REGISTRATION:
			return "Dear {username}, your {loanType} application {documentNo} for {amountApplied} is under review. Status: {status}.";
		// Add this case to your getDefaultMessage() switch statement in Utils.java
		case BILL_SUBMISSION_NOTIFICATION:
			return "Dear {username}, your {billType} bill of reference No: {billOrProformaInvoiceNo} for {amount} has been generated. "
					+ "Due Date: {dueDate}. Billing Period: {billingPeriodStart} to {billingPeriodEnd}. "
					+ "Download your bill here: {attachmentDownloadUrl}. " + "Thank you for your business.";

		case PROFORMA_INVOICE_SUBMISSION_NOTIFICATION:
			return "Dear {username}, your {billType} proforma invoice of reference No: {proformaInvoiceNo} for {amount} has been generated. "
					+ "This is a quote and not a final bill. Valid until: {validUntil}. "
					+ "Contains {itemsCount} item(s). Total: {amount} {currency}. "
					+ "Download your proforma invoice here: {attachmentDownloadUrl}. " + "Thank you.";

		case LOAN_APPROVAL_DEBT_APPROVAL:
			return "Dear {username}, your {loanType} {documentNo} has been approved. Amount: {amountApproved}. Disbursement: {disbursementDate}.";

		case LOAN_REJECTION_DEBT_REJECTION:
			return "Dear {username}, your {loanType} application {documentNo} has been declined. Reason: {reason}.";

		case LOAN_OR_DEBT_DUE_REMINDER:
			return "Dear {username}, payment of {amountDue} for {loanType} {documentNo} is due on {nextInstallmentStartDate}. Balance: {balance}.";

		case LOAN_OR_DEBT_OVERDUE_REMINDER:
			return "Dear {username}, your {loanType} {documentNo} is {daysOverDue} days overdue. Amount due: {amountOverDue}. Penalty: {penaltyAmountIncur}.";

		case PAYMENT_RECEIPT_CONFIRMATION:
			return "Dear {username}, payment of {amountPaid} received for {loanType} {documentNo}. Remaining balance: {remainingBalance}. Transaction: {transactionId}.";

		case INSTALLMENT_GENERATION_NOTIFICATION:
			return "Dear {username}, installment schedule generated for {loanType} {documentNo}. Installment amount: {installmentAmount}. Due date: {nextInstallmentStartDate}.";

		case INSTALLMENT_DUE_REMINDER:
			return "Dear {username}, your installment {installmentNumber} of {installmentAmount} for {loanType} {documentNo} is due on {installmentDueDate}. {daysRemaining} days remaining. Total due: {totalDue}";

		case INSTALLMENT_OVERDUE_REMINDER:
			return "Dear {username}, your installment {installmentNumber} of {installmentAmount} for {loanType} {documentNo} is overdue by {daysOverdue} days. Please pay {totalDue} including {penaltyAmount} penalty to avoid further charges.";

		case INSTALLMENT_PAYMENT_REMINDER:
			return "Dear {username}, reminder: Installment {installmentNumber} of {installmentAmount} for {loanType} {documentNo} is due on {installmentDueDate}. {daysRemaining} days remaining. Make payment via {paymentMethod} before {paymentDeadline}";

		case INSTALLMENT_PAYMENT_CONFIRMATION:
			return "Dear {username}, payment of {amountPaid} received for installment {installmentNumber} of {loanType} {documentNo} on {paymentDate}. Receipt: {receiptNumber}. Remaining balance: {remainingBalance}. Next due: {nextDueDate}";

		case INSTALLMENT_PARTIAL_PAYMENT:
			return "Dear {username}, partial payment of {amountPaid} received for installment {installmentNumber} of {loanType} {documentNo}. Remaining amount: {remainingAmount}. Late fee: {lateFee}. Next due: {nextDueDate}";

		case INSTALLMENT_MISSED_PAYMENT:
			return "Dear {username}, you missed installment {installmentNumber} payment of {installmentAmount} for {loanType} {documentNo} due on {installmentDueDate}. {daysMissed} days missed. Late fee: {lateFee}. Pay {totalDue} by {gracePeriodEnd}";

		case INSTALLMENT_ADJUSTMENT_NOTIFICATION:
			return "Dear {username}, installment {installmentNumber} for {loanType} {documentNo} has been adjusted from {oldInstallmentAmount} to {newInstallmentAmount}. Reason: {reason}. Next due: {nextDueDate}";

		case INSTALLMENT_RESCHEDULE_NOTIFICATION:
			return "Dear {username}, installment {installmentNumber} for {loanType} {documentNo} has been rescheduled from {oldDueDate} to {newDueDate}. Amount: {installmentAmount}. New payment plan: {newPaymentPlan}";

		case GRACE_PERIOD_EXPIRY_ALERT:
			return "Dear {username}, grace period for {loanType} {documentNo} ends in {daysRemaining} days. Amount due: {amountDue}. Next due date: {nextInstallmentStartDate}.";

		// REPAYMENT-RELATED MESSAGES
		case PARTIAL_REPAYMENT_NOTIFICATION:
			return "Dear {username}, partial payment of {amountPaid} received for {loanType} {documentNo}. Remaining balance: {remainingBalance}. Total paid: {totalPaid}.";

		case FULL_REPAYMENT_NOTIFICATION:
			return "Dear {username}, {loanType} {documentNo} has been fully settled. Total repaid: {totalRepaid}. Completion date: {completionDate}.";

		case EARLY_REPAYMENT_CONFIRMATION:
			return "Dear {username}, early repayment of {amountPaid} processed for {loanType} {documentNo}. Discount: {discountAmount}. Final balance: {remainingBalance}.";

		case MISSED_REPAYMENT_ALERT:
			return "Dear {username}, payment of {amountDue} for {loanType} {documentNo} was due on {dueDate}. Current overdue: {daysOverdue} days.";

		case REPAYMENT_SCHEDULE_UPDATE:
			return "Dear {username}, repayment schedule updated for {loanType} {documentNo}. New installment: {newInstallmentAmount}. Remaining installments: {remainingInstallments}.";

		case REPAYMENT_RESCHEDULE_REQUEST:
			return "Dear {username}, your reschedule request for {loanType} {documentNo} is {status}. Request date: {requestDate}. Current balance: {balance}.";

		case REPAYMENT_RESCHEDULE_APPROVAL:
			return "Dear {username}, reschedule approved for {loanType} {documentNo}. New installment: {newInstallmentAmount}. Remaining installments: {remainingInstallments}.";

		case REPAYMENT_RESCHEDULE_REJECTION:
			return "Dear {username}, reschedule request for {loanType} {documentNo} was declined. Reason: {rejectionReason}. Current due date: {nextInstallmentStartDate}.";

		// PENALTY-RELATED MESSAGES
		case PENALTY_APPLIED_NOTIFICATION:
			return "Dear {username}, penalty of {penaltyAmount} applied to {loanType} {documentNo}. Reason: {penaltyReason}. Total outstanding: {totalOutstanding}.";

		case PENALTY_WAIVER_NOTIFICATION:
			return "Dear {username}, penalty of {penaltyAmount} on {loanType} {documentNo} has been waived. "
					+ "Reason: {penaltyWaiverReason}. Date: {waiverDate}. Approved by: {approvedBy}. "
					+ "Outstanding balance: {outstandingBalance}.";
		case WAIVER_NOTIFICATION:
			return "Dear {username}, an amount of {waivedAmount} on {loanType} {documentNo} has been waived. "
					+ "Reason: {waiverReason}. Date: {waiverDate}. Approved by: {approvedBy}. "
					+ "Outstanding balance: {outstandingBalance}.";

		case WRITE_OFF_NOTIFICATION:
			return "Dear {username}, an amount of {writtenOffAmount} on {loanType} {documentNo} has been written off. "
					+ "Reason: {writeOffReason}. Date: {writeOffDate}. Approved by: {approvedBy}. "
					+ "Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_WAIVER_NOTIFICATION:
			return "Dear {guarantorName}, an amount of {waivedAmount} on loan {documentNo} (borrower: {borrowerName}) has been waived. "
					+ "Reason: {waiverReason}. Date: {waiverDate}. Approved by: {approvedBy}. "
					+ "Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_WRITE_OFF_NOTIFICATION:
			return "Dear {guarantorName}, an amount of {writtenOffAmount} on loan {documentNo} (borrower: {borrowerName}) has been written off. "
					+ "Reason: {writeOffReason}. Date: {writeOffDate}. Approved by: {approvedBy}. "
					+ "Outstanding balance: {outstandingBalance}.";

		// LOAN MANAGEMENT MESSAGES
		case LOAN_RESTRUCTURING_NOTIFICATION:
			return "Dear {username}, {loanType} {documentNo} has been restructured. New principal: {newPrincipal}. New term: {newTerm}. New installment: {newInstallment}.";

		case TOP_UP_LOAN_DISBURSEMENT:
			return "Dear {username}, top-up of {topUpAmount} disbursed for {loanType} {documentNo}. Total outstanding: {totalOutstanding}. New principal: {newPrincipal}.";

		case LOAN_CLOSURE_NOTIFICATION:
			return "Dear {username}, {loanType} {documentNo} has been closed. Settlement amount: {settlementAmount}. Total repaid: {totalRepaid}. Date: {closureDate}.";

		// FINANCIAL NOTIFICATIONS
		case INTEREST_CALCULATION_NOTIFICATION:
			return "Dear {username}, interest of {interestAmount} calculated for {loanType} {documentNo}. Total interest: {totalInterest}. Date: {calculationDate}.";

		case STATEMENT_READY_NOTIFICATION:
			return "Dear {username}, statement ready for {loanType} {documentNo}. Period: {period}. Amount due: {amountDue}. Due date: {dueDate}.";

		case AUTO_DEBIT_FAILURE:
			return "Dear {username}, auto debit failed for {loanType} {documentNo}. Amount: {amountDue}. Reason: {failureReason}. Next retry: {retryDate}.";

		case AUTO_DEBIT_SUCCESS:
			return "Dear {username}, auto debit of {amountPaid} successful for {loanType} {documentNo}. Remaining balance: {remainingBalance}. Date: {paymentDate}.";

		// ACCOUNT MANAGEMENT MESSAGES
		case ACCOUNT_ACTIVATION_NOTIFICATION:
			return "Dear {username}, your account has been activated. Activation date: {activationDate}.";

		case ACCOUNT_SUSPENSION_NOTIFICATION:
			return "Dear {username}, your account has been suspended. Reason: {suspensionReason}. Effective date: {suspensionDate}.";

		case ACCOUNT_REACTIVATION_NOTIFICATION:
			return "Dear {username}, your account has been reactivated. Date: {reactivationDate}.";

		case MEMBER_REGISTRATION_SUCCESS:
			return "Dear {username}, registration completed. Membership ID: {membershipAccountNo}. Date: {registrationDate}.";

		case MEMBERSHIP_RENEWAL_REMINDER:
			return "Dear {username}, membership {membershipAccountNo} expires on {expiryDate}. Days remaining: {daysRemaining}.";
		case MEMBERSHIP_REJECTION:
			return "Dear {username}, your membership application {membershipAccountNo} has been rejected. Reason: {reason}. Please contact support for further assistance.";

		// SAVINGS MESSAGES
		case SAVINGS_DEPOSIT_NOTIFICATION:
			return "Dear {username}, deposit of {depositAmount} received. Total balance: {totalBalance}. Date: {depositDate}.";

		case SAVINGS_WITHDRAWAL_NOTIFICATION:
			return "Dear {username}, withdrawal of {withdrawalAmount} processed. Remaining balance: {remainingBalance}. Date: {withdrawalDate}.";

		case ANNOUNCEMENT_NOTIFICATION:
			return "Announcement dated {announcementDate}. Please check your account for details.";

		// GUARANTOR-RELATED MESSAGES
		case GUARANTOR_APPROVAL_REQUEST:
			return "Dear {guarantorName}, guarantor request for {borrowerName}'s {loanType} {documentNo}. Amount: {amountApproved}. Please review and respond.";

		case GUARANTOR_APPROVAL_CONFIRMATION:
			return "Dear {guarantorName}, you are confirmed as guarantor for {borrowerName}'s {loanType} {documentNo}. Guarantee amount: {guaranteeAmount}.";

		case GUARANTOR_APPROVAL_REJECTION:
			return "Dear {guarantorName}, you have declined to guarantee {borrowerName}'s {loanType} {documentNo}. Date: {rejectionDate}.";

		case GUARANTOR_LOAN_DEFAULT_NOTIFICATION:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} is in default. Amount overdue: {amountOverDue}. Days overdue: {daysOverDue}.";

		case GUARANTOR_PAYMENT_REMINDER:
			return "Dear {guarantorName}, payment reminder for {borrowerName}'s {loanType} {documentNo}. Amount due: {amountDue}. Due date: {nextInstallmentStartDate}.";

		case GUARANTOR_LOAN_OVERDUE_ALERT:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} is overdue. Amount: {amountOverDue}. Days: {daysOverDue}. Penalty: {penaltyAmountIncur}.";

		case GUARANTOR_LOAN_SETTLEMENT:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} has been settled. Your guarantee obligation is complete. Date: {closureDate}.";

		case GUARANTOR_LOAN_RESTRUCTURING:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} has been restructured. New installment: {newInstallmentAmount}. New term: {newTerm}.";

		case GUARANTOR_PARTIAL_PAYMENT_NOTIFICATION:
			return "Dear {guarantorName}, partial payment of {amountPaid} received for {borrowerName}'s {loanType} {documentNo}. Remaining balance: {remainingBalance}.";

		case GUARANTOR_LOAN_ASSIGNMENT_NOTIFICATION:
			return "Dear {guarantorName}, you are assigned as guarantor for {borrowerName}'s {loanType} {documentNo}. Amount: {amountApproved}. Guarantee: {guaranteeAmount}.";

		case GUARANTOR_LOAN_CLOSURE:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} has been closed. Your guarantee obligation is complete. Date: {closureDate}.";

		case GUARANTOR_INSTALLMENT_DUE_REMINDER:
			return "Dear {guarantorName}, installment {installmentNumber} of {installmentAmount} for {borrowerName}'s {loanType} {documentNo} is due on {installmentDueDate}. {daysRemaining} days remaining. Your guarantee: {guaranteeAmount}";

		case GUARANTOR_INSTALLMENT_OVERDUE_ALERT:
			return "Dear {guarantorName}, installment {installmentNumber} of {installmentAmount} for {borrowerName}'s {loanType} {documentNo} is overdue by {daysOverdue} days. Late fee: {lateFee}. Your guarantee utilized: {currentGuaranteeUsed}";

		case GUARANTOR_INSTALLMENT_MISSED_PAYMENT:
			return "Dear {guarantorName}, {borrowerName} missed installment {installmentNumber} payment of {installmentAmount} for {loanType} {documentNo}. {daysMissed} days missed. Your potential liability: {guarantorLiability}. Pay by {gracePeriodEnd} to avoid guarantee call";

		case GUARANTOR_INSTALLMENT_PAYMENT_RECEIVED:
			return "Dear {guarantorName}, payment of {amountPaid} received for installment {installmentNumber} of {borrowerName}'s {loanType} {documentNo}. Remaining balance: {remainingBalance}. Your guarantee utilization: {guaranteeUtilization}";

		case GUARANTOR_INSTALLMENT_PARTIAL_PAYMENT:
			return "Dear {guarantorName}, partial payment of {amountPaid} received for installment {installmentNumber} of {borrowerName}'s {loanType} {documentNo}. Remaining amount: {remainingAmount}. Your guarantee risk level: {guaranteeRiskLevel}";

		case GUARANTOR_INSTALLMENT_ADJUSTMENT:
			return "Dear {guarantorName}, installment {installmentNumber} for {borrowerName}'s {loanType} {documentNo} adjusted from {oldInstallmentAmount} to {newInstallmentAmount}. Reason: {reason}. Your guarantee: {guaranteeAmount}";

		case GUARANTOR_INSTALLMENT_RESCHEDULE:
			return "Dear {guarantorName}, installment {installmentNumber} for {borrowerName}'s {loanType} {documentNo} rescheduled from {oldDueDate} to {newDueDate}. Amount: {installmentAmount}. Your guarantee: {guaranteeAmount}";

		case GUARANTOR_PENALTY_CALCULATION_NOTIFICATION:
			return "Dear {guarantorName}, a penalty has been applied on the loan guaranteed for {borrowerName}. Loan {documentNo} ({loanType}). Penalty amount: {penaltyAmount} at rate {penaltyRate}. Reason: {penaltyReason}. Overdue days: {overdueDays}. Overdue amount: {overdueAmount}. Total outstanding: {totalOutstanding}. Penalty type: {penaltyType}. Grace period used: {gracePeriodUsed}. Frequency: {penaltyFrequency}.";

		case GUARANTOR_INTEREST_ACCRUAL_NOTIFICATION:
			return "Dear {guarantorName}, interest has accrued on the loan guaranteed for {borrowerName}. Loan {documentNo} ({loanType}). Interest amount: {interestAmount}. Total interest accrued: {totalInterestAccrued}. Accrual date: {accrualDate}. Interest frequency: {interestFrequency}. Balance: {currentBalance}. Calculation method: {interestCalculationMethod}. Next accrual: {nextAccrualDate}. Period: {interestPeriod}.";

		case GUARANTOR_PENALTY_WAIVER_NOTIFICATION:
			return "Dear {guarantorName}, a penalty waiver has been granted on the loan guaranteed for {borrowerName}. Loan {documentNo} ({loanType}). Waived penalty: {waivedPenaltyAmount}. Previous penalty: {previousPenaltyAmount}. Reason: {penaltyWaiverReason}. Waiver date: {waiverDate}. Approved by: {approvedBy}.";

		case GUARANTOR_INTEREST_WAIVER_NOTIFICATION:
			return "Dear {guarantorName}, an interest waiver has been applied on the loan guaranteed for {borrowerName}. Loan {documentNo} ({loanType}). Waived interest: {waivedInterestAmount}. Previous interest: {previousInterestAmount}. Reason: {interestWaiverReason}. Waiver date: {waiverDate}. Approved by: {approvedBy}.";

		// ========== MISSING GUARANTOR MESSAGES ==========
		case GUARANTOR_LOAN_DUE_REMINDER:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} of {totalAmountDue} is due on {loanDueDate}. {daysRemaining} days remaining. Your guarantee: {guaranteeAmount}. Used: {currentGuaranteeUsed}.";

		case GUARANTOR_FULL_REPAYMENT_NOTIFICATION:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} has been fully repaid. Total repaid: {totalRepaid}. Your guarantee of {guaranteeAmount} has been released. Completion: {completionDate}.";

		case GUARANTOR_LIMIT_UPDATE_NOTIFICATION:
			return "Dear {guarantorName}, your guarantee limit has been updated from {oldGuaranteeLimit} to {newGuaranteeLimit}. Effective: {effectiveDate}. Reason: {reason}.";

		case GUARANTOR_STATUS_CHANGE_NOTIFICATION:
			return "Dear {guarantorName}, your guarantor status has been changed from {oldStatus} to {newStatus}. Effective: {effectiveDate}. Reason: {reason}.";

		case GUARANTOR_REPLACEMENT_NOTIFICATION:
			return "Dear {oldGuarantorName}, you have been replaced as guarantor for {borrowerName}'s {loanType} {documentNo}. New guarantor: {newGuarantorName}. Effective: {replacementDate}. Reason: {reason}.";

		case GUARANTOR_CALL_NOTIFICATION:
			return "Dear {guarantorName}, a guarantee call of {callAmount} has been initiated for {borrowerName}'s {loanType} {documentNo}. Total outstanding: {totalOutstanding}. Please pay by {paymentDeadline}. Reason: {reason}.";

		case GUARANTOR_RECOVERY_NOTIFICATION:
			return "Dear {guarantorName}, recovery amount of {recoveryAmount} has been processed for {borrowerName}'s {loanType} {documentNo}. Recovery method: {recoveryMethod}. Remaining balance: {remainingBalance}. Date: {recoveryDate}.";

		case GUARANTOR_RELEASE_NOTIFICATION:
			return "Dear {guarantorName}, you have been released as guarantor for {borrowerName}'s {loanType} {documentNo}. Release date: {releaseDate}. Reason: {releaseReason}. Guarantee amount: {guaranteeAmount}.";

		case GUARANTOR_STATEMENT_NOTIFICATION:
			return "Dear {guarantorName}, your guarantor statement for period {statementPeriod} is ready. Total guaranteed: {totalGuaranteedAmount}. Active guarantees: {activeGuarantees}. Utilization: {guaranteeUtilization}. Date: {statementDate}.";

		// ========== ADDITIONAL MISSING TYPES ==========
		case GUARANTOR_MISSED_REPAYMENT_ALERT:
			return "Dear {guarantorName}, {borrowerName}'s {loanType} {documentNo} payment of {amountDue} was due on {dueDate} and is now {daysOverdue} days overdue. Outstanding balance: {balance}. Penalty incurred: {penaltyAmountIncur}. Next due: {nextInstallmentDueDate}.";

		case MANUAL_SMS_FROM_MESSAGE_CENTER:
			return "Manual SMS sent from message center.";
		// ========== ADDITIONAL MEMBERSHIP MESSAGES ==========
		case MEMBER_WELCOME_MESSAGE:
			return "Dear {username}, welcome to our community! Your membership ({membershipAccountNo}) of type {membershipType} is active from {joinDate}. {welcomeMessage} Next steps: {nextSteps}.";

		case MEMBERSHIP_RENEWAL_SUCCESS:
			return "Dear {username}, your membership {membershipAccountNo} has been renewed successfully. New expiry date: {newExpiryDate}. Amount paid: {amountPaid}. Renewal date: {renewalDate}. Membership type: {membershipType}.";

		case MEMBERSHIP_RENEWAL_FAILED:
			return "Dear {username}, your membership {membershipAccountNo} renewal failed. Reason: {failureReason}. Next retry: {retryDate}. Contact support: {contactSupport}.";

		case MEMBERSHIP_EXPIRY_WARNING:
			return "Dear {username}, your membership {membershipAccountNo} expires on {expiryDate}. {daysRemaining} days remaining. Please renew at: {renewalLink}.";

		case MEMBERSHIP_EXPIRED:
			return "Dear {username}, your membership {membershipAccountNo} expired on {expiryDate}. Grace period ends on {gracePeriodEnds}. Reactivate now: {reactivationLink}.";

		case MEMBERSHIP_GRACE_PERIOD_REMINDER:
			return "Dear {username}, your membership {membershipAccountNo} grace period ends on {graceEndDate}. {daysRemaining} days remaining. Reactivation fee: {reactivationFee}.";

		case MEMBERSHIP_UPGRADE_CONFIRMATION:
			return "Dear {username}, your membership has been upgraded from {oldTier} to {newTier} on {upgradeDate}. Price difference: {priceDifference}. New benefits: {newBenefits}.";

		case MEMBERSHIP_DOWNGRADE_CONFIRMATION:
			return "Dear {username}, your membership has been downgraded from {oldTier} to {newTier} effective {effectiveDate}. Refund amount: {refundAmount}.";

		case MEMBERSHIP_PAYMENT_RECEIVED:
			return "Dear {username}, payment of {amountPaid} received for membership {membershipAccountNo} on {paymentDate}. Method: {paymentMethod}. Transaction ID: {transactionId}. Valid until: {validUntil}.";

		case MEMBERSHIP_PAYMENT_DUE:
			return "Dear {username}, payment of {amountDue} for membership {membershipAccountNo} is due on {dueDate}. {daysRemaining} days remaining. Pay now: {paymentLink}.";

		case MEMBERSHIP_PAYMENT_OVERDUE:
			return "Dear {username}, payment of {amountDue} for membership {membershipAccountNo} was due on {dueDate} and is {daysOverdue} days overdue. Late fee: {lateFee}. Your membership may be suspended on {suspensionDate}.";

		case MEMBERSHIP_AUTO_DEBIT_SUCCESS:
			return "Dear {username}, auto-debit of {amountPaid} for membership {membershipAccountNo} succeeded on {paymentDate}. Next billing date: {nextBillingDate}.";

		case MEMBERSHIP_AUTO_DEBIT_FAILED:
			return "Dear {username}, auto-debit of {amountDue} for membership {membershipAccountNo} failed. Reason: {failureReason}. Next retry: {retryDate}. Please update payment method: {updatePaymentMethod}.";

		case MEMBERSHIP_ACTIVATION:
			return "Dear {username}, your membership {membershipAccountNo} has been activated on {activationDate}. Membership type: {membershipType}. Valid until: {validUntil}.";

		case MEMBERSHIP_SUSPENSION:
			return "Dear {username}, your membership {membershipAccountNo} has been suspended. Reason: {suspensionReason}. Effective date: {suspensionDate}. Reactivation process: {reactivationProcess}.";

		case MEMBERSHIP_REACTIVATION:
			return "Dear {username}, your membership {membershipAccountNo} has been reactivated on {reactivationDate}. New expiry date: {newExpiryDate}.";

		case MEMBERSHIP_CANCELLATION:
			return "Dear {username}, your membership {membershipAccountNo} has been cancelled on {cancellationDate}. Reason: {cancellationReason}. Refund amount: {refundAmount}.";

		case MEMBERSHIP_BENEFITS_REMINDER:
			return "Dear {username}, as a {membershipTier} member, you have these benefits: {availableBenefits}. Some benefits expire on {benefitExpiryDate}.";

		case MEMBERSHIP_SPECIAL_OFFER:
			return "Dear {username}, exclusive offer: {offerTitle}. {offerDetails}. Valid until {offerExpiry}. Redeem now: {redeemLink}.";

		case MEMBERSHIP_ANNIVERSARY:
			return "Dear {username}, happy membership anniversary! You've been a member for {yearsAsMember} years since {joinDate}. As a token of appreciation, here's a special gift: {specialGift}.";

		case MEMBERSHIP_BIRTHDAY_GREETING:
			return "Dear {username}, happy birthday! Enjoy this special offer: {specialOffer}. Valid until {validityPeriod}.";

		case MEMBERSHIP_POINTS_EARNED:
			return "Dear {username}, you earned {pointsEarned} points on {transactionDetails}. Total points: {totalPoints}. Points expire on {pointsExpiry}.";

		case MEMBERSHIP_POINTS_EXPIRY:
			return "Dear {username}, {pointsExpiring} points are about to expire on {expiryDate}. Redeem them now: {redeemLink}.";

		case MEMBERSHIP_REWARD_REDEEMED:
			return "Dear {username}, you redeemed {rewardName} using {pointsUsed} points on {redemptionDate}. Delivery details: {deliveryDetails}.";

		case MEMBERSHIP_PROFILE_UPDATE:
			return "Dear {username}, your profile has been updated. Updated fields: {updatedFields} on {updateDate}.";

		case MEMBERSHIP_DOCUMENT_VERIFIED:
			return "Dear {username}, your {documentType} has been verified on {verificationDate}.";

		case MEMBERSHIP_DOCUMENT_REJECTED:
			return "Dear {username}, your {documentType} was rejected. Reason: {rejectionReason}. Please resubmit: {resubmissionLink}.";

		case MEMBERSHIP_INACTIVITY_REMINDER:
			return "Dear {username}, we noticed you haven't logged in since {lastActiveDate} ({daysInactive} days). Here's an offer to re-engage: {engagementOffer}.";

		case MEMBERSHIP_SURVEY_REQUEST:
			return "Dear {username}, we value your feedback. Please take a moment to complete our survey: {surveyLink}. As a thank you, you'll receive {incentive}.";

		case MEMBERSHIP_REFERRAL_SUCCESS:
			return "Dear {username}, thank you for referring {referredName}! You earned {rewardEarned}. Total referrals: {totalReferrals}.";

		case MEMBERSHIP_REFERRAL_REMINDER:
			return "Dear {username}, refer a friend and earn {rewardAmount}! Use your referral code: {referralCode} or share this link: {referralLink}.";

		case MEMBERSHIP_2FA_ENABLED:
			return "Dear {username}, two-factor authentication has been enabled on your account on {enableDate}. Your account is now more secure.";

		case MEMBERSHIP_PASSWORD_CHANGED:
			return "Dear {username}, your password was changed on {changeDate}. If you did not request this, please contact support immediately. IP Address: {ipAddress}.";

		case MEMBERSHIP_LOGIN_ALERT:
			return "Dear {username}, a login was detected on {loginTime} from device {deviceInfo} at location {location}. If this wasn't you, please secure your account: {actionLink}.";

		// ========== LOAN CANCELLATION NOTIFICATIONS ==========
		case LOAN_CANCELLATION_NOTIFICATION:
			return "Dear {username}, your {loanType} {documentNo} has been cancelled. Reason: {cancellationReason}. Cancellation date: {cancellationDate}. Amount applied: {amountApplied}. Current balance: {balance}. Loan status: {loanStatus}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_LOAN_CANCELLATION_NOTIFICATION:
			return "Dear {guarantorName}, the {loanType} {documentNo} for borrower {borrowerName} has been cancelled. Reason: {cancellationReason}. Cancellation date: {cancellationDate}. Outstanding balance: {outstandingBalance}.";

		// ========== LOAN CONSOLIDATION NOTIFICATIONS ==========
		case LOAN_CONSOLIDATION_NOTIFICATION:
			return "Dear {username}, your {loanType} {documentNo} has been consolidated with {childLoanCount} other loan(s) under group {consolidatedBillingGroupId}. Consolidation date: {consolidationDate}. Total consolidated balance: {totalChildBalance}. New total balance: {newTotalBalance}. New due date: {consolidatedDueDate}. Child loans: {childLoanRefs}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_LOAN_CONSOLIDATION_NOTIFICATION:
			return "Dear {guarantorName}, the {loanType} {documentNo} for borrower {borrowerName} has been consolidated under parent loan {parentLoanRef}. Consolidation date: {consolidationDate}. Group ID: {consolidatedBillingGroupId}. Outstanding balance: {outstandingBalance}.";

		// ========== BREAK CONSOLIDATION NOTIFICATIONS ==========
		case LOAN_BREAK_CONSOLIDATION_NOTIFICATION:
			return "Dear {username}, your {loanType} {documentNo} has been removed from consolidation. Break date: {breakDate}. Previous due date: {previousDueDate}. New due date: {newDueDate}. Current balance: {balance}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_LOAN_BREAK_CONSOLIDATION_NOTIFICATION:
			return "Dear {guarantorName}, the {loanType} {documentNo} for borrower {borrowerName} has been removed from consolidation. Break date: {breakDate}. Outstanding balance: {outstandingBalance}.";

		// ========== LOAN STATE CHANGE NOTIFICATIONS ==========
		case LOAN_STATE_CHANGE_NOTIFICATION:
			return "Dear {username}, your {loanType} {documentNo} state has changed from {oldState} to {newState} on {stateChangeDate}. Trigger: {stateChangeTrigger}. Current balance: {balance}. Loan status: {loanStatus}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_LOAN_STATE_CHANGE_NOTIFICATION:
			return "Dear {guarantorName}, the {loanType} {documentNo} for borrower {borrowerName} has changed state from {oldState} to {newState} on {stateChangeDate}. Outstanding balance: {outstandingBalance}.";

		// ========== LOAN REINSTATEMENT NOTIFICATIONS ==========
		case LOAN_REINSTATEMENT_NOTIFICATION:
			return "Dear {username}, your {loanType} {documentNo} has been reinstated on {reinstatementDate}. Reason: {reinstatementReason}. New due date: {newDueDate}. Current balance: {balance}. Loan status: {loanStatus}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_LOAN_REINSTATEMENT_NOTIFICATION:
			return "Dear {guarantorName}, the {loanType} {documentNo} for borrower {borrowerName} has been reinstated on {reinstatementDate}. Reason: {reinstatementReason}. Outstanding balance: {outstandingBalance}.";

		// ========== LOAN WRITE-OFF NOTIFICATIONS ==========
		case LOAN_WRITE_OFF_NOTIFICATION:
			return "Dear {username}, your {loanType} {documentNo} has been written off. Amount written off: {writtenOffAmount}. Reason: {writeOffReason}. Write-off date: {writeOffDate}. Approved by: {approvedBy}. Remaining balance: {remainingBalance}. Loan status: {loanStatus}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_LOAN_WRITE_OFF_NOTIFICATION:
			return "Dear {guarantorName}, the {loanType} {documentNo} for borrower {borrowerName} has been written off. Amount written off: {writtenOffAmount}. Reason: {writeOffReason}. Write-off date: {writeOffDate}. Approved by: {approvedBy}. Outstanding balance: {outstandingBalance}.";
			// ========== FEE NOTIFICATIONS ==========
		case FEE_APPLIED_NOTIFICATION:
		    return "Dear {username}, a {feeType} of {feeAmount} has been applied to your {loanType} {documentNo}. Fee description: {feeDescription}. Application date: {applicationDate}. Current balance: {balance}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_FEE_NOTIFICATION:
		    return "Dear {guarantorName}, a {feeType} of {feeAmount} has been applied to the {loanType} {documentNo} for borrower {borrowerName}. Fee description: {feeDescription}. Application date: {applicationDate}. Current balance: {balance}. Outstanding balance: {outstandingBalance}.";

		case DAILY_FEE_APPLIED_NOTIFICATION:
		    return "Dear {username}, a daily fee of {feeAmount} has been accrued on your {loanType} {documentNo}. Daily fee rate: {dailyFeeRate}. Days accrued: {daysAccrued}. Period: {periodStart} to {periodEnd}. Total daily fee accrued: {totalDailyFeeAccrued}. Current balance: {balance}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_DAILY_FEE_NOTIFICATION:
		    return "Dear {guarantorName}, a daily fee of {feeAmount} has been accrued on the {loanType} {documentNo} for borrower {borrowerName}. Daily fee rate: {dailyFeeRate}. Days accrued: {daysAccrued}. Period: {periodStart} to {periodEnd}. Total daily fee accrued: {totalDailyFeeAccrued}. Current balance: {balance}. Outstanding balance: {outstandingBalance}.";

		case SERVICE_FEE_APPLIED_NOTIFICATION:
		    return "Dear {username}, a service fee of {feeAmount} has been applied to your {loanType} {documentNo}. Service fee type: {serviceFeeType}. Fee timing: {feeTiming}. Application date: {applicationDate}. Current balance: {balance}. Outstanding balance: {outstandingBalance}.";

		case GUARANTOR_SERVICE_FEE_NOTIFICATION:
		    return "Dear {guarantorName}, a service fee of {feeAmount} has been applied to the {loanType} {documentNo} for borrower {borrowerName}. Service fee type: {serviceFeeType}. Fee timing: {feeTiming}. Current balance: {balance}. Outstanding balance: {outstandingBalance}.";

		
		
		default:
			return "Dear {username}, notification regarding your account. Please log in for details.";
		}
	}

	public String getFutureDate(int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_MONTH, days);

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.format(cal.getTime());
	}

	public Date getFutureDateUsingCalender(Date dateFrom, int days) {
		long millisecondsPerDay = 24 * 60 * 60 * 1000L;
		long futureTime = dateFrom.getTime() + (days * millisecondsPerDay);
		return new Date(futureTime);
	}

	public String normalizePhone(String phone) {
		if (phone.startsWith("+")) {
			phone = phone.substring(1);
		}
		if (phone.startsWith("0")) {
			phone = "254" + phone.substring(1);
		}
		return phone;
	}

	public int getNoOfDaysBetweenTwoDates(Date startDate, Date endDate) {
		if (startDate == null || endDate == null)
			return 0;
		long diffInMillis = endDate.getTime() - startDate.getTime();
		return (int) TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
	}

	public String formatAmount(BigDecimal amount, String currency) {
		if (amount == null)
			amount = BigDecimal.ZERO;
		DecimalFormat df = new DecimalFormat("#,##0.00");
		return currency + " " + df.format(amount);
	}

	public Date getDateDaysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		return cal.getTime();
	}

	public String getBorrowerPhone(MLoanApplication application) {
		if (application.getBorrowerType() == null)
			return null;

		switch (application.getBorrowerType()) {
		case GROUP:
			return application.getGroupBorrower() != null ? application.getGroupBorrower().getContactPhone() : null;
		case INSTITUTION:
			return application.getInstitutionBorrower() != null ? application.getInstitutionBorrower().getContactPhone()
					: null;
		case INDIVIDUAL:
			return application.getIndividualBorrower() != null ? application.getIndividualBorrower().getPhone() : null;
		default:
			return null;
		}
	}

	public MUser getUserByBorrower(MLoanApplication application) {
		if (application.getBorrowerType() == null)
			return null;

		switch (application.getBorrowerType()) {
		case GROUP:
			return application.getGroupBorrower() != null ? application.getGroupBorrower().getGroupRepresentative()
					: null;
		case INSTITUTION:
			return application.getInstitutionBorrower() != null
					? application.getInstitutionBorrower().getRepresentative()
					: null;
		case INDIVIDUAL:
			return application.getIndividualBorrower() != null ? application.getIndividualBorrower().getUser() : null;
		default:
			return null;
		}
	}

	public boolean isBorrowerEligible(MLoanApplication application) {
		if (application.getBorrowerType() == null)
			return false;

		switch (application.getBorrowerType()) {
		case GROUP:
			return application.getGroupBorrower() != null ? true : false;
		case INSTITUTION:
			return application.getInstitutionBorrower() != null ? true : false;
		case INDIVIDUAL:
			return application.getIndividualBorrower() != null ? application.getIndividualBorrower().isEligibleToPay()
					: false;
		default:
			return true;
		}
	}

	public String getBorrowerName(MLoanApplication application) {
		if (application.getBorrowerType() == null)
			return "";

		switch (application.getBorrowerType()) {
		case GROUP:
			return application.getGroupBorrower() != null ? application.getGroupBorrower().getGroupName() : "";
		case INSTITUTION:
			return application.getInstitutionBorrower() != null
					? application.getInstitutionBorrower().getInstitutionName()
					: "";
		case INDIVIDUAL:
			if (application.getIndividualBorrower() != null) {
				return application.getIndividualBorrower().getFirstName() + " "
						+ application.getIndividualBorrower().getLastName();
			}
			return "";
		default:
			return "";
		}
	}

	public Map<String, Long> getBorrowerIds(MLoanApplication application) {
		Map<String, Long> ids = new HashMap<>();
		ids.put("individualId", 0L);
		ids.put("groupId", 0L);
		ids.put("institutionId", 0L);

		if (application.getBorrowerType() == null)
			return ids;

		switch (application.getBorrowerType()) {
		case GROUP:
			if (application.getGroupBorrower() != null)
				ids.put("groupId", application.getGroupBorrower().getGroupBorrowerId());
			break;
		case INSTITUTION:
			if (application.getInstitutionBorrower() != null)
				ids.put("institutionId", application.getInstitutionBorrower().getInstitutionBorrowerId());
			break;
		case INDIVIDUAL:
			if (application.getIndividualBorrower() != null)
				ids.put("individualId", application.getIndividualBorrower().getIndividualBorrowerId());
			break;
		}

		return ids;
	}

	public boolean saveSmsRecord(MSms smsRecord) {
		try {
			// Validate required fields
			if (smsRecord == null) {
				log.error("Cannot save SMS: SMS record is null");
				return false;
			}

			if (smsRecord.getPhoneNo() == null || smsRecord.getPhoneNo().trim().isEmpty()) {
				log.error("Cannot save SMS: Phone number is required");
				return false;
			}

			if (smsRecord.getMessage() == null || smsRecord.getMessage().trim().isEmpty()) {
				log.error("Cannot save SMS: Message is required");
				return false;
			}

			if (smsRecord.getSmsType() == null) {
				log.error("Cannot save SMS: SMS type is required");
				return false;
			}

			if (smsRecord.getAdOrgID() == null) {
				log.error("Cannot save SMS: Organization ID is required");
				return false;
			}

			if (smsRecord.getCreated() == null) {
				smsRecord.setCreated(new Date());
			}

			// Save the record
			smsRepository.save(smsRecord);
			log.debug("✅ Saved SMS record - Type: {}, Phone: {}, Loan: {}", smsRecord.getSmsType(),
					smsRecord.getPhoneNo(), smsRecord.getLoanId());
			return true;

		} catch (Exception e) {
			log.error("❌ Error saving SMS record: {}", e.getMessage(), e);
			return false;
		}
	}

	public String getBorrowerEmail(MLoanApplication application) {
		if (application.getBorrowerType() == null)
			return null;

		switch (application.getBorrowerType()) {
		case GROUP:
			return application.getGroupBorrower() != null && application.getGroupBorrower().getContactEmail() != null
					? application.getGroupBorrower().getContactEmail()
					: application.getGroupBorrower().getGroupRepresentative().getEmail();
		case INSTITUTION:
			return application.getInstitutionBorrower() != null
					&& application.getInstitutionBorrower().getContactEmail() != null
							? application.getInstitutionBorrower().getContactEmail()
							: application.getInstitutionBorrower().getRepresentative().getEmail();
		case INDIVIDUAL:
			return application.getIndividualBorrower() != null && application.getIndividualBorrower().getUser() != null
					? application.getIndividualBorrower().getUser().getEmail()
					: application.getIndividualBorrower().getEmail();
		default:
			return null;
		}
	}

	public String getGuarantorEmail(MNextOfKin guarantor) {
		return guarantor != null ? guarantor.getEmail() : null;
	}

	public boolean saveGuarantorSms(Long guarantorId, String phone, String message, Long orgId, Long clientId,
			SmsTypeEnum smsType, Long installmentId, Long loanId, LocalDateTime timeToSend, Long reminderId) {

		// Validate input
		if (phone == null || phone.trim().isEmpty()) {
			log.error("❌ Cannot save guarantor SMS: Phone number is empty");
			return false;
		}

		if (message == null || message.trim().isEmpty()) {
			log.error("❌ Cannot save guarantor SMS: Message is empty");
			return false;
		}

		try {
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phone));
			sms.setMessage(message);
			sms.setSmsType(smsType != null ? smsType : SmsTypeEnum.GUARANTOR_PAYMENT_REMINDER);
			sms.setAdOrgID(orgId != null ? orgId : 0L);
			sms.setAdClientId(clientId != null ? clientId : 0L);
			sms.setLoanId(loanId != null ? loanId : 0L);
			sms.setInstallmentId(installmentId != null ? installmentId : 0L);
			sms.setReminderId(reminderId != null ? reminderId : 0L);
			sms.setTimesTosend(timeToSend);
			sms.setGuarantorId(guarantorId != null ? guarantorId : 0L);

			return saveSmsRecord(sms);

		} catch (Exception e) {
			log.error("❌ Error in saveGuarantorSms: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 📩 Save SMS entry to database
	 */
	public void saveSms(Map<String, Long> borrowerIds, String phone, String message, long orgId, long clientId,
			SmsTypeEnum smsType, long installmentId, long loanAppId, LocalDateTime timeToSend, Long reminderId,
			Long guarantorId) {
		log.warn("⚠️ Using deprecated saveSms method. Use saveReminderSms instead.");

		saveReminderSms(borrowerIds, phone, message, orgId, clientId, smsType, installmentId, loanAppId, timeToSend,
				reminderId, guarantorId);
	}

	public boolean isSmsAlreadySent(SmsTypeEnum smsType, Long loanId, Long installmentId, Long reminderId) {
		try {
			Date oneHourAgo = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

			if (installmentId != null && installmentId > 0) {
				return smsRepository.existsBySmsTypeAndInstallmentIdAndCreatedAfter(smsType, installmentId, oneHourAgo);
			} else if (reminderId != null && reminderId > 0) {
				return smsRepository.existsBySmsTypeAndReminderIdAndLoanIdAndCreatedAfter(smsType, reminderId, loanId,
						oneHourAgo);
			} else if (loanId != null && loanId > 0) {
				return smsRepository.existsBySmsTypeAndLoanIdAndCreatedAfter(smsType, loanId, oneHourAgo);
			}

			return false;
		} catch (Exception e) {
			log.error("Error checking if SMS was already sent: {}", e.getMessage());
			return false;
		}
	}

	public void saveSmsLoanAmendmentAprrovalSms(String phone, String message, long orgId, long clientId,
			LocalDateTime timeToSend) {
		try {
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setPhoneNo(phone);
			sms.setMessage(message);
			sms.setSmsType(SmsTypeEnum.ANNOUNCEMENT_NOTIFICATION);
			sms.setAdOrgID(orgId);
			sms.setAdClientId(clientId);

			sms.setTimesTosend(timeToSend);

			smsRepository.save(sms);
		} catch (Exception e) {
		}
	}

	public void saveuserNotificationSms(long userId, String phone, String message, long orgId, long clientId,
			LocalDateTime timeToSend) {
		try {
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setPhoneNo(phone);
			sms.setMessage(message);
			sms.setSmsType(SmsTypeEnum.ANNOUNCEMENT_NOTIFICATION);
			sms.setAdOrgID(orgId);
			sms.setAdClientId(clientId);
			sms.setCreatedBy(userId);
			sms.setTimesTosend(timeToSend);

			smsRepository.save(sms);
		} catch (Exception e) {
		}
	}

	public boolean saveReminderSms(Map<String, Long> borrowerIds, String phone, String message, Long orgId,
			Long clientId, SmsTypeEnum smsType, Long installmentId, Long loanId, LocalDateTime timeToSend,
			Long reminderId, Long guarantorId) {

		// Validate input
		if (phone == null || phone.trim().isEmpty()) {
			log.error("❌ Cannot save reminder SMS: Phone number is empty");
			return false;
		}

		if (message == null || message.trim().isEmpty()) {
			log.error("❌ Cannot save reminder SMS: Message is empty");
			return false;
		}

		if (smsType == null) {
			log.error("❌ Cannot save reminder SMS: SMS type is required");
			return false;
		}

		if (orgId == null) {
			log.error("❌ Cannot save reminder SMS: Organization ID is required");
			return false;
		}

		try {
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phone));
			sms.setMessage(message);
			sms.setSmsType(smsType);
			sms.setAdOrgID(orgId);
			sms.setAdClientId(clientId != null ? clientId : 0L);
			sms.setLoanId(loanId != null ? loanId : 0L);
			sms.setInstallmentId(installmentId != null ? installmentId : 0L);
			sms.setReminderId(reminderId != null ? reminderId : 0L); // Fixed: was using loanId!
			sms.setTimesTosend(timeToSend);
			sms.setGuarantorId(guarantorId != null ? guarantorId : 0L);
			if (sms.getCreated() == null) {
				sms.setCreated(new Date());
			}

			// Set borrower IDs if available
			if (borrowerIds != null) {
				sms.setIndividualBorrowerId(borrowerIds.getOrDefault("individualId", 0L));
				sms.setGroupBorrowerId(borrowerIds.getOrDefault("groupId", 0L));
				sms.setInstitutionBorrowerId(borrowerIds.getOrDefault("institutionId", 0L));

			}

			return saveSmsRecord(sms);

		} catch (Exception e) {
			log.error("❌ Error in saveReminderSms: {}", e.getMessage(), e);
			return false;
		}
	}

	private String formatPhoneNumberDefaultKenyan(String phone) {
		if (phone == null) {
			return null;
		}

		// Remove all non-digit characters
		String digits = phone.replaceAll("[^0-9]", "");

		// Handle Kenyan phone numbers
		if (digits.startsWith("254") && digits.length() == 12) {
			return digits;
		} else if (digits.startsWith("07") && digits.length() == 10) {
			return "254" + digits.substring(1);
		} else if (digits.startsWith("7") && digits.length() == 9) {
			return "254" + digits;
		} else if (digits.length() >= 9 && digits.length() <= 12) {
			return digits;
		}

		return phone; // Return original if formatting fails
	}

	// --- Helper: Replace placeholders in template ---
	public String processTemplate(String template, Map<String, String> params) {
		if (template == null || params == null)
			return template;

		String result = template;
		for (Map.Entry<String, String> entry : params.entrySet()) {
			String placeholder = "\\{" + entry.getKey() + "\\}";
			String value = entry.getValue() != null ? entry.getValue() : "";
			result = result.replaceAll(placeholder, value);
		}
		return result;
	}

	public void saveSms(long individualBorrowerId, long groupId, long institutionId, String phoneNo, String message,
			long orgId, long clientId, SmsTypeEnum smstype, long installmentId, long laonId, LocalDateTime timeToSend) {
		if (message == null || phoneNo == null) {
			return;
		}

		MSms sms = new MSms();
		sms.setMessageStatus(MessageStatus.PENDING);
		sms.setMessage(message);
		sms.setPhoneNo(phoneNo);
		sms.setIndividualBorrowerId(individualBorrowerId);
		sms.setGroupBorrowerId(groupId);
		sms.setInstitutionBorrowerId(institutionId);
		sms.setLoanId(laonId);
		sms.setInstallmentId(installmentId);
		sms.setAdOrgID(orgId);
		sms.setAdClientId(clientId);
		sms.setSmsType(smstype);
		sms.setTimesTosend(timeToSend);
		smsRepository.save(sms);

	}

	public <T> Page<T> paginate(List<T> list, int page, int size) {

		int start = page * size;
		int end = Math.min(start + size, list.size());

		List<T> subList = (start > end) ? new ArrayList<>() : list.subList(start, end);

		return new PageImpl<>(subList, PageRequest.of(page, size), list.size());
	}

	public void saveManualSms(long individualBorrowerId, long groupId, long institutionId, String phoneNo,
			String message, long orgId, long clientId, SmsTypeEnum smstype, long messagingCenterId) {
		MSms sms = new MSms();
		sms.setMessageStatus(MessageStatus.PENDING);
		sms.setMessage(message);
		sms.setPhoneNo(phoneNo);
		sms.setIndividualBorrowerId(individualBorrowerId);
		sms.setGroupBorrowerId(groupId);
		sms.setInstitutionBorrowerId(institutionId);
		sms.setAdOrgID(orgId);
		sms.setAdClientId(clientId);
		sms.setSmsType(smstype);
		sms.setMessageCenterId(messagingCenterId);
		smsRepository.save(sms);

	}

	public String formatDate(Date date) {
		if (date == null) {
			return "N/A";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
		return sdf.format(date);
	}

	public String formatAmount(Number amount, String currency) {
		if (amount == null) {
			return currency + " 0.00";
		}
		DecimalFormat df = new DecimalFormat("#,##0.00");
		return currency + " " + df.format(amount);
	}

	public String generateReferralCode() {
		StringBuilder code = new StringBuilder("REF-");

		for (int i = 0; i < CODE_LENGTH; i++) {
			int index = random.nextInt(CHARACTERS_CODE.length());
			code.append(CHARACTERS_CODE.charAt(index));
		}

		return code.toString();
	}

	public static String generateRandomPassword() {
		int length = 6 + random.nextInt(7);
		StringBuilder sb = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
		}

		return sb.toString();
	}

	public boolean isSearchEnabled() {
		MADSysConfig conf = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true, getAD_Org_ID(),
				SettingCategoriesEnum.CONFIGURATIONS_SETTINGS);
		if (conf != null) {
			return conf.isSearchEnabled();
		} else {
			return false;
		}

	}

	public MADSysConfig getOrganizationSystemConfiguratins(SettingCategoriesEnum settingCategory) {
		MADSysConfig conf = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true, getAD_Org_ID(),
				settingCategory);
		if (conf != null) {
			return conf;
		} else {
			return null;
		}

	}

	public MADSysConfig getOrganizationSystemConfiguratinsByUser(SettingCategoriesEnum settingCategory, long adOrgId) {
		MADSysConfig conf = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true, adOrgId,
				settingCategory);
		if (conf != null) {
			return conf;
		} else {
			return null;
		}

	}

	public MADSysConfig getOrganizationSystemConfiguratinsByDynamicOrganisation(SettingCategoriesEnum settingCategory,
			long orgId) {
		MADSysConfig conf = sysConfigRepository.findTopByIsActiveAndAdOrgIDAndSettingCategory(true, orgId,
				settingCategory);
		if (conf != null) {
			return conf;
		} else {
			return null;
		}

	}

	public void saveOrganizationSystemConfiguratins(MADSysConfig conf) {
		sysConfigRepository.save(conf);
	}

	public long getAD_Org_ID() {
		MUserClientAudit audit = auditRepository.findByUser(getLoggedInUser());
		long AD_Org_ID = 0;
		if (audit != null) {

			AD_Org_ID = audit.getAD_Org_ID();
			if (AD_Org_ID == 0) {
				AD_Org_ID = userRepo.findById(getAD_User_ID()).map(MUser::getAdOrgId).orElse(0L);

			}
		} else {
			AD_Org_ID = userRepo.findById(getAD_User_ID()).map(MUser::getAdOrgId).orElse(0L);

		}

		return AD_Org_ID;

	}

	public long getAD_Org_IDRole() {

		return userRepo.findById(getAD_User_ID()).map(MUser::getAdOrgId).orElse(0L);

	}

	public String getOrganizationName(long id) {
		MOrg org = mOrgRepository.findById(id).orElse(null);
		if (org != null) {
			return org.getName();
		} else {
			return "-";

		}

	}

	public String getClientName(long id) {
		MClient client = mclientRepository.findById(id).orElse(null);
		if (client != null) {
			return client.getName() != null ? client.getName() : client.getDescription();
		} else {
			return "-";
		}
	}

	public long getAD_Client_ID() {
		long AD_Client_ID = 0;
		MUserClientAudit audit = auditRepository.findByUser(getLoggedInUser());
		if (audit != null) {
			AD_Client_ID = audit.getAD_Client_ID();
			if (AD_Client_ID == 0) {
				AD_Client_ID = userRepo.findById(getAD_User_ID()).map(MUser::getAdClientId).orElse(0L);

			}

		} else {
			AD_Client_ID = userRepo.findById(getAD_User_ID()).map(MUser::getAdClientId).orElse(0L);

		}

		return AD_Client_ID;

	}

	public Set<MRoles> getLogedInUserRoles() {
		Set<MRoles> roles = new HashSet<>();
		MUserClientAudit audit = auditRepository.findByUser(getLoggedInUser());
		if (audit != null) {
			roles = audit.getRoles();
			if (roles.size() == 0) {
				roles = getLoggedInUser().getRoles();
			}

		} else {
			if (getLoggedInUser() == null) {
				return null;
			} else {
				roles = getLoggedInUser().getRoles();
			}

		}

		return roles;

	}

	public boolean isAnySuperUserRole() {
		Set<MRoles> roles = new HashSet<>();
		if (getLoggedInUser() != null) {
			roles = getLoggedInUser().getRoles();
			if (!roles.isEmpty()) {
				for (MRoles role : roles) {
					System.out.println("Role Names:===" + role.getName());
					if (role.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN")) {
						return true;
					}

				}
			}
		}
		return false;

	}

	public boolean isAnyAssistantSuperUserRole() {
		Set<MRoles> roles = new HashSet<>();
		if (getLoggedInUser() != null) {
			roles = getLoggedInUser().getRoles();
			if (!roles.isEmpty()) {
				for (MRoles role : roles) {
					System.out.println("Role Names:===" + role.getName());
					if (role.getName().equalsIgnoreCase("ROLE_SUPER_ADMINISTRATOR")) {
						return true;
					}

				}
			}
		}
		return false;

	}

	public MUser getLoggedInUser() {
		return userRepo.findById(getAD_User_ID()).orElse(null);
	}

	public boolean isBider() {
		if (getLogedInUserRoles() != null && getLogedInUserRoles().size() == 1) {
			for (MRoles role : getLogedInUserRoles()) {
				if (role.getName().equalsIgnoreCase("ROLE_BIDDER")) {
					return true;
				}
			}
		}
		return false;

	}

	public long getAD_Role_ID() {
		Set<MRoles> roles = getLogedInUserRoles();
		MRoles topRole = null;
		if (roles != null) {
			topRole = Collections.max(roles, Comparator.comparingLong(MRoles::getId));
			if (topRole != null) {
				return topRole.getId();
			}

		}
		return 0;

	}

	public boolean isSuperUser() {
		boolean isSuperUser = false;
		if (getLogedInUserRoles() != null && getLogedInUserRoles().size() > 0) {
			for (MRoles role : getLogedInUserRoles()) {
				if (role.getName().equalsIgnoreCase("ROLE_SUPER_ADMIN")) {
					isSuperUser = true;
					return isSuperUser;
				}
			}
		}

		return isSuperUser;
	}

	public boolean isAdmin() {
		if (getLogedInUserRoles() != null && getLogedInUserRoles().size() > 0) {
			for (MRoles role : getLogedInUserRoles()) {
				if (role.getName().contains("ADMIN")) {
					return true;
				}
			}
		}

		return false;
	}

	public long getC_BPartner_ID() {
		long C_Bpartner_ID = 0;
		MUser user = getLoggedInUser();
		if (user != null) {
			C_Bpartner_ID = user.getC_BPartner_ID();
		}

		return C_Bpartner_ID;
	}

	public String uploadFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return null;
		}

		try {
			// Extract original file name and extension (if available)
			String originalFileName = file.getOriginalFilename();
			String extension = "";

			if (originalFileName != null && originalFileName.contains(".")) {
				extension = originalFileName.substring(originalFileName.lastIndexOf("."));
			} else {
				// If no extension, try detecting from content type or stream
				String mimeType = file.getContentType();
				if (mimeType == null || mimeType.isBlank()) {
					// Try to detect from actual content
					Path tempFile = Files.createTempFile("detect-", ".tmp");
					file.transferTo(tempFile.toFile());
					mimeType = Files.probeContentType(tempFile);
					Files.deleteIfExists(tempFile);
				}
				extension = getExtensionFromMimeType(mimeType);
			}

			// Ensure extension has a dot
			if (!extension.startsWith(".")) {
				extension = "." + extension;
			}

			// Generate unique safe name
			String safeFileName = "uploaded_" + System.currentTimeMillis() + extension;

			// Get upload directory from system config
			MADSysConfig config = getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS);
			if (config == null || config.getDocumentUploadDir() == null) {
				throw new IOException("Document upload directory not configured in system settings.");
			}

			Path directoryPath = Paths.get(config.getDocumentUploadDir());
			if (!Files.exists(directoryPath)) {
				Files.createDirectories(directoryPath);
			}

			// Copy file safely
			Path filePath = directoryPath.resolve(safeFileName);
			Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

			return safeFileName;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public FileUploads uploadFileFromBase64(String base64String) {
		if (base64String == null || base64String.isEmpty()) {
			return null;
		}

		try {
			FileUploads upload = new FileUploads();
			String[] parts = base64String.split(",");
			String header = parts.length > 1 ? parts[0] : "";
			String base64Data = parts.length > 1 ? parts[1] : parts[0];

			// Validate base64 data
			if (base64Data == null || base64Data.trim().isEmpty()) {
				throw new IllegalArgumentException("Invalid base64 data");
			}

			byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

			// File size validation (50MB max for videos, 10MB for others)
			long maxFileSize = 50 * 1024 * 1024; // 50MB
			if (decodedBytes.length > maxFileSize) {
				throw new IllegalArgumentException("File size exceeds maximum allowed size of 50MB");
			}

			// Enhanced MIME type detection with comprehensive media support
			String mimeType = null;
			String extension = ".bin";

			System.out.println("DEBUG: Processing file with " + decodedBytes.length + " bytes");

			// Detection priority: Header -> Content -> Enhanced Media -> Fallback
			if (!header.isEmpty() && header.contains(";base64")) {
				mimeType = header.substring(header.indexOf(":") + 1, header.indexOf(";"));
				extension = getExtensionFromMimeType(mimeType);
				System.out.println("DEBUG: Header detection - MIME: " + mimeType + ", Extension: " + extension);
			}

			// Content-based detection
			if (".bin".equals(extension) || "application/octet-stream".equals(mimeType)) {
				extension = detectExtensionFromContent(decodedBytes);
				mimeType = getMimeTypeFromExtension(extension);
				System.out.println("DEBUG: Content detection - MIME: " + mimeType + ", Extension: " + extension);
			}

			// Enhanced media detection for problematic files
			if (".bin".equals(extension) || "application/octet-stream".equals(mimeType)) {
				extension = enhancedMediaDetection(decodedBytes);
				mimeType = getMimeTypeFromExtension(extension);
				System.out.println("DEBUG: Enhanced media detection - MIME: " + mimeType + ", Extension: " + extension);
			}

			// Final fallback for undetectable files - detect JSON and CSV files
			if (".bin".equals(extension) || "application/octet-stream".equals(mimeType)) {
				System.err.println("WARNING: File type undetermined. First 16 bytes: " + bytesToHex(decodedBytes, 16));
				extension = detectTextBasedFormats(decodedBytes);
				mimeType = getMimeTypeFromExtension(extension);
				System.out.println("DEBUG: Text format detection - MIME: " + mimeType + ", Extension: " + extension);
			}

			// Validate file type is allowed
			if (!isFileTypeAllowed(mimeType, extension)) {
				throw new IllegalArgumentException(
						"File type not allowed: " + mimeType + " (extension: " + extension + ")");
			}

			String fileName = "uploaded_" + System.currentTimeMillis() + extension;

			MADSysConfig config = getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS);
			if (config == null || config.getDocumentUploadDir() == null) {
				throw new IOException("Document upload directory not configured in system settings.");
			}

			Path directoryPath = Paths.get(config.getDocumentUploadDir());
			if (!Files.exists(directoryPath)) {
				Files.createDirectories(directoryPath);
			}

			Path filePath = directoryPath.resolve(fileName);
			Files.write(filePath, decodedBytes);
			upload.setFileName(fileName);
			upload.setFullFilePath(filePath.toString());
			upload.setMimeType(mimeType);
			upload.setFileSize((long) decodedBytes.length);

			System.out.println("DEBUG: Successfully uploaded: " + fileName + " as " + mimeType);
			return upload;
		} catch (IOException | IllegalArgumentException e) {
			System.err.println("ERROR: Failed to upload file: " + e.getMessage());
			return null;
		}
	}

	// NEW: Improved text-based format detection
	private String detectTextBasedFormats(byte[] data) {
		if (data == null || data.length < 10)
			return ".bin";

		try {
			String content = new String(data, 0, Math.min(data.length, 1000), StandardCharsets.UTF_8);

			// JSON detection - starts with { or [
			if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
				// Validate JSON structure
				if (isValidJson(content)) {
					System.out.println("DEBUG: Detected JSON file");
					return ".json";
				}
			}

			// CSV detection - contains comma-separated values with headers
			if (isLikelyCsv(content)) {
				System.out.println("DEBUG: Detected CSV file");
				return ".csv";
			}

			// XML detection - starts with <?xml or <root>
			if (content.trim().startsWith("<?xml")
					|| (content.contains("<") && content.contains(">") && isLikelyXml(content))) {
				System.out.println("DEBUG: Detected XML file");
				return ".xml";
			}

			// Plain text fallback
			if (isLikelyTextFile(data)) {
				System.out.println("DEBUG: Fallback to plain text");
				return ".txt";
			}

		} catch (Exception e) {
			System.err.println("DEBUG: Error in text format detection: " + e.getMessage());
		}

		return ".bin";
	}

	// NEW: JSON validation
	private boolean isValidJson(String content) {
		try {
			// Simple JSON validation - check for balanced braces/brackets
			String trimmed = content.trim();
			if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
					|| (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
				return true;
			}

			// Check for common JSON patterns
			if (trimmed.contains("\":") && (trimmed.contains("{") || trimmed.contains("["))) {
				return true;
			}

			return false;
		} catch (Exception e) {
			return false;
		}
	}

	// NEW: Improved CSV detection
	private boolean isLikelyCsv(String content) {
		if (content == null || content.length() < 20)
			return false;

		String[] lines = content.split("\n");
		if (lines.length < 2)
			return false;

		// Check first line for header-like pattern (multiple commas)
		String firstLine = lines[0].trim();
		int commaCount = 0;
		for (char c : firstLine.toCharArray()) {
			if (c == ',')
				commaCount++;
		}

		// Should have multiple commas and reasonable field count
		if (commaCount < 2)
			return false;

		// Check if subsequent lines have similar comma count
		if (lines.length >= 2) {
			String secondLine = lines[1].trim();
			int secondCommaCount = 0;
			for (char c : secondLine.toCharArray()) {
				if (c == ',')
					secondCommaCount++;
			}

			// Allow some variation in comma count (for optional fields)
			return Math.abs(commaCount - secondCommaCount) <= 2;
		}

		return true;
	}

	// NEW: XML detection
	private boolean isLikelyXml(String content) {
		if (content == null)
			return false;

		// Check for XML declaration or root element
		if (content.contains("<?xml"))
			return true;

		// Check for opening and closing tags
		if (content.contains("<") && content.contains(">") && content.contains("</")) {
			return true;
		}

		return false;
	}

	// COMPREHENSIVE: MIME type to extension mapping
	private String getExtensionFromMimeType(String mimeType) {
		if (mimeType == null)
			return ".bin";

		switch (mimeType.toLowerCase()) {
		// Images
		case "image/png":
			return ".png";
		case "image/jpeg":
		case "image/jpg":
			return ".jpg";
		case "image/gif":
			return ".gif";
		case "image/webp":
			return ".webp";
		case "image/bmp":
			return ".bmp";
		case "image/svg+xml":
			return ".svg";
		case "image/tiff":
			return ".tiff";
		case "image/x-icon":
			return ".ico";

		// Documents
		case "application/pdf":
			return ".pdf";
		case "text/plain":
			return ".txt";
		case "application/msword":
			return ".doc";
		case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
			return ".docx";
		case "application/vnd.ms-excel":
			return ".xls";
		case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
			return ".xlsx";
		case "application/vnd.ms-powerpoint":
			return ".ppt";
		case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
			return ".pptx";

		// Videos
		case "video/mp4":
			return ".mp4";
		case "video/mpeg":
			return ".mpeg";
		case "video/ogg":
			return ".ogv";
		case "video/webm":
			return ".webm";
		case "video/quicktime":
			return ".mov";
		case "video/x-msvideo":
			return ".avi";
		case "video/x-ms-wmv":
			return ".wmv";
		case "video/3gpp":
			return ".3gp";
		case "video/mp2t":
			return ".ts";

		// Audio
		case "audio/mpeg":
			return ".mp3";
		case "audio/wav":
			return ".wav";
		case "audio/ogg":
			return ".oga";
		case "audio/aac":
			return ".aac";
		case "audio/webm":
			return ".weba";
		case "audio/x-wav":
			return ".wav";

		// Archives
		case "application/zip":
			return ".zip";
		case "application/x-rar-compressed":
			return ".rar";
		case "application/x-7z-compressed":
			return ".7z";
		case "application/gzip":
			return ".gz";
		case "application/x-tar":
			return ".tar";

		// Other documents
		case "application/rtf":
			return ".rtf";
		case "text/csv":
			return ".csv";
		case "application/json":
			return ".json";
		case "application/xml":
			return ".xml";
		case "text/html":
			return ".html";

		default:
			return ".bin";
		}
	}

	// COMPREHENSIVE: Primary content-based detection
	private String detectExtensionFromContent(byte[] data) {
		if (data == null || data.length < 4)
			return ".bin";

		// Images
		// PNG: 89 50 4E 47
		if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47)
			return ".png";

		// JPEG: FF D8 FF
		if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF)
			return ".jpg";

		// GIF: 47 49 46 38
		if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x38)
			return ".gif";

		// PDF: 25 50 44 46 (%PDF)
		if (data[0] == 0x25 && data[1] == 0x50 && data[2] == 0x44 && data[3] == 0x46)
			return ".pdf";

		// BMP: 42 4D (BM)
		if (data[0] == 0x42 && data[1] == 0x4D)
			return ".bmp";

		// WebP: 52 49 46 46 x x x x 57 45 42 50 (RIFF....WEBP)
		if (data.length >= 12 && data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46
				&& data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50)
			return ".webp";

		// ZIP-based formats (Office documents)
		if (data.length >= 4 && data[0] == 0x50 && data[1] == 0x4B && data[2] == 0x03 && data[3] == 0x04) {
			return detectOfficeDocumentType(data);
		}

		// Media files - Basic detection
		// MP4: ftyp at various positions
		if (data.length >= 12 && ((data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p')
				|| (data[8] == 'f' && data[9] == 't' && data[10] == 'y' && data[11] == 'p')
				|| (data[0] == 0x00 && data[1] == 0x00 && data[2] == 0x00 && (data[3] == 0x18 || data[3] == 0x20)
						&& data[4] == 0x66 && data[5] == 0x74 && data[6] == 0x79 && data[7] == 0x70)))
			return ".mp4";

		// MP3: MPEG frame or ID3 tag
		if ((data.length >= 3 && data[0] == (byte) 0xFF && (data[1] & 0xE0) == 0xE0)
				|| (data.length >= 10 && data[0] == 'I' && data[1] == 'D' && data[2] == '3'))
			return ".mp3";

		// WAV: 52 49 46 46 x x x x 57 41 56 45 (RIFF....WAVE)
		if (data.length >= 12 && data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46
				&& data[8] == 0x57 && data[9] == 0x41 && data[10] == 0x56 && data[11] == 0x45)
			return ".wav";

		// AVI: 52 49 46 46 x x x x 41 56 49 20 (RIFF....AVI )
		if (data.length >= 12 && data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46
				&& data[8] == 0x41 && data[9] == 0x56 && data[10] == 0x49 && data[11] == 0x20)
			return ".avi";

		return ".bin";
	}

	// ENHANCED: Office document type detection
	private String detectOfficeDocumentType(byte[] data) {
		if (data.length < 100)
			return ".zip";

		try {
			String headerContent = new String(data, 0, Math.min(data.length, 10000), StandardCharsets.UTF_8);

			// Word detection
			if (headerContent.contains("word/") || headerContent.contains("wordprocessingml")
					|| headerContent.contains("WordDocument"))
				return ".docx";

			// Excel detection
			if (headerContent.contains("xl/") || headerContent.contains("spreadsheetml")
					|| headerContent.contains("workbook.xml"))
				return ".xlsx";

			// PowerPoint detection
			if (headerContent.contains("ppt/") || headerContent.contains("presentationml")
					|| headerContent.contains("slides/"))
				return ".pptx";

			// Byte pattern fallback
			if (containsBytePattern(data, "word/".getBytes(StandardCharsets.UTF_8)))
				return ".docx";
			if (containsBytePattern(data, "xl/".getBytes(StandardCharsets.UTF_8)))
				return ".xlsx";
			if (containsBytePattern(data, "ppt/".getBytes(StandardCharsets.UTF_8)))
				return ".pptx";

			return ".zip";
		} catch (Exception e) {
			return ".zip";
		}
	}

	// COMPREHENSIVE: Enhanced media file detection
	private String enhancedMediaDetection(byte[] data) {
		if (data == null || data.length < 12)
			return ".bin";

		System.out.println("DEBUG: Enhanced media detection for " + data.length + " bytes");

		// Enhanced MP4 detection
		if (isLikelyMP4(data)) {
			System.out.println("DEBUG: Enhanced MP4 detection successful");
			return ".mp4";
		}

		// Enhanced MP3 detection
		if (isLikelyMP3(data)) {
			System.out.println("DEBUG: Enhanced MP3 detection successful");
			return ".mp3";
		}

		// Enhanced AVI detection
		if (isLikelyAVI(data)) {
			System.out.println("DEBUG: Enhanced AVI detection successful");
			return ".avi";
		}

		// Enhanced WAV detection
		if (isLikelyWAV(data)) {
			System.out.println("DEBUG: Enhanced WAV detection successful");
			return ".wav";
		}

		// Enhanced MOV detection
		if (isLikelyMOV(data)) {
			System.out.println("DEBUG: Enhanced MOV detection successful");
			return ".mov";
		}

		// Enhanced WebM detection
		if (isLikelyWebM(data)) {
			System.out.println("DEBUG: Enhanced WebM detection successful");
			return ".webm";
		}

		return ".bin";
	}

	// ADVANCED: MP4 detection with multiple signatures
	private boolean isLikelyMP4(byte[] data) {
		if (data.length < 16)
			return false;

		// Check for 'ftyp' at various common positions
		for (int i = 4; i <= 12 && i + 4 <= data.length; i += 4) {
			if (data[i] == 'f' && data[i + 1] == 't' && data[i + 2] == 'y' && data[i + 3] == 'p') {
				return true;
			}
		}

		// Check for MP4 specific atoms
		for (int i = 0; i < Math.min(data.length - 8, 2000); i++) {
			// Look for 'moov', 'mdat', 'free' atoms
			if ((data[i] == 'm' && data[i + 1] == 'o' && data[i + 2] == 'o' && data[i + 3] == 'v')
					|| (data[i] == 'm' && data[i + 1] == 'd' && data[i + 2] == 'a' && data[i + 3] == 't')
					|| (data[i] == 'f' && data[i + 1] == 'r' && data[i + 2] == 'e' && data[i + 3] == 'e')) {
				return true;
			}
		}

		return false;
	}

	// ADVANCED: MP3 detection with comprehensive pattern matching
	private boolean isLikelyMP3(byte[] data) {
		if (data.length < 10)
			return false;

		// ID3v2 tag at beginning
		if (data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
			return true;
		}

		// ID3v1 tag at end (128 bytes from end)
		if (data.length >= 128) {
			if (data[data.length - 128] == 'T' && data[data.length - 127] == 'A' && data[data.length - 126] == 'G') {
				return true;
			}
		}

		// MPEG frame sync detection throughout file
		int mp3FrameCount = 0;
		int checkLimit = Math.min(data.length, 10000);

		for (int i = 0; i < checkLimit - 4; i++) {
			// MPEG frame sync: 11111111 111 (0xFF 0xE0)
			if (data[i] == (byte) 0xFF && (data[i + 1] & 0xE0) == 0xE0) {
				mp3FrameCount++;
				i += 100; // Skip ahead to avoid counting same frame multiple times
			}
		}

		if (mp3FrameCount >= 2) {
			System.out.println("DEBUG: Found " + mp3FrameCount + " MP3 frames");
			return true;
		}

		return false;
	}

	// ADVANCED: AVI detection
	private boolean isLikelyAVI(byte[] data) {
		if (data.length < 16)
			return false;

		// Standard AVI signature
		if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' && data.length > 12 && data[8] == 'A'
				&& data[9] == 'V' && data[10] == 'I' && data[11] == ' ') {
			return true;
		}

		// Alternative AVI detection
		for (int i = 0; i < Math.min(data.length - 8, 1000); i++) {
			if (data[i] == 'a' && data[i + 1] == 'v' && data[i + 2] == 'i' && data[i + 3] == 'h') {
				return true;
			}
			if (data[i] == 's' && data[i + 1] == 't' && data[i + 2] == 'r' && data[i + 3] == 'h') {
				return true;
			}
		}

		return false;
	}

	// ADVANCED: WAV detection
	private boolean isLikelyWAV(byte[] data) {
		if (data.length < 16)
			return false;

		// Standard WAV signature
		if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' && data.length > 12 && data[8] == 'W'
				&& data[9] == 'A' && data[10] == 'V' && data[11] == 'E') {
			return true;
		}

		// Look for 'fmt ' chunk
		for (int i = 12; i < Math.min(data.length - 8, 100); i++) {
			if (data[i] == 'f' && data[i + 1] == 'm' && data[i + 2] == 't' && data[i + 3] == ' ') {
				return true;
			}
		}

		return false;
	}

	// NEW: MOV (QuickTime) detection
	private boolean isLikelyMOV(byte[] data) {
		if (data.length < 16)
			return false;

		// QuickTime file format signatures
		for (int i = 4; i <= 12 && i + 8 <= data.length; i += 4) {
			if ((data[i] == 'm' && data[i + 1] == 'o' && data[i + 2] == 'o' && data[i + 3] == 'v')
					|| (data[i] == 'f' && data[i + 1] == 'r' && data[i + 2] == 'e' && data[i + 3] == 'e')
					|| (data[i] == 'm' && data[i + 1] == 'd' && data[i + 2] == 'a' && data[i + 3] == 't')
					|| (data[i] == 'w' && data[i + 1] == 'i' && data[i + 2] == 'd' && data[i + 3] == 'e')) {
				return true;
			}
		}

		return false;
	}

	// NEW: WebM detection
	private boolean isLikelyWebM(byte[] data) {
		if (data.length < 16)
			return false;

		// WebM files start with EBML header
		if (data[0] == 0x1A && data[1] == 0x45 && data[2] == 0xDF && data[3] == 0xA3) {
			return true;
		}

		return false;
	}

	// COMPREHENSIVE: Extension to MIME type mapping
	private String getMimeTypeFromExtension(String extension) {
		if (extension == null)
			return "application/octet-stream";

		switch (extension.toLowerCase()) {
		case ".png":
			return "image/png";
		case ".jpg":
		case ".jpeg":
			return "image/jpeg";
		case ".gif":
			return "image/gif";
		case ".webp":
			return "image/webp";
		case ".bmp":
			return "image/bmp";
		case ".svg":
			return "image/svg+xml";
		case ".tiff":
		case ".tif":
			return "image/tiff";
		case ".ico":
			return "image/x-icon";

		case ".pdf":
			return "application/pdf";
		case ".txt":
			return "text/plain";
		case ".doc":
			return "application/msword";
		case ".docx":
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		case ".xls":
			return "application/vnd.ms-excel";
		case ".xlsx":
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		case ".ppt":
			return "application/vnd.ms-powerpoint";
		case ".pptx":
			return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

		case ".mp4":
			return "video/mp4";
		case ".mpeg":
		case ".mpg":
			return "video/mpeg";
		case ".ogv":
			return "video/ogg";
		case ".webm":
			return "video/webm";
		case ".mov":
			return "video/quicktime";
		case ".avi":
			return "video/x-msvideo";
		case ".wmv":
			return "video/x-ms-wmv";
		case ".3gp":
			return "video/3gpp";
		case ".ts":
			return "video/mp2t";

		case ".mp3":
			return "audio/mpeg";
		case ".wav":
			return "audio/wav";
		case ".oga":
			return "audio/ogg";
		case ".aac":
			return "audio/aac";
		case ".weba":
			return "audio/webm";

		case ".zip":
			return "application/zip";
		case ".rar":
			return "application/x-rar-compressed";
		case ".7z":
			return "application/x-7z-compressed";
		case ".gz":
			return "application/gzip";
		case ".tar":
			return "application/x-tar";

		case ".rtf":
			return "application/rtf";
		case ".csv":
			return "text/csv";
		case ".json":
			return "application/json";
		case ".xml":
			return "application/xml";
		case ".html":
		case ".htm":
			return "text/html";

		default:
			return "application/octet-stream";
		}
	}

	// COMPREHENSIVE: File type security validation
	private boolean isFileTypeAllowed(String mimeType, String extension) {
		if (mimeType == null || extension == null)
			return false;

		Set<String> allowedMimeTypes = Set.of(
				// Images
				"image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp", "image/bmp", "image/svg+xml",
				"image/tiff", "image/x-icon",

				// Documents
				"application/pdf", "text/plain", "application/msword",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-powerpoint",
				"application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/rtf",
				"text/csv", "application/json", "application/xml", "text/html",

				// Videos
				"video/mp4", "video/mpeg", "video/ogg", "video/webm", "video/quicktime", "video/x-msvideo",
				"video/x-ms-wmv", "video/3gpp", "video/mp2t",

				// Audio
				"audio/mpeg", "audio/wav", "audio/ogg", "audio/aac", "audio/webm", "audio/x-wav",

				// Archives
				"application/zip", "application/x-rar-compressed", "application/x-7z-compressed", "application/gzip",
				"application/x-tar");

		return allowedMimeTypes.contains(mimeType.toLowerCase());
	}

	// UTILITY: Byte pattern matching
	private boolean containsBytePattern(byte[] data, byte[] pattern) {
		if (data == null || pattern == null || data.length < pattern.length)
			return false;

		for (int i = 0; i <= data.length - pattern.length; i++) {
			boolean found = true;
			for (int j = 0; j < pattern.length; j++) {
				if (data[i + j] != pattern[j]) {
					found = false;
					break;
				}
			}
			if (found)
				return true;
		}
		return false;
	}

	// UTILITY: Hex conversion for debugging
	private String bytesToHex(byte[] bytes, int length) {
		if (bytes == null || bytes.length == 0)
			return "";
		int len = Math.min(bytes.length, length);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			sb.append(String.format("%02X ", bytes[i]));
		}
		return sb.toString().trim();
	}

	// UTILITY: Text file detection
	private boolean isLikelyTextFile(byte[] data) {
		if (data.length == 0)
			return false;

		int textChars = 0;
		int sampleSize = Math.min(data.length, 1000);

		for (int i = 0; i < sampleSize; i++) {
			byte b = data[i];
			// Printable ASCII + common whitespace
			if ((b >= 0x20 && b <= 0x7E) || b == 0x09 || b == 0x0A || b == 0x0D) {
				textChars++;
			}
		}

		return (textChars * 100 / sampleSize) > 95;
	}

	public String getFilePath() {
		MADSysConfig config = this.getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS);

		if (config != null && config.getFilePaths() != null) {
			String path = config.getFilePaths().trim();

			if (!path.endsWith("/")) {
				path = path + "/";
			}

			return path;
		}

		return null;
	}

	public String formatPhoneNumber(String phoneNumber, String defaultRegion) {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

		try {
			// Parse with default region (e.g. "KE" for Kenya, "US" for USA)
			Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNumber, defaultRegion);

			if (phoneUtil.isValidNumber(numberProto)) {
				// Format to international (E.164) e.g. +254712345678
				return phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
			} else {
				return "Invalid phone number";
			}
		} catch (NumberParseException e) {
			return "Invalid phone number";
		}
	}

	public static int getCurrentYear() {
		return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static void updateCommonFields() {

	}

	public long getAD_User_ID() {
		Optional<Long> userId = auditorAware.getCurrentAuditor();
		return userId.orElse(0L);

	}

	public static String toReadableFormat(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		String[] parts = input.toLowerCase().split("_");

		StringBuilder readable = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty())
				continue;
			readable.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
		}

		return readable.toString().trim();
	}

	public String encodePassword(String rawPassword) {
		String encodedpassword = passwordEncoder.encode(rawPassword);
		return encodedpassword;

	}

	public UserResponse mapUser(MUser user) {
		if (user == null) {
			return null;
		}
		// TODO Auto-generated method stub
		UserResponse response = new UserResponse();
		response.setDocumentNo(user.getDocumentNo());
		response.setEmail(user.getEmail());
		response.setExternalRefrenceNo(user.getExternalRefrenceNo());
		response.setFirstName(user.getFirstName());
		response.setLastName(user.getLastName());
		response.setDateOfBirth(user.getDateOfBirth());
		response.setUserId(user.getUserId());
		response.setActive(user.isActive());
		response.setCreated(user.getCreated());
		response.setCreatedBy(user.getCreatedBy());
		response.setUpdated(user.getUpdated());
		response.setUpdatedBy(user.getUpdatedBy());
		response.setOrganizationName(user.getOrganizationName());
		response.setPhoneNumber(user.getPhoneNumber());
		response.setClientName(user.getClientName());
		response.setGender(user.getGender());
		if (user.getFullName() != null) {
			response.setFullName(user.getFullName());
		} else {
			response.setFullName(user.getFirstName() + " " + user.getLastName());
		}
		Set<RoleResponse> roles = new HashSet<>();
		if (user.getRoles() != null) {
			for (MRoles role : user.getRoles()) {
				RoleResponse rol = mapRole(role);
				roles.add(rol);
			}
			response.setRoles(roles);
		}
		response.setCountryCode(user.getCountryCode());
		response.setCountryName(user.getCountryName());
		response.setTimeZone(user.getTimezone());
		response.setLanguage(user.getLanguages());
		response.setCity(user.getCity());
		response.setCurrency(user.getCurrency());
		response.setNoOftimesLoggedIn(user.getNoOftimesLoggedIn());
		response.setCountryCapital(user.getCountryCapital());
		if (user.getReferredBy() != null) {
			response.setReferredBy(user.getReferredBy().getFullName());
		}

		return response;
	}

	public User mapUserBreif(MUser user) {
		if (user == null) {
			return null;
		}
		// TODO Auto-generated method stub
		User response = new User();
		response.setEmail(user.getEmail());
		response.setFirstName(user.getFirstName());
		response.setLastName(user.getLastName());
		response.setUserId(user.getUserId());
		response.setExternalRefrenceNo(user.getExternalRefrenceNo());
		response.setCreated(user.getCreated());

		response.setPhoneNumber(user.getPhoneNumber());
		response.setGender(user.getGender());
		if (user.getFullName() != null) {
			response.setFullName(user.getFullName());
		} else {
			response.setFullName(user.getFirstName() + " " + user.getLastName());
		}

		return response;
	}

	public UserResponse mapLoggedInUser() {
		MUser user = getLoggedInUser();
		if (user == null) {
			return null;
		}
		// TODO Auto-generated method stub
		UserResponse response = new UserResponse();
		response.setDocumentNo(user.getDocumentNo());
		response.setEmail(user.getEmail());
		response.setFirstName(user.getFirstName());
		response.setLastName(user.getLastName());
		response.setExternalRefrenceNo(user.getExternalRefrenceNo());
		response.setUserId(user.getUserId());
		response.setActive(user.isActive());
		response.setCreated(user.getCreated());
		response.setUpdated(user.getUpdated());
		response.setUpdatedBy(user.getUpdatedBy());
		response.setGender(user.getGender());
		response.setOrganizationName(user.getOrganizationName());
		response.setPhoneNumber(user.getPhoneNumber());
		response.setClientName(user.getClientName());
		if (user.getFullName() != null) {
			response.setFullName(user.getFullName());
		} else {
			response.setFullName(user.getFirstName() + " " + user.getLastName());
		}
		Set<RoleResponse> roles = new HashSet<>();
		if (getLogedInUserRoles() != null) {
			for (MRoles role : getLogedInUserRoles()) {
				RoleResponse rol = mapRole(role);
				roles.add(rol);
			}
			response.setRoles(roles);
		}
//		if (user.getReferredBy() != null) {
//			response.setReferredBy(this.mapUser(user.getReferredBy()));
//		}
		return response;
	}

	public RoleResponse mapRole(MRoles r) {
		if (r == null) {
			return null;
		}
		Set<OrgResponse> allowedOrganisations = new HashSet<>();
		if (!r.getAllowedOrganisations().isEmpty()) {
			for (MOrg org : r.getAllowedOrganisations()) {
				allowedOrganisations.add(mapOrganisation(org));
			}
		}

		RoleResponse role = new RoleResponse(r.getId(), r.getName(), r.getFormattedName().replace("Role ", ""),
				r.isAllowMultiOrgAccess(), allowedOrganisations);
		return role;
	}

	public MWFMail sendOTPEmail(MUser mUser, int otp) {
		MWFMail wfMail = new MWFMail();
		MEmail email = emailRepository.findTop1ByIsActiveAndAdOrgID(true, getAD_Org_ID());
		if (email != null) {

			String mailTemplate = "<div style=\"font-family: Arial, Helvetica, sans-serif; background-color:#f9fafb; padding:20px; color:#333;\">"
					+ "  <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:600px; margin:auto; background:#ffffff; border-radius:8px; box-shadow:0 2px 6px rgba(0,0,0,0.1);\">"
					+

					// Header with Logo
					"    <tr>" + "      <td align=\"center\" style=\"padding:20px; border-bottom:1px solid #e5e7eb;\">"
					+ "        <img src='"
					+ getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS).getFilePaths()
					+ "logo.png' alt='Company Logo' style='max-height:50px;'/>" + "      </td>" + "    </tr>" +

					// Title
					"    <tr>" + "      <td style=\"padding:20px; font-size:18px; font-weight:600; color:#0d3e6d;\">"
					+ "        Login Verification" + "      </td>" + "    </tr>" +

					// Greeting + Message
					"    <tr>" + "      <td style=\"padding:0 20px 20px 20px; font-size:14px; color:#374151;\">"
					+ "        Dear <strong>" + mUser.getFirstName() + "</strong>,<br/><br/>"
					+ "        Please use the One-Time Password (OTP) below to complete your login:" + "      </td>"
					+ "    </tr>" +

					// OTP Display
					"    <tr>" + "      <td align=\"center\" style=\"padding:20px;\">"
					+ "        <div style=\"font-size:26px; font-weight:bold; color:#0d3e6d; letter-spacing:4px;\">"
					+ otp + "        </div>" + "      </td>" + "    </tr>" +

					// Note
					"    <tr>" + "      <td style=\"padding:0 20px 20px 20px; font-size:13px; color:#6b7280;\">"
					+ "        This is a system-generated code for two-factor authentication. It is valid for a single use only."
					+ "      </td>" + "    </tr>" +

					// System Link
					"    <tr>"
					+ "      <td style=\"padding:20px; background:#f3f4f6; font-size:13px; color:#374151; border-top:1px solid #e5e7eb;\">"
					+ "        Access your account here: <a href='"
					+ getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS).getDomainUrl()
					+ "' style=\"color:#0d3e6d; text-decoration:none; font-weight:500;\">"
					+ getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS).getDomainUrl() + "</a>"
					+ "      </td>" + "    </tr>" +

					// Footer Disclaimer
					"    <tr>"
					+ "      <td style=\"padding:20px; font-size:12px; color:#9ca3af; text-align:center; border-top:1px solid #e5e7eb;\">"
					+ "        If you did not request this login attempt, please ignore this email or contact support immediately.<br/><br/>"
					+ "        &copy; " + java.time.Year.now() + " "
					+ getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS).getPlatformName()
					+ ". All rights reserved." + "      </td>" + "    </tr>" +

					"  </table>" + "</div>";

			wfMail.setMailFrom(email.getUsername());
			wfMail.setMailTo(mUser.getEmail());
			wfMail.setMailSubject("OTP Code");

			wfMail.setMailContent(mailTemplate);
			return mailRepository.save(wfMail);

		} else {
			return null;
		}

	}

	public void sendGuarantorEmail(MNextOfKin guarantor, String message, String subject, Long orgId) {
		MADSysConfig sys = getOrganizationSystemConfiguratinsByUser(SettingCategoriesEnum.GENERAL_SETTINGS, orgId);
		MEmail email = emailRepository.findTop1ByIsActiveAndAdOrgID(true, orgId);

		if (email != null && guarantor != null && guarantor.getEmail() != null) {
			String htmlContent = "<html>"
					+ "<body style='font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;'>"
					+ "<div style='max-width: 600px; margin: auto; background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>"
					+ "<h2 style='color: #333; border-bottom: 2px solid #FF9800; padding-bottom: 10px;'>" + subject
					+ "</h2>" + "<p style='font-size: 14px; color: #666;'>Dear <strong>" + guarantor.getFullName()
					+ "</strong>,</p>"
					+ "<div style='background-color: #f9f9f9; padding: 15px; border-left: 4px solid #FF9800; margin: 15px 0;'>"
					+ "<p style='font-size: 14px; color: #333; line-height: 1.6;'>" + message.replace("\n", "<br>")
					+ "</p>" + "</div>"
					+ "<p style='font-size: 12px; color: #999;'>This is an automated notification regarding your guarantor obligations.</p>"
					+ "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>"
					+ "<p style='font-size: 12px; color: #999; text-align: center;'>" + "© " + java.time.Year.now()
					+ " " + (sys != null ? sys.getPlatformName() : "Our System") + ". All rights reserved." + "</p>"
					+ "</div>" + "</body>" + "</html>";

			MWFMail wfMail = new MWFMail();
			wfMail.setMailFrom(email.getUsername());
			wfMail.setMailTo(guarantor.getEmail());
			wfMail.setMailSubject(subject);
			wfMail.setMailContent(htmlContent);
			mailRepository.save(wfMail);

			log.debug("✅ Guarantor email saved for {}", guarantor.getEmail());
		} else {
			log.warn("Cannot send guarantor email: Missing email configuration or guarantor email");
		}
	}

	public String getSystemDomainUrl(long adOrgId) {
		return getOrganizationSystemConfiguratinsByUser(SettingCategoriesEnum.GENERAL_SETTINGS, adOrgId) != null
				? getOrganizationSystemConfiguratinsByUser(SettingCategoriesEnum.GENERAL_SETTINGS, adOrgId)
						.getDomainUrl()
				: "";
	}

	public int generatedOTP(long userId) {
		int otp = 0;
		Random rand = new Random();
		otp = 1000 + rand.nextInt(9000);
		MUser user = userRepo.findById(userId).orElse(null);
		if (user != null) {
			user.setOtpCode(otp);
			userRepo.save(user);
		}

		System.out.println("Generated OTP: " + otp);

		return otp;
	}

	public int generatedSixDigitOTP(long userId) {
		int otp = 100000 + new Random().nextInt(900000);

		MUser user = userRepo.findById(userId).orElse(null);
		if (user != null) {
			user.setOtpCode(otp);
			userRepo.save(user);
		}

		System.out.println("Generated OTP: " + otp);
		return otp;
	}

	public void sendEmail(MUser user, String message, String subject) {
		MWFMail wfMail = new MWFMail();
		MADSysConfig sys = getOrganizationSystemConfiguratinsByUser(SettingCategoriesEnum.GENERAL_SETTINGS,
				user.getAdOrgId());
		MEmail email = emailRepository.findTop1ByIsActiveAndAdOrgID(true, user.getAdOrgId());
		if (email != null) {

			String htmlContent = "<html>"
					+ "  <body style=\"font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px;\">"
					+ "    <div style=\"max-width: 600px; margin: auto; background: #ffffff; border-radius: 10px; "
					+ "                box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;\">"
					+ "      <h2 style=\"color: #2c3e50; text-align: center;\">" + subject + "</h2>"
					+ "      <p style=\"font-size: 16px; color: #333333; line-height: 1.5;\">" + "        Dear <strong>"
					+ user.getFirstName() + " " + user.getLastName() + "</strong>," + "      </p>"
					+ "      <p style=\"font-size: 15px; color: #555555; line-height: 1.6;\">" + message + "</p>"
					+ "      <div style=\"margin-top: 30px; text-align: center;\">" + "        <a href=\""
					+ sys.getDomainUrl() + "\" "
					+ "           style=\"background-color: #3498db; color: white; padding: 12px 24px; "
					+ "                  text-decoration: none; border-radius: 6px; font-size: 14px;\">"
					+ "          Visit Our Website" + "        </a>" + "      </div>"
					+ "      <hr style=\"margin: 30px 0; border: none; border-top: 1px solid #eee;\">"
					+ "      <p style=\"font-size: 13px; color: #888888; text-align: center;\">" + "        ©"
					+ getCurrentYear() + " " + sys.getPlatformName() + ". All rights reserved." + "      </p>"
					+ "    </div>" + "  </body>" + "</html>";

			wfMail.setMailFrom(email.getUsername());
			wfMail.setMailTo(user.getEmail());
			wfMail.setMailSubject(subject);

			wfMail.setMailContent(htmlContent);
			mailRepository.save(wfMail);
		}

	}

	private LocalDateTime calculateMonthlyNextDateTime(LocalDateTime fromDateTime, Integer cycleDay) {
		int day = (cycleDay != null && cycleDay >= 1 && cycleDay <= 31) ? cycleDay : 1;
// Build candidate date with the target day, same month and year as fromDateTime
		LocalDate candidateDate;
		try {
			candidateDate = fromDateTime.toLocalDate().withDayOfMonth(day);
		} catch (DateTimeException e) {
// Day out of range for this month (e.g., 31 in April) – use last day of month
			candidateDate = fromDateTime.toLocalDate().withDayOfMonth(fromDateTime.toLocalDate().lengthOfMonth());
		}
// If candidate is after or equal to fromDateTime, it's the next occurrence
		if (!candidateDate.isAfter(fromDateTime.toLocalDate())) {
			candidateDate = candidateDate.plusMonths(1);
// After adding a month, day may need adjustment (e.g., Jan 31 + 1 month = Feb 28/29)
// Use withDayOfMonth again to handle safely
			try {
				candidateDate = candidateDate.withDayOfMonth(day);
			} catch (DateTimeException e) {
				candidateDate = candidateDate.withDayOfMonth(candidateDate.lengthOfMonth());
			}
		}
// Preserve the original time
		return candidateDate.atTime(fromDateTime.toLocalTime());
	}

	private LocalDateTime calculateSpecificDayNextDateTime(LocalDateTime fromDateTime, Integer dayOfMonth) {
		if (dayOfMonth == null)
			return null;
// Similar to monthly but using the day only (month stays the same, then advance)
		LocalDate candidateDate;
		try {
			candidateDate = fromDateTime.toLocalDate().withDayOfMonth(dayOfMonth);
		} catch (DateTimeException e) {
			candidateDate = fromDateTime.toLocalDate().withDayOfMonth(fromDateTime.toLocalDate().lengthOfMonth());
		}
		if (!candidateDate.isAfter(fromDateTime.toLocalDate())) {
			candidateDate = candidateDate.plusMonths(1);
			try {
				candidateDate = candidateDate.withDayOfMonth(dayOfMonth);
			} catch (DateTimeException e) {
				candidateDate = candidateDate.withDayOfMonth(candidateDate.lengthOfMonth());
			}
		}
		return candidateDate.atTime(fromDateTime.toLocalTime());
	}

	private LocalDateTime calculateSpecificMonthNextDateTime(LocalDateTime fromDateTime, Integer dayOfMonth,
			Integer month) {
		if (month == null)
			return null;
		int day = (dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31) ? dayOfMonth : 1;
		int year = fromDateTime.getYear();
		LocalDate candidateDate;
		try {
			candidateDate = LocalDate.of(year, month, day);
		} catch (DateTimeException e) {
// Handle invalid day in month (e.g., 31 in April) – use last day of that month
			candidateDate = LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
		}
		if (!candidateDate.isAfter(fromDateTime.toLocalDate())) {
			candidateDate = candidateDate.plusYears(1);
// After adding a year, month remains same, day may need adjustment (e.g., leap year)
			try {
				candidateDate = candidateDate.withDayOfMonth(day);
			} catch (DateTimeException e) {
				candidateDate = candidateDate.withDayOfMonth(candidateDate.lengthOfMonth());
			}
		}
		return candidateDate.atTime(fromDateTime.toLocalTime());
	}

	private LocalDateTime calculateSpecificDateNextDateTime(LocalDateTime fromDateTime, Integer dayOfMonth,
			Integer month) {
		if (dayOfMonth == null || month == null)
			return null;
// Exactly the same as specific month (since day+month fully specify the date)
		return calculateSpecificMonthNextDateTime(fromDateTime, dayOfMonth, month);
	}

	public void sendEmail(String firstName, String lastName, String emailAddress, String message, String subject) {
		MWFMail wfMail = new MWFMail();
		MADSysConfig sys = getOrganizationSystemConfiguratins(SettingCategoriesEnum.GENERAL_SETTINGS);
		MEmail email = emailRepository.findTop1ByIsActiveAndAdOrgID(true, getAD_Org_ID());
		if (email != null) {

			String htmlContent = "<html>"
					+ "  <body style=\"font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px;\">"
					+ "    <div style=\"max-width: 600px; margin: auto; background: #ffffff; border-radius: 10px; "
					+ "                box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;\">"
					+ "      <h2 style=\"color: #2c3e50; text-align: center;\">" + subject + "</h2>"
					+ "      <p style=\"font-size: 16px; color: #333333; line-height: 1.5;\">" + "        Dear <strong>"
					+ firstName + " " + lastName + "</strong>," + "      </p>"
					+ "      <p style=\"font-size: 15px; color: #555555; line-height: 1.6;\">" + message + "</p>"
					+ "      <div style=\"margin-top: 30px; text-align: center;\">" + "        <a href=\""
					+ sys.getDomainUrl() + "\" "
					+ "           style=\"background-color: #3498db; color: white; padding: 12px 24px; "
					+ "                  text-decoration: none; border-radius: 6px; font-size: 14px;\">"
					+ "          Visit Our Website" + "        </a>" + "      </div>"
					+ "      <hr style=\"margin: 30px 0; border: none; border-top: 1px solid #eee;\">"
					+ "      <p style=\"font-size: 13px; color: #888888; text-align: center;\">" + "        ©"
					+ getCurrentYear() + " " + sys.getPlatformName() + ". All rights reserved." + "      </p>"
					+ "    </div>" + "  </body>" + "</html>";

			wfMail.setMailFrom(email.getUsername());
			wfMail.setMailTo(emailAddress);
			wfMail.setMailSubject(subject);

			wfMail.setMailContent(htmlContent);
			mailRepository.save(wfMail);
		}

	}

	public boolean isDebtor() {
		// TODO Auto-generated method stub
		if (getLogedInUserRoles() != null) {
			for (MRoles role : getLogedInUserRoles()) {
				if (role.getName().equalsIgnoreCase("ROLE_DEBTOR")) {
					return true;
				}
			}
		}
		return false;
	}

	// In your Utils class
	public Date getStartOfDay() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	public Date getStartOfDay(Date date) {
		if (date == null) {
			return null;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	public Date getEndOfDay(Date date) {
		if (date == null) {
			return null;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	public static MultipartFile convertBase64StringFileToMultipartFile(FileUploads fileUpload) {

		if (fileUpload == null || fileUpload.getBase64File() == null || fileUpload.getBase64File().isBlank()) {
			throw new IllegalArgumentException("Base64 file content is empty");
		}

		String base64 = fileUpload.getBase64File();
		String contentType = fileUpload.getMimeType();
		String fileName = fileUpload.getFileName();

		// Defaults (important)
		if (fileName == null || fileName.isBlank()) {
			fileName = "upload.csv";
		}
		if (contentType == null || contentType.isBlank()) {
			contentType = "text/csv";
		}

		// Handle data URL format: data:text/csv;base64,XXXX
		if (base64.startsWith("data:")) {
			String[] parts = base64.split(",", 2);
			String meta = parts[0];
			base64 = parts[1];

			if (meta.contains(":") && meta.contains(";")) {
				contentType = meta.substring(meta.indexOf(":") + 1, meta.indexOf(";"));
			}
		}

		byte[] decodedBytes;
		try {
			decodedBytes = Base64.getDecoder().decode(base64);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid Base64 file content", ex);
		}

		// Optional size check (recommended)
		if (fileUpload.getFileSize() > 0 && decodedBytes.length != fileUpload.getFileSize()) {
			throw new IllegalArgumentException("File size mismatch");
		}

		byte[] finalBytes = decodedBytes;
		String finalFileName = fileName;
		String finalContentType = contentType;

		return new MultipartFile() {

			@Override
			public String getName() {
				return "file";
			}

			@Override
			public String getOriginalFilename() {
				return finalFileName;
			}

			@Override
			public String getContentType() {
				return finalContentType;
			}

			@Override
			public boolean isEmpty() {
				return finalBytes.length == 0;
			}

			@Override
			public long getSize() {
				return finalBytes.length;
			}

			@Override
			public byte[] getBytes() {
				return finalBytes;
			}

			@Override
			public InputStream getInputStream() {
				return new ByteArrayInputStream(finalBytes);
			}

			@Override
			public void transferTo(File dest) throws IOException {
				Files.write(dest.toPath(), finalBytes);
			}
		};
	}

	public Set<MNextOfKin> getPrimaryGuarantorActingAsBorrower(long orgId, long loanId, boolean isPrimaryGuarantor) {

		String sql = "SELECT DISTINCT k.* " + "FROM ad_next_of_kin k " + "INNER JOIN ad_borrower_guarantors g "
				+ "    ON g.ad_guarantor_id = k.ad_next_of_kin_id " + "INNER JOIN ad_loan_application l "
				+ "    ON l.ad_loan_application_id = g.ad_loan_application_id " + "WHERE l.isactive = true "
				+ "  AND l.balance > 0 " + "  AND l.ad_org_id = :orgId " + "  AND l.approvalstage = 'APPROVED' "
				+ "  AND k.primary_guarantor = :isPrimaryGuarantor " + "  AND l.ad_loan_application_id = :loanId";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("orgId", orgId).addValue("loanId", loanId)
				.addValue("isPrimaryGuarantor", isPrimaryGuarantor);

		List<MNextOfKin> resultList = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
			MNextOfKin nextOfKin = new MNextOfKin();

			nextOfKin.setNextOfKinId(rs.getLong("ad_next_of_kin_id"));
			nextOfKin.setFullName(rs.getString("full_name"));
			nextOfKin.setPhoneNumber(rs.getString("phone_number"));
			nextOfKin.setAddress(rs.getString("address"));
			nextOfKin.setPrimaryGuarantor(rs.getBoolean("primary_guarantor"));
			nextOfKin.setEmail(rs.getString("email"));
			nextOfKin.setNationalId(rs.getString("national_id"));

			String relationship = rs.getString("relationship");
			if (relationship != null) {
				nextOfKin.setRelationship(RelationShipEnum.valueOf(relationship));
			}

			return nextOfKin;
		});

		return new HashSet<>(resultList);
	}

	/**
	 * Saves a membership reminder SMS record with all membership-specific fields.
	 *
	 * @param memberIds           (Optional) Map of member IDs – currently unused,
	 *                            kept for signature consistency.
	 * @param phoneNumber         Recipient phone number
	 * @param message             SMS content
	 * @param orgId               Organization ID
	 * @param adClientId          Client ID
	 * @param smsType             SMS type enum
	 * @param memberShipBillId    Bill ID (maps to billId)
	 * @param memberShipPlanId    Plan ID (maps to memberPlanId)
	 * @param time                Scheduled send time
	 * @param reminderId          Reminder configuration ID
	 * @param membershipInvoiceId Invoice ID (maps to invoiceId)
	 * @param individualMemberId  Individual member ID
	 * @param groupMemberId       Group member ID
	 * @param institutionMemberId Institution member ID
	 * @return true if saved successfully, false otherwise
	 */
	public boolean saveMembershipReminderSms(String phoneNumber, String message, Long orgId, Long adClientId,
			SmsTypeEnum smsType, Long memberShipBillId, Long memberShipPlanId, LocalDateTime time, Long reminderId,
			Long membershipInvoiceId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long membershiAccountId) {

		// Validate required fields
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			log.error("❌ Cannot save membership reminder SMS: Phone number is empty");
			return false;
		}
		if (message == null || message.trim().isEmpty()) {
			log.error("❌ Cannot save membership reminder SMS: Message is empty");
			return false;
		}
		if (smsType == null) {
			log.error("❌ Cannot save membership reminder SMS: SMS type is required");
			return false;
		}
		if (orgId == null) {
			log.error("❌ Cannot save membership reminder SMS: Organization ID is required");
			return false;
		}

		try {
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phoneNumber));
			sms.setMessage(message);
			sms.setSmsType(smsType);
			sms.setAdOrgID(orgId);
			sms.setAdClientId(adClientId != null ? adClientId : 0L);
			sms.setMembershipAccountId(membershiAccountId != null ? membershiAccountId : 0L);
			// Membership-specific fields
			sms.setIndividualMemberId(individualMemberId != null ? individualMemberId : 0L);
			sms.setGroupMemberId(groupMemberId != null ? groupMemberId : 0L);
			sms.setInstitutionMemberId(institutionMemberId != null ? institutionMemberId : 0L);
			sms.setBillId(memberShipBillId != null ? memberShipBillId : 0L);
			sms.setMemberPlanId(memberShipPlanId != null ? memberShipPlanId : 0L);
			sms.setInvoiceId(membershipInvoiceId != null ? membershipInvoiceId : 0L);

			// Scheduling and reference
			sms.setReminderId(reminderId != null ? reminderId : 0L);
			sms.setTimesTosend(time != null ? time : LocalDateTime.now());

			// Loan-related fields remain 0 (default) – this is a membership SMS
			sms.setLoanId(0L);
			sms.setInstallmentId(0L);
			sms.setPaymentId(0L);
			sms.setGuarantorId(0L);
			sms.setIndividualBorrowerId(0L);
			sms.setGroupBorrowerId(0L);
			sms.setInstitutionBorrowerId(0L);

			// Creation timestamp
			if (sms.getCreated() == null) {
				sms.setCreated(new Date());
			}

			return saveSmsRecord(sms);

		} catch (Exception e) {
			log.error("❌ Error in saveMembershipReminderSms: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Convenience wrapper for saveMembershipReminderSms. Similar to the deprecated
	 * saveSms for loans.
	 */
	public void saveMembershipSms(SmsTypeEnum smsType, String phoneNumber, String message, Long orgId, Long adClientId,
			Long memberShipBillId, Long memberShipPlanId, LocalDateTime time, Long reminderId, Long membershipInvoiceId,
			Long individualMemberId, Long groupMemberId, Long institutionMemberId, Long membershipAccountId) {

		log.info("Saving membership SMS of type {}", smsType);
		saveMembershipReminderSms(phoneNumber, message, orgId, adClientId, smsType, memberShipBillId, memberShipPlanId,
				time, reminderId, membershipInvoiceId, individualMemberId, groupMemberId, institutionMemberId,
				membershipAccountId);
	}

	public boolean saveBillOrInvoiceAndProformaSms(String phoneNumber, String message, Long orgId, Long adClientId,
			SmsTypeEnum smsType, Long memberShipBillId, Long memberShipPlanId, LocalDateTime time, Long reminderId,
			Long membershipInvoiceId, Long individualMemberId, Long groupMemberId, Long institutionMemberId,
			Long membershiAccountId, Long individualBorrowerId, Long groupBorrowerId, Long institutionBorrowerId) {

		// Validate required fields
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			log.error("❌ Cannot save membership reminder SMS: Phone number is empty");
			return false;
		}
		if (message == null || message.trim().isEmpty()) {
			log.error("❌ Cannot save membership reminder SMS: Message is empty");
			return false;
		}
		if (smsType == null) {
			log.error("❌ Cannot save membership reminder SMS: SMS type is required");
			return false;
		}
		if (orgId == null) {
			log.error("❌ Cannot save membership reminder SMS: Organization ID is required");
			return false;
		}

		try {
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phoneNumber));
			sms.setMessage(message);
			sms.setSmsType(smsType);
			sms.setAdOrgID(orgId);
			sms.setAdClientId(adClientId != null ? adClientId : 0L);
			sms.setMembershipAccountId(membershiAccountId != null ? membershiAccountId : 0L);
			// Membership-specific fields
			sms.setIndividualMemberId(individualMemberId != null ? individualMemberId : 0L);
			sms.setGroupMemberId(groupMemberId != null ? groupMemberId : 0L);
			sms.setInstitutionMemberId(institutionMemberId != null ? institutionMemberId : 0L);
			sms.setBillId(memberShipBillId != null ? memberShipBillId : 0L);
			sms.setMemberPlanId(memberShipPlanId != null ? memberShipPlanId : 0L);
			sms.setInvoiceId(membershipInvoiceId != null ? membershipInvoiceId : 0L);

			// Scheduling and reference
			sms.setReminderId(reminderId != null ? reminderId : 0L);
			sms.setTimesTosend(time != null ? time : LocalDateTime.now());

			// Loan-related fields remain 0 (default) – this is a membership SMS
			sms.setLoanId(0L);
			sms.setInstallmentId(0L);
			sms.setPaymentId(0L);
			sms.setGuarantorId(0L);
			sms.setIndividualBorrowerId(individualBorrowerId != null ? individualBorrowerId : 0L);
			sms.setGroupBorrowerId(groupBorrowerId != null ? groupBorrowerId : 0L);
			sms.setInstitutionBorrowerId(institutionBorrowerId != null ? institutionBorrowerId : 0L);

			// Creation timestamp
			if (sms.getCreated() == null) {
				sms.setCreated(new Date());
			}

			return saveSmsRecord(sms);

		} catch (Exception e) {
			log.error("❌ Error in saveMembershipReminderSms: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Saves an email with attachment information to the database for later
	 * processing by scheduler Exactly follows the pattern of sendEmail() method
	 * 
	 * @param recipientEmail     The recipient's email address
	 * @param recipientFirstName The recipient's first name
	 * @param recipientLastName  The recipient's last name
	 * @param message            The email message content
	 * @param subject            Email subject
	 * @param filePath           The full path to the attachment file on disk
	 * @param fileUrl            The URL to access the attachment online
	 * @param fileName           The name of the attachment file
	 * @param orgId              Organization ID for email configuration
	 */
	public void sendEmailWithAttachment(String recipientEmail, String recipientFirstName, String recipientLastName,
			String message, String subject, String filePath, String fileUrl, String fileName, Long orgId,
			LocalDateTime timeToSend) {

		MWFMail wfMail = new MWFMail();
		MADSysConfig sys = getOrganizationSystemConfiguratinsByUser(SettingCategoriesEnum.GENERAL_SETTINGS, orgId);
		MEmail email = emailRepository.findTop1ByIsActiveAndAdOrgID(true, orgId);

		if (email != null) {

			String htmlContent = "<html>"
					+ "  <body style=\"font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px;\">"
					+ "    <div style=\"max-width: 600px; margin: auto; background: #ffffff; border-radius: 10px; "
					+ "                box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;\">"
					+ "      <h2 style=\"color: #2c3e50; text-align: center;\">" + subject + "</h2>"
					+ "      <p style=\"font-size: 16px; color: #333333; line-height: 1.5;\">" + "        Dear <strong>"
					+ recipientFirstName + " " + recipientLastName + "</strong>," + "      </p>"
					+ "      <p style=\"font-size: 15px; color: #555555; line-height: 1.6;\">" + message + "</p>"

					// Add attachment notification
					+ "      <div style=\"background-color: #e8f4fd; padding: 15px; border-radius: 6px; margin: 20px 0; font-size: 14px; color: #0d3e6d;\">"
					+ "        <strong>📎 PDF Attached:</strong> Please find the attachment attached to this email."
					+ "      </div>"

					+ "      <div style=\"margin-top: 30px; text-align: center;\">" + "        <a href=\""
					+ (sys != null ? sys.getDomainUrl() : "") + "\" "
					+ "           style=\"background-color: #3498db; color: white; padding: 12px 24px; "
					+ "                  text-decoration: none; border-radius: 6px; font-size: 14px;\">"
					+ "          Visit Our Website" + "        </a>" + "      </div>"
					+ "      <hr style=\"margin: 30px 0; border: none; border-top: 1px solid #eee;\">"
					+ "      <p style=\"font-size: 13px; color: #888888; text-align: center;\">" + "        ©"
					+ getCurrentYear() + " " + (sys != null ? sys.getPlatformName() : "Our System")
					+ ". All rights reserved." + "      </p>" + "    </div>" + "  </body>" + "</html>";

			wfMail.setMailFrom(email.getUsername());
			wfMail.setMailTo(recipientEmail);
			wfMail.setMailSubject(subject);
			wfMail.setMailContent(htmlContent);
			if (timeToSend == null) {
				timeToSend = LocalDateTime.now();
			}
			wfMail.setTimeToSend(timeToSend);

			// Set attachment fields (this is the only difference from sendEmail)
			wfMail.setAttachmentName(fileName);
			wfMail.setFilePath(filePath);
			wfMail.setFileUrl(fileUrl);
			wfMail.setAdOrgID(orgId);

			mailRepository.save(wfMail);
			log.info("✅ Email with attachment saved for scheduling to: {}", recipientEmail);
		}
	}

	/**
	 * Saves a bill/proforma invoice reminder SMS record with all bill-specific
	 * fields. Follows the same pattern as saveMembershipReminderSms
	 * 
	 * @param borrowerIds         Map containing borrower IDs (individualId,
	 *                            groupId, institutionId)
	 * @param phoneNumber         Recipient phone number
	 * @param message             SMS content
	 * @param orgId               Organization ID
	 * @param clientId            Client ID
	 * @param type                SMS type enum (BILL_SUBMISSION_NOTIFICATION or
	 *                            PROFORMA_INVOICE_SUBMISSION_NOTIFICATION)
	 * @param membershipAccountId Membership account ID (if applicable)
	 * @param billId              Bill ID (for both bills and proformas)
	 * @param timeToSend          Scheduled send time
	 * @param reminderId          Reminder configuration ID
	 * @param proformaInvoiceId   Proforma invoice ID (if different from billId)
	 * @return true if saved successfully, false otherwise
	 */
	public boolean saveBillReminderSms(Map<String, Long> borrowerIds, String phoneNumber, String message, Long orgId,
			Long clientId, SmsTypeEnum type, Long membershipAccountId, Long billId, LocalDateTime timeToSend,
			Long reminderId, Long proformaInvoiceId) {

		// Validate required fields
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			log.error("❌ Cannot save bill reminder SMS: Phone number is empty");
			return false;
		}
		if (message == null || message.trim().isEmpty()) {
			log.error("❌ Cannot save bill reminder SMS: Message is empty");
			return false;
		}
		if (type == null) {
			log.error("❌ Cannot save bill reminder SMS: SMS type is required");
			return false;
		}
		if (orgId == null) {
			log.error("❌ Cannot save bill reminder SMS: Organization ID is required");
			return false;
		}

		try {
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phoneNumber));
			sms.setMessage(message);
			sms.setSmsType(type);
			sms.setAdOrgID(orgId);
			sms.setAdClientId(clientId != null ? clientId : 0L);

			// Bill-specific fields
			sms.setBillId(billId != null ? billId : 0L);
			sms.setMembershipAccountId(membershipAccountId != null ? membershipAccountId : 0L);

			sms.setProformaInvoiceId(proformaInvoiceId != null ? proformaInvoiceId : 0L);
			// Scheduling and reference
			sms.setReminderId(reminderId != null ? reminderId : 0L);
			sms.setTimesTosend(timeToSend != null ? timeToSend : LocalDateTime.now());

			// Set borrower IDs from the map (if provided)
			if (borrowerIds != null) {
				sms.setIndividualBorrowerId(borrowerIds.getOrDefault("individualId", 0L));
				sms.setGroupBorrowerId(borrowerIds.getOrDefault("groupId", 0L));
				sms.setInstitutionBorrowerId(borrowerIds.getOrDefault("institutionId", 0L));
			} else {
				sms.setIndividualBorrowerId(0L);
				sms.setGroupBorrowerId(0L);
				sms.setInstitutionBorrowerId(0L);
			}

			// Set member IDs to 0 (not applicable for bills unless linked to membership)
			sms.setIndividualMemberId(0L);
			sms.setGroupMemberId(0L);
			sms.setInstitutionMemberId(0L);

			// Loan-related fields remain 0 (not applicable for bills)
			sms.setLoanId(0L);
			sms.setInstallmentId(0L);
			sms.setPaymentId(0L);
			sms.setGuarantorId(0L);
			sms.setMemberPlanId(0L);
			sms.setInvoiceId(0L);

			// Creation timestamp
			if (sms.getCreated() == null) {
				sms.setCreated(new Date());
			}

			return saveSmsRecord(sms);

		} catch (Exception e) {
			log.error("❌ Error in saveBillReminderSms: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Formats LocalDateTime to a short format (dd/MM/yyyy HH:mm)
	 */
	public String formatDateTime(LocalDateTime dateTime) {
		if (dateTime == null) {
			return "N/A";
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
		return dateTime.format(formatter);
	}

	/**
	 * Formats LocalDateTime to a short format (dd/MM/yyyy HH:mm)
	 */
	public String formatDateLocalDate(LocalDate dateTime) {
		if (dateTime == null) {
			return "N/A";
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		return dateTime.format(formatter);
	}

	/**
	 * Saves a manual SMS for membership accounts from the message center. This
	 * method follows the same pattern as saveManualSms for borrowers.
	 * 
	 * @param membershipAccountId The membership account ID
	 * @param individualMemberId  The individual member ID (if applicable)
	 * @param groupMemberShipId   The group member ID (if applicable)
	 * @param institutionMemberId The institution member ID (if applicable)
	 * @param phoneNumber         The recipient's phone number
	 * @param message             The SMS message content
	 * @param adOrgID             The organization ID
	 * @param adClientId          The client ID
	 * @param smsType             The SMS type (should be
	 *                            MANUAL_SMS_FROM_MESSAGE_CENTER)
	 * @param messagingId         The message center ID for reference
	 */
	public void saveManualSmsForMembership(long membershipAccountId, long individualMemberId, long groupMemberShipId,
			long institutionMemberId, String phoneNumber, String message, Long adOrgID, Long adClientId,
			SmsTypeEnum smsType, long messagingId) {

		try {
			// Validate required fields
			if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
				log.error("❌ Cannot save manual membership SMS: Phone number is empty");
				return;
			}

			if (message == null || message.trim().isEmpty()) {
				log.error("❌ Cannot save manual membership SMS: Message is empty");
				return;
			}

			if (smsType == null) {
				log.error("❌ Cannot save manual membership SMS: SMS type is required");
				return;
			}

			if (adOrgID == null) {
				log.error("❌ Cannot save manual membership SMS: Organization ID is required");
				return;
			}

			log.info("Saving manual membership SMS for account ID: {}, phone: {}", membershipAccountId, phoneNumber);

			// Create and populate SMS entity
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			// Format phone number (Kenyan format)
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phoneNumber));
			sms.setMessage(message);
			sms.setSmsType(smsType);
			sms.setAdOrgID(adOrgID);
			sms.setAdClientId(adClientId != null ? adClientId : 0L);

			// Set membership-specific fields
			sms.setMembershipAccountId(membershipAccountId);
			sms.setIndividualMemberId(individualMemberId);
			sms.setGroupMemberId(groupMemberShipId);
			sms.setInstitutionMemberId(institutionMemberId);

			// Reference to message center
			sms.setMessageCenterId(messagingId);

			// Set scheduling - send immediately (no specific time)
			sms.setTimesTosend(LocalDateTime.now());

			// Zero out all loan/borrower related fields (not applicable for membership)
			sms.setLoanId(0L);
			sms.setInstallmentId(0L);
			sms.setPaymentId(0L);
			sms.setGuarantorId(0L);
			sms.setIndividualBorrowerId(0L);
			sms.setGroupBorrowerId(0L);
			sms.setInstitutionBorrowerId(0L);
			sms.setBillId(0L);
			sms.setMemberPlanId(0L);
			sms.setInvoiceId(0L);
			sms.setReminderId(0L);

			// Set creation timestamp if not already set
			if (sms.getCreated() == null) {
				sms.setCreated(new Date());
			}

			// Set initial status
			sms.setDocStatus(DocStatus.DRAFT);
			sms.setMessageStatus(MessageStatus.PENDING);
			sms.setApprovalStage(ApprovalStage.DRAFT);

			// Save to database
			boolean saved = saveSmsRecord(sms);

			if (saved) {
				log.info("✅ Manual membership SMS saved successfully. ID: {}, Phone: {}", sms.getSmsId(), phoneNumber);
			} else {
				log.error("❌ Failed to save manual membership SMS");
			}

		} catch (Exception e) {
			log.error("❌ Error in saveManualSmsForMembership: {}", e.getMessage(), e);
		}
	}

	/**
	 * Enhanced version with option to schedule for later delivery
	 * 
	 * @param membershipAccountId The membership account ID
	 * @param individualMemberId  The individual member ID
	 * @param groupMemberShipId   The group member ID
	 * @param institutionMemberId The institution member ID
	 * @param phoneNumber         The recipient's phone number
	 * @param message             The SMS message content
	 * @param adOrgID             The organization ID
	 * @param adClientId          The client ID
	 * @param smsType             The SMS type
	 * @param messagingId         The message center ID
	 * @param scheduledTime       The time to send the SMS (null for immediate)
	 */
	public void saveManualSmsForMembership(long membershipAccountId, long individualMemberId, long groupMemberShipId,
			long institutionMemberId, String phoneNumber, String message, Long adOrgID, Long adClientId,
			SmsTypeEnum smsType, long messagingId, LocalDateTime scheduledTime) {

		try {
			// Validate required fields
			if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
				log.error("❌ Cannot save scheduled manual membership SMS: Phone number is empty");
				return;
			}

			if (message == null || message.trim().isEmpty()) {
				log.error("❌ Cannot save scheduled manual membership SMS: Message is empty");
				return;
			}

			log.info("Saving scheduled manual membership SMS for account ID: {}, phone: {}, scheduled: {}",
					membershipAccountId, phoneNumber, scheduledTime);

			// Create and populate SMS entity
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			// Format phone number (Kenyan format)
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phoneNumber));
			sms.setMessage(message);
			sms.setSmsType(smsType);
			sms.setAdOrgID(adOrgID);
			sms.setAdClientId(adClientId != null ? adClientId : 0L);

			// Set membership-specific fields
			sms.setMembershipAccountId(membershipAccountId);
			sms.setIndividualMemberId(individualMemberId);
			sms.setGroupMemberId(groupMemberShipId);
			sms.setInstitutionMemberId(institutionMemberId);

			// Reference to message center
			sms.setMessageCenterId(messagingId);

			// Set scheduling
			if (scheduledTime != null) {
				sms.setTimesTosend(scheduledTime);
				sms.setMessageStatus(MessageStatus.SCHEDULED);
			} else {
				sms.setTimesTosend(LocalDateTime.now());
				sms.setMessageStatus(MessageStatus.PENDING);
			}

			// Zero out all loan/borrower related fields
			sms.setLoanId(0L);
			sms.setInstallmentId(0L);
			sms.setPaymentId(0L);
			sms.setGuarantorId(0L);
			sms.setIndividualBorrowerId(0L);
			sms.setGroupBorrowerId(0L);
			sms.setInstitutionBorrowerId(0L);
			sms.setBillId(0L);
			sms.setMemberPlanId(0L);
			sms.setInvoiceId(0L);
			sms.setReminderId(0L);

			// Set creation timestamp
			if (sms.getCreated() == null) {
				sms.setCreated(new Date());
			}

			// Set initial status
			sms.setDocStatus(DocStatus.DRAFT);
			sms.setApprovalStage(ApprovalStage.DRAFT);

			// Save to database
			boolean saved = saveSmsRecord(sms);

			if (saved) {
				String timing = scheduledTime != null ? "scheduled for " + scheduledTime : "immediate";
				log.info("✅ Manual membership SMS saved successfully ({}). ID: {}, Phone: {}", timing, sms.getSmsId(),
						phoneNumber);
			} else {
				log.error("❌ Failed to save manual membership SMS");
			}

		} catch (Exception e) {
			log.error("❌ Error in saveManualSmsForMembership (scheduled): {}", e.getMessage(), e);
		}
	}

	/**
	 * Save wallet SMS notification for deposit/withdrawal transactions
	 * 
	 * @param phoneNumber         The recipient's phone number
	 * @param message             The SMS message content
	 * @param orgId               The organization ID
	 * @param clientId            The client ID
	 * @param type                The SMS type (WALLET_DEPOSIT_SUCCESS,
	 *                            WALLET_WITHDRAWAL_SUCCESS, etc.)
	 * @param timeToSend          The time to send the SMS (null for immediate)
	 * @param reminderId          The reminder configuration ID (can be null)
	 * @param membershipAccountId The membership account ID
	 * @param individualMemberId  The individual member ID
	 * @param groupMemberId       The group member ID
	 * @param institutionMemberId The institution member ID
	 * @return true if saved successfully, false otherwise
	 */
	public boolean saveWalletSms(String phoneNumber, String message, Long orgId, Long clientId, SmsTypeEnum type,
			LocalDateTime timeToSend, Long reminderId, Long membershipAccountId, Long individualMemberId,
			Long groupMemberId, Long institutionMemberId) {

		try {
			// Validate required fields
			if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
				log.error("❌ Cannot save wallet SMS: Phone number is empty");
				return false;
			}

			if (message == null || message.trim().isEmpty()) {
				log.error("❌ Cannot save wallet SMS: Message is empty");
				return false;
			}

			if (type == null) {
				log.error("❌ Cannot save wallet SMS: SMS type is null");
				return false;
			}

			log.info("Saving wallet SMS for account ID: {}, phone: {}, type: {}, scheduled: {}", membershipAccountId,
					phoneNumber, type.getValue(), timeToSend);

			// Create and populate SMS entity
			MSms sms = new MSms();
			sms.setMessageStatus(MessageStatus.PENDING);
			// Format phone number (Kenyan format)
			sms.setPhoneNo(formatPhoneNumberDefaultKenyan(phoneNumber));
			sms.setMessage(message);
			sms.setSmsType(type);
			sms.setAdOrgID(orgId);
			sms.setAdClientId(clientId != null ? clientId : 0L);

			// Set membership-specific fields for wallet
			sms.setMembershipAccountId(membershipAccountId != null ? membershipAccountId : 0L);
			sms.setIndividualMemberId(individualMemberId != null ? individualMemberId : 0L);
			sms.setGroupMemberId(groupMemberId != null ? groupMemberId : 0L);
			sms.setInstitutionMemberId(institutionMemberId != null ? institutionMemberId : 0L);

			// Set reminder ID if provided
			if (reminderId != null) {
				sms.setReminderId(reminderId);
			} else {
				sms.setReminderId(0L);
			}

			// Set scheduling
			if (timeToSend != null) {
				sms.setTimesTosend(timeToSend);
				sms.setMessageStatus(MessageStatus.SCHEDULED);
			} else {
				sms.setTimesTosend(LocalDateTime.now());
				sms.setMessageStatus(MessageStatus.PENDING);
			}

			// Zero out all loan/borrower/bill related fields (not applicable for wallet)
			sms.setLoanId(0L);
			sms.setInstallmentId(0L);
			sms.setPaymentId(0L);
			sms.setGuarantorId(0L);
			sms.setIndividualBorrowerId(0L);
			sms.setGroupBorrowerId(0L);
			sms.setInstitutionBorrowerId(0L);
			sms.setBillId(0L);
			sms.setMemberPlanId(0L);
			sms.setInvoiceId(0L);
			sms.setMessageCenterId(0L); // Not from message center

			// Set creation timestamp
			if (sms.getCreated() == null) {
				sms.setCreated(new Date());
			}

			// Set initial status
			sms.setDocStatus(DocStatus.DRAFT);
			sms.setApprovalStage(ApprovalStage.DRAFT);

			// Save to database
			boolean saved = saveSmsRecord(sms);

			if (saved) {
				String timing = timeToSend != null ? "scheduled for " + timeToSend : "immediate";
				log.info("✅ Wallet SMS saved successfully ({}). ID: {}, Phone: {}, Type: {}", timing, sms.getSmsId(),
						phoneNumber, type.getDescription());
				return true;
			} else {
				log.error("❌ Failed to save wallet SMS for phone: {}, type: {}", phoneNumber, type.getDescription());
				return false;
			}

		} catch (Exception e) {
			log.error("❌ Error in saveWalletSms for phone {}: {}", phoneNumber, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Overloaded method for immediate sending (convenience method)
	 */
	public boolean saveWalletSms(String phoneNumber, String message, Long orgId, Long clientId, SmsTypeEnum type,
			Long membershipAccountId, Long individualMemberId, Long groupMemberId, Long institutionMemberId) {
		return saveWalletSms(phoneNumber, message, orgId, clientId, type, LocalDateTime.now(), null,
				membershipAccountId, individualMemberId, groupMemberId, institutionMemberId);
	}

}
