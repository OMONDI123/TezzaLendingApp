package co.ke.tezza.loanapp.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.response.UserResponse;

@Repository
public interface UserRepository extends JpaRepository<MUser, Long> {

	@Query(value = "SELECT u.* FROM ad_user u " + "WHERE u.ad_org_id = :orgId "
			+ "AND (u.fullname ILIKE '%' || :search || '%' " + "OR u.email ILIKE '%' || :search || '%' "
			+ "OR u.phone_number ILIKE '%' || :search || '%' " + "OR u.first_name ILIKE '%' || :search || '%' "
			+ "OR u.last_name ILIKE '%' || :search || '%' " + "OR u.documentno ILIKE '%' || :search || '%' "
			+ "OR u.ad_user_uu ILIKE '%' || :search || '%') "
			+ "ORDER BY u.created DESC", countQuery = "SELECT COUNT(u.ad_user_id) FROM ad_user u "
					+ "WHERE u.ad_org_id = :orgId " + "AND (u.fullname ILIKE '%' || :search || '%' "
					+ "OR u.email ILIKE '%' || :search || '%' " + "OR u.phone_number ILIKE '%' || :search || '%' "
					+ "OR u.first_name ILIKE '%' || :search || '%' " + "OR u.last_name ILIKE '%' || :search || '%' "
					+ "OR u.documentno ILIKE '%' || :search || '%' "
					+ "OR u.ad_user_uu ILIKE '%' || :search || '%')", nativeQuery = true)
	Page<MUser> searchUser(@Param("orgId") long orgId, @Param("search") String search, Pageable pageable);

	MUser findTop1ByEmailAndIsActive(String userName, boolean isActive);

	MUser findTop1ByIpAndIsActive(String userName, boolean isActive);

	MUser findTop1ByEmail(String userName);

	MUser findTop1ByEmailAndUserIdNotIn(String userName, List<Long> userIds);

	MUser findByOtpCode(int otp);

	MUser findTop1ByIsActiveAndReferralCode(boolean isActive, String referralCode);

	Page<MUser> findByIsActiveOrderByUserIdAsc(boolean isActive, Pageable pageable);

//	@Query(value = "SELECT * FROM ad_user where email=?1", nativeQuery = true)
//	MUser findByUserName(String userName);

	Page<MUser> findByCreatedByOrderByUserIdAsc(long createdBy, Pageable pageable);

	Page<MUser> findByAdOrgIdOrderByNoOftimesLoggedInDesc(long ad_Org_ID, Pageable pageable);

	Page<MUser> findByIsActiveAndAdOrgIdAndReferredByOrderByUserIdDesc(boolean isActive, long ad_Org_ID,
			MUser referedBy, Pageable pageable);

	MUser findTop1ByEmailAndIsActiveAndPhoneNumber(String email, boolean isActive, String phone);

	MUser findTop1ByPhoneNumber(String phoneNo);

	MUser findTop1ByIsActiveAndPhoneNumber(boolean b, String formatPhoneNumber);

	List<MUser> findByRolesAndIsActiveAndAdOrgId(MRoles nextRole, boolean b, long ad_Org_ID);


	@Query("SELECT DISTINCT u FROM MUser u " + "JOIN u.roles r " + "JOIN r.allowedOrganisations o "
			+ "WHERE r IN :roles " + "AND u.isActive = :isActive " + "AND o.id = :orgId "
			+ "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR u.phoneNumber LIKE CONCAT('%', :searchTerm, '%'))")
	List<MUser> searchAssignees(@Param("roles") Set<MRoles> roles, @Param("isActive") boolean isActive,
			@Param("orgId") long orgId, @Param("searchTerm") String searchTerm);

	@Query("SELECT DISTINCT u FROM MUser u " + "JOIN u.roles r " + "JOIN r.allowedOrganisations o "
			+ "WHERE r IN :roles " + "AND u.isActive = :isActive " + "AND o.id = :orgId")
	List<MUser> findByRolesAndIsActiveAndOrganisation(@Param("roles") Set<MRoles> roles,
			@Param("isActive") boolean isActive, @Param("orgId") long orgId);

	@Query(value = " SELECT DISTINCT u.* FROM AD_User u \n"
			+ "INNER JOIN AD_User_Role ur ON ur.muser_ad_user_id=u.ad_user_id\n"
			+ "INNER JOIN AD_Role r ON r.AD_Role_ID=ur.roles_ad_role_id\n"
			+ "INNER JOIN AD_Role_Org_Access rg ON rg.mroles_AD_Role_ID=r.AD_Role_ID\n"
			+ "INNER JOIN AD_Org g ON g.AD_Org_ID=rg.allowed_organisations_ad_org_id\n"
			+ "WHERE g.AD_Org_ID IN (:adOrgIds) AND r.AD_ROLE_ID=:roleId  AND u.isactive=true  LIMIT 10", nativeQuery = true)
	List<MUser> getUserByRole(@Param("roleId") long roleId, @Param("adOrgIds") List<Long> adOrgIds);

	@Query(value = "SELECT DISTINCT u.* FROM AD_User u "
			+ "INNER JOIN AD_User_Role ur ON ur.muser_ad_user_id = u.ad_user_id "
			+ "INNER JOIN AD_Role r ON r.AD_Role_ID = ur.roles_ad_role_id "
			+ "INNER JOIN AD_Role_Org_Access rg ON rg.mroles_AD_Role_ID = r.AD_Role_ID "
			+ "INNER JOIN AD_Org g ON g.AD_Org_ID = rg.allowed_organisations_ad_org_id "
			+ "WHERE g.AD_Org_ID IN (:adOrgIds) " + "AND r.AD_ROLE_ID = :roleId " + "AND u.isactive = true "
			+ "AND (LOWER(u.fullname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
			+ "OR u.phone_Number LIKE CONCAT('%', :searchTerm, '%'))  LIMIT 10", nativeQuery = true)
	List<MUser> getUserByRoleAndSearch(@Param("roleId") long roleId, @Param("adOrgIds") List<Long> adOrgIds,
			@Param("searchTerm") String searchTerm);

	MUser findTop1ByIsActiveAndOtpCode(boolean b, int otpVerificationCode);

}
