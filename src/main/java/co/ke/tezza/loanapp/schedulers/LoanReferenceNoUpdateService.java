package co.ke.tezza.loanapp.schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.ke.tezza.loanapp.entity.MLoanApplication;
import co.ke.tezza.loanapp.repository.LoanApplicationRepository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LoanReferenceNoUpdateService {
    
    @Autowired
    private LoanApplicationRepository loanApplicationRepository;
    
    private static final Pattern REF_NO_PATTERN = 
        Pattern.compile("^APPL/DB/\\d{4}/(\\d+)$");
    
    private static final Pattern REF_NO_PATTERN_LOAN = 
            Pattern.compile("^APPL/LN/\\d{4}/(\\d+)$");
    /**
     * Runs only once on application startup
     */
    @PostConstruct
    public void updateLoanReferenceNumbersOnStartup() {
        System.out.println("Starting loan reference number update...");
        
        List<MLoanApplication> loans = loanApplicationRepository.findAll();
        int updatedCount = 0;
        int updatedCountLoan=0;
        
        for (MLoanApplication loan : loans) {
            if (updateAPPL_DB_CurrentYear_LoanIdToDB_LoanId(loan)) {
                updatedCount++;
            }
            if(updateAPPL_LN_CurrentYear_LoanIdToDB_LoanId(loan)) {
            	updatedCountLoan++;
            }
        }
        
        if (updatedCount > 0 || updatedCountLoan>0) {
            loanApplicationRepository.saveAll(loans);
            System.out.println("Updated " + updatedCount + " debt reference numbers");
            System.out.println("Updated " + updatedCountLoan + " loan reference numbers");
        } else {
            System.out.println("No loan reference numbers needed updating");
        }
    }
    
    
    
    
    /**
     * Updates individual loan reference number from APPL/DB/YYYY/000367 to DB/000367
     * @return true if the reference was updated, false otherwise
     */
    private boolean updateAPPL_DB_CurrentYear_LoanIdToDB_LoanId(MLoanApplication loan) {
        if (loan.getDocumentNo() != null) {
            Matcher matcher = REF_NO_PATTERN.matcher(loan.getDocumentNo());
            
            if (matcher.matches()) {
                String loanId = matcher.group(1); // Extract the 000367 part
                String newReferenceNo = "DB/" + loanId;
                loan.setDocumentNo(newReferenceNo);
                return true;
            }
        }
        return false;
    }
    /**
     * Updates individual loan reference number from APPL/DB/YYYY/000367 to DB/000367
     * @return true if the reference was updated, false otherwise
     */
    private boolean updateAPPL_LN_CurrentYear_LoanIdToDB_LoanId(MLoanApplication loan) {
        if (loan.getDocumentNo() != null) {
            Matcher matcher = REF_NO_PATTERN_LOAN.matcher(loan.getDocumentNo());
            
            if (matcher.matches()) {
                String loanId = matcher.group(1); // Extract the 000367 part
                String newReferenceNo = "LN/" + loanId;
                loan.setDocumentNo(newReferenceNo);
                return true;
            }
        }
        return false;
    }
}