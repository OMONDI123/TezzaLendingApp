package co.ke.tezza.loanapp.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.ke.tezza.loanapp.entity.MBPartner;
import co.ke.tezza.loanapp.entity.MDebtor;
import co.ke.tezza.loanapp.entity.MDocuments;
import co.ke.tezza.loanapp.entity.MGroupDebtors;
import co.ke.tezza.loanapp.entity.MGroupMembers;
import co.ke.tezza.loanapp.entity.MInstitutionBorrower;
import co.ke.tezza.loanapp.entity.MNextOfKin;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.entity.MWard;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.FileUploads;
import co.ke.tezza.loanapp.model.GroupBorrowerModel;
import co.ke.tezza.loanapp.model.GroupMembers;
import co.ke.tezza.loanapp.model.IndividualBorrowerModel;
import co.ke.tezza.loanapp.model.InstitutionBorrowerModel;
import co.ke.tezza.loanapp.repository.AttachmentRepository;
import co.ke.tezza.loanapp.repository.GroupBorrowersRepository;
import co.ke.tezza.loanapp.repository.GroupMembersRepository;
import co.ke.tezza.loanapp.repository.IndividualBorrowersRepository;
import co.ke.tezza.loanapp.repository.InstitutionBorrowersRepository;
import co.ke.tezza.loanapp.repository.MBPartnerRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.repository.UserRepository;
import co.ke.tezza.loanapp.repository.WardRepository;
import co.ke.tezza.loanapp.response.BorrowerAttachments;
import co.ke.tezza.loanapp.response.GroupBorrowerResponse;
import co.ke.tezza.loanapp.response.GroupMembersResponse;
import co.ke.tezza.loanapp.response.IndividualBorrowerResponse;
import co.ke.tezza.loanapp.response.InstitutionBorrowerResponse;
import co.ke.tezza.loanapp.response.NextOfKins;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

import org.slf4j.LoggerFactory;

@Service
public class BorrowersServices {

	@Autowired
	private IndividualBorrowersRepository individualBorrowersRepository;
	@Autowired
	private Utils utils;
	@Autowired
	private WardRepository wardRepository;
	@Autowired
	private InstitutionBorrowersRepository institutionBorrowersRepository;

	@Autowired
	private GroupBorrowersRepository groupBorrowersRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MBPartnerRepository mBPartnerRepository;
	@Autowired
	RoleRepository roleRepository;

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private GroupMembersRepository groupMembersRepository;
	private static final Logger logger = LoggerFactory.getLogger(BorrowersServices.class);

	public Page<GroupBorrowerResponse> getAllGroupBorrowers(int page, int size, String searchTerm, String statusFilter,
			String typeFilter) {
		if (searchTerm != null && !searchTerm.isEmpty()) {
			if (typeFilter != null && !typeFilter.isEmpty() && !typeFilter.equals("all")) {
				return groupBorrowersRepository.searchGrupBorrowerByGroupType(true, utils.getAD_Org_ID(), searchTerm,
						typeFilter, PageRequest.of(page, size)).map(this::mapGroupBorrower);
			}
			return groupBorrowersRepository
					.searchGrupBorrower(true, utils.getAD_Org_ID(), searchTerm, PageRequest.of(page, size))
					.map(this::mapGroupBorrower);

		} else {
			if (typeFilter != null && !typeFilter.isEmpty() && !typeFilter.equals("all")) {
				return groupBorrowersRepository.findByIsActiveAndAdOrgIDAndGroupTypeOrderByGroupBorrowerIdDesc(true,
						utils.getAD_Org_ID(), typeFilter, PageRequest.of(page, size)).map(this::mapGroupBorrower);
			}
			return groupBorrowersRepository.findByIsActiveAndAdOrgIDOrderByGroupBorrowerIdDesc(true,
					utils.getAD_Org_ID(), PageRequest.of(page, size)).map(this::mapGroupBorrower);

		}

	}

