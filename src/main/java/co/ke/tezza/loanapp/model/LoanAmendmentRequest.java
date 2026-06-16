package co.ke.tezza.loanapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import co.ke.tezza.loanapp.enums.AmendmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanAmendmentRequest {
    
    // Basic Information
    @NotNull(message = "Loan ID is required")
    private Long loanId;
     
    private String requestReason;
    
    // Amendments Data
    @NotNull(message = "At least one amendment must be selected")
    private List<AmendmentDetail> amendments = new ArrayList<>();
  
    
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AmendmentDetail {
        @NotNull(message = "Amendment configuration ID is required")
        private Long amendmentConfigId;
                
        // Amendment-specific data
        private BigDecimal newPrincipalAmount;
        private BigDecimal newInterestRate;
        private BigDecimal newFlatRateAmount;
        private Long newLoanProductId;
        private AmendmentType amendmentType;
        private Integer newTermInDays;
        private LocalDateTime effectiveDate;
        private String amendmentReason;
        
       
    }
    
    
}