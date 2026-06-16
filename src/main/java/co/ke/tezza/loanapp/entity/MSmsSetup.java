package co.ke.tezza.loanapp.entity;

import java.util.Random;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator;

import co.ke.tezza.loanapp.enums.SmsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Sms_Setup")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MSmsSetup extends AuditModel{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Sms_Setup_ID")
    private Long smsSetupId;

    @Column(columnDefinition = "TEXT")
    private String messageTemplate; 

    @Enumerated(EnumType.STRING)
    private SmsTypeEnum smsType;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean debt;
    
}