	@Transactional
	public ResponseEntity<GroupBorrowerResponse> createUpdateGroupBorrower(@Valid GroupBorrowerModel model) {
		String message = "Failed to create Group Borrower";
		int code = 201;
		if (model.getMembers().isEmpty() && model.getMembers() == null) {
			throw new SetUpExceptions(
					"Group Registration requires atleast two members to qualify as a group. Please add atleast two members");
		}
		if (model.getContactPhone() == null) {
			throw new SetUpExceptions("Please include group contact number");
		}
		if (model.getContactEmail() == null) {
			throw new SetUpExceptions("Please include group contact email");

		}
		if (model.getGroupName() == null) {
			throw new SetUpExceptions("Please provide the name of the group");

		}

		MGroupDebtors borrower = groupBorrowersRepository.findById(model.getGroupBorrowerId())
				.orElse(new MGroupDebtors());

		if (borrower.getGroupBorrowerId() == 0) {
			if (groupBorrowersRepository.findTop1ByIsActiveAndAdOrgIDAndGroupName(true, utils.getAD_Org_ID(),
					model.getGroupName()) != null) {
				throw new SetUpExceptions("The Group Name Provided is Already Registered");
			}
		}

		// Basic Info
		borrower.setGroupName(model.getGroupName());
		borrower.setRegistrationNumber(model.getRegistrationNumber());
		borrower.setFormationDate(model.getFormationDate());
		borrower.setGroupType(model.getGroupType());
		borrower.setContactPhone(model.getContactPhone());
		borrower.setContactEmail(model.getContactEmail());
		borrower.setMeetingFrequency(model.getMeetingFrequency());
		borrower.setMeetingPlace(model.getMeetingPlace());
		borrower.setLoanOfficer(model.getLoanOfficer());
		borrower.setNotes(model.getNotes());

		// Location
		MWard ward = wardRepository.findById(model.getWardId()).orElse(null);
		if (ward != null)
			borrower.setWard(ward);
		borrower.setCountryId(model.getCountryId());
		borrower.setCountyId(model.getCountyId());
		borrower.setSubCountyId(model.getSubCountyId());
		borrower.setPhysicalAddress(model.getPhysicalAddress());
		borrower.setPostalAddress(model.getPostalAddress());
		borrower.setExternalRefrenceNo(model.getExternalRefrenceNo());

		// Members
		Set<MGroupMembers> members = new HashSet<>();
		if (model.getMembers().size() > 0) {
			for (GroupMembers member : model.getMembers()) {
				MGroupMembers gm = groupMembersRepository.findById(member.getId()).orElse(null);
				if (gm == null) {
					gm = new MGroupMembers();
				}
				gm.setGroupRepresentative(member.isGroupRepresentative());
				gm.setFirstName(member.getFirstName());
				gm.setLastName(member.getLastName());
				gm.setPhoneNumber(member.getPhoneNumber());
				gm.setResidence(member.getResidence());

				groupMembersRepository.save(gm);
				members.add(gm);
			}
		}
		borrower.setMembers(members);

		// Attachments

		Set<MDocuments> docs = model.getAttachments().stream().filter(d -> d.getAttachmentId() > 0).map(d -> {
			MDocuments doc = new MDocuments();

			if (d.getFileUpload() != null && !d.getFileUpload().isEmpty()) {
				FileUploads fileUpload = utils.uploadFileFromBase64(d.getFileUpload());
				if(fileUpload!=null) {
					doc.setFileName(fileUpload.getFileName());
					doc.setActualFilePath(fileUpload.getFullFilePath());
					doc.setMimeType(fileUpload.getMimeType());
					doc.setFileSize(fileUpload.getFileSize());
					doc.setMimeType(fileUpload.getMimeType());
					doc.setFileSize(fileUpload.getFileSize());
					if (doc.getFileName() == null) {
						throw new SetUpExceptions("Document file path is not configured in the system.");
					}
					doc.setFilepath(utils.getFilePath() + doc.getFileName());
					doc.setAD_Document_UU(UUID.randomUUID().toString());
					doc.setAttachment(attachmentRepository.findById(d.getAttachmentId()).get());
					return doc;
					
				}
				
			}

			return null;
		}).collect(Collectors.toSet());

		borrower.setAttachments(docs);

		MUser mUser = borrower.getGroupRepresentative();
		if (mUser == null) {
			MUser existingUser = userRepository.findTop1ByEmailAndIsActive(model.getContactEmail(), true);
			if (existingUser != null) {
				groupBorrowersRepository.delete(borrower);
				throw new SetUpExceptions("User with Email " + existingUser.getEmail()
						+ " already exists, please use a different email address.");
			}

			mUser = new MUser();
		}

		MBPartner partner = null;
		if (mUser.getC_BPartner_ID() > 0) {
			partner = mBPartnerRepository.findById(mUser.getC_BPartner_ID()).orElse(null);
		}

		if (partner == null) {
			partner = new MBPartner();
			partner.setC_BPartner_UU(UUID.randomUUID().toString());
		}

		partner.setActive(true);
		partner.setName(model.getGroupName());
		partner.setValue(model.getContactEmail());
		partner = mBPartnerRepository.save(partner);
		partner.setDocumentNo("CB/DRV/" + Utils.getCurrentYear() + "/" + partner.getId());
		partner = mBPartnerRepository.save(partner);
		if (borrower.getGroupRepresentative() != null) {
			mUser.setFirstName(borrower.getGroupRepresentative().getFirstName());
			mUser.setLastName(borrower.getGroupRepresentative().getLastName());

		}
		mUser.setFullName(model.getGroupName());
		mUser.setEmail(model.getContactEmail());

		mUser.setPhoneNumber(utils.formatPhoneNumber(model.getContactPhone(), "KE"));
		mUser.setExternalRefrenceNo(model.getExternalRefrenceNo());
		mUser.setRand(Utils.generateRandomPassword());
		mUser.setPassword(passwordEncoder.encode(mUser.getRand()));
		mUser.setAD_User_UU(UUID.randomUUID().toString());
		mUser.setC_BPartner_ID(partner.getId());
		mUser.setAdOrgId(partner.getAD_Org_ID());
		mUser.setAdClientId(partner.getAD_Client_ID());
		mUser.setApprovalStage("Approved");
		mUser.setApproved(true);
		mUser.setDocStatus("CO");

		Set<MRoles> roles = new HashSet<>();
		String roleName=utils.getDefaultUserRole();
		MRoles role = roleRepository.findTop1ByNameAndIsActiveAndAdOrgIDOrderByCreatedAsc(roleName, true, utils.getAD_Org_ID());
		if (role != null)
			roles.add(role);
		mUser.setRoles(roles);

		mUser = userRepository.save(mUser);
		mUser.setDocumentNo("USER/DRV/" + Utils.getCurrentYear() + "/" + mUser.getUserId());
		mUser = userRepository.save(mUser);

		borrower.setGroupRepresentative(mUser);

		message = (model.getGroupBorrowerId() == 0) ? "Group Borrower Created Successfully" : "Group Borrower Updated";
		code = 200;
		borrower = groupBorrowersRepository.save(borrower);
		borrower.setDocumentNo("GRP/BRW/" + Utils.getCurrentYear() + "/" + borrower.getGroupBorrowerId());
		borrower = groupBorrowersRepository.save(borrower);
		return new ResponseEntity<>(message, code, mapGroupBorrower(borrower));
	}

