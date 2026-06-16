package co.ke.tezza.loanapp.entity;

import java.util.UUID;

import javax.persistence.*;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Sub_Menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class MADSubMenu extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Sub_Menu_ID")
    private long id;

    private String title;

    private String view;

    @Column(name = "AD_Sub_Menu_UU")
    private String AD_Sub_Menu_UU;
    
    private long subMenuOrder;

   
}
