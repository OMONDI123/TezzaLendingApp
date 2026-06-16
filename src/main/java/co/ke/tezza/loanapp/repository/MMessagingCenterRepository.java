package co.ke.tezza.loanapp.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.ke.tezza.loanapp.entity.MMessagingCenter;
import co.ke.tezza.loanapp.enums.BorrowerTypeEnum;
import co.ke.tezza.loanapp.enums.MessageStatus;

@Repository
public interface MMessagingCenterRepository extends JpaRepository<MMessagingCenter, Long> {
    
    // =========================================================================
    // BORROWER/LOAN RELATED METHODS
    // =========================================================================
    
    Page<MMessagingCenter> findByIsActiveAndAdOrgIDAndMessageStatus(boolean active, long orgId,
            MessageStatus messageStatus, Pageable pageable);

    Page<MMessagingCenter> findByIsActiveAndAdOrgID(boolean active, long orgId, Pageable pageable);

    @Query("SELECT m FROM MMessagingCenter m " + "WHERE m.isActive = :active " + "AND m.adOrgID = :orgId "
            + "AND ( m.messageStatus = :messageStatus) " + "AND  ("
            + "LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND (m.messagingTime >= :dateFrom) " + "AND (m.messagingTime <= :dateTo)")
    Page<MMessagingCenter> searchMessages(@Param("active") boolean active, @Param("orgId") long orgId,
            @Param("messageStatus") MessageStatus messageStatus, @Param("searchTerm") String searchTerm,
            @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, Pageable pageable);

    @Query("SELECT m FROM MMessagingCenter m " + "WHERE m.isActive = :active " + "AND m.adOrgID = :orgId "
            + "AND ( m.messageStatus = :messageStatus) " + "AND  ("
            + "LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND (m.messagingTime >= :dateFrom) " + "AND (m.messagingTime <= :dateTo) "
            + "AND (m.receiverId = :receiverId) " + "AND (m.borrowerType = :borrowerType)")
    Page<MMessagingCenter> searchMessagesByBorrowerId(@Param("active") boolean active, @Param("orgId") long orgId,
            @Param("messageStatus") MessageStatus messageStatus, @Param("searchTerm") String searchTerm,
            @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
            @Param("borrowerType") BorrowerTypeEnum borrowerType, @Param("receiverId") long receiverId,
            Pageable pageable);

    @Query("SELECT m FROM MMessagingCenter m " + "WHERE m.isActive = :active " + "AND m.adOrgID = :orgId " + "AND ("
            + "LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND ( m.messagingTime >= :dateFrom) " + "AND ( m.messagingTime <= :dateTo)")
    Page<MMessagingCenter> searchMessagesNoStatus(@Param("active") boolean active, @Param("orgId") long orgId,
            @Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo,
            Pageable pageable);

    @Query("SELECT m FROM MMessagingCenter m "
            + "WHERE m.receiverId=:receiverId AND m.borrowerType=:borrowerType AND  m.isActive = :active "
            + "AND m.adOrgID = :orgId " + "AND (" + "LOWER(m.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(m.receiverEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND ( m.messagingTime >= :dateFrom) " + "AND ( m.messagingTime <= :dateTo)")
    Page<MMessagingCenter> searchMessagesNoStatusByBorrowerId(@Param("active") boolean active,
            @Param("orgId") long orgId, @Param("searchTerm") String searchTerm, @Param("dateFrom") Date dateFrom,
            @Param("dateTo") Date dateTo, @Param("borrowerType") BorrowerTypeEnum borrowerType,
            @Param("receiverId") long receiverId, Pageable pageable);

    Page<MMessagingCenter> findByIsActiveAndAdOrgIDAndMessageStatusAndCreatedBetween(boolean b, long ad_Org_ID,
            MessageStatus fromValue, Date dateFrom, Date dateTo, Pageable pageable);
    
    Page<MMessagingCenter> findByIsActiveAndAdOrgIDAndMessageStatusAndCreatedBetweenAndReceiverIdAndBorrowerTypeOrderByMessagingTimeDesc(
            boolean b, long ad_Org_ID, MessageStatus fromValue, Date dateFrom, Date dateTo, long receiverId, 
            BorrowerTypeEnum borrowerType, Pageable pageable);

    Page<MMessagingCenter> findByIsActiveAndAdOrgIDAndCreatedBetween(boolean b, long ad_Org_ID, Date dateFrom,
            Date dateTo, Pageable pageable);
    
    Page<MMessagingCenter> findByIsActiveAndAdOrgIDAndCreatedBetweenAndReceiverIdAndBorrowerTypeOrderByMessagingTimeDesc(
            boolean b, long ad_Org_ID, Date dateFrom, Date dateTo, long receiverId, 
            BorrowerTypeEnum borrowerType, Pageable pageable);
    
    List<MMessagingCenter> findByIsActiveAndMessageStatusAndMessagingTimeLessThanEqual(boolean active,
            MessageStatus status, Date now);
  
}