	public Page<InstitutionBorrowerResponse> getAllInstitutionBorrowers(int page, int size, String searchTerm) {
		if (searchTerm != null && !searchTerm.isEmpty()) {
			return institutionBorrowersRepository
					.searchInstitutionBorrower(true, utils.getAD_Org_ID(), searchTerm, PageRequest.of(page, size))
					.map(this::mapInstitutionBorrower);
		} else {
			return institutionBorrowersRepository.findByIsActiveAndAdOrgIDOrderByInstitutionBorrowerIdDesc(true,
					utils.getAD_Org_ID(), PageRequest.of(page, size)).map(this::mapInstitutionBorrower);
		}
	}

	@Transactional
	public ResponseEntity<InstitutionBorrowerResponse> createUpdateInstitutionBorrower(
			@Valid InstitutionBorrowerModel model) {
		String message = "Failed to create Institution Borrower";
		int code = 201;

		MInstitutionBorrower borrower = institutionBorrowersRepository.findById(model.getInstitutionBorrowerId())
				.orElse(new MInstitutionBorrower());

		// Uniqueness checks
		if (borrower.getInstitutionBorrowerId() == 0) {
			if (institutionBorrowersRepository.findTop1ByIsActiveAndAdOrgIDAndRegistrationNumber(true,
					utils.getAD_Org_ID(), model.getRegistrationNumber()) != null) {
				throw new SetUpExceptions("The Registration Number Provided is Already Registered");
			}
		}
		if (model.getContactPhone() == null) {
			throw new SetUpExceptions("Please include Institution contact number");
		}
		if (model.getContactEmail() == null) {
			throw new SetUpExceptions("Please include Institution contact email");

		}
		if (model.getInstitutionName() == null) {
			throw new SetUpExceptions("Please provide the name of the Institution");

		}

		if (model.getContactPerson() == null) {
			throw new SetUpExceptions(
					"Please provide the name of the person to be contacted on behalf of the Institution");

		}

		// Map model fields
		borrower.setInstitutionName(model.getInstitutionName());
		borrower.setRegistrationNumber(model.getRegistrationNumber());
		borrower.setRegistrationDate(model.getRegistrationDate());
		borrower.setTaxId(model.getTaxId());
		borrower.setContactPerson(model.getContactPerson());
		borrower.setContactPhone(model.getContactPhone());
		borrower.setContactEmail(model.getContactEmail());
		borrower.setSector(model.getSector());
		borrower.setAnnualRevenue(model.getAnnualRevenue());
		borrower.setNotes(model.getNotes());

		MWard ward = wardRepository.findById(model.getWardId()).orElse(null);
		if (ward != null)
			borrower.setWard(ward);
		borrower.setCountryId(model.getCountryId());
		borrower.setCountyId(model.getCountyId());
		borrower.setSubCountyId(model.getSubCountyId());
		borrower.setPhysicalAddress(model.getPhysicalAddress());
		borrower.setPostalAddress(model.getPostalAddress());
		borrower.setExternalRefrenceNo(model.getExternalRefrenceNo());

		// Attachments and Contacts
		Set<MNextOfKin> contacts = model.getAuthorizedContacts().stream().map(n -> {
			MNextOfKin mKin = new MNextOfKin();
			mKin.setFullName(n.getFullName());
			mKin.setPhoneNumber(n.getPhoneNumber());
			mKin.setRelationship(n.getRelationship());
			mKin.setAddress(n.getAddress());
			mKin.setEmail(n.getEmail());
			mKin.setNationalId(n.getNationalId());
			return mKin;
		}).collect(Collectors.toSet());
		borrower.setAuthorizedContacts(contacts);

		// Attachments
		Set<MDocuments> docs = model.getAttachments().stream().filter(d -> d.getAttachmentId() > 0).map(d -> {
			MDocuments doc = new MDocuments();

			if (d.getFileUpload() != null && !d.getFileUpload().isEmpty()) {
				FileUploads fileUpload = utils.uploadFileFromBase64(d.getFileUpload());
				if(fileUpload!=null) {
					doc.setFileName(fileUpload.getFileName());
					doc.setActualFilePath(fileUpload.getFullFilePath());
					doc.setMimeType(fileUpload.getMimeType());
					doc.setFileSize(fileUpload.getFileSize());
					doc.setFilepath(utils.getFilePath() + doc.getFileName());
					doc.setAD_Document_UU(UUID.randomUUID().toString());
					doc.setAttachment(attachmentRepository.findById(d.getAttachmentId()).get());
					return doc;
				}
				
			}

			return null;
		}).collect(Collectors.toSet());

		borrower.setAttachments(docs);

		MUser mUser = borrower.getRepresentative();
		if (mUser == null) {
			MUser existingUser = userRepository.findTop1ByEmailAndIsActive(model.getContactEmail(), true);
			if (existingUser != null) {
				institutionBorrowersRepository.delete(borrower);
				throw new SetUpExceptions("User with Email " + existingUser.getEmail()
						+ " already exists, please use a different email address.");
			}

			mUser = new MUser();
		}

		MBPartner partner = null;
		if (mUser.getC_BPartner_ID() > 0) {
			partner = mBPartnerRepository.findById(mUser.getC_BPartner_ID()).orElse(null);
		}

		if (partner == null) {
			partner = new MBPartner();
			partner.setC_BPartner_UU(UUID.randomUUID().toString());
		}

		partner.setActive(true);
		partner.setName(model.getInstitutionName());
		partner.setValue(model.getContactEmail());
		partner = mBPartnerRepository.save(partner);
		partner.setDocumentNo("CB/DRV/" + Utils.getCurrentYear() + "/" + partner.getId());
		partner = mBPartnerRepository.save(partner);

		mUser.setFullName(model.getInstitutionName());
		mUser.setEmail(model.getContactEmail());
		mUser.setExternalRefrenceNo(model.getExternalRefrenceNo());
		mUser.setPhoneNumber(utils.formatPhoneNumber(model.getContactPhone(), "KE"));
		mUser.setRand(Utils.generateRandomPassword());
		mUser.setPassword(passwordEncoder.encode(mUser.getRand()));
		mUser.setAD_User_UU(UUID.randomUUID().toString());
		mUser.setC_BPartner_ID(partner.getId());
		mUser.setAdOrgId(partner.getAD_Org_ID());
		mUser.setAdClientId(partner.getAD_Client_ID());
		mUser.setApprovalStage("Approved");
		mUser.setApproved(true);
		mUser.setDocStatus("CO");

		Set<MRoles> roles = new HashSet<>();
		String roleName=utils.getDefaultUserRole();
		MRoles role = roleRepository.findTop1ByNameAndIsActiveAndAdOrgIDOrderByCreatedAsc(roleName, true, utils.getAD_Org_ID());
		if (role != null)
			roles.add(role);
		mUser.setRoles(roles);

		mUser = userRepository.save(mUser);
		mUser.setDocumentNo("USER/DRV/" + Utils.getCurrentYear() + "/" + mUser.getUserId());
		mUser = userRepository.save(mUser);

		borrower.setRepresentative(mUser);

		borrower = institutionBorrowersRepository.save(borrower);
		borrower.setDocumentNo("INST/BRW/" + Utils.getCurrentYear() + "/" + borrower.getInstitutionBorrowerId());
		borrower = institutionBorrowersRepository.save(borrower);

		message = (model.getInstitutionBorrowerId() == 0) ? "Institution Borrower Created Successfully"
				: "Institution Borrower Updated";
		code = 200;
		return new ResponseEntity<>(message, code, mapInstitutionBorrower(borrower));
	}

