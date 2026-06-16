package co.ke.tezza.loanapp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import co.ke.tezza.loanapp.enums.AttachmentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AD_Attachment")
public class MAttachment extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Attachment_ID")
    private long attachmentId;

    @Column(name = "AD_Attachment_UU", nullable = false, unique = true)
    private String AD_Attachment_UU;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = true)
    private AttachmentType attachmentType;

    @Column(name = "name", nullable = false)
    private String name;
}