	public ResponseEntity<GroupBorrowerResponse> deleteGroupBorrowerById(Long id) {
		String message = "Failed to delete Group Borrower. Please try again later or contact the system admin for assistance";
		int code = 201;

		MGroupDebtors borrower = groupBorrowersRepository.findById(id)
				.orElseThrow(() -> new SetUpExceptions("Group Borrower with ID " + id + " not found."));

		borrower.setActive(false);
		groupBorrowersRepository.save(borrower);

		message = "Group Borrower Deleted Successfully.";
		code = 200;
		GroupBorrowerResponse response = mapGroupBorrower(borrower);

		return new ResponseEntity<>(message, code, response);
	}

	public ResponseEntity<InstitutionBorrowerResponse> deleteInstitutionBorrowerById(Long id) {
		String message = "Failed to delete Institution Borrower. Please try again later or contact the system admin for assistance";
		int code = 201;

		MInstitutionBorrower borrower = institutionBorrowersRepository.findById(id)
				.orElseThrow(() -> new SetUpExceptions("Institution Borrower with ID " + id + " not found."));

		borrower.setActive(false);
		institutionBorrowersRepository.save(borrower);

		message = "Institution Borrower Deleted Successfully.";
		code = 200;
		InstitutionBorrowerResponse response = mapInstitutionBorrower(borrower);

		return new ResponseEntity<>(message, code, response);
	}

	public Page<IndividualBorrowerResponse> getAllIndividualBorrowers(int page, int size, String searchTerm) {

		if (searchTerm != null && !searchTerm.isEmpty()) {
			return individualBorrowersRepository
					.searchIndividualBorrowers(true, utils.getAD_Org_ID(), searchTerm, PageRequest.of(page, size))
					.map(this::mapIndividualBorrowers);
		} else {
			return individualBorrowersRepository.findByIsActiveAndAdOrgIDOrderByIndividualBorrowerIdDesc(true,
					utils.getAD_Org_ID(), PageRequest.of(page, size)).map(this::mapIndividualBorrowers);
		}

	}

	public ResponseEntity<IndividualBorrowerResponse> deleteIndividualBorrowerById(Long id) {
		String message = "Failed to delete Borrower. Please try again later or contact the system admin for assistance";
		int code = 201;
		MDebtor borrower = individualBorrowersRepository.findById(id).get();
		borrower.setActive(false);
		individualBorrowersRepository.save(borrower);
		message = "Borrower Deleted Successfully.";
		code = 200;
		IndividualBorrowerResponse response = mapIndividualBorrowers(borrower);

		return new ResponseEntity<>(message, code, response);

	}

	@Transactional
	public ResponseEntity<IndividualBorrowerResponse> createUpdateIndividualBorrowers(
			@Valid IndividualBorrowerModel model) {
		String message = "Failed to create Borrower. Please try again later or contact the system admin for assistance";
		int code = 201;

		MDebtor borrower = individualBorrowersRepository.findById(model.getIndividualBorrowerId()).orElse(null);

		if (borrower == null) {
			MDebtor existingBorrowerIdNumber = individualBorrowersRepository
					.findTop1ByIsActiveAndAdOrgIDAndNationalId(true, utils.getAD_Org_ID(), model.getNationalId());

			if (existingBorrowerIdNumber != null) {
				throw new SetUpExceptions("The National ID Number Provided is Already registered");
			}

			MDebtor existingBorrowerPhoneNumber = individualBorrowersRepository
					.findTop1ByIsActiveAndAdOrgIDAndPhone(true, utils.getAD_Org_ID(), model.getPhone());

			if (existingBorrowerPhoneNumber != null) {
				throw new SetUpExceptions("The Phone Number Provided is Already registered");
			}

			MDebtor existingBorrowerEmail = individualBorrowersRepository.findTop1ByIsActiveAndAdOrgIDAndEmail(true,
					utils.getAD_Org_ID(), model.getEmail());

			if (existingBorrowerEmail != null) {
				throw new SetUpExceptions("The Email Provided is Already registered");
			}

			borrower = new MDebtor();

		}

		// Step 1: Personal Information
		borrower.setFirstName(model.getFirstName());
		borrower.setMiddleName(model.getMiddleName());
		borrower.setLastName(model.getLastName());
		borrower.setGender(model.getGender());
		borrower.setDob(model.getDob());
		borrower.setMaritalStatus(model.getMaritalStatus());
		borrower.setEducationLevel(model.getEducationLevel());
		borrower.setNationalId(model.getNationalId());

		// Step 2: Contact Information
		borrower.setPhone(model.getPhone());
		borrower.setEmail(model.getEmail());
		borrower.setCountryId(model.getCountryId());
		borrower.setCountyId(model.getCountyId());
		borrower.setSubCountyId(model.getSubCountyId());

		MWard ward = wardRepository.findById(model.getWardId()).orElse(null);
		if (ward != null) {
			borrower.setWard(ward);
		}
		borrower.setCountryId(model.getCountryId());
		borrower.setCountyId(model.getCountyId());
		borrower.setSubCountyId(model.getSubCountyId());

		borrower.setLocation(model.getLocation());
		borrower.setPhysicalAddress(model.getPhysicalAddress());
		borrower.setPostalAddress(model.getPostalAddress());

		// Step 3: Employment / Income Info
		borrower.setEmploymentStatus(model.getEmploymentStatus());
		borrower.setOccupation(model.getOccupation());
		borrower.setEmployer(model.getEmployer());
		borrower.setMonthlyIncome(model.getMonthlyIncome());
		borrower.setOtherIncome(model.getOtherIncome());

		// Step 6: Internal Details
		MUser loanOfficer = userRepository.findById(model.getLoanOfficerId()).orElse(null);
		if (loanOfficer != null) {
			borrower.setLoanOfficer(loanOfficer);
		}

		borrower.setReferralSource(model.getReferralSource());
		borrower.setRiskRating(model.getRiskRating());
		borrower.setNotes(model.getNotes());

		Set<MNextOfKin> nextOfKins = model.getBorrowerNextOfKins().stream().map(n -> {
			MNextOfKin mKin = new MNextOfKin();
			mKin.setFullName(n.getFullName());
			mKin.setPhoneNumber(n.getPhoneNumber());
			mKin.setRelationship(n.getRelationship());
			mKin.setAddress(n.getAddress());
			mKin.setEmail(n.getEmail());
			mKin.setNationalId(n.getNationalId());
			
			return mKin;
		}).collect(Collectors.toSet());
		borrower.setBorrowerNextOfKins(nextOfKins);

		// Attachments
		Set<MDocuments> docs = model.getBorrowerAttachments().stream().filter(d -> d.getAttachmentId() > 0).map(d -> {
			MDocuments doc = new MDocuments();

			if (d.getFileUpload() != null && !d.getFileUpload().isEmpty()) {
				FileUploads fileUpload = utils.uploadFileFromBase64(d.getFileUpload());
				if(fileUpload!=null) {
					doc.setFileName(fileUpload.getFileName());
					doc.setActualFilePath(fileUpload.getFullFilePath());
					doc.setMimeType(fileUpload.getMimeType());
					doc.setFileSize(fileUpload.getFileSize());
					doc.setFilepath(utils.getFilePath() + doc.getFileName());
					doc.setColleteralOwner(d.getColleteralOwner());
					doc.setColleteralValue(d.getColleteralValue());
					doc.setExpiryDate(d.getExpiryDate());
					doc.setStorageDurationDaysOnLoanCompletion(d.getStorageDurationDaysOnLoanCompletion());
					doc.setColleteralNo(d.getColleteralNo());
					doc.setAD_Document_UU(UUID.randomUUID().toString());
					doc.setAttachment(attachmentRepository.findById(d.getAttachmentId()).orElse(null));
					return doc;
				}
				
			}

			return null;
		}).collect(Collectors.toSet());

		borrower.setBorrowerAttachments(docs);
		borrower.setExternalRefrenceNo(model.getExternalRefrenceNo());
		borrower.setEligibleToPay(model.isEligibleToPay());

		// Save to DB
		borrower = individualBorrowersRepository.save(borrower);
		borrower.setDocumentNo("INDV/BRW/" + Utils.getCurrentYear() + "/" + borrower.getIndividualBorrowerId());
		borrower = individualBorrowersRepository.save(borrower);

		MUser mUser = borrower.getUser();
		if (mUser == null) {
			MUser existingUser = userRepository.findTop1ByEmailAndIsActive(model.getEmail(), true);
			if (existingUser != null) {
				individualBorrowersRepository.delete(borrower);
				throw new SetUpExceptions("User with Email " + existingUser.getEmail()
						+ " already exists, please use a different email address.");
			}

			mUser = new MUser();
		}

		MBPartner partner = null;
		if (mUser.getC_BPartner_ID() > 0) {
			partner = mBPartnerRepository.findById(mUser.getC_BPartner_ID()).orElse(null);
		}

		if (partner == null) {
			partner = new MBPartner();
			partner.setC_BPartner_UU(UUID.randomUUID().toString());
		}

		partner.setActive(true);
		partner.setName(model.getFirstName() + " " + model.getLastName());
		partner.setValue(model.getEmail());
		partner = mBPartnerRepository.save(partner);
		partner.setDocumentNo("CB/DRV/" + Utils.getCurrentYear() + "/" + partner.getId());
		partner = mBPartnerRepository.save(partner);

		mUser.setFirstName(borrower.getFirstName());
		mUser.setLastName(borrower.getLastName());
		mUser.setFullName(partner.getName());
		mUser.setEmail(model.getEmail());
		mUser.setExternalRefrenceNo(model.getExternalRefrenceNo());
		mUser.setPhoneNumber(utils.formatPhoneNumber(model.getPhone(), "KE"));
		mUser.setRand(Utils.generateRandomPassword());
		mUser.setPassword(passwordEncoder.encode(mUser.getRand()));
		mUser.setAD_User_UU(UUID.randomUUID().toString());
		mUser.setC_BPartner_ID(partner.getId());
		mUser.setAdOrgId(partner.getAD_Org_ID());
		mUser.setAdClientId(partner.getAD_Client_ID());
		mUser.setApprovalStage("Approved");
		mUser.setApproved(true);
		mUser.setDocStatus("CO");

		Set<MRoles> roles = new HashSet<>();
		String roleName=utils.getDefaultUserRole();
		MRoles role = roleRepository.findTop1ByNameAndIsActiveAndAdOrgIDOrderByCreatedAsc(roleName, true, utils.getAD_Org_ID());
		if (role != null)
			roles.add(role);
		mUser.setRoles(roles);

		mUser = userRepository.save(mUser);
		mUser.setDocumentNo("USER/DRV/" + Utils.getCurrentYear() + "/" + mUser.getUserId());
		mUser = userRepository.save(mUser);

		borrower.setUser(mUser);

		message = (model.getIndividualBorrowerId() == 0) ? "Debtor Registered successfully"
				: "Debtor's details updated successfully";
		code = 200;
		IndividualBorrowerResponse response = mapIndividualBorrowers(borrower);

		return new ResponseEntity<>(message, code, response);
	}

	public IndividualBorrowerResponse mapIndividualBorrowers(MDebtor b) {
		IndividualBorrowerResponse response = new IndividualBorrowerResponse();

		// Step 1: Personal Information
		response.setIndividualBorrowerId(b.getIndividualBorrowerId());
		response.setFirstName(b.getFirstName());
		response.setMiddleName(b.getMiddleName());
		response.setLastName(b.getLastName());
		response.setGender(b.getGender());
		response.setDob(b.getDob());
		response.setMaritalStatus(b.getMaritalStatus());
		response.setExternalRefrenceNo(b.getExternalRefrenceNo());
		response.setEducationLevel(b.getEducationLevel());
		response.setNationalId(b.getNationalId());
		if (b.getUser() != null) {
			response.setUser(utils.mapUser(b.getUser()));

		}
		response.setGroupRepresentative(b.isGroupRepresentative());
		// Step 2: Contact Information
		response.setPhone(b.getPhone());
		response.setEmail(b.getEmail());
		response.setCounty(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getName() : "N/A");
		response.setSubCounty(b.getWard() != null ? b.getWard().getSubCounty().getName() : "N/A");
		response.setWard(b.getWard() != null ? b.getWard().getName() : "N/A");

		response.setCountryId(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getCountry().getCountryId()
				: b.getCountryId());
		response.setCountyId(
				b.getWard() != null ? b.getWard().getSubCounty().getCounty().getCountyId() : b.getCountyId());
		response.setSubCountyId(b.getWard() != null ? b.getWard().getSubCounty().getSubContyId() : b.getSubCountyId());
		response.setWardId(b.getWard() != null ? b.getWard().getWardId() : 0);

		response.setLocation(b.getLocation());
		response.setPhysicalAddress(b.getPhysicalAddress());
		response.setPostalAddress(b.getPostalAddress());

		// Step 3: Employment / Income Info
		response.setEmploymentStatus(b.getEmploymentStatus());
		response.setOccupation(b.getOccupation());
		response.setEmployer(b.getEmployer());
		response.setMonthlyIncome(b.getMonthlyIncome());
		response.setOtherIncome(b.getOtherIncome());

		// Step 6: Internal Details
		response.setLoanOfficer(utils.mapUserBreif(b.getLoanOfficer()));
		response.setReferralSource(b.getReferralSource());
		response.setRiskRating(b.getRiskRating());
		response.setNotes(b.getNotes());

		// Metadata
		response.setActive(b.isActive());
		response.setDocumentNo(b.getDocumentNo());
		response.setDocStatus(b.getDocStatus());
		response.setApprovalStage(b.getApprovalStage());
		response.setCreated(b.getCreated());
		response.setUpdated(b.getUpdated());
		response.setEligibleToPay(b.isEligibleToPay());

		// Attachments and Next of Kins
		response.setBorrowerAttachments(mappBorrowerAttachments(b.getBorrowerAttachments()));
		response.setBorrowerNextOfKins(mappBorrowerNextOfKins(b.getBorrowerNextOfKins()));

		return response;
	}

	public Set<BorrowerAttachments> mappBorrowerAttachments(Set<MDocuments> docs) {
		Set<BorrowerAttachments> attachments = new HashSet<>();
		if (docs.size() > 0) {
			for (MDocuments doc : docs) {
				BorrowerAttachments attach = new BorrowerAttachments();
				attach.setAttachmentId(doc.getAttachmentDocumentId());
				attach.setActive(doc.isActive());
				attach.setCreated(doc.getCreated());
				attach.setDocStatus(doc.getDocStatus().getDescription());

				if (doc.getAttachment() != null) {
					attach.setAttachmentType(doc.getAttachment().getAttachmentType().getDescription());
					attach.setAttachmentName(doc.getAttachment().getName());
				}

				attach.setDocumentNo(doc.getDocumentNo());
				attach.setFileName(doc.getFileName());
				attach.setFilepath(doc.getFilepath());
				attach.setUpdated(doc.getUpdated());
				attach.setActualFilePath(doc.getActualFilePath());
				attach.setFileSize(doc.getFileSize());
				attach.setMimeType(doc.getMimeType());
				attach.setColleteralOwner(doc.getColleteralOwner());
				attach.setColleteralValue(doc.getColleteralValue());
				attach.setExpiryDate(doc.getExpiryDate());
				attach.setStorageDurationDaysOnLoanCompletion(doc.getStorageDurationDaysOnLoanCompletion());
				attach.setColleteralNo(doc.getColleteralNo());
				attachments.add(attach);

			}
		}
		return attachments;
	}

	public Set<NextOfKins> mappBorrowerNextOfKins(Set<MNextOfKin> kins) {
		Set<NextOfKins> nextOfKins = new HashSet<>();
		if (kins.size() > 0) {
			for (MNextOfKin kin : kins) {
				NextOfKins nextkin = new NextOfKins();
				nextkin.setNextOfKinId(kin.getNextOfKinId());
				nextkin.setActive(kin.isActive());
				nextkin.setCreated(kin.getCreated());
				nextkin.setDocStatus(kin.getDocStatus());
				nextkin.setDocumentNo(kin.getDocumentNo());
				nextkin.setUpdated(kin.getUpdated());
				nextkin.setAddress(kin.getAddress());
				nextkin.setFullName(kin.getFullName());
				nextkin.setPhoneNumber(kin.getPhoneNumber());
				nextkin.setRelationship(kin.getRelationship());
				nextkin.setEmail(kin.getEmail());
				nextkin.setNationalId(kin.getNationalId());

				nextOfKins.add(nextkin);

			}
		}
		return nextOfKins;
	}

	public InstitutionBorrowerResponse mapInstitutionBorrower(MInstitutionBorrower b) {
		InstitutionBorrowerResponse r = new InstitutionBorrowerResponse();
		r.setInstitutionBorrowerId(b.getInstitutionBorrowerId());
		r.setInstitutionName(b.getInstitutionName());
		r.setRegistrationNumber(b.getRegistrationNumber());
		r.setRegistrationDate(b.getRegistrationDate());
		r.setTaxId(b.getTaxId());
		r.setExternalRefrenceNo(b.getExternalRefrenceNo());
		r.setContactPerson(b.getContactPerson());
		r.setContactPhone(b.getContactPhone());
		r.setContactEmail(b.getContactEmail());
		r.setSector(b.getSector());
		r.setAnnualRevenue(b.getAnnualRevenue());
		r.setNotes(b.getNotes());

		r.setCountry("Kenya"); // You may map properly if needed
		r.setCounty(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getName() : "N/A");
		r.setSubCounty(b.getWard() != null ? b.getWard().getSubCounty().getName() : "N/A");
		r.setWard(b.getWard() != null ? b.getWard().getName() : "N/A");

		r.setCountryId(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getCountry().getCountryId()
				: b.getCountryId());
		r.setCountyId(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getCountyId() : b.getCountyId());
		r.setSubCountyId(b.getWard() != null ? b.getWard().getSubCounty().getSubContyId() : b.getSubCountyId());
		r.setWardId(b.getWard() != null ? b.getWard().getWardId() : 0);
		r.setPhysicalAddress(b.getPhysicalAddress());
		r.setPostalAddress(b.getPostalAddress());

		r.setActive(b.isActive());
		r.setDocumentNo(b.getDocumentNo());
		r.setDocStatus(b.getDocStatus());
		r.setApprovalStage(b.getApprovalStage());
		r.setCreated(b.getCreated());
		r.setUpdated(b.getUpdated());

		r.setAuthorizedContacts(mappBorrowerNextOfKins(b.getAuthorizedContacts()));
		r.setAttachments(mappBorrowerAttachments(b.getAttachments()));
		return r;
	}

	public GroupBorrowerResponse mapGroupBorrower(MGroupDebtors b) {
		GroupBorrowerResponse r = new GroupBorrowerResponse();
		r.setGroupBorrowerId(b.getGroupBorrowerId());
		r.setGroupName(b.getGroupName());
		r.setRegistrationNumber(b.getRegistrationNumber());
		r.setExternalRefrenceNo(b.getExternalRefrenceNo());
		r.setFormationDate(b.getFormationDate());
		r.setGroupType(b.getGroupType());
		r.setContactPhone(b.getContactPhone());
		r.setContactEmail(b.getContactEmail());
		r.setMeetingFrequency(b.getMeetingFrequency());
		r.setMeetingPlace(b.getMeetingPlace());
		r.setLoanOfficer(b.getLoanOfficer());
		r.setNotes(b.getNotes());

		r.setCountry("Kenya");
		r.setCounty(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getName() : "N/A");
		r.setSubCounty(b.getWard() != null ? b.getWard().getSubCounty().getName() : "N/A");
		r.setWard(b.getWard() != null ? b.getWard().getName() : "N/A");
		r.setPhysicalAddress(b.getPhysicalAddress());
		r.setPostalAddress(b.getPostalAddress());

		r.setCountryId(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getCountry().getCountryId()
				: b.getCountryId());
		r.setCountyId(b.getWard() != null ? b.getWard().getSubCounty().getCounty().getCountyId() : b.getCountyId());
		r.setSubCountyId(b.getWard() != null ? b.getWard().getSubCounty().getSubContyId() : b.getSubCountyId());
		r.setWardId(b.getWard() != null ? b.getWard().getWardId() : 0);

		r.setActive(b.isActive());
		r.setDocumentNo(b.getDocumentNo());
		r.setDocStatus(b.getDocStatus());
		r.setApprovalStage(b.getApprovalStage());
		r.setCreated(b.getCreated());
		r.setUpdated(b.getUpdated());

		// Attachments
		r.setAttachments(mappBorrowerAttachments(b.getAttachments()));

		// Members
		Set<GroupMembersResponse> memberResponses = b.getMembers().stream().map(this::mapMGroupMembers)
				.collect(Collectors.toSet());
		r.setMembers(memberResponses);

		return r;
	}

	public GroupMembersResponse mapMGroupMembers(MGroupMembers m) {
		if (m == null) {
			return null;
		} else {
			GroupMembersResponse response = new GroupMembersResponse();
			response.setFirstName(m.getFirstName());
			response.setGroupMemberId(m.getGroupMemberId());
			response.setGroupRepresentative(m.isGroupRepresentative());
			response.setLastName(m.getLastName());
			response.setPhoneNumber(m.getPhoneNumber());
			response.setResidence(m.getResidence());
			return response;
		}
	}

}
